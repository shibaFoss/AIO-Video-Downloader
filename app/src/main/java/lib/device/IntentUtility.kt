package lib.device

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.core.net.toUri
import lib.networks.URLUtility
import java.lang.ref.WeakReference

/**
 * Utility class for handling various Intent-related operations like opening URLs
 * in browsers or extracting intent data from activities.
 */
object IntentUtility {
	
	/**
	 * Opens the specified URL in the default browser if any browser app is available.
	 *
	 * @param context The context used to start the browser activity.
	 * @param url The URL string to open.
	 *
	 * @throws IllegalStateException If no application is available to handle the intent.
	 */
	@JvmStatic
	fun openUrlInBrowser(context: Context?, url: String) {
		if (url.isEmpty()) return
		
		WeakReference(context).get()?.let { safeContextRef ->
			val webpage = url.toUri()
			val intent = Intent(ACTION_VIEW, webpage)
			if (intent.resolveActivity(safeContextRef.packageManager) != null) {
				safeContextRef.startActivity(intent)
			} else {
				throw IllegalStateException(
					"No application can handle this request. Please install a web browser."
				)
			}
		}
	}
	
	/**
	 * Retrieves the data URI from the given [Activity]'s intent, if available.
	 * Supports both `ACTION_SEND` and `ACTION_VIEW`.
	 *
	 * @param activity The activity whose intent data is to be extracted.
	 * @return A string representing the data URI or `null` if not available.
	 */
	@JvmStatic
	fun getIntentDataURI(activity: Activity?): String? {
		WeakReference(activity).get()?.let { safeContextRef ->
			val intent = safeContextRef.intent
			val action = intent.action
			
			val dataURI = when (action) {
				ACTION_SEND -> intent.getStringExtra(EXTRA_TEXT)
				ACTION_VIEW -> intent.dataString
				else -> null
			}
			return dataURI
		} ?: run { return null }
	}
	
	/**
	 * Opens the specified URL in the system's default browser. If the URL is invalid or fails to open,
	 * the provided `onFailed` callback is invoked.
	 *
	 * @param fileUrl The URL string to open.
	 * @param activity The current activity context.
	 * @param onFailed Callback invoked when the URL fails to open.
	 */
	@JvmStatic
	fun openLinkInSystemBrowser(fileUrl: String, activity: Activity?, onFailed: () -> Unit) {
		WeakReference(activity).get()?.let { safeContextRef ->
			try {
				if (fileUrl.isNotEmpty() && URLUtility.isValidURL(fileUrl)) {
					val intent = Intent(ACTION_VIEW, fileUrl.toUri()).apply {
						addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
					}
					safeContextRef.startActivity(intent)
				} else onFailed()
			} catch (error: Exception) {
				error.printStackTrace()
				onFailed()
			}
		}
	}
}