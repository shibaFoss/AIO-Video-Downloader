package app.ui.main.fragments.browser.webengine

import android.view.View
import android.view.View.GONE
import android.view.View.inflate
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import app.core.engines.video_parser.parsers.SupportedURLs.isM3U8Url
import app.core.engines.video_parser.parsers.VideoFormatsUtils
import app.ui.main.MotherActivity
import app.ui.main.fragments.browser.webengine.M3U8InfoExtractor.InfoCallback
import app.ui.main.fragments.downloads.intercepter.SharedVideoURLIntercept
import com.aio.R
import lib.networks.DownloaderUtils.getVideoResolutionFromUrl
import lib.process.AsyncJobUtils.executeInBackground
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.ThreadsUtility
import lib.texts.ClipboardUtils.copyTextToClipboard
import lib.texts.CommonTextUtils.getText
import lib.ui.ViewUtility.animateFadInOutAnim
import lib.ui.ViewUtility.closeAnyAnimation
import lib.ui.builders.ToastView.Companion.showToast
import java.lang.ref.WeakReference

/**
 * Adapter class to show a list of extracted video links in a dialog.
 * Each item represents a video URL with resolution and metadata information.
 *
 * @param extractedLinksDialog Reference to the dialog showing this adapter.
 * @param webviewEngine WebView engine instance to extract cookies, titles, and context.
 * @param listOfVideoUrlInfos List of extracted video URL information to display.
 */
