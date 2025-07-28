package app.ui.main.fragments.downloads.fragments.finished

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioBackend
import app.core.AIOApp.Companion.downloadSystem
import app.core.engines.downloader.DownloadDataModel
import app.core.engines.downloader.DownloadDataModel.Companion.DOWNLOAD_MODEL_ID_KEY
import app.core.engines.downloader.DownloadDataModel.Companion.THUMB_EXTENSION
import app.ui.main.fragments.downloads.dialogs.DownloadFileRenamer
import app.ui.main.fragments.downloads.dialogs.DownloadInfoTracker
import app.ui.others.media_player.MediaPlayerActivity
import app.ui.others.media_player.MediaPlayerActivity.Companion.FROM_FINISHED_DOWNLOADS_LIST
import app.ui.others.media_player.MediaPlayerActivity.Companion.PLAY_MEDIA_FILE_PATH
import app.ui.others.media_player.MediaPlayerActivity.Companion.WHERE_DID_YOU_COME_FROM
import com.aio.R
import lib.device.ShareUtility.openApkFile
import lib.device.ShareUtility.openFile
import lib.device.ShareUtility.shareMediaFile
import lib.files.FileUtility.endsWithExtension
import lib.files.FileUtility.isAudioByName
import lib.files.FileUtility.isVideoByName
import lib.process.AsyncJobUtils.executeInBackground
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.process.CommonTimeUtils.delay
import lib.texts.CommonTextUtils.getText
import lib.ui.ActivityAnimator.animActivityFade
import lib.ui.MsgDialogUtils
import lib.ui.MsgDialogUtils.getMessageDialog
import lib.ui.ViewUtility
import lib.ui.ViewUtility.getThumbnailFromFile
import lib.ui.ViewUtility.rotateBitmap
import lib.ui.ViewUtility.saveBitmapToFile
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.builders.DialogBuilder
import lib.ui.builders.ToastView.Companion.showToast
import java.io.File

/**
 * A class that handles showing and managing options for finished downloads.
 * Provides functionality like playing media, opening files, sharing, deleting, renaming, etc.
 *
 * @param finishedTasksFragment The parent fragment that contains the finished downloads list
 */
class FinishedDownloadOptions(finishedTasksFragment: FinishedTasksFragment?) : OnClickListener {
	
	// Safe references to avoid memory leaks
	private val safeFinishedTasksFragmentRef = finishedTasksFragment?.safeFinishTasksFragment
	private val safeMotherActivityRef = safeFinishedTasksFragmentRef?.safeMotherActivityRef
	
	// Lazy initialization of dialog builder
	private val dialogBuilder: DialogBuilder? by lazy { DialogBuilder(safeMotherActivityRef) }
	
	// Components for file operations
	private lateinit var downloadFileRenamer: DownloadFileRenamer
	private lateinit var downloadInfoTracker: DownloadInfoTracker
	private var downloadDataModel: DownloadDataModel? = null
	
	init {
		// Initialize dialog view and set click listeners for all option buttons
		dialogBuilder?.let { dialogBuilder ->
			dialogBuilder.setView(R.layout.frag_down_4_finish_1_onclick_1)
			ViewUtility.setViewOnClickListener(
				onClickListener = this,
				layout = dialogBuilder.view,
				ids = listOf(
					R.id.button_file_info_card,
					R.id.button_open_download_file,
					R.id.button_share_download_file,
					R.id.button_clear_download,
					R.id.button_delete_download,
					R.id.button_rename_download,
					R.id.button_discover_more,
					R.id.button_show_download_information
				).toIntArray()
			)
		}
	}
	
	/**
	 * Shows the options dialog for a specific download.
	 * @param dataModel The download data model to show options for
	 */
	fun show(dataModel: DownloadDataModel) {
		safeFinishedTasksFragmentRef?.let { _ ->
			safeMotherActivityRef?.let { _ ->
				dialogBuilder?.let { dialogBuilder ->
					if (!dialogBuilder.isShowing) {
						setDownloadModel(dataModel)
						updateTitleAndThumbnails(dataModel)
						dialogBuilder.show()
					}
				}
			}
		}
	}
	
	/**
	 * Sets the current download model to operate on.
	 * @param model The download data model
	 */
	fun setDownloadModel(model: DownloadDataModel) {
		this.downloadDataModel = model
	}
	
	/**
	 * Closes the options dialog if it's showing.
	 */
	fun close() {
		dialogBuilder?.let { dialogBuilder ->
			if (dialogBuilder.isShowing) dialogBuilder.close()
		}
	}
	
