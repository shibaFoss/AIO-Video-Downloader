@file:Suppress("DEPRECATION")

package lib.process

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * A utility object for handling runtime localization in the application.
 * This class allows changing the app's language dynamically and retrieving
 * localized strings based on the currently set locale.
 */
object LocalizationHelper {
    
    /**
     * Holds the currently set locale for the application.
     * If null, the system default locale will be used.
     */
    private var currentLocale: Locale? = null
    
    /**
     * Sets the application's locale to the specified [locale].
     *
     * This method updates the internal [currentLocale] reference and modifies
     * the configuration of the provided [context] to reflect the new locale.
     *
     * @param context The context whose resources will be updated with the new locale.
     * @param locale The [Locale] to set for the application.
     */
    @JvmStatic
    fun setAppLocale(context: Context, locale: Locale) {
        currentLocale = locale
        updateResources(context, locale)
    }
    
    /**
     * Returns a localized string based on the currently set [Locale].
     *
     * If a locale has been set using [setAppLocale], it returns the string from that locale.
     * Otherwise, it falls back to the system default locale.
     *
     * @param context The context used to access resources.
     * @param resId The resource ID of the string to retrieve.
     * @return The localized string.
     */
    @JvmStatic
    fun getLocalizedString(context: Context, resId: Int): String {
        return if (currentLocale != null) {
            val config = Configuration(context.resources.configuration)
            config.setLocale(currentLocale)
            context.createConfigurationContext(config).resources.getString(resId)
        } else {
            context.resources.getString(resId)
        }
    }
    
    /**
     * Updates the application's [Configuration] and resources to use the specified [locale].
     *
     * This method modifies the configuration of the [context] to use the new [locale],
     * and applies the change to the current resources.
     *
     * @param context The context whose resources will be updated.
     * @param locale The new [Locale] to be applied.
     */
    @JvmStatic
    private fun updateResources(context: Context, locale: Locale) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
