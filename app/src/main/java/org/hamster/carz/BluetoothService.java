package org.hamster.carz;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Hamster on 2015/12/19.
 * <p/>
 * Service to perform Bluetooth connection and data transfer.
 * Specifically, only one connection is allowed since one adapter can connect to one SPP
 * instance only.
 * <p/>
 * NOTE: Service runs in the same thread as MainActivity. Don't do time consuming things here.
 */
public class BluetoothService extends Service {
    private static final String TAG = "Carz_BTSrv";
    private static final boolean VDBG = true;

    private BluetoothCarConnection mConnection;

    public void connect(BluetoothDevice device,
                        BluetoothCarConnection.ConnectionStateChangeListener stateListener,
                        BluetoothCarConnection.DataReceivedListener dataListener) {
        if (VDBG) Log.d(TAG, "connect: " + device.getAddress());
        if (mConnection != null) {
            mConnection.update(device, stateListener, dataListener);
        } else {
            mConnection = new BluetoothCarConnection(device, stateListener, dataListener);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                mConnection.connect();
            }
        }).start();
    }

    public void sendBytes(byte[] data) {
        mConnection.sendBytes(data);
    }

    public void disconnect() {
        if (mConnection == null) return;
        mConnection.disconnect();
    }

    public void setOnDataReceivedListener(BluetoothCarConnection.DataReceivedListener listener) {
        mConnection.setOnDataReceivedListener(listener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (VDBG) Log.d(TAG, "onBind: I'm bound.");
        return new BluetoothServiceBinder();
    }

    public class BluetoothServiceBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }
}
