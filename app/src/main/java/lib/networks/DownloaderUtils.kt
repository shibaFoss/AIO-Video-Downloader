package lib.networks

import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import androidx.documentfile.provider.DocumentFile
import androidx.documentfile.provider.DocumentFile.fromFile
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.IS_PREMIUM_USER
import app.core.engines.downloader.DownloadDataModel
import com.aio.R
import lib.device.DateTimeUtils.calculateTime
import lib.device.DateTimeUtils.formatVideoDuration
import lib.files.FileUtility.isAudio
import lib.files.FileUtility.isVideo
import lib.files.FileUtility.isWritableFile
import lib.texts.CommonTextUtils.removeDuplicateSlashes
import java.io.File
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/**
 * Utility class for download-related operations including formatting, calculations,
 * file handling, and metadata extraction.
 */
object DownloaderUtils {
	
	// Decimal formatter for consistent number formatting
	private val decimalFormat = DecimalFormat("##.##")
	
	/**
	 * Formats the download progress percentage.
	 * @param model The download data model containing progress information
	 * @return Formatted percentage string
	 */
	@JvmStatic
	fun getFormattedPercentage(model: DownloadDataModel): String {
		return decimalFormat.format(model.progressPercentage.toDouble())
	}
	
	/**
	 * Formats a double value using the standard decimal format.
	 * @param input The double value to format
	 * @return Formatted string
	 */
	@JvmStatic fun getFormatted(input: Double): String = decimalFormat.format(input)
	
	/**
	 * Formats a float value using the standard decimal format.
	 * @param input The float value to format
	 * @return Formatted string
	 */
	@JvmStatic fun getFormatted(input: Float): String = decimalFormat.format(input.toDouble())
	
	/**
	 * Determines the optimal number of download parts based on file size and user premium status.
	 * Premium users get more parts for better performance.
	 * @param totalFileLength The total size of the file to download
	 * @return Optimal number of download parts
	 */
	@JvmStatic
	fun getOptimalNumberOfDownloadParts(totalFileLength: Long): Int {
		val mb1 = 1000000
		val mb5 = (1000000 * 5)
		val mb10 = (1000000 * 10)
		val mb50 = (1000000 * 50)
		val mb100 = (1000000 * 100)
		val mb200 = (1000000 * 200)
		val mb400 = (1000000 * 400)
		
		return if (IS_PREMIUM_USER) {
			if (totalFileLength < mb1) 1
			else if (totalFileLength < mb5) 1
			else if (totalFileLength < mb10) 2
			else if (totalFileLength < mb50) 3
			else if (totalFileLength < mb100) 5
			else if (totalFileLength < mb200) 10
			else if (totalFileLength < mb400) 12
			else 18
		} else {
			if (totalFileLength < mb1) 1
			else if (totalFileLength < mb5) 2
			else if (totalFileLength < mb10) 2
			else if (totalFileLength < mb50) 3
			else if (totalFileLength < mb100) 3
			else if (totalFileLength < mb200) 4
			else if (totalFileLength < mb400) 5
			else 5
		}
	}
	
	/**
	 * Calculates the chunk size for each download part.
	 * @param isResumable Whether the download is resumable
	 * @param totalFileLength Total size of the file
	 * @param numberOfParts Number of parts to divide the download into
	 * @return Size of each download chunk
	 */
	@JvmStatic
	fun generateDownloadPartChunkSize(isResumable: Boolean,
		totalFileLength: Long, numberOfParts: Int): Long {
		return if (isResumable) totalFileLength / numberOfParts else totalFileLength
	}
	
	/**
	 * Extracts and formats the playback duration of an audio or video file if available.
	 * @param downloadDataModel The download model containing file information
	 * @return Formatted duration string or empty string if not available
	 */
	@JvmStatic
	fun getAudioPlaybackTimeIfAvailable(downloadDataModel: DownloadDataModel): String {
		val downloadedFile: DocumentFile = downloadDataModel.getDestinationDocumentFile()
		if (isAudio(downloadedFile) || isVideo(downloadedFile)) {
			try {
				if (!isWritableFile(downloadedFile)) return ""
				val mediaFileUri = downloadedFile.uri
				val retriever = MediaMetadataRetriever()
				retriever.setDataSource(INSTANCE, mediaFileUri)
				
				val extractCode = MediaMetadataRetriever.METADATA_KEY_DURATION
				val durationMs = retriever.extractMetadata(extractCode)?.toLongOrNull()
				retriever.release()
				
				val formattedDuration = formatVideoDuration(durationMs)
				return "($formattedDuration)"
			} catch (error: Exception) {
				error.printStackTrace()
				return ""
			}
		} else return ""
	}
	
