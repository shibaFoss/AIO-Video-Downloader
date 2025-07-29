package app.core.engines.caches

import app.core.AIOApp
import com.aio.R
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory.fromRawRes
import lib.process.ThreadsUtility
import lib.process.ThreadsUtility.executeInBackground

/**
 * AIORawFiles - Lottie Animation Cache Manager
 *
 * A centralized cache for preloading and managing Lottie animations used throughout the application.
 * This class improves performance by:
 * 1. Loading animations in the background during app initialization
 * 2. Maintaining strong references to prevent garbage collection
 * 3. Providing thread-safe access to cached animations
 *
 * Usage:
 * 1. Access animations through the provided getter methods
 * 2. All animations are loaded asynchronously during class initialization
 * 3. Returns null if animation hasn't finished loading when requested
 */
class AIORawFiles {

    /**
     * Cached composition for circular gradient motion animation
     * Used for: Loading indicators, progress animations
     */
    private var circularMotionComposition: LottieComposition? = null

    /**
     * Cached composition for colored circle loading animation
     * Used for: General loading states
     */
    private var circleLoadingComposition: LottieComposition? = null

    /**
     * Cached composition for empty box animation
     * Used for: Empty state illustrations
     */
    private var emptyBoxAnimationComposition: LottieComposition? = null

    /**
     * Cached composition for active tasks animation
     * Used for: Background task indicators
     */
    private var openActiveTasksAnimationComposition: LottieComposition? = null

    /**
     * Cached composition for video parsing animation
     * Used for: Video analysis/processing states
     */
    private var downloadParsingAnimationComposition: LottieComposition? = null

    /**
     * Cached composition for download found animation
     * Used for: Success states when media is found
     */
    private var downloadFoundAnimationComposition: LottieComposition? = null

    /**
     * Retrieves the circular motion animation composition.
     * @return Cached LottieComposition or null if not yet loaded
     */
    fun getCircularMotionComposition(): LottieComposition? {
        return circularMotionComposition
    }

    /**
     * Retrieves the circle loading animation composition.
     * @return Cached LottieComposition or null if not yet loaded
     */
    fun getCircleLoadingComposition(): LottieComposition? {
        return circleLoadingComposition
    }

    /**
     * Retrieves the empty box animation composition.
     * @return Cached LottieComposition or null if not yet loaded
     */
    fun getEmptyBoxAnimComposition(): LottieComposition? {
        return emptyBoxAnimationComposition
    }

    /**
     * Retrieves the active tasks animation composition.
     * @return Cached LottieComposition or null if not yet loaded
     */
    fun getOpenActiveTasksAnimationComposition(): LottieComposition? {
        return openActiveTasksAnimationComposition
    }

    /**
     * Retrieves the video parsing animation composition.
     * @return Cached LottieComposition or null if not yet loaded
     */
    fun getDownloadParsingAnimationComposition(): LottieComposition? {
        return downloadParsingAnimationComposition
    }

    /**
     * Retrieves the download found animation composition.
     * @return Cached LottieComposition or null if not yet loaded
     */
    fun getDownloadFoundAnimationComposition(): LottieComposition? {
        return downloadFoundAnimationComposition
    }

    /**
     * Preloads all Lottie animations from raw resources into memory cache.
     * Each animation is loaded asynchronously with a completion listener.
     *
     * Note: Runs on background thread during initialization.
     */
    fun preloadLottieAnimation() {
        ThreadsUtility.executeInBackground(codeBlock = {
            // Circular gradient motion (loading/progress)
            fromRawRes(AIOApp.INSTANCE, R.raw.animation_circular_gradient)
                .addListener { composition ->
                    circularMotionComposition = composition
                }

            // Colored circle loading indicator
            fromRawRes(AIOApp.INSTANCE, R.raw.anim_color_circle_loading)
                .addListener { composition ->
                    circleLoadingComposition = composition
                }

            // Empty state illustration
            fromRawRes(AIOApp.INSTANCE, R.raw.animation_empty_box)
                .addListener { composition ->
                    emptyBoxAnimationComposition = composition
                }

            // Background tasks indicator
            fromRawRes(AIOApp.INSTANCE, R.raw.animation_active_tasks)
                .addListener { composition ->
                    openActiveTasksAnimationComposition = composition
                }

            // Video processing state
            fromRawRes(AIOApp.INSTANCE, R.raw.animation_video_parsing)
                .addListener { composition ->
                    downloadParsingAnimationComposition = composition
                }

            // Media found success state
            fromRawRes(AIOApp.INSTANCE, R.raw.animation_videos_found)
                .addListener { composition ->
                    downloadFoundAnimationComposition = composition
                }
        })
    }
}