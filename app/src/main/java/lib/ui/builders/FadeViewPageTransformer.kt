package lib.ui.builders

import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2

/**
 * A [ViewPager2.PageTransformer] implementation that applies a simple fade in/out effect
 * to each page during a transition.
 *
 * This transformer removes the default sliding animation by offsetting the page translation
 * and instead fades pages in and out based on their position.
 *
 * Use this transformer when you want a soft visual transition between ViewPager2 pages.
 */
class FadeViewPageTransformer : ViewPager.PageTransformer {

    /**
     * Applies a transformation to the given page based on its position relative to the current front-and-center page.
     *
     * @param page The currently visible page to transform.
     * @param position The position of the page relative to the current front-and-center position:
     *   - Position `0` is the currently centered page.
     *   - Position `1` is the page immediately to the right, `-1` is the page to the left.
     *   - Values beyond `-1` or `1` are off-screen.
     */
    override fun transformPage(page: android.view.View, position: Float) {
        page.apply {
            when {
                // Page is way off-screen to the left.
                position < -1 -> {
                    alpha = 0f
                }

                // Page is visible. Fade it based on its position.
                position <= 1 -> {
                    alpha = 1 - kotlin.math.abs(position)
                    translationX = page.width * -position
                }

                // Page is way off-screen to the right.
                else -> {
                    alpha = 0f
                }
            }
        }
    }
}
