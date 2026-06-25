package userInterface.termsConsPolicy;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.nextgen.R;
import com.nextgen.databinding.ActivityTermsCondi1Binding;
import com.nextgen.databinding.ActivityTermsCondi1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.ActivityAnimator;
import coreUtils.library.views.StylizedToastView;
import coreUtils.library.views.TextViewsUtils;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsRepo;
import userInterface.mainScreen.MainActivity;
import userInterface.openingSplash.OpeningActivity;

/**
 * Activity that displays the application's Terms and Conditions and Privacy Policy.
 * This screen presents a scrollable legal document with expandable sections for
 * detailed clauses, a terms acceptance checkbox, and action buttons for agreement
 * or navigation back. The activity tracks whether it was launched from the opening
 * screen to determine appropriate navigation behavior upon agreement.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Displays expandable/collapsible terms sections (Acceptance, Use of App,
 *     Intellectual Property, Prohibited Activities, Limitations, Changes).</li>
 * <li>Provides a checkbox for the user to indicate agreement with the terms.</li>
 * <li>Saves the user's agreement status to {@link AppConfigsRepo} when confirmed.</li>
 * <li>Applies gradient color span to the word "Conditions" in the title.</li>
 * <li>Shows an error toast with haptic feedback if user declines terms.</li>
 * </ul>
 *
 * <p><strong>Navigation behavior:</strong>
 * If launched from {@link OpeningActivity} and user accepts terms → proceeds to
 * next screen. If launched from elsewhere and user accepts → back button press.
 * If user declines terms → error toast with vibration.
 *
 * @see BaseActivity
 * @see ActivityTermsCondi1Binding
 * @see AppConfigsRepo
 */
