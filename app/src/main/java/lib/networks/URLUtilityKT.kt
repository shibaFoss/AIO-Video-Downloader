package lib.networks

import app.core.engines.video_parser.parsers.SupportedURLs
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit

/**
 * Kotlin utility object for URL-related operations including parsing, validation,
 * content fetching, and metadata extraction.
 */
object URLUtilityKT {
	
	/**
	 * Extracts the host URL (scheme + host) from a given URL string.
	 * @param urlString The complete URL string
	 * @return Base host URL (e.g., "https://example.com")
	 */
	@JvmStatic
	fun extractHostUrl(urlString: String): String {
		val encodedUrl = URLEncoder.encode(urlString, UTF_8.toString())
		val uri = URI(encodedUrl)
		return "${uri.scheme}://${uri.host}"
	}
	
	/**
	 * Checks if a URL contains only the host without any path.
	 * @param url The URL to check
	 * @return true if URL has no path or only root path, false otherwise
	 */
	@JvmStatic
	fun isHostOnly(url: String): Boolean {
		return try {
			val parsedUrl = URL(url)
			val path = parsedUrl.path
			path.isNullOrEmpty() || path == "/"
		} catch (error: Exception) {
			error.printStackTrace()
			false
		}
	}
	
	/**
	 * Asynchronously fetches and parses the HTML title from a webpage.
	 * @param url The webpage URL
	 * @param callback Callback that receives the title (or null if unavailable)
	 */
	@JvmStatic
	fun getTitleByParsingHTML(url: String, callback: (String?) -> Unit) {
		try {
			val client = OkHttpClient()
			val request = Request.Builder().url(url).build()
			
			client.newCall(request).enqueue(object : Callback {
				override fun onFailure(call: Call, e: IOException) {
					callback(null)
				}
				
				override fun onResponse(call: Call, response: Response) {
					response.use {
						if (!response.isSuccessful) {
							callback(null); return
						}
						val html = response.body.string()
						if (html.isEmpty()) {
							callback(null); return
						}
						val document = Jsoup.parse(html)
						val title = document.title().ifEmpty { null }
						callback(title)
					}
				}
			})
		} catch (error: Exception) {
			error.printStackTrace()
			callback(null)
		}
	}
	
	/**
	 * Fetches either the OpenGraph title or description from a webpage.
	 * @param websiteUrl The webpage URL
	 * @param returnDescriptionL If true returns description, otherwise title
	 * @param userGivenHtmlBody Optional pre-fetched HTML content
	 * @param callback Callback that receives the result (or null if unavailable)
	 */
	@JvmStatic
	fun getWebpageTitleOrDescription(
		websiteUrl: String,
		returnDescriptionL: Boolean = false,
		userGivenHtmlBody: String? = null,
		callback: (String?) -> Unit
	) {
		try {
			val htmlBody = if (userGivenHtmlBody.isNullOrEmpty()) {
				fetchWebPageContent(
					url = websiteUrl,
					retry = true,
					numOfRetry = 6
				) ?: return callback(null)
			} else userGivenHtmlBody
			val document = Jsoup.parse(htmlBody)
			val metaTag = document.selectFirst(
				if (returnDescriptionL) "meta[property=og:description]"
				else "meta[property=og:title]"
			)
			return callback(metaTag?.attr("content"))
		} catch (error: Exception) {
			error.printStackTrace()
			callback(null)
		}
	}
	
	/**
	 * Finds the favicon URL for a website.
	 * @param websiteUrl The website URL
	 * @return Favicon URL if found, null otherwise
	 */
	@JvmStatic
	fun getFaviconUrl(websiteUrl: String): String? {
		val standardFaviconUrl = "$websiteUrl/favicon.ico"
		if (isFaviconAvailable(standardFaviconUrl)) return standardFaviconUrl
		
		return try {
			val doc: Document = Jsoup.connect(websiteUrl).get()
			val faviconUrl = doc.head().select("link[rel~=(icon|shortcut icon)]")
				.mapNotNull { it.attr("href").takeIf { href -> href.isNotEmpty() } }
				.map { href -> if (href.startsWith("http")) href else "$websiteUrl/$href" }
				.firstOrNull { isFaviconAvailable(it) }
			faviconUrl
		} catch (error: Exception) {
			error.printStackTrace()
			null
		}
	}
	
