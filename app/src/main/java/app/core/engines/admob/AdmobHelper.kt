package app.core.engines.admob

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import app.core.AIOApp.Companion.INSTANCE
import app.core.AIOApp.Companion.IS_AD_NOT_WORKING
import app.core.AIOApp.Companion.IS_PREMIUM_USER
import app.core.AIOApp.Companion.aioSettings
import app.core.engines.admob.AdUnitIds.INTERSTITIAL_AD_UNIT_ID
import app.core.engines.admob.AdUnitIds.REWARDED_INTERSTITIAL_AD_UNIT_ID
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.MobileAds.setRequestConfiguration
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.process.CommonTimeUtils.delay
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * Helper class for managing AdMob advertisements including banner ads, interstitial ads,
 * and rewarded interstitial ads. Handles ad loading, showing, and lifecycle management.
 * Automatically skips ads for premium users.
 */
class AdmobHelper {
	
	companion object {
		/**
		 * The delay (in milliseconds) before an ad is shown after being loaded.
		 */
		private const val AD_SHOW_DELAY_MS = 500L
		
		/**
		 * Duration (in milliseconds) after which a loaded ad is considered expired and must be reloaded.
		 * Currently set to 1 hour.
		 */
		private val AD_EXPIRATION_TIME = TimeUnit.HOURS.toMillis(1)
	}
	
	/**
	 * Holds the currently loaded interstitial ad instance, if any.
	 */
	private var interstitialAd: InterstitialAd? = null
	
	/**
	 * Timestamp (in milliseconds) indicating when the interstitial ad was last loaded.
	 */
	private var interstitialAdLoadTime: Long = 0
	
	/**
	 * Holds the currently loaded rewarded interstitial ad instance, if any.
	 */
	private var rewardedInterstitialAd: RewardedInterstitialAd? = null
	
	/**
	 * Timestamp (in milliseconds) indicating when the rewarded ad was last loaded.
	 */
	private var rewardedAdLoadTime: Long = 0
	
	/**
	 * Flag indicating whether an interstitial ad is currently being shown.
	 */
	private var isInterstitialAdShowing = false
	
	/**
	 * Flag indicating whether a rewarded interstitial ad is currently being shown.
	 */
	private var isRewardedAdShowing = false
	
	/**
	 * Handler tied to the main looper, used for posting delayed tasks (e.g., retrying ad load).
	 */
	private val handler = Handler(Looper.getMainLooper())
	
	/**
	 * Initializes the AdMob helper singleton and the MobileAds SDK.
	 * Called automatically upon instantiation of the helper class.
	 */
	init {
		initialize(INSTANCE)
	}
	
	/**
	 * Clears any cached ad instances and resets ad display state flags.
	 *
	 * @param isAdShowing Set to true to preserve the ad instances but mark them as currently showing.
	 *                    Useful if cleanup is invoked from within an ad display context.
	 */
	fun cleanupAds(
		isNormalInterstitialAd: Boolean = false,
		isAdShowing: Boolean = false
	) {
		if (isNormalInterstitialAd) {
			if (!isAdShowing) interstitialAd = null
			isInterstitialAdShowing = isAdShowing
			handler.removeCallbacksAndMessages(null)
		} else {
			if (!isAdShowing) rewardedInterstitialAd = null
			isRewardedAdShowing = isAdShowing
		}
	}
	
