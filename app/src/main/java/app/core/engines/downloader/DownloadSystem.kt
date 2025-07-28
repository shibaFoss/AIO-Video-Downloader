package app.core.engines.downloader

import app.core.AIOApp.Companion.aioBackend
import app.core.AIOApp.Companion.aioSettings
import app.core.AIOApp.Companion.aioTimer
import app.core.AIOApp.Companion.idleForegroundService
import app.core.AIOTimer.AIOTimerListener
import app.core.engines.downloader.DownloadStatus.CLOSE
import app.core.engines.downloader.DownloadStatus.COMPLETE
import app.core.engines.downloader.DownloadStatus.DOWNLOADING
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lib.files.FileUtility.updateMediaStore
import lib.process.ThreadsUtility

/**
 * Core implementation of the download management system.
 *
 * This class handles:
 * - Download task lifecycle (add/start/pause/resume/delete)
 * - Parallel download management
 * - Task queue management
 * - Status updates and notifications
 * - UI coordination
 * - Automatic cleanup of completed downloads
 *
 * Implements both DownloadSysInf interface for system operations and
 * DownloadTaskListener for task status updates.
 */
class DownloadSystem : AIOTimerListener, DownloadSysInf, DownloadTaskListener {
	
	// System state flags
	override var isInitializing: Boolean = false
	
	// Notification handler
	override val downloadNotification: DownloadNotification = DownloadNotification()
	
	// Data collections
	override val activeDownloadDataModels: ArrayList<DownloadDataModel> = ArrayList()
	override val finishedDownloadDataModels: ArrayList<DownloadDataModel> = ArrayList()
	
	override val runningDownloadTasks: ArrayList<DownloadTaskInf> = ArrayList()
	override val waitingDownloadTasks: ArrayList<DownloadTaskInf> = ArrayList()
	
	// UI coordination
	override val downloadsUIManager: DownloadUIManager = DownloadUIManager(this)
	override var downloadOnFinishListeners: ArrayList<DownloadFinishUIListener> = ArrayList()
	
	init {
		initSystem()
		aioTimer.register(this)
	}
	
	/**
	 * Timer callback that processes pending tasks and updates foreground service.
	 * @param loopCount Current timer iteration count
	 */
	override fun onAIOTimerTick(loopCount: Double) {
		CoroutineScope(Dispatchers.IO).launch {
			startPendingTasksFromWaitingList()
			idleForegroundService.updateService()
		}
	}
	
	/**
	 * Adds a new download to the system.
	 * @param downloadModel Download to add
	 * @param onAdded Callback invoked after successful addition
	 */
	override fun addDownload(downloadModel: DownloadDataModel, onAdded: () -> Unit) {
		ThreadsUtility.executeInBackground(codeBlock = {
			if (existsInActiveDownloadDataModelsList(downloadModel)) {
				resumeDownload(downloadModel)
				return@executeInBackground
			}
			
			generateDownloadTask(downloadModel).let { downloadTask ->
				if (!waitingDownloadTasks.contains(downloadTask)) {
					waitingDownloadTasks.add(downloadTask)
					addAndSortActiveDownloadModelList(downloadModel)
					
					downloadsUIManager.addNewActiveUI(
						downloadModel,
						activeDownloadDataModels.indexOf(downloadModel)
					)
					onAdded()
				}
			}
		})
	}
	
	/**
	 * Resumes a paused download.
	 * @param downloadModel Download to resume
	 * @param onResumed Callback invoked after successful resume
	 */
	override fun resumeDownload(downloadModel: DownloadDataModel, onResumed: () -> Unit) {
		ThreadsUtility.executeInBackground(codeBlock = {
			generateDownloadTask(downloadModel).let { downloadTask ->
				if (!waitingDownloadTasks.contains(downloadTask)) {
					waitingDownloadTasks.add(downloadTask)
					downloadsUIManager.updateActiveUI(downloadModel)
					onResumed()
				}
			}
		})
	}
	
