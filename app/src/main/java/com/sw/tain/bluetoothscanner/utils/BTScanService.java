package com.sw.tain.bluetoothscanner.utils;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by home on 2017/1/18.
 */

public class BTScanService extends Service{
    private static final String TAG = BTScanService.class.getName();
    private static final long SCAN_PERIOD = 15000;
    private boolean scanning = false;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter mBluetoothAdapter; //deviceListAdapter to scan for le devices
    // Initializes Bluetooth deviceListAdapter.
    private static BluetoothManager mBluetoothManager; //bluetooth system service management class
    private static boolean mBound = false;
    private static boolean mBluetoothSupported = false;
    private static boolean mGattConnected;
    private HashMap<String, OADDevice> mAvailableDevices = new HashMap<>();
    private ConcurrentMap<String, BluetoothGatt> mBluetoothGatts=new ConcurrentHashMap<>();
    private ConcurrentMap<String, Integer> mDeviceReadTasks = new ConcurrentHashMap<>();
    private final IBinder mBinder = new BLEServiceBinder();
    private OnBTDeviceFoundListner mInterface = null;
    private ConnectToDeviceQueue mConnectQueue = new ConnectToDeviceQueue();
    private AccessDeviceQueue mAccessQueue = new AccessDeviceQueue();
    private ConcurrentMap<String, Integer> mNotifiedDevices = new ConcurrentHashMap();


    public final static UUID UUID_DEVICE_NAME =
            UUID.fromString(SampleGattAttributes.DEVICE_NAME);
    public final static UUID UUID_MANUFACTURE_NAME =
            UUID.fromString(SampleGattAttributes.MANUFACTURE_NAME);


    public final static String ACTION_DEVICE_UPDATE =
            "com.sw.tain.bluetoothscanner.utils.BTScanService.ACTION_DEVICE_UPDATE";
    public final static String EXTRA_DATA =
            "com.sw.tain.bluetoothscanner.utils.BTScanService.EXTRA_DATA";

