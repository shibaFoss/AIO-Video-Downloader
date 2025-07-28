package app.ui.main.fragments.browser.webengine

import android.webkit.DownloadListener
import androidx.documentfile.provider.DocumentFile.fromFile
import app.core.AIOApp.Companion.IS_PREMIUM_USER
import app.core.AIOApp.Companion.IS_ULTIMATE_VERSION_UNLOCKED
import app.core.AIOApp.Companion.admobHelper
import app.core.AIOApp.Companion.aioSettings
import app.core.AIOApp.Companion.downloadSystem
import app.core.engines.downloader.DownloadDataModel
import com.aio.R
import lib.device.DateTimeUtils.millisToDateTimeString
import lib.device.StorageUtility.getFreeExternalStorageSpace
import lib.files.FileUtility.decodeURLFileName
import lib.files.FileUtility.extractFileNameFromContentDisposition
import lib.files.FileUtility.isFileNameValid
import lib.files.FileUtility.sanitizeFileNameExtreme
import lib.files.FileUtility.sanitizeFileNameNormal
import lib.networks.DownloaderUtils.getHumanReadableFormat
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.ThreadsUtility
import lib.process.ThreadsUtility.executeOnMain
import lib.texts.CommonTextUtils.fromHtmlStringToSpanned
import lib.texts.CommonTextUtils.getText
import lib.texts.CommonTextUtils.removeDuplicateSlashes
import lib.texts.CommonTextUtils.removeEmptyLines
import lib.ui.MsgDialogUtils.showMessageDialog
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.builders.ToastView.Companion.showToast
import java.io.File
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * Handles download requests initiated by WebView in the browser fragment.
 * Provides UI prompt for the user to accept the download, and handles download metadata.
 *
 * @property webviewEngine Instance of the WebViewEngine triggering downloads.
 */
class WebViewDownloadHandler(val webviewEngine: WebViewEngine) : DownloadListener {
	
	// A reference to the safe WebView engine used for initiating downloads.
	// This is obtained safely from the current WebView engine instance to avoid memory leaks.
	private val safeWebEngineRef = WeakReference(webviewEngine).get()
	
	/**
	 * Called when a download is initiated from the WebView.
	 *
	 * @param url The URL of the downloadable file.
	 * @param userAgent The user-agent string from the WebView.
	 * @param contentDisposition The Content-Disposition header from the server.
	 * @param mimetype The MIME type of the file.
	 * @param contentLength The size of the file in bytes.
	 */
	override fun onDownloadStart(
		url: String?,
		userAgent: String?,
		contentDisposition: String?,
		mimetype: String?,
		contentLength: Long
	) {
		val lastDownloadLink = webviewEngine.browserWebClient.lastTimeDownloadLink
		if (lastDownloadLink == url) return
		
		// Delegate download handling to show a user dialog.
		showDownloadAvailableDialog(
			contentDisposition = contentDisposition,
			url = url,
			contentLength = contentLength,
			mimetype = mimetype,
			safeWebEngineRef = safeWebEngineRef,
			userAgent = userAgent,
			userGivenFileName = null
		)
	}
	
