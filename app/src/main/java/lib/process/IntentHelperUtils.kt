package lib.process

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.core.net.toUri
import java.lang.ref.WeakReference

/**
 * Utility class for common Intent operations with safety checks and fallback handling.
 *
 * Provides static methods to:
 * - Check for available activities that can handle specific intents
 * - Safely start activities with null checks
 * - Handle common social media app intents with fallbacks
 * - Extract data from incoming intents
 *
 * Features:
 * - Automatic null checks for activities and intents
 * - Weak reference handling to prevent memory leaks
 * - Consistent error handling patterns
 * - Support for common social media apps (Facebook, YouTube, Instagram, WhatsApp)
 */
object IntentHelperUtils {

    /**
     * Retrieves all activities that can handle the given intent.
     *
     * @param activity Context activity (uses weak reference)
     * @param intent The intent to resolve
     * @return List of matching activities or empty list if:
     *         - Activity is null
     *         - Intent is null
     *         - No matching activities found
     */
    @JvmStatic
    fun getMatchingActivities(activity: Activity?, intent: Intent?): List<ResolveInfo> {
        if (intent == null || activity == null) return emptyList()
        WeakReference(activity).get()?.let { safeRef ->
            return safeRef.packageManager.queryIntentActivities(intent, 0)
        } ?: run { return emptyList() }
    }

    /**
     * Extracts shared data from an activity's intent.
     * Supports both ACTION_SEND (sharing) and ACTION_VIEW (deep links).
     *
     * @param activity Source activity containing the intent
     * @return The shared:
     *         - Text content (for ACTION_SEND)
     *         - URI string (for ACTION_VIEW)
     *         - null if no matching data found
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
     * Checks if any app can handle the given intent.
     *
     * @param activity Context activity (uses weak reference)
     * @param intent Intent to verify
     * @return true if at least one activity can handle the intent
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
     * Safely starts an activity if possible.
     *
     * @param activity Source activity (uses weak reference)
     * @param intent Intent to launch
     * @return true if activity was started successfully
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
     * Gets the package name of the primary handler for an intent.
     *
     * @param activity Context activity (uses weak reference)
     * @param intent Intent to resolve
     * @return Package name of default handler or empty string if none found
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
     * Opens Facebook app to a specific page, profile, or the home feed.
     *
     * @param context Context to start activity from
     * @param targetUrl Optional Facebook URL (supports multiple formats):
     *                  - Profile: "https://www.facebook.com/username"
     *                  - Page: "https://www.facebook.com/pagename"
     *                  - Post: "https://www.facebook.com/permalink.php?story_fbid=POST_ID"
     *                  - Defaults to Facebook home feed ("https://www.facebook.com") if null
     * @param onError Callback invoked when:
     *                - Facebook app is not installed
     *                - Invalid URL format provided
     *                - Any other exception occurs
     * @return true if intent was launched successfully, false otherwise
     */
    @JvmStatic
    fun openFacebookApp(
        context: Context,
        targetUrl: String? = "https://www.facebook.com",
        onError: (() -> Unit)? = null
    ): Boolean {
        return try {
            val uri = (targetUrl ?: "https://www.facebook.com").toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.facebook.katana")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                onError?.invoke()
                false
            }
        } catch (error: Exception) {
            onError?.invoke()
            false
        }
    }

    /**
     * Opens YouTube app with fallback handling.
     *
     * @param context Context to start activity
     * @param videoOrChannelUrl Optional YouTube URL (e.g., "https://youtube.com/watch?v=VIDEO_ID"
     *                          or "https://youtube.com/c/CHANNEL_NAME").
     *                          Defaults to YouTube home page if null.
     * @param onError Callback invoked if:
     *                - YouTube app not installed
     *                - Any exception occurs
     */
    @JvmStatic
    fun openYouTubeApp(
        context: Context,
        videoOrChannelUrl: String? = "https://www.youtube.com",
        onError: (() -> Unit)? = null
    ) {
        try {
            val uri = (videoOrChannelUrl ?: "https://www.youtube.com").toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                onError?.invoke()
            }
        } catch (error: Exception) {
            onError?.invoke()
        }
    }

    /**
     * Opens Instagram app with fallback handling.
     *
     * @param context Context to start activity
     * @param profileUrl Optional Instagram profile URL (e.g., "http://instagram.com/username")
     *                   Defaults to Instagram home page if null
     * @param onError Callback invoked if:
     *                - Instagram app not installed
     *                - Any exception occurs
     */
    @JvmStatic
    fun openInstagramApp(
        context: Context,
        profileUrl: String? = "http://instagram.com",
        onError: (() -> Unit)? = null
    ) {
        try {
            val uri = (profileUrl ?: "http://instagram.com").toUri()
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                onError?.invoke()
            }
        } catch (error: Exception) {
            onError?.invoke()
        }
    }

    /**
     * Opens WhatsApp app with fallback handling.
     *
     * @param context Context to start activity
     * @param onError Callback invoked if:
     *                - WhatsApp not installed
     *                - Any exception occurs
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