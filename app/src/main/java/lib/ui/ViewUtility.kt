package lib.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator.ofFloat
import android.app.Activity
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Context.WINDOW_SERVICE
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.decodeByteArray
import android.graphics.BitmapFactory.decodeFile
import android.graphics.BitmapFactory.decodeStream
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowMetrics
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils.loadAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import app.core.AIOApp
import com.aio.R
import com.bumptech.glide.Glide
import lib.files.FileSystemUtility
import lib.process.ThreadsUtility
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Utility object providing commonly used view-related helper functions for Android UI components.
 *
 * Includes methods for retrieving screen dimensions, dynamically adjusting layout properties of views
 * like GridView, unbinding view resources for memory optimization, setting click listeners efficiently
 * across multiple views, retrieving nested views safely, and triggering basic view animations.
 *
 * These utilities help in maintaining cleaner UI code and ensure better compatibility across Android versions.
 */
object ViewUtility {
	
	/**
	 * Retrieves the width of the device screen in pixels.
	 *
	 * This function uses the modern [WindowMetrics] API on Android R (API level 30)
	 * and above for more accurate window dimensions. On older devices, it falls
	 * back to the deprecated [DisplayMetrics] API.
	 *
	 * @return The width of the device screen in pixels.
	 */
	@JvmStatic
	fun getDeviceWidth(): Int {
		return if (VERSION.SDK_INT >= VERSION_CODES.R)
			getCurrentWindowMetrics().bounds.width()
		else getLegacyDisplayMetrics().widthPixels
	}
	
	/**
	 * Retrieves the current [WindowMetrics] of the application window.
	 *
	 * This function is only available on Android R (API level 30) and above.
	 * It provides more accurate information about the current window's dimensions
	 * and state compared to legacy APIs.
	 *
	 * @return The current [WindowMetrics].
	 */
	@RequiresApi(VERSION_CODES.R)
	private fun getCurrentWindowMetrics(): WindowMetrics {
		val windowService = AIOApp.INSTANCE.getSystemService(WINDOW_SERVICE)
		val windowManager = windowService as WindowManager
		return windowManager.currentWindowMetrics
	}
	
	/**
	 * Retrieves [DisplayMetrics] using the legacy [WindowManager] API.
	 *
	 * This function is used for devices running versions older than Android R
	 * (API level 30). It retrieves the real display metrics, which include
	 * screen decorations like navigation bars.
	 *
	 * @return The [DisplayMetrics] of the display.
	 */
	@Suppress("DEPRECATION")
	private fun getLegacyDisplayMetrics(): DisplayMetrics {
		return DisplayMetrics().apply {
			val windowService = AIOApp.INSTANCE.getSystemService(WINDOW_SERVICE)
			val windowManager = windowService as WindowManager
			windowManager.defaultDisplay.getRealMetrics(this)
		}
	}
	
	/**
	 * Calculates the number of rows required to display all items in a [GridView].
	 *
	 * This function determines the number of columns in the [GridView] and then
	 * calculates the number of rows needed based on the total number of items
	 * in the adapter.
	 *
	 * @param gridView The [GridView] for which to calculate the number of rows.
	 * Can be null, in which case 0 is returned.
	 * @return The number of rows in the [GridView], or 0 if the [gridView] is null
	 * or the number of columns is 0.
	 */
	@JvmStatic
	fun getNumberOfGridRows(gridView: GridView?): Int {
		if (gridView == null) return 0
		val totalItems = gridView.adapter?.count ?: 0
		val numberOfColumns = getNumberOfGridColumns(gridView)
		return if (numberOfColumns > 0) {
			val rows = totalItems / numberOfColumns
			if (totalItems % numberOfColumns == 0) rows else rows + 1
		} else 0
	}
	
	/**
	 * Determines the number of columns that can fit within the [GridView]'s width.
	 *
	 * This function calculates the number of columns by dividing the device width
	 * by a predefined column width dimension (`R.dimen._150`).
	 *
	 * @param gridView The [GridView] for which to determine the number of columns.
	 * Can be null, in which case 1 is returned (as a default).
	 * @return The number of columns in the [GridView], or 1 if the [gridView] is null
	 * or if the column width dimension is not greater than 0.
	 */
	@JvmStatic
	fun getNumberOfGridColumns(gridView: GridView?): Int {
		if (gridView == null) return 1
		val deviceWidth = getDeviceWidth()
		val columnWidth = gridView.resources.getDimensionPixelSize(R.dimen._150)
		return if (columnWidth > 0) deviceWidth / columnWidth else 1
	}
	
	/**
	 * Sets the height of a [GridView] dynamically based on the number of its children.
	 *
	 * This function calculates the total height required to display all rows of the
	 * [GridView] without scrolling, based on the number of rows and a predefined
	 * item height dimension (`R.dimen._135`). It then updates the layout parameters
	 * of the [GridView] to match this calculated height.
	 *
	 * @param gridView The [GridView] whose height needs to be adjusted.
	 */
	@JvmStatic
	fun setGridViewHeightBasedOnChildren(gridView: GridView) {
		val rowCount = getNumberOfGridRows(gridView)
		val itemHeight = gridView.resources.getDimensionPixelSize(R.dimen._135)
		val totalHeight = itemHeight * rowCount
		val params = gridView.layoutParams
		params.height = totalHeight
		gridView.layoutParams = params
		gridView.requestLayout()
	}
	
