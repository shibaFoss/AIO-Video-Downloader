package app.core.engines.downloader

import app.core.engines.downloader.DownloadStatus.CLOSE
import app.core.engines.downloader.DownloadStatus.COMPLETE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import lib.networks.NetworkUtility.isNetworkAvailable
import lib.networks.NetworkUtility.isWifiEnabled
import lib.networks.URLUtilityKT
import lib.networks.URLUtilityKT.extractHostUrl
import lib.process.ThreadsUtility
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Handles downloading a specific part of a file using regular HTTP(s) connection.
 * This is used as part of multi-threaded or single-threaded downloading by [RegularDownloader].
 *
 * @param regularDownloader The parent downloader coordinating the entire download.
 */
class RegularDownloadPart(private val regularDownloader: RegularDownloader) {
	
	// Starting byte position of this part in the overall file
	private var partStartPoint: Long = 0
	
	// Ending byte position of this part in the overall file
	private var partEndingPoint: Long = 0
	
	// Flag to determine if this download part has been canceled
	private var isPartDownloadCanceled: Boolean = false
	
	/** Index of the current part among all the parts */
	var partIndex: Int = 0
	
	/** Total size (in bytes) of this part */
	var partChunkSize: Long = 0
	
	/** Number of bytes downloaded so far for this part */
	var partDownloadedByte: Long = 0
	
	/** Exception encountered during downloading this part, if any */
	var partDownloadErrorException: Exception? = null
	
	/** Current status of this download part */
	var partDownloadStatus: Int = CLOSE
	
	/** Reference to the download data model object */
	private val downloadDataModel = regularDownloader.downloadDataModel
	
	/** Reference to the download configuration (e.g., buffer size, WiFi-only, etc.) */
	private val downloadDataModelConfig = downloadDataModel.globalSettings
	
	/** Listener to notify status changes (e.g., cancel or complete) for this part */
	private val partStatusDownloadPartListener: DownloadPartListener = regularDownloader
	
	/**
	 * Initializes this download part with required details.
	 *
	 * @param partIndex The index of the part in the total parts list
	 * @param startingPoint The start byte offset of this part in the file
	 * @param endingPoint The end byte offset of this part in the file
	 * @param chunkSize Total number of bytes assigned for this part
	 * @param downloadedByte Number of bytes already downloaded (useful for resume)
	 */
	fun initiate(
		partIndex: Int, startingPoint: Long,
		endingPoint: Long, chunkSize: Long, downloadedByte: Long
	) {
		this.partIndex = partIndex
		this.partStartPoint = startingPoint
		this.partEndingPoint = endingPoint
		this.partChunkSize = chunkSize
		this.partDownloadedByte = downloadedByte
	}
	
	/**
	 * Starts downloading this part in a background thread.
	 * It checks for readiness and calls the internal download handler.
	 */
	fun startDownload() {
		ThreadsUtility.executeInBackground(codeBlock = {
			try {
				prepareForDownloading().let { isReady ->
					if (isReady) tryDownloading()
				}
			} catch (error: Exception) {
				error.printStackTrace()
				cancelDownload()
			}
		})
	}
	
	/**
	 * Cancels the download of this part by setting the cancellation flag.
	 */
	fun stopDownload() {
		isPartDownloadCanceled = true
	}
	
	/**
	 * Verifies if the device is connected to a suitable network.
	 * Cancels download if connection is not valid (e.g., no internet or Wi-Fi required but not connected).
	 *
	 * @return `true` if network conditions are valid, `false` otherwise.
	 */
	fun verifyNetworkConnection(): Boolean {
		val isWifiOnly = downloadDataModelConfig.downloadWifiOnly
		if (!isNetworkAvailable() || (isWifiOnly && !isWifiEnabled())) {
			cancelDownload()
			return false
		}
		return true
	}
	
	/**
	 * Checks whether internet is accessible by attempting to connect.
	 * This is a more reliable indicator than just checking network status.
	 *
	 * @return `true` if the internet is reachable, `false` otherwise.
	 */
	fun isInternetConnected(): Boolean = URLUtilityKT.isInternetConnected()
	
