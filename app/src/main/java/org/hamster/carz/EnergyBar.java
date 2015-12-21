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
     */
    private int baseX, baseY,
            deltaX, deltaY,
            barWidthBase, barHeightBase,
            barWidthDelta, barHeightDelta,
            maxHeight, maxWidth;

    public EnergyBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        int[] set = {
                android.R.attr.layout_gravity,
                R.attr.barColor,
                R.attr.spacing,
                R.attr.barCount,
        };

        TypedArray a = context.obtainStyledAttributes(attrs, set);
        mLayoutGravity = a.getInt(0, Gravity.LEFT);
        mBackgroundColor = a.getColor(1, getResources().getColor(android.R.color.holo_red_dark));
        mSpacing = a.getDimensionPixelSize(2, 24);
        mBarCount = a.getInt(3, 7);
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
                break;
            case Gravity.RIGHT:
                baseX = maxWidth;
                baseY = maxHeight;
                barWidthBase = maxWidth / mBarCount;
                barWidthDelta = -barWidthBase;
                barHeightBase = (maxHeight - ((mBarCount - 1) * mSpacing)) / mBarCount;
                barHeightDelta = 0;
                deltaX = 0;
                deltaY = -(barHeightDelta + barHeightBase + mSpacing);
                break;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int drawnBars = 0;
        int currentX, currentY, targetX, targetY;
        while (true) {
            /* Draw bars one by one */
            currentX = baseX + drawnBars * deltaX;
            currentY = baseY + drawnBars * deltaY;
            targetX = currentX + barWidthBase + drawnBars * barWidthDelta;
            targetY = currentY + barHeightBase + drawnBars * barHeightDelta;
            if (drawnBars >= mBarCount) break;
            canvas.drawRect(Math.min(currentX, targetX), Math.min(currentY, targetY),
                    Math.max(currentX, targetX), Math.max(currentY, targetY), mPaint);
            drawnBars++;
            Log.i(TAG, "onDraw: drawRect " + Math.min(currentX, targetX) + " " + Math.min(currentY, targetY) + " " +
                    Math.max(currentX, targetX) + " " + Math.max(currentY, targetY));
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

        // TODO Is this proper?
        maxHeight = heightSize;
        maxWidth = width;
        calculateSizes();
    }
}
