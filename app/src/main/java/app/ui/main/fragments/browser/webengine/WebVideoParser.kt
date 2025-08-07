package app.ui.main.fragments.browser.webengine

import android.view.View
import app.core.AIOApp.Companion.IS_ULTIMATE_VERSION_UNLOCKED
import app.core.AIOApp.Companion.aioAdblocker
import app.core.AIOApp.Companion.aioSettings
import app.core.engines.video_parser.parsers.SupportedURLs
import app.core.engines.video_parser.parsers.SupportedURLs.isSocialMediaUrl
import app.core.engines.video_parser.parsers.SupportedURLs.isYouTubeUrl
import app.core.engines.video_parser.parsers.SupportedURLs.isYtdlpSupportedUrl
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoInfo
import app.core.engines.video_parser.parsers.VideoThumbGrabber.startParsingVideoThumbUrl
import app.ui.main.fragments.downloads.intercepter.SharedVideoURLIntercept
import com.aio.R
import com.airbnb.lottie.LottieAnimationView
import lib.networks.URLUtilityKT.isHostOnly
import lib.process.AsyncJobUtils
import lib.process.ThreadsUtility
import lib.texts.CommonTextUtils.getText

/**
 * WebVideoParser is responsible for detecting and handling downloadable video links
 * from webpages opened in the browser WebViewEngine.
 */
object WebVideoParser {
	
	/**
	 * Analyzes the current webpage URL to detect downloadable videos.
	 *
	 * @param webpageUrl The URL of the webpage to analyze.
	 * @param webviewEngine The active WebViewEngine instance hosting the page.
	 */
	fun analyzeUrl(webpageUrl: String? = null, webviewEngine: WebViewEngine) {
		if (webpageUrl == null) return
		
		val browserFragment = webviewEngine.browserFragment
		val browserFragmentBody = browserFragment.browserFragmentBody
		val videoGrabberButton = browserFragmentBody.videoGrabberButton
		
		// Handle YouTube case
		if (!IS_ULTIMATE_VERSION_UNLOCKED && isYouTubeUrl(webpageUrl)) {
			videoGrabberButton.visibility = View.GONE
			val msgYtNotSupported = getText(R.string.text_youtube_download_not_supported)
			webviewEngine.showQuickBrowserInfo(msgYtNotSupported)
			return
		} else {
			if (aioSettings.browserEnableVideoGrabber) {
				videoGrabberButton.visibility = View.VISIBLE
			}
		}
		
		// Handle YTDLP supported URLs
		if (isYtdlpSupportedUrl(webpageUrl) && !isHostOnly(webpageUrl)) {
			ThreadsUtility.executeInBackground(codeBlock = {
				val thumbnailUrl = startParsingVideoThumbUrl(webpageUrl)
				if (!thumbnailUrl.isNullOrEmpty()) {
					ThreadsUtility.executeOnMain {
						videoGrabberButton.setAnimation(R.raw.animation_videos_found)
						
						videoGrabberButton.setOnClickListener {
							if (isSocialMediaUrl(webpageUrl)) {
								SingleResolutionPrompter(
									baseActivity = browserFragment.safeMotherActivityRef,
									singleResolutionName = "HD Quality",
									extractedVideoLink = webpageUrl,
									currentWebUrl = webpageUrl,
									videoCookie = webviewEngine.getCurrentWebViewCookies(),
									videoTitle = webviewEngine.currentWebView?.title,
									videoUrlReferer = webpageUrl,
									isSocialMediaUrl = true,
									isDownloadFromBrowser = true
								).show()
							} else {
								val userGivenVideoInfo = VideoInfo(
									videoUrlReferer = webpageUrl,
									videoThumbnailUrl = thumbnailUrl,
									videoCookie = webviewEngine.getCurrentWebViewCookies()
								)
								val baseActivity = webviewEngine.safeMotherActivityRef
								val videoParser = SharedVideoURLIntercept(baseActivity, userGivenVideoInfo)
								videoParser.interceptIntentURI(webpageUrl, false)
							}
						}
					}
				}
			})
		} else {
			val currentWebVideosLibrary = webviewEngine.listOfWebVideosLibrary
				.find { it.webViewId == webviewEngine.currentWebView?.id }
			
			if (currentWebVideosLibrary != null) {
				val listOfAvailableVideoUrls = currentWebVideosLibrary.listOfAvailableVideoUrlInfos
				if (listOfAvailableVideoUrls.isEmpty()) {
					assignNoVideoLinkFoundDialog(videoGrabberButton, webviewEngine)
				} else videoFoundIndicate(webviewEngine, listOfAvailableVideoUrls)
			} else {
				assignNoVideoLinkFoundDialog(videoGrabberButton, webviewEngine)
			}
		}
	}
	