	/**
	 * Handles click events for all option buttons.
	 */
	override fun onClick(view: View?) {
		view?.let {
			when (view.id) {
				R.id.button_file_info_card -> playTheMedia()
				R.id.button_open_download_file -> openFile()
				R.id.button_share_download_file -> shareFile()
				R.id.button_clear_download -> clearFromList()
				R.id.button_delete_download -> deleteFile()
				R.id.button_rename_download -> renameFile()
				R.id.button_discover_more -> discoverMore()
				R.id.button_show_download_information -> downloadInfo()
			}
		}
	}
	
	/**
	 * Updates the dialog's title and thumbnail views with download information.
	 * @param downloadModel The download data model containing the information to display
	 */
	private fun updateTitleAndThumbnails(downloadModel: DownloadDataModel) {
		dialogBuilder?.let { dialogBuilder ->
			dialogBuilder.view.apply {
				findViewById<TextView>(R.id.text_file_url).apply {
					text = downloadModel.fileURL
				}
				
				findViewById<TextView>(R.id.text_file_title).apply {
					isSelected = true
					text = downloadModel.fileName
				}
				
				findViewById<ImageView>(R.id.image_file_thumbnail).apply {
					updateThumbnail(this, downloadModel)
				}
			}
		}
	}
	
	/**
	 * Updates the thumbnail image for the download.
	 * @param thumbImageView The ImageView to display the thumbnail in
	 * @param downloadModel The download data model containing thumbnail information
	 */
	private fun updateThumbnail(thumbImageView: ImageView, downloadModel: DownloadDataModel) {
		val destinationFile = downloadModel.getDestinationFile()
		val defaultThumb = downloadModel.getThumbnailDrawableID()
		val defaultThumbDrawable = getDrawable(INSTANCE.resources, defaultThumb, null)
		
		// Try to load APK thumbnail first (returns if successful)
		if (loadApkThumbnail(downloadModel, thumbImageView, defaultThumbDrawable)) return
		
		// Load thumbnail in background to avoid UI freezing
		executeInBackground {
			// Check if we have a cached thumbnail path
			val cachedThumbPath = downloadModel.thumbPath
			if (cachedThumbPath.isNotEmpty()) {
				executeOnMainThread {
					loadBitmapWithGlide(thumbImageView, downloadModel.thumbPath, defaultThumb)
				}; return@executeInBackground
			}
			
			// Generate thumbnail from file or URL
			val thumbnailUrl = downloadModel.videoInfo?.videoThumbnailUrl
			val bitmap = getThumbnailFromFile(destinationFile, thumbnailUrl, 420)
			if (bitmap != null) {
				// Rotate portrait images to landscape for better display
				val isPortrait = bitmap.height > bitmap.width
				val rotatedBitmap = if (isPortrait) {
					rotateBitmap(bitmap, 270f)
				} else bitmap
				
				// Save thumbnail to file for caching
				val thumbnailName = "${downloadModel.id}$THUMB_EXTENSION"
				saveBitmapToFile(rotatedBitmap, thumbnailName)?.let { filePath ->
					downloadModel.thumbPath = filePath
					downloadModel.updateInStorage()
					executeOnMainThread {
						loadBitmapWithGlide(
							thumbImageView = thumbImageView,
							thumbFilePath = downloadModel.thumbPath,
							defaultThumb = defaultThumb
						)
					}
				}
			}
		}
	}
	
	/**
	 * Loads a thumbnail image into an ImageView using Glide.
	 * @param thumbImageView The ImageView to load the thumbnail into
	 * @param thumbFilePath The path to the thumbnail file
	 * @param defaultThumb The default thumbnail resource ID to use if loading fails
	 */
	private fun loadBitmapWithGlide(
		thumbImageView: ImageView,
		thumbFilePath: String,
		defaultThumb: Int
	) {
		try {
			val imgURI = File(thumbFilePath).toUri()
			thumbImageView.setImageURI(imgURI)
		} catch (error: Exception) {
			error.printStackTrace()
			thumbImageView.setImageResource(defaultThumb)
		}
	}
	