	/**
	 * Cancels the current download part and updates its status.
	 * Triggers listener callback to notify cancellation.
	 */
	private fun cancelDownload() {
		isPartDownloadCanceled = true
		updateDownloadPartStatus(CLOSE)
	}
	/**
	 * Prepares this download part for downloading.
	 * Resets cancellation flag and verifies if network conditions are valid.
	 *
	 * @return `true` if network is available and valid for download, `false` otherwise.
	 */
	private fun prepareForDownloading(): Boolean {
		isPartDownloadCanceled = false
		val isNetworkReady = verifyNetworkConnection()
		return isNetworkReady
	}
	
	/**
	 * Attempts to download the assigned part.
	 * If already completed, updates status.
	 * Otherwise, checks readiness and initiates server download.
	 */
	private suspend fun tryDownloading() {
		if (isPartCompleted()) {
			updateDownloadPartStatus(COMPLETE)
			return
		}
		
		isPartReadyToDownload().let { isReady ->
			if (isReady) downloadFromServer()
		}
	}
	
	/**
	 * Checks whether this part has already been fully downloaded.
	 *
	 * @return `true` if downloaded bytes are equal to or exceed chunk size, `false` otherwise.
	 */
	private fun isPartCompleted(): Boolean {
		val isCompleted = partDownloadedByte >= partChunkSize
		return isCompleted
	}
	
	/**
	 * Checks if this part is in a valid state to begin downloading.
	 *
	 * @return `true` if the part is not canceled and not already completed, `false` otherwise.
	 */
	private fun isPartReadyToDownload(): Boolean {
		val isReady = !isPartDownloadCanceled && partDownloadStatus != COMPLETE
		return isReady
	}
	
	/**
	 * Determines whether the current download is using only a single thread.
	 *
	 * @return `true` if file size is unknown or only 1 thread is allowed, `false` otherwise.
	 */
	private fun isSingleThreaded(): Boolean {
		return regularDownloader.downloadDataModel.isUnknownFileSize
				|| regularDownloader.downloadDataModel
			.globalSettings.downloadDefaultThreadConnections == 1
	}
	
	/**
	 * Updates the status of this part and notifies the listener.
	 *
	 * @param status The new status of this part (e.g., CLOSE or COMPLETE).
	 */
	private fun updateDownloadPartStatus(status: Int) {
		partDownloadStatus = status
		when (status) {
			CLOSE -> partStatusDownloadPartListener.onPartCanceled(this)
			COMPLETE -> partStatusDownloadPartListener.onPartCompleted(this)
		}
	}
	
	/**
	 * Downloads the assigned part of the file from the server.
	 * Manages the connection, reading data, writing to the file, and speed control.
	 * If an error occurs during the download, it cancels the download and handles the exception.
	 */
	private suspend fun downloadFromServer() {
		lateinit var urlConnection: HttpsURLConnection
		lateinit var inputStream: InputStream
		lateinit var fileURL: URL
		
		try {
			// Run network-related operations on the IO thread using the context dispatcher.
			withContext(Dispatchers.IO) {
				// Get the destination file and prepare it if necessary.
				val destinationFile = regularDownloader.downloadDataModel.getDestinationFile()
				// If single-threaded and the file doesn't exist, reset the file length.
				if (isSingleThreaded() && !destinationFile.exists())
					RandomAccessFile(destinationFile, "rw").setLength(0)
				
				// Open the destination file for random access to write bytes at specific positions.
				val randomAccessFile = RandomAccessFile(destinationFile, "rw")
				val fileOutputPosition = calculateFileOutputPosition(randomAccessFile)
				randomAccessFile.seek(fileOutputPosition)
				
				// Prepare the connection to the server.
				fileURL = URL(regularDownloader.downloadDataModel.fileURL)
				urlConnection = fileURL.openConnection() as HttpsURLConnection
				
				// Calculate the range for this part and configure the connection.
				val fileByteRange: String = calculateRange()
				configureConnection(urlConnection, fileByteRange)
				urlConnection.connect()
				inputStream = urlConnection.inputStream
				
				// Set the buffer size and create a buffer for downloading data.
				val bufferSize = regularDownloader.downloadDataModel.globalSettings.downloadBufferSize
				val buffer = ByteArray(bufferSize)
				var fetchedBytes = 0
				val startTime = System.currentTimeMillis()
				
				// Download data in chunks, ensuring the download is not canceled.
				while (
					!isPartDownloadCanceled && destinationFile.exists() &&
					inputStream.read(buffer).also { fetchedBytes = it } != -1
				) {
					// Limit the download speed based on the time taken for previous downloads.
					limitDownloadSpeed(startTime, fetchedBytes)
					
					// Write the downloaded data to the file, either single-threaded or multi-threaded.
					if (isSingleThreaded()) {
						randomAccessFile.write(buffer, 0, fetchedBytes)
						partDownloadedByte += fetchedBytes
					} else {
						val bytesToWrite = minOf(fetchedBytes.toLong(), getRemainingByteToWrite())
						randomAccessFile.write(buffer, 0, bytesToWrite.toInt())
						partDownloadedByte += bytesToWrite
						// Break if the part has been fully downloaded.
						if (partDownloadedByte >= partChunkSize) break
					}
				}
				
				// Clean up by closing the streams and connections.
				inputStream.close()
				urlConnection.disconnect()
				randomAccessFile.close()
				
				// Handle case where the destination file no longer exists.
				if (!destinationFile.exists()) {
					partDownloadErrorException = Exception("Destination file is not found")
					cancelDownload()
					return@withContext
				}
				
				// Update the status to COMPLETE if the download was not canceled.
				if (!isPartDownloadCanceled) {
					updateDownloadPartStatus(COMPLETE)
				}
			}
		} catch (error: Exception) {
			// Catch any errors during the download process.
			error.printStackTrace()
			partDownloadErrorException = error
			cancelDownload()
		}
	}
	
