package app.core.engines.video_parser.parsers

import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioSettings
import app.core.AIOApp.Companion.internalDataFolder
import app.core.AIOApp.Companion.ytdlpInstance
import app.core.engines.video_parser.parsers.SupportedURLs.filterYoutubeUrlWithoutPlaylist
import app.core.engines.video_parser.parsers.SupportedURLs.isYouTubeUrl
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoFormat
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoInfo
import com.aio.R
import com.anggrayudi.storage.file.getAbsolutePath
import com.yausername.youtubedl_android.YoutubeDL.version
import com.yausername.youtubedl_android.YoutubeDLRequest
import lib.device.DateTimeUtils.calculateTime
import lib.files.FileSystemUtility.sanitizeFileNameExtreme
import lib.files.FileSystemUtility.sanitizeFileNameNormal
import lib.files.FileSystemUtility.saveStringToInternalStorage
import lib.networks.DownloaderUtils.generateNetscapeFormattedCookieString
import lib.networks.URLUtilityKT.getBaseDomain
import lib.networks.URLUtilityKT.getWebpageTitleOrDescription
import lib.process.UniqueNumberUtils.getUniqueNumberForDownloadModels
import lib.texts.CommonTextUtils.cutTo60Chars
import lib.texts.CommonTextUtils.getText
import lib.texts.CommonTextUtils.removeEmptyLines
import java.io.File

/**
 * Utility object providing functions for extracting video information and formats
 * using yt-dlp via the youtubedl-android integration.
 */
object VideoParserUtility {
	/**
	 * Attempts to fetch a list of available video formats using yt-dlp with optional retry mechanism.
	 *
	 * @param videoURL The URL of the video to analyze.
	 * @param videoCookie Optional cookie string for handling authenticated requests.
	 * @param maxRetries The number of times to retry if extraction fails.
	 * @return List of [VideoFormat] extracted from the video.
	 */
	fun getYtdlpVideoFormatsListWithRetry(videoURL: String,
		videoCookie: String? = null,
		maxRetries: Int = 1
	): List<VideoFormat> {
		var attempts = 0
		var videoFormats: List<VideoFormat>
		do {
			videoFormats = getYtdlpVideoFormatsList(videoURL, videoCookie)
			attempts++
		} while (videoFormats.isEmpty() && attempts < maxRetries)
		return videoFormats
	}
	
	/**
	 * Attempts to fetch a direct video stream URL for a specific format ID using yt-dlp with retry mechanism.
	 *
	 * @param videoFormatId Format ID to fetch.
	 * @param videoURL The original video URL.
	 * @param videoCookie Optional cookie string for session-based access.
	 * @param maxRetries Maximum number of retry attempts.
	 * @return Direct video URL for the given format ID or null if unsuccessful.
	 */
	fun getYtdlpVideoFormatsUrlWithRetry(
		videoFormatId: String,
		videoURL: String,
		videoCookie: String? = null,
		maxRetries: Int = 1
	): String? {
		var attempts = 0
		var videoFormatUrl: String?
		do {
			videoFormatUrl = getYtdlpVideoFormatUrl(
				videoFormatId = videoFormatId,
				videoURL = videoURL,
				videoCookie = videoCookie
			); attempts++
		} while (videoFormatUrl.isNullOrEmpty() && attempts < maxRetries)
		return videoFormatUrl
	}
	
	/**
	 * Executes yt-dlp command to extract direct URL for a specific video format.
	 */
	private fun getYtdlpVideoFormatUrl(
		videoFormatId: String,
		videoURL: String,
		videoCookie: String? = null
	): String? {
		try {
			println("Retrieving video format's ($videoFormatId) url : $videoURL " +
					"YT-DLP Version: ${version(INSTANCE)}")
			
			var filteredUrl = videoURL
			if (isYouTubeUrl(videoURL)) {
				filteredUrl = filterYoutubeUrlWithoutPlaylist(videoURL)
			}
			
			val startTime = System.currentTimeMillis()
			val cookieTempFile = File(
				internalDataFolder.getAbsolutePath(INSTANCE),
				"${getUniqueNumberForDownloadModels()}.txt"
			)
			
			if (!videoCookie.isNullOrEmpty()) {
				val cookieString = generateNetscapeFormattedCookieString(videoCookie)
				saveStringToInternalStorage(cookieTempFile.name, cookieString)
			}
			
			val ytdlpRequest = YoutubeDLRequest(filteredUrl)
			ytdlpRequest.addOption("-f", videoFormatId)
			ytdlpRequest.addOption("--get-url")
			ytdlpRequest.addOption("--no-check-certificate")
			ytdlpRequest.addOption("--no-cache-dir")
			ytdlpRequest.addOption("--skip-download")
			ytdlpRequest.addOption("--playlist-items", "1")
			ytdlpRequest.addOption("--user-agent", aioSettings.downloadHttpUserAgent)
			if (cookieTempFile.exists() && cookieTempFile.canWrite()) {
				ytdlpRequest.addOption("--cookies", cookieTempFile.absolutePath)
			}
			
			val ytdlpResponse = ytdlpInstance.execute(ytdlpRequest).out
			println("Extracted Format URL =$ytdlpResponse")
			
			if (cookieTempFile.exists()) cookieTempFile.delete()
			val endTime = System.currentTimeMillis()
			val timeTaken = endTime - startTime
			
			println("Yt-dlp execution time: ${calculateTime(timeTaken.toFloat())}")
			return ytdlpResponse
			
		} catch (error: Exception) {
			error.printStackTrace()
			return null
		}
	}
	
