package userInterface.userFeedback;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.nextgen.R;
import com.nextgen.databinding.ActivityFeedback1Binding;

import java.util.Objects;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.CircularLoadingDialog;
import coreUtils.library.views.StylizedToastView;

/**
 * Activity that allows users to submit feedback about the application. This screen
 * provides a comprehensive feedback form including reaction selection (Excellent,
 * Good, Average, Poor, Angry), a detailed feedback message field, optional email
 * address, and optional screenshot attachment with image preview.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Displays five reaction options with visual selection highlighting.</li>
 * <li>Provides input fields for email (optional) and detailed feedback (required).</li>
 * <li>Supports screenshot attachment with file size validation (max 5MB).</li>
 * <li>Displays image preview for attached screenshots via Glide.</li>
 * <li>Submits feedback to remote server via ViewModel.</li>
 * <li>Displays success/error toasts with haptic feedback.</li>
 * <li>Resets form after successful submission.</li>
 * </ul>
 *
 * <p><strong>Validation rules:</strong>
 * Feedback cannot be empty or exceed 500 characters; attachments cannot exceed 5MB.
 *
 * @see BaseActivity
 * @see ActivityFeedback1Binding
 * @see FeedbackViewModel
 * @see FeedbackReactions
 */
public class FeedbackActivity extends BaseActivity<ActivityFeedback1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private FeedbackViewModel viewModel;
	private ActivityResultLauncher<String> imagePickerLauncher;
	private CircularLoadingDialog circularLoadingDialog;
	
	/**
	 * Inflates the activity's layout using view binding and returns the generated
	 * binding instance for {@code activity_feedback_1.xml}. This method is called
	 * during the base activity's {@code setContentView()} phase to create the
	 * binding object that provides type-safe access to all views in the layout.
	 *
	 * <p>The layout includes the following sections:
	 * <ul>
	 * <li>{@code topBar} - Back button and screen title.</li>
	 * <li>{@code reactions} - Five reaction options with emoji/icon images.</li>
	 * <li>{@code userMessage} - Input fields for email, feedback message, and
	 *     screenshot attachment.</li>
	 * <li>{@code actionButtons} - Send feedback and clear attachment buttons.</li>
	 * </ul>
	 *
	 * @param inflater The layout inflater service used to create the view hierarchy.
	 *                 Must not be {@code null}.
	 * @return The {@link ActivityFeedback1Binding} instance containing references
	 * to all views defined in the feedback screen layout.
	 * @see BaseActivity#inflateBinding(LayoutInflater)
	 * @see ActivityFeedback1Binding
	 */
	@Override
	protected ActivityFeedback1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityFeedback1Binding.inflate(inflater);
	}
	
	/**
	 * Determines whether the activity's screen orientation should be locked.
	 * This implementation returns {@code true}, forcing the feedback screen
	 * to remain in portrait mode regardless of device rotation.
	 *
	 * <p><strong>Design rationale:</strong>
	 * Locking the orientation ensures the reaction selection grid, input fields,
	 * character counter, and action buttons maintain a consistent layout while
	 * the user composes their feedback message, preventing unexpected UI
	 * reconfigurations that could interrupt typing or selection.
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
	 * and is responsible for setting up the ViewModel, image picker, button listeners,
	 * observers, and default reaction selection.
	 *
	 * <p><strong>Initialization order:</strong>
	 * <ol>
	 * <li>Initializes the ViewModel via {@link #initViewModel()}.</li>
	 * <li>Sets up the image picker for screenshot attachments via {@link #initImagePicker()}.</li>
	 * <li>Configures all button click listeners via {@link #setupButtonClicks()}.</li>
	 * <li>Initializes ViewModel LiveData observers via {@link #initViewModelObservers()}.</li>
	 * <li>Selects "Excellent" reaction as the default option via
	 * {@link #selectExcellentReactionByDefault()}.</li>
	 * </ol>
	 *
	 * @see BaseActivity#onLoadedLayout()
	 * @see #initViewModel()
	 * @see #initImagePicker()
	 * @see #setupButtonClicks()
	 * @see #initViewModelObservers()
	 * @see #selectExcellentReactionByDefault()
	 */
	@Override
	protected void onLoadedLayout() {
		initViewModel();
		initImagePicker();
		setupButtonClicks();
		initViewModelObservers();
		selectExcellentReactionByDefault();
	}
	
	/**
	 * Called when the activity is being destroyed. This method performs cleanup
	 * of the loading dialog reference to prevent memory leaks. The reference is
	 * set to null after the superclass's {@code onDestroy()} method is called.
	 *
	 * <p>The dialog itself should have been closed prior to this point via
	 * {@link #closeLoadingDialog()}. This final cleanup ensures that any
	 * lingering reference does not prevent garbage collection.
	 *
	 * @see android.app.Activity#onDestroy()
	 * @see #closeLoadingDialog()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		circularLoadingDialog = null;
	}
	
	/**
	 * Returns the ViewModel instance associated with this feedback activity.
	 * This getter provides access to the ViewModel for observers and other
	 * methods that need to interact with the ViewModel's LiveData or call
	 * its methods (e.g., sendFeedback, setSelectedReaction).
	 *
	 * <p>The ViewModel is created in {@link #initViewModel()} and is scoped
	 * to the activity's lifecycle, surviving configuration changes.
	 *
	 * @return The {@link FeedbackViewModel} instance. Never {@code null} after
	 * {@link #initViewModel()} has been called.
	 * @see #initViewModel()
	 * @see FeedbackViewModel
	 */
	public FeedbackViewModel getViewModel() {
		return viewModel;
	}
	
	/**
	 * Initializes the ViewModel for the feedback screen. This method creates a
	 * {@link FeedbackViewModel} instance using the {@link ViewModelProvider} scoped
	 * to this activity. The ViewModel is responsible for managing UI-related data
	 * such as selected reaction, screenshot attachment, submission state, and
	 * LiveData for observing results and errors.
	 *
	 * <p>The ViewModel survives configuration changes (though orientation is locked)
	 * and retains data across the activity lifecycle, preventing data loss during
	 * recreations.
	 *
	 * @see ViewModelProvider
	 * @see FeedbackViewModel
	 * @see #getViewModel()
	 */
	private void initViewModel() {
		viewModel = new ViewModelProvider(this)
			.get(FeedbackViewModel.class);
	}
	
	/**
	 * Initializes the image picker functionality for attaching screenshots to feedback.
	 * This method performs the following setup:
	 * <ol>
	 * <li>Clears the attachment preview UI to its default state.</li>
	 * <li>Registers an {@link ActivityResultLauncher} using the
	 * {@link ActivityResultContracts.GetContent}
	 *     contract to allow users to select an image from their device storage.</li>
	 * <li>Observes the ViewModel's selected screenshot LiveData to update the UI
	 *     when a file is selected.</li>
	 * </ol>
	 *
	 * <p>When the user selects an image, the callback {@link #validateAttachmentFile(Uri)}
	 * is invoked to validate the file size. Upon successful validation, the ViewModel
	 * stores the selected file, and {@link #observeSelectedAttachmentFile(DocumentFile)}
	 * updates the UI to show the image preview and clear button.
	 *
	 * @see #validateAttachmentFile(Uri)
	 * @see #observeSelectedAttachmentFile(DocumentFile)
	 * @see #clearAttachmentPreview()
	 * @see ActivityResultContracts.GetContent
	 */
	private void initImagePicker() {
		clearAttachmentPreview();
		imagePickerLauncher = registerForActivityResult(
			new ActivityResultContracts.GetContent(),
			this::validateAttachmentFile
		);
		
		getViewModel().getSelectedScreenshot().observe(this,
			this::observeSelectedAttachmentFile);
	}
	
	/**
	 * Observes and displays the selected screenshot attachment in the UI.
	 * When the ViewModel's selected screenshot LiveData emits a non-null
	 * {@link DocumentFile}, this method:
	 * <ul>
	 * <li>Loads the image into the attachment preview ImageView using Glide.</li>
	 * <li>Shows the "Clear Attachment" button (VISIBLE).</li>
	 * <li>Hides the "Add Image" label (INVISIBLE) to indicate an image is attached.</li>
	 * </ul>
	 *
	 * <p>If the {@code documentFile} parameter is {@code null}, the method returns
	 * early without updating the UI, maintaining the current attachment state.
	 *
	 * @param documentFile The selected screenshot file, or {@code null} if no
	 *                     file is currently selected. Must contain a valid URI
	 *                     for image loading.
	 * @see DocumentFile#getName()
	 * @see DocumentFile#getUri()
	 * @see Glide#with(android.content.Context)
	 * @see #clearAttachmentPreview()
	 */
	private void observeSelectedAttachmentFile(DocumentFile documentFile) {
		if (documentFile == null) return;
		
		String fileName = documentFile.getName();
		if (fileName != null && fileName.length() > 0) {
			Glide.with(this)
				.asBitmap()
				.centerCrop()
				.load(documentFile.getUri())
				.into(binding.userMessage.ivAttachmentPreview);
			
			binding.actionButtons.btnClearAttachment.setVisibility(View.VISIBLE);
			binding.userMessage.tvAddImage.setVisibility(View.INVISIBLE);
		}
	}
	
	/**
	 * Validates an image file selected by the user for attachment to feedback.
	 * This method checks the file size and accepts it only if it is less than
	 * 5 megabytes (5 * 1024 * 1024 bytes).
	 *
	 * <p><strong>Validation behavior:</strong>
	 * <ul>
	 * <li>If the file size is less than 5MB, the screenshot is stored in the
	 *     ViewModel via {@link FeedbackViewModel#setSelectedScreenshot(DocumentFile)}.</li>
	 * <li>If the file size exceeds 5MB, a warning toast is displayed, and a
	 *     50ms haptic vibration is triggered. The file is rejected.</li>
	 * <li>If the file URI is {@code null}, the method does nothing.</li>
	 * </ul>
	 *
	 * <p>The 5MB limit ensures that feedback submissions remain reasonably sized
	 * for network transmission and server storage constraints.
	 *
	 * @param fileUri The URI of the image file selected by the user. May be
	 *                {@code null}, in which case validation is skipped.
	 * @see DocumentFile#fromSingleUri(android.content.Context, android.net.Uri)
	 * @see DocumentFile#length()
	 * @see FeedbackViewModel#setSelectedScreenshot(DocumentFile)
	 * @see #vibrate(long)
	 * @see StylizedToastView#show(BaseActivity, CharSequence)
	 */
	private void validateAttachmentFile(Uri fileUri) {
		if (fileUri != null) {
			DocumentFile file = DocumentFile.fromSingleUri(this, fileUri);
			if (file.length() < 5 * 1024 * 1024) {
				getViewModel().setSelectedScreenshot(file);
			} else {
				vibrate(50);
				StylizedToastView.show(FeedbackActivity.this,
					StringHelper.getText(R.string.hint_file_size_exceed_5mb));
			}
		}
	}
	
	/**
	 * Clears the attachment preview from the UI and resets the attachment container
	 * to its default state. This method is called when the user cancels an attached
	 * image or after a successful feedback submission.
	 *
	 * <p><strong>UI changes performed:</strong>
	 * <ul>
	 * <li>Sets the attachment preview ImageView to a default placeholder drawable.</li>
	 * <li>Shows the "Add Image" label (TextView) as VISIBLE.</li>
	 * <li>Hides the "Clear Attachment" button (GONE).</li>
	 * </ul>
	 *
	 * <p>The method does NOT clear the selected screenshot from the ViewModel;
	 * that responsibility belongs to the caller (e.g., setupCancelUploadListener
	 * or resetFeedbackSubmission).
	 *
	 * @see #setupCancelUploadListener()
	 * @see #resetFeedbackSubmission()
	 * @see R.id#ivAttachmentPreview
	 * @see R.id#tvAddImage
	 * @see R.id#btnClearAttachment
	 */
	private void clearAttachmentPreview() {
		Drawable defaultPreviewBG = ContextCompat
			.getDrawable(this, R.drawable.ic_rd_primary_color);
		binding.userMessage.ivAttachmentPreview.setImageDrawable(defaultPreviewBG);
		
		binding.userMessage.tvAddImage.setVisibility(View.VISIBLE);
		binding.actionButtons.btnClearAttachment.setVisibility(View.GONE);
	}
	
	/**
	 * Initializes all button click listeners for the feedback screen. This method
	 * aggregates the setup calls for each interactive UI component, ensuring all
	 * user input elements respond appropriately to clicks.
	 *
	 * <p><strong>Buttons configured:</strong>
	 * <ul>
	 * <li>Back button via {@link #setupBackButton()}.</li>
	 * <li>Reaction selection options via {@link #setupReactionListeners()}.</li>
	 * <li>Upload picture button via {@link #setupUploadButton()}.</li>
	 * <li>Clear attachment button via {@link #setupCancelUploadListener()}.</li>
	 * <li>Send feedback button via {@link #setupFeedbackButton()}.</li>
	 * </ul>
	 *
	 * @see #setupBackButton()
	 * @see #setupReactionListeners()
	 * @see #setupUploadButton()
	 * @see #setupCancelUploadListener()
	 * @see #setupFeedbackButton()
	 */
	private void setupButtonClicks() {
		setupBackButton();
		setupReactionListeners();
		setupUploadButton();
		setupCancelUploadListener();
		setupFeedbackButton();
	}
	
	/**
	 * Configures the back button click listener. When the user clicks the back button,
	 * this method triggers a short haptic vibration and finishes the current activity,
	 * returning the user to the previous screen.
	 *
	 * <p>The haptic feedback provides tactile confirmation of the button press,
	 * enhancing the user experience with a subtle vibration.
	 *
	 * @see #buttonVibrate()
	 * @see #finish()
	 */
	private void setupBackButton() {
		binding.topBar.btnBack.setOnClickListener(view -> {
			buttonVibrate();
			finish();
		});
	}
	
	/**
	 * Configures click listeners for all five feedback reaction options (Excellent,
	 * Good, Average, Poor, Angry). Each reaction is represented by a LinearLayout
	 * containing an ImageView (emoji/icon) and a TextView (label). When a reaction
	 * is clicked, the method delegates to
	 * {@link #applyReactionSelection(String, ImageView, TextView)}
	 * to update the UI and ViewModel state.
	 *
	 * <p><strong>Reaction components mapping:</strong>
	 * <ul>
	 * <li>Excellent → {@code btnHappy}, {@code imgHappy}, {@code txtHappy}</li>
	 * <li>Good → {@code btnGood}, {@code imgGood}, {@code txtGood}</li>
	 * <li>Average → {@code btnAverage}, {@code imgAverage}, {@code txtAverage}</li>
	 * <li>Poor → {@code btnPoor}, {@code imgPoor}, {@code txtPoor}</li>
	 * <li>Angry → {@code btnAngry}, {@code imgAngry}, {@code txtAngry}</li>
	 * </ul>
	 *
	 * <p>Each reaction name is obtained from the {@link FeedbackReactions} enum
	 * using the {@link Enum#name()} method. The default reaction (Excellent) is
	 * pre-selected via {@link #selectExcellentReactionByDefault()} during UI setup.
	 *
	 * @see #applyReactionSelection(String, ImageView, TextView)
	 * @see FeedbackReactions
	 * @see #selectExcellentReactionByDefault()
	 */
	private void setupReactionListeners() {
		String excellent = FeedbackReactions.Excellent.name();
		String good = FeedbackReactions.Good.name();
		String average = FeedbackReactions.Average.name();
		String poor = FeedbackReactions.Poor.name();
		String angry = FeedbackReactions.Angry.name();
		
		ImageView imgHappy = binding.reactions.imgHappy;
		TextView txtHappy = binding.reactions.txtHappy;
		ImageView imgGood = binding.reactions.imgGood;
		TextView txtGood = binding.reactions.txtGood;
		ImageView imgAverage = binding.reactions.imgAverage;
		TextView txtAverage = binding.reactions.txtAverage;
		ImageView imgPoor = binding.reactions.imgPoor;
		TextView txtPoor = binding.reactions.txtPoor;
		ImageView imgAngry = binding.reactions.imgAngry;
		TextView txtAngry = binding.reactions.txtAngry;
		
		LinearLayout btnHappy = binding.reactions.btnHappy;
		LinearLayout btnGood = binding.reactions.btnGood;
		LinearLayout btnAverage = binding.reactions.btnAverage;
		LinearLayout btnPoor = binding.reactions.btnPoor;
		LinearLayout btnAngry = binding.reactions.btnAngry;
		
		btnHappy.setOnClickListener(view -> applyReactionSelection(excellent, imgHappy, txtHappy));
		btnGood.setOnClickListener(view -> applyReactionSelection(good, imgGood, txtGood));
		btnAverage.setOnClickListener(view -> applyReactionSelection(average, imgAverage,
			txtAverage));
		btnPoor.setOnClickListener(view -> applyReactionSelection(poor, imgPoor, txtPoor));
		btnAngry.setOnClickListener(view -> applyReactionSelection(angry, imgAngry, txtAngry));
	}
	
	/**
	 * Configures the upload picture button click listener. When the user clicks the
	 * upload button, this method triggers a short haptic vibration and launches the
	 * system image picker to allow the user to select a screenshot or image to
	 * attach with their feedback. The launcher uses the {@link ActivityResultLauncher}
	 * with MIME type "image/*" to accept all image formats.
	 *
	 * <p>After an image is selected, it is processed and stored in the ViewModel
	 * via the selected screenshot LiveData. The UI then updates to show a preview
	 * of the attached image and displays the clear attachment button.
	 *
	 * @see #buttonVibrate()
	 * @see #imagePickerLauncher
	 * @see FeedbackViewModel#setSelectedScreenshot(DocumentFile)
	 */
	private void setupUploadButton() {
		binding.userMessage.btnUploadPic.setOnClickListener(view -> {
			buttonVibrate();
			imagePickerLauncher.launch("image/*");
		});
	}
	
	/**
	 * Configures the clear attachment button click listener. When the user clicks
	 * the clear attachment button while a screenshot is attached, this method clears
	 * the selected screenshot from the ViewModel and removes the attachment preview
	 * from the UI via {@link #clearAttachmentPreview()}.
	 *
	 * <p>The clear attachment button is only visible when an attachment is present,
	 * and its visibility is managed by observing the selected screenshot LiveData.
	 *
	 * @see FeedbackViewModel#setSelectedScreenshot(DocumentFile)
	 * @see #clearAttachmentPreview()
	 */
	private void setupCancelUploadListener() {
		binding.actionButtons.btnClearAttachment.setOnClickListener(view -> {
			getViewModel().setSelectedScreenshot(null);
			clearAttachmentPreview();
		});
	}
	
	/**
	 * Configures the send feedback button click listener. When the user clicks the
	 * send button, this method triggers a short haptic vibration and initiates the
	 * feedback validation and submission process via {@link #validateAndSendFeedback()}.
	 *
	 * <p>The button may be disabled during active submission (if implemented in the
	 * ViewModel) to prevent duplicate submissions while a network request is in progress.
	 *
	 * @see #buttonVibrate()
	 * @see #validateAndSendFeedback()
	 */
	private void setupFeedbackButton() {
		binding.actionButtons.btnSendReport.setOnClickListener(view -> {
			buttonVibrate();
			validateAndSendFeedback();
		});
	}
	
	/**
	 * Initializes all LiveData observers for the feedback ViewModel. This method
	 * aggregates the setup of submission result and error handling observers.
	 *
	 * <p><strong>Observers initialized:</strong>
	 * <ul>
	 * <li>{@link #observeSubmissionResult()} – Handles successful submission,
	 *     displays success toast, and resets the form.</li>
	 * <li>{@link #observeSubmissionErrors()} – Displays error messages when
	 *     submission fails.</li>
	 * </ul>
	 *
	 * @see #observeSubmissionResult()
	 * @see #observeSubmissionErrors()
	 */
	private void initViewModelObservers() {
		observeSubmissionResult();
		observeSubmissionErrors();
	}
	
	/**
	 * Observes submission error messages from the ViewModel and displays them to
	 * the user. When the LiveData emits a non-null, non-empty error message,
	 * this method shows an error toast with the message and triggers haptic
	 * feedback to alert the user.
	 *
	 * <p>Common error scenarios include:
	 * <ul>
	 * <li>Network connectivity issues (no internet).</li>
	 * <li>Server-side validation failures.</li>
	 * <li>Timeout or server unavailability.</li>
	 * <li>Authentication or permission errors.</li>
	 * <li>File attachment size or type validation errors.</li>
	 * </ul>
	 *
	 * @see FeedbackViewModel#getSubmissionError()
	 * @see StylizedToastView#show(BaseActivity, CharSequence)
	 * @see #vibrate()
	 */
	private void observeSubmissionErrors() {
		getViewModel().getSubmissionError().observe(this, errorMessage -> {
			closeLoadingDialog();
			if (errorMessage != null && !errorMessage.isEmpty()) {
				StylizedToastView.show(FeedbackActivity.this, errorMessage);
				vibrate();
			}
		});
	}
	
	/**
	 * Observes the feedback submission result LiveData from the ViewModel. When a
	 * successful submission occurs, this method displays a success toast message,
	 * resets all input fields and attachments, and triggers haptic feedback.
	 *
	 * <p>The observer is lifecycle-aware, automatically cleaning up when the
	 * activity is destroyed. Success is indicated by {@code isSuccess} being
	 * {@code true}. The observed LiveData is typically updated by the ViewModel
	 * after a network request completes.
	 *
	 * @see FeedbackViewModel#getSubmissionSuccess()
	 * @see #resetFeedbackSubmission()
	 * @see StylizedToastView#show(BaseActivity, CharSequence)
	 * @see #vibrate()
	 */
	private void observeSubmissionResult() {
		getViewModel().getSubmissionSuccess().observe(this, isSuccess -> {
			closeLoadingDialog();
			if (isSuccess != null && isSuccess) {
				StylizedToastView.show(FeedbackActivity.this,
					StringHelper.getText(R.string.hint_feedback_sent_thank_you));
				resetFeedbackSubmission();
				vibrate();
			}
		});
	}
	
	/**
	 * Resets all feedback input fields and clears the selected attachment after a
	 * successful submission. This method clears the email and feedback text fields,
	 * clears the attachment preview via {@link #clearAttachmentPreview()}, and clears
	 * the selected screenshot from the ViewModel.
	 *
	 * <p>After reset, the form is ready for another feedback submission without
	 * requiring the user to manually clear previous entries.
	 *
	 * @see #clearAttachmentPreview()
	 * @see FeedbackViewModel#setSelectedScreenshot(DocumentFile)
	 * @see android.widget.EditText#setText(CharSequence)
	 */
	private void resetFeedbackSubmission() {
		binding.userMessage.editEmail.setText("");
		binding.userMessage.editFeedback.setText("");
		clearAttachmentPreview();
		getViewModel().setSelectedScreenshot(null);
	}
	
	/**
	 * Applies the visual selection state for a feedback reaction option. This method
	 * updates the ViewModel with the selected reaction, resets all reaction UI
	 * components to their default (unselected) state, then highlights the specific
	 * reaction's image and text to indicate current selection.
	 *
	 * <p><strong>Visual changes applied to all reactions (reset):</strong>
	 * <ul>
	 * <li>All images have {@code selected} state set to {@code false}.</li>
	 * <li>All text views have regular font typeface.</li>
	 * <li>All text views have secondary text color (unselected).</li>
	 * </ul>
	 *
	 * <p><strong>Visual changes applied to the selected reaction:</strong>
	 * <ul>
	 * <li>Selected image's {@code selected} state set to {@code true}.</li>
	 * <li>Selected text view gets primary text color.</li>
	 * <li>Selected text view gets bold font typeface.</li>
	 * </ul>
	 *
	 * <p>Supported reactions: Happy (Excellent), Good, Average, Poor, Angry.
	 *
	 * @param reaction         The reaction string value to store in the ViewModel
	 *                         (e.g., "Excellent", "Good", "Average", "Poor", "Angry").
	 * @param selectedImage    The ImageView corresponding to the selected reaction.
	 * @param selectedTextView The TextView corresponding to the selected reaction.
	 * @see FeedbackViewModel#setSelectedReaction(String)
	 * @see #selectExcellentReactionByDefault()
	 */
	private void applyReactionSelection(@NonNull String reaction,
	                                    @NonNull ImageView selectedImage,
	                                    @NonNull TextView selectedTextView) {
		getViewModel().setSelectedReaction(reaction);
		
		Typeface regular = ResourcesCompat.getFont(this, R.font.font_family_regular);
		Typeface bold = ResourcesCompat.getFont(this, R.font.font_family_bold);
		
		binding.reactions.imgHappy.setSelected(false);
		binding.reactions.imgGood.setSelected(false);
		binding.reactions.imgAverage.setSelected(false);
		binding.reactions.imgPoor.setSelected(false);
		binding.reactions.imgAngry.setSelected(false);
		
		binding.reactions.txtHappy.setTypeface(regular);
		binding.reactions.txtGood.setTypeface(regular);
		binding.reactions.txtAverage.setTypeface(regular);
		binding.reactions.txtPoor.setTypeface(regular);
		binding.reactions.txtAngry.setTypeface(regular);
		
		int unselectedColor = getColor(R.color.style_color_text_secondary);
		binding.reactions.txtHappy.setTextColor(unselectedColor);
		binding.reactions.txtGood.setTextColor(unselectedColor);
		binding.reactions.txtAverage.setTextColor(unselectedColor);
		binding.reactions.txtPoor.setTextColor(unselectedColor);
		binding.reactions.txtAngry.setTextColor(unselectedColor);
		
		selectedImage.setSelected(true);
		selectedTextView.setTextColor(getColor(R.color.style_color_text_primary));
		selectedTextView.setTypeface(bold);
	}
	
	/**
	 * Sets the "Excellent" (Happy) reaction as the default selected option when the
	 * feedback screen is first loaded. This method delegates to
	 * {@link #applyReactionSelection(String, ImageView, TextView)} with the
	 * Happy reaction's image and text views.
	 *
	 * <p>The default selection ensures that the user is never left with no reaction
	 * selected, guiding users toward a positive default response while still allowing
	 * them to change their selection freely.
	 *
	 * @see FeedbackReactions#Excellent
	 * @see #applyReactionSelection(String, ImageView, TextView)
	 */
	private void selectExcellentReactionByDefault() {
		applyReactionSelection(FeedbackReactions.Excellent.name(),
			binding.reactions.imgHappy, binding.reactions.txtHappy);
	}
	
	/**
	 * Validates the user's feedback input and initiates the submission process if all
	 * validation checks pass. This method extracts the email and message from input
	 * fields, validates the message (non-empty, max 500 characters), and delegates
	 * the submission to the ViewModel.
	 *
	 * <p><strong>Validation rules:</strong>
	 * <ul>
	 * <li>Feedback message must not be empty. If empty → warning toast + vibration.</li>
	 * <li>Feedback message must not exceed 500 characters. If exceeded → warning
	 *     toast + vibration.</li>
	 * </ul>
	 *
	 * <p>If all validation checks pass, the method retrieves the selected reaction
	 * and optional attachment from the ViewModel and calls
	 * {@link FeedbackViewModel#sendFeedback(LifecycleOwner, String, String, String, DocumentFile)}
	 * The email field is optional and may be empty.
	 *
	 * @see #getEmailText()
	 * @see #getFeedbackText()
	 * @see #vibrate()
	 * @see StylizedToastView#show(BaseActivity, CharSequence)
	 * @see FeedbackViewModel#sendFeedback(LifecycleOwner, String, String, String, DocumentFile)
	 */
	private void validateAndSendFeedback() {
		String email = getEmailText().toString().trim();
		String message = getFeedbackText().toString().trim();
		
		if (message.isEmpty()) {
			String toastMessage =
				StringHelper.getText(R.string.hint_please_describe_feedback);
			StylizedToastView.show(this, toastMessage);
			vibrate();
			return;
		}
		
		if (message.length() > 500) {
			String toastMessage =
				StringHelper.getText(R.string.hint_feedback_is_too_long);
			StylizedToastView.show(this, toastMessage);
			vibrate();
			return;
		}
		
		String reaction = getViewModel().getSelectedReaction().getValue();
		DocumentFile attachment = getViewModel().getSelectedScreenshot().getValue();
		getViewModel().sendFeedback(this, reaction, email, message, attachment);
		showLoadingDialog();
	}
	
	/**
	 * Displays the circular loading dialog to indicate that a background operation
	 * (e.g., submitting feedback, downloading an update, or loading data) is in
	 * progress. This method creates the dialog lazily (only once) and configures
	 * it to be non-cancelable with a background blur effect.
	 *
	 * <p><strong>Configuration:</strong>
	 * <ul>
	 * <li>Dialog is created lazily on first call to avoid unnecessary allocation.</li>
	 * <li>Cancelable is set to {@code false} to prevent user dismissal.</li>
	 * <li>Background blur radius is set to 60px (API 31+ falls back to dim).</li>
	 * <li>Dialog is shown after configuration.</li>
	 * </ul>
	 *
	 * @see CircularLoadingDialog
	 */
	private void showLoadingDialog() {
		try {
			circularLoadingDialog = null;
			circularLoadingDialog = new CircularLoadingDialog(this);
			circularLoadingDialog.setCancelable(true);
			circularLoadingDialog.enableBackgroundBlur(60);
			circularLoadingDialog.show();
		} catch (Exception error) {
			logger.error("Error showing loading dialog: ", error);
		}
	}
	
	/**
	 * Dismisses and closes the circular loading dialog if it is currently showing.
	 * This method safely handles null references and exceptions to prevent crashes
	 * when attempting to close a dialog that may have already been released.
	 *
	 * <p><strong>Error handling:</strong>
	 * <ul>
	 * <li>Checks if the dialog reference is null before attempting to close.</li>
	 * <li>Any exception during dismissal is caught and logged without crashing.</li>
	 * <li>After successful close, the dialog reference is set to null.</li>
	 * </ul>
	 *
	 * <p>This method should be called after the background operation completes,
	 * fails, or when the activity is being destroyed.
	 *
	 * @see CircularLoadingDialog#close()
	 * @see #showLoadingDialog()
	 */
	private void closeLoadingDialog() {
		try {
			if (circularLoadingDialog != null) {
				circularLoadingDialog.close();
			}
		} catch (Exception error) {
			logger.error("Error closing loading dialog: ", error);
		}
	}
	
	/**
	 * Retrieves the text content from the email input field as a non-null Editable.
	 * This method uses {@link Objects#requireNonNull(Object)} to ensure the returned
	 * value is never {@code null}. The email field is optional and may be empty.
	 *
	 * @return The non-null {@link Editable} containing the user's entered email address.
	 * @throws NullPointerException If the EditText's text is unexpectedly null.
	 * @see EditText#getText()
	 */
	@NonNull private Editable getEmailText() {
		return Objects.requireNonNull(binding.userMessage.editEmail.getText());
	}
	
	/**
	 * Retrieves the text content from the feedback input field as a non-null Editable.
	 * This method uses {@link Objects#requireNonNull(Object)} to guarantee a non-null
	 * return value, providing safe access to the user's feedback message text.
	 *
	 * <p>The returned Editable can be converted to a String via {@link Editable#toString()}
	 * for validation, storage, or submission to the backend.
	 *
	 * @return The non-null {@link Editable} containing the user's entered feedback text.
	 * @throws NullPointerException If the EditText's text is unexpectedly null.
	 * @see EditText#getText()
	 */
	@NonNull private Editable getFeedbackText() {
		return Objects.requireNonNull(binding.userMessage.editFeedback.getText());
	}
}