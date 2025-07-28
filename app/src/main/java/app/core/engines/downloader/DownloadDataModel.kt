package app.core.engines.downloader

import android.net.Uri
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import app.core.AIOApp
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioGSONInstance
import app.core.AIOApp.Companion.aioSettings
import app.core.engines.settings.AIOSettings
import app.core.engines.settings.AIOSettings.Companion.PRIVATE_FOLDER
import app.core.engines.settings.AIOSettings.Companion.SYSTEM_GALLERY
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoFormat
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoInfo
import com.aio.R.drawable
import com.aio.R.string
import com.anggrayudi.storage.file.getAbsolutePath
import com.google.gson.Gson
import lib.files.FileExtensions.ARCHIVE_EXTENSIONS
import lib.files.FileExtensions.DOCUMENT_EXTENSIONS
import lib.files.FileExtensions.IMAGE_EXTENSIONS
import lib.files.FileExtensions.MUSIC_EXTENSIONS
import lib.files.FileExtensions.PROGRAM_EXTENSIONS
import lib.files.FileExtensions.VIDEO_EXTENSIONS
import lib.files.FileSizeFormatter
import lib.files.FileUtility.endsWithExtension
import lib.files.FileUtility.isWritableFile
import lib.files.FileUtility.saveStringToInternalStorage
import lib.networks.DownloaderUtils.getHumanReadableFormat
import lib.process.CopyObjectUtils.deepCopy
import lib.process.ThreadsUtility
import lib.process.UniqueNumberUtils.getUniqueNumberForDownloadModels
import lib.texts.CommonTextUtils.getText
import lib.texts.CommonTextUtils.removeDuplicateSlashes
import java.io.File
import java.io.Serializable

/**
 * A comprehensive data model class representing a download item in the application.
 * This class holds all metadata and state information related to a download operation,
 * including progress tracking, status information, file details, and network parameters.
 *
 * The class implements Serializable to allow for persistence and transfer between components.
 */
class DownloadDataModel : Serializable {
	
	// Basic download identification and state tracking
	var id: Int = 0                           // Unique identifier for the download
	var status: Int = DownloadStatus.CLOSE    // Current status (see DownloadStatus constants)
	var isRunning: Boolean = false            // Whether download is actively running
	var isComplete: Boolean = false           // Whether download completed successfully
	var isDeleted: Boolean = false            // Whether download was deleted
	var isRemoved: Boolean = false            // Whether download was removed from UI
	
	// Error and special case flags
	var isWentToPrivateFolder: Boolean = false            // If file was saved to private storage
	var isFileUrlExpired: Boolean = false                 // If source URL expired
	var isYtdlpHavingProblem: Boolean = false             // If yt-dlp encountered issues
	var ytdlpProblemMsg: String = ""                      // Detailed yt-dlp error message
	var isDestinationFileNotExisted: Boolean = false      // If target file doesn't exist
	var isFileChecksumValidationFailed: Boolean = false   // If checksum verification failed
	
	// Network and operational flags
	var isWaitingForNetwork: Boolean = false          // Waiting for network availability
	var isFailedToAccessFile: Boolean = false         // Failed to access source file
	var isExpiredURLDialogShown: Boolean = false      // If URL expiry dialog was shown
	var isSmartCategoryDirProcessed: Boolean = false  // If file was categorized automatically
	
	// User communication and metadata
	var msgToShowUserViaDialog: String = ""           // Message to display to user
	var isDownloadFromBrowser: Boolean = false        // If initiated from browser
	var isBasicYtdlpModelInitialized: Boolean = false // If yt-dlp metadata initialized
	var additionalWebHeaders: Map<String, String>? = null // Custom HTTP headers
	
	// File information
	var fileName: String = ""             // Name of the file being downloaded
	var fileURL: String = ""              // Source URL of the download
	var siteReferrer: String = ""         // HTTP Referrer header value
	var fileDirectory: String = ""        // Target directory path
	
	// Metadata and technical details
	var fileMimeType: String = ""                 // MIME type of the file
	var fileContentDisposition: String = ""       // Content-Disposition header value
	var siteCookieString: String = ""             // Cookies for the download
	var thumbPath: String = ""                    // Local path to thumbnail
	var thumbnailUrl: String = ""                 // Remote URL of thumbnail
	
