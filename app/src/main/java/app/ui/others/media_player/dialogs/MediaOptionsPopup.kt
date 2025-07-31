package app.ui.others.media_player.dialogs

import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.downloadSystem
import app.core.engines.downloader.DownloadDataModel
import app.ui.others.media_player.MediaPlayerActivity
import com.aio.R
import com.aio.R.layout
import com.aio.R.string
import lib.device.DateTimeUtils.millisToDateTimeString
import lib.files.FileSystemUtility
import lib.files.VideoToAudioConverter
import lib.files.VideoToAudioConverter.ConversionListener
import lib.networks.DownloaderUtils.getHumanReadableFormat
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.CopyObjectUtils.deepCopy
import lib.process.ThreadsUtility
import lib.process.UniqueNumberUtils.getUniqueNumberForDownloadModels
import lib.texts.CommonTextUtils.getText
import lib.ui.MsgDialogUtils.getMessageDialog
import lib.ui.MsgDialogUtils.showMessageDialog
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.ViewUtility.setTextColorKT
import lib.ui.builders.PopupBuilder
import lib.ui.builders.ToastView.Companion.showToast
import lib.ui.builders.WaitingDialog
import java.io.File
import java.lang.ref.WeakReference

/**
 * A popup dialog that provides various media options for the currently playing media file.
 *
 * This class handles operations like:
 * - Deleting the media file
 * - Converting video to audio
 * - Opening the media in another app
 * - Showing media information
 * - Discovering related content
 *
 * @property mediaPlayerActivity The parent MediaPlayerActivity instance (held weakly to prevent leaks)
 */
@UnstableApi
class MediaOptionsPopup(private val mediaPlayerActivity: MediaPlayerActivity?) {
    
    // Weak reference to prevent memory leaks
    private val safeMediaPlayerActivityRef = WeakReference(mediaPlayerActivity).get()
    private lateinit var popupBuilder: PopupBuilder
    
    init {
        setupPopupBuilder()
        setupClickEvents()
    }
    
    /**
     * Displays the media options popup.
     */
    fun show() {
        popupBuilder.show()
    }
    
    /**
     * Closes the media options popup.
     */
    fun close() {
        popupBuilder.close()
    }
    
    /**
     * Initializes the popup builder with the appropriate layout and anchor view.
     */
    private fun setupPopupBuilder() {
        safeMediaPlayerActivityRef?.let { safeActivityRef ->
            popupBuilder = PopupBuilder(
                activityInf = safeActivityRef,
                popupLayoutId = layout.activity_player_5_options,
                popupAnchorView = safeActivityRef.buttonOptionActionbar
            )
        }
    }
    
    /**
     * Sets up click listeners for all options in the popup.
     */
    private fun setupClickEvents() {
        safeMediaPlayerActivityRef?.let { _ ->
            with(popupBuilder.getPopupView()) {
                // Map of view IDs to their corresponding actions
                mapOf(
                    R.id.button_delete_file to { close(); deleteFile() },
                    R.id.button_convert_to_audio to { close(); convertAudio() },
                    R.id.button_open_in_another to { close(); openMediaFile() },
                    R.id.button_media_info to { close(); openMediaFileInfo() },
                    R.id.button_discover_video to { close(); discoverMore() }
                ).forEach { (id, action) ->
                    setClickListener(id) { action() }
                }
            }
        }
    }
    
    /**
     * Helper function to set click listeners on views.
     *
     * @param id The view ID to set the listener on
     * @param action The action to perform when clicked
     */
    private fun View.setClickListener(id: Int, action: () -> Unit) {
        findViewById<View>(id)?.setOnClickListener { action() }
    }
    
