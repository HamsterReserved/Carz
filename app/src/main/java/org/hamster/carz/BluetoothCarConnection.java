package org.hamster.carz;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Hamster on 2015/12/19.
 * <p/>
 * Represents a connection to a car. Handles data transfer.
 */
public class BluetoothCarConnection {
    private BluetoothDevice mBluetoothDevice;
    private CarConnectionState mState;
    private ConnectionStateChangeListener mListener;

    BluetoothCarConnection(@NonNull BluetoothDevice device,
                           @Nullable ConnectionStateChangeListener listener) {
        mBluetoothDevice = device;
        mListener = listener;
        setState(CarConnectionState.STATE_NOT_CONNECTED);
    }

    public void connect() {
        setState(CarConnectionState.STATE_CONNECTING);
        // TODO establish real SPP connection to the device
    }

    public BluetoothDevice getTargetDevice() {
        return mBluetoothDevice;
    }

    public void disconnect() {
    }

    public CarConnectionState getState() {
        return mState;
    }

    private void setState(CarConnectionState newState) {
        mState = newState;
        if (mListener != null) {
            mListener.onCarConnectionStateChanged(mState, mBluetoothDevice);
        }
    }

    public enum CarConnectionState {
        STATE_NOT_CONNECTED,
        STATE_CONNECTING,
        STATE_FAILED,
        STATE_CONNECTED
    }

    public interface ConnectionStateChangeListener {
        void onCarConnectionStateChanged(CarConnectionState state, BluetoothDevice device);
    }


}
