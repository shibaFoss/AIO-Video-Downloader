package userInterface.appCrashed;

import android.content.Intent;
import android.text.Editable;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;
import com.nextgen.databinding.ActivityAppCrashed1Binding;

import java.io.Serializable;
import java.util.Objects;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.ActivityAnimator;
import coreUtils.library.views.StylizedDialogBuilder;
import coreUtils.library.views.StylizedToastView;
import sysModules.crashedHandler.AppCrashedInfo;
import sysModules.crashedHandler.GlobalCrashedHandler;
import userInterface.openingSplash.LauncherActivity;

/**
 * Activity displayed when the application crashes unexpectedly. This screen
 * allows the user to send a crash report to the server, control which data is
 * shared via checkboxes, and continue using the app after acknowledging the
 * crash.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Collects a user-written description of what they were doing before the crash.</li>
 * <li>Provides privacy checkboxes to opt out of sharing the crash log and/or device info.</li>
 * <li>Provides a "Send Report" button to upload crash data to the server.</li>
 * <li>Provides a "Continue Anyway" button to restart the app normally.</li>
 * </ul>
 *
 * <p><strong>Input data:</strong>
 * The activity expects an {@link AppCrashedInfo} object as an intent extra
 * with key {@link #CRASHED_INFO_INTENT_KEY}. This object contains the
 * stacktrace and other crash metadata captured by the global crash handler.
 *
 * <p>The activity locks screen orientation to portrait and uses view binding
 * for type-safe view access. Crash reports are sent asynchronously via
 * {@link AppCrashedViewModel}.
 *
 * @see BaseActivity
 * @see AppCrashedInfo
 * @see GlobalCrashedHandler
 */
