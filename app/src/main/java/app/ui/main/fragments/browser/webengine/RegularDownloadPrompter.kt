package app.ui.main.fragments.browser.webengine

import android.view.View
import android.view.View.GONE
import android.widget.ImageView
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile.fromFile
import app.core.AIOApp.Companion.IS_PREMIUM_USER
import app.core.AIOApp.Companion.IS_ULTIMATE_VERSION_UNLOCKED
import app.core.AIOApp.Companion.admobHelper
import app.core.AIOApp.Companion.aioSettings
import app.core.AIOApp.Companion.downloadSystem
import app.core.engines.downloader.DownloadDataModel
import app.core.engines.video_parser.parsers.SupportedURLs.isFacebookUrl
import app.core.engines.video_parser.parsers.VideoThumbGrabber.startParsingVideoThumbUrl
import app.ui.main.MotherActivity
import com.aio.R
import com.google.android.gms.ads.AdView
import lib.device.DateTimeUtils.millisToDateTimeString
import lib.device.IntentUtility.openLinkInSystemBrowser
import lib.device.StorageUtility.getFreeExternalStorageSpace
import lib.files.FileUtility.isFileNameValid
import lib.files.FileUtility.sanitizeFileNameExtreme
import lib.files.FileUtility.sanitizeFileNameNormal
import lib.networks.URLUtilityKT
import lib.networks.URLUtilityKT.getWebpageTitleOrDescription
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.ThreadsUtility
import lib.process.ThreadsUtility.executeOnMain
import lib.texts.CommonTextUtils.getText
import lib.texts.CommonTextUtils.removeDuplicateSlashes
import lib.texts.CommonTextUtils.removeEmptyLines
import lib.ui.MsgDialogUtils.showMessageDialog
import lib.ui.ViewUtility.animateFadInOutAnim
import lib.ui.ViewUtility.closeAnyAnimation
import lib.ui.ViewUtility.loadThumbnailFromUrl
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.builders.DialogBuilder
import lib.ui.builders.ToastView.Companion.showToast
import java.io.File
import java.lang.ref.WeakReference

