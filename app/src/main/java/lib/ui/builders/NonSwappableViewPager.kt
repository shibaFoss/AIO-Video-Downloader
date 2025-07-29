package lib.ui.builders

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * A custom ViewPager that disables swipe gestures.
 * Useful when you want to control page changes programmatically only.
 */
class NonSwappableViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {
	
	/**
	 * Prevents the ViewPager from intercepting touch events.
	 */
	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = false
	
	/**
	 * Prevents handling of touch events (no swipe effect).
	 */
	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(ev: MotionEvent): Boolean = false
}
