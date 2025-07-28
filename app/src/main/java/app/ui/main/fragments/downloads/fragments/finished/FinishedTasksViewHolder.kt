package app.ui.main.fragments.downloads.fragments.finished

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.net.toUri
import app.core.AIOApp.Companion.INSTANCE
import app.core.engines.downloader.DownloadDataModel
import app.core.engines.downloader.DownloadDataModel.Companion.THUMB_EXTENSION
import com.aio.R
import lib.device.DateTimeUtils.formatLastModifiedDate
import lib.files.FileSizeFormatter.humanReadableSizeOf
import lib.networks.DownloaderUtils.getAudioPlaybackTimeIfAvailable
import lib.process.AsyncJobUtils.executeInBackground
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.ThreadsUtility
import lib.texts.CommonTextUtils.fromHtmlStringToSpanned
import lib.texts.CommonTextUtils.getText
import lib.ui.ViewUtility.getThumbnailFromFile
import lib.ui.ViewUtility.rotateBitmap
import lib.ui.ViewUtility.saveBitmapToFile
import java.io.File

/**
 * ViewHolder class for displaying a finished download item in the list.
 * It handles view initialization, data binding, and thumbnail/image loading.
 */
class FinishedTasksViewHolder(val layout: View) {
	
	// Cache to store already formatted detail info for reuse
	private val detailsCache = mutableMapOf<String, Spanned>()
	
	// UI components
	private val container: RelativeLayout by lazy { layout.findViewById(R.id.button_finish_download_row) }
	private val thumbnail: ImageView by lazy { layout.findViewById(R.id.image_file_thumbnail) }
	private val title: TextView by lazy { layout.findViewById(R.id.text_file_name) }
	private val fileInfo: TextView by lazy { layout.findViewById(R.id.text_file_info) }
	
	/**
	 * Binds the download data and sets up click listeners.
	 */
	fun updateView(
		downloadDataModel: DownloadDataModel,
		onClickItemEvent: FinishedTasksClickEvents
	) {
		showDownloadedFileInfo(downloadDataModel)
		setupItemClickEvents(onClickItemEvent, downloadDataModel)
	}
	
	/**
	 * Displays file information like name, category, size, playback time, and last modified date.
	 * Also initiates thumbnail update.
	 */
	private fun showDownloadedFileInfo(downloadDataModel: DownloadDataModel) {
		title.apply { text = downloadDataModel.fileName }
		ThreadsUtility.executeInBackground(codeBlock = {
			val cacheDetails = detailsCache[downloadDataModel.id.toString()]
			if (cacheDetails != null) {
				executeOnMainThread { fileInfo.text = cacheDetails }
				return@executeInBackground
			}
			
			val category = downloadDataModel.getUpdatedCategoryName(shouldRemoveAIOPrefix = true)
			val fileSize = humanReadableSizeOf(downloadDataModel.fileSize.toDouble())
			val playbackTime = downloadDataModel.mediaFilePlaybackDuration.ifEmpty {
				getAudioPlaybackTimeIfAvailable(downloadDataModel)
			}
			
			// Save playback time if newly fetched
			if (downloadDataModel.mediaFilePlaybackDuration.isEmpty() && playbackTime.isNotEmpty()) {
				downloadDataModel.mediaFilePlaybackDuration = playbackTime
				downloadDataModel.updateInStorage()
			}
			
			val modifyDate = formatLastModifiedDate(downloadDataModel.lastModifiedTimeDate)
			
			// Format and display the file info text
			executeOnMainThread {
				fileInfo.apply {
					val detail = fromHtmlStringToSpanned(
						context.getString(
							R.string.text_b_b_b_date_b,
							getText(R.string.text_info), category.removePrefix("AIO"),
							fileSize, playbackTime, modifyDate
						)
					)
					detailsCache[downloadDataModel.id.toString()] = detail
					fileInfo.text = detail
				}
			}
		})
		
		updateThumbnailInfo(downloadDataModel)
	}
	
