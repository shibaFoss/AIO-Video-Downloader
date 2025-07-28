package app.ui.main.fragments.browser.webengine

import android.net.Uri
import android.view.View
import android.view.View.inflate
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import app.core.AIOApp.Companion.aioFavicons
import app.ui.main.MotherActivity
import app.ui.main.fragments.browser.BrowserFragment
import app.ui.main.fragments.browser.BrowserFragmentBody
import app.ui.main.fragments.browser.BrowserFragmentTop
import com.aio.R
import lib.networks.URLUtilityKT.removeWwwFromUrl
import lib.process.AsyncJobUtils.executeInBackground
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.process.CommonTimeUtils.OnTaskFinishListener
import lib.process.CommonTimeUtils.delay
import java.io.File

/**
 * Adapter class used to display a list of WebView instances as browser tabs
 * within a navigation drawer or similar component.
 *
 * @property browserFragment Reference to the associated [BrowserFragment].
 * @property listOfWebViews The list of active [WebView] instances (tabs).
 */
class WebTabListAdapter(
	val browserFragment: BrowserFragment,
	val listOfWebViews: ArrayList<WebView>
) : BaseAdapter() {
	
	val browserFragmentTop: BrowserFragmentTop = browserFragment.browserFragmentTop
	val browserFragmentBody: BrowserFragmentBody = browserFragment.browserFragmentBody
	val motherActivity: MotherActivity = browserFragment.safeMotherActivityRef
	val leftSideNavigation = motherActivity.sideNavigation
	
	override fun getCount(): Int {
		return listOfWebViews.size
	}
	
	override fun getItem(position: Int): Any {
		return listOfWebViews[position]
	}
	
	override fun getItemId(position: Int): Long {
		return position.toLong()
	}
	
	/**
	 * Returns the view corresponding to a tab item at a given position.
	 *
	 * @param position Index of the tab.
	 * @param convertView Reusable view for performance.
	 * @param parent The parent view group.
	 * @return Configured [View] representing a browser tab.
	 */
	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
		val correspondingWebView = listOfWebViews[position]
		var correspondingView = convertView
		if (correspondingView == null) {
			val layoutResId = R.layout.activity_mother_3_left_nav_item
			correspondingView = inflate(motherActivity, layoutResId, null)
		}
		
		val tabItemViewHolder = if (correspondingView!!.tag == null) {
			ViewHolder(position, correspondingView, motherActivity)
				.apply { correspondingView.tag = this }
		} else {
			(correspondingView.tag as ViewHolder)
		}
		
		tabItemViewHolder.updateView(correspondingWebView)
		return correspondingView
	}
	
	/**
	 * Custom method to notify data set changes, ensuring any associated video
	 * libraries are updated and synchronized with the current list of WebViews.
	 */
	fun notifyDataSetChangedOnSort() {
		super.notifyDataSetChanged()
		
		val webViewEngine = browserFragment.getBrowserWebEngine()
		val listOfVideosLibrary = webViewEngine.listOfWebVideosLibrary
		
		val webViewIds = listOfWebViews.map { it.id }
		webViewEngine.listOfWebVideosLibrary = listOfVideosLibrary.filter { library ->
			library.webViewId in webViewIds
		} as ArrayList<WebVideosLibrary>
	}
	
	/**
	 * ViewHolder class to cache and manage views for each browser tab item.
	 *
	 * @property position The tab's position in the adapter.
	 * @property layoutView The root layout view for the tab item.
	 * @property safeMotherActivityRef Reference to the [MotherActivity].
	 */
	data class ViewHolder(
		val position: Int, val layoutView: View,
		val safeMotherActivityRef: MotherActivity
	) {
		
		var itemClickableContainer: View = layoutView.findViewById(R.id.browser_tab_info)
		var browserFavicon: ImageView = layoutView.findViewById(R.id.browser_tab_favicon)
		var browserTabTitle: TextView = layoutView.findViewById(R.id.browser_tab_title)
		var browserTabUrl: TextView = layoutView.findViewById(R.id.browser_tab_url)
		var browserTabCloseButton: View = layoutView.findViewById(R.id.browser_tab_close_button)
		
		/**
		 * Updates the tab view contents based on the given [WebView].
		 *
		 * @param targetWebview The web view representing the tab.
		 */
		fun updateView(targetWebview: WebView) {
			safeMotherActivityRef.sideNavigation?.apply {
				// Set click listener to open the tab
				itemClickableContainer.setOnClickListener {
					openWebViewTab(targetWebview)
					delay(200, object : OnTaskFinishListener {
						override fun afterDelay() {
							this@ViewHolder.safeMotherActivityRef.openBrowserFragment()
							closeDrawerNavigation()
						}
					})
				}
				
				// Set click listener to close the tab
				browserTabCloseButton.setOnClickListener {
					closeWebViewTab(position, targetWebview)
				}
			}
			
			targetWebview.apply {
				val webpageUrl = targetWebview.url.toString()
				browserTabUrl.text = removeWwwFromUrl(webpageUrl)
				if (!title.isNullOrEmpty()) browserTabTitle.text = targetWebview.title.toString()
				else browserTabTitle.text = webpageUrl.ifEmpty {
					context.getString(R.string.text_waiting_for_server_to_respond)
				}
				
				// Load favicon asynchronously
				executeInBackground {
					val faviconCachedPath = aioFavicons.getFavicon(webpageUrl)
					if (!faviconCachedPath.isNullOrEmpty()) {
						val faviconImg = File(faviconCachedPath)
						if (faviconImg.exists()) {
							executeOnMainThread {
								browserFavicon.setImageURI(Uri.fromFile(faviconImg))
							}
						}
					}
				}
			}
		}
	}
}