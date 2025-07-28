package app.ui.main.fragments.browser

import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.view.View
import androidx.core.net.toUri
import app.core.AIOApp.Companion.aioBookmark
import app.core.engines.browser.bookmarks.BookmarkModel
import app.core.engines.video_parser.dialogs.VideoLinkPasteEditor
import app.ui.main.MotherActivity
import app.ui.main.fragments.browser.activities.BookmarksActivity
import app.ui.main.fragments.browser.activities.HistoryActivity
import app.ui.main.guides.GuidePlatformPicker
import app.ui.others.information.UserFeedbackActivity
import com.aio.R
import lib.networks.URLUtility
import lib.texts.ClipboardUtils.copyTextToClipboard
import lib.texts.CommonTextUtils.getText
import lib.ui.ViewUtility.hideOnScreenKeyboard
import lib.ui.builders.PopupBuilder
import lib.ui.builders.ToastView.Companion.showToast
import java.util.Date

/**
 * A helper class responsible for displaying and managing a popup menu
 * containing various browser-related actions in the browser fragment.
 *
 * This includes navigation (back/forward), bookmark management,
 * sharing or copying the current URL, opening the URL in the system browser,
 * accessing bookmarks/history, and more.
 */
class BrowserOptionsPopup(val browserFragment: BrowserFragment) {
	
	// Safe reference to the parent activity hosting the fragment
	private val safeMotherActivityRef = browserFragment.safeBaseActivityRef!! as MotherActivity
	
	// Builder instance used to show and configure the popup UI
	private lateinit var popupBuilder: PopupBuilder
	
	// Initialization block sets up the popup UI and its click handlers
	init {
		setupPopupBuilder()
		setupClickEvents()
	}
	
	/**
	 * Displays the popup menu with all the browser options.
	 */
	fun show() {
		val browserFragmentBody = browserFragment.browserFragmentBody
		val webviewEngine = browserFragmentBody.webviewEngine
		val focusedView = webviewEngine.currentWebView
		hideOnScreenKeyboard(safeMotherActivityRef, focusedView)
		safeMotherActivityRef.sideNavigation?.closeDrawerNavigation()
		popupBuilder.show()
	}
	
	/**
	 * Closes the popup menu.
	 */
	fun close() {
		popupBuilder.close()
	}
	
	/**
	 * Initializes the popup builder with layout and anchor information.
	 */
	private fun setupPopupBuilder() {
		popupBuilder = PopupBuilder(
			activityInf = safeMotherActivityRef,
			popupLayoutId = R.layout.frag_brow_1_top_popup_1,
			popupAnchorView = browserFragment.browserFragmentTop.webviewOptionPopup
		)
	}
	
	/**
	 * Binds click listeners for each option in the popup.
	 * Each button triggers a specific browser-related action.
	 */
	private fun setupClickEvents() {
		popupBuilder.getPopupView().apply {
			val clickActions = mapOf(
				findViewById<View>(R.id.button_webpage_back) to { goToPreviousWebpage() },
				findViewById<View>(R.id.button_webpage_forward) to { goToNextWebpage() },
				findViewById<View>(R.id.button_save_bookmark) to { saveCurrentWebpageAsBookmark() },
				findViewById<View>(R.id.button_share_webpage_url) to { shareCurrentWebpageURL() },
				findViewById<View>(R.id.button_copy_webpage_url) to { copyCurrentWebpageURL() },
				findViewById<View>(R.id.button_open_with_system_browser) to { openWebpageInSystemBrowser() },
				findViewById<View>(R.id.button_add_download_task_editor) to { openNewDownloadTaskEditor() },
				findViewById<View>(R.id.button_open_bookmark) to { openBookmarkActivity() },
				findViewById<View>(R.id.button_open_history) to { openHistoryActivity() },
				findViewById<View>(R.id.button_how_to_use) to { openHowToDownload() },
				findViewById<View>(R.id.button_open_feedback) to { openFeedbackActivity() }
			)
			
			clickActions.forEach { (view, action) ->
				view.setOnClickListener { action() }
			}
		}
	}
	
	/**
	 * Navigates the current webview to the previous page if available.
	 */
	private fun goToPreviousWebpage() {
		close()
		val webviewEngine = browserFragment.getBrowserWebEngine()
		webviewEngine.currentWebView?.let { webView ->
			if (webView.canGoBack()) {
				webView.goBack()
			} else {
				safeMotherActivityRef.doSomeVibration(20)
				showToast(msgId = R.string.text_reached_limit_for_going_back)
			}
		}
	}
	
	/**
	 * Navigates the current webview to the next page if available.
	 */
	private fun goToNextWebpage() {
		close()
		browserFragment.getBrowserWebEngine().currentWebView?.let { webView ->
			if (webView.canGoForward()) {
				webView.goForward()
			} else {
				safeMotherActivityRef.doSomeVibration(20)
				showToast(msgId = R.string.text_reached_limit_for_going_forward)
			}
		}
	}
	
