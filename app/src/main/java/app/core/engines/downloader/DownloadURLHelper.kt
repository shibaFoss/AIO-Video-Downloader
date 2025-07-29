package app.core.engines.downloader

import com.aio.R
import lib.networks.URLUtilityKT.extractHostUrl
import lib.process.LogHelperUtils
import lib.texts.CommonTextUtils.getText
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URL
import java.net.URLEncoder.encode
import java.security.MessageDigest

/**
 * Utility class for handling download-related URL operations.
 *
 * Provides functionality to:
 * - Retrieve file information from URLs (size, name, support for resuming)
 * - Handle HTTP requests with proper headers and cookies
 * - Calculate file checksums
 * - Manage redirects and browser-specific downloads
 *
 * Uses OkHttp for network operations with custom cookie handling and redirect policies.
 */
object DownloadURLHelper {
	
	// Logger instance for error tracking
	private val logger = LogHelperUtils.from(javaClass)
	
	// HTTP header constants
	private const val CONTENT_LENGTH = "Content-Length"
	private const val ACCEPT_RANGE = "Accept-Ranges"
	private const val BYTES = "bytes"
	private const val E_TAG = "ETag"
	private const val LAST_MODIFIED = "Last-Modified"
	private const val CONTENT_DISPOSITION = "Content-Disposition"
	private const val FILE_NAME = "filename="
	private const val SHA_256 = "SHA-256"
	private const val USER_AGENT = "User-Agent"
	private const val HOST = "Host"
	private const val REFERER = "Referer"
	private const val RANGE = "Range"
	
	/**
	 * Data class representing file information retrieved from a URL.
	 *
	 * @property isFileForbidden Whether access to the file is restricted
	 * @property errorMessage Error description if retrieval failed
	 * @property fileName Name of the file from server headers or URL
	 * @property fileSize Size in bytes (-1 if unknown)
	 * @property fileChecksum SHA-256 hash of file contents
	 * @property isSupportsMultipart Whether server supports range requests
	 * @property isSupportsResume Whether download can be resumed
	 */
	data class URLFileInfo(
		var isFileForbidden: Boolean = false,
		var errorMessage: String = "",
		var fileName: String = "",
		var fileSize: Long = 0L,
		var fileChecksum: String = "",
		var isSupportsMultipart: Boolean = false,
		var isSupportsResume: Boolean = false,
	)
	
	/**
	 * Retrieves file information from a URL.
	 *
	 * @param url The URL to inspect
	 * @param downloadModel Optional download model for context (user agent, referrer)
	 * @return URLFileInfo containing all retrieved metadata
	 *
	 * This method:
	 * 1. Creates a custom HTTP client with cookie support
	 * 2. Sends a HEAD request (or GET for browser downloads)
	 * 3. Parses response headers for file metadata
	 * 4. Handles redirects and authentication
	 * 5. Returns comprehensive file information
	 */
	fun getFileInfoFromSever(url: URL, downloadModel: DownloadDataModel? = null): URLFileInfo {
		val fileInfo = URLFileInfo()
		try {
			// Custom cookie jar for session persistence
			val cookieJar = object : CookieJar {
				private val cookies = mutableMapOf<String, List<Cookie>>()
				override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
					this.cookies[url.host] = cookies
				}
				
				override fun loadForRequest(url: HttpUrl): List<Cookie> {
					return cookies[url.host] ?: emptyList()
				}
			}
			
			// Configure HTTP client with redirect and cookie support
			val client: OkHttpClient = OkHttpClient.Builder()
				.cookieJar(cookieJar)
				.followRedirects(true)
				.followSslRedirects(true)
				.addInterceptor { chain ->
					val request = chain.request()
					val response = chain.proceed(request)
					
					// Check if we got redirected
					if (response.isRedirect) {
						val location = response.header("Location")
						logger.d("Redirected to: $location")
					}
					response
				}
				.build()
			
			// Create the HTTP request builder based on whether the download is from the browser
			val browserDownloadRequest = createRequestBuilderFromUrl(
				url.toString(),
				downloadModel?.globalSettings?.downloadHttpUserAgent,
				downloadModel?.siteReferrer
			).build()
			
			val normalRequest: Request = Request.Builder().url(url).head().build()
			val request = if (downloadModel != null && downloadModel.isDownloadFromBrowser)
				browserDownloadRequest else normalRequest
			
			// Make the HTTP request and process the response
			client.newCall(request).execute().use { response ->
				if (response.isSuccessful) {
					// Extract file size
					val contentLength = response.header(CONTENT_LENGTH)
					fileInfo.fileSize = contentLength?.toLong() ?: -1
					
					// Check for multipart download support
					val acceptRanges = response.header(ACCEPT_RANGE)
					if (acceptRanges == BYTES) fileInfo.isSupportsMultipart = true
					
					// Check if the file can be resumed (ETag, Last-Modified headers)
					val eTag = response.header(E_TAG)
					val lastModified = response.header(LAST_MODIFIED)
					if (fileInfo.isSupportsMultipart || eTag != null || lastModified != null)
						fileInfo.isSupportsResume = true
					
					// Extract the file name
					var fileName: String? = null
					val contentDisposition = response.header(CONTENT_DISPOSITION)
					if (contentDisposition != null && contentDisposition.contains(FILE_NAME)) {
						fileName = contentDisposition.split(FILE_NAME.toRegex())
							.dropLastWhile { it.isEmpty() }
							.toTypedArray()[1].replace("\"", "").trim()
					}
					
					if (fileName == null) {
						val path = url.path
						fileName = path.substring(path.lastIndexOf('/') + 1)
					}
					
					fileInfo.fileName = fileName
					if (fileInfo.fileName.isEmpty()) {
						fileInfo.fileName = getText(R.string.title_unknown)
					}
				} else {
					// Handle unsuccessful response
					fileInfo.isFileForbidden = true
					fileInfo.fileSize = -1
					fileInfo.errorMessage = "Failed to fetch file details: " +
							"${response.message} (HTTP ${response.code})"
				}
			}
		} catch (error: Exception) {
			// Catch any exceptions and update fileInfo with error details
			fileInfo.isFileForbidden = true
			fileInfo.fileSize = -1
			fileInfo.errorMessage = "Error fetching file details:" +
					" ${error.message ?: "Unknown error"}"
			logger.e(error)
		}
		
