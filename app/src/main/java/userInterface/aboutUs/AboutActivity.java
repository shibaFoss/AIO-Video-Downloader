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
import coreUtils.library.views.ActivityAnimator;

/**
 * About screen activity that displays information about the application, including
 * the current version number and a detailed user agreement with clickable links.
 * This screen provides users with transparency about the app's terms and policies.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Displays the current application version name.</li>
 * <li>Shows the user agreement text with HTML formatting and clickable links.</li>
 * <li>Provides a back button to return to the previous screen with a fade animation.</li>
 * <li>Locks screen orientation to portrait for consistent layout rendering.</li>
 * </ul>
 *
 * <p>The user agreement text is loaded from an HTML string resource and rendered
 * with support for hyperlinks. Links are clickable and styled with the app's
 * primary link color, with a transparent highlight on touch.
 *
 * @see BaseActivity
 * @see VersionInfo
 * @see HtmlCompat
 */
public class AboutActivity extends BaseActivity<ActivityAbout1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	/**
	 * Determines whether the activity's screen orientation should be locked to portrait.
	 * This implementation returns {@code true} to prevent screen rotation on the about
	 * screen, ensuring consistent layout and visual appearance across all devices.
	 *
	 * @return {@code true} to lock the activity to portrait orientation.
	 * @see BaseActivity#shouldLockOrientation()
	 */
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the activity's layout using view binding and returns the generated
	 * binding instance for {@code activity_about_1.xml}. This method is called
	 * during the base activity's {@code setContentView()} phase to provide type-safe
	 * access to all views in the layout, including the action bar, version info,
	 * and user agreement sections.
	 *
	 * @param inflater The LayoutInflater used to inflate the layout.
	 * @return The {@link ActivityAbout1Binding} instance for the about screen.
	 * @see BaseActivity#inflateBinding(LayoutInflater)
	 */
	@Override protected ActivityAbout1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityAbout1Binding.inflate(inflater);
	}
	
	/**
	 * Performs post-layout initialization after the content view has been inflated.
	 * This method is invoked at the end of {@code onCreate()} and sets up the back
	 * button click listener, loads the current application version information, and
	 * displays the user agreement text with clickable links.
	 *
	 * <p><strong>Initialization order:</strong>
	 * <ol>
	 * <li>Configures button click listeners via {@link #setupButtonsClicks()}.</li>
	 * <li>Loads and displays version info via {@link #loadVersionInfo()}.</li>
	 * <li>Loads and displays user agreement via {@link #loadUserAgreement()}.</li>
	 * </ol>
	 *
	 * @see BaseActivity#onLoadedLayout()
	 * @see #setupButtonsClicks()
	 * @see #loadVersionInfo()
	 * @see #loadUserAgreement()
	 */
	@Override protected void onLoadedLayout() {
		setupButtonsClicks();
		loadVersionInfo();
		loadUserAgreement();
	}
	
	/**
	 * Configures the back button click listener to finish the current activity with a
	 * smooth fade animation. When the user presses the back button, the activity is
	 * closed and the previous screen appears with a fade transition effect applied via
	 * {@link ActivityAnimator#animActivityFade(BaseActivity)}.
	 *
	 * @see ActivityAnimator#animActivityFade(BaseActivity)
	 * @see #finish()
	 */
	private void setupButtonsClicks() {
		binding.actionBar.btnBack.setOnClickListener(view -> {
			finish();
			ActivityAnimator.animActivityFade(AboutActivity.this);
		});
	}
	
	/**
	 * Retrieves the current application version name from the system and displays it
	 * in the version TextView. The version name is obtained from the application context
	 * using {@link VersionInfo#getVersionName(android.content.Context)}, which reads
	 * the version name defined in the app's build.gradle file.
	 *
	 * @see VersionInfo#getVersionName(android.content.Context)
	 */
	private void loadVersionInfo() {
		String versionName = VersionInfo.getVersionName(getApplicationContext());
		binding.appVersion.tvVersion.setText(versionName);
	}
	
	/**
	 * Loads and displays the user agreement text from an HTML string resource.
	 * The HTML content is parsed with legacy compatibility mode to support common
	 * HTML tags like links, lists, and text formatting. Clickable links are enabled
	 * via {@link LinkMovementMethod#getInstance()}, and the link text color is set
	 * to the app's primary link color. The highlight color is set to transparent to
	 * avoid visual distraction when users tap on links.
	 *
	 * @see HtmlCompat#fromHtml(String, int)
	 * @see LinkMovementMethod#getInstance()
	 * @see ContextCompat#getColor(android.content.Context, int)
	 */
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
