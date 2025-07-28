package app.core.engines.backend

import android.os.Build
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioBookmark
import app.core.AIOApp.Companion.aioHistory
import app.core.AIOApp.Companion.aioSettings
import app.core.engines.downloader.DownloadDataModel
import com.aio.R
import com.parse.Parse
import com.parse.ParseInstallation.getCurrentInstallation
import com.parse.ParseObject
import lib.device.AppVersionUtility.versionName
import lib.device.DeviceInfoUtils.getDeviceInformation
import lib.device.DeviceUtility.getDeviceUserCountry
import lib.networks.NetworkUtility.isWifiEnabled
import lib.process.ThreadsUtility.executeInBackground
import lib.texts.CommonTextUtils.getText

/**
 * Handles all backend operations including:
 * - Parse server initialization
 * - Analytics tracking
 * - Crash reporting
 * - User feedback collection
 * - Download logging
 *
 * All operations are performed asynchronously to avoid blocking the main thread.
 */
class AIOBackend {
	
	/**
	 * Initializes the Parse backend when the class is instantiated.
	 * Runs in background thread to prevent UI blocking.
	 */
	init {
		executeInBackground(codeBlock = { initParseBackend() })
	}
	
	/**
	 * Initializes Parse backend with configuration from string resources.
	 * Sets up installation tracking with device and app information.
	 */
	private fun initParseBackend() {
		try {
			// Initialize Parse with credentials from resources
			Parse.initialize(
				Parse.Configuration.Builder(INSTANCE)
					.applicationId(getText(R.string.text_back4app_app_id))
					.clientKey(getText(R.string.text_back4app_client_key))
					.server(getText(R.string.text_back4app_server_url))
					.build()
			)
			
			// Track installation information
			val installation = getCurrentInstallation()
			installation.apply {
				put("user_country", getDeviceUserCountry())
				put("device_model", Build.MODEL)
				put("device_brand", Build.BRAND)
				put("device_version", Build.VERSION.RELEASE)
				put("app_version", versionName ?: "n/a")
				put(
					"network_type", if (isWifiEnabled())
						"Wifi" else "Mobile Data"
				)
			}
			
			// Save installation info to server and locally
			installation.saveInBackground { error ->
				if (error == null) {
					val installationId = installation.installationId
					saveInstallationIdLocally(installationId)
				}
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Tracks application usage statistics and user behavior.
	 * Includes metrics like runtime, downloads, clicks, and settings.
	 */
	fun trackApplicationInfo() {
		try {
			val installation = getCurrentInstallation()
			installation.apply {
				put("app_runtime", aioSettings.totalUsageTimeInFormat)
				put("total_downloads", aioSettings.totalNumberOfSuccessfulDownloads)
				put("rating_clicks", aioSettings.totalClickCountOnRating)
				put("guide_clicks", aioSettings.totalClickCountOnHowToGuide)
				put("media_playbacks", aioSettings.totalClickCountOnMediaPlayback)
				put("language_changes", aioSettings.totalClickCountOnLanguageChange)
				put("video_editor_clicks", aioSettings.totalClickCountOnVideoUrlEditor)
				put("browser_bookmarks", aioBookmark.getBookmarkLibrary().size)
				put("browser_history", aioHistory.getHistoryLibrary().size)
				put("aio_settings_json", aioSettings.convertClassToJSON())
			}
			installation.saveInBackground()
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Logs download information to the backend.
	 * @param downloadDataModel Contains all details about the download
	 */
	fun saveDownloadLog(downloadDataModel: DownloadDataModel) {
		try {
			val cloudTable = ParseObject("DownloadLogs").apply {
				put("installation_id", getCurrentInstallation().installationId)
				put("file_name", downloadDataModel.fileName)
				put("file_directory", downloadDataModel.fileDirectory)
				put("file_url", downloadDataModel.fileURL)
				put("downloaded_date", downloadDataModel.lastModifiedTimeDateInFormat)
				put("downloaded_time", downloadDataModel.timeSpentInFormat)
				put("average_speed", downloadDataModel.averageSpeedInFormat)
				put("entire_class_json", downloadDataModel.convertClassToJSON())
				put("user_country", getDeviceUserCountry())
				put("device_model", Build.MODEL)
				put("device_brand", Build.BRAND)
				put("device_version", Build.VERSION.RELEASE)
				put("app_version", versionName ?: "n/a")
				put(
					"network_type", if (isWifiEnabled())
						"Wifi" else "Mobile Data"
				)
			}
			
			cloudTable.saveInBackground()
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Saves user feedback to the backend.
	 * @param userMessage The feedback message from the user
	 */
	fun saveUserFeedback(userMessage: String) {
		try {
			val cloudTable = ParseObject("UserFeedbacks")
				.apply { put("message", userMessage) }
			cloudTable.saveInBackground()
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Logs crash information to the backend.
	 * @param detailedLogMsg The crash details including stack trace
	 */
	fun saveAppCrashedInfo(detailedLogMsg: String) {
		try {
			val cloudTable = ParseObject("AppCrashedInfo").apply {
				put("detailed_log_msg", detailedLogMsg)
				put("device_details", getDeviceInformation(INSTANCE))
			}
			
			cloudTable.saveInBackground()
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Stores the installation ID locally for future reference.
	 * @param installationId Unique ID generated by Parse
	 */
	private fun saveInstallationIdLocally(installationId: String) {
		try {
			if (installationId.isNotEmpty()) {
				aioSettings.userInstallationId = installationId
				aioSettings.updateInStorage()
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	// Below are click tracking methods for various UI elements
	
	/** Tracks clicks on the video URL editor */
	fun updateClickCountOnVideoUrlEditor() {
		aioSettings.totalClickCountOnVideoUrlEditor++
		aioSettings.updateInStorage()
	}
	
	/** Tracks clicks on the rating prompt */
	fun updateClickCountOnRating() {
		aioSettings.totalClickCountOnRating++
		aioSettings.updateInStorage()
	}
	
	/** Tracks clicks on media play buttons */
	fun updateClickCountOnMediaPlayButton() {
		aioSettings.totalClickCountOnMediaPlayback++
		aioSettings.updateInStorage()
	}
	
	/** Tracks language selector changes */
	fun updateClickCountOnLanguageChanger() {
		aioSettings.totalClickCountOnLanguageChange++
		aioSettings.updateInStorage()
	}
	
	/** Tracks views of the how-to-download guide */
	fun updateClickCountOnHowToDownload() {
		aioSettings.totalClickCountOnHowToGuide++
		aioSettings.updateInStorage()
	}
	
	/** Tracks clicks on home screen bookmarks */
	fun updateClickCountOnHomeBookmark() {
		aioSettings.totalClickCountOnHomeBookmarks++
		aioSettings.updateInStorage()
	}
	
	/** Tracks clicks on home screen favicons */
	fun updateClickCountOnHomesFavicon() {
		aioSettings.totalClickCountOnHomeFavicon++
		aioSettings.updateInStorage()
	}
	
	/** Tracks views of recent downloads list */
	fun updateClickCountOnRecentDownloadsList() {
		aioSettings.totalClickCountOnRecentDownloads++
		aioSettings.updateInStorage()
	}
	
	/** Tracks clicks on home screen history */
	fun updateClickCountOnHomeHistory() {
		aioSettings.totalClickCountOnHomeHistory++
		aioSettings.updateInStorage()
	}
	
	/** Tracks version update checks */
	fun updateClickCountOnCheckVersionUpdate() {
		aioSettings.totalClickCountOnVersionCheck++
		aioSettings.updateInStorage()
	}
}