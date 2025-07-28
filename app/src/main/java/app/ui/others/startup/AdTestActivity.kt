package app.ui.others.startup

import android.view.View
import app.core.AIOApp
import app.core.bases.BaseActivity
import com.aio.R

class AdTestActivity : BaseActivity() {
	
	override fun onRenderingLayout(): Int {
		return R.layout.ad_test_activity_1
	}
	
	override fun onAfterLayoutRender() {
		getActivity()?.let { safeActivityRef ->
			findViewById<View>(R.id.click_here).setOnClickListener {
				AIOApp.admobHelper.loadInterstitialAd(safeActivityRef, onAdLoaded = {
					AIOApp.admobHelper.showInterstitialAd(safeActivityRef)
				})
			}
		}
	}
	
	override fun onBackPressActivity() {
		closeActivityWithFadeAnimation(true)
	}
}