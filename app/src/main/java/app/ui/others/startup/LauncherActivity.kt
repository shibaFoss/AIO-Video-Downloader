package app.ui.others.startup

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import app.core.AIOApp.Companion.aioSettings
import app.core.bases.BaseActivity
import app.ui.others.information.UserFeedbackActivity
import app.ui.others.information.UserFeedbackActivity.FROM_CRASH_HANDLER
import app.ui.others.information.UserFeedbackActivity.WHERE_DIS_YOU_COME_FROM
import lib.ui.ActivityAnimator.animActivityFade
import java.lang.ref.WeakReference

/**
 * LauncherActivity serves as the entry point of the application.
 * It determines whether the app should launch the main interface or direct the user
 * to a feedback screen in case of a recent crash.
 *
 * This activity doesn't render any UI layout.
 */
class LauncherActivity : BaseActivity() {

    // A weak reference to avoid memory leaks and hold a reference to this activity safely
    private val safeSelfReference = WeakReference(this).get()

    /**
     * This activity doesn't need a UI, so we return -1 to skip layout inflation.
     * @return -1 indicating no layout should be rendered
     */
    override fun onRenderingLayout(): Int {
        return -1
    }

    /**
     * This method is called right after the layout would have been rendered.
     * Since there's no layout here, we use this opportunity to perform conditional navigation.
     */
    override fun onAfterLayoutRender() {
        if (aioSettings.hasAppCrashedRecently) {
            launchFeedbackActivity()
        } else {
            openActivity(OpeningActivity::class.java, true)
            finish()
        }
    }

    /**
     * Defines behavior for the back button. In this case,
     * it exits the app only on double press for safety.
     */
    override fun onBackPressActivity() {
        exitActivityOnDoubleBackPress()
    }

    /**
     * Launches the UserFeedbackActivity to collect crash-related feedback.
     * Resets the crash flag and applies a fade animation on transition.
     */
    private fun launchFeedbackActivity() {
        safeSelfReference?.let { context ->
            aioSettings.hasAppCrashedRecently = false // Clear crash flag
            aioSettings.updateInStorage() // Persist updated settings

            Intent(context, UserFeedbackActivity::class.java).apply {
                flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
                putExtra(WHERE_DIS_YOU_COME_FROM, FROM_CRASH_HANDLER)
                startActivity(this)
                finish()
                animActivityFade(context) // Smooth fade animation
            }
        }
    }
}