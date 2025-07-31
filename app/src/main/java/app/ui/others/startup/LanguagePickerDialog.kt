package app.ui.others.startup

import android.view.View
import android.view.View.inflate
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.RadioButton
import android.widget.RadioGroup
import app.core.AIOApp.Companion.aioLanguage
import app.core.AIOApp.Companion.aioSettings
import app.core.bases.BaseActivity
import com.aio.R
import lib.ui.builders.DialogBuilder
import java.lang.ref.WeakReference

/**
 * A custom dialog that allows users to select a language from a list of available options.
 * Upon selection, the language preference is stored in [aioSettings] and persisted.
 *
 * @param baseActivity the activity context used to build the dialog.
 */
class LanguagePickerDialog(private val baseActivity: BaseActivity) {
	
	// WeakReference to the base activity to prevent memory leaks
	private val safeBaseActivityRef by lazy { WeakReference(baseActivity).get() }
	
	// Lazy-initialized dialog builder to construct and show the language selection UI
	private val languageSelectionDialog by lazy {
		DialogBuilder(safeBaseActivityRef).apply {
			setView(R.layout.dialog_language_pick_1)
		}
	}
	
	/**
	 * Callback that is invoked when the user applies their selected language.
	 * Can be used by callers to perform additional actions, such as restarting the activity.
	 */
	var onApplyListener: () -> Unit? = {}
	
	init {
		// Make the dialog non-cancelable and set up the view and buttons
		languageSelectionDialog.setCancelable(false)
		languageSelectionDialog.view.apply {
			setAvailableLanguages(this)
			setButtonOnClickListeners(this)
		}
	}
	
	/** Returns the internal dialog builder instance. */
	fun getDialogBuilder(): DialogBuilder {
		return languageSelectionDialog
	}
	
	/** Closes the dialog if it is currently shown. */
	fun close() {
		if (languageSelectionDialog.isShowing) {
			languageSelectionDialog.close()
		}
	}
	
	/** Shows the dialog if it is not already visible. */
	fun show() {
		if (!languageSelectionDialog.isShowing) {
			languageSelectionDialog.show()
		}
	}
	
	/** Returns whether the dialog is currently being shown. */
	fun isShowing(): Boolean {
		return languageSelectionDialog.isShowing
	}
	
	/**
	 * Dynamically populates the dialog with available language options using radio buttons.
	 * Highlights the currently selected language based on user preferences.
	 */
	private fun setAvailableLanguages(dialogLayoutView: View) {
		safeBaseActivityRef?.let { safeActivityRef ->
			removeAllRadioSelectionViews(dialogLayoutView)
			
			aioLanguage.languagesList.forEachIndexed { index, (_, name) ->
				inflate(safeActivityRef, R.layout.dialog_language_pick_1_item_1, null).apply {
					(this as RadioButton).apply {
						id = index
						text = name
						
						// Set the height of the RadioButton
						val radioButtonHeight = resources.getDimensionPixelSize(R.dimen._40)
						layoutParams = LayoutParams(MATCH_PARENT, radioButtonHeight)
						
						// Set padding inside the RadioButton for visual spacing
						val horizontalPadding = resources.getDimensionPixelSize(R.dimen._5)
						val verticalPadding = resources.getDimensionPixelSize(R.dimen._5)
						setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
					}
					getLanguageRadioGroupView(dialogLayoutView).addView(this)
				}
			}
			
			// Highlight the currently selected language
			val currentLanguageCode = aioSettings.userSelectedUILanguage
			val selectedIndex = aioLanguage.languagesList.indexOfFirst { it.first == currentLanguageCode }
			if (selectedIndex >= 0) {
				getLanguageRadioGroupView(dialogLayoutView)
					.findViewById<RadioButton>(selectedIndex)?.isChecked = true
			}
		}
	}
	
	/**
	 * Clears all previously added language selection radio buttons.
	 */
	private fun removeAllRadioSelectionViews(dialogLayoutView: View) {
		getLanguageRadioGroupView(dialogLayoutView).removeAllViews()
	}
	
	/**
	 * Returns the [RadioGroup] that contains the language selection options.
	 */
	private fun getLanguageRadioGroupView(view: View): RadioGroup {
		return view.findViewById(R.id.language_options_container)
	}
	
	/**
	 * Sets up the "Apply" button click listener to save the selected language and close the dialog.
	 */
	private fun setButtonOnClickListeners(dialogLayoutView: View) {
		dialogLayoutView.findViewById<View>(R.id.btn_dialog_positive_container).apply {
			setOnClickListener { applySelectedApplicationLanguage(dialogLayoutView) }
		}
	}
	
	/**
	 * Applies the selected language from the radio group and updates the app settings.
	 */
	private fun applySelectedApplicationLanguage(dialogLayoutView: View) {
		val languageRadioGroup = getLanguageRadioGroupView(dialogLayoutView)
		val selectedLanguageId = languageRadioGroup.checkedRadioButtonId
		
		if (selectedLanguageId == -1) return // No selection
		
		val (selectedLanguageCode, _) = aioLanguage.languagesList[selectedLanguageId]
		
		// Save the new language preference and persist it
		aioSettings.userSelectedUILanguage = selectedLanguageCode
		aioSettings.updateInStorage()
		
		close()             // Close the dialog
		onApplyListener()   // Notify listener
	}
}