	/**
	 * Loads an APK file's icon as its thumbnail.
	 * @param downloadModel The download data model containing APK information
	 * @param imageViewHolder The ImageView to display the APK icon
	 * @param defaultThumbDrawable The default thumbnail to use if APK icon can't be loaded
	 * @return true if APK icon was loaded successfully, false otherwise
	 */
	private fun loadApkThumbnail(
		downloadModel: DownloadDataModel,
		imageViewHolder: ImageView,
		defaultThumbDrawable: Drawable?
	): Boolean {
		safeFinishedTasksFragmentRef?.let { _ ->
			safeMotherActivityRef?.let { safeMotherActivityRef ->
				dialogBuilder?.let { _ ->
					val apkFile = downloadModel.getDestinationFile()
					if (!apkFile.exists() || !apkFile.name.lowercase().endsWith(".apk")) {
						imageViewHolder.setImageDrawable(defaultThumbDrawable)
						return false
					}
					
					val packageManager: PackageManager = safeMotherActivityRef.packageManager
					return try {
						val packageInfo: PackageInfo? = packageManager.getPackageArchiveInfo(
							apkFile.absolutePath, PackageManager.GET_ACTIVITIES
						)
						packageInfo?.applicationInfo?.let { appInfo ->
							appInfo.sourceDir = apkFile.absolutePath
							appInfo.publicSourceDir = apkFile.absolutePath
							val icon: Drawable = appInfo.loadIcon(packageManager)
							imageViewHolder.setImageDrawable(icon)
							true
						} ?: false
					} catch (error: Exception) {
						error.printStackTrace()
						imageViewHolder.apply {
							scaleType = ImageView.ScaleType.FIT_CENTER
							setPadding(0, 0, 0, 0)
							setImageDrawable(defaultThumbDrawable)
						}
						false
					}
				}
			}
		} ?: run { return false }
	}
	
	/**
	 * Plays the media file associated with the download.
	 * Uses MediaPlayerActivity for audio/video, falls back to openFile() for other types.
	 */
	@OptIn(UnstableApi::class)
	fun playTheMedia() {
		safeFinishedTasksFragmentRef?.let { _ ->
			safeMotherActivityRef?.let { safeMotherActivityRef ->
				dialogBuilder?.let { _ ->
					downloadDataModel?.let {
						if (isAudioByName(it.fileName) || isVideoByName(it.fileName) ){
							// Start media player activity for audio/video files
							safeMotherActivityRef.startActivity(
								Intent(safeMotherActivityRef, MediaPlayerActivity::class.java).apply {
									flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
									downloadDataModel?.let {
										putExtra(DOWNLOAD_MODEL_ID_KEY, downloadDataModel!!.id)
										putExtra(PLAY_MEDIA_FILE_PATH, true)
										putExtra(WHERE_DID_YOU_COME_FROM, FROM_FINISHED_DOWNLOADS_LIST)
									}
								})
							animActivityFade(safeMotherActivityRef)
							close()
						} else {
							// For non-media files, just open them
							openFile()
						}
						
						// Track media play button click
						aioBackend.updateClickCountOnMediaPlayButton()
					}
				}
			}
		}
	}
	
	/**
	 * Opens the downloaded file using appropriate application.
	 * Handles APK files specially with proper installation flow.
	 */
	private fun openFile() {
		safeFinishedTasksFragmentRef?.let { _ ->
			safeMotherActivityRef?.let { safeActivityRef ->
				dialogBuilder?.let { _ ->
					close()
					val extensions = listOf("apk").toTypedArray()
					if (endsWithExtension(downloadDataModel!!.fileName, extensions)) {
						// Special handling for APK files
						val authority = "${safeActivityRef.packageName}.provider"
						val apkFile = downloadDataModel!!.getDestinationFile()
						openApkFile(safeActivityRef, apkFile, authority)
					} else {
						// Open other file types normally
						openFile(downloadDataModel!!.getDestinationFile(), safeActivityRef)
					}
				}
			}
		}
	}
	
	/**
	 * Shares the downloaded file with other applications.
	 */
	private fun shareFile() {
		safeFinishedTasksFragmentRef?.let { _ ->
			safeMotherActivityRef?.let { safeMotherActivityRef ->
				close()
				shareMediaFile(
					context = safeMotherActivityRef,
					file = downloadDataModel!!.getDestinationFile()
				)
			}
		}
	}
	
	/**
	 * Clears the download from the list without deleting the file.
	 */
	private fun clearFromList() {
		safeFinishedTasksFragmentRef?.let { _ ->
			safeMotherActivityRef?.let { safeMotherActivityRef ->
				getMessageDialog(
					baseActivityInf = safeMotherActivityRef,
					isTitleVisible = true,
					isNegativeButtonVisible = false,
					titleTextViewCustomize =
						{ it.setText(R.string.title_are_you_sure_about_this) },
					messageTextViewCustomize =
						{ it.setText(R.string.text_are_you_sure_about_clear) },
					positiveButtonTextCustomize = {
						it.setText(R.string.title_clear_from_list)
						it.setLeftSideDrawable(R.drawable.ic_button_clear)
					}
				)?.apply {
					setOnClickForPositiveButton {
						close()
						this@FinishedDownloadOptions.close()
						// Remove from storage and list
						downloadDataModel?.deleteModelFromDisk()
						downloadSystem.finishedDownloadDataModels.remove(downloadDataModel!!)
						showToast(getText(R.string.title_successful))
					}; show()
				}
			}
		}
	}
	
