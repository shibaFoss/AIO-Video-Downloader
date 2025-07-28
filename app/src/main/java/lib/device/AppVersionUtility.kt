package lib.device

import android.content.pm.PackageManager.GET_SIGNATURES
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.PackageManager.PackageInfoFlags.of
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import app.core.AIOApp

/**
 * Utility object to fetch app-related version information and the device's SDK version.
 *
 * Provides methods to retrieve:
 * - Application version name (`versionName`)
 * - Application version code (`versionCode`)
 * - Android SDK version of the device (`deviceSDKVersion`)
 *
 * These are helpful for logging, analytics, debugging, or showing version info in the UI.
 */
object AppVersionUtility {
	
	/**
	 * Gets the version name of the application, as specified in the `build.gradle` file.
	 *
	 * @return The version name as a [String], or `null` if it cannot be retrieved.
	 */
	@JvmStatic
	val versionName: String?
		get() {
			val context = AIOApp.INSTANCE
			val packageName = context.packageName
			return try {
				val packageManager = context.packageManager
				
				// Use appropriate method based on SDK version
				val packageInfo = if (SDK_INT >= TIRAMISU) {
					val flags = of(GET_SIGNING_CERTIFICATES.toLong())
					packageManager.getPackageInfo(packageName, flags)
				} else {
					@Suppress("DEPRECATION")
					packageManager.getPackageInfo(packageName, GET_SIGNATURES)
				}
				
				packageInfo.versionName
			} catch (error: NameNotFoundException) {
				// Log the error and return null if package info cannot be found
				error.printStackTrace()
				null
			}
		}
	
	/**
	 * Gets the version code of the application, which is an incrementing number defined in the build file.
	 *
	 * @return The version code as a [Long], or `0` if it cannot be retrieved.
	 */
	@JvmStatic
	val versionCode: Long
		get() {
			val context = AIOApp.INSTANCE
			val packageName = context.packageName
			return try {
				val packageManager = context.packageManager
				
				// Use proper method depending on the Android version
				val packageInfo = if (SDK_INT >= TIRAMISU) {
					val flags = of(GET_SIGNING_CERTIFICATES.toLong())
					packageManager.getPackageInfo(packageName, flags)
				} else {
					@Suppress("DEPRECATION")
					packageManager.getPackageInfo(packageName, GET_SIGNATURES)
				}
				
				packageInfo.longVersionCode
			} catch (error: NameNotFoundException) {
				// Print error and return default version code
				error.printStackTrace()
				0
			}
		}
	
	/**
	 * Gets the Android SDK version of the device (e.g., 33 for Android 13).
	 *
	 * @return The device's SDK version as an [Int].
	 */
	@JvmStatic
	val deviceSDKVersion: Int
		get() = SDK_INT
}