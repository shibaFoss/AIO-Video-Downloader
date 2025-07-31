@file:Suppress("DEPRECATION")

package lib.ui.builders

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import app.core.AIOApp.Companion.INSTANCE
import com.aio.R.id
import com.aio.R.layout
import lib.networks.URLUtility.isValidURL
import java.lang.ref.WeakReference

/**
 * A custom Toast class that allows setting a custom layout and icon.
 *
 * This class provides a reusable and centralized way to show toasts with
 * enhanced customization in your application. It prevents showing toasts for URLs
 * and uses a consistent layout across the app.
 *
 * @constructor Creates a ToastView instance with the given context.
 * @param context The context to use. Usually your Application or Activity context.
 */
class ToastView(context: Context) : Toast(context) {
	
	/**
	 * Sets the icon for the toast if a valid view is available.
	 *
	 * @param iconResId The resource ID of the drawable to use as the icon.
	 */
	fun setIcon(iconResId: Int) {
		view?.findViewById<ImageView>(id.img_toast_app_icon)
			?.apply { setImageResource(iconResId) }
	}
	
	companion object {
		
		/**
		 * Shows a toast message. This method accepts either a string message or a resource ID.
		 * It automatically avoids displaying if the message is a valid URL.
		 *
		 * @param msg The string message to display. Optional.
		 * @param msgId The resource ID for the message string. Optional.
		 */
		@JvmStatic
		fun showToast(msg: String? = null, msgId: Int = -1) {
			when {
				msgId != -1 -> showResourceToast(msgId)
				msg != null -> showTextToast(msg)
			}
		}
		
		/**
		 * Displays a toast using a string resource ID.
		 * Skips displaying if the resolved message is a URL.
		 *
		 * @param msgId The resource ID of the message string.
		 */
		private fun showResourceToast(msgId: Int) {
			val message = INSTANCE.getText(msgId)
			if (isValidURL(message.toString())) return
			makeText(INSTANCE, message).show()
		}
		
		/**
		 * Displays a toast using a string message.
		 * Skips displaying if the message is a URL.
		 *
		 * @param msg The message string to show.
		 */
		private fun showTextToast(msg: String) {
			if (isValidURL(msg)) return
			makeText(INSTANCE, msg).show()
		}
		
		/**
		 * Creates and configures a [ToastView] instance using a custom layout and duration.
		 *
		 * @param context The context used to create the toast.
		 * @param message The message to display in the toast.
		 * @param duration The duration of the toast. Defaults to [Toast.LENGTH_LONG].
		 * @return A configured [ToastView] instance.
		 */
		private fun makeText(
			context: Context, message: CharSequence?, duration: Int = LENGTH_LONG
		): ToastView {
			return WeakReference(context).get()?.let { safeContext ->
				configureToastView(safeContext, message, duration)
			} ?: run { ToastView(context) }
		}
		
		/**
		 * Inflates the custom toast layout and sets its properties.
		 *
		 * @param context The context used to inflate the view.
		 * @param message The text to display.
		 * @param duration How long to display the toast.
		 * @return A [ToastView] instance with the custom layout and message.
		 */
		@SuppressLint("InflateParams") private fun configureToastView(
			context: Context, message: CharSequence?, duration: Int
		): ToastView {
			return ToastView(context).apply {
				val inflater = context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
				val toastView = inflater.inflate(layout.lay_custom_toast_view_1, null)
				toastView.findViewById<TextView>(id.txt_toast_message).text = message
				view = toastView
				setDuration(duration)
			}
		}
	}
}