    /**
     * Handles the file deletion operation with confirmation dialog.
     */
    private fun deleteFile() {
        safeMediaPlayerActivityRef?.let { safeActivityRef ->
            // Check if currently playing streaming video (cannot delete)
            if (safeActivityRef.isPlayingStreamingVideo()) {
                showMessageDialog(
                    baseActivityInf = safeActivityRef,
                    isTitleVisible = true,
                    isNegativeButtonVisible = false,
                    titleTextViewCustomize = { titleView ->
                        titleView.setText(string.text_unavailable_for_streaming)
                        titleView.setTextColorKT(R.color.color_error)
                    },
                    positiveButtonTextCustomize = { positiveButton ->
                        positiveButton.setLeftSideDrawable(R.drawable.ic_okay_done)
                        positiveButton.setText(string.title_okay)
                    },
                    messageTextViewCustomize = { it.setText(string.text_delete_stream_media_unavailable) }
                ); return
            }
            
            // Show confirmation dialog for deletion
            val dialogBuilder = getMessageDialog(
                baseActivityInf = safeActivityRef,
                titleText = getText(string.title_are_you_sure_about_this),
                isTitleVisible = true,
                isNegativeButtonVisible = false,
                positiveButtonText = getText(string.title_delete_file),
                messageTextViewCustomize = { it.setText(string.text_are_you_sure_about_delete) },
                negativeButtonTextCustomize = { it.setLeftSideDrawable(R.drawable.ic_button_cancel) },
                positiveButtonTextCustomize = { it.setLeftSideDrawable(R.drawable.ic_button_delete) }
            )
            
            dialogBuilder?.setOnClickForPositiveButton {
                dialogBuilder.close()
                this@MediaOptionsPopup.close()
                safeActivityRef.deleteMediaFile()
            }
            
            dialogBuilder?.show()
        }
    }
    
    /**
     * Opens the media file information dialog.
     */
    private fun openMediaFileInfo() {
        safeMediaPlayerActivityRef?.openMediaFileInfo()
    }
    
