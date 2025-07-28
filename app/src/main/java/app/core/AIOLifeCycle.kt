package app.core

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle

/**
 * AIOLifeCycle is a simplified interface that implements [ActivityLifecycleCallbacks] with empty default methods.
 *
 * This allows you to override only the lifecycle methods you care about in your implementation,
 * rather than implementing all seven methods of the interface.
 *
 * It's useful when you want to observe the lifecycle of all activities globally from the Application class.
 *
 * Example usage:
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         registerActivityLifecycleCallbacks(object : AIOLifeCycle {
 *             override fun onActivityResumed(activity: Activity) {
 *                 // Perform something when any activity is resumed
 *             }
 *         })
 *     }
 * }
 * ```
 */
interface AIOLifeCycle : ActivityLifecycleCallbacks {
	
	/**
	 * Called when an activity is first created.
	 *
	 * @param activity The activity being created.
	 * @param savedInstanceState The saved state passed to the activity.
	 */
	override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
	
	/**
	 * Called when an activity becomes visible to the user.
	 *
	 * @param activity The activity that is starting.
	 */
	override fun onActivityStarted(activity: Activity) {}
	
	/**
	 * Called when the activity starts interacting with the user.
	 *
	 * @param activity The activity that is resuming.
	 */
	override fun onActivityResumed(activity: Activity) {}
	
	/**
	 * Called when the system is about to resume another activity.
	 *
	 * @param activity The activity being paused.
	 */
	override fun onActivityPaused(activity: Activity) {}
	
	/**
	 * Called when the activity is no longer visible to the user.
	 *
	 * @param activity The activity being stopped.
	 */
	override fun onActivityStopped(activity: Activity) {}
	
	/**
	 * Called before the activity is destroyed to save its current state.
	 *
	 * @param activity The activity being saved.
	 * @param outState Bundle to save state information.
	 */
	override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
	
	/**
	 * Called when the activity is destroyed.
	 *
	 * @param activity The activity being destroyed.
	 */
	override fun onActivityDestroyed(activity: Activity) {}
}
