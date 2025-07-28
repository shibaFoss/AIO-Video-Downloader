package lib.device

import android.os.Environment
import android.os.Environment.MEDIA_MOUNTED
import android.os.StatFs

/**
 * Utility class for retrieving storage-related information on the device.
 */
object StorageUtility {
	
	/**
	 * Returns the total internal storage space available on the device in bytes.
	 *
	 * @return Total space in bytes from the internal data directory.
	 */
	@JvmStatic
	fun getTotalStorageSpace(): Long {
		val path = Environment.getDataDirectory()
		val stat = StatFs(path.path)
		val blockSize = stat.blockSizeLong
		val totalBlocks = stat.blockCountLong
		return totalBlocks * blockSize
	}
	
	/**
	 * Returns the free internal storage space available on the device in bytes.
	 *
	 * @return Available space in bytes from the internal data directory.
	 */
	@JvmStatic
	fun getFreeStorageSpace(): Long {
		val path = Environment.getDataDirectory()
		val stat = StatFs(path.path)
		val blockSize = stat.blockSizeLong
		val availableBlocks = stat.availableBlocksLong
		return availableBlocks * blockSize
	}
	
	/**
	 * Returns the percentage of free internal storage available.
	 *
	 * @return Percentage of available space, as a float.
	 */
	@JvmStatic
	fun getFreeStoragePercentage(): Float {
		val totalSpace = getTotalStorageSpace()
		val freeSpace = getFreeStorageSpace()
		val freePercentage = (freeSpace.toFloat() / totalSpace) * 100
		return freePercentage
	}
	
	/**
	 * Returns the total external storage space available (e.g., SD card) in bytes.
	 * This only works if the external storage is mounted.
	 *
	 * @return Total space in bytes or 0 if the external storage is not mounted.
	 */
	@JvmStatic
	fun getTotalExternalStorageSpace(): Long {
		return if (Environment.getExternalStorageState() == MEDIA_MOUNTED) {
			val path = Environment.getExternalStorageDirectory()
			val stat = StatFs(path.path)
			val blockSize = stat.blockSizeLong
			val totalBlocks = stat.blockCountLong
			totalBlocks * blockSize
		} else 0
	}
	
	/**
	 * Returns the free external storage space available (e.g., SD card) in bytes.
	 * This only works if the external storage is mounted.
	 *
	 * @return Available space in bytes or 0 if the external storage is not mounted.
	 */
	@JvmStatic
	fun getFreeExternalStorageSpace(): Long {
		return if (Environment.getExternalStorageState() == MEDIA_MOUNTED) {
			val path = Environment.getExternalStorageDirectory()
			val stat = StatFs(path.path)
			val blockSize = stat.blockSizeLong
			val availableBlocks = stat.availableBlocksLong
			availableBlocks * blockSize
		} else 0
	}
	
	/**
	 * Returns the percentage of free external storage space available.
	 * This only works if the external storage is mounted.
	 *
	 * @return Percentage of available space or 0 if external storage is not mounted.
	 */
	@JvmStatic
	fun getFreeExternalStoragePercentage(): Float {
		val totalSpace = getTotalExternalStorageSpace()
		val freeSpace = getFreeExternalStorageSpace()
		val freePercentage = if (totalSpace != 0L) {
			(freeSpace.toFloat() / totalSpace) * 100
		} else 0f
		return freePercentage
	}
}
