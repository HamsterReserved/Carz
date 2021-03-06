package org.hamster.carz;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Hamster on 2015/12/20.
 * <p/>
 * Bluetooth Car Controller. Use left and right parts of screen as left/right motor speed controller.
 */
public class ControllerFragment extends Fragment {
    private static final String TAG = "Carz_CtrlFrag";
    private static final boolean VDBG = false;
    private View mRootView;
    private EnergyBar mLeftBar;
    private EnergyBar mRightBar;
    private TextView mDistanceText;
    private ArrayList<Integer> mBytesReceived;
    private BluetoothService mService;
    private View.OnClickListener fabOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mService.disconnect();
        }
    };
    private ServiceConnection btServConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((BluetoothService.BluetoothServiceBinder) service).getService();
            mService.setOnDataReceivedListener(new DataReceiver());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final MainActivity activity = (MainActivity) getActivity();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                activity.hideBars();
            }
        }, 500);
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Intent intent = new Intent();
        intent.setClass(getActivity(), BluetoothService.class);
        getActivity().bindService(intent, btServConn, Context.BIND_AUTO_CREATE);

        mBytesReceived = new ArrayList<>(4);

        mRootView = inflater.inflate(R.layout.frag_touch_controller, container, false);
        mRootView.findViewById(R.id.fab_disconnect).setOnClickListener(fabOnClickListener);
        mLeftBar = (EnergyBar) mRootView.findViewById(R.id.energy_bar_left);
        mRightBar = (EnergyBar) mRootView.findViewById(R.id.energy_bar_right);
        mDistanceText = (TextView) mRootView.findViewById(R.id.tv_bt);

        mLeftBar.setDrawPercentage(0);
        mRightBar.setDrawPercentage(0);

        TouchControllerListener controllerListener = new TouchControllerListener();
        BarHeightAdjuster adjuster = new BarHeightAdjuster();
        BluetoothCommandSender sender = new BluetoothCommandSender(activity);

        controllerListener.addTouchStateChangedListener(sender);
        controllerListener.addTouchStateChangedListener(adjuster);

        View touchController = mRootView.findViewById(R.id.touch_area);
        touchController.setOnTouchListener(controllerListener);

        return mRootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().unbindService(btServConn);
        final MainActivity activity = (MainActivity) getActivity();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                activity.showBars();
            }
        }, 500);
    }

    private class DataReceiver implements BluetoothCarConnection.DataReceivedListener {
        @Override
        public void onDataReceived(int data) {
            mBytesReceived.add(data);
            if (mBytesReceived.size() == 4) {
                // Received 4 bytes
                if (mBytesReceived.get(0) == 'D') {
                    // Data integrity
                    final String distanceString;
                    if (mBytesReceived.get(1) < 10) {
                        distanceString = "Current space ahead: " +
                                String.valueOf(mBytesReceived.get(1)
                                        + mBytesReceived.get(2) * 0.1f
                                        + mBytesReceived.get(3) * 0.01f) +
                                "m";
                    } else {
                        distanceString = "To the Infinity and Beyond!";
                    }
                    mDistanceText.post(new Runnable() {
                        @Override
                        public void run() {
                            mDistanceText.setText(distanceString);
                        }
                    });
                    mBytesReceived.clear();
                }
            }
        }
    }

    /**
     * Adjusts EnergyBars' height according to the distance fingers moved.
     */
    private class BarHeightAdjuster implements TouchControllerListener.OnTouchStateChangedListener {
        private ObjectAnimator animatorLeft;
        private ObjectAnimator animatorRight;

        public BarHeightAdjuster() {
            animatorLeft = ObjectAnimator.ofFloat(mLeftBar, "DrawPercentage", 0); // 0 is stub
            animatorRight = ObjectAnimator.ofFloat(mRightBar, "DrawPercentage", 0);
        }

        @Override
        public void onTouchStateChanged(TouchControllerListener.TouchState left,
                                        TouchControllerListener.TouchState right,
                                        int maxWidth, int maxHeight) {
            if (VDBG)
                Log.d(TAG, "barHeight: onTouchStateChanged: left.start = " + left.mStartPoint
                        + "left.end = " + left.mCurrentPoint
                        + "right.start = " + right.mStartPoint
                        + "right.end = " + right.mCurrentPoint);
            if (!(left.isValid() && right.isValid())) {
                float percentLeft = mLeftBar.getDrawPercentage();
                float percentRight = mRightBar.getDrawPercentage();
                /* No/One finger is on screen. Animate the bars back */
                if (percentLeft != 0) {
                    animatorLeft.setFloatValues(percentLeft, 0);
                    animatorLeft.setDuration(1000);
                    animatorLeft.setInterpolator(new DecelerateInterpolator(5f));
                    animatorLeft.start();
                }
                if (percentRight != 0) {
                    animatorRight.setFloatValues(percentRight, 0);
                    animatorRight.setDuration(1000);
                    animatorRight.setInterpolator(new DecelerateInterpolator(5f));
                    animatorRight.start();
                }
            } else {
                animatorLeft.end();
                animatorRight.end();
                mLeftBar.setDrawPercentage((left.mStartPoint.y - left.mCurrentPoint.y) / (float) maxHeight);
                mRightBar.setDrawPercentage((right.mStartPoint.y - right.mCurrentPoint.y) / (float) maxHeight);
            }
        }
    }
}
