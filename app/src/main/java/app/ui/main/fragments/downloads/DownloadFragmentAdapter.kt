@file:Suppress("DEPRECATION")

package app.ui.main.fragments.downloads

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import app.ui.main.fragments.downloads.fragments.active.ActiveTasksFragment
import app.ui.main.fragments.downloads.fragments.finished.FinishedTasksFragment

/**
 * [DownloadFragmentAdapter] is a [FragmentPagerAdapter] that manages the two primary fragments
 * used in the Downloads section of the app:
 * - [FinishedTasksFragment] for showing completed downloads
 * - [ActiveTasksFragment] for showing ongoing downloads
 *
 * @constructor Accepts a [FragmentManager] to initialize the adapter.
 *
 * NOTE: This uses the deprecated FragmentPagerAdapter due to legacy support.
 */
class DownloadFragmentAdapter(fragmentManager: FragmentManager) :
	FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
	
	/**
	 * Returns the total number of tabs/fragments managed by this adapter.
	 * In this case, it returns 2: Finished and Active.
	 */
	override fun getCount(): Int {
		return 2
	}
	
	/**
	 * Returns the [Fragment] corresponding to the given position:
	 * - Position 0 → [FinishedTasksFragment]
	 * - Position 1 → [ActiveTasksFragment]
	 *
	 * @param position Index of the fragment to be returned.
	 * @return Corresponding [Fragment]
	 * @throws IllegalArgumentException if position is not 0 or 1.
	 */
	override fun getItem(position: Int): Fragment {
		return when (position) {
			0 -> FinishedTasksFragment()
			1 -> ActiveTasksFragment()
			else -> throw IllegalArgumentException("Invalid position: $position")
		}
	}
}