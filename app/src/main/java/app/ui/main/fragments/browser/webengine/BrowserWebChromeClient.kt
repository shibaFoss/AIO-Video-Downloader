package app.ui.main.fragments.browser.webengine

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import app.core.AIOApp.Companion.aioFavicons
import app.core.AIOApp.Companion.aioHistory
import app.core.AIOApp.Companion.aioSettings
import app.core.bases.BaseActivity
import app.core.engines.browser.history.HistoryModel
import app.ui.main.MotherActivity
import com.aio.R
import com.bumptech.glide.Glide
import lib.process.AsyncJobUtils.executeInBackground
import lib.process.AsyncJobUtils.executeOnMainThread
import lib.texts.CommonTextUtils.getText
import lib.ui.ViewUtility.closeAnyAnimation
import java.io.File
import java.util.Date

class BrowserWebChromeClient(val webviewEngine: WebViewEngine) : WebChromeClient() {
	
	private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
	private var customView: View? = null
	private var videoView: View? = null
	private var customViewCallback: CustomViewCallback? = null
	
	override fun onProgressChanged(webView: WebView?, progress: Int) {
		if (webviewEngine.currentWebView != webView) return
		webviewEngine.updateWebViewProgress(webView, progress)
	}
	
	override fun onCreateWindow(
		view: WebView?, isDialog: Boolean,
		isUserGesture: Boolean, resultMsg: Message?
	): Boolean {
		if (webviewEngine.currentWebView != view) return false
		
		val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
		val tempWebView = webviewEngine.generateNewWebview() as WebView
		tempWebView.apply {
			layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
			webViewClient = object : WebViewClient() {
				override fun onPageFinished(view: WebView, url: String) {
					webviewEngine.safeMotherActivityRef.sideNavigation?.addNewBrowsingTab(url, webviewEngine)
					(parent as? ViewGroup)?.removeView(this@apply)
				}
			}
		}
		
		transport.webView = tempWebView
		if (!aioSettings.browserEnablePopupBlocker) {
			resultMsg.sendToTarget()
		} else {
			val messageResId = getText(R.string.text_blocked_unwanted_popup_links)
			webviewEngine.showQuickBrowserInfo(messageResId)
		}
		return true
	}
	
	override fun onReceivedIcon(webview: WebView, icon: Bitmap?) {
		super.onReceivedIcon(webview, icon)
		try {
			if (webviewEngine.currentWebView != webview) return
			if (icon != null) {
				webviewEngine.browserFragment.browserFragmentTop.animateDefaultFaviconLoading(true)
				val webViewFavicon = webviewEngine.browserFragment.browserFragmentTop.webViewFavicon
				Glide.with(webview.context).load(icon).into(webViewFavicon)
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	override fun onReceivedTitle(webView: WebView?, title: String?) {
		if (webviewEngine.currentWebView != webView) return
		try {
			val motherActivity = webView?.context as MotherActivity
			title?.let { titleText ->
				motherActivity.let { activity ->
					activity.browserFragment?.let {
						it.browserFragmentTop.webviewTitle.text = titleText
						activity.sideNavigation?.webTabListAdapter?.notifyDataSetChanged()
						val correspondingWebViewUrl = webView.url.toString()
						
						executeInBackground {
							executeOnMainThread { it.browserFragmentTop.animateDefaultFaviconLoading(true) }
							val faviconCachedPath = aioFavicons.getFavicon(correspondingWebViewUrl)
							if (!faviconCachedPath.isNullOrEmpty()) {
								val faviconImg = File(faviconCachedPath)
								if (faviconImg.exists()) {
									executeOnMainThread {
										it.browserFragmentTop.webViewFavicon.let { faviconViewer ->
											faviconViewer.setImageURI(Uri.fromFile(faviconImg))
											closeAnyAnimation(faviconViewer)
										}
									}
								} else {
									executeOnMainThread {
										val defaultFaviconResId = R.drawable.ic_button_browser_favicon
										it.browserFragmentTop.webViewFavicon.let { faviconViewer ->
											Glide.with(motherActivity).load(defaultFaviconResId).into(faviconViewer)
											closeAnyAnimation(faviconViewer)
										}
									}
								}
							}
						}
					}
				}
				
				if (webView.url.toString() != "about:blank") {
					aioHistory.getHistoryLibrary().apply {
						val existingEntryIndex = indexOfFirst { it.historyUrl == webView.url.toString() }
						if (existingEntryIndex != -1) removeAt(existingEntryIndex)
					}
					
					aioHistory.getHistoryLibrary().add(0, HistoryModel().apply {
						historyUserAgent = webView.settings.userAgentString.toString()
						historyVisitDateTime = Date()
						historyUrl = webView.url.toString(); historyTitle = titleText
					})
					
					aioHistory.updateInStorage()
				}
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}
	}
	
	override fun onShowFileChooser(
		webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?,
		fileChooserParams: FileChooserParams?
	): Boolean {
		try {
			fileUploadCallback?.onReceiveValue(null)
			fileUploadCallback = filePathCallback
			
			val baseActivity = webView?.context as BaseActivity
			baseActivity.scopedStorageHelper?.openFilePicker(allowMultiple = true)
			baseActivity.scopedStorageHelper?.onFileSelected = { _, files ->
				val uris = files.map { it.uri }.toTypedArray()
				if (uris.isEmpty()) fileUploadCallback?.onReceiveValue(null)
				else fileUploadCallback?.onReceiveValue(uris)
				fileUploadCallback = null
			}
		} catch (error: Exception) {
			error.printStackTrace()
		}; return true
	}
	
	override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
		webviewEngine.currentWebView?.visibility = View.GONE
		val browserFragment = webviewEngine.browserFragment
		val browserFragmentBody = browserFragment.browserFragmentBody
		val webViewContainer = browserFragmentBody.webViewContainer
		webViewContainer.removeAllViews()
		
		view?.let { video ->
			videoView = video
			val wrapper = FrameLayout(video.context).apply {
				layoutParams = FrameLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT
				)
				addView(
					video, FrameLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT
					)
				)
			}
			
			webViewContainer.addView(wrapper)
			customView = wrapper
			customViewCallback = callback
			
			forceLandscapeRotation()
		}
	}
	
	private fun forceLandscapeRotation() {
		val browserFragment = webviewEngine.browserFragment
		val browserFragmentBody = browserFragment.browserFragmentBody
		val webViewContainer = browserFragmentBody.webViewContainer
		
		videoView?.let { view ->
			view.post {
				val containerWidth = webViewContainer.width
				val containerHeight = webViewContainer.height
				val params = view.layoutParams
				params.width = containerHeight
				params.height = containerWidth
				view.layoutParams = params
				
				view.pivotX = 0f
				view.pivotY = 0f
				view.rotation = 90f
				view.translationX = containerWidth.toFloat()
				view.translationY = 0f
			}
		}
	}
	
	override fun onHideCustomView() {
		videoView?.let { view ->
			view.rotation = 0f
			view.translationX = 0f
			view.translationY = 0f
			view.layoutParams = FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			)
		}
		
		videoView = null
		
		val browserFragment = webviewEngine.browserFragment
		val browserFragmentBody = browserFragment.browserFragmentBody
		val webViewContainer = browserFragmentBody.webViewContainer
		
		webViewContainer.removeAllViews()
		webViewContainer.addView(webviewEngine.currentWebView)
		webviewEngine.currentWebView?.visibility = View.VISIBLE
		
		customViewCallback?.onCustomViewHidden()
		customView = null
	}
}