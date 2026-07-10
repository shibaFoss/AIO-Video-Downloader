package coreUtils.library.views;

import static android.view.LayoutInflater.from;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.nextgen.R;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

/**
 * A fluent builder for creating and configuring custom-styled dialogs with
 * consistent Material Design aesthetics. This builder provides chainable methods
 * for setting dialog properties including title, positive/negative buttons,
 * custom content views, animations, margins, background blur, and positioning.
 *
 * <p><strong>Core features:</strong>
 * <ul>
 * <li>Fluent API with method chaining for convenient dialog configuration.</li>
 * <li>Custom title and button text (supports string resources and raw strings).</li>
 * <li>Optional custom content view via layout resource or existing View.</li>
 * <li>Slide-up or fade-in animations for dialog appearance.</li>
 * <li>Background blur (API 31+) with dim fallback for older platforms.</li>
 * <li>Bottom positioning for bottom sheets and action dialogs.</li>
 * <li>Configurable margins and title visibility.</li>
 * <li>Automatic resource cleanup via {@link #close()}.</li>
 * </ul>
 *
 * <p>Usage: Create builder with activity, configure properties, then call {@link #show()}.
 *
 * @see BaseActivity
 * @see #show()
 * @see #close()
 */
public final class StylizedDialogBuilder {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final WeakReference<BaseActivity<?>> weakActivityRef;
	
	private AlertDialog alertDialog;
	private View dialogRootView;
	private WeakReference<LinearLayout> weakMainContentRef;
	private WeakReference<View> weakCustomChildViewRef;
	
	private int dialogMarginPx = -1;
	
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
	public StylizedDialogBuilder(@NonNull BaseActivity<?> activity) {
		this.weakActivityRef = new WeakReference<>(activity);
		initializeBaseLayout();
	}
	
	/**
	 * Initializes the base dialog layout, window properties, and default behavior.
	 * This method inflates the stylized dialog layout, creates the AlertDialog
	 * instance, sets up the close button click listener, configures default margins,
	 * and applies default animations and cancellability.
	 *
	 * <p><strong>Initialization steps:</strong>
	 * <ol>
	 * <li>Validates that the activity reference is still valid (not finishing/destroyed).</li>
	 * <li>Inflates {@code R.layout.dialog_stylized_window_0} into the content root.</li>
	 * <li>Stores a weak reference to the main content container.</li>
	 * <li>Creates an AlertDialog with custom styling ({@code R.style.style_dialog_window}).</li>
	 * <li>Sets a dismiss listener to automatically call {@link #close()}.</li>
	 * <li>Configures the close button to dismiss the dialog when clicked.</li>
	 * <li>Sets default margin (10dp) and applies slide-up animation.</li>
	 * <li>Makes the dialog cancelable by default.</li>
	 * </ol>
	 *
	 * @throws IllegalStateException If the activity reference is invalid or null.
	 */
	private void initializeBaseLayout() {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
		
		ViewGroup root = activity.findViewById(android.R.id.content);
		dialogRootView = from(activity).inflate(R.layout.dialog_stylized_window_0, root, false);
		LinearLayout mainContent = dialogRootView.findViewById(R.id.mainContent);
		this.weakMainContentRef = new WeakReference<>(mainContent);
		
		int styledResId = R.style.style_dialog_window;
		AlertDialog.Builder nativeBuilder = new AlertDialog.Builder(activity, styledResId);
		nativeBuilder.setView(dialogRootView);
		this.alertDialog = nativeBuilder.create();
		
		this.alertDialog.setOnDismissListener(dialogInterface -> close());
		View btnClose = dialogRootView.findViewById(R.id.btnCloseDialog);
		if (btnClose != null) {
			btnClose.setOnClickListener(v -> close());
		}
		
		dialogMarginPx = activity.getResources().getDimensionPixelSize(R.dimen._10);
		setPositiveButtonIcon(null);
		enableSlideUpAnimation();
		setCancelable(true);
	}
	
	/**
	 * Sets a custom content view for the dialog using a layout resource ID. The
	 * provided layout is inflated and added to the dialog's main content container,
	 * replacing any previously set custom content view. The child view reference
	 * is stored in a {@link WeakReference} to prevent memory leaks.
	 *
	 * @param childLayoutResId The layout resource ID to inflate as custom content.
	 * @return This builder instance for method chaining.
	 * @throws IllegalStateException If the activity or content container is null.
	 */
	@NonNull
	public StylizedDialogBuilder setCustomContentView(@LayoutRes int childLayoutResId) {
		BaseActivity<?> activity = weakActivityRef.get();
		LinearLayout container = weakMainContentRef != null ? weakMainContentRef.get() : null;
		
		if (activity != null && container != null) {
			container.removeAllViews();
			View childView = from(activity).inflate(childLayoutResId, container, true);
			this.weakCustomChildViewRef = new WeakReference<>(childView);
		}
		return this;
	}
	
