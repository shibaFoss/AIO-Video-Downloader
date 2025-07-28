package app.core.engines.video_parser.parsers

import app.core.AIOApp.Companion.INSTANCE
import com.aio.R
import lib.device.DateTimeUtils.calculateTime
import lib.process.LogHelperUtils
import lib.texts.CommonTextUtils.getText
import java.io.File
import java.util.Locale

/**
 * Utility class for processing video format information from yt-dlp output.
 *
 * Handles:
 * - Parsing yt-dlp format listings
 * - Formatting download progress information
 * - Calculating file sizes
 * - Managing video format metadata
 * - Processing cookies for authenticated downloads
 *
 * All operations are designed to handle yt-dlp's specific output format.
 */
object VideoFormatsUtils {
	
	// Logger instance for debugging
	private val logger: LogHelperUtils = LogHelperUtils.from(javaClass)
	
	/**
	 * Data class representing a video format with all its metadata.
	 *
	 * @property isFromSocialMedia Whether the format is from a social media platform
	 * @property formatId Unique identifier for the format
	 * @property formatExtension File extension (mp4, webm, etc.)
	 * @property formatResolution Video resolution (1080p, 720p, etc.)
	 * @property formatFileSize Size of the format file
	 * @property formatVcodec Video codec information
	 * @property formatAcodec Audio codec information
	 * @property formatTBR Total bitrate
	 * @property formatProtocol Protocol used (http, https, etc.)
	 * @property formatStreamingUrl Direct streaming URL if available
	 */
	data class VideoFormat(
		var isFromSocialMedia: Boolean = false,
		var formatId: String = "",
		var formatExtension: String = "",
		var formatResolution: String = "",
		var formatFileSize: String = "",
		var formatVcodec: String = "",
		var formatAcodec: String = "",
		var formatTBR: String = "",
		var formatProtocol: String = "",
		var formatStreamingUrl: String = ""
	)
	
	/**
	 * Data class representing complete video information.
	 *
	 * @property videoTitle Title of the video
	 * @property videoThumbnailUrl URL of the video thumbnail
	 * @property videoThumbnailByReferer Whether thumbnail requires referer header
	 * @property videoDescription Video description text
	 * @property videoUrlReferer Referer URL if needed
	 * @property videoUrl Original video URL
	 * @property videoFormats List of available formats
	 * @property videoCookie Cookie string for authenticated requests
	 * @property videoCookieTempPath Temporary file path for cookie storage
	 */
	data class VideoInfo(
		var videoTitle: String? = null,
		var videoThumbnailUrl: String? = null,
		var videoThumbnailByReferer: Boolean = false,
		var videoDescription: String? = null,
		var videoUrlReferer: String? = null,
		var videoUrl: String = "",
		var videoFormats: List<VideoFormat> = emptyList(),
		var videoCookie: String? = "",
		var videoCookieTempPath: String = ""
	)
	