class ExtractedLinksAdapter(
	private val extractedLinksDialog: ExtractedLinksDialog,
	private val webviewEngine: WebViewEngine,
	private val listOfVideoUrlInfos: ArrayList<VideoUrlInfo>
) : BaseAdapter() {
	
	/** Reference to the safe [MotherActivity] from the WebViewEngine. */
	private val safeMotherActivityRef = WeakReference(webviewEngine.safeMotherActivityRef).get()
	
	override fun getCount(): Int {
		return listOfVideoUrlInfos.size
	}
	
	override fun getItem(position: Int): VideoUrlInfo {
		return listOfVideoUrlInfos[position]
	}
	
	override fun getItemId(position: Int): Long {
		return position.toLong()
	}
	
	/**
	 * Binds view for each video item in the list.
	 * @param position Index of the item.
	 * @param convertView Reusable view if available.
	 * @param parent Parent view group.
	 */
	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
		val extractedVideoLink = listOfVideoUrlInfos[position]
		var itemLayout = convertView
		
		if (itemLayout == null) {
			val layoutResId = R.layout.dialog_extracted_links_item
			itemLayout = inflate(safeMotherActivityRef, layoutResId, null)
		}
		
		if (itemLayout!!.tag == null) {
			ViewHolder(
				extractedLinksDialog = extractedLinksDialog,
				webviewEngine = webviewEngine,
				position = position,
				layoutView = itemLayout,
				safeMotherActivity = safeMotherActivityRef
			).apply {
				updateView(extractedVideoLink)
				itemLayout.tag = this
			}
		} else itemLayout.tag as ViewHolder
		
		return itemLayout
	}
	
	/**
	 * ViewHolder class for caching views and logic per extracted video item.
	 */
	class ViewHolder(
		private val extractedLinksDialog: ExtractedLinksDialog,
		private val webviewEngine: WebViewEngine,
		private val position: Int,
		private val layoutView: View,
		private val safeMotherActivity: MotherActivity?
	) {
		private val m3U8InfoExtractor = M3U8InfoExtractor()
		private var itemClickableContainer: View = layoutView.findViewById(R.id.main_container)
		private var linkItemUrl: TextView = layoutView.findViewById(R.id.txt_video_url)
		private var linkItemInfo: TextView = layoutView.findViewById(R.id.txt_video_info)
		private var textLinkItemInfo: String = ""
		
		/**
		 * Updates the UI with video URL and metadata.
		 * Triggers resolution extraction and sets click listeners.
		 */
		fun updateView(videoUrlInfo: VideoUrlInfo) {
			linkItemUrl.text = videoUrlInfo.fileUrl
			
			if (textLinkItemInfo.isNotEmpty()) {
				linkItemInfo.text = textLinkItemInfo; return
			} else {
				if (isM3U8Url(videoUrlInfo.fileUrl)) showHSLVideoLinkInfo(videoUrlInfo)
				else showNormalVideoLinkInfo(videoUrlInfo)
			}
			
			setupLongClickItemListener(videoUrlInfo.fileUrl)
			setupOnClickItemListener(videoUrlInfo)
		}
		
		/**
		 * Displays metadata for HLS/M3U8 streams.
		 * Handles async resolution extraction and caching.
		 */
		private fun showHSLVideoLinkInfo(videoUrlInfo: VideoUrlInfo) {
			safeMotherActivity?.let { motherActivity ->
				ThreadsUtility.executeInBackground(codeBlock = {
					ThreadsUtility.executeOnMain { animateFadInOutAnim(linkItemInfo) }
					
					if (videoUrlInfo.infoCached.isNotEmpty()) {
						linkItemInfo.text = videoUrlInfo.infoCached
						ThreadsUtility.executeOnMain { closeAnyAnimation(linkItemInfo) }
						return@executeInBackground
					}
					
					m3U8InfoExtractor.extractResolutions(videoUrlInfo.fileUrl, object : InfoCallback {
						override fun onResolutions(resolutions: List<String>) {
							closeAnyAnimation(linkItemInfo)
							if (resolutions.size > 1) {
								val stringResId = R.string.type_video_type_m3u8_available_resolutions
								val infoText = motherActivity.getString(stringResId, "${resolutions.size}")
								linkItemInfo.text = infoText
							} else {
								val stringResId = R.string.text_video_type_m3u8_resolution
								val infoText = motherActivity.getString(stringResId, resolutions[0])
								linkItemInfo.text = infoText
							}
							
							textLinkItemInfo = linkItemInfo.text.toString()
							videoUrlInfo.infoCached = textLinkItemInfo
							videoUrlInfo.fileResolution = resolutions[0]
							videoUrlInfo.totalResolutions = resolutions.size
							videoUrlInfo.isM3U8 = true
						}
						
						override fun onError(errorMessage: String) {
							layoutView.visibility = GONE
						}
					})
				})
			}
		}
		
		/**
		 * Displays metadata for regular (non-HLS) video URLs.
		 * Uses async resolution detection and caches result.
		 */
		private fun showNormalVideoLinkInfo(videoUrlInfo: VideoUrlInfo) {
			safeMotherActivity?.let { safeMotherActivity ->
				executeInBackground {
					executeOnMainThread {
						linkItemInfo.text = getText(R.string.text_fetching_file_info)
						animateFadInOutAnim(linkItemInfo)
					}
					
					if (videoUrlInfo.infoCached.isNotEmpty()) {
						executeOnMainThread {
							linkItemInfo.text = videoUrlInfo.infoCached
							closeAnyAnimation(linkItemInfo)
						}; return@executeInBackground
					}
					
					try {
						getVideoResolutionFromUrl(videoUrlInfo.fileUrl)?.let { resolution ->
							executeOnMainThread {
								closeAnyAnimation(linkItemInfo)
								videoUrlInfo.totalResolutions = 1
								videoUrlInfo.fileResolution = "${resolution.second}p"
								val stringResId = R.string.text_video_type_mp4_resolution
								val infoText = safeMotherActivity.getString(stringResId, videoUrlInfo.fileResolution)
								linkItemInfo.text = infoText
								videoUrlInfo.infoCached = infoText
								videoUrlInfo.isM3U8 = false
							}
						} ?: run {
							closeAnyAnimation(linkItemInfo)
							videoUrlInfo.totalResolutions = 1
							videoUrlInfo.fileResolution = getText(R.string.title_unknown)
							val stringResId = R.string.text_video_type_mp4_resolution
							val infoText = safeMotherActivity.getString(stringResId, videoUrlInfo.fileResolution)
							linkItemInfo.text = infoText
							videoUrlInfo.infoCached = infoText
							videoUrlInfo.isM3U8 = false
						}
						
						textLinkItemInfo = linkItemInfo.text.toString()
						
					} catch (error: Exception) {
						error.printStackTrace()
						executeOnMainThread {
							closeAnyAnimation(linkItemInfo)
							safeMotherActivity.getString(
								R.string.text_video_type_mp4,
								safeMotherActivity.getText(R.string.text_click_to_get_info)
							).let { linkItemInfo.text = it }
							textLinkItemInfo = linkItemInfo.text.toString()
							layoutView.visibility = GONE
						}
					}
				}
			}
		}
		
		/**
		 * Handles click event to prompt download or show further options
		 * depending on whether it's M3U8 or regular video.
		 */
		private fun setupOnClickItemListener(videoUrlInfo: VideoUrlInfo) {
			itemClickableContainer.setOnClickListener {
				if (videoUrlInfo.infoCached.isEmpty()) {
					safeMotherActivity?.doSomeVibration(50)
					showToast(msgId = R.string.text_wait_for_video_info)
					return@setOnClickListener
				}
				
				val videoTitle = webviewEngine.currentWebView?.title
				if (isM3U8Url(videoUrlInfo.fileUrl)) {
					val currentWebUrl = webviewEngine.currentWebView?.url
					val videoCookie = webviewEngine.getCurrentWebViewCookies()
					
					if (videoUrlInfo.totalResolutions > 1) {
						executeOnMainThread {
							SharedVideoURLIntercept(
								baseActivity = safeMotherActivity,
								userGivenVideoInfo = VideoFormatsUtils.VideoInfo(
									videoTitle = videoTitle,
									videoUrlReferer = currentWebUrl,
									videoThumbnailByReferer = true,
									videoCookie = videoCookie
								)
							).interceptIntentURI(videoUrlInfo.fileUrl, false)
							extractedLinksDialog.close()
						}
					} else {
						safeMotherActivity?.let { safeMotherActivity ->
							extractedLinksDialog.close()
							SingleResolutionPrompter(
								baseActivity = safeMotherActivity,
								singleResolutionName = videoUrlInfo.fileResolution,
								extractedVideoLink = videoUrlInfo.fileUrl,
								currentWebUrl = currentWebUrl,
								videoCookie = videoCookie,
								videoTitle = videoTitle,
								videoUrlReferer = currentWebUrl,
								isSocialMediaUrl = false,
								isDownloadFromBrowser = true,
							).show()
						}
					}
				} else {
					try {
						val currentWebUrl = webviewEngine.currentWebView?.url
						val videoCookie = webviewEngine.getCurrentWebViewCookies()
						
						safeMotherActivity?.let { safeMotherActivity ->
							extractedLinksDialog.close()
							RegularDownloadPrompter(
								motherActivity = safeMotherActivity,
								singleResolutionName = videoUrlInfo.fileResolution,
								extractedVideoLink = videoUrlInfo.fileUrl,
								currentWebUrl = currentWebUrl,
								videoCookie = videoCookie,
								videoTitle = videoTitle,
								videoUrlReferer = currentWebUrl,
								isFromSocialMedia = false
							).show()
						}
					} catch (error: Exception) {
						extractedLinksDialog.close()
						error.printStackTrace()
					}
				}
			}
		}
		
		/**
		 * Placeholder: Set up long-click functionality to copy URL or other future actions.
		 */
		private fun setupLongClickItemListener(extractedVideoLink: String) {
			itemClickableContainer.setOnLongClickListener {
				copyTextToClipboard(safeMotherActivity, extractedVideoLink)
				showToast(msgId = R.string.text_copied_url_to_clipboard)
				return@setOnLongClickListener true
			}
		}
	}
}