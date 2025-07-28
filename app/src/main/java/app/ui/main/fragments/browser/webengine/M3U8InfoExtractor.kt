package app.ui.main.fragments.browser.webengine

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceInputStream
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import com.aio.R
import lib.process.ThreadsUtility
import lib.texts.CommonTextUtils.getText
import java.util.regex.Pattern

/**
 * Extracts video resolutions from HLS (M3U8) streams.
 *
 * This class handles both master playlists (containing multiple resolutions) and
 * media playlists (single resolution) by examining:
 * 1. Playlist metadata
 * 2. URL patterns
 * 3. Stream variants
 */
class M3U8InfoExtractor {
	
	/**
	 * Initiates resolution extraction in a background thread.
	 *
	 * @param m3u8Url The URL of the HLS playlist
	 * @param callback Receiver for results or errors
	 */
	fun extractResolutions(m3u8Url: String, callback: InfoCallback) {
		ThreadsUtility.executeInBackground(codeBlock = {
			try {
				val resolutions = fetchResolutionsFromM3U8(m3u8Url)
				if (resolutions.isNotEmpty()) {
					ThreadsUtility.executeOnMain {
						callback.onResolutions(resolutions)
					}
				}
			} catch (error: Exception) {
				error.printStackTrace()
				ThreadsUtility.executeOnMain {
					callback.onError("Resolution extraction failed.")
				}
			}
		})
	}
	
	/**
	 * Fetches and parses the M3U8 playlist to determine available resolutions.
	 *
	 * @param m3u8Url The URL of the HLS playlist
	 * @return List of resolution strings (e.g., ["1920×1080", "1280×720"])
	 */
	@OptIn(UnstableApi::class)
	private fun fetchResolutionsFromM3U8(m3u8Url: String): List<String> {
		// Configure HTTP data source with user agent
		val factory = DefaultHttpDataSource.Factory()
		val userAgent = getText(R.string.text_mobile_user_agent)
		val dataSourceFactory = factory.setUserAgent(userAgent)
		val playlistParser = HlsPlaylistParser()
		
		try {
			val dataSource = dataSourceFactory.createDataSource()
			val dataSpec = DataSpec(m3u8Url.toUri())
			dataSource.open(dataSpec)
			
			DataSourceInputStream(dataSource, dataSpec).use { stream ->
				return when (val playlist = playlistParser.parse(dataSpec.uri, stream)) {
					// Case 1: Master playlist with multiple variants
					is HlsMultivariantPlaylist -> {
						playlist.variants.mapNotNull { variant ->
							when {
								variant.format.width > 0 && variant.format.height > 0 ->
									"${variant.format.width}×${variant.format.height}"
								variant.format.height > 0 ->
									"${variant.format.height}p"
								else -> null
							}
						}.distinct()
					}
					
					// Case 2: Media playlist (single stream)
					is HlsMediaPlaylist -> {
						extractResolutionFromUrl(m3u8Url)?.let { listOf(it) }
							?: listOf(getText(R.string.title_player_default))
					}
					
					else -> emptyList()
				}
			}
		} catch (error: Exception) {
			error.printStackTrace()
			return emptyList()
		}
	}
	
