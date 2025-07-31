package app.ui.main.fragments.browser.activities

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.View.GONE
import android.widget.ListView
import app.core.AIOApp.Companion.aioHistory
import app.core.AIOApp.Companion.aioTimer
import app.core.AIOTimer.AIOTimerListener
import app.core.bases.BaseActivity
import app.core.engines.browser.history.HistoryModel
import app.ui.main.MotherActivity.Companion.ACTIVITY_RESULT_KEY
import app.ui.main.fragments.browser.activities.HistoryAdapter.OnHistoryItemClick
import app.ui.main.fragments.browser.activities.HistoryAdapter.OnHistoryItemLongClick
import com.aio.R
import lib.process.CommonTimeUtils
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.ui.MsgDialogUtils
import lib.ui.ViewUtility.hideView
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.ViewUtility.showView
import lib.ui.builders.ToastView.Companion.showToast
import java.lang.ref.WeakReference

/**
 * Activity for displaying and managing browser history.
 */
class HistoryActivity : BaseActivity(),
	AIOTimerListener, OnHistoryItemClick, OnHistoryItemLongClick {
	
	// WeakReference to avoid memory leaks
	private val safeHistoryActivityRef = WeakReference(this).get()
	
	// UI elements
	private lateinit var emptyHistoryIndicator: View
	private lateinit var historyList: ListView
	private lateinit var buttonLoadMoreHistory: View
	
	// Popup menu for history item options
	private var historyOptionPopup: HistoryOptionPopup? = null
	
	// Adapter for managing history data in the list
	private val arg = safeHistoryActivityRef
	private val historyAdapter by lazy { HistoryAdapter(arg, arg, arg) }
	
	/**
	 * Provides the layout resource for this activity.
	 */
	override fun onRenderingLayout(): Int {
		return R.layout.activity_browser_history_1
	}
	
	/**
	 * Called after the layout has been rendered. Initializes views and click handlers.
	 */
	override fun onAfterLayoutRender() {
		initializeViews()
		initializeViewsOnClickEvents()
	}
	
	/**
	 * Called when the activity is resumed. Registers the timer and updates the adapter.
	 */
	override fun onResumeActivity() {
		super.onResumeActivity()
		registerToAIOTimer()
		updateHistoryListAdapter()
	}
	
	/**
	 * Called when the activity is paused. Unregisters from the timer.
	 */
	override fun onPauseActivity() {
		super.onPauseActivity()
		unregisterToAIOTimer()
	}
	
	/**
	 * Handles back press logic.
	 */
	override fun onBackPressActivity() {
		closeActivityWithFadeAnimation(false)
	}
	
	/**
	 * Cleans up resources on activity destroy.
	 */
	override fun onDestroy() {
		unregisterToAIOTimer()
		historyList.adapter = null
		historyOptionPopup?.close()
		historyOptionPopup = null
		super.onDestroy()
	}
	
	/**
	 * Called on each AIOTimer tick. Used to manage visibility of the "Load More" button.
	 */
	override fun onAIOTimerTick(loopCount: Double) {
		updateLoadMoreButtonVisibility()
	}
	
	/**
	 * Handles click events on a history item.
	 */
	override fun onHistoryItemClick(historyModel: HistoryModel) {
		openHistoryInBrowser(historyModel)
	}
	
	/**
	 * Handles long-click events on a history item. Shows the option popup.
	 */
	override fun onHistoryItemLongClick(historyModel: HistoryModel, position: Int, listView: View) {
		safeHistoryActivityRef?.let { safeActivityRef ->
			try {
				historyOptionPopup = HistoryOptionPopup(
					historyActivity = safeActivityRef,
					historyModel = historyModel,
					listView = listView
				)
				historyOptionPopup?.show()
			} catch (error: Exception) {
				error.printStackTrace()
				showToast(msgId = R.string.text_something_went_wrong)
			}
		}
	}
	
	/**
	 * Initializes views and sets the adapter.
	 */
	private fun initializeViews() {
		safeHistoryActivityRef?.let {
			emptyHistoryIndicator = findViewById(R.id.empty_history_indicator)
			historyList = findViewById(R.id.list_history)
			buttonLoadMoreHistory = findViewById(R.id.btn_load_more_history)
			
			// Set adapter and hide views initially
			historyList.adapter = historyAdapter
			historyList.visibility = GONE
			
			emptyHistoryIndicator.visibility = GONE
			buttonLoadMoreHistory.visibility = GONE
			updateLoadMoreButtonVisibility()
		}
	}
	
	/**
	 * Sets up click event listeners for UI elements.
	 */
	private fun initializeViewsOnClickEvents() {
		findViewById<View>(R.id.btn_left_actionbar)
			.setOnClickListener { onBackPressActivity() }
		
		findViewById<View>(R.id.btn_right_actionbar)
			.setOnClickListener { deleteAllHistory() }
		
		buttonLoadMoreHistory.setOnClickListener {
			historyAdapter.loadMoreHistory()
			showToast(msgId = R.string.text_loaded_successfully)
		}
	}
	
	/**
	 * Displays a confirmation dialog and clears all browsing history if confirmed.
	 */
	private fun deleteAllHistory() {
		MsgDialogUtils.getMessageDialog(
			baseActivityInf = safeHistoryActivityRef,
			isTitleVisible = true,
			titleTextViewCustomize =
				{ it.setText(R.string.title_are_you_sure_about_this) },
			messageTxt = getText(R.string.text_delete_browsing_history_confirmation),
			isNegativeButtonVisible = false,
			positiveButtonTextCustomize = {
				it.setText(R.string.title_clear_now)
				it.setLeftSideDrawable(R.drawable.ic_button_clear)
			})?.apply {
			setOnClickForPositiveButton {
				close()
				aioHistory.getHistoryLibrary().clear()
				aioHistory.updateInStorage()
				historyAdapter.resetHistoryAdapter()
			}
		}?.show()
	}
	
	/**
	 * Registers this activity to receive timer tick events.
	 */
	private fun registerToAIOTimer() {
		safeHistoryActivityRef?.let { aioTimer.register(it) }
	}
	
	/**
	 * Unregisters this activity from receiving timer tick events.
	 */
	private fun unregisterToAIOTimer() {
		safeHistoryActivityRef?.let { aioTimer.unregister(it) }
	}
	
	/**
	 * Resets and reloads the history adapter.
	 */
	fun updateHistoryListAdapter() {
		historyAdapter.resetHistoryAdapter()
		historyAdapter.loadMoreHistory()
	}
	
	/**
	 * Controls visibility of the "Load More" button and empty state indicator.
	 */
	private fun updateLoadMoreButtonVisibility() {
		val historySize = aioHistory.getHistoryLibrary().size
		if (historyAdapter.count >= historySize) hideView(buttonLoadMoreHistory, true)
		else {
			showView(buttonLoadMoreHistory, true)
		}
		
		if (historyAdapter.isEmpty) showView(emptyHistoryIndicator, true)
		else {
			hideView(emptyHistoryIndicator, true).let {
				CommonTimeUtils.delay(500, object: OnTaskFinishListener {
					override fun afterDelay() {
						showView(historyList, true)
					}
				})
			}
		}
	}
	
	/**
	 * Sends the clicked history item's URL back to the browser fragment.
	 */
	private fun openHistoryInBrowser(historyModel: HistoryModel) {
		sendResultBackToBrowserFragment(historyModel.historyUrl)
		onBackPressActivity()
	}
	
	/**
	 * Passes selected result to parent activity and finishes this activity.
	 */
	fun sendResultBackToBrowserFragment(result: String) {
		setResult(Activity.RESULT_OK, Intent().apply {
			putExtra(ACTIVITY_RESULT_KEY, result)
		}).let { onBackPressActivity() }
	}
}