	/**
	 * Executes yt-dlp to retrieve the list of available video formats for the given URL.
	 */
	private fun getYtdlpVideoFormatsList(
		videoURL: String,
		videoCookie: String? = null
	): List<VideoFormat> {
		try {
			println("Retrieving video formats list : $videoURL YT-DLP Version: ${version(INSTANCE)}")
			var filteredUrl = videoURL
			if (isYouTubeUrl(videoURL)) {
				filteredUrl = filterYoutubeUrlWithoutPlaylist(videoURL)
			}
			
			val startTime = System.currentTimeMillis()
			val cookieTempFile = File(
				internalDataFolder.getAbsolutePath(INSTANCE),
				"${getUniqueNumberForDownloadModels()}.txt"
			)
			if (!videoCookie.isNullOrEmpty()) {
				val cookieString = generateNetscapeFormattedCookieString(videoCookie)
				saveStringToInternalStorage(cookieTempFile.name, cookieString)
			}
			
			val ytdlpRequest = YoutubeDLRequest(filteredUrl)
			ytdlpRequest.addOption("-F")
			ytdlpRequest.addOption("--no-check-certificate")
			ytdlpRequest.addOption("--no-cache-dir")
			ytdlpRequest.addOption("--skip-download")
			ytdlpRequest.addOption("--user-agent", aioSettings.downloadHttpUserAgent)
			if (cookieTempFile.exists() && cookieTempFile.canWrite()) {
				ytdlpRequest.addOption("--cookies", cookieTempFile.absolutePath)
			}
			ytdlpRequest.addOption("--playlist-items", "1")
			val ytdlpResponse = ytdlpInstance.execute(ytdlpRequest).out
			val videoFormats = VideoFormatsUtils.getVideoFormatsList(ytdlpResponse)
			if (cookieTempFile.exists()) cookieTempFile.delete()
			
			val endTime = System.currentTimeMillis()
			val timeTaken = endTime - startTime
			println("Yt-dlp execution time: ${calculateTime(timeTaken.toFloat())}")
			
			return videoFormats
		} catch (error: Exception) {
			error.printStackTrace()
			return emptyList()
		}
	}
	
	/**
	 * Returns a sanitized and trimmed title for display or filename usage.
	 *
	 * @param videoInfo The [VideoInfo] object containing the original title and URL.
	 * @param videoFormat The selected [VideoFormat] for fallback title generation.
	 * @param useExtremeNameSanitizer If true, applies more aggressive file name cleaning.
	 * @return A [CharSequence] representing the cleaned and trimmed title.
	 */
	fun getSanitizedTitle(
		videoInfo: VideoInfo,
		videoFormat: VideoFormat,
		useExtremeNameSanitizer: Boolean = false
	): CharSequence {
		if (videoInfo.videoTitle.isNullOrEmpty()) {
			val madeUpTitle = "${videoFormat.formatId}_" +
					"${videoFormat.formatResolution}_" +
					"${videoFormat.formatVcodec}_" +
					"${getBaseDomain(videoInfo.videoUrl)}"
			videoInfo.videoTitle = madeUpTitle
			
			return if (videoInfo.videoTitle.isNullOrEmpty()) {
				getText(R.string.text_unknown_video_title)
			} else videoInfo.videoTitle!!
			
		} else {
			val sanitizedName = if (useExtremeNameSanitizer) {
				sanitizeFileNameExtreme(videoInfo.videoTitle!!)
			} else sanitizeFileNameNormal(videoInfo.videoTitle!!)
			
			val removedDoubleSlashes = removeEmptyLines(sanitizedName)
			val finalVideoTitle = cutTo60Chars(removedDoubleSlashes ?: "N/A") ?: "N/A"
			return finalVideoTitle
		}
	}
	
	/**
	 * Attempts to extract the web page title or description from a URL.
	 *
	 * @param videoUrl URL of the video or page.
	 * @return Best-effort title string based on web page metadata.
	 */
	fun getVideoTitleFromURL(videoUrl: String): String {
		val title = getWebpageTitleOrDescription(videoUrl) { result -> result.toString() }
		return title.toString()
	}
}