	// Temporary storage and processing info
	var tempYtdlpDestinationFilePath: String = "" // Temp path for yt-dlp processing
	var tempYtdlpStatusInfo: String = ""          // Temp status from yt-dlp
	var fileDirectoryURI: String = ""             // URI of target directory
	var fileCategoryName: String = ""             // Auto-categorized file type
	var startTimeDateInFormat: String = ""        // Formatted start timestamp
	var startTimeDate: Long = 0L                  // Start time in milliseconds
	
	// Timestamps
	var lastModifiedTimeDateInFormat: String = "" // Formatted modification time
	var lastModifiedTimeDate: Long = 0L           // Modification time in milliseconds
	var isUnknownFileSize: Boolean = false        // If file size couldn't be determined
	
	// Size information
	var fileSize: Long = 0L                       // Total file size in bytes
	var fileChecksum: String = "--"               // File checksum/hash
	var fileSizeInFormat: String = ""             // Human-readable file size
	
	// Speed metrics
	var averageSpeed: Long = 0L                   // Average download speed (bytes/sec)
	var maxSpeed: Long = 0L                       // Peak download speed
	var realtimeSpeed: Long = 0L                  // Current speed
	var averageSpeedInFormat: String = "--"       // Formatted average speed
	var maxSpeedInFormat: String = "--"           // Formatted max speed
	var realtimeSpeedInFormat: String = "--"      // Formatted current speed
	
	// Download capabilities
	var isResumeSupported: Boolean = false        // If download supports resuming
	var isMultiThreadSupported: Boolean = false   // If multi-threaded download supported
	
	// Progress tracking
	var totalConnectionRetries: Int = 0           // Number of retry attempts
	var progressPercentage: Long = 0L             // Completion percentage (0-100)
	var progressPercentageInFormat: String = ""   // Formatted percentage string
	
	// Byte-level tracking
	var downloadedByte: Long = 0L                 // Bytes downloaded so far
	var downloadedByteInFormat: String = "--"     // Formatted byte count
	var partStartingPoint: LongArray = LongArray(18)  // Start bytes for each chunk
	var partEndingPoint: LongArray = LongArray(18)    // End bytes for each chunk
	var partChunkSizes: LongArray = LongArray(18)     // Size of each chunk
	var partsDownloadedByte: LongArray = LongArray(18) // Bytes downloaded per chunk
	
	// Chunk progress
	var partProgressPercentage: IntArray = IntArray(18) // Completion % per chunk
	var timeSpentInMilliSec: Long = 0L            // Total time spent (ms)
	var remainingTimeInSec: Long = 0L             // Estimated time remaining (sec)
	var timeSpentInFormat: String = "--"          // Formatted time spent
	var remainingTimeInFormat: String = "--"      // Formatted remaining time
	var statusInfo: String = "--"                 // Current status message
	var videoInfo: VideoInfo? = null              // Video metadata (for media downloads)
	var videoFormat: VideoFormat? = null          // Video format details
	var executionCommand: String = ""             // Command used for download
	var mediaFilePlaybackDuration: String = ""    // Duration of media file
	
	// Application settings snapshot
	lateinit var globalSettings: AIOSettings      // Copy of app settings at download start
	
	companion object {
		// Constants for file naming and storage
		const val DOWNLOAD_MODEL_ID_KEY = "DOWNLOAD_MODEL_ID_KEY"
		const val DOWNLOAD_MODEL_FILE_EXTENSION = "_download.json"
		const val DOWNLOAD_MODEL_COOKIES_EXTENSION = "_cookies.txt"
		const val THUMB_EXTENSION = "_download.jpg"
		const val TEMP_EXTENSION = ".aio_download"
		
		/**
		 * Converts a JSON string back into a DownloadDataModel instance.
		 * @param jsonStringData The JSON representation of the download model
		 * @return Deserialized DownloadDataModel or null if conversion failed
		 */
		fun convertJSONStringToClass(jsonStringData: String): DownloadDataModel? {
			return try {
				aioGSONInstance.fromJson(jsonStringData, DownloadDataModel::class.java)
			} catch (error: Exception) {
				error.printStackTrace()
				null
			}
		}
	}
	
	init {
		resetToDefaultValues()
	}
	
