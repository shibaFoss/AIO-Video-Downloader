package app.ui.others.information

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.graphics.Color.TRANSPARENT
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.core.graphics.drawable.toDrawable
import app.core.AIOApp.Companion.IS_PREMIUM_USER
import app.core.AIOApp.Companion.IS_ULTIMATE_VERSION_UNLOCKED
import app.core.bases.BaseActivity
import app.core.engines.video_parser.parsers.SupportedURLs.isSocialMediaUrl
import app.core.engines.video_parser.parsers.VideoThumbGrabber.startParsingVideoThumbUrl
import app.ui.main.MotherActivity
import app.ui.main.fragments.browser.webengine.SingleResolutionPrompter
import app.ui.main.fragments.downloads.intercepter.SharedVideoURLIntercept
import com.aio.R
import lib.device.IntentUtility.getIntentDataURI
import lib.networks.URLUtility
import lib.networks.URLUtilityKT.fetchWebPageContent
import lib.networks.URLUtilityKT.getWebpageTitleOrDescription
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.ThreadsUtility
import lib.ui.builders.ToastView.Companion.showToast
import lib.ui.builders.WaitingDialog
import java.lang.ref.WeakReference

/**
 * This activity intercepts external video URLs (usually from Share actions or browser) and attempts
 * to extract video metadata or redirect it properly based on user's premium status and URL type.
 *
 * For social media URLs, it attempts to grab the title and thumbnail to prompt the user for download.
 * For other URLs, it passes them through a shared video URL interceptor.
 * If user is not premium, it forwards the intent to MotherActivity.
 */
class IntentInterceptActivity : BaseActivity() {

    // Weak reference to avoid memory leaks in background thread operations
    private val weakSelfReference = WeakReference(this)
    private val safeIntentInterceptActivityRef = weakSelfReference.get()

    // Flag to abort processing if the user exits during metadata fetching
    private var isParsingTitleFromUrlAborted = false

    /**
     * No layout is rendered directly for this activity.
     * It performs background operations and forwards/redirects based on the intent.
     */
    override fun onRenderingLayout(): Int {
        return -1
    }

    /**
     * Handles the back press to close the activity with fade animation.
     */
    override fun onBackPressActivity() {
        closeActivityWithFadeAnimation(true)
    }

    /**
     * This is called after the layout is "rendered" (in this case, skipped).
     * Handles the intent URL processing and dispatches to proper flow based on logic.
     */
    override fun onAfterLayoutRender() {
        setUpWindowConfiguration()

        val intentUrl = getIntentDataURI(getActivity())
        if (URLUtility.isValidURL(intentUrl) == false) {
			doSomeVibration(50)
            showToast(msgId = R.string.text_invalid_url)
            onBackPressActivity()
            return
        }

        // No valid URL found, exit activity
        if (intentUrl.isNullOrEmpty()) {
            onBackPressActivity()
        } else {
            // Only premium users with ultimate unlocked can access parsing features
            if (IS_PREMIUM_USER && IS_ULTIMATE_VERSION_UNLOCKED) {
                safeIntentInterceptActivityRef?.let {
                    // Handle social media URLs with advanced parser
                    if (isSocialMediaUrl(intentUrl)) {
                        val waitingDialog = WaitingDialog(
                            isCancelable = false,
                            baseActivityInf = it,
                            loadingMessage = getString(R.string.text_analyzing_url_please_wait),
                            dialogCancelListener = { dialog -> dialog.dismiss() }
                        )
                        waitingDialog.show()

                        // Perform parsing in background
                        ThreadsUtility.executeInBackground(codeBlock = {
                            val htmlBody = fetchWebPageContent(intentUrl, true)
                            val thumbnailUrl = startParsingVideoThumbUrl(intentUrl, htmlBody)

                            getWebpageTitleOrDescription(
                                websiteUrl = intentUrl,
                                userGivenHtmlBody = htmlBody
                            ) { resultedTitle ->
                                waitingDialog.close()

                                val userDidNotCancelExecution = !isParsingTitleFromUrlAborted
                                val validIntentUrl = !resultedTitle.isNullOrEmpty()

                                if (validIntentUrl && userDidNotCancelExecution) {
                                    // Show prompter dialog in UI thread
                                    executeOnMainThread {
                                        val prompter = SingleResolutionPrompter(
                                            baseActivity = it,
                                            singleResolutionName = getText(R.string.title_high_quality).toString(),
                                            extractedVideoLink = intentUrl,
                                            currentWebUrl = intentUrl,
                                            videoTitle = resultedTitle,
                                            videoUrlReferer = intentUrl,
                                            dontParseFBTitle = true,
                                            thumbnailUrlProvided = thumbnailUrl,
                                            isSocialMediaUrl = true,
                                            isDownloadFromBrowser = false
                                        )

                                        prompter.show()

                                        // Close this activity when dialog is cancelled or dismissed
                                        with(prompter.getDialogBuilder().dialog) {
                                            setOnCancelListener { onBackPressActivity() }
                                            setOnDismissListener { onBackPressActivity() }
                                        }
                                    }
                                } else {
                                    executeOnMainThread {
                                        safeIntentInterceptActivityRef.doSomeVibration(50)
                                        showToast(msgId = R.string.text_couldnt_get_video_title)
                                        onBackPressActivity()
                                    }
                                }
                            }
                        })
                    } else {
                        // Use generic interceptor for non-social media URLs
                        val interceptor = SharedVideoURLIntercept(
                            baseActivity = safeIntentInterceptActivityRef,
                            onOpenBrowser = { forwardIntentToMotherActivity() }
                        )

                        interceptor.interceptIntentURI(
                            intentUrl = intentUrl,
                            shouldOpenBrowserAsFallback = false
                        )
                    }
                }
            } else {
                // Non-premium users are forwarded directly to the main activity
                forwardIntentToMotherActivity()
            }
        }
    }

    /**
     * Clears the weak reference to this activity.
     * Ensures that memory is released when activity is destroyed.
     */
    override fun clearWeakActivityReference() {
        weakSelfReference.clear()
        super.clearWeakActivityReference()
    }

    /**
     * Forward the intercepted intent directly to MotherActivity (main screen),
     * preserving the original data and intent action.
     */
    private fun forwardIntentToMotherActivity() {
        val originalIntent = intent
        val targetIntent = Intent(getActivity(), MotherActivity::class.java).apply {
            action = originalIntent.action
            setDataAndType(originalIntent.data, originalIntent.type)
            putExtras(originalIntent)
            flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
        }

        startActivity(targetIntent)
        closeActivityWithFadeAnimation(true)
    }

    /**
     * Configure the activity window:
     * - Make background transparent
     * - Hide action bar
     * - Remove layout limits for a clean overlay style
     */
    private fun setUpWindowConfiguration() {
        try {
            window.setBackgroundDrawable(TRANSPARENT.toDrawable())
            supportActionBar?.hide()
            window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }
}