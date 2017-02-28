package com.sw.tain.bluetoothscanner.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by home on 2017/1/5.
 */


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BTScanner {
    private final static String TAG = BTScanner.class.getSimpleName();
    private static final long SCAN_PERIOD = 10000;

    private static BTScanner mBTScanner;
    private final Context mContext;
    private final BluetoothAdapter mBluetoothAdapter;
    private static ArrayList<OADDevice> mBluetoothDevices;
    private final Handler mMainUIHnadler;
    private BluetoothLeScanner mLEScanner=null;
    private ScanSettings settings=null;
    private ArrayList<ScanFilter> filters=null;
    private OnBTDeviceFoundListner mOnBTDeviceFoundListner;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mReadBLECharacteristicTasks;



    public final static UUID UUID_DEVICE_NAME =
            UUID.fromString(SampleGattAttributes.DEVICE_NAME);
    public final static UUID UUID_MANUFACTURE_NAME =
            UUID.fromString(SampleGattAttributes.MANUFACTURE_NAME);

    private BluetoothAdapter.LeScanCallback mLeScanCallback =  new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            final OADDevice bondDevice = new OADDevice(device.getName(), device.getAddress(),
                    OADDevice.NetworkType.BLUETOOTH_BLE_DEVICE, false, device, device.getBluetoothClass());

            mLock.lock();
            try{
                int pos = isDeviceFounded(bondDevice.getMacAddress());
                if(pos<0){
                    mBluetoothDevices.add(bondDevice);
                    mOnBTDeviceFoundListner.OnBTDeviceFound();
                    Log.d(TAG, "find new BLE device: " + bondDevice.getMacAddress());

                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            copnnectToBLEDevice(bondDevice);
                        }
                    });
                    t.start();
                }else{
                    if(mBluetoothDevices.get(pos).getNetworkType()!=OADDevice.NetworkType.BLUETOOTH_BLE_DEVICE){
                        mBluetoothDevices.get(pos).setNetworkType(OADDevice.NetworkType.BLUETOOTH_BLE_DEVICE);
                        mOnBTDeviceFoundListner.OnBTDeviceFound();
                        Log.d(TAG, "find new BLE device: " + bondDevice.getMacAddress() + "; Stop discovery, Reading device detail ");
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                copnnectToBLEDevice(bondDevice);
                            }
                        });
                        t.start();
                    }
                }

            }finally {
                mLock.unlock();
            }
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for(ScanResult sr: results){
                Log.d(TAG, "ScanResult_Results:  "+ sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "Scan Failed, Error Code: " + errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            final OADDevice bondDevice = new OADDevice(device.getName(), device.getAddress(),
                    OADDevice.NetworkType.BLUETOOTH_BLE_DEVICE, false, device, device.getBluetoothClass());

            mLock.lock();
            try{
                int pos = isDeviceFounded(bondDevice.getMacAddress());
                if(pos<0){
                    mBluetoothDevices.add(bondDevice);
                    mOnBTDeviceFoundListner.OnBTDeviceFound();
                    Log.d(TAG, "find new BLE device: " + bondDevice.getMacAddress());
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            copnnectToBLEDevice(bondDevice);
                        }
                    });
                    t.start();
                }else{
                    if(mBluetoothDevices.get(pos).getNetworkType()!=OADDevice.NetworkType.BLUETOOTH_BLE_DEVICE){
                        mBluetoothDevices.get(pos).setNetworkType(OADDevice.NetworkType.BLUETOOTH_BLE_DEVICE);
                        mOnBTDeviceFoundListner.OnBTDeviceFound();
                        Log.d(TAG, "find new BLE device: " + bondDevice.getMacAddress() + "; Stop discovery, Reading device detail ");
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                copnnectToBLEDevice(bondDevice);
                            }
                        });
                        t.start();
                    }
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

 //       mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


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

        if(!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(mContext, "BLE not supported", Toast.LENGTH_SHORT).show();
        }
        final BluetoothManager bluetoothManager = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if(Build.VERSION.SDK_INT>=21){
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();
        }

    }


    public void initBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            // Bluetooth isn't enabled, prompt the user to turn it on.
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(intent);
        }
    }

    public void startDiscovery() {

        Log.d(TAG, "start discovery");
        stopDiscovery();
        mBluetoothDevices.clear();


        //step 1. scan the bonded bluetooth devices
        Set<BluetoothDevice> bondedDeviceList = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice device : bondedDeviceList){
            OADDevice bondDevice = new OADDevice(device.getName(), device.getAddress(),
                    OADDevice.NetworkType.BLUETOOTH_BONDED_DEVICE, false, device, device.getBluetoothClass());
            mBluetoothDevices.add(bondDevice);

        }
        mOnBTDeviceFoundListner.OnBTDeviceFound();

        resumeDiscovery();
    }

    public void resumeDiscovery(){

        Log.d(TAG, "resume discovery");
        //step 2. scan the classic bluetooth devices
        mContext.registerReceiver(discoveryResult,
                new IntentFilter(BluetoothDevice.ACTION_FOUND));

        if (mBluetoothAdapter.isEnabled()){
            if(mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.cancelDiscovery();
            }
            mBluetoothAdapter.startDiscovery();
        }

        //step 3. scan the BLE devices
        if(!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mContext, "BLE hardware not supported!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

            scanLeDevice(true);
        }else{
            Toast.makeText(mContext, "SDK version is low to support BLE!", Toast.LENGTH_SHORT).show();
        }
    }

    private void scanLeDevice(final boolean enable){
        if(enable){
            mMainUIHnadler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(Build.VERSION.SDK_INT<21){
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }else{
                        mLEScanner.stopScan(mScanCallback);
                    }
                }
            }, SCAN_PERIOD);
            if(Build.VERSION.SDK_INT<21){
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }else{
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        }else{
            if(Build.VERSION.SDK_INT<21){
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }else{
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    public void stopDiscovery() {
        //Cancel scan of classic bluetooth devices
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            mContext.unregisterReceiver(discoveryResult);
        }
        //Cancel scan of BLE bluetooth devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            scanLeDevice(false);
        }
    }

    private int isDeviceFounded(String deviceAddress){
        for(int i=0;  i<mBluetoothDevices.size(); i++){
            if(mBluetoothDevices.get(i).getMacAddress().equals(deviceAddress)) return i;
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
                    if(isDeviceFounded(device.getMacAddress())<0){
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


    private void copnnectToBLEDevice(OADDevice bondDevice) {
        if(!connect(bondDevice.getMacAddress())){
            Log.d(TAG, "failed to connect BLE device: " + bondDevice.getMacAddress());
            return;
        }
    }

    private void readOADDeviceService(String deviceAddress, BluetoothGatt gatt) {
        int pos = isDeviceFounded(deviceAddress);
        if(pos<0) return;
        OADDevice device = mBluetoothDevices.get(pos);

        List<BluetoothGattService> gattSeriveList = gatt.getServices();

        StringBuilder serviceStrBuilder = new StringBuilder("");

        BluetoothGattCharacteristic devicename_character=null;
        BluetoothGattCharacteristic manufacturename_character=null;
        mReadBLECharacteristicTasks = 0;

        for(BluetoothGattService service :gattSeriveList){
            String uuid = service.getUuid().toString();
            String serviceStr = SampleGattAttributes.lookup(uuid, "");
            if(!serviceStr.equals("")){
                serviceStrBuilder.append(serviceStr+" | ");
            }
            List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
            for(BluetoothGattCharacteristic characteristic : characteristicList){
                UUID characterUuid = characteristic.getUuid();
                if(characterUuid.equals(UUID_DEVICE_NAME)){
                    devicename_character = characteristic;
                    mReadBLECharacteristicTasks++;
                }
                if(characterUuid.equals(UUID_MANUFACTURE_NAME)){
                    manufacturename_character = characteristic;
                    mReadBLECharacteristicTasks++;
                }
            }
        }
        if(mReadBLECharacteristicTasks==0)  { closeBLEConnection(); resumeDiscovery();}
        if(devicename_character!=null) gatt.readCharacteristic(devicename_character);
        if(manufacturename_character!=null) gatt.readCharacteristic(manufacturename_character);
        device.setDeviceService(serviceStrBuilder.toString());
    }

    private void readLeDeviceCharacter(String deviceAddress, BluetoothGattCharacteristic characteristic) {

        int pos = isDeviceFounded(deviceAddress);
        if(pos<0) return;
        OADDevice device = mBluetoothDevices.get(pos);

        if(characteristic.getUuid().equals(UUID_DEVICE_NAME)) {
            String name = characteristic.getStringValue(0);
            Log.d(TAG, "read device:  " + device.getMacAddress() + "'s device name:  " + name);
            device.setName(name);
            return;
        }
        if(characteristic.getUuid().equals(UUID_MANUFACTURE_NAME)){
            String name = characteristic.getStringValue(0);
            Log.d(TAG, "read device:  " + device.getMacAddress() + "'s manufacture name: " + name);
            device.setManufacture(name);
            return;
        }
        return;

    }


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        gatt.discoverServices() + "on device: " + gatt.getDevice().getAddress());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server, restart discovery");
                closeBLEConnection();
                resumeDiscovery();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                List<BluetoothGattService> gattSeriveList = gatt.getServices();
                readOADDeviceService(gatt.getDevice().getAddress(), gatt);
                mMainUIHnadler.post(new Runnable() {
                    @Override
                    public void run() {
                        mOnBTDeviceFoundListner.OnBTDeviceFound();
                    }
                });
            } else {
                Log.w(TAG, "onServicesDiscovered failed on devcie:" + gatt.getDevice().getAddress() + "; resume discovery");
                closeBLEConnection();
                resumeDiscovery();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onCharacteristicRead: " + characteristic);
                if(!characteristic.getUuid().equals(UUID_DEVICE_NAME)
                        && !characteristic.getUuid().equals(UUID_MANUFACTURE_NAME)){
                    return;
                }
                mReadBLECharacteristicTasks--;
                readLeDeviceCharacter(gatt.getDevice().getAddress(), characteristic);
                if(mReadBLECharacteristicTasks==0){
                    gatt.disconnect();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.w(TAG, "onCharacteristicChanged: " + characteristic);
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };




    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }


        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        BluetoothGatt bluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection with device: " + device.getAddress());
        stopDiscovery(); //will stop after first device detection
        Log.d(TAG, "Stop discovery, start to reading device detail. ");
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void closeBLEConnection() {

        if (mBluetoothGatt == null) {
            return;
        }

        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

}