public final class TermsPolicyActivity
	extends BaseActivity<ActivityTermsCondi1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	public static final String KEY_ACTIVITY_LAUNCHED_LOCATION = "Location";
	public static final short LAUNCHED_FROM_OPENING_SCREEN = 1;
	public static final short LAUNCHED_FROM_SETTINGS_SCREEN = 2;
	
	/**
	 * Determines whether the activity's screen orientation should be locked.
	 * This implementation returns {@code true}, forcing the terms and conditions
	 * screen to remain in portrait mode regardless of device rotation.
	 *
	 * <p><strong>Design rationale:</strong>
	 * Locking the orientation ensures the expandable terms sections, checkbox,
	 * and agree button maintain a consistent layout without unexpected UI
	 * reconfigurations while the user reads the legal document.
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
	 * binding instance for {@code activity_terms_con1.xml}. This method is called
	 * during the base activity's {@code setContentView()} phase to create the
	 * binding object that provides type-safe access to all views in the layout,
	 * including the title TextView, expandable terms sections, terms checkbox,
	 * agree button, and back button.
	 *
	 * <p>The layout uses a merged view structure where {@code top1} contains the
	 * title and {@code top2} contains the scrollable terms content, checkbox,
	 * and action buttons.
	 *
	 * @param inflater The layout inflater service used to create the view hierarchy.
	 *                 Must not be {@code null}.
	 * @return The {@link ActivityTermsCondi1Binding} instance containing references
	 * to all views defined in the terms and conditions screen layout.
	 * @see BaseActivity#inflateBinding(LayoutInflater)
	 */
	@Override
	protected ActivityTermsCondi1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityTermsCondi1Binding.inflate(inflater);
	}
	
	@Override
	protected void onLoadedLayout() {
		hideExpandedDetails();
		setupButtonClickEvents();
	}
	
	
	private void setupButtonClickEvents() {
		setupBackButtonClickEvent();
		setupExpandTermsButtonClickEvent();
		setupAgreeTermsButton();
	}
	
	/**
	 * Configures the back button click listener. When the user clicks the back button,
	 * the current activity is finished and removed from the activity stack,
	 * returning the user to the previous screen.
	 *
	 * <p>This provides a consistent navigation mechanism for returning to the
	 * previous screen, whether the terms activity was launched from the opening
	 * screen, settings menu, or other entry points.
	 *
	 * @see #finish()
	 */
	private void setupBackButtonClickEvent() {
		binding.topBar.btnBack.setOnClickListener(view -> finish());
	}
	
	/**
	 * Sets up click listeners for all expandable terms and conditions sections.
	 * Each item in the terms list (e.g., Acceptance, Use of App, Intellectual Property)
	 * toggles the visibility of its corresponding detailed explanation view when clicked.
	 *
	 * <p><strong>Section mappings:</strong>
	 * <ul>
	 * <li>{@code itemAcceptance} toggles {@code extraAcceptance}</li>
	 * <li>{@code itemUseOfApp} toggles {@code extraUseOfApp}</li>
	 * <li>{@code itemIntellectual} toggles {@code extraIntellectual}</li>
	 * <li>{@code itemProhibited} toggles {@code extraProhibited}</li>
	 * <li>{@code itemLimitation} toggles {@code extraLimitation}</li>
	 * <li>{@code itemChanges} toggles {@code extraChanges}</li>
	 * </ul>
	 *
	 * <p>Each click listener invokes {@link #toggleVisibility(View)} to show or
	 * hide the corresponding detailed content section, providing an expandable/
	 * collapsible user interface pattern.
	 *
	 * @see #toggleVisibility(View)
	 * @see #hideExpandedDetails()
	 */
	private void setupExpandTermsButtonClickEvent() {
		binding.terms.itemAcceptance.setOnClickListener(view ->
			toggleVisibility(binding.terms.extraAcceptance));
		
		binding.terms.itemUseOfApp.setOnClickListener(view ->
			toggleVisibility(binding.terms.extraUseOfApp));
		
		binding.terms.itemIntellectual.setOnClickListener(view ->
			toggleVisibility(binding.terms.extraIntellectual));
		
		binding.terms.itemProhibited.setOnClickListener(view ->
			toggleVisibility(binding.terms.extraProhibited));
		
		binding.terms.itemLimitation.setOnClickListener(view ->
			toggleVisibility(binding.terms.extraLimitation));
		
		binding.terms.itemChanges.setOnClickListener(view ->
			toggleVisibility(binding.terms.extraChanges));
	}
	
	private void setupAgreeTermsButton() {
		binding.actionButtons.btnAgreeContinue.setOnClickListener(view -> {
			AppConfigs appConfigs = AppConfigsRepo.getConfig();
			appConfigs.isTermsConditionsAgreed = true;
			appConfigs.save();
			
			if (appConfigs.isTermsConditionsAgreed && isLaunchedFromOpeningScreen()) {
				Intent intent = new Intent(this, MainActivity.class);
				startActivity(intent);
				ActivityAnimator.animActivityFade(this);
				finish();
			} else if (appConfigs.isTermsConditionsAgreed && !isLaunchedFromOpeningScreen()) {
				binding.topBar.btnBack.performClick();
			} else {
				String toastMessage = StringHelper.getText(R.string.hint_you_must_accept_the_terms);
				StylizedToastView.showError(this, toastMessage);
				vibrate();
				finish();
			}
		});
	}
	
	/**
	 * Hides all expandable detail sections within the terms and conditions screen.
	 * This method sets the visibility of multiple extra content views to
	 * {@link View#GONE}, including acceptance terms, app usage guidelines,
	 * intellectual property information, prohibited activities, limitation of
	 * liability, and changes to terms sections.
	 *
	 * <p><strong>Sections hidden:</strong>
	 * <ul>
	 * <li>{@code extraAcceptance} - Terms acceptance details</li>
	 * <li>{@code extraUseOfApp} - Proper app usage guidelines</li>
	 * <li>{@code extraIntellectual} - Intellectual property rights</li>
	 * <li>{@code extraProhibited} - Prohibited activities list</li>
	 * <li>{@code extraLimitation} - Limitation of liability</li>
	 * <li>{@code extraChanges} - Changes to terms notification</li>
	 * </ul>
	 *
	 * <p>This method is typically called when collapsing all expanded sections
	 * or resetting the UI to its default collapsed state. The sections can later
	 * be shown individually when the user expands specific details.
	 *
	 * @see View#setVisibility(int)
	 * @see View#GONE
	 */
	private void hideExpandedDetails() {
		binding.terms.extraAcceptance.setVisibility(View.GONE);
		binding.terms.extraUseOfApp.setVisibility(View.GONE);
		binding.terms.extraIntellectual.setVisibility(View.GONE);
		binding.terms.extraProhibited.setVisibility(View.GONE);
		binding.terms.extraLimitation.setVisibility(View.GONE);
		binding.terms.extraChanges.setVisibility(View.GONE);
	}
	
	/**
	 * Toggles the visibility state of the specified view between visible and gone.
	 * If the target view is currently {@link View#GONE}, it becomes {@link View#VISIBLE}.
	 * If the target view is currently {@link View#VISIBLE}, it becomes {@link View#GONE}.
	 *
	 * <p>This utility method is commonly used for expanding/collapsing UI sections,
	 * showing/hiding loading indicators, or toggling additional options panels.
	 *
	 * @param targetView The view whose visibility state should be toggled.
	 *                   Must not be {@code null}.
	 * @see View#setVisibility(int)
	 */
	private void toggleVisibility(View targetView) {
		boolean isNoVisibility = targetView.getVisibility() == View.GONE;
		targetView.setVisibility(isNoVisibility ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Determines whether this activity was launched from the opening screen
	 * (i.e., {@link OpeningActivity}) rather than from another entry point.
	 * This method extracts the {@link #KEY_ACTIVITY_LAUNCHED_LOCATION} extra
	 * from the intent and compares it against the default value
	 * {@link #LAUNCHED_FROM_OPENING_SCREEN}.
	 *
	 * <p><strong>Usage context:</strong>
	 * The terms policy activity can be launched from multiple locations (e.g.,
	 * opening screen, settings menu, or after an update). This method allows
	 * the activity to customize its behavior—such as showing a back button or
	 * modifying the UI—based on its origin.
	 *
	 * @return {@code true} if the activity was launched from {@link OpeningActivity},
	 * {@code false} otherwise (e.g., launched from settings or other screens).
	 * @see #getIntent()
	 * @see #KEY_ACTIVITY_LAUNCHED_LOCATION
	 * @see #LAUNCHED_FROM_OPENING_SCREEN
	 */
	private boolean isLaunchedFromOpeningScreen() {
		Intent intent = getIntent();
		short defaultIntentValue = LAUNCHED_FROM_OPENING_SCREEN;
		short launchLocation = intent.getShortExtra(
			KEY_ACTIVITY_LAUNCHED_LOCATION, defaultIntentValue);
		
		return launchLocation == defaultIntentValue;
	}
}
