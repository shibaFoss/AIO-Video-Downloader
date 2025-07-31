package app.core.engines.downloader

import android.view.View
import android.view.View.GONE
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.net.toUri
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.downloadSystem
import app.core.AIOApp.Companion.internalDataFolder
import app.core.engines.downloader.DownloadDataModel.Companion.THUMB_EXTENSION
import app.core.engines.downloader.DownloadStatus.DOWNLOADING
import com.aio.R
import com.anggrayudi.storage.file.getAbsolutePath
import lib.files.FileSystemUtility.isVideoByName
import lib.process.AsyncJobUtils.executeInBackground
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.texts.CommonTextUtils.getText
import lib.ui.MsgDialogUtils
import lib.ui.ViewUtility.getThumbnailFromFile
import lib.ui.ViewUtility.isBlackThumbnail
import lib.ui.ViewUtility.rotateBitmap
import lib.ui.ViewUtility.saveBitmapToFile
import java.io.File
import java.lang.ref.WeakReference

/**
 * A UI controller class that manages the display and interaction of download items in a list row.
 * Handles all visual aspects of a download item including thumbnails, progress indicators,
 * status messages, and error dialogs.
 *
 * Uses WeakReference to prevent memory leaks from holding strong references to views.
 */
class DownloaderRowUI(private val rowLayout: View) {
	
	// Weak reference to the row layout to prevent memory leaks
	private val safeRowLayoutRef = WeakReference(rowLayout)
	
	// State tracking variables
	private var isShowingAnyDialog = false      // Tracks if an error dialog is currently shown
	private var cachedThumbLoaded = false      // Tracks if thumbnail has been loaded from cache
	private var isThumbnailSettingsChanged = false // Tracks changes to thumbnail visibility setting
	
	// Lazy-initialized view references
	private val mainLayoutRowContainer: View by lazy { rowLayout.findViewById(R.id.container_running_info) }
	private val thumbImageView: ImageView by lazy { rowLayout.findViewById(R.id.img_file_thumbnail) }
	private val statusIndicationImageView: ImageView by lazy { rowLayout.findViewById(R.id.img_status_indicator) }
	private val fileNameTextView: TextView by lazy { rowLayout.findViewById(R.id.txt_file_name) }
	private val statusInfo: TextView by lazy { rowLayout.findViewById(R.id.txt_download_status) }
	
	/**
	 * Main update method that refreshes all UI elements for a download item.
	 * @param downloadModel The DownloadDataModel containing current download state
	 */
	fun updateView(downloadModel: DownloadDataModel) {
		safeRowLayoutRef.get()?.let { safeRowLayoutRef ->
			updateEntireVisibility(downloadModel, safeRowLayoutRef)
			updateFileName(downloadModel)
			updateDownloadProgress(downloadModel)
			updateFileThumbnail(downloadModel)
			updateAlertMessage(downloadModel)
		}
	}
	
	/**
	 * Controls the overall visibility of the row based on download state.
	 * Hides rows for completed, removed, or private folder downloads.
	 */
	private fun updateEntireVisibility(downloadModel: DownloadDataModel, rowLayout: View) {
		if (downloadModel.isRemoved ||
			downloadModel.isComplete ||
			downloadModel.isWentToPrivateFolder
		) {
			if (rowLayout.visibility != GONE) rowLayout.visibility = GONE
		}
	}
	
	/**
	 * Updates the filename display, showing placeholder text if name isn't available yet.
	 */
	private fun updateFileName(downloadModel: DownloadDataModel) {
		fileNameTextView.text = downloadModel.fileName.ifEmpty {
			getText(R.string.text_getting_name_from_server)
		}
	}
	
	/**
	 * Updates all progress-related UI elements.
	 */
	private fun updateDownloadProgress(downloadModel: DownloadDataModel) {
		updateProgressBars(downloadModel)
	}
	
	/**
	 * Updates the progress bar and status text with current download information.
	 * Shows error messages in red if there are yt-dlp problems.
	 */
	private fun updateProgressBars(downloadModel: DownloadDataModel) {
		if (downloadModel.status != DOWNLOADING && downloadModel.ytdlpProblemMsg.isNotEmpty()) {
			statusInfo.text = downloadModel.ytdlpProblemMsg
			statusInfo.setTextColor(INSTANCE.getColor(R.color.color_error))
		} else {
			statusInfo.text = downloadModel.generateDownloadInfoInString()
			statusInfo.setTextColor(INSTANCE.getColor(R.color.color_text_primary))
		}
	}
	
	/**
	 * Updates the file thumbnail based on download state and settings.
	 * Handles both video thumbnails and default icons for other file types.
	 */
	private fun updateFileThumbnail(downloadModel: DownloadDataModel) {
		// Check if thumbnail visibility setting changed
		if (downloadModel.globalSettings.downloadHideVideoThumbnail
			!= isThumbnailSettingsChanged
		) isThumbnailSettingsChanged = true
		
		// Only update thumbnail if not set or settings changed
		if (thumbImageView.tag == null || isThumbnailSettingsChanged) {
			if (downloadModel.globalSettings.downloadHideVideoThumbnail) {
				// Show actual thumbnail if enabled in settings
				thumbImageView.setImageURI(downloadModel.getThumbnailURI())
				thumbImageView.tag = true
				isThumbnailSettingsChanged =
					downloadModel.globalSettings.downloadHideVideoThumbnail
			} else {
				// Show default thumbnail
				updateDefaultThumbnail(downloadModel)
			}
		}
	}
	
