package org.hamster.carz;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Carz-MainActivity";
    private static final boolean VDBG = true;
    private static final int FILTER_TYPE_ALL = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (VDBG)
                    Log.v(TAG, "FAB click, starting BT device chooser.");

                BluetoothDeviceManager.BluetoothDevicePickResultHandler handler;
                handler = new BluetoothDeviceManager.BluetoothDevicePickResultHandler() {
                    @Override
                    public void onDevicePicked(BluetoothDevice device) {
                        Log.v(TAG, "Device picked, name=" + device.getName() + " address=" + device.getAddress());
                    }
                };

                BluetoothDeviceManager manager = new BluetoothDeviceManager(MainActivity.this);
                manager.pickDevice(handler);
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
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