	/**
	 * Parses yt-dlp format listing and returns structured format information.
	 *
	 * @param ytdlFormatsResponse Raw yt-dlp format listing output
	 * @return List of parsed VideoFormat objects
	 */
	fun getVideoFormatsList(ytdlFormatsResponse: String): List<VideoFormat> {
		val startTime = System.currentTimeMillis()
		val inputData = ytdlFormatsResponse.trimIndent()
		logger.d("YT-DLP Response\n${inputData}")
		
		// Extract and clean the formats table
		val formatTable = extractFormatsFromInfoLine(inputData)
		val cleanFormatTable = clearEmptyLines(formatTable)
		val lines = cleanFormatTable.split("\n").map { it.trim() }
			.filterIndexed { index, _ -> index != 1 }.toTypedArray()
		
		// Find longest line for column alignment
		var maxLength = 0
		var longestLineIndex = -1
		
		for (index in lines.indices) {
			if (lines[index].length > maxLength) {
				maxLength = lines[index].length
				longestLineIndex = index
			}
		}
		
		// Ensure consistent line lengths for column parsing
		if (longestLineIndex != -1) {
			lines[longestLineIndex] += " "
		}
		
		maxLength = lines.maxOfOrNull { it.length } ?: 0
		logger.d("YT-DLP formats list lines max length=$maxLength")
		
		// Pad all lines to max length
		for (index in lines.indices) {
			val line = lines[index]
			if (line.length != maxLength) {
				val needChars = maxLength - line.length
				val updatedLine = line + " ".repeat(needChars)
				lines[index] = updatedLine
			}
		}
		
		// Detect column boundaries
		val columnPositionMap: MutableList<Pair<Int, Int>> = mutableListOf()
		var startingIndex = 0
		
		var currentLoopingIndex = 0
		while (currentLoopingIndex < maxLength) {
			var whiteSpaceEncountered = 0
			for (line in lines)
				if (line[currentLoopingIndex].isWhitespace())
					whiteSpaceEncountered++
			
			if (whiteSpaceEncountered == lines.size) {
				columnPositionMap.add(Pair(startingIndex, currentLoopingIndex))
				logger.d(
					"YT-DLP formats column found starting" +
							"=$startingIndex ending=$currentLoopingIndex"
				)
				startingIndex = currentLoopingIndex + 1
			}
			
			currentLoopingIndex++
		}
		
		// Extract text from each column
		val extractedTexts: MutableList<String> = mutableListOf()
		for ((start, end) in columnPositionMap) {
			val extractedText = StringBuilder()
			for (line in lines) {
				if (start < line.length) {
					extractedText.append(
						line.substring(start, minOf(end + 1, line.length))
					).append("\n")
				}
			}
			logger.d("YT-DLP parsing on extracted formats text=\n${extractedText}")
			extractedTexts.add(extractedText.toString().trim())
		}
		
		logger.d("============================")
		val extractedVideoFormats = extractVideoFormats(extractedTexts)
		logger.d("Total yt-dlp video formats found=${extractedVideoFormats.size}")
		logger.d("============================")
		
		// Filter formats to keep highest quality per resolution
		val filteredVideoFormats: ArrayList<VideoFormat> = ArrayList()
		val formatsByResolution = extractedVideoFormats.groupBy { it.formatResolution }
		for ((resolution, formats) in formatsByResolution) {
			var highestTBRFormat: VideoFormat? = null
			var maxTBRValue = 0.0
			
			for (format in formats) {
				val numericTBR = format.formatTBR
					.replace(Regex("[^0-9.]"), "")
					.toDoubleOrNull() ?: 0.0
				
				if (numericTBR > maxTBRValue) {
					maxTBRValue = numericTBR
					highestTBRFormat = format
				}
			}
			
			highestTBRFormat?.let {
				filteredVideoFormats.add(it)
				logger.d(
					"Added format with Resolution" +
							"=$resolution and Highest TBR=$maxTBRValue"
				)
			}
		}
		
		// Fallback if no filtered formats found
		if (filteredVideoFormats.isEmpty() && extractedVideoFormats.isNotEmpty()) {
			filteredVideoFormats.addAll(extractedVideoFormats)
			filteredVideoFormats.removeAll { it.formatId.isEmpty() }
		}
		
		// Post-process format information
		for (format in filteredVideoFormats) {
			if (format.formatResolution.lowercase() == "unknown") {
				format.formatResolution = format.formatId
			}
			
			if (format.formatFileSize.isEmpty()) {
				format.formatFileSize = getText(R.string.title_not_available)
			}
		}
		
		val endTime = System.currentTimeMillis()
		val timeTaken = endTime - startTime
		logger.d(
			"Yt-dlp video formats text parsing time:" +
					" ${calculateTime(timeTaken.toFloat())}"
		)
		return filteredVideoFormats
	}
	
	/**
	 * Logs detailed video information for debugging purposes.
	 *
	 * @param videoInfo VideoInfo object containing all video metadata
	 */
	fun logVideoInfo(videoInfo: VideoInfo) {
		val logMessage = StringBuilder()
		logMessage.append("Video URL: ${videoInfo.videoUrl}\n")
		logMessage.append("Available Formats:\n")
		
		videoInfo.videoFormats.forEachIndexed { index, format ->
			logMessage.append("Format ${index + 1}:\n")
			logMessage.append("  Format ID: ${format.formatId}\n")
			logMessage.append("  Extension: ${format.formatExtension}\n")
			logMessage.append("  Resolution: ${format.formatResolution}\n")
			logMessage.append("  File Size: ${format.formatFileSize}\n")
			logMessage.append("  Video Codec: ${format.formatVcodec}\n")
			logMessage.append("  Audio Codec: ${format.formatAcodec}\n")
			logMessage.append("  Total Bitrate (tbr): ${format.formatTBR}\n")
			logMessage.append("------------------------\n")
		}
		logger.d(logMessage.toString())
	}
	
