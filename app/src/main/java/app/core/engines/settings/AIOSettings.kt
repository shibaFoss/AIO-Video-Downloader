package app.core.engines.settings

import androidx.documentfile.provider.DocumentFile
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioGSONInstance
import app.core.AIOApp.Companion.aioSettings
import app.core.AIOLanguage.Companion.ENGLISH
import com.aio.R.string
import com.anggrayudi.storage.file.DocumentFileCompat.fromFullPath
import lib.files.FileUtility.isWritableFile
import lib.files.FileUtility.readStringFromInternalStorage
import lib.files.FileUtility.saveStringToInternalStorage
import lib.process.ThreadsUtility
import lib.texts.CommonTextUtils.getText
import java.io.Serializable

/**
 * Class representing persistent user settings for the AIO application.
 *
 * Stores app state, user preferences, browser and download configuration,
 * and handles serialization to and from internal storage.
 */
class AIOSettings : Serializable {
    
    // Basic user state
    var userInstallationId: String = ""
    var isFirstTimeLanguageSelectionComplete = false
    var hasUserRatedTheApplication: Boolean = false
    var totalNumberOfSuccessfulDownloads = 0
    var totalUsageTimeInMs = 0.0f
    var totalUsageTimeInFormat = ""
    var lastProcessedClipboardText = ""
    
    // Default download location
    var defaultDownloadLocation = PRIVATE_FOLDER
    
    // Language settings
    var userSelectedUILanguage: String = ENGLISH
    
    // Analytics / interaction counters
    var totalClickCountOnRating = 0
    var totalClickCountOnLanguageChange = 0
    var totalClickCountOnMediaPlayback = 0
    var totalClickCountOnHowToGuide = 0
    var totalClickCountOnVideoUrlEditor = 0
    var totalClickCountOnHomeHistory = 0
    var totalClickCountOnHomeBookmarks = 0
    var totalClickCountOnRecentDownloads = 0
    var totalClickCountOnHomeFavicon = 0
    var totalClickCountOnVersionCheck = 0
    var totalInterstitialAdClick = 0
    var totalInterstitialImpression = 0
    var totalRewardedAdClick = 0
    var totalRewardedImpression = 0
    
    // Path to WhatsApp Statuses folder (used in status saving)
    val whatsAppStatusFullFolderPath =
        "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/"
    
    // Download preferences
    var downloadSingleUIProgress: Boolean = true
    var downloadHideVideoThumbnail: Boolean = false
    var downloadPlayNotificationSound: Boolean = true
    var downloadHideNotification: Boolean = false
    var downloadAutoRemoveTasks: Boolean = false
    var downloadAutoRemoveTaskAfterNDays: Int = 0
    var openDownloadedFileOnSingleClick: Boolean = true
    
    // Advanced download features
    var downloadAutoResume: Boolean = true
    var downloadAutoResumeMaxErrors: Int = 35
    var downloadAutoLinkRedirection: Boolean = true
    var downloadAutoFolderCatalog: Boolean = true
    var downloadAutoThreadSelection: Boolean = true
    var downloadAutoFileMoveToPrivate: Boolean = false
    var downloadAutoConvertVideosToMp3: Boolean = false
    
    // Download performance settings
    var downloadBufferSize: Int = 1024 * 8
    var downloadMaxHttpReadingTimeout: Int = 1000 * 10
    var downloadDefaultThreadConnections: Int = 1
    var downloadDefaultParallelConnections: Int = 10
    var downloadVerifyChecksum: Boolean = false
    var downloadMaxNetworkSpeed: Long = 0
    var downloadWifiOnly: Boolean = false
    var downloadHttpUserAgent: String =
        getText(string.text_downloads_default_http_user_agent)
    var downloadHttpProxyServer = ""
    
    // Crash handling
    var hasAppCrashedRecently: Boolean = false
    
    // Privacy and limits
    var privateFolderPassword: String = ""
    var numberOfMaxDownloadThreshold = 1
    var numberOfDownloadsUserDid = 0
    
    // Browser-specific settings
    var browserDefaultHomepage: String = "https://google.com/"
    var browserDesktopBrowsing: Boolean = false
    var browserEnableAdblocker: Boolean = false
    var browserEnableJavascript: Boolean = true
    var browserEnableHideImages: Boolean = false
    var browserEnablePopupBlocker: Boolean = false
    var browserEnableVideoGrabber: Boolean = true
    var browserHttpUserAgent: String =
        getText(string.text_browser_default_mobile_http_user_agent)
    
    /**
     * Reads settings from internal storage and applies them to the current app instance.
     * If read is successful, updates the app state and validates user folder selection.
     */
    fun readObjectFromStorage() {
        ThreadsUtility.executeInBackground(codeBlock = {
            try {
                readStringFromInternalStorage(AIO_SETTINGS_FILE_NAME).let { jsonString ->
                    convertJSONStringToClass(data = jsonString).let {
                        aioSettings = it
                        aioSettings.updateInStorage()
                        validateUserSelectedFolder()
                    }
                }
            } catch (error: Exception) {
                error.printStackTrace()
            }
        })
    }
    
    /**
     * Saves current settings to internal storage as a JSON file.
     */
    fun updateInStorage() {
        ThreadsUtility.executeInBackground(codeBlock = {
            saveStringToInternalStorage(
                fileName = AIO_SETTINGS_FILE_NAME,
                data = convertClassToJSON()
            )
        })
    }
    
    /**
     * Validates whether the user-selected folder is writable.
     * If not, falls back to creating a default download folder.
     */
    fun validateUserSelectedFolder() {
        if (!isWritableFile(getUserSelectedDir())) {
            createDefaultAIODownloadFolder()
        }; aioSettings.updateInStorage()
    }
    
    /**
     * Returns a [DocumentFile] representing the user-selected directory,
     * depending on whether it's a private folder or system gallery.
     */
    private fun getUserSelectedDir(): DocumentFile? {
        when (aioSettings.defaultDownloadLocation) {
            PRIVATE_FOLDER -> {
                val internalDataFolderPath = INSTANCE.dataDir.absolutePath
                return fromFullPath(
                    context = INSTANCE,
                    fullPath = internalDataFolderPath,
                    requiresWriteAccess = true
                )
            }
            
            SYSTEM_GALLERY -> {
                val externalDataFolderPath =
                    getText(string.text_default_aio_download_folder_path)
                return fromFullPath(
                    context = INSTANCE,
                    fullPath = externalDataFolderPath,
                    requiresWriteAccess = true
                )
            }; else -> return null
        }
    }
    
    /**
     * Attempts to create a default AIO folder in the public download directory.
     */
    private fun createDefaultAIODownloadFolder() {
        try {
            val defaultFolderName = getText(string.title_default_application_folder)
            INSTANCE.getPublicDownloadDir()?.createDirectory(defaultFolderName)
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }
    
    /**
     * Converts this settings object into a JSON string using Gson.
     */
    fun convertClassToJSON(): String {
        return aioGSONInstance.toJson(this)
    }
    
    /**
     * Converts a JSON string into an [AIOSettings] object using Gson.
     */
    private fun convertJSONStringToClass(data: String): AIOSettings {
        return aioGSONInstance.fromJson(data, AIOSettings::class.java)
    }
    
    companion object {
        const val AIO_SETTINGS_FILE_NAME: String = "aio_settings.json"
        const val PRIVATE_FOLDER = 1
        const val SYSTEM_GALLERY = 2
    }
}