	/**
	 * Pauses an active download.
	 * @param downloadModel Download to pause
	 * @param onPaused Callback invoked after successful pause
	 */
	override fun pauseDownload(downloadModel: DownloadDataModel, onPaused: () -> Unit) {
		CoroutineScope(Dispatchers.IO).launch {
			if (canDownloadTaskBePaused(downloadModel)) {
				val resultedTask = searchActiveDownloadTaskWith(downloadModel)
				
				resultedTask?.let {
					it.cancelDownload()
					runningDownloadTasks.remove(it)
					waitingDownloadTasks.remove(it)
					onPaused()
				}
				
				downloadsUIManager.updateActiveUI(downloadModel)
			}
		}
	}
	
	/**
	 * Clears a download (removes from system but keeps files).
	 * @param downloadModel Download to clear
	 * @param onCleared Callback invoked after successful clear
	 */
	override fun clearDownload(downloadModel: DownloadDataModel, onCleared: () -> Unit) {
		CoroutineScope(Dispatchers.IO).launch {
			pauseDownload(downloadModel)
			downloadModel.isRemoved = true
			downloadModel.deleteModelFromDisk()
			activeDownloadDataModels.remove(downloadModel)
			
			searchActiveDownloadTaskWith(downloadModel)?.let { downloadTask ->
				runningDownloadTasks.remove(downloadTask)
				waitingDownloadTasks.remove(downloadTask)
			}
			
			downloadNotification.updateNotification(downloadModel)
			downloadsUIManager.updateActiveUI(downloadModel)
			onCleared()
		}
	}
	
	/**
	 * Deletes a download and its associated files.
	 * @param downloadModel Download to delete
	 * @param onDone Callback invoked after successful deletion
	 */
	override fun deleteDownload(downloadModel: DownloadDataModel, onDone: () -> Unit) {
		CoroutineScope(Dispatchers.IO).launch {
			clearDownload(downloadModel)
			downloadModel.isDeleted = true
			downloadModel.deleteModelFromDisk()
			downloadModel.getDestinationFile().delete()
			onDone()
		}
	}
	
	// Bulk operations
	override fun resumeAllDownloads() {
		activeDownloadDataModels.forEach { resumeDownload(it) }
	}
	
	override fun pauseAllDownloads() {
		activeDownloadDataModels.forEach { pauseDownload(it) }
	}
	
	override fun clearAllDownloads() {
		activeDownloadDataModels.forEach { clearDownload(it) }
	}
	
	override fun deleteAllDownloads() {
		activeDownloadDataModels.forEach { deleteDownload(it) }
	}
	
	/**
	 * Handles status updates from download tasks.
	 * @param downloadTaskInf The task reporting status change
	 */
	override fun onStatusUpdate(downloadTaskInf: DownloadTaskInf) {
		CoroutineScope(Dispatchers.IO).launch {
			val downloadDataModel = downloadTaskInf.downloadDataModel
			
			if (downloadDataModel.isRunning && downloadDataModel.status == DOWNLOADING)
				addToRunningDownloadTasksList(downloadTaskInf)
			
			if (!downloadDataModel.isRunning && downloadDataModel.status == CLOSE)
				removeFromRunningDownloadTasksList(downloadTaskInf)
			
			if (downloadDataModel.isComplete && downloadDataModel.status == COMPLETE) {
				aioBackend.saveDownloadLog(downloadDataModel)
				removeFromActiveDownloadDataModelsList(downloadDataModel)
				withContext(Dispatchers.Main) { downloadsUIManager.updateActiveUI(downloadDataModel) }
				updateFinishedDownloadDataModelsList(downloadDataModel)
				withContext(Dispatchers.Main) {
					downloadOnFinishListeners.forEach { finishUIListener ->
						finishUIListener.onFinishUIDownload(downloadDataModel)
					}
				}
			}
			
			updateUIAndNotification(downloadDataModel)
		}
	}
	
	
	/**
	 * Adds a task to the running tasks list if not already present.
	 */
	private fun addToRunningDownloadTasksList(downloadTaskInf: DownloadTaskInf) {
		if (!runningDownloadTasks.contains(downloadTaskInf)) {
			runningDownloadTasks.add(downloadTaskInf)
		}
	}
	
