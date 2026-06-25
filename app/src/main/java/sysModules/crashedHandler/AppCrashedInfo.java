package sysModules.crashedHandler;

import android.content.pm.PackageInfo;

import java.io.Serializable;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * A serializable data container that captures application crash context information
 * for logging, analytics, or remote reporting purposes.
 * <p>
 * This immutable-style POJO stores device-specific and environment details at the
 * moment of an application crash. All fields are optional and default to {@code null}
 * until populated via setters. The class implements {@link Serializable} to support
 * persistence to disk, transmission via {@link android.content.Intent} extras, or
 * attachment to bug reporting intents.
 * </p>
 * <ul>
 * <li>All fields are package-private and accessed via standard getter/setter pairs</li>
 * <li>No validation is performed on setter inputs; callers must sanitize where
 *     required</li>
 * <li>Thread-safety is not guaranteed; intended for single-threaded crash handling
 *     scenarios</li>
 * </ul>
 *
 * @see Serializable
 * @see Thread#getDefaultUncaughtExceptionHandler()
 */
@Entity
public final class AppCrashedInfo implements Serializable {
    public static final long APP_CRASHED_OBJECT_BOX_ID = 1L;

    @Id(assignable = true)
    public long id = 0L;
    private String deviceId;
    private String androidVersion;
    private String userCountry;
    private String userGivenMessage;
    private String crashTimestamp;
    private String applicationVersion;
    private String stackStraceInfo;
    private String detailedInfo;

    /**
     * Returns the unique hardware identifier of the device where the crash occurred.
     * <p>
     * This value is typically obtained from {@link android.provider.Settings.Secure#ANDROID_ID}
     * or a generated advertising ID. It helps distinguish unique devices in crash
     * analytics, enabling calculation of unique user impact and per-device issue
     * frequency. Note that this identifier may be reset on factory reset or when
     * the app is reinstalled on some Android versions.
     * </p>
     *
     * @return the device ID string, or {@code null} if not set
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the unique hardware identifier of the device where the crash occurred.
     * <p>
     * The provided ID should be a stable per-device identifier that persists across
     * app restarts. Common sources include {@link android.provider.Settings.Secure#ANDROID_ID}
     * or a hashed version of the MAC address. This field is used to deduplicate
     * crash reports from the same physical device.
     * </p>
     *
     * @param deviceId the device ID string (e.g., ANDROID_ID or advertising ID);
     *                 may be {@code null} if the identifier cannot be obtained
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Returns the Android OS version running on the device at the time of crash.
     * <p>
     * This value is typically sourced from {@link android.os.Build.VERSION#RELEASE}
     * (e.g., "14", "13", "12.0"). It is critical for identifying version-specific
     * bugs, particularly those introduced or resolved in particular Android
     * releases or vendor-specific ROMs.
     * </p>
     *
     * @return the Android version string (e.g., "14" or "Android 14"), or
     * {@code null} if not set
     */
    public String getAndroidVersion() {
        return androidVersion;
    }

    /**
     * Sets the Android OS version running on the device at the time of crash.
     * <p>
     * This value should be populated from {@link android.os.Build.VERSION#RELEASE}
     * and may optionally include the API level from
     * {@link android.os.Build.VERSION#SDK_INT}. Including this information allows
     * crash reporting systems to surface Android version-specific patterns and
     * target fixes appropriately.
     * </p>
     *
     * @param androidVersion the Android version string (typically from
     *                       {@link android.os.Build.VERSION#RELEASE}); may be
     *                       {@code null} if the version cannot be resolved
     */
    public void setAndroidVersion(String androidVersion) {
        this.androidVersion = androidVersion;
    }

    /**
     * Returns the geographical country code of the user when the crash occurred.
     * <p>
     * The country code is formatted as an ISO 3166-1 alpha-2 string (e.g., "US",
     * "IN", "GB") typically obtained from the device's default locale using
     * {@link java.util.Locale#getCountry()}. This field helps correlate crashes
     * with regional factors such as network conditions, localized content, or
     * region-specific API behavior.
     * </p>
     *
     * @return the country code string (e.g., "US", "IN", "GB"), or {@code null}
     * if not set
     */
    public String getUserCountry() {
        return userCountry;
    }

    /**
     * Sets the geographical country code of the user when the crash occurred.
     * <p>
     * The country code should be formatted as an ISO 3166-1 alpha-2 string
     * (e.g., "US", "IN", "GB") obtained from
     * {@link java.util.Locale#getCountry()}. This value helps correlate crashes
     * with regional network conditions or localized application behavior.
     * </p>
     *
     * @param userCountry the ISO 3166-1 alpha-2 country code; may be {@code null}
     *                    if the country could not be determined
     */
    public void setUserCountry(String userCountry) {
        this.userCountry = userCountry;
    }

    /**
     * Returns the version name of the crashing application.
     * <p>
     * This value typically comes from
     * {@link android.content.pm.PackageInfo#versionName} and represents the
     * user-facing version string (e.g., "3.2.1", "2.0.0-beta"). It is used to
     * identify which application release produced the crash and to filter or
     * group crash reports by version.
     * </p>
     *
     * @return the application version string, or {@code null} if not set
     */
    public String getApplicationVersion() {
        return applicationVersion;
    }

