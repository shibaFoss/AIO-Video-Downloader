package app.ui.main.fragments.browser.webengine

import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import app.core.AIOApp.Companion.aioSettings
import app.core.engines.downloader.DownloadURLHelper.getFileInfoFromSever
import app.core.engines.video_parser.parsers.SupportedURLs.isM3U8Url
import app.ui.main.fragments.browser.webengine.WebVideoParser.resetVideoGrabbingButton
import com.bumptech.glide.Glide
import lib.files.FileExtensions.ALL_DOWNLOADABLE_FORMATS
import lib.files.FileExtensions.ONLINE_VIDEO_FORMATS
import lib.files.FileUtility
import lib.files.FileUtility.decodeURLFileName
import lib.networks.URLUtility.isValidURL
import lib.process.ThreadsUtility
import java.io.ByteArrayInputStream
import java.net.URL

/**
 * Custom WebViewClient implementation that handles:
 * - Download detection for supported file formats
 * - URL interception and special case handling
 * - Favicon updates
 * - Video URL collection for in-page videos
 *
 * @property webviewEngine The parent WebViewEngine instance
 */
class BrowserWebClient(val webviewEngine: WebViewEngine) : WebViewClient() {
	
	// Weak reference to parent activity to prevent leaks
	val safeMotherActivityRef = webviewEngine.safeMotherActivityRef
	
	// Tracks last detected download URL to prevent duplicate dialogs
	var lastTimeDownloadLink: String = ""
	
	/**
	 * Called when page starts loading
	 * @param webView The WebView that initiated the callback
	 * @param url The URL being loaded
	 * @param favicon The favicon for this page if available
	 */
	override fun onPageStarted(webView: WebView?, url: String?, favicon: Bitmap?) {
		super.onPageStarted(webView, url, favicon)
		resetLastTimeDownloadLink()
		updateBrowserFavicon(webView)
		resetVideoGrabbingButton(webviewEngine)
	}
	
	/**
	 * Called when page finishes loading
	 * @param webView The WebView that initiated the callback
	 * @param url The URL of the page
	 */
	override fun onPageFinished(webView: WebView?, url: String?) {
		super.onPageFinished(webView, url)
		updateBrowserFavicon(webView)
		analyzeDownloadableLink(url)
	}
	
	/**
	 * Intercepts URL loading requests
	 * @return true if the host app wants to handle the URL, false to let WebView handle it
	 */
	override fun shouldOverrideUrlLoading(
		view: WebView?,
		request: WebResourceRequest?
	): Boolean {
		val url = request?.url?.toString() ?: return true
		if (isInvalidScheme(url)) return true
		if (isSpecialCaseUrl(url)) return handleSpecialUrl(url)
		analyzeDownloadableLink(url)
		return false
	}
	
	/**
	 * Intercepts resource requests to detect video URLs
	 * @return WebResourceResponse to return, or null to let WebView handle normally
	 */
	override fun shouldInterceptRequest(
		view: WebView?,
		request: WebResourceRequest?
	): WebResourceResponse? {
		val url = request?.url.toString()
		if (url.isNotEmpty()) {
			ONLINE_VIDEO_FORMATS.find { fileFormat ->
				url.endsWith(".$fileFormat") ||
						url.contains(".$fileFormat?") ||
						url.contains(".$fileFormat&")
			}?.let {
				webviewEngine.currentWebView?.let { currentWebView ->
					val webViewLists = webviewEngine
						.getListOfWebViewOnTheSystem().filter { it.id == currentWebView.id }
					if (webViewLists.isNotEmpty()) {
						val webViewId = webViewLists[0].id
						webviewEngine.listOfWebVideosLibrary
							.find { it.webViewId == webViewId }?.addVideoUrlInfo(
								VideoUrlInfo(
									fileUrl = url,
									fileResolution = "",
									isM3U8 = isM3U8Url(url),
									totalResolutions = 0
								)
							)
					}
				}
			}
		}
		
		return super.shouldInterceptRequest(view, request)
	}
	
