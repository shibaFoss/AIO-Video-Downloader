package app.ui.main.fragments.home

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.core.content.res.ResourcesCompat.getDrawable
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.core.AIOApp
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.aioBackend
import app.core.AIOApp.Companion.downloadSystem
import app.core.AIOTimer
import app.core.bases.BaseFragment
import app.core.engines.downloader.DownloadDataModel
import app.core.engines.downloader.DownloadDataModel.Companion.DOWNLOAD_MODEL_ID_KEY
import app.core.engines.downloader.DownloadDataModel.Companion.THUMB_EXTENSION
import app.core.engines.video_parser.dialogs.VideoLinkPasteEditor
import app.ui.main.MotherActivity
import app.ui.main.fragments.browser.activities.BookmarksActivity
import app.ui.main.fragments.browser.activities.HistoryActivity
import app.ui.main.guides.GuidePlatformPicker
import app.ui.others.media_player.MediaPlayerActivity
import app.ui.others.media_player.MediaPlayerActivity.Companion.FROM_FINISHED_DOWNLOADS_LIST
import app.ui.others.media_player.MediaPlayerActivity.Companion.PLAY_MEDIA_FILE_PATH
import app.ui.others.media_player.MediaPlayerActivity.Companion.WHERE_DID_YOU_COME_FROM
import com.aio.R
import lib.files.FileSystemUtility.isAudioByName
import lib.files.FileSystemUtility.isVideo
import lib.files.FileSystemUtility.isVideoByName
import lib.networks.URLUtility
import lib.process.AsyncJobUtils.executeInBackground
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.IntentHelperUtils.openFacebookApp
import lib.process.IntentHelperUtils.openInstagramApp
import lib.texts.CommonTextUtils.getText
import lib.ui.ActivityAnimator.animActivityFade
import lib.ui.MsgDialogUtils
import lib.ui.ViewUtility.getThumbnailFromFile
import lib.ui.ViewUtility.hideOnScreenKeyboard
import lib.ui.ViewUtility.hideView
import lib.ui.ViewUtility.rotateBitmap
import lib.ui.ViewUtility.saveBitmapToFile
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.ViewUtility.showOnScreenKeyboard
import lib.ui.ViewUtility.showView
import lib.ui.builders.ToastView.Companion.showToast
import java.io.File
import java.lang.ref.WeakReference

/**
 * The home fragment that serves as the main landing page of the application.
 *
 * Responsibilities:
 * - Displays quick access to favorite sites
 * - Shows recent downloads
 * - Provides URL input for downloads
 * - Manages premium subscription prompts
 * - Handles navigation to history/bookmarks
 */
class HomeFragment : BaseFragment(), AIOTimer.AIOTimerListener {
	
	// Weak references to prevent memory leaks
	private val safeMotherActivityRef by lazy {
		WeakReference(safeBaseActivityRef as MotherActivity).get()
	}
	
	private val safeHomeFragmentRef by lazy {
		WeakReference(this).get()
	}
	
	// State tracking variables
	var isParsingTitleFromUrlAborted = false
	var lastCheckedFinishedTasksSize = 0
	var lastCheckedActiveTasksSize = 0
	
	/**
	 * Returns the layout resource ID for this fragment.
	 * @return The layout resource ID (R.layout.frag_home_1_main_1)
	 */
	override fun getLayoutResId(): Int {
		return R.layout.frag_home_1_main_1
	}
	
	/**
	 * Called after the fragment's layout is loaded.
	 * @param layoutView The inflated layout view
	 * @param state The saved instance state bundle
	 */
	override fun onAfterLayoutLoad(layoutView: View, state: Bundle?) {
		setupPremiumSubscriptionCard(layoutView)
		setupPasteVideoLinkEditor(layoutView)
		setupHistoryAndBookmarks(layoutView)
		setupFavoriteSitesAdapter(layoutView)
	}
	
	/**
	 * Called when the fragment resumes.
	 * Registers with timer and updates references.
	 */
	override fun onResumeFragment() {
		registerSelfReferenceInMotherActivity()
		safeHomeFragmentRef?.let { AIOApp.aioTimer.register(it) }
	}
	
	/**
	 * Called when the fragment pauses.
	 * Unregisters from timer.
	 */
	override fun onPauseFragment() {
		safeHomeFragmentRef?.let { AIOApp.aioTimer.register(it) }
	}
	
