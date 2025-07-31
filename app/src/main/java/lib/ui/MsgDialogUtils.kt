package lib.ui

import android.content.Context
import android.view.View
import android.view.View.GONE
import android.view.View.OnClickListener
import android.widget.RelativeLayout
import android.widget.TextView
import app.core.AIOApp
import app.core.AIOApp.Companion.INSTANCE
import app.core.bases.interfaces.BaseActivityInf
import com.aio.R
import lib.texts.CommonTextUtils.getText
import lib.ui.builders.DialogBuilder
import java.lang.ref.WeakReference

/**
 * A utility object for displaying customizable message dialogs in the application.
 *
 * This object provides methods to create and show dialogs with various options such as
 * custom titles, messages, button texts, and more. The dialog can be customized through various
 * callback functions for styling the views and handling button click actions.
 */
object MsgDialogUtils {
	
	/**
	 * The application context reference, retrieved through [AIOApp.INSTANCE].
	 */
	private val applicationContext: Context
		get() = INSTANCE
	
	/**
	 * Displays a customizable message dialog with the specified options.
	 *
	 * @param baseActivityInf The base activity interface, used to access the activity context.
	 * @param isCancelable Whether the dialog can be dismissed by clicking outside of it. Default is true.
	 * @param isTitleVisible Whether the dialog's title should be visible. Default is false.
	 * @param titleText The title text for the dialog. Default is a placeholder text.
	 * @param messageTxt The message text to be displayed in the dialog. Default is a placeholder text.
	 * @param positiveButtonText Text for the positive button. Default is "OK".
	 * @param negativeButtonText Text for the negative button. Default is "Cancel".
	 * @param isNegativeButtonVisible Whether the negative button should be visible. Default is true.
	 * @param onPositiveButtonClickListener The click listener for the positive button.
	 * @param onNegativeButtonClickListener The click listener for the negative button.
	 * @param messageTextViewCustomize A callback for customizing the message TextView.
	 * @param titleTextViewCustomize A callback for customizing the title TextView.
	 * @param dialogBuilderCustomize A callback for customizing the DialogBuilder.
	 * @param positiveButtonTextCustomize A callback for customizing the positive button TextView.
	 * @param negativeButtonTextCustomize A callback for customizing the negative button TextView.
	 * @param positiveButtonContainerCustomize A callback for customizing the positive button container.
	 * @param negativeButtonContainerCustomize A callback for customizing the negative button container.
	 * @return The created [DialogBuilder] instance, which can be used to show the dialog.
	 */
	@JvmStatic
	fun showMessageDialog(
		baseActivityInf: BaseActivityInf?,
		isCancelable: Boolean = true,
		isTitleVisible: Boolean = false,
		titleText: CharSequence = getText(R.string.text_title_goes_here),
		messageTxt: CharSequence = applicationContext.getString(R.string.title_message_goes_here),
		positiveButtonText: CharSequence = applicationContext.getString(R.string.title_okay),
		negativeButtonText: CharSequence = applicationContext.getString(R.string.title_cancel),
		isNegativeButtonVisible: Boolean = true,
		onPositiveButtonClickListener: OnClickListener? = null,
		onNegativeButtonClickListener: OnClickListener? = null,
		messageTextViewCustomize: ((TextView) -> Unit)? = {},
		titleTextViewCustomize: ((TextView) -> Unit)? = {},
		dialogBuilderCustomize: ((DialogBuilder) -> Unit)? = {},
		positiveButtonTextCustomize: ((TextView) -> Unit)? = {},
		negativeButtonTextCustomize: ((TextView) -> Unit)? = {},
		positiveButtonContainerCustomize: ((RelativeLayout) -> Unit)? = {},
		negativeButtonContainerCustomize: ((RelativeLayout) -> Unit)? = {}
	): DialogBuilder? {
		val dialogBuilder = getMessageDialog(
			baseActivityInf = baseActivityInf,
			isCancelable = isCancelable,
			isTitleVisible = isTitleVisible,
			titleText = titleText,
			messageTxt = messageTxt,
			positiveButtonText = positiveButtonText,
			negativeButtonText = negativeButtonText,
			isNegativeButtonVisible = isNegativeButtonVisible,
			onPositiveButtonClickListener = onPositiveButtonClickListener,
			onNegativeButtonClickListener = onNegativeButtonClickListener,
			messageTextViewCustomize = messageTextViewCustomize,
			titleTextViewCustomize = titleTextViewCustomize,
			dialogBuilderCustomize = dialogBuilderCustomize,
			positiveButtonTextCustomize = positiveButtonTextCustomize,
			positiveButtonContainerCustomize = positiveButtonContainerCustomize,
			negativeButtonTextCustomize = negativeButtonTextCustomize,
			negativeButtonContainerCustomize = negativeButtonContainerCustomize
		)
		dialogBuilder?.show()
		return dialogBuilder
	}
	
