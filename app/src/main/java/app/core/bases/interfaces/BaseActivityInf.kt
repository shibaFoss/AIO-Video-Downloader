package app.core.bases.interfaces

import app.core.bases.BaseActivity

/**
 * Defines a contract for base activities to implement standardized behaviors
 * related to lifecycle, permissions, navigation, system UI, and other common features.
 */
interface BaseActivityInf {
	
	/**
	 * Called to provide the layout resource ID for the activity.
	 *
	 * @return The layout resource ID to be used in setContentView().
	 */
	fun onRenderingLayout(): Int
	
	/**
	 * Called after the activity's layout has been rendered.
	 * Can be used to initialize views or listeners.
	 */
	fun onAfterLayoutRender()
	
	/**
	 * Called when the activity is resumed (in onResume).
	 */
	fun onResumeActivity()
	
	/**
	 * Called when the activity is paused (in onPause).
	 */
	fun onPauseActivity()
	
	/**
	 * Called when the back button is pressed.
	 * Should handle any custom back press behavior.
	 */
	fun onBackPressActivity()
	
	/**
	 * Launches a permission request for the specified permissions.
	 *
	 * @param permissions A list of permissions to request from the user.
	 */
	fun launchPermissionRequest(permissions: ArrayList<String>)
	
	/**
	 * Sets the colors of the system status bar and navigation bar.
	 *
	 * @param statusBarColorResId Resource ID of the desired status bar color.
	 * @param navigationBarColorResId Resource ID of the desired navigation bar color.
	 * @param isLightStatusBar Whether to use dark icons for the status bar.
	 * @param isLightNavigationBar Whether to use dark icons for the navigation bar.
	 */
	fun setSystemBarsColors(
		statusBarColorResId: Int,
		navigationBarColorResId: Int,
		isLightStatusBar: Boolean = false,
		isLightNavigationBar: Boolean = false
	)
	
	/**
	 * Opens another activity.
	 *
	 * @param activity The class of the activity to open.
	 * @param shouldAnimate Whether to apply an opening animation.
	 */
	fun openActivity(activity: Class<*>, shouldAnimate: Boolean = true)
	
	/**
	 * Closes the activity with a swipe animation.
	 *
	 * @param shouldAnimate Whether to apply the swipe closing animation.
	 */
	fun closeActivityWithSwipeAnimation(shouldAnimate: Boolean = false)
	
	/**
	 * Closes the activity with a fade animation.
	 *
	 * @param shouldAnimate Whether to apply the fade closing animation.
	 */
	fun closeActivityWithFadeAnimation(shouldAnimate: Boolean = false)
	
	/**
	 * Exits the activity if the back button is pressed twice within a short time frame.
	 */
	fun exitActivityOnDoubleBackPress()
	
	/**
	 * Forcefully quits the application.
	 */
	fun forceQuitApplication()
	
	/**
	 * Opens the app's information screen in the system settings.
	 */
	fun openAppInfoSetting()
	
	/**
	 * Opens the application's page in the Google Play Store.
	 */
	fun openApplicationInPlayStore()
	
	/**
	 * Retrieves the current device time zone ID.
	 *
	 * @return The time zone ID string.
	 */
	fun getTimeZoneId(): String
	
	/**
	 * Returns a reference to the BaseActivity implementing this interface.
	 *
	 * @return The current activity instance, or null if not available.
	 */
	fun getActivity(): BaseActivity?
	
	/**
	 * Triggers a short vibration on the device.
	 *
	 * @param timeInMillis Duration of the vibration in milliseconds.
	 */
	fun doSomeVibration(timeInMillis: Int)
	
	/**
	 * Provides flags that should be set on an Intent to ensure it launches the target activity
	 * in singleTop mode (i.e., reuse an existing instance if one exists).
	 *
	 * @return The appropriate intent flags.
	 */
	fun getSingleTopIntentFlags(): Int
}