    /**
     * Sets the version name of the crashing application.
     * <p>
     * This value should be populated from {@link PackageInfo#versionName}
     * at crash time. It enables server-side aggregation of crashes by application
     * version, helping developers prioritize fixes for the most affected releases.
     * </p>
     *
     * @param applicationVersion the app version string (e.g., "3.2.1"); may be
     *                           {@code null} if the version could not be resolved
     */
    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    /**
     * Returns the raw stack trace string from the crash exception.
     * <p>
     * The returned string is typically formatted using
     * {@link android.util.Log#getStackTraceString(Throwable)} and contains the
     * complete exception chain with line numbers and class names. This is the
     * primary diagnostic data used to identify the root cause of the crash.
     * </p>
     * <p>
     * <b>Note:</b> The method name contains the typo "Strace" instead of "Trace"
     * for backward compatibility with existing serialized data.
     * </p>
     *
     * @return the formatted stack trace string, or {@code null} if not set
     */
    public String getStackStraceInfo() {
        return stackStraceInfo;
    }

    /**
     * Sets the raw stack trace string from the crash exception.
     * <p>
     * The provided string is typically obtained from
     * {@link android.util.Log#getStackTraceString(Throwable)} and contains the
     * full exception stack trace formatted as a single string. This method
     * stores the value for later serialization or remote reporting.
     * </p>
     * <p>
     * <b>Note:</b> The method name contains the typo "Strace" instead of "Trace".
     * This is preserved for backward compatibility with existing serialized data
     * and should not be corrected to avoid breaking deserialization.
     * </p>
     *
     * @param stackStraceInfo the formatted stack trace string; may be
     *                        {@code null} if no stack trace is available
     */
    public void setStackStraceInfo(String stackStraceInfo) {
        this.stackStraceInfo = stackStraceInfo;
    }

    /**
     * Returns the optional message provided by the user when reporting the crash.
     * <p>
     * This field stores free-form text entered by the user in the crash report
     * screen, describing what they were doing when the crash occurred. It helps
     * developers reproduce the issue by providing step-by-step user context that
     * is not captured in the stack trace alone.
     * </p>
     *
     * @return the user's descriptive message, or {@code null} if the user did
     * not provide any additional information
     */
    public String getUserGivenMessage() {
        return userGivenMessage;
    }

    /**
     * Sets the optional message provided by the user when reporting the crash.
     * <p>
     * This value should be populated from the crash report UI's text input field
     * where the user can describe their actions leading up to the crash. The
     * message is stored alongside the stack trace and device info for complete
     * crash context.
     * </p>
     *
     * @param userGivenMessage the user's description of events leading to the
     *                         crash; may be {@code null} if the user skipped
     *                         providing details
     */
    public void setUserGivenMessage(String userGivenMessage) {
        this.userGivenMessage = userGivenMessage;
    }

    /**
     * Returns the timestamp when the crash was captured.
     * <p>
     * The returned string is formatted as a human-readable date-time stamp
     * (e.g., "11th Jun 2026 | 12:12:05 AM") that records exactly when the
     * crash handler processed the exception. This timestamp is generated at
     * crash time and reflects the device's local time.
     * </p>
     *
     * @return the formatted crash timestamp string, or {@code null} if not set
     */
    public String getCrashTimestamp() {
        return crashTimestamp;
    }

    /**
     * Sets the timestamp when the crash was captured.
     * <p>
     * This value should be generated at crash time using the current system
     * clock and formatted as a human-readable string (e.g.,
     * "11th Jun 2026 | 12:12:05 AM"). The timestamp is stored alongside
     * other crash metadata for chronological sorting and correlation.
     * </p>
     *
     * @param crashTimestamp the formatted crash timestamp string (e.g.,
     *                       "11th Jun 2026 | 12:12:05 AM"); may be
     *                       {@code null} if the time could not be obtained
     */
    public void setCrashTimestamp(String crashTimestamp) {
        this.crashTimestamp = crashTimestamp;
    }

    /**
     * Returns any additional developer-provided context information for the crash.
     * <p>
     * This field can contain arbitrary supplemental data such as the current
     * screen name, network connectivity state, user session identifiers, or any
     * other debugging context that helps diagnose the crash conditions. The
     * content is developer-defined and unstructured.
     * </p>
     *
     * @return a detailed info string (e.g., custom metadata, user actions), or
     * {@code null} if no additional information was provided
     */
    public String getDetailedInfo() {
        return detailedInfo;
    }

    /**
     * Sets any additional developer-provided context information for the crash.
     * <p>
     * This setter allows crash reporting code to attach supplemental debugging
     * context that is not captured by other dedicated fields (e.g., current
     * activity name, last user action, API request state). The value is stored
     * as-is and will be serialized alongside other crash data.
     * </p>
     *
     * @param detailedInfo an arbitrary string containing supplemental crash
     *                     details (e.g., current screen name, network state);
     *                     may be {@code null}
     */
    public void setDetailedInfo(String detailedInfo) {
        this.detailedInfo = detailedInfo;
    }
}