	/**
	 * Checks if a favicon URL is accessible.
	 * @param faviconUrl The favicon URL to check
	 * @return true if favicon is accessible, false otherwise
	 */
	@JvmStatic
	fun isFaviconAvailable(faviconUrl: String): Boolean {
		return try {
			val url = URL(faviconUrl)
			val connection = url.openConnection() as HttpURLConnection
			connection.requestMethod = "HEAD"
			val isAvailable = connection.responseCode == HTTP_OK
			isAvailable
		} catch (error: Exception) {
			error.printStackTrace()
			false
		}
	}
	
	/**
	 * Fetches the file size from a URL using OkHttp.
	 * @param httpClient Configured OkHttpClient instance
	 * @param url The file URL
	 * @return File size in bytes, or -1 if unavailable
	 */
	@JvmStatic
	fun fetchFileSize(httpClient: OkHttpClient, url: String): Long {
		return try {
			val request = Request.Builder().url(url).head().build()
			httpClient.newCall(request).execute().use { response ->
				val fileSize = response.header("Content-Length")?.toLong() ?: -1L
				fileSize
			}
		} catch (error: Exception) {
			error.printStackTrace()
			-1L
		}
	}
	
	/**
	 * Checks internet connectivity by attempting to reach google.com.
	 * @return true if connection succeeds, false otherwise
	 */
	@JvmStatic
	fun isInternetConnected(): Boolean {
		return try {
			val url = URL("https://www.google.com")
			with(url.openConnection() as HttpURLConnection) {
				requestMethod = "GET"
				connectTimeout = 1000
				readTimeout = 1000
				connect()
				val isConnected = responseCode == HTTP_OK
				isConnected
			}
		} catch (error: Exception) {
			error.printStackTrace()
			false
		}
	}
	
	/**
	 * Converts a string to URL-safe format by encoding spaces.
	 * @param input The input string
	 * @return URL-encoded string
	 */
	@JvmStatic
	fun getUrlSafeString(input: String): String {
		return input.replace(" ", "%20")
	}
	
	/**
	 * Extracts the base domain from a URL.
	 * @param url The complete URL
	 * @return Base domain (e.g., "google" from "www.google.com")
	 */
	@JvmStatic
	fun getBaseDomain(url: String): String? {
		return try {
			val domain = URL(url).host
			val parts = domain.split(".")
			val baseDomain = if (parts.size > 2) {
				parts[parts.size - 2]
			} else {
				parts[0]
			}
			
			baseDomain
		} catch (error: Exception) {
			error.printStackTrace()
			null
		}
	}
	
	/**
	 * Extracts the host from a URL string.
	 * @param urlString The URL string
	 * @return Host name or null if invalid URL
	 */
	@JvmStatic
	fun getHostFromUrl(urlString: String?): String? {
		return try {
			urlString?.let { URL(it).host }
		} catch (error: Exception) {
			error.printStackTrace()
			null
		}
	}
	
	/**
	 * Generates a Google favicon URL for a domain.
	 * @param domain The target domain
	 * @return Google favicon service URL
	 */
	@JvmStatic
	fun getGoogleFaviconUrl(domain: String): String {
		return "https://www.google.com/s2/favicons?domain=$domain&sz=128"
	}
	
	/**
	 * Checks if a URL is expired (returns 4xx/5xx status).
	 * @param urlString The URL to check
	 * @return true if URL returns error status, false otherwise
	 */
	@JvmStatic
	fun isUrlExpired(urlString: String): Boolean {
		return try {
			val url = URL(urlString)
			val connection = url.openConnection() as HttpURLConnection
			connection.requestMethod = "HEAD"
			connection.connectTimeout = 5000
			connection.readTimeout = 5000
			connection.connect()
			val responseCode = connection.responseCode
			val isExpired = responseCode >= 400
			isExpired
		} catch (error: Exception) {
			error.printStackTrace()
			true
		}
	}
	
	/**
	 * Removes 'www.' prefix from a URL if present.
	 * @param url The URL to process
	 * @return URL without 'www.' prefix
	 */
	@JvmStatic
	fun removeWwwFromUrl(url: String?): String {
		if (url == null) return ""
		return try {
			url.replaceFirst("www.", "")
		} catch (error: Exception) {
			error.printStackTrace()
			url
		}
	}
	
