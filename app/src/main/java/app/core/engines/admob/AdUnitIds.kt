package app.core.engines.admob

import app.core.AIOApp.Companion.INSTANCE
import com.aio.R

/**
 * Object containing all AdMob ad unit IDs used in the application.
 * These IDs are retrieved from string resources to allow for easy configuration
 * and environment-specific values (e.g., test vs production).
 *
 * All ad unit IDs are loaded from the application's string resources at runtime.
 */
object AdUnitIds {
	
	/**
	 * Ad unit ID for app open ads.
	 * These ads appear when users open or switch back to the app.
	 */
	val APP_OPEN_AD_UNIT_ID = INSTANCE.getString(R.string.text_app_open_ad_unit_id)
	
	/**
	 * Ad unit ID for fixed-size banner ads.
	 * These banners maintain a constant size regardless of screen dimensions.
	 */
	val FIXED_SIZED_BANNER_UNIT_ID = INSTANCE.getString(R.string.text_fixed_sized_banner_ad_unit_id)
	
	/**
	 * Ad unit ID for interstitial ads.
	 * Full-screen ads that appear at natural transition points in the app.
	 */
	val INTERSTITIAL_AD_UNIT_ID = INSTANCE.getString(R.string.text_interstitial_ad_unit_id)
	
	/**
	 * Ad unit ID for rewarded interstitial ads.
	 * Combines aspects of interstitial and rewarded ads in one format.
	 */
	val REWARDED_INTERSTITIAL_AD_UNIT_ID = INSTANCE.getString(R.string.text_rewarded_interstitial_ad_unit_id)
}