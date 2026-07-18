package userInterface.appUpdater;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;
import com.nextgen.databinding.ActivityUpdater1Binding;
import com.nextgen.databinding.ActivityUpdater1P0Binding;
import com.nextgen.databinding.ActivityUpdater1P1Binding;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Objects;

import javax.annotation.Nullable;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.AppDirsValidator;
import coreUtils.library.process.IntentLinkHelper;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.TimeFormats;
import coreUtils.library.process.VersionInfo;
import coreUtils.library.storage.FileStorageUtility;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.ActivityAnimator;
import coreUtils.library.views.MessageDialogBuilder;
import coreUtils.library.views.StylizedDialogBuilder;
import coreUtils.library.views.StylizedToastView;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsRepo;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;
import userInterface.appUpdater.AppUpdaterViewModel.DownloadStatus;
import userInterface.mainScreen.MainActivity;

/**
 * Activity that handles application update downloads and installations.
 * This screen displays available update information including version details,
 * release date, file size, and a "What's New" changelog. It provides buttons
 * to start the download, postpone the update with a reminder, or visit the
 * official website for more information.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Displays latest update version information and changelog.</li>
 * <li>Manages APK download with progress tracking (percentage, speed, time).</li>
 * <li>Requests storage permission if needed (MANAGE_EXTERNAL_STORAGE on API 30+).</li>
 * <li>Handles download errors and provides user feedback via error messages.</li>
 * <li>Launches APK installer upon successful download completion.</li>
 * <li>Supports download cancellation via a custom progress dialog.</li>
 * </ul>
 *
 * @see BaseActivity
 * @see ActivityUpdater1Binding
 * @see AppUpdaterViewModel
 * @see ApkDownloader
 */
