package app.ui.main.fragments.browser.activities

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.View.GONE
import android.widget.ListView
import app.core.AIOApp
import app.core.AIOApp.Companion.admobHelper
import app.core.AIOApp.Companion.aioBookmark
import app.core.AIOApp.Companion.aioTimer
import app.core.AIOTimer.AIOTimerListener
import app.core.bases.BaseActivity
import app.core.engines.browser.bookmarks.BookmarkModel
import app.ui.main.MotherActivity.Companion.ACTIVITY_RESULT_KEY
import app.ui.main.fragments.browser.activities.BookmarkAdapter.OnBookmarkItemClick
import app.ui.main.fragments.browser.activities.BookmarkAdapter.OnBookmarkItemLongClick
import com.aio.R
import com.google.android.gms.ads.AdView
import lib.process.CommonTimeUtils
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.ui.MsgDialogUtils
import lib.ui.ViewUtility.hideView
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.ViewUtility.showView
import lib.ui.builders.ToastView.Companion.showToast
import java.lang.ref.WeakReference

/**
 * Activity to display and manage user's saved bookmarks.
 * Provides functionality to view, open, delete, and load more bookmarks.
 */
class BookmarksActivity : BaseActivity(),
	AIOTimerListener, OnBookmarkItemClick, OnBookmarkItemLongClick {
	
	// Weak reference to the activity to prevent memory leaks
	private val weakSelfReference = WeakReference(this)
	private val safeBookmarksActivityRef = WeakReference(this).get()
	
	// UI elements
	private lateinit var emptyBookmarksIndicator: View
	private lateinit var bookmarkListView: ListView
	private lateinit var buttonLoadMoreBookmarks: View
	
	// Popup for bookmark actions (edit, delete, etc.)
	private var bookmarkOptionPopup: BookmarkOptionPopup? = null
	
	// Adapter to display bookmarks in a list view
	private val bookmarksAdapter by lazy {
		val activityRef = safeBookmarksActivityRef
		BookmarkAdapter(activityRef, activityRef, activityRef)
	}
	
	/** Specifies the layout to use for this activity */
	override fun onRenderingLayout(): Int {
		return R.layout.activity_bookmarks_1
	}
	
	/** Called after the layout is rendered. Initializes views and click events. */
	override fun onAfterLayoutRender() {
		initializeViews()
		initializeViewsOnClickEvents()
	}
	
	/** Registers timer and updates bookmark list when activity resumes. */
	override fun onResumeActivity() {
		super.onResumeActivity()
		registerToAIOTimer()
		updateBookmarkListAdapter()
	}
	
	/** Unregisters timer when activity is paused. */
	override fun onPauseActivity() {
		super.onPauseActivity()
		unregisterToAIOTimer()
	}
	
	/** Handles back press with fade-out animation. */
	override fun onBackPressActivity() {
		closeActivityWithFadeAnimation(false)
	}
	
	/** Cleans up resources and unregisters listeners on activity destroy. */
	override fun onDestroy() {
		unregisterToAIOTimer()
		bookmarkListView.adapter = null
		bookmarkOptionPopup?.close()
		bookmarkOptionPopup = null
		super.onDestroy()
	}
	
	/** Called periodically by AIOTimer. Used to update UI elements like the Load More button. */
	override fun onAIOTimerTick(loopCount: Double) {
		updateLoadMoreButtonVisibility()
	}
	
	/** Called when a bookmark is clicked. Opens the URL in browser. */
	override fun onBookmarkClick(bookmarkModel: BookmarkModel) {
		openBookmarkInBrowser(bookmarkModel)
	}
	
	/** Called when a bookmark is long-pressed. Shows context menu popup. */
	override fun onBookmarkLongClick(
		bookmarkModel: BookmarkModel,
		position: Int, listView: View
	) {
		safeBookmarksActivityRef?.let { safeBookmarkActivityRef ->
			try {
				bookmarkOptionPopup = null
				bookmarkOptionPopup = BookmarkOptionPopup(
					bookmarksActivity = safeBookmarkActivityRef,
					bookmarkModel = bookmarkModel,
					listView = listView
				).apply { show() }
			} catch (error: Exception) {
				error.printStackTrace()
				showToast(msgId = R.string.text_something_went_wrong)
			}
		}
	}
	
	/**
	 * Clears the weak reference to this activity.
	 * Ensures that memory is released when activity is destroyed.
	 */
	override fun clearWeakActivityReference() {
		weakSelfReference.clear()
		super.clearWeakActivityReference()
	}
	
	/** Initializes views and sets adapter for the bookmark list. */
	private fun initializeViews() {
		safeBookmarksActivityRef?.let {
			emptyBookmarksIndicator = findViewById(R.id.empty_bookmarks_indicator)
			bookmarkListView = findViewById(R.id.list_bookmarks)
			buttonLoadMoreBookmarks = findViewById(R.id.button_load_more_bookmarks)
			
			// Set adapter and hide views initially
			bookmarkListView.adapter = bookmarksAdapter
			bookmarkListView.visibility = GONE
			
			emptyBookmarksIndicator.visibility = GONE
			buttonLoadMoreBookmarks.visibility = GONE
			updateLoadMoreButtonVisibility()
			
			// Set up AdMob banner if user is not premium
			val admobView: AdView = findViewById(R.id.admob_fixed_sized_banner_ad)
			admobHelper.loadBannerAd(admobView)
			
			// Hide ad space for premium users
			if (AIOApp.IS_PREMIUM_USER) {
				findViewById<View>(R.id.ad_space_container).visibility = GONE
			}
		}
	}
	
	/** Sets up click listeners for buttons. */
	private fun initializeViewsOnClickEvents() {
		findViewById<View>(R.id.button_left_actionbar)
			.setOnClickListener { onBackPressActivity() }
		
		findViewById<View>(R.id.button_right_actionbar)
			.setOnClickListener { deleteAllBookmarks() }
		
		buttonLoadMoreBookmarks.setOnClickListener {
			bookmarksAdapter.loadMoreBookmarks()
			showToast(msgId = R.string.text_loaded_successfully)
		}
	}
	
	/** Shows confirmation dialog and deletes all bookmarks if user confirms. */
	private fun deleteAllBookmarks() {
		MsgDialogUtils.getMessageDialog(
			baseActivityInf = safeBookmarksActivityRef,
			isTitleVisible = true,
			titleTextViewCustomize =
				{ it.setText(R.string.title_are_you_sure_about_this) },
			messageTxt = getText(R.string.text_delete_bookmarks_confirmation),
			isNegativeButtonVisible = false,
			positiveButtonTextCustomize = {
				it.setText(R.string.title_clear_now)
				it.setLeftSideDrawable(R.drawable.ic_button_clear)
			}
		)?.apply {
			setOnClickForPositiveButton {
				close()
				aioBookmark.getBookmarkLibrary().clear()
				aioBookmark.updateInStorage()
				bookmarksAdapter.resetBookmarkAdapter()
			}
		}?.show()
	}
	
	/** Registers this activity to receive AIOTimer ticks. */
	private fun registerToAIOTimer() {
		safeBookmarksActivityRef?.let { aioTimer.register(it) }
	}
	
	/** Unregisters from AIOTimer to prevent leaks or unnecessary updates. */
	private fun unregisterToAIOTimer() {
		safeBookmarksActivityRef?.let { aioTimer.unregister(it) }
	}
	
	/** Resets and reloads the bookmark adapter data. */
	fun updateBookmarkListAdapter() {
		bookmarksAdapter.resetBookmarkAdapter()
		bookmarksAdapter.loadMoreBookmarks()
	}
	
	/** Shows/hides Load More button and empty state based on list content. */
	private fun updateLoadMoreButtonVisibility() {
		val bookmarkSize = aioBookmark.getBookmarkLibrary().size
		if (bookmarksAdapter.count >= bookmarkSize) hideView(buttonLoadMoreBookmarks, true)
		else {
			showView(buttonLoadMoreBookmarks, true)
		}
		
		if (bookmarksAdapter.isEmpty) showView(emptyBookmarksIndicator, true)
		else {
			hideView(emptyBookmarksIndicator, true).let {
				CommonTimeUtils.delay(500, object : OnTaskFinishListener {
					override fun afterDelay() {
						showView(bookmarkListView, true)
					}
				})
			}
		}
	}
	
	/**
	 * Opens the selected bookmark in the browser and closes the activity.
	 *
	 * @param bookmarkModel The selected bookmark.
	 */
	private fun openBookmarkInBrowser(bookmarkModel: BookmarkModel) {
		sendResultBackToBrowserFragment(bookmarkModel.bookmarkUrl)
		onBackPressActivity()
	}
	
	/**
	 * Sends the selected bookmark URL back to the browser fragment via intent.
	 *
	 * @param result The URL of the selected bookmark.
	 */
	fun sendResultBackToBrowserFragment(result: String) {
		setResult(Activity.RESULT_OK, Intent().apply {
			putExtra(ACTIVITY_RESULT_KEY, result)
		}).let { onBackPressActivity() }
	}
}