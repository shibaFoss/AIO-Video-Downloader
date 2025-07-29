package app.ui.others.information

import android.view.View
import android.widget.TextView
import app.core.bases.BaseActivity
import com.aio.R
import lib.texts.CommonTextUtils.fromHtmlStringToSpanned
import lib.texts.CommonTextUtils.getHtmlString
import java.lang.ref.WeakReference

/**
 * Displays the application's Terms & Conditions to the user.
 *
 * The content is read from a raw HTML file and rendered in a TextView.
 * This activity provides a simple, scrollable interface to review the legal
 * information, and optionally displays a banner ad for non-premium users.
 *
 * Typical use case: Accessed via the app’s legal or settings section.
 */
class TermsConditionsActivity : BaseActivity() {
	
	// Weak reference to the activity instance to prevent memory leaks
	private val weakSelfReference = WeakReference(this)
	private val safeTermsConditionsActivityRef = weakSelfReference.get()
	
	/**
	 * Specifies the layout resource used to render this activity.
	 *
	 * @return Resource ID of the layout file for the terms and conditions screen.
	 */
	override fun onRenderingLayout(): Int {
		return R.layout.activity_terms_con_1
	}
	
	/**
	 * Called after the layout is fully rendered.
	 * Sets up the system UI and initializes all views.
	 */
	override fun onAfterLayoutRender() {
		setLightSystemBarTheme()
		initializeViews()
	}
	
	/**
	 * Handles the back press behavior.
	 * Closes the activity immediately without animation.
	 */
	override fun onBackPressActivity() {
		closeActivityWithFadeAnimation(shouldAnimate = false)
	}
	
	/**
	 * Clears the weak reference to this activity.
	 * Helps in avoiding memory leaks during the lifecycle teardown.
	 */
	override fun clearWeakActivityReference() {
		weakSelfReference.clear()
		super.clearWeakActivityReference()
	}
	
	/**
	 * Initializes and configures view components:
	 *
	 * - Sets up the action bar's back button.
	 * - Loads and displays the Terms & Conditions HTML content.
	 * - Loads an AdMob banner for non-premium users.
	 * - Hides ad space if the user has a premium subscription.
	 */
	private fun initializeViews() {
		// Configure back button behavior
		findViewById<View>(R.id.button_left_actionbar).apply {
			setOnClickListener { onBackPressActivity() }
		}
		
		// Render HTML content into TextView
		try {
			val contentHTML = fromHtmlStringToSpanned(getHtmlString(R.raw.msg_terms_condition))
			findViewById<TextView>(R.id.text_terms_policy).text = contentHTML
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
}