	/**
	 * Extracts the formats table from yt-dlp info output.
	 *
	 * @param input Raw yt-dlp output
	 * @return Extracted formats table as string
	 */
	private fun extractFormatsFromInfoLine(input: String): String {
		val lines = input.split("\n")
		val startIndex = lines.indexOfFirst { it.contains("[info] Available formats for") }
		val downloadIndex = lines.drop(startIndex + 1).indexOfFirst { it.contains("[download]") }
		return if (startIndex != -1) {
			if (downloadIndex != -1) {
				lines.subList(startIndex + 1, startIndex + 1 + downloadIndex).joinToString("\n")
			} else {
				lines.subList(startIndex + 1, lines.size).joinToString("\n")
			}
		} else {
			""
		}
	}
	
	/**
	 * Checks if a speed string is in valid format (e.g., "100K", "10M").
	 *
	 * @param speed Speed string to validate
	 * @return true if valid format, false otherwise
	 */
	fun isValidSpeedFormat(speed: String): Boolean {
		val regex = Regex("^\\d+([GMKB])$")
		return regex.matches(speed)
	}
	
	/**
	 * Formats download speed for yt-dlp command line argument.
	 *
	 * @param downloadMaxNetworkSpeed Speed in bytes
	 * @return Formatted speed string (e.g., "10M" for 10,000,000 bytes)
	 */
	fun formatDownloadSpeedForYtDlp(downloadMaxNetworkSpeed: Long): String {
		return when {
			downloadMaxNetworkSpeed >= 1_000_000_000 -> "${downloadMaxNetworkSpeed / 1_000_000_000}G"
			downloadMaxNetworkSpeed >= 1_000_000 -> "${downloadMaxNetworkSpeed / 1_000_000}M"
			downloadMaxNetworkSpeed >= 1_000 -> "${downloadMaxNetworkSpeed / 1_000}K"
			else -> "${downloadMaxNetworkSpeed}B"
		}
	}
	
	/**
	 * Parses size string (e.g., "10MiB") to bytes.
	 *
	 * @param size Size string to parse
	 * @return Size in bytes
	 */
	fun parseSize(size: String): Long {
		val regex = """(\d+(\.\d+)?)([KMGT]?i?B)""".toRegex()
		val matchResult = regex.matchEntire(size)
		
		if (matchResult != null) {
			val (value, _, unit) = matchResult.destructured
			val sizeInBytes = value.toDouble()
			
			return when (unit) {
				"B" -> (sizeInBytes).toLong()
				"KiB" -> (sizeInBytes * 1024).toLong()
				"MiB" -> (sizeInBytes * 1024 * 1024).toLong()
				"GiB" -> (sizeInBytes * 1024 * 1024 * 1024).toLong()
				"TiB" -> (sizeInBytes * 1024 * 1024 * 1024 * 1024).toLong()
				else -> 0L
			}
		}
		return 0L
	}
	
	/**
	 * Calculates total file size by combining video and audio sizes.
	 *
	 * @param videoSize Video size string
	 * @param audioSize Audio size string
	 * @return Combined size string (e.g., "15.23 MiB")
	 */
	fun calculateTotalFileSize(videoSize: String, audioSize: String): String {
		val videoSizeBytes = parseSize(videoSize)
		val audioSizeBytes = parseSize(audioSize)
		
		val totalSizeBytes = videoSizeBytes + audioSizeBytes
		
		return formatSize(totalSizeBytes)
	}
	
	/**
	 * Formats size in bytes to human-readable string.
	 *
	 * @param sizeInBytes Size in bytes
	 * @return Formatted size string
	 */
	private fun formatSize(sizeInBytes: Long): String {
		val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
		var size = sizeInBytes.toDouble()
		var unitIndex = 0
		
		while (size >= 1024 && unitIndex < units.size - 1) {
			size /= 1024
			unitIndex++
		}
		
		return String.format(Locale.US, "%.2f %s", size, units[unitIndex])
	}
	
	/**
	 * Checks if a video format has no audio track.
	 *
	 * @param videoFormat VideoFormat to check
	 * @return true if format has no audio, false otherwise
	 */
	fun isFormatHasNoAudio(videoFormat: VideoFormat) =
		videoFormat.formatAcodec.isEmpty() ||
				videoFormat.formatAcodec.lowercase().contains("video only")
	
