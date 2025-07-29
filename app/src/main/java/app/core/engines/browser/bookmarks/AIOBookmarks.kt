package app.core.engines.browser.bookmarks

import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioBookmark
import app.core.AIOApp.Companion.aioGSONInstance
import lib.files.FileSystemUtility.saveStringToInternalStorage
import lib.process.ThreadsUtility
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.Serializable
import java.nio.channels.FileChannel
import java.util.Locale

/**
 * Manages browser bookmarks including storage, retrieval, and various operations.
 *
 * Features:
 * - Persistent storage using JSON serialization
 * - Efficient file reading strategies based on file size
 * - Thread-safe operations
 * - Advanced bookmark filtering and sorting
 * - Support for bookmark metadata (ratings, tags, favorites, etc.)
 *
 * All file operations are performed asynchronously to avoid blocking the UI thread.
 */
class AIOBookmarks : Serializable {
	
	// Configuration
	private val bookmarkConfigFileName: String = "aio_bookmarks.json"
	
	// Main bookmark storage
	private var bookmarkLibrary: ArrayList<BookmarkModel> = ArrayList()
	
	/**
	 * Reads bookmarks from persistent storage.
	 * Automatically selects appropriate reading strategy based on file size:
	 * - Small files (<0.5MB): Simple read
	 * - Medium files (0.5-5MB): Buffered read
	 * - Large files (>5MB): Memory-mapped or line-by-line read
	 */
	fun readObjectFromStorage() {
		ThreadsUtility.executeInBackground(codeBlock = {
			try {
				val configFile = File(INSTANCE.filesDir, bookmarkConfigFileName)
				if (!configFile.exists()) return@executeInBackground
				
				// Determine file size and select appropriate reading strategy
				val fileSizeMb = configFile.length().toDouble() / (1024 * 1024)
				val json = when {
					fileSizeMb <= 0.5 -> readSmallFile(configFile)
					fileSizeMb <= 5.0 -> readMediumFile(configFile)
					else -> readLargeFile(configFile)
				}
				
				// Update bookmark library if data was read successfully
				if (json.isNotEmpty()) {
					convertJSONStringToClass(json).let { bookmarkClass ->
						aioBookmark.bookmarkLibrary = bookmarkClass.bookmarkLibrary
						aioBookmark.updateInStorage()
					}
				}
			} catch (error: Exception) {
				error.printStackTrace()
			}
		})
	}
	
	/**
	 * Reads small files using simple file read operation.
	 * @param file The file to read
	 * @return File contents as String
	 */
	private fun readSmallFile(file: File): String {
		return file.readText(Charsets.UTF_8)
	}
	
	/**
	 * Reads medium files using buffered reader for better performance.
	 * @param file The file to read
	 * @return File contents as String
	 */
	private fun readMediumFile(file: File): String {
		return BufferedReader(FileReader(file)).use { it.readText() }
	}
	
	/**
	 * Reads large files using memory-mapped I/O with fallback to line-by-line reading.
	 * @param file The file to read
	 * @return File contents as String
	 */
	private fun readLargeFile(file: File): String {
		return try {
			// First try memory-mapped I/O for best performance
			FileInputStream(file).channel.use { channel ->
				val buffer = channel.map(
					FileChannel.MapMode.READ_ONLY,
					0, channel.size()
				)
				Charsets.UTF_8.decode(buffer).toString()
			}
		} catch (error: OutOfMemoryError) {
			// Fallback to line-by-line reading if memory mapping fails
			error.printStackTrace()
			buildString {
				BufferedReader(FileReader(file)).forEachLine { line ->
					append(line)
				}
			}
		}
	}
	
	/**
	 * Serializes the current bookmark state to JSON.
	 * @return JSON string representation of bookmarks
	 */
	private fun convertClassToJSON(): String {
		return aioGSONInstance.toJson(this)
	}
	
	/**
	 * Deserializes JSON string back to AIOBookmarks instance.
	 * @param data JSON string to convert
	 * @return AIOBookmarks instance
	 */
	private fun convertJSONStringToClass(data: String): AIOBookmarks {
		return aioGSONInstance.fromJson(data, AIOBookmarks::class.java)
	}
	
	/**
	 * Persists current bookmark state to storage.
	 * Runs asynchronously in background thread.
	 */
	fun updateInStorage() {
		ThreadsUtility.executeInBackground(codeBlock = {
			try {
				saveStringToInternalStorage(
					fileName = bookmarkConfigFileName,
					data = convertClassToJSON()
				)
			} catch (error: Exception) {
				error.printStackTrace()
			}
		})
	}
	
	/**
	 * Gets all bookmarks in the library.
	 * @return List of all bookmarks
	 */
	fun getBookmarkLibrary(): ArrayList<BookmarkModel> {
		return bookmarkLibrary
	}
	