	/**
	 * Updates the thumbnail to either a default icon or generates a video thumbnail.
	 */
	private fun updateDefaultThumbnail(downloadModel: DownloadDataModel) {
		// For non-video files or unknown sizes, show default icon
		if (!isVideoByName(downloadModel.fileName) || downloadModel.isUnknownFileSize) {
			showDefaultDownloadThumb(downloadModel)
			thumbImageView.tag = true; return
		}
		
		// For videos with sufficient progress or known thumbnail URLs
		if (downloadModel.progressPercentage > 5 ||
			downloadModel.videoInfo?.videoThumbnailUrl != null ||
			downloadModel.thumbnailUrl.isNotEmpty()
		) {
			executeInBackground {
				if (cachedThumbLoaded) return@executeInBackground
				
				val defaultThumb = downloadModel.getThumbnailDrawableID()
				val cachedThumbPath = downloadModel.thumbPath
				
				// Check for cached thumbnail first
				if (cachedThumbPath.isNotEmpty() && File(cachedThumbPath).exists()) {
					executeOnMainThread {
						loadBitmapToImageView(downloadModel.thumbPath, defaultThumb)
						cachedThumbLoaded = true
					}
					return@executeInBackground
				} else {
					// Determine which file to use for thumbnail generation
					val videoDestinationFile = if (downloadModel.videoInfo != null
						&& downloadModel.videoFormat != null
					) {
						// Handle yt-dlp partial downloads
						val ytdlpId = File(downloadModel.tempYtdlpDestinationFilePath).name
						var destinationFile = File(downloadModel.tempYtdlpDestinationFilePath)
						internalDataFolder.listFiles().forEach { file ->
							try {
								file?.let {
									if (!file.isFile) return@let
									if (file.name!!.startsWith(ytdlpId)
										&& file.name!!.endsWith("part")
									)
										destinationFile = File(file.getAbsolutePath(INSTANCE))
								}
							} catch (error: Exception) {
								error.printStackTrace()
							}
						}; destinationFile
					} else {
						// Regular download file
						downloadModel.getDestinationFile()
					}
					
					// Get thumbnail URL from either video info or download model
					val videoInfoThumbnailUrl = downloadModel.videoInfo?.videoThumbnailUrl
					val downloadModelThumbUrl = downloadModel.thumbnailUrl
					val thumbnailUrl = videoInfoThumbnailUrl ?: downloadModelThumbUrl
					
					// Generate thumbnail bitmap
					val bitmap = getThumbnailFromFile(
						targetFile = videoDestinationFile,
						thumbnailUrl = thumbnailUrl,
						requiredThumbWidth = 420
					)
					
					if (bitmap != null) {
						// Rotate portrait videos to landscape
						val isPortrait = bitmap.height > bitmap.width
						val rotatedBitmap = if (isPortrait) {
							rotateBitmap(bitmap, 270f)
						} else bitmap
						
						// Save and display the thumbnail
						val thumbnailName = "${downloadModel.id}$THUMB_EXTENSION"
						saveBitmapToFile(rotatedBitmap, thumbnailName)?.let { filePath ->
							if (!isBlackThumbnail(File(filePath))) {
								downloadModel.thumbPath = filePath
								downloadModel.updateInStorage()
								executeOnMainThread {
									loadBitmapToImageView(downloadModel.thumbPath, defaultThumb)
								}
							}
						}
					}
				}
			}
		} else {
			// Not enough progress for thumbnail - show default
			showDefaultDownloadThumb(downloadModel)
		}
	}
	
	/**
	 * Loads a thumbnail bitmap into the ImageView, falling back to default icon on error.
	 */
	private fun loadBitmapToImageView(thumbFilePath: String, defaultThumb: Int) {
		try {
			thumbImageView.setImageURI(File(thumbFilePath).toUri())
		} catch (error: Exception) {
			error.printStackTrace()
			thumbImageView.setImageResource(defaultThumb)
		}
	}
	
	/**
	 * Shows the default thumbnail icon based on file type.
	 */
	private fun showDefaultDownloadThumb(downloadModel: DownloadDataModel) {
		thumbImageView.setImageDrawable(
			getDrawable(
				INSTANCE,
				downloadModel.getThumbnailDrawableID()
			)
		)
	}
	
	/**
	 * Shows error messages to the user via dialog if present in the download model.
	 */
	private fun updateAlertMessage(downloadDataModel: DownloadDataModel) {
		if (downloadDataModel.msgToShowUserViaDialog.isNotEmpty()) {
			downloadSystem.downloadsUIManager.activeTasksFragment?.let {
				if (!isShowingAnyDialog) {
					MsgDialogUtils.showMessageDialog(
						baseActivityInf = it.safeBaseActivityRef,
						titleText = getText(R.string.txt_download_failed),
						isTitleVisible = true,
						isCancelable = false,
						messageTxt = downloadDataModel.msgToShowUserViaDialog,
						isNegativeButtonVisible = false,
						dialogBuilderCustomize = { dialogBuilder ->
							isShowingAnyDialog = true
							dialogBuilder.setOnClickForPositiveButton {
								dialogBuilder.close()
								this@DownloaderRowUI.isShowingAnyDialog = false
								downloadDataModel.msgToShowUserViaDialog = ""
								downloadDataModel.updateInStorage()
							}
						}
					)
				}
			}
		}
	}
}