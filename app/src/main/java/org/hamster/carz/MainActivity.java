package org.hamster.carz;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.ColorRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Carz-MainActivity";
    private static final boolean VDBG = true;
    private static final int REQCODE_BLUETOOTH_ON = 0;

    private CoordinatorLayout mRootView;
    private Toolbar mToolbar;
    private FloatingActionButton mFAB;
    private Menu mMenu;

    private Handler mHandler;

    private BluetoothDevice mDeviceToConnect;
    private BluetoothDeviceManager mBluetoothManager;
    private BluetoothCarConnection.ConnectionStateChangeListener
            bluetoothStateChangeListener = new BluetoothCarConnection.ConnectionStateChangeListener() {
        @Override
        public void onCarConnectionStateChanged(final BluetoothCarConnection connection) {

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BluetoothCarConnection.CarConnectionState state = connection.getState();
                    final BluetoothDevice device = connection.getTargetDevice();

                    /* We do only snackbars here. Other layout changes will be in separate methods */
                    switch (state) {
                        case STATE_FAILED:
                            final String errMsg = "Connection with " + btDevToStr(device)
                                    + " failed! Caused by: " + connection.getErrorMessage();
                            final Snackbar bar = Snackbar.make(mRootView, errMsg,
                                    Snackbar.LENGTH_INDEFINITE);
                            bar.setAction("View All", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setMessage(errMsg);
                                    builder.setPositiveButton("OK", null);
                                    builder.show();
                                }
                            });
                            bar.show();
                            // don't call onBluetoothDisconnected here
                            // since this state will only trigger when connection failed
                            break;
                        case STATE_CONNECTING:
                            /* Will be replaced by later snacks */
                            Snackbar.make(mRootView, "Connecting with " + btDevToStr(device) + " \u2026",
                                    Snackbar.LENGTH_INDEFINITE).show();
                            break;
                        case STATE_CONNECTED:
                            Snackbar.make(mRootView, "Connected to " + btDevToStr(device) + ".",
                                    Snackbar.LENGTH_LONG).show();
                            onBluetoothConnected(device);
                            break;
                        case STATE_DISCONNECTED:
                            if (connection.getLastState() ==
                                    BluetoothCarConnection.CarConnectionState.STATE_CONNECTED ||
                                    connection.getLastState() ==
                                            BluetoothCarConnection.CarConnectionState.STATE_DISCONNECTING) {
                                // CONNECTED -> NOT_CONNECTED : connection terminated by the other peer
                                // This is thrown by detectionThread
                                // DISCONNECTING -> NOT_CONNECTED : connection terminated by us
                                // Set by disconnect()
                                Snackbar.make(mRootView, "Disconnected with " + btDevToStr(device) + ".",
                                        Snackbar.LENGTH_LONG).show();
                                onBluetoothDisconnected(device);
                            }
                            break;
                    }
                }
            }, 200); // delay 200ms to let the slide-in animation display correctly
        }
    };
    private BluetoothService.BluetoothServiceBinder mBinder;
    private ServiceConnection btServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (VDBG) Log.d(TAG, "onServiceConnected: +1");
            mBinder = (BluetoothService.BluetoothServiceBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (VDBG) Log.d(TAG, "onServiceDisconnected: -1");
        }
    };
    private BluetoothDeviceManager.BluetoothDevicePickResultHandler
            bluetoothDevicePickResultHandler = new BluetoothDeviceManager.BluetoothDevicePickResultHandler() {
        @Override
        public void onDevicePicked(BluetoothDevice device) {
            mDeviceToConnect = device;
            mBinder.getService().connect(device, bluetoothStateChangeListener);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mRootView = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mHandler = new Handler();
        mBluetoothManager = new BluetoothDeviceManager(this);

        mFAB = (FloatingActionButton) findViewById(R.id.fab);
        mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* If BT is on, we launch device picker here. Or we launch in onActivityResult */
                if (requestBluetoothOn())
                    mBluetoothManager.pickDevice(bluetoothDevicePickResultHandler);
            }
        });


        Intent intent = new Intent();
        intent.setClass(MainActivity.this, BluetoothService.class);
        bindService(intent, btServiceConn, BIND_AUTO_CREATE);

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_container, new NoDeviceFragment())
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent context in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_disconnect) {
            mBinder.getService().disconnect();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(btServiceConn);
    }

    private String btDevToStr(BluetoothDevice device) {
        return device.getName() + " @ " + device.getAddress();
    }

    /**
     * Check if Bluetooth is on. If it's not, request user to turn it on.
     * If it's already on, do nothing.
     *
     * @return true if BT is already on. False if we need to start first or error.
     */
    private boolean requestBluetoothOn() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Log.e(TAG, "requestBluetoothOn: No Bluetooth adapter available");
            Snackbar.make(mRootView, "No Bluetooth adapter available!", Snackbar.LENGTH_LONG)
                    .show();
            return false;
        }

        if (!adapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQCODE_BLUETOOTH_ON);
            return false;
        }
        return true;
    }

    /**
     * Mainly for activating Bluetooth (after Bluetooth picker)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQCODE_BLUETOOTH_ON) {
            if (resultCode == RESULT_OK) {
                BluetoothDeviceManager manager = new BluetoothDeviceManager(MainActivity.this);
                manager.pickDevice(bluetoothDevicePickResultHandler);
            } else {
                Snackbar.make(mRootView, "You need to turn on Bluetooth to control cars.", Snackbar.LENGTH_LONG)
                        .show();
            }
        }
    }

    /**
     * Perform layout/text changes after BT connected.
     * This is not a callback.
     */
    private void onBluetoothConnected(BluetoothDevice device) {
        animateToolbarColor(R.color.colorPrimary, R.color.colorAccent, 500);
        // animateStatusBarColor(R.color.colorPrimaryDark, R.color.colorAccentDark, 500);
        // Because controller fragment doesn't have StatusBar
        animateStatusBarColor(R.color.colorPrimaryDark, android.R.color.black, 500);

        ObjectAnimator.ofFloat(mFAB, "alpha", 1f, 0f).setDuration(500).start();
        mFAB.setVisibility(View.GONE);

        mMenu.getItem(0).setVisible(true);
        mToolbar.setTitle(getString(R.string.app_name) + " - " + device.getName());
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_container, new ControllerFragment())
                .commit();
    }

    private void onBluetoothDisconnected(BluetoothDevice device) {
        animateToolbarColor(R.color.colorAccent, R.color.colorPrimary, 500);
        // animateStatusBarColor(R.color.colorAccentDark, R.color.colorPrimaryDark, 500);
        // Same as above
        animateStatusBarColor(android.R.color.black, R.color.colorPrimaryDark, 500);

        mFAB.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(mFAB, "alpha", 0f, 1f).setDuration(500).start();

        mMenu.getItem(0).setVisible(false);
        mToolbar.setTitle(getString(R.string.app_name));
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_container, new NoDeviceFragment())
                .commit();
    }

    private void animateToolbarColor(@ColorRes int oldColor, @ColorRes int newColor, long duration) {
        int oldValue = getResources().getColor(oldColor);
        int newValue = getResources().getColor(newColor);

        ValueAnimator valueAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), oldValue, newValue);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mToolbar.setBackgroundColor((int) animation.getAnimatedValue());
            }
        });
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    private void animateStatusBarColor(@ColorRes int oldColor, @ColorRes int newColor, long duration) {
        int oldValue = getResources().getColor(oldColor);
        int newValue = getResources().getColor(newColor);

        ValueAnimator valueAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), oldValue, newValue);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRootView.setStatusBarBackgroundColor((int) animation.getAnimatedValue());
            }
        });
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mRootView.requestLayout();
    }

    /* Change isControllerRunning before calling these! */
    public void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        mRootView.setFitsSystemWindows(false);
        Animation animSlideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        animSlideUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                getSupportActionBar().hide();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mToolbar.startAnimation(animSlideUp);
    }

    public void showBars() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        mRootView.setFitsSystemWindows(true);
        Animation animSlideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        animSlideDown.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                getSupportActionBar().show();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mToolbar.startAnimation(animSlideDown);

    }
}
