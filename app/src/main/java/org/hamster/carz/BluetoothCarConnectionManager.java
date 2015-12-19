package org.hamster.carz;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

/**
 * Created by Hamster on 2015/12/19.
 * <p/>
 * Manages all {@link BluetoothCarConnection} instances. Singleton.
 */
public class BluetoothCarConnectionManager {
    private static BluetoothCarConnectionManager mInstance;
    private ArrayList<BluetoothCarConnection> mConnections;

    private BluetoothCarConnectionManager() {
    }

    public static BluetoothCarConnectionManager getInstance() {
        if (mInstance == null) {
            mInstance = new BluetoothCarConnectionManager();
            mInstance.mConnections = new ArrayList<>(1); // default size is 1 connection
        }
        return mInstance;
    }

    public void initializeConnection(@NonNull BluetoothDevice device,
                                     @Nullable BluetoothCarConnection.ConnectionStateChangeListener listener) {
        BluetoothCarConnection connection = new BluetoothCarConnection(device, listener);
        for (BluetoothCarConnection existingConnection : mConnections) {
            if (existingConnection.getTargetDevice().getAddress().equals(device.getAddress())) {
                // Same connection, redo it
                existingConnection.disconnect();
                existingConnection.connect();
                return;
            }
        }
        // No existing same connection
        mConnections.add(connection);
        connection.connect();
    }
}
