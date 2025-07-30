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

class OpeningActivity : BaseActivity() {

    // A weak reference to avoid memory leaks and hold a reference to this activity safely
    private val safeSelfReference = WeakReference(this).get()

    override fun onRenderingLayout(): Int {
        setLightSystemBarTheme()
        return R.layout.activity_opening_1
    }

    override fun onAfterLayoutRender() {
        showApkVersionInfo()
        ThreadsUtility.executeInBackground(codeBlock = {
            // Display splash screen for minimum 2 seconds
            delay(2000)

            ThreadsUtility.executeOnMain {
                // Transition to main activity
                launchMotherActivity()
            }
        })
    }

    override fun onBackPressActivity() {
        exitActivityOnDoubleBackPress()
    }

    /**
     * Displays the current app version on the splash screen.
     *
     * This retrieves the version name from [AppVersionUtility] and sets it to
     * the [TextView] with ID [R.id.txt_version_info] using a formatted string
     * from resources.
     */
    private fun showApkVersionInfo() {
        val versionName = AppVersionUtility.versionName
        "${getString(R.string.title_version)} : $versionName".apply {
            findViewById<TextView>(R.id.txt_version_info).text = this
        }
    }

    /**
     * Launches the main activity (MotherActivity) of the app.
     * This is the default flow if no crash was recently detected.
     */
    private fun launchMotherActivity() {
        safeSelfReference?.let { context ->
            Intent(context, MotherActivity::class.java).apply {
                flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
                startActivity(this)
                finish()
                animActivityFade(getActivity()) // Apply fade animation
            }
        }
    }
}