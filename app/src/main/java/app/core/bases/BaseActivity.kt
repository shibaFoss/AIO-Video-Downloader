@file:Suppress("DEPRECATION")

package app.core.bases

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
import android.view.MotionEvent
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioAdblocker
import app.core.AIOApp.Companion.aioLanguage
import app.core.AIOApp.Companion.aioSettings
import app.core.AIOApp.Companion.idleForegroundService
import app.core.CrashHandler
import app.core.bases.interfaces.BaseActivityInf
import app.core.bases.interfaces.PermissionsResult
import app.core.bases.language.LanguageAwareActivity
import app.ui.main.MotherActivity
import com.aio.R
import com.anggrayudi.storage.SimpleStorageHelper
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.PermissionX.isGranted
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.process.CommonTimeUtils.delay
import lib.ui.ActivityAnimator.animActivityFade
import lib.ui.ActivityAnimator.animActivitySwipeRight
import lib.ui.MsgDialogUtils
import lib.ui.MsgDialogUtils.showMessageDialog
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.builders.ToastView.Companion.showToast
import java.lang.Thread.setDefaultUncaughtExceptionHandler
import java.lang.ref.WeakReference
import java.util.TimeZone
import kotlin.system.exitProcess

/**
 * Base activity class that provides common functionality for all activities in the application.
 *
 * Features include:
 * - Lifecycle management
 * - Permission handling
 * - Activity transitions and animations
 * - System UI customization (status bar, navigation bar)
 * - Vibration feedback
 * - Crash handling
 * - Language support
 * - Ad integration
 * - Storage management
 *
 * Implements [BaseActivityInf] interface for common activity operations.
 */
abstract class BaseActivity : LanguageAwareActivity(), BaseActivityInf {
	
	// Weak reference to the activity instance for safe access
	private var weakBaseActivityRef: WeakReference<BaseActivity>? = null
	private var safeBaseActivityRef: BaseActivity? = null
	
	// Flag to track if permission check is in progress
	private var isUserPermissionCheckingActive = false
	
	// Flag to track activity running state
	private var isActivityRunning = false
	
	// Counter for back button presses
	private var isBackButtonEventFired = 0
	
	// Helper for scoped storage access
	open var scopedStorageHelper: SimpleStorageHelper? = null
	
	// Listener for permission check results
	open var permissionCheckListener: PermissionsResult? = null
	
