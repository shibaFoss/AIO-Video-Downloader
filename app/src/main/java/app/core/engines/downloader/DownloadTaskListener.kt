package app.core.engines.downloader

/**
 * Callback interface for receiving status updates from download tasks.
 *
 * Implement this interface to be notified about:
 * - Progress updates during download execution
 * - State changes (started, paused, completed, failed)
 * - Significant events occurring during the download process
 *
 * Usage:
 * 1. Implement this interface in classes that need download status updates
 * 2. Register the implementation with a DownloadTaskInf instance
 * 3. Handle updates in the onStatusUpdate callback
 *
 * Note: Implementations should process updates quickly and avoid blocking
 * operations as this may affect download performance.
 */
interface DownloadTaskListener {
	
	/**
	 * Called when a download task's status changes.
	 *
	 * @param downloadTaskInf The download task reporting the status change.
	 * Provides access to:
	 * - Current download state via downloadDataModel
	 * - Progress information
	 * - Status details
	 *
	 * Typical implementation should:
	 * - Update UI elements with new progress
	 * - Handle completion/failure cases
	 * - Trigger any follow-up actions
	 * - Avoid long-running operations
	 */
	fun onStatusUpdate(downloadTaskInf: DownloadTaskInf)
}