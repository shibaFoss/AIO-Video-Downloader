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
import coreUtils.library.views.ActivityAnimator;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsRepo;

public class PrivacyPolicyActivity extends BaseActivity<ActivityPrivacyPolicy1Binding> {
	
	private AppConfigs config;
	
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
		config = AppConfigsRepo.getConfig();
		setupBackButton();
		setupConfigToggles();
		setupPrivacyPolicyButton();
	}

	private void setupBackButton() {
		binding.topBar.btnBack.setOnClickListener(view -> {
			ActivityAnimator.animActivityFade(PrivacyPolicyActivity.this);
			finish();
		});
	}
	
	private void setupConfigToggles() {
		setupToggle(binding.configurations.btnEnableCrashReport,
			binding.configurations.ivCrashReport,
			config.isCrashReportingEnabled,
			enabled -> config.isCrashReportingEnabled = enabled);
		setupToggle(binding.configurations.btnEnableAnalytics,
			binding.configurations.ivEnableAnalytics,
			config.isAnalyticsEnabled,
			enabled -> config.isAnalyticsEnabled = enabled);
		setupToggle(binding.configurations.btnRecommendations,
			binding.configurations.ivRecommendations,
			config.isPersonalizedRecommendationsEnabled,
			enabled -> config.isPersonalizedRecommendationsEnabled = enabled);
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
			config.save();
		});
	}
	
	private void setupPrivacyPolicyButton() {
		binding.actionButtons.btnOpenPrivacyPolicy.setOnClickListener(v -> {
			Intent intent = new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://privacy.tubeaio.com/"));
			startActivity(intent);
		});
	}
}
