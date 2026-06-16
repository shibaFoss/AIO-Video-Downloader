package userInterface.userPrivacy;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.nextgen.R;
import com.nextgen.databinding.ActivityPrivacyPolicy1Binding;

import java.util.function.Consumer;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.ActivityAnimator;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsRepo;

/**
 * Activity that displays privacy policy information and allows users to configure
 * their privacy preferences. This screen presents three toggle-able settings:
 * crash reporting, analytics, and personalized recommendations, along with
 * security badges and a link to the full privacy policy document.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Displays privacy configuration options (Crash Report, Analytics,
 *     Personalized Recommendations).</li>
 * <li>Persists user privacy preferences to {@link AppConfigs}.</li>
 * <li>Shows security badges explaining data usage and security measures.</li>
 * <li>Provides a link to the full privacy policy webpage.</li>
 * <li>Locks screen orientation to portrait for consistent layout.</li>
 * </ul>
 *
 * <p>All preferences are loaded from and saved to {@link AppConfigsRepo}.
 *
 * @see BaseActivity
 * @see ActivityPrivacyPolicy1Binding
 * @see AppConfigs
 * @see AppConfigsRepo
 */
public class PrivacyPolicyActivity extends BaseActivity<ActivityPrivacyPolicy1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private AppConfigs appConfig;
	
	/**
	 * Inflates the activity's layout using view binding and returns the generated
	 * binding instance for {@code activity_privacy_policy_1.xml}. This method is called
	 * during the base activity's {@code setContentView()} phase to create the
	 * binding object that provides type-safe access to all views in the layout.
	 *
	 * @param inflater The layout inflater service used to create the view hierarchy.
	 *                 Must not be {@code null}.
	 * @return The {@link ActivityPrivacyPolicy1Binding} instance containing references
	 * to all views defined in the privacy policy screen layout.
	 * @see BaseActivity#inflateBinding(LayoutInflater)
	 */
	@Override
	protected ActivityPrivacyPolicy1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityPrivacyPolicy1Binding.inflate(inflater);
	}
	
	/**
	 * Determines whether the activity's screen orientation should be locked.
	 * This implementation returns {@code true}, forcing the privacy policy screen
	 * to remain in portrait mode regardless of device rotation.
	 *
	 * <p><strong>Design rationale:</strong>
	 * Locking the orientation ensures the toggle switches, security badges,
	 * and action button maintain a consistent layout while the user reviews
	 * privacy settings, preventing unexpected UI reconfigurations.
	 *
	 * @return {@code true} to lock the activity to portrait orientation.
	 * @see BaseActivity#shouldLockOrientation()
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Performs post-layout initialization after the content view has been inflated.
	 * This method is invoked by the base activity at the end of {@code onCreate()}
	 * and is responsible for loading the privacy configuration, setting up the
	 * back button, configuring privacy toggles, and initializing the privacy
	 * policy button.
	 *
	 * <p><strong>Initialization order:</strong>
	 * <ol>
	 * <li>Loads the current privacy configuration via {@link AppConfigsRepo#getConfig()}.</li>
	 * <li>Logs the current state of all privacy toggles for debugging.</li>
	 * <li>Sets up the back button via {@link #setupBackButton()}.</li>
	 * <li>Configures privacy toggles via {@link #setupConfigToggles()}.</li>
	 * <li>Configures the privacy policy button via {@link #setupPrivacyPolicyButton()}.</li>
	 * </ol>
	 *
	 * @see BaseActivity#onLoadedLayout()
	 * @see #setupBackButton()
	 * @see #setupConfigToggles()
	 * @see #setupPrivacyPolicyButton()
	 */
	@Override
	protected void onLoadedLayout() {
		appConfig = AppConfigsRepo.getConfig();
		logger.debug("Privacy config loaded: crash=" + appConfig.isCrashReportingEnabled
			+ " analytics=" + appConfig.isAnalyticsEnabled
			+ " recommendations=" + appConfig.isPersonalizedRecommendationsEnabled);
		setupBackButton();
		setupConfigToggles();
		setupPrivacyPolicyButton();
	}
	
	/**
	 * Configures the back button click listener. When the user presses the back
	 * button, this method triggers a fade animation, logs the action, and finishes
	 * the current activity, returning to the previous screen.
	 *
	 * <p>The fade animation provides a smooth visual transition when exiting
	 * the privacy policy screen.
	 *
	 * @see ActivityAnimator#animActivityFade(BaseActivity)
	 * @see #finish()
	 */
	private void setupBackButton() {
		binding.topBar.btnBack.setOnClickListener(view -> {
			logger.debug("Back button pressed, finishing activity");
			ActivityAnimator.animActivityFade(PrivacyPolicyActivity.this);
			finish();
		});
	}
	
	/**
	 * Configures all privacy toggle controls in the settings section. This method
	 * initializes three toggle rows:
	 * <ul>
	 * <li>Crash Reporting – controlled by {@code appconfig.isCrashReportingEnabled}</li>
	 * <li>Analytics – controlled by {@code appconfig.isAnalyticsEnabled}</li>
	 * <li>Personalized Recommendations – controlled by
	 *     {@code appconfig.isPersonalizedRecommendationsEnabled}</li>
	 * </ul>
	 *
	 * <p>Each toggle is initialized with the current state from the application
	 * configuration and updates the corresponding field when toggled. All changes
	 * are automatically saved via {@link AppConfigs#save()}.
	 *
	 * @see #setupToggle(View, ImageView, boolean, Consumer)
	 * @see AppConfigs
	 */
	private void setupConfigToggles() {
		setupToggle(binding.configurations.btnEnableCrashReport,
			binding.configurations.ivCrashReport,
			appConfig.isCrashReportingEnabled,
			enabled -> appConfig.isCrashReportingEnabled = enabled);
		setupToggle(binding.configurations.btnEnableAnalytics,
			binding.configurations.ivEnableAnalytics,
			appConfig.isAnalyticsEnabled,
			enabled -> appConfig.isAnalyticsEnabled = enabled);
		setupToggle(binding.configurations.btnRecommendations,
			binding.configurations.ivRecommendations,
			appConfig.isPersonalizedRecommendationsEnabled,
			enabled -> appConfig.isPersonalizedRecommendationsEnabled = enabled);
	}
	
	/**
	 * Configures a privacy toggle row with a click listener that toggles its
	 * state, updates the associated icon, and persists the change via the
	 * application configuration. The toggle state is maintained in a boolean
	 * array to allow mutation within the lambda expression.
	 *
	 * <p><strong>Behavior:</strong>
	 * <ul>
	 * <li>Sets the initial icon state based on {@code initialState}.</li>
	 * <li>On click: toggles the state, updates the icon (check/uncheck).</li>
	 * <li>Calls the {@code setter} consumer with the new state.</li>
	 * <li>Saves the updated configuration via {@link AppConfigs#save()}.</li>
	 * <li>Logs the toggle change for debugging.</li>
	 * </ul>
	 *
	 * @param row          The toggle row view to attach the click listener to.
	 * @param icon         The ImageView displaying the check/uncheck icon.
	 * @param initialState The initial state of the toggle (true = checked).
	 * @param setter       Consumer to update the corresponding configuration field.
	 * @see #getToggleLabel(View)
	 * @see AppConfigs#save()
	 */
	private void setupToggle(View row, @NonNull ImageView icon,
	                         boolean initialState, @NonNull Consumer<Boolean> setter) {
		boolean[] state = {initialState};
		icon.setImageResource(state[0]
			? R.drawable.ic_check_box : R.drawable.ic_uncheck_box);
		row.setOnClickListener(v -> {
			state[0] = !state[0];
			icon.setImageResource(state[0]
				? R.drawable.ic_check_box : R.drawable.ic_uncheck_box);
			setter.accept(state[0]);
			appConfig.save();
			logger.debug("Privacy toggle changed: " + getToggleLabel(row)
				+ " -> " + state[0]);
		});
	}
	
	/**
	 * Configures the privacy policy button to open the official privacy policy
	 * webpage in the system browser. When clicked, this method creates an Intent
	 * with action {@link Intent#ACTION_VIEW} and navigates to the privacy policy
	 * URL ({@code https://privacy.tubeaio.com/}).
	 *
	 * <p>The URL is logged for debugging purposes before the intent is launched.
	 *
	 * @see Intent#ACTION_VIEW
	 * @see Uri#parse(String)
	 */
	private void setupPrivacyPolicyButton() {
		binding.actionButtons.btnOpenPrivacyPolicy.setOnClickListener(v -> {
			logger.debug("Opening privacy policy URL: https://privacy.tubeaio.com/");
			Intent intent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://privacy.tubeaio.com/"));
			startActivity(intent);
		});
	}
	
	/**
	 * Returns a string identifier for a privacy toggle row based on its view ID.
	 * This method maps each toggle row's ID to a simple string label used for
	 * logging, analytics tracking, or state management.
	 *
	 * <p><strong>Mapping:</strong>
	 * <ul>
	 * <li>{@code R.id.btnEnableCrashReport} → "crash_report"</li>
	 * <li>{@code R.id.btnEnableAnalytics} → "analytics"</li>
	 * <li>{@code R.id.btnRecommendations} → "recommendations"</li>
	 * <li>Any other ID → "unknown"</li>
	 * </ul>
	 *
	 * @param row The view whose ID should be mapped to a label string.
	 * @return A string identifier representing the toggle type, or "unknown"
	 *         if the ID is not recognized.
	 */
	@NonNull
	private String getToggleLabel(View row) {
		int id = row.getId();
		if (id == R.id.btnEnableCrashReport) return "crash_report";
		if (id == R.id.btnEnableAnalytics) return "analytics";
		if (id == R.id.btnRecommendations) return "recommendations";
		return "unknown";
	}
}
