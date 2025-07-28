package app.core

import app.core.AIOApp.Companion.aioBackend
import app.core.AIOApp.Companion.aioSettings
import java.io.PrintWriter
import java.io.StringWriter

/**
 * CrashHandler is a custom [Thread.UncaughtExceptionHandler] that handles uncaught exceptions
 * across the application and performs necessary logging and state updates.
 *
 * When an uncaught exception occurs, this handler:
 * - Extracts the full stack trace.
 * - Saves the crash information using [aioBackend].
 * - Marks that the app has recently crashed in [aioSettings].
 * - Updates the persisted settings.
 */
class CrashHandler : Thread.UncaughtExceptionHandler {
	
	/** Holds the application context instance from [AIOApp]. */
	private val appInstance = AIOApp.INSTANCE.applicationContext
	
	/**
	 * Called when an uncaught exception occurs in any thread.
	 *
	 * @param thread The thread that has encountered the exception.
	 * @param exception The uncaught exception.
	 */
	override fun uncaughtException(thread: Thread, exception: Throwable) {
		try {
			// Extract full stack trace as a string
			val stackTrace = StringWriter().use { sw ->
				PrintWriter(sw).use { pw ->
					exception.printStackTrace(pw)
					sw.toString()
				}
			}
			
			// Save crash information for later inspection
			aioBackend.saveAppCrashedInfo(stackTrace)
			
			// Update crash flag in settings
			aioSettings.hasAppCrashedRecently = true
			aioSettings.updateInStorage()
		} catch (error: Exception) {
			// Log any errors that occur while handling the original crash
			error.printStackTrace()
		}
	}
}