public class AppUpdaterActivity extends BaseActivity<ActivityUpdater1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final String KEY_INTENT_RECEIVED_KEY = "KEY_INTENT_RECEIVED_KEY";
	
	private StylizedDialogBuilder downloadDialog;
	private AppUpdaterViewModel viewModel;
	
	private long lastProgressBytes = 0;
	private long lastProgressTime = 0;
	private double smoothedSpeed = 0;
	private boolean isDownloadActive = false;
	
	private TextView tvPercentage, tvSpeedValue;
	private TextView tvTimeValue, tvTotalSize;
	private ProgressBar pbDownload;
	
	/**
	 * Determines whether the activity's screen orientation should be locked.
	 * This implementation returns {@code true}, forcing the app updater screen
	 * to remain in portrait mode regardless of device rotation.
	 *
	 * <p><strong>Design rationale:</strong>
	 * Locking the orientation ensures the update details, progress indicators,
	 * and action buttons maintain a consistent layout during download progress
	 * updates and user interactions, preventing unexpected UI reconfigurations.
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
	 * binding instance for {@code activity_updater_1.xml}. This method is called
	 * during the base activity's {@code setContentView()} phase to create the
	 * binding object that provides type-safe access to all views in the layout,
	 * including update details, progress indicators, and action buttons.
	 *
	 * <p>The layout consists of multiple merged view components:
	 * <ul>
	 * <li>{@code updateDetails} – Contains version info, changelog, download error views.</li>
	 * <li>{@code actionButtons} – Contains Update Now, Remind Later, and Visit Official buttons
	 * .</li>
	 * </ul>
	 *
	 * @param inflater The layout inflater service used to create the view hierarchy.
	 *                 Must not be {@code null}.
	 * @return The {@link ActivityUpdater1Binding} instance containing references
	 *         to all views defined in the app updater screen layout.
	 * @see BaseActivity#inflateBinding(LayoutInflater)
	 */
	@Override
	protected ActivityUpdater1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityUpdater1Binding.inflate(inflater);
	}
	
	/**
	 * Performs post-layout initialization after the content view has been inflated.
	 * This method is invoked by the base activity at the end of {@code onCreate()}
	 * and is responsible for initializing the ViewModel, displaying update details,
	 * setting up button listeners, and observing download status.
	 *
	 * <p><strong>Initialization order:</strong>
	 * <ol>
	 * <li>Initializes the ViewModel via {@link #initializeViewModel()}.</li>
	 * <li>Displays the latest update version info via {@link #showLatestUpdateVersion()}.</li>
	 * <li>Renders the "What's New" changelog via {@link #showWhatsNewChangelog()}.</li>
	 * <li>Configures button click events via {@link #setupButtonClickEvents()}.</li>
	 * <li>Observes download status LiveData via {@link #observeDownloadStatus()}.</li>
	 * </ol>
	 *
	 * @see BaseActivity#onLoadedLayout()
	 * @see #initializeViewModel()
	 * @see #showLatestUpdateVersion()
	 * @see #showWhatsNewChangelog()
	 * @see #setupButtonClickEvents()
	 * @see #observeDownloadStatus()
	 */
	@Override
	protected void onLoadedLayout() {
		initializeViewModel();
		showLatestUpdateVersion();
		showWhatsNewChangelog();
		setupButtonClickEvents();
		observeDownloadStatus();
	}
	
	/**
	 * Initializes the ViewModel for the app updater screen. This method creates a
	 * {@link AppUpdaterViewModel} instance using the {@link ViewModelProvider}
	 * scoped to this activity. The ViewModel manages download state, status
	 * LiveData, and the APK download operation.
	 *
	 * <p>The ViewModel survives configuration changes and retains download state
	 * across screen rotations.
	 *
	 * @see ViewModelProvider
	 * @see AppUpdaterViewModel
	 */
	private void initializeViewModel() {
		ViewModelProvider viewModelProvider = new ViewModelProvider(this);
		viewModel = viewModelProvider.get(AppUpdaterViewModel.class);
	}
	
	/**
	 * Returns the initialized ViewModel instance for the app updater screen.
	 * If the ViewModel has not been initialized yet, this method calls
	 * {@link #initializeViewModel()} to create it before returning.
	 *
	 * <p>This lazy initialization pattern ensures the ViewModel is only created
	 * when first needed, and subsequent calls return the existing instance.
	 *
	 * @return The {@link AppUpdaterViewModel} instance associated with this activity.
	 */
	private AppUpdaterViewModel getViewModel() {
		if (viewModel == null) initializeViewModel();
		return viewModel;
	}
	
	/**
	 * Displays the "What's New" changelog for the available update in HTML format.
	 * This method retrieves the UpdateInfo object from the intent, extracts the
	 * JSON changelog string, and renders it as styled HTML in the corresponding
	 * TextView. The HTML is parsed using {@link Html#fromHtml(String, int)} with
	 * the {@link Html#FROM_HTML_MODE_LEGACY} flag for maximum compatibility.
	 *
	 * <p>If the UpdateInfo object is not available from the intent, the method
	 * returns without making any UI changes.
	 *
	 * @see #getUpdateInfoPackageFromIntent()
	 * @see UpdateInfo#getWhatsNewJSON()
	 * @see Html#fromHtml(String, int)
	 */
	private void showWhatsNewChangelog() {
		UpdateInfo updateInfo = getUpdateInfoPackageFromIntent();
		if (updateInfo == null) return;
		String changeLogHtmlString = updateInfo.getWhatsNewJSON();
		TextView tvChangelog = binding.updateDetails.tvVersionChangelog;
		tvChangelog.setText(Html.fromHtml(changeLogHtmlString, Html.FROM_HTML_MODE_LEGACY));
	}
	
	/**
	 * Configures click listeners for all actionable buttons on the app updater screen.
	 * This method assigns the appropriate behavior to each button:
	 *
	 * <ul>
	 * <li>Update Now → {@link #validateAndStartDownload()} – Initiates the update
	 *     download after validating storage permissions.</li>
	 * <li>Remind Later → {@link #scheduleUpdateReminder()} – Sets a reminder flag
	 *     and returns to the main activity.</li>
	 * <li>Visit Official Website → {@link #openOfficialWebsite()} – Opens the
	 *     application's official site in the system browser.</li>
	 * </ul>
	 *
	 * @see #validateAndStartDownload()
	 * @see #scheduleUpdateReminder()
	 * @see #openOfficialWebsite()
	 */
	private void setupButtonClickEvents() {
		ActivityUpdater1P1Binding buttons = binding.actionButtons;
		buttons.btnUpdateNow.setOnClickListener(view -> validateAndStartDownload());
		buttons.btnRemindLater.setOnClickListener(view -> scheduleUpdateReminder());
		buttons.btnVisitOfficial.setOnClickListener(view -> openOfficialWebsite());
	}
	
	/**
	 * Schedules an update reminder by marking the user preference in the app
	 * configuration and navigating back to the main activity. This method sets
	 * {@code isUserNeedToRemindForAppUpdate} to {@code true}, persists the change,
	 * and returns the user to {@link MainActivity} with the activity stack cleared.
	 *
	 * <p>The reminder flag can be used by other parts of the application to
	 * periodically prompt the user about available updates without being intrusive.
	 *
	 * @see AppConfigs#isUserNeedToRemindForAppUpdate
	 * @see AppConfigsRepo#save(AppConfigs)
	 */
	private void scheduleUpdateReminder() {
		AppConfigs appConfigs = AppConfigsRepo.getConfig();
		appConfigs.isUserNeedToRemindForAppUpdate = true;
		appConfigs.save();
		
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		ActivityAnimator.animActivityFade(this);
		
		finish();
	}
	
	/**
	 * Opens the application's official website in the system default browser.
	 * This method retrieves the website URL from string resources and delegates
	 * to {@link IntentLinkHelper#openLinkInSystemBrowser(Context, String)}.
	 *
	 * <p>If an error occurs during the operation (e.g., no browser installed,
	 * malformed URL), a toast message is displayed to the user and the error
	 * is logged for debugging.
	 *
	 * @see IntentLinkHelper#openLinkInSystemBrowser(Context, String)
	 * @see StylizedToastView#showError(BaseActivity, CharSequence)
	 */
	private void openOfficialWebsite() {
		try {
			String webAddress = StringHelper.getText(R.string.http_tubeaio_official_page_url);
			IntentLinkHelper.openLinkInSystemBrowser(this, webAddress);
		} catch (Exception error) {
			String somethingWentWrong =
				StringHelper.getText(R.string.label_something_went_wrong);
			StylizedToastView.showError(this, somethingWentWrong);
			logger.error("Error opening system browser for official site: ", error);
		}
	}
	
	/**
	 * Observes the download status LiveData from the ViewModel and processes
	 * each status update. This method filters updates only when a download is
	 * active, handles errors first, dismisses any existing error display,
	 * updates the UI progress, and checks for download completion.
	 *
	 * <p><strong>Processing order:</strong>
	 * <ol>
	 * <li>If no download is active, the update is ignored.</li>
	 * <li>If an error exists, it is handled via {@link #handleDownloadError(DownloadStatus)}.</li>
	 * <li>If no error, existing error display is dismissed.</li>
	 * <li>Progress UI is updated via {@link #updateDownloadProgress(DownloadStatus)}.</li>
	 * <li>Completion is checked via {@link #handleDownloadComplete(DownloadStatus)}.</li>
	 * </ol>
	 *
	 * @see AppUpdaterViewModel#getDownloadStatusLiveData()
	 * @see #handleDownloadError(DownloadStatus)
	 * @see #dismissDownloadError()
	 * @see #updateDownloadProgress(DownloadStatus)
	 * @see #handleDownloadComplete(DownloadStatus)
	 */
	private void observeDownloadStatus() {
		getViewModel().getDownloadStatusLiveData().observe(this, downloadStatus -> {
			if (!isDownloadActive) return;
			if (handleDownloadError(downloadStatus)) return;
			dismissDownloadError();
			updateDownloadProgress(downloadStatus);
			handleDownloadComplete(downloadStatus);
		});
	}
	
	/**
	 * Displays the download progress dialog and initiates the APK download process.
	 * This method prevents duplicate dialogs by checking the download active flag.
	 * It creates and shows the dialog, marks the download as active, starts the
	 * download via {@link #startDownloadingLatestApk()}, and then extracts references
	 * to the dialog's progress UI components (percentage, speed, time, total size, progress bar).
	 *
	 * <p>The dialog is configured with a custom layout that contains the progress
	 * reporting views. After the download starts, the dialog updates automatically
	 * via LiveData observation.
	 *
	 * @see #getStylizedDialogBuilder()
	 * @see #startDownloadingLatestApk()
	 * @see #observeDownloadStatus()
	 */
	private void showDownloaderDialog() {
		if (isDownloadActive) return;
		downloadDialog = getStylizedDialogBuilder();
		downloadDialog.show();
		isDownloadActive = true;
		
		startDownloadingLatestApk();
		
		View contentView = downloadDialog.getCustomContentView();
		tvPercentage = contentView.findViewById(R.id.tvPercentage);
		tvSpeedValue = contentView.findViewById(R.id.tvSpeedValue);
		tvTimeValue = contentView.findViewById(R.id.tvTimeValue);
		tvTotalSize = contentView.findViewById(R.id.tvTotalSize);
		pbDownload = contentView.findViewById(R.id.pbDownload);
	}
	
	/**
	 * Updates the UI with current download progress information including percentage,
	 * downloaded/total size, download speed, and estimated remaining time. This method
	 * applies exponential smoothing to the speed calculation to provide stable readings
	 * without sudden fluctuations.
	 *
	 * <p><strong>UI updates performed:</strong>
	 * <ul>
	 * <li>Percentage text and progress bar.</li>
	 * <li>Total file size (after first progress tick).</li>
	 * <li>Download speed with smoothing factor (80% previous, 20% current).</li>
	 * <li>Estimated time remaining based on smoothed speed.</li>
	 * </ul>
	 *
	 * <p>Speed smoothing uses an exponential moving average formula:
	 * {@code smoothed = (previous * 0.8) + (current * 0.2)}.
	 *
	 * @param downloadStatus The current download status containing progress metrics.
	 * @see FileStorageUtility#toMegabytesString(double)
	 * @see TimeFormats#toHumanReadableTime(long)
	 */
	private void updateDownloadProgress(DownloadStatus downloadStatus) {
		int percentage = downloadStatus.getProgress();
		long downloadedByte = downloadStatus.getDownloadedByte();
		long totalFileSizeInByte = downloadStatus.getTotalFileSize();
		long currentTime = System.currentTimeMillis();
		
		String totalInFormat = FileStorageUtility
			.toMegabytesString(totalFileSizeInByte);
		
		String downloadedInFormat = FileStorageUtility
			.toMegabytesString(downloadedByte);
		
		tvPercentage.setText(MessageFormat.format("{0}%", percentage));
		pbDownload.setProgress(percentage);
		
		if (percentage > 0) tvTotalSize.setText(totalInFormat);
		else tvTotalSize.setText("--");
		
		if (lastProgressTime > 0 && currentTime > lastProgressTime) {
			long timeDelta = currentTime - lastProgressTime;
			long bytesDelta = downloadedByte - lastProgressBytes;
			
			if (bytesDelta >= 0) {
				double currentSpeed = (bytesDelta * 1000.0) / timeDelta;
				
				if (smoothedSpeed == 0) smoothedSpeed = currentSpeed;
				else smoothedSpeed = (smoothedSpeed * 0.8) + (currentSpeed * 0.2);
				
				tvSpeedValue.setText(MessageFormat.format("{0}/s",
					FileStorageUtility.toMegabytesString(smoothedSpeed)));
				
				if (smoothedSpeed > 0 && totalFileSizeInByte > downloadedByte) {
					long bytesRemaining = totalFileSizeInByte - downloadedByte;
					long secondsRemaining = (long) (bytesRemaining / smoothedSpeed);
					
					tvTimeValue.setText(TimeFormats.toHumanReadableTime(secondsRemaining));
				} else if (totalFileSizeInByte == downloadedByte) {
					tvTimeValue.setText(R.string.label_finishing);
				} else {
					tvTimeValue.setText("--:--");
				}
			}
		} else {
			tvSpeedValue.setText(R.string.label_connecting);
			tvTimeValue.setText("--:--");
		}
		
		lastProgressBytes = downloadedByte;
		lastProgressTime = currentTime;
	}
	
	/**
	 * Handles successful completion of an APK download. This method verifies that the
	 * download status is {@link DownloadStatus.State#COMPLETED} and that the downloaded
	 * file exists and has content. If valid, it resets the download active flag,
	 * closes any active download dialog, and launches the APK installer via
	 * {@link FileStorageUtility#openApkFile(Context, File)}.
	 *
	 * <p>Any exception during file validation or installation is caught and logged
	 * without crashing the application.
	 *
	 * @param downloadStatus The download status containing the completed file reference.
	 * @see DownloadStatus#getState()
	 * @see DownloadStatus#getFile()
	 * @see FileStorageUtility#openApkFile(Context, File)
	 */
	private void handleDownloadComplete(DownloadStatus downloadStatus) {
		try {
			if (downloadStatus.getState() == DownloadStatus.State.COMPLETED) {
				File apkFile = downloadStatus.getFile();
				if (apkFile.exists() && apkFile.isFile() && apkFile.length() > 0) {
					isDownloadActive = false;
					if (downloadDialog != null) {
						downloadDialog.close();
						downloadDialog = null;
					}
					FileStorageUtility.openApkFile(this, apkFile);
				}
			}
		} catch (Exception error) {
			logger.error("Failed to install apk file: ", error);
		}
	}
	
	/**
	 * Handles download error conditions and updates the UI to display an error
	 * message. This method checks if the download status contains an error message.
	 * If an error is present, it resets the download active flag, closes any active
	 * download dialog, clears UI progress references, and shows the error container
	 * with appropriate error text.
	 *
	 * @param downloadStatus The download status potentially containing an error message.
	 * @return {@code true} if an error was handled, {@code false} otherwise.
	 * @see DownloadStatus#getError()
	 */
	private boolean handleDownloadError(DownloadStatus downloadStatus) {
		if (downloadStatus.getError() == null) return false;
		if (!downloadStatus.getError().isEmpty()) {
			isDownloadActive = false;
			if (downloadDialog != null) {
				downloadDialog.close();
				downloadDialog = null;
			}
			tvPercentage = null;
			pbDownload = null;
			tvTotalSize = null;
			tvSpeedValue = null;
			tvTimeValue = null;
			
			ActivityUpdater1P0Binding updateDetails = binding.updateDetails;
			updateDetails.tvDownloadError.setText(R.string.label_download_has_failed);
			updateDetails.tvDownloadErrorDetailed.setText(R.string.desc_download_failed_reason);
			updateDetails.containerDownloadError.setVisibility(View.VISIBLE);
			return true;
		}
		return false;
	}
	
	/**
	 * Dismisses the download error notification by clearing error message text
	 * and hiding the error container view. This method resets the error display
	 * to its default hidden state, allowing the UI to return to normal after
	 * an error has been acknowledged or resolved.
	 *
	 * <p>The error text fields are set to an empty string placeholder
	 * ({@code R.string.label__}) which typically represents an empty or
	 * invisible placeholder value.
	 */
	private void dismissDownloadError() {
		ActivityUpdater1P0Binding updateDetails = binding.updateDetails;
		updateDetails.tvDownloadError.setText(R.string.label__);
		updateDetails.tvDownloadErrorDetailed.setText(R.string.label__);
		updateDetails.containerDownloadError.setVisibility(View.GONE);
	}
	
	/**
	 * Creates and configures a fully-styled dialog for displaying download progress
	 * and cancellation options. The dialog includes a custom layout, background blur,
	 * slide-up animation with fade, and buttons to cancel the ongoing download.
	 *
	 * <p><strong>Dialog configuration:</strong>
	 * <ul>
	 * <li>Custom content view: {@code R.layout.activity_updater_1_dialog_1}.</li>
	 * <li>Background blur radius: 60px (API 31+) with fade animation.</li>
	 * <li>Bottom positioning with slide-up animation.</li>
	 * <li>Title hidden, cancelable disabled, negative button hidden.</li>
	 * <li>Close button visible → calls {@link #stopDownloadingLatestApk()}.</li>
	 * <li>Positive button labeled "Cancel Installing" → stops download and closes.</li>
	 * </ul>
	 *
	 * @return A configured {@link StylizedDialogBuilder} instance ready to be shown.
	 */
	@NonNull private StylizedDialogBuilder getStylizedDialogBuilder() {
		StylizedDialogBuilder downloadDialog = new StylizedDialogBuilder(this);
		downloadDialog.setCustomContentView(R.layout.activity_updater_1_dialog_1);
		downloadDialog.enableBackgroundBlur(60);
		downloadDialog.enableFadeInAnimation();
		downloadDialog.applyBottomPositioning();
		downloadDialog.enableSlideUpAnimation();
		
		downloadDialog.setTitleVisible(false);
		downloadDialog.setCancelable(false);
		downloadDialog.setNegativeButtonVisible(false);
		
		downloadDialog.setCloseButtonVisible(true);
		downloadDialog.setOnCloseClickListener(view -> stopDownloadingLatestApk());
		
		downloadDialog.setPositiveButtonText(R.string.label_cancel_installing);
		downloadDialog.setPositiveButtonIcon(R.drawable.ic_cancel_circle);
		downloadDialog.setOnPositiveClickListener(view -> stopDownloadingLatestApk(), true);
		return downloadDialog;
	}
	
	/**
	 * Initiates the APK download process for the latest update. This method validates
	 * application directories, constructs the target folder path, and delegates the
	 * actual download operation to the ViewModel.
	 *
	 * <p><strong>Steps performed:</strong>
	 * <ol>
	 * <li>Retrieves the UpdateInfo object from the intent (returns if null).</li>
	 * <li>Validates application directories via {@link AppDirsValidator}.</li>
	 * <li>Obtains the application directory (returns if null).</li>
	 * <li>Creates a subdirectory named "TubeAIO Programs".</li>
	 * <li>Delegates to
	 * {@link AppUpdaterViewModel#downloadUpdatedAPK(UpdateInfo, File, LifecycleOwner)}.</li>
	 * </ol>
	 *
	 * @see #getUpdateInfoPackageFromIntent()
	 * @see AppDirsValidator#performValidation()
	 * @see AppDirsValidator#getApplicationDirectory()
	 * @see AppUpdaterViewModel#downloadUpdatedAPK(UpdateInfo, File, LifecycleOwner)
	 */
	private void startDownloadingLatestApk() {
		UpdateInfo updateInfo = getUpdateInfoPackageFromIntent();
		if (updateInfo == null) return;
		
		AppDirsValidator.performValidation();
		File applicationDirectory = AppDirsValidator.getApplicationDirectory();
		if (applicationDirectory == null) return;
		
		String subDirName = StringHelper.getText(R.string.title_tubeaio_programs);
		File appProgramsFolder = new File(applicationDirectory, subDirName);
		File programsFolder = Objects.requireNonNull(appProgramsFolder);
		getViewModel().downloadUpdatedAPK(updateInfo, programsFolder, this);
	}
	
	/**
	 * Stops the active APK download operation. This method sets the download
	 * active flag to {@code false} and delegates to the ViewModel to cancel
	 * the download task, ensuring network resources are released and any
	 * ongoing file I/O is interrupted.
	 *
	 * <p>This is typically called when the user navigates away from the screen
	 * or manually cancels the download via the UI.
	 *
	 * @see #isDownloadActive
	 * @see AppUpdaterViewModel#stopDownloadingAPK()
	 */
	private void stopDownloadingLatestApk() {
		isDownloadActive = false;
		getViewModel().stopDownloadingAPK();
	}
	
	/**
	 * Displays the latest update information retrieved from the intent on the UI.
	 * This method extracts the {@link UpdateInfo} object from the intent and
	 * populates the relevant TextViews with version details, release date, and
	 * APK file size.
	 *
	 * <p><strong>Fields displayed:</strong>
	 * <ul>
	 * <li>Version name with code in parentheses (e.g., "2.1.0 (15)").</li>
	 * <li>Current installed app version.</li>
	 * <li>Latest available version.</li>
	 * <li>Release date of the update.</li>
	 * <li>APK file size (e.g., "12.5 MB").</li>
	 * </ul>
	 *
	 * <p>If the update info is not available from the intent, the method returns
	 * without making any UI updates.
	 *
	 * @see #getUpdateInfoPackageFromIntent()
	 * @see UpdateInfo#getVersionName()
	 * @see UpdateInfo#getVersionCode()
	 * @see UpdateInfo#getApkFileSize()
	 * @see UpdateInfo#getReleaseDate()
	 */
	private void showLatestUpdateVersion() {
		UpdateInfo updateInfo = getUpdateInfoPackageFromIntent();
		if (updateInfo == null) return;
		
		String latestVersionName = updateInfo.getVersionName();
		int latestVersionCode = updateInfo.getVersionCode();
		String latestVersionInfo = latestVersionName + " (" + latestVersionCode + ")";
		String currentVersionInfo = VersionInfo.getVersionName(getApplicationContext());
		String latestVersionApkSize = updateInfo.getApkFileSize();
		String latestVersionReleaseDate = updateInfo.getReleaseDate();
		
		binding.updateDetails.tvVersionName.setText(latestVersionInfo);
		binding.updateDetails.txtCurrentVersion.setText(currentVersionInfo);
		binding.updateDetails.txtLatestVersion.setText(latestVersionInfo);
		binding.updateDetails.txtReleaseDate.setText(latestVersionReleaseDate);
		binding.updateDetails.txtUpdateSize.setText(latestVersionApkSize);
	}
	
	/**
	 * Validates whether the app has full file system access before starting the
	 * update download. If full access is granted, the downloader dialog is shown
	 * immediately. If not, the user is prompted to grant storage permission via
	 * {@link #promptForStorageAccess()}.
	 *
	 * <p>Full file system access (MANAGE_EXTERNAL_STORAGE) is required on Android
	 * 11+ to write APK files to external storage directories. This method ensures
	 * the necessary permission is obtained before proceeding with the download.
	 *
	 * @see FileStorageUtility#hasFullFileSystemAccess(Context)
	 * @see #promptForStorageAccess()
	 * @see #showDownloaderDialog()
	 */
	private void validateAndStartDownload() {
		if (!FileStorageUtility.hasFullFileSystemAccess(this)) promptForStorageAccess();
		else showDownloaderDialog();
	}
	
	/**
	 * Displays a dialog prompting the user to grant full file system access permission.
	 * This method creates a custom-styled message dialog with blur background,
	 * fade-in animation, and two action buttons: Cancel and Allow Now.
	 *
	 * <p><strong>Dialog configuration:</strong>
	 * <ul>
	 * <li>Background blur radius: 60px (API 31+).</li>
	 * <li>Cancelable via back button or touch outside.</li>
	 * <li>Title: "Storage Permission Needed".</li>
	 * <li>Message: Explanation of why storage access is required for updates.</li>
	 * <li>Cancel button: Closes the dialog without action.</li>
	 * <li>Allow Now button: Calls {@link #requestForAllFilesAccess()} and closes the dialog.</li>
	 * </ul>
	 *
	 * @see MessageDialogBuilder
	 * @see #requestForAllFilesAccess()
	 */
	private void promptForStorageAccess() {
		new MessageDialogBuilder(this)
			.enableBackgroundBlur(60)
			.setCancelable(true)
			.enableFadeInAnimation()
			.setDialogTitle(R.string.label_storage_permission_needed)
			.setDialogTitle(R.string.text_storage_permission_required)
			.setLeftButtonText(R.string.label_cancel)
			.setRightButtonText(R.string.label_allow_now)
			.setLeftButtonIcons(R.drawable.ic_cancel_circle, 0)
			.setRightButtonIcons(R.drawable.ic_okay_check, 0)
			.setOnRightClickListener(view -> requestForAllFilesAccess(), true)
			.show();
	}
	
	/**
	 * Retrieves the UpdateInfo object from the activity's intent extras. This method
	 * extracts the serializable extra using the key {@link #KEY_INTENT_RECEIVED_KEY}
	 * and verifies that the object is an instance of {@link UpdateInfo}. If the
	 * extra is missing or of the wrong type, the method returns {@code null}.
	 *
	 * <p>This is typically called during {@link Activity#onCreate(Bundle)}
	 * to obtain the update details that were passed from a previous screen or service.
	 *
	 * @return The {@link UpdateInfo} object from the intent, or {@code null} if not
	 * found or of incorrect type.
	 * @see #KEY_INTENT_RECEIVED_KEY
	 * @see Intent#getSerializableExtra(String)
	 */
	@Nullable
	private UpdateInfo getUpdateInfoPackageFromIntent() {
		Intent intent = getIntent();
		Serializable packageExtra =
			intent.getSerializableExtra(KEY_INTENT_RECEIVED_KEY);
		
		if (packageExtra != null) {
			if (!(packageExtra instanceof UpdateInfo)) {
				return null;
			}
			return (UpdateInfo) packageExtra;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the "What's New" JSON string from the provided UpdateInfo object.
	 * This JSON typically contains a list of changes, new features, bug fixes,
	 * and improvements included in the update, formatted for display in the UI.
	 *
	 * <p>The returned string can be parsed by the UI layer to show release notes
	 * or a changelog to the user before they decide to download the update.
	 *
	 * @param updateInfo The UpdateInfo object containing the update metadata.
	 *                   Must not be null.
	 * @return A JSON string describing what's new in this update.
	 * @see UpdateInfo#getWhatsNewJSON()
	 */
	private String getWhatsNewJSON(@NotNull UpdateInfo updateInfo) {
		return updateInfo.getWhatsNewJSON();
	}
}