	/**
	 * Unbinds drawables from a [View] and its children to help with memory management.
	 *
	 * This function recursively traverses the view hierarchy and nullifies the callbacks
	 * of background drawables. For [ImageView]s, it sets the image bitmap to null.
	 * For [ViewGroup]s (excluding [AdapterView]s), it recursively unbinds drawables
	 * of its children and then removes all views. This can be useful in `onDestroy()`
	 * or when a view is no longer needed to free up resources.
	 *
	 * @param view The [View] or [ViewGroup] to unbind drawables from. Can be null, in which case nothing happens.
	 */
	@JvmStatic
	fun unbindDrawables(view: View?) {
		try {
			view?.background?.callback = null
			if (view is ImageView) {
				view.setImageBitmap(null)
			} else if (view is ViewGroup) {
				for (index in 0 until view.childCount)
					unbindDrawables(view.getChildAt(index))
				if (view !is AdapterView<*>) view.removeAllViews()
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Sets an [View.OnClickListener] on multiple [View]s within an [Activity].
	 *
	 * This function takes a common [View.OnClickListener] and an array of view IDs
	 * and sets the listener on each found [View] within the provided [Activity].
	 * It uses a [WeakReference] to the [Activity] to avoid potential memory leaks
	 * if the listener outlives the activity (though in most common `OnClickListener`
	 * implementations, this is not a concern).
	 *
	 * @param clickListener The [View.OnClickListener] to set on the views. Can be null to clear existing listeners.
	 * @param activity The [Activity] containing the views. Using a [WeakReference] to avoid leaks.
	 * @param ids A vararg of integer IDs ([IdRes]) of the views to set the listener on.
	 */
	@JvmStatic
	fun setViewOnClickListener(
		clickListener: View.OnClickListener?,
		activity: Activity?, @IdRes vararg ids: Int
	) {
		for (id in ids) {
			WeakReference(activity).get()?.findViewById<View>(id).apply {
				this?.setOnClickListener(clickListener)
			}
		}
	}
	
	/**
	 * Sets an [View.OnClickListener] on multiple [View]s within a given parent [View].
	 *
	 * This function takes a common [View.OnClickListener] and an array of view IDs
	 * and sets the listener on each found [View] within the provided parent [View].
	 *
	 * @param onClickListener The [View.OnClickListener] to set on the views. Can be null to clear existing listeners.
	 * @param layout The parent [View] containing the views.
	 * @param ids A vararg of integer IDs ([IdRes]) of the views to set the listener on.
	 */
	@JvmStatic
	fun setViewOnClickListener(
		onClickListener: View.OnClickListener?,
		layout: View, @IdRes vararg ids: Int
	) {
		for (id in ids) {
			layout.findViewById<View>(id).apply {
				this?.setOnClickListener(onClickListener)
			}
		}
	}
	
	/**
	 * Retrieves a specific [View] by its ID from a parent [ViewGroup].
	 *
	 * This is a utility function to simplify finding a view within a layout.
	 *
	 * @param parentLayout The parent [ViewGroup] to search within. Can be null.
	 * @param id The integer ID ([IdRes]) of the view to find.
	 * @return The [View] with the given ID, or null if not found or if [parentLayout] is null.
	 */
	@JvmStatic
	fun getView(parentLayout: View?, @IdRes id: Int): View? {
		return parentLayout?.findViewById(id)
	}
	
	/**
	 * Starts an infinite clockwise rotation animation on the given [view].
	 * The animation is loaded from the specified animation resource.
	 *
	 * @param activity The [Activity] context (used via [WeakReference]).
	 * @param view The [View] to animate.
	 */
	@JvmStatic
	fun animateInfiniteRotation(activity: Activity?, view: View?) {
		WeakReference(activity).get()?.let { safeContextRef ->
			val animResId = R.anim.anim_rotate_clockwise
			val animation = loadAnimation(safeContextRef, animResId)
			view?.startAnimation(animation)
		}
	}
	
	/**
	 * Converts Density Pixels (DP) to Pixels (PX).
	 *
	 * This utility function helps in converting density-independent units
	 * to device-specific pixel units.
	 *
	 * @param context The [Context] to retrieve display metrics.
	 * @param dp The value in DP to convert.
	 * @return The equivalent value in PX, or -1 if the context or display metrics are null.
	 */
	@JvmStatic
	fun dpToPx(context: Context?, dp: Float): Int {
		val metrics = WeakReference(context).get()?.resources?.displayMetrics
		return if (metrics != null) {
			Math.round(dp * (metrics.densityDpi / 160f))
		} else -1
	}
	
	/**
	 * Converts Pixels (PX) to Density Pixels (DP).
	 *
	 * This utility function helps in converting device-specific pixel units
	 * to density-independent units.
	 *
	 * @param context The [Context] to retrieve display metrics.
	 * @param px The value in PX to convert.
	 * @return The equivalent value in DP, or -1.0f if the context or display metrics are null.
	 */
	@JvmStatic
	fun pxToDp(context: Context?, px: Int): Float {
		val metrics = WeakReference(context).get()?.resources?.displayMetrics
		return if (metrics != null) {
			px / (metrics.densityDpi / 160f)
		} else -1.0f
	}
	
	/**
	 * Shows the on-screen keyboard for the given [view].
	 *
	 * This function requests focus on the [view] and then uses the
	 * [InputMethodManager] to make the soft keyboard visible.
	 *
	 * @param activity The [Activity] context (used via [WeakReference]).
	 * @param view The [View] that should receive focus and trigger the keyboard.
	 */
	@JvmStatic
	fun showOnScreenKeyboard(activity: Activity?, view: View?) {
		WeakReference(activity).get()?.let { safeContextRef ->
			val inputService = safeContextRef.getSystemService(INPUT_METHOD_SERVICE)
			val inputMethodManager = inputService as InputMethodManager
			view?.requestFocus()
			inputMethodManager.showSoftInput(view, 0)
		}
	}
	
	/**
	 * Hides the on-screen keyboard.
	 *
	 * This function uses the [InputMethodManager] to hide the soft keyboard
	 * that is currently associated with the window token of the provided [focusedView].
	 *
	 * @param activity The [Activity] context (used via [WeakReference]).
	 * @param focusedView The [View] that currently has focus (its window token is used).
	 */
	@JvmStatic
	fun hideOnScreenKeyboard(activity: Activity?, focusedView: View?) {
		WeakReference(activity).get()?.let { safeContextRef ->
			if (focusedView != null) {
				val service = safeContextRef.getSystemService(INPUT_METHOD_SERVICE)
				val inputMethodManager = service as InputMethodManager
				inputMethodManager.hideSoftInputFromWindow(focusedView.windowToken, 0)
			}
		}
	}
	
	/**
	 * Tints the given [Drawable] with the application's primary color.
	 *
	 * This function uses [DrawableCompat.setTint] to change the color of the
	 * [targetDrawable] to the color defined in `R.color.color_primary`.
	 *
	 * @param targetDrawable The [Drawable] to tint. If null, the function does nothing.
	 */
	@JvmStatic
	fun tintDrawableWithPrimaryColor(targetDrawable: Drawable?) {
		if (targetDrawable == null) return
		val tintColor = getColor(AIOApp.INSTANCE, R.color.color_primary)
		DrawableCompat.setTint(targetDrawable, tintColor)
	}
	
	/**
	 * Tints the given [Drawable] with the application's secondary color.
	 *
	 * This function uses [DrawableCompat.setTint] to change the color of the
	 * [targetDrawable] to the color defined in `R.color.color_secondary`.
	 *
	 * @param targetDrawable The [Drawable] to tint. If null, the function does nothing.
	 */
	@JvmStatic
	fun tintDrawableWithSecondaryColor(targetDrawable: Drawable?) {
		if (targetDrawable == null) return
		val tintColor = getColor(AIOApp.INSTANCE, R.color.color_secondary)
		DrawableCompat.setTint(targetDrawable, tintColor)
	}
	
	/**
	 * Tints the given [Drawable] with the color specified by the [colorResId].
	 *
	 * This function uses [DrawableCompat.setTint] to change the color of the
	 * [targetDrawable] to the color defined by the provided resource ID.
	 *
	 * @param targetDrawable The [Drawable] to tint. If null, the function does nothing.
	 * @param colorResId The resource ID of the color to use for tinting.
	 */
	@JvmStatic
	fun tintDrawableWithProvidedColor(targetDrawable: Drawable?, colorResId: Int) {
		if (targetDrawable == null) return
		val tintColor = getColor(AIOApp.INSTANCE, colorResId)
		DrawableCompat.setTint(targetDrawable, tintColor)
	}
	
	/**
	 * Checks if the on-screen keyboard is currently visible.
	 *
	 * This function determines the visibility of the keyboard by comparing the
	 * height of the visible display frame with the total screen height. If the
	 * difference is significant (greater than 100 pixels), it's assumed the keyboard is visible.
	 *
	 * @param activity The [Activity] context (used via [WeakReference]).
	 * @return `true` if the on-screen keyboard is visible, `false` otherwise.
	 */
	@JvmStatic
	fun isOnScreenKeyboardVisible(activity: Activity?): Boolean {
		WeakReference(activity).get()?.let { safeContextRef ->
			val rootView = safeContextRef.findViewById<View>(android.R.id.content)
			val rect = Rect()
			rootView?.getWindowVisibleDisplayFrame(rect)
			val screenHeight = rootView?.rootView?.height
			val keypadHeight = screenHeight?.minus(rect.bottom)
			return if (keypadHeight != null) keypadHeight > 100 else false
		} ?: run { return false }
	}
	
	/**
	 * Sets the visibility of multiple [View]s to either [VISIBLE] or [GONE].
	 *
	 * @param isVisible `true` to set visibility to [VISIBLE], `false` to set to [GONE].
	 * @param views A vararg of [View]s whose visibility needs to be set. Null views are ignored.
	 */
	@JvmStatic
	fun setViewsVisibility(isVisible: Boolean, vararg views: View?) {
		val visibility = if (isVisible) VISIBLE else GONE
		for (view in views) view?.visibility = visibility
	}
	
	/**
	 * Sets the visibility of multiple [View]s to a specific visibility state.
	 *
	 * @param visibility The desired visibility state ([VISIBLE], [GONE], or [INVISIBLE]).
	 * @param views A vararg of [View]s whose visibility needs to be set. Null views are ignored.
	 */
	@JvmStatic
	fun setViewsVisibility(visibility: Int, vararg views: View?) {
		for (view in views) view?.visibility = visibility
	}
	
	/**
	 * Toggles the visibility of a [targetView] with an optional fade animation.
	 *
	 * @param targetView The [View] whose visibility needs to be toggled.
	 * @param shouldAnimate If `true`, a fade-in/fade-out animation will be used.
	 * @param animTimeout The duration of the animation in milliseconds (default: 300ms).
	 */
	@JvmStatic
	fun toggleViewVisibility(
		targetView: View,
		shouldAnimate: Boolean = false,
		animTimeout: Long = 300
	) {
		if (shouldAnimate) {
			if (targetView.isVisible) {
				targetView.animate().alpha(0f)
					.setDuration(animTimeout)
					.withEndAction { targetView.visibility = GONE }
				
			} else {
				targetView.alpha = 0f
				targetView.visibility = VISIBLE
				targetView.animate().alpha(1f).setDuration(animTimeout)
			}
		} else {
			targetView.visibility = if (targetView.isVisible) GONE
			else VISIBLE
		}
	}
	
	/**
	 * Animates a view by scaling it up slightly and then fading it out.
	 *
	 * @param targetView The [View] to animate. Can be null, in which case nothing happens.
	 * @param duration The duration of the animation in milliseconds.
	 */
	@JvmStatic
	fun animatePopAndFadeOut(targetView: View?, duration: Long) {
		val scaleUpX = ofFloat(targetView, "scaleX", 1f, 1.2f)
		val scaleUpY = ofFloat(targetView, "scaleY", 1f, 1.2f)
		val fadeOut = ofFloat(targetView, "alpha", 1f, 0f)
		val animatorSet = AnimatorSet()
		animatorSet.playTogether(scaleUpX, scaleUpY, fadeOut)
		animatorSet.duration = duration
		animatorSet.start()
	}
	
	/**
	 * Animates a view by fading it out and sliding it to the left.
	 *
	 * @param targetView The [View] to animate. If null, the function returns immediately.
	 * @param duration The duration of the animation in milliseconds (default: 300ms).
	 */
	@JvmStatic
	fun fadeOutAndSlideLeft(targetView: View?, duration: Long = 300) {
		if (targetView == null) return
		val fadeOut = ofFloat(targetView, "alpha", 1f, 0f)
		val slideLeft = ofFloat(targetView, "translationX", 0f, -targetView.width.toFloat())
		val animatorSet = AnimatorSet()
		animatorSet.playTogether(fadeOut, slideLeft)
		animatorSet.duration = duration
		animatorSet.start()
		animatorSet.addListener(onEnd = { targetView.visibility = INVISIBLE })
	}
	
	/**
	 * Animates a view by sliding it in from the left and fading it in.
	 *
	 * @param targetView The [View] to animate. If null, the function returns immediately.
	 * @param duration The duration of the animation in milliseconds (default: 300ms).
	 */
	@JvmStatic
	fun slideInFromLeftAndFadeIn(targetView: View?, duration: Long = 300) {
		if (targetView == null) return
		targetView.translationX = -targetView.width.toFloat()
		targetView.alpha = 0f
		targetView.visibility = VISIBLE
		val slideIn = ofFloat(targetView, "translationX", -targetView.width.toFloat(), 0f)
		val fadeIn = ofFloat(targetView, "alpha", 0f, 1f)
		val animatorSet = AnimatorSet()
		animatorSet.playTogether(slideIn, fadeIn)
		animatorSet.duration = duration
		animatorSet.start()
	}
	
	/**
	 * Animates a view by fading it out and sliding it up.
	 *
	 * @param targetView The [View] to animate. If null, the function returns immediately.
	 * @param duration The duration of the animation in milliseconds (default: 300ms).
	 */
	@JvmStatic
	fun fadeOutAndSlideUp(targetView: View?, duration: Long = 300) {
		if (targetView == null) return
		
		val fadeOut = ofFloat(targetView, "alpha", 1f, 0f)
		val slideUp = ofFloat(targetView, "translationY", 0f, -targetView.height.toFloat())
		
		AnimatorSet().apply {
			playTogether(fadeOut, slideUp)
			this.duration = duration
			addListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					targetView.visibility = INVISIBLE
				}
			})
			start()
		}
	}
	