	/**
	 * Limits the download speed by controlling the time between data fetches
	 * based on the specified maximum network speed in the global settings.
	 * This ensures that the download does not exceed the defined speed limit.
	 *
	 * @param startTime The time when the download started.
	 * @param fetchedBytes The number of bytes fetched in the current iteration.
	 */
	private suspend fun limitDownloadSpeed(startTime: Long, fetchedBytes: Int) {
		val elapsedTime = System.currentTimeMillis() - startTime
		val speedLimit = regularDownloader.downloadDataModel.globalSettings.downloadMaxNetworkSpeed
		// If speed limit is greater than zero, check if download speed needs to be limited.
		if (speedLimit > 0) {
			// Calculate the expected time to fetch the current bytes at the speed limit.
			val expectedTime = (fetchedBytes * 1000L) / speedLimit
			// Delay the download if the elapsed time is less than the expected time.
			if (elapsedTime < expectedTime) {
				delay(expectedTime - elapsedTime)
			}
		}
	}
	
	/**
	 * Calculates the number of bytes remaining to be written in this part of the download.
	 *
	 * @return The remaining bytes to write.
	 */
	private fun getRemainingByteToWrite(): Long = partChunkSize - partDownloadedByte
	
	/**
	 * Calculates the file output position based on the download part's starting point
	 * and the number of bytes already downloaded. Resets the data model if single-threaded.
	 *
	 * @param file The `RandomAccessFile` representing the destination file.
	 * @return The position in the file where the download should continue.
	 */
	private fun calculateFileOutputPosition(file: RandomAccessFile): Long {
		return if (isResumeNotSupported()) resetDataModelForSingleThread(file)
		else partStartPoint + partDownloadedByte
	}
	
	/**
	 * Resets the data model for single-threaded downloads, setting the progress to zero
	 * and updating the file length.
	 *
	 * @param file The `RandomAccessFile` representing the destination file.
	 * @return The reset file position (0 bytes downloaded).
	 */
	private fun resetDataModelForSingleThread(file: RandomAccessFile): Long {
		partDownloadedByte = 0
		downloadDataModel.partsDownloadedByte[partIndex] = 0
		downloadDataModel.partStartingPoint[partIndex] = 0
		downloadDataModel.partProgressPercentage[partIndex] = 0
		file.setLength(partDownloadedByte)
		return partDownloadedByte
	}
	
	/**
	 * Determines if resuming the download is not supported by checking the data model.
	 *
	 * @return `true` if resume is not supported, `false` otherwise.
	 */
	private fun isResumeNotSupported(): Boolean = !regularDownloader.downloadDataModel.isResumeSupported
	
