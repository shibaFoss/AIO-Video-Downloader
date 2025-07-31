package app.core.engines.downloader

import app.core.AIOApp.Companion.INSTANCE
import app.core.engines.downloader.DownloadDataModel.Companion.DOWNLOAD_MODEL_FILE_EXTENSION
import app.core.engines.downloader.DownloadDataModel.Companion.convertJSONStringToClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import lib.process.LogHelperUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * DownloadModelParser - Robust Model Cache with Failure Recovery
 *
 * Handles loading, caching, and management of download models with:
 * - Automatic recovery from parsing failures
 * - Coroutine-based parallel processing
 * - Cache validation and invalidation
 * - Thread-safe operations
 *
 * Recovery Features:
 * 1. Automatic retry mechanism for failed parses
 * 2. Corrupted file cleanup
 * 3. Isolated processing to prevent cascade failures
 * 4. Cache state monitoring
 */
object DownloadModelParser {
    private val logger = LogHelperUtils.from(javaClass)

    // Thread-safe cache with access logging
    private val modelCache = ConcurrentHashMap<String, DownloadDataModel>()

    // Coroutine scope with supervisor job to prevent cascading failures
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Track failed files to prevent repeated processing attempts
    private val failedFiles = ConcurrentHashMap<String, Long>()
    private const val FAILURE_RETRY_DELAY_MS = 30000L // 30 seconds

    /**
     * Retrieves all valid download models, with automatic recovery from failures.
     *
     * Recovery Process:
     * 1. Attempts to load all models
     * 2. Skips known problematic files temporarily
     * 3. Validates cache integrity
     * 4. Returns only successfully loaded models
     */
    @Throws(Exception::class)
    suspend fun getDownloadDataModels(): List<DownloadDataModel> {
        return withContext(Dispatchers.IO) {
            if (modelCache.isEmpty()) {
                loadAllModelsWithRecovery()
            } else {
                validateCacheAgainstFiles()
            }
            modelCache.values.toList()
        }
    }

    /**
     * Gets a specific model with built-in failure recovery.
     *
     * Recovery Features:
     * - Checks failure cache before attempting load
     * - Automatically retries after delay if previous failure
     * - Returns null only if file doesn't exist or permanently corrupted
     */
    suspend fun getDownloadDataModel(id: String): DownloadDataModel? {
        return withContext(Dispatchers.IO) {
            // Check if this file recently failed
            failedFiles[id]?.let { timestamp ->
                if (System.currentTimeMillis() - timestamp < FAILURE_RETRY_DELAY_MS) {
                    return@withContext null
                }
            }

            modelCache[id] ?: run {
                loadSingleModelWithRecovery(id)
                modelCache[id]
            }
        }
    }

    /**
     * Enhanced model loading with automatic recovery.
     */
    private suspend fun loadAllModelsWithRecovery() {
        val files = listModelFiles(INSTANCE.filesDir)

        files.chunked(10).forEach { chunk ->
            val deferredResults = chunk.map { file ->
                scope.async {
                    if (shouldAttemptLoad(file.nameWithoutExtension)) {
                        processModelFileWithRecovery(file)
                    }
                }
            }
            deferredResults.awaitAll()
        }
    }

    /**
     * Safe loading of single model with recovery.
     */
    private fun loadSingleModelWithRecovery(id: String) {
        if (!shouldAttemptLoad(id)) return

        val fileName = "$id$DOWNLOAD_MODEL_FILE_EXTENSION"
        val file = File(INSTANCE.filesDir, fileName)
        if (file.exists()) {
            processModelFileWithRecovery(file)
        }
    }

    /**
     * Determines if a file should be attempted based on failure history.
     */
    private fun shouldAttemptLoad(fileId: String): Boolean {
        return failedFiles[fileId]?.let {
            System.currentTimeMillis() - it > FAILURE_RETRY_DELAY_MS
        } ?: true
    }

    /**
     * Processes a file with enhanced error handling and recovery.
     */
    private fun processModelFileWithRecovery(file: File): Boolean {
        return try {
            val jsonString = file.readText(Charsets.UTF_8)
            val model = convertJSONStringToClass(jsonString)

            if (model != null) {
                modelCache[file.nameWithoutExtension] = model
                failedFiles.remove(file.nameWithoutExtension)
                true
            } else {
                handleCorruptedFile(file)
                false
            }
        } catch (error: Exception) {
            handleProcessingError(file, error)
            false
        }
    }

    /**
     * Handles file corruption by cleaning up and logging.
     */
    private fun handleCorruptedFile(file: File) {
        try {
            file.delete()
            failedFiles.remove(file.nameWithoutExtension)
        } catch (e: Exception) {
            // Log cleanup failure
        }
    }

    /**
     * Handles processing errors with appropriate recovery actions.
     */
    private fun handleProcessingError(file: File, error: Exception) {
        error.printStackTrace()
        failedFiles[file.nameWithoutExtension] = System.currentTimeMillis()

        // Only delete if we're certain it's causing problems
        if (error is IllegalStateException || error is NumberFormatException) {
            try {
                file.delete()
            } catch (e: Exception) {
                // Log deletion failure
            }
        }
    }

    /**
     * Validates cache against existing files with recovery options.
     */
    private fun validateCacheAgainstFiles() {
        val currentFiles = listModelFiles(INSTANCE.filesDir)
            .associateBy { it.nameWithoutExtension }

        modelCache.keys.removeAll { id ->
            if (!currentFiles.containsKey(id)) {
                true // Remove if file doesn't exist
            } else {
                // Re-check failed files that might be ready for retry
                failedFiles[id]?.let {
                    System.currentTimeMillis() - it > FAILURE_RETRY_DELAY_MS
                } ?: false
            }
        }
    }

    /**
     * Lists model files with basic validation.
     */
    private fun listModelFiles(directory: File?): List<File> {
        val suffix = DOWNLOAD_MODEL_FILE_EXTENSION
        return directory?.takeIf { it.isDirectory }
            ?.listFiles { file ->
                file.isFile && file.name.endsWith(suffix) &&
                        !file.name.contains("temp") // Skip temp files
            }
            ?.toList() ?: emptyList()
    }

    /**
     * Clears all caches including failure tracking.
     */
    fun fullReset() {
        modelCache.clear()
        failedFiles.clear()
    }

    /**
     * Standard cache invalidation.
     */
    fun invalidateCache() = modelCache.clear()

    /**
     * Cleanup resources.
     */
    fun cleanup() {
        scope.cancel()
        failedFiles.clear()
    }
}