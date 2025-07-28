package app.core.engines.caches

import app.core.AIOApp.Companion.INSTANCE
import lib.networks.URLUtilityKT.getBaseDomain
import lib.networks.URLUtilityKT.getGoogleFaviconUrl
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * AIOFavicons is responsible for downloading and caching website favicons locally.
 *
 * It uses Google's favicon service to fetch favicons and stores them in the app's internal file directory.
 */
class AIOFavicons {
	
	// Directory for storing cached favicon images
	private val faviconDir = File(INSTANCE.filesDir, "favicons")
		.apply { if (!exists()) mkdirs() }
	
	/**
	 * Downloads and saves the favicon for the given URL to the local favicon directory.
	 *
	 * @param url The URL of the website whose favicon is to be saved.
	 * @return The absolute path to the saved favicon file, or null if the download fails.
	 */
	private fun saveFavicon(url: String): String? {
		val baseDomain = getBaseDomain(url) ?: return null
		val faviconFile = File(faviconDir, "$baseDomain.png")
		if (faviconFile.exists()) return faviconFile.absolutePath
		val faviconUrl = getGoogleFaviconUrl(url)
		
		return try {
			val connection = URL(faviconUrl).openConnection() as HttpsURLConnection
			connection.inputStream.use { input ->
				FileOutputStream(faviconFile).use { output ->
					input.copyTo(output)
				}
			}
			faviconFile.absolutePath
		} catch (error: Exception) {
			error.printStackTrace()
			null
		}
	}
	
	/**
	 * Returns the path to the cached favicon image for the given URL.
	 * If the favicon is not cached, it attempts to download and save it.
	 *
	 * @param url The URL of the website.
	 * @return The absolute path to the favicon image, or null if it couldn't be retrieved.
	 */
	fun getFavicon(url: String): String? {
		// Extract base domain from the URL
		val baseDomain = getBaseDomain(url) ?: return null
		
		// Check if the favicon is already cached
		val faviconFile = File(faviconDir, "$baseDomain.png")
		return if (faviconFile.exists())
			faviconFile.absolutePath else saveFavicon(url)
	}
}