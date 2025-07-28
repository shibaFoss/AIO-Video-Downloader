package app.ui.main.fragments.browser

import android.util.Patterns.WEB_URL
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import app.ui.main.MotherActivity
import com.aio.R
import com.bumptech.glide.Glide
import lib.ui.ViewUtility.animateFadInOutAnim
import lib.ui.ViewUtility.closeAnyAnimation
import lib.ui.ViewUtility.hideOnScreenKeyboard
import lib.ui.ViewUtility.showOnScreenKeyboard
import java.net.URLEncoder.encode
import java.nio.charset.StandardCharsets.UTF_8

/**
 * Manages the top section of the browser fragment UI, including URL input,
 * favicon, title, reload button, and popup options.
 *
 * @property browserFragment The fragment instance this top bar is attached to.
 */
class BrowserFragmentTop(val browserFragment: BrowserFragment) {
	
	/** Reference to the hosting activity. */
	val safeMotherActivityRef = browserFragment.safeBaseActivityRef!! as MotherActivity
	
	/** The currently active WebView instance. */
	val currentWebView by lazy { browserFragment.browserFragmentBody.webviewEngine.currentWebView }
	
	/** UI components **/
	lateinit var webViewReloadButton: ImageView
	lateinit var webviewTitle: TextView
	lateinit var webViewFavicon: ImageView
	lateinit var webViewProgress: ProgressBar
	lateinit var webviewOptionPopup: View
	lateinit var browserOptionsPopup: BrowserOptionsPopup
	
	lateinit var browserTopTitleSection: View
	lateinit var browserUrlEditFieldContainer: View
	lateinit var browserUrlEditField: EditText
	
	init {
		initializeViews(browserFragment.safeFragmentLayoutRef)
		setupClicksEvents(browserFragment.safeFragmentLayoutRef)
	}
	
	/**
	 * Displays or stops the default favicon animation.
	 * @param shouldStopAnimation If true, stops any ongoing animation.
	 */
	fun animateDefaultFaviconLoading(shouldStopAnimation: Boolean = false) {
		if (shouldStopAnimation) {
			closeAnyAnimation(webViewFavicon)
			return
		}
		val defaultFaviconResId = R.drawable.ic_button_browser_favicon
		Glide.with(safeMotherActivityRef).load(defaultFaviconResId).into(webViewFavicon)
		animateFadInOutAnim(webViewFavicon)
	}
	
	/**
	 * Initializes view references from the layout.
	 * @param layoutView The root view of the fragment.
	 */
	private fun initializeViews(layoutView: View?) {
		layoutView?.let { _ ->
			browserTopTitleSection = layoutView.findViewById(R.id.top_layout_actionbar_section)
			browserUrlEditFieldContainer = layoutView.findViewById(R.id.top_url_edit_section)
			browserUrlEditField = layoutView.findViewById(R.id.edit_field_url)
			webViewReloadButton = layoutView.findViewById(R.id.button_browser_reload)
			webViewFavicon = layoutView.findViewById(R.id.image_browser_favicon)
			webviewTitle = layoutView.findViewById(R.id.edit_search_suggestion)
			webViewProgress = layoutView.findViewById(R.id.webview_progress_bar)
			webviewOptionPopup = layoutView.findViewById(R.id.button_browser_options)
		}
	}
	
	/**
	 * Sets up click listeners for all interactive UI components.
	 * @param layoutView The root view of the fragment.
	 */
	private fun setupClicksEvents(layoutView: View?) {
		val slideNavigation = safeMotherActivityRef.sideNavigation
		
		layoutView?.apply {
			val clickActions = mapOf(
				findViewById<View>(R.id.button_actionbar_back) to { invisibleUrlEditSection() },
				findViewById<View>(R.id.button_clear_url_edit_field) to { clearEditTextField() },
				findViewById<View>(R.id.button_load_url_to_browser) to { loadUrlToBrowser() },
				findViewById<View>(R.id.button_open_navigation) to { slideNavigation?.openDrawerNavigation() },
				findViewById<View>(R.id.container_edit_browser_url) to { visibleUrlEditSection() },
				findViewById<View>(R.id.button_browser_reload) to { toggleWebviewLoading() },
				findViewById<View>(R.id.button_browser_options) to { openBrowserPopupOptions() })
			
			clickActions.forEach { (view, clickAction) ->
				view.setOnClickListener { clickAction() }
			}
		}
	}
	
	/**
	 * Displays the browser options popup menu.
	 */
	private fun openBrowserPopupOptions() {
		if (!::browserOptionsPopup.isInitialized)
			browserOptionsPopup = BrowserOptionsPopup(browserFragment)
		browserOptionsPopup.show()
	}
	
	/**
	 * Reloads the current WebView or stops loading if already loading.
	 */
	private fun toggleWebviewLoading() {
		browserFragment.browserFragmentBody
			.webviewEngine.toggleCurrentWebViewLoading()
	}
	
	/**
	 * Shows the URL edit field and allows user input.
	 */
	fun visibleUrlEditSection() {
		browserTopTitleSection.visibility = View.INVISIBLE
		browserUrlEditFieldContainer.visibility = View.VISIBLE
		
		val browserWebEngine = browserFragment.getBrowserWebEngine()
		val currentWebviewUrl = browserWebEngine.currentWebView?.url
		currentWebviewUrl?.let {
			browserUrlEditField.setText(currentWebviewUrl)
			focusEditTextFieldAndShowKeyboard()
			browserUrlEditField.selectAll()
		}
		
		browserUrlEditField.setOnEditorActionListener { _, actionId, _ ->
			if (actionId == EditorInfo.IME_ACTION_DONE ||
				actionId == EditorInfo.IME_ACTION_SEARCH) {
				loadUrlToBrowser(); true
			} else false
		}
	}
	
	/**
	 * Hides the URL edit field and restores the default title section.
	 */
	fun invisibleUrlEditSection() {
		browserTopTitleSection.visibility = View.VISIBLE
		browserUrlEditFieldContainer.visibility = View.GONE
	}
	
	/**
	 * Focuses the URL input field and displays the soft keyboard.
	 */
	fun focusEditTextFieldAndShowKeyboard() {
		browserUrlEditField.requestFocus()
		showOnScreenKeyboard(safeMotherActivityRef, browserUrlEditField)
	}
	
	/**
	 * Clears the URL input field and refocuses it.
	 */
	private fun clearEditTextField() {
		browserUrlEditField.setText("")
		focusEditTextFieldAndShowKeyboard()
	}
	
	/**
	 * Loads the URL entered by the user into the browser WebView.
	 * If the input is not a valid URL, performs a Google search.
	 */
	private fun loadUrlToBrowser() {
		hideOnScreenKeyboard(safeMotherActivityRef, browserUrlEditField)
		var urlToLoad = browserUrlEditField.text.toString()
		urlToLoad = (if (WEB_URL.matcher(urlToLoad).matches()) urlToLoad
		else "https://www.google.com/search?q=${encode(urlToLoad, UTF_8.toString())}")
		val browserWebEngine = browserFragment.getBrowserWebEngine()
		browserWebEngine.loadURLIntoCurrentWebview(urlToLoad)
		invisibleUrlEditSection()
	}
}