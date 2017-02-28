package com.sw.tain.bluetoothscanner;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sw.tain.bluetoothscanner.utils.BTScanner;
import com.sw.tain.bluetoothscanner.utils.OADDevice;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.support.v7.widget.RecyclerView.*;

/**
 * A simple {@link Fragment} subclass.
 */
public class BluetoothScannerFragment extends Fragment{
    private static final long SCAN_PERIOD = 10000;


    private RecyclerView mRecyclerView;
    private BluetoothDeviceAdapter mAdapter;
    private BTScanner mScanner;
    private Handler mHandler;
    private List<OADDevice> mDeviceList;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        mRecyclerView =  (RecyclerView)inflater.inflate(R.layout.fragment_bluetooth_scanner, container, false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mScanner = BTScanner.getInstance(getContext());
        if(mScanner==null) return mRecyclerView;
        mDeviceList = mScanner.getBluetoothDevices();

        mAdapter = new BluetoothDeviceAdapter();
        mRecyclerView.setAdapter(mAdapter);

        mHandler = new Handler();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
//                mHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        mScanner.cancelDiscovery();
//                    }
//                }, SCAN_PERIOD);
                mScanner.initBluetooth();
//                mScanner.stopDiscovery();
                mScanner.startDiscovery();
            }
        });
        t.start();
        return mRecyclerView;

    }

    @Override
    public void onPause() {
        super.onPause();
        mScanner.stopDiscovery();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_bluetooth_scanner_fragment, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        mScanner.startDiscovery();
        mAdapter.notifyDataSetChanged();

        return super.onOptionsItemSelected(item);
    }

    public void updateUI() {
        mAdapter.notifyDataSetChanged();
    }

    private class BluetoothDeviceHolder extends ViewHolder {

        private final TextView mTextViewDeviceName;
        private final TextView mTextViewDeviceAdress;
        private final TextView mTextViewDeviceNetworkType;
        private final TextView mTextViewDeviceMajorClass;

        public BluetoothDeviceHolder(View itemView) {
            super(itemView);
            mTextViewDeviceName = (TextView)itemView.findViewById(R.id.text_view_device_name_device_list);
            mTextViewDeviceAdress = (TextView)itemView.findViewById(R.id.text_view_device_address_device_list);
            mTextViewDeviceMajorClass = (TextView)itemView.findViewById(R.id.text_view_device_major_class_device_list);
            mTextViewDeviceNetworkType = (TextView)itemView.findViewById(R.id.text_view_device_network_type_device_list);

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = mRecyclerView.getLayoutManager().getPosition(v);
                    Intent intent = BluetoothDeviceActivity.newIntent(getActivity(), pos);
                    mScanner.stopDiscovery();
                    startActivity(intent);
                }
            });
        }

        public void BindDevice(OADDevice device){
            mTextViewDeviceName.setText(device.getName()!=null?device.getName():"unknown");
            mTextViewDeviceAdress.setText(device.getMacAddress()!=null?device.getMacAddress():"unknown");
            mTextViewDeviceMajorClass.setText(device.getDeviceClasse()!=null?device.getDeviceClasse():"unknown");
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

    private class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceHolder>{

        @Override
        public BluetoothDeviceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_bluetooth_device_list, parent, false);
            return new BluetoothDeviceHolder(view);
        }

        @Override
        public void onBindViewHolder(BluetoothDeviceHolder holder, int position) {
            holder.BindDevice(mDeviceList.get(position));
        }

        @Override
        public int getItemCount() {
            return mDeviceList.size();
        }
    }

}
