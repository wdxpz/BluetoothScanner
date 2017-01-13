package com.sw.tain.bluetoothscanner;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.sw.tain.bluetoothscanner.utils.BTScanner;
import com.sw.tain.bluetoothscanner.utils.OADDevice;

public class BluetoothDeviceActivity extends AppCompatActivity {

    private static final String ARGS_DEVICE = "args_device";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device);

        Intent intent = getIntent();
        int position = intent.getIntExtra(ARGS_DEVICE, 0);

        FragmentManager fm = getSupportFragmentManager();
        BluetoothDeviceFragment fragment = (BluetoothDeviceFragment)fm.findFragmentById(R.id.layout_fragment_bluetooth_device);

        if(fragment==null){
            fragment = BluetoothDeviceFragment.newInstance(position);
            fm.beginTransaction()
                    .add(R.id.layout_fragment_bluetooth_device, fragment)
                    .commit();
        }


    }

    public static Intent newIntent(Context context, int position){
        Intent intent = new Intent(context, BluetoothDeviceActivity.class);
        intent.putExtra(ARGS_DEVICE, position);

        return intent;
    }
}
