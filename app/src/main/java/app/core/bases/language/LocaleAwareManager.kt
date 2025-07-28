@file:Suppress("DEPRECATION")

package app.core.bases.language

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.preference.PreferenceManager.getDefaultSharedPreferences
import androidx.core.content.edit
import java.util.Locale

/**
 * Utility class responsible for handling locale changes within the app.
 * It persists the selected language and applies it to the application context.
 *
 * @param context The context used to access shared preferences.
 */
class LocaleAwareManager(context: Context?) {
	
	private val preferences: SharedPreferences = getDefaultSharedPreferences(context)
	
	/**
	 * Applies the currently persisted locale setting to the provided context.
	 *
	 * @param context The original context.
	 * @return The updated context with the applied locale.
	 */
	fun setLocale(context: Context): Context {
		return updateResources(context, language)
	}
	
	/**
	 * Sets a new locale and persists it in shared preferences.
	 * Recreates the context with the new locale applied.
	 *
	 * @param context The original context.
	 * @param language The language code to set (e.g., "en", "fr").
	 * @return The updated context with the new locale.
	 */
	fun setNewLocale(context: Context, language: String): Context {
		persistLanguage(language)
		return updateResources(context, language)
	}
	
	/**
	 * Gets the currently persisted language code.
	 */
	val language: String?
		get() = preferences.getString(LANGUAGE_KEY, LANGUAGE_ENGLISH)
	
	/**
	 * Persists the selected language into shared preferences.
	 *
	 * @param language The language code to persist.
	 */
	@SuppressLint("ApplySharedPref")
	private fun persistLanguage(language: String) {
		preferences.edit(commit = true) { putString(LANGUAGE_KEY, language) }
	}
	
	/**
	 * Updates the resources and configuration of the context with the specified language.
	 *
	 * @param context The original context.
	 * @param language The language code to apply.
	 * @return The updated context with locale changes.
	 */
	private fun updateResources(context: Context, language: String?): Context {
		if (language == null) return context
		
		val locale = Locale(language)
		Locale.setDefault(locale)
		
		val config = Configuration(context.resources.configuration)
		config.setLocale(locale)
		
		return context.createConfigurationContext(config)
	}
	
	companion object {
		/** Default language code (English). */
		const val LANGUAGE_ENGLISH = "en"
		
		/** SharedPreferences key for storing the language setting. */
		private const val LANGUAGE_KEY = "language_key"
		
		/**
		 * Retrieves the current [Locale] from the provided [Resources].
		 *
		 * @param res The [Resources] object to extract locale from.
		 * @return The [Locale] currently used in the configuration.
		 */
		fun getLocale(res: Resources): Locale {
			return res.configuration.locales[0]
		}
	}
}