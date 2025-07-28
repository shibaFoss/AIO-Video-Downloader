package app.ui.main.guides

import android.view.View
import app.core.bases.BaseActivity
import com.aio.R
import lib.ui.ViewUtility.setViewOnClickListener
import lib.ui.builders.DialogBuilder
import java.lang.ref.WeakReference

/**
 * A dialog guide that provides instructions for downloading from general websites.
 *
 * This class displays a tutorial dialog showing users how to download content
 * from various websites. It handles the dialog lifecycle and user interactions.
 *
 * @param baseActivity The parent activity that will host this dialog
 */
class WebDownloadGuide(private val baseActivity: BaseActivity?) {
	
	// Weak reference to parent activity to prevent memory leaks
	private val safeBaseActivityRef = WeakReference(baseActivity).get()
	
	// Dialog builder for creating and managing the tutorial dialog
	private val dialogBuilder: DialogBuilder = DialogBuilder(safeBaseActivityRef)
	
	init {
		safeBaseActivityRef?.let { _ ->
			// Set up the dialog layout using website tutorial template
			dialogBuilder.setView(R.layout.dialog_website_tutorial_1)
			
			// Allow dialog to be dismissed by tapping outside
			dialogBuilder.setCancelable(true)
			
			// Set up click listener for the primary action button
			setViewOnClickListener(
				{ button: View -> this.setupClickEvents(button) },
				dialogBuilder.view,
				R.id.button_dialog_positive_container
			)
		}
	}
	
	/**
	 * Handles click events for dialog buttons.
	 * Currently only handles the close/dismiss button.
	 *
	 * @param button The view that was clicked
	 */
	private fun setupClickEvents(button: View) {
		when (button.id) {
			R.id.button_dialog_positive_container -> close()
		}
	}
	
	/**
	 * Displays the website download guide dialog.
	 * Only shows the dialog if it's not already visible.
	 */
	fun show() {
		if (!dialogBuilder.isShowing) {
			dialogBuilder.show()
		}
	}
	
	/**
	 * Closes the website download guide dialog.
	 * Only attempts to close if the dialog is currently showing.
	 */
	fun close() {
		if (dialogBuilder.isShowing) {
			dialogBuilder.close()
		}
	}
}