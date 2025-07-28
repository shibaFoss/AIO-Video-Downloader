package app.ui.main.fragments.browser.webengine

/**
 * Represents information about a video URL detected within a web page.
 *
 * This data class holds essential metadata related to a specific video stream or file
 * that can be downloaded or played. It is typically used in conjunction with video
 * detection logic in the browser engine to provide details like file type, resolution,
 * and caching status.
 *
 * @property fileUrl The actual URL of the video file. This is used for playback or download.
 * @property isM3U8 A boolean flag indicating whether the video is an M3U8 (HLS stream) format.
 * @property totalResolutions The number of available resolutions for the video, useful for M3U8 playlists.
 * @property fileResolution A string representing the resolution of this particular video file (e.g., "720p", "1080p").
 * @property infoCached An optional string to cache additional metadata or state related to the video (e.g., pre-parsed info).
 */
data class VideoUrlInfo(
	var fileUrl: String,
	var isM3U8: Boolean,
	var totalResolutions: Int,
	var fileResolution: String,
	var infoCached: String = ""
)
