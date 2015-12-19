package org.hamster.carz;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
    private BluetoothDeviceManager mBluetoothManager;
    private BluetoothCarConnection.ConnectionStateChangeListener
            bluetoothStateChangeListener = new BluetoothCarConnection.ConnectionStateChangeListener() {
        @Override
        public void onCarConnectionStateChanged(final BluetoothCarConnection.CarConnectionState state,
                                                final BluetoothDevice device) {

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    switch (state) {
                        case STATE_FAILED:
                            Snackbar.make(mRootView, "Connection with " + btDevToStr(device) + " failed!",
                                    Snackbar.LENGTH_LONG).show();
                            break;
                        case STATE_CONNECTING:
                            /* Will be replaced by later snacks */
                            Log.d(TAG, "run: STATE_CONNECTING");
                            Snackbar.make(mRootView, "Connecting to " + btDevToStr(device) + "\u2026",
                                    Snackbar.LENGTH_INDEFINITE).show();
                            break;
                        case STATE_CONNECTED:
                            Snackbar.make(mRootView, "Connected with " + btDevToStr(device) + ".",
                                    Snackbar.LENGTH_LONG).show();
                            break;
                    }
                }
            }, 200);
        }
    };
    private BluetoothDeviceManager.BluetoothDevicePickResultHandler
            bluetoothDevicePickResultHandler = new BluetoothDeviceManager.BluetoothDevicePickResultHandler() {
        @Override
        public void onDevicePicked(BluetoothDevice device) {
            BluetoothCarConnectionManager.getInstance().initializeConnection(device, bluetoothStateChangeListener);
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
                /* If BT is on, we show device picker here. Or we show in onActivityResult */
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
     *
     * @param requestCode
     * @param resultCode
     * @param data
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
