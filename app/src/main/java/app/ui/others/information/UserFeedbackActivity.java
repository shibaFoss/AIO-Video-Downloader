package app.ui.others.information;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static lib.device.DeviceInfoUtils.getDeviceInformation;
import static lib.process.CommonTimeUtils.delay;
import static lib.ui.MsgDialogUtils.showMessageDialog;
import static lib.ui.builders.ToastView.showToast;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.content.res.AppCompatResources;

import com.aio.R;

import app.core.AIOApp;
import app.core.bases.BaseActivity;
import app.ui.main.MotherActivity;
import lib.device.ShareUtility;
import lib.ui.builders.DialogBuilder;

/**
 * Activity to collect user feedback about issues in the app.
 * Users can report problems such as app crashes, no sound in videos, etc.
 * The activity also collects user email and custom messages.
 */
public class UserFeedbackActivity extends BaseActivity {

    public static final String WHERE_DIS_YOU_COME_FROM = "WHERE_DIS_YOU_COME_FROM";
    public static final int FROM_CRASH_HANDLER = 12;

    private View buttonSendMessage;
    private EditText editEmailField, editMessageField;
    private View buttonBack;
    private CheckBox checkBoxNoSound, checkBoxIncompleteVideo,
            checkBoxAppCrashed, checkBoxGlitchedVideos,
            checkBoxShowingManyAds, checkBoxOtherProblem;

    /**
     * Returns the layout resource for this activity.
     */
    @Override
    public int onRenderingLayout() {
        return R.layout.activity_feedback_1;
    }

    /**
     * Called after the layout is rendered.
     * Sets up the UI, event listeners, and handles incoming intent data.
     */
    @Override
    public void onAfterLayoutRender() {
        setLightSystemBarTheme();
        initializeViews();
        initializeViewClickEvents();
        handleIntentExtra();
    }

    /**
     * Called when the back button is pressed.
     * Closes the activity with animation.
     */
    @Override
    public void onBackPressActivity() {
        closeActivityWithSwipeAnimation(false);
    }

    /**
     * Binds the XML views with their respective objects in Java.
     */
    private void initializeViews() {
        buttonBack = findViewById(R.id.btn_left_actionbar);
        buttonSendMessage = findViewById(R.id.btn_right_actionbar);
        editEmailField = findViewById(R.id.edit_email_field);
        editMessageField = findViewById(R.id.edit_message_field);
        checkBoxNoSound = findViewById(R.id.checkbox_no_sound);
        checkBoxIncompleteVideo = findViewById(R.id.checkbox_incomplete_video);
        checkBoxAppCrashed = findViewById(R.id.checkbox_app_crashed);
        checkBoxGlitchedVideos = findViewById(R.id.checkbox_glitch_in_video);
        checkBoxShowingManyAds = findViewById(R.id.checkbox_showing_too_many_ads);
        checkBoxOtherProblem = findViewById(R.id.checkbox_other_problem);
    }

    /**
     * Retrieves the user's message from the input field.
     */
    private String getUserMessage() {
        return editMessageField.getText().toString();
    }

    /**
     * Retrieves the user's email from the input field.
     */
    private String getUserEmail() {
        return editEmailField.getText().toString();
    }

    /**
     * Validates if both message and email fields are filled.
     */
    private boolean isUserMessageValid() {
        return !getUserMessage().isEmpty() && !getUserEmail().isEmpty();
    }

    /**
     * Initializes click listeners for the back and send message buttons.
     */
    private void initializeViewClickEvents() {
        buttonBack.setOnClickListener(view -> onBackPressActivity());

        buttonSendMessage.setOnClickListener(view -> {
            String messageToSend = generateMessage();
            if (messageToSend.isEmpty()) {
                doSomeVibration(50);
                showToast(getString(R.string.text_enter_your_email_message_first), -1);
                return;
            }

            ShareUtility.shareText(AIOApp.INSTANCE, messageToSend,
                    getText(R.string.title_share_feedback).toString(), () -> {
                        showToast(getString(R.string.text_feedbacks_sent_successfully), -1);
                        return null;
                    });

            resetFormFields();
        });
    }

