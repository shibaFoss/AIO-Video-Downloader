package app.core.engines.caches

import app.core.AIOApp
import com.aio.R
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory.fromRawRes
import lib.process.ThreadsUtility

class AIORawFiles {
    private var loadingComposition: LottieComposition? = null
    private var openActiveTasksComposition: LottieComposition? = null
    private var downloadReadyComposition: LottieComposition? = null

    fun getCircleLoadingComposition(): LottieComposition? {
        return loadingComposition
    }

    fun getOpenActiveTasksAnimationComposition(): LottieComposition? {
        return openActiveTasksComposition
    }

    fun getDownloadFoundAnimationComposition(): LottieComposition? {
        return downloadReadyComposition
    }

    fun preloadLottieAnimation() {
        ThreadsUtility.executeInBackground(codeBlock = {

            fromRawRes(AIOApp.INSTANCE, R.raw.animation_circle_loading)
                .addListener { composition ->
                    loadingComposition = composition
                }

            fromRawRes(AIOApp.INSTANCE, R.raw.animation_active_tasks)
                .addListener { composition ->
                    openActiveTasksComposition = composition
                }

            fromRawRes(AIOApp.INSTANCE, R.raw.animation_videos_found)
                .addListener { composition ->
                    downloadReadyComposition = composition
                }
        })
    }
}