package sysModules.crashedHandler;

import android.os.Build;

import androidx.annotation.NonNull;

import com.nextgen.BuildConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import coreUtils.base.BaseApplication;
import coreUtils.library.process.DeviceInfoUtils;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.VersionInfo;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsRepo;
import dataRepo.dbManager.ObjectBoxHelper;
import dataRepo.userDetails.AppUserRepo;
import io.objectbox.Box;

/**
 * Global exception handler that captures uncaught crashes, persists crash data
 * locally, and flags application state for recovery handling on next launch.
 * <p>
 * This class implements {@link Thread.UncaughtExceptionHandler} and is designed
 * to be registered early in the application lifecycle. When an uncaught exception
 * occurs, it captures the full stack trace, stores the crash information in the
 * local ObjectBox database, and sets a flag ({@code hasAppCrashedRecently}) in the
 * application configuration to indicate that a crash has occurred.
 * </p>
 * <p>
 * <b>Important characteristics:</b>
 * <ul>
 * <li>Crash persistence occurs in ALL build configurations (both debug and
 *     release), enabling crash reporting across all environments</li>
 * <li>The original default exception handler is not chained; this handler
 *     intentionally does not call {@code super.uncaughtException()}, meaning
 *     the system crash dialog is suppressed and the application process
 *     continues running (which may lead to undefined behavior)</li>
 * <li>Secondary exceptions during crash processing are caught and logged to
 *     prevent recursive failures</li>
 * <li>An empty stack trace causes the method to return early without
 *     persisting any crash data</li>
 * </ul>
 * </p>
 *
 * @see Thread.UncaughtExceptionHandler
 * @see AppCrashedInfo
 * @see ObjectBoxHelper#getAppCrashedInfoBox()
 * @see AppConfigs#hasAppCrashedRecently
 */
public final class GlobalCrashedHandler implements Thread.UncaughtExceptionHandler {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	/**
	 * Handles uncaught exceptions by capturing stack trace, persisting crash data
	 * locally, and updating application state regardless of build configuration.
	 * <p>
	 * This method is invoked by the Android framework when a thread terminates due
	 * to an uncaught exception. It captures the full stack trace using a
	 * {@link StringWriter} and {@link PrintWriter} combination, then checks that
	 * the stack trace is not empty before proceeding. The crash information is
	 * persisted to the local ObjectBox database and the application configuration
	 * is updated to flag a recent crash occurrence.
	 * </p>
	 * <p>
	 * <b>Note:</b> Unlike previous versions, this implementation does NOT check
	 * {@link BuildConfig#IS_DEBUG_MODE_ON}. Crash data is persisted in both debug
	 * and release builds, enabling crash reporting across all environments.
	 * </p>
	 * <p>
	 * The method is designed to fail safely: any secondary exception during crash
	 * processing (e.g., database write failure, configuration save error) is caught
	 * and logged without re-throwing, ensuring the original crash does not cause
	 * further disruption.
	 * </p>
	 * <ul>
	 * <li>Uses try-with-resources to automatically close {@link StringWriter} and
	 *     {@link PrintWriter}</li>
	 * <li>Stores crash data via {@link ObjectBoxHelper#getAppCrashedInfoBox()}</li>
	 * <li>Sets {@code hasAppCrashedRecently = true} in the {@link AppConfigs} entity</li>
	 * <li>Method returns early (without side effects) only when stack trace is empty</li>
	 * <li>The original default exception handler is not chained; no system crash
	 *     dialog is displayed</li>
	 * </ul>
	 *
	 * @param thread    the thread that encountered the uncaught exception; never
	 *                  {@code null}
	 * @param throwable the uncaught exception that caused thread termination;
	 *                  never {@code null}
	 * @see Thread.UncaughtExceptionHandler
	 * @see #getConfiguredAppCrashInfo(String)
	 * @see ObjectBoxHelper#getAppCrashedInfoBox()
	 */
	@Override
	public void uncaughtException(@NonNull Thread thread,
	                              @NonNull Throwable throwable) {
		try {
			logger.debug("Uncaught exception in thread: " + thread.getName());
			
			String stackTrace;
			try (StringWriter sw = new StringWriter();
			     PrintWriter pw = new PrintWriter(sw)) {
				throwable.printStackTrace(pw);
				stackTrace = sw.toString();
			}
			
			logger.debug("Stack trace successfully captured");
			if (stackTrace.isEmpty()) return;
			
			Box<AppCrashedInfo> crashedInfoBox = ObjectBoxHelper.getAppCrashedInfoBox();
			crashedInfoBox.removeAll();
			
			AppCrashedInfo appCrashInfo = getConfiguredAppCrashInfo(stackTrace);
			appCrashInfo.id = AppCrashedInfo.APP_CRASHED_OBJECT_BOX_ID;
			crashedInfoBox.put(appCrashInfo);
			
			AppConfigs appConfig = AppConfigsRepo.getConfig();
			appConfig.hasAppCrashedRecently = true;
			appConfig.save();
			
		} catch (Exception error) {
			logger.error("Secondary error during crash handling: ", error);
		}
	}
	
