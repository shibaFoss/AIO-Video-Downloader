package userInterface.appCrashed;

import androidx.lifecycle.ViewModel;

import org.jetbrains.annotations.NotNull;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import coreUtils.library.process.ThreadTask.BackgroundTaskNoResult;
import sysModules.crashedHandler.AppCrashedInfo;
import sysModules.crashedHandler.AppCrashedPocketbase;

/**
 * Manages crash report data and server submission operations for the crash
 * reporting UI, surviving configuration changes such as screen rotations.
 * <p>
 * This ViewModel acts as the bridge between the crash reporting activity (or
 * fragment) and the underlying data layer. It holds references to crash
 * information objects, exposes methods for asynchronous server submission, and
 * maintains state across lifecycle events. The ViewModel is scoped to the
 * activity lifecycle and is never recreated during configuration changes,
 * preventing data loss and eliminating the need to re-fetch or re-submit crash
 * reports unnecessarily.
 * </p>
 *
 * <ul>
 * <li>All network operations are delegated to background threads via
 *     {@link ThreadTask} to avoid blocking the main thread</li>
 * <li>Uses {@link AppCrashedPocketbase} as the underlying API client for
 *     server communication</li>
 * <li>Logs success and failure events using a dedicated logger instance</li>
 * <li>Declared {@code final} to prevent subclassing and ensure ViewModel
 *     lifecycle integrity</li>
 * </ul>
 *
 * @see ViewModel
 * @see AppCrashedInfo
 * @see AppCrashedPocketbase
 * @see #sendCrashInfoToServer(AppCrashedInfo)
 */
public final class AppCrashedViewModel extends ViewModel {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	/**
	 * Asynchronously submits crash information to a remote server with execution
	 * callbacks for success and failure scenarios.
	 * <p>
	 * This method offloads the network operation to a background thread using
	 * {@link ThreadTask#executeInBackground(BackgroundTaskNoResult)},Upon successful
	 * server submission, the {@link ServerReportCallback#onCrashReportSaved()} method
	 * is invoked on the main thread. If an exception occurs during the submission
	 * process (network failure, JSON parsing error, server error), the
	 * {@link ServerReportCallback#onCrashReportSaveFailed()} callback is triggered
	 * on the main thread. All network and error logging is handled internally.
	 * </p>
	 * <ul>
	 * <li>Network operations are performed asynchronously; no UI thread blocking</li>
	 * <li>Creates a new {@link AppCrashedPocketbase} instance per request</li>
	 * <li>Callbacks are guaranteed to execute on the main thread for safe UI updates</li>
	 * <li>Exceptions are caught and logged; they do not propagate to the caller</li>
	 * </ul>
	 *
	 * @param crashedInfo    the populated crash information object; must not be
	 *                       {@code null}
	 * @param reportCallback the callback interface for success and failure
	 *                       notifications; must not be {@code null}
	 * @see #sendCrashInfoToServer(AppCrashedInfo)
	 * @see ServerReportCallback
	 */
	public void sendCrashInfoToServer(@NotNull AppCrashedInfo crashedInfo,
	                                  @NotNull ServerReportCallback reportCallback) {
		ThreadTask.executeInBackground(() -> {
			try {
				AppCrashedPocketbase appCrashedPocketbase = new AppCrashedPocketbase();
				appCrashedPocketbase.sendCrashInfoToServer(crashedInfo);
				logger.debug("Successful at saving crashed report to the server");
				ThreadTask.executeOnMainThread(reportCallback::onCrashReportSaved);
				
			} catch (Exception error) {
				logger.error("Failed on saving crashed report to server: ", error);
				ThreadTask.executeOnMainThread(reportCallback::onCrashReportSaveFailed);
			}
		});
	}
	
	/**
	 * Asynchronously submits crash information to a remote server without any
	 * callback notifications.
	 * <p>
	 * This simplified version performs the same background network submission as
	 * the overloaded method but does not provide execution callbacks. It is
	 * suitable for "fire-and-forget" scenarios where the caller does not need to
	 * react to success or failure outcomes. Success and failure are still logged
	 * internally via the {@code logger} instance for debugging purposes.
	 * </p>
	 * <ul>
	 * <li>Network operation runs on a background thread</li>
	 * <li>No UI thread callbacks are invoked upon completion</li>
	 * <li>Exceptions are caught and logged without propagation</li>
	 * </ul>
	 *
	 * @param crashedInfo the populated crash information object; must not be
	 *                    {@code null}
	 * @see #sendCrashInfoToServer(AppCrashedInfo, ServerReportCallback)
	 */
	public void sendCrashInfoToServer(@NotNull AppCrashedInfo crashedInfo) {
		ThreadTask.executeInBackground(() -> {
			try {
				AppCrashedPocketbase appCrashedPocketbase = new AppCrashedPocketbase();
				appCrashedPocketbase.sendCrashInfoToServer(crashedInfo);
				logger.debug("Successful at saving crashed report to the server");
			} catch (Exception error) {
				logger.error("Failed on saving crashed report to server: ", error);
			}
		});
	}
	
	/**
	 * Callback interface for receiving asynchronous crash report submission results.
	 * <p>
	 * Implementations of this interface are notified when an asynchronous crash
	 * report submission operation completes, either successfully or with an error.
	 * Both callback methods are guaranteed to be invoked on the main thread,
	 * allowing safe updates to UI components or user-facing notifications.
	 * </p>
	 *
	 * @see #sendCrashInfoToServer(AppCrashedInfo, ServerReportCallback)
	 */
	public interface ServerReportCallback {
		
		/**
		 * Called when the crash report has been successfully saved to the remote server.
		 * <p>
		 * This method executes on the main thread and can be used to update UI,
		 * dismiss progress dialogs, or navigate to a confirmation screen.
		 * </p>
		 */
		void onCrashReportSaved();
		
		/**
		 * Called when the crash report submission failed due to network issues,
		 * server errors, or data serialization problems.
		 * <p>
		 * This method executes on the main thread and can be used to display
		 * error messages, retry dialogs, or save the report locally for later
		 * transmission.
		 * </p>
		 */
		void onCrashReportSaveFailed();
	}
}