	/**
	 * Sets up click and long click listeners on the finished download item.
	 */
	private fun setupItemClickEvents(
		onClick: FinishedTasksClickEvents,
		downloadDataModel: DownloadDataModel
	) {
		container.apply {
			isClickable = true
			setOnClickListener { onClick.onFinishedDownloadClick(downloadDataModel) }
			setOnLongClickListener(View.OnLongClickListener {
				onClick.onFinishedDownloadLongClick(downloadDataModel)
				return@OnLongClickListener true
			})
		}
	}
	
	/**
	 * Determines and sets an appropriate thumbnail for the downloaded file.
	 * Loads APK icons, cached thumbnails, or generates a new thumbnail if needed.
	 */
	private fun updateThumbnailInfo(downloadDataModel: DownloadDataModel) {
		val destinationFile = downloadDataModel.getDestinationFile()
		val defaultThumb = downloadDataModel.getThumbnailDrawableID()
		val defaultThumbDrawable = getDrawable(INSTANCE.resources, defaultThumb, null)
		
		// If APK icon can be used, use it
		val isApkThumbnailFound = loadApkThumbnail(
			downloadDataModel,
			thumbnail,
			defaultThumbDrawable
		)
		
		if (isApkThumbnailFound) return
		
		// Otherwise attempt to use or generate a thumbnail
		executeInBackground {
			val cachedThumbPath = downloadDataModel.thumbPath
			if (cachedThumbPath.isNotEmpty()) {
				executeOnMainThread {
					loadBitmapWithGlide(
						thumbFilePath = downloadDataModel.thumbPath,
						defaultThumb = defaultThumb
					)
				}
				
				return@executeInBackground
			}
			
			val bitmap = getThumbnailFromFile(
				targetFile = destinationFile,
				thumbnailUrl = downloadDataModel.videoInfo?.videoThumbnailUrl,
				requiredThumbWidth = 420
			)
			
			if (bitmap != null) {
				val isPortrait = bitmap.height > bitmap.width
				val rotatedBitmap = if (isPortrait) {
					rotateBitmap(bitmap, 270f)
				} else {
					bitmap
				}
				
				val thumbnailName = "${downloadDataModel.id}$THUMB_EXTENSION"
				saveBitmapToFile(rotatedBitmap, thumbnailName)?.let { filePath ->
					downloadDataModel.thumbPath = filePath
					downloadDataModel.updateInStorage()
					executeOnMainThread {
						loadBitmapWithGlide(
							thumbFilePath = downloadDataModel.thumbPath,
							defaultThumb = defaultThumb
						)
					}
				}
			}
		}
	}
	
	/**
	 * Tries to load a thumbnail image using URI and sets it to the ImageView.
	 * Falls back to default if loading fails.
	 */
	private fun loadBitmapWithGlide(thumbFilePath: String, defaultThumb: Int) {
		try {
			val imgURI = File(thumbFilePath).toUri()
			thumbnail.setImageURI(imgURI)
		} catch (error: Exception) {
			error.printStackTrace()
			thumbnail.setImageResource(defaultThumb)
		}
	}
	
	/**
	 * Loads the icon of an APK file as a thumbnail if the file is an APK.
	 *
	 * @return true if APK icon was successfully set, false otherwise.
	 */
	private fun loadApkThumbnail(
		downloadDataModel: DownloadDataModel,
		imageViewHolder: ImageView,
		defaultThumbDrawable: Drawable?
	): Boolean {
		val apkFile = downloadDataModel.getDestinationFile()
		if (!apkFile.exists() || !apkFile.name.lowercase().endsWith(".apk")) {
			imageViewHolder.setImageDrawable(defaultThumbDrawable)
			return false
		}
		
		val packageManager: PackageManager = layout.context.packageManager
		return try {
			val getActivities = PackageManager.GET_ACTIVITIES
			val apkFileAbsolutePath = apkFile.absolutePath
			val packageInfo: PackageInfo? =
				packageManager.getPackageArchiveInfo(apkFileAbsolutePath, getActivities)
			
			packageInfo?.applicationInfo?.let { appInfo ->
				appInfo.sourceDir = apkFileAbsolutePath
				appInfo.publicSourceDir = apkFileAbsolutePath
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