	/**
	 * Animates a view by fading it in and sliding it down.
	 *
	 * @param targetView The [View] to animate. If null, the function returns immediately.
	 * @param duration The duration of the animation in milliseconds (default: 300ms).
	 */
	@JvmStatic
	fun fadeInAndSlideDown(targetView: View?, duration: Long = 300) {
		if (targetView == null) return
		
		targetView.translationY = -targetView.height.toFloat()
		targetView.alpha = 0f
		targetView.visibility = VISIBLE
		
		val fadeIn = ofFloat(targetView, "alpha", 0f, 1f)
		val slideDown = ofFloat(targetView, "translationY", -targetView.height.toFloat(), 0f)
		
		AnimatorSet().apply {
			playTogether(fadeIn, slideDown)
			this.duration = duration
			start()
		}
	}
	
	/**
	 * Hides a [targetView] with an optional fade-out animation.
	 *
	 * @param targetView The [View] to hide.
	 * @param shouldAnimate If `true`, a fade-out animation will be used.
	 * @param animTimeout The duration of the animation in milliseconds (default: 500ms).
	 */
	@JvmStatic
	fun hideView(
		targetView: View,
		shouldAnimate: Boolean = false,
		animTimeout: Long = 500
	) {
		if (!targetView.isVisible) return
		if (shouldAnimate) {
			targetView.animate().alpha(0f)
				.setDuration(animTimeout)
				.withEndAction { targetView.visibility = GONE }
		} else targetView.visibility = GONE
	}
	