	/**
	 * Called when the fragment view is destroyed.
	 * Cleans up references and adapters.
	 */
	override fun onDestroyView() {
		unregisterSelfReferenceInMotherActivity()
		clearFragmentAdapterFromMemory()
		super.onDestroyView()
	}
	
	/**
	 * Timer callback that updates the UI periodically.
	 * @param loopCount The current timer loop count
	 */
	override fun onAIOTimerTick(loopCount: Double) {
		safeFragmentLayoutRef?.let { layout ->
			try {
				val activeDownloadsContainer = layout.findViewById<View>(R.id.active_downloads_section)
				val recentContainer = layout.findViewById<View>(R.id.recent_downloads_section)
				val emptyDownloadContainer = layout.findViewById<View>(R.id.empty_downloads_container)
				val buttonHowToDownload = layout.findViewById<View>(R.id.button_how_to_download)
				
				buttonHowToDownload.setOnClickListener {
					GuidePlatformPicker(safeMotherActivityRef).show()
				}
				
				val finishedDownloadModels = downloadSystem.finishedDownloadDataModels
				val activeDownloadModels = downloadSystem.activeDownloadDataModels
				if (finishedDownloadModels.isEmpty()) {
					lastCheckedFinishedTasksSize = 0
					hideView(recentContainer, true, 300)
					showView(emptyDownloadContainer, true, 100)
				} else {
					hideView(emptyDownloadContainer, true, 100)
					showView(recentContainer, true, 300)
					if (finishedDownloadModels.size != lastCheckedFinishedTasksSize) {
						lastCheckedFinishedTasksSize = finishedDownloadModels.size
						setupRecentDownloadsSitesAdapter(layout)
					}
				}
				
				if (activeDownloadModels.isEmpty()) {
					lastCheckedActiveTasksSize = 0
					hideView(activeDownloadsContainer, true, 300).let {
						activeDownloadsContainer.visibility = GONE
					}
				} else {
					showView(activeDownloadsContainer, true, 300)
					if (activeDownloadModels.size != lastCheckedActiveTasksSize) {
						lastCheckedActiveTasksSize = activeDownloadModels.size
						val activeDownloadsInfo = layout.findViewById<TextView>(R.id.text_active_downloads_info)
						val string = getString(
							R.string.text_you_have_b_active_downloads_b,
							activeDownloadModels.size.toString()
						); activeDownloadsInfo.setText(string)
						
						with(layout.findViewById<View>(R.id.button_open_active_downloads)) {
							setOnClickListener {
								safeMotherActivityRef?.downloadFragment?.openActiveTab()?.let {
									safeMotherActivityRef?.openDownloadsFragment()
								}
							}
						}
					}
				}
			} catch (error: Exception) {
				error.printStackTrace()
			}
		}
	}
	
	/**
	 * Registers this fragment with the parent activity.
	 */
	private fun registerSelfReferenceInMotherActivity() {
		safeMotherActivityRef?.homeFragment = safeHomeFragmentRef
		safeMotherActivityRef?.sideNavigation?.closeDrawerNavigation()
	}
	
	/**
	 * Unregisters this fragment from the parent activity.
	 */
	private fun unregisterSelfReferenceInMotherActivity() {
		safeMotherActivityRef?.homeFragment = null
	}
	
	/**
	 * Clears adapter references to prevent memory leaks.
	 */
	private fun clearFragmentAdapterFromMemory() {
		safeFragmentLayoutRef?.apply {
			val faviconList = findViewById<RecyclerView>(R.id.favicons_recycler_list)
			val recentDownloadsList = findViewById<RecyclerView>(R.id.recent_downloads_recycle_list)
			faviconList.adapter = null
			recentDownloadsList.adapter = null
		}
	}
	
