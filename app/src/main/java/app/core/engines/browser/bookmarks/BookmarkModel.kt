package app.core.engines.browser.bookmarks

import java.io.Serializable
import java.util.Date

/**
 * Represents a bookmark entry in the browser engine.
 *
 * This model holds all relevant metadata for a bookmark, such as its URL,
 * creation/modification dates, tags, notes, priority, and sharing information.
 *
 * @constructor Creates a new instance of BookmarkModel with default values.
 */
class BookmarkModel : Serializable {
	
	/** The URL of the bookmarked web page. */
	var bookmarkUrl: String = ""
	
	/** A user-defined name or title for the bookmark. */
	var bookmarkName: String = ""
	
	/** The date and time when the bookmark was created. */
	var bookmarkCreationDate: Date = Date()
	
	/** The date and time when the bookmark was last modified. */
	var bookmarkModifiedDate: Date = Date()
	
	/** File path to a local thumbnail image for the bookmark. */
	var bookmarkThumbFilePath: String = ""
	
	/** A brief description or summary of the bookmark. */
	var bookmarkDescription: String = ""
	
	/** A list of user-defined tags for categorizing the bookmark. */
	var bookmarkTags: List<String> = emptyList()
	
	/** Indicates whether the bookmark is marked as a favorite. */
	var bookmarkFavorite: Boolean = false
	
	/** Name of the folder or category this bookmark belongs to. */
	var bookmarkFolder: String = ""
	
	/** Tracks how many times this bookmark has been accessed. */
	var bookmarkAccessCount: Int = 0
	
	/** Stores the last time the bookmark was accessed (as a string). */
	var bookmarkLastAccessed: String = ""
	
	/** A user-assigned rating for the bookmark (0.0 to 5.0, for example). */
	var bookmarkRating: Float = 0.0f
	
	/** Priority level for the bookmark, used for sorting or highlighting. */
	var bookmarkPriority: Int = 0
	
	/** Whether the bookmark is archived (no longer active but retained). */
	var bookmarkArchived: Boolean = false
	
	/** URL or local path to an icon representing the bookmark. */
	var bookmarkIcon: String = ""
	
	/** Additional user notes related to the bookmark. */
	var bookmarkNotes: String = ""
	
	/** The owner or creator of the bookmark, useful in shared environments. */
	var bookmarkOwner: String = ""
	
	/** List of users/emails with whom the bookmark is shared. */
	var bookmarkSharedWith: List<String> = emptyList()
	
	/**
	 * Resets all fields of the bookmark to their default values.
	 *
	 * This can be useful when reusing the model or resetting user input.
	 */
	fun defaultAllFields() {
		bookmarkUrl = ""
		bookmarkName = ""
		bookmarkCreationDate = Date()
		bookmarkModifiedDate = Date()
		bookmarkThumbFilePath = ""
		bookmarkDescription = ""
		bookmarkTags = emptyList()
		bookmarkFavorite = false
		bookmarkFolder = ""
		bookmarkAccessCount = 0
		bookmarkLastAccessed = ""
		bookmarkRating = 0.0f
		bookmarkPriority = 0
		bookmarkArchived = false
		bookmarkIcon = ""
		bookmarkNotes = ""
		bookmarkOwner = ""
		bookmarkSharedWith = emptyList()
	}
}
