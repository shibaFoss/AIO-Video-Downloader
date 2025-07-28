package app.ui.others.media_player.dialogs

import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.webkit.MimeTypeMap
import android.webkit.MimeTypeMap.getSingleton
import androidx.documentfile.provider.DocumentFile.fromFile
import app.core.AIOApp.Companion.INSTANCE
import app.core.engines.downloader.DownloadDataModel
import app.core.engines.downloader.DownloadStatus.CLOSE
import app.core.engines.downloader.DownloadStatus.COMPLETE
import app.core.engines.downloader.DownloadStatus.DOWNLOADING
import com.aio.R
import lib.files.FileUtility.getFileExtension
import lib.files.FileUtility.isVideo
import lib.texts.CommonTextUtils.getText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale.getDefault

/**
 * Object responsible for building HTML-formatted media information strings
 * for display in the app's UI. Extracts metadata from media files and formats
 * it into a readable HTML structure.
 */
object MediaInfoHtmlBuilder {
	
	/**
	 * Builds an HTML-formatted string containing media file information.
	 *
	 * @param dataModel The DownloadDataModel containing information about the media file
	 * @return HTML-formatted string with media information, or error message if extraction fails
	 */
	fun buildMediaInfoHtmlString(dataModel: DownloadDataModel): String {
		val mediaFile = dataModel.getDestinationFile()
		
		return try {
			// Use MediaMetadataRetriever to extract metadata from the file
			MediaMetadataRetriever().use { retriever ->
				retriever.setDataSource(mediaFile.path)
				buildHtmlInfo(retriever, dataModel, mediaFile)
			}
		} catch (error: Exception) {
			error.printStackTrace()
			INSTANCE.getString(R.string.text_failed_to_load_media_info)
		}
	}
	
	/**
	 * Constructs the HTML information string from extracted metadata.
	 *
	 * @param mediaMetaDataRetriever The MediaMetadataRetriever instance with loaded data
	 * @param downloadDataModel The DownloadDataModel containing download information
	 * @param mediaFile The actual media file
	 * @return Formatted HTML string with all media information
	 */
	private fun buildHtmlInfo(
		mediaMetaDataRetriever: MediaMetadataRetriever,
		downloadDataModel: DownloadDataModel,
		mediaFile: File
	): String {
		// Basic file information
		val fileName = downloadDataModel.fileName
		val fileSize = downloadDataModel.fileSizeInFormat
		val fileDirectory = downloadDataModel.fileDirectory
		
		// File format and type
		val format = getFileExtension(fileName)
		val isVideo = isVideo(fromFile(mediaFile))
		
		// Duration information
		val duration = mediaMetaDataRetriever
			.extractMetadata(METADATA_KEY_DURATION)?.toLongOrNull()
		val durationFormatted = duration?.let { formatDuration(it) }
		
		// Media-specific metadata
		val width = mediaMetaDataRetriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)
		val height = mediaMetaDataRetriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)
		val videoCodec = mediaMetaDataRetriever.extractMetadata(METADATA_KEY_MIMETYPE)
		val bitRate = mediaMetaDataRetriever.extractMetadata(METADATA_KEY_BITRATE)
		val audioCodec = mediaMetaDataRetriever.extractMetadata(METADATA_KEY_MIMETYPE)
		
		// Format download date
		val downloadDateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", getDefault())
			.format(downloadDataModel.startTimeDate)
		
		// Build the HTML string using StringBuilder
		return StringBuilder().apply {
			val mediaFileType = if (isVideo) getText(R.string.title_videos)
			else getText(R.string.title_sounds)
			
			// Append basic file information
			append(INSTANCE.getString(R.string.text_file_name_b_br, fileName))
			append(INSTANCE.getString(R.string.text_file_directory_b, fileDirectory))
			append(INSTANCE.getString(R.string.text_file_size_b_br, fileSize))
			append(INSTANCE.getString(R.string.text_file_type_b_br, mediaFileType))
			
			append("------------------------<br>")
			
			// Append format and duration information
			append(INSTANCE.getString(R.string.text_file_format_b_br, format))
			append(INSTANCE.getString(R.string.text_duration_b_br, durationFormatted))
			
			// Append download status information
			val downloadStatusText = getDownloadStatusText(downloadDataModel.status)
			append(INSTANCE.getString(R.string.text_download_status_b_br, downloadStatusText))
			append(INSTANCE.getString(R.string.text_downloaded_date_b_br, downloadDateFormatted))
			append("------------------------<br>")
			
			// Append video-specific information if file is a video
			if (isVideo) {
				if (width == null && height == null) {
					val resolution = INSTANCE.getString(R.string.text_resolution_b_x_br, "-- ", " --")
					append(resolution).append(getVideoCodec(videoCodec))
				} else {
					val resolution = INSTANCE.getString(R.string.text_resolution_b_x_br, width, height)
					append(resolution).append(getVideoCodec(videoCodec))
				}
			}
			
			// Append audio and bitrate information
			append(INSTANCE.getString(R.string.text_bitrate_b_bps_br, bitRate))
			append(INSTANCE.getString(R.string.text_audio_codec_b_br, audioCodec))
		}.toString()
	}
	
	/**
	 * Formats the video codec information for display.
	 *
	 * @param videoCodec The raw video codec string
	 * @return Formatted string with video codec information
	 */
	private fun getVideoCodec(videoCodec: String?): String {
		return INSTANCE.getString(R.string.text_video_codec_b_br, videoCodec)
	}
	
	/**
	 * Formats duration from milliseconds to a human-readable format.
	 *
	 * @param durationMs Duration in milliseconds
	 * @return Formatted duration string (e.g. "5 min 30 sec")
	 */
	private fun formatDuration(durationMs: Long): String {
		val seconds = (durationMs / 1000) % 60
		val minutes = (durationMs / 1000) / 60
		return "$minutes min $seconds sec"
	}
	
	/**
	 * Gets the MIME type of a file.
	 *
	 * @param file The file to check
	 * @return The MIME type string, or null if not found
	 */
	private fun getMimeType(file: File): String? {
		return try {
			val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)
			getSingleton().getMimeTypeFromExtension(extension)
		} catch (error: Exception) {
			error.printStackTrace()
			null
		}
	}
	
	/**
	 * Converts download status code to a human-readable string.
	 *
	 * @param status The download status code
	 * @return Corresponding status text
	 */
	private fun getDownloadStatusText(status: Int): String {
		return when (status) {
			CLOSE -> getText(R.string.text_closed)
			DOWNLOADING -> getText(R.string.title_in_progress)
			COMPLETE -> getText(R.string.title_completed)
			else -> getText(R.string.title_unknown)
		}
	}
}