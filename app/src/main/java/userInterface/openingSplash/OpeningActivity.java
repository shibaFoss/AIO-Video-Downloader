package userInterface.openingSplash;

import static userInterface.appUpdater.AppUpdaterUtils.isUpdateAvailable;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.nextgen.databinding.ActivityOpening1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.base.BaseApplication;
import coreUtils.library.process.DeviceSignature;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import coreUtils.library.process.VersionInfo;
import coreUtils.library.views.ActivityAnimator;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsHelper;
import dataRepo.appConfigs.AppConfigsRepo;
import dataRepo.userDetails.AppUserRepo;
import userInterface.aboutUs.AboutActivity;
import userInterface.appUpdater.AppUpdaterActivity;
import userInterface.appUpdater.AppUpdaterUtils;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;
import userInterface.languagePicker.LanguageActivity;
import userInterface.mainScreen.MainActivity;
import userInterface.termsConsPolicy.TermsPolicyActivity;

/**
 * The initial splash activity shown when the application is launched.
 * It performs several startup tasks in parallel — displaying the app version,
 * syncing the download engine configuration, and checking for remote updates —
 * before routing the user to the appropriate next screen.
 *
 * <p><strong>Startup flow:</strong>
 * <ol>
 *   <li>Display the current version name and code on screen.</li>
 *  <li>Fire off a background sync of the download engine configuration
 *       (5 sec timeout, fire‑and‑forget, lifecycle‑aware).</li>
 *   <li>After 500 ms, fetch the latest update info from the server (background,
 *       5 sec timeout, lifecycle‑aware). On success: navigate to the update
 *       screen or proceed; on failure: proceed without update.</li>
 *   <li>Schedule a 3 seconds fallback that force‑navigates if none of the
 *       above paths have completed yet.</li>
 * </ol>
 *
 * <p>The three navigation paths ({@link #checkUpdatesAndNavigate()} success,
 * its error handler, and the 3 sec fallback) are mutually exclusive — whichever
 * fires first sets {@link #hasNavigated} and the others become no‑ops.
 *
 * @see BaseActivity
 * @see ActivityOpening1Binding
 * @see AppUpdaterUtils
 * @see ThreadTask
 */
public final class OpeningActivity extends BaseActivity<ActivityOpening1Binding> {

    private final LoggerUtils logger = LoggerUtils.from(getClass());

    ThreadTask<Boolean, Boolean> versionSyncTask = new ThreadTask<>();
    ThreadTask<Boolean, Boolean> configSyncTask = new ThreadTask<>();

    /**
     * Guards against duplicate navigation. Set to {@code true} by whichever
     * path completes first (update check result, update check error, or
     * the 3sec fallback timer). All other paths check this flag and bail.
     */
    private boolean hasNavigated = false;

    /**
     * Holds the most recently fetched {@link UpdateInfo} so it can be passed
     * from the background thread to the main‑thread result callback without
     * making a second network call.
     */
    private UpdateInfo latestUpdateInfo;


    /**
     * Determines whether the activity's screen orientation should be locked.
     * This implementation returns {@code true}, forcing the opening screen
     * to remain in portrait mode regardless of device rotation.
     *
     * <p><strong>Design rationale:</strong>
     * Locking the orientation ensures the version information, loading
     * indicator, and any introductory content maintain a consistent layout
     * without unexpected reconfigurations during the brief display period
     * before navigation.
     *
     * @return {@code true} to lock the activity to portrait orientation.
     * @see BaseActivity#shouldLockOrientation()
     */
    @Override
    protected boolean shouldLockOrientation() {
        return true;
    }

    /**
     * Inflates the activity's layout using view binding and returns the generated
     * binding instance for {@code activity_opening_0.xml}. This method is called
     * during the base activity's {@code setContentView()} phase to create the
     * binding object that provides type-safe access to all views in the layout,
     * including the version TextView and any loading animations.
     *
     * @param inflater The layout inflater service used to create the view hierarchy.
     *                 Must not be {@code null}.
     * @return The {@link ActivityOpening1Binding} instance containing references
     * to all views defined in the opening screen layout.
     * @see BaseActivity#inflateBinding(LayoutInflater)
     */
    @Override
    protected ActivityOpening1Binding inflateBinding(LayoutInflater inflater) {
        return ActivityOpening1Binding.inflate(inflater);
    }

