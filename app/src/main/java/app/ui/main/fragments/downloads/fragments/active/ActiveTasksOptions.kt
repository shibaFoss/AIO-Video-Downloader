package app.ui.main.fragments.downloads.fragments.active

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.downloadSystem
import app.core.engines.downloader.DownloadDataModel
import app.core.engines.downloader.DownloadDataModel.Companion.THUMB_EXTENSION
import app.core.engines.downloader.DownloadStatus.DOWNLOADING
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoFormat
import app.core.engines.video_parser.parsers.VideoFormatsUtils.VideoInfo
import app.ui.main.MotherActivity
import app.ui.main.fragments.downloads.dialogs.DownloadFileRenamer
import app.ui.main.fragments.downloads.dialogs.DownloadInfoTracker
import app.ui.others.media_player.MediaPlayerActivity
import app.ui.others.media_player.MediaPlayerActivity.Companion.STREAM_MEDIA_TITLE
import app.ui.others.media_player.MediaPlayerActivity.Companion.STREAM_MEDIA_URL
import com.aio.R
import com.aio.R.layout
import com.aio.R.string
import com.yausername.youtubedl_android.YoutubeDL.getInstance
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.delay
import lib.device.ShareUtility.shareUrl
import lib.networks.URLUtility.isValidURL
import lib.process.AsyncJobUtils.executeInBackground
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.ThreadsUtility
import lib.texts.ClipboardUtils.copyTextToClipboard
import lib.texts.CommonTextUtils.getText
import lib.ui.ActivityAnimator.animActivitySwipeLeft
import lib.ui.MsgDialogUtils.getMessageDialog
import lib.ui.MsgDialogUtils.showMessageDialog
import lib.ui.ViewUtility.getThumbnailFromFile
import lib.ui.ViewUtility.rotateBitmap
import lib.ui.ViewUtility.saveBitmapToFile
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.builders.DialogBuilder
import lib.ui.builders.ToastView.Companion.showToast
import lib.ui.builders.WaitingDialog
import java.io.File
import java.lang.ref.WeakReference

/**
 * Class that handles options and actions for active download tasks.
 *
 * Provides functionality for:
 * - Managing download tasks (pause/resume/remove/delete)
 * - Viewing/download information
 * - Sharing/copying download URLs
 * - Playing media files
 * - Renaming downloads
 *
 * @param motherActivity The parent activity that hosts these options
 */
class ActiveTasksOptions(private val motherActivity: MotherActivity?) {
	
	// Weak reference to parent activity to prevent memory leaks
	private val safeMotherActivityRef by lazy { WeakReference(motherActivity).get() }
	
	// Dialog builder for showing options
	private val dialogBuilder: DialogBuilder = DialogBuilder(safeMotherActivityRef)
	
	// Current download model being acted upon
	private var downloadDataModel: DownloadDataModel? = null
	
	// Lazy initialized components
	private lateinit var downloadFileRenamer: DownloadFileRenamer
	private lateinit var downloadInfoTracker: DownloadInfoTracker
	
	init {
		initializeDialogViews()
	}
	
	/**
	 * Shows the options dialog for a specific download model
	 * @param downloadModel The download model to show options for
	 */
	fun show(downloadModel: DownloadDataModel) {
		if (!dialogBuilder.isShowing) {
			downloadDataModel = downloadModel
			dialogBuilder.show()
			updateDialogFileInfo(downloadModel)
		}
	}
	
	/**
	 * Closes the options dialog if it's showing
	 */
	fun close() {
		if (dialogBuilder.isShowing) dialogBuilder.close()
	}
	
	/**
	 * Updates the dialog with file information from the download model
	 * @param downloadModel The download model containing file info
	 */
	private fun updateDialogFileInfo(downloadModel: DownloadDataModel) {
		dialogBuilder.view.apply {
			findViewById<TextView>(R.id.text_file_title).apply { isSelected = true; text = downloadModel.fileName }
			findViewById<TextView>(R.id.text_file_url).apply { text = downloadModel.fileURL }
			findViewById<ImageView>(R.id.image_file_thumbnail).apply { updateThumbnail(this, downloadModel) }
		}
	}
	
