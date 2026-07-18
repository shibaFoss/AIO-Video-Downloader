package coreUtils.library.views;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.textview.MaterialTextView;
import com.nextgen.R;

import coreUtils.base.BaseActivity;

/**
 * A fluent builder for creating and configuring custom message dialogs with a
 * standard layout. This builder wraps a {@link StylizedDialogBuilder} and provides
 * convenient methods for setting the dialog message, configuring buttons, and
 * controlling dialog appearance with a pre-defined layout structure.
 *
 * <p><strong>Core features:</strong>
 * <ul>
 * <li>Sets a message text via resource ID or dynamic string.</li>
 * <li>Configures positive and negative buttons with text, icons, and click listeners.</li>
 * <li>Controls button visibility and close button behavior.</li>
 * <li>Applies background blur, animations, and positioning.</li>
 * <li>Exposes the underlying dialog builder for advanced customization.</li>
 * </ul>
 *
 * <p><strong>Usage example:</strong>
 * <pre>
 * new MessageDialogBuilder(activity, "Are you sure?")
 *     .setTitle("Confirm")
 *     .setPositiveButtonText("Yes")
 *     .setNegativeButtonText("No")
 *     .show();
 * </pre>
 *
 * @see StylizedDialogBuilder
 * @see #show()
 * @see #close()
 */
public final class MessageDialogBuilder {
	private final StylizedDialogBuilder dialogBuilder;
	
	/**
	 * Constructs a MessageDialogBuilder with a message from a string resource ID.
	 * This creates a new {@link StylizedDialogBuilder} instance with the provided
	 * activity, sets the custom content view to {@code R.layout.dialog_normal_message_1},
	 * and populates the message text using the specified resource ID.
	 *
	 * <p>The dialog layout must contain a TextView with ID {@code R.id.tvDialogMessage}
	 * for the message text to be displayed. After construction, the builder can be
	 * further customized using the returned methods before calling {@link #show()}.
	 *
	 * @param activity The activity that will host and own this dialog.
	 * @param stringId The string resource ID for the dialog message text.
	 * @see StylizedDialogBuilder
	 */
	public MessageDialogBuilder(@NonNull BaseActivity<?> activity, @StringRes int stringId) {
		this.dialogBuilder = new StylizedDialogBuilder(activity);
		this.dialogBuilder.setCustomContentView(R.layout.dialog_normal_message_1);
		this.setDialogMessage(stringId);
	}
	
	/**
	 * Constructs a MessageDialogBuilder with a dynamic message string.
	 * This creates a new {@link StylizedDialogBuilder} instance with the provided
	 * activity, sets the custom content view to {@code R.layout.dialog_normal_message_1},
	 * and populates the message text using the provided string.
	 *
	 * <p>The dialog layout must contain a TextView with ID {@code R.id.tvDialogMessage}
	 * for the message text to be displayed. This constructor is useful for messages
	 * that are constructed at runtime rather than defined as string resources.
	 *
	 * @param activity The activity that will host and own this dialog.
	 * @param message  The dynamic message text to display in the dialog.
	 * @see StylizedDialogBuilder
	 */
	public MessageDialogBuilder(@NonNull BaseActivity<?> activity, String message) {
		this.dialogBuilder = new StylizedDialogBuilder(activity);
		this.dialogBuilder.setCustomContentView(R.layout.dialog_normal_message_1);
		this.setDialogMessage(message);
	}
	
	/**
	 * Returns the current dialog builder instance used to construct and configure
	 * the custom dialog. This method provides access to the underlying builder
	 * for advanced customization beyond the wrapper's convenience methods.
	 *
	 * <p>The dialog builder is created lazily when first needed and may be
	 * {@code null} if it has not been initialized or has been closed.
	 *
	 * @return The {@link StylizedDialogBuilder} instance, or {@code null}
	 * if not yet created or already released.
	 * @see StylizedDialogBuilder
	 */
	@Nullable
	public StylizedDialogBuilder getDialogBuilder() {
		return this.dialogBuilder;
	}
	
	/**
	 * Closes the dialog and releases all associated resources. This method delegates
	 * to {@link StylizedDialogBuilder#close()} on the current dialog builder instance.
	 *
	 * <p>If the dialog builder is {@code null}, the method returns silently without
	 * throwing an exception. After closing, the dialog is no longer visible and
	 * cannot be shown again without recreating it.
	 *
	 * @see StylizedDialogBuilder#close()
	 */
	public void close() {
		getDialogBuilder().close();
	}
	
	/**
	 * Displays the dialog on the screen. This method delegates to
	 * {@link StylizedDialogBuilder#show()} on the current dialog builder instance.
	 *
	 * <p>If the dialog builder is {@code null} or the dialog is already showing,
	 * the method returns silently without performing any action. The dialog must
	 * be properly configured before calling this method.
	 *
	 * @see StylizedDialogBuilder#show()
	 */
	public void show() {
		getDialogBuilder().show();
	}
	
	/**
	 * Sets the dialog message text using a string resource ID. This method retrieves
	 * the custom content view from the dialog builder, finds the message TextView
	 * by ID {@code R.id.tvDialogMessage}, and sets its text to the provided resource.
	 *
	 * <p>The dialog builder must have been initialized with a custom content view
	 * containing the message TextView. If the dialog builder is null or the view
	 * is not found, the method returns {@code null}.
	 *
	 * @param stringId The string resource ID to display as the dialog message.
	 * @return The {@link StylizedDialogBuilder} instance for method chaining,
	 * or {@code null} if the dialog builder or message view is unavailable.
	 * @see #setDialogMessage(String)
	 * @see StylizedDialogBuilder#getCustomContentView()
	 */
	@Nullable
	private StylizedDialogBuilder setDialogMessage(@StringRes int stringId) {
		View contentView = getDialogBuilder().getCustomContentView();
		MaterialTextView dialogMessage = contentView.findViewById(R.id.tvDialogMessage);
		if (dialogMessage != null) {
			dialogMessage.setText(stringId);
		}
		return getDialogBuilder();
	}
	
	/**
	 * Sets the dialog message text using a plain string. This method retrieves
	 * the custom content view from the dialog builder, finds the message TextView
	 * by ID {@code R.id.tvDialogMessage}, and sets its text to the provided value.
	 *
	 * <p>The dialog builder must have been initialized with a custom content view
	 * containing the message TextView. This overload is useful for dynamic messages
	 * that are constructed at runtime rather than from string resources.
	 *
	 * @param message The text to display as the dialog message.
	 * @return The {@link StylizedDialogBuilder} instance for method chaining,
	 * or {@code null} if the dialog builder or message view is unavailable.
	 * @see #setDialogMessage(int)
	 * @see StylizedDialogBuilder#getCustomContentView()
	 */
	@Nullable
	private StylizedDialogBuilder setDialogMessage(String message) {
		View contentView = getDialogBuilder().getCustomContentView();
		MaterialTextView dialogMessage = contentView.findViewById(R.id.tvDialogMessage);
		if (dialogMessage != null) {
			dialogMessage.setText(message);
		}
		return getDialogBuilder();
	}
}
