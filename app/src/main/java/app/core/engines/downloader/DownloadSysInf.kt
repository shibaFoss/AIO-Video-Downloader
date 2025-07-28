package app.core.engines.downloader

import app.core.AIOApp
import app.core.AIOApp.Companion.aioSettings
import app.core.engines.downloader.DownloadModelParser.getDownloadDataModels
import app.core.engines.downloader.DownloadStatus.COMPLETE
import com.aio.R
import lib.device.DateTimeUtils.getDaysPassedSince
import lib.process.LogHelperUtils.from
import lib.process.ThreadsUtility
import lib.texts.CommonTextUtils.getText

/**
 * Core interface for managing the download system in the application.
 *
 * Provides functionality for:
 * - Managing active and completed downloads
 * - Controlling download operations (start/pause/resume/delete)
 * - Maintaining download task lists
 * - Synchronizing download states with UI
 * - Automatic cleanup of old downloads
 *
 * The system maintains several collections:
 * - Active downloads (in-progress or paused)
 * - Completed downloads
 * - Currently running tasks
 * - Waiting tasks (queued downloads)
 */
interface DownloadSysInf {
	
	/**
	 * Flag indicating if the download system is currently initializing.
	 */
	var isInitializing: Boolean
	
	/**
	 * Application context reference.
	 */
	val appContext: AIOApp
		get() = AIOApp.INSTANCE
	
	/**
	 * Notification manager for download progress/complete alerts.
	 */
	val downloadNotification: DownloadNotification
	
	/**
	 * List of active (in-progress/paused) download models.
	 */
	val activeDownloadDataModels: ArrayList<DownloadDataModel>
	
	/**
	 * List of successfully completed download models.
	 */
	val finishedDownloadDataModels: ArrayList<DownloadDataModel>
	
	/**
	 * List of currently executing download tasks.
	 */
	val runningDownloadTasks: ArrayList<DownloadTaskInf>
	
	/**
	 * List of queued download tasks waiting to execute.
	 */
	val waitingDownloadTasks: ArrayList<DownloadTaskInf>
	
	/**
	 * Manager for coordinating download state with UI.
	 */
	val downloadsUIManager: DownloadUIManager
	
	/**
	 * Listeners to be notified when downloads complete.
	 */
	var downloadOnFinishListeners: ArrayList<DownloadFinishUIListener>
	
	/**
	 * Adds a new download to the system.
	 * @param downloadModel The download to add
	 * @param onAdded Callback invoked after successful addition
	 */
	fun addDownload(downloadModel: DownloadDataModel, onAdded: () -> Unit = {}) {
		onAdded()
	}
	
	/**
	 * Resumes a paused download.
	 * @param downloadModel The download to resume
	 * @param onResumed Callback invoked after successful resume
	 */
	fun resumeDownload(downloadModel: DownloadDataModel, onResumed: () -> Unit = {}) {
		onResumed()
	}
	
	/**
	 * Pauses an active download.
	 * @param downloadModel The download to pause
	 * @param onPaused Callback invoked after successful pause
	 */
	fun pauseDownload(downloadModel: DownloadDataModel, onPaused: () -> Unit = {}) {
		onPaused()
	}
	
	/**
	 * Clears a download (removes from active lists but keeps files).
	 * @param downloadModel The download to clear
	 * @param onCleared Callback invoked after successful clear
	 */
	fun clearDownload(downloadModel: DownloadDataModel, onCleared: () -> Unit = {}) {
		onCleared()
	}
	
	/**
	 * Deletes a download and its associated files.
	 * @param downloadModel The download to delete
	 * @param onDone Callback invoked after successful deletion
	 */
	fun deleteDownload(downloadModel: DownloadDataModel, onDone: () -> Unit = {}) {
		onDone()
	}
	
	/**
	 * Resumes all paused downloads.
	 */
	fun resumeAllDownloads() {}
	
	/**
	 * Pauses all active downloads.
	 */
	fun pauseAllDownloads() {}
	
	/**
	 * Clears all downloads (keeps files).
	 */
	fun clearAllDownloads() {}
	
	/**
	 * Deletes all downloads and their files.
	 */
	fun deleteAllDownloads() {}
	
	/**
	 * Initializes the download system by loading and syncing existing downloads.
	 */
	fun initSystem() {
		parseDownloadDataModelsAndSync()
	}
	
	/**
	 * Gets the count of currently running download tasks.
	 * @return Number of active downloads
	 */
	fun numberOfRunningTasks(): Int = runningDownloadTasks.size
	
	/**
	 * Checks if a download exists in the running tasks list.
	 * @param downloadModel The download to check
	 * @return true if the download is currently running
	 */
	fun existsInRunningTasksList(downloadModel: DownloadDataModel): Boolean {
		return runningDownloadTasks.any { it.downloadDataModel.id == downloadModel.id }
	}
	
	/**
	 * Checks if a download exists in the waiting tasks list.
	 * @param downloadModel The download to check
	 * @return true if the download is queued but not yet running
	 */
	fun existsInWaitingTasksList(downloadModel: DownloadDataModel): Boolean {
		return waitingDownloadTasks.any { it.downloadDataModel.id == downloadModel.id }
	}
	