	/**
	 * Calculates and formats the remaining download time.
	 * @param bytesRemaining Bytes left to download
	 * @param downloadSpeed Current download speed in bytes per second
	 * @return Formatted time string or "calculating" if speed is 0
	 */
	@JvmStatic
	fun getRemainingDownloadTimeInFormat(
		bytesRemaining: Long, downloadSpeed: Long
	): String {
		if (downloadSpeed <= 0) return INSTANCE.getString(R.string.calculating)
		val remainingMillis = (bytesRemaining * 1000) / downloadSpeed
		return calculateTime(remainingMillis.toFloat())
	}
	
	/**
	 * Calculates the remaining download time in milliseconds.
	 * @param bytesRemaining Bytes left to download
	 * @param downloadSpeed Current download speed in bytes per second
	 * @return Remaining time in milliseconds or 0 if speed is 0
	 */
	@JvmStatic
	fun getRemainingDownloadTime(
		bytesRemaining: Long, downloadSpeed: Long
	): Long {
		if (downloadSpeed <= 0) return 0
		val remainingMillis = (bytesRemaining * 1000) / downloadSpeed
		return remainingMillis
	}
	
	/**
	 * Converts bytes to a human-readable format (KB, MB, GB, etc.).
	 * @param downloadedByte Number of bytes
	 * @return Formatted string with appropriate unit
	 */
	@JvmStatic
	fun getHumanReadableFormat(
		downloadedByte: Long
	): String {
		if (downloadedByte < 1024) return "$downloadedByte B"
		val exp = (ln(downloadedByte.toDouble()) / ln(1024.0)).toInt()
		val pre = "KMGTPE"[exp - 1] + "B"
		return String.format(
			Locale.US, "%.1f %s", downloadedByte / 1024.0.pow(exp.toDouble()), pre
		)
	}
	
	/**
	 * Calculates and formats the current download speed.
	 * @param bytesDownloaded Bytes downloaded so far
	 * @param timeMillis Time taken in milliseconds
	 * @return Formatted speed string with appropriate unit
	 */
	@JvmStatic
	fun calculateDownloadSpeedInFormat(
		bytesDownloaded: Long, timeMillis: Long
	): String {
		if (timeMillis == 0L) return "0B/s"
		val speedBytesPerSecond = (bytesDownloaded * 1000).toDouble() / timeMillis
		return formatDownloadSpeedInSimpleForm(speedBytesPerSecond)
	}
	
	/**
	 * Calculates the current download speed in bytes per second.
	 * @param bytesDownloaded Bytes downloaded so far
	 * @param timeMillis Time taken in milliseconds
	 * @return Speed in bytes per second
	 */
	@JvmStatic
	fun calculateDownloadSpeed(bytesDownloaded: Long, timeMillis: Long): Long {
		if (timeMillis == 0L) return 0L
		return ((bytesDownloaded * 1000) / timeMillis)
	}
	
	/**
	 * Formats download speed with appropriate unit (B/s, KB/s, MB/s, etc.).
	 * @param speedBytesPerSecond Speed in bytes per second
	 * @return Formatted speed string
	 */
	@JvmStatic
	fun formatDownloadSpeedInSimpleForm(
		speedBytesPerSecond: Double
	): String {
		val oneKB: Long = 1024
		val oneMB = oneKB * 1024
		val oneGB = oneMB * 1024
		val oneTB = oneGB * 1024
		val df = decimalFormat
		return when {
			speedBytesPerSecond >= oneTB -> df.format(speedBytesPerSecond / oneTB) + "TB/s"
			speedBytesPerSecond >= oneGB -> df.format(speedBytesPerSecond / oneGB) + "GB/s"
			speedBytesPerSecond >= oneMB -> df.format(speedBytesPerSecond / oneMB) + "MB/s"
			speedBytesPerSecond >= oneKB -> df.format(speedBytesPerSecond / oneKB) + "KB/s"
			else -> df.format(speedBytesPerSecond) + "B/s"
		}
	}
	
