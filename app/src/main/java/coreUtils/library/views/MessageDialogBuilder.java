package coreUtils.library.views;

import android.app.AlertDialog;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.nextgen.R;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

public final class MessageDialogBuilder {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final WeakReference<BaseActivity<?>> weakActivityRef;
	
	private AlertDialog alertDialog;
	private View dialogRootView;
	
	public MessageDialogBuilder(@NonNull BaseActivity<?> activity) {
		this.weakActivityRef = new WeakReference<>(activity);
		initializeBaseLayout();
	}
	
	private void initializeBaseLayout() {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
		
		ViewGroup root = activity.findViewById(android.R.id.content);
		dialogRootView = activity.getLayoutInflater()
			.inflate(R.layout.dialog_normal_message_1, root, false);
		
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
		
		enableSlideUpAnimation();
		setCancelable(true);
	}
	
	@NonNull
	public MessageDialogBuilder setTitle(@NonNull String text) {
		if (dialogRootView != null) {
			TextView tvTitle = dialogRootView.findViewById(R.id.tvDialogTitle);
			if (tvTitle != null) {
				tvTitle.setText(text);
			}
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setTitle(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setTitle(activity.getString(resId));
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setMessage(@NonNull String text) {
		if (dialogRootView != null) {
			TextView tvMessage = dialogRootView.findViewById(R.id.tvDialogMessage);
			if (tvMessage != null) {
				tvMessage.setText(text);
			}
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setMessage(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setMessage(activity.getString(resId));
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setLeftButtonText(@NonNull String text) {
		if (dialogRootView != null) {
			TextView tvLeftBtn = dialogRootView.findViewById(R.id.tvDialogLeftBtn);
			if (tvLeftBtn != null) {
				tvLeftBtn.setText(text);
			}
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setLeftButtonText(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setLeftButtonText(activity.getString(resId));
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setLeftButtonIcons(
		@Nullable Drawable leftIcon, @Nullable Drawable rightIcon) {
		if (dialogRootView != null) {
			TextView tvLeftBtn = dialogRootView.findViewById(R.id.tvDialogLeftBtn);
			if (tvLeftBtn != null) {
				tvLeftBtn.setCompoundDrawablesWithIntrinsicBounds(
					leftIcon, null, rightIcon, null);
			}
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setLeftButtonIcons(
		@DrawableRes int leftResId, @DrawableRes int rightResId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			Drawable leftDrawable = leftResId != 0
				? ContextCompat.getDrawable(activity, leftResId) : null;
			Drawable rightDrawable = rightResId != 0
				? ContextCompat.getDrawable(activity, rightResId) : null;
			return setLeftButtonIcons(leftDrawable, rightDrawable);
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setOnLeftClickListener(
		@NonNull View.OnClickListener listener, boolean shouldCloseAfterExecution) {
		if (dialogRootView != null) {
			View btnContainer = dialogRootView.findViewById(R.id.btnDialogLeft);
			if (btnContainer != null) {
				btnContainer.setOnClickListener(view -> {
					listener.onClick(view);
					if (shouldCloseAfterExecution) {
						close();
					}
				});
			}
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setRightButtonText(@NonNull String text) {
		if (dialogRootView != null) {
			TextView tvRightBtn = dialogRootView.findViewById(R.id.tvDialogRightBtn);
			if (tvRightBtn != null) {
				tvRightBtn.setText(text);
			}
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setRightButtonText(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setRightButtonText(activity.getString(resId));
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setRightButtonIcons(
		@Nullable Drawable leftIcon, @Nullable Drawable rightIcon) {
		if (dialogRootView != null) {
			TextView tvRightBtn = dialogRootView.findViewById(R.id.tvDialogRightBtn);
			if (tvRightBtn != null) {
				tvRightBtn.setCompoundDrawablesWithIntrinsicBounds(
					leftIcon, null, rightIcon, null
				);
			}
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setRightButtonIcons(
		@DrawableRes int leftResId, @DrawableRes int rightResId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			Drawable leftDrawable = leftResId != 0
				? ContextCompat.getDrawable(activity, leftResId) : null;
			Drawable rightDrawable = rightResId != 0
				? ContextCompat.getDrawable(activity, rightResId) : null;
			return setRightButtonIcons(leftDrawable, rightDrawable);
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setOnRightClickListener(
		@NonNull View.OnClickListener listener, boolean shouldCloseAfterExecution) {
		if (dialogRootView != null) {
			View btnContainer = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnContainer != null) {
				btnContainer.setOnClickListener(view -> {
					listener.onClick(view);
					if (shouldCloseAfterExecution) {
						close();
					}
				});
			}
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setCancelable(boolean cancelable) {
		if (alertDialog != null) {
			alertDialog.setCancelable(cancelable);
			alertDialog.setCanceledOnTouchOutside(cancelable);
		}
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder enableBackgroundBlur(int blurRadius) {
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
	
	@NonNull
	public MessageDialogBuilder setDialogAnimation(int animationResId) {
		if (alertDialog != null && alertDialog.getWindow() != null) {
			alertDialog
				.getWindow()
				.getAttributes()
				.windowAnimations = animationResId;
		}
		return this;
	}
	
	@NotNull
	public MessageDialogBuilder enableSlideUpAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
		return this;
	}
	
	@NotNull
	public MessageDialogBuilder enableFadeInAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
		return this;
	}
	
	@NonNull
	public MessageDialogBuilder setDialogDismissListener(OnDismissListener listener) {
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
	
	@NonNull
	public MessageDialogBuilder applyBottomPositioning() {
		enableBottomPosition();
		enableSlideUpAnimation();
		return this;
	}
	
	@Nullable
	public AlertDialog getAlertDialog() {
		return alertDialog;
	}
	
	@Nullable
	public BaseActivity<?> getActivity() {
		return weakActivityRef != null ? weakActivityRef.get() : null;
	}
	
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
	
	private void clearAllLayoutListeners(@Nullable View view) {
		if (view == null) return;
		view.setOnClickListener(null);
		if (view instanceof ViewGroup group) {
			for (int index = 0; index < group.getChildCount(); index++) {
				clearAllLayoutListeners(group.getChildAt(index));
			}
		}
	}
	
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