	/**
	 * Cleans file size string by removing non-numeric prefixes.
	 *
	 * @param input Raw file size string
	 * @return Cleaned numeric portion
	 */
	fun cleanFileSize(input: String): String {
		return input.replace(Regex("^\\D+"), "")
	}
	
	/**
	 * Cleans and formats yt-dlp progress output for UI display.
	 *
	 * @param input Raw yt-dlp progress line
	 * @return Formatted progress information
	 */
	fun cleanYtdlpLoggingSting(input: String): String {
		try {
			val a1 = input.replace(Regex("\\[.*?]"), "")
				.replace("Unknown", "--:--").trim()
			val a2 = speedStringFromYtdlpLogs(a1)
			val a3 = processDeletionLine(a2)
			val a4 = processMergerLine(a3)
			val a5 = processDownloadLine(a4)
			val a6 = getSecondDownloadLineIfExists(a5)
			return formatDownloadLine(a6)
		} catch (error: Exception) {
			error.printStackTrace()
			return input
		}
	}
	
	/**
	 * Replaces invalid speed string values in yt-dlp logs with a default readable value.
	 * Example: "N/A/s" -> "0KiB/s"
	 */
	private fun speedStringFromYtdlpLogs(speed: String): String {
		return if (speed.contains("N/A/s")) speed.replace("N/A/s", "0KiB/s") else speed
	}
	
	/**
	 * Extracts the second "download" line from yt-dlp logs, if available.
	 * Useful for progress tracking across fragmented downloads.
	 */
	private fun getSecondDownloadLineIfExists(input: String): String {
		val lines = input.lines()
		val downloadLines = lines.filter { it.startsWith("[download]") }
		return if (downloadLines.size >= 2) downloadLines[1] else input
	}
	
	/**
	 * Processes lines related to downloading, particularly M3U8 information.
	 * Returns a localized string for specific download states.
	 */
	private fun processDownloadLine(input: String): String {
		val stage1 = if (input.contains("Downloading m3u8 information")) {
			getText(R.string.text_downloading_m3u8_information)
		} else input
		
		val stage2 = if (stage1.contains("Downloading")) {
			stage1.substring(stage1.indexOf("Downloading"))
		} else stage1
		
		return stage2
	}
	
	/**
	 * Converts merge-related log lines to a localized message.
	 */
	private fun processMergerLine(line: String): String {
		return if (line.contains("Merging formats into"))
			getText(R.string.text_merging_video_and_audio_format) else line
	}
	
	/**
	 * Converts file deletion-related log lines to a localized message.
	 */
	private fun processDeletionLine(line: String): String {
		return if (line.contains("Deleting original file"))
			getText(R.string.text_finishing_up_the_download) else line
	}
	
	/**
	 * Formats download log line with detailed status including progress, total size, speed and ETA.
	 * Tries the main format first, then fallback to Stage2 if not matched.
	 */
	private fun formatDownloadLine(input: String): String {
		val regex = Regex("""(\d+\.\d+%) of ~?\s+([\d.]+[KMGT]iB) at\s+([\d.]+[KMGT]iB/s) ETA\s+([\d:]+) \(frag (\d+/\d+)\)""")
		val matchResult = regex.find(input)
		
		return if (matchResult != null) {
			val (progress, totalSize, speed, eta, _) = matchResult.destructured
			"$progress Of $totalSize | $speed | $eta Left"
		} else {
			formatDownloadLineStage2(input)
		}
	}
	
	/**
	 * Fallback formatter for download logs if main formatter fails.
	 */
	private fun formatDownloadLineStage2(input: String): String {
		val regex = Regex("""(\d+\.\d+%) of ~?\s+([\d.]+[KMGT]iB) at\s+([\d.]+[KMGT]iB/s) ETA\s+([:\-\w]+) \(frag (\d+)/(\d+)\)""")
		val matchResult = regex.find(input)
		
		return if (matchResult != null) {
			val (progress, totalSize, speed, eta, _, _) = matchResult.destructured
			"$progress Of $totalSize | $speed | $eta Left"
		} else {
			formatDownloadStatus(input)
		}
	}
	
	/**
	 * Fallback for formatting download progress lines with a simpler format.
	 */
	private fun formatDownloadStatus(input: String): String {
		val regex = Regex("""(\d+\.\d+%) of\s+([\d.]+[KMGT]iB) at\s+([\d.]+[KMGT]iB/s) ETA\s+([\d:]+)""")
		val matchResult = regex.find(input)
		return if (matchResult != null) {
			val (progress, totalSize, speed, eta) = matchResult.destructured
			"$progress Of $totalSize | $speed | $eta Left"
		} else {
			formatSessionLine(input)
		}
	}
	