	/**
	 * Persists the current state of the download model to storage.
	 * Performs cleanup before saving and handles cookies separately.
	 */
	@Synchronized
	fun updateInStorage() {
		ThreadsUtility.executeInBackground(codeBlock = {
			if (fileName.isEmpty() && fileURL.isEmpty()) return@executeInBackground
			saveCookiesIfAvailable()
			cleanTheModelBeforeSavingToStorage()
			saveStringToInternalStorage(
				fileName = "$id$DOWNLOAD_MODEL_FILE_EXTENSION",
				data = convertClassToJSON()
			)
		}, errorHandler = { error -> error.printStackTrace() })
	}
	
	/**
	 * Deletes all files associated with this download model from disk.
	 * Includes the model file, cookies, thumbnails, and temporary files.
	 */
	@Synchronized
	fun deleteModelFromDisk() {
		ThreadsUtility.executeInBackground(codeBlock = {
			val internalDir = AIOApp.internalDataFolder
			val modelFile = internalDir.findFile("$id$DOWNLOAD_MODEL_FILE_EXTENSION")
			val cookieFile = internalDir.findFile("$id$DOWNLOAD_MODEL_COOKIES_EXTENSION")
			val thumbFile = internalDir.findFile("$id$THUMB_EXTENSION")
			
			isWritableFile(modelFile).let { if (it) modelFile?.delete() }
			isWritableFile(thumbFile).let { if (it) thumbFile?.delete() }
			isWritableFile(cookieFile).let { if (it) cookieFile?.delete() }
			deleteAllTempDownloadedFiles(internalDir)
			
			if (globalSettings.defaultDownloadLocation == PRIVATE_FOLDER) {
				val downloadedFile = getDestinationDocumentFile()
				isWritableFile(downloadedFile).let { if (it) downloadedFile.delete() }
			}
		}, errorHandler = { error -> error.printStackTrace() })
	}
	
	/**
	 * Retrieves the path to the cookies file if available.
	 * @return Absolute path to cookies file or null if no cookies exist
	 */
	fun getCookieFilePathIfAvailable(): String? {
		if (siteCookieString.isEmpty()) return null
		val cookieFileName = "$id$DOWNLOAD_MODEL_COOKIES_EXTENSION"
		val internalDir = AIOApp.internalDataFolder
		val cookieFile = internalDir.findFile(cookieFileName)
		return if (cookieFile != null && cookieFile.exists())
			cookieFile.getAbsolutePath(INSTANCE)
		else null
	}
	
	/**
	 * Saves cookies to disk in Netscape format if they exist.
	 * @param shouldOverride Whether to overwrite existing cookie file
	 */
	fun saveCookiesIfAvailable(shouldOverride: Boolean = false) {
		if (siteCookieString.isEmpty()) return
		val cookieFileName = "$id$DOWNLOAD_MODEL_COOKIES_EXTENSION"
		val internalDir = AIOApp.internalDataFolder
		val cookieFile = internalDir.findFile(cookieFileName)
		if (!shouldOverride && cookieFile != null && cookieFile.exists()) return
		saveStringToInternalStorage(
			cookieFileName,
			generateNetscapeFormattedCookieString(siteCookieString)
		)
	}
	