	/**
	 * Generates a unique filename by appending a timestamp to prevent conflicts.
	 * @param baseFileName Original filename
	 * @return Unique filename with timestamp
	 */
	@JvmStatic
	fun generateUniqueDownloadFileName(baseFileName: String): String {
		val timestamp = System.currentTimeMillis()
		val extensionIndex = baseFileName.lastIndexOf('.')
		return if (extensionIndex != -1) {
			"${baseFileName.substring(0, extensionIndex)}_$timestamp${
				baseFileName.substring(extensionIndex)
			}"
		} else {
			"${baseFileName}_$timestamp"
		}
	}
	
	/**
	 * Updates the download directory path based on smart catalog settings.
	 * Creates subdirectories if smart catalog is enabled.
	 * @param downloadModel The download model to update
	 */
	@JvmStatic
	fun updateSmartCatalogDownloadDir(downloadModel: DownloadDataModel) {
		if (isSmartDownloadCatalogEnabled(downloadModel)) {
			val fileCategoryName = downloadModel.getUpdatedCategoryName()
			val aioRootDir = File(downloadModel.fileDirectory)
			val aioSubDir = fromFile(aioRootDir).createDirectory(fileCategoryName)
			aioSubDir?.canWrite().let { canWrite ->
				if (canWrite == true) {
					downloadModel.fileCategoryName = fileCategoryName
				}
			}
		} else downloadModel.fileCategoryName = ""
		
		val finalDirWithCategory = removeDuplicateSlashes(
			"${downloadModel.fileDirectory}/${
				if (downloadModel.fileCategoryName.isNotEmpty())
					downloadModel.fileCategoryName + "/" else ""
			}")
		
		finalDirWithCategory?.let { downloadModel.fileDirectory = it }
	}
	
	/**
	 * Renames the download file if a file with the same name already exists.
	 * Adds a numeric prefix to prevent conflicts.
	 * @param downloadModel The download model containing file information
	 */
	@JvmStatic
	fun renameIfDownloadFileExistsWithSameName(downloadModel: DownloadDataModel) {
		var index: Int
		val regex = Regex("^(\\d+)_")
		
		while (downloadModel.getDestinationDocumentFile().exists()) {
			val matchResult = regex.find(downloadModel.fileName)
			if (matchResult != null) {
				val currentIndex = matchResult.groupValues[1].toInt()
				downloadModel.fileName = downloadModel.fileName.replaceFirst(regex, "")
				index = currentIndex + 1
			} else index = 1
			downloadModel.fileName = "${index}_${downloadModel.fileName}"
		}
	}
	
	/**
	 * Validates and modifies a filename if it already exists in the directory.
	 * @param directory The target directory
	 * @param fileName Original filename
	 * @return Modified filename that doesn't conflict with existing files
	 */
	@JvmStatic
	fun validateExistedDownloadedFileName(directory: String, fileName: String): String {
		var index: Int
		val regex = Regex("^(\\d+)_")
		var modifiedName = fileName
		
		while (File(directory, modifiedName).exists()) {
			val matchResult = regex.find(modifiedName)
			if (matchResult != null) {
				val currentIndex = matchResult.groupValues[1].toInt()
				modifiedName = modifiedName.replaceFirst(regex, "")
				index = currentIndex + 1
			} else index = 1
			modifiedName = "${index}_${modifiedName}"
		}; return modifiedName
	}
	
	/**
	 * Checks if smart download catalog feature is enabled for the download.
	 * @param downloadModel The download model to check
	 * @return True if smart catalog is enabled, false otherwise
	 */
	@JvmStatic
	fun isSmartDownloadCatalogEnabled(downloadModel: DownloadDataModel): Boolean {
		return downloadModel.globalSettings.downloadAutoFolderCatalog
	}
	
	/**
	 * Converts a standard cookie string to Netscape HTTP Cookie File format.
	 * @param cookieString Standard cookie string
	 * @return Formatted Netscape cookie file content
	 */
	@JvmStatic
	fun generateNetscapeFormattedCookieString(cookieString: String): String {
		val cookies = cookieString.split(";").map { it.trim() }
		val domain = ""
		val path = "/"
		val secure = "FALSE"
		val expiry = "2147483647"
		
		val stringBuilder = StringBuilder()
		stringBuilder.append("# Netscape HTTP Cookie File\n")
		stringBuilder.append("# This file was generated by the app.\n\n")
		
		for (cookie in cookies) {
			val parts = cookie.split("=", limit = 2)
			if (parts.size == 2) {
				val name = parts[0].trim()
				val value = parts[1].trim()
				val pattern = "$domain\tFALSE\t$path\t$secure\t$expiry\t$name\t$value\n"
				stringBuilder.append(pattern)
			}
		}
		return stringBuilder.toString()
	}
	
	/**
	 * Extracts video resolution (width and height) from a video URL.
	 * @param videoUrl URL of the video
	 * @return Pair of width and height in pixels, or null if unavailable
	 */
	@JvmStatic
	fun getVideoResolutionFromUrl(videoUrl: String): Pair<Int, Int>? {
		val retriever = MediaMetadataRetriever()
		try {
			retriever.setDataSource(videoUrl, HashMap<String, String>())
			val width = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toInt()
			val height = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toInt()
			return if (width != null && height != null) Pair(width, height) else null
		} catch (error: Exception) {
			error.printStackTrace()
			return null
		} finally {
			retriever.release()
		}
	}
}