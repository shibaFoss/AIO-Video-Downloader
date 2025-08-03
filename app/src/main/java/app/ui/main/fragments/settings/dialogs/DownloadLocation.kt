package app.ui.main.fragments.settings.dialogs

import android.view.View
import android.widget.ImageView
import app.core.AIOApp.Companion.aioSettings
import app.core.bases.BaseActivity
import app.core.engines.settings.AIOSettings.Companion.PRIVATE_FOLDER
import app.core.engines.settings.AIOSettings.Companion.SYSTEM_GALLERY
import com.aio.R
import lib.ui.builders.DialogBuilder
import java.lang.ref.WeakReference

/**
 * Dialog class for selecting the default download location.
 *
 * This class provides a user interface to choose between two download locations:
 * - App's private folder
 * - System gallery
 *
 * The selected option is stored in aioSettings.defaultDownloadLocation and persisted in storage.
 * If the dialog is canceled or dismissed without applying changes, the original setting is restored.
 *
 * @property baseActivity The activity context required for building the dialog.
 */
class DownloadLocation(private val baseActivity: BaseActivity) {
	
	/** Weak reference to avoid memory leaks with the base activity */
	private val safeBaseActivity = WeakReference(baseActivity).get()
	
	/** Flag to track whether user has applied a new setting */
	private var hasSettingApplied = false
	
	/** Store the original location to restore in case changes are not applied */
	private val originalLocation = aioSettings.defaultDownloadLocation
	
	/**
	 * Lazily-initialized dialog for selecting download location.
	 *
	 * It provides options to choose the app's private folder or the system gallery,
	 * and applies the selected location only if the "Apply" button is pressed.
	 */
	private val dialog by lazy {
		DialogBuilder(safeBaseActivity).apply {
			setView(R.layout.dialog_default_location_1)
			setCancelable(true)
			
			view.apply {
				val privateBtn = findViewById<View>(R.id.button_app_private_folder)
				val galleryBtn = findViewById<View>(R.id.btn_system_gallery)
				val applyBtn = findViewById<View>(R.id.btn_dialog_positive_container)
				val privateRadio = findViewById<ImageView>(R.id.btn_app_private_folder_radio)
				val galleryRadio = findViewById<ImageView>(R.id.button_system_gallery_radio)
				
				updateRadioButtons(privateRadio, galleryRadio)
				
				privateBtn.setOnClickListener {
					aioSettings.defaultDownloadLocation = PRIVATE_FOLDER
					updateRadioButtons(privateRadio, galleryRadio)
				}
				
				galleryBtn.setOnClickListener {
					aioSettings.defaultDownloadLocation = SYSTEM_GALLERY
					updateRadioButtons(privateRadio, galleryRadio)
				}
				
				applyBtn.setOnClickListener {
					hasSettingApplied = true
					aioSettings.updateInStorage()
					close()
				}
			}
			
			dialog.setOnCancelListener { restoreIfNotApplied() }
			dialog.setOnDismissListener { restoreIfNotApplied() }
		}
	}
	
	/**
	 * Updates the radio button icons to reflect the current selection.
	 *
	 * @param privateRadio ImageView for the private folder radio icon.
	 * @param galleryRadio ImageView for the system gallery radio icon.
	 */
	private fun updateRadioButtons(privateRadio: ImageView, galleryRadio: ImageView) {
		val isPrivate = aioSettings.defaultDownloadLocation == PRIVATE_FOLDER
		privateRadio.setImageResource(
			if (isPrivate) R.drawable.ic_button_checked_circle
			else R.drawable.ic_button_unchecked_circle
		)
		galleryRadio.setImageResource(
			if (isPrivate) R.drawable.ic_button_unchecked_circle
			else R.drawable.ic_button_checked_circle
		)
	}
	
	/**
	 * Restores the original download location if no setting was applied.
	 */
	private fun restoreIfNotApplied() {
		if (!hasSettingApplied) {
			aioSettings.defaultDownloadLocation = originalLocation
			aioSettings.updateInStorage()
		}
	}
	
	/**
	 * Shows the dialog if it is not already showing.
	 */
	fun show() = takeIf { !dialog.isShowing }?.run { dialog.show() }
	
	/**
	 * Closes the dialog if it is currently showing.
	 */
	fun close() = takeIf { dialog.isShowing }?.run { dialog.close() }
}
