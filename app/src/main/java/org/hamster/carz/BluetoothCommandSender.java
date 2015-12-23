package org.hamster.carz;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Created by Hamster on 2015/12/21.
 * <p/>
 * Sends motor control commands to Bluetooth device.
 * <p/>
 * Current control gestures:
 * Hold & push forward/backward on left/right screen will make left/right motor accelerate/decelerate.
 * If swiped past the starting point, the motor will go backwards.
 * <p/>
 * E.g.:
 * If two fingers are at the same horizontal line, the car will go straight ahead.
 * If there is difference between vertical distance of two finger's movement, the car will turn around.
 * <p/>
 * Bluetooth Commands:
 * 'H' 'S' '\000' '\000'
 * "HS" is flag, 1st \0 is speed for left motor. 2nd \0 if speed for right motor.
 * <p/>
 * Bluetooth Commands(debug mode):
 * HS:S Stop
 * HS:Lxxx,Rxxx Left motor speed=xxx, Right motor speed=xxx (Positive number for forward, negative for backward)
 */
public class BluetoothCommandSender implements TouchControllerListener.OnTouchStateChangedListener {
    /**
     * If abs(deltaX1 - deltaX2) < maxHeight * THRESHOLD, they will be treated the same. The car will go straight.
     * If deltaX1(X2) < maxHeight * THRESHOLD,
     */
    private static final float IGNORE_DELTA_THRESHOLD = 0.1f;
    /**
     * Convert maxHeight to this value. 127 is maximum of one signed byte.
     */
    private static final int MAX_VALUE = 127;
    /**
     * Convert minHeight to this value. Low values will cause motors to stop working (and burning)
     */
    private static final int MIN_VALUE = 30;
    /**
     * Set this to true and we will send human readable outputs to Bluetooth port.
     */
    private static final boolean DEBUG_MODE = false;
    private BluetoothService mService;
    private Context mContext;
    private ServiceConnection btServConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((BluetoothService.BluetoothServiceBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    BluetoothCommandSender(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, BluetoothService.class);
        context.bindService(intent, btServConn, 0);
        mContext = context;
    }

    @Override
    public void onTouchStateChanged(TouchControllerListener.TouchState left,
                                    TouchControllerListener.TouchState right,
                                    int maxWidth, int maxHeight) {
        int touches = 0;
        if (left.isValid()) touches++;
        if (right.isValid()) touches++;

        switch (touches) {
            case 0:
            case 1:
                /* No/One finger is on screen. Stop. */
                if (DEBUG_MODE)
                    mService.sendBytes("HS:S\r\n".getBytes());
                else
                    mService.sendBytes(new byte[]{'H', 'S', 0, 0});
                break;
            case 2:
                /* Two finger is on screen. Android's y-axis is different from human's sense */
                int deltaXL = left.mStartPoint.y - left.mCurrentPoint.y;
                int deltaXR = right.mStartPoint.y - right.mCurrentPoint.y;
                /* Ignore small deltas (caused by finger shaking) */
                if (Math.abs(deltaXL) < maxHeight * IGNORE_DELTA_THRESHOLD) deltaXL = 0;
                if (Math.abs(deltaXR) < maxHeight * IGNORE_DELTA_THRESHOLD) deltaXR = 0;
                /* Ignore close deltas (make it easier to go straight) */
                if (Math.abs(deltaXL - deltaXR) < maxHeight * IGNORE_DELTA_THRESHOLD)
                    deltaXL = deltaXR = Math.min(deltaXL, deltaXR);
                /* Convert values */
                if (deltaXL >= 0)
                    deltaXL = (int) ((float) deltaXL / maxHeight * (MAX_VALUE - MIN_VALUE) + MIN_VALUE);
                else
                    deltaXL = (int) ((float) deltaXL / maxHeight * (MAX_VALUE - MIN_VALUE) - MIN_VALUE);
                if (deltaXR >= 0)
                    deltaXR = (int) ((float) deltaXR / maxHeight * (MAX_VALUE - MIN_VALUE) + MIN_VALUE);
                else
                    deltaXR = (int) ((float) deltaXR / maxHeight * (MAX_VALUE - MIN_VALUE) - MIN_VALUE);
                /* Send it */
                if (DEBUG_MODE)
                    mService.sendBytes(String.format("HS:L%03d,R%03d\r\n", deltaXL, deltaXR).getBytes());
                else
                    mService.sendBytes(new byte[]{'H', 'S', (byte) deltaXL, (byte) deltaXR});
                break;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        mContext.unbindService(btServConn);
        super.finalize();
    }
}
