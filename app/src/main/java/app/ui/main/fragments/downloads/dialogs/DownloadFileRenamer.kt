package app.ui.main.fragments.downloads.dialogs

import android.view.View
import android.widget.EditText
import androidx.documentfile.provider.DocumentFile.fromFile
import app.core.AIOApp.Companion.downloadSystem
import app.core.engines.downloader.DownloadDataModel
import app.ui.main.MotherActivity
import com.aio.R
import com.aio.R.id
import com.aio.R.layout
import com.aio.R.string
import lib.files.FileUtility.getFileExtension
import lib.files.FileUtility.getFileNameWithoutExtension
import lib.files.FileUtility.isFileNameValid
import lib.files.FileUtility.sanitizeFileNameExtreme
import lib.files.FileUtility.sanitizeFileNameNormal
import lib.process.AsyncJobUtils.executeInBackground
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.CommonTimeUtils
import lib.texts.CommonTextUtils.cutTo60Chars
import lib.texts.CommonTextUtils.getText
import lib.texts.CommonTextUtils.removeEmptyLines
import lib.ui.MsgDialogUtils.showMessageDialog
import lib.ui.ViewUtility
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.builders.DialogBuilder
import lib.ui.builders.ToastView.Companion.showToast
import java.io.File
import java.lang.ref.WeakReference

/**
 * A dialog component responsible for renaming a downloaded file.
 *
 * It ensures proper validation, sanitization of the user input file name,
 * checks for name collisions, and updates the download task accordingly.
 *
 * @param motherActivity The hosting activity from which this dialog is invoked.
 * @param downloadDataModel The model containing the current download data.
 * @param oneDone Callback invoked after a successful rename operation.
 */
class DownloadFileRenamer(
	val motherActivity: MotherActivity?,
	var downloadDataModel: DownloadDataModel,
	val oneDone: () -> Unit
) {
	
	// Safe reference to the activity to prevent memory leaks
	val safeMotherActivityRef = WeakReference(motherActivity).get()
	
	// Builder used to construct and show the renaming dialog
	val dialogBuilder: DialogBuilder = DialogBuilder(safeMotherActivityRef)
	
	init {
		// Only proceed if activity reference is still valid
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			dialogBuilder.apply {
				setView(layout.frag_down_2_file_renamer_1)
				setOnClickForPositiveButton {
					val userGivenText = view.findViewById<EditText>(id.edit_field_file_name).text.toString()
					
					// Show error if file name is empty
					if (userGivenText.isEmpty()) {
						safeMotherActivityRef.doSomeVibration(50)
						showToast(getText(string.text_file_name_must_not_be_empty))
						return@setOnClickForPositiveButton
					}
					
					try {
						val fileDirectory = downloadDataModel.fileDirectory
						
						// Sanitize and finalize the file name
						sanitizedFileName(fileDirectory, userGivenText) { sanitizedName ->
							val fileExtension = getFileExtension(downloadDataModel.fileName)
							val generatedFileName = "$sanitizedName.$fileExtension"
							
							// Perform the actual rename operation
							renameDownloadTask(downloadDataModel, generatedFileName) {
								dialogBuilder.close()
								oneDone()
							}
						}
					} catch (error: Exception) {
						// Show a fallback dialog on exception
						error.printStackTrace()
						safeMotherActivityRef.doSomeVibration(50)
						showMessageDialog(
							baseActivityInf = safeMotherActivityRef,
							messageTxt = getText(string.text_error_advice_on_renaming_download_file),
							negativeButtonTextCustomize = { it.setLeftSideDrawable(R.drawable.ic_button_cancel) },
							positiveButtonTextCustomize = { it.setLeftSideDrawable(R.drawable.ic_button_checked_circle) })
						return@setOnClickForPositiveButton
					}
				}
			}
		}
	}
	
	/**
	 * Displays the rename dialog and initializes the input field.
	 *
	 * @param downloadModel The download model to operate on.
	 */
	fun show(downloadModel: DownloadDataModel) {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			this.downloadDataModel = downloadModel
			dialogBuilder.apply {
				val editTextFiledContainer = view.findViewById<View>(id.edit_field_file_name_container)
				val editTextFiled = view.findViewById<EditText>(id.edit_field_file_name)
				
				// Pre-fill file name without extension
				editTextFiled.setText(getFileNameWithoutExtension(downloadModel.fileName))
				editTextFiledContainer.setOnClickListener { editTextFiled.requestFocus() }
				
				show()
				
				// Automatically select text and show keyboard after delay
				CommonTimeUtils.delay(200, object : CommonTimeUtils.OnTaskFinishListener {
					override fun afterDelay() {
						editTextFiled.requestFocus()
						editTextFiled.selectAll()
						ViewUtility.showOnScreenKeyboard(safeMotherActivityRef, editTextFiled)
					}
				})
			}
		}
	}
	
	/**
	 * Sanitizes the given file name and ensures it is unique within the directory.
	 *
	 * @param directory The target directory to check for existing file names.
	 * @param fileName The user input file name to sanitize.
	 * @param onSanitized Callback with the final sanitized and unique file name.
	 */
	private fun sanitizedFileName(
		directory: String,
		fileName: String,
		onSanitized: (sanitizedName: String) -> Unit) {
		executeInBackground {
			var sanitizedFileName = sanitizeFileNameNormal(fileName)
			
			// Use extreme sanitization if normal is not valid
			if (!isFileNameValid(sanitizedFileName))
				sanitizedFileName = sanitizeFileNameExtreme(fileName)
			
			val removedDoubleSlashes = removeEmptyLines(sanitizedFileName)
			sanitizedFileName = cutTo60Chars(removedDoubleSlashes ?: "") ?: ""
			
			// Ensure file name is unique by prepending a numeric index if needed
			var index: Int
			val regex = Regex("^(\\d+)_")
			while (fromFile(File(directory, sanitizedFileName)).exists()) {
				val matchResult = regex.find(sanitizedFileName)
				if (matchResult != null) {
					val currentIndex = matchResult.groupValues[1].toInt()
					sanitizedFileName = sanitizedFileName.replaceFirst(regex, "")
					index = currentIndex + 1
				} else {
					index = 1
				}
				sanitizedFileName = "${index}_${sanitizedFileName}"
			}
			executeOnMainThread { onSanitized(sanitizedFileName) }
		}
	}
	
	/**
	 * Renames the actual download file and updates the model.
	 *
	 * @param model The download model being renamed.
	 * @param sanitizedName The final, sanitized file name.
	 * @param onDone Callback after the renaming operation is completed.
	 */
	private fun renameDownloadTask(
		model: DownloadDataModel,
		sanitizedName: String,
		onDone: () -> Unit
	) {
		val isRunningTask = model.isRunning
		downloadSystem.pauseDownload(model)
		
		executeInBackground {
			// Rename physical file
			model.getDestinationDocumentFile().renameTo(sanitizedName)
			model.fileName = sanitizedName
			
			// Update associated video info if applicable
			if (model.videoInfo != null && model.videoFormat != null) {
				model.videoInfo!!.videoTitle = model.fileName
			}
			
			// Persist changes
			model.updateInStorage()
			downloadSystem.downloadsUIManager.updateActiveUI(model)
			
			// Resume if it was running before
			if (isRunningTask) downloadSystem.resumeDownload(model)
			onDone()
		}
	}
}