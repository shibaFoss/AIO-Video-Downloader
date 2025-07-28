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
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * A parser utility for managing and caching download model data.
 *
 * This class provides functionality to:
 * - Load download models from storage
 * - Cache models for performance
 * - Validate cache against actual files
 * - Handle model data in a thread-safe manner
 * - Process models in parallel using coroutines
 */
object DownloadModelParser {
	
	// Thread-safe cache for storing loaded models
	private val modelCache = ConcurrentHashMap<String, DownloadDataModel>()
	
	// Coroutine scope for parallel processing of model files
	private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
	
	/**
	 * Retrieves all download data models, loading them if cache is empty.
	 *
	 * @return List of all available DownloadDataModel instances
	 * @throws Exception if there are issues loading the models
	 */
	@Throws(Exception::class)
	suspend fun getDownloadDataModels(): List<DownloadDataModel> {
		return withContext(Dispatchers.IO) {
			if (modelCache.isEmpty()) loadAllModels() else validateCacheAgainstFiles()
			return@withContext modelCache.values.toList()
		}
	}
	
	/**
	 * Retrieves a specific download model by ID, loading it if not in cache.
	 *
	 * @param id The unique identifier of the download model
	 * @return The requested DownloadDataModel or null if not found
	 */
	suspend fun getDownloadDataModel(id: String): DownloadDataModel? {
		return withContext(Dispatchers.IO) {
			modelCache[id] ?: run {
				loadSingleModel(id)
				modelCache[id]
			}
		}
	}
	
	/**
	 * Loads all model files from storage in parallel chunks.
	 */
	private suspend fun loadAllModels() {
		val files = listModelFiles(INSTANCE.filesDir)
		// Process files in chunks of 10 for balanced parallel loading
		files.chunked(10).forEach { chunk ->
			val deferredResults = chunk.map { file ->
				scope.async { processModelFile(file) }
			}; deferredResults.awaitAll()
		}
	}
	
	/**
	 * Loads a single model file by ID.
	 *
	 * @param id The ID of the model to load
	 */
	private fun loadSingleModel(id: String) {
		val fileName = "$id$DOWNLOAD_MODEL_FILE_EXTENSION"
		val file = File(INSTANCE.filesDir, fileName)
		if (file.exists()) processModelFile(file)
	}
	
	/**
	 * Validates the cache against actual files, removing any stale entries.
	 */
	private fun validateCacheAgainstFiles() {
		val currentFiles = listModelFiles(INSTANCE.filesDir)
			.associateBy { it.nameWithoutExtension }
		modelCache.keys.removeAll { !currentFiles.containsKey(it) }
	}
	
	/**
	 * Processes an individual model file, adding it to cache if valid.
	 *
	 * @param file The model file to process
	 * @return true if the file was processed successfully, false otherwise
	 */
	private fun processModelFile(file: File): Boolean {
		return try {
			val jsonString = file.readText(Charsets.UTF_8)
			convertJSONStringToClass(jsonString)?.let { model ->
				modelCache[file.nameWithoutExtension] = model; true
			}
				?: run { file.delete(); false }
		} catch (error: Exception) {
			error.printStackTrace()
			file.delete()
			false
		}
	}
	
	/**
	 * Lists all model files in the specified directory.
	 *
	 * @param directory The directory to search for model files
	 * @return List of found model files (empty if none found)
	 */
	private fun listModelFiles(directory: File?): List<File> {
		val suffix = DOWNLOAD_MODEL_FILE_EXTENSION
		return directory?.takeIf { it.isDirectory }
			?.listFiles { file -> file.isFile && file.name.endsWith(suffix) }
			?.toList() ?: emptyList()
	}
	
	/**
	 * Clears the model cache.
	 */
	fun invalidateCache() = modelCache.clear()
	
	/**
	 * Cleans up resources by canceling the coroutine scope.
	 */
	fun cleanup() = scope.cancel()
}