	/**
	 * Fetches Facebook webpage content with desktop user agents and retry logic.
	 * @param url Facebook URL
	 * @param retry Whether to enable retry on failure
	 * @param numOfRetry Number of retry attempts
	 * @param timeoutSeconds Connection timeout in seconds
	 * @return HTML content or null if failed
	 */
	@JvmStatic
	fun fetchFBWebPageContent(
		url: String,
		retry: Boolean = false,
		numOfRetry: Int = 0,
		timeoutSeconds: Int = 30
	): String? {
		val desktopUserAgents = listOf(
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
					"AppleWebKit/537.36 (KHTML, like Gecko) " +
					"Chrome/91.0.4472.124 Safari/537.36",
			"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
					"AppleWebKit/537.36 (KHTML, like Gecko) " +
					"Chrome/91.0.4472.124 Safari/537.36",
			"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) " +
					"Gecko/20100101 Firefox/89.0"
		)
		
		fun fetch(attempt: Int = 0): String? {
			val client = OkHttpClient.Builder()
				.connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
				.readTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
				.writeTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
				.build()
			
			val request = Request.Builder()
				.url(url)
				.header("User-Agent", desktopUserAgents[attempt % desktopUserAgents.size])
				.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
				.header("Accept-Language", "en-US,en;q=0.5")
				.build()
			
			return try {
				client.newCall(request).execute().use { response ->
					if (response.isSuccessful)
                        response.body.string().takeIf { it.isNotEmpty() }
					else null
				}
			} catch (error: IOException) {
				error.printStackTrace(); null
			} catch (error: Exception) {
				error.printStackTrace(); null
			}
		}
		
		if (retry && numOfRetry > 0) {
			var attempt = 0
			while (attempt < numOfRetry) {
				fetch(attempt)?.let { return it }
				attempt++
				if (attempt < numOfRetry) Thread.sleep(1000L * attempt)
			}; return null
		}
		
		return fetch()
	}
	
	/**
	 * Fetches webpage content with retry logic.
	 * @param url The webpage URL
	 * @param retry Whether to enable retry on failure
	 * @param numOfRetry Number of retry attempts
	 * @return HTML content or null if failed
	 */
	@JvmStatic
	fun fetchWebPageContent(
		url: String,
		retry: Boolean = false,
		numOfRetry: Int = 0
	): String? {
		if (SupportedURLs.isFacebookUrl(url)) {
			return fetchFBWebPageContent(url, retry, numOfRetry)
		}
		
		fun fetch(): String? {
			val client = OkHttpClient()
			val request = Request.Builder().url(url).build()
			return try {
				client.newCall(request).execute().use { response ->
					if (response.isSuccessful) response.body.string() else null
				}
			} catch (error: IOException) {
				error.printStackTrace()
				null
			}
		}
		
		if (retry && numOfRetry > 0) {
			var index = 0
			var htmlBody: String? = ""
			while (index < numOfRetry || htmlBody.isNullOrEmpty()) {
				htmlBody = fetch()
				if (!htmlBody.isNullOrEmpty()) return htmlBody
				index++
			}
		}
		
		return fetch()
	}
	
	/**
	 * Normalizes a URL by decoding and re-encoding components consistently.
	 * @param url The URL to normalize
	 * @return Normalized URL or original if error occurs
	 */
	@JvmStatic
	fun normalizeEncodedUrl(url: String): String {
		try {
			val unescapedUrl = url.replace("\\/", "/")
			val uri = URI(unescapedUrl)
			val baseUrl = "${uri.scheme}://${uri.host}${uri.path}"
			val query = uri.query ?: return baseUrl
			
			val queryParams = query.split("&").associate {
				it.split("=").let { pair ->
					val key = URLDecoder.decode(pair[0], "UTF-8")
					val value = if (pair.size > 1) URLDecoder.decode(pair[1], "UTF-8") else ""
					key to value
				}
			}.toSortedMap()
			
			val normalizedQuery = queryParams.map { (key, value) ->
				"${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
			}.joinToString("&")
			
			return "$baseUrl?$normalizedQuery"
		} catch (error: Exception) {
			error.printStackTrace()
			return url
		}
	}
}