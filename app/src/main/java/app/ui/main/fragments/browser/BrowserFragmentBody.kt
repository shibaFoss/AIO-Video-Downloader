package app.ui.main.fragments.browser

import android.view.View.GONE
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import app.core.AIOApp.Companion.IS_ULTIMATE_VERSION_UNLOCKED
import app.core.AIOApp.Companion.aioSettings
import app.core.engines.video_parser.parsers.SupportedURLs.isSocialMediaUrl
import app.core.engines.video_parser.parsers.SupportedURLs.isYouTubeUrl
import app.core.engines.video_parser.parsers.VideoThumbGrabber.startParsingVideoThumbUrl
import app.ui.main.MotherActivity
import app.ui.main.MotherActivity.SharedViewModel
import app.ui.main.fragments.browser.webengine.SingleResolutionPrompter
import app.ui.main.fragments.browser.webengine.WebViewEngine
import app.ui.main.fragments.downloads.intercepter.SharedVideoURLIntercept
import com.aio.R
import com.airbnb.lottie.LottieAnimationView
import lib.device.IntentUtility
import lib.networks.URLUtilityKT.fetchWebPageContent
import lib.networks.URLUtilityKT.getWebpageTitleOrDescription
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.ThreadsUtility
import lib.texts.CommonTextUtils.getText
import lib.ui.builders.ToastView.Companion.showToast
import lib.ui.builders.WaitingDialog

/**
 * Handles the core logic and lifecycle for a single browser tab instance inside the BrowserFragment.
 * Responsible for initializing the web engine, managing intent URLs, handling video URL detection,
 * ad display, and user navigation interactions.
 *
 * @property browserFragment The parent fragment instance tied to this browser body.
 */
class BrowserFragmentBody(val browserFragment: BrowserFragment) {
	
	/** Reference to the parent activity. */
	val safeMotherActivityRef = browserFragment.safeBaseActivityRef!! as MotherActivity
	
	/** WebView engine responsible for rendering and managing webpages. */
	var webviewEngine = WebViewEngine(browserFragment)
	
	/** Keeps track of the last loaded intent URL to avoid duplicate loading. */
	var alreadyLoadedIntentURL: String? = null
	
	/** Layout container for the webview. */
	lateinit var webViewContainer: LinearLayout
	
	/** Button to trigger the video grabbing feature. */
	lateinit var videoGrabberButton: LottieAnimationView
	
	/** TextView to display temporary browser messages/info. */
	lateinit var quickBrowserInfo: TextView
	
	/** Flag used to cancel ongoing title parsing tasks. */
	private var isParsingTitleFromUrlAborted = false
	
	init {
		initializeBackPressedEvent()
		initializeWebView()
	}
	
	/**
	 * Called from Fragment.onResume. Resumes the current WebView and processes any new intent URLs.
	 */
	fun onResumeBrowserFragment() {
		webviewEngine.resumeCurrentWebView()
		loadURLFromIntent()
	}
	
	/**
	 * Called from Fragment.onPause. Pauses the current WebView.
	 */
	fun onPauseBrowserFragment() {
		webviewEngine.pageCurrentWebView()
	}
	
	/**
	 * Called from Fragment.onDestroy. Destroys the current WebView to release resources.
	 */
	fun onDestroyBrowserFragment() {
		webviewEngine.destroyCurrentWebView()
	}
	
	/**
	 * Loads the default homepage defined in the app settings into a new browsing tab.
	 */
	fun loadDefaultWebpage() {
		val defaultUrl = aioSettings.browserDefaultHomepage
		safeMotherActivityRef.sideNavigation?.addNewBrowsingTab(defaultUrl, webviewEngine)
	}
	
	/**
	 * Sets up listener for back-press events to either navigate back in the WebView
	 * or close the current tab appropriately.
	 */
	private fun initializeBackPressedEvent() {
		ViewModelProvider(browserFragment.requireActivity())[SharedViewModel::class.java]
			.apply {
				backPressLiveEvent.observe(browserFragment.viewLifecycleOwner) {
					goBackToPreviousWebPage()
				}
			}
	}
	
	/**
	 * Initializes the WebView layout and supporting UI elements inside the fragment's view.
	 */
	private fun initializeWebView() {
		browserFragment.safeFragmentLayoutRef?.apply {
			webviewEngine = WebViewEngine(browserFragment)
			webViewContainer = findViewById(R.id.browser_webview_container)
			videoGrabberButton = findViewById(R.id.btn_video_grabber)
			quickBrowserInfo = findViewById(R.id.txt_browser_quick_info)
			quickBrowserInfo.visibility = GONE
		}
	}
	
