package app.ui.main

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.widget.ViewPager2
import app.core.AIOApp.Companion.IS_ULTIMATE_VERSION_UNLOCKED
import app.core.AIOApp.Companion.aioSettings
import app.core.bases.BaseActivity
import app.core.engines.downloader.DownloadNotification.Companion.FROM_DOWNLOAD_NOTIFICATION
import app.core.engines.video_parser.parsers.SupportedURLs.isSocialMediaUrl
import app.core.engines.video_parser.parsers.SupportedURLs.isYouTubeUrl
import app.core.engines.video_parser.parsers.SupportedURLs.isYtdlpSupportedUrl
import app.core.engines.video_parser.parsers.VideoThumbGrabber.startParsingVideoThumbUrl
import app.ui.main.fragments.browser.BrowserFragment
import app.ui.main.fragments.browser.webengine.SingleResolutionPrompter
import app.ui.main.fragments.downloads.DownloadsFragment
import app.ui.main.fragments.downloads.intercepter.SharedVideoURLIntercept
import app.ui.main.fragments.home.HomeFragment
import app.ui.main.fragments.settings.SettingsFragment
import app.ui.others.media_player.MediaPlayerActivity.Companion.WHERE_DID_YOU_COME_FROM
import com.aio.R
import lib.device.IntentUtility.getIntentDataURI
import lib.networks.URLUtility
import lib.networks.URLUtilityKT.fetchWebPageContent
import lib.networks.URLUtilityKT.getWebpageTitleOrDescription
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.process.CommonTimeUtils.delay
import lib.process.ThreadsUtility
import lib.texts.ClipboardUtils.getTextFromClipboard
import lib.ui.MsgDialogUtils
import lib.ui.ViewUtility.setLeftSideDrawable
import lib.ui.builders.ToastView
import lib.ui.builders.WaitingDialog
import java.lang.ref.WeakReference

/**
 * The main activity that hosts all fragments and manages navigation.
 *
 * Responsibilities:
 * - Hosts the main ViewPager for fragment navigation
 * - Manages bottom tab navigation
 * - Handles deep linking and intent URLs
 * - Monitors clipboard for URLs
 * - Shows interstitial ads
 * - Manages back press behavior
 * - Coordinates between fragments
 */
class MotherActivity : BaseActivity() {
	
	companion object {
		// Key for activity result data
		const val ACTIVITY_RESULT_KEY = "ACTIVITY_RESULT_KEY"
	}
	
	// Weak reference to avoid memory leaks
	private var weakMotherActivityRef = WeakReference(this)
	private val safeMotherActivityRef = weakMotherActivityRef.get()
	
	// ViewModel for sharing data between fragments
	private lateinit var sharedViewModel: SharedViewModel
	
	// UI Components
	private lateinit var fragmentViewPager: ViewPager2
	private lateinit var motherBottomTabs: MotherBottomTabs
	
	// Lazy initialized side navigation drawer
	val sideNavigation: WebNavigationDrawer? by lazy {
		WebNavigationDrawer(safeMotherActivityRef).apply { initialize() }
	}
	
	// Activity result launcher
	val resultLauncher = registerResultOrientedActivityLauncher()
	
	// Fragment references
	var homeFragment: HomeFragment? = null
	var browserFragment: BrowserFragment? = null
	var downloadFragment: DownloadsFragment? = null
	var settingsFragment: SettingsFragment? = null
	
	// Flag for URL parsing
	var isParsingTitleFromUrlAborted = false
	
	/**
	 * Returns the layout resource ID for this activity
	 * @return The layout resource ID (R.layout.activity_mother_1)
	 */
	override fun onRenderingLayout(): Int {
		return R.layout.activity_mother_1
	}
	
	/**
	 * Called after layout is rendered
	 * Sets up main components and shows initial ad
	 */
	override fun onAfterLayoutRender() {
		safeMotherActivityRef?.let { _ ->
			setLightSystemBarTheme()
			setupFragmentViewpager()
			setupBottomTabs()
		}
	}
	
	/**
	 * Handles back press events
	 */
	override fun onBackPressActivity() {
		handleBackPressEvent()
	}
	
	/**
	 * Called when activity resumes
	 * Sets up monitoring and handles pending intents
	 */
	@OptIn(UnstableApi::class)
	override fun onResumeActivity() {
		//monitorClipboardForUrls()
		handleIntentURL()
		openDownloadsFragmentIfIntended()
		updateButtonTabSelectionUI()
	}
	
	/**
	 * Clear the weak reference of the activity. Careful using the function,
	 * and should call by the application life cycle manager to automatically
	 * clear up the reference.
	 */
	override fun clearWeakActivityReference() {
		weakMotherActivityRef.clear()
		super.clearWeakActivityReference()
	}
	
