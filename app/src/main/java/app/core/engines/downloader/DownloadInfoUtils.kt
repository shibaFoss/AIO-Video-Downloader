package app.core.engines.downloader

import app.core.AIOApp
import com.aio.R
import lib.files.FileSizeFormatter.humanReadableSizeOf
import lib.networks.DownloaderUtils.formatDownloadSpeedInSimpleForm
import lib.texts.CommonTextUtils.getText

/**
 * Utility class for generating HTML-formatted download information strings.
 * Provides detailed information about download status, progress, and configuration.
 */
object DownloadInfoUtils {
	
	/**
	 * Builds an HTML-formatted string containing detailed information about a download.
	 *
	 * @param ddm The DownloadDataModel containing all download information
	 * @return HTML-formatted string with download details
	 */
	fun buildDownloadInfoHtmlString(ddm: DownloadDataModel): String {
		val context = AIOApp.INSTANCE
		val stringBuilder = StringBuilder()
		
		// Check if this is a video download with special video info
		if (ddm.videoInfo != null && ddm.videoFormat != null) {
			stringBuilder.append("<html><body>")
				.append(context.getString(R.string.b_download_id_b_br, "${ddm.id}"))
				.append(context.getString(R.string.b_file_name_b_br, ddm.fileName))
				.append(context.getString(R.string.b_progress_percentage_b_br, "${ddm.progressPercentage}%"))
			
			// Show temporary ytdlp status if download isn't complete
			if (ddm.status != DownloadStatus.COMPLETE) {
				stringBuilder.append(context.getString(R.string.b_download_stream_info_b_br, ddm.tempYtdlpStatusInfo))
			} else {
				// Show file size info when complete
				stringBuilder.append(context.getString(R.string.b_file_size_b_br, ddm.downloadedByteInFormat))
					.append(context.getString(R.string.b_downloaded_bytes_b_bytes_br, "${ddm.downloadedByte}"))
					.append(context.getString(R.string.b_downloaded_bytes_in_format_b_br, ddm.downloadedByteInFormat))
			}
			
			stringBuilder
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_file_category_b_br, ddm.fileCategoryName))
				.append(context.getString(R.string.b_file_directory_b_br, ddm.fileDirectory))
				.append(context.getString(R.string.b_file_url_b_br, buildUrlTag(ddm.fileURL)))
				.append(context.getString(R.string.b_download_webpage_b_br, buildUrlTag(ddm.siteReferrer)))
				
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_download_status_info_b_br, ddm.statusInfo))
				.append(context.getString(R.string.b_download_started_b_br, ddm.startTimeDateInFormat))
				.append(context.getString(R.string.b_download_last_modified_b_br, ddm.lastModifiedTimeDateInFormat))
				.append(context.getString(R.string.b_time_spent_b_br, ddm.timeSpentInFormat))
				
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_is_file_url_expired_b_br, "${ddm.isFileUrlExpired}"))
				.append(context.getString(R.string.b_is_failed_to_access_file_b_br, "${ddm.isFailedToAccessFile}"))
				.append(context.getString(R.string.b_is_waiting_for_network_b_br, "${ddm.isWaitingForNetwork}"))
				
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_checksum_validation_b_br, ifChecksumVerified(ddm)))
				.append(context.getString(R.string.b_multi_thread_support_b_br, isMultithreadingSupported(ddm)))
				.append(context.getString(R.string.b_resume_support_b_br, isResumeSupported(ddm)))
				.append(context.getString(R.string.b_unknown_file_size_b_br, isUnknownFile(ddm)))
				.append(context.getString(R.string.b_connection_retry_counts_b_times_br, "${ddm.totalConnectionRetries}"))
				
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_default_parallel_connections_b_br, "${defaultParallelConnection(ddm)}"))
				.append(context.getString(R.string.b_default_thread_connections_b_br, "${defaultNumOfThreadsAssigned(ddm)}"))
				.append(context.getString(R.string.b_buffer_size_b_br, getBufferSize(ddm)))
				.append(context.getString(R.string.b_http_proxy_b_br, getHttpProxy(ddm)))
				.append(context.getString(R.string.b_download_speed_limiter_b_br, formatNetworkSpeedLimit(ddm)))
				.append(context.getString(R.string.b_user_agent_b_br, ddm.globalSettings.downloadHttpUserAgent))
		} else {
			// Standard file download information
			stringBuilder.append("<html><body>")
				.append(context.getString(R.string.b_download_id_b_br, "${ddm.id}"))
				.append(context.getString(R.string.b_file_name_b_br, ddm.fileName))
				.append(context.getString(R.string.b_file_size_b_br, ddm.fileSizeInFormat))
				.append(context.getString(R.string.b_downloaded_bytes_b_bytes_br, "${ddm.downloadedByte}"))
				.append(context.getString(R.string.b_downloaded_bytes_in_format_b_br, ddm.downloadedByteInFormat))
				.append(context.getString(R.string.b_progress_percentage_b_br, "${ddm.progressPercentage}%"))
				
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_file_category_b_br, ddm.fileCategoryName))
				.append(context.getString(R.string.b_file_directory_b_br, ddm.fileDirectory))
				.append(context.getString(R.string.b_file_url_b_br, buildUrlTag(ddm.fileURL)))
				.append(context.getString(R.string.b_download_webpage_b_br, buildUrlTag(ddm.siteReferrer)))
				
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_download_status_info_b_br, ddm.statusInfo))
				.append(context.getString(R.string.b_download_started_b_br, ddm.startTimeDateInFormat))
				.append(context.getString(R.string.b_download_last_modified_b_br, ddm.lastModifiedTimeDateInFormat))
				.append(context.getString(R.string.b_time_spent_b_br, ddm.timeSpentInFormat))
				.append(context.getString(R.string.b_remaining_time_b_br, ddm.remainingTimeInFormat))
				
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_is_file_url_expired_b_br, "${ddm.isFileUrlExpired}"))
				.append(context.getString(R.string.b_is_failed_to_access_file_b_br, "${ddm.isFailedToAccessFile}"))
				.append(context.getString(R.string.b_is_waiting_for_network_b_br, "${ddm.isWaitingForNetwork}"))
				
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_realtime_network_speed_b_br, getRealtimeNetworkSpeed(ddm)))
				.append(context.getString(R.string.b_average_network_speed_b_br, ddm.averageSpeedInFormat))
				.append(context.getString(R.string.b_max_network_speed_b_br, ddm.maxSpeedInFormat))
				
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_checksum_validation_b_br, ifChecksumVerified(ddm)))
				.append(context.getString(R.string.b_multi_thread_support_b_br, isMultithreadingSupported(ddm)))
				.append(context.getString(R.string.b_resume_support_b_br, isResumeSupported(ddm)))
				.append(context.getString(R.string.b_unknown_file_size_b_br, isUnknownFile(ddm)))
				.append(context.getString(R.string.b_connection_retry_counts_b_times_br, "${ddm.totalConnectionRetries}"))
				
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_default_parallel_connections_b_br, "${defaultParallelConnection(ddm)}"))
				.append(context.getString(R.string.b_default_thread_connections_b_br, "${defaultNumOfThreadsAssigned(ddm)}"))
				.append(context.getString(R.string.b_buffer_size_b_br, getBufferSize(ddm)))
				.append(context.getString(R.string.b_http_proxy_b_br, getHttpProxy(ddm)))
				.append(context.getString(R.string.b_download_speed_limiter_b_br, formatNetworkSpeedLimit(ddm)))
				.append(context.getString(R.string.b_user_agent_b_br, ddm.globalSettings.downloadHttpUserAgent))
				.append(context.getString(R.string.b_est_part_chunk_size_b_br, estPartChunkSize(ddm)))
				
				.append("<br>---------------------------------<br>")
				.append(context.getString(R.string.b_part_progress_percentages_b_br, getDownloadPartPercentage(ddm)))
				.append(context.getString(R.string.b_parts_downloaded_bytes_b_br, getPartDownloadedByte(ddm)))
				
				.append("</body></html>")
		}; return stringBuilder.toString()
	}
	
	/**
	 * Estimates the size of each download chunk/part for multi-threaded downloads.
	 *
	 * @param downloadModel The download model containing file size information
	 * @return Human-readable string of estimated chunk size
	 */
	private fun estPartChunkSize(downloadModel: DownloadDataModel): String {
		return humanReadableSizeOf(
			downloadModel.fileSize /
					downloadModel.globalSettings.downloadDefaultThreadConnections
		)
	}
	
	/**
	 * Formats the network speed limit setting.
	 *
	 * @param downloadModel The download model containing speed limit settings
	 * @return Formatted speed limit string
	 */
	private fun formatNetworkSpeedLimit(downloadModel: DownloadDataModel): String {
		return formatDownloadSpeedInSimpleForm(
			downloadModel.globalSettings.downloadMaxNetworkSpeed.toDouble()
		)
	}
	
	/**
	 * Gets the downloaded bytes for each part in a multi-threaded download.
	 *
	 * @param downloadModel The download model containing part information
	 * @return HTML-formatted string showing bytes downloaded per part
	 */
	private fun getPartDownloadedByte(downloadModel: DownloadDataModel): String {
		val sb = StringBuilder()
		sb.append("<br>")
		downloadModel.partsDownloadedByte.forEachIndexed { index, downloadedByte ->
			sb.append("[${index} = ${humanReadableSizeOf(downloadedByte)}]<br>")
		}
		return sb.toString()
	}
	
	/**
	 * Gets the progress percentage for each part in a multi-threaded download.
	 *
	 * @param downloadModel The download model containing part information
	 * @return HTML-formatted string showing progress percentage per part
	 */
	private fun getDownloadPartPercentage(downloadModel: DownloadDataModel): String {
		val sb = StringBuilder()
		sb.append("<br>")
		downloadModel.partProgressPercentage.forEachIndexed { index, percent ->
			sb.append("[${index} = ${percent}%]<br>")
		}
		return sb.toString()
	}
	
	/**
	 * Gets the configured HTTP proxy information.
	 *
	 * @param downloadModel The download model containing proxy settings
	 * @return Proxy server string or "not configured" message
	 */
	private fun getHttpProxy(downloadModel: DownloadDataModel): String {
		return downloadModel.globalSettings.downloadHttpProxyServer.ifEmpty {
			getText(R.string.text_not_configured)
		}
	}
	
	/**
	 * Gets the configured buffer size in human-readable format.
	 *
	 * @param downloadModel The download model containing buffer settings
	 * @return Formatted buffer size string
	 */
	private fun getBufferSize(downloadModel: DownloadDataModel): String {
		return humanReadableSizeOf(
			downloadModel.globalSettings.downloadBufferSize.toDouble()
		)
	}
	
	/**
	 * Gets the default number of threads configured for downloads.
	 *
	 * @param downloadModel The download model containing thread settings
	 * @return Number of threads configured
	 */
	private fun defaultNumOfThreadsAssigned(downloadModel: DownloadDataModel): Int {
		return downloadModel.globalSettings.downloadDefaultThreadConnections
	}
	
	/**
	 * Gets the default number of parallel connections configured.
	 *
	 * @param downloadModel The download model containing connection settings
	 * @return Number of parallel connections configured
	 */
	private fun defaultParallelConnection(downloadModel: DownloadDataModel): Int {
		return downloadModel.globalSettings.downloadDefaultParallelConnections
	}
	
	/**
	 * Checks if the download has unknown file size.
	 *
	 * @param downloadModel The download model to check
	 * @return "Yes" or "No" string indicating unknown file size status
	 */
	private fun isUnknownFile(downloadModel: DownloadDataModel): String {
		return if (downloadModel.isUnknownFileSize)
			getText(R.string.title_yes) else getText(R.string.text_no)
	}
	
	/**
	 * Checks if the download supports resume functionality.
	 *
	 * @param downloadModel The download model to check
	 * @return "Yes" or "No" string indicating resume support
	 */
	private fun isResumeSupported(downloadModel: DownloadDataModel): String {
		return if (downloadModel.isResumeSupported)
			getText(R.string.title_yes) else getText(R.string.text_no)
	}
	
	/**
	 * Checks if the download supports multi-threading.
	 *
	 * @param downloadModel The download model to check
	 * @return "Yes" or "No" string indicating multi-thread support
	 */
	private fun isMultithreadingSupported(downloadModel: DownloadDataModel): String {
		return if (downloadModel.isMultiThreadSupported)
			getText(R.string.title_yes) else getText(R.string.text_no)
	}
	
	/**
	 * Checks if checksum verification is enabled for the download.
	 *
	 * @param downloadModel The download model to check
	 * @return String indicating whether checksum verification is performed
	 */
	private fun ifChecksumVerified(downloadModel: DownloadDataModel): String {
		return if (downloadModel.globalSettings.downloadVerifyChecksum)
			getText(R.string.title_performed) else getText(R.string.title_not_performed)
	}
	
	/**
	 * Gets the real-time network speed of an active download.
	 *
	 * @param downloadModel The download model to check
	 * @return Formatted speed string or "--" if download isn't running
	 */
	private fun getRealtimeNetworkSpeed(downloadModel: DownloadDataModel): String {
		return if (!downloadModel.isRunning) "--" else downloadModel.realtimeSpeedInFormat
	}
	
	/**
	 * Creates an HTML anchor tag for a URL.
	 *
	 * @param url The URL to link to
	 * @return HTML anchor tag string
	 */
	private fun buildUrlTag(url: String): String {
		return "<a href=\"$url\">${getText(R.string.text_click_here_to_open_link)}</a>"
	}
}