package com.sw.tain.bluetoothscanner;


import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.sw.tain.bluetoothscanner.utils.BTScanService;
import com.sw.tain.bluetoothscanner.utils.BTScanner;
import com.sw.tain.bluetoothscanner.utils.BluetoothLeService;
import com.sw.tain.bluetoothscanner.utils.OADDevice;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.Adapter;
import static android.support.v7.widget.RecyclerView.OnClickListener;
import static android.support.v7.widget.RecyclerView.ViewHolder;

/**
 * A simple {@link Fragment} subclass.
 */
public class BluetoothScanListFragment extends Fragment implements BTScanService.OnBTDeviceFoundListner{
    private static final long SCAN_PERIOD = 30000;


    private RecyclerView mRecyclerView;
    private BluetoothDeviceAdapter mAdapter;
    private Handler mHandler;
    private ArrayList<OADDevice> mDeviceList = new ArrayList<>();

    private BTScanService mBTScanSerice=null;
    private boolean mBound = false;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        1);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        mRecyclerView =  (RecyclerView)inflater.inflate(R.layout.fragment_bluetooth_scanner, container, false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new BluetoothDeviceAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mHandler = new Handler();

        return mRecyclerView;

    }

    @Override
    public void onStart() {
        super.onStart();


        // Bind to GattClientService
        Intent intent = new Intent(getActivity(), BTScanService.class);
        getActivity().registerReceiver(mUpdateReceiver, makeGattUpdateIntentFilter());
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBound) {
            if(mBTScanSerice != null) {
                mBTScanSerice.scanLeDevice(false);
                mBTScanSerice.clearAvailableDevices();
                mBTScanSerice.close();
            }
            getActivity().unregisterReceiver(mUpdateReceiver);
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            OADDevice device = null;
            if (BTScanService.ACTION_DEVICE_UPDATE.equals(action)) {
                String address = intent.getStringExtra(BTScanService.EXTRA_DATA);
                if(mBTScanSerice.getAvailableDevices().containsKey(address)){
                    device = mBTScanSerice.getAvailableDevices().get(address);
                    if(device!=null){
                        mDeviceList.add(device);
                        updateUI();
                    }
                }

            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BTScanService.ACTION_DEVICE_UPDATE);
        return intentFilter;
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.i("ServiceConnection", "GattClientService Connected to activity");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mBTScanSerice = ((BTScanService.BLEServiceBinder) service).getService();
            if(mBTScanSerice!=null){
                mBTScanSerice.setInterface(BluetoothScanListFragment.this);
                mBTScanSerice.scanLeDevice(!mBTScanSerice.isScanning()); //toggle by NOTting current scan status
                if(!mBTScanSerice.isBluetoothEnabled()){ //if not enabled show user
                    Log.i("MainActivity", "Bluetooth Not Enabled");
                    Toast.makeText(getActivity(), "Bluetooth Not Enabled!", Toast.LENGTH_SHORT).show();
                }
            mBTScanSerice.setInterface(BluetoothScanListFragment.this);
                mBound = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.i("ServiceConnection", "GattClientService Disconnected from activity");

        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_bluetooth_scanner_fragment, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

    public void updateUI() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void OnBTDeviceFound() {
        updateUI();
    }

    private class BluetoothDeviceHolder extends ViewHolder {

        private final TextView mTextViewDeviceName;
        private final TextView mTextViewDeviceAdress;
        private final TextView mTextViewDeviceNetworkType;
        private final TextView mTextViewDeviceMajorClass;
        private final TextView mTextViewDeviceManufacture;
        private final TextView mTextViewDeviceService;

        public BluetoothDeviceHolder(View itemView) {
            super(itemView);
            mTextViewDeviceName = (TextView)itemView.findViewById(R.id.text_view_device_name_device_list);
            mTextViewDeviceAdress = (TextView)itemView.findViewById(R.id.text_view_device_address_device_list);
            mTextViewDeviceMajorClass = (TextView)itemView.findViewById(R.id.text_view_device_major_class_device_list);
            mTextViewDeviceNetworkType = (TextView)itemView.findViewById(R.id.text_view_device_network_type_device_list);
            mTextViewDeviceManufacture = (TextView)itemView.findViewById(R.id.text_view_device_manufacture_device_list);
            mTextViewDeviceService = (TextView)itemView.findViewById(R.id.text_view_device_service_device_list);


            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = mRecyclerView.getLayoutManager().getPosition(v);

//                    Intent intent = BluetoothDeviceActivity.newIntent(getActivity(), pos);
//                    startActivity(intent);
                    OADDevice device = mDeviceList.get(pos);
                    final Intent intent = new Intent(getActivity(), DeviceControlActivity.class);
                    intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                    intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getMacAddress());
                    startActivity(intent);
                }
            });
        }

        public void BindDevice(OADDevice device){
            mTextViewDeviceName.setText(device.getName()!=null?device.getName():"unknown");
            mTextViewDeviceAdress.setText(device.getMacAddress()!=null?device.getMacAddress():"unknown");
            mTextViewDeviceMajorClass.setText(device.getDeviceMajorClass()!=null?device.getDeviceMajorClass():"unknown");
            mTextViewDeviceManufacture.setText(device.getManufacture()!=null?device.getManufacture():"unknown");
            mTextViewDeviceService.setText(device.getDeviceService()!=null?device.getDeviceService():"unknown");
            switch (device.getNetworkType()){
                case OADDevice.NetworkType.BLUETOOTH_BONDED_DEVICE:
                    mTextViewDeviceNetworkType.setText("Bonded");
                    break;
                case OADDevice.NetworkType.BLUETOOTH_BLE_DEVICE:
                    mTextViewDeviceNetworkType.setText("BLE");
                    break;
                case OADDevice.NetworkType.BLUETOOTH_DEVICE:
                    mTextViewDeviceNetworkType.setText("Classic");
                    break;
                default:
                    mTextViewDeviceNetworkType.setText("");
                    break;
            }
        }

    }

    private class BluetoothDeviceAdapter extends Adapter<BluetoothDeviceHolder>{

        @Override
        public BluetoothDeviceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_bluetooth_device_list, parent, false);
            return new BluetoothDeviceHolder(view);
        }

        @Override
        public void onBindViewHolder(BluetoothDeviceHolder holder, int position) {
            if(mDeviceList==null) return;
            holder.BindDevice(mDeviceList.get(position));
        }

        @Override
        public int getItemCount() {
            if(mDeviceList==null) return 0;
            return mDeviceList.size();
        }
    }

}