	/**
	 * Attempts to load a URL passed via Intent. Based on the URL type, it may open
	 * the downloads section, show video parsing prompt, or load in a browser tab.
	 */
	private fun loadURLFromIntent() {
		val intentURL = IntentUtility.getIntentDataURI(safeMotherActivityRef)
		if (intentURL.isNullOrEmpty()) return
		if (intentURL == alreadyLoadedIntentURL) return
		
		if (isSocialMediaUrl(intentURL)) {
			// Handle social media URLs
			alreadyLoadedIntentURL = intentURL
			safeMotherActivityRef.openDownloadsFragment()
			
			val waitingDialog = WaitingDialog(
				isCancelable = false,
				baseActivityInf = safeMotherActivityRef,
				loadingMessage = getText(R.string.text_analyzing_url_please_wait),
				dialogCancelListener = { dialog ->
					isParsingTitleFromUrlAborted = true
					dialog.dismiss()
				}
			); waitingDialog.show()
			
			ThreadsUtility.executeInBackground(codeBlock = {
				val htmlBody = fetchWebPageContent(intentURL, true)
				val thumbnailUrl = startParsingVideoThumbUrl(intentURL, htmlBody)
				getWebpageTitleOrDescription(intentURL, userGivenHtmlBody = htmlBody) { resultedTitle ->
					waitingDialog.close()
					if (!resultedTitle.isNullOrEmpty() && !isParsingTitleFromUrlAborted) {
						executeOnMainThread {
							SingleResolutionPrompter(
								baseActivity = safeMotherActivityRef,
								singleResolutionName = getText(R.string.title_high_quality),
								extractedVideoLink = intentURL,
								currentWebUrl = intentURL,
								videoTitle = resultedTitle,
								videoUrlReferer = intentURL,
								dontParseFBTitle = true,
								thumbnailUrlProvided = thumbnailUrl,
								isSocialMediaUrl = true,
								isDownloadFromBrowser = false
							).show()
						}
					} else {
						executeOnMainThread {
							safeMotherActivityRef.doSomeVibration(50)
							showToast(msgId = R.string.text_couldnt_get_video_title)
						}
					}
				}
			})
		} else {
			// Handle YouTube or generic URLs
			if (IS_ULTIMATE_VERSION_UNLOCKED && isYouTubeUrl(intentURL)) {
				parseYtdlpVideoLink(intentURL)
			} else {
				safeMotherActivityRef.openBrowserFragment()
				safeMotherActivityRef.sideNavigation?.addNewBrowsingTab(intentURL, webviewEngine)
				alreadyLoadedIntentURL = intentURL
			}
		}
	}
	
	/**
	 * Uses the app’s video intercept system to parse a YouTube URL directly using yt-dlp logic.
	 */
	private fun parseYtdlpVideoLink(intentURL: String?) {
		alreadyLoadedIntentURL = intentURL
		safeMotherActivityRef.openDownloadsFragment()
		SharedVideoURLIntercept(safeMotherActivityRef).interceptIntentURI(intentURL)
	}
	
	/**
	 * Handles back navigation logic for the browser tab. If the WebView cannot go back,
	 * it closes the tab or redirects to the home screen.
	 */
	private fun goBackToPreviousWebPage() {
		if (browserFragment.browserFragmentTop.browserUrlEditFieldContainer.isVisible) {
			browserFragment.browserFragmentTop.invisibleUrlEditSection()
			return
		}
		
		webviewEngine.currentWebView?.let {
			if (it.canGoBack()) it.goBack() else {
				try {
					val tabAdapter = safeMotherActivityRef.sideNavigation?.webTabListAdapter ?: return
					if (tabAdapter.listOfWebViews.size == 1) {
						safeMotherActivityRef.openHomeFragment()
					} else {
						val currentTabPosition = tabAdapter.listOfWebViews.indexOf(it)
						safeMotherActivityRef.sideNavigation?.closeWebViewTab(currentTabPosition, it)
						webviewEngine.showQuickBrowserInfo(getText(R.string.text_closed_tab))
					}
				} catch (error: Exception) {
					error.printStackTrace()
					safeMotherActivityRef.openHomeFragment()
				}
			}
		} ?: run { safeMotherActivityRef.openHomeFragment() }
	}
}