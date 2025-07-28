package app.core.engines.caches

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lib.process.ThreadsUtility.executeInBackground
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * AIOAdBlocker is responsible for managing a list of hostnames used for ad blocking.
 *
 * It retrieves an updated list of ad-blocking hostnames from a remote GitHub raw file.
 * If the fetch fails, it falls back to a default hardcoded list.
 * This class uses OkHttp for HTTP requests and Kotlin coroutines for asynchronous I/O.
 */
class AIOAdBlocker {
	
	companion object {
		// Remote URL containing ad-block hostnames (one per line)
		private const val GITHUB_RAW_URL =
			"https://github.com/shibaFoss/aio_version/raw/main/adblock_host"
	}
	
	// Set of hostnames currently used for ad blocking
	private var adBlockHosts: Set<String> = emptySet()
	
	// OkHttp client for making network requests
	private val client = OkHttpClient()
	
	/**
	 * Returns the current set of ad-block hostnames.
	 */
	fun getAdBlockHosts(): Set<String> {
		return adBlockHosts
	}
	
	/**
	 * Asynchronously fetches ad-block filter hostnames from the remote URL.
	 * Falls back to default hosts if network request fails.
	 */
	fun fetchAdFilters() {
		executeInBackground(codeBlock = {
			try {
				adBlockHosts = fetchHostsFromUrl() ?: defaultHosts.toSet()
			} catch (error: IOException) {
				error.printStackTrace()
				adBlockHosts = defaultHosts.toSet()
			}
		})
	}
	
	/**
	 * Suspended function to fetch and parse ad-block hostnames from the remote URL.
	 * Skips comments and blank lines in the host file.
	 *
	 * @return A Set of valid hostnames, or null if the request fails.
	 */
	private suspend fun fetchHostsFromUrl(): Set<String>? {
		return withContext(Dispatchers.IO) {
			val request = Request.Builder()
				.url(GITHUB_RAW_URL)
				.build()
			
			client.newCall(request).execute().use { response ->
				if (!response.isSuccessful) return@use null
				
				response.body?.string()
					?.lines()?.filterNot { it.startsWith("#") || it.isBlank() }
					?.map { it.trim() }?.toSet()
			}
		}
	}
	
	// Fallback list of hostnames used if remote fetch fails
	private val defaultHosts = listOf(
		"afcdn.net",
		"aucdn.net",
		"tsyndicate.com"
	)
}