	/**
	 * Sets up the premium subscription card with click listener.
	 * @param layoutView The fragment's root view
	 */
	private fun setupPremiumSubscriptionCard(layoutView: View) {
		val premiumCard = layoutView.findViewById<View>(R.id.button_upgrade_to_premium)
		premiumCard.setOnClickListener {
			safeMotherActivityRef?.let { safeMotherActivityRef ->
				safeMotherActivityRef.doSomeVibration(50)
				if (AIOApp.IS_PREMIUM_USER && AIOApp.IS_ULTIMATE_VERSION_UNLOCKED) {
					MsgDialogUtils.showMessageDialog(
						baseActivityInf = safeMotherActivityRef,
						isTitleVisible = true,
						titleTextViewCustomize = { it.setText(R.string.title_thank_you_so_much) },
						isNegativeButtonVisible = false,
						messageTextViewCustomize = {
							it.setText(R.string.text_thank_you_using_premium_version)
						},
						positiveButtonTextCustomize = {
							it.setText(R.string.title_okay)
							it.setLeftSideDrawable(R.drawable.ic_button_checked_circle)
						}
					)
				} else {
					MsgDialogUtils.showMessageDialog(
						baseActivityInf = safeMotherActivityRef,
						isTitleVisible = true,
						titleTextViewCustomize = { it.setText(R.string.title_thank_you_so_much) },
						isNegativeButtonVisible = false,
						messageTextViewCustomize = {
							it.setText(R.string.text_thank_you_taking_interest_in_premium)
						},
						positiveButtonTextCustomize = {
							it.setText(R.string.title_okay)
							it.setLeftSideDrawable(R.drawable.ic_button_checked_circle)
						}
					)
				}
			}
		}
		
		if (AIOApp.IS_PREMIUM_USER && AIOApp.IS_ULTIMATE_VERSION_UNLOCKED) {
			layoutView.findViewById<TextView>(R.id.text_premium_status)?.text =
				getText(R.string.title_you_are_using_aio_premium)
		}
	}
	
	/**
	 * Sets up the URL input editor and download button.
	 * @param layoutView The fragment's root view
	 */
	private fun setupPasteVideoLinkEditor(layoutView: View) {
		safeMotherActivityRef?.let { safeMotherActivity ->
			with(layoutView) {
				val editFiledUrlContainer = findViewById<View>(R.id.edit_field_file_url_container)
				val editFiledUrl = findViewById<EditText>(R.id.edit_field_file_url)
				val buttonDownload = findViewById<View>(R.id.button_add_download)
				
				editFiledUrlContainer.setOnClickListener {
					editFiledUrl.focusable
					editFiledUrl.selectAll()
					showOnScreenKeyboard(safeMotherActivity, editFiledUrl)
				}
				
				buttonDownload.setOnClickListener {
					try {
						hideOnScreenKeyboard(safeMotherActivity, editFiledUrl)
						val userEnteredUrl = editFiledUrl.text.toString()
						if (URLUtility.isValidURL(userEnteredUrl)) {
							VideoLinkPasteEditor(
								motherActivity = safeMotherActivity,
								passOnUrl = userEnteredUrl,
								autoStart = true
							).show()
							editFiledUrl.setText(getString(R.string.text_empty_string))
						} else {
							safeMotherActivity.doSomeVibration(50)
							showToast(msgId = R.string.text_file_url_not_valid)
						}
						
						aioBackend.updateClickCountOnVideoUrlEditor()
					} catch (error: Exception) {
						error.printStackTrace()
						safeMotherActivity.doSomeVibration(50)
						showToast(msgId = R.string.text_something_went_wrong)
					}
				}
			}
		}
	}
	
	/**
	 * Sets up history and bookmarks buttons with click listeners.
	 * @param layoutView The fragment's root view
	 */
	private fun setupHistoryAndBookmarks(layoutView: View) {
		val buttonHistory = layoutView.findViewById<View>(R.id.button_open_history)
		val buttonBookmark = layoutView.findViewById<View>(R.id.button_open_bookmark)
		
		buttonBookmark.setOnClickListener {
			safeMotherActivityRef?.let { motherActivity ->
				val input = Intent(motherActivity, BookmarksActivity::class.java)
				motherActivity.resultLauncher.launch(input)
				aioBackend.updateClickCountOnHomeBookmark()
			}
		}
		
		buttonHistory.setOnClickListener {
			safeMotherActivityRef?.let { motherActivity ->
				val input = Intent(motherActivity, HistoryActivity::class.java)
				motherActivity.resultLauncher.launch(input)
				aioBackend.updateClickCountOnHomeHistory()
			}
		}
	}
	
