package app.ui.main.fragments.browser.webengine

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import app.core.AIOApp.Companion.IS_PREMIUM_USER
import app.core.AIOApp.Companion.IS_ULTIMATE_VERSION_UNLOCKED
import app.core.AIOApp.Companion.aioSettings
import app.core.AIOApp.Companion.downloadSystem
import app.core.bases.BaseActivity
import app.core.engines.downloader.DownloadDataModel
import app.core.engines.video_parser.parsers.SupportedURLs.isFacebookUrl
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoFormat
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoInfo
import app.core.engines.video_parser.parsers.VideoThumbGrabber.startParsingVideoThumbUrl
import com.aio.R
import lib.device.IntentUtility.openLinkInSystemBrowser
import lib.networks.URLUtilityKT
import lib.networks.URLUtilityKT.getWebpageTitleOrDescription
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.ThreadsUtility
import lib.process.ThreadsUtility.executeOnMain
import lib.texts.CommonTextUtils.getText
import lib.ui.ViewUtility.animateFadInOutAnim
import lib.ui.ViewUtility.closeAnyAnimation
import lib.ui.ViewUtility.loadThumbnailFromUrl
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.builders.DialogBuilder
import lib.ui.builders.ToastView.Companion.showToast
import java.lang.ref.WeakReference

/**
 * A dialog prompter that shows single resolution download options for videos.
 * Handles video metadata display, thumbnail loading, and download initiation.
 *
 * @property baseActivity The parent activity reference
 * @property singleResolutionName The resolution name to display (e.g. "720p")
 * @property extractedVideoLink The direct video URL to download
 * @property currentWebUrl The webpage URL where video was found (optional)
 * @property videoCookie Cookie string for authenticated downloads (optional)
 * @property videoTitle Pre-extracted video title (optional)
 * @property videoUrlReferer Referer URL for the video (optional)
 * @property isSocialMediaUrl Whether the URL is from a social media platform
 * @property dontParseFBTitle Skip Facebook title parsing if true
 * @property thumbnailUrlProvided Pre-extracted thumbnail URL (optional)
 * @property isDownloadFromBrowser Whether download originated from browser
 */
