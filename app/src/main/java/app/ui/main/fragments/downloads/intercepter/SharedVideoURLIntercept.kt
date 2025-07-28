package app.ui.main.fragments.downloads.intercepter

import android.widget.TextView
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.IS_ULTIMATE_VERSION_UNLOCKED
import app.core.bases.BaseActivity
import app.core.engines.video_parser.parsers.SupportedURLs.isYouTubeUrl
import app.core.engines.video_parser.parsers.SupportedURLs.isYtdlpSupportedUrl
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoFormat
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoInfo
import app.core.engines.video_parser.parsers.VideoParserUtility.getYtdlpVideoFormatsListWithRetry
import app.ui.main.MotherActivity
import app.ui.others.information.ContentPolicyActivity
import com.aio.R
import lib.device.IntentUtility.openLinkInSystemBrowser
import lib.networks.URLUtility.isValidURL
import lib.process.AsyncJobUtils
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.process.CommonTimeUtils.delay
import lib.process.ThreadsUtility
import lib.texts.CommonTextUtils.getText
import lib.ui.MsgDialogUtils.showMessageDialog
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.builders.ToastView.Companion.showToast
import lib.ui.builders.WaitingDialog
import java.lang.ref.WeakReference

/**
 * Intercepts shared video URLs and processes them to extract downloadable formats,
 * especially using YTDLP (youtube-dl/pytube) supported parsing, and displays
 * a resolution picker dialog if formats are available.
 *
 * @param baseActivity Activity context used to show UI components and dialogs.
 * @param userGivenVideoInfo Optional pre-filled video info (title, description, etc.).
 * @param onOpenBrowser Callback triggered when user opts to open the link externally.
 */