	/**
	 * Checks if a download exists in the active downloads list.
	 * @param downloadModel The download to check
	 * @return true if the download is active (running or paused)
	 */
	fun existsInActiveDownloadDataModelsList(downloadModel: DownloadDataModel): Boolean {
		return activeDownloadDataModels.contains(downloadModel)
	}
	
	/**
	 * Finds a download task by its model.
	 * @param downloadModel The download model to search for
	 * @return The matching task if found (either running or waiting), null otherwise
	 */
	fun searchActiveDownloadTaskWith(downloadModel: DownloadDataModel): DownloadTaskInf? {
		return runningDownloadTasks.find { it.downloadDataModel.id == downloadModel.id }
			?: waitingDownloadTasks.find { it.downloadDataModel.id == downloadModel.id }
	}
	
	/**
	 * Checks if a download can be paused (is either running or waiting).
	 * @param downloadModel The download to check
	 * @return true if the download can be paused
	 */
	fun canDownloadTaskBePaused(downloadModel: DownloadDataModel): Boolean {
		return existsInRunningTasksList(downloadModel) || existsInWaitingTasksList(downloadModel)
	}
	
	/**
	 * Loads and synchronizes download models from disk.
	 * Handles automatic cleanup of old downloads based on settings.
	 */
	fun parseDownloadDataModelsAndSync() {
		ThreadsUtility.executeInBackground(codeBlock = {
			isInitializing = true
			getDownloadDataModels().forEach {
				if (isValidCompletedDownloadModel(it)) {
					if (it.globalSettings.downloadAutoRemoveTasks) {
						if (it.globalSettings.downloadAutoRemoveTaskAfterNDays == 0) {
							it.deleteModelFromDisk(); return@forEach
						}
						
						val autoRemoveDaysSettings = aioSettings.downloadAutoRemoveTaskAfterNDays
						if (getDaysPassedSince(it.lastModifiedTimeDate) > autoRemoveDaysSettings) {
							it.deleteModelFromDisk(); return@forEach
						}
					}
					
					it.statusInfo = getText(R.string.text_completed)
					addAndSortFinishedDownloadDataModels(it)
				}
				
				if (isValidActiveDownloadModel(it)) {
					it.statusInfo = getText(R.string.title_paused)
					addAndSortActiveDownloadModelList(it)
				}
			}
			isInitializing = false
		}, errorHandler = {
			isInitializing = false
			error("Error found in parsing download model from disk.")
		})
	}
	
	/**
	 * Adds a download to the active list and sorts the collection.
	 * @param downloadModel The download to add
	 */
	fun addAndSortActiveDownloadModelList(downloadModel: DownloadDataModel) {
		if (!activeDownloadDataModels.contains(downloadModel)) {
			activeDownloadDataModels.add(downloadModel)
		}; sortActiveDownloadDataModels()
	}
	
	/**
	 * Sorts active downloads by start time (newest first).
	 */
	fun sortActiveDownloadDataModels() {
		activeDownloadDataModels.sortByDescending { it.startTimeDate }
	}
	
	/**
	 * Adds a download to the completed list and sorts the collection.
	 * @param downloadModel The download to add
	 */
	fun addAndSortFinishedDownloadDataModels(downloadModel: DownloadDataModel) {
		if (!finishedDownloadDataModels.contains(downloadModel)) {
			finishedDownloadDataModels.add(downloadModel)
		}; sortFinishedDownloadDataModels()
	}
	
	/**
	 * Sorts completed downloads by start time (newest first).
	 */
	fun sortFinishedDownloadDataModels() {
		finishedDownloadDataModels.sortByDescending { it.startTimeDate }
	}
	
	/**
	 * Validates if a download model represents an active (incomplete) download.
	 * @param downloadModel The model to validate
	 * @return true if the model represents a valid active download
	 */
	fun isValidActiveDownloadModel(downloadModel: DownloadDataModel): Boolean {
		val isValidActiveTask = (downloadModel.status != COMPLETE && !downloadModel.isComplete)
		return isValidActiveTask && !downloadModel.isRemoved && !downloadModel.isDeleted
				&& !downloadModel.isWentToPrivateFolder
	}
	
	/**
	 * Validates if a download model represents a successfully completed download.
	 * @param downloadModel The model to validate
	 * @return true if the model represents a valid completed download
	 */
	fun isValidCompletedDownloadModel(downloadModel: DownloadDataModel): Boolean {
		val isValid = (downloadModel.status == COMPLETE || downloadModel.isComplete) &&
				!downloadModel.isWentToPrivateFolder &&
				downloadModel.getDestinationFile().exists()
		val isExisted = downloadModel.getDestinationFile().exists()
		if (!isExisted) downloadModel.deleteModelFromDisk()
		return isValid && isExisted
	}
	
	/**
	 * Logs the current status of a download task.
	 * @param downloadModel The download to log
	 */
	fun logDownloadTaskStatus(downloadModel: DownloadDataModel) {
		from(javaClass).d("Download ${downloadModel.id} status: ${downloadModel.status}")
	}
}