	/**
	 * Shows a [targetView] with an optional fade-in animation.
	 *
	 * @param targetView The [View] to show.
	 * @param shouldAnimate If `true`, a fade-in animation will be used.
	 * @param animTimeout The duration of the animation in milliseconds (default: 500ms).
	 */
	@JvmStatic
	fun showView(
		targetView: View,
		shouldAnimate: Boolean = false,
		animTimeout: Long = 500
	) {
		if (targetView.isVisible) return
		if (shouldAnimate) {
			targetView.alpha = 0f
			targetView.visibility = VISIBLE
			targetView.animate().alpha(1f).setDuration(animTimeout)
		} else targetView.visibility = VISIBLE
	}
	
	/**
	 * Gets the height of the top cutout (notch) of the device display.
	 *
	 * This function retrieves the top safe inset from the displayCutout
	 * to determine the height of the area that might be occluded by a display cutout.
	 *
	 * @param activity The [Activity] context (used via [WeakReference]).
	 * @return The height of the top cutout in pixels, or 0 if there is no cutout
	 * or if the activity is null.
	 */
	@JvmStatic
	fun getTopCutoutHeight(activity: Activity?): Int {
		WeakReference(activity).get()?.let { safeContextRef ->
			val windowInsets = safeContextRef.window?.decorView?.rootWindowInsets
			val displayCutout = windowInsets?.displayCutout
			return displayCutout?.safeInsetTop ?: 0
		} ?: run { return 0 }
	}
	
	/**
	 * Sets the top margin of a [view] to accommodate a display cutout (notch).
	 *
	 * This function retrieves the top cutout height from the activity and applies
	 * it as the top margin to the provided view's layout parameters.
	 *
	 * @param view The [View] to adjust the top margin for. If null, the function does nothing.
	 * @param activity The [Activity] context (used via [WeakReference]).
	 */
	@JvmStatic
	fun setTopMarginWithCutout(view: View?, activity: Activity?) {
		if (view == null) return
		WeakReference(activity).get()?.let { safeContextRef ->
			val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
			val topCutoutHeight = getTopCutoutHeight(safeContextRef)
			layoutParams.topMargin = topCutoutHeight
			view.layoutParams = layoutParams
		}
	}
	
	/**
	 * Enables or disables multiple [View]s.
	 *
	 * @param enabled `true` to enable the views, `false` to disable them.
	 * @param views A vararg of [View]s to enable or disable. Null views are ignored.
	 */
	@JvmStatic
	fun setViewsEnabled(enabled: Boolean, vararg views: View?) {
		for (view in views) if (view != null) view.isEnabled = enabled
	}
	
	/**
	 * Sets the transparency (alpha) of multiple [View]s.
	 *
	 * @param alpha The alpha value to set (0.0f for fully transparent, 1.0f for fully opaque).
	 * @param views A vararg of [View]s to set the transparency of. Null views are ignored.
	 */
	@JvmStatic
	fun setViewsTransparency(alpha: Float, vararg views: View?) {
		for (view in views) if (view != null) view.alpha = alpha
	}
	
	/**
	 * Checks if a [targetView] is currently visible.
	 *
	 * This is a simple wrapper around the [View.isVisible] property for direct access.
	 *
	 * @param targetView The [View] to check.
	 * @return `true` if the view is visible, `false` otherwise.
	 */
	@JvmStatic
	fun isViewVisible(targetView: View): Boolean = targetView.isVisible
	
	/**
	 * Recursively finds a [View] within a view hierarchy by its tag.
	 *
	 * This function performs a depth-first search to locate a view with the specified tag.
	 *
	 * @param rootView The root [View] of the hierarchy to search within. Can be null.
	 * @param tag The tag to search for.
	 * @return The [View] with the matching tag, or null if not found or if [rootView] is null.
	 */
	@JvmStatic
	fun findViewByTag(rootView: View?, tag: Any): View? {
		if (rootView == null) return null
		if (tag == rootView.tag) return rootView
		if (rootView is ViewGroup) {
			for (index in 0 until rootView.childCount) {
				val child = findViewByTag(rootView.getChildAt(index), tag)
				if (child != null) {
					return child
				}
			}
		}
		return null
	}
	
