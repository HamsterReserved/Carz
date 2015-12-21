package org.hamster.carz;

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

/**
 * Created by Hamster on 2015/12/20.
 * <p/>
 * Bluetooth Car Controller. Consist of L/R/U/D buttons.
 */
public class ControllerFragment extends Fragment {
    private View mRootView;
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

        View touchController = mRootView.findViewById(R.id.touch_area);
        touchController.setOnTouchListener(new TouchControllerListener(null));

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
}