    /**
     * Resets all fields to their default state after feedback is submitted.
     */
    private void resetFormFields() {
        editEmailField.setText("");
        editMessageField.setText("");
        checkBoxNoSound.setChecked(false);
        checkBoxIncompleteVideo.setChecked(false);
        checkBoxAppCrashed.setChecked(false);
        checkBoxGlitchedVideos.setChecked(false);
        checkBoxShowingManyAds.setChecked(false);
        checkBoxOtherProblem.setChecked(false);
    }

    /**
     * Constructs the feedback message including selected checkboxes and device info.
     */
    private String generateMessage() {
        StringBuilder msgBuilder = new StringBuilder();
        String userMessage = getUserMessage();
        String userEmail = getUserEmail();

        if (isUserMessageValid()) {
            msgBuilder.append(getString(R.string.text_user_message))
                    .append(userMessage).append("\n\n");
            msgBuilder.append(getString(R.string.text_user_email))
                    .append(userEmail).append("\n\n");
        } else return "";

        // Append selected issues
        msgBuilder.append(getString(R.string.text_issues_reported));
        if (checkBoxNoSound.isChecked())
            msgBuilder.append(getString(R.string.text_no_sound_in_videos));
        if (checkBoxIncompleteVideo.isChecked())
            msgBuilder.append(getString(R.string.text_videos_are_incomplete));
        if (checkBoxAppCrashed.isChecked())
            msgBuilder.append(getString(R.string.text_app_crashed_during_use));
        if (checkBoxGlitchedVideos.isChecked())
            msgBuilder.append(getString(R.string.text_videos_are_glitching));
        if (checkBoxShowingManyAds.isChecked())
            msgBuilder.append(getString(R.string.text_too_many_ads_are_being_shown));
        if (checkBoxOtherProblem.isChecked())
            msgBuilder.append(getString(R.string.text_other_problems_experienced));

        // No specific issues
        if (!checkBoxNoSound.isChecked() && !checkBoxIncompleteVideo.isChecked() &&
                !checkBoxAppCrashed.isChecked() && !checkBoxGlitchedVideos.isChecked() &&
                !checkBoxShowingManyAds.isChecked() && !checkBoxOtherProblem.isChecked()) {
            msgBuilder.append(getString(R.string.text_no_specific_issues_reported));
        }

        // Append device information
        String deviceInfo = getDeviceInformation(this);
        msgBuilder.append(getString(R.string.text_device_information)).append(deviceInfo);
        return msgBuilder.toString();
    }

    /**
     * Handles incoming intent to auto-check app crash checkbox if coming from crash handler.
     */
    private void handleIntentExtra() {
        Intent intent = getIntent();
        if (intent.getIntExtra(WHERE_DIS_YOU_COME_FROM, -1) == FROM_CRASH_HANDLER) {
            checkBoxAppCrashed.setChecked(true);

            DialogBuilder dialogBuilder = showMessageDialog(
                    this, false, true,
                    getString(R.string.text_oops_app_was_crashed),
                    getString(R.string.text_app_crash_feedback_message),
                    getString(R.string.title_send_feedback),
                    getString(R.string.title_cancel),
                    false, null, null,
                    messageTextView -> {
                        messageTextView.setText(R.string.text_app_crash_feedback_message);
                        return null;
                    },
                    null, null,
                    positiveButtonText -> {
                        if (getActivity() == null) return null;
                        Drawable drawable = AppCompatResources.getDrawable(
                                getActivity(), R.drawable.ic_button_actionbar_send);
                        if (drawable != null)
                            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                        positiveButtonText.setCompoundDrawables(drawable, null, null, null);
                        return null;
                    },
                    null, null, null
            );

            if (dialogBuilder == null) return;

            dialogBuilder.setOnClickForPositiveButton(view -> {
                dialogBuilder.close();
                showToast(null, R.string.text_feedbacks_sent_successfully);

                delay(200, () -> {
                    try {
                        Intent activityIntent = new Intent(getActivity(), MotherActivity.class);
                        int flags = FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP;
                        activityIntent.setFlags(flags);
                        startActivity(activityIntent);
                        finish();
                    } catch (Exception error) {
                        error.printStackTrace();
                    }
                });
            });
        }
    }
}