	/**
	 * Configures the HTTP connection with the required headers, including range information,
	 * user agent, cookies, and other settings necessary for downloading the file.
	 *
	 * @param urlConnection The HTTP connection to be configured.
	 * @param range The byte range for this download part.
	 */
	private fun configureConnection(urlConnection: HttpsURLConnection, range: String) {
		val settings = regularDownloader.downloadDataModel.globalSettings
		// General connection settings
		urlConnection.instanceFollowRedirects = true
		urlConnection.useCaches = false
		urlConnection.setRequestProperty("Accept", "*/*")
		urlConnection.setRequestProperty("Range", range)
		urlConnection.setRequestProperty(
			"User-Agent",
			settings.downloadHttpUserAgent.ifEmpty { settings.browserHttpUserAgent }
		)
		
		// Additional browser-specific headers if downloading from a browser
		if (downloadDataModel.isDownloadFromBrowser) {
			with(downloadDataModel) {
				URI(fileURL).host?.let { urlConnection.setRequestProperty("Host", it) }
				siteReferrer.takeIf { it.isNotEmpty() }?.let {
					urlConnection.setRequestProperty("Referer", extractHostUrl(it))
				}
				fileContentDisposition.takeIf { it.isNotEmpty() }?.let {
					urlConnection.setRequestProperty("Content-Disposition", it)
				}
				siteCookieString.takeIf { it.isNotEmpty() }?.let {
					urlConnection.setRequestProperty("Cookie", it)
				}
				// Additional browser headers
				urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
				urlConnection.setRequestProperty("Sec-Fetch-Dest", "document")
				urlConnection.setRequestProperty("Sec-Fetch-Mode", "navigate")
			}
		}
		
		// Timeout settings for connection and reading
		urlConnection.setReadTimeout(settings.downloadMaxHttpReadingTimeout)
		urlConnection.setConnectTimeout(settings.downloadMaxHttpReadingTimeout)
	}
	
	/**
	 * Calculates the byte range for the current part based on the start and end points
	 * and the number of bytes already downloaded. This range is used in the HTTP request.
	 *
	 * @return The byte range string used in the HTTP "Range" header.
	 */
	private fun calculateRange(): String {
		return if (isSingleThreaded()) {
			// For single-threaded download, continue from the current position.
			"bytes=$partDownloadedByte-"
			
		} else if (downloadDataModel.isMultiThreadSupported) {
			// Multi-threaded range.
			"bytes=${partStartPoint + partDownloadedByte}-${partEndingPoint}"
			
		} else {
			// Fall-back range for unknown conditions.
			"bytes=${partStartPoint + partDownloadedByte}-"
		}
	}
	
	/**
	 * Verifies whether the server supports byte range requests by sending a "HEAD" request
	 * and checking the response headers for the "Accept-Ranges" field.
	 *
	 * @return `true` if the server supports range requests, `false` otherwise.
	 */
	private suspend fun verifyRangeSupport(): Boolean {
		return withContext(Dispatchers.IO) {
			try {
				// Open a connection to the file URL and send a HEAD request.
				val urlConnection = URL(downloadDataModel.fileURL).openConnection() as HttpsURLConnection
				urlConnection.requestMethod = "HEAD"
				urlConnection.connect()
				// Check if the server accepts byte ranges.
				val acceptsRanges = urlConnection.getHeaderField("Accept-Ranges") == "bytes"
				urlConnection.disconnect()
				acceptsRanges
			} catch (e: Exception) {
				// In case of any error, assume the server does not support range requests.
				false
			}
		}
	}
	
	/**
	 * Interface for listeners to handle download part state changes, such as when a part
	 * is canceled or completed.
	 */
	interface DownloadPartListener {
		/**
		 * Called when a download part is canceled.
		 *
		 * @param downloadPart The download part that was canceled.
		 */
		fun onPartCanceled(downloadPart: RegularDownloadPart)
		
		/**
		 * Called when a download part is completed.
		 *
		 * @param downloadPart The download part that was completed.
		 */
		fun onPartCompleted(downloadPart: RegularDownloadPart)
	}
}