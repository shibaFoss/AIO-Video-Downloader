package lib.device

import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDateTime.ofInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Locale.getDefault
import java.util.concurrent.TimeUnit

/**
 * Utility object for handling date and time operations across the application.
 */
object DateTimeUtils {
	
	/** Standard formatter for date and time (e.g., 2023-08-01 14:30:00). */
	@JvmStatic
	val dateFormatter: DateTimeFormatter =
		DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
	
	/** Formatter for 12-hour clock time format (e.g., 02:30 PM). */
	@JvmStatic
	val timeFormatter12Hour: DateTimeFormatter =
		DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
	
	/** Formatter for 24-hour clock time format (e.g., 14:30). */
	@JvmStatic
	val timeFormatter24Hour: DateTimeFormatter =
		DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
	
	/**
	 * Returns the current date and time formatted with the default formatter.
	 */
	@JvmStatic
	fun getCurrentDateTime(): String {
		return LocalDateTime.now().format(dateFormatter)
	}
	
	/**
	 * Returns the current date formatted using the given format.
	 */
	@JvmStatic
	fun getCurrentDate(format: String?): String {
		val formatter = DateTimeFormatter.ofPattern(format, getDefault())
		return LocalDateTime.now().format(formatter)
	}
	
	/**
	 * Returns the current time formatted using the given format.
	 */
	@JvmStatic
	fun getCurrentTime(format: String?): String {
		val formatter = DateTimeFormatter.ofPattern(format, getDefault())
		return LocalDateTime.now().format(formatter)
	}
	
	/**
	 * Converts a timestamp to a formatted date string.
	 */
	@JvmStatic
	fun timestampToDateString(timestamp: Long, format: String?): String {
		val formatter = DateTimeFormatter.ofPattern(format, getDefault())
		val dateTime = ofInstant(Date(timestamp).toInstant(), ZoneId.systemDefault())
		return dateTime.format(formatter)
	}
	
