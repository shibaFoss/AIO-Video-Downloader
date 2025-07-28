package lib.process

import android.os.CountDownTimer
import java.lang.ref.WeakReference

/**
 * Utility object providing common timing-related functionalities using [CountDownTimer].
 * Includes methods for delaying tasks, running countdowns, setting intervals, and stopwatch timers.
 */
object CommonTimeUtils {
	
	private val logger = LogHelperUtils.from(javaClass)
	
	/**
	 * Executes a delayed task once after a specified duration.
	 *
	 * @param timeInMile The delay duration in milliseconds.
	 * @param listener A callback invoked after the delay completes.
	 * @return The [CountDownTimer] instance managing the delay.
	 */
	@JvmStatic
	fun delay(timeInMile: Int, listener: OnTaskFinishListener): CountDownTimer {
		val safeTaskRef = WeakReference(listener)
		return object : CountDownTimer(timeInMile.toLong(), timeInMile.toLong()) {
			override fun onTick(millisUntilFinished: Long) = Unit
			
			override fun onFinish() {
				safeTaskRef.get()?.afterDelay()
			}
		}.start()
	}
	
	/**
	 * Starts a countdown timer with periodic tick updates and a finish callback.
	 *
	 * @param totalTime Total countdown time in milliseconds.
	 * @param interval Tick interval in milliseconds.
	 * @param listener Callback for tick updates and finish event.
	 * @return The [CountDownTimer] instance managing the countdown.
	 */
	@JvmStatic
	fun startCountDown(
		totalTime: Long,
		interval: Long,
		listener: OnCountDownListener
	): CountDownTimer {
		val safeTaskRef = WeakReference(listener)
		return object : CountDownTimer(totalTime, interval) {
			override fun onTick(millisUntilFinished: Long) {
				safeTaskRef.get()?.onTick(millisUntilFinished)
			}
			
			override fun onFinish() {
				safeTaskRef.get()?.onFinish()
			}
		}.start()
	}
	
	/**
	 * Sets up a repeated task executed at a fixed interval indefinitely.
	 *
	 * @param interval Interval duration in milliseconds.
	 * @param listener Callback to be executed on each interval.
	 * @return The [CountDownTimer] instance managing the repeated intervals.
	 */
	@JvmStatic
	fun setInterval(interval: Long, listener: OnIntervalListener): CountDownTimer {
		val safeTaskRef = WeakReference(listener)
		return object : CountDownTimer(Long.MAX_VALUE, interval) {
			override fun onTick(millisUntilFinished: Long) {
				safeTaskRef.get()?.onInterval()
			}
			
			override fun onFinish() = Unit
		}.start()
	}
	
	/**
	 * Starts a stopwatch-like timer that calls back with the elapsed time at each interval.
	 *
	 * @param interval Interval duration in milliseconds.
	 * @param listener Callback providing elapsed time since start.
	 * @return The [CountDownTimer] instance managing the stopwatch.
	 */
	@JvmStatic
	fun startStopWatch(interval: Long, listener: OnStopWatchListener): CountDownTimer {
		val safeTaskRef = WeakReference(listener)
		return object : CountDownTimer(Long.MAX_VALUE, interval) {
			private val startTime: Long = System.currentTimeMillis()
			
			override fun onTick(millisUntilFinished: Long) {
				val elapsedTime = System.currentTimeMillis() - startTime
				safeTaskRef.get()?.onTick(elapsedTime)
			}
			
			override fun onFinish() = Unit
		}.start()
	}
	
	/**
	 * Cancels a given [CountDownTimer] if it's running.
	 *
	 * @param timer The timer instance to cancel.
	 */
	@JvmStatic
	fun cancelTimer(timer: CountDownTimer?) {
		timer?.cancel()
	}
	
	/**
	 * Listener interface for delayed task completion.
	 */
	interface OnTaskFinishListener {
		fun afterDelay()
	}
	
	/**
	 * Listener interface for countdown timers.
	 */
	interface OnCountDownListener {
		fun onTick(millisUntilFinished: Long)
		fun onFinish()
	}
	
	/**
	 * Listener interface for interval-based callbacks.
	 */
	interface OnIntervalListener {
		fun onInterval()
	}
	
	/**
	 * Listener interface for stopwatch updates.
	 */
	interface OnStopWatchListener {
		fun onTick(elapsedTime: Long)
	}
}
