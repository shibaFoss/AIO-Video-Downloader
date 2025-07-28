package app.ui.main.fragments.downloads

import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import app.core.bases.BaseFragment
import app.core.engines.video_parser.dialogs.VideoLinkPasteEditor
import app.ui.main.MotherActivity
import app.ui.main.fragments.downloads.fragments.active.ActiveTasksFragment
import app.ui.main.fragments.downloads.fragments.finished.FinishedTasksFragment
import com.aio.R
import java.lang.ref.WeakReference

/**
 * DownloadsFragment handles the UI and logic for managing download-related tabs (active/finished),
 * interactions with the main activity, and banner ads display.
 */
open class DownloadsFragment : BaseFragment() {
	
	// Weak reference to the host activity to avoid memory leaks
	private val safeMotherActivityRef by lazy { WeakReference(safeBaseActivityRef as MotherActivity).get() }
	
	// Weak reference to this fragment instance for safe usage across contexts
	private val safeDownloadFragmentRef by lazy { WeakReference(this).get() }
	
	private lateinit var fragmentLayoutView: View                   // Root layout view of this fragment
	open lateinit var fragmentViewPager: ViewPager                  // ViewPager to host child fragments
	open var finishedTasksFragment: FinishedTasksFragment? = null   // Reference to finished downloads fragment
	open var activeTasksFragment: ActiveTasksFragment? = null       // Reference to active downloads fragment
	
	/**
	 * Returns the layout resource ID for this fragment.
	 */
	override fun getLayoutResId(): Int {
		return R.layout.frag_down_1_main_1
	}
	
	/**
	 * Called after the fragment layout is inflated and ready.
	 * Initializes views, ads, child fragments, and click events.
	 */
	override fun onAfterLayoutLoad(layoutView: View, state: Bundle?) {
		registerSelfReferenceInMotherActivity()
		initializeViewProperties(layoutView)
		initializeChildFragments()
		initializeOnClickEvents(layoutView)
	}
	
	/**
	 * Called when fragment becomes visible again.
	 */
	override fun onResumeFragment() {
		registerSelfReferenceInMotherActivity()
	}
	
	/**
	 * Called when fragment goes into background. No operation is required here.
	 */
	override fun onPauseFragment() {
		// Do nothing
	}
	
	/**
	 * Cleans up references and adapter to prevent memory leaks.
	 */
	override fun onDestroyView() {
		unregisterSelfReferenceInMotherActivity()
		clearFragmentAdapterFromMemory()
		super.onDestroyView()
	}
	
	/**
	 * Initializes internal references and AdMob view if the activity is valid.
	 */
	private fun initializeViewProperties(layoutView: View) {
		safeMotherActivityRef?.let {
			fragmentLayoutView = layoutView
			initializeViews(layoutView)
		}
	}
	
	/**
	 * Registers this fragment instance in MotherActivity for later reference.
	 */
	private fun registerSelfReferenceInMotherActivity() {
		safeMotherActivityRef?.downloadFragment = safeDownloadFragmentRef
		safeMotherActivityRef?.sideNavigation?.closeDrawerNavigation()
	}
	
	/**
	 * Removes fragment reference from MotherActivity when not in use.
	 */
	private fun unregisterSelfReferenceInMotherActivity() {
		safeMotherActivityRef?.downloadFragment = null
	}
	
	/**
	 * Clears the ViewPager adapter to allow garbage collection of fragments.
	 */
	private fun clearFragmentAdapterFromMemory() {
		fragmentViewPager.adapter = null
	}
	
	/**
	 * Binds view components from the layout.
	 */
	private fun initializeViews(layoutView: View) {
		fragmentViewPager = layoutView.findViewById(R.id.fragment_viewpager)
	}
	
	/**
	 * Initializes child fragments inside the ViewPager.
	 */
	private fun initializeChildFragments() {
		safeDownloadFragmentRef?.let {
			fragmentViewPager.adapter = DownloadFragmentAdapter(childFragmentManager)
			fragmentViewPager.offscreenPageLimit = 1 // Cache only one offscreen fragment
		}
	}
	
	/**
	 * Assigns click actions to UI elements such as "Back" and "Add Download" buttons.
	 */
	private fun initializeOnClickEvents(layoutView: View) {
		val clickActionsMap = mapOf(
			layoutView.findViewById<View>(R.id.button_actionbar_back) to {
				if (fragmentViewPager.currentItem == 1) openFinishedTab()
				else navigateToBrowserFragment()
			},
			
			layoutView.findViewById<View>(R.id.button_actionbar_add_download) to {
				showDownloadTaskEditorDialog()
			}
		)
		
		clickActionsMap.forEach { (view, action) ->
			view.setOnClickListener { action() }
		}
	}
	
	/**
	 * Navigates the user to the browser fragment.
	 */
	private fun navigateToBrowserFragment() {
		safeMotherActivityRef?.openBrowserFragment()
	}
	
	/**
	 * Shows the dialog for adding a new download task manually via pasted video URL.
	 */
	private fun showDownloadTaskEditorDialog() {
		safeMotherActivityRef?.let { safeActivityRef ->
			VideoLinkPasteEditor(safeActivityRef).show()
		}
	}
	
	/**
	 * Switches ViewPager to show the Finished Downloads tab.
	 */
	fun openFinishedTab() {
		if (fragmentViewPager.currentItem != 0) {
			fragmentViewPager.currentItem = 0
		}
	}
	
	/**
	 * Switches ViewPager to show the Active Downloads tab.
	 */
	fun openActiveTab() {
		if (fragmentViewPager.currentItem != 1) {
			fragmentViewPager.currentItem = 1
		}
	}
}