    /**
     * Performs post-layout initialization after the content view has been inflated.
     * This method is invoked by the base activity at the end of {@code onCreate()}
     * and kicks off all startup tasks in parallel.
     *
     * <p><strong>Initialization order:</strong>
     * <ol>
     * <li>Loads and displays the current app version via {@link #loadVersionInfo()}.</li>
     * <li>Syncs download engine configuration via {@link #syncDownloadEngineConfig()}
     *     (background, 2 sec timeout, fire‑and‑forget).</li>
     * <li>Checks for remote updates via {@link #checkUpdatesAndNavigate()}
     *     (background, 2 sec timeout, navigates on result/error).</li>
     * <li>Schedules a 3 second fallback via {@link #scheduleFallbackNavigation()}
     *     that force‑navigates if nothing else has completed.</li>
     * </ol>
     *
     * @see BaseActivity#onLoadedLayout()
     * @see #loadVersionInfo()
     * @see #syncDownloadEngineConfig()
     * @see #checkUpdatesAndNavigate()
     * @see #scheduleFallbackNavigation()
     */
    @Override
    protected void onLoadedLayout() {
        loadVersionInfo();
        syncDownloadEngineConfig();
        checkUpdatesAndNavigate();
        scheduleFallbackNavigation();
    }

    /**
     * Synchronizes the local download engine configuration with the remote server.
     * This method retrieves the current user's device ID from {@link AppUserRepo}
     * and the local application configuration from {@link AppConfigsRepo}, then
     * delegates to {@link AppConfigsHelper#syncDownloadEngineConfig(String, AppConfigs)}
     * to perform the actual sync operation asynchronously on a background thread.
     *
     * <p>Configuration data synced typically includes download concurrency limits,
     * speed restrictions, Wi-Fi-only preferences, and auto-resume settings.
     *
     * @see AppUserRepo#getUser()
     * @see AppConfigsRepo#getConfig()
     * @see AppConfigsHelper#syncDownloadEngineConfig(String, AppConfigs)
     */
    private void syncDownloadEngineConfig() {
        if (!configSyncTask.isCancelled()) configSyncTask.cancel();
        configSyncTask.setMaxExecutionTimeMs(5000);
        configSyncTask.observeLifecycle(this);
        configSyncTask.setBackgroundTask(callback -> {
            try {
                String userDeviceId = AppUserRepo.getUser().userDeviceId;
                AppConfigs appConfigs = AppConfigsRepo.getConfig();
                AppConfigsHelper.syncDownloadEngineConfig(userDeviceId, appConfigs);
                logger.debug("Download engine config synced successfully");
                return true;
            } catch (Exception error) {
                logger.error("Error syncing download engine: ", error);
                return false;
            }
        });
        configSyncTask.start();
    }

    /**
     * Loads and displays the current application version information on the screen.
     * This method retrieves both the version name (e.g., "1.2.3") and version code
     * (e.g., "4") from {@link VersionInfo}, formats them as a combined string,
     * logs the result for debugging, and sets the text on the version TextView.
     *
     * <p><strong>Format example:</strong> "1.2.3 (4)"
     *
     * @see VersionInfo#getVersionCode(android.content.Context)
     * @see VersionInfo#getVersionName(android.content.Context)
     */
    private void loadVersionInfo() {
        BaseApplication app = BaseApplication.AppContext;
        String versionCode = String.valueOf(VersionInfo.getVersionCode(app));
        String versionName = String.valueOf(VersionInfo.getVersionName(app));
        String versionInfo = versionName + " (" + versionCode + ")";
        logger.debug("Version result: " + versionInfo);
        binding.tvVersion.setText(versionInfo);
    }


    /**
     * Checks for application updates after a 500ms delay, then navigates either
     * to the app updater screen or the next screen based on update availability.
     * This method uses a {@link ThreadTask} to perform the network request
     * asynchronously on a background thread with a 5-second timeout.
     *
     * <p><strong>Execution flow:</strong>
     * <ol>
     * <li>Delays execution by 500ms to allow the opening screen to be visible.</li>
     * <li>If navigation has already occurred, returns early.</li>
     * <li>Configures a ThreadTask with 5 second max execution time.</li>
     * <li>Executes {@link #getLatestUpdateInfo()} and
     * {@link AppUpdaterUtils#isUpdateAvailable(Context, UpdateInfo)} in background thread.</li>
     * <li>On success, calls {@link #onUpdateAvailable(UpdateInfo)} or
     *     {@link #continueWithoutUpdate()} based on availability.</li>
     * <li>On error, logs a warning and proceeds without update.</li>
     * </ul>
     *
     * <p>The {@code hasNavigated} flag prevents duplicate navigation if multiple
     * callbacks are triggered or if the fallback timeout also fires.
     *
     * @see #getLatestUpdateInfo()
     * @see AppUpdaterUtils#isUpdateAvailable(Context, UpdateInfo)
     * @see #onUpdateAvailable(UpdateInfo)
     * @see #continueWithoutUpdate()
     * @see #scheduleFallbackNavigation()
     */
    private void checkUpdatesAndNavigate() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (hasNavigated) return;
            if (!versionSyncTask.isCancelled()) versionSyncTask.cancel();

