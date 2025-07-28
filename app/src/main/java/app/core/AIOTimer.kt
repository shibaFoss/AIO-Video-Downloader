package app.core

import android.os.CountDownTimer
import java.lang.ref.WeakReference

/**
 * AIOTimer is a recurring countdown timer that notifies registered listeners on each tick.
 *
 * This timer restarts itself automatically upon finishing, effectively making it infinite
 * until stopped manually via [stop]. Listeners can register to receive tick callbacks and
 * will be weakly referenced to prevent memory leaks.
 *
 * @param millisInFuture The number of milliseconds in the future until the timer is done.
 * @param countDownInterval The interval in milliseconds between callbacks to [onTick].
 */
open class AIOTimer(millisInFuture: Long, countDownInterval: Long) :
	CountDownTimer(millisInFuture, countDownInterval) {
	
	/** Holds weak references to registered [AIOTimerListener]s. */
	private val timerListeners = ArrayList<WeakReference<AIOTimerListener>>()
	
	/** Tracks the number of times the timer has ticked. */
	private var loopCount = 0.0
	
	/**
	 * Called at every countDownInterval. Notifies all valid registered listeners.
	 *
	 * @param millisUntilFinished The amount of time until finished. Ignored since this timer restarts itself.
	 */
	override fun onTick(millisUntilFinished: Long) {
		loopCount++
		// Remove any listeners that have been garbage collected
		timerListeners.removeAll { it.get() == null }
		// Notify all active listeners
		timerListeners.forEach { listenerRef ->
			listenerRef.get()?.onAIOTimerTick(loopCount)
		}
	}
	
	/**
	 * Called when the countdown finishes. Restarts the timer to simulate infinite behavior.
	 */
	override fun onFinish() {
		this.start()
	}
	
	/**
	 * Registers a listener to receive tick events.
	 *
	 * @param listener The [AIOTimerListener] to register.
	 */
	fun register(listener: AIOTimerListener) {
		if (timerListeners.none { it.get() == listener }) {
			timerListeners.add(WeakReference(listener))
		}
	}
	
	/**
	 * Unregisters a previously registered listener.
	 *
	 * @param listener The [AIOTimerListener] to unregister.
	 */
	fun unregister(listener: AIOTimerListener) {
		timerListeners.removeAll { it.get() == listener }
	}
	
	/**
	 * Stops the timer and clears all registered listeners.
	 */
	fun stop() {
		this.cancel()
		timerListeners.clear()
	}
	
	/**
	 * Interface for listeners interested in timer ticks.
	 */
	interface AIOTimerListener {
		/**
		 * Called every time the timer ticks.
		 *
		 * @param loopCount The number of times the timer has ticked since start.
		 */
		fun onAIOTimerTick(loopCount: Double)
	}
}