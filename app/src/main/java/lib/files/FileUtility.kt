@file:Suppress("DEPRECATION")

package lib.files

import android.content.ContentResolver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE
import android.net.Uri
import android.provider.OpenableColumns.DISPLAY_NAME
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.downloadSystem
import app.core.AIOApp.Companion.internalDataFolder
import com.aio.R
import com.anggrayudi.storage.file.getAbsolutePath
import lib.files.FileExtensions.ARCHIVE_EXTENSIONS
import lib.files.FileExtensions.DOCUMENT_EXTENSIONS
import lib.files.FileExtensions.IMAGE_EXTENSIONS
import lib.files.FileExtensions.MUSIC_EXTENSIONS
import lib.files.FileExtensions.PROGRAM_EXTENSIONS
import lib.files.FileExtensions.VIDEO_EXTENSIONS
import lib.texts.CommonTextUtils.getText
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Locale

/**
 * Utility functions for file operations, such as extracting file information, saving data,
 * handling file extensions, and interacting with Android's media store.
 */
object FileUtility {
	
	/**
	 * Updates the media store with files from the finished download data models.
	 */
	@JvmStatic
	fun updateMediaStore() {
		try {
			if (downloadSystem.isInitializing) return
			var index = 0
			while (index < downloadSystem.finishedDownloadDataModels.size) {
				val model = downloadSystem.finishedDownloadDataModels[index]
				val downloadedFile = model.getDestinationFile()
				addToMediaStore(downloadedFile)
				index++
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Extracts the file name from the content disposition header of a response.
	 * @param contentDisposition The content disposition header string.
	 * @return The extracted file name, or null if not found.
	 */
	@JvmStatic
	fun extractFileNameFromContentDisposition(contentDisposition: String?): String? {
		if (!contentDisposition.isNullOrEmpty()) {
			val filenameRegex = """(?i)filename=["']?([^";]+)""".toRegex()
			val filenameMatch = filenameRegex.find(contentDisposition)
			if (filenameMatch != null) {
				val filename = filenameMatch.groupValues[1]
				return filename
			}
		}
		return null
	}
	
	/**
	 * Decodes a URL-encoded file name.
	 * @param encodedString The URL-encoded file name.
	 * @return The decoded file name.
	 */
	@JvmStatic
	fun decodeURLFileName(encodedString: String): String {
		return try {
			val decodedFileName = URLDecoder.decode(encodedString, UTF_8.name())
			decodedFileName
		} catch (error: Exception) {
			error.printStackTrace()
			encodedString
		}
	}
	
	/**
	 * Retrieves the file name from a given URI.
	 * @param context The application context.
	 * @param uri The URI of the file.
	 * @return The file name, or null if it cannot be retrieved.
	 */
	@JvmStatic
	fun getFileNameFromUri(context: Context, uri: Uri): String? {
		try {
			var fileName: String? = null
			if ("content" == uri.scheme) {
				val cursor = context.contentResolver.query(uri, null, null, null, null)
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						val nameIndex = cursor.getColumnIndex(DISPLAY_NAME)
						if (nameIndex != -1) {
							fileName = cursor.getString(nameIndex)
						}
					}
					cursor.close()
				}
			} else if ("file" == uri.scheme) {
				fileName = File(uri.path!!).name
			}
			return fileName
		} catch (error: Exception) {
			error.printStackTrace()
			return null
		}
	}
	
	/**
	 * Retrieves a `File` object from a given URI.
	 * @param uri The URI of the file.
	 * @return The `File` object, or null if it cannot be created.
	 */
	@JvmStatic
	fun getFileFromUri(uri: Uri): File? {
		return try {
			val filePath = uri.path
			val file = if (filePath != null) File(filePath) else null
			file
		} catch (error: Exception) {
			error.printStackTrace()
			null
		}
	}
	
	/**
	 * Saves a string of data to the internal storage of the app.
	 * @param fileName The name of the file to save the data to.
	 * @param data The string data to save.
	 */
	@JvmStatic
	fun saveStringToInternalStorage(fileName: String, data: String) {
		val context = INSTANCE
		try {
			val fileOutputStream = context.openFileOutput(fileName, MODE_PRIVATE)
			fileOutputStream.write(data.toByteArray())
			fileOutputStream.close()
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Reads a string of data from the internal storage of the app.
	 * @param fileName The name of the file to read the data from.
	 * @return The string data read from the file.
	 * @throws Exception If there is an error reading the file.
	 */
	@JvmStatic
	fun readStringFromInternalStorage(fileName: String): String {
		val context = INSTANCE
		return try {
			val fileInputStream: FileInputStream = context.openFileInput(fileName)
			val content = fileInputStream.readBytes().toString(Charsets.UTF_8)
			fileInputStream.close()
			content
		} catch (error: Exception) {
			throw error
		}
	}
	
	/**
	 * Sanitizes a file name by replacing invalid characters with underscores.
	 * @param fileName The original file name to sanitize.
	 * @return The sanitized file name.
	 */
	@JvmStatic
	fun sanitizeFileNameExtreme(fileName: String): String {
		val sanitizedFileName = fileName.replace(Regex("[^a-zA-Z0-9()@\\[\\]_.-]"), "_")
			.replace(" ", "_")
			.replace("___", "_")
			.replace("__", "_")
		return sanitizedFileName
	}
	
	/**
	 * Sanitizes a file name by removing invalid characters and trimming excess underscores and spaces.
	 * @param fileName The original file name to sanitize.
	 * @return The sanitized file name.
	 */
	@JvmStatic
	fun sanitizeFileNameNormal(fileName: String): String {
		val sanitizedFileName = fileName
			.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}\u0000-\u001F\u007F]"), "_")
			.trimEnd('.')
			.trim()
			.replace(Regex("_+"), "_")
			.replace(" ", "_")
			.replace("___", "_")
			.replace("__", "_")
		return sanitizedFileName
	}
	
	/**
	 * Checks whether a file name is valid and can be created in the app's internal storage.
	 * @param fileName The file name to check.
	 * @return `true` if the file name is valid, `false` otherwise.
	 */
	@JvmStatic
	fun isFileNameValid(fileName: String): Boolean {
		return try {
			val directory = File(internalDataFolder.getAbsolutePath(INSTANCE))
			val tempFile = File(directory, fileName)
			tempFile.createNewFile()
			tempFile.delete()
			true
		} catch (error: IOException) {
			error.printStackTrace()
			false
		}
	}
	
	/**
	 * Checks if a given file is writable.
	 * @param file The file to check.
	 * @return `true` if the file is writable, `false` otherwise.
	 */
	@JvmStatic
	fun isWritableFile(file: DocumentFile?): Boolean {
		val isWritable = file?.canWrite() ?: false
		return isWritable
	}
	
	/**
	 * Checks if a given folder has write access.
	 * @param folder The folder to check.
	 * @return `true` if the folder has write access, `false` otherwise.
	 */
	@JvmStatic
	fun hasWriteAccess(folder: DocumentFile?): Boolean {
		if (folder == null) {
			return false
		}
		
		return try {
			val tempFile = folder.createFile("text/plain", "temp_check_file.txt")
			if (tempFile != null) {
				INSTANCE.contentResolver.openOutputStream(tempFile.uri)?.use { stream ->
					stream.write("test".toByteArray())
					stream.flush()
				}
				tempFile.delete()
				true
			} else false
		} catch (error: IOException) {
			error.printStackTrace()
			false
		}
	}
	
	/**
	 * Creates an empty file in the given context and file.
	 * @param context The context used to access the content resolver.
	 * @param file The document file where the empty file is created.
	 * @param fileSize The size of the empty file to create.
	 * @return `true` if the file is created successfully, `false` otherwise.
	 */
	@JvmStatic
	fun writeEmptyFile(context: Context, file: DocumentFile, fileSize: Long): Boolean {
		return try {
			val contentResolver: ContentResolver = context.contentResolver
			val outputStream: OutputStream? = contentResolver.openOutputStream(file.uri)
			
			outputStream?.use { stream ->
				val placeholder = ByteArray(fileSize.toInt())
				stream.write(placeholder)
				stream.flush()
			}
			true
		} catch (error: IOException) {
			error.printStackTrace()
			false
		}
	}
	
	/**
	 * Generates a unique file name by appending a number to the file name if it already exists in the directory.
	 * @param fileDirectory The directory where the file will be stored.
	 * @param originalFileName The original file name to sanitize.
	 * @return A unique file name.
	 */
	@JvmStatic
	fun generateUniqueFileName(fileDirectory: DocumentFile, originalFileName: String): String {
		var sanitizedFileName = sanitizeFileNameExtreme(originalFileName)
		var index = 1
		val regex = Regex("^(\\d+)_")
		
		while (fileDirectory.findFile(sanitizedFileName) != null) {
			val matchResult = regex.find(sanitizedFileName)
			if (matchResult != null) {
				val currentIndex = matchResult.groupValues[1].toInt()
				sanitizedFileName = sanitizedFileName.replaceFirst(regex, "")
				index = currentIndex + 1
			}
			sanitizedFileName = "${index}_$sanitizedFileName"
			index++
		}
		
		return sanitizedFileName
	}
	
	/**
	 * Finds the first file in the given directory whose name starts with the specified prefix.
	 *
	 * @param internalDir The directory to search within.
	 * @param namePrefix The prefix the file name should start with.
	 * @return The first file matching the prefix, or null if no file is found.
	 */
	@JvmStatic
	fun findFileStartingWith(internalDir: File, namePrefix: String): File? {
		val result = internalDir.listFiles()?.find {
			it.isFile && it.name.startsWith(namePrefix)
		}
		return result
	}
	
	/**
	 * Creates a new directory within the specified parent folder.
	 *
	 * @param parentFolder The parent folder where the new directory should be created.
	 * @param folderName The name of the new directory.
	 * @return The newly created directory, or null if the creation fails.
	 */
	@JvmStatic
	fun makeDirectory(parentFolder: DocumentFile?, folderName: String): DocumentFile? {
		val newDirectory = parentFolder?.createDirectory(folderName)
		return newDirectory
	}
	
	/**
	 * Returns the MIME type of a file based on its extension or content.
	 *
	 * @param fileName The name of the file.
	 * @return The MIME type of the file, or null if it cannot be determined.
	 */
	@JvmStatic
	fun getMimeType(fileName: String): String? {
		val extension = getFileExtension(fileName)?.lowercase(Locale.getDefault())
		val mimeType = extension?.let {
			MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
		} ?: run {
			val uri = "content://$extension".toUri()
			INSTANCE.contentResolver.getType(uri)
		}
		return mimeType
	}
	
	/**
	 * Extracts the file extension from the given file name.
	 *
	 * @param fileName The name of the file.
	 * @return The file extension, or null if the file name does not have an extension.
	 */
	@JvmStatic
	fun getFileExtension(fileName: String): String? {
		return fileName.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
	}
	
	/**
	 * Determines whether the given file has one of the specified extensions.
	 *
	 * @param file The file to check.
	 * @param extensions The list of file extensions to check against.
	 * @return True if the file's name ends with one of the specified extensions, false otherwise.
	 */
	@JvmStatic
	fun DocumentFile.isFileType(extensions: Array<String>): Boolean {
		return endsWithExtension(name, extensions)
	}
	
	/**
	 * Checks if the given file is an audio file based on its extension.
	 *
	 * @param file The file to check.
	 * @return True if the file is an audio file, false otherwise.
	 */
	@JvmStatic
	fun isAudio(file: DocumentFile): Boolean {
		return file.isFileType(MUSIC_EXTENSIONS)
	}
	
	/**
	 * Checks if the given file is an archive file based on its extension.
	 *
	 * @param file The file to check.
	 * @return True if the file is an archive file, false otherwise.
	 */
	@JvmStatic
	fun isArchive(file: DocumentFile): Boolean {
		return file.isFileType(ARCHIVE_EXTENSIONS)
	}
	
	/**
	 * Checks if the given file is a program file based on its extension.
	 *
	 * @param file The file to check.
	 * @return True if the file is a program file, false otherwise.
	 */
	@JvmStatic
	fun isProgram(file: DocumentFile): Boolean {
		return file.isFileType(PROGRAM_EXTENSIONS)
	}
	
	/**
	 * Checks if the given file is a video file based on its extension.
	 *
	 * @param file The file to check.
	 * @return True if the file is a video file, false otherwise.
	 */
	@JvmStatic
	fun isVideo(file: DocumentFile): Boolean {
		return file.isFileType(VIDEO_EXTENSIONS)
	}
	
	/**
	 * Checks if the given file is a document file based on its extension.
	 *
	 * @param file The file to check.
	 * @return True if the file is a document file, false otherwise.
	 */
	@JvmStatic
	fun isDocument(file: DocumentFile): Boolean {
		return file.isFileType(DOCUMENT_EXTENSIONS)
	}
	
	/**
	 * Checks if the given file is an image file based on its extension.
	 *
	 * @param file The file to check.
	 * @return True if the file is an image file, false otherwise.
	 */
	@JvmStatic
	fun isImage(file: DocumentFile): Boolean {
		return file.isFileType(IMAGE_EXTENSIONS)
	}
	
	/**
	 * Returns the type of the given file based on its extension.
	 *
	 * @param file The file to check.
	 * @return A string representing the file's type (e.g., "Audio", "Video", etc.).
	 */
	@JvmStatic
	fun getFileType(file: DocumentFile): String {
		return getFileType(file.name)
	}
	
	/**
	 * Returns the type of the file based on its name.
	 *
	 * @param fileName The name of the file.
	 * @return A string representing the file's type (e.g., "Audio", "Video", etc.).
	 */
	@JvmStatic
	fun getFileType(fileName: String?): String {
		return when {
			isAudioByName(fileName) -> getText(R.string.title_sounds)
			isArchiveByName(fileName) -> getText(R.string.title_archives)
			isProgramByName(fileName) -> getText(R.string.title_programs)
			isVideoByName(fileName) -> getText(R.string.title_videos)
			isDocumentByName(fileName) -> getText(R.string.title_documents)
			isImageByName(fileName) -> getText(R.string.title_images)
			else -> getText(R.string.title_others)
		}
	}
	
	/**
	 * Triggers a media scan to add the given file to the device's media store.
	 *
	 * @param file The file to add to the media store.
	 */
	@JvmStatic
	fun addToMediaStore(file: File) {
		try {
			val fileUri = Uri.fromFile(file)
			val mediaScanIntent = Intent(ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
				data = fileUri
			}
			INSTANCE.sendBroadcast(mediaScanIntent)
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Checks if the given file name ends with one of the specified extensions.
	 *
	 * @param fileName The name of the file.
	 * @param extensions The list of extensions to check against.
	 * @return True if the file name ends with one of the extensions, false otherwise.
	 */
	@JvmStatic
	fun endsWithExtension(fileName: String?, extensions: Array<String>): Boolean {
		return extensions.any {
			fileName?.lowercase(Locale.getDefault())?.endsWith(".$it") == true
		}
	}
	
	/**
	 * Checks if the file name represents an audio file based on its extension.
	 *
	 * @param name The name of the file.
	 * @return True if the file is an audio file, false otherwise.
	 */
	@JvmStatic
	fun isAudioByName(name: String?): Boolean {
		return endsWithExtension(name, MUSIC_EXTENSIONS)
	}
	
	/**
	 * Checks if the file name represents an archive file based on its extension.
	 *
	 * @param name The name of the file.
	 * @return True if the file is an archive file, false otherwise.
	 */
	@JvmStatic
	fun isArchiveByName(name: String?): Boolean {
		return endsWithExtension(name, ARCHIVE_EXTENSIONS)
	}
	
	/**
	 * Checks if the file name represents a program file based on its extension.
	 *
	 * @param name The name of the file.
	 * @return True if the file is a program file, false otherwise.
	 */
	@JvmStatic
	fun isProgramByName(name: String?): Boolean {
		return endsWithExtension(name, PROGRAM_EXTENSIONS)
	}
	
	/**
	 * Checks if the file name represents a video file based on its extension.
	 *
	 * @param name The name of the file.
	 * @return True if the file is a video file, false otherwise.
	 */
	@JvmStatic
	fun isVideoByName(name: String?): Boolean {
		return endsWithExtension(name, VIDEO_EXTENSIONS)
	}
	
	/**
	 * Checks if the file name represents a document file based on its extension.
	 *
	 * @param name The name of the file.
	 * @return True if the file is a document file, false otherwise.
	 */
	@JvmStatic
	fun isDocumentByName(name: String?): Boolean {
		return endsWithExtension(name, DOCUMENT_EXTENSIONS)
	}
	
	/**
	 * Checks if the file name represents an image file based on its extension.
	 *
	 * @param name The name of the file.
	 * @return True if the file is an image file, false otherwise.
	 */
	@JvmStatic
	fun isImageByName(name: String?): Boolean {
		return endsWithExtension(name, IMAGE_EXTENSIONS)
	}
	
	/**
	 * Removes the file extension from the file name.
	 *
	 * @param fileName The name of the file.
	 * @return The file name without the extension, or the original name if no extension exists.
	 */
	@JvmStatic
	fun getFileNameWithoutExtension(fileName: String): String {
		return try {
			val dotIndex = fileName.lastIndexOf('.')
			if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
		} catch (error: Exception) {
			error.printStackTrace()
			fileName
		}
	}
	
}
