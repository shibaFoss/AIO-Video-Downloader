package app.core.engines.browser.history

import java.io.Serializable
import java.util.Calendar
import java.util.Date

/**
 * HistoryModel represents the metadata and interaction details
 * for a single browser history entry.
 *
 * This model stores information such as URL, title, visit time,
 * user engagement (like rating and priority), device data, and user context.
 *
 * It implements Serializable to allow the object to be saved and restored,
 * for example during session management or persistent storage.
 */
class HistoryModel : Serializable {
	
	// The URL of the visited web page
	var historyUrl: String = ""
	
	// The title of the visited web page
	var historyTitle: String = ""
	
	// The date and time when the page was visited
	var historyVisitDateTime: Date = Date()
	
	// Path to the favicon image file associated with the visited page
	var historyFaviconFilePath: String = ""
	
	// Description or summary of the web page
	var historyDescription: String = ""
	
	// Flag indicating whether the history item is marked as important
	var historyImportant: Boolean = false
	
	// Name of the folder this history entry may be categorized under
	var historyFolder: String = ""
	
	// Number of times the URL has been accessed
	var historyAccessCount: Int = 0
	
	// The last time this page was accessed
	var historyLastAccessed: Date = Date()
	
	// Total duration (in milliseconds) the page was viewed
	var historyDuration: Long = 0L
	
	// User-provided rating of the visited page (0.0 to 5.0)
	var historyRating: Float = 0.0f
	
	// Priority level (custom sorting or filtering)
	var historyPriority: Int = 0
	
	// Flag indicating whether this history item has been archived
	var historyArchived: Boolean = false
	
	// Icon associated with the page or custom category
	var historyIcon: String = ""
	
	// Any notes or annotations about the visit
	var historyNotes: String = ""
	
	// The user who owns this history entry
	var historyOwner: String = ""
	
	// List of other users the history entry is shared with
	var historySharedWith: List<String> = emptyList()
	
	// Session identifier that this visit is part of
	var historySessionId: String = ""
	
	// User-Agent string from the browser/device
	var historyUserAgent: String = ""
	
	// The referring URL (if any) that led to this visit
	var historyReferrer: String = ""
	
	// Tags associated with this history entry
	var historyTags: List<String> = emptyList()
	
	// Search terms used to find or navigate to this page
	var historySearchTerms: List<String> = emptyList()
	
	// Content type of the visited page (e.g., "text/html", "application/pdf")
	var historyContentType: String = ""
	
	// Type of device used (e.g., "mobile", "desktop")
	var historyDeviceType: String = ""
	
	// Location of the user at the time of visit
	var historyLocation: String = ""
	
	/**
	 * Resets all fields in the model to their default values.
	 */
	fun defaultAllFields() {
		historyUrl = ""
		historyTitle = ""
		historyVisitDateTime = Date()
		historyFaviconFilePath = ""
		historyDescription = ""
		historyImportant = false
		historyFolder = ""
		historyAccessCount = 0
		historyLastAccessed = Date()
		historyDuration = 0L
		historyRating = 0.0f
		historyPriority = 0
		historyArchived = false
		historyIcon = ""
		historyNotes = ""
		historyOwner = ""
		historySharedWith = emptyList()
		historySessionId = ""
		historyUserAgent = ""
		historyReferrer = ""
		historyTags = emptyList()
		historySearchTerms = emptyList()
		historyContentType = ""
		historyDeviceType = ""
		historyLocation = ""
	}
	
	/**
	 * Increments the access count for this history item.
	 */
	fun updateAccessCount() {
		historyAccessCount += 1
	}
	
	/**
	 * Sets the visit duration based on start and end timestamps.
	 *
	 * @param startTime The start time of the visit
	 * @param endTime The end time of the visit
	 */
	fun setVisitDuration(startTime: Date, endTime: Date) {
		historyDuration = endTime.time - startTime.time
	}
	
	/**
	 * Checks if this history entry was visited within a given number of days.
	 *
	 * @param thresholdDays Number of past days to consider
	 * @return true if the visit is recent, false otherwise
	 */
	fun isRecent(thresholdDays: Int): Boolean {
		val calendar = Calendar.getInstance()
		calendar.add(Calendar.DAY_OF_YEAR, -thresholdDays)
		val cutoffDate = calendar.time
		return historyVisitDateTime.after(cutoffDate)
	}
	
	/**
	 * Adds a new tag to the history entry.
	 *
	 * @param tag The tag to add
	 */
	fun addTag(tag: String) {
		historyTags = historyTags + tag
	}
	
	/**
	 * Removes an existing tag from the history entry.
	 *
	 * @param tag The tag to remove
	 */
	fun removeTag(tag: String) {
		historyTags = historyTags - tag
	}
	
	/**
	 * Returns a formatted string representation of key history fields.
	 *
	 * @return A human-readable summary string
	 */
	fun toFormattedString(): String {
		return "Title: $historyTitle\n" +
				"URL: $historyUrl\n" +
				"Visited on: ${historyVisitDateTime}\n" +
				"Duration: ${historyDuration / 1000} seconds"
	}
	
	/**
	 * Clears sensitive information from the history entry.
	 * This might be used before sharing or exporting history data.
	 */
	fun clearSensitiveData() {
		historyReferrer = ""
		historyLocation = ""
	}
}