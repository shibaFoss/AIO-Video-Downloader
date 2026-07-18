package userInterface.languagePicker;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.nextgen.R;
import com.nextgen.databinding.ActivityLanguage1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LocaleHelper;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.ActivityAnimator;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsRepo;
import userInterface.mainScreen.MainActivity;

/**
 * Activity that allows users to select their preferred language for the application.
 * This screen displays a list of language options as radio buttons, provides a
 * continue button to confirm selection, and a skip button to bypass configuration.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Displays a radio button list of available languages from the ViewModel.</li>
 * <li>Pre-selects the currently configured app language (if any).</li>
 * <li>Saves selected language to {@link AppConfigsRepo} and applies locale changes.</li>
 * <li>Locks screen orientation to portrait for consistent layout rendering.</li>
 * </ul>
 *
 * <p>Upon selection or skip, navigates to {@link MainActivity} with a fade animation.
 *
 * @see BaseActivity
 * @see LanguageViewModel
 * @see LanguageItem
 */
public final class LanguageActivity extends BaseActivity<ActivityLanguage1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	/**
	 * Determines whether the activity's screen orientation should be locked.
	 * This implementation returns {@code true}, forcing the language selection
	 * screen to remain in portrait mode regardless of device rotation.
	 *
	 * @return {@code true} to lock the activity to portrait orientation.
	 * @see BaseActivity#shouldLockOrientation()
	 * @see #onLoadedLayout()
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the activity's layout using view binding and returns the generated
	 * binding instance for {@code activity_language_0.xml}. This method is called
	 * during the base activity's {@code setContentView()} phase to create the
	 * binding object that provides type-safe access to all views in the layout,
	 * including the language RadioGroup container and the action buttons.
	 *
	 * @param inflater The layout inflater service used to create the view hierarchy.
	 *                 Must not be {@code null}.
	 * @return The {@link ActivityLanguage1Binding} instance containing references
	 * to all views defined in the language selection screen layout.
	 * @see BaseActivity#inflateBinding(LayoutInflater)
	 */
	@Override
	protected ActivityLanguage1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityLanguage1Binding.inflate(inflater);
	}
	
	/**
	 * Performs post-layout initialization after the content view has been inflated.
	 * This method is invoked by the base activity at the end of {@code onCreate()}
	 * and is responsible for populating the language list in the RadioGroup,
	 * setting up the continue button click listener, and configuring the skip button.
	 *
	 * <p><strong>Initialization order:</strong>
	 * <ol>
	 * <li>Populates language options via {@link #populateLanguages(LanguageViewModel)}.</li>
	 * <li>Sets up continue button via {@link #setupContinueButton()}.</li>
	 * <li>Configures skip button via {@link #setupSkipButton()}.</li>
	 * </ol>
	 *
	 * @see BaseActivity#onLoadedLayout()
	 * @see #populateLanguages(LanguageViewModel)
	 * @see #setupContinueButton()
	 * @see #setupSkipButton()
	 */
	@Override
	protected void onLoadedLayout() {
		populateLanguages(getLanguageViewModel());
		setupContinueButton();
		setupSkipButton();
		setupBackButton();
	}
	
	/**
	 * Provides the ViewModel instance for the language selection screen. This method
	 * uses {@link ViewModelProvider} to obtain or create the {@link LanguageViewModel},
	 * which manages the list of available languages.
	 *
	 * <p>The ViewModel is scoped to the activity's lifecycle, surviving configuration
	 * changes such as screen rotations. This ensures language data is preserved and
	 * not re-fetched unnecessarily.
	 *
	 * @return A non-null {@link LanguageViewModel} instance associated with this activity.
	 * @see ViewModelProvider
	 * @see LanguageViewModel
	 */
	@NonNull
	private LanguageViewModel getLanguageViewModel() {
		ViewModelProvider provider = new ViewModelProvider(this);
		return provider.get(LanguageViewModel.class);
	}
	
	/**
	 * Populates the RadioGroup with language options from the ViewModel. This method
	 * observes the language LiveData, dynamically creates MaterialRadioButton views
	 * for each language item, adds them to the container, and pre-selects the
	 * currently configured app language (if any). The radio button text is set to
	 * the language's native display name, and the LanguageItem is stored as a tag.
	 *
	 * @param viewModel The ViewModel providing the list of available languages.
	 * @see #setupContinueButton()
	 * @see LanguageViewModel#getLanguages()
	 */
	private void populateLanguages(LanguageViewModel viewModel) {
		viewModel.getLanguages().observe(this, languages -> {
			RadioGroup group = binding.languagesContainer.languageRadioGroup;
			group.removeAllViews();
			
			LayoutInflater inflater = LayoutInflater.from(this);
			for (int i = 0; i < languages.size(); i++) {
				LanguageItem item = languages.get(i);
				MaterialRadioButton radioButton = (MaterialRadioButton)
					inflater.inflate(R.layout.activity_language_1_p3, group, false);
				radioButton.setText(item.getLanguageName());
				radioButton.setTag(item);
				group.addView(radioButton);
			}
			
			int count = group.getChildCount();
			for (int i = 0; i < count; i++) {
				View child = group.getChildAt(i);
				if (child instanceof MaterialRadioButton radioButton) {
					if (child.getTag() != null) {
						String langTag = ((LanguageItem) child.getTag()).getLanguageCode();
						String appLanguage = AppConfigsRepo.getConfig().selectedLanguageCode;
						if (langTag.equals(appLanguage)) {
							child.performClick();
						}
					}
				}
			}
		});
	}
	
	/**
	 * Configures the continue button click listener. When clicked, this method
	 * iterates through all radio buttons in the language RadioGroup to find the
	 * currently selected option. If a selected radio button is found, its associated
	 * LanguageItem tag is retrieved and passed to {@link #applyLanguage(LanguageItem)}.
	 *
	 * <p>If no radio button is selected, the method returns without performing
	 * any action, leaving the user to select a language before proceeding.
	 *
	 * @see #applyLanguage(LanguageItem)
	 * @see #populateLanguages(LanguageViewModel)
	 */
	private void setupContinueButton() {
		binding.buttons.btnContinue.setOnClickListener(view -> {
			RadioGroup group = binding.languagesContainer.languageRadioGroup;
			int count = group.getChildCount();
			for (int i = 0; i < count; i++) {
				View child = group.getChildAt(i);
				if (child instanceof MaterialRadioButton radioButton) {
					if (radioButton.isChecked()) {
						LanguageItem item = (LanguageItem) radioButton.getTag();
						applyLanguage(item);
						return;
					}
				}
			}
		});
	}
	
	/**
	 * Configures the skip button click listener. When the user clicks the skip button,
	 * the method marks the locale as configured by setting
	 * {@link AppConfigs#isLocaleConfigured} to {@code true}, persists the change via
	 * {@link AppConfigs#save()}, and navigates to the next activity. No language
	 * change is applied, so the app continues with the system default or previously
	 * selected language.
	 *
	 * @see #openNextActivity()
	 * @see AppConfigsRepo#getConfig()
	 */
	private void setupSkipButton() {
		binding.buttons.btnSkipForNow.setOnClickListener(view -> {
			AppConfigsRepo.getConfig().isLocaleConfigured = true;
			AppConfigsRepo.getConfig().save();
			openNextActivity();
		});
	}
	
	/**
	 * Configures the back button click listener. When clicked, this method
	 * invokes the default system back button behavior by calling
	 * {@link #onBackPressed()}. This allows the user to return to the
	 * previous screen in the navigation stack without applying any
	 * language changes.
	 *
	 * @see #onBackPressed()
	 */
	private void setupBackButton() {
		binding.actionBar.btnBack.setOnClickListener(view -> {
			finish();
			ActivityAnimator.animActivityFade(LanguageActivity.this);
		});
	}
	
	/**
	 * Applies the selected language to the application configuration and locale.
	 * This method logs the selected language, updates {@link AppConfigs} with the
	 * new language code and sets {@code isLocaleConfigured} to {@code true},
	 * persists the changes, applies the locale change via
	 * {@link LocaleHelper#changeLanguage(String, BaseActivity)}
	 * and navigates to the next activity.
	 *
	 * @param item The selected {@link LanguageItem} containing the language code.
	 * @see #openNextActivity()
	 * @see LocaleHelper#changeLanguage(String, BaseActivity)
	 */
	private void applyLanguage(LanguageItem item) {
		logger.debug("Language selected: " + item.getLanguageName()
			+ " (" + item.getLanguageCode() + ")");
		AppConfigsRepo.getConfig().selectedLanguageCode = item.getLanguageCode();
		AppConfigsRepo.getConfig().isLocaleConfigured = true;
		AppConfigsRepo.getConfig().save();
		LocaleHelper.changeLanguage(item.getLanguageCode(), this);
		openNextActivity();
	}
	
	/**
	 * Navigates to the main activity and finishes the current language selection screen.
	 * This method constructs an intent targeting {@link MainActivity}, adds flags to
	 * clear the activity back stack, starts the activity with a fade transition
	 * animation, and closes the current activity.
	 *
	 * <p><strong>Intent flags:</strong>
	 * {@link Intent#FLAG_ACTIVITY_NEW_TASK} and {@link Intent#FLAG_ACTIVITY_CLEAR_TASK}
	 * ensure the user cannot press the back button to return to the language
	 * selection screen after completing the initial setup flow.
	 *
	 * @see ActivityAnimator#animActivityFade(BaseActivity)
	 */
	private void openNextActivity() {
		Intent intent = new Intent(LanguageActivity.this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		ActivityAnimator.animActivityFade(LanguageActivity.this);
		finish();
	}
}
