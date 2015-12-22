package org.hamster.carz;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
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
    private boolean stopDetectionFlag;
    private InputStream mInputStream;
    private String mErrorMessage;
    private CarConnectionState mLastState = CarConnectionState.STATE_DISCONNECTED;
    private CarConnectionState mState = CarConnectionState.STATE_DISCONNECTED;
    private ConnectionStateChangeListener mListener;
    private UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Thread detectionThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!stopDetectionFlag) {
                try {
                    mInputStream.read();
                } catch (IOException e) {
                    setState(CarConnectionState.STATE_DISCONNECTED);
                    stopDetectionFlag = true;
                }
            }
        }
    });

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
            mInputStream = mSocket.getInputStream();
        } catch (IOException e) { // One IOException catches them all. Can be distinguished by e.
            setState(CarConnectionState.STATE_FAILED);
            mErrorMessage = e.getLocalizedMessage();
            Log.e(TAG, "connect: operation failed: ", e);
            return;
        }

        stopDetectionFlag = false;
        detectionThread.start();
        setState(CarConnectionState.STATE_CONNECTED);
    }

    public BluetoothDevice getTargetDevice() {
        return mBluetoothDevice;
    }

    public void disconnect() {
        setState(CarConnectionState.STATE_DISCONNECTING);
        try {
            stopDetectionFlag = true; // Break the loop
            mSocket.close();
            detectionThread.join(); // Wait for finish
            mState = mLastState; // Recover the state destroyed by detectionThread
        } catch (IOException e) {
            setState(CarConnectionState.STATE_FAILED);
            Log.e(TAG, "disconnect: socket close failed", e);
            return;
        } catch (InterruptedException e) {
            // do nothing
        }
        setState(CarConnectionState.STATE_DISCONNECTED);
    }

    public CarConnectionState getState() {
        return mState;
    }

    private void setState(CarConnectionState newState) {
        mLastState = mState;
        mState = newState;
        notifyStateChanged();
    }

    public void notifyStateChanged() {
        if (mListener != null) {
            mListener.onCarConnectionStateChanged(this);
        }
    }

    public CarConnectionState getLastState() {
        return mLastState;
    }

    public OutputStream getOutputStream() {
        return mOutputStream;
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
            if (device.getAddress().equals(mBluetoothDevice.getAddress())) {
                Log.d(TAG, "update: Already connected to the same device. Just notify.");
                mListener = listener;
                notifyStateChanged();
                return;
            } else {
                Log.d(TAG, "update: Already connected to different device. Disconnect first.");
                disconnect();
            }
        } else if (mState == CarConnectionState.STATE_CONNECTING) {
            Log.i(TAG, "update: The previous connection is CONNECTING.");
            // TODO: 2015/12/19 disconnect();?
        }
        mBluetoothDevice = device;
        mListener = listener;
        setState(CarConnectionState.STATE_DISCONNECTED);
    }

    public void sendBytes(byte[] data) {
        try {
            mOutputStream.write(data);
        } catch (IOException e) {
            Log.e(TAG, "sendBytes: IO", e);
            mErrorMessage = e.getLocalizedMessage();
            setState(CarConnectionState.STATE_FAILED);
        }
    }

    public enum CarConnectionState {
        STATE_DISCONNECTED,
        STATE_CONNECTING,
        STATE_FAILED,
        STATE_DISCONNECTING,
        STATE_CONNECTED
    }

    public interface ConnectionStateChangeListener {
        void onCarConnectionStateChanged(BluetoothCarConnection connection);
    }
}
