package app.core.engines.browser.history

import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioGSONInstance
import app.core.AIOApp.Companion.aioHistory
import lib.files.FileSystemUtility.saveStringToInternalStorage
import lib.process.ThreadsUtility
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.Serializable
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Represents the complete browsing history managed by the application.
 *
 * This class handles storing, retrieving, filtering, and updating user browsing history,
 * and persists data in JSON format using internal storage.
 */
class AIOHistory : Serializable {
	
	/** File name where browsing history JSON is stored internally. */
	private val historyConfigFileName: String = "browsing_history.json"
	
	/** List containing all recorded history entries. */
	private var historyLibrary: ArrayList<HistoryModel> = ArrayList()
	
	/**
	 * Loads history data from internal storage and deserializes it into this class.
	 * Uses different reading strategies based on file size to optimize performance.
	 */
	fun readObjectFromStorage() {
		ThreadsUtility.executeInBackground(codeBlock = {
			try {
				val configFile = File(INSTANCE.filesDir, historyConfigFileName)
				if (!configFile.exists()) return@executeInBackground
				
				val fileSizeMb = configFile.length().toDouble() / (1024 * 1024)
				
				val json = when {
					fileSizeMb <= 0.5 -> readSmallFile(configFile)
					fileSizeMb <= 5.0 -> readMediumFile(configFile)
					else -> readLargeFile(configFile)
				}
				
				if (json.isNotEmpty()) {
					convertJSONStringToClass(json).let { historyClass ->
						aioHistory.historyLibrary = historyClass.historyLibrary
						aioHistory.updateInStorage()
					}
				}
			} catch (error: Exception) {
				error.printStackTrace()
			}
		})
	}
	
	/** Reads small-sized history files directly using UTF-8 encoding. */
	private fun readSmallFile(file: File): String {
		return file.readText(Charsets.UTF_8)
	}
	
	/** Reads medium-sized history files using BufferedReader for efficiency. */
	private fun readMediumFile(file: File): String {
		return BufferedReader(FileReader(file)).use { it.readText() }
	}
	
	/**
	 * Reads large history files using memory mapping.
	 * Falls back to line-by-line reading if OutOfMemoryError occurs.
	 */
	private fun readLargeFile(file: File): String {
		return try {
			FileInputStream(file).channel.use { channel ->
				val buffer = channel.map(
					FileChannel.MapMode.READ_ONLY, 0, channel.size()
				)
				Charsets.UTF_8.decode(buffer).toString()
			}
		} catch (error: OutOfMemoryError) {
			error.printStackTrace()
			buildString {
				BufferedReader(FileReader(file)).forEachLine { line -> append(line) }
			}
		}
	}
	
	/** Converts the current instance of [AIOHistory] into a JSON string. */
	private fun convertClassToJSON(): String {
		return aioGSONInstance.toJson(this)
	}
	
	/** Converts a JSON string back into an [AIOHistory] instance. */
	private fun convertJSONStringToClass(data: String): AIOHistory {
		return aioGSONInstance.fromJson(data, AIOHistory::class.java)
	}
	
	/**
	 * Saves the current history data into internal storage as JSON.
	 * This operation is done asynchronously.
	 */
	fun updateInStorage() {
		ThreadsUtility.executeInBackground(codeBlock = {
			try {
				saveStringToInternalStorage(
					fileName = historyConfigFileName,
					data = convertClassToJSON()
				)
			} catch (error: Exception) {
				error.printStackTrace()
			}
		})
	}
	
	/** Returns the current history list. */
	fun getHistoryLibrary(): ArrayList<HistoryModel> = historyLibrary
	
	/** Inserts a new history entry into the library. */
	fun insertNewHistory(historyModel: HistoryModel) {
		historyLibrary.add(historyModel)
	}
	
	/** Updates an existing history entry by replacing it with a new one. */
	fun updateHistory(oldHistory: HistoryModel, newHistory: HistoryModel) {
		val index = historyLibrary.indexOf(oldHistory)
		if (index != -1) {
			historyLibrary[index] = newHistory
		}
	}
	
	/**
	 * Searches the history entries by title or URL.
	 *
	 * @param query The search keyword.
	 * @return List of matching history items.
	 */
	fun searchHistory(query: String): List<HistoryModel> {
		return historyLibrary.filter {
			it.historyTitle.contains(query, ignoreCase = true) ||
					it.historyUrl.contains(query, ignoreCase = true)
		}
	}
	
	/** Finds and returns duplicate history entries based on URL. */
	fun findDuplicateHistory(): List<HistoryModel> {
		return historyLibrary
			.groupBy { it.historyUrl }
			.filter { it.value.size > 1 }
			.flatMap { it.value }
	}
	
	/** Removes a specific history item from the library. */
	fun removeHistory(historyModel: HistoryModel) {
		historyLibrary.remove(historyModel)
	}
	
	/**
	 * Returns the history sorted by a given attribute.
	 *
	 * @param attribute Either "title" or "date".
	 */
	fun getHistorySortedBy(attribute: String): List<HistoryModel> {
		return when (attribute.lowercase(Locale.ROOT)) {
			"title" -> historyLibrary.sortedBy { it.historyTitle }
			"date" -> historyLibrary.sortedBy { it.historyVisitDateTime }
			else -> historyLibrary
		}
	}
	
	/** Clears all entries from the history. */
	fun clearAllHistory() {
		historyLibrary.clear()
	}
	
	/** Returns the total number of history entries. */
	fun countHistory(): Int = historyLibrary.size
	
	/**
	 * Returns recent history items within the last [days] number of days.
	 */
	fun getRecentHistory(days: Int): List<HistoryModel> {
		val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
		val calendar = Calendar.getInstance()
		calendar.add(Calendar.DAY_OF_YEAR, -days)
		val cutoffDate = calendar.time
		
		val cutoffDateString = dateFormat.format(cutoffDate)
		val cutoffDateParsed = dateFormat.parse(cutoffDateString) ?: return emptyList()
		
		return historyLibrary.filter {
			val visitDate = dateFormat.parse(it.historyVisitDateTime.toString())
			val lastAccessedDate = dateFormat.parse(it.historyLastAccessed.toString())
			visitDate != null && visitDate.after(cutoffDateParsed) ||
					lastAccessedDate != null && lastAccessedDate.after(cutoffDateParsed)
		}
	}
	
	/**
	 * Filters and returns history entries that have a visit duration
	 * greater than or equal to [minDuration] in milliseconds.
	 */
	fun filterHistoryByDuration(minDuration: Long): List<HistoryModel> {
		return historyLibrary.filter { it.historyDuration >= minDuration }
	}
	
	/** Returns all history entries marked as important. */
	fun getImportantHistory(): List<HistoryModel> {
		return historyLibrary.filter { it.historyImportant }
	}
	
	/**
	 * Filters history items by a specific tag.
	 *
	 * @param tag The tag to filter by.
	 */
	fun filterHistoryByTag(tag: String): List<HistoryModel> {
		return historyLibrary.filter { it.historyTags.contains(tag) }
	}
	
	/** Marks a history entry as archived without removing it. */
	fun archiveHistory(historyModel: HistoryModel) {
		val index = historyLibrary.indexOf(historyModel)
		if (index != -1) {
			historyLibrary[index].historyArchived = true
		}
	}
	
	/** Returns all archived history entries. */
	fun getArchivedHistory(): List<HistoryModel> {
		return historyLibrary.filter { it.historyArchived }
	}
}