	/**
	 * Sets a custom content view for the dialog using an existing {@link View}
	 * instance. The provided view is added to the dialog's main content container,
	 * replacing any previously set custom content view. The child view reference
	 * is stored in a {@link WeakReference} to prevent memory leaks.
	 *
	 * <p>This method is useful when the custom content view is already inflated
	 * or programmatically constructed, avoiding redundant inflation.
	 *
	 * @param childView The view to use as custom content. Must not be null.
	 * @return This builder instance for method chaining.
	 * @throws IllegalStateException If the content container is null.
	 */
	@NonNull
	public StylizedDialogBuilder setCustomContentView(@NonNull View childView) {
		LinearLayout container = weakMainContentRef != null ? weakMainContentRef.get() : null;
		if (container != null) {
			container.removeAllViews();
			container.addView(childView);
			this.weakCustomChildViewRef = new WeakReference<>(childView);
		}
		return this;
	}
	
	/**
	 * Sets whether the dialog is cancelable by user actions. When cancelable is
	 * {@code true}, the dialog can be dismissed by pressing the back button or
	 * tapping outside the dialog area. When {@code false}, the dialog requires
	 * explicit user interaction with the provided buttons to be dismissed.
	 *
	 * <p>Both {@link AlertDialog#setCancelable(boolean)} and
	 * {@link AlertDialog#setCanceledOnTouchOutside(boolean)} are set to the same
	 * value to ensure consistent behavior.
	 *
	 * @param cancelable {@code true} to allow cancellation, {@code false} to require
	 *                   explicit button action.
	 * @return This builder instance for method chaining.
	 */
	@NonNull
	public StylizedDialogBuilder setCancelable(boolean cancelable) {
		if (alertDialog != null) {
			alertDialog.setCancelable(cancelable);
			alertDialog.setCanceledOnTouchOutside(cancelable);
		}
		return this;
	}
	
	/**
	 * Sets the dialog title using a plain string. This method finds the title
	 * TextView by ID ({@code R.id.tvDialogTitle}) within the dialog's root view
	 * and sets its text to the provided value. If the dialog root view or the
	 * title view is null, the method returns without making any changes.
	 *
	 * <p>For string resource-based titles, use {@link #setDialogTitle(int)} instead.
	 *
	 * @param title The title text to display. Must not be null.
	 * @return This builder instance for method chaining.
	 * @see #setDialogTitle(int)
	 * @see #setTitleVisible(boolean)
	 */
	@NonNull
	public StylizedDialogBuilder setDialogTitle(@NonNull String title) {
		if (dialogRootView != null) {
			TextView tvTitle = dialogRootView.findViewById(R.id.tvDialogTitle);
			if (tvTitle != null) tvTitle.setText(title);
		}
		return this;
	}
	
