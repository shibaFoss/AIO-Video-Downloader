package lib.process

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext

/**
 * A utility object to simplify threading and coroutine operations using Kotlin Coroutines.
 *
 * Provides methods for executing tasks on background threads, main thread, with timeout, or in lifecycle-aware scopes.
 * Also includes coroutine helpers for better structure and error handling.
 */
object ThreadsUtility : CoroutineScope {
	
	/** Logger for debugging and error tracking. */
	private val logger = LogHelperUtils.from(javaClass)
	
	/** Default timeout duration for background tasks (in milliseconds). */
	private const val JOB_TIMEOUT = 500000L
	
	/** The root SupervisorJob for the scope, allowing structured concurrency. */
	private val job = SupervisorJob()
	
	/** The coroutine context combining a default dispatcher, the job, and an error handler. */
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Default + job + CoroutineExceptionHandler { _, throwable ->
			logger.d("Coroutine error: ${throwable.message}")
		}
	
	/**
	 * Executes a suspending block of code in the IO dispatcher with a default timeout.
	 *
	 * @param codeBlock The suspending code block to execute.
	 * @param errorHandler Optional callback for handling any thrown exception.
	 * @return The launched coroutine [Job].
	 */
	fun executeInBackground(
		codeBlock: suspend () -> Unit,
		errorHandler: ((Throwable) -> Unit)? = null,
	): Job = launch(Dispatchers.IO) {
		try {
			withTimeout(JOB_TIMEOUT) { codeBlock() }
		} catch (error: Exception) {
			logger.e("Error in executing code in background thread:", error)
			errorHandler?.invoke(error)
		}
	}
	
	/**
	 * Executes a suspending block of code on the main thread.
	 *
	 * @param codeBlock The suspending code block to run on the main thread.
	 */
	suspend fun executeOnMain(codeBlock: suspend () -> Unit) {
		withContext(Dispatchers.Main) { codeBlock() }
	}
	
	/**
	 * Executes a background task and then updates the UI on the main thread with the result.
	 *
	 * @param backgroundTask The suspending background task to execute.
	 * @param uiTask The suspending UI update task with the result from background.
	 * @return The launched coroutine [Job].
	 */
	fun <T> executeAsync(
		backgroundTask: suspend () -> T,
		uiTask: suspend (T) -> Unit
	): Job = launch {
		try {
			val result = withContext(Dispatchers.IO) {
				backgroundTask()
			}
			executeOnMain { uiTask(result) }
		} catch (error: Exception) {
			logger.e("Error in executing code in async background thread:", error)
		}
	}
	
	/**
	 * Cancels all child coroutines launched under the [SupervisorJob] context.
	 */
	fun cancelAll() = job.cancelChildren()
	
	/**
	 * Returns the lifecycle-aware [CoroutineScope] for the given [LifecycleOwner].
	 *
	 * @param lifecycleOwner The lifecycle owner, usually an Activity or Fragment.
	 * @return The corresponding lifecycleScope for coroutine operations.
	 */
	fun lifecycleScope(lifecycleOwner: LifecycleOwner) = lifecycleOwner.lifecycleScope
}

/**
 * Extension function to launch a coroutine in the IO dispatcher within a given [CoroutineScope].
 *
 * @param block The suspending block to execute in the IO context.
 * @return The launched coroutine [Job].
 */
fun CoroutineScope.executeInIO(block: suspend CoroutineScope.() -> Unit): Job {
	return launch(Dispatchers.IO) { block() }
}

/**
 * Suspends the current coroutine and switches to the Main thread for the given block.
 *
 * @param block The suspending block to run on the Main dispatcher.
 * @return The result of the block execution.
 */
suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T {
	return withContext(Dispatchers.Main) { block() }
}
