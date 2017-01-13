package com.sw.tain.bluetoothscanner.utils;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by home on 2017/1/5.
 */

public class BTScanner {
    private static String TAG = "BTScanner";

    private static BTScanner mBTScanner;
    private final Context mContext;
    private final BluetoothAdapter mBluetoothAdapter;
    private static ArrayList<OADDevice> mBluetoothDevices;
    private final Handler mMainUIHnadler;
    private OnBTDeviceFoundListner mOnBTDeviceFoundListner;

    private BluetoothAdapter.LeScanCallback mLeScanCallback =  new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            OADDevice bondDevice = new OADDevice(device.getName(), device.getAddress(),
                    OADDevice.NetworkType.BLUETOOTH_BLE_DEVICE, false, device, device.getBluetoothClass());

            mLock.lock();
            try{
                int pos = isDeviceFounded(bondDevice);
                if(pos<0){
                    mBluetoothDevices.add(bondDevice);
                    mOnBTDeviceFoundListner.OnBTDeviceFound();
                }else{
                    mBluetoothDevices.get(pos).setNetworkType(OADDevice.NetworkType.BLUETOOTH_BLE_DEVICE);
                }

            }finally {
                mLock.unlock();
            }

        }
    };

    private static final Lock mLock = new ReentrantLock();


    public interface OnBTDeviceFoundListner{
        public void OnBTDeviceFound();
    }

    public static BTScanner getInstance(Context context) {

        if(mBTScanner==null){
            mBTScanner = new BTScanner(context);

        }
        if(mBTScanner.mBluetoothAdapter==null)
            return null;
        else
            return mBTScanner;
    }

    public static ArrayList<OADDevice> getBluetoothDevices() {
        return mBluetoothDevices;
    }

    private BTScanner(Context context) {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mContext = context;
        mMainUIHnadler = new Handler(mContext.getMainLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((AppCompatActivity)mContext,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
        }


        mOnBTDeviceFoundListner = (OnBTDeviceFoundListner)context;

        mBluetoothDevices = new ArrayList<>();



    }


    public void initBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth isn't enabled, prompt the user to turn it on.
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(intent);
        }
    }

    public void startDiscovery() {
        //step 1. scan the bonded bluetooth devices
        Set<BluetoothDevice> bondedDeviceList = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice device : bondedDeviceList){
            OADDevice bondDevice = new OADDevice(device.getName(), device.getAddress(),
                    OADDevice.NetworkType.BLUETOOTH_BONDED_DEVICE, false, device, device.getBluetoothClass());
            mBluetoothDevices.add(bondDevice);

        }
        mOnBTDeviceFoundListner.OnBTDeviceFound();

        //step 2. scan the classic bluetooth devices
        mContext.registerReceiver(discoveryResult,
                new IntentFilter(BluetoothDevice.ACTION_FOUND));

        if (mBluetoothAdapter.isEnabled()){
            if(mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.cancelDiscovery();
            }
            mBluetoothDevices.clear();
            mBluetoothAdapter.startDiscovery();
        }

        //step 3. scan the BLE devices
        if(!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mContext, "BLE hardware not supported!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }else{
            Toast.makeText(mContext, "SDK version is low to support BLE!", Toast.LENGTH_SHORT).show();
        }


    }
    public void cancelDiscovery() {
        //Cancel scan of classic bluetooth devices
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            mContext.unregisterReceiver(discoveryResult);
        }


        //Cancel scan of BLE bluetooth devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private int isDeviceFounded(OADDevice device){
        for(int i=0;  i<mBluetoothDevices.size(); i++){
            if(mBluetoothDevices.get(i).getMacAddress().equals(device.getMacAddress())) return i;
        }
        return -1;
    }

    BroadcastReceiver discoveryResult = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){
                String remoteDeviceName =
                        intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                BluetoothDevice remoteDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothClass remoteDeviceClass =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);

                OADDevice device = new OADDevice(remoteDevice.getName(), remoteDevice.getAddress(),
                        OADDevice.NetworkType.BLUETOOTH_DEVICE, false, remoteDevice, remoteDeviceClass);

                mLock.lock();
                try{
                    if(isDeviceFounded(device)<0){
                        mBluetoothDevices.add(device);
                    }
                    mOnBTDeviceFoundListner.OnBTDeviceFound();
                }finally {
                    mLock.unlock();
                }
                Log.d(TAG, "Discovered " + remoteDeviceName);
            }
        }
    };
}