	/**
	 * Opens the Home fragment
	 */
	fun openHomeFragment() {
		if (fragmentViewPager.currentItem != 0) {
			fragmentViewPager.currentItem = 0
		}
	}
	
	/**
	 * Opens the Browser fragment
	 */
	fun openBrowserFragment() {
		if (fragmentViewPager.currentItem != 1) {
			fragmentViewPager.currentItem = 1
		}
	}
	
	/**
	 * Opens the Downloads fragment
	 */
	fun openDownloadsFragment() {
		if (fragmentViewPager.currentItem != 2) {
			fragmentViewPager.currentItem = 2
		}
	}
	
	/**
	 * Opens the Settings fragment
	 */
	fun openSettingsFragment() {
		if (fragmentViewPager.currentItem != 3) {
			fragmentViewPager.currentItem = 3
		}
	}
	
	/**
	 * Gets the current fragment position
	 * @return Current fragment position (0-3)
	 */
	fun getCurrentFragmentNumber(): Int {
		return fragmentViewPager.currentItem
	}
	
	/**
	 * Sets up the ViewPager for fragment navigation
	 */
	private fun setupFragmentViewpager() {
		safeMotherActivityRef?.let {
			fragmentViewPager = findViewById(R.id.fragment_viewpager)
			// Keep all fragments in memory
			fragmentViewPager.offscreenPageLimit = 4
			fragmentViewPager.adapter = FragmentsPageAdapter(it)

			// Listener for page changes
			//fragmentViewPager.setPageTransformer(FadeViewPage2Transformer())
			fragmentViewPager.isUserInputEnabled = false

			fragmentViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
				override fun onPageSelected(position: Int) {
					updateButtonTabSelectionUI(position)
				}
			})
		}
	}
	
	/**
	 * Sets up the bottom navigation tabs
	 */
	private fun setupBottomTabs() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			motherBottomTabs = MotherBottomTabs(safeMotherActivityRef)
			motherBottomTabs.initialize()
		}
	}
	
	/**
	 * Updates the UI for selected tab
	 * @param currentItem The currently selected tab position (0-3)
	 */
	private fun updateButtonTabSelectionUI(currentItem: Int = fragmentViewPager.currentItem) {
		when (currentItem) {
			0 -> motherBottomTabs.updateTabSelectionUI(MotherBottomTabs.Tab.HOME_TAB)
			1 -> motherBottomTabs.updateTabSelectionUI(MotherBottomTabs.Tab.BROWSER_TAB)
			2 -> motherBottomTabs.updateTabSelectionUI(MotherBottomTabs.Tab.DOWNLOADS_TAB)
			3 -> motherBottomTabs.updateTabSelectionUI(MotherBottomTabs.Tab.SETTINGS_TAB)
			else -> motherBottomTabs.updateTabSelectionUI(MotherBottomTabs.Tab.HOME_TAB)
		}
	}
	
	/**
	 * Registers an activity result launcher
	 * @return The configured ActivityResultLauncher
	 */
	private fun registerResultOrientedActivityLauncher() =
		registerForActivityResult(StartActivityForResult()) { result ->
			if (result.resultCode == RESULT_OK) {
				val data: Intent? = result.data
				data?.getStringExtra(ACTIVITY_RESULT_KEY)?.let { resultString ->
					// Initialize ViewModel if needed
					if (!::sharedViewModel.isInitialized) sharedViewModel =
						ViewModelProvider(this)[SharedViewModel::class.java]
					
					// Open browser and update URL if needed
					if (fragmentViewPager.currentItem != 1) openBrowserFragment()
					sharedViewModel.updateBrowserURLEditResult(resultString)
				}
			}
		}
	
	/**
	 * Handles back press events with custom behavior
	 */
	private fun handleBackPressEvent() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			sideNavigation?.let { sideNavigation ->
				// Close drawer if open
				if (sideNavigation.isDrawerOpened()) {
					sideNavigation.closeDrawerNavigation(); return
				}
				
				// Custom back behavior per fragment
				when (fragmentViewPager.currentItem) {
					1 -> { // Browser fragment
						if (!::sharedViewModel.isInitialized) {
							val viewModelClass = SharedViewModel::class.java
							sharedViewModel = ViewModelProvider(
								owner = safeMotherActivityRef
							)[viewModelClass]
						}; sharedViewModel.triggerBackPressEvent()
					}
					
					2 -> { // Downloads fragment
						val fragmentViewPager = downloadFragment?.fragmentViewPager
						if (fragmentViewPager?.currentItem == 1) {
							downloadFragment?.openFinishedTab()
						} else {
							openBrowserFragment()
						}
					}
					
					3 -> openDownloadsFragment() // From settings
					4 -> openSettingsFragment() // Shouldn't happen (only 4 fragments)
					else -> {
						// Exit handling
						if (sideNavigation.totalWebViews.size > 1) showExitWarningDialog()
						else exitActivityOnDoubleBackPress()
					}
				}
			}
		}
	}
	
	/**
	 * Shows exit warning dialog when multiple tabs are open
	 */
	private fun showExitWarningDialog() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			sideNavigation?.let { sideNavigation ->
				
				val dialogBuilder = MsgDialogUtils.getMessageDialog(
					baseActivityInf = safeMotherActivityRef,
					messageTextViewCustomize = {
						val resId = R.string.text_many_tabs_open_consider_warning
						val totalWebViews = sideNavigation.totalWebViews
						it.text = getString(resId, totalWebViews.size.toString())
					},
					negativeButtonTextCustomize = {
						it.setText(R.string.text_exit_the_app)
						it.setLeftSideDrawable(R.drawable.ic_button_exit)
					},
					positiveButtonTextCustomize = {
						it.setText(R.string.text_close_tabs)
						it.setLeftSideDrawable(R.drawable.ic_button_cancel)
					}
				)
				
				dialogBuilder?.let { dialog ->
					dialog.setOnClickForNegativeButton {
						dialog.close()
						delay(200, object : OnTaskFinishListener {
							override fun afterDelay() = finish()
						})
					}
					
					dialog.setOnClickForPositiveButton {
						dialog.close()
						openBrowserFragment()
						delay(200, object : OnTaskFinishListener {
							override fun afterDelay() {
								sideNavigation.openDrawerNavigation()
							}
						})
					}
					
					dialog.show()
				}
			}
		}
	}
	
	/**
	 * Opens Downloads fragment if coming from notification
	 */
	@UnstableApi
	private fun openDownloadsFragmentIfIntended() {
		intent.getIntExtra(WHERE_DID_YOU_COME_FROM, -2).let { result ->
			if (result == -2) return
			if (result == FROM_DOWNLOAD_NOTIFICATION) {
				openDownloadsFragment()
				return
			}
		}
	}
	
	/**
	 * Handles incoming intent URLs
	 */
	private fun handleIntentURL() {
		safeMotherActivityRef?.let { safeMotherActivityRef ->
			val intentURL = getIntentDataURI(safeMotherActivityRef)
			if (intentURL.isNullOrEmpty()) return
			
			val browserFragmentBody = browserFragment?.browserFragmentBody
			val alreadyLoadedIntentURL = browserFragmentBody?.alreadyLoadedIntentURL
			val isIntentURLLoaded = alreadyLoadedIntentURL == intentURL
			
			if (!isIntentURLLoaded) {
				openBrowserFragment()
			}
		}
	}
	/**
	 * Monitors clipboard for URLs and shows prompt if found
	 */
	fun monitorClipboardForUrls() {
		delay(500, object : OnTaskFinishListener {
			override fun afterDelay() {
				safeMotherActivityRef?.let {
					val clipboardText = getTextFromClipboard(safeMotherActivityRef).trim()
					if (clipboardText.isNotEmpty()) {
						// Skip if same URL was already processed
						if (aioSettings.lastProcessedClipboardText == clipboardText) return
						if (URLUtility.isValidURL(clipboardText)) {
							aioSettings.lastProcessedClipboardText = clipboardText
							aioSettings.updateInStorage()
							delay(500, object : OnTaskFinishListener{
								override fun afterDelay() {
									MsgDialogUtils.getMessageDialog(
										baseActivityInf = safeMotherActivityRef,
										isTitleVisible = true,
										titleTextViewCustomize = {
											it.setText(R.string.title_copied_link)
										},
										messageTextViewCustomize = {
											it.text = clipboardText
											val colorSecondary = R.color.color_secondary
											val color = getColor(safeMotherActivityRef, colorSecondary)
											it.setTextColor(color)
											it.maxLines = 2
										},
										isNegativeButtonVisible = false,
										positiveButtonTextCustomize = {
											it.setText(R.string.title_open_the_link)
											it.setLeftSideDrawable(R.drawable.ic_button_url_link)
										}
									)?.apply {
										setOnClickForPositiveButton {
											close()
											if (isYtdlpSupportedUrl(clipboardText)) {
												val isYouTubeUrl = isYouTubeUrl(clipboardText)
												val isGooglePlayVersion = !IS_ULTIMATE_VERSION_UNLOCKED
												if (isGooglePlayVersion && isYouTubeUrl) {
													openBrowserFragmentWithLink(clipboardText)
												} else {
													showVideoResolutionPicker(
														clipboardText = clipboardText,
														safeMotherActivityRef = safeMotherActivityRef
													)
												}
											} else {
												openBrowserFragmentWithLink(clipboardText)
											}
										}
									}?.show()
								}
							})
						}
					}
				}
			}
		})
	}
	
	/**
	 * Shows video resolution picker for supported URLs
	 * @param clipboardText The URL to process
	 * @param safeMotherActivityRef The activity reference
	 */
	private fun showVideoResolutionPicker(
		clipboardText: String,
		safeMotherActivityRef: MotherActivity
	) {
		if (URLUtility.isValidURL(clipboardText)) {
			if (isSocialMediaUrl(clipboardText)) {
				val waitingMsg = getText(R.string.text_analyzing_url_please_wait)
				val waitingDialog = WaitingDialog(
					isCancelable = false,
					baseActivityInf = safeMotherActivityRef,
					loadingMessage = waitingMsg.toString(),
					dialogCancelListener = { dialog ->
						isParsingTitleFromUrlAborted = true
						dialog.dismiss()
					}
				); waitingDialog.show()
				
				ThreadsUtility.executeInBackground(codeBlock = {
					val htmlBody = fetchWebPageContent(clipboardText, true)
					val thumbnailUrl = startParsingVideoThumbUrl(clipboardText, htmlBody)
					getWebpageTitleOrDescription(
						clipboardText,
						userGivenHtmlBody = htmlBody
					) { resultedTitle ->
						waitingDialog.close()
						if (!resultedTitle.isNullOrEmpty() &&
							!isParsingTitleFromUrlAborted
						) {
							executeOnMainThread {
								SingleResolutionPrompter(
									baseActivity = safeMotherActivityRef,
									singleResolutionName = getString(R.string.title_high_quality),
									extractedVideoLink = clipboardText,
									currentWebUrl = clipboardText,
									videoTitle = resultedTitle,
									videoUrlReferer = clipboardText,
									isSocialMediaUrl = true,
									isDownloadFromBrowser = false,
									dontParseFBTitle = true,
									thumbnailUrlProvided = thumbnailUrl
								).show()
							}
						}
					}
				})
			} else {
				startParingVideoURL(safeMotherActivityRef, clipboardText)
			}
		} else {
			invalidUrlErrorToast(safeMotherActivityRef)
		}
	}
	
	/**
	 * Shows invalid URL error toast
	 * @param safeMotherActivity The activity reference
	 */
	private fun invalidUrlErrorToast(safeMotherActivity: MotherActivity) {
		safeMotherActivity.doSomeVibration(50)
		ToastView.showToast(msgId = R.string.text_file_url_not_valid)
	}
	
	/**
	 * Starts parsing video URL
	 * @param safeActivity The activity reference
	 * @param userEnteredUrl The URL to parse
	 */
	private fun startParingVideoURL(safeActivity: MotherActivity, userEnteredUrl: String) {
		val videoInterceptor = SharedVideoURLIntercept(safeActivity)
		videoInterceptor.interceptIntentURI(userEnteredUrl)
	}
	
	/**
	 * Opens browser fragment with given URL
	 * @param targetUrl The URL to load in browser
	 */
	private fun openBrowserFragmentWithLink(targetUrl: String) {
		try {
			browserFragment?.getBrowserWebEngine()?.let { webEngine ->
				sideNavigation?.addNewBrowsingTab(targetUrl, webEngine)
				openBrowserFragment()
			}
		} catch (error: Exception) {
			error.printStackTrace()
			doSomeVibration(50)
			ToastView.showToast(msgId = R.string.text_something_went_wrong)
		}
	}
	
	/**
	 * ViewModel for sharing data between fragments
	 */
	class SharedViewModel : ViewModel() {
		// LiveData for URL sharing
		private val liveURLStringData = MutableLiveData<String>()
		val liveURLString: LiveData<String> get() = liveURLStringData
		
		// LiveData for back press events
		private val backPressLiveEventData = MutableLiveData<Unit>()
		val backPressLiveEvent: LiveData<Unit> get() = backPressLiveEventData
		
		/**
		 * Updates the browser URL result
		 * @param result The URL string to share
		 */
		fun updateBrowserURLEditResult(result: String) {
			liveURLStringData.value = result
		}
		
		/**
		 * Triggers a back press event
		 */
		fun triggerBackPressEvent() {
			backPressLiveEventData.value = Unit
		}
	}
}