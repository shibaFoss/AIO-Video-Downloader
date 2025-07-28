package lib.ui.builders

import android.view.View
import android.view.View.GONE
import android.widget.TextView
import app.core.AIOApp
import app.core.AIOApp.Companion.admobHelper
import app.core.bases.interfaces.BaseActivityInf
import com.aio.R
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.ads.AdView
import lib.process.LogHelperUtils
import lib.ui.ViewUtility
import lib.ui.ViewUtility.showView
import lib.ui.builders.DialogBuilder.OnCancelListener

/**
 * A custom dialog for displaying a loading or waiting indicator with optional cancel handling.
 *
 * This dialog uses a Lottie animation and a message to inform users about an ongoing operation.
 * It optionally shows an OK button and supports cancelability with a custom cancel listener.
 *
 * @property activityInf Interface to the base activity, required to build and manage the dialog lifecycle.
 * @property loadingMessage The message displayed to inform the user about the operation in progress.
 * @property shouldHideOkayButton If true, the OK button is hidden from the dialog.
 * @property isCancelable Whether the dialog can be canceled by the user.
 * @property dialogCancelListener Optional listener triggered when the dialog is canceled.
 */
class WaitingDialog(
    private val activityInf: BaseActivityInf?,
    private val loadingMessage: String,
    private val shouldHideOkayButton: Boolean = false,
    private val isCancelable: Boolean = true,
    private val dialogCancelListener: OnCancelListener? = null
) {
    
    /** Logger instance for debugging and error reporting */
    private val logger = LogHelperUtils.from(javaClass)
    
    /**
     * Lazily initializes the [DialogBuilder] instance and configures its components.
     */
    val dialogBuilder: DialogBuilder? by lazy {
        DialogBuilder(activityInf).apply { initializeDialogComponents() }
    }
    
    /**
     * Initializes the dialog layout and behavior.
     * This method sets the view, handles cancel configuration, and loads dialog content.
     */
    private fun DialogBuilder.initializeDialogComponents() {
        setView(R.layout.dialog_waiting_progress_1)
        setCancelable(isCancelable)
        configureCancelListener()
        configureDialogContent()
    }
    
    /**
     * Sets the cancel listener for the dialog. If no listener is provided, the dialog cancels itself.
     */
    private fun DialogBuilder.configureCancelListener() {
        dialog.setOnCancelListener { dialog ->
            dialogCancelListener?.onCancel(dialog) ?: dialog?.cancel()
        }
    }
    
    /**
     * Configures the text, buttons, and animation for the waiting dialog.
     */
    private fun DialogBuilder.configureDialogContent() {
        view.apply {
            
            // Set up AdMob banner if user is not premium
            val admobView: AdView = findViewById(R.id.admob_fixed_sized_banner_ad)
            admobHelper.loadBannerAd(admobView)
            
            // Hide ad space for premium users
            if (AIOApp.IS_PREMIUM_USER) {
                findViewById<View>(R.id.ad_space_container).visibility = GONE
            }
            
            // Set loading message
            findViewById<TextView>(R.id.txt_progress_info).let {
                it.text = loadingMessage
                ViewUtility.animateFadInOutAnim(it)
            }
            
            // Configure OK button visibility and click listener
            findViewById<View>(R.id.button_dialog_positive_container).apply {
                setOnClickListener { close() }
                visibility = if (shouldHideOkayButton) View.GONE else View.VISIBLE
            }
            
            // Setup animation
            findViewById<LottieAnimationView>(R.id.img_progress_circle).apply {
                AIOApp.aioRawFiles.getCircularMotionComposition()?.let {
                    setComposition(it)
                    playAnimation()
                } ?: run { setAnimation(R.raw.circular_motion_anim) }
                
                showView(targetView = this, shouldAnimate = true, animTimeout = 400)
            }
        }
    }
    
    /**
     * Displays the dialog if it's not already shown.
     *
     * @throws IllegalStateException if the dialog context is unavailable.
     */
    fun show() {
        dialogBuilder?.let { dialogBuilder ->
            if (!dialogBuilder.isShowing) {
                dialogBuilder.show()
            }
        } ?: run {
            logger.e("Cannot show dialog - invalid context")
            throw IllegalStateException("Dialog context unavailable")
        }
    }
    
    /**
     * Closes the dialog if it is currently showing.
     *
     * @return `true` if the dialog was showing and was closed, `false` otherwise.
     */
    fun close(): Boolean {
        return dialogBuilder?.let {
            if (it.isShowing) {
                it.close()
                true
            } else false
        } ?: false
    }
}