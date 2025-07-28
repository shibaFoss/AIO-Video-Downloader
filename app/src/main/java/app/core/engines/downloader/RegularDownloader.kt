package app.core.engines.downloader

import android.os.CountDownTimer
import app.core.AIOApp.Companion.INSTANCE
import app.core.engines.downloader.DownloadStatus.CLOSE
import app.core.engines.downloader.DownloadStatus.COMPLETE
import app.core.engines.downloader.DownloadStatus.DOWNLOADING
import app.core.engines.downloader.DownloadURLHelper.getFileInfoFromSever
import app.core.engines.downloader.RegularDownloadPart.DownloadPartListener
import com.aio.R.raw
import com.aio.R.string
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lib.device.DateTimeUtils.calculateTime
import lib.device.DateTimeUtils.millisToDateTimeString
import lib.files.FileSizeFormatter.humanReadableSizeOf
import lib.networks.DownloaderUtils.calculateDownloadSpeed
import lib.networks.DownloaderUtils.calculateDownloadSpeedInFormat
import lib.networks.DownloaderUtils.formatDownloadSpeedInSimpleForm
import lib.networks.DownloaderUtils.getFormattedPercentage
import lib.networks.DownloaderUtils.getHumanReadableFormat
import lib.networks.DownloaderUtils.getRemainingDownloadTime
import lib.networks.NetworkUtility.isNetworkAvailable
import lib.networks.NetworkUtility.isWifiEnabled
import lib.networks.URLUtility.getOriginalURL
import lib.networks.URLUtility.isValidURL
import lib.networks.URLUtilityKT.isInternetConnected
import lib.process.AudioPlayerUtils
import lib.texts.CommonTextUtils.getText
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URL

/**
 * A class that handles regular file downloads with support for multi-threading, resume functionality,
 * and progress tracking. Implements [DownloadTaskInf] and [DownloadPartListener] interfaces.
 *
 * @property downloadDataModel The data model containing download configuration and progress information.
 */