	/**
	 * Sets the dialog title using a string resource ID. This method retrieves the
	 * string from the activity context and delegates to {@link #setDialogTitle(String)}.
	 * If the activity reference is null, the method returns without making changes.
	 *
	 * <p>Using string resources is recommended for supporting multiple locales and
	 * easier maintenance.
	 *
	 * @param resId The string resource ID for the title text.
	 * @return This builder instance for method chaining.
	 * @see #setDialogTitle(String)
	 * @see #setTitleVisible(boolean)
	 */
	@NonNull
	public StylizedDialogBuilder setDialogTitle(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setDialogTitle(activity.getString(resId));
		}
		return this;
	}
	
	/**
	 * Sets the margin around the dialog in density-independent pixels (dp). The
	 * margin is applied uniformly to all four sides of the dialog's root view.
	 * The provided dimension resource ID is converted to pixels based on the
	 * activity's display metrics.
	 *
	 * <p>If the activity reference is null, the method returns without applying
	 * the margin. This is useful for creating dialogs that do not fill the entire
	 * screen width, providing a more compact appearance on larger displays.
	 *
	 * @param marginDp The dimension resource ID for the margin in dp (e.g., R.dimen._16dp).
	 * @return This builder instance for method chaining.
	 * @see #dialogMarginPx
	 * @see DisplayMetrics#density
	 */
	@NonNull
	public StylizedDialogBuilder setDialogMargin(@DimenRes int marginDp) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			dialogMarginPx = (int) (marginDp * activity.getResources()
				.getDisplayMetrics().density);
		}
		return this;
	}
	
	/**
	 * Sets the visibility of the dialog's title container. The title area typically
	 * contains the dialog title text and optionally the close button. Hiding the
	 * title container may be appropriate for simple confirmation dialogs where
	 * the message alone suffices, or for fully custom content layouts.
	 *
	 * <p>If the dialog root view or the title container view is null, the method
	 * returns without making any changes.
	 *
	 * @param visible {@code true} to show the title container, {@code false} to hide it.
	 * @return This builder instance for method chaining.
	 * @see #setDialogTitle(int)
	 * @see #setCloseButtonVisible(boolean)
	 */
	@NonNull
	public StylizedDialogBuilder setTitleVisible(boolean visible) {
		if (dialogRootView != null) {
			View container = dialogRootView.findViewById(R.id.containerTitle);
			if (container != null) {
				container.setVisibility(visible ? View.VISIBLE : View.GONE);
			}
		}
		return this;
	}
	
	/**
	 * Enables background blur (or dim fallback for older Android versions) behind
	 * the dialog. On Android S (API 31) and above, this method applies a native
	 * blur effect behind the dialog window with the specified radius. On older
	 * platforms, it falls back to a dim effect with 60% opacity.
	 *
	 * <p><strong>Behavior by API level:</strong>
	 * <ul>
	 * <li>API 31+ → Native blur behind with specified blur radius.</li>
	 * <li>API ≤ 30 → Dim background with 0.6 dim amount.</li>
	 * </ul>
	 *
	 * @param blurRadius The radius of the blur effect in pixels (only applies on API 31+).
	 * @return This builder instance for method chaining.
	 * @see WindowManager.LayoutParams#FLAG_BLUR_BEHIND
	 * @see WindowManager.LayoutParams#FLAG_DIM_BEHIND
	 */
	@NonNull
	public StylizedDialogBuilder enableBackgroundBlur(int blurRadius) {
		if (alertDialog == null) return this;
		
		Window window = alertDialog.getWindow();
		if (window != null) {
			window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				window.getAttributes().setBlurBehindRadius(blurRadius);
			} else {
				window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				window.getAttributes().dimAmount = 0.6f;
			}
		}
		return this;
	}
	
	/**
	 * Applies bottom positioning to the dialog along with a slide-up animation.
	 * This convenience method calls {@link #enableBottomPosition()} to anchor the
	 * dialog to the bottom of the screen and {@link #enableSlideUpAnimation()} to
	 * apply a slide animation.
	 *
	 * <p>This combination is ideal for bottom sheets, action sheets, or dialogs
	 * that should appear from the bottom edge of the screen with a smooth
	 * sliding motion.
	 *
	 * @return This builder instance for method chaining.
	 * @see #enableBottomPosition()
	 * @see #enableSlideUpAnimation()
	 */
	@NonNull
	public StylizedDialogBuilder applyBottomPositioning() {
		enableBottomPosition();
		enableSlideUpAnimation();
		return this;
	}
	
	/**
	 * Sets a custom window animation for the dialog. The animation resource ID is
	 * applied to the dialog's window attributes, controlling the enter and exit
	 * animations when the dialog is shown and dismissed.
	 *
	 * <p>If the alert dialog or its window is null, the method returns without
	 * applying the animation. Standard Android window animation styles should be
	 * used (e.g., {@link android.R.style#Animation_Dialog}).
	 *
	 * @param animationResId The resource ID of the animation style to apply.
	 * @return This builder instance for method chaining.
	 * @see android.view.Window#getAttributes()
	 * @see android.view.WindowManager.LayoutParams#windowAnimations
	 */
	@NonNull
	public StylizedDialogBuilder setDialogAnimation(int animationResId) {
		if (alertDialog != null && alertDialog.getWindow() != null) {
			alertDialog.getWindow().getAttributes().windowAnimations = animationResId;
		}
		
		return this;
	}
	
	/**
	 * Applies a slide-up animation to the dialog. This is a convenience method that
	 * calls {@link #setDialogAnimation(int)} with a predefined slide animation style
	 * ({@code R.style.style_dialog_window_slide_animation}), causing the dialog to
	 * slide upwards when shown and slide downwards when dismissed.
	 *
	 * <p>This animation is commonly used for bottom sheets or dialogs that appear
	 * from the bottom edge of the screen.
	 *
	 * @return This builder instance for method chaining.
	 * @see #enableFadeInAnimation()
	 * @see #setDialogAnimation(int)
	 */
	@NotNull
	public StylizedDialogBuilder enableSlideUpAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
		return this;
	}
	
	/**
	 * Applies a fade-in/fade-out animation to the dialog. This is a convenience
	 * method that calls {@link #setDialogAnimation(int)} with a predefined fade
	 * animation style ({@code R.style.style_dialog_window_fade_animation}), causing
	 * the dialog to smoothly fade in when shown and fade out when dismissed.
	 *
	 * <p>Fade animations provide a subtle, professional appearance and work well
	 * for most dialog types without distracting the user.
	 *
	 * @return This builder instance for method chaining.
	 * @see #enableSlideUpAnimation()
	 * @see #setDialogAnimation(int)
	 */
	@NotNull
	public StylizedDialogBuilder enableFadeInAnimation() {
		setDialogAnimation(R.style.style_dialog_window_fade_animation);
		return this;
	}
	
	/**
	 * Sets a dismiss listener for the dialog that is invoked when the dialog is
	 * dismissed (either by user action, system action, or programmatically). If
	 * the listener is provided and the alert dialog exists, the listener is attached
	 * and will automatically call {@link #close()} after the listener's
	 * {@code onDismiss} method executes.
	 *
	 * <p>This ensures that dialog resources are properly cleaned up when the dialog
	 * is dismissed, regardless of whether the dismissal was triggered by the
	 * listener or by other means. The close operation clears click listeners and
	 * nullifies references to prevent memory leaks.
	 *
	 * @param listener The {@link OnDismissListener} to invoke when the dialog is dismissed.
	 * @return This builder instance for method chaining.
	 * @see android.app.Dialog#setOnDismissListener(DialogInterface.OnDismissListener)
	 * @see #close()
	 */
	@NonNull
	public StylizedDialogBuilder setDialogDismissListener(OnDismissListener listener) {
		if (alertDialog != null && listener != null) {
			alertDialog.setOnDismissListener(dialogInterface -> {
				listener.onDismiss(dialogInterface);
				close();
			});
		}
		return this;
	}
	
	/**
	 * Sets the text for the dialog's positive (right) button using a plain string.
	 * This method finds the right button by ID ({@code R.id.btnDialogRight}) and
	 * sets its text to the provided value. If the dialog root view or the button
	 * view is null, the method returns without making any changes.
	 *
	 * <p>The positive button typically represents an affirmative action (e.g.,
	 * "OK", "Confirm", "Yes", "Accept"). For string resource-based text, use
	 * {@link #setPositiveButtonText(int)} instead.
	 *
	 * @param text The text to display on the positive button. Must not be null.
	 * @return This builder instance for method chaining.
	 * @see #setPositiveButtonText(int)
	 */
	@NonNull
	public StylizedDialogBuilder setPositiveButtonText(@NonNull String text) {
		if (dialogRootView != null) {
			MaterialButton btnRight = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnRight != null) btnRight.setText(text);
		}
		return this;
	}
	
	/**
	 * Sets the text for the dialog's positive (right) button using a string resource ID.
	 * This method retrieves the string from the activity context and delegates to
	 * {@link #setPositiveButtonText(String)}. If the activity reference is null,
	 * the method returns without making any changes.
	 *
	 * <p>The positive button typically represents an affirmative action (e.g.,
	 * "OK", "Confirm", "Yes", "Accept"). Using string resources is recommended
	 * for supporting multiple locales.
	 *
	 * @param resId The string resource ID for the button text.
	 * @return This builder instance for method chaining.
	 * @see #setPositiveButtonText(String)
	 */
	@NonNull
	public StylizedDialogBuilder setPositiveButtonText(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setPositiveButtonText(activity.getString(resId));
		}
		return this;
	}
	
	/**
	 * Sets the icon for the dialog's positive (right) button using a {@link Drawable}
	 * object. This method finds the right button by ID ({@code R.id.btnDialogRight})
	 * and applies the provided drawable as its icon via
	 * {@link MaterialButton#setIcon(Drawable)}.
	 *
	 * <p>If the dialog root view or the button view is null, the method returns
	 * without making any changes. Passing {@code null} clears any existing icon
	 * from the button.
	 *
	 * <p>MaterialButton supports inline icons that appear to the left of the button
	 * text, providing a more visually engaging dialog layout, especially for
	 * confirmations and important actions.
	 *
	 * @param iconDrawable The drawable to use as the button icon, or {@code null}
	 *                     to remove the icon.
	 * @return This builder instance for method chaining.
	 * @see MaterialButton#setIcon(Drawable)
	 * @see #setPositiveButtonIcon(int)
	 */
	@NonNull
	public StylizedDialogBuilder setPositiveButtonIcon(@Nullable Drawable iconDrawable) {
		if (dialogRootView != null) {
			MaterialButton btnRight = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnRight != null) {
				btnRight.setIcon(iconDrawable);
			}
		}
		return this;
	}
	
	/**
	 * Sets the icon for the dialog's positive (right) button using a drawable
	 * resource ID. This method retrieves the drawable from the activity context
	 * using {@link ContextCompat#getDrawable(Context, int)}. If the icon resource
	 * ID is {@code 0}, the icon is cleared by setting it to {@code null}.
	 *
	 * <p>The positive button typically represents an affirmative action (e.g.,
	 * "OK", "Confirm", "Yes", "Accept"). Adding an icon can improve visual
	 * clarity, especially for important confirmation dialogs.
	 *
	 * @param iconResId The drawable resource ID for the icon, or {@code 0} to remove the icon.
	 * @return This builder instance for method chaining.
	 * @see #setPositiveButtonIcon(Drawable)
	 */
	@NonNull
	public StylizedDialogBuilder setPositiveButtonIcon(@DrawableRes int iconResId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			Drawable iconDrawable = (iconResId != 0)
				? ContextCompat.getDrawable(activity, iconResId) : null;
			return setPositiveButtonIcon(iconDrawable);
		}
		return this;
	}
	
	/**
	 * Sets a click listener for the dialog's positive (right) button with an option
	 * to automatically close the dialog after the listener executes. The right button
	 * container is found by ID ({@code R.id.btnDialogRight}) and its click listener
	 * is replaced with the provided implementation.
	 *
	 * <p>If {@code shouldCloseAfterExecution} is {@code true}, the dialog is
	 * dismissed immediately after the listener's {@code onClick} method returns.
	 * This is useful for actions like "Submit" or "Confirm" where the dialog should
	 * close after user interaction. When {@code false}, the dialog remains open,
	 * allowing the user to continue interacting (e.g., for multi-step confirmations).
	 *
	 * @param listener                   The click listener to attach to the positive button.
	 * @param shouldCloseAfterExecution If {@code true}, closes the dialog after listener
	 *                                     execution.
	 * @return This builder instance for method chaining.
	 * @see #setCloseOnPositiveButtonClick()
	 * @see #close()
	 */
	@NonNull
	public StylizedDialogBuilder setOnPositiveClickListener(
		@NonNull View.OnClickListener listener, boolean shouldCloseAfterExecution) {
		if (dialogRootView != null) {
			View btnContainer = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnContainer != null) {
				btnContainer.setOnClickListener(v -> {
					listener.onClick(v);
					if (shouldCloseAfterExecution) {
						close();
					}
				});
			}
		}
		return this;
	}
	
	/**
	 * Configures the dialog to dismiss automatically when the positive (right)
	 * button is clicked. This method finds the right button container view by
	 * ID ({@code R.id.btnDialogRight}) and sets its click listener to call
	 * {@link #close()}.
	 *
	 * <p>This is a convenience method for the common use case where the positive
	 * button (typically labeled "OK", "Confirm", or "Yes") should simply dismiss
	 * the dialog after being clicked, without executing additional logic. For
	 * custom behavior, use {@link #setOnPositiveClickListener(View.OnClickListener, boolean)}.
	 *
	 * @return This builder instance for method chaining.
	 * @see #setOnPositiveClickListener(View.OnClickListener, boolean)
	 * @see #close()
	 */
	@NonNull
	public StylizedDialogBuilder setCloseOnPositiveButtonClick() {
		if (dialogRootView != null) {
			View btnContainer = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnContainer != null) {
				btnContainer.setOnClickListener(v -> close());
			}
		}
		return this;
	}
	
	/**
	 * Sets the text for the dialog's negative (left) button using a plain string.
	 * This method finds the left button by ID ({@code R.id.btnDialogLeft}) and
	 * sets its text to the provided value. If the dialog root view or the button
	 * view is null, the method returns without making any changes.
	 *
	 * <p>The negative button typically represents a cancel or reject action
	 * (e.g., "Cancel", "No", "Decline"). For string resource-based text, use
	 * {@link #setNegativeButtonText(int)} instead.
	 *
	 * @param text The text to display on the negative button. Must not be null.
	 * @return This builder instance for method chaining.
	 * @see #setNegativeButtonText(int)
	 * @see #setNegativeButtonVisible(boolean)
	 */
	@NonNull
	public StylizedDialogBuilder setNegativeButtonText(@NonNull String text) {
		if (dialogRootView != null) {
			MaterialButton btnLeft = dialogRootView.findViewById(R.id.btnDialogLeft);
			if (btnLeft != null) btnLeft.setText(text);
		}
		return this;
	}
	
	/**
	 * Sets the text for the dialog's negative (left) button using a string resource ID.
	 * This method retrieves the string from the activity context and delegates to
	 * {@link #setNegativeButtonText(String)}. If the activity reference is null,
	 * the method returns without making any changes.
	 *
	 * <p>The negative button typically represents a cancel or reject action
	 * (e.g., "Cancel", "No", "Decline"). Using string resources is recommended
	 * for supporting multiple locales.
	 *
	 * @param resId The string resource ID for the button text.
	 * @return This builder instance for method chaining.
	 * @see #setNegativeButtonText(String)
	 * @see #setNegativeButtonVisible(boolean)
	 */
	@NonNull
	public StylizedDialogBuilder setNegativeButtonText(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setNegativeButtonText(activity.getString(resId));
		}
		return this;
	}
	
	/**
	 * Sets the icon for the dialog's negative (left) button using a {@link Drawable}
	 * object. This method finds the left button by ID ({@code R.id.btnDialogLeft})
	 * and applies the provided drawable as its icon via
	 * {@link MaterialButton#setIcon(Drawable)}.
	 *
	 * <p>If the dialog root view or the button view is null, the method returns
	 * without making any changes. Passing {@code null} clears any existing icon
	 * from the button.
	 *
	 * <p>MaterialButton supports inline icons that appear to the left of the button
	 * text, providing a more visually engaging dialog layout.
	 *
	 * @param iconDrawable The drawable to use as the button icon, or {@code null}
	 *                     to remove the icon.
	 * @return This builder instance for method chaining.
	 * @see MaterialButton#setIcon(Drawable)
	 * @see #setNegativeButtonIcon(int)
	 */
	@NonNull
	public StylizedDialogBuilder setNegativeButtonIcon(@Nullable Drawable iconDrawable) {
		if (dialogRootView != null) {
			MaterialButton btnLeft = dialogRootView.findViewById(R.id.btnDialogLeft);
			if (btnLeft != null) {
				btnLeft.setIcon(iconDrawable);
			}
		}
		return this;
	}
	
	/**
	 * Sets the icon for the dialog's negative (left) button using a drawable
	 * resource ID. This method retrieves the drawable from the activity context
	 * using {@link ContextCompat#getDrawable(Context, int)}. If the icon resource
	 * ID is {@code 0}, the icon is cleared by setting it to {@code null}.
	 *
	 * <p>The negative button typically represents a cancel or reject action
	 * (e.g., "Cancel", "No", "Decline"). Adding an icon can improve visual
	 * clarity, especially for dialogs with multiple action buttons.
	 *
	 * @param iconResId The drawable resource ID for the icon, or {@code 0} to remove the icon.
	 * @return This builder instance for method chaining.
	 * @see #setNegativeButtonIcon(Drawable)
	 * @see #setNegativeButtonVisible(boolean)
	 */
	@NonNull
	public StylizedDialogBuilder setNegativeButtonIcon(@DrawableRes int iconResId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			Drawable iconDrawable = iconResId != 0
				? ContextCompat.getDrawable(activity, iconResId) : null;
			return setNegativeButtonIcon(iconDrawable);
		}
		return this;
	}
	
	/**
	 * Sets a click listener for the dialog's negative (left) button with an option
	 * to automatically close the dialog after the listener executes. The left button
	 * container is found by ID ({@code R.id.btnDialogLeft}) and its click listener
	 * is replaced with the provided implementation.
	 *
	 * <p>If {@code shouldCloseAfterExecution} is {@code true}, the dialog is
	 * dismissed immediately after the listener's {@code onClick} method returns.
	 * This is useful for actions like "Cancel" or "Reject" where the dialog should
	 * close after user interaction. When {@code false}, the dialog remains open,
	 * allowing the user to continue interacting without re-opening.
	 *
	 * @param listener                   The click listener to attach to the negative button.
	 * @param shouldCloseAfterExecution If {@code true}, closes the dialog after listener
	 *                                     execution.
	 * @return This builder instance for method chaining.
	 * @see #setNegativeButtonVisible(boolean)
	 * @see #setCloseOnNegativeButtonClick()
	 * @see #close()
	 */
	@NonNull
	public StylizedDialogBuilder setOnNegativeClickListener(
		@NonNull View.OnClickListener listener,
		boolean shouldCloseAfterExecution) {
		if (dialogRootView != null) {
			View btnContainer = dialogRootView.findViewById(R.id.btnDialogLeft);
			if (btnContainer != null) {
				btnContainer.setOnClickListener(v -> {
					listener.onClick(v);
					if (shouldCloseAfterExecution) {
						close();
					}
				});
			}
		}
		return this;
	}
	
	/**
	 * Configures the dialog to dismiss automatically when the negative (left)
	 * button is clicked. This method finds the left button container view by
	 * ID ({@code R.id.btnDialogLeft}) and sets its click listener to call
	 * {@link #close()}.
	 *
	 * <p>This is a convenience method for the common use case where the negative
	 * button (typically labeled "Cancel" or "No") should simply dismiss the dialog
	 * without executing any additional logic. For custom behavior, use
	 * {@link #setOnNegativeClickListener(View.OnClickListener, boolean)} instead.
	 *
	 * @return This builder instance for method chaining.
	 * @see #setNegativeButtonVisible(boolean)
	 * @see #setOnNegativeClickListener(View.OnClickListener, boolean)
	 * @see #close()
	 */
	@NonNull
	public StylizedDialogBuilder setCloseOnNegativeButtonClick() {
		if (dialogRootView != null) {
			View btnContainer = dialogRootView.findViewById(R.id.btnDialogLeft);
			if (btnContainer != null) {
				btnContainer.setOnClickListener(v -> close());
			}
		}
		return this;
	}
	
	/**
	 * Sets the visibility of the dialog's negative (left) button. This method
	 * finds the left button container view by ID ({@code R.id.btnDialogLeft})
	 * and toggles its visibility based on the provided parameter.
	 *
	 * <p>The negative button typically represents a cancel or reject action
	 * (e.g., "Cancel", "No", "Decline"). Hiding this button may be appropriate
	 * when the dialog only requires an affirmative response (e.g., confirmation
	 * with a single "OK" button).
	 *
	 * @param visible {@code true} to show the negative button, {@code false} to hide it.
	 * @return This builder instance for method chaining.
	 * @see #setCloseOnNegativeButtonClick()
	 */
	@NonNull
	public StylizedDialogBuilder setNegativeButtonVisible(boolean visible) {
		if (dialogRootView != null) {
			View btnLeft = dialogRootView.findViewById(R.id.btnDialogLeft);
			if (btnLeft != null) {
				btnLeft.setVisibility(visible ? View.VISIBLE : View.GONE);
			}
		}
		return this;
	}
	
	/**
	 * Sets the visibility of the dialog's close button. This method finds the
	 * close button view by ID ({@code R.id.btnCloseDialog}) within the dialog's
	 * root view and toggles its visibility based on the provided parameter.
	 *
	 * <p>If the dialog root view or the close button view is null, the method
	 * returns without making any changes. This is typically used to hide the
	 * close button when the dialog should only be dismissible by user action
	 * on a primary button (e.g., "Confirm" or "Cancel").
	 *
	 * @param visible {@code true} to show the close button, {@code false} to hide it.
	 * @return This builder instance for method chaining.
	 * @see View#setVisibility(int)
	 */
	@NonNull
	public StylizedDialogBuilder setCloseButtonVisible(boolean visible) {
		if (dialogRootView != null) {
			View btnClose = dialogRootView.findViewById(R.id.btnCloseDialog);
			if (btnClose != null) {
				btnClose.setVisibility(visible ? View.VISIBLE : View.GONE);
			}
		}
		return this;
	}
	
	/**
	 * Sets a click listener for the dialog's close button. If a custom listener
	 * is provided, it is invoked before the dialog is automatically closed. If
	 * {@code null} is provided, the button retains its default behavior of simply
	 * closing the dialog without any additional actions.
	 *
	 * <p>In both cases, the dialog is closed after the click event is processed.
	 * This ensures consistent behavior where clicking the close button always
	 * dismisses the dialog, optionally after executing custom logic.
	 *
	 * @param listener The click listener to invoke when the close button is clicked,
	 *                 or {@code null} to use the default close-only behavior.
	 * @return This builder instance for method chaining.
	 * @see #setCloseButtonVisible(boolean)
	 * @see #close()
	 */
	@NonNull
	public StylizedDialogBuilder setOnCloseClickListener(@Nullable
	                                                     View.OnClickListener listener) {
		if (dialogRootView != null) {
			View btnClose = dialogRootView.findViewById(R.id.btnCloseDialog);
			if (btnClose != null) {
				if (listener == null) {
					btnClose.setOnClickListener(v -> close());
				} else {
					btnClose.setOnClickListener(v -> {
						listener.onClick(v);
						close();
					});
				}
			}
		}
		return this;
	}
	
	/**
	 * Returns the custom content view that was previously set for this dialog.
	 * This method retrieves the view from a {@link WeakReference} to avoid memory
	 * leaks. If the view reference is null or has been garbage collected, an
	 * {@link IllegalStateException} is thrown to indicate that the content view
	 * has not been properly initialized.
	 *
	 * <p>This method is typically called after {@link #setCustomContentView(View)}
	 * has been invoked to inflate and attach the dialog's content layout.
	 *
	 * @return The non-null custom content view associated with this dialog.
	 * @throws IllegalStateException If the content view is null (not set or
	 *                               already garbage collected).
	 * @see #setCustomContentView(View)
	 * @see #close()
	 */
	@NonNull
	public View getCustomContentView() throws IllegalStateException {
		View view = weakCustomChildViewRef != null ?
			weakCustomChildViewRef.get() : null;
		
		if (view == null) {
			throw new IllegalStateException("Content view elements are unallocated. " +
				"Invoke setCustomContentView first.");
		}
		return view;
	}
	
	/**
	 * Returns the underlying {@link AlertDialog} instance managed by this dialog
	 * wrapper. This method provides direct access to the dialog for advanced
	 * customization or operations not covered by the wrapper's public methods.
	 *
	 * <p>The returned dialog may be {@code null} if it has not been created yet
	 * or has been closed and released via {@link #close()}.
	 *
	 * @return The {@link AlertDialog} instance, or {@code null} if not available.
	 * @see #close()
	 * @see #show()
	 */
	@Nullable
	public AlertDialog getAlertDialog() {
		return alertDialog;
	}
	
	/**
	 * Returns the activity that owns and hosts this dialog. The activity reference
	 * is stored as a {@link WeakReference} to prevent memory leaks when the dialog
	 * outlives the activity lifecycle.
	 *
	 * <p>The returned activity may be {@code null} if the reference has been
	 * cleared (e.g., after garbage collection) or if the activity was destroyed.
	 *
	 * @return The {@link BaseActivity} instance hosting this dialog, or
	 *         {@code null} if the reference is no longer valid.
	 * @see BaseActivity
	 * @see WeakReference
	 */
	@Nullable
	public BaseActivity<?> getActivity() {
		return weakActivityRef != null ? weakActivityRef.get() : null;
	}
	
	/**
	 * Displays the custom styled dialog on the screen. This method checks that the
	 * referenced activity is still valid (not finishing or destroyed) before showing
	 * the dialog. It also applies a transparent background to the dialog window and
	 * optionally sets margins on the root view if configured.
	 *
	 * <p><strong>Preconditions checked:</strong>
	 * <ul>
	 * <li>The activity reference is not null.</li>
	 * <li>The activity is not finishing or destroyed.</li>
	 * <li>The alert dialog exists and is not already showing.</li>
	 * </ul>
	 *
	 * <p>If dialog margin pixels have been set (non-negative), they are applied as
	 * uniform margins to the root view. Any exception during the show process is
	 * caught and logged without crashing the application.
	 *
	 * @see #close()
	 * @see android.app.Dialog#show()
	 */
	public void show() {
		try {
			BaseActivity<?> activity = weakActivityRef.get();
			if (activity == null || activity.isFinishing() ||
				activity.isDestroyed()) return;
			
			if (alertDialog != null && !alertDialog.isShowing()) {
				alertDialog.show();
				Window alertDialogWindow = alertDialog.getWindow();
				if (alertDialogWindow != null) {
					int resId = R.color.style_color_transparent;
					alertDialogWindow.setBackgroundDrawableResource(resId);
				}
				
				if (dialogRootView != null && dialogMarginPx >= 0) {
					FrameLayout.LayoutParams params =
						new FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					params.setMargins(dialogMarginPx, dialogMarginPx,
						dialogMarginPx, dialogMarginPx);
					
					dialogRootView.setLayoutParams(params);
				}
			}
		} catch (Exception error) {
			logger.error("Failed to cleanly present custom stylizer" +
				" dialog metrics: ", error);
		}
	}
	
	/**
	 * Dismisses the dialog and releases all associated resources. This method
	 * removes the dismiss listener, dismisses the dialog if it is showing,
	 * clears click listeners from the root view hierarchy, and nullifies all
	 * references to prevent memory leaks.
	 *
	 * <p><strong>Cleanup performed:</strong>
	 * <ul>
	 * <li>Dismisses the alert dialog and sets its reference to null.</li>
	 * <li>Recursively clears click listeners via {@link #clearAllLayoutListeners(View)}.</li>
	 * <li>Clears weak references to the main content and custom child view.</li>
	 * </ul>
	 *
	 * <p>Any exception during cleanup is caught and logged, ensuring the method
	 * does not throw and interrupt the calling code.
	 *
	 * @see #show()
	 * @see #clearAllLayoutListeners(View)
	 * @see android.app.Dialog#dismiss()
	 */
	public void close() {
		try {
			if (alertDialog != null) {
				alertDialog.setOnDismissListener(null);
				if (alertDialog.isShowing()) {
					alertDialog.dismiss();
				}
				alertDialog = null;
			}
			if (dialogRootView != null) {
				clearAllLayoutListeners(dialogRootView);
				dialogRootView = null;
			}
			if (weakMainContentRef != null) weakMainContentRef.clear();
			if (weakCustomChildViewRef != null) weakCustomChildViewRef.clear();
		} catch (Exception error) {
			logger.error("Exception handled during dialog reference " +
				"recycling closures: ", error);
		}
	}
	
	/**
	 * Recursively clears click listeners from a view and all its child views.
	 * This method traverses the view hierarchy starting from the given view,
	 * setting {@code null} click listeners on each view to prevent memory leaks
	 * and ensure that no lingering callbacks remain after the view is dismissed.
	 *
	 * <p>If the provided view is null, the method returns immediately. For
	 * {@link ViewGroup} instances, the method recursively processes all child
	 * views. This is typically called before showing a new dialog or when
	 * cleaning up resources to avoid unwanted click events.
	 *
	 * @param view The root view from which to clear listeners, or {@code null}.
	 * @see View#setOnClickListener(android.view.View.OnClickListener)
	 */
	private void clearAllLayoutListeners(@Nullable View view) {
		if (view == null) return;
		view.setOnClickListener(null);
		if (view instanceof ViewGroup group) {
			for (int index = 0; index < group.getChildCount(); index++) {
				clearAllLayoutListeners(group.getChildAt(index));
			}
		}
	}
	
	/**
	 * Configures the alert dialog to appear at the bottom of the screen with
	 * custom window parameters. This method sets the window gravity to
	 * {@link Gravity#BOTTOM}, resets the Y offset to 0, applies a transparent
	 * background, and ensures the dialog width fills the parent while height
	 * wraps its content.
	 *
	 * <p>If the dialog or its window is null, the method returns silently.
	 * This configuration is typically used for bottom sheets or modal dialogs
	 * that should slide up from the bottom edge of the screen.
	 *
	 * @see android.app.Dialog#getWindow()
	 * @see Gravity#BOTTOM
	 * @see WindowManager.LayoutParams
	 */
	private void enableBottomPosition() {
		if (alertDialog == null) return;
		if (alertDialog.getWindow() != null) {
			alertDialog.getWindow().setGravity(Gravity.BOTTOM);
			WindowManager.LayoutParams params = alertDialog.getWindow().getAttributes();
			params.y = 0;
			alertDialog.getWindow().setAttributes(params);
			alertDialog.getWindow().setBackgroundDrawableResource(R.color.style_color_transparent);
			alertDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		}
	}
}