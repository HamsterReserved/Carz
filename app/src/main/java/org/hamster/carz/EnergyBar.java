package org.hamster.carz;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

/**
 * Created by Hamster on 2015/12/21.
 * <p/>
 * A view that shows an energy bar, just like the aged Nokia phones which show battery and signal level
 * at left and right side of screen.
 * <p/>
 * layout_gravity
 */
public class EnergyBar extends View {

    private static final String TAG = "Carz_EnergyBar";
    private static final boolean VDBG = false;
    private int mLayoutGravity;
    private int mBackgroundColor;
    private int mSpacing;
    private int mBarCount;
    private Paint mPaint;
    /**
     * baseX/Y: where to start drawing (the first bar)
     * deltaX/Y: distance between two bars, including spacing
     * barWidthBase/HeightBase: base size of a (unit 1) bar
     * barWidthDelta/HeightDelta: increment of bar width/height
     * maxWidth/Height: size of canvas
     * drawWidth/Height: only draw below this threshold (like battery indicator/masking the rest of bars)
     */
    private int baseX, baseY,
            deltaX, deltaY,
            barWidthBase, barHeightBase,
            barWidthDelta, barHeightDelta,
            maxHeight, maxWidth,
            drawMaxWidth, drawMaxHeight;

    public EnergyBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        int[] set = {
                R.attr.spacing,
                R.attr.barCount,
                R.attr.barColor,
                android.R.attr.layout_gravity,
        };

        TypedArray a = context.obtainStyledAttributes(attrs, set);
        mBackgroundColor = a.getColor(2, getResources().getColor(android.R.color.holo_red_dark));
        mSpacing = a.getDimensionPixelSize(0, 24);
        mBarCount = a.getInt(1, 7);
        mLayoutGravity = a.getInt(3, Gravity.LEFT);
        a.recycle();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mBackgroundColor);
    }

    public EnergyBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public EnergyBar(Context context) {
        super(context);
    }

    private void calculateSizes() {
        switch (mLayoutGravity) {
            case Gravity.LEFT:
                baseX = 0;
                baseY = maxHeight;
                barWidthBase = maxWidth / mBarCount;
                barWidthDelta = barWidthBase;
                barHeightBase = (maxHeight - ((mBarCount - 1) * mSpacing)) / mBarCount;
                barHeightDelta = 0;
                deltaX = 0;
                deltaY = -(barHeightDelta + barHeightBase + mSpacing);
                drawMaxWidth = maxWidth;
                drawMaxHeight = maxHeight;
                break;
            case Gravity.RIGHT:
                baseX = maxWidth;
                baseY = maxHeight;
                barWidthBase = -maxWidth / mBarCount;
                barWidthDelta = barWidthBase;
                barHeightBase = (maxHeight - ((mBarCount - 1) * mSpacing)) / mBarCount;
                barHeightDelta = 0;
                deltaX = 0;
                deltaY = -(barHeightDelta + barHeightBase + mSpacing);
                drawMaxWidth = maxWidth;
                drawMaxHeight = maxHeight;
                break;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int drawnBars = 0;
        int currentX, currentY, targetX, targetY;
        int drawStartX, drawStartY, drawEndX, drawEndY;
        while (true) {
            /* Draw bars one by one */
            if (drawnBars >= mBarCount) break;
            currentX = baseX + drawnBars * deltaX;
            currentY = baseY + drawnBars * deltaY;
            targetX = currentX + barWidthBase + drawnBars * barWidthDelta;
            targetY = currentY + barHeightBase + drawnBars * barHeightDelta;
            drawStartX = Math.min(currentX, targetX);
            drawEndX = Math.max(currentX, targetX);
            drawStartY = Math.min(currentY, targetY);
            drawEndY = Math.max(currentY, targetY);
            /* Respect drawMaxHeight/Width */
            switch (mLayoutGravity) {
                case Gravity.LEFT:
                case Gravity.RIGHT:
                    if (baseY - drawStartY > drawMaxHeight) {
                        drawStartY = baseY - drawMaxHeight;
                        drawnBars = mBarCount; /* Terminate drawing now */
                    }
                    break;
            }
            canvas.drawRect(drawStartX, drawStartY, drawEndX, drawEndY, mPaint);
            drawnBars++;
            if (VDBG) {
                Log.d(TAG, "onDraw: drawRect " + Math.min(currentX, targetX) + " " + Math.min(currentY, targetY) + " " +
                        Math.max(currentX, targetX) + " " + Math.max(currentY, targetY));
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width;
        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            width = 50; // WRAP_CONTENT default
        }

        setMeasuredDimension(width, heightSize);

        // TODO Is this proper? padding?
        maxHeight = heightSize;
        maxWidth = width;
        calculateSizes();
    }

    public float getDrawPercentage() {
        return (float) drawMaxHeight / maxHeight;
    }

    /**
     * Only draw such portion of bars
     * <p/>
     * TODO Add code for horizontal bars
     *
     * @param percentage Like battery percentage (0~1)
     */
    public void setDrawPercentage(float percentage) {
        switch (mLayoutGravity) {
            case Gravity.RIGHT:
            case Gravity.LEFT:
                drawMaxHeight = (int) (percentage * maxHeight);
                invalidate();
                if (VDBG)
                    Log.d(TAG, "setDrawPercentage: drawMaxHeight is " + drawMaxHeight);
                break;
        }
    }
}
