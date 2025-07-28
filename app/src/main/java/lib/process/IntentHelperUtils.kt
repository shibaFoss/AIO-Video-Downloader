package lib.process

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.core.net.toUri
import java.lang.ref.WeakReference

/**
 * A utility object to simplify and safeguard common [Intent] handling operations.
 */
object IntentHelperUtils {
	
	/**
	 * Returns a list of activities that can handle the given [intent].
	 *
	 * @param activity The [Activity] context used to query the package manager.
	 * @param intent The [Intent] to be resolved.
	 * @return List of [ResolveInfo] for matching activities, or an empty list if none found.
	 */
	@JvmStatic
	fun getMatchingActivities(activity: Activity?, intent: Intent?): List<ResolveInfo> {
		if (intent == null || activity == null) return emptyList()
		WeakReference(activity).get()?.let { safeRef ->
			return safeRef.packageManager.queryIntentActivities(intent, 0)
		} ?: run { return emptyList() }
	}
	
	/**
	 * Retrieves the primary data shared via the [Activity]'s intent, supporting SEND and VIEW actions.
	 *
	 * @param activity The source [Activity].
	 * @return The shared URL or text if available, otherwise null.
	 */
	@JvmStatic
	fun getIntentData(activity: Activity?): String? {
		val intent = activity?.intent
		val action = intent?.action
		
		return when (action) {
			Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
			Intent.ACTION_VIEW -> intent.dataString
			else -> null
		}
	}
	
	/**
	 * Checks whether the provided [intent] can be handled by any installed app.
	 *
	 * @param activity The [Activity] context.
	 * @param intent The [Intent] to verify.
	 * @return `true` if there's at least one activity that can handle it, `false` otherwise.
	 */
	@JvmStatic
	fun canHandleIntent(activity: Activity?, intent: Intent?): Boolean {
		if (intent == null || activity == null) return false
		WeakReference(activity).get()?.let { safeRef ->
			val activities = safeRef.packageManager.queryIntentActivities(intent, 0)
			return activities.isNotEmpty()
		} ?: run { return false }
	}
	
	/**
	 * Attempts to start an [Intent] if possible.
	 *
	 * @param activity The [Activity] initiating the intent.
	 * @param intent The [Intent] to launch.
	 * @return `true` if successfully started, `false` if not.
	 */
	@JvmStatic
	fun startActivityIfPossible(activity: Activity?, intent: Intent?): Boolean {
		if (intent == null || activity == null) return false
		WeakReference(activity).get()?.let { safeRef ->
			return if (canHandleIntent(safeRef, intent)) {
				activity.startActivity(intent); true
			} else false
		} ?: run { return false }
	}
	
	/**
	 * Retrieves the package name of the first app that can handle the given [intent].
	 *
	 * @param activity The [Activity] context.
	 * @param intent The [Intent] to resolve.
	 * @return The package name, or an empty string if none is found.
	 */
	@JvmStatic
	fun getPackageNameForIntent(activity: Activity?, intent: Intent?): String {
		if (intent == null || activity == null) return ""
		WeakReference(activity).get()?.let { safeRef ->
			val activities = safeRef.packageManager.queryIntentActivities(intent, 0)
			return if (activities.isNotEmpty()) activities[0].activityInfo.packageName else ""
		} ?: run { return "" }
	}
	
	/**
	 * Opens the Facebook app via intent. Falls back to error callback if not available.
	 *
	 * @param context The context used to launch the intent.
	 * @param onError Optional callback triggered when the app is not available.
	 */
	@JvmStatic
	fun openFacebookApp(context: Context, onError: (() -> Unit)? = null) {
		try {
			val uri = "https://www.facebook.com".toUri()
			val intent = Intent(Intent.ACTION_VIEW, uri).apply {
				setPackage("com.facebook.katana")
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			}
			
			if (intent.resolveActivity(context.packageManager) != null) {
				context.startActivity(intent)
			} else onError?.invoke()
		} catch (e: Exception) {
			onError?.invoke()
		}
	}
	
	/**
	 * Opens the YouTube app via intent. Falls back to error callback if not available.
	 *
	 * @param context The context used to launch the intent.
	 * @param onError Optional callback triggered when the app is not available.
	 */
	@JvmStatic
	fun openYouTubeApp(context: Context, onError: (() -> Unit)? = null) {
		try {
			val uri = "https://www.youtube.com".toUri()
			val intent = Intent(Intent.ACTION_VIEW, uri).apply {
				setPackage("com.google.android.youtube")
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			}
			
			if (intent.resolveActivity(context.packageManager) != null) {
				context.startActivity(intent)
			} else onError?.invoke()
		} catch (e: Exception) {
			onError?.invoke()
		}
	}
	
	/**
	 * Opens the Instagram app via intent. Falls back to error callback if not available.
	 *
	 * @param context The context used to launch the intent.
	 * @param onError Optional callback triggered when the app is not available.
	 */
	@JvmStatic
	fun openInstagramApp(context: Context, onError: (() -> Unit)? = null) {
		try {
			val uri = "http://instagram.com".toUri()
			val intent = Intent(Intent.ACTION_VIEW, uri).apply {
				setPackage("com.instagram.android")
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			}
			
			if (intent.resolveActivity(context.packageManager) != null) {
				context.startActivity(intent)
			} else onError?.invoke()
		} catch (e: Exception) {
			onError?.invoke()
		}
	}
	
	/**
	 * Attempts to launch the WhatsApp application. Triggers [onError] if not installed.
	 *
	 * @param context The context used to launch WhatsApp.
	 * @param onError Optional callback triggered when WhatsApp is not found.
	 */
	@JvmStatic
	fun openWhatsappApp(context: Context, onError: (() -> Unit)? = null) {
		try {
			val intent = context.packageManager
				.getLaunchIntentForPackage("com.whatsapp")
			if (intent != null) {
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				context.startActivity(intent)
			} else {
				onError?.invoke()
			}
		} catch (error: Exception) {
			error.printStackTrace()
			onError?.invoke()
		}
	}
}