	/**
	 * Extracts resolution information from the URL pattern.
	 *
	 * Example supports three pattern types:
	 * 1. Direct resolution in path (e.g., "480p.av1.mp4.m3u8")
	 * 2. Multi-resolution declarations (e.g., "multi=256x144:144p:,...")
	 * 3. Traditional patterns (e.g., "/1280x720/", "_720p")
	 *
	 * @param url The M3U8 URL to analyze
	 * @return Extracted resolution string or null if no match found
	 */
	private fun extractResolutionFromUrl(url: String): String? {
		// Pattern 1: Direct resolution in filename (e.g., "480p.av1.mp4.m3u8")
		"/(\\d{3,4})[pP]\\.".toRegex().find(url)?.let { match ->
			return "${match.groupValues[1]}p"
		}
		
		// Pattern 2: Multi-resolution declaration (e.g., "multi=256x144:144p:,...")
		"multi=(.*?):(.*?):/".toRegex().find(url)?.let { match ->
			return match.groupValues[1]
				.split(",")
				.last()
				.substringBefore(":")
				.let { if (it.contains("x")) it else "${it}p" }
		}
		
		// Pattern 3: Direct resolution in filename (e.g., "440x250.mp4.m3u8")
		"(\\d{3,4})[xX×](\\d{3,4})\\.mp4\\.m3u8".toRegex().find(url)?.let { match ->
			return "${match.groupValues[1]}×${match.groupValues[2]}"
		}
		
		// Pattern 4: Multi-resolution declaration (new format)
		"multi=(\\d+x\\d+):(\\d+x\\d+)/".toRegex().find(url)?.let { match ->
			// Return the highest resolution (last one)
			return match.groupValues.last().replace("x", "×")
		}
		
		// Pattern 5: Resolution in path (e.g., "/720p/stream.m3u8")
		"/(\\d{3,4})[pP]/".toRegex().find(url)?.let { match ->
			return "${match.groupValues[1]}p"
		}
		
		// Pattern 6. Quality suffix filename (e.g., "stream_720p.m3u8")
		"[_\\-](\\d{3,4})[pP]\\.m3u8".toRegex().find(url)?.let {
			return "${it.groupValues[1]}p"
		}
		
		// Pattern 7. Compact multi-resolution (e.g., "multi=444x250:440x250/")
		"multi=([^:]+)".toRegex().find(url)?.let {
			return it.groupValues[1].split(":").last().replace("x", "×")
		}
		
		// Pattern 8. Dimensions in sub-path (e.g., "/1280x720/stream.m3u8")
		"/(\\d{3,4})[xX×](\\d{3,4})/".toRegex().find(url)?.let {
			return "${it.groupValues[1]}×${it.groupValues[2]}"
		}
		
		// Pattern 9. Bitrate-quality suffix (e.g., "stream_3000k_1080p.m3u8")
		"[_\\-]\\d+k[_\\-](\\d{3,4})[pP]\\.m3u8".toRegex().find(url)?.let {
			return "${it.groupValues[1]}p"
		}
		
		// Pattern 10. CDN-style format (e.g., "stream_1080p_h264.m3u8")
		"[_\\-](\\d{3,4})[pP]_[a-z0-9]+\\.m3u8".toRegex().find(url)?.let {
			return "${it.groupValues[1]}p"
		}
		
		// Pattern 11. Quality prefix (e.g., "hls_720p_stream.m3u8")
		"[_\\-](\\d{3,4})[pP][_\\-]".toRegex().find(url)?.let {
			return "${it.groupValues[1]}p"
		}
		
		// Pattern 12: Traditional resolution patterns
		val patterns = listOf(
			"/(\\d{3,4})[xX×](\\d{3,4})/",  // e.g., /1280x720/
			"[_\\-](\\d{3,4})[pP]",          // e.g., _720p
			"[_\\-](\\d{3,4})[xX×](\\d{3,4})" // e.g., -1280x720
		)
		
		// Pattern 13: for PHNCDN-style URLs (e.g., "720P_4000K_465426665.mp4")
		"/(\\d{3,4})[pP]_(\\d+)K_\\d+\\.mp4/".toRegex().find(url)?.let {
			return "${it.groupValues[1]}p"  // Returns "720p" for the example
		}
		
		patterns.forEach { pattern ->
			Pattern.compile(pattern).matcher(url).let { matcher ->
				if (matcher.find()) {
					return when (matcher.groupCount()) {
						1 -> "${matcher.group(1)}p"
						2 -> "${matcher.group(1)}×${matcher.group(2)}"
						else -> null
					}
				}
			}
		}
		
		return null
	}
	
	/**
	 * Callback interface for resolution extraction results.
	 */
	interface InfoCallback {
		/**
		 * Called when resolutions are successfully extracted.
		 * @param resolutions List of detected resolutions (e.g., ["1080p", "720p"])
		 */
		fun onResolutions(resolutions: List<String>)
		
		/**
		 * Called when extraction fails.
		 * @param errorMessage Description of the failure
		 */
		fun onError(errorMessage: String)
	}
}