            versionSyncTask.setMaxExecutionTimeMs(5000);
            versionSyncTask.observeLifecycle(this);
            versionSyncTask.setBackgroundTask(callback -> {
                latestUpdateInfo = getLatestUpdateInfo();
                Context context = getApplicationContext();
                return isUpdateAvailable(context, latestUpdateInfo);
            });

            versionSyncTask.setResultTask(updateAvailable -> {
                if (hasNavigated) return;
                hasNavigated = true;

                if (updateAvailable) onUpdateAvailable(latestUpdateInfo);
                else continueWithoutUpdate();
            });
            versionSyncTask.setErrorTask(error -> {
                if (hasNavigated) return;
                hasNavigated = true;
                logger.warning("Version check failed, proceeding without update");
                continueWithoutUpdate();
            });

            versionSyncTask.start();
        }, 500);
    }

    /**
     * Schedules a fallback navigation task that triggers after a 3-second timeout.
     * This method ensures the opening screen does not hang indefinitely if the
     * update check or configuration sync tasks take too long to complete.
     *
     * <p><strong>Behavior:</strong>
     * <ul>
     * <li>Posts a delayed runnable to the main thread with a 3000ms delay.</li>
     * <li>If navigation has already occurred or the activity is finishing, returns early.</li>
     * <li>Sets the {@code hasNavigated} flag to {@code true} to prevent duplicate navigation.</li>
     * <li>Logs a warning indicating that timeout has been reached.</li>
     * <li>Cancels both configuration and version sync tasks if they are still running.</li>
     * <li>Forces navigation to the next screen via {@link #proceedToNextScreen()}.</li>
     * </ul>
     *
     * <p>This is a defensive mechanism to prevent the user from being stuck on the
     * opening screen due to network failures or slow API responses.
     *
     * @see #proceedToNextScreen()
     * @see #hasNavigated
     * @see Handler#postDelayed(Runnable, long)
     */
    private void scheduleFallbackNavigation() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (hasNavigated || isFinishing()) return;
            hasNavigated = true;

            logger.warning("3s timeout reached, forcing navigation");
            if (!configSyncTask.isCancelled()) configSyncTask.cancel();
            if (!versionSyncTask.isCancelled()) versionSyncTask.cancel();

            proceedToNextScreen();
        }, 3000);
    }

    /**
     * Handles the scenario where a newer version of the application is available.
     * This method logs the update availability, launches the app updater screen
     * via {@link #launchAppUpdater(UpdateInfo)} with the latest update information,
     * and finishes the current opening activity to prevent returning to it via
     * the back button after the update flow.
     *
     * @param latestUpdateInfo The {@link UpdateInfo} object containing version
     *                         details, changelog, and download URL. Must not be
     *                         {@code null} when an update is available.
     * @see #launchAppUpdater(UpdateInfo)
     * @see #continueWithoutUpdate()
     */
    private void onUpdateAvailable(UpdateInfo latestUpdateInfo) {
        logger.debug("Update available, launching app updater");
        launchAppUpdater(latestUpdateInfo);
        finish();
    }

    /**
     * Handles the scenario where no application update is available or the user
     * has chosen to skip the update. This method logs the continuation, then
     * proceeds to the next screen based on the application's configuration state
     * (terms acceptance and locale configuration) via {@link #proceedToNextScreen()}.
     *
     * <p>The next screen may be {@link TermsPolicyActivity}, {@link LanguageActivity},
     * or {@link MainActivity} depending on the current configuration state.
     *
     * @see #proceedToNextScreen()
     * @see #onUpdateAvailable(UpdateInfo)
     */
    private void continueWithoutUpdate() {
        logger.debug("No update available, proceeding to next screen");
        proceedToNextScreen();
    }

    /**
     * Launches the application updater screen with the latest update information.
     * This method constructs an intent targeting {@link AppUpdaterActivity},
     * attaches the {@link UpdateInfo} object as an extra using the key
     * {@link AppUpdaterActivity#KEY_INTENT_RECEIVED_KEY}, triggers a short
     * haptic vibration via {@link #vibrate()}, starts the activity with a
     * fade animation, and finishes the current opening activity.
     *
     * @param latestUpdateInfo The {@link UpdateInfo} object containing version
     *                         details, changelog, and download URL. Must not be
     *                         {@code null} when an update is available.
     * @see AppUpdaterActivity
     * @see UpdateInfo
     * @see #vibrate()
     * @see ActivityAnimator#animActivityFade(BaseActivity)
     */
    private void launchAppUpdater(UpdateInfo latestUpdateInfo) {
        Intent intent = new Intent(OpeningActivity.this, AppUpdaterActivity.class);
        intent.putExtra(AppUpdaterActivity.KEY_INTENT_RECEIVED_KEY, latestUpdateInfo);
        vibrate();
        startActivity(intent);
        ActivityAnimator.animActivityFade(OpeningActivity.this);
    }

    /**
     * Navigates to the appropriate next screen based on the application's
     * configuration state. This method constructs an intent using
     * {@link #getDestinationIntent()}, adds flags to clear the activity stack,
     * starts the target activity with a fade transition animation, and finishes
     * the current opening screen.
     *
     * <p><strong>Intent flags:</strong>
     * <ul>
     * <li>{@link Intent#FLAG_ACTIVITY_NEW_TASK} - Starts the activity as a new task.</li>
     * <li>{@link Intent#FLAG_ACTIVITY_CLEAR_TASK} - Clears any existing activities
     *     from the task stack before launching.</li>
     * </ul>
     *
     * <p>The method logs whether the locale is already configured for debugging
     * purposes, then applies a fade animation and closes the opening screen.
     *
     * @see #getDestinationIntent()
     * @see ActivityAnimator#animActivityFade(BaseActivity)
     */
    private void proceedToNextScreen() {
        Intent destinationIntent = getDestinationIntent();
        destinationIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (AppConfigsRepo.getConfig().isLocaleConfigured)
            logger.debug("Locale is already configured, opening main activity");
        else logger.debug("Locale is not configured, opening language activity.");

        startActivity(destinationIntent);
        ActivityAnimator.animActivityFade(OpeningActivity.this);
        finish();
    }


    /**
     * Determines the appropriate destination activity based on the current
     * application configuration state. The decision logic evaluates:
     * <ol>
     * <li>If terms and conditions have not been accepted, opens
     *     {@link TermsPolicyActivity}.</li>
     * <li>If terms are accepted but locale is not configured, opens
     *     {@link LanguageActivity}.</li>
     * <li>If both terms and locale are configured, opens {@link MainActivity}.</li>
     * </ol>
     *
     * <p>The method also adds an extra with key
     * {@link TermsPolicyActivity#KEY_ACTIVITY_LAUNCHED_LOCATION} and value
     * {@link TermsPolicyActivity#LAUNCHED_FROM_OPENING_SCREEN} to indicate the
     * origin of the launch for tracking or behavior customization.
     *
     * @return A non-null {@link Intent} targeting the determined destination
     *         {@link BaseActivity} subclass.
     * @see AppConfigsRepo#getConfig()
     * @see TermsPolicyActivity
     * @see LanguageActivity
     * @see MainActivity
     */
    @NonNull
    private Intent getDestinationIntent() {
        Intent destinationIntent;
        AppConfigs appConfig = AppConfigsRepo.getConfig();
        boolean isTermsAccepted = appConfig.isTermsConditionsAgreed;
        boolean isLocaleConfigured = appConfig.isLocaleConfigured;

        Class<? extends BaseActivity<? extends ViewBinding>> nextActivityToOpen =
                isTermsAccepted ? LanguageActivity.class : TermsPolicyActivity.class;

        if (isLocaleConfigured) nextActivityToOpen = MainActivity.class;

        destinationIntent = new Intent(this, AboutActivity.class);
        //destinationIntent = new Intent(this, nextActivityToOpen);
        destinationIntent.getShortExtra(
                TermsPolicyActivity.KEY_ACTIVITY_LAUNCHED_LOCATION,
                TermsPolicyActivity.LAUNCHED_FROM_OPENING_SCREEN);

        return destinationIntent;
    }

    /**
     * Fetches the latest application update information from the remote server.
     * This method creates an {@link AppUpdaterUtils} instance, generates a unique
     * device identifier via {@link DeviceSignature}, and retrieves update details
     * such as version code, changelog, and download URL.
     *
     * <p>The fetched {@link UpdateInfo} can be used to check for available updates
     * and prompt the user to install a newer version of the application.
     *
     * @return An {@link UpdateInfo} object containing the latest version details,
     * or {@code null} if the fetch operation fails or no update is available.
     * @see AppUpdaterUtils#fetchLatestUpdateInfo(String)
     * @see DeviceSignature#generate()
     */
    private UpdateInfo getLatestUpdateInfo() {
        AppUpdaterUtils appUpdaterUtils = new AppUpdaterUtils();
        Context context = getApplicationContext();
        String deviceId = DeviceSignature.getInstance(context).generate();
        return appUpdaterUtils.fetchLatestUpdateInfo(deviceId);
    }
}
