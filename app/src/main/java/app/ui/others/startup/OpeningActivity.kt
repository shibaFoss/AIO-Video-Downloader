package app.ui.others.startup

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.widget.TextView
import app.core.bases.BaseActivity
import app.ui.main.MotherActivity
import com.aio.R
import kotlinx.coroutines.delay
import lib.device.AppVersionUtility
import lib.process.ThreadsUtility
import lib.ui.ActivityAnimator.animActivityFade
import java.lang.ref.WeakReference

/**
 * **OpeningActivity**
 *
 * This is the **splash/launch screen** of the application.
 *
 * Purpose:
 * - Displays a splash screen for a minimum of 2 seconds.
 * - Shows the current application version information.
 * - Loads essential model data or performs initial setup tasks in the background.
 * - Navigates to the main screen of the application ([MotherActivity]).
 *
 * Key Features:
 * - Uses a [WeakReference] to hold a self-reference to avoid memory leaks.
 * - Uses [ThreadsUtility] for running background tasks and UI updates.
 * - Displays app version text retrieved via [AppVersionUtility].
 *
 * Lifecycle Flow:
 * 1. **onRenderingLayout()** inflates the layout and sets the system bar theme.
 * 2. **onAfterLayoutRender()** triggers the version display and a delayed transition to the main activity.
 * 3. **launchMotherActivity()** launches [MotherActivity] after the splash delay.
 *
 * Layout: [R.layout.activity_opening_1]
 */
class OpeningActivity : BaseActivity() {

    /**
     * Holds a weak reference to this activity instance.
     *
     * Using [WeakReference] prevents memory leaks if the activity is destroyed
     * while background tasks are still running.
     */
    private val safeSelfReference = WeakReference(this).get()

    /**
     * Called before layout rendering begins.
     *
     * @return The layout resource ID to be rendered ([R.layout.activity_opening_1]).
     */
    override fun onRenderingLayout(): Int {
        setLightSystemBarTheme() // Set light status bar and navigation bar
        return R.layout.activity_opening_1
    }

    /**
     * Called after the layout has been rendered.
     *
     * - Displays the app version info on the splash screen.
     * - Waits for at least 2 seconds before launching the main activity.
     */
    override fun onAfterLayoutRender() {
        showApkVersionInfo()

        // Run background tasks on a background thread
        ThreadsUtility.executeInBackground(codeBlock = {
            delay(2000) // Minimum splash delay of 2 seconds

            // Once delay is complete, switch back to the main thread
            ThreadsUtility.executeOnMain(::launchMotherActivity)
        })
    }

    /**
     * Called when the user presses the back button.
     *
     * Uses a double-back press mechanism to exit the activity gracefully.
     */
    override fun onBackPressActivity() {
        exitActivityOnDoubleBackPress()
    }

    /**
     * Displays the current app version on the splash screen.
     *
     * - Retrieves the version name from [AppVersionUtility.versionName].
     * - Sets it to the [TextView] with ID [R.id.txt_version_info].
     */
    private fun showApkVersionInfo() {
        val versionName = AppVersionUtility.versionName
        "${getString(R.string.title_version)} : $versionName".apply {
            findViewById<TextView>(R.id.txt_version_info).text = this
        }
    }

    /**
     * Launches the main activity ([MotherActivity]) after the splash delay.
     *
     * Flags used:
     * - [FLAG_ACTIVITY_CLEAR_TOP]: Clears any existing instances of the main activity.
     * - [FLAG_ACTIVITY_SINGLE_TOP]: Reuses the existing instance if already at the top.
     *
     * Also applies a fade animation when transitioning from the splash screen.
     */
    private fun launchMotherActivity() {
        safeSelfReference?.let { context ->
            Intent(context, MotherActivity::class.java).apply {
                flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
                startActivity(this)
                finish() // Close the splash activity
                animActivityFade(getActivity()) // Apply fade animation during transition
            }
        }
    }
}