	/**
	 * Displays a download confirmation dialog to the user and handles reward ad logic if necessary.
	 *
	 * @param contentDisposition The server's content disposition header (may contain filename).
	 * @param url The URL to download.
	 * @param contentLength The size of the file in bytes.
	 * @param mimetype The file's MIME type.
	 * @param safeWebEngineRef The associated WebView engine to extract cookies, referrer, etc.
	 * @param userAgent The HTTP user-agent string.
	 * @param userGivenFileName An optional filename provided by the user (fallback).
	 */
	fun showDownloadAvailableDialog(
		contentDisposition: String?,
		url: String?,
		contentLength: Long,
		mimetype: String?,
		safeWebEngineRef: WebViewEngine?,
		userAgent: String?,
		userGivenFileName: String? = null
	) {
		val safeActivityRef = safeWebEngineRef?.safeMotherActivityRef ?: return
		
		// Try to extract filename from content disposition or use fallback
		val extractedName = extractFileNameFromContentDisposition(contentDisposition)
		val filename = extractedName?.let { decodeURLFileName(it) } ?: run { userGivenFileName }
		
		// Abort if filename or URL is missing
		if (filename.isNullOrEmpty()) return
		if (url.isNullOrEmpty()) return
		
		// If content length is invalid (zero or negative), show unsupported file warning
		if (contentLength < 1) {
			safeActivityRef.doSomeVibration(50)
			showToast(msgId = R.string.text_unsupported_file_link)
			return
		}
		
		// Populate a download model with extracted data
		val downloadModel = DownloadDataModel()
		downloadModel.fileName = filename
		downloadModel.fileURL = url
		downloadModel.fileContentDisposition = contentDisposition ?: ""
		downloadModel.fileMimeType = mimetype ?: ""
		downloadModel.fileSize = contentLength
		downloadModel.fileSizeInFormat = getHumanReadableFormat(contentLength)
		downloadModel.isUnknownFileSize = false
		downloadModel.siteCookieString = safeWebEngineRef.getCurrentWebViewCookies() ?: ""
		downloadModel.siteReferrer = safeWebEngineRef.currentWebView?.url ?: ""
		downloadModel.isDownloadFromBrowser = true
		userAgent?.let { downloadModel.globalSettings.downloadHttpUserAgent = it }
		downloadModel.additionalWebHeaders = getWebViewRequestHeaders()
		
		try {
			// Show download confirmation dialog
			showMessageDialog(
				baseActivityInf = safeActivityRef,
				titleTextViewCustomize = { it.setText(R.string.title_download_available) },
				isTitleVisible = true,
				isNegativeButtonVisible = false,
				positiveButtonTextCustomize = {
					it.setText(R.string.title_download_now)
					it.setLeftSideDrawable(R.drawable.ic_button_download)
					
					// If user has exceeded free download limit, change button to "Watch Ad"
					val numberOfDownloadsUserDid = aioSettings.numberOfDownloadsUserDid
					val maxDownloadThreshold = aioSettings.numberOfMaxDownloadThreshold
					if (numberOfDownloadsUserDid >= maxDownloadThreshold) {
						if (!IS_PREMIUM_USER && !IS_ULTIMATE_VERSION_UNLOCKED) {
							it.setLeftSideDrawable(R.drawable.ic_button_video)
							it.setText(R.string.text_watch_ad_to_download)
						}
					}
				},
				messageTextViewCustomize = {
					val info = """
							${filename}<br/><br/>
							<b>${getText(R.string.title_file_size)} </b>
							${getHumanReadableFormat(contentLength)}
						""".trimIndent()
					it.text = fromHtmlStringToSpanned(info)
				}
			)?.let { dialogBuilder ->
				dialogBuilder.dialog.setOnDismissListener {
					webviewEngine.browserWebClient.lastTimeDownloadLink = ""
				}
				
				dialogBuilder.dialog.setOnCancelListener {
					webviewEngine.browserWebClient.lastTimeDownloadLink = ""
				}
				
				dialogBuilder.show()
				dialogBuilder.setOnClickForPositiveButton {
					dialogBuilder.close()
					val numberOfDownloadsUserDid = aioSettings.numberOfDownloadsUserDid
					val maxDownloadThreshold = aioSettings.numberOfMaxDownloadThreshold
					val condition1 = numberOfDownloadsUserDid < maxDownloadThreshold
					val condition2 = !admobHelper.isRewardedInterstitialAdReady()
					if (condition1 || condition2) { //Admob reward interstitial ads is not loaded.
						addToDownloadSystem(downloadModel)
						admobHelper.loadRewardedInterstitialAd(safeActivityRef)
					} else {
						if (admobHelper.isRewardedInterstitialAdReady()) {
							admobHelper.showRewardedInterstitialAd(
								activity = safeActivityRef,
								onAdCompleted = { addToDownloadSystem(downloadModel) },
								onAdClosed = {
									safeActivityRef.doSomeVibration(50)
									showToast(msgId = R.string.text_failed_to_add_download_task)
								}
							)
						} else {
							admobHelper.loadRewardedInterstitialAd(safeActivityRef)
							addToDownloadSystem(downloadModel)
						}
					}
				}
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Generates a map of standard HTTP headers and device/browser identification headers
	 * used for making download-related network requests from the WebView context.
	 *
	 * @return A map containing the default HTTP request headers.
	 */
	private fun getWebViewRequestHeaders(): Map<String, String> {
		val headers = mutableMapOf<String, String>().apply {
			// Standard HTTP headers for web requests
			put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
			put("Accept-Language", Locale.getDefault().toLanguageTag())
			put("Accept-Encoding", "gzip, deflate, br")
			put("Connection", "keep-alive")
			put("Cache-Control", "no-cache")
			put("Pragma", "no-cache")
			
			// Browser identification headers (helps mimic real browser requests)
			put("Sec-Fetch-Dest", "document")
			put("Sec-Fetch-Mode", "navigate")
			put("Sec-Fetch-Site", "same-origin")
			put("Sec-Fetch-User", "?1")
			
			// App identification header (replace with actual app package name)
			put("X-Requested-With", "com.your.app.package")
		}
		return headers
	}
	
	/**
	 * Validates and processes a [DownloadDataModel], then adds it to the download system.
	 * Performs background operations such as checking storage, validating input, and preparing the download entry.
	 *
	 * @param downloadModel The download model to validate and queue.
	 */
	private fun addToDownloadSystem(downloadModel: DownloadDataModel) {
		ThreadsUtility.executeInBackground(codeBlock = {
			try {
				// Step-by-step validation and preparation of the download
				validateDownloadFileName(downloadModel)
				validateDownloadDir(downloadModel)
				validateDownloadStartDate(downloadModel)
				validateOptionalConfigs(downloadModel)
				renameIfFileExistsWithSameName(downloadModel)
				
				// Check available storage before proceeding
				checkStorageSpace(downloadModel).let { hasEnoughSpace ->
					if (hasEnoughSpace) {
						downloadSystem.addDownload(downloadModel)
						executeOnMainThread {
							val toastMsgResId = R.string.text_download_added_successfully
							showToast(msgId = toastMsgResId)
						}
						
						// Update user settings for successful download
						aioSettings.numberOfDownloadsUserDid++
						aioSettings.totalNumberOfSuccessfulDownloads++
						aioSettings.updateInStorage()
					} else {
						// Not enough space warning
						executeOnMainThread {
							showMessageDialog(
								baseActivityInf = safeWebEngineRef?.safeMotherActivityRef,
								isNegativeButtonVisible = false,
								messageTextViewCustomize = {
									it.setText(R.string.text_warning_not_enough_space_msg)
								}, positiveButtonTextCustomize = {
									it.setText(R.string.title_okay)
									it.setLeftSideDrawable(R.drawable.ic_button_checked_circle)
								}
							)
						}
					}
				}
			} catch (error: Exception) {
				error.printStackTrace()
				executeOnMain {
					// Show failure feedback
					safeWebEngineRef?.safeMotherActivityRef?.doSomeVibration(50)
					showToast(msgId = R.string.text_failed_to_add_download_task)
				}
			}
		})
	}
	
	/**
	 * Sanitizes and validates the filename within the given [DownloadDataModel].
	 * Falls back to extreme sanitization if normal checks fail.
	 */
	private fun validateDownloadFileName(downloadModel: DownloadDataModel) {
		downloadModel.fileName = sanitizeFileNameNormal(downloadModel.fileName)
		if (!isFileNameValid(downloadModel.fileName)) {
			val sanitizeFileNameExtreme = sanitizeFileNameExtreme(downloadModel.fileName)
			downloadModel.fileName = sanitizeFileNameExtreme
		}; downloadModel.fileName = removeEmptyLines(downloadModel.fileName) ?: ""
	}
	
	/**
	 * Validates and updates the download directory based on the file category.
	 * Also attempts to create a category subdirectory if necessary.
	 */
	private fun validateDownloadDir(downloadModel: DownloadDataModel) {
		val fileCategoryName: String = downloadModel.getUpdatedCategoryName()
		val videoFileDir = fromFile(File(downloadModel.fileDirectory))
		val categoryDocumentFile = videoFileDir.createDirectory(fileCategoryName)
		if (categoryDocumentFile?.canWrite() == true) downloadModel.fileCategoryName = fileCategoryName
		generateDestinationFilePath(downloadModel)?.let { downloadModel.fileDirectory = it }
	}
	
	/**
	 * Generates a normalized destination directory path for the download file.
	 *
	 * @return The generated file path with category included if applicable.
	 */
	private fun generateDestinationFilePath(downloadModel: DownloadDataModel): String? {
		return removeDuplicateSlashes(
			"${downloadModel.fileDirectory}/${
				if (downloadModel.fileCategoryName.isNotEmpty())
					downloadModel.fileCategoryName + "/" else ""
			}"
		)
	}
	
	/**
	 * Sets the current system time as the download start timestamp.
	 */
	private fun validateDownloadStartDate(downloadModel: DownloadDataModel) {
		System.currentTimeMillis().apply {
			downloadModel.startTimeDate = this
			downloadModel.startTimeDateInFormat = millisToDateTimeString(this)
		}
	}
	
	/**
	 * Resets optional configurations in the download model, if previously set.
	 */
	private fun validateOptionalConfigs(downloadModel: DownloadDataModel) {
		downloadModel.videoInfo = null
		downloadModel.videoFormat = null
	}
	
	/**
	 * If a file with the same name exists, prepends an incrementing numeric prefix (e.g., "1_filename.ext")
	 * to avoid overwriting existing files.
	 */
	private fun renameIfFileExistsWithSameName(downloadModel: DownloadDataModel) {
		var index: Int
		val regex = Regex("^(\\d+)_")
		
		while (downloadModel.getDestinationDocumentFile().exists()) {
			val matchResult = regex.find(downloadModel.fileName)
			if (matchResult != null) {
				val currentIndex = matchResult.groupValues[1].toInt()
				downloadModel.fileName = downloadModel.fileName.replaceFirst(regex, "")
				index = currentIndex + 1
			} else {
				index = 1
			}
			downloadModel.fileName = "${index}_${downloadModel.fileName}"
		}
	}
	
	/**
	 * Checks whether the available storage space is sufficient to accommodate the download.
	 *
	 * @return `true` if there is enough space, `false` otherwise.
	 */
	private fun checkStorageSpace(downloadModel: DownloadDataModel): Boolean {
		val freeSpace = getFreeExternalStorageSpace()
		return freeSpace > downloadModel.fileSize
	}
}
