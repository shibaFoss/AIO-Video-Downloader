package userInterface.userPrivacy;

import android.view.LayoutInflater;

import com.nextgen.databinding.ActivityPrivacyPolicy1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.views.ActivityAnimator;

public class PrivacyPolicyActivity extends BaseActivity<ActivityPrivacyPolicy1Binding> {
	
	@Override protected ActivityPrivacyPolicy1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityPrivacyPolicy1Binding.inflate(inflater);
	}
	
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected void onLoadedLayout() {
		setupBackButton();
	}
	
	private void setupBackButton() {
		binding.topBar.btnBack.setOnClickListener(view -> {
			ActivityAnimator.animActivityFade(PrivacyPolicyActivity.this);
			finish();
		});
	}
	
}
