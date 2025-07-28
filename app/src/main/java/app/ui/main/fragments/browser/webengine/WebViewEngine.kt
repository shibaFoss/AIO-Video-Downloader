@file:Suppress("DEPRECATION")

package app.ui.main.fragments.browser.webengine

import android.annotation.SuppressLint
import android.graphics.Color.WHITE
import android.os.Handler
import android.os.Looper
import android.util.Patterns.WEB_URL
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.LAYER_TYPE_HARDWARE
import android.view.View.OVER_SCROLL_NEVER
import android.view.View.SCROLLBARS_INSIDE_OVERLAY
import android.view.View.VISIBLE
import android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
import android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
import android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
import android.webkit.WebView
import android.webkit.WebView.RENDERER_PRIORITY_IMPORTANT
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import androidx.core.view.isVisible
import androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF
import androidx.webkit.WebSettingsCompat.setForceDark
import androidx.webkit.WebViewFeature.FORCE_DARK
import androidx.webkit.WebViewFeature.isFeatureSupported
import app.core.AIOApp.Companion.aioSettings
import app.ui.main.fragments.browser.BrowserFragment
import com.aio.R
import lib.networks.URLUtility
import lib.process.AsyncJobUtils.executeOnMainThread
import java.net.URLEncoder.encode
import java.nio.charset.StandardCharsets.UTF_8

class WebViewEngine(val browserFragment: BrowserFragment) {
	
	// References and shared objects
	val safeMotherActivityRef = browserFragment.safeMotherActivityRef
	val webViewDownloadHandler = WebViewDownloadHandler(this)
	var browserWebChromeClient = BrowserWebChromeClient(this)
	var browserWebClient = BrowserWebClient(this)
	var currentWebView: WebView? = null
	
	// Stores video libraries associated with WebViews
	var listOfWebVideosLibrary = ArrayList<WebVideosLibrary>()
	
	// Handler and delayed runnable to show temporary browser messages
	private var delayRunnable: Runnable? = null
	private val handler = Handler(Looper.getMainLooper())
	
	/** Pauses the current WebView (e.g., when switching tabs or app is paused). */
	fun pageCurrentWebView() {
		currentWebView?.onPause()
	}
	
	/** Resumes the current WebView (e.g., when coming back to a tab). */
	fun resumeCurrentWebView() {
		currentWebView?.onResume()
	}
	
	/** Destroys the current WebView to free resources. */
	fun destroyCurrentWebView() {
		pageCurrentWebView()
		currentWebView?.destroy()
	}
	
	/**
	 * Reloads or stops the current WebView depending on loading state.
	 * If page is fully loaded, it reloads. Otherwise, it stops loading.
	 */
	fun toggleCurrentWebViewLoading() {
		currentWebView?.let { webView ->
			if (webView.progress >= 100) webView.reload()
			else webView.stopLoading()
		}
	}
	
	/** Reloads the current WebView with its last known URL. */
	fun reloadCurrentWebView() {
		currentWebView?.let {
			updateEngineOfWebView(it)
			val currentWebViewUrl = it.url.toString()
			it.clearCache(true)
			it.loadUrl(currentWebViewUrl)
		}
	}
	
	/** Displays a short-lived info message in the browser fragment. */
	fun showQuickBrowserInfo(message: String) {
		val textQuickInfo = browserFragment.browserFragmentBody.quickBrowserInfo
		textQuickInfo.apply { visibility = VISIBLE; text = message }
		delayRunnable?.let { handler.removeCallbacks(it) }
		delayRunnable = Runnable {
			textQuickInfo.apply {
				visibility = GONE; text = ""
			}
		}; handler.postDelayed(delayRunnable!!, 1500)
	}
	
	/** Opens a URL in a new tab. If invalid, performs a Google search. */
	fun openURLInNewTab(url: String) {
		if (url.isEmpty()) return
		if (URLUtility.isValidURL(url)) {
			safeMotherActivityRef.sideNavigation?.addNewBrowsingTab(url, this)
		} else {
			safeMotherActivityRef.sideNavigation?.addNewBrowsingTab(
				if (WEB_URL.matcher(url).matches()) url else
					"https://www.google.com/search?q=" +
							encode(url, UTF_8.toString()), this)
		}
		safeMotherActivityRef.openBrowserFragment()
	}
	
	/** Loads a given URL into the current WebView and updates UI. */
	fun loadURLIntoCurrentWebview(urlString: String) {
		browserFragment.browserFragmentTop.webviewTitle.text = urlString
		browserFragment.browserFragmentTop.animateDefaultFaviconLoading()
		currentWebView?.loadUrl(urlString)
	}
	
	/** Returns a list of all WebViews currently managed in the system. */
	fun getListOfWebViewOnTheSystem(): ArrayList<WebView> {
		return safeMotherActivityRef.sideNavigation?.totalWebViews ?: ArrayList()
	}
	
	/** Dynamically generates a new WebView view and tracks it. */
	fun generateNewWebview(): View? {
		val generatedView = View.inflate(safeMotherActivityRef, R.layout.frag_brow_2_body_webview_1, null)
		generatedView.id = generateUniqueIdForWebView()
		listOfWebVideosLibrary.add(WebVideosLibrary().apply {
			webViewId = generatedView.id
		})
		return generatedView
	}
	
