package lib.device

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Context.TELEPHONY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.os.Build
import android.telephony.TelephonyManager
import app.core.AIOApp.Companion.INSTANCE
import lib.texts.CommonTextUtils.capitalizeFirstLetter
import java.lang.ref.WeakReference
import java.util.Locale.getDefault

/**
 * Utility class for retrieving information related to the device hardware,
 * display, connectivity, and locale settings.
 */
object DeviceUtility {
	
	/**
	 * Retrieves the raw screen density of the device.
	 *
	 * @param context A context to access resources and metrics.
	 * @return A float value representing the screen density (e.g., 1.5, 2.0).
	 */
	@JvmStatic
	fun getDeviceScreenDensity(context: Context?): Float {
		WeakReference(context).get()?.let { safeRef ->
			val displayMetrics = safeRef.resources.displayMetrics
			val density = displayMetrics.density
			return density
		} ?: run { return 0.0f }
	}
	
	/**
	 * Returns the screen density formatted as a human-readable bucket like `hdpi`, `xxhdpi`, etc.
	 *
	 * @param context A context to access display metrics.
	 * @return A string representing the display density category.
	 */
	@JvmStatic
	fun getDeviceScreenDensityInFormat(context: Context?): String {
		WeakReference(context).get()?.let { safeRef ->
			val displayMetrics = safeRef.resources.displayMetrics
			val density = displayMetrics.density
			val formattedDensity = when {
				density >= 4.0 -> "xxxhdpi"
				density >= 3.0 -> "xxhdpi"
				density >= 2.0 -> "xhdpi"
				density >= 1.5 -> "hdpi"
				density >= 1.0 -> "mdpi"
				else -> "ldpi"
			}
			return formattedDensity
		} ?: run { return "" }
	}
	
	/**
	 * Returns the device manufacturer and model as a formatted name.
	 *
	 * @return A capitalized string combining manufacturer and model (e.g., "Samsung Galaxy S10").
	 */
	@JvmStatic
	fun getDeviceManufactureModelName(): String? {
		val manufacturer = Build.MANUFACTURER
		val model: String = Build.MODEL
		val deviceName = if (model.startsWith(manufacturer)) {
			capitalizeFirstLetter(model)
		} else capitalizeFirstLetter("$manufacturer $model")
		return deviceName
	}
	
	/**
	 * Checks whether the device has an active internet connection through
	 * any known transport methods (Wi-Fi, cellular, Ethernet, or Bluetooth).
	 *
	 * @return `true` if connected to the internet, `false` otherwise.
	 */
	@JvmStatic
	fun isDeviceConnectedToInternet(): Boolean {
		val appContext = INSTANCE
		val connectivityService = appContext.getSystemService(CONNECTIVITY_SERVICE)
		val connectivityManager = connectivityService as ConnectivityManager
		val network = connectivityManager.activeNetwork
		val nc = connectivityManager.getNetworkCapabilities(network) ?: run {
			return false
		}
		val wifiChecked = nc.hasTransport(TRANSPORT_WIFI)
		val cellularChecked = nc.hasTransport(TRANSPORT_CELLULAR)
		val ethernetChecked = nc.hasTransport(TRANSPORT_ETHERNET)
		val bluetoothChecked = nc.hasTransport(TRANSPORT_BLUETOOTH)
		
		val isOnline = wifiChecked || cellularChecked || ethernetChecked || bluetoothChecked
		return isOnline
	}
	
	/**
	 * Attempts to determine the user's country based on locale or SIM info.
	 *
	 * @return A string representing the country code (e.g., "US", "IN") or a localized unknown label.
	 */
	@JvmStatic
	fun getDeviceUserCountry(): String {
		val country = getDefault().country
		if (country.isNotEmpty()) return country
		val telephoneService = INSTANCE.getSystemService(TELEPHONY_SERVICE)
		val telephonyManager = telephoneService as TelephonyManager
		val simCountryIso = telephonyManager.simCountryIso
		if (simCountryIso.isNotEmpty()) return simCountryIso.uppercase(getDefault())
		return "Unknown"
	}
}