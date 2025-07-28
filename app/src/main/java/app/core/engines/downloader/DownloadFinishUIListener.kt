package app.core.engines.downloader

/**
 * An interface defining a callback to be notified when a download completes and requires UI updates.
 * This listener pattern allows for decoupled communication between download processing logic
 * and UI components that need to respond to download completion events.
 */
interface DownloadFinishUIListener {
	
	/**
	 * Called when a download has finished processing and requires UI updates.
	 * Implement this method to handle UI changes such as:
	 * - Updating download completion status
	 * - Showing completion notifications
	 * - Refreshing download lists
	 * - Initiating post-download processing
	 *
	 * @param downloadDataModel The completed download's data model containing:
	 *                         - Final status (success/failure)
	 *                         - File metadata
	 *                         - Download statistics
	 *                         - Any error messages
	 *
	 * Usage Example:
	 * ```
	 * class DownloadManager : DownloadFinishUIListener {
	 *     override fun onFinishUIDownload(downloadDataModel: DownloadDataModel) {
	 *         // Update UI with completed download
	 *         notifyDownloadComplete(downloadDataModel)
	 *         // Refresh download list
	 *         refreshDownloadsList()
	 *     }
	 * }
	 * ```
	 *
	 * Threading Note:
	 * Implementations should handle thread safety as this may be called from background threads.
	 * Use runOnUiThread or similar mechanisms for direct UI manipulations.
	 */
	fun onFinishUIDownload(downloadDataModel: DownloadDataModel)
}