	private fun generateUniqueIdForWebView(): Int {
		val existingIds = getListOfWebViewOnTheSystem().map { it.id }.toSet()
		var newId = View.generateViewId()
		
		while (existingIds.contains(newId)) {
			newId = View.generateViewId()
		}
		return newId
	}
	
	@SuppressLint("ClickableViewAccessibility")
	fun updateEngineOfWebView(currentWebView: WebView) {
		currentWebView.layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
		currentWebView.scrollBarStyle = SCROLLBARS_INSIDE_OVERLAY
		currentWebView.webViewClient = browserWebClient
		currentWebView.webChromeClient = browserWebChromeClient
		
		currentWebView.isFocusableInTouchMode = true
		currentWebView.isFocusable = true
		
		currentWebView.isScrollbarFadingEnabled = true
		currentWebView.isSaveEnabled = true
		currentWebView.overScrollMode = OVER_SCROLL_NEVER
		
		currentWebView.setBackgroundColor(WHITE)
		currentWebView.setLayerType(LAYER_TYPE_HARDWARE, null)
		currentWebView.setRendererPriorityPolicy(RENDERER_PRIORITY_IMPORTANT, false)
		currentWebView.setNetworkAvailable(true)
		currentWebView.clearCache(true)
		currentWebView.setDownloadListener(webViewDownloadHandler)
		
		// Long-press handling (e.g., open link/image in new tab)
		currentWebView.setOnLongClickListener { view ->
			val hitTestResult = (view as WebView).hitTestResult
			if (hitTestResult.type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
				hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
				hitTestResult.extra?.let {
					safeMotherActivityRef.sideNavigation?.addNewBrowsingTab(it, this@WebViewEngine)
				}
				true
			} else {
				false
			}
		}
		
		// Settings configuration
		currentWebView.settings.apply {
			setSupportZoom(true)
			setGeolocationEnabled(true)
			setNeedInitialFocus(true)
			
			if (isFeatureSupported(FORCE_DARK))
				setForceDark(this, FORCE_DARK_OFF)
			
			builtInZoomControls = true
			displayZoomControls = false
			allowContentAccess = true
			allowFileAccess = true
			safeBrowsingEnabled = false
			
			cacheMode = LOAD_CACHE_ELSE_NETWORK
			domStorageEnabled = true
			mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
			
			mediaPlaybackRequiresUserGesture = false
			loadWithOverviewMode = true
			useWideViewPort = true
			offscreenPreRaster = false
			layoutAlgorithm = TEXT_AUTOSIZING
			
			javaScriptCanOpenWindowsAutomatically = true
			setSupportMultipleWindows(true)
			
			if (aioSettings.browserEnableHideImages) {
				blockNetworkImage = true
				loadsImagesAutomatically = false
			} else {
				blockNetworkImage = false
				loadsImagesAutomatically = true
			}
			
			javaScriptEnabled = aioSettings.browserEnableJavascript
			userAgentString = aioSettings.browserHttpUserAgent
		}
		
		// Close drawer if user touches the webview
		currentWebView.setOnTouchListener { view, event ->
			if (event.action == MotionEvent.ACTION_DOWN) {
				safeMotherActivityRef.sideNavigation?.closeDrawerNavigation()
				if (view.hasOnClickListeners()) view.performClick()
			}; false
		}
		
		WebVideoParser.resetVideoGrabbingButton(this)
	}
	
	/**
	 * Updates UI progress bar and reload/cancel icon as WebView loads.
	 */
	fun updateWebViewProgress(webView: WebView?, progress: Int) {
		try {
			if (currentWebView != webView) return
			val browserFragmentTop = browserFragment.browserFragmentTop
			val webViewProgressBar = browserFragmentTop.webViewProgress
			webViewProgressBar.visibility = VISIBLE
			webViewProgressBar.progress = progress
			val webViewReloadingButton = browserFragmentTop.webViewReloadButton
			if (webViewProgressBar.progress >= 100) {
				webViewProgressBar.visibility = GONE
				webViewReloadingButton.setImageResource(R.drawable.ic_button_restart_v2)
				webView?.url?.let { WebVideoParser.analyzeUrl(it, this) }
			} else webViewReloadingButton.setImageResource(R.drawable.ic_button_cancel)
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/** Returns cookies for the current WebView if available. */
	fun getCurrentWebViewCookies(): String? {
		if (currentWebView != null) {
			if (!currentWebView!!.url.isNullOrEmpty()) {
				val cookieManager = android.webkit.CookieManager.getInstance()
				val cookies = cookieManager.getCookie(currentWebView!!.url) ?: null
				return cookies
			}
		}; return null
	}
	
	/** Enables/disables video grabber button based on user settings. */
	fun toggleVideoGrabbingFeature() {
		executeOnMainThread {
			try {
				val browserFragmentBody = browserFragment.browserFragmentBody
				val videoGrabberButton = browserFragmentBody.videoGrabberButton
				if (aioSettings.browserEnableVideoGrabber) {
					if (!videoGrabberButton.isVisible) {
						videoGrabberButton.visibility = VISIBLE
					}
				} else {
					if (videoGrabberButton.isVisible) {
						videoGrabberButton.visibility = GONE
					}
				}
			} catch (error: Exception) {
				error.printStackTrace()
			}
		}
	}
}