public final class AppCrashedActivity extends BaseActivity<ActivityAppCrashed1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final String CRASHED_INFO_INTENT_KEY = "CRASHED_INFO_INTENT_KEY";
	private StylizedDialogBuilder progressDialogBuilder;
	
	/**
	 * Locks the activity's screen orientation to prevent rotation during display.
	 * <p>
	 * This method returns true to lock the orientation, preventing the system from
	 * recreating the activity when the device is rotated. This keeps the crash
	 * report screen stable so the user can read details and take action without
	 * UI disruption.
	 * </p>
	 *
	 * @return true to lock orientation, false to allow rotation
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the view binding for the app crashed activity.
	 * <p>
	 * This method uses the generated ActivityAppCrashed1Binding class to inflate the
	 * crash report layout XML, providing type-safe access to all UI components
	 * including error message displays, crash details, and action buttons for
	 * reporting or restarting the application.
	 * </p>
	 *
	 * @param inflater the LayoutInflater instance used to inflate the layout
	 * @return the ActivityAppCrashed1Binding containing references to all UI elements
	 */
	@Override
	protected ActivityAppCrashed1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityAppCrashed1Binding.inflate(inflater);
	}
	
	/**
	 * Performs post-layout initialization after the content view has been inflated.
	 * This method is invoked by the base activity at the end of {@code onCreate()}
	 * and configures both action buttons.
	 *
	 * <p><strong>Initialization order:</strong>
	 * <ol>
	 * <li>Configures the "Continue Anyway" button via {@link #setupContinueButton()}.</li>
	 * <li>Configures the "Send Report" button via {@link #setupSendReportButton()}.</li>
	 * </ol>
	 *
	 * @see BaseActivity#onLoadedLayout()
	 * @see #setupContinueButton()
	 * @see #setupSendReportButton()
	 */
	@Override
	protected void onLoadedLayout() {
		setupBackButton();
		setupContinueButton();
		setupSendReportButton();
	}
	
	/**
	 * Retrieves or creates the {@link AppCrashedViewModel} associated with this
	 * activity.
	 * <p>
	 * This method uses the standard Android ViewModel provider pattern to obtain
	 * a ViewModel instance scoped to this activity's lifecycle. The ViewModel
	 * survives configuration changes (e.g., screen rotations) and holds crash
	 * report data that would otherwise be lost during recreation. The returned
	 * instance is typically used to observe LiveData, process crash information,
	 * or trigger server submission operations.
	 * </p>
	 * <p>
	 * The method delegates to {@link ViewModelProvider#get(Class)} which either
	 * returns an existing ViewModel from the store or creates a new one using
	 * the default {@link ViewModelProvider.NewInstanceFactory}.
	 * </p>
	 *
	 * @return the activity-scoped {@link AppCrashedViewModel} instance, never
	 * {@code null}
	 * @see ViewModelProvider
	 * @see AppCrashedViewModel
	 * @see androidx.lifecycle.ViewModel
	 */
	private AppCrashedViewModel getViewModel() {
		ViewModelProvider viewModelProvider = new ViewModelProvider(this);
		return viewModelProvider.get(AppCrashedViewModel.class);
	}
	
	/**
	 * Configures the click listener for the "Send Report" button. When clicked,
	 * this method retrieves the crash information from the intent, stamps the
	 * user's written description onto the report, applies privacy restrictions
	 * based on checkbox states, sends the report to the server via the ViewModel,
	 * shows a confirmation toast, and programmatically clicks the "Continue
	 * Anyway" button to proceed with app navigation.
	 *
	 * <p>The crash report is sent asynchronously; the user is not blocked while
	 * the network request completes. Even if sending fails, the user can still
	 * continue using the app.
	 *
	 * @see #getCrashedInfoFromIntent()
	 * @see #getEnteredDescription()
	 * @see #applyPrivacyRestrictions(AppCrashedInfo)
	 * @see AppCrashedViewModel#sendCrashInfoToServer(AppCrashedInfo)
	 */
	private void setupSendReportButton() {
		binding.actionButtons.btnSendReport.setOnClickListener(view -> {
			AppCrashedInfo crashedInfo = getCrashedInfoFromIntent();
			if (crashedInfo != null) {
				crashedInfo.setUserGivenMessage(getEnteredDescription());
				applyPrivacyRestrictions(crashedInfo);
				getViewModel().sendCrashInfoToServer(crashedInfo);
				
				buttonVibrate();
				String toastMessage = StringHelper.getText(R.string.hint_feedback_sent_thank_you);
				StylizedToastView.show(AppCrashedActivity.this, toastMessage);
				binding.actionButtons.btnContinueAnyway.performClick();
			} else {
				logger.debug("AppCrashInfo object is null, failed to submit report.");
			}
		});
	}
	
	/**
	 * Replaces sensitive crash data with a "User Denied" placeholder when the
	 * user has left the corresponding privacy checkbox checked.
	 * <p>
	 * If the "Include crash log" checkbox is checked, the stacktrace field is
	 * replaced with "User Denied". If the "Include device & app information"
	 * checkbox is checked, the detailed info field is replaced with
	 * "User Denied".
	 * </p>
	 *
	 * @param crashedInfo the crash info object whose fields will be sanitized
	 *                    in-place based on checkbox states
	 * @see #setupSendReportButton()
	 */
	private void applyPrivacyRestrictions(AppCrashedInfo crashedInfo) {
		if (binding.crashInfo.btnCheckCrashLog.isChecked()) {
			crashedInfo.setStackStraceInfo("user_denied");
		}
		if (binding.crashInfo.btnCheckDeviceInfo.isChecked()) {
			crashedInfo.setDetailedInfo("user_denied");
		}
	}
	
	/**
	 * Retrieves the text content from the description input field as a non-null
	 * String. This method uses {@link Objects#requireNonNull(Object)} to ensure
	 * the returned value is never {@code null}, even if the underlying EditText
	 * returns null (which should not happen under normal circumstances).
	 *
	 * <p>If the EditText is empty, an empty string is returned. Whitespace is
	 * preserved as entered.
	 *
	 * @return The non-null string containing the user's entered description text
	 * @throws NullPointerException If the EditText's text is unexpectedly null
	 * @see android.widget.EditText#getText()
	 */
	@NonNull
	private String getEnteredDescription() {
		Editable descriptionText = binding.crashInfo.editDescription.getText();
		return Objects.requireNonNull(descriptionText).toString();
	}
	
	/**
	 * Configures the click listener for the back navigation button.
	 * <p>
	 * This method initializes the click behavior for the back button in the UI,
	 * typically found in the toolbar or header. When clicked, it triggers the
	 * standard {@link #onBackPressed()} behavior, allowing the user to navigate
	 * away from the crash screen.
	 * </p>
	 *
	 * @see #onBackPressed()
	 */
	private void setupBackButton() {
		binding.actionBar.btnBack.setOnClickListener(view -> {
			finish();
			ActivityAnimator.animActivityFade(AppCrashedActivity.this);
		});
	}
	
	/**
	 * Configures the click listener for the "Continue Anyway" button. When clicked,
	 * this method navigates back to the {@link LauncherActivity} to restart the
	 * app normally, bypassing the crash loop. A fade animation is applied during
	 * the transition.
	 *
	 * @see LauncherActivity
	 * @see ActivityAnimator#animActivityFade(BaseActivity)
	 */
	private void setupContinueButton() {
		binding.actionButtons.btnContinueAnyway.setOnClickListener(view -> {
			Intent intent = new Intent(AppCrashedActivity.this, LauncherActivity.class);
			ActivityAnimator.animActivityFade(AppCrashedActivity.this);
			startActivity(intent);
			finish();
		});
	}
	
	/**
	 * Extracts a crash information object from the activity's launching intent.
	 * <p>
	 * This method retrieves a serialized {@link AppCrashedInfo} instance that was
	 * previously attached to the intent as an extra under the key
	 * {@link #CRASHED_INFO_INTENT_KEY}. The extracted object contains detailed
	 * crash context (device ID, stack trace, application version, etc.) and is
	 * typically used to display crash details on a dedicated error screen or to
	 * submit the report to a remote server.
	 * </p>
	 *
	 * @return the extracted {@link AppCrashedInfo} object containing crash details,
	 * or {@code null} if the intent extra is absent, null, or of an
	 * incorrect type
	 * @see Intent#getSerializableExtra(String)
	 * @see AppCrashedInfo
	 * @see #CRASHED_INFO_INTENT_KEY
	 */
	@Nullable
	private AppCrashedInfo getCrashedInfoFromIntent() {
		Intent intent = getIntent();
		Serializable packageExtra = intent
			.getSerializableExtra(CRASHED_INFO_INTENT_KEY);
		
		if (packageExtra != null) {
			if (!(packageExtra instanceof AppCrashedInfo)) {
				return null;
			}
			return (AppCrashedInfo) packageExtra;
		} else {
			return null;
		}
	}
}