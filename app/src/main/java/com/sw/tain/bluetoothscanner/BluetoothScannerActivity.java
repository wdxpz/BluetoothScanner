package com.sw.tain.bluetoothscanner;


import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.sw.tain.bluetoothscanner.utils.BTScanner;


public class BluetoothScannerActivity extends AppCompatActivity implements BTScanner.OnBTDeviceFoundListner{


    private BluetoothScanListFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_scanner);

        FragmentManager fm = getSupportFragmentManager();
        mFragment = (BluetoothScanListFragment)fm.findFragmentById(R.id.fragment_scan_result_list);
//        if(mFragment==null)
//            mFragment = new BluetoothScannerFragment();

        if(mFragment==null)
            mFragment = new BluetoothScanListFragment();

        fm.beginTransaction()
                .add(R.id.fragment_scan_result_list, mFragment)
                .commit();
    }


    @Override
    public void OnBTDeviceFound() {
        mFragment.updateUI();
    }
}