    /**
     * Handles the "discover more" action by opening the referrer URL.
     */
    private fun discoverMore() {
        safeMediaPlayerActivityRef?.let { playerActivityRef ->
            // Check if currently playing streaming video (cannot discover)
            if (playerActivityRef.isPlayingStreamingVideo()) {
                showMessageDialog(
                    baseActivityInf = playerActivityRef,
                    isTitleVisible = true,
                    isNegativeButtonVisible = false,
                    titleTextViewCustomize = { titleView ->
                        titleView.setText(string.text_unavailable_for_streaming)
                        titleView.setTextColorKT(R.color.color_error)
                    }, messageTextViewCustomize = { msgTextView ->
                        msgTextView.setText(string.text_no_discovery_during_streaming)
                    }, positiveButtonTextCustomize = { positiveButton ->
                        positiveButton.setLeftSideDrawable(R.drawable.ic_okay_done)
                        positiveButton.setText(string.title_okay)
                    }
                ); return
            }
            
            // Try to open the referrer URL in a browser
            playerActivityRef.getCurrentPlayingDownloadModel()?.siteReferrer?.let { referrer ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, referrer.toUri())
                    playerActivityRef.startActivity(intent)
                } catch (error: Exception) {
                    error.printStackTrace()
                    showToast(msgId = string.text_no_app_can_handle_this_request)
                }
            }
        }
    }
    
    /**
     * Converts the current video file to audio format.
     */
    private fun convertAudio() {
        safeMediaPlayerActivityRef?.let { safeActivityRef ->
            val videoToAudioConverter = VideoToAudioConverter()
            // Setup waiting dialog with progress updates
            val waitingDialog = WaitingDialog(
                baseActivityInf = safeActivityRef,
                loadingMessage = getText(string.text_converting_audio_progress_0),
                isCancelable = false,
                shouldHideOkayButton = false
            )
            
            var messageTextView: TextView? = null
            waitingDialog.dialogBuilder?.view?.apply {
                messageTextView = findViewById(R.id.text_progress_info)
                findViewById<TextView>(R.id.btn_dialog_positive)?.apply {
                    this.setText(string.text_cancel_converting)
                    this.setLeftSideDrawable(R.drawable.ic_button_cancel)
                }
                
                findViewById<View>(R.id.btn_dialog_positive_container)
                    ?.setOnClickListener { videoToAudioConverter.cancel(); waitingDialog.close() }
            }
            
            safeActivityRef.pausePlayer()
            waitingDialog.show()
            
            val downloadModel = safeActivityRef.getCurrentPlayingDownloadModel()
            // Perform conversion in background thread
            ThreadsUtility.executeInBackground(codeBlock = {
                try {
                    downloadModel?.let { downloadDataModel ->
                        val inputMediaFilePath = downloadDataModel.getDestinationFile().absolutePath
                        val convertedAudioFileName = downloadDataModel.fileName + "_converted.mp3"
                        val outputPath = "${getText(string.text_default_aio_download_folder_path)}/AIO Sounds/"
                        val outputMediaFile = File(outputPath, convertedAudioFileName)
                        
                        // Start the conversion process
                        videoToAudioConverter.extractAudio(
                            inputFile = inputMediaFilePath,
                            outputFile = outputMediaFile.absolutePath,
                            listener = object : ConversionListener {
                                override fun onProgress(progress: Int) {
                                    executeOnMainThread {
                                        val progressString = INSTANCE.getString(
                                            string.text_converting_audio_progress,
                                            "${progress}%"
                                        ); messageTextView?.text = progressString
                                    }
                                }
                                
                                override fun onSuccess(outputFile: String) {
                                    executeOnMainThread {
                                        waitingDialog.close()
                                        safeActivityRef.resumePlayer()
                                        // Add to media store and show success message
                                        FileSystemUtility.addToMediaStore(outputMediaFile)
                                        showToast(msgId = string.text_converting_audio_has_been_successful)
                                        try {
                                            addNewDownloadModelToSystem(downloadDataModel, outputMediaFile)
                                        } catch (error: Exception) {
                                            error.printStackTrace()
                                        }
                                    }
                                }
                                
                                override fun onFailure(errorMessage: String) {
                                    executeOnMainThread {
                                        waitingDialog.close()
                                        safeActivityRef.resumePlayer()
                                        showToast(msgId = string.text_converting_audio_has_been_failed)
                                    }
                                }
                            }
                        )
                    }
                } catch (error: Exception) {
                    executeOnMainThread {
                        waitingDialog.close()
                        safeActivityRef.resumePlayer()
                        showToast(msgId = string.text_something_went_wrong)
                    }
                }
            })
        }
    }
    
    /**
     * Adds the converted audio file to the download system as a new entry.
     *
     * @param downloadDataModel The original download model to copy properties from
     * @param outputMediaFile The converted audio file
     */
    private fun addNewDownloadModelToSystem(
        downloadDataModel: DownloadDataModel,
        outputMediaFile: File
    ) {
        // Create a copy of the original model with updated properties
        val copiedDataModel = deepCopy(downloadDataModel)
        copiedDataModel?.id = getUniqueNumberForDownloadModels()
        copiedDataModel?.apply {
            fileName = outputMediaFile.name
            fileDirectory = outputMediaFile.parentFile?.absolutePath.toString()
            fileSize = outputMediaFile.length()
            fileSizeInFormat = getHumanReadableFormat(fileSize)
            fileCategoryName = getUpdatedCategoryName()
            startTimeDate = System.currentTimeMillis()
            startTimeDateInFormat = millisToDateTimeString(lastModifiedTimeDate)
            lastModifiedTimeDate = System.currentTimeMillis()
            lastModifiedTimeDateInFormat = millisToDateTimeString(lastModifiedTimeDate)
        }?.let {
            // Update storage and UI
            it.updateInStorage()
            downloadSystem.addAndSortFinishedDownloadDataModels(it)
            val downloadsUIManager = downloadSystem.downloadsUIManager
            val finishedTasksFragment = downloadsUIManager.finishedTasksFragment
            val finishedTasksListAdapter = finishedTasksFragment?.finishedTasksListAdapter
            finishedTasksListAdapter?.notifyDataSetChangedOnSort(false)
        }
    }
    
    /**
     * Opens the current media file in another application.
     */
    private fun openMediaFile() {
        safeMediaPlayerActivityRef?.openMediaFile()
    }
}