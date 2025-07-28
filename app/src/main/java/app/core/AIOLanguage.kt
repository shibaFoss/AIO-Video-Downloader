package app.core

import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioSettings
import app.core.bases.interfaces.BaseActivityInf
import app.core.bases.language.LocaleAwareManager
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.process.CommonTimeUtils.delay
import lib.process.LocalizationHelper
import java.util.Locale

/**
 * AIOLanguage manages application-wide language preferences.
 *
 * This class provides functionality to apply a user-selected UI language,
 * retrieve the list of supported languages, and gracefully restart the app
 * or finish activities when language changes require a UI refresh.
 */
open class AIOLanguage {
	
	companion object {
		const val ENGLISH = "en"
		const val BENGALI = "bn"
		const val HINDI = "hi"
		const val TELUGU = "te"
		const val JAPANESE = "ja"
		const val DANISH = "da"
		const val GERMAN = "de"
	}
	
	/**
	 * A list of supported languages represented as (language code, display name).
	 */
	val languagesList: List<Pair<String, String>> = listOf(
		ENGLISH to "English (Default)",
		HINDI to "Hindi (हिंदी)",
		TELUGU to "Telugu (తెలుగు)",
		BENGALI to "Bengali (বাংলা)",
		JAPANESE to "Japanese (日本語)",
		DANISH to "Danish (Dansk)",
		GERMAN to "German (Deutsch)"
	)
	
	/** Flag to indicate whether the activity should be finished upon resume. */
	open var finishActivityOnResume = false
	
	/** Flag to trigger a complete application quit. */
	private var quitApplicationCommand = false
	
	/**
	 * Applies the language selected by the user in settings to the current activity and application context.
	 *
	 * @param baseActivityInf A reference to the activity implementing BaseActivityInf interface.
	 * @param onComplete A callback to be invoked after the language is applied.
	 */
	fun applyUserSelectedLanguage(baseActivityInf: BaseActivityInf?, onComplete: () -> Unit = {}) {
		baseActivityInf?.getActivity()?.let { safeActivityRef ->
			this.finishActivityOnResume = false
			val languageCode = aioSettings.userSelectedUILanguage
			LocaleAwareManager(safeActivityRef).setNewLocale(safeActivityRef, languageCode)
			LocaleAwareManager(INSTANCE).setNewLocale(INSTANCE, languageCode)
			LocalizationHelper.setAppLocale(INSTANCE, Locale(languageCode))
			onComplete()
		}
	}
	
	/**
	 * Closes the current activity if language change was applied and a restart is needed.
	 *
	 * @param baseActivityInf A reference to the activity implementing BaseActivityInf interface.
	 */
	fun closeActivityIfLanguageChanged(baseActivityInf: BaseActivityInf?) {
		baseActivityInf?.getActivity()?.let { safeActivityRef ->
			if (finishActivityOnResume) {
				safeActivityRef.finishAffinity()
				quitApplicationCommand = true
				delay(300, object : OnTaskFinishListener {
					override fun afterDelay() = quitApplication(safeActivityRef)
				})
			}
		}
	}
	
	/**
	 * Quits the application by finishing all activities if flagged by quitApplicationCommand.
	 *
	 * @param baseActivityInf A reference to the activity implementing BaseActivityInf interface.
	 */
	private fun quitApplication(baseActivityInf: BaseActivityInf?) {
		baseActivityInf?.getActivity()?.let { safeActivityRef ->
			if (quitApplicationCommand) {
				quitApplicationCommand = false
				safeActivityRef.finishAffinity()
			}
		}
	}
}