    public interface OnBTDeviceFoundListner{
        public void OnBTDeviceFound();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class BLEServiceBinder extends Binder {
        public BTScanService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BTScanService.this;
        }
}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            return null;
        }

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); //get service
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothSupported=true;
        if(mBluetoothAdapter == null){
            Log.i("TAG", "Bluetooth NOT SUPPORTED");
            mBluetoothSupported = false;
        }else if(mBluetoothAdapter.isEnabled()) {
            Log.i("TAG", "Bluetooth ENABLED");
        }else {
            Log.i("TAG", "Bluetooth DISABLED");
        }

        IntentFilter filter = new IntentFilter();
        //for test
         filter.addAction(BluetoothDevice.ACTION_UUID);
        //for test
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mActionUuidReciever, filter);




        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        //for test
        unregisterReceiver(mActionUuidReciever);
        //for test
        return super.onUnbind(intent);
    }

    //user methods//
    public boolean scanLeDevice(final boolean enable) {
        scanning = false;
        if(mBound && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())//if enabled
        {
            if (enable) {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            Thread.sleep(SCAN_PERIOD);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    }
//                }).start();
            /*

            和与扫描的同时连接设备相比，在结束扫描后在进行设备连接和读写操作似乎并没有提高效率

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(SCAN_PERIOD+10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mConnectQueue.processConnectQueue();
                    }
                }).start();
                */

                mBluetoothAdapter.startLeScan(mLeScanCallback);
                scanning = true;
            } else {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
        return scanning;
    }

    public void setInterface(OnBTDeviceFoundListner bleInterface) {
        mInterface = bleInterface;
    }
    public boolean isScanning() {return scanning;}
    public boolean isBluetoothSupported() {return mBluetoothSupported;}
    public boolean isBluetoothEnabled() {return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();}
    public boolean isConnected(){
        return mGattConnected;
    }

    public boolean connect(String address){
        if(mBluetoothAdapter==null || address == null) return false;

        BluetoothDevice d = mAvailableDevices.get(address).getDevice();
        mGattConnected = false;
        if(d != null)
        {

            BluetoothGatt bluetoothGatt=null;
            if(mBluetoothGatts.containsKey(address)){
//                bluetoothGatt = mBluetoothGatts.get(address);
//                return bluetoothGatt.connect();
                return false;
            }else{
                bluetoothGatt = d.connectGatt(this, false, mGattCallback);
                /*
                Passing true to connectGatt() autoconnect argument requests a background connection,
                while passing false requests a direct connection. BluetoothGatt#connect() always requests a background connection.

                Background connection (according to Bluedroid sources from 4.4.2 AOSP) has scan interval
                of 1280ms and a window of 11.25ms. This corresponds to about 0.9% duty cycle which explains why connections, when not scanning, can take a long time to complete.
                Direct connection has interval of 60ms and window of 30ms so connections complete much faster.

                Additionally there can only be one direct connection request pending at a time and it times out after 30 seconds.
                onConnectionStateChange() gets called with state=2, status=133 to indicate this timeout.
                */

//                try{
//                    Method m = d.getClass().getDeclaredMethod("connectGatt", Context.class, boolean.class, BluetoothGattCallback.class, int.class);
//                    int transport = d.getClass().getDeclaredField("TRANSPORT_LE").getInt(null);
//                    bluetoothGatt = (BluetoothGatt)m.invoke(d, this, false, mGattCallback, transport);
//                } catch (NoSuchMethodException e) {
//                    e.printStackTrace();
//                } catch (NoSuchFieldException e) {
//                    e.printStackTrace();
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                } catch (InvocationTargetException e) {
//                    e.printStackTrace();
//                }

                Log.d(TAG, "Trying to create a new connection.");
                if(bluetoothGatt != null){
                    mBluetoothGatts.put(address, bluetoothGatt);
                    return true;
                }else {
                    Log.i("GattClientService", "Device returned null Gatt Object");
                    return false;
                }
            }
        }
        return false;
    }

    public void disconnect(){
        if (mBluetoothAdapter == null || mBluetoothGatts.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        for(String key: mBluetoothGatts.keySet()){
            BluetoothGatt bluetoothGatt = mBluetoothGatts.get(key);
            bluetoothGatt.disconnect();
        }
    }

    public void close(){
        if (mBluetoothGatts.isEmpty()) {
            return;
        }
        listClose(null);
    }

    public HashMap<String, OADDevice>getAvailableDevices(){
//        ArrayList<Pair<String, OADDevice>> devices = new ArrayList<>();
//        for(String s: mAvailableDevices.keySet()){
//            devices.add(new Pair<>(s, mAvailableDevices.get(s)));
//        }
        return mAvailableDevices;

    }


    public void clearAvailableDevices(){
        mAvailableDevices.clear(); }


    public BluetoothGattCharacteristic findCharacteristic(BluetoothGatt gatt, String serviceUUID, String characteristicUUID){
        if(gatt == null) return null;
        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {
            if (service.getUuid().toString().equalsIgnoreCase(serviceUUID) ){
                for (BluetoothGattCharacteristic characteristicInList : service.getCharacteristics()) {
                    if (characteristicInList.getUuid().toString().equalsIgnoreCase(characteristicUUID) ){
                        return characteristicInList;
                    }
                }
            }
        }
        Log.d(TAG, "Characterisctic not found. Service: " + serviceUUID + " Characterisctic: " + characteristicUUID);
        return null;
    }


    public void read(BluetoothGatt gatt, BluetoothGattCharacteristic character){
        if(character!=null){
            gatt.readCharacteristic(character);
        } else {
            Log.d(TAG,"Read Characteristic not found in device");
        }

    }

    public void write(BluetoothGatt gatt, BluetoothGattCharacteristic character, String data){
        write(gatt, character,data.getBytes());
    }

    public void write(BluetoothGatt gatt, BluetoothGattCharacteristic character, byte[] data){

        if(character!=null){
            character.setValue(data);
            gatt.writeCharacteristic(character);
            Log.d(TAG,"Write Characteristic " + character.getUuid().toString() + " with value " + data);
        } else {
            Log.d(TAG,"Write Characteristic not found in device");
        }

    }

    //private methods
    private boolean checkGatt(String address) {
        if(mBluetoothGatts.containsKey(address)) return false;
        return true;
    }
    private void listClose(BluetoothGatt gatt) {
        if (!mBluetoothGatts.isEmpty()) {
            if (gatt != null) {
                if(mBluetoothGatts.containsValue(gatt)) {
                    gatt.close();
                    for(String key:mBluetoothGatts.keySet()) {
                        if(mBluetoothGatts.get(key).equals(gatt)) {
                            mBluetoothGatts.remove(key, gatt);
                            return;
                        }
                    }
                }
            }else{
                for (String key:mBluetoothGatts.keySet()) {
                    mBluetoothGatts.get(key).close();
                }
                mBluetoothGatts.clear();
            }
        }
    }

    private ConcurrentMap<String, List<UUID>> mDeviceGattSerivceListFromGattConnect = new ConcurrentHashMap<>();
    private ConcurrentMap<String, List<UUID>> mDeviceGattSerivceListFromFetchSrvs = new ConcurrentHashMap<>();

    private void readDeviceDetail(String deviceAddress, BluetoothGatt gatt) {
        OADDevice device = isDeviceDiscovered(deviceAddress);
        if(device==null) return;

        List<BluetoothGattService> gattSeriveList = gatt.getServices();

        //for test the differences between gatt.getServices and fetchUuidsWithSdp
//        List<UUID> uuidList = new ArrayList<>();
//        Log.d("GattService", gattSeriveList.size() + " Gatt services found on device " + deviceAddress + " :");
//        int i=0;
//        for(BluetoothGattService service:gattSeriveList){
//            uuidList.add(service.getUuid());
//            Log.d("GattService", "Service " + ++i + " UUID : " + service.getUuid().toString() );
//        }
//
//        mDeviceGattSerivceListFromGattConnect.put(deviceAddress, uuidList);

        //for test the differences between gatt.getServices and fetchUuidsWithSdp

        StringBuilder serviceStrBuilder = new StringBuilder("");

        BluetoothGattCharacteristic devicename_character=null;
        BluetoothGattCharacteristic manufacturename_character=null;
        int tasks = 0;

        for(BluetoothGattService service :gattSeriveList){
            String uuid = service.getUuid().toString();
            String serviceStr = SampleGattAttributes.lookup(uuid, "");
            if(!TextUtils.isEmpty(serviceStr)){
                serviceStrBuilder.append(serviceStr+" | ");
            }
            List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
            for(BluetoothGattCharacteristic characteristic : characteristicList){
                UUID characterUuid = characteristic.getUuid();
                if(characterUuid.equals(UUID_DEVICE_NAME)){
                    tasks++;
                    devicename_character = characteristic;
                }
                if(characterUuid.equals(UUID_MANUFACTURE_NAME)){
                    tasks++;
                    manufacturename_character = characteristic;
                }
            }
        }
        device.setDeviceService(serviceStrBuilder.toString());

        Log.d(TAG, "discover services on " + deviceAddress);

        if(tasks==0)  {
            broadcastUpdate(BTScanService.ACTION_DEVICE_UPDATE, deviceAddress);
            finishDeviceReadTask(deviceAddress);
            Log.d(TAG, "connection to " + gatt.getDevice().getAddress() + " disconnected");
            return;
        }else{
            mDeviceReadTasks.put(deviceAddress, tasks);
            if(devicename_character!=null) {
                TxQueueItem item = new TxQueueItem(TxQueueItemType.ReadCharacteristic, gatt, devicename_character, null);
                mAccessQueue.addToTxQueue(item);
            }
            if(manufacturename_character!=null){
                TxQueueItem item = new TxQueueItem(TxQueueItemType.ReadCharacteristic, gatt, manufacturename_character, null);
                mAccessQueue.addToTxQueue(item);
            }
        }
        Log.d(TAG, "discover services finished on " + deviceAddress);
    }

    private void readDeviceCharacter(String address, BluetoothGattCharacteristic characteristic) {
        OADDevice device = isDeviceDiscovered(address);
        if(device==null) return;

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

    private void finishDeviceReadTask(String deviceAddress) {

        Log.d(TAG, "close task on " + deviceAddress);
        mDeviceReadTasks.remove(deviceAddress);
        BluetoothGatt gatt = mBluetoothGatts.get(deviceAddress);
        gatt.disconnect();
//        listClose(gatt);
// 有人说为什么不在gatt.disconnect();后加一条gatt.close();呢，原因是如果立即执行gatt.close();会导致gattCallback无法收到STATE_DISCONNECTED的状态。
// 当然，最好的办法是在gattCallback收到STATE_DISCONNECTED后再执行gatt.close();，这样逻辑上会更清析一些。
//        mConnectQueue.processConnectQueue();
    }


    private OADDevice isDeviceDiscovered(String deviceAddress){
        if(mAvailableDevices.containsKey(deviceAddress)){
            return mAvailableDevices.get(deviceAddress);
        }else {
            return null;
        }
    }

    private void broadcastUpdate(final String action, final String macAddress) {
        if(mNotifiedDevices.containsKey(macAddress)) return;
        mNotifiedDevices.put(macAddress, 1);

        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, macAddress);
        sendBroadcast(intent);
    }

    /* An enqueueable operation for device connection at maximum connection number*/
    class ConnectToDeviceQueueItem{

        private final String mDeviceAddress;
        public String getDeviceAddress() {
            return mDeviceAddress;
        }

        public ConnectToDeviceQueueItem(String deviceAddress) {
            mDeviceAddress = deviceAddress;
        }
    }

    class ConnectToDeviceQueue{
        static final int MAX_CONNECT_QUEUE_SIZE = 1;
        ConcurrentLinkedQueue<ConnectToDeviceQueueItem> mConnectQueue = new ConcurrentLinkedQueue<>();
        int mConnectingTasks = 0;


        void  addToConnectQueue(ConnectToDeviceQueueItem item){
            mConnectQueue.add(item);

            if(mConnectingTasks<MAX_CONNECT_QUEUE_SIZE){
                processConnectQueue();
            }
        }

        void processConnectQueue(){

            if(mConnectQueue.size()==0 || mConnectingTasks==MAX_CONNECT_QUEUE_SIZE ) return;
            mConnectingTasks++;

            ConnectToDeviceQueueItem item = mConnectQueue.remove();
            Log.d(TAG, "connection to " + item.getDeviceAddress() + " started");
            connect(item.getDeviceAddress());
        }

        void disconnectOneConnection(){
            mConnectingTasks--;
        }
    }

    /* An enqueueable read/write operation for device characteristics*/
    class TxQueueItem{
        BluetoothGatt mGatt;
        BluetoothGattCharacteristic mCharacteristic;
        byte[] dataToWrite;
        public TxQueueItemType mType;

        public TxQueueItem(TxQueueItemType type, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] dataToWrite) {
            mType = type;
            mGatt = gatt;
            mCharacteristic = characteristic;
            this.dataToWrite = dataToWrite;
        }
    }

    enum TxQueueItemType{
        ReadCharacteristic,
        WriteCharacteristic
    }

    class AccessDeviceQueue{
        Queue<TxQueueItem> mAccessQueue = new LinkedList<>();
        boolean mAccessQueueProcessing = false;

        void addToTxQueue(TxQueueItem item){
            mAccessQueue.add(item);

            if(!mAccessQueueProcessing){
                processTxQueue();
            }
        }

        void processTxQueue(){
            if(mAccessQueue.size()<=0){
                mAccessQueueProcessing = false;
                return;
            }

            mAccessQueueProcessing = true;
            TxQueueItem item = mAccessQueue.remove();
            switch (item.mType){
                case WriteCharacteristic:
                    write(item.mGatt, item.mCharacteristic, item.dataToWrite);
                    break;
                case ReadCharacteristic:
                    read(item.mGatt, item.mCharacteristic);
                    break;
                default:
                    return;
            }
        }
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    if(mAvailableDevices.get(device.getAddress()) == null)
                    {

                        /**
                         * this call to fetchUuidsWithSdp will cause bluetooth share stopped in htc m9
                         * may be can not called during discovery
                         */
                        //for test
                        //device.fetchUuidsWithSdp();
                        //for test

                        new NewDeviceTask(device, rssi, scanRecord).execute();

//                        OADDevice bondDevice = new OADDevice(device.getName(), device.getAddress(),
//                                OADDevice.NetworkType.BLUETOOTH_BLE_DEVICE, false, device, device.getBluetoothClass());
//                        mAvailableDevices.put(device.getAddress(), bondDevice);
//                        ConnectToDeviceQueueItem item = new ConnectToDeviceQueueItem(device.getAddress());
//                        mConnectQueue.addToConnectQueue(item);
                    }
                }
            };

    //AsynchTask to check it's a new beacon(GAP) device or GATT device
    final static BeaconParser[] mBeaconParsers = {new BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT),
            new BeaconParser().setBeaconLayout(BeaconParser.IBEACON_LAYOUT),
            new BeaconParser().setBeaconLayout(BeaconParser.URI_BEACON_LAYOUT),
            new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT),
            new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT),
            new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT)};

    private class NewDeviceTask extends AsyncTask<Void, Void, Void>{
        BluetoothDevice device;
        int rssi;
        byte[] scanRecord;

        NewDeviceTask (final BluetoothDevice device, final int rssi,
                       final byte[] scanRecord){
            this.device = device;
            this.rssi = rssi;
            this.scanRecord = scanRecord;

        }
        @Override
        protected Void doInBackground(Void... params) {

            Beacon beacon=null;
            for(BeaconParser parser : mBeaconParsers ){
                beacon = parser.fromScanData(scanRecord, rssi, device);
                if(beacon != null) break;
            }

            OADDevice bondDevice = new OADDevice(device.getName(), device.getAddress(),
                    OADDevice.NetworkType.BLUETOOTH_BLE_DEVICE, false, device, device.getBluetoothClass());
            if (beacon != null) {

                if(beacon.getServiceUuid()== 0xfeaa) { // This is an Eddystone beacon
                    switch (beacon.getBeaconTypeCode()) {
                        case 0x00: bondDevice.setName("Eddystone-UID"); break;
                        case 0x10: bondDevice.setName("Eddystone-URL"); break;
                        case 0x20: bondDevice.setName("Eddystone-TLM"); break;
                        default:   bondDevice.setName("Eddystone"); break;
                    }
                }else if(beacon.getServiceUuid()== 0xfed8){
                    bondDevice.setName("URI_BEACON");
                }else{// This is an iBeacon or ALTBeacon
                    if(beacon.getBeaconTypeCode()==0xbeac) bondDevice.setName("ALTBEACON");
                    if(beacon.getBeaconTypeCode()==0x0215) bondDevice.setName("iBEACON");
                }
                bondDevice.setDeviceService("UUID: " + beacon.getId1().toString()
                        + "; Major: " + beacon.getId2().toString()
                        + "; Minor: " + beacon.getId3().toString());
                mAvailableDevices.put(device.getAddress(), bondDevice);
                broadcastUpdate(ACTION_DEVICE_UPDATE, device.getAddress());
            }else{
                mAvailableDevices.put(device.getAddress(), bondDevice);
                ConnectToDeviceQueueItem item = new ConnectToDeviceQueueItem(device.getAddress());
                mConnectQueue.addToConnectQueue(item);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

        }
    }

    //Action_UUID callback for fetchUuidsWithSdp
    private final BroadcastReceiver mActionUuidReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_UUID.equals(action)){
                BluetoothDevice device =  intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable[] puuids;
                puuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                Log.d("Fetch Service", puuids.length + " fetch UUID services found on device " + device.getAddress() + " :");
                List<UUID> uuidList = new ArrayList<>();
                int i=0;
                for(;i<puuids.length; i++){
                    uuidList.add(UUID.fromString(puuids[i].toString()));
                    Log.d("Fetch Service", "Service " + i+1 + " UUID : " + puuids[i].toString());
                }
                mDeviceGattSerivceListFromFetchSrvs.put(device.getAddress(), uuidList);
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                /**
                 * do not happen when call startLeScan
                 */
            }
        }
    };

    //gatt callback
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback(){
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "connection to " + gatt.getDevice().getAddress() + " connected, with status: " + status + "state: " + newState );
                String s = gatt.discoverServices()==true? "success":"false";
                Log.d(TAG, "connection to " + gatt.getDevice().getAddress() + " connected, start service discovery " + s);
                final BluetoothGatt mGatt = gatt;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(15000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if(mBluetoothGatts.containsKey(mGatt.getDevice().getAddress())){
                            mGatt.disconnect();
                            Log.d(TAG, "connection to " + mGatt.getDevice().getAddress() + " forced to be disconnected!!!");
                        }
                    }
                }).start();


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("GattClientService:", "Disonnected to Gatt Server on Device: " + gatt.getDevice());
                //设备连接中断，关闭连接，并调用mConnectQueue.processConnectQueue()建立下一个设备的连接
 //               gatt.disconnect();//required?
                Log.d(TAG, "connection to " + gatt.getDevice().getAddress() + " disconnected");
                listClose(gatt);
                broadcastUpdate(ACTION_DEVICE_UPDATE, gatt.getDevice().getAddress());//会造成重复通知
                mConnectQueue.disconnectOneConnection();
                mConnectQueue.processConnectQueue();
            }
        }
        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readDeviceDetail(gatt.getDevice().getAddress(), gatt);
            }

        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //需要在此处判断设备的读取任务是否已完成，若完成则可调用mConnectQueue.processConnectQueue()建立下一个设备的连接
                Log.i("GattClientService:", "Read GattCharacteristic:" + characteristic);
                if(!characteristic.getUuid().equals(UUID_DEVICE_NAME)
                        && !characteristic.getUuid().equals(UUID_MANUFACTURE_NAME)){
                    return;
                }
                if(!mDeviceReadTasks.containsKey(gatt.getDevice().getAddress())) return;

                int tasks = mDeviceReadTasks.get(gatt.getDevice().getAddress());
                readDeviceCharacter(gatt.getDevice().getAddress(), characteristic);

                tasks--;
                if(tasks==0) {
                    broadcastUpdate(ACTION_DEVICE_UPDATE, gatt.getDevice().getAddress());
                    finishDeviceReadTask(gatt.getDevice().getAddress());
                    Log.d(TAG, "connection to " + gatt.getDevice().getAddress() + " finished");
                }else{
                    mDeviceReadTasks.put(gatt.getDevice().getAddress(), tasks);
                }
                mInterface.OnBTDeviceFound();
                mAccessQueue.processTxQueue();
                return;
            }else{
                Log.i("GattClientService:", "Failed to Read GattCharacteristic: STATUS:" +status);
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("GattClientService:", "Wrote GattCharacteristic:" + characteristic);
            }else{
                Log.i("GattClientService:", "Failed to Wrote GattCharacteristic: STATUS:" +status);
            }
        }
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("GattClientService:", "Wrote GattDescriptor" + descriptor);
            }else{
                Log.i("GattClientService:", "Failed to Wrote GattDescriptor: STATUS:" +status);
            }
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("GattClientService:", "Wrote GattDescriptor:" + descriptor);
            }else{
                Log.i("GattClientService:", "Failed to Wrote GattDescriptor: STATUS:" +status);
            }
        }
    };




}
