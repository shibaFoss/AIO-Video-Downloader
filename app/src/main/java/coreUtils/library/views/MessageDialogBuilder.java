package coreUtils.library.views;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.textview.MaterialTextView;
import com.nextgen.R;

import coreUtils.base.BaseActivity;

public final class MessageDialogBuilder extends StylizedDialogBuilder {
	
	/**
	 * Constructs a new StylizedDialogBuilder instance for the given activity.
	 * The builder creates a weak reference to the activity to prevent memory
	 * leaks when the dialog outlives the activity lifecycle. Upon construction,
	 * the base dialog layout is initialized via {@link #initializeBaseLayout()}.
	 *
	 * <p>The builder pattern allows fluent configuration of dialog properties
	 * such as title, buttons, animations, margins, and custom content views
	 * before the dialog is shown via {@link #show()}.
	 *
	 * <p><strong>Usage example:</strong>
	 * <pre>
	 * new StylizedDialogBuilder(activity)
	 *     .setDialogTitle("Confirm")
	 *     .setPositiveButtonText("OK")
	 *     .setNegativeButtonText("Cancel")
	 *     .show();
	 * </pre>
	 *
	 * @param activity The activity that will host and own this dialog. Must not be
	 *                 null and must be a valid {@link BaseActivity} instance.
	 * @see #show()
	 * @see #close()
	 * @see #initializeBaseLayout()
	 */
	public MessageDialogBuilder(@NonNull BaseActivity<?> activity) {
		super(activity);
		setCustomContentView(R.layout.dialog_normal_message_1);
	}
	
	public StylizedDialogBuilder setDialogMessage(@StringRes int stringId) {
		View contentView = getCustomContentView();
		MaterialTextView dialogMessage = contentView.findViewById(R.id.tvDialogMessage);
		if (dialogMessage != null) {
			dialogMessage.setText(stringId);
		}
		return this;
	}
	
	public StylizedDialogBuilder setDialogMessage(String message) {
		View contentView = getCustomContentView();
		MaterialTextView dialogMessage = contentView.findViewById(R.id.tvDialogMessage);
		if (dialogMessage != null) {
			dialogMessage.setText(message);
		}
		return this;
	}
}
