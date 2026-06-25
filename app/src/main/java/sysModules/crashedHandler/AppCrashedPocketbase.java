package sysModules.crashedHandler;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import coreUtils.library.process.LoggerUtils;
import dataRepo.dbManager.PocketBaseClient;

/**
 * Concrete implementation of {@link PocketBaseClient} specifically designed for
 * submitting application crash reports to a remote PocketBase collection.
 * <p>
 * This class specializes the abstract {@link PocketBaseClient} to handle crash
 * telemetry data. It provides a fixed collection name (via {@link #COLLECTION_NAME})
 * and exposes a convenience method {@link #sendCrashInfoToServer(AppCrashedInfo)}
 * that transforms an {@link AppCrashedInfo} object into a JSON payload and
 * transmits it using the inherited {@link #post(JSONObject, String)} method. All network
 * operations inherit the HTTP client configuration, error handling, and logging
 * behavior from the parent class.
 * </p>
 * <ul>
 * <li>The collection name is expected to be a constant matching a preconfigured
 *     PocketBase collection (e.g., "app_crashes")</li>
 * <li>Field keys (e.g., {@code FILED_DEVICE_ID}, {@code FIELD_STACKTRACE},
 *     {@code FIELD_USER_MESSAGE}, {@code FIELD_CRASH_TIMESTAMP}) must align
 *     with the server-side schema</li>
 * <li>Network failures are logged but do not propagate exceptions to the caller;
 *     {@code null} is returned instead</li>
 * <li>Designed for use within a crash reporting service or uncaught exception
 *     handler</li>
 * </ul>
 *
 * @see PocketBaseClient
 * @see AppCrashedInfo
 * @see #sendCrashInfoToServer(AppCrashedInfo)
 */
public final class AppCrashedPocketbase extends PocketBaseClient {

    private final LoggerUtils logger = LoggerUtils.from(getClass());
    private static final String COLLECTION_NAME = "appCrashes";
    private static final String FILED_DEVICE_ID = "deviceId";
    private static final String FIELD_ANDROID_VERSION = "androidVersion";
    private static final String FIELD_APP_VERSION = "appVersion";
    private static final String FIELD_USER_COUNTRY = "userCountry";
    private static final String FIELD_STACKTRACE = "stackStrace";
    private static final String FIELD_USER_MESSAGE = "userMessage";
    private static final String FIELD_CRASH_TIMESTAMP = "crashTimestamp";
    private static final String FIELD_DETAILED_INFO = "detailedInfo";

    /**
     * Returns the fixed collection name used for crash report storage on the server.
     * <p>
     * This implementation overrides the abstract getCollectionName()
     * method from {@link PocketBaseClient}, returning a constant string that
     * identifies the remote collection where crash information is stored.
     * The collection name is expected to be preconfigured on the server side
     * (e.g., "app_crashes" or "crash_reports").
     * </p>
     *
     * @return the non-null collection name string as defined by
     * {@code COLLECTION_NAME} constant
     */
    @NonNull
    @Override
    protected String getCollectionName() {
        return COLLECTION_NAME;
    }

    /**
     * Constructs a JSON payload from crash information and submits it to the server
     * via a POST request.
     * <p>
     * This method transforms an {@link AppCrashedInfo} object into a structured
     * {@link JSONObject} payload, mapping each crash field to a corresponding
     * server-side field key. The payload is then sent using the inherited
     * {@link #post(JSONObject, String)} method, which handles network transmission and
     * response parsing. A {@link JSONException} may be thrown if the payload
     * construction fails (e.g., due to invalid field values), but network
     * errors are caught and logged internally by {@code post()}, returning
     * {@code null} instead.
     * </p>
     * <ul>
     * <li>Uses static field keys for consistent server mapping
     *     (e.g., {@code FILED_DEVICE_ID}, {@code FIELD_ANDROID_VERSION},
     *     {@code FIELD_APP_VERSION}, {@code FIELD_USER_COUNTRY},
     *     {@code FIELD_STACKTRACE}, {@code FIELD_USER_MESSAGE},
     *     {@code FIELD_CRASH_TIMESTAMP}, {@code FIELD_DETAILED_INFO})</li>
     * <li>Returns the server response as a {@link JSONObject} or {@code null}
     *     if the POST request fails</li>
     * </ul>
     *
     * @param crashInfo the populated crash information container; must not be
     *                  {@code null}
     * @return a {@link JSONObject} containing the server's response (typically
     * the created record with server-generated fields), or {@code null}
     * if the network request fails
     * @throws JSONException if constructing the JSON payload fails due to
     *                       invalid data types or missing keys
     * @see #post(JSONObject, String)
     * @see AppCrashedInfo
     */
    public JSONObject sendCrashInfoToServer(AppCrashedInfo crashInfo) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put(FILED_DEVICE_ID, crashInfo.getDeviceId());
        payload.put(FIELD_ANDROID_VERSION, crashInfo.getAndroidVersion());
        payload.put(FIELD_APP_VERSION, crashInfo.getApplicationVersion());
        payload.put(FIELD_USER_COUNTRY, crashInfo.getUserCountry());
        payload.put(FIELD_STACKTRACE, crashInfo.getStackStraceInfo());
        payload.put(FIELD_USER_MESSAGE, crashInfo.getUserGivenMessage());
        payload.put(FIELD_CRASH_TIMESTAMP, crashInfo.getCrashTimestamp());
        payload.put(FIELD_DETAILED_INFO, crashInfo.getDetailedInfo());
        return post(payload, crashInfo.getDeviceId());
    }
}