	/**
	 * Detects and replaces session initialization message with localized string.
	 */
	private fun formatSessionLine(input: String): String {
		val targetPhrase = getText(R.string.text_setting_up_session)
		return if (input.contains(targetPhrase)) {
			targetPhrase
		} else {
			formatDestinationLine(input)
		}
	}
	
	/**
	 * Detects and replaces destination message with localized string.
	 */
	private fun formatDestinationLine(input: String): String {
		return if (input.startsWith("Destination:")) {
			getText(R.string.text_setting_destination_files)
		} else {
			formatDownloadingLine(input)
		}
	}
	
	/**
	 * Detects format-check messages and returns localized text for user interface.
	 */
	private fun formatDownloadingLine(input: String): String {
		return if (input.startsWith("Downloading") && input.contains("format(s):")) {
			getText(R.string.text_checking_formats_to_download)
		} else {
			formatProgressLine(input)
		}
	}
	
	/**
	 * Formats generic progress lines showing percentage, size, duration, and speed.
	 */
	private fun formatProgressLine(input: String): String {
		val regex = """(\d+%)\s+of\s+([\d.]+[KMGT]?iB)\s+in\s+([\d:]+)\s+at\s+([\d.]+[KMGT]?iB/s)""".toRegex()
		val matchResult = regex.find(input)
		
		return if (matchResult != null) {
			val (percentage, size, time, speed) = matchResult.destructured
			"$percentage Of $size  |  $time  |  $speed"
		} else {
			formatDownloadedPartMessage(input)
		}
	}
	
	/**
	 * Detects already downloaded part message and replaces with localized message.
	 */
	private fun formatDownloadedPartMessage(input: String): String {
		val regex = """.*/.*\.part-Frag\d+ has already been downloaded""".toRegex()
		
		return if (regex.matches(input)) {
			getText(R.string.text_validating_already_downloaded_part)
		} else {
			formatDownloadProgress(input)
		}
	}
	
	/**
	 * Parses and formats full download progress line including fragment info.
	 */
	private fun formatDownloadProgress(input: String): String {
		val regex = """(\d+\.\d+%) of ~?\s+([\d.]+[A-Z]iB) at\s+([\d.]+[A-Z]?B/s) ETA ([\d:]+|--:--) \(frag (\d+)/(\d+)\)""".toRegex()
		val matchResult = regex.find(input)
		
		return if (matchResult != null) {
			val (percentage, size, speed, eta, _, _) = matchResult.destructured
			"$percentage Of $size  |  $speed  |  $eta Left  "
		} else {
			modifyRetryingMessage(input)
		}
	}
	
	/**
	 * Detects retrying log messages and formats them with localized retry count.
	 */
	private fun modifyRetryingMessage(input: String): String {
		return if (input.contains("Retrying", ignoreCase = true)) {
			val regex = Regex("Retrying \\((\\d+)/(\\d+)\\)")
			val matchResult = regex.find(input)
			matchResult?.let {
				INSTANCE.getString(
					R.string.text_connection_failed_retrying,
					it.groups[1]?.value, it.groups[2]?.value
				)
			} ?: input
		} else {
			modifyExtractingMessage(input)
		}
	}
	
	/**
	 * Detects and converts URL extraction message to a localized version.
	 */
	private fun modifyExtractingMessage(input: String): String {
		return if (input.contains("Extracting URL", ignoreCase = true)) {
			getText(R.string.text_extracting_source_url)
		} else {
			checkM3u8LiveStatus(input)
		}
	}
	
	/**
	 * Converts M3U8 live check logs to localized string.
	 */
	private fun checkM3u8LiveStatus(input: String): String {
		return if (input.contains("Checking m3u8 live status", ignoreCase = true)) {
			getText(R.string.text_checking_m3u8_live_status)
		} else {
			fixingMpegTsMp4(input)
		}
	}
	
	/**
	 * Converts MPEG-TS to MP4 container fix log to a user-readable message.
	 */
	private fun fixingMpegTsMp4(input: String): String {
		return if (input.contains("Fixing MPEG-TS in MP4 container", ignoreCase = true)) {
			getText(R.string.text_fixing_mpeg_ts_in_mp4_container)
		} else {
			input
		}
	}
	