		return fileInfo
	}
	
	/**
	 * Creates a configured HTTP request builder for URL requests.
	 *
	 * @param urlString URL to request
	 * @param userAgent Optional custom user agent
	 * @param siteReferer Optional referrer URL
	 * @param byteRange Optional byte range for partial requests
	 * @return Configured Request.Builder instance
	 *
	 * Handles:
	 * - Host header injection
	 * - User agent customization
	 * - Referrer tracking
	 * - Special AWS S3 content disposition
	 */
	private fun createRequestBuilderFromUrl(
		urlString: String, userAgent: String? = null,
		siteReferer: String? = null, byteRange: String? = null
	): Request.Builder {
		val uri = URI(urlString)
		val host = uri.host
		val query = uri.query
		
		val requestBuilder = Request.Builder().url(urlString)
		requestBuilder.addHeader(HOST, host)
		
		if (!userAgent.isNullOrEmpty()) requestBuilder.addHeader(USER_AGENT, userAgent)
		if (!siteReferer.isNullOrEmpty()) requestBuilder.addHeader(REFERER, extractHostUrl(siteReferer))
		if (!byteRange.isNullOrEmpty()) requestBuilder.addHeader(RANGE, byteRange)
		val responseContentDisposition = query?.let { it ->
			it.split("&").find { it.contains("response-content-disposition") }
		}
		
		responseContentDisposition?.let {
			val decodedDisposition = it.split("=")[1]
			requestBuilder.addHeader(
				CONTENT_DISPOSITION,
				"attachment; filename=${encode(decodedDisposition, "UTF-8")}"
			)
		}
		
		return requestBuilder
	}
	
	/**
	 * Calculates a file checksum via HTTP GET request.
	 *
	 * @param client Configured OkHttpClient instance
	 * @param url URL of file to checksum
	 * @param algorithm Hashing algorithm (default SHA-256)
	 * @return Hex string of checksum or null if failed
	 */
	private fun calculateChecksum(
		client: OkHttpClient, url: URL,
		algorithm: String = SHA_256
	): String? {
		try {
			val request = Request.Builder().url(url).get().build()
			client.newCall(request).execute().use { response ->
				if (response.isSuccessful) {
					val digest = MessageDigest.getInstance(algorithm)
					response.body.byteStream().use { inputStream ->
						val buffer = ByteArray(1024)
						var bytesRead: Int
						while (inputStream.read(buffer).also { bytesRead = it } != -1) {
							digest.update(buffer, 0, bytesRead)
						}
					}
					return digest.digest().joinToString("") { "%02x".format(it) }
				}
			}
		} catch (error: Exception) {
			logger.e(error)
		}
		return null
	}
}
