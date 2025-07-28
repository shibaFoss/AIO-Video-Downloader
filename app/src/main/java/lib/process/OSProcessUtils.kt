package lib.process

import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import app.core.AIOApp

/**
 * A collection of static utility functions related to the current OS process, thread, and execution context.
 * <p>
 * Useful for logging, debugging, stack trace inspection, and app-level operations like restarts.
 *
 * Usage:
 * ```
 * val methodName = OSProcessUtils.getCurrentMethodName()
 * if (OSProcessUtils.isMainThread()) { ... }
 * OSProcessUtils.restartApp()
 * ```
 */
object OSProcessUtils {
    
    /**
     * Returns the name of the current method in execution.
     *
     * @return The method name from the call stack.
     */
    @JvmStatic
    fun getCurrentMethodName(): String {
        return Thread.currentThread().stackTrace[2].methodName
    }
    
    /**
     * Returns the name of the current class in execution.
     *
     * @return The class name from the call stack.
     */
    @JvmStatic
    fun getCurrentClassName(): String {
        return Thread.currentThread().stackTrace[2].className
    }
    
    /**
     * Returns the full stack trace of the current thread as a string.
     *
     * @return A formatted string of the current stack trace.
     */
    @JvmStatic
    fun getStackTraceAsString(): String {
        val stackTrace = Thread.currentThread().stackTrace
        return stackTrace.joinToString(separator = "\n") { it.toString() }
    }
    
    /**
     * Checks if the current thread is the main (UI) thread.
     *
     * @return True if the current thread's name is "main".
     */
    @JvmStatic
    fun isMainThread(): Boolean {
        return Thread.currentThread().name == "main"
    }
    
    /**
     * Converts a lambda function to its string representation (may be JVM-generated class reference).
     *
     * @param lambda The lambda expression to stringify.
     * @return A string representation of the lambda.
     */
    @JvmStatic
    fun lambdaToString(lambda: () -> Unit): String {
        return lambda.toString()
    }
    
    /**
     * Retrieves the filename from which this method was called.
     *
     * @return The file name in the call stack.
     */
    @JvmStatic
    fun getCurrentFileName(): String {
        return Thread.currentThread().stackTrace[2].fileName
    }
    
    /**
     * Retrieves the line number from which this method was called.
     *
     * @return The line number in the source file.
     */
    @JvmStatic
    fun getCurrentLineNumber(): Int {
        return Thread.currentThread().stackTrace[2].lineNumber
    }
    
    /**
     * Generates a unique ID using the current timestamp and a random number.
     *
     * @return A unique string ID (e.g., ID_1683745982374_5321).
     */
    @JvmStatic
    fun generateUniqueId(): String {
        val timestamp = System.currentTimeMillis()
        val randomNum = (1000..9999).random()
        return "ID_${timestamp}_$randomNum"
    }
    
    /**
     * Gets the name of the caller class and method.
     *
     * @return A string in the format "CallerClassName -> callerMethodName".
     */
    @JvmStatic
    fun getCallerClassNameAndMethodName(): String {
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace[3]
        return "${caller.className} -> ${caller.methodName}"
    }
    
    /**
     * Returns whether the app is running in debug mode based on {@link AIOApp#IS_DEBUG_MODE_ON}.
     *
     * @return True if debug mode is enabled, false otherwise.
     */
    @JvmStatic
    fun isDebugMode(): Boolean {
        return AIOApp.IS_DEBUG_MODE_ON
    }
    
    /**
     * Restarts the application by relaunching the launcher activity.
     *
     * @param shouldKillProcess If true, kills the current process after launching the new instance.
     */
    @JvmStatic
    fun restartApp(shouldKillProcess: Boolean = false) {
        val context = AIOApp.INSTANCE
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        if (shouldKillProcess) Runtime.getRuntime().exit(0)
    }
}
