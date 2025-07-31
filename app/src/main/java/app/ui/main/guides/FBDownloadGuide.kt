package app.ui.main.guides

import android.view.View
import app.core.bases.BaseActivity
import com.aio.R
import lib.ui.ViewUtility.setViewOnClickListener
import lib.ui.builders.DialogBuilder
import java.lang.ref.WeakReference

/**
 * A dialog guide that provides instructions for downloading from Facebook.
 *
 * This class displays a tutorial dialog showing users how to download content
 * from Facebook. It handles the dialog lifecycle and user interactions.
 *
 * @param baseActivity The parent activity that will host this dialog
 */
class FBDownloadGuide(private val baseActivity: BaseActivity?) {
	
	// Weak reference to parent activity to prevent memory leaks
	private val safeBaseActivityRef = WeakReference(baseActivity).get()
	
	// Dialog builder for creating and managing the tutorial dialog
	private val dialogBuilder: DialogBuilder = DialogBuilder(safeBaseActivityRef)
	
	init {
		safeBaseActivityRef?.let { _ ->
			// Set up the dialog layout and properties
			dialogBuilder.setView(R.layout.dialog_facebook_tutorial_1)
			dialogBuilder.setCancelable(true)
			
			// Set up click listeners for dialog buttons
			setViewOnClickListener(
				{ button: View -> this.setupClickEvents(button) },
				dialogBuilder.view,
				R.id.btn_dialog_positive_container
			)
		}
	}
	
	/**
	 * Handles click events for dialog buttons.
	 * @param button The view that was clicked
	 */
	private fun setupClickEvents(button: View) {
		when (button.id) {
			R.id.btn_dialog_positive_container -> close()
		}
	}
	
	/**
	 * Shows the Facebook download guide dialog if it's not already showing.
	 */
	fun show() {
		if (!dialogBuilder.isShowing) {
			dialogBuilder.show()
		}
	}
	
	/**
	 * Closes the Facebook download guide dialog if it's currently showing.
	 */
	fun close() {
		if (dialogBuilder.isShowing) {
			dialogBuilder.close()
		}
	}
}