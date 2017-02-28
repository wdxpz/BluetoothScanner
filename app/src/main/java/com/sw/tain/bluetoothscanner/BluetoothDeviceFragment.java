package com.sw.tain.bluetoothscanner;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.sw.tain.bluetoothscanner.utils.BTScanner;
import com.sw.tain.bluetoothscanner.utils.OADDevice;


/**
 * A simple {@link Fragment} subclass.
 */
public class BluetoothDeviceFragment extends Fragment {


    private static final String ARGS_DEVICE = "args_device";
    private OADDevice mOADDevice;
    private TextView mDeviceName;
    private TextView mDeviceAddress;
    private TextView mDeviceMajorClass;
    private TextView mDeviceClass;
    private TextView mDeviceService;
    private Button mButtonConnectDevice;

    public static BluetoothDeviceFragment newInstance(int position) {

        Bundle args = new Bundle();
        args.putInt(ARGS_DEVICE, position);

        BluetoothDeviceFragment fragment = new BluetoothDeviceFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Bundle args = getArguments();
        int position = args.getInt(ARGS_DEVICE);
        mOADDevice = BTScanner.getBluetoothDevices().get(position);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_bluetooth_device, container, false);

        mDeviceName = (TextView)v.findViewById(R.id.text_view_device_name);
        mDeviceName.setText(mOADDevice.getName()!=null?mOADDevice.getName():"Unkown");

        mDeviceAddress = (TextView)v.findViewById(R.id.text_view_device_address);
        mDeviceAddress.setText(mOADDevice.getMacAddress()!=null?mOADDevice.getMacAddress():"Unkown");

        mDeviceMajorClass = (TextView)v.findViewById(R.id.text_view_device_major_class);
        mDeviceMajorClass.setText(mOADDevice.getManufacture()!=null?mOADDevice.getManufacture():"Unkown");

        mDeviceClass = (TextView)v.findViewById(R.id.text_view_device_class);
        mDeviceClass.setText(mOADDevice.getDeviceClasse()!=null?mOADDevice.getDeviceClasse():"Unkown");

        mDeviceService = (TextView)v.findViewById(R.id.text_view_device_service);
        mDeviceService.setText(mOADDevice.getDeviceService()!=null?mOADDevice.getDeviceService():"Unkown");

        mButtonConnectDevice = (Button)v.findViewById(R.id.button_connect_device);
        mButtonConnectDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(getActivity(), DeviceControlActivity.class);
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, mOADDevice.getName());
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, mOADDevice.getMacAddress());
                startActivity(intent);
            }
        });

        return v;
    }

}
