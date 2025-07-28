@file:Suppress("DEPRECATION")

package lib.ui

import android.app.Activity
import androidx.core.app.ActivityOptionsCompat
import com.aio.R
import java.lang.ref.WeakReference

/**
 * A utility object to handle different types of activity transition animations.
 *
 * This object provides a set of functions to apply various animation effects during activity transitions.
 * These animations are applied using the `overridePendingTransition` method, which specifies the animation
 * for entering and exiting the activity.
 */
object ActivityAnimator {
	
	/**
	 * Applies a fade-in and fade-out animation for the activity transition.
	 *
	 * @param activity The activity where the transition animation is applied.
	 */
	@JvmStatic
	fun animActivityFade(activity: Activity?) {
		WeakReference(activity).get()?.overridePendingTransition(
			R.anim.anim_fade_enter,
			R.anim.anim_fade_exit
		)
	}
	
	/**
	 * Applies an in-and-out slide animation for the activity transition.
	 *
	 * @param activity The activity where the transition animation is applied.
	 */
	@JvmStatic
	fun animActivityInAndOut(activity: Activity?) {
		WeakReference(activity).get()?.overridePendingTransition(
			R.anim.anim_in_out_enter,
			R.anim.anim_in_out_exit
		)
	}
	
	/**
	 * Applies a slide-down animation for the activity transition.
	 *
	 * @param activity The activity where the transition animation is applied.
	 */
	@JvmStatic
	fun animActivitySlideDown(activity: Activity?) {
		WeakReference(activity).get()?.overridePendingTransition(
			R.anim.anim_slide_down_enter,
			R.anim.anim_slide_down_exit
		)
	}
	
	/**
	 * Applies a slide-left animation for the activity transition.
	 *
	 * @param activity The activity where the transition animation is applied.
	 */
	@JvmStatic
	fun animActivitySlideLeft(activity: Activity?) {
		WeakReference(activity).get()?.overridePendingTransition(
			R.anim.anim_slide_left_enter,
			R.anim.anim_slide_left_exit
		)
	}
	
	/**
	 * Applies a swipe-right animation for the activity transition.
	 *
	 * @param activity The activity where the transition animation is applied.
	 */
	@JvmStatic
	fun animActivitySwipeRight(activity: Activity?) {
		WeakReference(activity).get()?.overridePendingTransition(
			R.anim.anim_swipe_right_enter,
			R.anim.anim_swipe_right_exit
		)
	}
	
	/**
	 * Applies a slide-up animation for the activity transition.
	 *
	 * @param activity The activity where the transition animation is applied.
	 */
	@JvmStatic
	fun animActivitySlideUp(activity: Activity?) {
		WeakReference(activity).get()?.overridePendingTransition(
			R.anim.anim_slide_up_enter,
			R.anim.anim_slide_up_exit
		)
	}
	
	/**
	 * Applies a swipe-left animation for the activity transition.
	 *
	 * @param activity The activity where the transition animation is applied.
	 */
	@JvmStatic
	fun animActivitySwipeLeft(activity: Activity?) {
		WeakReference(activity).get()?.overridePendingTransition(
			R.anim.anim_swipe_left_enter,
			R.anim.anim_swipe_left_exit
		)
	}
	
	/**
	 * Applies a slide-in-left and slide-out-right animation for the activity transition.
	 *
	 * @param activity The activity where the transition animation is applied.
	 */
	@JvmStatic
	fun animActivitySlideRight(activity: Activity?) {
		WeakReference(activity).get()?.overridePendingTransition(
			R.anim.anim_slide_in_left,
			R.anim.anim_slide_out_right
		)
	}
	
	/**
	 * Creates a material slide animation for activity transition.
	 * This uses a sliding in from the left and sliding out to the right animation.
	 *
	 * @param activity The activity where the transition animation is applied.
	 * @return An [ActivityOptionsCompat] object with the specified animation options, or `null` if the activity is null.
	 */
	@JvmStatic
	fun getMaterialSlideOptions(activity: Activity?): ActivityOptionsCompat? {
		return WeakReference(activity).get()?.let { safeContextRef ->
			ActivityOptionsCompat.makeCustomAnimation(
				safeContextRef,
				android.R.anim.slide_in_left,
				android.R.anim.slide_out_right
			)
		} ?: run { null }
	}
}