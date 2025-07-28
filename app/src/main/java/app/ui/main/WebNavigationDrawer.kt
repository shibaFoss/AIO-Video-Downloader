package app.ui.main

import android.view.View
import android.webkit.WebView
import android.widget.ListView
import androidx.core.view.isVisible
import app.core.AIOApp.Companion.aioSettings
import app.ui.main.fragments.browser.webengine.WebTabListAdapter
import app.ui.main.fragments.browser.webengine.WebViewEngine
import com.aio.R
import com.bumptech.glide.Glide
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.process.CommonTimeUtils.delay
import lib.ui.ViewUtility
import lib.ui.ViewUtility.hideView
import lib.ui.ViewUtility.showView
import lib.ui.builders.ToastView
import java.lang.ref.WeakReference

/**
 * Manages the web navigation drawer functionality.
 *
 * Responsibilities:
 * - Handles creation and management of browser tabs
 * - Controls the navigation drawer UI and state
 * - Maintains the list of active web views
 * - Coordinates tab switching and lifecycle events
 */
class WebNavigationDrawer(motherActivity: MotherActivity?) {
	
	// Weak reference to parent activity to prevent memory leaks
	val safeMotherActivityRef = WeakReference(motherActivity).get()
	
	// List of all active web views
	val totalWebViews: ArrayList<WebView> = ArrayList()
	
	// UI Components
	lateinit var sideNavigationDrawer: View
	lateinit var buttonAddNewTab: View
	lateinit var browserTabsListView: ListView
	lateinit var webTabListAdapter: WebTabListAdapter
	
