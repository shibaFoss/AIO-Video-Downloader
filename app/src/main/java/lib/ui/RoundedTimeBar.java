package lib.ui;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.DefaultTimeBar;

/**
 * Custom implementation of a time bar with rounded corners, extending the {@link DefaultTimeBar}.
 * This class overrides the drawing behavior to clip the corners of the time bar for a smoother, rounded appearance.
 *
 * <p>It also provides customization options for the time bar's appearance through paint and clip paths.
 * The corner radius is configurable and is defined in density-independent pixels (DIP).
 * </p>
 */
@UnstableApi
public class RoundedTimeBar extends DefaultTimeBar {
    // Paint object used to configure the rendering of the time bar
    private final Paint roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Path used to define the clipping area with rounded corners
    private final Path clipPath = new Path();

    // RectF object representing the bounds of the time bar
    private final RectF rectF = new RectF();

    // Corner radius for the rounded corners of the time bar
    private final float cornerRadius;

    /**
     * Constructor that initializes the RoundedTimeBar with context and attributes.
     *
     * @param context The context used to access resources
     * @param attrs The set of attributes that may customize the view (such as corner radius, etc.)
     */
    public RoundedTimeBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Get the display metrics for the current screen to calculate the corner radius in DIP
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        // Set the corner radius (default value of 10 DIP)
        cornerRadius = applyDimension(COMPLEX_UNIT_DIP, 10, displayMetrics);
    }

    /**
     * Called when the size of the view has changed. This method updates the clipping path
     * to reflect the new size of the time bar.
     *
     * @param w The new width of the time bar
     * @param h The new height of the time bar
     * @param oldw The old width of the time bar
     * @param oldh The old height of the time bar
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Set the bounds of the time bar to match the new width and height
        rectF.set(0, 0, w, h);

        // Reset and redefine the clipping path with rounded corners
        clipPath.reset();
        clipPath.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW);
    }

    /**
     * Draws the time bar with rounded corners by clipping the canvas to the rounded path
     * before drawing the default time bar content.
     *
     * @param canvas The canvas to draw the time bar on
     */
    @Override
    public void onDraw(Canvas canvas) {
        // Save the current canvas state
        canvas.save();

        // Apply the clipping path to the canvas
        canvas.clipPath(clipPath);

        // Call the super method to draw the default time bar
        super.onDraw(canvas);

        // Restore the canvas state
        canvas.restore();
    }

    /**
     * Returns the {@link Paint} object used to configure the appearance of the time bar.
     * The {@link Paint} is set to use anti-aliasing, but can be customized further if desired.
     *
     * @return The {@link Paint} object used for rendering the time bar
     */
    public Paint getRoundPaint() {
        return roundPaint;
    }
}