	/**
	 * Updates the thumbnail image for the download
	 * @param thumbImageView The ImageView to display the thumbnail
	 * @param downloadModel The download model containing thumbnail info
	 */
	private fun updateThumbnail(thumbImageView: ImageView, downloadModel: DownloadDataModel) {
		val destinationFile = downloadModel.getDestinationFile()
		val defaultThumb = downloadModel.getThumbnailDrawableID()
		val defaultThumbDrawable = getDrawable(INSTANCE.resources, defaultThumb, null)
		
		// Try to load APK thumbnail first (returns if successful)
		if (loadApkThumbnail(
				downloadModel = downloadModel,
				imageViewHolder = thumbImageView,
				defaultThumbDrawable = defaultThumbDrawable
			)
		) return
		
		// Load thumbnail in background
		ThreadsUtility.executeInBackground(codeBlock = {
			// Check for cached thumbnail first
			val cachedThumbPath = downloadModel.thumbPath
			if (cachedThumbPath.isNotEmpty()) {
				executeOnMainThread {
					loadBitmapWithGlide(thumbImageView, downloadModel.thumbPath, defaultThumb)
				}; return@executeInBackground
			}
			
			// Generate new thumbnail if no cached version exists
			val thumbnailUrl = downloadModel.videoInfo?.videoThumbnailUrl
			val bitmap = getThumbnailFromFile(destinationFile, thumbnailUrl, 420)
			if (bitmap != null) {
				// Rotate portrait images for better display
				val isPortrait = bitmap.height > bitmap.width
				val rotatedBitmap = if (isPortrait) {
					rotateBitmap(bitmap = bitmap, angle = 270f)
				} else bitmap
				
				// Save thumbnail for future use
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
		})
	}
	
	/**
	 * Loads a bitmap into an ImageView using Glide
	 * @param thumbImageView The ImageView to load into
	 * @param thumbFilePath Path to the thumbnail file
	 * @param defaultThumb Default thumbnail resource if loading fails
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
	 * Loads an APK file's icon as its thumbnail
	 * @param downloadModel The download model containing APK info
	 * @param imageViewHolder The ImageView to display the icon
	 * @param defaultThumbDrawable Default thumbnail if APK icon can't be loaded
	 * @return true if APK icon was loaded successfully, false otherwise
	 */
	private fun loadApkThumbnail(
		downloadModel: DownloadDataModel,
		imageViewHolder: ImageView,
		defaultThumbDrawable: Drawable?
	): Boolean {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			val apkFile = downloadModel.getDestinationFile()
			if (!apkFile.exists() || !apkFile.name.lowercase().endsWith(".apk")) {
				imageViewHolder.setImageDrawable(defaultThumbDrawable)
				return false
			}
			
			// Try to extract APK icon
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
		}; return false
	}
	
