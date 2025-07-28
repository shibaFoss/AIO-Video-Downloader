package app.core.engines.downloader

import lib.networks.DownloaderUtils.formatDownloadSpeedInSimpleForm

/**
 * Utility class that tracks and calculates the current download speed over time.
 *
 * This class is designed to be lightweight and easy to integrate into any download-related system.
 * It calculates speed based on the change in bytes downloaded over the time elapsed between updates.
 *
 * @param initialBytesDownloaded The starting byte count of the download at the time of initialization.
 */
class NetSpeedTracker(initialBytesDownloaded: Long) {
	
	// Holds the number of bytes downloaded at the last checkpoint
	private var previousBytes: Long = initialBytesDownloaded
	
	// Holds the timestamp (in milliseconds) at the last checkpoint
	private var previousTime: Long = System.currentTimeMillis()
	
	// The most recently calculated speed in bytes per second
	private var currentSpeed: Long = 0L
	
	/**
	 * Updates the tracker with the current total bytes downloaded.
	 * This method calculates the new speed based on the difference in bytes and time since the last update.
	 *
	 * @param bytesRead The total number of bytes downloaded so far.
	 */
	fun update(bytesRead: Long) {
		val currentTime = System.currentTimeMillis()
		val elapsedTime = (currentTime - previousTime) / 1000.0
		val bytesDelta = bytesRead - previousBytes
		
		if (elapsedTime > 0) {
			currentSpeed = (bytesDelta / elapsedTime).toLong()
		}
		
		previousBytes = bytesRead
		previousTime = currentTime
	}
	
	/**
	 * Returns the most recently calculated download speed in bytes per second.
	 *
	 * @return The current download speed in B/s.
	 */
	fun getCurrentSpeed(): Long = currentSpeed
	
	/**
	 * Returns the formatted string representation of the current download speed.
	 * Useful for displaying user-friendly speed metrics (e.g., "1.2 MB/s").
	 *
	 * @return A human-readable string of the current download speed.
	 */
	fun getFormattedSpeed(): String = formatDownloadSpeedInSimpleForm(currentSpeed.toDouble())
}
