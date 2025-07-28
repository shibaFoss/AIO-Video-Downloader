package app.ui.main.fragments.downloads.fragments.finished

import app.core.engines.downloader.DownloadDataModel

/**
 * Interface defining click event callbacks for items in the list of finished downloads.
 * Implemented by UI components (such as [FinishedTasksFragment]) to handle user interactions
 * with finished download items.
 */
interface FinishedTasksClickEvents {
	
	/**
	 * Called when the user performs a standard (short) click on a finished download item.
	 *
	 * @param downloadModel The data model representing the clicked finished download.
	 * Typically used to preview, open, or show info about the downloaded file.
	 */
	fun onFinishedDownloadClick(downloadModel: DownloadDataModel)
	
	/**
	 * Called when the user performs a long click on a finished download item.
	 *
	 * @param downloadModel The data model representing the long-clicked finished download.
	 * Typically used to show additional options such as delete, share, or re-download.
	 */
	fun onFinishedDownloadLongClick(downloadModel: DownloadDataModel)
}