	/**
	 * Initializes the Google Mobile Ads SDK for the application.
	 *
	 * If `testDeviceIds` are provided, it configures the request system to treat those devices as test devices.
	 *
	 * @param context The application context used to initialize the SDK.
	 * @param testDeviceIds Optional list of test device IDs for enabling test ads.
	 */
	fun initialize(context: Context?, testDeviceIds: List<String> = emptyList()) {
		try {
			WeakReference(context).get()?.let { safeContextRef ->
				MobileAds.initialize(safeContextRef)
				
				if (testDeviceIds.isNotEmpty()) {
					val configuration = RequestConfiguration.Builder()
						.setTestDeviceIds(testDeviceIds)
						.build()
					setRequestConfiguration(configuration)
				}
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	/**
	 * Loads a banner ad into the given [AdView].
	 *
	 * This function does nothing if the user is marked as a premium user (`IS_PREMIUM_USER`).
	 *
	 * @param adView The AdView in which the banner ad should be loaded.
	 */
	fun loadBannerAd(adView: AdView) {
		if (IS_PREMIUM_USER) return
		val adRequest = AdRequest.Builder().build()
		adView.loadAd(adRequest)
	}
	
	/**
	 * Asynchronously loads an interstitial ad using the provided context and optional AdMob ad unit ID.
	 *
	 * This method performs several safety and state checks before attempting to load:
	 * - Skips loading if the user has a premium subscription (`IS_PREMIUM_USER`).
	 * - Skips if ads are globally marked as non-functional (`IS_AD_NOT_WORKING`).
	 * - Skips if the given context is null.
	 * - Immediately calls the `onAdLoaded` callback if a valid ad is already cached.
	 *
	 * If none of the above apply, it triggers a fresh load request.
	 * Upon success:
	 * - Caches the loaded interstitial ad for later display.
	 * - Records the load time.
	 * - Invokes the `onAdLoaded` callback, if provided.
	 *
	 * Upon failure:
	 * - Clears the cached ad.
	 * - Invokes the `onAdFailed` callback, if provided.
	 * - Retries the load operation after a 1-second delay.
	 *
	 * @param context The Android context (Activity or Application) used to initialize the ad request.
	 * @param adUnitId Optional custom AdMob ad unit ID. Defaults to `INTERSTITIAL_AD_UNIT_ID` if not provided.
	 * @param onAdLoaded Optional lambda invoked when the ad loads successfully.
	 * @param onAdFailed Optional lambda invoked if the ad fails to load.
	 */
	fun loadInterstitialAd(
		context: Context?,
		adUnitId: String? = null,
		onAdLoaded: (() -> Unit)? = null,
		onAdFailed: (() -> Unit)? = null
	) {
		// Avoid ad loading for premium users or if ads are disabled
		if (IS_PREMIUM_USER) return
		if (IS_AD_NOT_WORKING) return
		if (context == null) return
		
		// If an ad is already ready, skip loading and notify
		if (isInterstitialAdReady()) {
			onAdLoaded?.invoke()
			return
		}
		
		// Clear any previously cached ads
		cleanupAds(isNormalInterstitialAd = true)
		
		// Start loading a new interstitial ad
		InterstitialAd.load(
			context,
			adUnitId ?: INTERSTITIAL_AD_UNIT_ID,
			AdRequest.Builder().build(),
			object : InterstitialAdLoadCallback() {
				
				override fun onAdLoaded(ad: InterstitialAd) {
					interstitialAd = ad
					interstitialAdLoadTime = System.currentTimeMillis()
					onAdLoaded?.invoke()
				}
				
				override fun onAdFailedToLoad(error: LoadAdError) {
					interstitialAd = null
					onAdFailed?.invoke()
					
					// Retry ad load after 1 second
					delay(1000, object : OnTaskFinishListener {
						override fun afterDelay() {
							loadInterstitialAd(context)
						}
					})
				}
			}
		)
	}
	
	/**
	 * Asynchronously loads a Rewarded Interstitial Ad using the given context and AdMob ad unit ID.
	 *
	 * This method first ensures ads should be shown:
	 * - Skips loading if the user is a premium user.
	 * - Skips if ads are globally marked as not working.
	 * - Immediately returns if an ad is already loaded and ready.
	 *
	 * If all conditions are favorable, it initiates loading a new ad using a weak reference to the context
	 * to avoid memory leaks. The result of the ad loading operation is handled through provided callbacks.
	 *
	 * On successful load:
	 * - Stores the loaded ad reference.
	 * - Records the load time.
	 * - Invokes the optional `onAdLoaded` callback.
	 *
	 * On failure:
	 * - Clears the ad reference.
	 * - Invokes the optional `onAdFailed` callback.
	 * - Automatically retries loading after 5 seconds using a delay handler.
	 *
	 * @param context The Android context used to load the ad (activity or application context).
	 * @param adUnitId The AdMob ad unit ID to use (defaults to REWARDED_INTERSTITIAL_AD_UNIT_ID).
	 * @param onAdLoaded Optional callback triggered when the ad loads successfully.
	 * @param onAdFailed Optional callback triggered when the ad fails to load.
	 */
	fun loadRewardedInterstitialAd(
		context: Context?,
		adUnitId: String? = null,
		onAdLoaded: (() -> Unit)? = null,
		onAdFailed: (() -> Unit)? = null
	) {
		// Avoid ad loading for premium users or if ads are disabled
		if (IS_PREMIUM_USER) return
		if (IS_AD_NOT_WORKING) return
		if (context == null) return
		
		// If already loaded, invoke callback and exit
		if (isRewardedInterstitialAdReady()) {
			onAdLoaded?.invoke()
			return
		}
		
		// Clean up any previous ad references
		cleanupAds(isNormalInterstitialAd = false)
		
		RewardedInterstitialAd.load(
			context,
			adUnitId ?: REWARDED_INTERSTITIAL_AD_UNIT_ID,
			AdRequest.Builder().build(),
			object : RewardedInterstitialAdLoadCallback() {
				
				override fun onAdLoaded(ad: RewardedInterstitialAd) {
					rewardedInterstitialAd = ad
					rewardedAdLoadTime = System.currentTimeMillis()
					onAdLoaded?.invoke()
				}
				
				override fun onAdFailedToLoad(error: LoadAdError) {
					rewardedInterstitialAd = null
					onAdFailed?.invoke()
					
					// Retry loading the ad after 1 seconds
					delay(1000, object : OnTaskFinishListener {
						override fun afterDelay() {
							loadRewardedInterstitialAd(context)
						}
					})
				}
			}
		)
	}
	
	/**
	 * Attempts to show a loaded interstitial ad if all required conditions are met.
	 *
	 * This method ensures that:
	 * - The user is not a premium user (ads are not shown to premium users).
	 * - Ads are not globally disabled due to known issues.
	 * - The interstitial ad is ready to be shown.
	 * - The associated activity is not destroyed.
	 * - No other interstitial ad is currently being shown.
	 * - The activity is not null.
	 *
	 * If all checks pass, the interstitial ad is shown with a delay.
	 * A callback can be optionally provided to be notified when the ad is closed.
	 *
	 * The method also handles:
	 * - Ad click and impression tracking.
	 * - Ad dismissal and failure to show.
	 * - Automatic reloading of the ad after it's dismissed.
	 *
	 * @param activity The activity context to show the interstitial ad in.
	 * @param onAdClosed An optional lambda to be executed when the ad is dismissed or skipped.
	 */
	fun showInterstitialAd(activity: Activity?, onAdClosed: (() -> Unit)? = null) {
		try {
			// Skip if user is premium or ads are disabled globally
			if (IS_PREMIUM_USER || IS_AD_NOT_WORKING) {
				onAdClosed?.invoke()
				return
			}
			
			// Skip if no ad is loaded
			if (!isInterstitialAdReady()) {
				onAdClosed?.invoke()
				return
			}
			
			// Skip if activity is not in a valid state
			if (isActivityDestroyed(activity)) {
				onAdClosed?.invoke()
				return
			}
			
			// Skip if another ad is already being shown
			if (isInterstitialAdShowing || isRewardedAdShowing) {
				onAdClosed?.invoke()
				return
			}
			
			// Skip if activity is null
			if (activity == null) {
				onAdClosed?.invoke()
				return
			}
			
			// Show the interstitial ad with callbacks
			interstitialAd?.let { adUnit ->
				adUnit.fullScreenContentCallback = object : FullScreenContentCallback() {
					override fun onAdShowedFullScreenContent() {
						super.onAdShowedFullScreenContent()
						cleanupAds(isNormalInterstitialAd = true, isAdShowing = true)
					}
					
					override fun onAdDismissedFullScreenContent() {
						cleanupAds(isNormalInterstitialAd = true)
						onAdClosed?.invoke()
						delay(500, object : OnTaskFinishListener {
							override fun afterDelay() = loadInterstitialAd(activity)
						})
					}
					
					override fun onAdFailedToShowFullScreenContent(error: AdError) {
						cleanupAds(isNormalInterstitialAd = true)
						onAdClosed?.invoke()
					}
					
					override fun onAdClicked() {
						super.onAdClicked()
						aioSettings.totalInterstitialAdClick++
						aioSettings.updateInStorage()
					}
					
					override fun onAdImpression() {
						super.onAdImpression()
						aioSettings.totalInterstitialImpression++
						aioSettings.updateInStorage()
					}
				}
				
				// Schedule ad to be shown with a delay
				handler.postDelayed({
					try {
						adUnit.show(activity)
						cleanupAds(isNormalInterstitialAd = true)
					} catch (error: Exception) {
						error.printStackTrace()
						cleanupAds(isNormalInterstitialAd = true)
						onAdClosed?.invoke()
					}
				}, AD_SHOW_DELAY_MS)
			}
		} catch (error: Exception) {
			error.printStackTrace()
			cleanupAds(isNormalInterstitialAd = true)
			onAdClosed?.invoke()
		}
	}
	
	/**
	 * Displays a rewarded interstitial ad if it is available and the user meets the required conditions.
	 *
	 * This method checks whether the ad is ready to be shown and whether the user has exceeded the
	 * download threshold, unless bypassed. It also handles the process of showing the ad, tracking
	 * user interactions with the ad (clicks, impressions), and invoking appropriate callbacks for
	 * ad completion or dismissal.
	 *
	 * @param activity The [Activity] context in which the ad will be displayed. It must not be destroyed
	 *                 or finishing.
	 * @param bypassCheckingDownloadNumber If true, skips the check for the number of downloads the user
	 *                                     has completed. By default, the ad will only be shown if the
	 *                                     user has exceeded a threshold of downloads.
	 * @param onAdCompleted Optional callback that is invoked when the user successfully completes the
	 *                      reward action (e.g., watched the entire ad).
	 * @param onAdClosed Optional callback that is invoked when the ad is dismissed or fails to load.
	 *                   This provides a way to handle the ad's lifecycle and perform cleanup.
	 */
	fun showRewardedInterstitialAd(
		activity: Activity?,
		bypassCheckingDownloadNumber: Boolean = false,
		onAdCompleted: (() -> Unit)? = null,
		onAdClosed: (() -> Unit)? = null
	) {
		// Skip if activity is null
		if (activity == null) {
			onAdClosed?.invoke()
			return
		}
		
		// Early exit if the user is premium or ads are disabled
		if (IS_PREMIUM_USER || IS_AD_NOT_WORKING) {
			onAdClosed?.invoke()
			return
		}
		
		// Check if the rewarded interstitial ad is ready to be shown
		if (!isRewardedInterstitialAdReady()) {
			onAdClosed?.invoke()
			return
		}
		
		// Skip if activity is not in a valid state
		if (isActivityDestroyed(activity)) {
			onAdClosed?.invoke()
			return
		}
		
		// Skip if another ad is already being shown
		if (isRewardedAdShowing || isInterstitialAdShowing) {
			onAdClosed?.invoke()
			return
		}
		
		// Check download threshold unless bypassed
		if (!bypassCheckingDownloadNumber) {
			val numberOfDownloadUserDid = aioSettings.numberOfDownloadsUserDid
			val maxNumberOfDownloadThreshold = aioSettings.numberOfMaxDownloadThreshold
			if (numberOfDownloadUserDid < maxNumberOfDownloadThreshold) return
		}
		
		// Set up the rewarded interstitial ad's callbacks
		rewardedInterstitialAd?.let { rewardAd ->
			rewardAd.fullScreenContentCallback = object : FullScreenContentCallback() {
				override fun onAdShowedFullScreenContent() {
					cleanupAds(isNormalInterstitialAd = false, isAdShowing = true)
				}
				
				override fun onAdDismissedFullScreenContent() {
					cleanupAds(isNormalInterstitialAd = false)
					onAdClosed?.invoke()
				}
				
				override fun onAdFailedToShowFullScreenContent(error: AdError) {
					cleanupAds(isNormalInterstitialAd = false)
					onAdClosed?.invoke()
				}
				
				override fun onAdClicked() {
					aioSettings.totalRewardedAdClick++
					aioSettings.updateInStorage()
				}
				
				override fun onAdImpression() {
					aioSettings.totalRewardedImpression++
					aioSettings.updateInStorage()
				}
			}
			
			try {
				rewardAd.show(activity) { _ ->
					onAdCompleted?.invoke()
					// Reset the download counter and notify the completion callback
					aioSettings.numberOfDownloadsUserDid = 0
					aioSettings.updateInStorage()
				}
			} catch (error: Exception) {
				error.printStackTrace()
				cleanupAds(isNormalInterstitialAd = false)
				onAdClosed?.invoke()
			}
		} ?: run {
			onAdClosed?.invoke()
		}
	}
	
	/**
	 * Checks whether the given [Activity] is finished or destroyed.
	 *
	 * This method is used to ensure that the activity is still in a valid state before attempting to show an ad.
	 * It helps avoid potential crashes caused by showing an ad on a destroyed or finishing activity.
	 *
	 * @param activity The [Activity] to check for its destruction or finishing status.
	 * @return true if the activity is either finishing or destroyed, false otherwise.
	 */
	private fun isActivityDestroyed(activity: Activity?): Boolean {
		return activity?.isFinishing == true || activity?.isDestroyed == true
	}
	
	/**
	 * Checks if an interstitial ad is loaded, valid, and ready to be displayed.
	 *
	 * This method checks whether the interstitial ad is currently available and has not expired.
	 * It helps to avoid showing an expired or null ad, ensuring the ad is ready to be shown.
	 *
	 * @return true if the interstitial ad is available and valid, false otherwise.
	 */
	fun isInterstitialAdReady(): Boolean {
		val adValid = interstitialAd != null &&
				(System.currentTimeMillis() - interstitialAdLoadTime < AD_EXPIRATION_TIME)
		return adValid
	}
	
	/**
	 * Checks if a rewarded interstitial ad is loaded, valid, and ready to be displayed.
	 *
	 * This method checks whether the rewarded interstitial ad is currently available and has not expired.
	 * It helps to avoid showing an expired or null ad, ensuring the ad is ready to be shown.
	 *
	 * @return true if the rewarded interstitial ad is available and valid, false otherwise.
	 */
	fun isRewardedInterstitialAdReady(): Boolean {
		val adValid = rewardedInterstitialAd != null &&
				(System.currentTimeMillis() - rewardedAdLoadTime < AD_EXPIRATION_TIME)
		return adValid
	}
	
}