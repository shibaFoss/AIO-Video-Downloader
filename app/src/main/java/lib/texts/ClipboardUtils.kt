package lib.texts

import android.content.ClipData.newHtmlText
import android.content.ClipData.newPlainText
import android.content.ClipboardManager
import android.content.ClipboardManager.OnPrimaryClipChangedListener
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import java.lang.ref.WeakReference

/**
 * Utility object for interacting with the system clipboard.
 *
 * This object provides static helper methods to read, write, clear, append, and monitor clipboard content,
 * including support for plain text and HTML formats. It safely manages the `Context` using weak references
 * to avoid memory leaks.
 */
object ClipboardUtils {
	
	/**
	 * Clears the system clipboard by setting an empty plain text clip.
	 *
	 * @param context The context used to access the clipboard service.
	 */
	@JvmStatic
	fun clearClipboard(context: Context?) {
		WeakReference(context).get()?.let {
			(it.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).apply {
				setPrimaryClip(newPlainText("", ""))
			}
		}
	}
	
	/**
	 * Checks if the clipboard currently contains any non-empty plain text.
	 *
	 * @param context The context used to access the clipboard service.
	 * @return `true` if there is non-empty text in the clipboard, otherwise `false`.
	 */
	@JvmStatic
	fun hasTextInClipboard(context: Context?): Boolean {
		return WeakReference(context).get()?.let {
			val clipboard = it.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
			clipboard.primaryClip?.takeIf { clip ->
				clip.itemCount > 0 && clip.getItemAt(0).text?.isNotEmpty() == true
			} != null
		} ?: false
	}
	
	/**
	 * Retrieves the HTML content from the clipboard, if available.
	 *
	 * @param context The context used to access the clipboard service.
	 * @return The HTML content as a [String], or an empty string if not available.
	 */
	@JvmStatic
	fun getHtmlFromClipboard(context: Context?): String {
		return WeakReference(context).get()?.let {
			val clipboard = it.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
			clipboard.primaryClip?.takeIf { clip -> clip.itemCount > 0 }?.getItemAt(0)?.htmlText ?: ""
		} ?: ""
	}
	
	/**
	 * Copies the provided HTML content to the clipboard.
	 *
	 * @param context The context used to access the clipboard service.
	 * @param html The HTML content to copy. Ignored if null or empty.
	 */
	@JvmStatic
	fun copyHtmlToClipboard(context: Context?, html: String?) {
		html?.takeIf { it.isNotEmpty() }?.let { validHtml ->
			WeakReference(context).get()?.let {
				(it.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).apply {
					setPrimaryClip(newHtmlText("html_clip", validHtml, validHtml))
				}
			}
		}
	}
	
	/**
	 * Appends the provided text to the current clipboard content.
	 *
	 * @param context The context used to access the clipboard service.
	 * @param text The text to append. Ignored if null or empty.
	 */
	@JvmStatic
	fun appendTextToClipboard(context: Context?, text: String?) {
		text?.takeIf { it.isNotEmpty() }?.let { validText ->
			WeakReference(context).get()?.let { ctx ->
				val current = getTextFromClipboard(ctx)
				copyTextToClipboard(ctx, current + validText)
			}
		}
	}
	
	/**
	 * Retrieves plain text content from the clipboard.
	 *
	 * @param context The context used to access the clipboard service.
	 * @return The plain text content as a [String], or an empty string if not available.
	 */
	@JvmStatic
	fun getTextFromClipboard(context: Context?): String {
		return WeakReference(context).get()?.let {
			val clipboard = it.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
			clipboard.primaryClip?.takeIf { clip ->
				clip.itemCount > 0
			}?.getItemAt(0)?.text?.toString() ?: ""
		} ?: ""
	}
	
	/**
	 * Copies plain text to the clipboard.
	 *
	 * @param context The context used to access the clipboard service.
	 * @param text The plain text to copy. Ignored if null or empty.
	 */
	@JvmStatic
	fun copyTextToClipboard(context: Context?, text: String?) {
		text?.takeIf { it.isNotEmpty() }?.let { validText ->
			WeakReference(context).get()?.let {
				(it.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).apply {
					setPrimaryClip(newPlainText("text_clip", validText))
				}
			}
		}
	}
	
	/**
	 * Registers a clipboard listener to be notified when the primary clip changes.
	 *
	 * @param context The context used to access the clipboard service.
	 * @param listener The listener to be registered. Ignored if null.
	 */
	@JvmStatic
	fun setClipboardListener(
		context: Context?,
		listener: OnPrimaryClipChangedListener? = null
	) {
		listener?.let { validListener ->
			WeakReference(context).get()?.let {
				(it.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).apply {
					addPrimaryClipChangedListener(validListener)
				}
			}
		}
	}
	
	/**
	 * Unregisters a clipboard listener from the clipboard manager.
	 *
	 * @param context The context used to access the clipboard service.
	 * @param listener The listener to be removed. Ignored if null.
	 */
	@JvmStatic
	fun removeClipboardListener(
		context: Context?,
		listener: OnPrimaryClipChangedListener? = null
	) {
		listener?.let { validListener ->
			WeakReference(context).get()?.let {
				(it.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).apply {
					removePrimaryClipChangedListener(validListener)
				}
			}
		}
	}
}
