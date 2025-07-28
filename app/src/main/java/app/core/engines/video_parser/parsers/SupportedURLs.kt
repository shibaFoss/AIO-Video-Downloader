package app.core.engines.video_parser.parsers

import androidx.core.net.toUri
import lib.networks.URLUtilityKT.getBaseDomain
import java.net.URL

/**
 * Utility object for identifying and filtering supported video URLs for parsing.
 *
 * This object includes functions to detect if a URL belongs to a major media platform,
 * to normalize certain URLs (like YouTube links), and to determine whether a URL
 * is supported by tools like yt-dlp.
 */
object SupportedURLs {
	
	/**
	 * A set of base domain keywords that are supported for video parsing using yt-dlp or custom logic.
	 */
	private val supportedBaseDomains = setOf(
		"youtube", "youtu", "facebook", "instagram", "twitter", "x", "tiktok", "reddit", "tumblr",
		"soundcloud", "bandcamp", "9gag", "vk", "imdb", "dailymotion", "bilibili", "twitch",
		"likee", "snapchat", "pinterest", "linkedin", "mixcloud", "audiomack", "periscope",
		"youku", "rumble", "odysee", "peertube", "bitchute", "liveleak"
	)
	
	/**
	 * Normalizes YouTube URLs by removing playlist parameters and ensuring a consistent format.
	 *
	 * For example, short links (youtu.be) are expanded to full URLs.
	 *
	 * @param url The input YouTube URL.
	 * @return A normalized YouTube watch URL, or the original URL if not YouTube or parsing fails.
	 */
	fun filterYoutubeUrlWithoutPlaylist(url: String): String {
		return try {
			if (!isYouTubeUrl(url)) return url
			
			val uri = url.toUri()
			val host = uri.host ?: return url
			
			when {
				host.contains("youtu.be") -> {
					val videoId = uri.lastPathSegment ?: return url
					"https://www.youtube.com/watch?v=$videoId"
				}
				
				host.contains("youtube.com") -> {
					val videoId = uri.getQueryParameter("v") ?: return url
					"https://www.youtube.com/watch?v=$videoId"
				}
				
				else -> url
			}
		} catch (error: Exception) {
			error.printStackTrace()
			url
		}
	}
	
	/**
	 * Checks whether the given URL is a valid YouTube URL.
	 *
	 * @param url The URL to validate.
	 * @return True if the URL belongs to YouTube; false otherwise.
	 */
	fun isYouTubeUrl(url: String): Boolean {
		return try {
			val parsedUrl = URL(url)
			val host = parsedUrl.host
			host.endsWith("youtube.com") || host.endsWith("youtu.be")
		} catch (error: Exception) {
			error.printStackTrace()
			false
		}
	}
	
	/**
	 * Checks whether the given URL is from Instagram.
	 */
	fun isInstagramUrl(url: String): Boolean {
		return try {
			val host = URL(url).host
			host.contains("instagram.com", ignoreCase = true)
		} catch (error: Exception) {
			error.printStackTrace()
			false
		}
	}
	
	/**
	 * Checks whether the given URL is from Facebook.
	 */
	fun isFacebookUrl(url: String): Boolean {
		return try {
			val host = URL(url).host
			host.contains("facebook.com", ignoreCase = true)
		} catch (error: Exception) {
			error.printStackTrace()
			false
		}
	}
	
	/**
	 * Checks whether the given URL is from TikTok.
	 */
	fun isTiktokUrl(url: String): Boolean {
		return try {
			val host = URL(url).host
			host.contains("tiktok.com", ignoreCase = true)
		} catch (error: Exception) {
			error.printStackTrace()
			false
		}
	}
	
	/**
	 * Checks whether the given URL is from a major social media platform
	 * (Facebook, Instagram, TikTok).
	 *
	 * @param url The URL to test.
	 * @return True if the URL belongs to a supported social media site.
	 */
	fun isSocialMediaUrl(url: String): Boolean {
		return isInstagramUrl(url) || isFacebookUrl(url) || isTiktokUrl(url)
	}
	
	/**
	 * Checks whether the given URL is supported by yt-dlp or by internal logic
	 * based on its base domain or if it's an HLS stream (.m3u8).
	 *
	 * @param url The URL to check.
	 * @return True if the domain or stream format is supported.
	 */
	fun isYtdlpSupportedUrl(url: String): Boolean {
		val baseDomain = getBaseDomain(url)
		val isSupportedUrl = supportedBaseDomains.contains(baseDomain) || isM3U8Url(url)
		return baseDomain != null && isSupportedUrl
	}
	
	/**
	 * Checks whether the URL points to an M3U8 (HLS) playlist, which typically indicates
	 * a streamable video.
	 *
	 * @param url The URL to test.
	 * @return True if the URL is a known m3u8 path.
	 */
	fun isM3U8Url(url: String): Boolean {
		return url.contains("/playlist.m3u8", ignoreCase = true) ||
				url.contains("/index.m3u8", ignoreCase = true) ||
				url.contains(".m3u8", ignoreCase = true) ||
				url.contains("m3u8", ignoreCase = true)
	}
}
