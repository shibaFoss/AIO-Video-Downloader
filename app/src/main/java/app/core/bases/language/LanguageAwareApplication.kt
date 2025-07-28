package app.core.bases.language

import android.app.Application
import android.content.Context
import android.content.res.Configuration

/**
 * A base [Application] class that supports runtime locale changes.
 * This class initializes and manages the [LocaleAwareManager] for applying locale settings
 * globally throughout the app.
 */
open class LanguageAwareApplication : Application() {
	
	/**
	 * Called before [onCreate]. Initializes the [LocaleAwareManager] and wraps
	 * the base context with the user's preferred locale settings.
	 *
	 * @param base The original application context.
	 */
	override fun attachBaseContext(base: Context) {
		localeAwareManager = LocaleAwareManager(base)
		super.attachBaseContext(localeAwareManager?.setLocale(base) ?: base)
	}
	
	/**
	 * Called when the system configuration changes while the app is running.
	 * Ensures that the locale is reapplied when configuration (e.g., orientation, UI mode) changes.
	 *
	 * @param newConfig The new device configuration.
	 */
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		localeAwareManager?.setLocale(this)
	}
	
	companion object {
		/**
		 * Singleton instance of [LocaleAwareManager] used to manage locale settings.
		 */
		var localeAwareManager: LocaleAwareManager? = null
			private set
	}
}