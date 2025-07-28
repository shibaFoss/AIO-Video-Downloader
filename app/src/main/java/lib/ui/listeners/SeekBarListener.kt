package lib.ui.listeners

import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener

/**
 * An abstract implementation of [OnSeekBarChangeListener] that simplifies the usage of SeekBar listeners.
 *
 * This class provides default implementations for `onStartTrackingTouch` and `onStopTrackingTouch`,
 * so you only need to override and implement the `onProgressChange` method to handle progress updates.
 *
 * Use this listener when you only care about the progress change and want to avoid dealing with the
 * start and stop tracking events.
 *
 * Example usage:
 * ```
 * seekBar.setOnSeekBarChangeListener(object : SeekBarListener() {
 *     override fun onProgressChange(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
 *         // Handle progress change
 *     }
 * })
 * ```
 */
abstract class SeekBarListener : OnSeekBarChangeListener {
	
	/**
	 * Called when the progress of the SeekBar has changed.
	 * This is the method that needs to be implemented by subclasses.
	 *
	 * @param seekBar The SeekBar whose progress has changed.
	 * @param progress The current progress of the SeekBar.
	 * @param fromUser True if the progress change was initiated by the user, false if it was programmatically.
	 */
	abstract fun onProgressChange(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
	
	/**
	 * Called when the progress of the SeekBar has changed.
	 * This implementation forwards the event to [onProgressChange].
	 *
	 * @param seekBar The SeekBar whose progress has changed.
	 * @param progress The current progress of the SeekBar.
	 * @param fromUser True if the progress change was initiated by the user, false if it was programmatically.
	 */
	override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
		onProgressChange(seekBar, progress, fromUser)
	}
	
	/**
	 * Called when the user starts touching the SeekBar.
	 * This implementation is empty, and can be overridden if needed.
	 *
	 * @param seekBar The SeekBar that is being tracked.
	 */
	override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
	
	/**
	 * Called when the user stops touching the SeekBar.
	 * This implementation is empty, and can be overridden if needed.
	 *
	 * @param seekBar The SeekBar that is being tracked.
	 */
	override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
}
