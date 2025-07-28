package lib.texts

import android.text.Html.FROM_HTML_MODE_COMPACT
import android.text.Html.fromHtml
import android.text.Spanned
import app.core.AIOApp.Companion.INSTANCE
import lib.process.LocalizationHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * Utility object providing various common text processing functions.
 * It includes string trimming, capitalization, HTML parsing, and localized text fetching.
 */
object CommonTextUtils {
	
	/**
	 * Removes consecutive slashes (`/`) and replaces them with a single slash.
	 */
	@JvmStatic
	fun removeDuplicateSlashes(input: String?): String? {
		if (input == null) return null
		val result = input.replace("/{2,}".toRegex(), "/")
		return result
	}
	
	/**
	 * Retrieves a localized string using a given resource ID.
	 */
	@JvmStatic
	fun getText(resID: Int): String {
		return LocalizationHelper.getLocalizedString(INSTANCE, resID)
	}
	
	/**
	 * Generates a random alphanumeric string of the specified [length].
	 */
	@JvmStatic
	fun generateRandomString(length: Int): String {
		val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcd" +
				"efghijklmnopqrstuvwxyz0123456789"
		val sb = StringBuilder(length)
		for (index in 0 until length) {
			val randomIndex = (characters.indices).random()
			sb.append(characters[randomIndex])
		}; return sb.toString()
	}
	
	/**
	 * Trims the input to a maximum of 100 characters, removing incomplete or invalid trailing characters.
	 */
	@JvmStatic
	fun cutTo100Chars(input: String?): String? {
		if (input == null) return null
		if (input.length > 100) {
			var result = input.substring(0, 100)
			val lastChar = result[result.length - 1]
			if (lastChar.isWhitespace() || !lastChar.isValidCharacter()) {
				result = result.trimEnd()
			}
			
			if (!lastChar.isWhitespace() && result.isNotEmpty()) {
				val secondLastChar = result[result.length - 2]
				if (!secondLastChar.isValidCharacter()) {
					result = result.dropLast(1)
				}
			}; return result
		}; return input
	}
	

	@JvmStatic
	fun cutTo30Chars(input: String): String {
		if (input.length > 30) {
			var result = input.substring(0, 30)
			val lastChar = result[result.length - 1]
			if (lastChar.isWhitespace() || !lastChar.isValidCharacter()) {
				result = result.trimEnd()
			}
			
			if (!lastChar.isWhitespace() && result.isNotEmpty()) {
				val secondLastChar = result[result.length - 2]
				if (!secondLastChar.isValidCharacter()) {
					result = result.dropLast(1)
				}
			}; return result
		}; return input
	}
	

	@JvmStatic
	fun cutTo60Chars(input: String?): String? {
		if (input == null) return null
		if (input.length > 60) {
			var result = input.substring(0, 60)
			val lastChar = result[result.length - 1]
			if (lastChar.isWhitespace() || !lastChar.isValidCharacter()) {
				result = result.trimEnd()
			}
			
			if (!lastChar.isWhitespace() && result.isNotEmpty()) {
				val secondLastChar = result[result.length - 2]
				if (!secondLastChar.isValidCharacter()) {
					result = result.dropLast(1)
				}
			}; return result
		}; return input
	}
	
	/**
	 * Extension to check if a character is valid for text display.
	 */
	@JvmStatic
	fun Char.isValidCharacter(): Boolean {
		return this.isLetterOrDigit() ||
				this in setOf('_', '-', '.', '@', ' ', '[', ']', '(', ')')
	}
	
	/**
	 * Joins multiple [elements] into a single string separated by [delimiter].
	 */
	@JvmStatic
	fun join(delimiter: String, vararg elements: String): String {
		if (elements.isEmpty()) return ""
		val result = elements.joinToString(separator = delimiter)
		return result
	}
	
	/**
	 * Reverses the given [input] string.
	 */
	@JvmStatic
	fun reverse(input: String?): String? {
		if (input == null) return null
		val result = StringBuilder(input).reverse().toString()
		return result
	}
	
	/**
	 * Capitalizes the first letter of the given string.
	 */
	@JvmStatic
	fun capitalizeFirstLetter(string: String?): String? {
		if (string.isNullOrEmpty()) return null
		val first = string[0]
		val capitalized = if (Character.isUpperCase(first)) string
		else first.uppercaseChar().toString() + string.substring(1)
		return capitalized
	}
	
	/**
	 * Capitalizes the first letter of each word in the input string.
	 * Preserves whitespace-only strings and returns null if input is null.
	 */
	@JvmStatic
	fun capitalizeWords(input: String?): String? {
		if (input.isNullOrBlank()) return input // handles null, empty, and whitespace-only
		return input
			.trim()
			.split("\\s+".toRegex())
			.joinToString(" ") { word ->
				word.replaceFirstChar {
					if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
				}
			}
	}
	
	
	/**
	 * Converts an HTML-formatted [htmlString] into a Spanned text object.
	 */
	@JvmStatic
	fun fromHtmlStringToSpanned(htmlString: String): Spanned {
		val result = fromHtml(htmlString, FROM_HTML_MODE_COMPACT)
		return result
	}
	
	/**
	 * Reads an HTML string stored in the raw resources identified by [resId].
	 */
	@JvmStatic
	fun getHtmlString(resId: Int): String {
		val result = convertRawHtmlFileToString(resId)
		return result
	}
	
	/**
	 * Converts a raw HTML resource file to a plain string.
	 */
	@JvmStatic
	fun convertRawHtmlFileToString(resourceId: Int): String {
		val inputStream = INSTANCE.resources.openRawResource(resourceId)
		val reader = BufferedReader(InputStreamReader(inputStream))
		val stringBuilder = StringBuilder()
		var line: String?
		try {
			while (reader.readLine()
					.also { line = it } != null) stringBuilder.append(line)
		} catch (error: Throwable) {
			error.printStackTrace()
		} finally {
			try {
				inputStream.close()
				reader.close()
			} catch (err: Exception) {
				err.printStackTrace()
			}
		}; return stringBuilder.toString()
	}
	
	/**
	 * Counts how many times the character [char] appears in the [input] string.
	 */
	@JvmStatic
	fun countOccurrences(input: String?, char: Char?): Int {
		if (input == null || char == null) return 0
		val count = input.count { it == char }
		return count
	}
	
	/**
	 * Removes empty or blank lines from a multiline string.
	 */
	@JvmStatic
	fun removeEmptyLines(input: String?): String? {
		if (input.isNullOrEmpty()) return null
		return input.split("\n")
			.filter { it.isNotBlank() }
			.joinToString("\n")
	}
}