	/**
	 * Measures the size (width and height) of a [view].
	 *
	 * This function measures the view with unspecified constraints to determine its
	 * intrinsic size.  Use this to get the view's desired size before layout.
	 *
	 * @param view The [View] to measure.
	 * @return An [IntArray] containing the measured width and height in pixels,
	 * or an empty array if the view is null.
	 */
	@JvmStatic
	fun measureViewSize(view: View): IntArray {
		view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
		return intArrayOf(view.measuredWidth, view.measuredHeight)
	}
	
	/**
	 * Animates the visibility of a [targetView] with a fade-in or fade-out effect.
	 *
	 * @param targetView The [View] whose visibility needs to be animated.
	 * @param visibility The target visibility state ([VISIBLE], [GONE], or [INVISIBLE]).
	 * @param duration The duration of the animation in milliseconds.
	 */
	@JvmStatic
	fun animateViewVisibility(
		targetView: View,
		visibility: Int,
		duration: Int
	) {
		val alpha = if (visibility == VISIBLE) 1f else 0f
		targetView
			.animate()
			.alpha(alpha)
			.setDuration(duration.toLong())
			.withStartAction { if (visibility == VISIBLE) targetView.visibility = VISIBLE }
			.withEndAction { if (visibility != VISIBLE) targetView.visibility = visibility }
	}
	
	/**
	 * Applies a shake animation to a [targetView].
	 *
	 * This function creates a horizontal shake animation using ObjectAnimator
	 * and applies it to the view. The animation shakes the view back and forth
	 * for the specified duration.
	 *
	 * @param targetView The [View] to apply the shake animation to.
	 * @param durationOfShake The duration of each individual shake movement in milliseconds.
	 * @param durationOfAnim The total duration of the entire shake animation sequence in milliseconds.
	 */
	@JvmStatic
	fun shakeAnimationOnView(
		targetView: View,
		durationOfShake: Long,
		durationOfAnim: Long
	) {
		val shakeX = ofFloat(
			targetView, "translationX",
			0f, 10f, -10f, 10f, -10f, 5f, -5f, 0f
		)
		shakeX.duration = durationOfShake
		
		val animatorSet = AnimatorSet()
		animatorSet.play(shakeX)
		
		val handler = Handler(Looper.getMainLooper())
		val endTime = System.currentTimeMillis() + durationOfAnim
		
		fun startShaking() {
			if (System.currentTimeMillis() < endTime) {
				animatorSet.start()
				handler.postDelayed({ startShaking() }, shakeX.duration)
			}
		}
		
		startShaking()
	}
	
	/**
	 * Animates a view with a fade-in and fade-out effect, repeating infinitely.
	 *
	 * This function uses the older [AlphaAnimation] class.  For newer code,
	 * consider using ViewPropertyAnimator.
	 *
	 * @param targetView The [View] to animate. If null, the function does nothing.
	 */
	@JvmStatic
	fun animateFadInOutAnim(targetView: View?) {
		if (targetView == null) return
		val anim = AlphaAnimation(0f, 1f)
		anim.duration = 500
		anim.repeatCount = Animation.INFINITE
		anim.repeatMode = Animation.REVERSE
		targetView.startAnimation(anim)
	}
	
	/**
	 * Stops any running animation on the given [view].
	 *
	 * This function uses the older [View.clearAnimation] method.  For ObjectAnimator
	 * animations, you should also use view.animate().cancel().
	 *
	 * @param view The [View] to stop the animation on.
	 */
	@JvmStatic
	fun closeAnyAnimation(view: View) = view.clearAnimation()
	
	/**
	 * Fades out a view with an optional callback when the animation ends.
	 *
	 * This function uses the older [AlphaAnimation] class.  For newer code,
	 * consider using ViewPropertyAnimator.
	 *
	 * @param view The [View] to fade out.
	 * @param duration The duration of the fade-out animation in milliseconds (default: 300ms).
	 * @param onAnimationEnd An optional callback to execute when the animation ends.
	 */
	@JvmStatic
	fun fadeOutView(
		view: View, duration: Long = 300L,
		onAnimationEnd: (() -> Unit)? = null
	) {
		val fadeOut = AlphaAnimation(1.0f, 0.0f).apply {
			this.duration = duration
			fillAfter = true
			setAnimationListener(object : Animation.AnimationListener {
				override fun onAnimationStart(animation: Animation?) {}
				override fun onAnimationEnd(animation: Animation?) {
					onAnimationEnd?.invoke()
				}
				
				override fun onAnimationRepeat(animation: Animation?) {}
			})
		}; view.startAnimation(fadeOut)
	}
	
	/**
	 * Sets the text of a [TextView] within an [Activity].
	 *
	 * @param activity The [Activity] context (used via [WeakReference]).
	 * @param id The ID of the [TextView] to set the text on.
	 * @param text The text to set.
	 */
	@JvmStatic
	fun setTextViewText(activity: Activity?, @IdRes id: Int, text: String) {
		WeakReference(activity).get()?.let { safeContextRef ->
			safeContextRef.findViewById<TextView>(id)?.text = text
		}
	}
	
	/**
	 * Sets the drawable of an [ImageView] within an [Activity].
	 *
	 * @param activity The [Activity] context (used via [WeakReference]).
	 * @param id The ID of the [ImageView] to set the drawable on.
	 * @param drawable The [Drawable] to set.
	 */
	@JvmStatic
	fun setImageViewDrawable(
		activity: Activity?, @IdRes id: Int, drawable: Drawable?
	) {
		WeakReference(activity).get()?.let { safeContextRef ->
			safeContextRef.findViewById<ImageView>(id)?.setImageDrawable(drawable)
		}
	}
	
	/**
	 * Gets the text of a [TextView] within an [Activity].
	 *
	 * @param activity The [Activity] context (used via [WeakReference]).
	 * @param id The ID of the [TextView] to get the text from.
	 * @return The text of the [TextView] as a [String], or an empty string if the
	 * Activity or TextView is null.
	 */
	@JvmStatic
	fun getTextViewText(activity: Activity?, @IdRes id: Int): String {
		WeakReference(activity).get()?.let { safeContextRef ->
			return safeContextRef.findViewById<TextView>(id)?.text.toString()
		} ?: run { return "" }
	}
	
