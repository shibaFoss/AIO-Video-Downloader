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

public class PrivacyPolicyActivity extends BaseActivity<ActivityPrivacyPolicy1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private AppConfigs appconfig;
	
	@Override
	protected ActivityPrivacyPolicy1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityPrivacyPolicy1Binding.inflate(inflater);
	}
	
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override
	protected void onLoadedLayout() {
		appconfig = AppConfigsRepo.getConfig();
		logger.debug("Privacy config loaded: crash=" + appconfig.isCrashReportingEnabled
			+ " analytics=" + appconfig.isAnalyticsEnabled
			+ " recommendations=" + appconfig.isPersonalizedRecommendationsEnabled);
		setupBackButton();
		setupConfigToggles();
		setupPrivacyPolicyButton();
	}

	private void setupBackButton() {
		binding.topBar.btnBack.setOnClickListener(view -> {
			logger.debug("Back button pressed, finishing activity");
			ActivityAnimator.animActivityFade(PrivacyPolicyActivity.this);
			finish();
		});
	}
	
	private void setupConfigToggles() {
		setupToggle(binding.configurations.btnEnableCrashReport,
			binding.configurations.ivCrashReport,
			appconfig.isCrashReportingEnabled,
			enabled -> appconfig.isCrashReportingEnabled = enabled);
		setupToggle(binding.configurations.btnEnableAnalytics,
			binding.configurations.ivEnableAnalytics,
			appconfig.isAnalyticsEnabled,
			enabled -> appconfig.isAnalyticsEnabled = enabled);
		setupToggle(binding.configurations.btnRecommendations,
			binding.configurations.ivRecommendations,
			appconfig.isPersonalizedRecommendationsEnabled,
			enabled -> appconfig.isPersonalizedRecommendationsEnabled = enabled);
	}
	
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
			appconfig.save();
			logger.debug("Privacy toggle changed: " + getToggleLabel(row)
				+ " -> " + state[0]);
		});
	}
	
	private void setupPrivacyPolicyButton() {
		binding.actionButtons.btnOpenPrivacyPolicy.setOnClickListener(v -> {
			logger.debug("Opening privacy policy URL: https://privacy.tubeaio.com/");
			Intent intent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://privacy.tubeaio.com/"));
			startActivity(intent);
		});
	}

	@NonNull
	private String getToggleLabel(View row) {
		int id = row.getId();
		if (id == R.id.btnEnableCrashReport) return "crash_report";
		if (id == R.id.btnEnableAnalytics) return "analytics";
		if (id == R.id.btnRecommendations) return "recommendations";
		return "unknown";
	}
}
