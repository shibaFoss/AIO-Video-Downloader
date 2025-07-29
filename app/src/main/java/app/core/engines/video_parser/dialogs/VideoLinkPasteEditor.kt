package app.core.engines.video_parser.dialogs

import android.view.View
import android.widget.EditText
import app.core.engines.video_parser.parsers.SupportedURLs.isSocialMediaUrl
import app.core.engines.video_parser.parsers.VideoThumbGrabber.startParsingVideoThumbUrl
import app.ui.main.MotherActivity
import app.ui.main.fragments.browser.webengine.SingleResolutionPrompter
import app.ui.main.fragments.downloads.intercepter.SharedVideoURLIntercept
import com.aio.R
import lib.networks.URLUtility
import lib.networks.URLUtilityKT.fetchWebPageContent
import lib.networks.URLUtilityKT.getWebpageTitleOrDescription
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.process.CommonTimeUtils.delay
import lib.process.ThreadsUtility
import lib.texts.CommonTextUtils.getText
import lib.ui.ViewUtility.showOnScreenKeyboard
import lib.ui.builders.DialogBuilder
import lib.ui.builders.ToastView.Companion.showToast
import lib.ui.builders.WaitingDialog
import java.lang.ref.WeakReference

/**
 * A dialog component that allows users to paste and process video URLs manually.
 * Supports both direct video links and social media URLs, with support for
 * resolving thumbnails, titles, and showing interstitial ads.
 *
 * @property motherActivity The base activity hosting this dialog.
 * @property passOnUrl Url passed on by provider class.
 * @property autoStart Indicate whether the dialog auto start the parsing.
 */
class VideoLinkPasteEditor(
	val motherActivity: MotherActivity,
	val passOnUrl: String? = null,
	val autoStart: Boolean = false
) {
	
	// Weak reference to avoid memory leaks
	private val safeMotherActivityRef = WeakReference(motherActivity).get()
	private var dialogBuilder: DialogBuilder? = DialogBuilder(safeMotherActivityRef)
	
	// UI components
	private lateinit var buttonDownload: View
	private lateinit var editFieldFileURL: EditText
	private lateinit var editFieldContainer: View
	
	private var userGivenURL: String = ""
	private var isParsingTitleFromUrlAborted = false
	
	/**
	 * Initializes the dialog and preloads the interstitial ad if needed.
	 */
	init {
		safeMotherActivityRef?.let {
			dialogBuilder?.let { builder ->
				builder.setView(R.layout.dialog_video_link_editor_1)
				builder.view.apply {
					buttonDownload = findViewById(R.id.button_dialog_positive_container)
					editFieldContainer = findViewById(R.id.edit_field_file_url_container)
					editFieldFileURL = findViewById(R.id.edit_field_file_url)
					passOnUrl?.let { editFieldFileURL.setText(it) }
					
					val clickActions = mapOf(
						editFieldContainer to { focusEditTextField() },
						buttonDownload to { downloadVideo() }
					)
					
					clickActions.forEach { (view, action) ->
						view.setOnClickListener { action() }
					}
				}
			}
		}
	}
	
	/**
	 * Displays the dialog and focuses on the input field.
	 */
	fun show() {
		if (!passOnUrl.isNullOrEmpty() && autoStart){
			downloadVideo()
		} else {
			dialogBuilder?.show()
			delay(200, object : OnTaskFinishListener {
				override fun afterDelay() {
					focusEditTextField()
					editFieldFileURL.selectAll()
					showOnScreenKeyboard(safeMotherActivityRef, editFieldFileURL)
				}
			})
		}
	}
	
	/**
	 * Closes the dialog if it's open.
	 */
	fun close() = dialogBuilder?.close()
	
	/**
	 * Focuses the input field inside the dialog.
	 */
	private fun focusEditTextField() {
		editFieldFileURL.requestFocus()
	}
	
	/**
	 * Handles the logic when user clicks on the download button.
	 * Validates the URL, shows appropriate dialogs, and processes the video URL.
	 */
	private fun downloadVideo() {
		safeMotherActivityRef?.let { safeActivity ->
			userGivenURL = editFieldFileURL.text.toString()
			if (!URLUtility.isValidURL(userGivenURL)) {
				safeActivity.doSomeVibration(50)
				showToast(getText(R.string.text_file_url_not_valid))
				return
			} else {
				close()
				
				if (isSocialMediaUrl(userGivenURL)) {
					// Handle social media URL
					val waitingDialog = WaitingDialog(
						isCancelable = false,
						baseActivityInf = motherActivity,
						loadingMessage = getText(R.string.text_analyzing_url_please_wait),
						dialogCancelListener = { dialog ->
							isParsingTitleFromUrlAborted = true
							dialog.dismiss()
						}
					); waitingDialog.show()
					
					ThreadsUtility.executeInBackground(codeBlock = {
						val htmlBody = fetchWebPageContent(userGivenURL, true)
						val thumbnailUrl = startParsingVideoThumbUrl(userGivenURL, htmlBody)
						
						getWebpageTitleOrDescription(userGivenURL, userGivenHtmlBody = htmlBody) { resultedTitle ->
							waitingDialog.close()
							if (!resultedTitle.isNullOrEmpty() && !isParsingTitleFromUrlAborted) {
								executeOnMainThread {
									SingleResolutionPrompter(
										baseActivity = motherActivity,
										singleResolutionName = getText(R.string.title_high_quality),
										extractedVideoLink = userGivenURL,
										currentWebUrl = userGivenURL,
										videoTitle = resultedTitle,
										videoUrlReferer = userGivenURL,
										isSocialMediaUrl = true,
										isDownloadFromBrowser = false,
										dontParseFBTitle = true,
										thumbnailUrlProvided = thumbnailUrl
									).show()
								}
							} else {
								executeOnMainThread {
									safeMotherActivityRef.doSomeVibration(50)
									showToast(msgId = R.string.text_couldnt_get_video_title)
								}
							}
						}
					})
				} else {
					// Direct video URL
					startParingVideoURL(safeActivity)
				}
			}
		}
	}
	
	/**
	 * Triggers interception of the provided video URL for non-social platforms.
	 */
	private fun startParingVideoURL(safeActivity: MotherActivity) {
		close()
		val videoInterceptor = SharedVideoURLIntercept(safeActivity)
		videoInterceptor.interceptIntentURI(userGivenURL)
	}
}