	/**
	 * Creates a temporary file containing cookie information.
	 *
	 * @param tempFileName The name of the temporary file to be created.
	 * @param tempFilePath The directory path where the temporary file should be stored.
	 * @param cookiesString The content (cookie data) to be written to the file.
	 * @return The created temporary [File] object.
	 */
	fun createCookieTempFile(
		tempFileName: String,
		tempFilePath: String,
		cookiesString: String
	): File {
		val tempFile = File(tempFilePath, tempFileName)
		tempFile.writeText(cookiesString)
		return tempFile
	}
	
	/**
	 * Removes all empty or blank lines from a given multi-line string.
	 *
	 * @param input The original string potentially containing empty lines.
	 * @return A string with all non-blank lines joined with newline characters.
	 */
	private fun clearEmptyLines(input: String): String {
		return input.lines()
			.filter { it.isNotBlank() }
			.joinToString("\n")
	}
	
	/**
	 * Extracts video format information from a list of column strings.
	 * Each column represents a specific metadata type such as ID, EXT, RESOLUTION, etc.
	 *
	 * @param columns A mutable list of strings where each string contains newline-separated values for a column.
	 * @return A list of [VideoFormat] objects, each representing a row in the tabular data.
	 */
	private fun extractVideoFormats(columns: MutableList<String>): List<VideoFormat> {
		// Get the total number of rows based on the first column.
		val totalRows = columns[0].split("\n").map { it.trim() }
		
		val videoFormats: ArrayList<VideoFormat> = ArrayList()
		
		// Extract relevant data from each column.
		val idColumn = getColumnData("ID", columns, totalRows.size)
		val extColumn = getColumnData("EXT", columns, totalRows.size)
		val resolutionColumn = getColumnData("RESOLUTION", columns, totalRows.size)
		val fileSizeColumn = getColumnData("FILESIZE", columns, totalRows.size)
		val tbrColumn = getColumnData("TBR", columns, totalRows.size)
		val protocolColumn = getColumnData("PROTO", columns, totalRows.size)
		val vcodecColumn = getColumnData("VCODEC", columns, totalRows.size)
		val acodecColumn = getColumnData("ACODEC", columns, totalRows.size)
		
		// Iterate through rows and create VideoFormat objects.
		for (index in totalRows.indices) {
			val formatId = idColumn.getOrNull(index) ?: ""
			val formatExtension = extColumn.getOrNull(index) ?: ""
			val formatResolution = resolutionColumn.getOrNull(index) ?: ""
			val formatFileSize = fileSizeColumn.getOrNull(index) ?: ""
			val formatTBR = tbrColumn.getOrNull(index) ?: ""
			val formatProtocol = protocolColumn.getOrNull(index) ?: ""
			val formatVcodec = vcodecColumn.getOrNull(index) ?: ""
			val formatAcodec = acodecColumn.getOrNull(index) ?: ""
			
			videoFormats.add(
				VideoFormat(
					formatId = formatId,
					formatExtension = formatExtension,
					formatResolution = formatResolution,
					formatFileSize = formatFileSize,
					formatTBR = formatTBR,
					formatVcodec = formatVcodec,
					formatAcodec = formatAcodec,
					formatProtocol = formatProtocol
				)
			)
		}
		
		return videoFormats
	}
	
	/**
	 * Retrieves column data by its header name.
	 *
	 * @param header The header of the column (e.g., "ID", "EXT").
	 * @param extractedTexts The list of all column strings where each entry starts with a header followed by newline-separated values.
	 * @param rowCount The expected number of rows for alignment.
	 * @return A list of values under the given header, padded with empty strings if fewer values are found.
	 */
	private fun getColumnData(
		header: String,
		extractedTexts: MutableList<String>,
		rowCount: Int
	): List<String> {
		val headerIndex = extractedTexts.indexOfFirst { it.startsWith(header) }
		
		return if (headerIndex != -1) {
			val columnData = extractedTexts[headerIndex]
				.split("\n")
				.drop(1) // Remove header line
				.map { it.trim() }
			
			// Pad with empty strings if rows are missing
			if (columnData.size < rowCount) {
				columnData + List(rowCount - columnData.size) { "" }
			} else {
				columnData
			}
		} else {
			// Return a list of empty strings if column is not found
			List(rowCount) { "" }
		}
	}
}