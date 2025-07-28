package app.core.engines.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.core.AIOApp

/**
 * RestartIdleServiceReceiver is a BroadcastReceiver that listens for system or app-specific
 * broadcast events and triggers the restart or update of the idle foreground service.
 *
 * Typically used when the system wants to re-initialize services after events like reboot,
 * alarm, or connectivity changes.
 */
class RestartIdleServiceReceiver : BroadcastReceiver() {
	
	/**
	 * Called when the BroadcastReceiver receives a broadcast Intent.
	 *
	 * This implementation calls `updateService()` on the singleton instance of the idle
	 * foreground service managed by `AIOApp`.
	 *
	 * @param context The Context in which the receiver is running.
	 * @param intent The Intent being received.
	 */
	override fun onReceive(context: Context, intent: Intent) {
		// Trigger an update or restart of the idle foreground service
		AIOApp.idleForegroundService.updateService()
	}
}
