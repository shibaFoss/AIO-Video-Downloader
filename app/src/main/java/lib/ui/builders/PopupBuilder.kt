package lib.ui.builders

import android.graphics.drawable.Drawable
import android.view.Gravity.NO_GRAVITY
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_OUTSIDE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.core.content.res.ResourcesCompat
import app.core.bases.interfaces.BaseActivityInf
import com.aio.R
import java.lang.ref.WeakReference

/**
 * A utility class that simplifies the creation and display of a PopupWindow anchored to a specific view.
 * It supports immersive mode, touch interactions, and flexible content initialization.
 *
 * @param activityInf A reference to the activity interface used for context and validation.
 * @param popupLayoutId Resource ID of the layout to inflate as the popup content. Optional if popupContentView is provided.
 * @param popupContentView A fully constructed View to be used as the popup content. Optional if popupLayoutId is provided.
 * @param popupAnchorView The view to which the popup will be anchored on the screen.
 */
class PopupBuilder(
	private val activityInf: BaseActivityInf?,
	private val popupLayoutId: Int = -1,
	private val popupContentView: View? = null,
	private val popupAnchorView: View
) {
	private val safeActivityRef = WeakReference(activityInf)
	private val popupWindow = PopupWindow(safeActivityRef.get()?.getActivity())
	private lateinit var popupLayout: View
	
	init {
		try {
			initializePopupContent() // Inflate or assign the content view
			validateContentView()    // Ensure valid content is provided
			setupPopupWindow()       // Setup dimensions, background, interaction handlers
		} catch (error: Exception) {
			error.printStackTrace()
			throw error
		}
	}
	
	/**
	 * Displays the popup window at a calculated position relative to the anchor view.
	 * Optionally enables immersive mode to hide system UI.
	 *
	 * @param shouldHideStatusAndNavbar If true, enters immersive mode hiding status and nav bars.
	 */
	fun show(shouldHideStatusAndNavbar: Boolean = false) {
		try {
			if (popupWindow.isShowing) return
			if (shouldHideStatusAndNavbar) enableImmersiveMode()
			showPopupWindow()
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Closes the popup if it's currently showing and the activity is valid.
	 */
	fun close() {
		try {
			val activity = safeActivityRef.get()?.getActivity() ?: return
			if (activity.isValidForWindowManagement() && popupWindow.isShowing) {
				popupWindow.dismiss()
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * @return The content view displayed inside the popup.
	 */
	fun getPopupView(): View = popupWindow.contentView
	
	/**
	 * @return The PopupWindow instance used by this builder.
	 */
	fun getPopupWindow(): PopupWindow = popupWindow
	
	/**
	 * Initializes the popup layout either by inflating the given layout ID or using the provided view.
	 * Throws if both are invalid.
	 */
	private fun initializePopupContent() {
		when {
			popupLayoutId != -1 -> {
				val inflater = LayoutInflater.from(safeActivityRef.get()?.getActivity())
				popupLayout = inflater.inflate(popupLayoutId, null, false)
			}
			popupContentView != null -> popupLayout = popupContentView
		}
	}
	
	/**
	 * Validates that popupLayout has been initialized.
	 * Throws IllegalArgumentException if not initialized properly.
	 */
	private fun validateContentView() {
		if (!::popupLayout.isInitialized) {
			throw IllegalArgumentException(
				"Must provide valid content via popupLayoutId or popupContentView"
			)
		}
	}
	
	/**
	 * Configures the popup window properties such as size, background, and touch behavior.
	 */
	private fun setupPopupWindow() {
		popupWindow.apply {
			isTouchable = true
			isFocusable = true
			isOutsideTouchable = true
			
			setBackgroundDrawable(createTransparentBackground())
			configureTouchHandling()
			
			width = WindowManager.LayoutParams.WRAP_CONTENT
			height = WindowManager.LayoutParams.WRAP_CONTENT
			contentView = popupLayout
		}
	}
	
	/**
	 * Creates a transparent background drawable using a predefined resource.
	 *
	 * @return A transparent background drawable or null if context is invalid.
	 */
	private fun createTransparentBackground(): Drawable? {
		return safeActivityRef.get()?.getActivity()?.let { ctx ->
			ResourcesCompat.getDrawable(
				ctx.resources,
				R.drawable.bg_image_transparent,
				ctx.theme
			)
		}
	}
	
	/**
	 * Configures how the popup should behave on various touch events such as outside taps and click releases.
	 */
	private fun configureTouchHandling() {
		popupWindow.setTouchInterceptor { view, event ->
			when (event.action) {
				ACTION_UP -> view.performClick().let { false }
				ACTION_OUTSIDE -> popupWindow.dismiss().let { true }
				else -> false
			}
		}
	}
	
	/**
	 * Enables full immersive mode to hide status and navigation bars.
	 * Uses deprecated API flags for backward compatibility.
	 */
	@Suppress("DEPRECATION")
	private fun enableImmersiveMode() {
		val s1 = SYSTEM_UI_FLAG_FULLSCREEN
		val s2 = SYSTEM_UI_FLAG_HIDE_NAVIGATION
		val s3 = SYSTEM_UI_FLAG_IMMERSIVE_STICKY
		popupWindow.contentView.systemUiVisibility = (s1 or s2 or s3)
	}
	
	/**
	 * Measures and positions the popup window on the screen relative to the anchor view.
	 */
	private fun showPopupWindow() {
		val anchorLocation = IntArray(2)
		popupAnchorView.getLocationOnScreen(anchorLocation)
		val anchorY = anchorLocation[1]
		
		val endMarginInPx = popupLayout.resources.getDimensionPixelSize(R.dimen._10)
		val displayMetrics = popupLayout.resources.displayMetrics
		val screenWidth = displayMetrics.widthPixels
		
		popupLayout.measure(UNSPECIFIED, UNSPECIFIED)
		val popupWidth = popupLayout.measuredWidth
		
		val xOffset = screenWidth - popupWidth - endMarginInPx
		popupWindow.showAtLocation(popupAnchorView, NO_GRAVITY, xOffset, anchorY)
	}
	
	/**
	 * Checks if the activity is in a valid state to perform window operations such as showing or dismissing a popup.
	 *
	 * @return True if the activity is not finishing or destroyed, false otherwise.
	 */
	private fun BaseActivityInf?.isValidForWindowManagement(): Boolean {
		this?.getActivity()?.let { activity ->
			return !activity.isFinishing && !activity.isDestroyed
		} ?: run { return false }
	}
}