class RegularDownloader(override val downloadDataModel: DownloadDataModel) :
	DownloadTaskInf, DownloadPartListener {
	
	// Global settings derived from the download data model, used to control download behavior
	private val downloadDataModelConfig = downloadDataModel.globalSettings
	
	// File object representing the final destination of the download
	private var destinationFile: File = downloadDataModel.getDestinationFile()
	
	// List of parts that represent segments of the file being downloaded
	private var downloadParts: ArrayList<RegularDownloadPart> = ArrayList()
	
	// Timer to periodically trigger download progress updates
	private var downloadTimer: CountDownTimer? = null
	
	// Utility to monitor and track current network speed during download
	private var netSpeedTracker: NetSpeedTracker? = null
	
	// Listener to report status updates such as progress, completion, cancellation, etc.
	override var statusListener: DownloadTaskListener? = null
	
	/**
	 * Initializes the download task by preparing the data model,
	 * ensuring the destination file is set, and setting up a timer
	 * to monitor progress during the download.
	 */
	override fun initiate() {
		CoroutineScope(Dispatchers.IO).launch {
			initDownloadDataModel()
			initDestinationFile()
			initDownloadTaskTimer()
		}
	}
	
	/**
	 * Starts the download process by:
	 * - Configuring the download model and its parts
	 * - Creating the destination file (as an empty file)
	 * - Spawning all download threads
	 * - Updating status and starting the download timer upon success
	 */
	override fun startDownload() {
		CoroutineScope(Dispatchers.IO).launch {
			configureDownloadModel()
			configureDownloadParts()
			createEmptyDestinationFile()
			startAllDownloadThreads().let { isSuccess ->
				if (isSuccess) {
					updateDownloadStatus(getText(string.title_started_downloading), DOWNLOADING)
					downloadTimer?.start()
				} else {
					updateDownloadStatus(getText(string.title_download_io_failed), CLOSE)
				}
			}
		}
	}
	
	/**
	 * Cancels the ongoing download operation.
	 * - Stops all part downloads
	 * - Updates the download status to paused or the specified reason
	 * - Ensures exception safety in cancellation flow
	 *
	 * @param cancelReason Optional text describing why the download was canceled
	 */
	override fun cancelDownload(cancelReason: String) {
		try {
			downloadParts.forEach { part -> part.stopDownload() }
			val statusMessage = cancelReason.ifEmpty { getText(string.title_paused) }
			updateDownloadStatus(statusMessage, CLOSE)
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Callback invoked when a download part has been canceled.
	 * Handles retrying or stopping the download depending on the error type.
	 *
	 * @param downloadPart The part of the download that triggered this cancellation
	 */
	@Synchronized
	override fun onPartCanceled(downloadPart: RegularDownloadPart) {
		CoroutineScope(Dispatchers.IO).launch {
			val isCritical = isCriticalErrorFoundInDownloadPart(downloadPart)
			if (isCritical) {
				if (downloadDataModel.isFileUrlExpired) {
					cancelDownload(getText(string.title_link_expired)); return@launch
				}
				
				if (downloadDataModel.isDestinationFileNotExisted) {
					cancelDownload(getText(string.title_file_deleted_paused)); return@launch
				}
			} else {
				restartDownload(downloadPart)
			}
			
			downloadDataModel.updateInStorage()
			
			// Clean up if the download was marked as deleted
			if (!downloadDataModel.isRemoved && downloadDataModel.isDeleted) {
				if (destinationFile.exists()) destinationFile.delete()
			}
		}
	}
	
	/**
	 * Callback invoked when a single download part completes.
	 * Checks if all parts are completed to finalize the entire download.
	 *
	 * @param downloadPart The completed part of the download
	 */
	@Synchronized
	override fun onPartCompleted(downloadPart: RegularDownloadPart) {
		CoroutineScope(Dispatchers.IO).launch {
			var isAllPartCompleted = true
			downloadParts.forEach { part ->
				if (part.partDownloadStatus != COMPLETE) isAllPartCompleted = false
			}
			
			// Return early if any part is not completed yet
			if (!isAllPartCompleted) return@launch
			
			// Play completion sound if enabled
			if (downloadDataModelConfig.downloadPlayNotificationSound) {
				AudioPlayerUtils(INSTANCE).play(raw.download_finished_sfx)
			}
			
			downloadDataModel.isRunning = false
			downloadDataModel.isComplete = true
			updateDownloadStatus(getText(string.title_completed), COMPLETE)
		}
	}
	
	/**
	 * Initializes the download data model with default values before starting a new download.
	 * Resets the status, flags, retry count, and displays a waiting message.
	 * Also persists the updated state to storage.
	 */
	private fun initDownloadDataModel() {
		downloadDataModel.status = CLOSE
		downloadDataModel.isRunning = false
		downloadDataModel.isWaitingForNetwork = false
		downloadDataModel.totalConnectionRetries = 0
		downloadDataModel.statusInfo = getText(string.text_waiting_to_join)
		downloadDataModel.updateInStorage()
	}
	
	/**
	 * Re-fetches and assigns the destination file reference from the data model.
	 * Ensures that the correct file is used for download storage.
	 */
	private fun initDestinationFile() {
		destinationFile = downloadDataModel.getDestinationFile()
	}
	
	/**
	 * Initializes a timer that ticks every 500 milliseconds to update download progress.
	 * If the download is still in progress when the timer finishes, it restarts itself.
	 */
	private fun initDownloadTaskTimer() {
		CoroutineScope(Dispatchers.Main).launch {
			downloadTimer = object : CountDownTimer((1000 * 60), 500) {
				
				override fun onTick(millisUntilFinished: Long) {
					updateDownloadProgress()
				}
				
				override fun onFinish() {
					if (downloadDataModel.status == DOWNLOADING) {
						start()
					}
				}
			}
		}
	}
	
	/**
	 * Configures the download model before beginning download.
	 * Skips configuration if previous download data is found.
	 * Otherwise, sets up auto-resume, auto-remove, URL filtering, file info and part range.
	 */
	private fun configureDownloadModel() {
		updateDownloadStatus(getText(string.title_validating_download))
		if (doesDownloadModelHasPreviousData()) return
		
		configureDownloadAutoResumeSettings()
		configureDownloadAutoRemoveSettings()
		configureDownloadAutoFilterURL()
		configureDownloadFileInfo()
		configureDownloadPartRange()
	}
	
	/**
	 * Checks if the download model contains previously downloaded data.
	 * If data is found but the destination file is missing, marks the model as failed.
	 *
	 * @return true if previous download data exists, false otherwise.
	 */
	private fun doesDownloadModelHasPreviousData(): Boolean {
		val isPreviousDataFound = downloadDataModel.downloadedByte > 0
		
		if (isPreviousDataFound) {
			updateDownloadStatus(getText(string.text_old_data_found))
			
			if (!destinationFile.exists()) {
				downloadDataModel.isFailedToAccessFile = true
				cancelDownload(getText(string.title_failed_deleted_paused))
			}
		}; return isPreviousDataFound
	}
	
	/**
	 * Configures auto-resume settings for the download model.
	 * Disables max error limit if auto-resume is not enabled.
	 */
	private fun configureDownloadAutoResumeSettings() {
		updateDownloadStatus(getText(string.text_configuring_auto_resume))
		if (!downloadDataModelConfig.downloadAutoResume) {
			downloadDataModelConfig.downloadAutoResumeMaxErrors = 0
		}
	}
	
	/**
	 * Configures auto-remove settings for the download model.
	 * Disables removal timing if auto-remove is not enabled.
	 */
	private fun configureDownloadAutoRemoveSettings() {
		updateDownloadStatus(statusInfo = getText(string.text_configuring_auto_remove))
		if (!downloadDataModelConfig.downloadAutoRemoveTasks) {
			downloadDataModelConfig.downloadAutoRemoveTaskAfterNDays = 0
		}
	}
	
	/**
	 * Attempts to clean and resolve redirected download links if link redirection is enabled.
	 * This ensures the actual file URL is used for downloading.
	 */
	private fun configureDownloadAutoFilterURL() {
		if (downloadDataModelConfig.downloadAutoLinkRedirection) {
			updateDownloadStatus(statusInfo = getText(string.text_filtering_url))
			val originalURL = getOriginalURL(downloadDataModel.fileURL)
			if (originalURL != null) downloadDataModel.fileURL = originalURL
		}
	}
	
	/**
	 * Fetches and configures detailed information about the file from the server.
	 * This includes file size, name, support for resume and multipart downloads.
	 * Sets flags for unknown file sizes and adjusts thread count as needed.
	 */
	private fun configureDownloadFileInfo() {
		if (downloadDataModel.fileSize <= 1) {
			updateDownloadStatus(statusInfo = getText(string.title_recalculating_file_size))
			
			if (!isValidURL(downloadDataModel.fileURL)) {
				cancelDownload(getText(string.title_invalid_file_url))
			} else {
				val fileInfo = getFileInfoFromSever(URL(downloadDataModel.fileURL))
				downloadDataModel.fileSize = fileInfo.fileSize
				downloadDataModel.fileChecksum = fileInfo.fileChecksum
				
				if (downloadDataModel.fileName.isEmpty()) {
					downloadDataModel.fileName = fileInfo.fileName
				}
				
				downloadDataModel.isUnknownFileSize = false
				downloadDataModel.isResumeSupported = fileInfo.isSupportsResume
				downloadDataModel.isMultiThreadSupported = fileInfo.isSupportsMultipart
				downloadDataModel.fileSizeInFormat = humanReadableSizeOf(downloadDataModel.fileSize)
				
				// Handle edge case: file size is still unknown after server check
				if (downloadDataModel.fileSize <= 1) {
					downloadDataModel.isUnknownFileSize = true
					downloadDataModel.isMultiThreadSupported = false
					downloadDataModelConfig.downloadDefaultThreadConnections = 1
					downloadDataModel.fileSizeInFormat = getText(string.title_unknown)
				}
			}
		}
	}
	
	/**
	 * Initializes part-wise download ranges based on the file size and number of threads.
	 * Sets up starting points, ending points, chunk sizes, and byte tracking arrays.
	 * If the file size is unknown, it sets the chunk size to the file size and skips range calculation.
	 */
	private fun configureDownloadPartRange() {
		val numberOfThreads = downloadDataModelConfig.downloadDefaultThreadConnections
		downloadDataModel.partStartingPoint = LongArray(numberOfThreads)
		downloadDataModel.partEndingPoint = LongArray(numberOfThreads)
		downloadDataModel.partChunkSizes = LongArray(numberOfThreads)
		downloadDataModel.partsDownloadedByte = LongArray(numberOfThreads)
		downloadDataModel.partProgressPercentage = IntArray(numberOfThreads)
		
		if (downloadDataModel.isUnknownFileSize || downloadDataModel.fileSize < 1) {
			downloadDataModel.partChunkSizes[0] = downloadDataModel.fileSize
		} else {
			val ranges = calculateAlignedPartRanges(downloadDataModel.fileSize, numberOfThreads)
			ranges.forEachIndexed { index, (start, end) ->
				downloadDataModel.partStartingPoint[index] = start
				downloadDataModel.partEndingPoint[index] = end
				downloadDataModel.partChunkSizes[index] = end - start + 1
			}
		}
	}
	
	/**
	 * Calculates the aligned byte ranges for multithreaded downloading.
	 *
	 * @param fileSize The total size of the file in bytes.
	 * @param numberOfThreads The number of threads to split the file into.
	 * @param alignmentBoundary Optional alignment size, defaulting to 4096 bytes (disk block size).
	 * @return A list of pairs containing start and end byte positions for each thread.
	 */
	private fun calculateAlignedPartRanges(
		fileSize: Long,
		numberOfThreads: Int,
		alignmentBoundary: Long = 4096L
	): List<Pair<Long, Long>> {
		val basePartSize = fileSize / numberOfThreads
		val ranges = mutableListOf<Pair<Long, Long>>()
		
		for (threadNumber in 0 until numberOfThreads) {
			val startByte = threadNumber * basePartSize
			val endByte = if (threadNumber == numberOfThreads - 1) {
				fileSize - 1
			} else {
				alignToBoundary(startByte + basePartSize - 1, alignmentBoundary)
			}
			ranges.add(Pair(startByte, endByte))
		}
		
		return ranges
	}
	
	/**
	 * Aligns a given byte position to the next multiple of the specified boundary.
	 *
	 * @param position The position to align.
	 * @param boundary The alignment boundary (e.g., 4096 for 4KB alignment).
	 * @return The aligned position.
	 */
	private fun alignToBoundary(position: Long, boundary: Long): Long {
		val alignedPosition = ((position + boundary - 1) / boundary) * boundary
		return alignedPosition
	}
	
	/**
	 * Generates and initializes download parts for multithreaded downloading.
	 */
	private fun configureDownloadParts() {
		downloadParts = generateDownloadParts()
	}
	
	/**
	 * Creates an empty file with the specified size for writing downloaded content.
	 * This is required for multi-threaded downloads using RandomAccessFile.
	 * Handles exceptions and updates download state if file access fails.
	 */
	private fun createEmptyDestinationFile() {
		if (downloadDataModel.isDeleted) return
		try {
			if (destinationFile.exists()) return
			if (isMultiThreadDownloadSupported()) {
				updateDownloadStatus(getText(string.title_creating_empty_file))
				RandomAccessFile(destinationFile, "rw").setLength(downloadDataModel.fileSize)
			}
		} catch (error: IOException) {
			error.printStackTrace()
			downloadDataModel.totalConnectionRetries++
			downloadDataModel.isFailedToAccessFile = true
			cancelDownload(getText(string.title_download_io_failed))
		}
	}
	
	/**
	 * Checks if the current download configuration supports multi-threaded downloads.
	 *
	 * @return True if multiple threads can be used; false otherwise.
	 */
	private fun isMultiThreadDownloadSupported(): Boolean {
		val maxThreadsAllowed = downloadDataModelConfig.downloadDefaultThreadConnections
		val isNotUnknownFileSize = !downloadDataModel.isUnknownFileSize
		val isSupported = downloadDataModel.fileSize > 0 && maxThreadsAllowed > 1 && isNotUnknownFileSize
		return isSupported
	}
	
	/**
	 * Checks if a critical error occurred in a download part.
	 *
	 * @param downloadPart The download part to inspect.
	 * @return True if a critical error (e.g., file not found) was found; false otherwise.
	 */
	private fun isCriticalErrorFoundInDownloadPart(downloadPart: RegularDownloadPart): Boolean {
		val errorFound = downloadPart.partDownloadErrorException != null
		if (errorFound) {
			if (downloadPart.partDownloadErrorException is FileNotFoundException) {
				downloadDataModel.isFileUrlExpired = true
				if (!destinationFile.exists()) downloadDataModel.isDestinationFileNotExisted = true
				return true
			}
		}
		return false
	}
	
	/**
	 * Determines if retrying a failed download is allowed based on retry count and running state.
	 *
	 * @return True if retrying is permitted; false otherwise.
	 */
	private fun isRetryingAllowed(): Boolean {
		val maxErrorAllowed = downloadDataModelConfig.downloadAutoResumeMaxErrors
		val retryAllowed = downloadDataModel.isRunning &&
				downloadDataModel.totalConnectionRetries < maxErrorAllowed
		return retryAllowed
	}
	
	/**
	 * Attempts to restart a failed or interrupted download part.
	 * Validates network availability and user-defined constraints (e.g., Wi-Fi only).
	 *
	 * @param downloadPart The part that should be restarted.
	 */
	private fun restartDownload(downloadPart: RegularDownloadPart) {
		if (isRetryingAllowed()) {
			if (!isNetworkAvailable()) {
				downloadDataModel.isWaitingForNetwork = true
				updateDownloadStatus(getText(string.text_waiting_for_network))
				return
			}
			
			if (downloadDataModelConfig.downloadWifiOnly && !isWifiEnabled()) {
				downloadDataModel.isWaitingForNetwork = true
				updateDownloadStatus(getText(string.text_waiting_for_wifi))
				return
			}
			
			if (!downloadPart.isInternetConnected()) {
				downloadDataModel.isWaitingForNetwork = true
				updateDownloadStatus(getText(string.text_waiting_for_internet))
				return
			}
			
			if (downloadDataModel.isWaitingForNetwork) {
				downloadDataModel.isWaitingForNetwork = false
				updateDownloadStatus(getText(string.title_started_downloading))
				downloadPart.startDownload()
			}
			
			downloadDataModel.totalConnectionRetries++
		}
	}
	
	/**
	 * Starts all download threads for each part of the file.
	 * Skips threads that are already marked as downloading.
	 *
	 * @return True if threads were started successfully; false if file access failed.
	 */
	private fun startAllDownloadThreads(): Boolean {
		if (downloadDataModel.isFailedToAccessFile) {
			downloadDataModel.msgToShowUserViaDialog =
				getText(string.text_failed_to_write_file_to_storage)
			return false
		} else {
			downloadParts.forEach { downloadPart ->
				if (downloadPart.partDownloadStatus != DOWNLOADING) {
					downloadPart.startDownload()
				}
			}
			return true
		}
	}
	
	/**
	 * Generates all the download parts based on the current configuration and initializes them.
	 * Each part represents a segment of the file to be downloaded.
	 */
	private fun generateDownloadParts(): ArrayList<RegularDownloadPart> {
		updateDownloadStatus(getText(string.title_generating_parts))
		
		val numberOfThreads = downloadDataModelConfig.downloadDefaultThreadConnections
		val regularDownloadParts = ArrayList<RegularDownloadPart>(numberOfThreads)
		
		// Create and configure each download part
		for (index in 0 until numberOfThreads) {
			val downloadPart = RegularDownloadPart(this@RegularDownloader)
			downloadPart.initiate(
				partIndex = index,
				startingPoint = downloadDataModel.partStartingPoint[index],
				endingPoint = downloadDataModel.partEndingPoint[index],
				chunkSize = downloadDataModel.partChunkSizes[index],
				downloadedByte = downloadDataModel.partsDownloadedByte[index]
			)
			regularDownloadParts.add(downloadPart)
		}
		
		return regularDownloadParts
	}
	
	/**
	 * Triggers download progress update on a background coroutine.
	 */
	private fun updateDownloadProgress() {
		CoroutineScope(Dispatchers.IO).launch {
			calculateProgressAndModifyDownloadModel()
			checkNetworkConnectionAndRetryDownload()
			if (!downloadDataModel.isRunning) downloadTimer?.cancel()
			updateDownloadStatus()
		}
	}
	
	/**
	 * Updates the current download status, status info, and notifies listeners on the main thread.
	 */
	override fun updateDownloadStatus(statusInfo: String?, status: Int) {
		CoroutineScope(Dispatchers.IO).launch {
			if (!statusInfo.isNullOrEmpty()) {
				downloadDataModel.statusInfo = statusInfo
			}
			
			downloadDataModel.status = status
			downloadDataModel.isRunning = (status == DOWNLOADING)
			downloadDataModel.isComplete = (status == COMPLETE)
			downloadDataModel.updateInStorage()
			
			CoroutineScope(Dispatchers.Main).launch {
				statusListener?.onStatusUpdate(this@RegularDownloader)
			}
			
			if (!downloadDataModel.isRunning || downloadDataModel.isComplete) {
				downloadTimer?.cancel()
			}
		}
	}
	
	/**
	 * Recalculates all download-related metrics and updates the model accordingly.
	 */
	private fun calculateProgressAndModifyDownloadModel() {
		calculateTotalDownloadedTime()
		calculateDownloadedBytes()
		calculateDownloadPercentage()
		updateLastModificationDate()
		calculateAverageDownloadSpeed()
		calculateRealtimeDownloadSpeed()
		calculateMaxDownloadSpeed()
		calculateRemainingDownloadTime()
		validatingDownloadCompletion()
		downloadDataModel.updateInStorage()
	}
	
	/**
	 * Ensures no download part is stuck even when it claims to be completed.
	 */
	private fun validatingDownloadCompletion() {
		if (downloadDataModel.isUnknownFileSize) return
		if (downloadDataModel.fileSize < 1) return
		
		downloadParts.forEach {
			if (it.partDownloadedByte >= it.partChunkSize) {
				if (it.partDownloadStatus != COMPLETE) {
					it.stopDownload().apply { startDownload() }
				}
			}
		}
	}
	
	/**
	 * Calculates the total bytes downloaded across all parts and updates the progress percentage.
	 */
	private fun calculateDownloadedBytes() {
		downloadDataModel.downloadedByte = 0
		
		downloadParts.forEach { downloadPart ->
			downloadDataModel.downloadedByte += downloadPart.partDownloadedByte
			downloadDataModel.partsDownloadedByte[downloadPart.partIndex] =
				downloadPart.partDownloadedByte
			downloadDataModel.partProgressPercentage[downloadPart.partIndex] =
				if (downloadDataModel.isUnknownFileSize) {
					0
				} else {
					((downloadPart.partDownloadedByte * 100) / downloadPart.partChunkSize).toInt()
				}
		}
		
		// Format total downloaded bytes
		downloadDataModel.downloadedByteInFormat = getHumanReadableFormat(downloadDataModel.downloadedByte)
		
		// Update unknown file size dynamically
		if (!isMultiThreadDownloadSupported() && downloadDataModel.isUnknownFileSize) {
			downloadDataModel.fileSize = downloadDataModel.getDestinationFile().length()
			downloadDataModel.fileSizeInFormat = humanReadableSizeOf(downloadDataModel.fileSize)
		}
	}
	
	/**
	 * Calculates the overall download percentage and formats it for display.
	 */
	private fun calculateDownloadPercentage() {
		downloadDataModel.progressPercentage = if (downloadDataModel.isUnknownFileSize) {
			0
		} else {
			if (downloadDataModel.fileSize != 0L) {
				(downloadDataModel.downloadedByte * 100) / downloadDataModel.fileSize
			} else {
				0
			}
		}
		
		downloadDataModel.progressPercentageInFormat = getFormattedPercentage(downloadDataModel)
	}
	
	/**
	 * Updates the last modified timestamp of the download session.
	 */
	private fun updateLastModificationDate() {
		downloadDataModel.lastModifiedTimeDate = System.currentTimeMillis()
		downloadDataModel.lastModifiedTimeDateInFormat =
			millisToDateTimeString(downloadDataModel.lastModifiedTimeDate)
	}
	
	/**
	 * Calculates the average download speed based on total bytes and elapsed time.
	 */
	private fun calculateAverageDownloadSpeed() {
		val downloadedByte = downloadDataModel.downloadedByte
		val downloadedTime = downloadDataModel.timeSpentInMilliSec
		downloadDataModel.averageSpeed = calculateDownloadSpeed(downloadedByte, downloadedTime)
		downloadDataModel.averageSpeedInFormat = calculateDownloadSpeedInFormat(
			downloadedByte,
			downloadedTime
		)
	}
	
	/**
	 * Calculates the real-time download speed using a network speed tracker.
	 */
	private fun calculateRealtimeDownloadSpeed() {
		if (netSpeedTracker == null) {
			netSpeedTracker = NetSpeedTracker(
				initialBytesDownloaded = downloadDataModel.downloadedByte
			)
		}
		
		netSpeedTracker?.let {
			it.update(downloadDataModel.downloadedByte)
			val currentSpeed = it.getCurrentSpeed()
			
			downloadDataModel.realtimeSpeed = (if (currentSpeed < 0) 0 else currentSpeed)
			downloadDataModel.realtimeSpeedInFormat = (if (currentSpeed < 0)
				formatDownloadSpeedInSimpleForm(0.0) else it.getFormattedSpeed())
			
			if (!downloadDataModel.isRunning) {
				downloadDataModel.realtimeSpeed = 0
				downloadDataModel.realtimeSpeedInFormat = "--"
			}
		}
	}
	
	/**
	 * Tracks the maximum download speed reached so far.
	 */
	private fun calculateMaxDownloadSpeed() {
		if (downloadDataModel.realtimeSpeed > downloadDataModel.maxSpeed) {
			downloadDataModel.maxSpeed = downloadDataModel.realtimeSpeed
			downloadDataModel.maxSpeedInFormat = formatDownloadSpeedInSimpleForm(downloadDataModel.maxSpeed.toDouble())
		}
	}
	
	/**
	 * Increments the total time spent downloading, ignoring idle/waiting time.
	 */
	private fun calculateTotalDownloadedTime() {
		if (!downloadDataModel.isWaitingForNetwork) {
			downloadDataModel.timeSpentInMilliSec += 500
		}
		
		val timeSpentInMillis = downloadDataModel.timeSpentInMilliSec.toFloat()
		downloadDataModel.timeSpentInFormat = calculateTime(timeSpentInMillis, getText(string.text_spent))
	}
	
	/**
	 * Estimates the remaining download time based on current speed and remaining bytes.
	 */
	private fun calculateRemainingDownloadTime() {
		if (!downloadDataModel.isUnknownFileSize || !downloadDataModel.isWaitingForNetwork) {
			val remainingByte = downloadDataModel.fileSize - downloadDataModel.downloadedByte
			val averageSpeed = downloadDataModel.averageSpeed
			val remainingTime = getRemainingDownloadTime(remainingByte, averageSpeed)
			
			downloadDataModel.remainingTimeInSec = remainingTime
			downloadDataModel.remainingTimeInFormat = 
				calculateTime(remainingTime.toFloat(), getText(string.text_left))
		} else {
			downloadDataModel.remainingTimeInSec = 0L
			downloadDataModel.remainingTimeInFormat = "-:-"
		}
	}
	
	/**
	 * Checks network state and resumes download if conditions are met.
	 */
	private fun checkNetworkConnectionAndRetryDownload() {
		// Check connectivity status for each download part
		downloadParts.forEach { downloadPart -> downloadPart.verifyNetworkConnection() }
		
		// If download is waiting for network, attempt resuming when possible
		if (downloadDataModel.isWaitingForNetwork) {
			if (isNetworkAvailable() && isInternetConnected()) {
				if (downloadDataModelConfig.downloadWifiOnly && !isWifiEnabled()) return
				
				downloadDataModel.isWaitingForNetwork = false
				updateDownloadStatus(getText(string.title_started_downloading))
				startAllDownloadThreads()
			}
		}
	}
}