	/**
	 * Removes a task from the running tasks list if present.
	 */
	private fun removeFromRunningDownloadTasksList(downloadTaskInfo: DownloadTaskInf) {
		if (runningDownloadTasks.contains(downloadTaskInfo)) {
			runningDownloadTasks.remove(downloadTaskInfo)
		}
	}
	
	/**
	 * Removes a download from active models list if present.
	 */
	private fun removeFromActiveDownloadDataModelsList(downloadDataModel: DownloadDataModel) {
		if (activeDownloadDataModels.contains(downloadDataModel)) {
			activeDownloadDataModels.remove(downloadDataModel)
		}
	}
	
	/**
	 * Starts pending downloads from the waiting list according to parallel limits.
	 */
	@Synchronized
	private fun startPendingTasksFromWaitingList() {
		verifyLeftoverRunningTasks()
		val maxAllowedParallelDownloads = aioSettings.downloadDefaultParallelConnections
		if (numberOfRunningTasks() >= maxAllowedParallelDownloads) return
		if (waitingDownloadTasks.isEmpty()) return
		
		val queuedTask = waitingDownloadTasks.removeAt(0)
		try {
			startDownloadTask(queuedTask)
		} catch (error: Exception) {
			error.printStackTrace()
			queuedTask.updateDownloadStatus(status = CLOSE)
		}
	}
	
	/**
	 * Starts execution of a download task.
	 */
	private fun startDownloadTask(downloadTaskInf: DownloadTaskInf) {
		ThreadsUtility.executeInBackground(codeBlock = {
			try {
				downloadTaskInf.startDownload()
			} catch (err: Exception) {
				err.printStackTrace()
				downloadTaskInf.updateDownloadStatus(status = CLOSE)
			}
		})
	}
	
	/**
	 * Cleans up any invalid running tasks from the list.
	 */
	private fun verifyLeftoverRunningTasks() {
		try {
			val iterator = runningDownloadTasks.iterator()
			while (iterator.hasNext()) {
				val runningTask = iterator.next()
				val dataModel = runningTask.downloadDataModel
				if (!dataModel.isRunning && dataModel.status != DOWNLOADING) {
					iterator.remove()
				}
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Creates appropriate download task based on file type.
	 */
	private fun generateDownloadTask(downloadDataModel: DownloadDataModel): DownloadTaskInf {
		if (downloadDataModel.videoInfo != null && downloadDataModel.videoFormat != null) {
			val videoDownloader = VideoDownloader(downloadDataModel)
			videoDownloader.statusListener = this
			videoDownloader.initiate()
			return videoDownloader
		} else {
			val regularDownloader = RegularDownloader(downloadDataModel)
			regularDownloader.statusListener = this
			regularDownloader.initiate()
			return regularDownloader
		}
	}
	
	/**
	 * Updates UI and notification for a download.
	 */
	private fun updateUIAndNotification(downloadDataModel: DownloadDataModel) {
		if (!downloadDataModel.isComplete) {
			downloadsUIManager.updateActiveUI(downloadDataModel)
		}
		
		downloadNotification.updateNotification(downloadDataModel)
	}
	
	
	/**
	 * Updates finished downloads list according to cleanup settings.
	 */
	private fun updateFinishedDownloadDataModelsList(downloadDataModel: DownloadDataModel) {
		val setting = downloadDataModel.globalSettings
		if (!setting.downloadAutoRemoveTasks ||
			setting.downloadAutoRemoveTaskAfterNDays != 0
		) {
			addToFinishDownloadList(downloadDataModel)
		}
	}
	
	/**
	 * Adds completed download to finished list and updates UI.
	 */
	private fun addToFinishDownloadList(downloadDataModel: DownloadDataModel) {
		addAndSortFinishedDownloadDataModels(downloadDataModel)
		CoroutineScope(Dispatchers.Main).launch {
			downloadsUIManager.finishedTasksFragment
				?.finishedTasksListAdapter?.notifyDataSetChangedOnSort(false)
			updateMediaStore()
		}
	}
	
	/**
	 * Cleans up system resources.
	 */
	fun cleanUp() {
		aioTimer.unregister(this)
	}
}