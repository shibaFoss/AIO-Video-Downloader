@file:Suppress("UNUSED_PARAMETER")

package lib.process

import android.os.CountDownTimer

/**
 * A utility class for creating and managing a simple countdown timer.
 *
 * This class provides basic timer operations such as start, pause, resume, cancel, and time update.
 * It also supports a listener interface for timer tick and finish events.
 *
 * Example usage:
 * ```
 * val timer = SimpleTimerUtils(60000, 1000)
 * timer.setTimerListener(object : SimpleTimerUtils.TimerListener {
 *     override fun onTick(millisUntilFinished: Long) { ... }
 *     override fun onFinish() { ... }
 * })
 * timer.start()
 * ```
 *
 * @param millisInFuture Total duration of the countdown in milliseconds.
 * @param countDownInterval Interval in milliseconds between `onTick` callbacks.
 */
class SimpleTimerUtils(private var millisInFuture: Long, private val countDownInterval: Long) {
	
	/** Remaining time in milliseconds. */
	var timeRemaining: Long = millisInFuture
	
	/** Indicates whether the timer is currently running. */
	var isRunning: Boolean = false
	
	/** Indicates whether the timer is currently paused. */
	var isPaused: Boolean = false
	
	/** Optional listener for timer events. */
	private var timerListener: TimerListener? = null
	
	/** Internal CountDownTimer instance managed by this utility. */
	private val countDownTimer: CountDownTimer by lazy {
		object : CountDownTimer(timeRemaining, countDownInterval) {
			override fun onTick(millisUntilFinished: Long) {
				timeRemaining = millisUntilFinished
				this@SimpleTimerUtils.onTick(millisUntilFinished)
				timerListener?.onTick(millisUntilFinished)
			}
			
			override fun onFinish() {
				isRunning = false
				isPaused = false
				this@SimpleTimerUtils.onFinish()
				timerListener?.onFinish()
			}
		}
	}
	
	/**
	 * Starts the timer. If the timer was paused, it will resume instead.
	 */
	fun start() {
		if (isPaused) {
			resume()
			return
		}
		countDownTimer.cancel()
		isRunning = true
		isPaused = false
		countDownTimer.start()
	}
	
	/**
	 * Pauses the currently running timer.
	 * Timer can be resumed later with [resume].
	 */
	fun pause() {
		if (isRunning && !isPaused) {
			countDownTimer.cancel()
			isPaused = true
			isRunning = false
		}
	}
	
	/**
	 * Resumes the timer if it was previously paused.
	 */
	fun resume() {
		if (isPaused) start()
	}
	
	/**
	 * Cancels the timer and resets its state.
	 */
	fun cancel() {
		countDownTimer.cancel()
		isRunning = false
		isPaused = false
	}
	
	/**
	 * Updates the total time for the countdown.
	 *
	 * @param millisInFuture New total countdown time in milliseconds.
	 * If the timer is active or paused, it will automatically restart with the new time.
	 */
	fun updateTime(millisInFuture: Long) {
		this.millisInFuture = millisInFuture
		this.timeRemaining = millisInFuture
		if (isRunning || isPaused) {
			start()
		}
	}
	
	/**
	 * Called on every tick of the countdown. Can be overridden by subclasses.
	 *
	 * @param millisUntilFinished Time left until the countdown finishes.
	 */
	fun onTick(millisUntilFinished: Long) = Unit
	
	/**
	 * Called when the countdown finishes. Can be overridden by subclasses.
	 */
	fun onFinish() = Unit
	
	/**
	 * Sets the listener to receive timer updates.
	 *
	 * @param timerListener Listener implementing [TimerListener] interface.
	 */
	fun setTimerListener(timerListener: TimerListener?) {
		this.timerListener = timerListener
	}
	
	/**
	 * Listener interface to receive callbacks on each tick and when the timer finishes.
	 */
	interface TimerListener {
		/**
		 * Called on each tick with the remaining time.
		 *
		 * @param millisUntilFinished Remaining time in milliseconds.
		 */
		fun onTick(millisUntilFinished: Long)
		
		/**
		 * Called when the countdown timer finishes.
		 */
		fun onFinish()
	}
}
