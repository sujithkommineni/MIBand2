package com.sujith.heartrate.btconnection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.sujith.heartrate.R;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

/**
 * Created by sujit on 27-10-2017.
 */

public class BTDeviceManager implements BluetoothAdapter.LeScanCallback {

    private Context mContext;
    private BTDeviceConnectivityListener mDeviceConnectionListener;
    private BluetoothAdapter mBTAdapter;
    private BTDeviceConnection mConnection;

    private static BTDeviceManager sInstance;

    public static final int CONNECTIVIY_NOT_CONNECTED = 0;
    public static final int CONNECTIVIY_PROGRESS = 1;
    public static final int CONNECTIVIY_CONNECTED = 2;

    private int mBTDeviceStatus = CONNECTIVIY_NOT_CONNECTED;

    public static final String DEV_NAME = "MI Band 2"; // WEB

    @Override
    public void onLeScan(BluetoothDevice bDevice, int i, byte[] bytes) {
        Timber.d("onLeScan .. " + bDevice.toString());
        Timber.d("onLeScan .. address" + bDevice.getAddress());
        Timber.d("onLeScan .. getName: " + bDevice.getName());
        Timber.d("onLeScan .. getType: " + bDevice.getType());
        Timber.d("onLeScan .. class : " + bDevice.getBluetoothClass());
        Timber.d("onLeScan .. UUIDs : " + Arrays.toString(bDevice.getUuids()));

        String deviceName = bDevice.getName();
        if (deviceName == null) {
            BleAdvertisedData data = BleUtil.parseAdertisedData(bytes);
            deviceName = data.getName();
        }
        if (DEV_NAME.equals(deviceName)) {
            Timber.d(deviceName + " found.");
            // TODO what if multiple devices are available
            mBTAdapter.stopLeScan(this);
            //mTextMain.setText("LE search, WEB found");
            connectToDevice(bDevice);
        }

    }

    public interface BTDeviceConnectivityListener {
        void onDeviceConnectionStateChange(int connected, BTDeviceConnection connection);
    }

    public static synchronized BTDeviceManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BTDeviceManager(context);
        }
        return sInstance;
    }

    private BTDeviceManager(Context context) {
        mContext = context.getApplicationContext();
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public String getDeviceName() {
        if (mConnection != null) {
            BluetoothDevice dev = mConnection.getmBTDevice();
            return dev.getName() + "(" + dev.getAddress() + ")";
        }
        return null;
    }

    public void close() {
        if (mBTAdapter != null) mBTAdapter.stopLeScan(this);
        if (mConnection != null) mConnection.close();
        mBTDeviceStatus = CONNECTIVIY_NOT_CONNECTED;
        mDeviceConnectionListener = null;
        mConnection = null;
    }

    // TODO implement disconnect device which clears the listener, otherwise there will be a Fragment leak.
    public void scanForLeDevice(BTDeviceConnectivityListener listener) {
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mContext, R.string.bt_le_not_supported, Toast.LENGTH_SHORT).show();
            listener.onDeviceConnectionStateChange(CONNECTIVIY_NOT_CONNECTED, null);
            return;
        }
        Timber.d("scanForLeDevice, current state: %s", mBTDeviceStatus);
        mDeviceConnectionListener = listener;

        if (mBTDeviceStatus == CONNECTIVIY_NOT_CONNECTED) {
            boolean foundPaired = false;

            BluetoothManager btManager =
                    (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            List<BluetoothDevice> devices = btManager.getConnectedDevices(BluetoothProfile.GATT);
            Timber.d("Paired devices size: " + devices.size());
            for (BluetoothDevice device : devices) {
                if (device.getName().toLowerCase().contains("mi")) {
                    Timber.d("connected device found. NOT SCANNING");
                    connectToDevice(device);
                    foundPaired = true;
                    break;
                }
            }

            // Check if the device is already paired.
            /*Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
            Timber.d("total paired devices: " + pairedDevices.size());
            for (BluetoothDevice device : pairedDevices) {
                Timber.d("device: " + device.getName());
                if (DEV_NAME.equals(device.getName())) {
                    Timber.d("Paired device found. NOT SCANNING");
                    connectToDevice(device);
                    foundPaired = true;
                    break;
                }
            }*/

            if (!foundPaired) startScan();
            mBTDeviceStatus = CONNECTIVIY_PROGRESS;
        } else if (mBTDeviceStatus == CONNECTIVIY_CONNECTED) {
            Timber.d("scanForLeDevice().. already connected. Not starting scan");
        }
        listener.onDeviceConnectionStateChange(mBTDeviceStatus, mConnection);
    }


    private void startScan() {
        Timber.d("startScan()..");
        mBTAdapter.startLeScan(BTDeviceConnection.MI_BAND_UUIDS, this);
    }

    private void connectToDevice(final BluetoothDevice device) {

        if (mConnection != null && mConnection.getmBTDevice() != null) {
            BluetoothDevice connectedDev = mConnection.getmBTDevice();
            if (device.getAddress().equals(connectedDev.getAddress())) {
                if (mConnection.reconnect()) {
                    Timber.d("ReConnect() is success!! not creating new connection");
                    return;
                }
            }
        }

        mConnection = new BTDeviceConnection(device, mContext, new BTDeviceConnection.GattConnectionListener() {
            @Override
            public void onConnectedToGatt(boolean connected) {
                if (connected) {
                    mBTDeviceStatus = CONNECTIVIY_CONNECTED;
                    if (mDeviceConnectionListener != null) mDeviceConnectionListener.onDeviceConnectionStateChange(mBTDeviceStatus, mConnection);
                } else {
                    mBTDeviceStatus = CONNECTIVIY_NOT_CONNECTED;
                    if (mDeviceConnectionListener != null) mDeviceConnectionListener.onDeviceConnectionStateChange(mBTDeviceStatus, null);
                }

            }
        });
    }

}
