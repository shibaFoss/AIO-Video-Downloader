package lib.files

import java.text.DecimalFormat

/**
 * A utility object that provides methods for formatting file sizes into human-readable formats.
 * It supports converting byte sizes into KB, MB, GB, and B with proper formatting.
 */
object FileSizeFormatter {
    
    // Constants for converting bytes to higher units
    private const val ONE_KB: Long = 1024       // 1 KB = 1024 bytes
    private const val ONE_MB = ONE_KB * 1024     // 1 MB = 1024 KB
    private const val ONE_GB = ONE_MB * 1024     // 1 GB = 1024 MB
    
    /**
     * Converts a file size in bytes to a human-readable format (e.g., 1.23 MB).
     *
     * @param size The file size in bytes.
     * @return A human-readable string representation of the file size (e.g., "1.23 MB").
     * @throws IllegalArgumentException If the size is negative.
     */
    @JvmStatic
    fun humanReadableSizeOf(size: Long): String {
        require(size >= 0) { "File size cannot be negative: $size" }
        return humanReadableSizeOf(size.toDouble())
    }
    
    /**
     * Converts a file size in bytes (as a double) to a human-readable format (e.g., 1.23 MB).
     *
     * @param size The file size in bytes as a double.
     * @return A human-readable string representation of the file size (e.g., "1.23 MB").
     * @throws IllegalArgumentException If the size is negative.
     */
    @JvmStatic
    fun humanReadableSizeOf(size: Double): String {
        require(size >= 0) { "File size cannot be negative: $size" }
        
        val df = DecimalFormat("##.##")  // Format to two decimal places
        return when {
            size >= ONE_GB -> formatUnit(size / ONE_GB, "GB", df)  // For GB
            size >= ONE_MB -> formatUnit(size / ONE_MB, "MB", df)  // For MB
            size >= ONE_KB -> formatUnit(size / ONE_KB, "KB", df)  // For KB
            else -> formatUnit(size, "B", df)  // For bytes (B)
        }
    }
    
    /**
     * Helper function to format the size value with the appropriate unit.
     *
     * @param value The file size value.
     * @param unit The unit for the size (e.g., "MB", "GB").
     * @param df The DecimalFormat instance to format the value.
     * @return A string representing the formatted file size with unit.
     */
    private fun formatUnit(value: Double, unit: String, df: DecimalFormat): String {
        val formatted = "${df.format(value)} $unit"
        return formatted
    }
}