class SingleResolutionPrompter(
    private val baseActivity: BaseActivity,
    private val singleResolutionName: String,
    private val extractedVideoLink: String,
    private val currentWebUrl: String? = null,
    private val videoCookie: String? = null,
    private var videoTitle: String? = null,
    private val videoUrlReferer: String? = null,
    private val isSocialMediaUrl: Boolean = false,
    private val dontParseFBTitle: Boolean = false,
    private val thumbnailUrlProvided: String? = null,
    private val isDownloadFromBrowser: Boolean = false
) {
    // Weak reference to prevent memory leaks
    private val safeBaseActivity = WeakReference(baseActivity).get()

    // Dialog builder for the resolution prompt
    private val dialogBuilder: DialogBuilder = DialogBuilder(safeBaseActivity)

    // Model to store download information
    private val downloadModel = DownloadDataModel()

    // Cache for video thumbnail URL
    private var videoThumbnailUrl: String = ""

    init {
        // Initialize dialog view and setup components
        dialogBuilder.setView(R.layout.dialog_single_m3u8_prompter_1)
        dialogBuilder.view.apply {
            setupTitleAndThumbnail()
            setupDownloadButton()
            setupCardInfoButton()
        }
    }

    /**
     * Shows the resolution prompt dialog if not already showing
     */
    fun show() {
        if (!dialogBuilder.isShowing) {
            dialogBuilder.show()
        }
    }

    /**
     * Closes the resolution prompt dialog if showing
     */
    fun close() {
        if (dialogBuilder.isShowing) {
            dialogBuilder.close()
        }
    }

    /**
     * Gets the dialog builder instance
     * @return DialogBuilder instance
     */
    fun getDialogBuilder(): DialogBuilder {
        return dialogBuilder
    }

    /**
     * Displays and fetches video title:
     * - Uses provided title if available
     * - Falls back to hostname + resolution
     * - Fetches Facebook title asynchronously if needed
     * @param layout The parent view containing title TextView
     */
    private fun showVideoTitleFromURL(layout: View) {
        val videoTitleView = layout.findViewById<TextView>(R.id.txt_video_title)

        // Use provided title if available
        if (!videoTitle.isNullOrEmpty()) {
            videoTitleView.isSelected = true
            videoTitleView.text = videoTitle

        } else {
            // Fallback title format: "hostname_resolution"
            val hostName = URLUtilityKT.getHostFromUrl(currentWebUrl)
            val resolutionName = singleResolutionName
            val finalTitle = "${hostName}_${resolutionName}"
            videoTitleView.text = finalTitle
        }

        // Skip Facebook title parsing if requested
        if (dontParseFBTitle) return

        // Special handling for Facebook URLs
        if (currentWebUrl?.let { isFacebookUrl(it) } == true) {
            ThreadsUtility.executeInBackground(codeBlock = {
                // Show loading animation while fetching
                executeOnMainThread { animateFadInOutAnim(videoTitleView) }

                // Fetch webpage title asynchronously
                getWebpageTitleOrDescription(currentWebUrl) { resultedTitle ->
                    if (!resultedTitle.isNullOrEmpty()) {
                        executeOnMainThread {
                            closeAnyAnimation(videoTitleView)
                            videoTitleView.text = resultedTitle
                            videoTitle = resultedTitle // Cache the fetched title
                        }
                    }
                }
            })
        }
    }

    /**
     * Displays video resolution information
     * @param layout The parent view containing resolution TextView
     */
    private fun showVideoResolution(layout: View) {
        safeBaseActivity?.let { safeMotherActivity ->
            val videoResView = layout.findViewById<TextView>(R.id.text_video_resolution)
            if (singleResolutionName.isNotEmpty()) {
                val resId = R.string.text_resolution_info
                videoResView.text = safeMotherActivity.getString(resId, singleResolutionName)
            } else videoResView.text = getText(R.string.title_not_available)
        }
    }

    /**
     * Loads and displays video thumbnail:
     * - Uses provided thumbnail if available
     * - Fetches thumbnail from URL if needed
     * @param layout The parent view containing thumbnail ImageView
     */
    private fun showVideoThumb(layout: View) {
        // Use provided thumbnail if available
        if (!thumbnailUrlProvided.isNullOrEmpty()) {
            videoThumbnailUrl = thumbnailUrlProvided
            val videoThumbnail = layout.findViewById<ImageView>(R.id.image_video_thumbnail)
            loadThumbnailFromUrl(videoThumbnailUrl, videoThumbnail)
            return
        }

        // Fetch thumbnail asynchronously
        ThreadsUtility.executeInBackground(codeBlock = {
            val websiteUrl = videoUrlReferer ?: currentWebUrl
            if (websiteUrl.isNullOrEmpty()) return@executeInBackground

            // Parse thumbnail URL from webpage
            val thumbImageUrl = startParsingVideoThumbUrl(websiteUrl)
            if (thumbImageUrl.isNullOrEmpty()) return@executeInBackground

            // Load thumbnail on UI thread
            executeOnMain {
                videoThumbnailUrl = thumbImageUrl
                val videoThumbnail = layout.findViewById<ImageView>(R.id.image_video_thumbnail)
                loadThumbnailFromUrl(thumbImageUrl, videoThumbnail)
            }
        })
    }

    /**
     * Sets up title, resolution and thumbnail views
     * @receiver The dialog content view
     */
    private fun View.setupTitleAndThumbnail() {
        showVideoTitleFromURL(layout = this)
        showVideoResolution(layout = this)
        showVideoThumb(layout = this)
    }

    /**
     * Sets up info button to open video URL in browser
     * @receiver The dialog content view
     */
    private fun View.setupCardInfoButton() {
        val buttonCardInfo = findViewById<View>(R.id.btn_file_info_card)
        buttonCardInfo.setOnClickListener { openVideoUrlInBrowser() }
    }

    /**
     * Sets up download button with appropriate state:
     * - Shows "Watch Ad to Download" after download threshold
     * - Handles premium user state
     * @receiver The dialog content view
     */
    private fun View.setupDownloadButton() {
        val buttonDownload = findViewById<View>(R.id.btn_dialog_positive_container)
        buttonDownload.setOnClickListener { addVideoFormatToDownloadSystem() }

        // Check if user exceeded free download limit
        val numberOfDownloadsUserDid = aioSettings.numberOfDownloadsUserDid
        val maxDownloadThreshold = aioSettings.numberOfMaxDownloadThreshold
        if (numberOfDownloadsUserDid >= maxDownloadThreshold) {
            if (!IS_PREMIUM_USER && !IS_ULTIMATE_VERSION_UNLOCKED) {
                // Show "Watch Ad to Download" for free users over limit
                val btnDownloadText = findViewById<TextView>(R.id.btn_dialog_positive)
                btnDownloadText.let {
                    it.setLeftSideDrawable(R.drawable.ic_button_video)
                    it.setText(R.string.text_watch_ad_to_download)
                }
            }
        }
    }

    /**
     * Opens video URL in system browser
     */
    private fun openVideoUrlInBrowser() {
        safeBaseActivity?.let { safeMotherActivityRef ->
            if (currentWebUrl.isNullOrEmpty()) return
            openLinkInSystemBrowser(currentWebUrl, safeMotherActivityRef) {
                // Handle browser open failure
                safeMotherActivityRef.doSomeVibration(40)
                showToast(getText(R.string.text_failed_open_the_video))
            }
        }
    }

    /**
     * Adds video format to download system and closes dialog
     */
    private fun addVideoFormatToDownloadSystem() {
        addToDownloadSystem()
        close()
    }

    /**
     * Prepares and adds download task to download system
     */
    private fun addToDownloadSystem() {
        ThreadsUtility.executeInBackground(codeBlock = {
            safeBaseActivity?.let { safeBaseActivityRef ->
                try {
                    // Validate required URL
                    if (currentWebUrl.isNullOrEmpty()) {
                        executeOnMain {
                            safeBaseActivityRef.doSomeVibration(50)
                            showToast(msgId = R.string.text_something_went_wrong)
                        }; return@executeInBackground
                    }

                    val videoCookie = videoCookie
                    val videoTitle = videoTitle
                    val videoThumbnailUrl = videoThumbnailUrl
                    val videoUrlReferer = videoUrlReferer
                    val videoThumbnailByReferer = true
                    val videoFormats = listOf(
                        VideoFormat(
                            formatId = safeBaseActivityRef.packageName,
                            isFromSocialMedia = isSocialMediaUrl,
                            formatResolution = singleResolutionName
                        )
                    )

                    // Prepare video info
                    val videoInfo = VideoInfo(
                        videoUrl = currentWebUrl,
                        videoTitle = videoTitle,
                        videoThumbnailUrl = videoThumbnailUrl,
                        videoUrlReferer = videoUrlReferer,
                        videoThumbnailByReferer = videoThumbnailByReferer,
                        videoCookie = videoCookie,
                        videoFormats = videoFormats
                    )

                    // Configure download model
                    downloadModel.videoInfo = videoInfo
                    downloadModel.videoFormat = videoFormats[0]
                    downloadModel.isDownloadFromBrowser = isDownloadFromBrowser

                    if (videoUrlReferer != null) downloadModel.siteReferrer = videoUrlReferer

                    val urlCookie = videoInfo.videoCookie
                    if (!urlCookie.isNullOrEmpty()) downloadModel.siteCookieString = urlCookie

                    // Add download to system
                    downloadSystem.addDownload(downloadModel) {
                        executeOnMainThread {
                            val toastMsgResId = R.string.text_download_added_successfully
                            showToast(msgId = toastMsgResId)
                        }
                    }

                    // Update download counters
                    aioSettings.numberOfDownloadsUserDid++
                    aioSettings.totalNumberOfSuccessfulDownloads++
                    aioSettings.updateInStorage()

                } catch (error: Exception) {
                    error.printStackTrace()
                    val failedToAddResId = R.string.text_failed_to_add_download_task
                    executeOnMain {
                        safeBaseActivityRef.doSomeVibration(20)
                        showToast(msgId = failedToAddResId)
                    }
                }
            }
        })
    }
}