	/**
	 * Saves the current webpage as a bookmark.
	 */
	private fun saveCurrentWebpageAsBookmark() {
		close()
		try {
			val currentWebpageUrl = getCurrentWebpageURL()
			val currentWebpageTitle = getCurrentWebpageTitle()
			if (currentWebpageUrl.isNullOrEmpty()) {
				safeMotherActivityRef.doSomeVibration(20)
				showToast(msgId = R.string.text_invalid_webpage_url)
				return
			}
			aioBookmark.getBookmarkLibrary().add(0, BookmarkModel().apply {
				bookmarkCreationDate = Date()
				bookmarkModifiedDate = Date()
				bookmarkUrl = currentWebpageUrl
				bookmarkName = if (currentWebpageTitle.isNullOrEmpty())
					getText(R.string.title_unknown) else currentWebpageTitle
			})
			
			aioBookmark.updateInStorage()
			showToast(msgId = R.string.text_bookmark_saved)
		} catch (error: Exception) {
			error.printStackTrace()
			safeMotherActivityRef.doSomeVibration(20)
			showToast(msgId = R.string.text_something_went_wrong)
		}
	}
	
	/**
	 * Opens a system share dialog to share the current webpage URL.
	 */
	private fun shareCurrentWebpageURL() {
		close()
		try {
			val currentWebviewUrl = getCurrentWebpageURL()
			if (currentWebviewUrl == null) {
				safeMotherActivityRef.doSomeVibration(20)
				showToast(msgId = R.string.text_invalid_webpage_url)
				return
			}
			
			val shareIntent = Intent().apply {
				action = ACTION_SEND
				putExtra(EXTRA_TEXT, currentWebviewUrl)
				type = "text/plain"
			}
			
			safeMotherActivityRef.startActivity(
				Intent.createChooser(shareIntent,
					safeMotherActivityRef.getString(R.string.title_share_with_others)))
		} catch (error: Exception) {
			error.printStackTrace()
			safeMotherActivityRef.doSomeVibration(20)
			showToast(msgId = R.string.text_something_went_wrong)
		}
	}
	
	/**
	 * Copies the current webpage URL to clipboard.
	 */
	private fun copyCurrentWebpageURL() {
		close()
		try {
			val currentWebpageUrl = getCurrentWebpageURL()
			if (currentWebpageUrl.isNullOrEmpty()) {
				safeMotherActivityRef.doSomeVibration(20)
				showToast(msgId = R.string.text_invalid_webpage_url)
				return
			}
			
			copyTextToClipboard(safeMotherActivityRef, currentWebpageUrl)
			showToast(msgId = R.string.text_copied_to_clipboard)
		} catch (error: Exception) {
			error.printStackTrace()
			safeMotherActivityRef.doSomeVibration(20)
			showToast(msgId = R.string.text_something_went_wrong)
		}
	}
	
	/**
	 * Opens the current URL using an external browser app.
	 */
	private fun openWebpageInSystemBrowser() {
		try {
			val fileUrl: String? = getCurrentWebpageURL()
			if (!fileUrl.isNullOrEmpty() && URLUtility.isValidURL(fileUrl)) {
				val intent = Intent(Intent.ACTION_VIEW, fileUrl.toUri()).apply {
					addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
				}; safeMotherActivityRef.startActivity(intent)
			} else showInvalidUrlToast()
		} catch (err: Exception) {
			err.printStackTrace()
			showToast(msgId = R.string.text_please_install_web_browser)
		}
	}
	
	/**
	 * Shows a toast indicating that the URL is invalid.
	 */
	private fun showInvalidUrlToast() {
		showToast(msgId = R.string.text_invalid_url)
	}
	
	/**
	 * Opens a dialog to manually add a new video/download task.
	 */
	private fun openNewDownloadTaskEditor() {
		close().let { VideoLinkPasteEditor(safeMotherActivityRef).show() }
	}
	
	/**
	 * Shows the platform picker guide on how to download videos.
	 */
	private fun openHowToDownload() {
		close()
		GuidePlatformPicker(safeMotherActivityRef).show()
	}
	
	/**
	 * Launches the bookmarks activity.
	 */
	private fun openBookmarkActivity() {
		close()
		val input = Intent(safeMotherActivityRef, BookmarksActivity::class.java)
		safeMotherActivityRef.resultLauncher.launch(input)
	}
	
	/**
	 * Launches the browsing history activity.
	 */
	private fun openHistoryActivity() {
		close()
		val input = Intent(safeMotherActivityRef, HistoryActivity::class.java)
		safeMotherActivityRef.resultLauncher.launch(input)
	}
	
	/**
	 * Opens the feedback activity for user input.
	 */
	private fun openFeedbackActivity() {
		close()
		safeMotherActivityRef.openActivity(UserFeedbackActivity::class.java, false)
	}
	
	/**
	 * Gets the URL of the current page displayed in the webview.
	 */
	private fun getCurrentWebpageURL(): String? {
		val browserFragmentBody = browserFragment.browserFragmentBody
		val webviewEngine = browserFragmentBody.webviewEngine
		val url = webviewEngine.currentWebView?.url
		return url
	}
	
	/**
	 * Gets the title of the current page displayed in the webview.
	 */
	private fun getCurrentWebpageTitle(): String? {
		val browserFragmentBody = browserFragment.browserFragmentBody
		val webviewEngine = browserFragmentBody.webviewEngine
		val title = webviewEngine.currentWebView?.title
		return title
	}
}