	/**
	 * Parses a date string into a timestamp using the given format.
	 */
	@JvmStatic
	fun dateStringToTimestamp(dateString: String?, format: String?): Long {
		val formatter = DateTimeFormatter.ofPattern(format, getDefault())
		val dateTime = LocalDateTime.parse(dateString, formatter)
		return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()).time
	}
	
	/**
	 * Returns the number of days between two formatted date strings.
	 */
	@JvmStatic
	fun getDaysDifference(startDate: String?, endDate: String?, format: String?): Long {
		val formatter = DateTimeFormatter.ofPattern(format, getDefault())
		val start = LocalDateTime.parse(startDate, formatter)
		val end = LocalDateTime.parse(endDate, formatter)
		return Duration.between(start, end).toDays()
	}
	
	/**
	 * Formats a date with a suffix (e.g., "1st Jan (13:00)").
	 */
	@JvmStatic
	fun formatDateWithSuffix(date: Date): String {
		val dayFormatter = SimpleDateFormat("d", Locale.US)
		val monthFormatter = SimpleDateFormat("MMM", Locale.US)
		val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)
		
		val day = dayFormatter.format(date).toInt()
		val dayWithSuffix = "$day${getDayOfMonthSuffix(day)}"
		
		return "$dayWithSuffix ${monthFormatter.format(date)} (${timeFormatter.format(date)})"
	}
	
	/**
	 * Checks if the current time is within a specified time range.
	 */
	@JvmStatic
	fun isCurrentTimeInRange(startTime: String?, endTime: String?, format: String?): Boolean {
		val formatter = DateTimeFormatter.ofPattern(format, getDefault())
		val start = LocalDateTime.parse(startTime, formatter)
		val end = LocalDateTime.parse(endTime, formatter)
		val currentTime = LocalDateTime.now()
		return currentTime.isAfter(start) && currentTime.isBefore(end)
	}
	
	/**
	 * Converts a date string from one format to another.
	 */
	@JvmStatic
	fun formatDateString(dateString: String?, fromFormat: String?, toFormat: String?): String? {
		val fromFormatter = DateTimeFormatter.ofPattern(fromFormat, getDefault())
		val toFormatter = DateTimeFormatter.ofPattern(toFormat, getDefault())
		val dateTime = LocalDateTime.parse(dateString, fromFormatter)
		return dateTime.format(toFormatter)
	}
	
	/** Returns the current time in 12-hour format. */
	@JvmStatic
	fun getCurrentTimeIn12HourFormat(): String {
		return LocalDateTime.now().format(timeFormatter12Hour)
	}
	
	/** Returns the current time in 24-hour format. */
	@JvmStatic
	fun getCurrentTimeIn24HourFormat(): String {
		return LocalDateTime.now().format(timeFormatter24Hour)
	}
	
	/**
	 * Formats a timestamp into a readable string with a day suffix (e.g., "23rd Jul").
	 */
	@JvmStatic
	fun formatLastModifiedDate(lastModifiedTimeDate: Long): String {
		val calendar = Calendar.getInstance().apply {
			timeInMillis = lastModifiedTimeDate
		}
		val day = calendar.get(Calendar.DAY_OF_MONTH)
		val daySuffix = getDayOfMonthSuffix(day)
		val dateFormat = SimpleDateFormat("MMM", getDefault())
		val month = dateFormat.format(calendar.time)
		
		return "$day$daySuffix $month"
	}
	
	/**
	 * Returns the appropriate suffix for a day of the month (e.g., "st", "nd", "rd", "th").
	 */
	@JvmStatic
	fun getDayOfMonthSuffix(day: Int): String {
		return when {
			day in 11..13 -> "th"
			else -> when (day % 10) {
				1 -> "st"
				2 -> "nd"
				3 -> "rd"
				else -> "th"
			}
		}
	}
	
	/**
	 * Formats video duration (in milliseconds) to a readable format (HH:mm:ss or mm:ss).
	 */
	@JvmStatic
	fun formatVideoDuration(durationMs: Long?): String {
		if (durationMs == null) return "00:00"
		
		val totalSeconds = durationMs / 1000
		val hours = totalSeconds / 3600
		val minutes = (totalSeconds % 3600) / 60
		val seconds = totalSeconds % 60
		
		return if (hours > 0) {
			String.format(getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
		} else {
			String.format(getDefault(), "%02d:%02d", minutes, seconds)
		}
	}
	
	/**
	 * Formats milliseconds to time in HH:mm:ss or mm:ss format.
	 */
	@JvmStatic
	fun formatTime(milliseconds: Long): String {
		val totalSeconds = milliseconds / 1000
		val hours = totalSeconds / 3600
		val minutes = (totalSeconds % 3600) / 60
		val seconds = totalSeconds % 60
		
		return if (hours > 0) {
			String.format(getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
		} else {
			String.format(getDefault(), "%02d:%02d", minutes, seconds)
		}
	}
	
	/**
	 * Returns the number of days passed since a given timestamp.
	 */
	@JvmStatic
	fun getDaysPassedSince(lastModifiedTime: Long): Long {
		val currentTime = System.currentTimeMillis()
		val timeDifference = currentTime - lastModifiedTime
		return TimeUnit.MILLISECONDS.toDays(timeDifference)
	}
	
	/**
	 * Converts milliseconds to a formatted date-time string.
	 */
	@JvmStatic
	fun millisToDateTimeString(millis: Long): String {
		val dateTime = ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
		val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
		return dateTime.format(formatter)
	}
	
	/**
	 * Converts a formatted date-time string to milliseconds.
	 */
	@JvmStatic
	fun dateTimeStringToMillis(dateTimeString: String): Long {
		val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
		val dateTime = LocalDateTime.parse(dateTimeString, formatter)
		return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
	}
	
	/**
	 * Converts milliseconds to a human-readable time with optional suffix.
	 */
	@JvmStatic
	fun calculateTime(millis: Float, suffix: String = ""): String {
		val totalSeconds = (millis / 1000).toInt()
		val second = totalSeconds % 60
		val minute = (totalSeconds / 60) % 60
		val hour = totalSeconds / 3600
		
		val timeString = if (hour > 0) {
			String.format(getDefault(), "%d:%02d:%02d", hour, minute, second)
		} else {
			String.format(getDefault(), "%02d:%02d", minute, second)
		}
		
		return "$timeString $suffix"
	}
	
	/**
	 * Converts a duration in milliseconds to HH:mm:ss format string.
	 */
	@JvmStatic
	fun formatTimeDurationToString(durationMillis: Long): String {
		val totalSeconds = durationMillis / 1000
		val hours = totalSeconds / 3600
		val minutes = (totalSeconds % 3600) / 60
		val seconds = totalSeconds % 60
		return String.format(getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
	}
}
