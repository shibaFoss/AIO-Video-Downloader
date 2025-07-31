package app.ui.main.fragments.downloads.intercepter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import app.core.AIOApp.Companion.IS_ULTIMATE_VERSION_UNLOCKED
import app.core.bases.BaseActivity
import app.core.engines.video_parser.parsers.SupportedURLs.isYouTubeUrl
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoFormat
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoInfo
import app.core.engines.video_parser.parsers.VideoFormatsUtils.cleanFileSize
import com.aio.R
import lib.texts.CommonTextUtils.capitalizeWords
import lib.texts.CommonTextUtils.fromHtmlStringToSpanned
import lib.texts.CommonTextUtils.getText
import java.lang.ref.WeakReference

/**
 * Adapter to display a list of [VideoFormat]s in a dialog for selecting resolution and format.
 *
 * @param baseActivity Reference to the activity context.
 * @param videoInfo Contains the video metadata and original URL.
 * @param videoFormats List of available video formats to choose from.
 * @param onVideoFormatClick Callback invoked when a format is selected.
 */
open class VideoFormatAdapter(
	private val baseActivity: BaseActivity?,
	private val videoInfo: VideoInfo,
	private val videoFormats: List<VideoFormat>,
	private val onVideoFormatClick: () -> Unit
) : BaseAdapter() {
	
	private val safeBaseActivityRef = WeakReference(baseActivity).get()
	
	/** Currently selected format position */
	open var selectedPosition: Int = -1
	
	override fun getCount(): Int = videoFormats.size
	
	override fun getItem(position: Int): VideoFormat = videoFormats[position]
	
	override fun getItemId(position: Int): Long = position.toLong()
	
	/**
	 * Returns the currently selected video format, or null if none is selected.
	 */
	fun getSelectedFormat(): VideoFormat? {
		if (selectedPosition == -1) return null
		return getItem(selectedPosition)
	}
	
	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
		val view: View = convertView ?: LayoutInflater.from(safeBaseActivityRef)
			.inflate(R.layout.dialog_video_res_picker_1_item, parent, false)
		
		safeBaseActivityRef?.let { safeBaseActivityRef ->
			val mainLayout = view.findViewById<View>(R.id.main_layout)
			val resolutionTextView: TextView = view.findViewById(R.id.txt_resolution)
			val fileSizeTextView: TextView = view.findViewById(R.id.txt_file_size)
			
			val videoFormat = getItem(position)
			
			// Format file size for better readability
			val cleanedFileSize = cleanFileSize(videoFormat.formatFileSize)
			val fileSizeSpanned = StringBuilder()
				.append("<html><body>")
				.append("<b>")
				.append(cleanedFileSize.ifEmpty { videoFormat.formatFileSize })
				.append("</b>")
				.append("</body></html>").toString()
			
			var resolution = videoFormat.formatResolution
			if (resolution.lowercase().contains("audio only")) {
				resolution = capitalizeWords(resolution) ?: resolution
			}
			
			resolutionTextView.text = extractHeightFromResolution(resolution)
			fileSizeTextView.text = fromHtmlStringToSpanned(fileSizeSpanned)
			
			// Hide unknown file size for YouTube if ultimate version is unlocked
			removeFileInfoOnYT(fileSizeTextView)
			
			// Store tag for potential view reuse
			if (view.tag == null) view.tag = videoFormat
			
			// Highlight selected item
			if (position == selectedPosition) {
				mainLayout.setBackgroundResource(R.drawable.rounded_secondary_color)
				resolutionTextView.setTextColor(safeBaseActivityRef.getColor(R.color.color_on_secondary))
				fileSizeTextView.setTextColor(safeBaseActivityRef.getColor(R.color.color_on_secondary))
			} else {
				mainLayout.setBackgroundResource(R.drawable.rounded_secondary_color_border)
				resolutionTextView.setTextColor(safeBaseActivityRef.getColor(R.color.color_text_primary))
				fileSizeTextView.setTextColor(safeBaseActivityRef.getColor(R.color.color_text_primary))
			}
			
			// Handle click to select format
			view.setOnClickListener {
				selectedPosition = position
				notifyDataSetChanged()
				onVideoFormatClick()
			}
		}
		
		return view
	}
	
	/**
	 * Replaces "Unknown" file size with "N/A" for YouTube videos when ultimate version is unlocked.
	 */
	private fun removeFileInfoOnYT(fileSizeTextView: TextView) {
		if (fileSizeTextView.text.toString() == getText(R.string.title_unknown)
			&& IS_ULTIMATE_VERSION_UNLOCKED && isYouTubeUrl(videoInfo.videoUrl)
		) fileSizeTextView.text = "N/A"
	}
	
	/**
	 * Attempts to extract vertical resolution (height in pixels) from the resolution string.
	 *
	 * @param resolution Raw resolution string like "1280x720", "720p", etc.
	 * @return A cleaned resolution string like "720p" or original string if no match.
	 */
	private fun extractHeightFromResolution(resolution: String): String {
		val patterns = listOf(
			// Common patterns
			Regex("(\\d+)p"),                         // 720p
			Regex("(\\d+)\\s*[xX×]\\s*(\\d+)p?"),    // 1280x720, 1280×720p
			Regex("[^\\d](\\d+)\\s*[pP]"),            // ...720p
			Regex("(\\d+)\\s*[pP][^\\d]"),           // 720p...
			Regex("(\\d+)\\s*[pP]\\s*[^\\d]"),        // 720p ...
			Regex("[^\\d](\\d+)\\s*[iI]"),            // ...720i
			Regex("(\\d+)\\s*[iI][^\\d]"),            // 720i...
			
			// Patterns with px
			Regex("(\\d+)px(\\d+)p?"),                // 1280px720, 1280px720p
			Regex("(\\d+)\\s*px\\s*(\\d+)\\s*[pP]"),  // 1280 px 720 p
			
			// Patterns with other separators
			Regex("(\\d+)\\s*[*]\\s*(\\d+)p?"),       // 1280*720, 1280*720p
			Regex("(\\d+)\\s*[|]\\s*(\\d+)p?"),       // 1280|720, 1280|720p
			
			// UHD/HD patterns
			Regex("(\\d+)\\s*[uU][hH][dD]"),         // 4UHD
			Regex("(\\d+)\\s*[hH][dD]")               // 1080HD
		)
		
		for (pattern in patterns) {
			val match = pattern.find(resolution)
			if (match != null) {
				// Try to get height (usually the last number)
				val groups = match.groupValues
				for (i in groups.size downTo 1) {
					val group = groups[i - 1]
					if (group.isNotEmpty() && group.all { it.isDigit() }) {
						return "${group}p"
					}
				}
			}
		}
		
		return resolution
	}
}
