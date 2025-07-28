package app.ui.main.fragments.downloads.dialogs

import android.text.Html
import android.text.Html.FROM_HTML_MODE_COMPACT
import android.text.method.LinkMovementMethod
import android.widget.TextView
import app.core.engines.downloader.DownloadDataModel
import app.core.engines.downloader.DownloadInfoUtils.buildDownloadInfoHtmlString
import app.ui.main.MotherActivity
import com.aio.R
import lib.process.SimpleTimerUtils
import lib.ui.builders.DialogBuilder
import java.lang.ref.WeakReference

/**
 * Dialog component that periodically displays real-time information
 * about a specific download task.
 *
 * @param motherActivity The reference to the host activity to create UI elements.
 */
class DownloadInfoTracker(motherActivity: MotherActivity?) {
    
    /** Weak reference to the host activity to avoid memory leaks */
    private val safeMotherActivityRef = WeakReference(motherActivity).get()
    
    /** DialogBuilder instance to manage and render the dialog UI */
    var dialogBuilder: DialogBuilder = DialogBuilder(safeMotherActivityRef)

    init {
        dialogBuilder.apply {
            // Set dialog layout containing a TextView to show download info
            setView(R.layout.frag_down_2_onclick_info_1)
            
            // Set a simple positive button that closes the dialog
            setOnClickForPositiveButton { close() }
            
            // Make the dialog cancelable
            setCancelable(true)
        }
    }
    
    /**
     * Displays the download information dialog for a given download model.
     * Periodically updates the content as long as the task is active.
     *
     * @param downloadModel The download task whose information is to be tracked.
     */
    fun show(downloadModel: DownloadDataModel) {
        dialogBuilder.show()
        
        // Timer updates the dialog every 1 second with current download info
        SimpleTimerUtils(1000, 1000).apply {
            setTimerListener(timerListener = object : SimpleTimerUtils.TimerListener {
                override fun onTick(millisUntilFinished: Long) {
                    showCurrentDownloadInfo(dialogBuilder, downloadModel)
                }
                
                override fun onFinish() {
                    // Continue refreshing the dialog as long as it's showing and download is running
                    if (dialogBuilder.isShowing && downloadModel.isRunning) {
                        start()
                    }
                }
            }); start()
        }
    }
    
    /**
     * Closes the dialog manually.
     */
    fun close() {
        dialogBuilder.close()
    }
    
    /**
     * Updates the dialog with the current information of the download task.
     * Converts HTML string into styled text and applies it to the TextView.
     *
     * @param dialogBuilder The dialog being displayed.
     * @param downloadModel The download task whose data is being shown.
     */
    private fun showCurrentDownloadInfo(
        dialogBuilder: DialogBuilder,
        downloadModel: DownloadDataModel
    ) {
        dialogBuilder.view.apply {
            val infoTextView = findViewById<TextView>(R.id.text_dialog_message)
            
            // Generate HTML-formatted download info
            val htmlText = buildDownloadInfoHtmlString(downloadModel)
            
            // Convert HTML to spanned text
            val spannedText = Html.fromHtml(htmlText, FROM_HTML_MODE_COMPACT)
            
            // Set the styled text and enable links
            infoTextView.text = spannedText
            infoTextView.movementMethod = LinkMovementMethod.getInstance()
        }
    }
}