	/**
	 * Indicates that videos were found on the webpage and sets up the grabber button.
	 *
	 * @param webviewEngine The active WebViewEngine.
	 * @param listOfVideoUrlInfos The list of video URLs found.
	 */
	private fun videoFoundIndicate(webviewEngine: WebViewEngine, listOfVideoUrlInfos: ArrayList<VideoUrlInfo>) {
		val browserFragment = webviewEngine.browserFragment
		val browserFragmentBody = browserFragment.browserFragmentBody
		val videoGrabberButton = browserFragmentBody.videoGrabberButton
		videoGrabberButton.setAnimation(R.raw.animation_videos_found)
		
		videoGrabberButton.setOnClickListener {
			// Retain only M3U8 URLs if present
			if (listOfVideoUrlInfos.any { SupportedURLs.isM3U8Url(it.fileUrl) }) {
				listOfVideoUrlInfos.removeAll { !SupportedURLs.isM3U8Url(it.fileUrl) }
			}
			
			// Filter out ad URLs
			val adHosts = aioAdblocker.getAdBlockHosts().toSet()
			listOfVideoUrlInfos.removeIf { videoUrlInfo ->
				adHosts.any { adHost -> videoUrlInfo.fileUrl.contains(adHost, ignoreCase = true) }
			}
			
			ExtractedLinksDialog(webviewEngine, listOfVideoUrlInfos).show()
		}
	}
	
	/**
	 * Resets the video grabbing state and clears all discovered video links for the current WebView.
	 *
	 * @param webviewEngine The WebViewEngine instance to reset.
	 */
	fun resetVideoGrabbingButton(webviewEngine: WebViewEngine) {
		AsyncJobUtils.executeOnMainThread {
			val browserFragment = webviewEngine.browserFragment
			val browserFragmentBody = browserFragment.browserFragmentBody
			val videoGrabberButton = browserFragmentBody.videoGrabberButton
			
			assignNoVideoLinkFoundDialog(videoGrabberButton, webviewEngine)
			webviewEngine.listOfWebVideosLibrary
				.find { it.webViewId == webviewEngine.currentWebView?.id }
				?.clearVideoUrls()
		}
	}
	
	/**
	 * Assigns the "no video found" animation and action to the grabber button.
	 *
	 * @param videoGrabberButton The grabber button instance.
	 * @param webviewEngine The associated WebViewEngine.
	 */
	private fun assignNoVideoLinkFoundDialog(
		videoGrabberButton: LottieAnimationView?,
		webviewEngine: WebViewEngine?
	) {
		videoGrabberButton?.setAnimation(R.raw.animation_circle_loading)
		videoGrabberButton?.setOnClickListener { webviewEngine?.let { showHowToInfoDialog(it) } }
	}
	
	/**
	 * Displays an informational dialog explaining how to find videos.
	 *
	 * @param webviewEngine The WebViewEngine requesting the dialog.
	 */
	private fun showHowToInfoDialog(webviewEngine: WebViewEngine) {
		NoVideoFoundDialog(webviewEngine.safeMotherActivityRef, webviewEngine).show()
	}
}