	/**
	 * Adds a new bookmark to the library.
	 * @param bookmarkModel Bookmark to add
	 */
	fun insertNewBookmark(bookmarkModel: BookmarkModel) {
		getBookmarkLibrary().add(bookmarkModel)
	}
	
	/**
	 * Updates an existing bookmark.
	 * @param oldBookmark Bookmark to replace
	 * @param newBookmark New bookmark data
	 */
	fun updateBookmark(oldBookmark: BookmarkModel, newBookmark: BookmarkModel) {
		val index = bookmarkLibrary.indexOf(oldBookmark)
		if (index != -1) {
			bookmarkLibrary[index] = newBookmark
		}
	}
	
	/**
	 * Removes a bookmark from the library.
	 * @param bookmarkModel Bookmark to remove
	 */
	fun removeBookmark(bookmarkModel: BookmarkModel) {
		bookmarkLibrary.remove(bookmarkModel)
	}
	
	/**
	 * Searches bookmarks by name or URL.
	 * @param query Search term
	 * @return List of matching bookmarks (case-insensitive)
	 */
	fun searchBookmarks(query: String): List<BookmarkModel> {
		return bookmarkLibrary.filter {
			it.bookmarkName.contains(query, ignoreCase = true) ||
					it.bookmarkUrl.contains(query, ignoreCase = true)
		}
	}
	
	/**
	 * Finds duplicate bookmarks (same URL).
	 * @return List of duplicate bookmarks
	 */
	fun findDuplicateBookmarks(): List<BookmarkModel> {
		return bookmarkLibrary
			.groupBy { it.bookmarkUrl }
			.filter { it.value.size > 1 }
			.flatMap { it.value }
	}
	
	/**
	 * Sorts bookmarks by specified attribute.
	 * @param attribute Attribute to sort by ("name" or "date")
	 * @return Sorted list of bookmarks
	 */
	fun getBookmarksSortedBy(attribute: String): List<BookmarkModel> {
		return when (attribute.lowercase(Locale.ROOT)) {
			"name" -> bookmarkLibrary.sortedBy { it.bookmarkName }
			"date" -> bookmarkLibrary.sortedBy { it.bookmarkCreationDate }
			else -> bookmarkLibrary
		}
	}
	
	/**
	 * Filters bookmarks by minimum rating.
	 * @param minRating Minimum rating threshold
	 * @return List of bookmarks meeting the rating criteria
	 */
	fun filterBookmarksByRating(minRating: Float): List<BookmarkModel> {
		return bookmarkLibrary.filter { it.bookmarkRating >= minRating }
	}
	
	/**
	 * Gets all favorite bookmarks.
	 * @return List of favorite bookmarks
	 */
	fun getFavoriteBookmarks(): List<BookmarkModel> {
		return bookmarkLibrary.filter { it.bookmarkFavorite }
	}
	
	/**
	 * Filters bookmarks by tag.
	 * @param tag Tag to filter by
	 * @return List of bookmarks containing the specified tag
	 */
	fun filterBookmarksByTag(tag: String): List<BookmarkModel> {
		return bookmarkLibrary.filter { it.bookmarkTags.contains(tag) }
	}
	
	/**
	 * Filters bookmarks by minimum priority.
	 * @param minPriority Minimum priority threshold
	 * @return List of bookmarks meeting the priority criteria
	 */
	fun filterBookmarksByPriority(minPriority: Int): List<BookmarkModel> {
		return bookmarkLibrary.filter { it.bookmarkPriority >= minPriority }
	}
	
	/**
	 * Gets all archived bookmarks.
	 * @return List of archived bookmarks
	 */
	fun getArchivedBookmarks(): List<BookmarkModel> {
		return bookmarkLibrary.filter { it.bookmarkArchived }
	}
	
	/**
	 * Archives a bookmark.
	 * @param bookmarkModel Bookmark to archive
	 */
	fun archiveBookmark(bookmarkModel: BookmarkModel) {
		val index = bookmarkLibrary.indexOf(bookmarkModel)
		if (index != -1) {
			bookmarkLibrary[index].bookmarkArchived = true
		}
	}
	
	/**
	 * Gets bookmarks shared with specific user.
	 * @param user User identifier
	 * @return List of shared bookmarks
	 */
	fun getBookmarksSharedWithUser(user: String): List<BookmarkModel> {
		return bookmarkLibrary.filter { it.bookmarkSharedWith.contains(user) }
	}
	
	/**
	 * Clears all bookmarks from the library.
	 */
	fun clearAllBookmarks() {
		bookmarkLibrary.clear()
	}
	
	/**
	 * Gets total number of bookmarks.
	 * @return Count of bookmarks
	 */
	fun countBookmarks(): Int {
		return bookmarkLibrary.size
	}
}