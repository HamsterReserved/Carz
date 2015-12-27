package org.hamster.carz;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDevicePicker;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Re-use Android's native Bluetooth Device Picker
 * <p/>
 * From https://gist.github.com/timnew/7908603
 */
public class BluetoothDeviceManager implements BluetoothDevicePicker {
    private static final String TAG = "Carz_BTDeviceMan";
    private static final boolean VDBG = false;

    protected Context context;

    BluetoothDeviceManager(Context context) {
        this.context = context;
    }

    /**
     * Use system's native device picker to select a device.
     *
     * @param handler the handler to process the result
     */
    public void pickDevice(BluetoothDevicePickResultHandler handler) {
        if (VDBG)
            Log.v(TAG, "pickDevice: showing picker");
        context.registerReceiver(new BluetoothDeviceManagerReceiver(handler), new IntentFilter(ACTION_DEVICE_SELECTED));

        context.startActivity(new Intent(ACTION_LAUNCH)
                .putExtra(EXTRA_NEED_AUTH, false)
                .putExtra(EXTRA_FILTER_TYPE, FILTER_TYPE_ALL)
                .setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
    }

    public interface BluetoothDevicePickResultHandler {
        void onDevicePicked(BluetoothDevice device);
    }

    private static class BluetoothDeviceManagerReceiver extends BroadcastReceiver {

        private final BluetoothDevicePickResultHandler handler;

        public BluetoothDeviceManagerReceiver(BluetoothDevicePickResultHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            context.unregisterReceiver(this);

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            handler.onDevicePicked(device);
        }
    }
}