class RegularDownloadPrompter(
	private val motherActivity: MotherActivity,
	private val singleResolutionName: String,
	private val extractedVideoLink: String,
	private val currentWebUrl: String? = null,
	private val videoCookie: String? = null,
	private var videoTitle: String? = null,
	private val videoUrlReferer: String? = null,
	private val isFromSocialMedia: Boolean = false,
	private val dontParseFBTitle: Boolean = false,
	private val thumbnailUrlProvided: String? = null
) {
	private val safeMotherActivity = WeakReference(motherActivity).get()
	private val dialogBuilder: DialogBuilder = DialogBuilder(safeMotherActivity)
	private val downloadModel = DownloadDataModel()
	private var videoThumbnailUrl: String = ""
	
	init {
		dialogBuilder.setView(R.layout.dialog_single_m3u8_prompter_1)
		dialogBuilder.view.apply {
			setupAdmobView()
			setupTitleAndThumbnail()
			setupDownloadButton()
			setupCardInfoButton()
		}
	}
	
	fun show() {
		if (!dialogBuilder.isShowing) {
			dialogBuilder.show()
		}
	}
	
	fun close() {
		if (dialogBuilder.isShowing) {
			dialogBuilder.close()
		}
	}
	
	private fun showAdmobAds(admobAdView: AdView, layout: View) {
		if (!IS_PREMIUM_USER) {
			admobHelper.loadBannerAd(admobAdView)
		} else {
			layout.findViewById<View>(R.id.ad_space_container).visibility = GONE
			admobAdView.visibility = GONE
		}
		
		admobHelper.loadRewardedInterstitialAd(safeMotherActivity)
	}
	
	private fun showVideoTitleFromURL(layout: View) {
		val videoTitleView = layout.findViewById<TextView>(R.id.text_video_title)
		if (!videoTitle.isNullOrEmpty()) {
			videoTitleView.isSelected = true
			videoTitleView.text = videoTitle
			
		} else {
			val hostName = URLUtilityKT.getHostFromUrl(currentWebUrl)
			val resolutionName = singleResolutionName
			val finalTitle = "${hostName}_${resolutionName}"
			videoTitleView.text = finalTitle
		}
		
		if (dontParseFBTitle) return
		if (currentWebUrl?.let { isFacebookUrl(it) } == true) {
			ThreadsUtility.executeInBackground(codeBlock = {
				executeOnMainThread { animateFadInOutAnim(videoTitleView) }
				getWebpageTitleOrDescription(currentWebUrl) { resultedTitle ->
					if (!resultedTitle.isNullOrEmpty()) {
						executeOnMainThread {
							closeAnyAnimation(videoTitleView)
							videoTitleView.text = resultedTitle
							videoTitle = resultedTitle
						}
					}
				}
			})
		}
	}
	
	private fun showVideoResolution(layout: View) {
		safeMotherActivity?.let { safeMotherActivity ->
			val videoResView = layout.findViewById<TextView>(R.id.text_video_resolution)
			if (singleResolutionName.isNotEmpty()) {
				val resId = R.string.text_resolution_info
				videoResView.text = safeMotherActivity.getString(resId, singleResolutionName)
			} else videoResView.text = getText(R.string.title_not_available)
		}
	}
	
	private fun showVideoThumb(layout: View) {
		if (!thumbnailUrlProvided.isNullOrEmpty()) {
			videoThumbnailUrl = thumbnailUrlProvided
			val videoThumbnail = layout.findViewById<ImageView>(R.id.image_video_thumbnail)
			loadThumbnailFromUrl(videoThumbnailUrl, videoThumbnail)
			return
		}
		
		ThreadsUtility.executeInBackground(codeBlock = {
			val websiteUrl = videoUrlReferer ?: currentWebUrl
			if (websiteUrl.isNullOrEmpty()) return@executeInBackground
			val thumbImageUrl = startParsingVideoThumbUrl(websiteUrl)
			if (thumbImageUrl.isNullOrEmpty()) return@executeInBackground
			executeOnMain {
				videoThumbnailUrl = thumbImageUrl
				val videoThumbnail = layout.findViewById<ImageView>(R.id.image_video_thumbnail)
				loadThumbnailFromUrl(thumbImageUrl, videoThumbnail)
			}
		})
	}
	
	private fun View.setupTitleAndThumbnail() {
		showVideoTitleFromURL(layout = this)
		showVideoResolution(layout = this)
		showVideoThumb(layout = this)
	}
	
	private fun View.setupAdmobView() {
		val admobAdView = findViewById<AdView>(R.id.admob_fixed_sized_banner_ad)
		showAdmobAds(admobAdView = admobAdView, layout = this)
	}
	
	private fun View.setupCardInfoButton() {
		val buttonCardInfo = findViewById<View>(R.id.button_file_info_card)
		buttonCardInfo.setOnClickListener { openVideoUrlInBrowser() }
	}
	
	private fun View.setupDownloadButton() {
		val buttonDownload = findViewById<View>(R.id.button_dialog_positive_container)
		buttonDownload.setOnClickListener { startDownload() }
		
		val numberOfDownloadsUserDid = aioSettings.numberOfDownloadsUserDid
		val maxDownloadThreshold = aioSettings.numberOfMaxDownloadThreshold
		if (numberOfDownloadsUserDid >= maxDownloadThreshold) {
			if (!IS_PREMIUM_USER && !IS_ULTIMATE_VERSION_UNLOCKED) {
				val btnDownloadText = findViewById<TextView>(R.id.button_dialog_positive)
				btnDownloadText.let {
					it.setLeftSideDrawable(R.drawable.ic_button_video)
					it.setText(R.string.text_watch_ad_to_download)
				}
			}
		}
	}
	
	private fun openVideoUrlInBrowser() {
		safeMotherActivity?.let { safeMotherActivityRef ->
			if (currentWebUrl.isNullOrEmpty()) return
			openLinkInSystemBrowser(currentWebUrl, safeMotherActivityRef) {
				safeMotherActivityRef.doSomeVibration(40)
				showToast(getText(R.string.text_failed_open_the_video))
			}
		}
	}
	
	private fun startDownload() {
		val numberOfDownloadsUserDid = aioSettings.numberOfDownloadsUserDid
		val maxDownloadThreshold = aioSettings.numberOfMaxDownloadThreshold
		val condition1 = numberOfDownloadsUserDid < maxDownloadThreshold
		val condition2 = !admobHelper.isRewardedInterstitialAdReady()
		if (condition1 || condition2) {
			addVideoUrlToDownloadSystem()
			admobHelper.loadRewardedInterstitialAd(safeMotherActivity)
		} else {
			if (admobHelper.isRewardedInterstitialAdReady()) {
				admobHelper.showRewardedInterstitialAd(
					activity = safeMotherActivity,
					onAdCompleted = { addVideoUrlToDownloadSystem() },
					onAdClosed = {
						safeMotherActivity?.doSomeVibration(20)
						showToast(msgId = R.string.text_failed_to_add_download_task)
					}
				); close()
			} else {
				admobHelper.loadRewardedInterstitialAd(safeMotherActivity)
				addVideoUrlToDownloadSystem()
			}
		}
	}
	
	private fun addVideoUrlToDownloadSystem() {
		addToDownloadSystem()
		close()
	}
	
	private fun addToDownloadSystem() {
		ThreadsUtility.executeInBackground(codeBlock = {
			safeMotherActivity?.let { safeBaseActivityRef ->
				try {
					if (!isFileUrlValid()) return@executeInBackground
					else downloadModel.fileURL = extractedVideoLink
					
					validateDownloadFileName()
					validateDownloadDir()
					validateDownloadStartDate()
					validateOptionalConfigs()
					renameIfFileExistsWithSameName()
					checkStorageSpace().let { hasEnoughSpace ->
						if (hasEnoughSpace) {
							close()
							downloadSystem.addDownload(downloadModel) {
								executeOnMainThread {
									val toastMsgResId = R.string.text_download_added_successfully
									showToast(msgId = toastMsgResId)
								}
							}
							aioSettings.numberOfDownloadsUserDid++
							aioSettings.totalNumberOfSuccessfulDownloads++
							aioSettings.updateInStorage()
						} else {
							executeOnMainThread {
								showMessageDialog(
									baseActivityInf = safeBaseActivityRef,
									isNegativeButtonVisible = false,
									messageTextViewCustomize = {
										it.setText(R.string.text_warning_not_enough_space_msg)
									}, positiveButtonTextCustomize = {
										it.setText(R.string.title_okay)
										it.setLeftSideDrawable(R.drawable.ic_button_checked_circle)
									}
								)?.apply {
									setOnClickForPositiveButton {
										this@RegularDownloadPrompter.close()
										close()
									}
								}
							}
						}
					}
				} catch (error: Exception) {
					error.printStackTrace()
					val failedToAddResId = R.string.text_failed_to_add_download_task
					executeOnMain {
						safeBaseActivityRef.doSomeVibration(50)
						showToast(msgId = failedToAddResId)
					}
				}
			}
		})
	}
	
	private suspend fun isFileUrlValid(): Boolean {
		if (extractedVideoLink.isEmpty()) {
			executeOnMain {
				safeMotherActivity?.doSomeVibration(50)
				showToast(msgId = R.string.text_something_went_wrong)
			}; return false
		} else return true
	}
	
	private fun validateOptionalConfigs() {
		downloadModel.videoInfo = null
		downloadModel.videoFormat = null
		downloadModel.thumbnailUrl = videoThumbnailUrl
		downloadModel.siteCookieString = videoCookie ?: ""
		downloadModel.siteReferrer = currentWebUrl ?: ""
	}
	
	private fun validateDownloadDir() {
		val fileCategoryName: String = downloadModel.getUpdatedCategoryName()
		val videoFileDir = fromFile(File(downloadModel.fileDirectory))
		val categoryDocumentFile = videoFileDir.createDirectory(fileCategoryName)
		if (categoryDocumentFile?.canWrite() == true) {
			downloadModel.fileCategoryName = fileCategoryName
		}
		generateDestinationFilePath()?.let { downloadModel.fileDirectory = it }
	}
	
	private fun validateDownloadFileName() {
		val layout = dialogBuilder.view
		val videoTitleView = layout.findViewById<TextView>(R.id.text_video_title)
		val videoTitle = "${videoTitle ?: videoTitleView.text.toString()}.mp4"
		
		downloadModel.fileName = sanitizeFileNameNormal(videoTitle)
		if (!isFileNameValid(downloadModel.fileName)) {
			val sanitizeFileNameExtreme = sanitizeFileNameExtreme(downloadModel.fileName)
			downloadModel.fileName = sanitizeFileNameExtreme
		}; downloadModel.fileName = removeEmptyLines(downloadModel.fileName) ?: ""
	}
	
	private fun generateDestinationFilePath(): String? {
		val finalDirWithCategory = removeDuplicateSlashes(
			"${downloadModel.fileDirectory}/${
				if (downloadModel.fileCategoryName.isNotEmpty()) {
					downloadModel.fileCategoryName + "/"
				} else {
					""
				}
			}"
		)
		return finalDirWithCategory
	}
	
	private fun renameIfFileExistsWithSameName() {
		var index: Int
		val regex = Regex("^(\\d+)_")
		
		while (downloadModel.getDestinationDocumentFile().exists()) {
			val matchResult = regex.find(downloadModel.fileName)
			if (matchResult != null) {
				val currentIndex = matchResult.groupValues[1].toInt()
				downloadModel.fileName = downloadModel.fileName.replaceFirst(regex, "")
				index = currentIndex + 1
			} else {
				index = 1
			}
			downloadModel.fileName = "${index}_${downloadModel.fileName}"
		}
	}
	
	private fun checkStorageSpace(): Boolean {
		val freeSpace = getFreeExternalStorageSpace()
		return freeSpace > downloadModel.fileSize
	}
	
	private fun validateDownloadStartDate() {
		System.currentTimeMillis().apply {
			downloadModel.startTimeDate = this
			downloadModel.startTimeDateInFormat = millisToDateTimeString(this)
		}
	}
}