	/**
	 * Sets up the favorite sites grid adapter.
	 * @param layoutView The fragment's root view
	 */
	private fun setupFavoriteSitesAdapter(layoutView: View) {
		with(layoutView) {
			val recyclerView = findViewById<RecyclerView>(R.id.favicons_recycler_list)
			val favicons = listOf(
				Pair(first = R.drawable.ic_site_google, second = R.string.title_google),
				Pair(first = R.drawable.ic_site_bing, second = R.string.title_bing),
				Pair(first = R.drawable.ic_site_yahoo, second = R.string.title_yahoo),
				Pair(first = R.drawable.ic_site_duckduckgo, second = R.string.title_duckduckgo),
				Pair(first = R.drawable.ic_site_dailymotion, second = R.string.title_dailymotion),
				Pair(first = R.drawable.ic_site_facebook, second = R.string.title_facebook),
				Pair(first = R.drawable.ic_site_twitter, second = R.string.title_x),
				Pair(first = R.drawable.ic_site_instagram, second = R.string.title_instagram),
			)
			
			recyclerView.layoutManager = GridLayoutManager(safeMotherActivityRef, 4)
			recyclerView.adapter = object : RecyclerView.Adapter<FaviconViewHolder>() {
				override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaviconViewHolder {
					val view = LayoutInflater.from(parent.context)
						.inflate(R.layout.frag_home_1_main_1_fav_item_1, parent, false)
					return FaviconViewHolder(view) { siteName ->
						val siteMap = mapOf(
							getText(R.string.title_google).toString() to "https://google.com",
							getText(R.string.title_bing).toString() to "https://bing.com",
							getText(R.string.title_yahoo).toString() to "https://yahoo.com",
							getText(R.string.title_duckduckgo).toString() to "https://duckduckgo.com",
							getText(R.string.title_dailymotion).toString() to "https://dailymotion.com",
							getText(R.string.title_facebook).toString() to "https://facebook.com",
							getText(R.string.title_whatsapp).toString() to "https://web.whatsapp.com",
							getText(R.string.title_x).toString() to "https://x.com",
							getText(R.string.title_instagram).toString() to "https://instagram.com"
						)
						
						siteMap.entries.firstOrNull { siteName.contains(it.key, ignoreCase = true) }?.let { match ->
							safeMotherActivityRef?.let { activity ->
								val webviewEngine = activity.browserFragment?.getBrowserWebEngine()
								webviewEngine?.let {
									when (match.key) {
										getText(R.string.title_facebook).toString() -> {
											openFacebookApp(INSTANCE) {
												activity.sideNavigation?.addNewBrowsingTab(match.value, it)
												activity.openBrowserFragment()
												activity.doSomeVibration(50)
												showToast(msgId = R.string.text_facebook_isnt_installed)
											}
										}
										
										getText(R.string.title_instagram).toString() -> {
											openInstagramApp(INSTANCE) {
												activity.sideNavigation?.addNewBrowsingTab(match.value, it)
												activity.openBrowserFragment()
												activity.doSomeVibration(50)
												showToast(msgId = R.string.text_instagram_isnt_installed)
											}
										}
										
										else -> {
											activity.sideNavigation?.addNewBrowsingTab(match.value, it)
											activity.openBrowserFragment()
										}
									}
								}
							}
						}
						
						aioBackend.updateClickCountOnHomesFavicon()
					}
				}
				
				override fun onBindViewHolder(holder: FaviconViewHolder, position: Int) {
					holder.setImageFavicon(favicons[position].first)
					holder.setFaviconTitle(favicons[position].second)
				}
				
				override fun getItemCount() = favicons.size
			}
		}
	}
	
	/**
	 * ViewHolder for favorite site items.
	 * @param itemView The item view
	 * @param onItemClick Callback when item is clicked
	 */
	open class FaviconViewHolder(
		private val itemView: View,
		private val onItemClick: (String) -> Unit
	) : RecyclerView.ViewHolder(itemView) {
		open val image: ImageView = itemView.findViewById(R.id.image_of_favicon)
		open val title: TextView = itemView.findViewById(R.id.title_of_favicon)
		
		init {
			itemView.setOnClickListener {
				onItemClick(title.text.toString())
			}
		}
		
		/**
		 * Sets the favicon image.
		 * @param resId The resource ID of the image
		 */
		fun setImageFavicon(resId: Int) {
			image.setImageResource(resId)
		}
		
		/**
		 * Sets the favicon title.
		 * @param resId The resource ID of the title string
		 */
		fun setFaviconTitle(resId: Int) {
			title.text = getText(resId)
		}
	}
	
