package app.ui.main.fragments.browser.webengine

import app.core.AIOApp.Companion.INSTANCE
import app.core.engines.video_parser.parsers.SupportedURLs
import app.ui.main.fragments.browser.webengine.M3U8InfoExtractor.InfoCallback
import com.aio.R
import lib.networks.DownloaderUtils.getVideoResolutionFromUrl
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.ThreadsUtility

/**
 * A helper class that maintains a list of detected video URLs from a WebView and enriches
 * their metadata such as resolution and format information. Handles both direct video files
 * (e.g., MP4) and adaptive streams (e.g., M3U8).
 */
class WebVideosLibrary {
	
	/**
	 * The unique identifier for the WebView instance from which these video URLs were extracted.
	 * This is useful when multiple WebViews are active and you need to map URLs to their source.
	 */
	var webViewId: Int = 0
	
	/**
	 * A list of available video URLs detected from the current page or session.
	 * Each entry contains video metadata such as resolution, type, and format information.
	 */
	val listOfAvailableVideoUrlInfos: ArrayList<VideoUrlInfo> = ArrayList()
	
	/**
	 * Adds a new [VideoUrlInfo] to the list if it's not already present.
	 * Automatically detects whether the video is an M3U8 stream or a direct video file
	 * and attempts to fetch resolution and metadata asynchronously.
	 *
	 * @param videoUrlInfo The video URL information to add and process.
	 */
	fun addVideoUrlInfo(videoUrlInfo: VideoUrlInfo) {
		if (!listOfAvailableVideoUrlInfos.contains(videoUrlInfo)) {
			listOfAvailableVideoUrlInfos.add(videoUrlInfo)
			
			ThreadsUtility.executeInBackground(codeBlock = {
				if (SupportedURLs.isM3U8Url(videoUrlInfo.fileUrl)) {
					// Handle M3U8 stream resolution detection
					M3U8InfoExtractor().extractResolutions(videoUrlInfo.fileUrl, object : InfoCallback {
						override fun onResolutions(resolutions: List<String>) {
							val infoText: String
							if (resolutions.size > 1) {
								val stringResId = R.string.type_video_type_m3u8_available_resolutions
								infoText = INSTANCE.getString(stringResId, "${resolutions.size}")
							} else {
								val stringResId = R.string.text_video_type_m3u8_resolution
								infoText = INSTANCE.getString(stringResId, resolutions[0])
							}
							
							videoUrlInfo.infoCached = infoText
							videoUrlInfo.fileResolution = resolutions[0]
							videoUrlInfo.totalResolutions = resolutions.size
							videoUrlInfo.isM3U8 = true
						}
						
						override fun onError(errorMessage: String) {
							// If error occurred during resolution detection, still mark as M3U8
							videoUrlInfo.isM3U8 = true
						}
					})
				} else {
					// Handle direct video file resolution detection
					getVideoResolutionFromUrl(videoUrlInfo.fileUrl)?.let { resolution ->
						executeOnMainThread {
							videoUrlInfo.totalResolutions = 1
							videoUrlInfo.fileResolution = "${resolution.second}p"
							val stringResId = R.string.text_video_type_mp4_resolution
							val infoText = INSTANCE.getString(stringResId, videoUrlInfo.fileResolution)
							videoUrlInfo.infoCached = infoText
							videoUrlInfo.isM3U8 = false
						}
					}
				}
			})
		}
	}
	
	/**
	 * Clears all detected video URLs from the internal list.
	 * Useful when navigating to a new webpage or resetting the session.
	 */
	fun clearVideoUrls() {
		listOfAvailableVideoUrlInfos.clear()
	}
	
	/**
	 * Returns the current list of collected [VideoUrlInfo] objects.
	 *
	 * @return A list of video URL metadata objects.
	 */
	fun getVideoUrls(): List<VideoUrlInfo> {
		return listOfAvailableVideoUrlInfos
	}
}
