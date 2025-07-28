package app.ui.main.fragments.browser.activities

import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.createChooser
import android.view.View
import android.webkit.CookieManager
import app.core.AIOApp.Companion.aioBookmark
import app.core.AIOApp.Companion.aioHistory
import app.core.engines.browser.bookmarks.BookmarkModel
import app.core.engines.browser.history.HistoryModel
import com.aio.R
import lib.texts.ClipboardUtils.copyTextToClipboard
import lib.texts.CommonTextUtils.getText
import lib.ui.builders.PopupBuilder
import lib.ui.builders.ToastView.Companion.showToast
import java.lang.ref.WeakReference
import java.util.Date

/**
 * Popup class for providing various options on a selected history item.
 *
 * Available options:
 * - Open history URL in browser
 * - Share history URL
 * - Copy URL to clipboard
 * - Add history item to bookmarks
 * - Delete history entry
 */
class HistoryOptionPopup(
	private val historyActivity: HistoryActivity?,
	private val historyModel: HistoryModel,
	private val listView: View
) {
	// Weak references to prevent memory leaks
	private val safeHistoryActivityRef = WeakReference(historyActivity).get()
	private val safeHistoryListView = WeakReference(listView).get()
	
	// Builder for displaying the popup menu
	private var popupBuilder: PopupBuilder? = null
	
	init {
		safeHistoryActivityRef?.let { activityRef ->
			safeHistoryListView?.let { listViewRef ->
				// Initialize popup with custom layout and anchor view
				popupBuilder = PopupBuilder(
					activityInf = activityRef,
					popupLayoutId = R.layout.activity_browser_history_1_option_1,
					popupAnchorView = listViewRef.findViewById(R.id.history_url_open_indicator)
				)
				
				popupBuilder?.let { builder ->
					// Set up click listeners for popup buttons
					builder.getPopupView().apply {
						val buttonActions = mapOf(
							R.id.button_open_history to { close(); openHistoryInBrowser() },
							R.id.button_share_history to { close(); shareHistoryLink() },
							R.id.button_copy_history to { close(); copyHistoryToClipboard() },
							R.id.button_add_to_bookmark to { close(); addHistoryToBookmark() },
							R.id.button_delete_history to { close(); deleteHistoryFromLibrary() }
						)
						
						// Attach click actions for each button
						buttonActions.forEach { (id, action) ->
							findViewById<View>(id).setOnClickListener { action() }
						}
					}
				}
			}
		}
	}
	
	/**
	 * Displays the popup.
	 */
	fun show() {
		popupBuilder?.show()
	}
	
	/**
	 * Closes the popup if visible.
	 */
	fun close() {
		popupBuilder?.close()
	}
	
	/**
	 * Copies the history URL to the clipboard and shows a confirmation toast.
	 */
	private fun copyHistoryToClipboard() {
		copyTextToClipboard(safeHistoryActivityRef, historyModel.historyUrl)
		showToast(msgId = R.string.text_copied_url_to_clipboard)
	}
	
	/**
	 * Opens the selected history URL in the browser.
	 */
	private fun openHistoryInBrowser() {
		safeHistoryActivityRef?.onHistoryItemClick(historyModel)
	}
	
	/**
	 * Removes the selected history entry from the history library,
	 * updates storage, and refreshes the history list.
	 */
	private fun deleteHistoryFromLibrary() {
		try {
			aioHistory.getHistoryLibrary().remove(historyModel)
			safeHistoryActivityRef?.updateHistoryListAdapter()
			aioHistory.updateInStorage()
			
			// Clear the browser cookies
			val cookieManager = CookieManager.getInstance()
			cookieManager.removeAllCookies(null)
			cookieManager.flush()
			
			showToast(msgId = R.string.title_successful)
		} catch (error: Exception) {
			error.printStackTrace()
			safeHistoryActivityRef?.doSomeVibration(20)
			showToast(msgId = R.string.text_something_went_wrong)
		}
	}
	
	/**
	 * Shares the selected history URL using the system share sheet.
	 */
	private fun shareHistoryLink() {
		try {
			val shareIntent = Intent(ACTION_SEND).apply {
				putExtra(EXTRA_TEXT, historyModel.historyUrl)
				type = "text/plain"
			}
			
			val titleText = getText(R.string.title_share_with_others)
			safeHistoryActivityRef?.startActivity(createChooser(shareIntent, titleText))
		} catch (error: Exception) {
			error.printStackTrace()
			safeHistoryActivityRef?.doSomeVibration(20)
			showToast(msgId = R.string.text_something_went_wrong)
		}
	}
	
	/**
	 * Adds the selected history URL to bookmarks with a timestamp.
	 */
	private fun addHistoryToBookmark() {
		try {
			// Create a new bookmark entry based on history data
			val bookmarkModel = BookmarkModel().apply {
				bookmarkCreationDate = Date()
				bookmarkModifiedDate = Date()
				bookmarkUrl = historyModel.historyUrl
				bookmarkName = historyModel.historyTitle.ifEmpty { getText(R.string.title_unknown) }
			}
			
			// Add to bookmark library and update storage
			aioBookmark.getBookmarkLibrary().add(0, bookmarkModel)
			aioBookmark.updateInStorage()
			showToast(msgId = R.string.text_bookmark_saved)
		} catch (error: Exception) {
			error.printStackTrace()
			safeHistoryActivityRef?.doSomeVibration(20)
			showToast(msgId = R.string.text_something_went_wrong)
		}
	}
}