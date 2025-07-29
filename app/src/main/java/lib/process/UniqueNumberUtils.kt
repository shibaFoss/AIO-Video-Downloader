package lib.process

import app.core.AIOApp.Companion.internalDataFolder
import app.core.engines.downloader.DownloadDataModel.Companion.DOWNLOAD_MODEL_FILE_EXTENSION
import java.util.Random

/**
 * Utility object for generating unique numbers and making random-based decisions.
 *
 * This class provides functions that are useful for generating IDs or filenames
 * in a unique manner and for determining whether to display ads randomly.
 */
object UniqueNumberUtils {
	
	/**
	 * Generates a pseudo-unique long number using the current time in milliseconds and a random component.
	 *
	 * This can be used as a quick method to generate unique identifiers, such as session IDs or filenames.
	 *
	 * @return A pseudo-unique [Long] number.
	 */
	@JvmStatic
	fun generateUniqueNumber(): Long {
		val random = Random()
		val currentTime = System.currentTimeMillis() % 1_000_000L
		val randomComponent = random.nextInt(1000)
		return currentTime * 1000 + randomComponent
	}
	
	/**
	 * Generates a unique integer ID for a new download model by checking the existing files.
	 *
	 * It parses existing filenames in the `internalDataFolder` that match the download model extension,
	 * extracts their prefix numbers, and returns one greater than the current maximum.
	 *
	 * @return A unique [Int] number suitable for naming a new download model.
	 */
	@JvmStatic
	fun getUniqueNumberForDownloadModels(): Int {
		val existingFiles = internalDataFolder.listFiles()
			.filter { it.name!!.endsWith(DOWNLOAD_MODEL_FILE_EXTENSION) }
		
		val existingNumbers = existingFiles.mapNotNull { file ->
			file.name!!.split("_").firstOrNull()?.toIntOrNull()
		}
		
		val maxNumber = existingNumbers.maxOrNull() ?: 0
		return maxNumber + 1
	}
}
