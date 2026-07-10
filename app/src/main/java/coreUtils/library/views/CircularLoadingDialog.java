package coreUtils.library.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nextgen.R;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

/**
 * A custom dialog that displays an indeterminate circular progress indicator
 * to indicate ongoing background operations (e.g., network requests, file
 * downloads, or data processing). This dialog provides a minimalist loading
 * animation with configurable appearance and behavior.
 *
 * <p><strong>Core features:</strong>
 * <ul>
 * <li>Indeterminate circular progress indicator with customizable colors.</li>
 * <li>Configurable cancellability (default is cancelable).</li>
 * <li>Background blur (API 31+) with dim fallback for older platforms.</li>
 * <li>Slide-up or fade-in animations for dialog appearance.</li>
 * <li>Bottom positioning for bottom sheet-style loading dialogs.</li>
 * <li>Automatic resource cleanup via {@link #close()}.</li>
 * </ul>
 *
 * <p>Usage: Create instance, configure, call {@link #show()}, then {@link #close()}.
 *
 * @see BaseActivity
 * @see #show()
 * @see #close()
 */
public final class CircularLoadingDialog {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final WeakReference<BaseActivity<?>> weakActivityRef;
	
	private AlertDialog alertDialog;
	private View dialogRootView;
	
	/**
	 * Constructs a new CircularLoadingDialog for the given activity. The builder
	 * creates a weak reference to the activity to prevent memory leaks when the
	 * dialog outlives the activity lifecycle. Upon construction, the base dialog
	 * layout is initialized via {@link #initializeBaseLayout()}.
	 *
	 * <p>The dialog displays an indeterminate circular progress indicator and
	 * can be configured with background blur, animations, and bottom positioning.
	 *
	 * @param activity The activity that will host and own this dialog. Must not be
	 *                 null and must be a valid {@link BaseActivity} instance.
	 * @see #show()
	 * @see #close()
	 * @see #initializeBaseLayout()
	 */
	public CircularLoadingDialog(BaseActivity<?> activity) {
		this.weakActivityRef = new WeakReference<>(activity);
		initializeBaseLayout();
	}
	