	/**
	 * Sets up the recent downloads grid adapter.
	 * @param layoutView The fragment's root view
	 */
	private fun setupRecentDownloadsSitesAdapter(layoutView: View) {
		with(layoutView) {
			if (downloadSystem.isInitializing) return
			
			val downloadsModels = downloadSystem.finishedDownloadDataModels
				.filter { isAudioByName(it.fileName) || isVideoByName(it.fileName) }
				.take(6)
			val recyclerView = findViewById<RecyclerView>(R.id.recent_downloads_recycle_list)
			recyclerView.layoutManager = GridLayoutManager(safeMotherActivityRef, 3)
			recyclerView.adapter = null
			recyclerView.adapter = object : RecyclerView.Adapter<RecentDownloadsViewHolder>() {
				override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentDownloadsViewHolder {
					val layoutInflater = LayoutInflater.from(parent.context)
					val view = layoutInflater.inflate(R.layout.frag_home_1_main_1_recent_item_1, parent, false)
					return RecentDownloadsViewHolder(view)
				}
				
				override fun onBindViewHolder(holder: RecentDownloadsViewHolder, position: Int) {
					holder.setImageThumbnail(downloadsModels[position])
					holder.setOnClickEvent(downloadsModels[position], safeMotherActivityRef)
				}
				
				override fun getItemCount(): Int {
					return downloadsModels.size
				}
			}
			
			layoutView.findViewById<View>(R.id.open_downloads_tab)
				.setOnClickListener { safeMotherActivityRef?.openDownloadsFragment() }
		}
	}
	
	/**
	 * ViewHolder for recent download items.
	 * @param itemView The item view
	 */
	open class RecentDownloadsViewHolder(private val itemView: View) : RecyclerView.ViewHolder(itemView) {
		open val thumbnail: ImageView = itemView.findViewById(R.id.image_file_thumbnail)
		
		/**
		 * Sets the click event for the download item.
		 * @param downloadDataModel The download data model
		 * @param safeMotherActivityRef The parent activity reference
		 */
		fun setOnClickEvent(
			downloadDataModel: DownloadDataModel,
			safeMotherActivityRef: MotherActivity?
		) {
			itemView.findViewById<View>(R.id.main_container).setOnClickListener {
				playTheMedia(downloadDataModel, safeMotherActivityRef)
				aioBackend.updateClickCountOnRecentDownloadsList()
			}
		}
		
		/**
		 * Sets the thumbnail image for the download item.
		 * @param downloadDataModel The download data model
		 */
		fun setImageThumbnail(downloadDataModel: DownloadDataModel) {
			updateThumbnailInfo(downloadDataModel)
		}
		
		/**
		 * Plays the media associated with the download.
		 * @param downloadDataModel The download data model
		 * @param safeMotherActivityRef The parent activity reference
		 */
		@OptIn(UnstableApi::class)
		private fun playTheMedia(
			downloadDataModel: DownloadDataModel,
			safeMotherActivityRef: MotherActivity?
		) {
			safeMotherActivityRef?.let { _ ->
				downloadDataModel.let { downloadDataModel ->
					if (isAudioByName(downloadDataModel.fileName)
						|| isVideoByName(downloadDataModel.fileName)
					) {
						safeMotherActivityRef.startActivity(
							Intent(safeMotherActivityRef, MediaPlayerActivity::class.java).apply {
								flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
								putExtra(DOWNLOAD_MODEL_ID_KEY, downloadDataModel.id)
								putExtra(PLAY_MEDIA_FILE_PATH, true)
								putExtra(WHERE_DID_YOU_COME_FROM, FROM_FINISHED_DOWNLOADS_LIST)
							})
						animActivityFade(safeMotherActivityRef)
					}
				}
			}
		}
		