	/**
	 * Constructs a customizable message dialog and returns the [DialogBuilder] instance for further manipulation.
	 *
	 * @param baseActivityInf The base activity interface, used to access the activity context.
	 * @param isCancelable Whether the dialog can be dismissed by clicking outside of it. Default is true.
	 * @param isTitleVisible Whether the dialog's title should be visible. Default is false.
	 * @param titleText The title text for the dialog. Default is a placeholder text.
	 * @param messageTxt The message text to be displayed in the dialog. Default is a placeholder text.
	 * @param positiveButtonText Text for the positive button. Default is "OK".
	 * @param negativeButtonText Text for the negative button. Default is "Cancel".
	 * @param isNegativeButtonVisible Whether the negative button should be visible. Default is true.
	 * @param onPositiveButtonClickListener The click listener for the positive button.
	 * @param onNegativeButtonClickListener The click listener for the negative button.
	 * @param messageTextViewCustomize A callback for customizing the message TextView.
	 * @param titleTextViewCustomize A callback for customizing the title TextView.
	 * @param dialogBuilderCustomize A callback for customizing the DialogBuilder.
	 * @param positiveButtonTextCustomize A callback for customizing the positive button TextView.
	 * @param negativeButtonTextCustomize A callback for customizing the negative button TextView.
	 * @param positiveButtonContainerCustomize A callback for customizing the positive button container.
	 * @param negativeButtonContainerCustomize A callback for customizing the negative button container.
	 * @return The created [DialogBuilder] instance, which can be used to show the dialog.
	 */
	@JvmStatic
	fun getMessageDialog(
		baseActivityInf: BaseActivityInf?,
		isCancelable: Boolean = true,
		isTitleVisible: Boolean = false,
		titleText: CharSequence = getText(R.string.text_title_goes_here),
		messageTxt: CharSequence = getText(R.string.title_message_goes_here),
		positiveButtonText: CharSequence = INSTANCE.getString(R.string.title_okay),
		negativeButtonText: CharSequence = INSTANCE.getString(R.string.title_cancel),
		isNegativeButtonVisible: Boolean = true,
		onPositiveButtonClickListener: OnClickListener? = null,
		onNegativeButtonClickListener: OnClickListener? = null,
		messageTextViewCustomize: ((TextView) -> Unit)? = {},
		titleTextViewCustomize: ((TextView) -> Unit)? = {},
		dialogBuilderCustomize: ((DialogBuilder) -> Unit)? = {},
		positiveButtonTextCustomize: ((TextView) -> Unit)? = {},
		negativeButtonTextCustomize: ((TextView) -> Unit)? = {},
		positiveButtonContainerCustomize: ((RelativeLayout) -> Unit)? = {},
		negativeButtonContainerCustomize: ((RelativeLayout) -> Unit)? = {},
	): DialogBuilder? {
		return WeakReference(baseActivityInf).get()?.getActivity()?.let { safeContextRef ->
			DialogBuilder(safeContextRef).apply {
				// View Configuration
				setView(R.layout.dialog_basic_message_1)
				setCancelable(isCancelable)
				
				// Component References
				val titleTextView = view.findViewById<TextView>(R.id.txt_dialog_title)
				val messageTextView = view.findViewById<TextView>(R.id.txt_dialog_message)
				val btnNegativeTextView = view.findViewById<TextView>(R.id.button_dialog_negative)
				val btnNegativeContainer = view.findViewById<RelativeLayout>(R.id.button_dialog_negative_container)
				val btnPositiveTextView = view.findViewById<TextView>(R.id.btn_dialog_positive)
				val btnPositiveContainer = view.findViewById<RelativeLayout>(R.id.btn_dialog_positive_container)
				
				// Set Text Content
				titleTextView.text = titleText
				messageTextView.text = messageTxt
				btnPositiveTextView.text = positiveButtonText
				btnNegativeTextView.text = negativeButtonText
				
				// Apply Custom Styling
				messageTextViewCustomize?.invoke(messageTextView)
				titleTextViewCustomize?.invoke(titleTextView)
				dialogBuilderCustomize?.invoke(this)
				positiveButtonTextCustomize?.invoke(btnPositiveTextView)
				positiveButtonContainerCustomize?.invoke(btnPositiveContainer)
				negativeButtonTextCustomize?.invoke(btnNegativeTextView)
				negativeButtonContainerCustomize?.invoke(btnNegativeContainer)
				
				// Set Visibility Rules
				btnNegativeTextView.visibility = if (isNegativeButtonVisible) View.VISIBLE else GONE
				btnNegativeContainer.visibility = if (isNegativeButtonVisible) View.VISIBLE else GONE
				
				titleTextView.visibility = when {
					!isTitleVisible -> GONE
					titleTextView.text.toString() == getText(R.string.text_title_goes_here) -> GONE
					else -> View.VISIBLE
				}
				
				// Set Click Handling
				btnNegativeContainer.setOnClickListener(onNegativeButtonClickListener ?: OnClickListener { close() })
				btnPositiveContainer.setOnClickListener(onPositiveButtonClickListener ?: OnClickListener { close() })
			}
		}
	}
}