package app.core.engines.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat.Builder
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.documentfile.provider.DocumentFile
import androidx.documentfile.provider.DocumentFile.fromFile
import androidx.media3.common.util.UnstableApi
import app.core.AIOApp.Companion.INSTANCE
import app.core.engines.downloader.DownloadDataModel.Companion.DOWNLOAD_MODEL_ID_KEY
import app.ui.main.MotherActivity
import app.ui.others.media_player.MediaPlayerActivity
import app.ui.others.media_player.MediaPlayerActivity.Companion.FROM_FINISHED_DOWNLOADS_LIST
import app.ui.others.media_player.MediaPlayerActivity.Companion.PLAY_MEDIA_FILE_PATH
import com.aio.R
import lib.files.FileUtility.isAudio
import lib.files.FileUtility.isVideo
import lib.texts.CommonTextUtils.getText
import java.io.File

/**
 * Handles download progress and completion notifications for the application.
 *
 * This class provides:
 * - Progress notifications during active downloads
 * - Completion notifications when downloads finish
 * - Proper notification channel setup for Android 8.0+
 * - Context-aware notification actions (opens appropriate activity)
 * - Media-type detection for proper intent routing
 *
 * Notifications can be:
 * - Updated with current progress
 * - Cancelled when downloads are removed
 * - Suppressed based on user preferences
 */
class DownloadNotification {
	
	private lateinit var notificationManager: NotificationManager
	
	companion object {
		/** Notification channel ID for download notifications */
		const val CHANNEL_ID = "Download_Notification_Channel"
		
		/** User-visible name for the notification channel */
		const val CHANNEL_NAME = "Download Notifications"
		
		/** Intent extra key for tracking notification source */
		const val WHERE_DID_YOU_COME_FROM = "WHERE_DID_YOU_COME_FROM"
		
		/** Value indicating the notification came from download completion */
		const val FROM_DOWNLOAD_NOTIFICATION = 4
	}
	
	init {
		createNotificationChannelForSystem()
	}
	
	/**
	 * Updates or cancels the notification based on download state.
	 *
	 * @param downloadDataModel The download model containing current state
	 */
	fun updateNotification(downloadDataModel: DownloadDataModel) {
		if (shouldStopNotifying(downloadDataModel)) return
		if (shouldCancelNotification(downloadDataModel)) {
			notificationManager.cancel(downloadDataModel.id)
			return
		}; updateDownloadProgress(downloadDataModel)
	}
	
	/**
	 * Creates or updates a progress notification for the download.
	 *
	 * @param downloadDataModel The download model containing current progress
	 */
	private fun updateDownloadProgress(downloadDataModel: DownloadDataModel) {
		val notificationId = downloadDataModel.id
		val notificationBuilder = Builder(INSTANCE, CHANNEL_ID)
		
		notificationBuilder
			.setContentTitle(downloadDataModel.fileName)
			.setContentText(getContentTextByStatus(downloadDataModel))
			.setSmallIcon(R.drawable.ic_launcher_logo_v4)
			.setPriority(PRIORITY_LOW)
			.setAutoCancel(true)
			.setContentIntent(
				createNotificationPendingIntent(
					isDownloadCompleted = downloadDataModel.isComplete,
					downloadModel = downloadDataModel
				)
			)
		
		notificationManager.notify(notificationId, notificationBuilder.build())
	}
	
	/**
	 * Generates appropriate notification text based on download status.
	 *
	 * @param downloadDataModel The download model to evaluate
	 * @return The appropriate status message text
	 */
	private fun getContentTextByStatus(downloadDataModel: DownloadDataModel): String {
		val completedText = getText(R.string.text_download_complete_click_to_open)
		val pausedText = getText(R.string.text_download_has_been_paused)
		val runningText = downloadDataModel.generateDownloadInfoInString()
		
		return when {
			downloadDataModel.isComplete -> completedText
			downloadDataModel.isRunning -> runningText
			else -> if (downloadDataModel.statusInfo.contains(
					getText(R.string.title_paused), true
				)
			) pausedText
			else downloadDataModel.statusInfo
		}
	}
	
	/**
	 * Checks if notifications should be suppressed for this download.
	 *
	 * @param downloadModel The download model to check
	 * @return true if notifications should be hidden, false otherwise
	 */
	private fun shouldStopNotifying(downloadModel: DownloadDataModel): Boolean {
		return downloadModel.globalSettings.downloadHideNotification
	}
	
	/**
	 * Determines if the notification should be cancelled.
	 *
	 * @param downloadModel The download model to check
	 * @return true if the download was removed/deleted, false otherwise
	 */
	private fun shouldCancelNotification(downloadModel: DownloadDataModel): Boolean {
		return downloadModel.isRemoved || downloadModel.isDeleted
	}
	
	/**
	 * Creates a pending intent appropriate for the download state.
	 *
	 * @param isDownloadCompleted Whether the download has finished
	 * @param downloadModel The associated download model
	 * @return Configured PendingIntent
	 */
	@OptIn(UnstableApi::class)
	private fun createNotificationPendingIntent(
		isDownloadCompleted: Boolean = false,
		downloadModel: DownloadDataModel
	): PendingIntent {
		return if (isDownloadCompleted) {
			getActivity(
				INSTANCE, 0, generatePendingIntent(downloadModel),
				FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
			)
		} else {
			getActivity(
				INSTANCE, 0, Intent(INSTANCE, MotherActivity::class.java),
				FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
			)
		}
	}
	
	/**
	 * Generates an intent based on the downloaded file type.
	 *
	 * @param downloadModel The completed download model
	 * @return Intent configured to open the appropriate activity
	 */
	@OptIn(UnstableApi::class)
	private fun generatePendingIntent(downloadModel: DownloadDataModel): Intent {
		val destFile = File("${downloadModel.fileDirectory}/${downloadModel.fileName}")
		val downloadFile = fromFile(destFile)
		
		return Intent(INSTANCE, getCorrespondingActivity(downloadFile)).apply {
			flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
			putExtra(DOWNLOAD_MODEL_ID_KEY, downloadModel.id)
			putExtra(PLAY_MEDIA_FILE_PATH, true)
			putExtra(WHERE_DID_YOU_COME_FROM, FROM_FINISHED_DOWNLOADS_LIST)
			putExtra(WHERE_DID_YOU_COME_FROM, FROM_DOWNLOAD_NOTIFICATION)
		}
	}
	
	/**
	 * Determines the appropriate activity class based on file type.
	 *
	 * @param downloadedFile The downloaded file to evaluate
	 * @return MediaPlayerActivity for media files, MotherActivity otherwise
	 */
	@OptIn(UnstableApi::class)
	private fun getCorrespondingActivity(downloadedFile: DocumentFile) =
		if (isVideo(downloadedFile) || isAudio(downloadedFile))
			MediaPlayerActivity::class.java else MotherActivity::class.java
	
	/**
	 * Creates the notification channel required for Android 8.0+.
	 */
	private fun createNotificationChannelForSystem() {
		val notificationChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_LOW)
		val nm = INSTANCE.getSystemService(NOTIFICATION_SERVICE)
		notificationManager = nm as NotificationManager
		notificationManager.createNotificationChannel(notificationChannel)
	}
}