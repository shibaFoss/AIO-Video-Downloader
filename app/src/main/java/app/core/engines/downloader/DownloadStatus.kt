package app.core.engines.downloader

/**
 * Defines constants representing various download states in the application.
 *
 * This object provides a centralized location for all download status codes
 * used throughout the download engine. Using these constants ensures:
 * - Type-safe status comparisons
 * - Consistent status values across the codebase
 * - Easy maintenance of status codes
 *
 * Status values represent the lifecycle of a download:
 * 1. DOWNLOADING - Active download in progress
 * 2. CLOSE - Download was stopped or cancelled
 * 3. COMPLETE - Download finished successfully
 */
object DownloadStatus {
	
	/**
	 * Indicates the download is currently in progress.
	 * Value: 2
	 */
	const val DOWNLOADING: Int = 2
	
	/**
	 * Indicates the download was stopped or cancelled.
	 * Value: 3
	 *
	 * Note: This could represent either user cancellation
	 * or system-initiated termination.
	 */
	const val CLOSE: Int = 3
	
	/**
	 * Indicates the download completed successfully.
	 * Value: 4
	 *
	 * The downloaded file should be fully available
	 * and verified when in this state.
	 */
	const val COMPLETE: Int = 4
}