	/**
	 * Initializes the navigation drawer components.
	 * Must be called after activity layout is rendered.
	 */
	fun initialize() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			safeMotherActivityRef.apply {
				// Initialize UI components
				sideNavigationDrawer = findViewById(R.id.navigation_drawer)
				buttonAddNewTab = findViewById(R.id.button_create_new_tab)
				browserTabsListView = findViewById(R.id.list_browser_tabs)
				
				// Set up event listeners
				initializeClickEvents()
				initializeDrawerListener()
			}
		}
	}
	
	/**
	 * Initializes the web tab list adapter if not already initialized.
	 */
	private fun initializeWebListAdapter() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			safeMotherActivityRef.browserFragment?.let {
				if (browserTabsListView.adapter == null) {
					browserTabsListView.adapter =
						if (!::webTabListAdapter.isInitialized) {
							// Create new adapter if needed
							webTabListAdapter = WebTabListAdapter(it, totalWebViews)
							webTabListAdapter
						} else webTabListAdapter
				}
			}
		}
	}
	
	/**
	 * Initializes drawer state listener and updates adapter.
	 */
	private fun initializeDrawerListener() {
		safeMotherActivityRef?.let { _ ->
			if (::webTabListAdapter.isInitialized) {
				webTabListAdapter.notifyDataSetChanged()
			}
		}
	}
	
	/**
	 * Sets up click event listeners for drawer components.
	 */
	private fun initializeClickEvents() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			buttonAddNewTab.setOnClickListener {
				val browserFragment = safeMotherActivityRef.browserFragment
				val browserFragmentBody = browserFragment?.browserFragmentBody
				val webviewEngine = browserFragmentBody?.webviewEngine
				
				webviewEngine?.apply {
					// Add new tab with default homepage
					addNewBrowsingTab(aioSettings.browserDefaultHomepage, this)
					this.safeMotherActivityRef.openBrowserFragment()
				}
			}
		}
	}
	
	/**
	 * Opens the navigation drawer with animation.
	 */
	fun openDrawerNavigation() {
		if (sideNavigationDrawer.isVisible) hideView(sideNavigationDrawer, true, 100)
		else showView(sideNavigationDrawer, true, 100)
	}
	
	/**
	 * Closes the navigation drawer with animation.
	 */
	fun closeDrawerNavigation() {
		hideView(sideNavigationDrawer, true, 100)
	}
	
	/**
	 * Checks if the drawer is currently open.
	 * @return true if drawer is visible, false otherwise
	 */
	fun isDrawerOpened(): Boolean {
		return sideNavigationDrawer.isVisible
	}
	
	/**
	 * Adds a new browsing tab with the specified URL.
	 * @param url The URL to load in the new tab
	 * @param webviewEngine The web view engine to use for the new tab
	 */
	fun addNewBrowsingTab(url: String, webviewEngine: WebViewEngine) {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			initializeWebListAdapter()
			
			webviewEngine.generateNewWebview()?.let { generatedWebView ->
				// Configure the new web view
				webviewEngine.currentWebView = (generatedWebView as WebView)
				
				val browserFragment = safeMotherActivityRef.browserFragment
				val webViewContainer = browserFragment?.getBrowserWebViewContainer()
				
				// Update UI
				webViewContainer?.removeAllViews()
				webViewContainer?.addView(webviewEngine.currentWebView)
				
				// Pause all other tabs and activate new one
				totalWebViews.forEach { webView -> webView.onPause() }
				webviewEngine.updateEngineOfWebView(webviewEngine.currentWebView!!)
				webviewEngine.loadURLIntoCurrentWebview(url)
				
				// Add to beginning of list and update UI
				totalWebViews.add(0, webviewEngine.currentWebView!!)
				webTabListAdapter.notifyDataSetChanged()
				closeDrawerNavigation()
			}
		}
	}
	
	/**
	 * Closes a web view tab at the specified position.
	 * @param position The position of the tab to close
	 * @param correspondingWebView The web view to close
	 */
	fun closeWebViewTab(position: Int, correspondingWebView: WebView) {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			initializeWebListAdapter()
			
			try {
				val browserFragment = safeMotherActivityRef.browserFragment
				val browserWebviewEngine = browserFragment?.getBrowserWebEngine()
				
				// Handle case when closing last tab
				if (position == 0 && totalWebViews.size == 1) {
					totalWebViews.remove(correspondingWebView)
					webTabListAdapter.notifyDataSetChanged()
					
					browserWebviewEngine?.let {
						// Create new default tab if closing last one
						addNewBrowsingTab(
							webviewEngine = it,
							url = aioSettings.browserDefaultHomepage
						)
					}
					closeDrawerNavigation()
					return
				}
				
				// Remove the tab
				totalWebViews.remove(correspondingWebView)
				webTabListAdapter.notifyDataSetChanged()
				totalWebViews.forEach { webView -> webView.onPause() }
				
				// Determine which tab to show next
				if (position == 0) {
					val nextPosition = if (totalWebViews.isNotEmpty()) 0 else -1
					if (nextPosition != -1) {
						openWebViewTab(totalWebViews[nextPosition])
					} else {
						browserWebviewEngine?.let {
							addNewBrowsingTab(
								webviewEngine = it,
								url = aioSettings.browserDefaultHomepage)
						}
						delay(
							timeInMile = 200,
							listener = object : OnTaskFinishListener {
								override fun afterDelay() = closeDrawerNavigation()
						})
					}
				} else {
					// Show previous tab
					val previousPosition = position - 1
					openWebViewTab(totalWebViews[previousPosition])
				}
				
				// Clean up the closed web view
				correspondingWebView.clearHistory()
				correspondingWebView.onPause()
				correspondingWebView.loadUrl("about:blank")
				ViewUtility.unbindDrawables(correspondingWebView)
				System.gc()
			} catch (error: Exception) {
				error.printStackTrace()
				safeMotherActivityRef.doSomeVibration(50)
				ToastView.showToast(msgId = R.string.text_something_went_wrong)
			}
		}
	}
	
	/**
	 * Opens and activates the specified web view tab.
	 * @param targetWebview The web view to activate
	 */
	fun openWebViewTab(targetWebview: WebView) {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			initializeWebListAdapter()
			try {
				val browserFragment = safeMotherActivityRef.browserFragment
				val browserWebviewEngine = browserFragment?.getBrowserWebEngine()
				val browserWebChromeClient = browserWebviewEngine?.browserWebChromeClient
				val browserWebViewContainer = browserFragment?.getBrowserWebViewContainer()
				val browserWebviewFavicon = browserFragment?.browserFragmentTop?.webViewFavicon
				
				// Update current web view reference
				browserWebviewEngine?.currentWebView = targetWebview
				
				// Update UI
				browserWebViewContainer?.removeAllViews()
				browserWebViewContainer?.addView(targetWebview)
				
				// Configure web view
				browserWebviewEngine?.updateEngineOfWebView(targetWebview)
				browserFragment?.browserFragmentTop?.webviewTitle?.text = if (
					targetWebview.title.isNullOrEmpty()
				) targetWebview.url else targetWebview.title
				
				targetWebview.requestFocus()
				browserWebviewEngine?.resumeCurrentWebView()
				
				// Update progress
				browserWebChromeClient?.onProgressChanged(targetWebview, targetWebview.progress)
				
				// Update favicon
				browserFragment?.browserFragmentTop?.animateDefaultFaviconLoading(true)
				targetWebview.favicon?.let { favicon ->
					browserWebviewFavicon?.let { imageView ->
						Glide.with(safeMotherActivityRef).load(favicon).into(imageView)
					}
				}
			} catch (error: Exception) {
				error.printStackTrace()
				safeMotherActivityRef.doSomeVibration(50)
				ToastView.showToast(msgId = R.string.text_something_went_wrong)
			}
		}
	}
}