package lib.ui.builders;

import static android.view.LayoutInflater.from;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static lib.ui.ViewUtility.hideOnScreenKeyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aio.R;

import java.lang.ref.WeakReference;

import app.core.bases.interfaces.BaseActivityInf;
import lib.process.LogHelperUtils;

/**
 * A builder class for displaying highly customizable and memory-leak-safe dialogs.
 * <p>
 * This class simplifies the creation and configuration of {@link AlertDialog} instances
 * by wrapping Android's native dialog APIs and ensuring lifecycle-safe usage with WeakReferences.
 * It also supports custom layout views, bottom positioning, animations, and more.
 */
public class DialogBuilder {

    private final LogHelperUtils logger = LogHelperUtils.from(getClass());
    private final WeakReference<BaseActivityInf> safeActivityRef;
    private View customView;
    private AlertDialog dialog;

    /**
     * Constructs a new {@link DialogBuilder} with a reference to an activity that implements {@link BaseActivityInf}.
     *
     * @param safeActivityRef a reference to the associated activity, used to access context safely.
     */
    public DialogBuilder(@Nullable BaseActivityInf safeActivityRef) {
        this.safeActivityRef = new WeakReference<>(safeActivityRef);
    }

    /**
     * Displays the dialog if the associated activity is valid and not finishing/destroyed.
     * Initializes the dialog if it hasn’t been created yet.
     */
    public void show() {
        try {
            if (dialog == null) dialog = create();
            BaseActivityInf inf = safeActivityRef.get();
            if (inf == null || inf.getActivity() == null) return;

            Activity act = inf.getActivity();
            if (!act.isFinishing() && !act.isDestroyed() && !dialog.isShowing()) {
                dialog.show();
            }
        } catch (Exception error) {
            logger.d("Dialog show failed: " + error.getMessage());
        }
    }

    /**
     * Returns the initialized dialog instance.
     *
     * @return the current {@link AlertDialog} instance.
     * @throws IllegalStateException if the dialog has not been initialized via {@link #show()}.
     */
    @NonNull
    public AlertDialog getDialog() {
        if (dialog == null) {
            throw new IllegalStateException("Dialog not initialized. Call show() first.");
        }
        return this.dialog;
    }

    /**
     * Sets a click listener to a view inside the dialog using its resource ID.
     *
     * @param viewId   the resource ID of the view.
     * @param listener the click listener to attach.
     */
    public void setOnClickListener(int viewId, @NonNull OnClickListener listener) {
        View view = customView.findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(listener);
        }
    }

    /**
     * Enables a slide-up window animation on the dialog.
     */
    public void enableSlideUpAnimation() {
        setDialogAnimation(R.style.style_dialog_animation);
    }

    /**
     * Configures the dialog to appear at the bottom of the screen with transparent background.
     */
    public void enableBottomPosition() {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            LayoutParams params = dialog.getWindow().getAttributes();
            params.y = 0;
            dialog.getWindow().setAttributes(params);
            dialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
            dialog.getWindow().setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    /**
     * Sets whether the dialog can be canceled and dismissed by tapping outside its bounds.
     *
     * @param cancelable true to allow cancellation; false to make it persistent.
     */
    public void setCancelable(boolean cancelable) {
        if (dialog == null) create();
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(cancelable);
    }

    /**
     * Returns whether the dialog is currently visible to the user.
     *
     * @return true if the dialog is showing; false otherwise.
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    /**
     * Closes the dialog if it is currently visible.
     */
    public void close() {
        if (dialog != null && dialog.isShowing()) {
            dialog.cancel();
        }
    }

    /**
     * Returns the root custom view set for this dialog.
     *
     * @return the inflated or manually set custom {@link View}.
     * @throws IllegalStateException if no view has been set using {@link #setView}.
     */
    @NonNull
    public View getView() {
        if (customView == null) {
            throw new IllegalStateException("No view set. Call setView() first.");
        }
        return customView;
    }

    /**
     * Sets a layout resource as the dialog's content view.
     *
     * @param layoutResId the layout resource ID to inflate and use as the dialog's view.
     * @return the current {@link DialogBuilder} instance.
     */
    @NonNull
    public DialogBuilder setView(int layoutResId) {
        LayoutInflater inflater = from(safeActivityRef.get().getActivity());
        customView = inflater.inflate(layoutResId, null);
        return this;
    }

    /**
     * Sets a custom {@link View} as the dialog's content.
     *
     * @param view the view to display in the dialog.
     * @return the current {@link DialogBuilder} instance.
     */
    @NonNull
    public DialogBuilder setView(@NonNull View view) {
        customView = view;
        return this;
    }

    /**
     * Returns the positive button container from the dialog layout.
     *
     * @return the {@link View} containing the positive button.
     */
    @NonNull
    public View getPositiveButtonView() {
        return getView().findViewById(R.id.btn_dialog_positive_container);
    }

    /**
     * Sets a click listener on the dialog's positive button.
     *
     * @param listener the listener to execute on click.
     * @return the current {@link DialogBuilder} instance.
     */
    @NonNull
    public DialogBuilder setOnClickForPositiveButton(@NonNull OnClickListener listener) {
        getPositiveButtonView().setOnClickListener(listener);
        return this;
    }

    /**
     * Returns the negative button container from the dialog layout.
     *
     * @return the {@link View} containing the negative button.
     */
    @NonNull
    public View getNegativeButtonView() {
        return getView().findViewById(R.id.button_dialog_negative_container);
    }

    /**
     * Sets a click listener on the dialog's negative button.
     *
     * @param listener the listener to execute on click.
     * @return the current {@link DialogBuilder} instance.
     */
    @NonNull
    public DialogBuilder setOnClickForNegativeButton(@NonNull OnClickListener listener) {
        getNegativeButtonView().setOnClickListener(listener);
        return this;
    }

    /**
     * Safely retrieves the associated {@link Activity} from the weak reference.
     *
     * @return the {@link Activity} instance or null if unavailable.
     */
    @Nullable
    public Activity getActivity() {
        BaseActivityInf inf = safeActivityRef.get();
        return inf != null ? inf.getActivity() : null;
    }

    /**
     * Hides the on-screen keyboard from the specified views.
     *
     * @param focusedView one or more views currently holding input focus.
     */
    public void hideKeyboard(@NonNull View... focusedView) {
        Activity act = getActivity();
        if (act == null) return;
        for (View view : focusedView) hideOnScreenKeyboard(act, view);
    }

    /**
     * Internally creates the dialog using the custom view and default styles.
     *
     * @return the constructed {@link AlertDialog} instance.
     */
    private AlertDialog create() {
        Activity activity = getActivity();
        Builder builder = new Builder(activity, R.style.style_dialog);
        builder.setView(customView);
        dialog = builder.create();
        applyBottomPositioning();
        return dialog;
    }

    /**
     * Applies both bottom gravity and animation styles to the dialog.
     */
    private void applyBottomPositioning() {
        enableBottomPosition();
        enableSlideUpAnimation();
    }

    /**
     * Applies a custom animation style to the dialog's window.
     *
     * @param animationResId the animation resource to apply.
     */
    private void setDialogAnimation(int animationResId) {
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().getAttributes().windowAnimations = animationResId;
        }
    }

    /**
     * A listener interface for receiving cancel events from the dialog.
     */
    public interface OnCancelListener extends DialogInterface.OnCancelListener {
        @Override
        void onCancel(DialogInterface dialog);
    }
}