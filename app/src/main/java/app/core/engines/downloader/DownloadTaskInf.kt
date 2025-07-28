package app.core.engines.downloader

/**
 * Interface defining the core operations for a download task.
 *
 * Implementations of this interface represent individual download operations
 * that can be managed by the download system. The interface provides:
 * - Lifecycle control (initiation, start, cancellation)
 * - Status reporting capabilities
 * - Access to underlying download model data
 *
 * All implementations should maintain thread safety and proper state management.
 */
interface DownloadTaskInf {
	
	/**
	 * The data model containing all information about this download.
	 * This provides access to:
	 * - Download progress and status
	 * - File metadata
	 * - Configuration settings
	 */
	val downloadDataModel: DownloadDataModel
	
	/**
	 * Listener for receiving status updates about the download progress.
	 * The implementing class should notify this listener about:
	 * - Status changes (started, paused, completed, failed)
	 * - Progress updates
	 * - Significant events during the download
	 */
	var statusListener: DownloadTaskListener?
	
	/**
	 * Prepares the download task for execution.
	 * This should:
	 * - Validate required parameters
	 * - Initialize any necessary resources
	 * - Set up the download environment
	 * - Not throw exceptions for initialization errors
	 */
	fun initiate()
	
	/**
	 * Begins the actual download operation.
	 * Implementations should:
	 * - Start the download process
	 * - Handle network operations
	 * - Update progress periodically
	 * - Manage file writing
	 * - Notify status changes
	 * - Handle errors gracefully
	 */
	fun startDownload()
	
	/**
	 * Stops an active download operation.
	 * @param cancelReason Optional description of why the download was cancelled
	 * Implementations should:
	 * - Clean up network resources
	 * - Finalize file writing if needed
	 * - Update the download status appropriately
	 * - Notify listeners of cancellation
	 */
	fun cancelDownload(cancelReason: String = "")
	
	/**
	 * Updates the status of the download task.
	 * @param statusInfo Optional detailed status message
	 * @param status The new status code (from DownloadStatus)
	 * Implementations should:
	 * - Update internal state
	 * - Validate status codes
	 * - Notify listeners of changes
	 * - Handle status transitions appropriately
	 */
	fun updateDownloadStatus(
		statusInfo: String? = null,
		status: Int = downloadDataModel.status
	)
}