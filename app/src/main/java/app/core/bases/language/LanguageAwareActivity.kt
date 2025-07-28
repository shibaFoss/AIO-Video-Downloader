package app.core.bases.language

import android.content.Context
import android.content.pm.PackageManager.GET_META_DATA
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import app.core.bases.language.LanguageAwareApplication.Companion.localeAwareManager

/**
 * An abstract base activity that supports dynamic language changes.
 * All activities that need to respond to runtime language changes should inherit from this class.
 */
abstract class LanguageAwareActivity : AppCompatActivity() {
	
	/**
	 * Attaches the base context with the appropriate locale as per user's language preference.
	 *
	 * @param context The original context.
	 */
	override fun attachBaseContext(context: Context) {
		super.attachBaseContext(localeAwareManager?.setLocale(context) ?: context)
	}
	
	/**
	 * Resets the activity's title to the one defined in AndroidManifest.xml
	 * after a language change. Useful when activity title is declared using a string resource.
	 */
	private fun resetTitle() {
		try {
			val labelRes = packageManager.getActivityInfo(componentName, GET_META_DATA).labelRes
			if (labelRes != 0) setTitle(labelRes)
		} catch (error: NameNotFoundException) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Ensures the configuration change maintains the current UI mode (e.g., night mode),
	 * while applying the new locale configuration.
	 *
	 * @param configuration The new configuration to apply.
	 */
	override fun applyOverrideConfiguration(configuration: Configuration?) {
		configuration?.let { safeConfig ->
			val uiMode = safeConfig.uiMode
			safeConfig.setTo(baseContext.resources.configuration)
			safeConfig.uiMode = uiMode
		}; super.applyOverrideConfiguration(configuration)
	}
	
	/**
	 * Sets a new locale for the activity and recreates it to apply the change.
	 *
	 * @param language The new language code (e.g., "en", "hi").
	 * @return `true` if the locale was set successfully, `false` otherwise.
	 */
	fun setNewLocale(language: String): Boolean {
		localeAwareManager?.setNewLocale(this, language) ?: return false
		recreate()
		return true
	}
}