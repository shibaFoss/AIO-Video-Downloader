package app.core.engines.backend

import app.core.AIOApp
import app.core.AIOApp.Companion.aioBackend
import app.core.AIOApp.Companion.aioSettings
import app.core.AIOTimer.AIOTimerListener
import lib.device.DateTimeUtils.calculateTime

/**
 * Tracks and records application usage time.
 *
 * This class implements a timer that periodically updates the total application
 * usage time and syncs it with both local storage and backend analytics.
 *
 * Usage tracking works by counting timer ticks and accumulating them into
 * meaningful time intervals before saving to prevent excessive I/O operations.
 */
class AppUsageTimer : AIOTimerListener {
	
	// Counter for tracking timer ticks between storage updates
	private var localLoopCounter = 0
	
	/**
	 * Starts the usage tracking by registering this listener with the AIOTimer.
	 *
	 * Call this method to begin tracking application usage time.
	 * The timer will automatically handle periodic updates.
	 */
	fun startTracking() {
		AIOApp.aioTimer.register(this)
	}
	
	/**
	 * Callback method invoked on each timer tick.
	 *
	 * @param loopCount The current loop count from the timer (unused in this implementation)
	 *
	 * This method:
	 * 1. Increments the local loop counter
	 * 2. Every 9000 ticks (approximately every X minutes):
	 *    - Updates the total usage time in milliseconds
	 *    - Converts to human-readable format
	 *    - Persists to local storage
	 *    - Syncs with backend analytics
	 *    - Resets the local counter
	 */
	override fun onAIOTimerTick(loopCount: Double) {
		localLoopCounter++
		
		// Update storage and backend every 9000 ticks to balance accuracy and performance
		if (localLoopCounter >= 9000) {
			// Accumulate time (200ms per tick)
			aioSettings.totalUsageTimeInMs += ((localLoopCounter * 200)).toFloat()
			
			// Convert to readable format
			aioSettings.totalUsageTimeInFormat = calculateTime(aioSettings.totalUsageTimeInMs)
			
			// Persist changes
			aioSettings.updateInStorage()
			
			// Sync with backend analytics
			aioBackend.trackApplicationInfo()
			
			// Reset counter
			localLoopCounter = 0
		}
	}
}