class SharedVideoURLIntercept(
	private val baseActivity: BaseActivity?,
	private val userGivenVideoInfo: VideoInfo? = null,
	private val onOpenBrowser: (() -> Unit?)? = null
) {
	
	/** Safe reference to activity to avoid memory leaks */
	private var safeBaseActivityRef = WeakReference(baseActivity).get()
	
	/** Indicates if the intercepting process is currently active */
	private var isInterceptingInProcess: Boolean = false
	
	/** Indicates whether the current interception was cancelled or finished */
	private var isInterceptingTerminated: Boolean = false
	
	/** UI dialog to show progress while analyzing video */
	private var waitingDialog: WaitingDialog? = null
	
	/** Whether to open browser automatically if intercept fails */
	private var shouldOpenBrowserAsFallback = true
	
	/**
	 * Starts processing the shared video URL.
	 * @param intentUrl The video URL received via intent.
	 * @param shouldOpenBrowserAsFallback Whether to fallback to browser if formats not found.
	 */
	fun interceptIntentURI(intentUrl: String?, shouldOpenBrowserAsFallback: Boolean = true) {
		this.shouldOpenBrowserAsFallback = shouldOpenBrowserAsFallback
		
		intentUrl?.let {
			if (!isValidURL(intentUrl)) return
			initWaitingMessageDialog()
			startIntercepting(intentUrl)
		}
	}
	
	/** Initializes the waiting dialog shown during video analysis */
	private fun initWaitingMessageDialog() {
		waitingDialog = WaitingDialog(
			isCancelable = false,
			activityInf = safeBaseActivityRef,
			loadingMessage = getText(R.string.text_analyzing_url_please_wait),
			dialogCancelListener = {
				waitingDialog?.let { waitingDialog ->
					waitingDialog.close()
					onCancelInterceptRequested(waitingDialog)
				}
			}
		)
	}
	
	/**
	 * Updates the dialog message during analysis.
	 * @param updatedMessage Message to display.
	 */
	private fun updateWaitingMessage(updatedMessage: String) {
		waitingDialog?.let { waitingDialog ->
			AsyncJobUtils.executeOnMainThread {
				waitingDialog.dialogBuilder?.apply {
					val textProgressInfo = view.findViewById<TextView>(R.id.txt_progress_info)
					textProgressInfo.text = updatedMessage
				}
			}
		}
	}
	
	/**
	 * Begins checking if the given video URL can be analyzed.
	 * @param targetVideoUrl The URL to analyze.
	 */
	private fun startIntercepting(targetVideoUrl: String) {
		waitingDialog?.let { waitingDialog ->
			if (isInterceptingInProcess) return
			
			waitingDialog.show()
			isInterceptingInProcess = true
			
			ThreadsUtility.executeInBackground(codeBlock = {
				if (isYoutubeUrlDetected(targetVideoUrl)) return@executeInBackground
				if (!isYtdlpSupportedUrl(targetVideoUrl)) {
					ThreadsUtility.executeOnMain {
						waitingDialog.close()
						safeBaseActivityRef?.doSomeVibration(50)
						showToast(msgId = R.string.text_unsupported_video_link)
						openInBuiltInBrowser(targetVideoUrl)
					}; return@executeInBackground
				} else startAnalyzingVideoUrl(targetVideoUrl)
			})
		}
	}
	
	/**
	 * Parses the video URL and shows resolution picker if successful.
	 * @param videoUrl The target video URL.
	 */
	private fun startAnalyzingVideoUrl(videoUrl: String) {
		safeBaseActivityRef?.let { safeBaseActivityRef ->
			val videoCookie = userGivenVideoInfo?.videoCookie
			val videoTitle = userGivenVideoInfo?.videoTitle
			val videoDescription = userGivenVideoInfo?.videoDescription
			val videoThumbnailUrl = userGivenVideoInfo?.videoThumbnailUrl
			val videoUrlReferer = userGivenVideoInfo?.videoUrlReferer
			val videoThumbnailByReferer = userGivenVideoInfo?.videoThumbnailByReferer
			val videoFormats = if (isYouTubeUrl(videoUrl) && IS_ULTIMATE_VERSION_UNLOCKED)
				getYoutubeVideoResolutions() else getYtdlpVideoFormatsListWithRetry(videoUrl, videoCookie)
			
			val videoInfo = VideoInfo(
				videoUrl = videoUrl,
				videoTitle = videoTitle,
				videoDescription = videoDescription,
				videoThumbnailUrl = videoThumbnailUrl,
				videoUrlReferer = videoUrlReferer,
				videoThumbnailByReferer = videoThumbnailByReferer ?: false,
				videoCookie = videoCookie,
				videoFormats = videoFormats
			)
			
			AsyncJobUtils.executeOnMainThread {
				waitingDialog?.let { waitingDialog ->
					waitingDialog.close()
					isInterceptingInProcess = false
					
					if (!isInterceptingTerminated) {
						if (videoInfo.videoFormats.isEmpty()) {
							if (shouldOpenBrowserAsFallback) openInBuiltInBrowser(videoUrl)
							else showToast(msgId = R.string.text_no_video_found)
							
						} else {
							VideoResolutionPicker(safeBaseActivityRef, videoInfo) {
								if (shouldOpenBrowserAsFallback) showOpeningInBrowserPrompt(videoUrl)
								else onOpenBrowser?.invoke()
							}.show()
						}
					}
				}
			}
		}
	}
	
	/**
	 * Provides default YouTube resolutions when Ultimate version is unlocked.
	 */
	private fun getYoutubeVideoResolutions() = listOf(
		"Audio", "144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p"
	).map {
		VideoFormat(
			formatId = INSTANCE.packageName,
			formatResolution = it,
			formatFileSize = getText(R.string.title_unknown)
		)
	}
	
	/**
	 * Shows a fallback prompt to open link in browser when no video format is found.
	 */
	private fun showOpeningInBrowserPrompt(videoUrl: String) {
		val msgResId = R.string.text_error_failed_to_fetch_video_format
		val buttonTextResId = R.string.title_open_link_in_browser
		showMessageDialog(
			baseActivityInf = safeBaseActivityRef,
			isNegativeButtonVisible = false,
			messageTextViewCustomize = { it.setText(msgResId) },
			positiveButtonTextCustomize = {
				it.setText(buttonTextResId)
				it.setLeftSideDrawable(R.drawable.ic_button_open_v2)
			}
		)?.apply {
			setOnClickForPositiveButton {
				close(); openInBuiltInBrowser(videoUrl)
			}
		}
	}
	
	/**
	 * Tries to open the URL using the app's internal browser.
	 */
	private fun openInBuiltInBrowser(urlFromIntent: String) {
		safeBaseActivityRef?.let { safeBaseActivityRef ->
			try {
				if (safeBaseActivityRef is MotherActivity) {
					val fileUrl: String = urlFromIntent
					val browserFragment = safeBaseActivityRef.browserFragment
					val browserFragmentBody = browserFragment?.browserFragmentBody
					val webviewEngine = browserFragmentBody?.webviewEngine ?: return
					
					if (fileUrl.isNotEmpty() && isValidURL(fileUrl)) {
						safeBaseActivityRef.sideNavigation?.addNewBrowsingTab(fileUrl, webviewEngine)
						safeBaseActivityRef.openBrowserFragment()
					} else showInvalidUrlToast()
				} else onOpenBrowser?.let { it() }
			} catch (error: Exception) {
				error.printStackTrace()
				openInSystemBrowser(urlFromIntent)
			}
		}
	}
	
	/**
	 * Opens the URL using the default system browser.
	 */
	private fun openInSystemBrowser(urlFromIntent: String) {
		openLinkInSystemBrowser(urlFromIntent, safeBaseActivityRef) {
			showToast(getText(R.string.text_failed_open_the_video))
		}
	}
	
	/** Shows a toast indicating invalid or malformed URL. */
	private fun showInvalidUrlToast() {
		showToast(msgId = R.string.text_invalid_url)
	}
	
	/**
	 * Handles cases where YouTube URL is detected but download isn't allowed.
	 * @return true if URL is blocked due to YouTube restrictions.
	 */
	private fun isYoutubeUrlDetected(urlFromIntent: String): Boolean {
		if (IS_ULTIMATE_VERSION_UNLOCKED) return false
		if (isYouTubeUrl(urlFromIntent)) {
			AsyncJobUtils.executeOnMainThread {
				waitingDialog?.let { waitingDialog ->
					delay(200, object : OnTaskFinishListener {
						override fun afterDelay() {
							waitingDialog.close()
							val titleResId = R.string.title_content_download_policy
							val msgResId = R.string.text_not_support_youtube_download
							val positiveButtonResId = R.string.title_content_policy
							val positiveButtonImgResId = R.drawable.ic_button_arrow_next
							R.string.title_cancel
							R.drawable.ic_button_cancel
							showMessageDialog(
								baseActivityInf = safeBaseActivityRef,
								isTitleVisible = true,
								isNegativeButtonVisible = false,
								titleTextViewCustomize = { it.setText(titleResId) },
								messageTextViewCustomize = { it.setText(msgResId) },
								positiveButtonTextCustomize = {
									it.setText(positiveButtonResId)
									it.setLeftSideDrawable(positiveButtonImgResId)
								}
							)?.apply {
								setOnClickForPositiveButton {
									close()
									val classToOpen = ContentPolicyActivity::class.java
									safeBaseActivityRef?.openActivity(classToOpen)
								}
							}
						}
					})
				}
			}
			return true
		}
		return false
	}
	
	/**
	 * Cancels the ongoing interception when user dismisses the dialog.
	 */
	private fun onCancelInterceptRequested(waitingDialog: WaitingDialog) {
		waitingDialog.dialogBuilder?.let { dialogBuilder ->
			if (dialogBuilder.isShowing) {
				dialogBuilder.close()
			}
		}
		isInterceptingInProcess = false
		isInterceptingTerminated = true
	}
}