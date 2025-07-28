package app.ui.others.information

import android.view.View
import android.view.View.GONE
import android.widget.TextView
import app.core.AIOApp
import app.core.AIOApp.Companion.admobHelper
import app.core.bases.BaseActivity
import com.aio.R
import com.google.android.gms.ads.AdView
import lib.texts.CommonTextUtils.fromHtmlStringToSpanned
import lib.texts.CommonTextUtils.getHtmlString
import java.lang.ref.WeakReference

/**
 * Activity responsible for displaying the app's privacy policy to the user.
 *
 * This screen presents static HTML-formatted text from the app's raw resources.
 * It is designed to be lightweight and user-friendly, with scrollable text and
 * optional advertisements based on the user's premium status.
 *
 * Features:
 * - Displays HTML-formatted privacy policy content
 * - Supports back navigation with fade-out animation
 * - Integrates a fixed-size AdMob banner (hidden for premium users)
 *
 * Layout: [R.layout.activity_privacy_1]
 */
class PrivacyPolicyActivity : BaseActivity() {
	
	// Weak reference to this activity to help avoid memory leaks
	private val weakSelfReference = WeakReference(this)
	private val safePrivacyPolicyActivityRef = weakSelfReference.get()
	
	/**
	 * Provides the layout resource to be inflated when the activity is created.
	 *
	 * @return Resource ID of the layout file used for this screen.
	 */
	override fun onRenderingLayout(): Int {
		return R.layout.activity_privacy_1
	}
	
	/**
	 * Called after the layout is rendered. Performs initial setup:
	 * - Applies light system UI theme
	 * - Initializes views and sets content
	 */
	override fun onAfterLayoutRender() {
		setLightSystemBarTheme()
		initializeViews()
	}
	
	/**
	 * Handles the back press action by closing the activity with no animation.
	 */
	override fun onBackPressActivity() {
		closeActivityWithFadeAnimation(shouldAnimate = false)
	}
	
	/**
	 * Clears the weak reference to this activity to prevent memory leaks.
	 * Always called during the activity's destruction lifecycle.
	 */
	override fun clearWeakActivityReference() {
		weakSelfReference.clear()
		super.clearWeakActivityReference()
	}
	
	/**
	 * Initializes all views in the layout:
	 *
	 * - Sets a click listener on the back button
	 * - Loads the privacy policy HTML from raw resources
	 * - Displays the formatted text in a TextView
	 * - Loads a banner ad for non-premium users
	 * - Hides ad container for premium users
	 */
	private fun initializeViews() {
		// Set up back button action
		findViewById<View>(R.id.button_left_actionbar).apply {
			setOnClickListener { onBackPressActivity() }
		}
		
		// Load and display the HTML content from raw resources
		try {
			val contentHTML = fromHtmlStringToSpanned(getHtmlString(R.raw.msg_privacy_policy))
			findViewById<TextView>(R.id.text_privacy_policy).text = contentHTML
		} catch (error: Exception) {
			error.printStackTrace()
		}
		
		// Load AdMob banner ad if user is not premium
		val admobView: AdView = findViewById(R.id.admob_fixed_sized_banner_ad)
		admobHelper.loadBannerAd(admobView)
		
		// Hide ad space if user has premium access
		if (AIOApp.IS_PREMIUM_USER) {
			findViewById<View>(R.id.ad_space_container).visibility = GONE
		}
	}
}