	/**
	 * Constructs and populates a fully configured {@link AppCrashedInfo} object
	 * using device state and user data from the application's repositories.
	 * <p>
	 * This method aggregates crash context from multiple sources:
	 * <ul>
	 * <li>Device identifier and user country from {@link AppUserRepo}</li>
	 * <li>Application version from {@link VersionInfo}</li>
	 * <li>Android version (SDK and release) via {@link #getAndroidVersion()}</li>
	 * <li>Provided stack trace string from the caught exception</li>
	 * <li>Comprehensive device metadata from
	 *     {@link DeviceInfoUtils#getDeviceInformation()}</li>
	 * </ul>
	 * The resulting object is ready for local persistence or remote submission
	 * to a crash reporting server.
	 * </p>
	 *
	 * @param stackTrace the formatted stack trace string obtained from the crash
	 *                   exception (typically via
	 *                   {@link android.util.Log#getStackTraceString(Throwable)})
	 * @return a fully populated {@link AppCrashedInfo} instance containing all
	 * available crash context data
	 * @see AppCrashedInfo
	 * @see AppUserRepo
	 * @see VersionInfo
	 * @see DeviceInfoUtils
	 */
	private AppCrashedInfo getConfiguredAppCrashInfo(String stackTrace) {
		AppCrashedInfo crashedInfo = new AppCrashedInfo();
		BaseApplication appContext = BaseApplication.AppContext;
		
		crashedInfo.setDeviceId(AppUserRepo.getUser().userDeviceId);
		crashedInfo.setUserCountry(AppUserRepo.getUser().countryCode);
		crashedInfo.setApplicationVersion(VersionInfo.getVersionName(appContext));
		crashedInfo.setAndroidVersion(getAndroidVersion());
		crashedInfo.setStackStraceInfo(stackTrace);
		crashedInfo.setDetailedInfo(DeviceInfoUtils.getDeviceInformation());
		crashedInfo.setCrashTimestamp(getCurrentFormattedTimestamp());
		
		return crashedInfo;
	}

    /**
     * Generates a formatted timestamp with an ordinal suffix for the day of the month
     * (e.g., "1st", "2nd", "3rd", "4th"). The format follows the pattern:
     * "d{ordinal} MMM yyyy | hh:mm:ss a" (e.g., "15th Jun 2024 | 03:45:30 PM").
     *
     * <p><strong>Ordinal suffix rules:</strong>
     * <ul>
     * <li>Days 11, 12, 13 → "th" (exception to normal rules).</li>
     * <li>Days ending in 1 → "st" (e.g., 1st, 21st, 31st).</li>
     * <li>Days ending in 2 → "nd" (e.g., 2nd, 22nd).</li>
     * <li>Days ending in 3 → "rd" (e.g., 3rd, 23rd).</li>
     * <li>All other days → "th".</li>
     * </ul>
     *
     * @return A formatted timestamp string with ordinal day suffix.
     * @see SimpleDateFormat
     */
    @NonNull
    private static String getCurrentFormattedTimestamp() {
        Date now = new Date();
        SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.US);
        int day = Integer.parseInt(dayFormat.format(now));
        String suffix;
        if (day >= 11 && day <= 13) {
            suffix = "th";
        } else {
            suffix = switch (day % 10) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
        }
        SimpleDateFormat fullFormat = new SimpleDateFormat(
                "d'" + suffix + "' MMM yyyy '|' hh:mm:ss a", Locale.US);
        return fullFormat.format(now);
    }
	
	/**
	 * Returns a formatted string combining the Android SDK API level and release
	 * version.
	 * <p>
	 * The returned string follows the pattern: {@code [API_LEVEL] [RELEASE_VERSION]}
	 * (e.g., {@code "[33] [13]"} for Android 13, or {@code "[34] [14]"} for
	 * Android 14). This format provides both the numeric SDK level (useful for
	 * API compatibility checks) and the user-facing release name (helpful for
	 * human readability) in a single compact string.
	 * </p>
	 * <ul>
	 * <li>Uses {@link android.os.Build.VERSION#SDK_INT} for the API level</li>
	 * <li>Uses {@link android.os.Build.VERSION#RELEASE} for the version name</li>
	 * <li>The result is never {@code null} and is safe for logging or server
	 *     transmission</li>
	 * </ul>
	 *
	 * @return a formatted string containing both SDK_INT and RELEASE values,
	 * e.g., {@code "[33] [13]"}
	 */
	@NonNull
	private static String getAndroidVersion() {
		return "[" + Build.VERSION.SDK_INT + "]" +
			" [" + Build.VERSION.RELEASE + "]";
	}
}
