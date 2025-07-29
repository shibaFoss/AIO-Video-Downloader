@file:Suppress("DEPRECATION")

package app.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import app.core.bases.BaseActivity
import app.ui.main.fragments.browser.BrowserFragment
import app.ui.main.fragments.downloads.DownloadsFragment
import app.ui.main.fragments.home.HomeFragment
import app.ui.main.fragments.settings.SettingsFragment

/**
 * Adapter for managing and supplying fragments for a ViewPager.
 *
 * This adapter maps each position to a specific fragment:
 * - 0 → HomeFragment
 * - 1 → BrowserFragment
 * - 2 → DownloadsFragment
 * - 3 → SettingsFragment
 *
 * @param fragmentManager The fragment manager used to handle fragment transactions.
 *
 * @note This class uses the deprecated [FragmentPagerAdapter]. Consider using [FragmentStateAdapter]
 * with ViewPager2 for modern implementations.
 */
class FragmentsPageAdapter(baseActivity: BaseActivity) : FragmentStateAdapter(baseActivity) {

	/**
	 * Returns the total number of fragments/pages managed by this adapter.
	 */
	override fun getItemCount(): Int = 4

	/**
	 * Returns the fragment associated with the specified position in the ViewPager.
	 *
	 * @param position The index of the requested fragment.
	 * @return The corresponding [Fragment] for the given position.
	 */
	override fun createFragment(position: Int): Fragment {
		return when (position) {
			0 -> HomeFragment()
			1 -> BrowserFragment()
			2 -> DownloadsFragment()
			3 -> SettingsFragment()
			else -> HomeFragment() // Fallback to HomeFragment in unexpected cases
		}
	}
}