	/**
	 * Gets the bitmap from an [ImageView] within an [Activity].
	 *
	 * This function retrieves the drawable from the ImageView and, if it's a
	 * BitmapDrawable, returns the underlying Bitmap.
	 *
	 * @param activity The [Activity] context (used via [WeakReference]).
	 * @param id The ID of the [ImageView] to get the bitmap from.
	 * @return The [Bitmap] from the ImageView, or null if the Activity, ImageView,
	 * or drawable is null, or if the drawable is not a BitmapDrawable.
	 */
	@JvmStatic
	fun getImageViewBitmap(activity: Activity?, @IdRes id: Int): Bitmap? {
		WeakReference(activity).get()?.let { safeContextRef ->
			val imageView = safeContextRef.findViewById<ImageView>(id)
			if (imageView != null) {
				val drawable = imageView.drawable
				if (drawable is BitmapDrawable) return drawable.bitmap
			}; return null
		} ?: run { return null }
	}
	
	/**
	 * Gets the ID of a [View].
	 *
	 * This function simply returns the ID of the provided view.
	 *
	 * @param view The [View] to get the ID from.
	 * @return The ID of the [View], or null if the view is null.
	 */
	@JvmStatic
	fun getId(view: View?): Int? = view?.id
	
	/**
	 * Loads a thumbnail image from a URL and sets it to an ImageView. If the image is
	 * in portrait orientation, it rotates the image 90 degrees before displaying it.
	 * If an error occurs during loading, it sets a placeholder image.
	 *
	 * @param thumbnailUrl The URL of the thumbnail image to load.
	 * @param targetImageView The ImageView where the thumbnail will be displayed.
	 * @param placeHolderDrawableId An optional placeholder drawable resource ID to
	 * display if the image fails to load.
	 */
	@JvmStatic
	fun loadThumbnailFromUrl(
		thumbnailUrl: String,
		targetImageView: ImageView,
		placeHolderDrawableId: Int? = null
	) {
		// Execute the image loading in a background thread
		ThreadsUtility.executeInBackground(codeBlock = {
			try {
				// Create a connection to the image URL
				val url = URL(thumbnailUrl)
				val connection = url.openConnection() as HttpURLConnection
				connection.doInput = true
				connection.connect()
				
				// Get the input stream and decode the image into a bitmap
				val input: InputStream = connection.inputStream
				val bitmap = decodeStream(input)
				
				// Check if the image is in portrait orientation (height > width)
				val isPortrait = bitmap.height > bitmap.width
				
				// Rotate the bitmap if it is portrait
				val rotatedBitmap = if (isPortrait) {
					rotateBitmap(bitmap, 90f)
				} else bitmap
				
				// Once the image is processed, update the UI on the main thread
				ThreadsUtility.executeOnMain {
					Glide.with(targetImageView.context)
						.load(rotatedBitmap).into(targetImageView)
				}
			} catch (error: Exception) {
				error.printStackTrace() // Print error stack trace in case of failure
				// Set placeholder image if provided, or leave it unchanged
				if (placeHolderDrawableId != null) {
					targetImageView.setImageResource(placeHolderDrawableId)
				}
			}
		})
	}
	
	/**
	 * Rotates a bitmap by a given angle.
	 *
	 * This method creates a new rotated bitmap using a matrix transformation.
	 * It also recycles the original bitmap if it's no longer needed to free up memory.
	 *
	 * @param bitmap The original bitmap to rotate.
	 * @param angle The angle (in degrees) to rotate the bitmap.
	 * @return A new bitmap rotated by the specified angle.
	 */
	@JvmStatic
	fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
		// Create a matrix for rotation with the specified angle
		val matrix = Matrix().apply { postRotate(angle) }
		
		// Create a rotated bitmap with the same configuration as the original
		val rotatedBitmap = Bitmap.createBitmap(
			bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
		)
		
		// Recycle the original bitmap to free up memory if it's no longer needed
		if (!bitmap.isRecycled) {
			bitmap.recycle()
		}
		