	/**
	 * Initializes the base dialog layout, window properties, and default behavior.
	 * This method inflates the loading dialog layout, creates the AlertDialog
	 * instance, sets up dismiss listeners for the close buttons, and configures
	 * default cancellability.
	 *
	 * <p><strong>Initialization steps:</strong>
	 * <ol>
	 * <li>Validates that the activity reference is still valid (not finishing/destroyed).</li>
	 * <li>Inflates {@code R.layout.dialog_static_loading_1} into the content root.</li>
	 * <li>Creates an AlertDialog with custom styling ({@code R.style.style_dialog_window}).</li>
	 * <li>Sets a dismiss listener to automatically call {@link #close()}.</li>
	 * <li>Configures left and right buttons (if present) to dismiss the dialog.</li>
	 * <li>Makes the dialog cancelable by default.</li>
	 * </ol>
	 *
	 * @throws IllegalStateException If the activity reference is invalid or null.
	 */
	private void initializeBaseLayout() {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
		
		ViewGroup root = activity.findViewById(android.R.id.content);
		dialogRootView = activity.getLayoutInflater()
			.inflate(R.layout.dialog_static_loading_1, root, false);
		
		int styledResId = R.style.style_dialog_window;
		AlertDialog.Builder nativeBuilder = new AlertDialog.Builder(activity, styledResId);
		nativeBuilder.setView(dialogRootView);
		this.alertDialog = nativeBuilder.create();
		
		this.alertDialog.setOnDismissListener(dialogInterface -> close());
		
		if (dialogRootView != null) {
			View btnLeft = dialogRootView.findViewById(R.id.btnDialogLeft);
			if (btnLeft != null) {
				btnLeft.setOnClickListener(view -> close());
			}
			
			View btnRight = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnRight != null) {
				btnRight.setOnClickListener(view -> close());
			}
		}
		setCancelable(true);
	}
	
	/**
	 * Sets whether the dialog is cancelable by user actions. When cancelable is
	 * {@code true}, the dialog can be dismissed by pressing the back button or
	 * tapping outside the dialog area. When {@code false}, the dialog requires
	 * explicit dismissal via {@link #close()}.
	 *
	 * <p>Both {@link AlertDialog#setCancelable(boolean)} and
	 * {@link AlertDialog#setCanceledOnTouchOutside(boolean)} are set to the same
	 * value to ensure consistent behavior.
	 *
	 * @param cancelable {@code true} to allow cancellation, {@code false} otherwise.
	 * @return This builder instance for method chaining.
	 */
	@NonNull
	public CircularLoadingDialog setCancelable(boolean cancelable) {
		if (alertDialog != null) {
			alertDialog.setCancelable(cancelable);
			alertDialog.setCanceledOnTouchOutside(cancelable);
		}
		return this;
	}
	
	/**
	 * Enables background blur (or dim fallback for older Android versions) behind
	 * the dialog. On Android S (API 31) and above, applies native blur with the
	 * specified radius. On older platforms, falls back to dim effect with 60% opacity.
	 *
	 * @param blurRadius The radius of the blur effect in pixels (only applies on API 31+).
	 * @return This builder instance for method chaining.
	 * @see WindowManager.LayoutParams#FLAG_BLUR_BEHIND
	 */
	@NonNull
	public CircularLoadingDialog enableBackgroundBlur(int blurRadius) {
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
	 * Sets a custom window animation for the dialog. The animation resource ID is
	 * applied to the dialog's window attributes, controlling enter and exit animations.
	 *
	 * @param animationResId The resource ID of the animation style to apply.
	 * @return This builder instance for method chaining.
	 */
	@NonNull
	public CircularLoadingDialog setDialogAnimation(int animationResId) {
		if (alertDialog != null && alertDialog.getWindow() != null) {
			alertDialog
				.getWindow()
				.getAttributes()
				.windowAnimations = animationResId;
		}
		return this;
	}
	
	/**
	 * Applies a slide-up animation to the dialog using the predefined slide style
	 * ({@code R.style.style_dialog_window_slide_animation}). The dialog slides upward
	 * when shown and slides downward when dismissed.
	 *
	 * <p>This animation is commonly used for bottom sheets or loading dialogs that
	 * appear from the bottom edge of the screen.
	 *
	 * @return This builder instance for method chaining.
	 * @see #setDialogAnimation(int)
	 */
	@NotNull
	public CircularLoadingDialog enableSlideUpAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
		return this;
	}
	
	/**
	 * Applies a fade-in/fade-out animation to the dialog using the predefined fade
	 * style ({@code R.style.style_dialog_window_slide_animation}). The dialog smoothly
	 * fades in when shown and fades out when dismissed.
	 *
	 * <p>Fade animations provide a subtle, professional appearance and work well
	 * for loading dialogs and popup notifications.
	 *
	 * @return This builder instance for method chaining.
	 * @see #setDialogAnimation(int)
	 */
	@NotNull
	public CircularLoadingDialog enableFadeInAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
		return this;
	}
	
	/**
	 * Sets a dismiss listener for the circular loading dialog. When the dialog is
	 * dismissed, this method first calls {@link #close()} to clean up resources,
	 * then invokes the provided listener's {@code onDismiss} callback (if present).
	 *
	 * <p>The listener is attached directly to the underlying {@link AlertDialog}.
	 * If the alert dialog is null, the method returns without making any changes.
	 *
	 * @param listener The dismiss listener to invoke after dialog cleanup, or null.
	 * @return This builder instance for method chaining.
	 * @see AlertDialog#setOnDismissListener(android.content.DialogInterface.OnDismissListener)
	 * @see #close()
	 */
	@NonNull
	public CircularLoadingDialog setDialogDismissListener(DialogInterface.OnDismissListener listener) {
		if (alertDialog != null) {
			alertDialog.setOnDismissListener(dialogInterface -> {
				close();
				if (listener != null) {
					listener.onDismiss(dialogInterface);
				}
			});
		}
		return this;
	}
	
	/**
	 * Applies bottom positioning to the dialog along with a slide-up animation.
	 * This convenience method calls {@link #enableBottomPosition()} to anchor the
	 * dialog to the bottom of the screen and {@link #enableSlideUpAnimation()} to
	 * apply a slide animation.
	 *
	 * <p>This combination is ideal for bottom sheets, action dialogs, or loading
	 * indicators that should appear from the bottom edge of the screen.
	 *
	 * @return This builder instance for method chaining.
	 * @see #enableBottomPosition()
	 * @see #enableSlideUpAnimation()
	 */
	@NonNull
	public CircularLoadingDialog applyBottomPositioning() {
		enableBottomPosition();
		enableSlideUpAnimation();
		return this;
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
	 * {@code null} if the reference is no longer valid.
	 * @see WeakReference
	 * @see BaseActivity
	 */
	@Nullable
	public BaseActivity<?> getActivity() {
		return weakActivityRef != null ? weakActivityRef.get() : null;
	}
	
	/**
	 * Displays the message dialog on the screen. This method checks that the
	 * referenced activity is still valid (not finishing or destroyed) before showing
	 * the dialog. If the dialog exists and is not already showing, it is displayed
	 * and a transparent background is applied to the window.
	 *
	 * <p><strong>Preconditions checked:</strong>
	 * <ul>
	 * <li>The activity reference is not null.</li>
	 * <li>The activity is not finishing or destroyed.</li>
	 * <li>The alert dialog exists and is not already showing.</li>
	 * </ul>
	 *
	 * <p>Any exception during the show process is caught and logged without
	 * crashing the application.
	 *
	 * @see #close()
	 * @see Dialog#show()
	 */
	public void show() {
		try {
			BaseActivity<?> activity = weakActivityRef.get();
			if (activity == null || activity.isFinishing() ||
				activity.isDestroyed()) return;
			
			if (alertDialog != null && !alertDialog.isShowing()) {
				alertDialog.show();
				Window window = alertDialog.getWindow();
				if (window != null) {
					window.setBackgroundDrawableResource(R.color.style_color_transparent);
				}
			}
		} catch (Exception error) {
			logger.error("Failed to present message dialog: ", error);
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
	 * <li>Logs any exception without rethrowing.</li>
	 * </ul>
	 *
	 * @see #clearAllLayoutListeners(View)
	 * @see Dialog#dismiss()
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
		} catch (Exception error) {
			logger.error("Exception during message dialog cleanup: ", error);
		}
	}
	
	/**
	 * Recursively clears click listeners from a view and all its child views.
	 * This method traverses the view hierarchy starting from the given view,
	 * setting {@code null} click listeners on each view to prevent memory leaks.
	 *
	 * <p>If the provided view is null, the method returns immediately. For
	 * {@link ViewGroup} instances, the method recursively processes all child views.
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
	 * <p>The dialog's window must not be null when this method is called.
	 *
	 * @see Dialog#getWindow()
	 * @see Gravity#BOTTOM
	 * @see WindowManager.LayoutParams
	 */
	private void enableBottomPosition() {
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
