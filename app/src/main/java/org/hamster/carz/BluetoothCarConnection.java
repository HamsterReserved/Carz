package org.hamster.carz;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Hamster on 2015/12/19.
 * <p/>
 * Represents a connection to a car. Handles data transfer.
 */
public class BluetoothCarConnection {
    private static final String TAG = "Carz_BTCarConn";
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mSocket;
    private OutputStream mOutputStream;
    private String mErrorMessage;
    private CarConnectionState mLastState = CarConnectionState.STATE_NOT_CONNECTED;
    private CarConnectionState mState = CarConnectionState.STATE_NOT_CONNECTED;
    private ConnectionStateChangeListener mListener;
    private UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothCarConnection(@NonNull BluetoothDevice device,
                           @Nullable ConnectionStateChangeListener listener) {
        update(device, listener);
    }

    public void connect() {
        setState(CarConnectionState.STATE_CONNECTING);
        try {
            mSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
            mSocket.connect();
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException e) { // One IOException catches them all. Can be distinguished by e.
            setState(CarConnectionState.STATE_FAILED);
            mErrorMessage = e.getLocalizedMessage();
            Log.e(TAG, "connect: operation failed: ", e);
            return;
        }
        setState(CarConnectionState.STATE_CONNECTED);
    }

    public BluetoothDevice getTargetDevice() {
        return mBluetoothDevice;
    }

    public void disconnect() {
        try {
            mSocket.close();
        } catch (IOException e) {
            setState(CarConnectionState.STATE_FAILED);
            Log.e(TAG, "disconnect: socket close failed", e);
            return;
        }
        mState = CarConnectionState.STATE_NOT_CONNECTED;
    }

    public CarConnectionState getState() {
        return mState;
    }

    private void setState(CarConnectionState newState) {
        mLastState = mState;
        mState = newState;
        if (mListener != null) {
            mListener.onCarConnectionStateChanged(this);
        }
    }

    public CarConnectionState getLastState() {
        return mLastState;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    /**
     * For re-use the connection object. This will cause a disconnection before updating.
     *
     * @param device   Target device
     * @param listener Can be null. Callback when connection state changes.
     */
    public void update(@NonNull BluetoothDevice device,
                       @Nullable ConnectionStateChangeListener listener) {
        if (mState == CarConnectionState.STATE_CONNECTED) {
            disconnect();
        } else if (mState == CarConnectionState.STATE_CONNECTING) {
            Log.i(TAG, "update: The previous connection is CONNECTING.");
            // TODO: 2015/12/19 disconnect();?
        }
        mBluetoothDevice = device;
        mListener = listener;
        setState(CarConnectionState.STATE_NOT_CONNECTED);
    }

    public enum CarConnectionState {
        STATE_NOT_CONNECTED,
        STATE_CONNECTING,
        STATE_FAILED,
        STATE_CONNECTED
    }

    public interface ConnectionStateChangeListener {
        void onCarConnectionStateChanged(BluetoothCarConnection connection);
    }
}
