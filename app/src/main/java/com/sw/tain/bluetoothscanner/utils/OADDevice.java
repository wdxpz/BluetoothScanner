package com.sw.tain.bluetoothscanner.utils;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;

/**
 * Created by home on 2017/1/6.
 */

public class OADDevice{
    private int mNetworkType;
    private boolean mIsAccessible;

    private String mName;
    private String mMacAddress;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothClass mBluetoothClass;

    private static final HashMap<Integer, String> sMajorDeviceNameMap;
    private static final HashMap<Integer, String> sDeviceNameMap;
    private static final HashMap<Integer, String> SDeviceServiceNameMap;

    public static final class NetworkType{
        public static final int BLUETOOTH_DEVICE = 1;
        public static final int BLUETOOTH_BONDED_DEVICE = 2;
        public static final int BLUETOOTH_BLE_DEVICE = 3;
        public static final int WIFI_DEVICE = 10;
        public static final int ZIGGY_DEVICE = 20;
    }

    static{
        sMajorDeviceNameMap = new HashMap<>();
        sMajorDeviceNameMap.put(BluetoothClass.Device.Major.AUDIO_VIDEO, "AUDIO_VIDEO");
        sMajorDeviceNameMap.put(BluetoothClass.Device.Major.COMPUTER, "COMPUTER");
        sMajorDeviceNameMap.put(BluetoothClass.Device.Major.HEALTH, "HEALTH");
        sMajorDeviceNameMap.put(BluetoothClass.Device.Major.IMAGING, "IMAGING");
        sMajorDeviceNameMap.put(BluetoothClass.Device.Major.MISC, "MISC");
        sMajorDeviceNameMap.put(BluetoothClass.Device.Major.NETWORKING, "NETWORKING");
        sMajorDeviceNameMap.put(BluetoothClass.Device.Major.PERIPHERAL, "PERIPHERAL");
        sMajorDeviceNameMap.put(BluetoothClass.Device.Major.PHONE, "PHONE");
        sMajorDeviceNameMap.put(BluetoothClass.Device.Major.TOY, "TOY");
        sMajorDeviceNameMap.put(BluetoothClass.Device.Major.UNCATEGORIZED, "UNCATEGORIZED");
        sMajorDeviceNameMap.put(BluetoothClass.Device.Major.WEARABLE, "WEARABLE");

        sDeviceNameMap = new HashMap<>();
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER, "AUDIO_VIDEO_CAMCORDER");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO, "AUDIO_VIDEO_CAR_AUDIO");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE, "AUDIO_VIDEO_HANDSFREE");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES, "AUDIO_VIDEO_HEADPHONES");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO, "AUDIO_VIDEO_HIFI_AUDIO");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER, "AUDIO_VIDEO_LOUDSPEAKER");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE, "AUDIO_VIDEO_MICROPHONE");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO, "AUDIO_VIDEO_PORTABLE_AUDIO");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX, "AUDIO_VIDEO_SET_TOP_BOX");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED, "AUDIO_VIDEO_UNCATEGORIZED");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_VCR, "AUDIO_VIDEO_VCR");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA, "AUDIO_VIDEO_VIDEO_CAMERA");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING, "AUDIO_VIDEO_VIDEO_CONFERENCING");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER, "AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY, "AUDIO_VIDEO_VIDEO_GAMING_TOY");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR, "AUDIO_VIDEO_VIDEO_MONITOR");
        sDeviceNameMap.put(BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET, "AUDIO_VIDEO_WEARABLE_HEADSET");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_DESKTOP, "COMPUTER_DESKTOP");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA, "COMPUTER_HANDHELD_PC_PDA");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_LAPTOP, "COMPUTER_LAPTOP");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA, "COMPUTER_PALM_SIZE_PC_PDA");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_SERVER, "COMPUTER_SERVER");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_UNCATEGORIZED, "COMPUTER_UNCATEGORIZED");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_WEARABLE, "COMPUTER_WEARABLE");
        sDeviceNameMap.put(BluetoothClass.Device.HEALTH_BLOOD_PRESSURE, "HEALTH_BLOOD_PRESSURE");
        sDeviceNameMap.put(BluetoothClass.Device.HEALTH_DATA_DISPLAY, "HEALTH_DATA_DISPLAY");
        sDeviceNameMap.put(BluetoothClass.Device.HEALTH_GLUCOSE, "HEALTH_GLUCOSE");
        sDeviceNameMap.put(BluetoothClass.Device.HEALTH_PULSE_OXIMETER, "HEALTH_PULSE_OXIMETER");
        sDeviceNameMap.put(BluetoothClass.Device.HEALTH_PULSE_RATE, "HEALTH_PULSE_RATE");
        sDeviceNameMap.put(BluetoothClass.Device.HEALTH_THERMOMETER, "HEALTH_THERMOMETER");
        sDeviceNameMap.put(BluetoothClass.Device.HEALTH_UNCATEGORIZED, "HEALTH_UNCATEGORIZED");
        sDeviceNameMap.put(BluetoothClass.Device.HEALTH_WEIGHING, "HEALTH_WEIGHING");
        sDeviceNameMap.put(BluetoothClass.Device.PHONE_CELLULAR, "PHONE_CELLULAR");
        sDeviceNameMap.put(BluetoothClass.Device.PHONE_CORDLESS, "PHONE_CORDLESS");
        sDeviceNameMap.put(BluetoothClass.Device.PHONE_ISDN, "PHONE_ISDN");
        sDeviceNameMap.put(BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY, "PHONE_MODEM_OR_GATEWAY");
        sDeviceNameMap.put(BluetoothClass.Device.PHONE_SMART, "PHONE_SMART");
        sDeviceNameMap.put(BluetoothClass.Device.TOY_CONTROLLER, "TOY_CONTROLLER");
        sDeviceNameMap.put(BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE, "TOY_DOLL_ACTION_FIGURE");
        sDeviceNameMap.put(BluetoothClass.Device.TOY_GAME, "TOY_GAME");
        sDeviceNameMap.put(BluetoothClass.Device.TOY_ROBOT, "TOY_ROBOT");
        sDeviceNameMap.put(BluetoothClass.Device.TOY_UNCATEGORIZED, "TOY_UNCATEGORIZED");
        sDeviceNameMap.put(BluetoothClass.Device.TOY_VEHICLE, "TOY_VEHICLE");
        sDeviceNameMap.put(BluetoothClass.Device.WEARABLE_GLASSES, "WEARABLE_GLASSES");
        sDeviceNameMap.put(BluetoothClass.Device.WEARABLE_HELMET, "WEARABLE_HELMET");
        sDeviceNameMap.put(BluetoothClass.Device.WEARABLE_JACKET, "WEARABLE_JACKET");
        sDeviceNameMap.put(BluetoothClass.Device.WEARABLE_PAGER, "WEARABLE_PAGER");
        sDeviceNameMap.put(BluetoothClass.Device.WEARABLE_UNCATEGORIZED, "WEARABLE_UNCATEGORIZED");
        sDeviceNameMap.put(BluetoothClass.Device.WEARABLE_WRIST_WATCH, "WEARABLE_WRIST_WATCH");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_LAPTOP, "COMPUTER_LAPTOP");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_LAPTOP, "COMPUTER_LAPTOP");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_LAPTOP, "COMPUTER_LAPTOP");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_LAPTOP, "COMPUTER_LAPTOP");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_LAPTOP, "COMPUTER_LAPTOP");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_LAPTOP, "COMPUTER_LAPTOP");
        sDeviceNameMap.put(BluetoothClass.Device.COMPUTER_LAPTOP, "COMPUTER_LAPTOP");

        SDeviceServiceNameMap = new HashMap<>();
        SDeviceServiceNameMap.put(BluetoothClass.Service.AUDIO, "AUDIO");
        SDeviceServiceNameMap.put(BluetoothClass.Service.CAPTURE, "CAPTURE");
        SDeviceServiceNameMap.put(BluetoothClass.Service.INFORMATION, "INFORMATION");
        SDeviceServiceNameMap.put(BluetoothClass.Service.LIMITED_DISCOVERABILITY, "LIMITED_DISCOVERABILITY");
        SDeviceServiceNameMap.put(BluetoothClass.Service.NETWORKING, "NETWORKING");
        SDeviceServiceNameMap.put(BluetoothClass.Service.OBJECT_TRANSFER, "OBJECT_TRANSFER");
        SDeviceServiceNameMap.put(BluetoothClass.Service.POSITIONING, "POSITIONING");
        SDeviceServiceNameMap.put(BluetoothClass.Service.RENDER, "RENDER");
        SDeviceServiceNameMap.put(BluetoothClass.Service.TELEPHONY, "TELEPHONY");

    }

    public int getNetworkType() {
        return mNetworkType;
    }

    public void setNetworkType(int networkType) {
        mNetworkType = networkType;
    }

    public OADDevice(String name, String macAddress) {

        new OADDevice(name, macAddress, NetworkType.BLUETOOTH_DEVICE, false);
    }



    public OADDevice(String name, String macAddress, int networkType, boolean isAccessible) {
        new OADDevice(name, macAddress, networkType, isAccessible, null, null);
    }

    public OADDevice(String name, String macAddress, int networkType, boolean isAccessible, BluetoothDevice bluetoothDevice, BluetoothClass bluetoothClass) {
        mName = name;
        mMacAddress = macAddress;
        mNetworkType = networkType;
        mIsAccessible = isAccessible;
        mBluetoothDevice = bluetoothDevice;
        mBluetoothClass = bluetoothClass;
    }

    public boolean isAccessible() {        return mIsAccessible;    }

    public void setAccessible(boolean accessible) {        mIsAccessible = accessible;    }

    public String getMacAddress() {        return mMacAddress;    }

    public void setMacAddress(String macAddress) {        mMacAddress = macAddress;    }

    public String getName() {        return mName;    }

    public void setName(String name) {        mName = name;    }

    public String getDeviceMajorClass(){
        if(mBluetoothClass==null){
            return null;
        }
        if(sMajorDeviceNameMap.containsKey(mBluetoothClass.getMajorDeviceClass())){
            Log.d("BTScanner", "Major Device Class: "+mBluetoothClass.getMajorDeviceClass());
            return sMajorDeviceNameMap.get(mBluetoothClass.getMajorDeviceClass());
        }else{
            return null;
        }

    }

    public String getDeviceClasse(){
        if(mBluetoothClass==null){
            return null;
        }
        if(sDeviceNameMap.containsKey(mBluetoothClass.getDeviceClass())){
            Log.d("BTScanner", "Device Class: "+mBluetoothClass.getDeviceClass());
            return sDeviceNameMap.get(mBluetoothClass.getDeviceClass());
        }else{
            return null;
        }

    }

    public String getDeviceService(){
        if(mBluetoothClass==null) return null;

        StringBuilder serviceStr=new StringBuilder("");

        for(Integer key: SDeviceServiceNameMap.keySet() ){
            if(mBluetoothClass.hasService(key)){
                serviceStr.append(SDeviceServiceNameMap.get(key));
                serviceStr.append("|");
            }
        }

        if(TextUtils.isEmpty(serviceStr.toString())){
            return null;
        }else{
            return serviceStr.replace(serviceStr.lastIndexOf("|"), serviceStr.length(), "").toString();
        }

    }

    @Override
    public String toString() {
        return "Device Name: " + mName
                + "; Device Address: " + mMacAddress;
 //               + "; Device Type: " + getDeviceMajorClass();
    }
}
