package userInterface.aboutUs;

import android.graphics.Color;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.nextgen.R;
import com.nextgen.databinding.ActivityAbout1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.VersionInfo;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.ActivityAnimator;

public class AboutActivity extends BaseActivity<ActivityAbout1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected ActivityAbout1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityAbout1Binding.inflate(inflater);
	}
	
	@Override protected void onLoadedLayout() {
		setupButtonsClicks();
		loadVersionInfo();
		loadUserAgreement();
	}
	
	private void setupButtonsClicks() {
		binding.actionBar.btnBack.setOnClickListener(view -> {
			finish();
			ActivityAnimator.animActivityFade(AboutActivity.this);
		});
	}
	
	private void loadVersionInfo() {
		String versionName = VersionInfo.getVersionName(getApplicationContext());
		binding.appVersion.tvVersion.setText(versionName);
	}
	
	private void loadUserAgreement() {
		TextView tvAgreement = binding.userAgreement.tvUserAgreement;
		tvAgreement.setText(HtmlCompat.fromHtml(
			getString(R.string.text_agreement_html),
			HtmlCompat.FROM_HTML_MODE_LEGACY));
		
		tvAgreement.setMovementMethod(LinkMovementMethod.getInstance());
		tvAgreement.setLinkTextColor(
			ContextCompat.getColor(this, R.color.style_color_text_link));
		tvAgreement.setHighlightColor(Color.TRANSPARENT);
	}
}
