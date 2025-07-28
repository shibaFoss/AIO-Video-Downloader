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
 * Displays the application's content policy in a user-friendly format.
 *
 * Features:
 * - Shows HTML-formatted content loaded from a raw resource.
 * - Includes a back navigation button in the action bar.
 * - Loads a fixed-size banner AdMob ad if the user is not premium.
 *
 * Layout: `activity_content_policy_1`
 */
class ContentPolicyActivity : BaseActivity() {
	
	// Weak reference to this activity for preventing memory leaks
	private val weakSelfReference = WeakReference(this)
	private val safeConditionsActivityRef = weakSelfReference.get()
	
	/**
	 * Specifies the layout file to render.
	 * @return Resource ID of the layout.
	 */
	override fun onRenderingLayout(): Int {
		return R.layout.activity_content_policy_1
	}
	
	/**
	 * Called after layout has been set.
	 * Performs theme adjustments and initializes UI components.
	 */
	override fun onAfterLayoutRender() {
		setLightSystemBarTheme()
		initViewElements()
	}
	
	/**
	 * Clears the weak reference to this activity.
	 * Ensures that memory is released when activity is destroyed.
	 */
	override fun clearWeakActivityReference() {
		weakSelfReference.clear()
		super.clearWeakActivityReference()
	}
	
	/**
	 * Handles the behavior when back is pressed.
	 * Closes the activity with an optional fade animation.
	 */
	override fun onBackPressActivity() {
		closeActivityWithFadeAnimation(shouldAnimate = false)
	}
	
	/**
	 * Initializes the view components and UI logic:
	 * - Sets up the action bar back button.
	 * - Loads and displays the content policy from a raw resource.
	 * - Manages AdMob visibility based on premium status.
	 */
	private fun initViewElements() {
		// Set listener for the left action bar back button
		findViewById<View>(R.id.button_left_actionbar).apply {
			setOnClickListener { onBackPressActivity() }
		}
		
		// Load and display the content policy text in HTML format
		try {
			val contentHTML = fromHtmlStringToSpanned(getHtmlString(R.raw.msg_content_policy))
			findViewById<TextView>(R.id.text_content_policy).text = contentHTML
		} catch (error: Exception) {
			error.printStackTrace()
		}
		
		// Set up AdMob banner if user is not premium
		val admobView: AdView = findViewById(R.id.admob_fixed_sized_banner_ad)
		admobHelper.loadBannerAd(admobView)
		
		// Hide ad space for premium users
		if (AIOApp.IS_PREMIUM_USER) {
			findViewById<View>(R.id.ad_space_container).visibility = GONE
		}
	}
}
