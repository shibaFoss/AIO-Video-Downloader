package lib.process;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import app.core.AIOApp;

/**
 * A utility logger class that simplifies and centralizes logging for debugging purposes.
 * This class automatically prefixes logs with the associated class name and only logs
 * messages when {@link AIOApp#IS_DEBUG_MODE_ON} is set to true.
 *
 * <p>This helps in maintaining clean logs in production and providing verbose logging during development.</p>
 *
 * Usage:
 * <pre>{@code
 *     LogHelperUtils logger = LogHelperUtils.from(MyClass.class);
 *     logger.d("Debug message");
 *     logger.e("Error occurred", throwable);
 * }</pre>
 */
public final class LogHelperUtils implements Serializable {

    /** The associated class whose name will be used as the tag in logs. */
    private final Class<?> class_;

    /** Flag to determine if debugging mode is enabled (driven by AIOApp.IS_DEBUG_MODE_ON). */
    private final boolean isDebuggingMode;

    /**
     * Private constructor to initialize logger for a specific class.
     *
     * @param class_ The class object used as the tag in log output.
     */
    private LogHelperUtils(Class<?> class_) {
        this.class_ = class_;
        this.isDebuggingMode = AIOApp.IS_DEBUG_MODE_ON;
    }

    /**
     * Factory method to create a {@link LogHelperUtils} instance for the given class.
     *
     * @param class_ The class for which logging is being done.
     * @return A new instance of {@link LogHelperUtils}.
     */
    @NonNull
    public static LogHelperUtils from(@NonNull Class<?> class_) {
        return new LogHelperUtils(class_);
    }

    /**
     * Converts a {@link Throwable} into a readable stack trace string.
     *
     * @param throwable The throwable to convert.
     * @return A string representation of the stack trace.
     */
    @NonNull
    public static String toString(final @NonNull Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    /**
     * Logs an error message with level 'E' if debugging is enabled.
     *
     * @param message The error message to log.
     */
    public void e(@NonNull String message) {
        if (isDebuggingMode) Log.e(class_.getSimpleName(), toMessage(message));
    }

    /**
     * Logs a throwable with level 'E' if debugging is enabled.
     *
     * @param error The throwable to log.
     */
    public void e(@NonNull Throwable error) {
        if (isDebuggingMode) {
            Log.e(class_.getSimpleName(), toString(error));
        }
    }

    /**
     * Logs a debug message with level 'D' if debugging is enabled.
     *
     * @param message The debug message to log.
     */
    public void d(@NonNull String message) {
        if (isDebuggingMode) Log.d(class_.getSimpleName(), toMessage(message));
    }

    /**
     * Logs a debug message with method name prefix.
     *
     * @param methodName The name of the method generating the log.
     * @param message The debug message to log.
     */
    public void d(@NonNull String methodName, @NonNull String message) {
        d(methodName + message);
    }

    /**
     * Logs a throwable with level 'D' if debugging is enabled.
     *
     * @param err The throwable to log.
     */
    public void d(@NonNull Throwable err) {
        if (isDebuggingMode) Log.d(class_.getSimpleName(), toString(err));
    }

    /**
     * Logs an info message with level 'I' if debugging is enabled.
     *
     * @param message The info message to log.
     */
    public void i(@NonNull String message) {
        if (isDebuggingMode) Log.i(class_.getSimpleName(), toMessage(message));
    }

    /**
     * Logs a throwable with level 'I' if debugging is enabled.
     *
     * @param err The throwable to log.
     */
    public void i(@NonNull Throwable err) {
        if (isDebuggingMode) Log.i(class_.getSimpleName(), toString(err));
    }

    /**
     * Logs an error message followed by the stack trace of the throwable.
     *
     * @param message The error message to log.
     * @param throwable The throwable to log.
     */
    public void e(@NonNull String message, @NonNull Throwable throwable) {
        if (isDebuggingMode) {
            Log.e(class_.getSimpleName(), toMessage(message));
            logThrowableStackTrace(throwable);
        }
    }

    /**
     * Ensures the message is non-null and formatted correctly.
     *
     * @param message The message to format.
     * @return A non-null, safe log message.
     */
    private String toMessage(String message) {
        return message == null ? "Error Message = NULL!!" : message;
    }

    /**
     * Logs the full stack trace of a throwable at error level.
     *
     * @param throwable The throwable to log.
     */
    private void logThrowableStackTrace(Throwable throwable) {
        if (isDebuggingMode) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter, true);
            throwable.printStackTrace(printWriter);
            Log.e(class_.getSimpleName(), stringWriter.toString());
        }
    }
}