		/**
		 * Updates the thumbnail information for the download item.
		 * @param downloadDataModel The download data model
		 */
		private fun updateThumbnailInfo(downloadDataModel: DownloadDataModel) {
			val destinationFile = downloadDataModel.getDestinationFile()
			val defaultThumb = downloadDataModel.getThumbnailDrawableID()
			val defaultThumbDrawable = getDrawable(INSTANCE.resources, defaultThumb, null)
			
			if (isVideoThumbnailNotAllowed(downloadDataModel)) {
				thumbnail.setImageDrawable(defaultThumbDrawable)
				return
			}
			
			if (loadApkThumbnail(
					downloadDataModel,
					thumbnail, defaultThumbDrawable
				)
			) return
			
			executeInBackground {
				val cachedThumbPath = downloadDataModel.thumbPath
				if (cachedThumbPath.isNotEmpty()) {
					executeOnMainThread {
						loadBitmapWithGlide(downloadDataModel.thumbPath, defaultThumb)
					}; return@executeInBackground
				}
				
				val bitmap = getThumbnailFromFile(
					destinationFile,
					downloadDataModel.videoInfo?.videoThumbnailUrl, requiredThumbWidth = 420
				)
				if (bitmap != null) {
					val isPortrait = bitmap.height > bitmap.width
					val rotatedBitmap = if (isPortrait) {
						rotateBitmap(bitmap, 270f)
					} else bitmap
					
					val thumbnailName = "${downloadDataModel.id}$THUMB_EXTENSION"
					saveBitmapToFile(rotatedBitmap, thumbnailName)?.let { filePath ->
						downloadDataModel.thumbPath = filePath
						downloadDataModel.updateInStorage()
						executeOnMainThread {
							loadBitmapWithGlide(downloadDataModel.thumbPath, defaultThumb)
						}
					}
				}
			}
		}
		
		/**
		 * Loads the thumbnail image using Glide.
		 * @param thumbFilePath The path to the thumbnail file
		 * @param defaultThumb The default thumbnail resource ID
		 */
		private fun loadBitmapWithGlide(thumbFilePath: String, defaultThumb: Int) {
			val imgURI = File(thumbFilePath).toUri()
			try {
				thumbnail.setImageURI(imgURI)
			} catch (error: Exception) {
				error.printStackTrace()
				thumbnail.setImageResource(defaultThumb)
			}
		}
		
		/**
		 * Checks if video thumbnails are allowed for this download.
		 * @param downloadDataModel The download data model
		 * @return true if thumbnails are not allowed, false otherwise
		 */
		private fun isVideoThumbnailNotAllowed(downloadDataModel: DownloadDataModel): Boolean {
			val isVideoHidden = downloadDataModel.globalSettings.downloadHideVideoThumbnail
			return isVideo(downloadDataModel.getDestinationDocumentFile()) && isVideoHidden
		}
		
		/**
		 * Loads an APK file's icon as its thumbnail.
		 * @param downloadDataModel The download data model
		 * @param imageViewHolder The ImageView to display the icon
		 * @param defaultThumbDrawable The default thumbnail to use if APK icon can't be loaded
		 * @return true if APK icon was loaded successfully, false otherwise
		 */
		private fun loadApkThumbnail(
			downloadDataModel: DownloadDataModel,
			imageViewHolder: ImageView, defaultThumbDrawable: Drawable?
		): Boolean {
			val apkFile = downloadDataModel.getDestinationFile()
			if (!apkFile.exists() || !apkFile.name.lowercase().endsWith(".apk")) {
				imageViewHolder.setImageDrawable(defaultThumbDrawable)
				return false
			}
			
			val packageManager: PackageManager = INSTANCE.packageManager
			return try {
				val packageInfo: PackageInfo? = packageManager.getPackageArchiveInfo(
					apkFile.absolutePath, PackageManager.GET_ACTIVITIES
				)
				packageInfo?.applicationInfo?.let { appInfo ->
					appInfo.sourceDir = apkFile.absolutePath
					appInfo.publicSourceDir = apkFile.absolutePath
					val icon: Drawable = appInfo.loadIcon(packageManager)
					imageViewHolder.setImageDrawable(icon)
					true
				} ?: false
			} catch (error: Exception) {
				error.printStackTrace()
				imageViewHolder.apply {
					scaleType = ImageView.ScaleType.FIT_CENTER
					setPadding(0, 0, 0, 0)
					setImageDrawable(defaultThumbDrawable)
				}; false
			}
		}
	}
}