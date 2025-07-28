package lib.device

import android.content.Context
import android.content.Context.TELEPHONY_SERVICE
import android.content.Intent
import android.content.Intent.ACTION_BATTERY_CHANGED
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.os.BatteryManager
import android.os.BatteryManager.EXTRA_LEVEL
import android.os.BatteryManager.EXTRA_SCALE
import android.os.BatteryManager.EXTRA_STATUS
import android.os.Build
import android.os.Environment.getExternalStorageDirectory
import android.os.StatFs
import android.telephony.TelephonyManager
import lib.networks.NetworkUtility.getServiceProvider
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * Utility object for retrieving various details about the current device,
 * such as app information, hardware specifications, storage, locale,
 * network details, and battery status.
 */
object DeviceInfoUtils {
	
	/**
	 * Returns a formatted string containing complete device and app information.
	 * This includes version info, device specs, screen resolution, locale, network,
	 * and battery status.
	 *
	 * @param context The context from which to retrieve system services and resources.
	 * @return A multi-line string with detailed information about the device and app.
	 */
	@JvmStatic
	fun getDeviceInformation(context: Context?): String {
		WeakReference(context).get()?.let { safeContextRef ->
			val sb = StringBuilder()
			val pm = safeContextRef.packageManager
			val packageInfo: PackageInfo = pm.getPackageInfo(safeContextRef.packageName, 0)
			
			// App details
			sb.append("App Version: ${getApplicationVersionName()} (${getApplicationVersionCode()})\n")
			sb.append("App Package Name: ${packageInfo.packageName}\n")
			
			// Device build info
			sb.append("Device Model: ${Build.MODEL}\n")
			sb.append("Device Manufacturer: ${Build.MANUFACTURER}\n")
			sb.append("Android Version: ${Build.VERSION.RELEASE}\n")
			sb.append("API Level: ${Build.VERSION.SDK_INT}\n")
			sb.append("Device Hardware: ${Build.HARDWARE}\n")
			sb.append("Device Brand: ${Build.BRAND}\n")
			sb.append("Device Board: ${Build.BOARD}\n")
			sb.append("Device Bootloader: ${Build.BOOTLOADER}\n")
			sb.append("Device Host: ${Build.HOST}\n")
			sb.append("Device Tags: ${Build.TAGS}\n")
			sb.append("Device Type: ${Build.TYPE}\n")
			sb.append("Device User: ${Build.USER}\n")
			
			// Screen metrics
			val metrics = safeContextRef.resources.displayMetrics
			sb.append("Screen Resolution: ${metrics.widthPixels}x${metrics.heightPixels}\n")
			sb.append("Screen Density: ${metrics.densityDpi} dpi\n")
			
			// Storage
			sb.append("Available Storage: ${getDeviceAvailableStorage()} bytes\n")
			sb.append("Total Storage: ${getDeviceTotalStorage()} bytes\n")
			
			// Network
			sb.append("Network Operator: ${getServiceProvider()}\n")
			sb.append("Network Country: ${getDeviceNetworkCountry(safeContextRef)}\n")
			sb.append("Sim Country: ${getDeviceSimCountry(safeContextRef)}\n")
			sb.append("Sim Operator: ${getDeviceSimOperator(safeContextRef)}\n")
			
			// Locale
			sb.append("Locale: ${Locale.getDefault().displayName}\n")
			sb.append("Language: ${Locale.getDefault().language}\n")
			sb.append("Country: ${Locale.getDefault().country}\n")
			
			// Battery
			getDeviceBatteryStatus(safeContextRef)?.let {
				sb.append("Battery Status: ${it.first}\n")
				sb.append("Battery Level: ${it.second}%\n")
			}
			
			return sb.toString()
		} ?: run { return "" }
	}
	
	/**
	 * Gets the app's version name.
	 */
	@JvmStatic
	private fun getApplicationVersionName(): String? {
		return AppVersionUtility.versionName
	}
	
	/**
	 * Gets the app's version code.
	 */
	@JvmStatic
	private fun getApplicationVersionCode(): String {
		return AppVersionUtility.versionCode.toString()
	}
	
	/**
	 * Calculates and returns the device's total external storage in bytes.
	 */
	@JvmStatic
	private fun getDeviceTotalStorage(): Long {
		val stat = StatFs(getExternalStorageDirectory().absolutePath)
		return stat.blockCountLong * stat.blockSizeLong
	}
	
	/**
	 * Calculates and returns the device's available external storage in bytes.
	 */
	@JvmStatic
	private fun getDeviceAvailableStorage(): Long {
		val stat = StatFs(getExternalStorageDirectory().absolutePath)
		return stat.availableBlocksLong * stat.blockSizeLong
	}
	
	/**
	 * Retrieves the current network country ISO from the TelephonyManager.
	 */
	@JvmStatic
	private fun getDeviceNetworkCountry(context: Context?): String {
		WeakReference(context).get()?.let { safeRes ->
			val telephonyService = safeRes.getSystemService(TELEPHONY_SERVICE)
			val telephonyManager =  telephonyService as TelephonyManager
			return telephonyManager.networkCountryIso
		} ?: run { return "" }
	}
	
	/**
	 * Retrieves the SIM card's country ISO from the TelephonyManager.
	 */
	@JvmStatic
	private fun getDeviceSimCountry(context: Context?): String {
		WeakReference(context).get()?.let { safeRes ->
			val telephonyService = safeRes.getSystemService(TELEPHONY_SERVICE)
			val telephonyManager =  telephonyService as TelephonyManager
			return telephonyManager.simCountryIso
		} ?: run { return "" }
	}
	
	/**
	 * Retrieves the SIM operator name from the TelephonyManager.
	 */
	@JvmStatic
	private fun getDeviceSimOperator(context: Context?): String {
		WeakReference(context).get()?.let { safeRes ->
			val telephonyService = safeRes.getSystemService(TELEPHONY_SERVICE)
			val telephonyManager = telephonyService as TelephonyManager
			return telephonyManager.simOperatorName
		} ?: run { return "" }
	}
	
	/**
	 * Fetches the device's current battery status and battery percentage.
	 *
	 * @return A [Pair] containing the battery status string and level in percentage.
	 */
	@JvmStatic
	private fun getDeviceBatteryStatus(context: Context?): Pair<String, Int>? {
		WeakReference(context).get()?.let { safeRes ->
			val batteryStatus: Intent? = IntentFilter(ACTION_BATTERY_CHANGED).let { filter ->
				safeRes.registerReceiver(null, filter)
			}
			val status = batteryStatus?.getIntExtra(EXTRA_STATUS, -1) ?: -1
			val batteryPct = batteryStatus?.let {
				it.getIntExtra(EXTRA_LEVEL, -1) * 100 / it.getIntExtra(EXTRA_SCALE, -1)
			} ?: -1
			val statusString = when (status) {
				BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
				BatteryManager.BATTERY_STATUS_FULL -> "Full"
				BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
				BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
				else -> "Unknown"
			}
			return Pair(statusString, batteryPct)
		} ?: run { return null }
	}
}
