package lib.ui.listeners

import android.text.Editable
import android.text.TextWatcher

/**
 * A simplified abstract implementation of [TextWatcher] to observe text changes in an EditText.
 *
 * This class provides default empty implementations for `beforeTextChanged` and `onTextChanged`,
 * so you only need to override and implement the `afterTextChanged` method.
 *
 * Use this listener when you're only interested in reacting to text changes **after** they occur.
 *
 * Example usage:
 * ```
 * editText.addTextChangedListener(object : EditTextListener() {
 *     override fun afterTextChanged(editable: Editable) {
 *         // Handle updated text
 *     }
 * })
 * ```
 */
abstract class EditTextListener : TextWatcher {
	
	/**
	 * Called after the text is changed. Must be implemented by subclasses.
	 *
	 * @param editable The text after the change.
	 */
	abstract override fun afterTextChanged(editable: Editable)
	
	/**
	 * Called to notify you that the characters within `charSequence` are about to be replaced
	 * with new text. Default implementation is empty.
	 *
	 * @param charSequence The text before the change.
	 * @param start The start position of the change.
	 * @param count The number of characters about to be replaced.
	 * @param after The number of characters that will replace the old text.
	 */
	override fun beforeTextChanged(
		charSequence: CharSequence,
		start: Int, count: Int, after: Int
	) = Unit
	
	/**
	 * Called to notify you that somewhere within `charSequence`, the text has been replaced.
	 * Default implementation is empty.
	 *
	 * @param charSequence The updated text.
	 * @param start The start position of the change.
	 * @param before The number of characters replaced.
	 * @param count The number of new characters added.
	 */
	override fun onTextChanged(
		charSequence: CharSequence,
		start: Int, before: Int, count: Int
	) = Unit
}