	/**
	 * Plays the media file associated with the download
	 * Uses YouTubeDL to extract streamable URL for videos
	 */
	@OptIn(UnstableApi::class)
	private fun playTheMedia() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			close()
			if (downloadDataModel?.videoInfo != null &&
				downloadDataModel?.videoFormat != null
			) {
				// Show loading dialog while preparing stream
				val waitingDialog = WaitingDialog(
					activityInf = safeMotherActivityRef,
					loadingMessage = getText(string.text_preparing_video_please_wait)
				); waitingDialog.show()
				
				// Extract streamable URL in background
				executeInBackground {
					val request = YoutubeDLRequest(downloadDataModel!!.videoInfo!!.videoUrl)
					request.addOption("-f", "best")
					getInstance().getInfo(request).let {
						executeOnMainThread {
							waitingDialog.dialogBuilder?.let { dialogBuilder ->
								if (dialogBuilder.isShowing) {
									waitingDialog.close()
									it.url?.let { videoUrl ->
										openMediaPlayerActivity(
											downloadDataModel!!.videoInfo!!,
											downloadDataModel!!.videoFormat!!,
											videoUrl
										)
									}
								}
							}
						}
					}
				}
				animActivitySwipeLeft(safeMotherActivityRef)
			} else {
				// Fallback for non-video files
				this.close()
				val context = safeMotherActivityRef
				val destinationActivity = MediaPlayerActivity::class.java
				
				context.startActivity(Intent(context, destinationActivity).apply {
					flags = context.getSingleTopIntentFlags()
					downloadDataModel?.let {
						putExtra(STREAM_MEDIA_URL, it.fileURL)
						putExtra(STREAM_MEDIA_TITLE, downloadDataModel!!.fileName)
					}
				})
				
				animActivitySwipeLeft(context)
				this@ActiveTasksOptions.close()
			}
		}
	}
	
	/**
	 * Opens the media player activity with streamable URL
	 * @param videoInfo Video metadata
	 * @param videoFormat Selected video format
	 * @param streamableMediaUrl The streamable media URL
	 */
	@OptIn(UnstableApi::class)
	private fun openMediaPlayerActivity(
		videoInfo: VideoInfo,
		videoFormat: VideoFormat,
		streamableMediaUrl: String
	) {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			val activity = safeMotherActivityRef
			val playerClass = MediaPlayerActivity::class.java
			activity.startActivity(Intent(activity, playerClass).apply {
				flags = activity.getSingleTopIntentFlags()
				putExtra(STREAM_MEDIA_URL, streamableMediaUrl)
				val selectedExtension = videoFormat.formatExtension
				val streamingTitle = "${videoInfo.videoTitle}.$selectedExtension"
				putExtra(STREAM_MEDIA_TITLE, streamingTitle)
			})
		}
	}
	
	/**
	 * Resumes a paused download task
	 * Handles special cases like login-required videos
	 */
	private fun resumeDownloadTask() {
		close()
		downloadDataModel?.let { model ->
			ThreadsUtility.executeInBackground(codeBlock = {
				// If already active, pause first then resume after delay
				if (downloadSystem.searchActiveDownloadTaskWith(model) != null) {
					ThreadsUtility.executeOnMain { pauseDownloadTask() }
					delay(1000)
				}
				
				ThreadsUtility.executeOnMain {
					val condition = model.isYtdlpHavingProblem &&
							model.ytdlpProblemMsg.isNotEmpty() &&
							model.status != DOWNLOADING
					val condition2 = model.ytdlpProblemMsg.contains("login", true)
					
					// Show login prompt for private videos
					if (condition && condition2) {
						showMessageDialog(
							baseActivityInf = safeMotherActivityRef,
							titleTextViewCustomize = {
								it.setText(string.title_login_required)
								safeMotherActivityRef?.getColor(R.color.color_error)
									?.let { colorResId -> it.setTextColor(colorResId) }
							},
							messageTextViewCustomize = {
								it.setText(string.text_login_to_download_private_videos)
							},
							isNegativeButtonVisible = false,
							positiveButtonTextCustomize = {
								it.setText(string.title_login_now)
								it.setLeftSideDrawable(R.drawable.ic_button_login)
							}
						)?.apply {
							setOnClickForPositiveButton {
								close()
								safeMotherActivityRef?.let { safeMotherActivityRef ->
									// Open browser to login page
									val browserFragment = safeMotherActivityRef.browserFragment
									val webviewEngine = browserFragment?.getBrowserWebEngine()
										?: return@setOnClickForPositiveButton
									val sideNavigation = safeMotherActivityRef.sideNavigation
									sideNavigation?.addNewBrowsingTab(model.siteReferrer, webviewEngine)
									safeMotherActivityRef.openBrowserFragment()
									model.isYtdlpHavingProblem = false
									model.ytdlpProblemMsg = ""
								}
							}
						}?.show()
					} else {
						// Normal resume operation
						downloadSystem.resumeDownload(downloadModel = model)
					}
				}
			})
		}
	}
	
	/**
	 * Pauses an active download task
	 * Shows warning if resume is not supported
	 */
	private fun pauseDownloadTask() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			close()
			downloadDataModel?.let { downloadModel ->
				// Check if already paused
				if (downloadSystem.searchActiveDownloadTaskWith(downloadModel) == null) {
					showToast(getText(string.text_download_task_already_paused))
					return
				}
				
				// Show warning if resume not supported
				if (!downloadModel.isResumeSupported) {
					getMessageDialog(
						baseActivityInf = safeMotherActivityRef,
						isNegativeButtonVisible = false,
						messageTextViewCustomize = { it.setText(string.text_warning_resume_not_supported) },
						negativeButtonTextCustomize = { it.setLeftSideDrawable(R.drawable.ic_button_cancel) },
						positiveButtonTextCustomize = {
							it.setText(string.title_pause_anyway)
							it.setLeftSideDrawable(R.drawable.ic_button_media_pause)
						}
					)?.apply {
						setOnClickForPositiveButton {
							this.close()
							dialogBuilder.close()
							downloadSystem.pauseDownload(downloadModel = downloadModel)
						}
						this.show()
					}
				} else {
					// Normal pause operation
					downloadSystem.pauseDownload(downloadModel = downloadModel)
				}
			}
		}
	}
	
	/**
	 * Removes a download task from the list (keeps file)
	 */
	private fun removeDownloadTask() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			downloadDataModel?.let { downloadDataModel ->
				ThreadsUtility.executeInBackground(codeBlock = {
					// If already active, pause first then resume after delay
					if (downloadSystem.searchActiveDownloadTaskWith(downloadDataModel) != null) {
						ThreadsUtility.executeOnMain { pauseDownloadTask() }
						delay(1500)
					}
					
					ThreadsUtility.executeOnMain {
						// Prevent removal of active downloads
						val taskInf = downloadSystem.searchActiveDownloadTaskWith(downloadDataModel)
						if (taskInf != null) {
							showMessageDialog(
								baseActivityInf = safeMotherActivityRef,
								isNegativeButtonVisible = false,
								messageTextViewCustomize = {
									it.setText(string.text_cant_remove_one_active_download)
								},
								positiveButtonTextCustomize = {
									it.setLeftSideDrawable(R.drawable.ic_button_checked_circle)
								}
							); return@executeOnMain
						}
					}
				})
			}
			
			// Confirmation dialog for removal
			getMessageDialog(
				baseActivityInf = safeMotherActivityRef,
				isTitleVisible = true,
				isNegativeButtonVisible = false,
				titleTextViewCustomize =
					{ it.setText(string.title_are_you_sure_about_this) },
				messageTextViewCustomize =
					{ it.setText(string.text_are_you_sure_about_clear) },
				positiveButtonTextCustomize = {
					it.setText(string.title_clear_from_list)
					it.setLeftSideDrawable(R.drawable.ic_button_clear)
				}
			)?.apply {
				setOnClickForPositiveButton {
					close()
					this@ActiveTasksOptions.close()
					downloadDataModel?.let {
						downloadSystem.clearDownload(it) {
							executeOnMainThread {
								showToast(getText(string.title_successful))
							}
						}
					}
				}; show()
			}
		}
	}
	
	/**
	 * Deletes a download task and its file
	 */
	private fun deleteDownloadTask() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			downloadDataModel?.let { downloadDataModel ->
				ThreadsUtility.executeInBackground(codeBlock = {
					// If already active, pause first then resume after delay
					if (downloadSystem.searchActiveDownloadTaskWith(downloadDataModel) != null) {
						ThreadsUtility.executeOnMain { pauseDownloadTask() }
						delay(1500)
					}
					
					ThreadsUtility.executeOnMain {
						// Prevent deletion of active downloads
						val taskInf = downloadSystem.searchActiveDownloadTaskWith(downloadDataModel)
						if (taskInf != null) {
							showMessageDialog(
								baseActivityInf = safeMotherActivityRef,
								isNegativeButtonVisible = false,
								messageTextViewCustomize = { it.setText(string.text_cant_delete_on_active_download) },
								positiveButtonTextCustomize = {
									it.setLeftSideDrawable(R.drawable.ic_button_checked_circle)
								}
							); return@executeOnMain
						}
					}
				})
			}
			
			// Confirmation dialog for deletion
			getMessageDialog(
				baseActivityInf = safeMotherActivityRef,
				titleTextViewCustomize = { it.setText(string.title_are_you_sure_about_this) },
				isTitleVisible = true,
				isNegativeButtonVisible = false,
				messageTextViewCustomize = { it.setText(string.text_are_you_sure_about_delete) },
				positiveButtonTextCustomize = {
					it.setText(string.title_delete_file)
					it.setLeftSideDrawable(R.drawable.ic_button_checked_circle)
				}
			)?.apply {
				setOnClickForPositiveButton {
					close()
					this@ActiveTasksOptions.close()
					downloadDataModel?.let {
						downloadSystem.deleteDownload(it) {
							executeOnMainThread {
								showToast(getText(string.title_successful))
							}
						}
					}
				}; show()
			}
		}
	}
	
	/**
	 * Renames a download task
	 */
	private fun renameDownloadTask() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			// Lazy initialize renamer
			if (!::downloadFileRenamer.isInitialized) {
				downloadFileRenamer = DownloadFileRenamer(safeMotherActivityRef, downloadDataModel!!)
				{ dialogBuilder.close() }
			}
			
			downloadDataModel?.let { downloadDataModel ->
				// Prevent rename of active downloads
				val taskInf = downloadSystem.searchActiveDownloadTaskWith(downloadDataModel)
				if (taskInf != null) {
					showMessageDialog(
						baseActivityInf = safeMotherActivityRef,
						isNegativeButtonVisible = false,
						messageTextViewCustomize = { it.setText(string.text_cant_rename_on_active_download) },
						positiveButtonTextCustomize = {
							it.setLeftSideDrawable(R.drawable.ic_button_checked_circle)
						}
					); return
				}; downloadFileRenamer.show(downloadDataModel)
			}
		}
	}
	
	/**
	 * Copies the download URL to clipboard
	 */
	private fun copyDownloadFileLink() {
		downloadDataModel?.fileURL?.takeIf { isValidURL(it) }?.let { fileUrl ->
			copyTextToClipboard(safeMotherActivityRef, fileUrl)
			showToast(getText(string.text_file_url_has_been_copied))
			close()
		} ?: run {
			showToast(getText(string.text_dont_have_anything_to_copy))
		}
	}
	
	/**
	 * Shares the download URL with other apps
	 */
	private fun shareDownloadFileLink() {
		downloadDataModel?.fileURL?.takeIf { isValidURL(it) }?.let { fileUrl ->
			val titleText = getText(string.text_share_download_file_url)
			shareUrl(safeMotherActivityRef, fileUrl, titleText) { close() }
		} ?: run {
			showToast(getText(string.text_dont_have_anything_to_share))
		}
	}
	
	/**
	 * Opens the download's referrer link in browser
	 */
	private fun openDownloadReferrerLink() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			val downloadSiteReferrerLink = downloadDataModel?.siteReferrer
			if (downloadSiteReferrerLink.isNullOrEmpty()) {
				safeMotherActivityRef.doSomeVibration(50)
				showToast(msgId = string.text_no_referer_link_found)
				return
			}
			this.close()
			this@ActiveTasksOptions.close()
			
			// Open in browser
			val webviewEngine = safeMotherActivityRef.browserFragment?.browserFragmentBody?.webviewEngine!!
			safeMotherActivityRef.sideNavigation?.addNewBrowsingTab(downloadSiteReferrerLink, webviewEngine)
			safeMotherActivityRef.openBrowserFragment()
		}
	}
	
	/**
	 * Shows detailed download information
	 */
	private fun openDownloadInfoTracker() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			// Lazy initialize info tracker
			if (!::downloadInfoTracker.isInitialized) {
				downloadInfoTracker = DownloadInfoTracker(safeMotherActivityRef)
			}
			
			downloadInfoTracker.show(downloadDataModel!!)
			close()
		}
	}
	
	/**
	 * Initializes dialog views and sets up click listeners
	 */
	private fun initializeDialogViews() {
		dialogBuilder.setView(layout.frag_down_3_active_1_onclick_1).view.apply {
			// Map of view IDs to their corresponding actions
			val clickActions = mapOf(
				R.id.button_file_info_card to { openDownloadReferrerLink() },
				R.id.button_resume_download to { resumeDownloadTask() },
				R.id.button_pause_download to { pauseDownloadTask() },
				R.id.button_clear_download to { removeDownloadTask() },
				R.id.button_delete_download to { deleteDownloadTask() },
				R.id.button_rename_download to { renameDownloadTask() },
				R.id.button_copy_download_url to { copyDownloadFileLink() },
				R.id.button_share_download_url to { shareDownloadFileLink() },
				R.id.button_discover_more to { openDownloadReferrerLink() },
				R.id.button_show_download_information to { openDownloadInfoTracker() }
			)
			
			// Set up click listeners for all buttons
			clickActions.forEach { (viewId, action) ->
				findViewById<View>(viewId).setOnClickListener {
					action()
				}
			}
		}
	}
}