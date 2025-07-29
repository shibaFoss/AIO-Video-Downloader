package app.core.engines.downloader

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.internalDataFolder
import app.core.engines.downloader.DownloadStatus.CLOSE
import app.core.engines.downloader.DownloadStatus.COMPLETE
import app.core.engines.downloader.DownloadStatus.DOWNLOADING
import app.core.engines.video_parser.parsers.SupportedURLs.filterYoutubeUrlWithoutPlaylist
import app.core.engines.video_parser.parsers.SupportedURLs.isSocialMediaUrl
import app.core.engines.video_parser.parsers.SupportedURLs.isYouTubeUrl
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoFormat
import app.core.engines.video_parser.parsers.VideoFormatsUtils.cleanYtdlpLoggingSting
import app.core.engines.video_parser.parsers.VideoFormatsUtils.formatDownloadSpeedForYtDlp
import app.core.engines.video_parser.parsers.VideoFormatsUtils.isValidSpeedFormat
import app.core.engines.video_parser.parsers.VideoParserUtility.getSanitizedTitle
import app.core.engines.video_parser.parsers.VideoParserUtility.getVideoTitleFromURL
import com.aio.R
import com.anggrayudi.storage.file.getAbsolutePath
import com.yausername.youtubedl_android.YoutubeDL.getInstance
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import lib.device.DateTimeUtils.calculateTime
import lib.device.DateTimeUtils.millisToDateTimeString
import lib.files.FileUtility.findFileStartingWith
import lib.files.FileUtility.isFileNameValid
import lib.files.FileUtility.sanitizeFileNameExtreme
import lib.networks.DownloaderUtils.getAudioPlaybackTimeIfAvailable
import lib.networks.DownloaderUtils.getFormattedPercentage
import lib.networks.DownloaderUtils.getHumanReadableFormat
import lib.networks.DownloaderUtils.renameIfDownloadFileExistsWithSameName
import lib.networks.DownloaderUtils.updateSmartCatalogDownloadDir
import lib.networks.DownloaderUtils.validateExistedDownloadedFileName
import lib.networks.NetworkUtility.isNetworkAvailable
import lib.networks.NetworkUtility.isWifiEnabled
import lib.networks.URLUtilityKT.getBaseDomain
import lib.networks.URLUtilityKT.isInternetConnected
import lib.networks.URLUtilityKT.isUrlExpired
import lib.process.AsyncJobUtils.executeInBackground
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.AudioPlayerUtils
import lib.process.ThreadsUtility.executeInBackground
import lib.texts.CommonTextUtils.generateRandomString
import lib.texts.CommonTextUtils.getText
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * A class that handles video downloads using youtube-dl/yt-dlp engine.
 * Supports social media downloads, adaptive bitrate streaming, and progress tracking.
 * Implements [DownloadTaskInf] interface for download task management.
 */
class VideoDownloader(override val downloadDataModel: DownloadDataModel) : DownloadTaskInf {
	
	// Configuration from the download model
	private val downloadDataModelConfig = downloadDataModel.globalSettings
	
	// Destination file where the download will be saved
	private var destinationFile = downloadDataModel.getDestinationFile()
	
	// Timer for retrying downloads when network issues occur
	private var retryingDownloadTimer: CountDownTimer? = null
	
	// Listener for download status updates
	override var statusListener: DownloadTaskListener? = null
	
	// Flag to track if WebView is currently loading (for cookie extraction)
	private var isWebViewLoading = false
	
	/**
	 * Initiates the download task by performing necessary setup in a background thread.
	 * - Initializes the download data model.
	 * - Sets up a download timer.
	 * - Updates the download status.
	 */
	override fun initiate() {
		executeInBackground {
			initDownloadDataModel()
			initDownloadTaskTimer()
			updateDownloadStatus()
		}
	}
	
	/**
	 * Starts the download process.
	 * - Updates the status to 'preparing'.
	 * - Configures the download model based on settings.
	 * - Creates an empty destination file.
	 * - Checks if the download is from a social media URL and chooses the appropriate method to proceed.
	 */
	override fun startDownload() {
		updateDownloadStatus(getText(R.string.title_preparing_download), DOWNLOADING)
		configureDownloadModel()
		createEmptyDestinationFile()
		if (isSocialMediaUrl(downloadDataModel.fileURL)) startSocialMediaDownload()
		else startRegularDownload()
	}
	