	/**
	 * Converts cookie string to Netscape formatted file content.
	 * @param cookieString Raw cookie string from HTTP headers
	 * @return Formatted cookie file content
	 */
	private fun generateNetscapeFormattedCookieString(cookieString: String): String {
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
				stringBuilder.append("$domain\tFALSE\t$path\t$secure\t$expiry\t$name\t$value\n")
			}
		}
		return stringBuilder.toString()
	}
	
	/**
	 * Serializes the model to JSON string.
	 * @return JSON representation of the model
	 */
	fun convertClassToJSON(): String = Gson().toJson(this)
	
	/**
	 * Gets the temporary directory for partial downloads.
	 * @return File object representing temp directory
	 */
	fun getTempDestinationDir(): File = File("${fileDirectory}.temp/")
	
	/**
	 * Gets the destination file as a DocumentFile.
	 * @return DocumentFile representing the download target
	 */
	fun getDestinationDocumentFile(): DocumentFile {
		val destinationPath = removeDuplicateSlashes("$fileDirectory/$fileName")
		return DocumentFile.fromFile(File(destinationPath!!))
	}
	
	/**
	 * Gets the destination file as a regular File.
	 * @return File object representing the download target
	 */
	fun getDestinationFile(): File {
		val destinationPath = removeDuplicateSlashes("$fileDirectory/$fileName")
		return File(destinationPath!!)
	}
	
	/**
	 * Gets the temporary download file (in-progress download).
	 * @return File object for the temporary download file
	 */
	fun getTempDestinationFile(): File {
		val tempFilePath = "${getDestinationFile().absolutePath}${TEMP_EXTENSION}"
		return File(tempFilePath)
	}
	
	/**
	 * Gets the URI of the thumbnail image if available.
	 * @return Uri of thumbnail or null if not available
	 */
	fun getThumbnailURI(): Uri? {
		val thumbFilePath = "$id$THUMB_EXTENSION"
		return AIOApp.internalDataFolder.findFile(thumbFilePath)?.uri
	}
	
	/**
	 * Clears any cached thumbnail file and updates storage.
	 */
	fun clearCachedThumbnailFile() {
		try {
			getThumbnailURI()?.toFile()?.delete()
			thumbPath = ""
			updateInStorage()
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Gets the default thumbnail drawable resource ID.
	 * @return Resource ID of default thumbnail drawable
	 */
	fun getThumbnailDrawableID(): Int {
		return drawable.image_no_thumb_available
	}
	
	/**
	 * Generates a formatted string with download status information.
	 * @return Human-readable status string
	 */
	fun generateDownloadInfoInString(): String {
		if (videoFormat != null && videoInfo != null) {
			return if (status == DownloadStatus.CLOSE) {
				val waitingToJoin = getText(string.text_waiting_to_join).lowercase()
				val preparingToDownload = getText(string.title_preparing_download).lowercase()
				val downloadFailed = getText(string.title_download_io_failed).lowercase()
				if (statusInfo.lowercase().startsWith(waitingToJoin) ||
					statusInfo.lowercase().startsWith(preparingToDownload) ||
					statusInfo.lowercase().startsWith(downloadFailed)
				) {
					statusInfo
				} else normalDownloadStatusInfo()
			} else {
				val currentStatus = getText(string.title_started_downloading).lowercase()
				if (!statusInfo.lowercase().startsWith(currentStatus))
					normalDownloadStatusInfo() else tempYtdlpStatusInfo
			}
		} else return normalDownloadStatusInfo()
	}
	
	/**
	 * Determines the appropriate category name for the file.
	 * @param shouldRemoveAIOPrefix Whether to exclude "AIO" prefix from category name
	 * @return Localized category name string
	 */
	fun getUpdatedCategoryName(shouldRemoveAIOPrefix: Boolean = false): String {
		if (shouldRemoveAIOPrefix) {
			val categoryName = when {
				endsWithExtension(fileName, IMAGE_EXTENSIONS) -> getText(string.title_images)
				endsWithExtension(fileName, VIDEO_EXTENSIONS) -> getText(string.title_videos)
				endsWithExtension(fileName, MUSIC_EXTENSIONS) -> getText(string.title_sounds)
				endsWithExtension(fileName, DOCUMENT_EXTENSIONS) -> getText(string.title_documents)
				endsWithExtension(fileName, PROGRAM_EXTENSIONS) -> getText(string.title_programs)
				endsWithExtension(fileName, ARCHIVE_EXTENSIONS) -> getText(string.title_archives)
				else -> getText(string.title_aio_others)
			}; return categoryName
		} else {
			val categoryName = when {
				endsWithExtension(fileName, IMAGE_EXTENSIONS) -> getText(string.title_aio_images)
				endsWithExtension(fileName, VIDEO_EXTENSIONS) -> getText(string.title_aio_videos)
				endsWithExtension(fileName, MUSIC_EXTENSIONS) -> getText(string.title_aio_sounds)
				endsWithExtension(fileName, DOCUMENT_EXTENSIONS) -> getText(string.title_aio_documents)
				endsWithExtension(fileName, PROGRAM_EXTENSIONS) -> getText(string.title_aio_programs)
				endsWithExtension(fileName, ARCHIVE_EXTENSIONS) -> getText(string.title_aio_archives)
				else -> getText(string.title_aio_others)
			}; return categoryName
		}
	}
	
	/**
	 * Gets formatted file size string.
	 * @return Human-readable size string or "Unknown" if size not available
	 */
	fun getFormattedFileSize(): String {
		return if (fileSize <= 1 || isUnknownFileSize) getText(string.title_unknown_size)
		else FileSizeFormatter.humanReadableSizeOf(fileSize.toDouble())
	}
	
	/**
	 * Extracts file extension from filename.
	 * @return File extension (without dot) or empty string if no extension
	 */
	fun getFileExtension(): String {
		return fileName.substringAfterLast('.', "")
	}
	
	/**
	 * Deletes all temporary files associated with this download.
	 * @param internalDir The directory containing temporary files
	 */
	private fun deleteAllTempDownloadedFiles(internalDir: DocumentFile) {
		try {
			if (videoFormat != null && videoInfo != null) {
				if (tempYtdlpDestinationFilePath.isNotEmpty()) {
					val tempYtdlpFileName = File(tempYtdlpDestinationFilePath).name
					internalDir.listFiles().forEach { file ->
						try {
							file?.let {
								if (!file.isFile) return@let
								if (file.name!!.startsWith(tempYtdlpFileName))
									file.delete()
							}
						} catch (error: Exception) {
							error.printStackTrace()
						}
					}
				}
				
				if (videoInfo!!.videoCookieTempPath.isNotEmpty()) {
					val tempCookieFile = File(videoInfo!!.videoCookieTempPath)
					if (tempCookieFile.isFile && tempCookieFile.exists()) {
						tempCookieFile.delete()
					}
				}
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Generates standard download status information string.
	 * @return Formatted status string with progress, speed, and time remaining
	 */
	private fun normalDownloadStatusInfo(): String {
		if (videoFormat != null && videoInfo != null) {
			val textDownload = getText(string.text_downloaded)
			val infoString = "$statusInfo  |  $textDownload ($progressPercentage%)" +
					"  |  --/s  |  --:-- "
			return infoString
		} else {
			val totalFileSize = fileSizeInFormat
			val downloadSpeedInfo = if (status == DownloadStatus.CLOSE) "--/s"
			else realtimeSpeedInFormat
			
			val remainingTimeInfo = if (status == DownloadStatus.CLOSE ||
				isWaitingForNetwork
			) "--:--" else remainingTimeInFormat
			
			val downloadingStatus = getText(string.title_started_downloading).lowercase()
			return if (statusInfo.lowercase().startsWith(downloadingStatus)) {
				"$progressPercentageInFormat% Of $totalFileSize | " +
						"$downloadSpeedInfo | $remainingTimeInfo"
			} else {
				"$statusInfo | $totalFileSize | " +
						"$downloadSpeedInfo | $remainingTimeInfo"
			}
		}
	}
	
	/**
	 * Resets all fields to default values and initializes required properties.
	 * Sets up default directory based on app settings.
	 */
	private fun resetToDefaultValues() {
		id = getUniqueNumberForDownloadModels()
		if (aioSettings.defaultDownloadLocation == PRIVATE_FOLDER) {
			val externalDataFolderPath = INSTANCE.getExternalDataFolder()?.getAbsolutePath(INSTANCE)
			if (!externalDataFolderPath.isNullOrEmpty()) {
				fileDirectory = externalDataFolderPath
			} else {
				val internalDataFolderPath = INSTANCE.dataDir.absolutePath
				fileDirectory = internalDataFolderPath
			}
		} else if (aioSettings.defaultDownloadLocation == SYSTEM_GALLERY) {
			val externalDataFolderPath = getText(string.text_default_aio_download_folder_path)
			fileDirectory = externalDataFolderPath
		}
		
		globalSettings = deepCopy(aioSettings) ?: aioSettings
	}
	
	/**
	 * Cleans up model data before persisting to storage.
	 * Resets transient values and ensures completed downloads show 100% progress.
	 */
	private fun cleanTheModelBeforeSavingToStorage() {
		if (isRunning && status == DownloadStatus.DOWNLOADING) return
		realtimeSpeed = 0L
		realtimeSpeedInFormat = "--"
		if (isComplete && status == DownloadStatus.COMPLETE) {
			remainingTimeInSec = 0
			remainingTimeInFormat = "--:--"
			progressPercentage = 100L
			progressPercentageInFormat = getText(string.text_100_percentage)
			downloadedByte = fileSize
			downloadedByteInFormat = getHumanReadableFormat(downloadedByte)
			partProgressPercentage.forEachIndexed { index, _ ->
				partProgressPercentage[index] = 100
				partsDownloadedByte[index] = partChunkSizes[index]
			}
		}
	}
}