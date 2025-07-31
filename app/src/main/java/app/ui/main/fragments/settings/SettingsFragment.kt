package app.ui.main.fragments.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import app.core.bases.BaseFragment
import app.ui.main.MotherActivity
import com.aio.R
import lib.device.AppVersionUtility.versionCode
import lib.device.AppVersionUtility.versionName
import lib.texts.CommonTextUtils.fromHtmlStringToSpanned
import java.lang.ref.WeakReference

/**
 * Fragment responsible for displaying and managing application settings.
 *
 * This fragment provides:
 * - Application version information
 * - Update checking functionality
 * - Various toggle settings (notifications, WiFi-only downloads, etc.)
 * - Navigation to legal documents (privacy policy, terms of service)
 * - Language and download location preferences
 * - Browser-related settings
 *
 * The fragment maintains weak references to avoid memory leaks and delegates
 * click handling to a separate [SettingsOnClickLogic] class.
 */
class SettingsFragment : BaseFragment() {

    /**
     * Weak reference to this fragment instance to prevent memory leaks.
     * Used when passing fragment reference to other components.
     */
    val safeSettingsFragmentRef = WeakReference(this).get()

    /**
     * Lazy-initialized weak reference to the host [MotherActivity].
     * Provides safe access to activity methods and properties.
     */
    val safeMotherActivityRef by lazy { WeakReference(safeBaseActivityRef as MotherActivity).get() }

    /**
     * Handler for all click events in the settings UI.
     * Manages the business logic for each settings option.
     */
    var settingsOnClickLogic: SettingsOnClickLogic? = null

    /**
     * Reference to the "Check for Update" button view.
     * Maintained as a field to allow dynamic updates to its state.
     */
    var buttonCheckNewUpdate: View? = null

    /**
     * Provides the layout resource ID for this fragment.
     * @return The layout resource ID (R.layout.frag_settings_1_main_1)
     */
    override fun getLayoutResId(): Int {
        return R.layout.frag_settings_1_main_1
    }

    /**
     * Called after the fragment's layout is inflated and ready.
     * Initializes all views and sets up click listeners.
     *
     * @param layoutView The inflated layout view
     * @param state The saved instance state bundle, if any
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
     * Called when the fragment becomes visible.
     * Re-registers with the activity and refreshes UI state.
     */
    override fun onResumeFragment() {
        registerSelfReferenceInMotherActivity()
        settingsOnClickLogic?.updateSettingStateUI()
    }

    /**
     * Called when the fragment is no longer visible.
     * Currently no cleanup needed here.
     */
    override fun onPauseFragment() {
        // Intentionally left blank
    }

    /**
     * Called when the fragment's view is being destroyed.
     * Cleans up references to prevent memory leaks.
     */
    override fun onDestroyView() {
        unregisterSelfReferenceInMotherActivity()
        super.onDestroyView()
    }

    /**
     * Registers this fragment with the host activity.
     * Allows the activity to reference this fragment when needed.
     */
    private fun registerSelfReferenceInMotherActivity() {
        safeMotherActivityRef?.settingsFragment = safeSettingsFragmentRef
        safeMotherActivityRef?.sideNavigation?.closeDrawerNavigation()
    }

    /**
     * Unregisters this fragment from the host activity.
     * Prevents memory leaks when the fragment is destroyed.
     */
    private fun unregisterSelfReferenceInMotherActivity() {
        safeMotherActivityRef?.settingsFragment = null
    }

    /**
     * Initializes all view references used in the fragment.
     * @param fragmentLayout The root view of the fragment
     */
    private fun initializeViews(fragmentLayout: View) {
        buttonCheckNewUpdate = fragmentLayout.findViewById(R.id.btn_check_new_update)
        initializeViewsInfo(fragmentLayout)
    }

    /**
     * Sets up all click listeners for the settings options.
     * Delegates actual click handling to [SettingsOnClickLogic].
     *
     * @param settingsFragmentRef Weak reference to this fragment
     * @param fragmentLayout The root view of the fragment
     */
    private fun initializeViewsOnClick(
        settingsFragmentRef: SettingsFragment,
        fragmentLayout: View
    ) {
        settingsOnClickLogic = SettingsOnClickLogic(settingsFragmentRef)

        // Map of view IDs to their corresponding click actions
        val clickActions = mapOf(
            R.id.btn_restart_application to { settingsOnClickLogic?.restartApplication() },
            R.id.btn_check_new_update to { settingsOnClickLogic?.checkForNewApkVersion() },
            R.id.btn_share_with_friends to { settingsOnClickLogic?.shareApplicationWithFriends() },
            R.id.btn_open_about_info to { settingsOnClickLogic?.openApplicationInformation() },
            R.id.btn_open_feedback to { settingsOnClickLogic?.openUserFeedbackActivity() },
            R.id.btn_open_content_policy to { settingsOnClickLogic?.showContentPolicyActivity() },
            R.id.btn_open_privacy_policy to { settingsOnClickLogic?.showPrivacyPolicyActivity() },
            R.id.btn_open_terms_condition to { settingsOnClickLogic?.showTermsConditionActivity() },
            R.id.btn_play_notification_sound to { settingsOnClickLogic?.toggleDownloadNotificationSound() },
            R.id.btn_wifi_only_downloads to { settingsOnClickLogic?.toggleWifiOnlyDownload() },
            R.id.btn_hide_task_notifications to { settingsOnClickLogic?.toggleHideDownloadNotification() },
            R.id.btn_enable_popup_blocker to { settingsOnClickLogic?.toggleBrowserPopupAdBlocker() },
            R.id.btn_enable_video_grabber to { settingsOnClickLogic?.toggleBrowserVideoGrabber() },
            R.id.btn_browser_homepage to { settingsOnClickLogic?.setBrowserDefaultHomepage() },
            R.id.btn_language_picker to { settingsOnClickLogic?.showApplicationLanguageChanger() },
            R.id.btn_default_download_folder to { settingsOnClickLogic?.setDefaultDownloadLocationPicker() }
        )

        // Apply all click listeners
        clickActions.forEach { (id, action) ->
            fragmentLayout.setClickListener(id) { action() }
        }
    }

    /**
     * Initializes the version information display.
     * Shows both version name (user-facing) and version code (build number).
     *
     * @param fragmentLayout The root view of the fragment
     */
    private fun initializeViewsInfo(fragmentLayout: View) {
        with(fragmentLayout) {
            findViewById<TextView>(R.id.txt_version_info)?.apply {
                val versionNameText = "${getString(R.string.title_version_number)} $versionName"
                val versionCodeText = "${getString(R.string.title_build_version)} $versionCode"
                text = fromHtmlStringToSpanned("${versionNameText}<br/>${versionCodeText}")
            }
        }
    }

    /**
     * Extension function to simplify setting click listeners.
     *
     * @param id The view ID to set the listener on
     * @param action The action to perform when clicked
     */
    private fun View.setClickListener(id: Int, action: () -> Unit) {
        findViewById<View>(id)?.setOnClickListener { action() }
    }
}