	// Vibrator instance for haptic feedback
	private val vibrator: Vibrator by lazy {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			val vmClass = VibratorManager::class.java
			val vibratorManager = getSystemService(vmClass)
			vibratorManager.defaultVibrator
		} else getSystemService(Vibrator::class.java)
	}
	
	override fun onStart() {
		super.onStart()
		isActivityRunning = true
	}
	
	@SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// Initialize weak reference to this activity
		weakBaseActivityRef = WeakReference(this)
		safeBaseActivityRef = weakBaseActivityRef?.get()
		
		safeBaseActivityRef?.let { safeActivityRef ->
			// Set up crash handler
			setDefaultUncaughtExceptionHandler(CrashHandler())
			
			// Configure system UI
			setLightSystemBarTheme()
			
			// Initialize storage helper
			scopedStorageHelper = SimpleStorageHelper(safeActivityRef)
			
			// Apply user selected language
			aioLanguage.applyUserSelectedLanguage(safeActivityRef)
			
			// Lock orientation to portrait
			requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
			
			// Set up back press handler
			WeakReference(object : OnBackPressedCallback(true) {
				override fun handleOnBackPressed() = onBackPressActivity()
			}).get()?.let { onBackPressedDispatcher.addCallback(safeActivityRef, it) }
			
			// Set content view if layout is specified
			if (onRenderingLayout() > -1) setContentView(onRenderingLayout())
		}
	}
	
	override fun onPostCreate(savedInstanceState: Bundle?) {
		super.onPostCreate(savedInstanceState)
		// Called after onCreate() has completed
		onAfterLayoutRender()
	}
	
	override fun onResume() {
		super.onResume()
		// Reinitialize weak reference if null
		if (safeBaseActivityRef == null) safeBaseActivityRef = WeakReference(this).get()
		
		safeBaseActivityRef?.let { safeActivityRef ->
			isActivityRunning = true
			
			// Check and request permissions if needed
			requestForPermissionIfRequired()
			
			// Update foreground service state
			idleForegroundService.updateService()
			
			// Validate user selected folder
			aioSettings.validateUserSelectedFolder()
			
			// Initialize YouTube DL
			INSTANCE.initializeYtDLP()
			
			// Call subclass resume handler
			onResumeActivity()
			
			// Check for language changes
			aioLanguage.closeActivityIfLanguageChanged(safeActivityRef)
			
			// Update ad blocker filters
			aioAdblocker.fetchAdFilters()
		}
	}
	
	override fun onPause() {
		super.onPause()
		isActivityRunning = false
		// Call subclass pause handler
		onPauseActivity()
	}
	
	override fun onDestroy() {
		super.onDestroy()
		isActivityRunning = false
		// Cancel any ongoing vibration
		vibrator.cancel()
	}
	
	/**
	 * Called when activity is paused. Subclasses can override to add custom behavior.
	 */
	override fun onPauseActivity() {
		// Default implementation does nothing
	}
	
	/**
	 * Called when activity is resumed. Subclasses can override to add custom behavior.
	 */
	override fun onResumeActivity() {
		// Default implementation does nothing
	}
	
	/**
	 * Configures system bars (status bar and navigation bar) appearance.
	 *
	 * @param statusBarColorResId Resource ID for status bar color
	 * @param navigationBarColorResId Resource ID for navigation bar color
	 * @param isLightStatusBar Whether status bar icons should be light (for dark backgrounds)
	 * @param isLightNavigationBar Whether navigation bar icons should be light (for dark backgrounds)
	 */
	override fun setSystemBarsColors(
		statusBarColorResId: Int,
		navigationBarColorResId: Int,
		isLightStatusBar: Boolean,
		isLightNavigationBar: Boolean,
	) {
		val activityWindow = window
		activityWindow.statusBarColor = getColor(this, statusBarColorResId)
		activityWindow.navigationBarColor = getColor(this, navigationBarColorResId)
		val decorView = activityWindow.decorView
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			val insetsController = activityWindow.insetsController
			insetsController?.setSystemBarsAppearance(
				if (isLightStatusBar) APPEARANCE_LIGHT_STATUS_BARS else 0,
				APPEARANCE_LIGHT_STATUS_BARS
			)
			
			insetsController?.setSystemBarsAppearance(
				if (isLightNavigationBar) APPEARANCE_LIGHT_NAVIGATION_BARS else 0,
				APPEARANCE_LIGHT_NAVIGATION_BARS
			)
		} else {
			if (isLightStatusBar) decorView.systemUiVisibility =
				decorView.systemUiVisibility or
						SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
			else decorView.systemUiVisibility =
				decorView.systemUiVisibility and
						SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
			
			if (isLightNavigationBar) decorView.systemUiVisibility =
				decorView.systemUiVisibility or
						SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
			else decorView.systemUiVisibility =
				decorView.systemUiVisibility and
						SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
		}
	}
	
	/**
	 * Handles touch events to dismiss soft keyboard when touching outside EditText.
	 */
	override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
		if (motionEvent.action == MotionEvent.ACTION_DOWN) {
			val focusedView = currentFocus
			if (focusedView is EditText) {
				val outRect = Rect()
				focusedView.getGlobalVisibleRect(outRect)
				if (!outRect.contains(motionEvent.rawX.toInt(), motionEvent.rawY.toInt())) {
					focusedView.clearFocus()
					val service = getSystemService(INPUT_METHOD_SERVICE)
					val imm = service as InputMethodManager
					imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
				}
			}
		}
		return super.dispatchTouchEvent(motionEvent)
	}
	
	/**
	 * Launches a permission request dialog for the specified permissions.
	 *
	 * @param permissions List of permissions to request
	 */
	override fun launchPermissionRequest(permissions: ArrayList<String>) {
		safeBaseActivityRef?.let { safeActivityRef ->
			PermissionX.init(safeActivityRef).permissions(permissions)
				.onExplainRequestReason { callback, deniedList ->
					// Show explanation dialog when permissions are denied
					callback.showRequestReasonDialog(
						permissions = deniedList,
						message = getString(R.string.text_allow_the_permissions),
						positiveText = getString(R.string.title_allow_now)
					)
				}.onForwardToSettings { scope, deniedList ->
					// Show dialog to redirect to settings when permissions are permanently denied
					scope.showForwardToSettingsDialog(
						permissions = deniedList,
						message = getString(R.string.text_allow_permission_in_setting),
						positiveText = getString(R.string.title_allow_now)
					)
				}.request { allGranted, grantedList, deniedList ->
					// Handle permission request results
					isUserPermissionCheckingActive = false
					permissionCheckListener?.onPermissionResultFound(
						isGranted = allGranted,
						grantedLs = grantedList,
						deniedLs = deniedList
					)
				}; isUserPermissionCheckingActive = true
		}
	}
	
	/**
	 * Opens another activity with optional animation.
	 *
	 * @param activity The activity class to open
	 * @param shouldAnimate Whether to show transition animation
	 */
	override fun openActivity(activity: Class<*>, shouldAnimate: Boolean) {
		safeBaseActivityRef?.let { safeActivityRef ->
			val intent = Intent(safeActivityRef, activity)
			intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
			startActivity(intent)
			if (shouldAnimate) {
				animActivityFade(safeActivityRef)
			}
		}
	}
	
	/**
	 * Closes the current activity with swipe animation.
	 *
	 * @param shouldAnimate Whether to show transition animation
	 */
	override fun closeActivityWithSwipeAnimation(shouldAnimate: Boolean) {
		safeBaseActivityRef?.apply {
			finish(); if (shouldAnimate) animActivitySwipeRight(this)
		}
	}
	
	/**
	 * Closes the current activity with fade animation.
	 *
	 * @param shouldAnimate Whether to show transition animation
	 */
	override fun closeActivityWithFadeAnimation(shouldAnimate: Boolean) {
		safeBaseActivityRef?.apply {
			finish(); if (shouldAnimate) animActivityFade(this)
		}
	}
	
	/**
	 * Handles double back press to exit the activity.
	 * Shows toast on first press, exits on second press within 2 seconds.
	 */
	override fun exitActivityOnDoubleBackPress() {
		if (isBackButtonEventFired == 0) {
			showToast(msgId = R.string.text_press_back_button_to_exit)
			isBackButtonEventFired = 1
			delay(2000, object : OnTaskFinishListener {
				override fun afterDelay() {
					isBackButtonEventFired = 0
				}
			})
		} else if (isBackButtonEventFired == 1) {
			isBackButtonEventFired = 0
			closeActivityWithSwipeAnimation()
		}
	}
	
	/**
	 * Force quits the application.
	 */
	override fun forceQuitApplication() {
		Process.killProcess(Process.myPid())
		exitProcess(0)
	}
	
	/**
	 * Opens the app info settings screen.
	 */
	override fun openAppInfoSetting() {
		val packageName = this.packageName
		val uri = "package:$packageName".toUri()
		val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS, uri)
		startActivity(intent)
	}
	
	/**
	 * Opens the app in Play Store.
	 */
	override fun openApplicationOfficialSite() {
		try {
			val uri = "https://github.com/shibaFoss/VideoMate"
			startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
		} catch (error: Exception) {
			error.printStackTrace()
			showToast(msgId = R.string.text_please_install_web_browser)
		}
	}
	
	/**
	 * Gets the current time zone ID.
	 *
	 * @return The time zone ID string
	 */
	override fun getTimeZoneId(): String {
		return TimeZone.getDefault().id
	}
	
	/**
	 * Gets the current activity instance.
	 *
	 * @return The current activity or null if not available
	 */
	override fun getActivity(): BaseActivity? {
		return safeBaseActivityRef
	}
	
	/**
	 * Clear the weak reference of the activity. Careful using the function,
	 * and should call by the application life cycle manager to automatically
	 * clear up the reference.
	 */
	open fun clearWeakActivityReference() {
		weakBaseActivityRef?.clear()
		safeBaseActivityRef = null
	}
	
	/**
	 * Triggers device vibration for the specified duration.
	 *
	 * @param timeInMillis Duration of vibration in milliseconds
	 */
	override fun doSomeVibration(timeInMillis: Int) {
		if (vibrator.hasVibrator()) {
			vibrator.vibrate(
				VibrationEffect.createOneShot(
					timeInMillis.toLong(), VibrationEffect.DEFAULT_AMPLITUDE
				)
			)
		}
	}
	
	/**
	 * Gets standard intent flags for single top activities.
	 *
	 * @return Combined intent flags
	 */
	override fun getSingleTopIntentFlags(): Int {
		return FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
	}
	
	/**
	 * Shows a dialog indicating upcoming features.
	 */
	fun showUpcomingFeatures() {
		doSomeVibration(50)
		safeBaseActivityRef?.let { safeActivityRef ->
			showMessageDialog(
				baseActivityInf = safeActivityRef,
				isTitleVisible = true,
				titleText = getString(R.string.text_feature_isnt_implemented),
				isNegativeButtonVisible = false,
				positiveButtonText = getString(R.string.title_okay),
				messageTextViewCustomize = { messageTextView ->
					messageTextView.setText(R.string.text_feature_isnt_available_yet)
				},
				titleTextViewCustomize = { titleTextView ->
					val colorResId = R.color.color_green
					val color = safeActivityRef.resources.getColor(colorResId, null)
					titleTextView.setTextColor(color)
				},
				positiveButtonTextCustomize = { positiveButton: TextView ->
					val drawable = getDrawable(applicationContext, R.drawable.ic_okay_done)
					drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
					positiveButton.setCompoundDrawables(drawable, null, null, null)
				}
			)
		}
	}
	
	/**
	 * Requests permissions if they haven't been granted yet.
	 */
	private fun requestForPermissionIfRequired() {
		safeBaseActivityRef?.let { safeActivityRef ->
			if (!isUserPermissionCheckingActive) {
				delay(1000, object : OnTaskFinishListener {
					override fun afterDelay() {
						val permissions = getRequiredPermissionsBySDKVersion()
						if (permissions.isNotEmpty() && !isGranted(safeActivityRef, permissions[0]))
							launchPermissionRequest(getRequiredPermissionsBySDKVersion()) else
							permissionCheckListener?.onPermissionResultFound(true, permissions, null)
					}
				})
			}
		}
	}

	/**
	 * Checks if activity is currently running.
	 *
	 * @return true if activity is running, false otherwise
	 */
	fun isActivityRunning(): Boolean {
		return isActivityRunning
	}
	
	/**
	 * Gets required permissions based on SDK version.
	 *
	 * @return List of required permissions
	 */
	fun getRequiredPermissionsBySDKVersion(): ArrayList<String> {
		val permissions: ArrayList<String> = ArrayList()
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			permissions.add(POST_NOTIFICATIONS)
		else permissions.add(WRITE_EXTERNAL_STORAGE)
		return permissions
	}
	
	/**
	 * Sets light theme for system bars (light icons on dark background).
	 */
	fun setLightSystemBarTheme() {
		setSystemBarsColors(
			statusBarColorResId = R.color.color_surface,
			navigationBarColorResId = R.color.color_surface,
			isLightStatusBar = true,
			isLightNavigationBar = true
		)
	}
	
	/**
	 * Sets dark theme for system bars (dark icons on light background).
	 */
	fun setDarkSystemBarTheme() {
		setSystemBarsColors(
			statusBarColorResId = R.color.color_primary,
			navigationBarColorResId = R.color.color_primary,
			isLightStatusBar = false,
			isLightNavigationBar = false
		)
	}
	
	/**
	 * Configures edge-to-edge fullscreen mode.
	 */
	fun setEdgeToEdgeFullscreen() {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			window.insetsController?.let {
				it.hide(WindowInsets.Type.systemBars())
				it.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
			}
		} else {
			window.decorView.systemUiVisibility = (SYSTEM_UI_FLAG_LAYOUT_STABLE
					or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					or SYSTEM_UI_FLAG_HIDE_NAVIGATION
					or SYSTEM_UI_FLAG_FULLSCREEN
					or SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
		}
		
		WindowCompat.getInsetsController(window, window.decorView).let { controller ->
			controller.hide(WindowInsetsCompat.Type.systemBars())
			val barsBySwipe = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
			controller.systemBarsBehavior = barsBySwipe
		}
	}
	
	/**
	 * Disables edge-to-edge mode.
	 */
	fun disableEdgeToEdge() {
		WindowCompat.setDecorFitsSystemWindows(window, true)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			window.insetsController?.let {
				it.show(WindowInsets.Type.systemBars())
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
				}
			}
		} else {
			val flags = (SYSTEM_UI_FLAG_LAYOUT_STABLE
					or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
			window.decorView.systemUiVisibility = flags
		}
		
		WindowCompat.getInsetsController(window, window.decorView).let { controller ->
			controller.show(WindowInsetsCompat.Type.systemBars())
			controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
		}
	}
	
	/**
	 * Configures edge-to-edge mode with custom cutout color.
	 *
	 * @param color The color to use for cutout areas
	 */
	fun setEdgeToEdgeCustomCutoutColor(@ColorInt color: Int) {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		window.statusBarColor = color
		window.navigationBarColor = color
		val shortEdges = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
		window.attributes.layoutInDisplayCutoutMode = shortEdges
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			window.setBackgroundDrawable(color.toDrawable())
		}
	}
	
	/**
	 * Checks if the app is currently ignoring battery optimizations.
	 *
	 * @return `true` if the app is excluded from battery optimizations, `false` otherwise.
	 */
	fun isBatteryOptimizationIgnored(): Boolean {
		val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
		return powerManager?.isIgnoringBatteryOptimizations(packageName) == true
	}
	
	/**
	 * Prompts the user to manually disable battery optimizations for the app.
	 *
	 * This method should only be triggered after the user has successfully completed at least one download.
	 * It shows a custom dialog explaining why battery optimization should be disabled (useful for background
	 * operations such as large file downloads). If the user agrees, it launches the system settings screen
	 * where the user can manually disable battery optimizations for the app.
	 */
	fun requestForDisablingBatteryOptimization() {
		if (aioSettings.totalNumberOfSuccessfulDownloads < 1) return
		if (safeBaseActivityRef !is MotherActivity) return
		
		MsgDialogUtils.getMessageDialog(
			baseActivityInf = safeBaseActivityRef,
			isTitleVisible = true,
			isNegativeButtonVisible = false,
			messageTextViewCustomize = { it.setText(R.string.text_battery_optimization_msg) },
			titleTextViewCustomize = { it.setText(R.string.title_turn_off_battery_optimization) },
			positiveButtonTextCustomize = {
				it.setText(R.string.title_disable_now)
				it.setLeftSideDrawable(R.drawable.ic_button_arrow_next)
			}
		)?.apply {
			setOnClickForPositiveButton {
				val intent = Intent(ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
				startActivity(intent)
			}
		}?.show()
	}
}