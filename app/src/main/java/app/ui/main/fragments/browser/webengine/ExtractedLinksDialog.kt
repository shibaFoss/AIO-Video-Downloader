package app.ui.main.fragments.browser.webengine

import android.view.View
import android.view.View.GONE
import android.widget.ListView
import app.core.AIOApp
import app.core.AIOApp.Companion.admobHelper
import app.ui.main.fragments.browser.webengine.WebVideoParser.analyzeUrl
import com.aio.R
import com.google.android.gms.ads.AdView
import lib.ui.builders.DialogBuilder
import lib.ui.builders.ToastView

/**
 * A dialog that displays a list of extracted video URLs from a web page.
 *
 * This dialog is typically shown after video parsing has been performed on a web page.
 * It provides options to view and interact with available video links, clear them,
 * and also displays ads based on user status.
 *
 * @property webviewEngine The associated [WebViewEngine] managing the current web session.
 * @property listOfVideoUrlInfos A list of parsed video information objects to be displayed.
 */
class ExtractedLinksDialog(
	private val webviewEngine: WebViewEngine,
	private val listOfVideoUrlInfos: ArrayList<VideoUrlInfo>
) {
	
	/** The parent activity associated with the web view engine. */
	private val motherActivity = webviewEngine.safeMotherActivityRef
	
	/** Dialog builder responsible for creating and displaying the dialog UI. */
	private val dialogBuilder: DialogBuilder = DialogBuilder(motherActivity)
	
	/** ListView component to display the list of extracted links. */
	private var linkListView: ListView? = null
	
	/** AdMob banner ad view shown in the dialog. */
	private var admobAdView: AdView? = null
	
	init {
		dialogBuilder.setView(R.layout.dialog_extracted_links)
		dialogBuilder.view.apply { setupDialogViews() }
		
		loadAdmobInterstitialAd()
		setupAdmobAdview()
		setupLinkListAdapter()
	}
	
	/**
	 * Displays the dialog to the user.
	 */
	fun show() {
		dialogBuilder.show()
	}
	
	/**
	 * Closes the dialog.
	 */
	fun close() {
		dialogBuilder.close()
	}
	
	/**
	 * Initializes view references and button click listeners within the dialog layout.
	 */
	private fun View.setupDialogViews() {
		admobAdView = findViewById(R.id.admob_fixed_sized_banner_ad)
		linkListView = findViewById(R.id.list_extracted_video_urls)
		
		val mapOfButtonActions = mapOf(
			R.id.button_dialog_positive_container to { close() },
			R.id.button_dialog_negative_container to { clearAllExtractedLinks() },
		)
		
		mapOfButtonActions.forEach { (viewResId, action) ->
			findViewById<View>(viewResId)?.setOnClickListener { action() }
		}
	}
	
	/**
	 * Sets up the adapter to display the list of extracted video URLs.
	 */
	private fun setupLinkListAdapter() {
		linkListView?.adapter = ExtractedLinksAdapter(
			extractedLinksDialog = this,
			webviewEngine = webviewEngine,
			listOfVideoUrlInfos = listOfVideoUrlInfos
		)
	}
	
	/**
	 * Configures the AdMob banner ad inside the dialog based on user's premium status.
	 */
	private fun setupAdmobAdview() {
		admobAdView?.let {
			if (!AIOApp.IS_PREMIUM_USER) {
				admobHelper.loadBannerAd(it)
			} else {
				it.visibility = GONE
				dialogBuilder.view.apply {
					findViewById<View>(R.id.ad_space_container).visibility = GONE
				}
			}
		}
	}
	
	/**
	 * Preloads an interstitial ad when the dialog is initialized.
	 */
	private fun loadAdmobInterstitialAd() {
		admobHelper.loadInterstitialAd(motherActivity)
	}
	
	/**
	 * Clears all extracted video links for the current web view and re-triggers parsing.
	 * Also shows a success toast and closes the dialog.
	 */
	private fun clearAllExtractedLinks() {
		try {
			val currentWebVideosLibrary = webviewEngine.listOfWebVideosLibrary
				.find { it.webViewId == webviewEngine.currentWebView?.id }
			
			listOfVideoUrlInfos.forEach { link ->
				currentWebVideosLibrary?.listOfAvailableVideoUrlInfos?.remove(link)
			}
			
			analyzeUrl(webviewEngine.currentWebView?.url, webviewEngine)
			ToastView.showToast(msgId = R.string.title_successful)
			close()
		} catch (error: Exception) {
			error.printStackTrace()
			close()
		}
	}
}