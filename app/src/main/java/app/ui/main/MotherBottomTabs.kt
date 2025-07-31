package app.ui.main

import android.graphics.PorterDuff.Mode.SRC_IN
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.res.ResourcesCompat.getDrawable
import com.aio.R
import java.lang.ref.WeakReference

/**
 * Handles the bottom navigation tabs in the main activity.
 *
 * Responsibilities:
 * - Manages the bottom tab UI and interactions
 * - Handles tab selection states (active/inactive)
 * - Coordinates with MotherActivity for fragment navigation
 * - Maintains visual consistency across tab states
 */
class MotherBottomTabs(motherActivity: MotherActivity?) {
	
	// Weak reference to parent activity to prevent memory leaks
	private val safeMotherActivityRef = WeakReference(motherActivity).get()
	
	// Lazy initialization of tab buttons with their click actions
	private val buttons by lazy {
		safeMotherActivityRef?.let { safeActivityRef ->
			// Map of view IDs to their corresponding navigation actions
			mapOf(
				R.id.btn_home_tab to { safeActivityRef.openHomeFragment() },
				R.id.btn_browser_tab to { safeActivityRef.openBrowserFragment() },
				R.id.btn_tasks_tab to { safeActivityRef.openDownloadsFragment() },
				R.id.btn_settings_tab to { safeActivityRef.openSettingsFragment() }
			)
		}
	}
	
	/**
	 * Initializes the bottom tabs by setting up click listeners.
	 * Must be called after activity layout is rendered.
	 */
	fun initialize() {
		safeMotherActivityRef?.let { safeActivityRef ->
			buttons?.let { buttons ->
				// Set click listener for each tab button
				buttons.keys.forEach { id ->
					val view = safeActivityRef.findViewById<View>(id)
					view.setOnClickListener { buttons[id]?.invoke() }
				}
			}
		}
	}
	
	/**
	 * Updates the UI to reflect the currently selected tab.
	 * @param tab The tab to highlight as selected
	 */
	fun updateTabSelectionUI(tab: Tab) {
		safeMotherActivityRef?.let { safeActivityRef ->
			// Mapping of tabs to their associated view IDs (container, icon, text)
			val buttonTabs = mapOf(
				Tab.HOME_TAB to listOf(R.id.btn_home_tab, R.id.img_home_tab, R.id.txt_home_tab),
				Tab.BROWSER_TAB to listOf(R.id.btn_browser_tab, R.id.img_browser_tab, R.id.txt_browser_tab),
				Tab.DOWNLOADS_TAB to listOf(R.id.btn_tasks_tab, R.id.img_tasks_tab, R.id.txt_task_tab),
				Tab.SETTINGS_TAB to listOf(R.id.btn_settings_tab, R.id.img_settings_tab, R.id.txt_settings_tab)
			)
			
			// First reset all tabs to inactive state
			buttonTabs.values.forEach { ids ->
				// Update container background
				safeActivityRef.findViewById<View>(ids[0])?.let { container ->
					val bgNegativeSelector = R.drawable.ic_button_negative_selector
					val resources = safeActivityRef.resources
					val activityTheme = safeActivityRef.theme
					val inactiveButtonBg = getDrawable(resources, bgNegativeSelector, activityTheme)
					container.background = inactiveButtonBg
					container.elevation = resources.getDimension(R.dimen._0)
				}
				
				// Update icon color
				safeActivityRef.findViewById<View>(ids[1])?.let { logoImage ->
					(logoImage as ImageView).apply {
						setColorFilter(getColor(context, R.color.color_secondary), SRC_IN)
					}
				}
				
				// Update text color
				safeActivityRef.findViewById<View>(ids[2])?.let { textTab ->
					(textTab as TextView).apply {
						setTextColor(getColor(context, R.color.color_text_primary))
					}
				}
			}
			
			// Then highlight the selected tab
			buttonTabs[tab]?.let { ids ->
				// Update selected container background
				safeActivityRef.findViewById<View>(ids[0])?.let { container ->
					val bgDrawableResId = R.drawable.rounded_secondary_color
					val resources = safeActivityRef.resources
					val activityTheme = safeActivityRef.theme
					val buttonBg = getDrawable(resources, bgDrawableResId, activityTheme)
					container.background = buttonBg
					container.elevation = resources.getDimension(R.dimen._3)
				}
				
				// Update selected icon color
				safeActivityRef.findViewById<View>(ids[1])?.let { logoImage ->
					(logoImage as ImageView).apply {
						setColorFilter(getColor(context, R.color.color_on_secondary), SRC_IN)
					}
				}
				
				// Update selected text color
				safeActivityRef.findViewById<View>(ids[2])?.let { textTab ->
					(textTab as TextView).apply {
						setTextColor(getColor(context, R.color.color_on_secondary))
					}
				}
			}
		}
	}
	
	/**
	 * Enum representing the available bottom tabs.
	 */
	enum class Tab {
		HOME_TAB,
		BROWSER_TAB,
		DOWNLOADS_TAB,
		SETTINGS_TAB
	}
}