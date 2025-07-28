package app.core.bases.dialogs

import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import app.core.AIOApp.Companion.aioBackend
import app.core.AIOApp.Companion.aioSettings
import app.core.bases.BaseActivity
import app.ui.main.MotherActivity
import com.aio.R
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.process.CommonTimeUtils.delay
import lib.ui.ViewUtility.setViewOnClickListener
import lib.ui.builders.DialogBuilder
import java.lang.ref.WeakReference

/**
 * Displays a rating prompt dialog to encourage users to rate the app.
 *
 * This class uses a custom dialog layout and provides buttons for users to rate the app
 * or dismiss the dialog. It also records user interaction for analytics and updates settings
 * based on user action.
 *
 * @param baseActivityInf A reference to the base activity from which the dialog will be shown.
 */
open class RatingPrompter(baseActivityInf: BaseActivity?) {
	
	companion object {
		var IS_DIALOG_SHOWING = false
	}
	
	/** Weak reference to the base activity to avoid memory leaks. */
	private val safeBaseActivityRef = WeakReference(baseActivityInf).get()
	
	/** DialogBuilder used to construct and display the rating prompt dialog. */
	private val dialogBuilder: DialogBuilder = DialogBuilder(safeBaseActivityRef)
	
	init {
		safeBaseActivityRef?.let {
			dialogBuilder.setView(R.layout.dialog_rating_prompt_1)
			dialogBuilder.setCancelable(true)
			dialogBuilder.dialog.setOnCancelListener { IS_DIALOG_SHOWING = false }
			dialogBuilder.dialog.setOnDismissListener { IS_DIALOG_SHOWING = false }
			
			// Attach click listeners to positive and negative buttons
			setViewOnClickListener(
				{ button: View -> this.setupClickEvents(button) },
				dialogBuilder.view,
				R.id.button_dialog_negative_container,
				R.id.button_dialog_positive_container
			)
		}
	}
	
	/**
	 * Handles click events for the dialog buttons.
	 *
	 * @param button The clicked view.
	 */
	private fun setupClickEvents(button: View) {
		when (button.id) {
			R.id.button_dialog_positive_container -> {
				close()
				openGooglePlayStoreForRating()
				aioBackend.updateClickCountOnRating()
			}
			
			R.id.button_dialog_negative_container -> close()
		}
	}
	
	/**
	 * Closes the dialog if it is currently shown.
	 */
	fun close() {
		if (dialogBuilder.isShowing) {
			dialogBuilder.close()
		}
	}
	
	/**
	 * Shows the dialog with an optional filter to exclude [MotherActivity] if needed.
	 *
	 * @param shouldIgnoreMotherActivity If true, dialog will show regardless of activity type.
	 */
	@OptIn(UnstableApi::class)
	fun show(shouldIgnoreMotherActivity: Boolean = false) {
		safeBaseActivityRef?.let { safeActivityRef ->
			if (shouldIgnoreMotherActivity) {
				showActualDialog()
			} else {
				if (safeActivityRef is MotherActivity) {
					showActualDialog()
				}
			}
		}
	}
	
	/**
	 * Actually displays the dialog after a short delay.
	 * Ensures the dialog is not already showing before displaying.
	 */
	private fun showActualDialog() {
		if (!dialogBuilder.isShowing) {
			delay(200, object : OnTaskFinishListener {
				override fun afterDelay() {
					dialogBuilder.show()
					IS_DIALOG_SHOWING = true
				}
			})
		}
	}
	
	/**
	 * @return True if the rating dialog is currently being shown, false otherwise.
	 */
	fun isShowing(): Boolean {
		val showing = dialogBuilder.isShowing
		return showing
	}
	
	/**
	 * Launches the Google Play Store for users to rate the app and updates user preferences.
	 */
	private fun openGooglePlayStoreForRating() {
		aioSettings.hasUserRatedTheApplication = true
		aioSettings.updateInStorage()
		safeBaseActivityRef?.openApplicationInPlayStore()
	}
}