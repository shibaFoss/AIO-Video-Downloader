package app.ui.main.fragments.browser

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import app.core.AIOApp.Companion.IS_ULTIMATE_VERSION_UNLOCKED
import app.core.AIOApp.Companion.aioAdblocker
import app.core.AIOApp.Companion.aioTimer
import app.core.AIOTimer
import app.core.bases.BaseFragment
import app.core.engines.video_parser.parsers.SupportedURLs.isYouTubeUrl
import app.ui.main.MotherActivity
import app.ui.main.MotherActivity.SharedViewModel
import app.ui.main.fragments.browser.webengine.WebVideoParser.analyzeUrl
import app.ui.main.fragments.browser.webengine.WebViewEngine
import com.aio.R
import lib.texts.CommonTextUtils

/**
 * Fragment that manages the in-app browser UI and behavior.
 * Handles webview display, video extraction, ad-blocking, and periodic checks using AIOTimer.
 */
class BrowserFragment : BaseFragment(), AIOTimer.AIOTimerListener {
	
	// Shared ViewModel for communicating between fragments
	lateinit var sharedViewModel: SharedViewModel
	
	// Top and body layout controllers for the browser fragment
	lateinit var browserFragmentTop: BrowserFragmentTop
	lateinit var browserFragmentBody: BrowserFragmentBody
	
	// Reference to the hosting activity
	lateinit var safeMotherActivityRef: MotherActivity

	/**
	 * Provides the layout resource ID for this fragment.
	 */
	override fun getLayoutResId(): Int {
		return R.layout.frag_brow_1_main_1
	}
	
	/**
	 * Called after the layout is loaded. Initializes the browser UI and loads the default webpage.
	 */
	override fun onAfterLayoutLoad(layoutView: View, state: Bundle?) {
		registerSelfInMotherActivity()
		initializeURLEditChangesListener()
		browserFragmentTop = BrowserFragmentTop(this)
		browserFragmentBody = BrowserFragmentBody(this)
		browserFragmentBody.loadDefaultWebpage()
	}
	
	/**
	 * Called when the fragment resumes. Registers timer and updates UI.
	 */
	override fun onResumeFragment() {
		aioTimer.register(this)
		registerSelfInMotherActivity()
		browserFragmentBody.onResumeBrowserFragment()
	}
	
	/**
	 * Called when the fragment is paused. Unregisters timer and pauses webview.
	 */
	override fun onPauseFragment() {
		aioTimer.unregister(this)
		browserFragmentBody.onPauseBrowserFragment()
	}
	
	/**
	 * Called when the fragment is destroyed. Cleans up browser resources.
	 */
	override fun onDestroy() {
		browserFragmentBody.onDestroyBrowserFragment()
		super.onDestroy()
	}
	
	/**
	 * Returns the current web engine used by the browser.
	 */
	fun getBrowserWebEngine(): WebViewEngine {
		return this.browserFragmentBody.webviewEngine
	}
	
	/**
	 * Returns the layout container that hosts the webview.
	 */
	fun getBrowserWebViewContainer(): LinearLayout {
		return this.browserFragmentBody.webViewContainer
	}
	
	/**
	 * Registers the fragment in the parent MotherActivity for inter-fragment communication.
	 */
	private fun registerSelfInMotherActivity() {
		safeMotherActivityRef = (safeBaseActivityRef as MotherActivity)
		safeMotherActivityRef.browserFragment = this@BrowserFragment
		safeMotherActivityRef.sideNavigation?.closeDrawerNavigation()
	}
	
	/**
	 * Sets up a LiveData observer to listen for changes in the URL entered by the user.
	 * Loads the new URL into the webview.
	 */
	private fun initializeURLEditChangesListener() {
		sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
		sharedViewModel.liveURLString.observe(viewLifecycleOwner) { result ->
			result?.let { loadURLIntoCurrentWebView(it) }
		}
	}
	
	/**
	 * Loads a URL string into the currently active webview.
	 */
	private fun loadURLIntoCurrentWebView(urlString: String) {
		getBrowserWebEngine().loadURLIntoCurrentWebview(urlString)
	}
	
	/**
	 * Periodic callback from the AIOTimer. Executes browser-related checks and updates.
	 */
	override fun onAIOTimerTick(loopCount: Double) {
		try {
			keepWebviewAlive()
			analyzeExtractedVideoLinks()
			filterOutAdsMu38Links()
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Attempts to extract playable video links from the current webview's URL.
	 * Toggles the video grabbing feature based on availability.
	 */
	private fun analyzeExtractedVideoLinks() {
		val url = getBrowserWebEngine().currentWebView?.url.toString()
		if (isYouTubeUrl(url) && !IS_ULTIMATE_VERSION_UNLOCKED) {
			browserFragmentBody.videoGrabberButton.visibility = GONE
			val msgYtNotSupported = CommonTextUtils.getText(R.string.text_youtube_download_not_supported)
			browserFragmentBody.webviewEngine.showQuickBrowserInfo(msgYtNotSupported)
		} else {
			analyzeUrl(getBrowserWebEngine().currentWebView?.url, getBrowserWebEngine())
			getBrowserWebEngine().toggleVideoGrabbingFeature()
		}
	}

	/**
	 * Keeps the current webview active and alive while the browser is visible.
	 */
	private fun keepWebviewAlive() {
		val condition1 = safeMotherActivityRef.isActivityRunning()
		val condition2 = safeMotherActivityRef.getCurrentFragmentNumber() == 1
		if (condition1 && condition2) {
			if (isFragmentRunning) getBrowserWebEngine().resumeCurrentWebView()
		}
	}
	
	/**
	 * Filters out video URLs that belong to known ad hosts using the internal ad-blocker.
	 */
	private fun filterOutAdsMu38Links() {
		try {
			val currentWebVideosLibrary = getBrowserWebEngine().listOfWebVideosLibrary
				.find { it.webViewId == getBrowserWebEngine().currentWebView?.id }
			
			if (currentWebVideosLibrary != null) {
				val listOfAvailableVideoUrls = currentWebVideosLibrary.listOfAvailableVideoUrlInfos
				val adHosts = aioAdblocker.getAdBlockHosts().toSet()
				
				// Remove video URLs that contain known ad hosts
				listOfAvailableVideoUrls.removeIf { videoUrlInfo ->
					adHosts.any { adHost -> videoUrlInfo.fileUrl.contains(adHost, ignoreCase = true) }
				}
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
}