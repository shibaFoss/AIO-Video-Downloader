package app.core.engines.caches

import app.core.AIOApp
import com.aio.R
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory.fromRawRes
import lib.process.ThreadsUtility.executeInBackground

/**
 * AIORawFiles is responsible for loading and caching Lottie animation resources used throughout the app.
 *
 * This class preloads frequently used Lottie compositions in a background thread to ensure
 * smoother playback and reduced loading times when animations are needed in the UI.
 */
class AIORawFiles {
    
    // Cached Lottie composition objects for different animation resources
    private var circularMotionComposition: LottieComposition? = null
    private var gradientMotionComposition: LottieComposition? = null
    private var emptyDownloadAnimationComposition: LottieComposition? = null
    private var openActiveTasksAnimationComposition: LottieComposition? = null
    private var downloadParsingAnimationComposition: LottieComposition? = null
    private var downloadFoundAnimationComposition: LottieComposition? = null
    
    /**
     * Initializes the class and triggers preloading of animations in a background thread.
     */
    init {
        executeInBackground(codeBlock = { preloadLottieAnimation() })
    }
    
    /**
     * Returns the cached Lottie composition for the circular loading animation.
     */
    fun getCircularMotionComposition(): LottieComposition? {
        return circularMotionComposition
    }
    
    /**
     * Returns the cached Lottie composition for the fullscreen gradient animation.
     */
    fun getGradientMotionComposition(): LottieComposition? {
        return gradientMotionComposition
    }
    
    /**
     * Returns the cached Lottie composition used for the empty downloads screen.
     */
    fun getEmptyDownloadAnimComposition(): LottieComposition? {
        return emptyDownloadAnimationComposition
    }
    
    /**
     * Returns the cached Lottie composition used to animate active download tasks.
     */
    fun getOpenActiveTasksAnimationComposition(): LottieComposition? {
        return openActiveTasksAnimationComposition
    }
    
    /**
     * Returns the cached Lottie composition used during the video parsing process.
     */
    fun getDownloadParsingAnimationComposition(): LottieComposition? {
        return downloadParsingAnimationComposition
    }
    
    /**
     * Returns the cached Lottie composition used when a downloadable video is found.
     */
    fun getDownloadFoundAnimationComposition(): LottieComposition? {
        return downloadFoundAnimationComposition
    }
    
    /**
     * Preloads all required Lottie compositions from raw resources into memory.
     * This is done asynchronously to avoid blocking the UI thread.
     */
    fun preloadLottieAnimation() {
        fromRawRes(AIOApp.INSTANCE, R.raw.animation_circular_gradient)
            .addListener { composition -> circularMotionComposition = composition }
        
        fromRawRes(AIOApp.INSTANCE, R.raw.fullscreen_gradient_anim)
            .addListener { composition -> gradientMotionComposition = composition }
        
        fromRawRes(AIOApp.INSTANCE, R.raw.animation_empty_box)
            .addListener { composition -> emptyDownloadAnimationComposition = composition }
        
        fromRawRes(AIOApp.INSTANCE, R.raw.animation_active_tasks)
            .addListener { composition -> openActiveTasksAnimationComposition = composition }
        
        fromRawRes(AIOApp.INSTANCE, R.raw.animation_video_parsing)
            .addListener { composition -> downloadParsingAnimationComposition = composition }
        
        fromRawRes(AIOApp.INSTANCE, R.raw.animation_videos_found)
            .addListener { composition -> downloadFoundAnimationComposition = composition }
    }
}