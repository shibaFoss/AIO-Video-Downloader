package lib.device

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.createChooser
import android.net.Uri
import android.webkit.MimeTypeMap.getSingleton
import androidx.core.content.FileProvider.getUriForFile
import androidx.documentfile.provider.DocumentFile
import app.core.bases.BaseActivity
import com.aio.R
import lib.texts.CommonTextUtils.getText
import lib.ui.builders.ToastView.Companion.showToast
import java.io.File
import java.lang.ref.WeakReference

/**
 * A utility object for sharing and opening files, URLs, media, and text using Android Intents.
 */
object ShareUtility {
	
	/**
	 * Shares a plain text URL using Android's share intent chooser.
	 *
	 * @param context The context to use for launching the intent.
	 * @param fileURL The URL string to share.
	 * @param titleText Title to show on the chooser dialog.
	 * @param onDone Callback to invoke after the intent is started.
	 */
	@JvmStatic
	fun shareUrl(
		context: Context?, fileURL: String,
		titleText: String = "Share", onDone: () -> Unit = {}) {
		WeakReference(context).get()?.let { safeContextRef ->
			Intent(ACTION_SEND).apply {
				type = "text/plain"
				putExtra(EXTRA_TEXT, fileURL)
				safeContextRef.startActivity(createChooser(this, titleText))
				onDone()
			}
		}
	}
	
	/**
	 * Shares a [DocumentFile] via a content URI with read permission.
	 *
	 * @param context The context to use.
	 * @param documentFile The document to share.
	 * @param titleText The chooser title text.
	 * @param onDone Callback after intent is launched.
	 */
	@JvmStatic
	fun shareDocumentFile(
		context: Context?, documentFile: DocumentFile,
		titleText: String = "Share", onDone: () -> Unit = {}) {
		WeakReference(context).get()?.let { safeContextRef ->
			val file = File(documentFile.uri.path ?: return)
			val fileUri: Uri = getUriForFile(safeContextRef,
				"${safeContextRef.packageName}.provider", file)
			
			Intent(ACTION_SEND).apply {
				type = safeContextRef.contentResolver.getType(documentFile.uri)
				putExtra(EXTRA_STREAM, fileUri)
				addFlags(FLAG_GRANT_READ_URI_PERMISSION)
				safeContextRef.startActivity(createChooser(this, titleText))
				onDone()
			}
		}
	}
	
	/**
	 * Opens a file with an appropriate app based on its MIME type.
	 *
	 * @param file The file to open.
	 * @param context The context to use.
	 */
	@JvmStatic
	fun openFile(file: File, context: Context?) {
		WeakReference(context).get()?.let { safeContextRef ->
			val fileUri: Uri = getUriForFile(safeContextRef,
				"${safeContextRef.packageName}.provider", file)
			val mimeType = getSingleton().getMimeTypeFromExtension(file.extension) ?: "*/*"
			
			val openFileIntent = Intent(ACTION_VIEW).apply {
				setDataAndType(fileUri, mimeType)
				addFlags(FLAG_GRANT_READ_URI_PERMISSION)
			}
			
			if (openFileIntent.resolveActivity(safeContextRef.packageManager) != null) {
				safeContextRef.startActivity(openFileIntent)
			} else {
				showToast(getText(R.string.txt_no_app_found_to_open_this_file))
			}
		}
	}
	
	/**
	 * Shares a media file such as audio, video, or image using content URI.
	 *
	 * @param context The context to use.
	 * @param file The file to share.
	 */
	@JvmStatic
	fun shareMediaFile(context: Context?, file: File) {
		WeakReference(context).get()?.let { safeContextRef ->
			try {
				val fileUri: Uri = getUriForFile(safeContextRef,
					"${safeContextRef.packageName}.provider", file)
				val shareIntent = Intent(ACTION_SEND).apply {
					type = safeContextRef.contentResolver.getType(fileUri) ?: "audio/*"
					putExtra(EXTRA_STREAM, fileUri)
					addFlags(FLAG_GRANT_READ_URI_PERMISSION)
				}
				val intentChooser = createChooser(shareIntent, getText(R.string.text_sharing_media_file))
				safeContextRef.startActivity(intentChooser)
			} catch (error: Exception) {
				error.printStackTrace()
			}
		}
	}
	
	/**
	 * Shares a video file via a [DocumentFile] using a generic video MIME type.
	 *
	 * @param context The context to use.
	 * @param videoFile The video file as a DocumentFile.
	 * @param title Chooser title text.
	 * @param onDone Callback after the intent is launched.
	 */
	@JvmStatic
	fun shareVideo(
		context: Context?, videoFile: DocumentFile,
		title: String = "Share", onDone: () -> Unit = {}) {
		WeakReference(context).get()?.let { safeContextRef ->
			Intent(ACTION_SEND).apply {
				val videoUri = videoFile.uri
				type = "video/*"
				putExtra(EXTRA_STREAM, videoUri)
				addFlags(FLAG_GRANT_READ_URI_PERMISSION)
				safeContextRef.startActivity(createChooser(this, title))
				onDone()
			}
		}
	}
	
	/**
	 * Shares plain text using Android's share intent.
	 *
	 * @param context The context to use.
	 * @param text The text to share.
	 * @param title The chooser title text.
	 * @param onDone Callback after the intent is launched.
	 */
	@JvmStatic
	fun shareText(
		context: Context?, text: String,
		title: String = "Share", onDone: () -> Unit = {}
	) {
		WeakReference(context).get()?.let { safeContextRef ->
			Intent(ACTION_SEND).apply {
				type = "text/plain"
				putExtra(EXTRA_TEXT, text)
				safeContextRef.startActivity(createChooser(this, title))
				onDone()
			}
		}
	}
	
	/**
	 * Opens an APK file for installation using the system package installer.
	 *
	 * @param baseActivity The activity context to use.
	 * @param apkFile The APK file to install.
	 * @param authority The FileProvider authority declared in manifest.
	 */
	@JvmStatic
	fun openApkFile(baseActivity: BaseActivity?, apkFile: File, authority: String) {
		WeakReference(baseActivity).get()?.let { safeContextRef ->
			val intent = Intent(ACTION_VIEW).apply {
				flags = Intent.FLAG_ACTIVITY_NEW_TASK or FLAG_GRANT_READ_URI_PERMISSION
				val apkUri: Uri = getUriForFile(safeContextRef, authority, apkFile)
				setDataAndType(apkUri, "application/vnd.android.package-archive")
			}
			safeContextRef.startActivity(intent)
		}
	}
}