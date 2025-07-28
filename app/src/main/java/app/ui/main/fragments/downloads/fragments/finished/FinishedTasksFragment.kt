package app.ui.main.fragments.downloads.fragments.finished

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.isVisible
import app.core.AIOApp
import app.core.AIOApp.Companion.aioRawFiles
import app.core.AIOApp.Companion.downloadSystem
import app.core.AIOTimer.AIOTimerListener
import app.core.bases.BaseFragment
import app.core.engines.downloader.DownloadDataModel
import app.ui.main.MotherActivity
import app.ui.main.fragments.downloads.DownloadsFragment
import app.ui.main.guides.GuidePlatformPicker
import com.aio.R
import com.airbnb.lottie.LottieAnimationView
import lib.ui.ViewUtility.hideView
import lib.ui.ViewUtility.showView
import java.lang.ref.WeakReference

/**
 * Fragment representing the Finished Downloads section.
 *
 * This fragment displays a list of downloads that have finished. It also manages UI states such as:
 * - Empty view when there are no finished downloads.
 * - A button to open the Active Downloads tab if active downloads exist.
 * - A guide button to show users how to start downloading.
 *
 * It integrates with [downloadSystem], and updates UI based on changes in finished downloads.
 */
open class FinishedTasksFragment : BaseFragment(), FinishedTasksClickEvents, AIOTimerListener {
	
	/** Safe reference to the parent activity (MotherActivity). */
	open val safeMotherActivityRef by lazy { WeakReference(safeBaseActivityRef as MotherActivity).get() }
	
	/** Safe reference to this fragment. */
	open val safeFinishTasksFragment by lazy { WeakReference(this).get() }
	
	/** Options menu for finished downloads (rename, delete, etc.). */
	private val finishTaskOptions by lazy { FinishedDownloadOptions(safeFinishTasksFragment) }
	
	private lateinit var emptyDownloadContainer: View
	private lateinit var emptyDownloadAnim: LottieAnimationView
	private lateinit var buttonOpenActiveTasks: View
	private lateinit var openActiveTasksAnim: LottieAnimationView
	private lateinit var buttonHowToDownload: View
	
	private lateinit var taskListView: ListView
	
	/** Adapter for listing finished download tasks. */
	open lateinit var finishedTasksListAdapter: FinishedTasksListAdapter
	
	/** Returns the layout resource ID to be inflated. */
	override fun getLayoutResId(): Int {
		return R.layout.frag_down_4_finish_1
	}
	
	/**
	 * Called after layout is loaded. Initializes all views and registers the fragment in DownloadSystem.
	 */
	override fun onAfterLayoutLoad(layoutView: View, state: Bundle?) {
		safeFinishTasksFragment?.let {
			selfReferenceRegisterIntoDownloadSystem()
			initializeViewsAndListAdapter(layoutView)
		}
	}
	
	/**
	 * Called when the fragment becomes visible.
	 * Registers the fragment with the parent and starts the AIO timer.
	 */
	override fun onResumeFragment() {
		selfReferenceRegisterToDownloadFragment()
		safeFinishTasksFragment?.let { AIOApp.aioTimer.register(it) }
	}
	
	/**
	 * Called when the fragment goes out of view.
	 * Unregisters the AIO timer.
	 */
	override fun onPauseFragment() {
		selfReferenceRegisterToDownloadFragment()
		safeFinishTasksFragment?.let { AIOApp.aioTimer.unregister(it) }
	}
	
	/**
	 * Triggered periodically by AIO timer to update UI.
	 */
	override fun onAIOTimerTick(loopCount: Double) {
		safeFinishTasksFragment?.let {
			toggleEmptyDownloadListviewVisibility(emptyDownloadContainer, taskListView)
			toggleOpenActiveTasksButtonVisibility(buttonOpenActiveTasks)
		}
	}
	
	/** Called when a finished download is clicked. Delegates to long-click handler. */
	override fun onFinishedDownloadClick(downloadModel: DownloadDataModel) {
		onFinishedDownloadLongClick(downloadModel)
	}
	
	/** Handles long-click on a finished download by showing options. */
	override fun onFinishedDownloadLongClick(downloadModel: DownloadDataModel) {
		finishTaskOptions.show(downloadModel)
	}
	
	/** Returns a list of all finished download models from DownloadSystem. */
	open fun getFinishedDownloadModels(): ArrayList<DownloadDataModel> {
		return downloadSystem.finishedDownloadDataModels
	}
	
