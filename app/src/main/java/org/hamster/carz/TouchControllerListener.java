package org.hamster.carz;

import android.graphics.Point;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by Hamster on 2015/12/21.
 * <p/>
 * Listen for touch events on controller.
 */
public class TouchControllerListener implements View.OnTouchListener {
    private static final String TAG = "Carz_TouchCtrl";
    private static final boolean VDBG = false;
    private int mWidth; /* This listener works with fullscreen views */
    private int mHeight;
    private View.OnTouchListener mOnTouchListener;
    private ArrayList<OnTouchStateChangedListener> mTouchStateChangedListeners;
    private TouchState mLeftTouch;
    private TouchState mRightTouch;

    TouchControllerListener() {
        mLeftTouch = new TouchState();
        mRightTouch = new TouchState();
        mTouchStateChangedListeners = new ArrayList<>(2);
    }

    public void addTouchStateChangedListener(OnTouchStateChangedListener mTouchStateChangedListener) {
        this.mTouchStateChangedListeners.add(mTouchStateChangedListener);
    }

    public void setOnTouchListener(View.OnTouchListener mOnTouchListener) {
        this.mOnTouchListener = mOnTouchListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getActionIndex() >= 2) {
            /* We don't need more than 2 fingers */
            return true;
        }

        int action = event.getActionMasked();
        int pointerId = event.getPointerId(event.getActionIndex());
        int x = (int) event.getX(event.getActionIndex());
        int y = (int) event.getY(event.getActionIndex());
        /* Note that we use pointerIndex to obtain position, pointerId to identify finger */

        mWidth = v.getWidth();
        mHeight = v.getHeight();

        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (VDBG)
                    Log.d(TAG, "onTouch: PUPPID " + pointerId);
                removeFinger(pointerId);
                break;
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (VDBG)
                    Log.d(TAG, "onTouch: PDOWNPID " + pointerId);
                addFinger(x, y, pointerId);
                break;
            case MotionEvent.ACTION_MOVE:
                /* i is pointerIndex */
                for (int i = 0; i < event.getPointerCount(); i++) {
                    x = (int) event.getX(i);
                    y = (int) event.getY(i);
                    updateFinger(x, y, event.getPointerId(i));
                }
                break;
        }

        onTouchStateChanged();
        return mOnTouchListener == null || mOnTouchListener.onTouch(v, event);
    }

    private void onTouchStateChanged() {
        for (OnTouchStateChangedListener listener : mTouchStateChangedListeners) {
            listener.onTouchStateChanged(mLeftTouch, mRightTouch, mWidth, mHeight);
        }
    }

    private void addFinger(int x, int y, int pointerId) {
        if (x < mWidth / 2) {
            /* Left side of screen */
            if (mLeftTouch.isValid() && pointerId != mLeftTouch.mPointerId) {
                /* A new finger is in left area while there's already one */
                Log.d(TAG, "addFinger: Refusing pointerId " + pointerId + " to enter left area");
                return;
            }
            mLeftTouch.mStartPoint.set(x, y);
            mLeftTouch.mPointerId = pointerId;
        } else {
            if (mRightTouch.isValid() && pointerId != mRightTouch.mPointerId) {
                Log.d(TAG, "addFinger: Refusing pointerId " + pointerId + " to enter right area");
                return;
            }
            mRightTouch.mStartPoint.set(x, y);
            mRightTouch.mPointerId = pointerId;
        }
    }

    private void updateFinger(int x, int y, int pointerId) {
        TouchState state = mLeftTouch.mPointerId == pointerId ? mLeftTouch :
                mRightTouch.mPointerId == pointerId ? mRightTouch :
                        null;
        if (state != null) {
            state.mCurrentPoint.set(x, y);
        } else {
            /*
            This is normal when ...
            1. A pressed in one area
            2. B pressed in the same area
            3. addFinger rejected B, so there is no data about B in TouchState(s)
            4. B moves, so this method is called only to find there is no such pointerId
             */
            Log.i(TAG, "updateFinger: pointerId " + pointerId + " not found");
        }
    }

    private void removeFinger(int pointerId) {
        TouchState state = mLeftTouch.mPointerId == pointerId ? mLeftTouch :
                mRightTouch.mPointerId == pointerId ? mRightTouch :
                        null;
        if (state != null) {
            state.invalidate();
        } else {
            Log.i(TAG, "removeFinger: pointerId " + pointerId + " is not found");
        }
    }

    /**
     * Find appropriate TouchState object to modify
     * <p/>
     * You can find either x&y and/or id. If x&y are not -1,
     * we will find the TouchState with given x&y.
     * If id is not -1, we will find the TouchState with given id.
     * If all of three are given, we will find the EXACT one.
     *
     * @param x  x to find
     * @param y  y to find
     * @param id pointer id to find
     * @return null if not found
     */
    private TouchState findTouchState(int x, int y, int id) {
        ArrayList<TouchState> availableTouches = new ArrayList<>();
        availableTouches.add(mLeftTouch);
        availableTouches.add(mRightTouch);

        if (x != -1 && y != -1) {
            for (TouchState state : availableTouches) {
                if (!state.equals(x, y)) availableTouches.remove(state);
            }
        }
        if (id != -1) {
            for (TouchState state : availableTouches) {
                if (state.mPointerId != id) availableTouches.remove(state);
            }
        }

        if (availableTouches.size() == 0)
            return null;
        else
            return availableTouches.get(0);
    }

    /**
     * Provides tracking service of gestures in left/right half od screen.
     */
    public interface OnTouchStateChangedListener {
        void onTouchStateChanged(TouchState left, TouchState right, int maxWidth, int maxHeight);
    }

    public class TouchState {
        public Point mStartPoint;
        public Point mCurrentPoint;
        public int mPointerId; /* Used for multi-touch */

        TouchState() {
            mStartPoint = new Point();
            mCurrentPoint = new Point();
            invalidate();
        }

        public void invalidate() {
            mStartPoint.set(-1, -1);
            mCurrentPoint.set(-1, -1);
            mPointerId = -1;
        }

        public boolean isValid() {
            return !mStartPoint.equals(-1, -1);
        }

        public boolean equals(int x, int y) {
            return mCurrentPoint.equals(x, y) || mStartPoint.equals(x, y);
        }
    }
}
