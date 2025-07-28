package app.ui.main.fragments.downloads.fragments.active

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import app.core.AIOApp
import app.core.AIOApp.Companion.downloadSystem
import app.core.AIOTimer.AIOTimerListener
import app.core.bases.BaseFragment
import app.core.engines.downloader.DownloadDataModel
import app.ui.main.MotherActivity
import app.ui.main.fragments.downloads.DownloadsFragment
import com.aio.R
import java.lang.ref.WeakReference

/**
 * Fragment that displays and manages currently active downloads.
 *
 * This fragment:
 * - Shows a list of ongoing downloads
 * - Registers with the download system to receive updates
 * - Handles timer-based checks for completed downloads
 * - Provides options for interacting with active downloads
 * - Automatically switches to finished downloads when all complete
 *
 * Inherits from BaseFragment for common fragment functionality
 * Implements AIOTimerListener for periodic updates
 */
open class ActiveTasksFragment : BaseFragment(), AIOTimerListener {
	
	// Weak references to avoid memory leaks
	open val safeMotherActivityRef by lazy {
		WeakReference(safeBaseActivityRef as MotherActivity).get()
	}
	
	open val safeActiveTasksFragmentRef by lazy { WeakReference(this).get() }
	
	// Options dialog for active download items
	private val activeTasksOptions by lazy {
		ActiveTasksOptions(motherActivity = safeMotherActivityRef)
	}
	
	// Container view for the list of active downloads
	open val activeTasksListContainer: LinearLayout? by lazy {
		safeFragmentLayoutRef?.findViewById(R.id.container_download_tasks_queue)
	}
	
	/**
	 * Provides the layout resource ID for this fragment
	 * @return The layout resource ID (R.layout.frag_down_3_active_1)
	 */
	override fun getLayoutResId(): Int {
		return R.layout.frag_down_3_active_1
	}
	
	/**
	 * Called after the fragment layout is loaded
	 * @param layoutView The inflated layout view
	 * @param state Saved instance state bundle
	 */
	override fun onAfterLayoutLoad(layoutView: View, state: Bundle?) {
		registerToDownloadSystem()
	}
	
	/**
	 * Called when the fragment resumes
	 * Registers with necessary systems and updates UI
	 */
	override fun onResumeFragment() {
		registerToDownloadSystem()
		selfRegisterToParentFragment()
		safeActiveTasksFragmentRef?.let { AIOApp.aioTimer.register(it) }
	}
	
	/**
	 * Called when the fragment pauses
	 * Unregisters from systems to prevent leaks
	 */
	override fun onPauseFragment() {
		unregisterFromDownloadSystem()
		selfRegisterToParentFragment()
		safeActiveTasksFragmentRef?.let { AIOApp.aioTimer.unregister(it) }
	}
	
	/**
	 * Called when the fragment view is destroyed
	 * Cleans up registration with download system
	 */
	override fun onDestroyView() {
		unregisterFromDownloadSystem()
		super.onDestroyView()
	}
	
	/**
	 * Timer callback that checks for completed downloads
	 * @param loopCount The current timer loop count
	 */
	override fun onAIOTimerTick(loopCount: Double) {
		// Switch to finished downloads if no active downloads remain
		if (downloadSystem.activeDownloadDataModels.isEmpty()) {
			openToFinishedTab()
		}
	}
	
	/**
	 * Opens the finished downloads tab in parent fragment
	 */
	private fun openToFinishedTab() {
		val downloadFragment = parentFragment as? DownloadsFragment
		downloadFragment?.openFinishedTab()
	}
	
	/**
	 * Registers this fragment with the download system for UI updates
	 */
	private fun registerToDownloadSystem() {
		// Clear previous registration before re-registering
		downloadSystem.downloadsUIManager.activeTasksFragment = null
		downloadSystem.downloadsUIManager.activeTasksFragment = safeActiveTasksFragmentRef
		
		// Force UI refresh
		downloadSystem.downloadsUIManager.redrawEverything()
	}
	
	/**
	 * Unregisters this fragment from the download system
	 */
	private fun unregisterFromDownloadSystem() {
		downloadSystem.downloadsUIManager.activeTasksFragment = null
	}
	
	/**
	 * Handles click events for download items
	 * @param downloadModel The DownloadDataModel associated with clicked item
	 */
	fun onDownloadUIItemClick(downloadModel: DownloadDataModel) {
		activeTasksOptions.show(downloadModel)
	}
	
	/**
	 * Registers this fragment with its parent DownloadsFragment
	 * Updates the parent's title and reference to this fragment
	 */
	private fun selfRegisterToParentFragment() {
		val downloadFragment = parentFragment as? DownloadsFragment
		// Update parent's reference to this fragment
		downloadFragment?.activeTasksFragment = safeActiveTasksFragmentRef
		// Update title in parent fragment
		downloadFragment?.safeFragmentLayoutRef?.let {
			val title = it.findViewById<TextView>(R.id.text_current_frag_name)
			title?.setText(R.string.title_active_downloads)
		}
	}
}