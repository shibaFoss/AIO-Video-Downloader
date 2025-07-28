package app.ui.main.fragments.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import app.core.AIOApp
import app.core.bases.BaseFragment
import app.ui.main.MotherActivity
import com.aio.R
import lib.device.AppVersionUtility.versionCode
import lib.device.AppVersionUtility.versionName
import lib.texts.CommonTextUtils.fromHtmlStringToSpanned
import java.lang.ref.WeakReference

/**
 * Fragment responsible for displaying the Settings UI.
 * Allows user interactions for checking app updates, changing preferences,
 * viewing legal info, and upgrading to premium.
 */
class SettingsFragment : BaseFragment() {
	
	// A weak reference to the current instance of this fragment to avoid memory leaks
	val safeSettingsFragmentRef = WeakReference(this).get()
	
	// A lazy-initialized weak reference to the host activity (MotherActivity)
	val safeMotherActivityRef by lazy { WeakReference(safeBaseActivityRef as MotherActivity).get() }
	
	// Logic handler for click actions in the settings UI
	var settingsOnClickLogic: SettingsOnClickLogic? = null
	
	// Reference to the "Check for Update" button
	var buttonCheckNewUpdate: View? = null
	
	// Reference to the "Upgrade to Premium" button
	var buttonUpgradeToPremium: View? = null
	
	// Reference to the Premium status text.
	var textPremiumStatus: TextView? = null
	
	/**
	 * Provides the layout resource ID for this fragment.
	 */
	override fun getLayoutResId(): Int {
		return R.layout.frag_settings_1_main_1
	}
	
	/**
	 * Called after the layout is loaded and ready to be used.
	 * Initializes views and sets up click listeners.
	 */
	override fun onAfterLayoutLoad(layoutView: View, state: Bundle?) {
		safeSettingsFragmentRef?.let { fragmentRef ->
			safeFragmentLayoutRef?.let { layoutRef ->
				registerSelfReferenceInMotherActivity()
				initializeViews(layoutRef)
				initializeViewsOnClick(fragmentRef, layoutRef)
			}
		}
	}
	
	/**
	 * Called when the fragment is resumed. Re-registers itself in the activity and refreshes UI state.
	 */
	override fun onResumeFragment() {
		registerSelfReferenceInMotherActivity()
		settingsOnClickLogic?.updateSettingStateUI()
	}
	
	/**
	 * Called when the fragment is paused. No special behavior needed here.
	 */
	override fun onPauseFragment() {
		// Do nothing
	}
	
	/**
	 * Called when the fragment's view is destroyed.
	 * Unregisters itself from the activity to avoid memory leaks.
	 */
	override fun onDestroyView() {
		unregisterSelfReferenceInMotherActivity()
		super.onDestroyView()
	}
	
	/**
	 * Registers this fragment instance in the host activity for callbacks or reference.
	 */
	private fun registerSelfReferenceInMotherActivity() {
		safeMotherActivityRef?.settingsFragment = safeSettingsFragmentRef
		safeMotherActivityRef?.sideNavigation?.closeDrawerNavigation()
	}
	
	/**
	 * Removes the fragment reference from the host activity to prevent leaks.
	 */
	private fun unregisterSelfReferenceInMotherActivity() {
		safeMotherActivityRef?.settingsFragment = null
	}
	
	/**
	 * Initializes views like buttons and labels used in the fragment.
	 */
	private fun initializeViews(fragmentLayout: View) {
		buttonCheckNewUpdate = fragmentLayout.findViewById(R.id.button_check_new_update)
		buttonUpgradeToPremium = fragmentLayout.findViewById(R.id.button_upgrade_to_premium)
		textPremiumStatus = fragmentLayout.findViewById(R.id.text_premium_status)
		initializeViewsInfo(fragmentLayout)
	}
	
	/**
	 * Sets click listeners for all interactive UI elements in the settings screen.
	 */
	private fun initializeViewsOnClick(settingsFragmentRef: SettingsFragment, fragmentLayout: View) {
		settingsOnClickLogic = SettingsOnClickLogic(settingsFragmentRef)
		if (AIOApp.IS_ULTIMATE_VERSION_UNLOCKED && AIOApp.IS_PREMIUM_USER) {
			textPremiumStatus?.text = getText(R.string.title_you_are_using_aio_premium)
		}
		
		with(fragmentLayout) {
			mapOf(
				R.id.button_upgrade_to_premium to { settingsOnClickLogic?.openApplicationUpgradePage() },
				R.id.button_restart_application to { settingsOnClickLogic?.restartApplication() },
				R.id.button_check_new_update to { settingsOnClickLogic?.openApplicationInPlayStore() },
				R.id.button_share_with_friends to { settingsOnClickLogic?.shareApplicationWithFriends() },
				R.id.button_open_about_info to { settingsOnClickLogic?.openApplicationInformation() },
				R.id.button_open_feedback to { settingsOnClickLogic?.openUserFeedbackActivity() },
				R.id.button_open_content_policy to { settingsOnClickLogic?.showContentPolicyActivity() },
				R.id.button_open_privacy_policy to { settingsOnClickLogic?.showPrivacyPolicyActivity() },
				R.id.button_open_terms_condition to { settingsOnClickLogic?.showTermsConditionActivity() },
				R.id.button_play_notification_sound to { settingsOnClickLogic?.toggleDownloadNotificationSound() },
				R.id.button_wifi_only_downloads to { settingsOnClickLogic?.toggleWifiOnlyDownload() },
				R.id.button_hide_task_notifications to { settingsOnClickLogic?.toggleHideDownloadNotification() },
				R.id.button_enable_popup_blocker to { settingsOnClickLogic?.toggleBrowserPopupAdBlocker() },
				R.id.button_enable_video_grabber to { settingsOnClickLogic?.toggleBrowserVideoGrabber() },
				R.id.button_browser_homepage to { settingsOnClickLogic?.setBrowserDefaultHomepage() },
				R.id.button_language_picker to { settingsOnClickLogic?.showApplicationLanguageChanger() },
				R.id.button_default_download_folder to { settingsOnClickLogic?.setDefaultDownloadLocationPicker() },
			).forEach { (id, action) ->
				setClickListener(id) { action() }
			}
		}
	}
	
	/**
	 * Initializes the version info displayed in the settings screen.
	 */
	private fun initializeViewsInfo(fragmentLayout: View) {
		with(fragmentLayout) {
			findViewById<TextView>(R.id.text_version_info)?.apply {
				val versionName = "${getString(R.string.title_version_number)} $versionName"
				val versionCode = "${getString(R.string.title_build_version)} $versionCode"
				text = fromHtmlStringToSpanned("${versionName}<br/>${versionCode}")
			}
		}
	}
	
	/**
	 * Helper extension function to set a click listener using resource ID and action lambda.
	 */
	private fun View.setClickListener(id: Int, action: () -> Unit) {
		findViewById<View>(id)?.setOnClickListener { action() }
	}
}
