package org.hamster.carz;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Carz-MainActivity";
    private static final boolean VDBG = true;
    private static final int REQCODE_BLUETOOTH_ON = 0;

    private View mRootView;
    private Handler mHandler;
    private boolean isServiceRunning = false;
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
                    BluetoothDevice device = connection.getTargetDevice();

                    switch (state) {
                        case STATE_FAILED:
                            Snackbar.make(mRootView, "Connection with " + btDevToStr(device)
                                            + " failed! Caused by: " + connection.getErrorMessage(),
                                    Snackbar.LENGTH_INDEFINITE).show();
                            break;
                        case STATE_CONNECTING:
                            /* Will be replaced by later snacks */
                            Snackbar.make(mRootView, "Connecting to " + btDevToStr(device) + "\u2026",
                                    Snackbar.LENGTH_INDEFINITE).show();
                            break;
                        case STATE_CONNECTED:
                            Snackbar.make(mRootView, "Connected with " + btDevToStr(device) + ".",
                                    Snackbar.LENGTH_LONG).show();
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
            isServiceRunning = true;
            mBinder = (BluetoothService.BluetoothServiceBinder) service;
            if (mDeviceToConnect != null) {
                mBinder.getService().connect(mDeviceToConnect, bluetoothStateChangeListener);
            }
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
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, BluetoothService.class);
            bindService(intent, btServiceConn, BIND_AUTO_CREATE);
            mDeviceToConnect = device;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mRootView = findViewById(R.id.coordinatorLayout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHandler = new Handler();
        mBluetoothManager = new BluetoothDeviceManager(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* If BT is on, we launch device picker here. Or we launch in onActivityResult */
                if (requestBluetoothOn())
                    mBluetoothManager.pickDevice(bluetoothDevicePickResultHandler);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent context in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceRunning) unbindService(btServiceConn);
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
     * Mainly for activating Bluetooth
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
}