		return rotatedBitmap
	}
	
	/**
	 * Retrieves a thumbnail image for a given file, either from the file itself or a provided URL.
	 *
	 * The method handles different file types, including audio, image, APK, and video files. It uses the
	 * file's name to determine the type and attempts to extract a corresponding thumbnail:
	 * - For audio files, it attempts to extract the album art.
	 * - For image files, it scales the image to the required width.
	 * - For APK files, it attempts to extract the app's icon.
	 * - For video files, it attempts to retrieve a frame from the video file or uses a provided URL.
	 *
	 * If no thumbnail is available from these sources, the method returns null.
	 *
	 * @param targetFile The file for which the thumbnail is being requested.
	 * @param thumbnailUrl An optional URL to an external thumbnail to be used if available.
	 * @param requiredThumbWidth The required width for the thumbnail. The height will be adjusted
	 *                           to maintain the aspect ratio.
	 * @return A Bitmap representing the thumbnail image, or null if no thumbnail is found.
	 */
	@JvmStatic
	fun getThumbnailFromFile(
		targetFile: File,
		thumbnailUrl: String? = null,
		requiredThumbWidth: Int
	): Bitmap? {
		// Check if the file is audio and attempt to extract album art
		if (FileSystemUtility.isAudioByName(targetFile.name)) {
			extractAudioAlbumArt(targetFile)?.let { return it }
		}
		
		// Check if the file is an image and retrieve the bitmap, scaling it to the required width
		else if (FileSystemUtility.isImageByName(targetFile.name)) {
			getBitmapFromFile(imageFile = targetFile)?.let {
				return scaleBitmap(it, requiredThumbWidth)
			}
		}
		
		// If the file is an APK, retrieve its icon/thumbnail
		else if (targetFile.name.lowercase().endsWith("apk")) {
			getApkThumbnail(targetFile, onApkIconFound = { bitmap ->
				return@getApkThumbnail bitmap
			})
		}
		
		// For video files or unknown types, attempt to extract a frame as the thumbnail
		val retriever = MediaMetadataRetriever()
		try {
			var originalBitmap: Bitmap? = null
			// Attempt to use the provided thumbnail URL
			if (!thumbnailUrl.isNullOrEmpty()) {
				originalBitmap = getBitmapFromThumbnailUrl(thumbnailUrl)
			}
			
			// If no URL, try extracting a frame from the video file itself
			if (originalBitmap == null) {
				retriever.setDataSource(targetFile.absolutePath)
				originalBitmap = retriever
					.getFrameAtTime(5_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
					?: retriever.frameAtTime
			}
			
			// Scale the bitmap to the required width, maintaining the aspect ratio
			originalBitmap?.let {
				val aspectRatio = it.height.toFloat() / it.width
				val targetHeight = (requiredThumbWidth * aspectRatio).toInt()
				return it.scale(requiredThumbWidth, targetHeight, false)
			}
		} catch (error: Exception) {
			error.printStackTrace()
			
		} finally {
			retriever.release()
		}
		
		return null  // Return null if no thumbnail could be found
	}
	
	/**
	 * Attempts to extract and display the application icon from an APK file.
	 * Falls back to a default drawable if extraction fails.
	 *
	 * @param apkFile The APK file to extract the icon from.
	 * @param imageViewHolder The ImageView in which to display the icon.
	 * @param defaultThumbDrawable Drawable to use if icon extraction fails.
	 * @return True if the icon was successfully extracted and set, false otherwise.
	 */
	@JvmStatic
	fun getApkThumbnail(
		apkFile: File,
		imageViewHolder: ImageView? = null,
		defaultThumbDrawable: Drawable? = null,
		onApkIconFound: ((Bitmap) -> Bitmap)? = null
	): Boolean {
		val apkExtension = ".apk".lowercase(Locale.ROOT)
		if (!apkFile.exists() || !apkFile.name.endsWith(apkExtension)) {
			imageViewHolder?.setImageDrawable(defaultThumbDrawable)
			return false
		}
		
		val packageManager = AIOApp.INSTANCE.packageManager
		return try {
			val apkPath = apkFile.absolutePath
			val packageInfo = packageManager.getPackageArchiveInfo(
				apkPath, PackageManager.GET_ACTIVITIES
			)
			
			packageInfo?.applicationInfo?.let { appInfo ->
				appInfo.sourceDir = apkPath
				appInfo.publicSourceDir = apkPath
				val drawableIcon = appInfo.loadIcon(packageManager)
				
				imageViewHolder?.setImageDrawable(drawableIcon)
				imageViewHolder?.scaleType = ImageView.ScaleType.CENTER_INSIDE
				imageViewHolder?.setPadding(0, 0, 0, 0)
				
				// Convert drawable to bitmap and return via callback
				val bitmap = drawableToBitmap(drawableIcon)
				if (bitmap != null) {
					onApkIconFound?.invoke(bitmap)
				}
				
				return true
			}
			
			imageViewHolder?.setImageDrawable(defaultThumbDrawable)
			false
		} catch (error: Exception) {
			error.printStackTrace()
			imageViewHolder?.apply {
				scaleType = ImageView.ScaleType.FIT_CENTER
				setPadding(0, 0, 0, 0)
				setImageDrawable(defaultThumbDrawable)
			}
			false
		}
	}
	
	/**
	 * Converts a [Drawable] to a [Bitmap]. If the [Drawable] is already a [BitmapDrawable],
	 * it returns the existing bitmap. Otherwise, it creates a new bitmap from the drawable's
	 * intrinsic width and height.
	 *
	 * @param drawable The [Drawable] to be converted into a [Bitmap]. This could be any type of drawable.
	 * @return A [Bitmap] representation of the drawable, or null if the conversion fails.
	 */
	@JvmStatic
	fun drawableToBitmap(drawable: Drawable): Bitmap? {
		// Check if the drawable is already a BitmapDrawable (no need to convert)
		if (drawable is BitmapDrawable) return drawable.bitmap
		
		// Set a default size if the drawable has no intrinsic size
		val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1
		val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1
		
		return try {
			// Create a bitmap with the drawable's intrinsic size
			val bitmap = createBitmap(width, height)
			val canvas = Canvas(bitmap)
			
			// Set bounds and draw the drawable onto the canvas
			drawable.setBounds(0, 0, canvas.width, canvas.height)
			drawable.draw(canvas)
			
			// Return the created bitmap
			bitmap
		} catch (error: Exception) {
			// Handle any exception that may occur during bitmap creation
			error.printStackTrace()
			null
		}
	}
	
	/**
	 * Scales the given [targetBitmap] to the specified width while maintaining aspect ratio.
	 * The scaling is done using a memory-efficient approach by avoiding filter scaling unless needed.
	 *
	 * @param targetBitmap The original Bitmap to be scaled.
	 * @param requiredThumbWidth The target width of the thumbnail.
	 * @return A new scaled Bitmap with preserved aspect ratio.
	 */
	@JvmStatic
	fun scaleBitmap(targetBitmap: Bitmap, requiredThumbWidth: Int): Bitmap {
		if (requiredThumbWidth <= 0 || targetBitmap.width <= 0) return targetBitmap
		
		val aspectRatio = targetBitmap.height.toFloat() / targetBitmap.width
		val targetHeight = (requiredThumbWidth * aspectRatio).toInt()
		
		// Avoid unnecessary scaling if dimensions are same
		if (targetBitmap.width == requiredThumbWidth && targetBitmap.height == targetHeight) {
			return targetBitmap
		}
		
		// Use createScaledBitmap directly with "filter = false" to reduce memory overhead
		return targetBitmap.scale(requiredThumbWidth, targetHeight, false)
	}
	
	/**
	 * Extracts embedded album art (if any) from the specified audio file as a Bitmap.
	 *
	 * Uses MediaMetadataRetriever to access the embedded image bytes and decodes them efficiently.
	 * Properly releases retriever resources and ensures memory usage is kept minimal.
	 *
	 * @param audioFile The audio file to extract album art from.
	 * @return A Bitmap of the embedded album art, or null if not present or on error.
	 */
	@JvmStatic
	fun extractAudioAlbumArt(audioFile: File): Bitmap? {
		if (!audioFile.exists()) return null
		
		val retriever = MediaMetadataRetriever()
		return try {
			retriever.setDataSource(audioFile.absolutePath)
			val embeddedPicture = retriever.embeddedPicture ?: return null
			
			// Use inJustDecodeBounds to avoid decoding large images unnecessarily
			val optionsBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
			decodeByteArray(embeddedPicture, 0, embeddedPicture.size, optionsBounds)
			
			// Define a reasonable max dimension for album art (e.g., 512x512)
			val maxSize = 412
			val scale = maxOf(1, maxOf(optionsBounds.outWidth, optionsBounds.outHeight) / maxSize)
			
			val decodeOptions = BitmapFactory.Options().apply {
				inSampleSize = scale
			}
			
			decodeByteArray(embeddedPicture, 0, embeddedPicture.size, decodeOptions)
		} catch (error: Exception) {
			error.printStackTrace()
			null
		} finally {
			retriever.release()
		}
	}
	
	/**
	 * Downloads and decodes a Bitmap from a given image URL if valid.
	 *
	 * This function opens a network connection, validates the response as an image,
	 * and decodes it into a Bitmap. It ensures streams and connections are properly closed,
	 * preventing memory leaks and resource leaks.
	 *
	 * @param thumbnailUrl The URL string pointing to the thumbnail image.
	 * @return The decoded Bitmap, or null if the URL is invalid or decoding fails.
	 */
	@JvmStatic
	fun getBitmapFromThumbnailUrl(thumbnailUrl: String?): Bitmap? {
		if (thumbnailUrl.isNullOrEmpty()) return null
		
		var connection: HttpURLConnection? = null
		var inputStream: InputStream? = null
		
		return try {
			val url = URL(thumbnailUrl)
			connection = url.openConnection() as? HttpURLConnection
			connection?.apply {
				connectTimeout = 5000
				readTimeout = 5000
				doInput = true
				connect()
			}
			
			if (connection?.responseCode == HttpURLConnection.HTTP_OK &&
				connection.contentType?.startsWith("image/") == true
			) {
				inputStream = BufferedInputStream(connection.inputStream)
				decodeStream(inputStream)
			} else {
				null
			}
		} catch (error: Exception) {
			error.printStackTrace()
			null
		} finally {
			try {
				inputStream?.close()
			} catch (_: IOException) {
			}
			connection?.disconnect()
		}
	}
	
	/**
	 * Saves the given [Bitmap] to the app's internal storage with the specified file name, format, and quality.
	 *
	 * @param bitmapToSave The Bitmap to be saved.
	 * @param fileName The name of the file (without path) to save the bitmap as.
	 * @param format The image format to use when saving. Defaults to [Bitmap.CompressFormat.JPEG].
	 * @param quality The quality of the compressed image (0–100). Lower values reduce file size. Defaults to 60.
	 * @return The absolute path to the saved file, or null if saving failed.
	 */
	@JvmStatic
	fun saveBitmapToFile(
		bitmapToSave: Bitmap,
		fileName: String,
		format: CompressFormat = CompressFormat.JPEG,
		quality: Int = 60
	): String? {
		return try {
			val modePrivate = Context.MODE_PRIVATE
			val appContext = AIOApp.INSTANCE
			appContext.openFileOutput(fileName, modePrivate).use { outputStream ->
				// Compress and write bitmap to output stream
				if (!bitmapToSave.compress(format, quality, outputStream)) return null
			}
			
			"${appContext.filesDir}/$fileName"
		} catch (error: Throwable) {
			error.printStackTrace()
			null
		}
	}
	
	/**
	 * Loads a [Bitmap] from a given image [File].
	 *
	 * @param imageFile The image file from which the bitmap should be decoded.
	 * @return A [Bitmap] if the decoding is successful, or `null` if the file is invalid or unreadable.
	 */
	@JvmStatic
	fun getBitmapFromFile(imageFile: File): Bitmap? {
		return try {
			if (imageFile.exists() && imageFile.isFile) {
				decodeFile(imageFile.absolutePath)
			} else {
				null
			}
		} catch (error: Exception) {
			error.printStackTrace()
			null
		}
	}
	
	/**
	 * Checks whether a given image file is entirely black by analyzing its pixels.
	 *
	 * This method loads a scaled-down version of the image to reduce memory usage,
	 * then iterates over its pixels to determine if every one of them is black.
	 *
	 * @param targetImageFile The image file to check.
	 * @return True if the image is completely black, false otherwise or if loading fails.
	 */
	@JvmStatic
	fun isBlackThumbnail(targetImageFile: File?): Boolean {
		if (targetImageFile == null || !targetImageFile.exists()) return false
		
		// Step 1: Decode image bounds only (no memory allocation for pixels)
		val options = BitmapFactory.Options().apply {
			inJustDecodeBounds = true
		}
		decodeFile(targetImageFile.absolutePath, options)
		
		// Step 2: Calculate downscale factor to reduce memory footprint
		val maxSize = 64
		val scale = maxOf(1, maxOf(options.outWidth, options.outHeight) / maxSize)
		
		// Step 3: Decode scaled-down bitmap for analysis
		val decodeOptions = BitmapFactory.Options().apply {
			inSampleSize = scale
		}
		
		val bitmap = decodeFile(targetImageFile.absolutePath, decodeOptions)
			?: return false
		
		// Step 4: Check if all pixels are black
		for (x in 0 until bitmap.width) {
			for (y in 0 until bitmap.height) {
				if (bitmap[x, y] != Color.BLACK) {
					bitmap.recycle() // Free memory
					return false
				}
			}
		}
		
		bitmap.recycle() // Always recycle bitmap to avoid memory leaks
		return true
	}
	
	/**
	 * Sets a drawable on the left side of a [TextView] using a given drawable resource ID.
	 *
	 * @receiver TextView on which the drawable will be set.
	 * @param drawableResIdRes The resource ID of the drawable to be displayed.
	 */
	@JvmStatic
	fun TextView.setLeftSideDrawable(drawableResIdRes: Int) {
		val drawable = getDrawable(AIOApp.INSTANCE, drawableResIdRes)
		drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
		this.setCompoundDrawables(drawable, null, null, null)
	}
	
	/**
	 * Matches the height of a [View] to the top cutout (notch) area if present.
	 *
	 * Should be used when designing layouts that adapt to phones with notches.
	 */
	@JvmStatic
	fun View.matchHeightToTopCutout() {
		doOnLayout { updateCutoutHeight() }
	}
	
	/**
	 * Updates the height of a [View] to match the height of the top cutout area (if any).
	 *
	 * This method checks the window insets for display cutouts and adjusts the view's layout params.
	 */
	@JvmStatic
	fun View.updateCutoutHeight() {
		val rootWindowInsets = rootWindowInsets
		val cutout = rootWindowInsets?.displayCutout
		
		if (cutout != null) {
			val cutoutHeight = cutout.boundingRects
				.firstOrNull { it.top == 0 }
				?.height() ?: 0
			
			val params = layoutParams
			params.height = cutoutHeight
			layoutParams = params
		}
	}
	
	/**
	 * Sets the text color of a [TextView] using a color resource ID.
	 *
	 * @receiver TextView whose text color will be changed.
	 * @param colorResId The color resource ID.
	 */
	@JvmStatic
	fun TextView.setTextColorKT(colorResId: Int) {
		this.setTextColor(AIOApp.INSTANCE.getColor(colorResId))
	}
	
	/**
	 * Converts an integer value in density-independent pixels (dp) to pixels (px).
	 *
	 * @receiver The dp value to convert.
	 * @return The corresponding px value as an integer.
	 */
	@JvmStatic
	fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
}