package app.core.engines.backend

import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.Process.killProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lib.process.AsyncJobUtils
import lib.process.ThreadsUtility
import java.net.URL
import kotlin.system.exitProcess

/**
 * Utility object responsible for handling self-destruction logic of the application
 * based on remote version information. If a self-destruct flag is set remotely,
 * the app will play a warning audio and then forcefully exit.
 */
object AIOSelfDestruct {
	
	/**
	 * Checks the remote version info for the `self_destruct` flag.
	 * If set to true, plays a self-destruct audio and then exits the app.
	 */
	@JvmStatic
	fun shouldSelfDestruct() {
		ThreadsUtility.executeInBackground(codeBlock = {
			readVersionInfoFromUrl()?.let { aioVersionInfo ->
				if (aioVersionInfo.selfDestruct) {
					AsyncJobUtils.executeOnMainThread {
						Handler(Looper.getMainLooper()).post {
							// Forcefully kill the app process
							killProcess(Process.myPid())
							exitProcess(0)
						}
					}
				}
			}
		})
	}
	
	/**
	 * Downloads and parses version information from a predefined remote text file.
	 * The file must be structured as key-value pairs, separated by "=".
	 *
	 * Example format:
	 * ```
	 * ver=20250506
	 * url=https://example.com/app.apk
	 * self_destruct=false
	 * ```
	 *
	 * @return [AIOVersionInfo] if parsing is successful, otherwise null.
	 */
	suspend fun readVersionInfoFromUrl(): AIOVersionInfo? = withContext(Dispatchers.IO) {
		try {
			val rawUrl = "https://raw.githubusercontent.com/shibaFoss/" +
					"aio_version/refs/heads/main/version_info"
			
			val content = URL(rawUrl).readText()
			
			// Parse content into a map of key-value pairs
			val infoMap = content
				.lineSequence()
				.filter { it.contains("=") }
				.map { line ->
					val (key, value) = line.split("=", limit = 2)
					key.trim() to value.trim()
				}.toMap()
			
			// Extract fields
			val version = infoMap["ver"]?.toIntOrNull() ?: return@withContext null
			val url = infoMap["url"] ?: return@withContext null
			val selfDestruct = infoMap["self_destruct"]?.toBoolean() ?: false
			
			return@withContext AIOVersionInfo(version, url, selfDestruct)
			
		} catch (error: Exception) {
			error.printStackTrace()
			return@withContext null
		}
	}
	
	/**
	 * Data class that holds version metadata parsed from the remote version info file.
	 *
	 * @property versionCode The version number of the app (used for comparison).
	 * @property downloadUrl URL pointing to the latest version's APK.
	 * @property selfDestruct Boolean flag indicating whether the app should terminate itself.
	 */
	data class AIOVersionInfo(
		val versionCode: Int,
		val downloadUrl: String,
		val selfDestruct: Boolean
	)
}