	/**
	 * Deletes the downloaded file from storage and removes it from the list.
	 */
	private fun deleteFile() {
		safeFinishedTasksFragmentRef?.let { _ ->
			safeMotherActivityRef?.let { safeMotherActivityRef ->
				getMessageDialog(
					baseActivityInf = safeMotherActivityRef,
					isTitleVisible = true,
					isNegativeButtonVisible = false,
					titleTextViewCustomize =
						{ it.setText(R.string.title_are_you_sure_about_this) },
					messageTextViewCustomize =
						{ it.setText(R.string.text_are_you_sure_about_delete) },
					positiveButtonTextCustomize = {
						it.setText(R.string.title_delete_file)
						it.setLeftSideDrawable(R.drawable.ic_button_delete)
					}
				)?.apply {
					setOnClickForPositiveButton {
						close()
						this@FinishedDownloadOptions.close()
						// Delete in background to avoid UI freezing
						executeInBackground {
							downloadDataModel?.deleteModelFromDisk()
							downloadDataModel?.getDestinationFile()?.delete()
							downloadSystem.finishedDownloadDataModels.remove(downloadDataModel!!)
							executeOnMainThread {
								showToast(getText(R.string.title_successful))
							}
						}
					}; show()
				}
			}
		}
	}
	
	/**
	 * Shows a dialog to rename the downloaded file.
	 */
	private fun renameFile() {
		safeFinishedTasksFragmentRef?.let { safeFinishedDownloadFragmentRef ->
			safeMotherActivityRef?.let { safeMotherActivityRef ->
				if (!::downloadFileRenamer.isInitialized) {
					// Initialize file renamer if not already done
					downloadFileRenamer =
						DownloadFileRenamer(safeMotherActivityRef, downloadDataModel!!) {
							// Callback after successful rename
							executeOnMainThread {
								dialogBuilder?.close()
								delay(300, object : OnTaskFinishListener {
									override fun afterDelay() = safeFinishedDownloadFragmentRef
										.finishedTasksListAdapter.notifyDataSetChangedOnSort(true)
								})
							}
						}
				}
				
				// Show rename dialog with current model
				downloadFileRenamer.downloadDataModel = downloadDataModel!!
				downloadFileRenamer.show(downloadDataModel!!)
			}
		}
	}
	
	/**
	 * Opens the associated webpage for the download in browser.
	 * Shows warning if no referrer link is available.
	 */
	private fun discoverMore() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			val siteReferrerLink = downloadDataModel!!.siteReferrer
			if (siteReferrerLink.isEmpty()) {
				close()
				safeMotherActivityRef.doSomeVibration(20)
				val msgTxt = getText(R.string.text_missing_webpage_link_info)
				MsgDialogUtils.showMessageDialog(
					baseActivityInf = safeMotherActivityRef,
					titleText = getText(R.string.text_missing_associate_webpage),
					isTitleVisible = true,
					messageTxt = msgTxt,
					isNegativeButtonVisible = false
				); return
			}
			
			val referrerLink = downloadDataModel?.siteReferrer
			val browserFragment = safeMotherActivityRef.browserFragment
			val webviewEngine = browserFragment?.browserFragmentBody?.webviewEngine!!
			
			if (referrerLink.isNullOrEmpty()) {
				// Fallback to download URL if referrer is missing
				getMessageDialog(
					baseActivityInf = safeMotherActivityRef,
					isTitleVisible = true,
					titleText = getText(R.string.title_no_referral_site_added),
					messageTxt = getText(R.string.text_no_referrer_message_warning),
					positiveButtonText = getText(R.string.title_open_download_url),
					negativeButtonText = getText(R.string.title_cancel)
				)?.apply {
					setOnClickForPositiveButton {
						val fileUrl = downloadDataModel!!.fileURL
						this.close()
						this@FinishedDownloadOptions.close()
						safeMotherActivityRef.sideNavigation
							?.addNewBrowsingTab(fileUrl, webviewEngine)
						safeMotherActivityRef.openBrowserFragment()
					}
				}?.show(); return
			}
			
			this.close()
			this@FinishedDownloadOptions.close()
			
			// Open the referrer link in browser
			safeMotherActivityRef.sideNavigation
				?.addNewBrowsingTab(referrerLink, webviewEngine)
			safeMotherActivityRef.openBrowserFragment()
		}
	}
	
	/**
	 * Shows detailed information about the download.
	 */
	private fun downloadInfo() {
		safeFinishedTasksFragmentRef?.let { _ ->
			safeMotherActivityRef?.let { safeMotherActivityRef ->
				close()
				if (!::downloadInfoTracker.isInitialized)
					downloadInfoTracker = DownloadInfoTracker(safeMotherActivityRef)
				downloadInfoTracker.show(downloadDataModel!!)
			}
		}
	}
}