	/**
	 * Analyzes URL for downloadable content and shows prompt if found
	 * @param url The URL to analyze
	 */
	private fun analyzeDownloadableLink(url: String?) {
		url?.let {
			if (lastTimeDownloadLink == it) return
			lastTimeDownloadLink = it
			triggerDownloadManually(it)
		}
	}
	
	/**
	 * Resets the last detected download URL
	 */
	private fun resetLastTimeDownloadLink() {
		lastTimeDownloadLink = ""
	}
	
	/**
	 * Checks URL for downloadable content and shows download dialog
	 * @param url The URL to check
	 */
	private fun triggerDownloadManually(url: String) {
		try {
			ALL_DOWNLOADABLE_FORMATS.find { fileFormat ->
				url.endsWith(".$fileFormat") ||
						url.contains(".$fileFormat?") ||
						url.contains(".$fileFormat&")
			}?.let {
				ThreadsUtility.executeInBackground(codeBlock = {
					if (isValidURL(url)) {
						getFileInfoFromSever(url = URL(url)).let { urlFileInfo ->
							if (urlFileInfo.fileSize > 0) {
								val filename = decodeURLFileName(urlFileInfo.fileName)
								ThreadsUtility.executeOnMain {
									webviewEngine.webViewDownloadHandler.showDownloadAvailableDialog(
										url = url,
										contentLength = urlFileInfo.fileSize,
										userAgent = aioSettings.downloadHttpUserAgent,
										contentDisposition = null,
										safeWebEngineRef = webviewEngine,
										mimetype = FileUtility.getMimeType(urlFileInfo.fileName),
										userGivenFileName = filename
									)
								}
							}
						}
					}
				})
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Updates the browser's favicon display
	 * @param webView The WebView containing the favicon
	 */
	private fun updateBrowserFavicon(webView: WebView?) {
		try {
			webviewEngine.browserFragment.browserFragmentTop.animateDefaultFaviconLoading(true)
			webView?.favicon?.let { fav ->
				webviewEngine.browserFragment.browserFragmentTop.webViewFavicon.let { imageView ->
					Glide.with(safeMotherActivityRef).load(fav).into(imageView)
				}
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Creates an empty WebResourceResponse
	 * @return Empty response with plain text type
	 */
	private fun createEmptyResponse(): WebResourceResponse {
		val byteArrayInputStream = ByteArrayInputStream(byteArrayOf())
		return WebResourceResponse("text/plain", "UTF-8", byteArrayInputStream)
	}
	
	/**
	 * Checks if URL scheme should be blocked
	 * @param url The URL to check
	 * @return true if scheme is invalid, false otherwise
	 */
	private fun isInvalidScheme(url: String): Boolean {
		val invalidSchemes = listOf(
			"sfbth://",
			"fb://",
			"intent://",
			"market://",
			"whatsapp://"
		)
		
		// Block if URL starts with any invalid scheme
		return invalidSchemes.any { url.startsWith(it) } ||
				// Also block malformed URLs without proper scheme
				!url.startsWith("http://") && !url.startsWith("https://")
	}
	
	/**
	 * Checks if URL matches special cases that need custom handling
	 * @param url The URL to check
	 * @return true if URL matches special patterns
	 */
	private fun isSpecialCaseUrl(url: String): Boolean {
		// Define patterns for special URLs that need custom handling
		val specialPatterns = listOf(
			Regex("facebook\\.com/.*app_link"),
			Regex("twitter\\.com/.*intent"),
			Regex("instagram\\.com/.*direct")
		)
		
		return specialPatterns.any { it.containsMatchIn(url) }
	}
	
	/**
	 * Handles special URL cases with appropriate behavior
	 * @param url The special URL to handle
	 * @return true if handled, false to continue normal processing
	 */
	private fun handleSpecialUrl(url: String): Boolean {
		val context = webviewEngine.safeMotherActivityRef
		return when {
			url.startsWith("fb://") -> {
				// Handle Facebook deep link
				try {
					context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
					true
				} catch (e: Exception) {
					// Fallback to web URL if app not installed
					val webUrl = url.replace("fb://", "https://www.facebook.com/")
					webviewEngine.currentWebView?.loadUrl(webUrl)
					true
				}
			}
			// Add other special cases here...
			else -> false
		}
	}
}