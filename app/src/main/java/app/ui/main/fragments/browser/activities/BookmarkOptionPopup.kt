package app.ui.main.fragments.browser.activities

import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.createChooser
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.core.AIOApp.Companion.aioBookmark
import app.core.engines.browser.bookmarks.BookmarkModel
import com.aio.R
import lib.texts.ClipboardUtils.copyTextToClipboard
import lib.texts.CommonTextUtils.getText
import lib.ui.builders.PopupBuilder
import lib.ui.builders.ToastView.Companion.showToast
import java.lang.ref.WeakReference

/**
 * BookmarkOptionPopup is a contextual popup UI for handling actions related to a single bookmark.
 *
 * It allows the user to perform operations such as:
 * - Opening the bookmark
 * - Sharing the bookmark URL
 * - Copying the bookmark to clipboard
 * - Deleting the bookmark
 *
 * This popup is lifecycle-aware and tied to a BookmarkActivity lifecycle.
 *
 * @property bookmarksActivity Reference to the hosting activity
 * @property bookmarkModel The bookmark model to operate on
 * @property listView The view used to anchor the popup menu
 */
class BookmarkOptionPopup(
	private val bookmarksActivity: BookmarksActivity,
	private val bookmarkModel: BookmarkModel,
	private val listView: View
) : DefaultLifecycleObserver {
	
	private val safeBookmarksActivityRef = WeakReference(bookmarksActivity).get()
	private val safeBookmarkListViewRef = WeakReference(listView).get()
	private var popupBuilder: PopupBuilder? = null
	
	init {
		// Register lifecycle observer to cleanup when activity is destroyed
		safeBookmarksActivityRef?.lifecycle?.addObserver(this)
		initializePopup()
	}
	
	/**
	 * Show the bookmark popup menu.
	 */
	fun show() {
		popupBuilder?.show()
	}
	
	/**
	 * Close the bookmark popup menu.
	 */
	fun close() {
		popupBuilder?.close()
	}
	
	/**
	 * Automatically called when the lifecycle owner is destroyed.
	 */
	override fun onDestroy(owner: LifecycleOwner) {
		cleanup()
	}
	
	/**
	 * Initializes the PopupBuilder with a layout and anchor view,
	 * and sets up button click handlers.
	 */
	private fun initializePopup() {
		safeBookmarksActivityRef?.let { activityRef ->
			safeBookmarkListViewRef?.let { listViewRef ->
				popupBuilder = PopupBuilder(
					activityInf = activityRef,
					popupLayoutId = R.layout.activity_bookmarks_1_option_1,
					popupAnchorView = listViewRef.findViewById(R.id.bookmark_url_open_indicator)
				).apply { initializePopupButtons(getPopupView()) }
			}
		}
	}
	
	/**
	 * Initialize all button actions inside the popup view.
	 */
	private fun initializePopupButtons(popupView: View?) {
		popupView?.apply {
			findViewById<View>(R.id.btn_open_bookmark)
				.setOnClickListener { closeAndCleanup { openBookmarkInBrowser() } }
			findViewById<View>(R.id.button_share_bookmark)
				.setOnClickListener { closeAndCleanup { shareBookmarkLink() } }
			findViewById<View>(R.id.button_copy_bookmark)
				.setOnClickListener { closeAndCleanup { copyBookmarkInClipboard() } }
			findViewById<View>(R.id.button_delete_bookmark)
				.setOnClickListener { closeAndCleanup { deleteBookmarkFromLibrary() } }
		}
	}
	
	/**
	 * Close the popup and run the optional cleanup action.
	 */
	private fun closeAndCleanup(action: (() -> Unit)? = null) {
		close()
		action?.invoke()
		cleanup()
	}
	
	/**
	 * Copy the bookmark URL to clipboard and show a toast.
	 */
	private fun copyBookmarkInClipboard() {
		safeBookmarksActivityRef?.let { activity ->
			copyTextToClipboard(activity, bookmarkModel.bookmarkUrl)
			showToast(msgId = R.string.text_copied_url_to_clipboard)
		}
	}
	
	/**
	 * Trigger the activity's handler to open the bookmark.
	 */
	private fun openBookmarkInBrowser() {
		safeBookmarksActivityRef?.onBookmarkClick(bookmarkModel)
	}
	
	/**
	 * Delete the bookmark from the library and update the adapter.
	 */
	private fun deleteBookmarkFromLibrary() {
		safeBookmarksActivityRef?.let { safeMotherActivityRef ->
			try {
				aioBookmark.getBookmarkLibrary().remove(bookmarkModel)
				aioBookmark.updateInStorage()
				safeMotherActivityRef.updateBookmarkListAdapter()
				showToast(msgId = R.string.title_successful)
			} catch (error: Exception) {
				safeMotherActivityRef.doSomeVibration(20)
				showToast(msgId = R.string.text_something_went_wrong)
			}
		}
	}
	
	/**
	 * Share the bookmark URL with other apps using a share intent.
	 */
	private fun shareBookmarkLink() {
		safeBookmarksActivityRef?.let { safeMotherActivityRef ->
			try {
				val bookmarkUrl = bookmarkModel.bookmarkUrl
				val shareIntent = Intent().apply {
					action = ACTION_SEND
					putExtra(EXTRA_TEXT, bookmarkUrl)
					type = "text/plain"
				}
				val intentChooser = createChooser(shareIntent, getText(R.string.title_share_with_others))
				safeMotherActivityRef.startActivity(intentChooser)
			} catch (error: Exception) {
				safeMotherActivityRef.doSomeVibration(20)
				showToast(msgId = R.string.text_something_went_wrong)
			}
		}
	}
	
	/**
	 * Nullify the popup builder reference to free memory.
	 */
	private fun cleanup() {
		popupBuilder = null
	}
}