	/**
	 * Cancels the ongoing download and updates the status accordingly.
	 *
	 * @param cancelReason A reason string for the cancellation. If empty, shows 'Paused' as default message.
	 */
	override fun cancelDownload(cancelReason: String) {
		try {
			closeYTDLProgress()
			val statusMessage = cancelReason.ifEmpty { getText(R.string.title_paused) }
			updateDownloadStatus(statusMessage, CLOSE)
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Initializes the `downloadDataModel` with default values and resets temporary state.
	 */
	private fun initDownloadDataModel() {
		downloadDataModel.status = CLOSE
		downloadDataModel.isRunning = false
		downloadDataModel.isWaitingForNetwork = false
		downloadDataModel.totalConnectionRetries = 0
		downloadDataModel.statusInfo = getText(R.string.text_waiting_to_join)
		initBasicDownloadModelInfo()
		downloadDataModel.updateInStorage()
	}
	
	/**
	 * Starts a repeating timer that periodically checks network availability.
	 * If waiting for network, it attempts to restart the download every 5 seconds.
	 */
	private fun initDownloadTaskTimer() {
		executeOnMainThread {
			retryingDownloadTimer = object : CountDownTimer((1000 * 60), 5000) {
				override fun onTick(millisUntilFinished: Long) {
					if (downloadDataModel.isWaitingForNetwork) {
						executeInBackground(::restartDownload)
					}
				}
				
				override fun onFinish() {
					if (downloadDataModel.isWaitingForNetwork) start()
				}
			}
		}
	}
	
	/**
	 * Applies all necessary configurations to the download model:
	 * - YTDLP settings
	 * - Auto-resume
	 * - Auto-remove
	 * - Part range allocation
	 */
	private fun configureDownloadModel() {
		configureDownloadModelForYTDLP()
		configureDownloadAutoResumeSettings()
		configureDownloadAutoRemoveSettings()
		configureDownloadPartRange()
	}
	
	/**
	 * Configures YTDLP-specific settings and prepares the temp file destination path.
	 * Ensures a valid random filename is created and stored in the internal directory.
	 * Updates model with generated execution command and resets error flags.
	 */
	private fun configureDownloadModelForYTDLP() {
		val videoFormat = downloadDataModel.videoFormat!!
		if (!downloadDataModel.isSmartCategoryDirProcessed) {
			updateSmartCatalogDownloadDir(downloadDataModel)
			renameIfDownloadFileExistsWithSameName(downloadDataModel)
			
			var randomFileName = sanitizeFileNameExtreme(generateRandomString(10))
			val internalDirPath = internalDataFolder.getAbsolutePath(INSTANCE)
			while (File(internalDirPath, randomFileName).exists()) {
				randomFileName = sanitizeFileNameExtreme(generateRandomString(10))
			}
			
			val sanitizedTempName = validateExistedDownloadedFileName(internalDirPath, randomFileName)
			val ytTempDownloadFile = File(internalDirPath, sanitizedTempName)
			
			downloadDataModel.tempYtdlpDestinationFilePath = ytTempDownloadFile.absolutePath
			destinationFile = downloadDataModel.getDestinationFile()
			downloadDataModel.isSmartCategoryDirProcessed = true
		}
		
		downloadDataModel.executionCommand = getYtdlpExecutionCommand(videoFormat)
		downloadDataModel.isYtdlpHavingProblem = false
		downloadDataModel.ytdlpProblemMsg = ""
		downloadDataModel.updateInStorage()
		println("Ytdlp execution command set to: ${downloadDataModel.executionCommand}")
	}
	
	/**
	 * Extracts the vertical resolution (height in pixels) from a resolution string.
	 *
	 * Supported formats:
	 * - "1280x720", "1920×1080" (using 'x' or '×' as a separator)
	 * - "720p", "1080P"
	 * - "1280px720p", "1920Px1080P"
	 * - "720" (a standalone number)
	 *
	 * @param resolutionStr The resolution string to parse.
	 * @return The extracted height as an Int, or null if no valid resolution found.
	 */
	private fun extractResolutionNumber(resolutionStr: String): Int? {
		val patterns = listOf(
			// Pattern 1: 1280x720 or 1920×1080 (with × symbol)
			Regex("""(\d{3,4})[xX×](\d{3,4})"""),
			// Pattern 2: 720p or 1080P
			Regex("""(\d{3,4})[pP]"""),
			// Pattern 3: 1280px720p or 1920Px1080P
			Regex("""(\d{3,4})[pPxX×](\d{3,4})[pP]"""),
			// Pattern 4: Standalone number
			Regex("""^(\d{3,4})$""")
		)
		
		for (regex in patterns) {
			val match = regex.find(resolutionStr) ?: continue
			// Return the last numeric group (height)
			return match.groupValues.last { it.isNotEmpty() && it.all(Char::isDigit) }.toIntOrNull()
		}
		
		return null
	}
	
	/**
	 * Initializes the download model with basic YTDLP-related values.
	 *
	 * This includes:
	 * - Resolving the filename.
	 * - Ensuring the video title is not null.
	 * - Setting default flags (resume support, threading, etc.).
	 * - Setting the start timestamp.
	 *
	 * Prevents re-initialization if already initialized.
	 */
	private fun initBasicDownloadModelInfo() {
		if (downloadDataModel.isBasicYtdlpModelInitialized) return
		val videoInfo = downloadDataModel.videoInfo!!
		val videoFormat = downloadDataModel.videoFormat!!
		val formatResolution = videoFormat.formatResolution.lowercase()
		val fileExtension: String = (if (formatResolution.contains("audio only")) "mp3" else "mp4")
		
		if (videoInfo.videoTitle.isNullOrEmpty()) {
			val titleFromURL = getVideoTitleFromURL(videoInfo.videoUrl)
			videoInfo.videoTitle =
				titleFromURL.ifEmpty(::madeUpTitleFromSelectedVideoFormat)
		}
		
		downloadDataModel.fileURL = videoInfo.videoUrl
		downloadDataModel.siteReferrer = videoInfo.videoUrlReferer ?: videoInfo.videoUrl
		downloadDataModel.fileName =
			"${getSanitizedTitle(videoInfo, videoFormat)}" + ".$fileExtension"
		
		if (!isFileNameValid(downloadDataModel.fileName))
			downloadDataModel.fileName =
				"${getSanitizedTitle(videoInfo, videoFormat, true)}" + ".$fileExtension"
		
		// Set basic model flags and info
		downloadDataModel.isUnknownFileSize = false
		downloadDataModel.isMultiThreadSupported = false
		downloadDataModel.isResumeSupported = true
		downloadDataModel.globalSettings.downloadDefaultThreadConnections = 1
		
		// Timestamp
		System.currentTimeMillis().apply {
			downloadDataModel.startTimeDate = this
			downloadDataModel.startTimeDateInFormat = millisToDateTimeString(this)
		}
		
		downloadDataModel.isBasicYtdlpModelInitialized = true
	}
	
	/**
	 * Generates a fallback title string using the current video format details.
	 *
	 * Format: formatId_formatResolution_formatVcodec_baseDomain
	 *
	 * @return A generated title string based on format and source domain.
	 */
	private fun madeUpTitleFromSelectedVideoFormat(): String {
		val videoFormat = downloadDataModel.videoFormat!!
		val madeUpTitle = "${videoFormat.formatId}_" +
				"${videoFormat.formatResolution}_" +
				"${videoFormat.formatVcodec}_" +
				"${getBaseDomain(downloadDataModel.videoInfo!!.videoUrl)}"
		return madeUpTitle
	}
	
	/**
	 * Constructs the YTDLP execution command string based on the selected video format.
	 *
	 * - If the format ID matches the app's package name, it constructs a dynamic command based on height.
	 * - Supports fallback to audio-only commands for YouTube if necessary.
	 * - Otherwise, returns the direct format ID.
	 *
	 * @param videoFormat The selected video format.
	 * @return A string command for YTDLP to execute.
	 */
	private fun getYtdlpExecutionCommand(videoFormat: VideoFormat): String {
		val packageName = INSTANCE.packageName
		val resolutionNumber = extractResolutionNumber(videoFormat.formatResolution)
		return if (videoFormat.formatId == packageName) {
			if (videoFormat.isFromSocialMedia) {
				"bestvideo[height<=2400]" +
						"+bestaudio/best[height<=2400]/best"
			} else {
				val commonPattern = "bestvideo[height<=$resolutionNumber]" +
						"+bestaudio/best[height<=$resolutionNumber]/best"
				
				if (isYouTubeUrl(downloadDataModel.fileURL)) {
					val isAudio = videoFormat.formatResolution.contains("audio", true)
					if (isAudio) "bestaudio" else commonPattern
					
				} else commonPattern
			}
		} else {
			videoFormat.formatId
		}
	}
	
	/**
	 * Configures auto-resume settings for a download.
	 * If the smart category processing has not yet been done,
	 * and auto-resume is disabled in the config, it ensures the allowed max error count is zero.
	 */
	private fun configureDownloadAutoResumeSettings() {
		if (!downloadDataModel.isSmartCategoryDirProcessed) {
			if (!downloadDataModelConfig.downloadAutoResume)
				downloadDataModelConfig.downloadAutoResumeMaxErrors = 0
		}
	}
	
	/**
	 * Configures auto-remove settings for completed download tasks.
	 * If the smart category directory has not been processed and auto-remove is disabled,
	 * then any pending removal time is reset to zero.
	 */
	private fun configureDownloadAutoRemoveSettings() {
		if (!downloadDataModel.isSmartCategoryDirProcessed) {
			if (!downloadDataModelConfig.downloadAutoRemoveTasks)
				downloadDataModelConfig.downloadAutoRemoveTaskAfterNDays = 0
		}
	}
	
	/**
	 * Initializes the range of download parts based on the number of thread connections.
	 * Only performed if the smart category directory hasn't already been processed.
	 */
	private fun configureDownloadPartRange() {
		if (!downloadDataModel.isSmartCategoryDirProcessed) {
			val numberOfThreads = downloadDataModelConfig.downloadDefaultThreadConnections
			downloadDataModel.partsDownloadedByte = LongArray(numberOfThreads)
			downloadDataModel.partProgressPercentage = IntArray(numberOfThreads)
		}
	}
	
	/**
	 * Updates the download progress in a synchronized block.
	 * It performs all necessary steps such as recalculating progress, checking connectivity,
	 * updating status, and persisting data to storage.
	 */
	@Synchronized
	private fun updateDownloadProgress() {
		calculateProgressAndModifyDownloadModel()
		checkNetworkConnectionAndRetryDownload()
		updateDownloadStatus()
	}
	
	/**
	 * Updates the status of the download and notifies the registered listener (if any).
	 *
	 * @param statusInfo Optional description for the new status.
	 * @param status The numeric status code indicating the download state.
	 */
	override fun updateDownloadStatus(statusInfo: String?, status: Int) {
		if (!statusInfo.isNullOrEmpty()) downloadDataModel.statusInfo = statusInfo
		val classRef = this@VideoDownloader
		downloadDataModel.status = status
		downloadDataModel.isRunning = (status == DOWNLOADING)
		downloadDataModel.isComplete = (status == COMPLETE)
		downloadDataModel.updateInStorage()
		executeOnMainThread {
			statusListener?.onStatusUpdate(classRef)
		}
	}
	
	/**
	 * Calculates download progress metrics such as bytes downloaded, percentage,
	 * and time spent, and persists these updates into storage.
	 */
	private fun calculateProgressAndModifyDownloadModel() {
		calculateTotalDownloadedTime()
		calculateDownloadedBytes()
		calculateDownloadPercentage()
		updateLastModificationDate()
		downloadDataModel.updateInStorage()
	}
	
	/**
	 * Checks network connectivity and resumes the download if conditions are favorable.
	 * It handles waiting state, retries, and calls the download executor if ready.
	 */
	private fun checkNetworkConnectionAndRetryDownload() {
		if (!verifyNetworkConnection()) closeYTDLProgress()
		
		if (downloadDataModel.isWaitingForNetwork) {
			if (isNetworkAvailable() && isInternetConnected()) {
				if (downloadDataModelConfig.downloadWifiOnly && !isWifiEnabled()) return
				downloadDataModel.isWaitingForNetwork = false
				updateDownloadStatus(getText(R.string.title_started_downloading))
				executeDownloadProcess()
			}
		}
	}
	
	/**
	 * Forcefully closes the temporary YTDL progress process related to the current download.
	 */
	private fun closeYTDLProgress() {
		getInstance().destroyProcessById(downloadDataModel.id.toString())
		println("YTDL progress closed for download task ID: ${downloadDataModel.id}")
	}
	
	/**
	 * Verifies network conditions to determine whether the download can proceed.
	 *
	 * @return `true` if connected to network and satisfies WiFi-only preference, else `false`.
	 */
	private fun verifyNetworkConnection(): Boolean {
		val isWifiOnly = downloadDataModelConfig.downloadWifiOnly
		return !(!isNetworkAvailable() || (isWifiOnly && !isWifiEnabled()))
	}
	
	/**
	 * Calculates the total time spent on downloading, excluding network wait periods,
	 * and updates the formatted time string in the model.
	 */
	private fun calculateTotalDownloadedTime() {
		if (!downloadDataModel.isWaitingForNetwork)
			downloadDataModel.timeSpentInMilliSec += 500
		
		val timeSpentMillis = downloadDataModel.timeSpentInMilliSec.toFloat()
		val format = calculateTime(timeSpentMillis, getText(R.string.text_spent))
		downloadDataModel.timeSpentInFormat = format
	}
	
	/**
	 * Calculates the total number of bytes downloaded so far and formats the size
	 * in human-readable format. Also updates the corresponding part progress.
	 */
	private fun calculateDownloadedBytes() {
		try {
			val ytTempDownloadFile = downloadDataModel.tempYtdlpDestinationFilePath
			val ytTempFileNamePrefix = File(ytTempDownloadFile).name
			val internalDir = internalDataFolder
			
			internalDir.listFiles().filter { file ->
				file.isFile &&
						file.name!!.startsWith(ytTempFileNamePrefix) &&
						file.name!!.endsWith(".part")
			}.forEach { file ->
				try {
					downloadDataModel.downloadedByte = file.length()
					downloadDataModel.fileSize = downloadDataModel.downloadedByte
				} catch (error: Exception) {
					error.printStackTrace()
				}
			}
			
			val downloadedByte = downloadDataModel.downloadedByte
			downloadDataModel.downloadedByteInFormat = getHumanReadableFormat(downloadedByte)
			downloadDataModel.partsDownloadedByte[0] = downloadedByte
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Calculates the overall download progress percentage.
	 * Updates both the numeric percentage and the formatted string.
	 */
	private fun calculateDownloadPercentage() {
		try {
			val totalProcess = downloadDataModel.progressPercentage
			downloadDataModel.partProgressPercentage[0] = totalProcess.toInt()
			downloadDataModel.progressPercentageInFormat = getFormattedPercentage(downloadDataModel)
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Updates the download model's last modification timestamp
	 * and converts it into a formatted date-time string.
	 */
	private fun updateLastModificationDate() {
		downloadDataModel.lastModifiedTimeDate = System.currentTimeMillis()
		downloadDataModel.lastModifiedTimeDateInFormat =
			millisToDateTimeString(downloadDataModel.lastModifiedTimeDate)
	}
	
	/**
	 * Creates an empty destination file for the download if it doesn't already exist.
	 * This file acts as a placeholder and ensures disk access is working correctly.
	 *
	 * If any I/O error occurs, the download is marked as failed and cancelled.
	 */
	private fun createEmptyDestinationFile() {
		if (downloadDataModel.isDeleted) return
		if (downloadDataModel.downloadedByte < 1) {
			try {
				if (destinationFile.exists()) return
				RandomAccessFile(destinationFile, "rw").setLength(108)
			} catch (error: IOException) {
				error.printStackTrace()
				downloadDataModel.totalConnectionRetries++
				downloadDataModel.isFailedToAccessFile = true
				cancelDownload(getText(R.string.title_download_io_failed))
			}
		}
	}
	
	/**
	 * Starts the download process for social media content.
	 * This typically involves checking if the download is initiated from a browser.
	 *
	 * If not browser-based, it triggers a regular download; otherwise,
	 * it resumes the process in a background thread.
	 */
	@SuppressLint("SetJavaScriptEnabled")
	private fun startSocialMediaDownload() {
		updateDownloadStatus(getText(R.string.title_started_downloading), DOWNLOADING)
		downloadDataModel.tempYtdlpStatusInfo = getText(R.string.title_getting_cookie)
		if (!downloadDataModel.isDownloadFromBrowser) {
			downloadDataModel.isDownloadFromBrowser = false
			downloadDataModel.updateInStorage()
			startRegularDownload()
		} else {
			executeInBackground(codeBlock = {
				downloadDataModel.isDownloadFromBrowser = false
				downloadDataModel.updateInStorage()
				executeDownloadProcess()
			})
		}
	}
	
	/**
	 * Initiates the standard download procedure, typically used when no browser involvement is needed.
	 * This method supports retries up to two times, falling back to direct execution on failure.
	 *
	 * It loads the download URL in a WebView to fetch cookies or session data if needed,
	 * and then triggers the actual download execution once the page finishes loading.
	 *
	 * @param retryCount Indicates how many times this method has been retried so far.
	 */
	@SuppressLint("SetJavaScriptEnabled")
	private fun startRegularDownload(retryCount: Int = 0) {
		if (retryCount >= 2) {
			executeInBackground { executeDownloadProcess() }
			return
		}
		
		updateDownloadStatus(getText(R.string.title_started_downloading), DOWNLOADING)
		downloadDataModel.tempYtdlpStatusInfo = getText(R.string.title_getting_cookie)
		
		Handler(Looper.getMainLooper()).post {
			val webView = WebView(INSTANCE).apply {
				settings.javaScriptEnabled = true
				webViewClient = object : WebViewClient() {
					override fun onPageFinished(view: WebView?, url: String?) {
						if (isWebViewLoading) {
							handlePageLoaded(url)
							destroy()
						}
					}
					
					@Deprecated("Deprecated in Java")
					override fun onReceivedError(
						view: WebView?, errorCode: Int,
						description: String?, failingUrl: String?
					) = retryOrFail(retryCount)
				}
				
				webChromeClient = object : WebChromeClient() {
					override fun onReceivedTitle(view: WebView?, title: String?) {
						super.onReceivedTitle(view, title)
						if (isWebViewLoading) {
							handlePageLoaded(url)
							destroy()
						}
					}
				}
				
				loadUrl(downloadDataModel.fileURL)
				isWebViewLoading = true
			}
			
			Handler(Looper.getMainLooper()).postDelayed({
				if (isWebViewLoading) {
					webView.destroy()
					retryOrFail(retryCount)
				}
			}, 10_000)
		}
	}
	
	/**
	 * Called when the WebView has finished loading the target page.
	 * Extracts cookies from the loaded URL and saves them into the data model.
	 *
	 * Triggers the actual download process in the background once cookies are obtained.
	 *
	 * @param url The URL that was loaded in the WebView.
	 */
	private fun handlePageLoaded(url: String?) {
		isWebViewLoading = false
		val cookies = CookieManager.getInstance().getCookie(url)
		cookies?.let {
			downloadDataModel.apply {
				videoInfo?.videoCookie = cookies
				siteCookieString = cookies
				updateInStorage()
				saveCookiesIfAvailable(shouldOverride = true)
			}
		}
		
		executeInBackground {
			executeDownloadProcess()
		}
	}
	
	/**
	 * Handles retrying of the WebView-based download initialization.
	 * If retry count is within limit, restarts the download using the regular path.
	 *
	 * @param currentRetry The number of retries that have already been attempted.
	 */
	private fun retryOrFail(currentRetry: Int) {
		isWebViewLoading = false
		Handler(Looper.getMainLooper()).post {
			startRegularDownload(currentRetry + 1)
		}
	}
	
	/**
	 * Executes the actual download command using yt-dlp.
	 * Builds and runs the YoutubeDLRequest with various custom options.
	 * Handles progress updates, cookie injection, retry logic, and success/failure cases.
	 */
	private fun executeDownloadProcess() {
		updateDownloadStatus(getText(R.string.title_started_downloading), DOWNLOADING)
		downloadDataModel.tempYtdlpStatusInfo = getText(R.string.title_connecting_to_the_server)
		
		val response: YoutubeDLResponse?
		try {
			var lastUpdateTime = System.currentTimeMillis()
			val request = YoutubeDLRequest(filterYoutubeUrlWithoutPlaylist(downloadDataModel.fileURL))
			
			// Add standard options
			request.addOption("--continue")
			request.addOption("-f", downloadDataModel.executionCommand)
			request.addOption("-o", downloadDataModel.tempYtdlpDestinationFilePath)
			request.addOption("--playlist-items", "1")
			request.addOption("--user-agent", downloadDataModelConfig.downloadHttpUserAgent)
			request.addOption("--retries", downloadDataModelConfig.downloadAutoResumeMaxErrors)
			request.addOption("--socket-timeout", downloadDataModelConfig.downloadMaxHttpReadingTimeout)
			request.addOption("--concurrent-fragments", 10)
			request.addOption("--fragment-retries", 10)
			request.addOption("--no-check-certificate")
			request.addOption("--force-ipv4")
			request.addOption("--socket-timeout", 30)
			request.addOption("--source-address", "0.0.0.0")
			
			// Add cookie support if available
			downloadDataModel.getCookieFilePathIfAvailable()?.let {
				val cookieFile = File(it)
				if (cookieFile.exists() && cookieFile.canWrite()) {
					println("Cookie File Found=${cookieFile.absolutePath}")
					request.addOption("--cookies", cookieFile.absolutePath)
				}
			}
			
			// Add download speed throttling if configured
			if (downloadDataModelConfig.downloadMaxNetworkSpeed > 0) {
				val downloadMaxNetworkSpeed = downloadDataModelConfig.downloadMaxNetworkSpeed
				val ytDlpSpeedLimit = formatDownloadSpeedForYtDlp(downloadMaxNetworkSpeed)
				if (isValidSpeedFormat(ytDlpSpeedLimit)) {
					request.addOption("--limit-rate", ytDlpSpeedLimit)
					println("ytDlpSpeedLimit=$ytDlpSpeedLimit")
				}
			}
			
			// Execute yt-dlp request with progress updates
			response = getInstance().execute(
				request = request,
				processId = downloadDataModel.id.toString()
			) { progress, _, status ->
				if (progress > 0) downloadDataModel.progressPercentage = progress.toLong()
				downloadDataModel.tempYtdlpStatusInfo = cleanYtdlpLoggingSting(status)
				println(downloadDataModel.tempYtdlpStatusInfo)
				
				val currentTime = System.currentTimeMillis()
				if (currentTime - lastUpdateTime >= 500) {
					lastUpdateTime = currentTime
					updateDownloadProgress()
				}
			}
			
			// Handle yt-dlp completion or failure
			if (response.exitCode != 0) onYtdlpDownloadFailed(response.out) else {
				moveToUserSelectedDestination()
				
				if (downloadDataModelConfig.downloadPlayNotificationSound)
					AudioPlayerUtils(INSTANCE).play(R.raw.sound_download_finished)
				
				downloadDataModel.isRunning = false
				downloadDataModel.isComplete = true
				updateDownloadStatus(getText(R.string.text_completed), COMPLETE)
				println("Download status updated to COMPLETE.")
				println("Download completed successfully in ${response.elapsedTime} ms.")
			}
		} catch (error: Exception) {
			error.printStackTrace()
			
			if (isRetryingAllowed()) {
				onYtdlpDownloadFailed(error.message)
			} else {
				cancelDownload()
				updateDownloadStatus(getText(R.string.title_paused), CLOSE)
			}
		}
	}
	
	/**
	 * Handles various failure scenarios of yt-dlp.
	 * Parses the output to detect critical issues and decides whether to cancel or retry.
	 *
	 * @param response The output message from yt-dlp on failure, if any.
	 */
	private fun onYtdlpDownloadFailed(response: String? = null) {
		executeInBackground(codeBlock = {
			if (response != null && isCriticalErrorFound(response)) {
				if (downloadDataModel.isYtdlpHavingProblem) {
					val pausedMsg = downloadDataModel.ytdlpProblemMsg.ifEmpty {
						getText(R.string.title_paused)
					}
					cancelDownload(pausedMsg)
					return@executeInBackground
				}
				
				if (downloadDataModel.isFileUrlExpired) {
					cancelDownload(getText(R.string.title_link_expired))
					return@executeInBackground
				}
				
				if (downloadDataModel.isDestinationFileNotExisted) {
					cancelDownload(getText(R.string.title_file_deleted_paused))
					return@executeInBackground
				}
			} else if (!response.isNullOrEmpty()) {
				updateDownloadStatus(getText(R.string.title_download_failed))
				cancelDownload(getText(R.string.title_download_failed))
				
			} else {
				restartDownload()
				retryingDownloadTimer?.start()
			}
			
			downloadDataModel.updateInStorage()
			if (!downloadDataModel.isRemoved && downloadDataModel.isDeleted) {
				if (destinationFile.exists()) destinationFile.delete()
			}
		})
	}
	
	/**
	 * Checks if the download has failed due to any critical issues.
	 * Updates appropriate flags in the downloadDataModel and persists the state.
	 *
	 * @param response The error message or output received from yt-dlp.
	 * @return `true` if a critical error is detected, otherwise `false`.
	 */
	private fun isCriticalErrorFound(response: String): Boolean {
		if (isYtdlHavingProblem(response)) {
			downloadDataModel.isYtdlpHavingProblem = true
			if (downloadDataModel.ytdlpProblemMsg.isEmpty()) {
				val msgString = getText(R.string.title_paused_server_problem)
				downloadDataModel.ytdlpProblemMsg = msgString
			}
			downloadDataModel.updateInStorage()
			return true
		}
		
		if (isUrlExpired(downloadDataModel.fileURL)) {
			downloadDataModel.isFileUrlExpired = true
			return true
		}
		
		if (!destinationFile.exists()) {
			downloadDataModel.isDestinationFileNotExisted = true
			return true
		}
		
		return false
	}
	
	/**
	 * Identifies if the given yt-dlp response contains any known fatal issues.
	 * Sets a human-readable message to `ytdlpProblemMsg` accordingly.
	 *
	 * @param response The raw response string from yt-dlp.
	 * @return `true` if a known yt-dlp problem is detected, otherwise `false`.
	 */
	private fun isYtdlHavingProblem(response: String): Boolean {
		return response.contains("rate-limit reached or login required", ignoreCase = true).apply {
			downloadDataModel.ytdlpProblemMsg = getText(R.string.title_paused_login_required)
		} || response.contains("Requested content is not available", ignoreCase = true).apply {
			downloadDataModel.ytdlpProblemMsg = getText(R.string.title_paused_content_not_available)
		} || response.contains("Requested format is not available", ignoreCase = true).apply {
			downloadDataModel.ytdlpProblemMsg = getText(R.string.title_paused_ytdlp_format_not_found)
		} || response.contains("Restricted Video", ignoreCase = true).apply {
			downloadDataModel.ytdlpProblemMsg = getText(R.string.title_paused_login_required)
		} || response.contains("--cookies for the authentication", ignoreCase = true).apply {
			downloadDataModel.ytdlpProblemMsg = getText(R.string.title_paused_login_required)
		} || response.contains("Connection reset by peer", ignoreCase = true).apply {
			downloadDataModel.ytdlpProblemMsg = getText(R.string.title_site_banned_in_your_area)
		} || response.contains("YoutubeDLException", ignoreCase = true).apply {
			downloadDataModel.ytdlpProblemMsg = INSTANCE.getString(
				R.string.title_server_issue,
				getText(R.string.title_download_failed)
			)
		}
	}
	
	/**
	 * Attempts to restart the download based on current network conditions and retry limits.
	 * Updates download status accordingly or resumes the download if network is available.
	 */
	private fun restartDownload() {
		if (isRetryingAllowed()) {
			if (!isNetworkAvailable()) {
				downloadDataModel.isWaitingForNetwork = true
				updateDownloadStatus(getText(R.string.text_waiting_for_network))
				println("Network not available. Waiting for network.")
				return
			}
			
			if (downloadDataModelConfig.downloadWifiOnly && !isWifiEnabled()) {
				downloadDataModel.isWaitingForNetwork = true
				updateDownloadStatus(getText(R.string.text_waiting_for_wifi))
				println("Wi-Fi is not enabled. Waiting for Wi-Fi.")
				return
			}
			
			if (!isInternetConnected()) {
				downloadDataModel.isWaitingForNetwork = true
				updateDownloadStatus(getText(R.string.text_waiting_for_internet))
				println("Internet connection is not available. Waiting for internet.")
				return
			}
			
			if (downloadDataModel.isWaitingForNetwork) {
				downloadDataModel.isWaitingForNetwork = false
				updateDownloadStatus(getText(R.string.title_started_downloading))
				println("Network or Wi-Fi is now available. Restarting download.")
				executeDownloadProcess()
			}
			
			downloadDataModel.totalConnectionRetries++
		}
	}
	
	/**
	 * Determines whether the download should be retried based on retry count and status.
	 *
	 * @return `true` if retrying is allowed, otherwise `false`.
	 */
	private fun isRetryingAllowed(): Boolean {
		val maxErrorAllowed = downloadDataModelConfig.downloadAutoResumeMaxErrors
		val retryAllowed = downloadDataModel.isRunning &&
				downloadDataModel.totalConnectionRetries < maxErrorAllowed
		return retryAllowed
	}
	
	/**
	 * Moves the downloaded temporary file to the user-selected destination.
	 * Handles overwrites and updates related metadata fields.
	 */
	private fun moveToUserSelectedDestination() {
		val inputFile = findFileStartingWith(
			internalDir = File(internalDataFolder.getAbsolutePath(INSTANCE)),
			namePrefix = File(downloadDataModel.tempYtdlpDestinationFilePath).name
		)
		
		inputFile?.let {
			try {
				val outputFile = downloadDataModel.getDestinationFile()
				it.copyTo(outputFile, overwrite = true)
				it.delete()
				
				downloadDataModel.fileSize = outputFile.length()
				downloadDataModel.fileSizeInFormat =
					getHumanReadableFormat(downloadDataModel.fileSize)
				
				downloadDataModel.downloadedByte = downloadDataModel.fileSize
				downloadDataModel.downloadedByteInFormat =
					getHumanReadableFormat(downloadDataModel.downloadedByte)
				
				downloadDataModel.progressPercentage = 100
				downloadDataModel.partsDownloadedByte[0] = downloadDataModel.downloadedByte
				downloadDataModel.partProgressPercentage[0] = 100
				
			} catch (error: Exception) {
				error.printStackTrace()
				val outputFile = downloadDataModel.getDestinationFile()
				outputFile.renameTo(File(inputFile.name))
				outputFile.delete()
				val currentName = downloadDataModel.fileName
				downloadDataModel.fileName = sanitizeFileNameExtreme(currentName)
				renameIfDownloadFileExistsWithSameName(downloadDataModel)
				copyFileToUserDestination(it)
			}
		}
	}
	
	/**
	 * Copies the specified file to the user's selected download location.
	 * Also deletes the cookie temp file if available and updates model metadata.
	 *
	 * @param it The file to be copied.
	 */
	private fun copyFileToUserDestination(it: File) {
		val outputFile = downloadDataModel.getDestinationFile()
		it.copyTo(outputFile, overwrite = true)
		it.delete()
		
		downloadDataModel.fileSize = outputFile.length()
		downloadDataModel.clearCachedThumbnailFile()
		downloadDataModel.mediaFilePlaybackDuration = getAudioPlaybackTimeIfAvailable(downloadDataModel)
		if (downloadDataModel.videoInfo!!.videoCookieTempPath.isNotEmpty()) {
			val tempCookieFile = File(downloadDataModel.videoInfo!!.videoCookieTempPath)
			if (tempCookieFile.isFile && tempCookieFile.exists()) tempCookieFile.delete()
		}; downloadDataModel.updateInStorage()
	}
	
}