	/** Initializes all views and sets up the finished downloads list adapter. */
	private fun initializeViewsAndListAdapter(layoutView: View) {
		safeFinishTasksFragment?.let { safeFragmentRef ->
			emptyDownloadContainer = layoutView.findViewById(R.id.empty_downloads_container)
			emptyDownloadAnim = layoutView.findViewById(R.id.image_empty_downloads)
			loadEmptyDownloadAnimation()
			
			buttonHowToDownload = layoutView.findViewById(R.id.button_how_to_download)
			buttonHowToDownload.setOnClickListener { GuidePlatformPicker(safeMotherActivityRef).show() }
			
			buttonOpenActiveTasks = layoutView.findViewById(R.id.button_open_active_downloads)
			buttonOpenActiveTasks.setOnClickListener { openActiveTasksFragment() }
			
			openActiveTasksAnim = layoutView.findViewById(R.id.image_open_active_downloads)
			loadOpenActiveTasksAnimation()
			
			taskListView = layoutView.findViewById(R.id.container_download_tasks_finished)
			finishedTasksListAdapter = FinishedTasksListAdapter(safeFragmentRef)
			taskListView.adapter = finishedTasksListAdapter
			
			toggleEmptyDownloadListviewVisibility(emptyDownloadContainer, taskListView)
			toggleOpenActiveTasksButtonVisibility(buttonOpenActiveTasks)
		}
	}
	
	/** Opens the Active Downloads tab from parent fragment. */
	private fun openActiveTasksFragment() {
		val downloadFragment = parentFragment as? DownloadsFragment
		downloadFragment?.openActiveTab()
	}
	
	/**
	 * Shows/hides the "Open Active Tasks" button based on whether any downloads are currently active.
	 */
	private fun toggleOpenActiveTasksButtonVisibility(buttonOpenActiveTasks: View?) {
		buttonOpenActiveTasks?.let {
			if (downloadSystem.activeDownloadDataModels.size > 0) {
				if (!it.isVisible) showView(it, true, 300)
			} else {
				if (it.isVisible) hideView(it, true, 300)
			}
		}
	}
	
	/**
	 * Toggles visibility of the empty-state animation or the task list depending on whether there are finished tasks.
	 */
	private fun toggleEmptyDownloadListviewVisibility(emptyDownloadImg: View, taskListView: ListView) {
		if (downloadSystem.isInitializing) return
		if (getFinishedDownloadModels().isEmpty()) {
			hideView(taskListView, true, 100).let {
				showView(emptyDownloadImg, true, 300)
			}
		} else {
			hideView(emptyDownloadImg, true, 100).let {
				showView(taskListView, true, 300)
			}
		}; finishedTasksListAdapter.notifyDataSetChangedOnSort(false)
	}
	
	/**
	 * Registers this fragment as the current FinishedTasksFragment in the parent DownloadsFragment.
	 */
	private fun selfReferenceRegisterToDownloadFragment() {
		safeFinishTasksFragment?.let { safeFinishedDownloadFragmentRef ->
			val downloadFragment = parentFragment as? DownloadsFragment
			downloadFragment?.finishedTasksFragment = safeFinishedDownloadFragmentRef
			downloadFragment?.safeFragmentLayoutRef?.let {
				val title = it.findViewById<TextView>(R.id.text_current_frag_name)
				title?.setText(R.string.title_downloaded_files)
			}
		}
	}
	
	/**
	 * Registers this fragment into the download system's UI manager for finished downloads.
	 */
	private fun selfReferenceRegisterIntoDownloadSystem() {
		safeFinishTasksFragment?.let { safeFinishedDownloadFragmentRef ->
			downloadSystem.downloadsUIManager.finishedTasksFragment = safeFinishedDownloadFragmentRef
		}
	}
	
	/**
	 * Loads the Lottie animation for empty downloads view.
	 */
	private fun loadEmptyDownloadAnimation() {
		emptyDownloadAnim.apply {
			clipToCompositionBounds = false
			setScaleType(ImageView.ScaleType.FIT_XY)
			aioRawFiles.getEmptyDownloadAnimComposition()?.let {
				setComposition(it)
				playAnimation()
			} ?: run {
				setAnimation(R.raw.empty_downloads_animation)
			}; showView(this, true, 100)
		}
	}
	
	/**
	 * Loads the Lottie animation for the Open Active Tasks button.
	 */
	private fun loadOpenActiveTasksAnimation() {
		openActiveTasksAnim.apply {
			clipToCompositionBounds = false
			setScaleType(ImageView.ScaleType.FIT_XY)
			aioRawFiles.getOpenActiveTasksAnimationComposition()?.let {
				setComposition(it)
				playAnimation()
			} ?: run {
				setAnimation(R.raw.active_tasks_animation)
			}; showView(this, true, 100)
		}
	}
}