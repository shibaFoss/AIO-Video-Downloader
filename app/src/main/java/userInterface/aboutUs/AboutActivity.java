package userInterface.aboutUs;

import android.view.LayoutInflater;

import com.nextgen.databinding.ActivityAbout1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

public class AboutActivity extends BaseActivity<ActivityAbout1Binding> {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected ActivityAbout1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityAbout1Binding.inflate(inflater);
	}
	
	@Override protected void onLoadedLayout() {
	
	}
}
