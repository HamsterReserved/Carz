package org.hamster.carz;

import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

/**
 * Created by Hamster on 2015/12/20.
 * <p/>
 * Bluetooth Car Controller. Use left and right parts of screen as left/right motor speed controller.
 */
public class ControllerFragment extends Fragment {
    private View mRootView;
    private EnergyBar mLeftBar;
    private EnergyBar mRightBar;
    private BluetoothService.BluetoothServiceBinder mBinder;
    private View.OnClickListener fabOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mBinder.getService().disconnect();
        }
    };
    private ServiceConnection btServConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (BluetoothService.BluetoothServiceBinder) service;
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
        getActivity().bindService(intent, btServConn, 0);

        mRootView = inflater.inflate(R.layout.frag_touch_controller, container, false);
        mRootView.findViewById(R.id.fab_disconnect).setOnClickListener(fabOnClickListener);
        mLeftBar = (EnergyBar) mRootView.findViewById(R.id.energy_bar_left);
        mRightBar = (EnergyBar) mRootView.findViewById(R.id.energy_bar_right);
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
        final MainActivity activity = (MainActivity) getActivity();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                activity.showBars();
            }
        }, 500);
    }

    private class BarHeightAdjuster implements TouchControllerListener.OnTouchStateChangedListener {
        private ObjectAnimator animatorLeft;
        private ObjectAnimator animatorRight;

        /* For pointer up events*/
        @Override
        public void onTouchStateChanged(TouchControllerListener.TouchState left,
                                        TouchControllerListener.TouchState right,
                                        int maxWidth, int maxHeight) {
            if (!left.isValid() || !right.isValid()) {
                /* No/One finger is on screen. Animate the bars back */
                if (mLeftBar.getDrawPercentage() != 0) {
                    animatorLeft = ObjectAnimator.ofFloat(mLeftBar, "DrawPercentage", mLeftBar.getDrawPercentage(), 0)
                            .setDuration(1000);
                    animatorLeft.setInterpolator(new DecelerateInterpolator(5f));
                    animatorLeft.start();
                }
                if (mRightBar.getDrawPercentage() != 0) {
                    animatorRight = ObjectAnimator.ofFloat(mRightBar, "DrawPercentage", mRightBar.getDrawPercentage(), 0)
                            .setDuration(1000);
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
