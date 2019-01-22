package com.sujith.heartrate.btconnection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;


import com.sujith.heartrate.btconnection.data.GetLogDataResponse;
import com.sujith.heartrate.btconnection.data.ResponseData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import timber.log.Timber;

/**
 * Created by sujit on 27-10-2017.
 */

public class BTDeviceConnection extends BluetoothGattCallback {

    private static final UUID WATCH_SERVICE_UUID = UUID.fromString("c51ba5d5-0002-8000-1000-000012340000");
    private static final UUID CHAR_WRITE_UUID = UUID.fromString("c51da5d5-0002-8000-1000-000012340000");
    private static final UUID CHAR_NOTIFY_UUID = UUID.fromString("c51ca5d5-0002-8000-1000-000012340000");

    public static final String LOG_TAG = "BTDeviceConnection";

    private static final UUID MI_BAND_SERVICE = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    public static final UUID[] MI_BAND_UUIDS = {MI_BAND_SERVICE};

    private BluetoothGatt mBTGatt;
    private BluetoothDevice mBTDevice;
    private Context mContext;
    private GattConnectionListener mConnectionListener;
    private boolean mConnectionSuccessful = false;
    private HandlerThread mHandlerThread = new HandlerThread("btRequestQueueThread");

    private Handler mWorkerHandler;
    private Object lock = new Object();
    private boolean mWaitingForResponse;
    private ArrayList<Byte> mResponseList = new ArrayList<>();
    private boolean mExpectingMultipleResponses;


    private static final int MSG_RESPONSE_WAITING_TIMER_UP = 10;
    private Handler mUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_RESPONSE_WAITING_TIMER_UP) {
                Timber.d("Waiting timer expired... ");
                continueQueueExecution();
                return;
            }
            super.handleMessage(msg);
        }
    };

    public interface ResponseListener {
        void onResponse(ResponseData object);
    }

    interface GattConnectionListener {
        void onConnectedToGatt(boolean connected);
    }



    public BTDeviceConnection(BluetoothDevice device, Context context, GattConnectionListener listener) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("WRONG THREAD.. Should be initialized in main thread");
        }
        mHandlerThread.start();
        mWorkerHandler = new Handler(mHandlerThread.getLooper());
        mBTDevice = device;
        mContext = context;
        mConnectionListener = listener;
        mBTGatt = device.connectGatt(context, false, this);
    }

    /**
     * Should be called before discarding this object. Once called, this Object can't be
     * used anymore.
     */
    public void close() {
        Timber.d("close()..");
        if (mBTGatt != null) {
            mBTGatt.disconnect();
            mBTGatt.close();
            mUiHandler.removeCallbacksAndMessages(null);
            mWorkerHandler.removeCallbacksAndMessages(null);
            mHandlerThread.quit();
        }
    }

    public boolean reconnect() {
        if (mBTGatt != null) {
            return mBTGatt.connect();
        }
        return false;
    }

    public BluetoothDevice getmBTDevice() {
        return mBTDevice;
    }




    public void getHeartRate(final ResponseListener listener) {
        Timber.d("getHeartRate()..");
        if (!mConnectionSuccessful) {
            Timber.i("Connection to BT device is not ready yet.. ignoring");
            return;
        }
        final byte[] getHeartRate = MiBandUtils.getHeartRate();
        mWorkerHandler.post(new Runnable() {
            @Override
            public void run() {
                final byte[] response = writeAndWaitForResponse(getHeartRate);
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onResponse(new ResponseData(true, Arrays.toString(response)));
                    }
                });
            }
        });
    }

    private byte[] writeAndWaitForResponse(byte[] query) {
        Timber.v("writeAndWaitForResponse().. data: %s", Arrays.toString(query));
        BluetoothGattCharacteristic characteristic
                =  mBTGatt.getService(MiBandUtils.HeartRate.service).getCharacteristic(MiBandUtils.HeartRate.controlCharacteristic);
        characteristic.setValue(query);
        mWaitingForResponse = true;
        mBTGatt.writeCharacteristic(characteristic);
        waitForResponse();
        BluetoothGattCharacteristic changedCharacteristic
                =  mBTGatt.getService(MiBandUtils.HeartRate.service).getCharacteristic(MiBandUtils.HeartRate.measurementCharacteristic);
        byte[] response = changedCharacteristic.getValue();
        changedCharacteristic.setValue(new byte[0]);
        Timber.d("writeAndWaitForResponse().. response : %s", Arrays.toString(response));
        return response;
    }

    private byte[] writeAndWaitForNoResponse(byte[] query) {
        Timber.v("writeAndWaitForNoResponse().. data: %s", Arrays.toString(query));
        BluetoothGattCharacteristic characteristic
                =  mBTGatt.getService(WATCH_SERVICE_UUID).getCharacteristic(CHAR_WRITE_UUID);
        characteristic.setValue(query);
        mWaitingForResponse = true;
        mBTGatt.writeCharacteristic(characteristic);

        // Though we don't expect any response for this, There may be NACK responses. So, we'll wait
        // for a moment & continue.
        setWaitingTimer();
        waitForResponse();
        removeWaitingTimer();

        BluetoothGattCharacteristic changedCharacteristic
                =  mBTGatt.getService(WATCH_SERVICE_UUID).getCharacteristic(CHAR_NOTIFY_UUID);
        byte[] response = changedCharacteristic.getValue();
        changedCharacteristic.setValue(new byte[0]);
        Timber.d("writeAndWaitForNoResponse().. response : %s", Arrays.toString(response));
        return response;
    }

    private void writeAndWaitForMultipleResponses(byte[] query) {
        Timber.v("writeAndWaitForMultipleResponses().. data: %s", Arrays.toString(query));
        BluetoothGattCharacteristic characteristic
                =  mBTGatt.getService(WATCH_SERVICE_UUID).getCharacteristic(CHAR_WRITE_UUID);
        characteristic.setValue(query);
        mWaitingForResponse = true;
        mExpectingMultipleResponses = true;
        mResponseList.clear();
        mBTGatt.writeCharacteristic(characteristic);

        setWaitingTimer();
        waitForResponse();
        removeWaitingTimer();

        mWaitingForResponse = false;
        mExpectingMultipleResponses = false;

        BluetoothGattCharacteristic changedCharacteristic
                =  mBTGatt.getService(WATCH_SERVICE_UUID).getCharacteristic(CHAR_NOTIFY_UUID);

        changedCharacteristic.setValue(new byte[0]);

        Timber.d("writeAndWaitForMultipleResponses().. response size : %s", mResponseList.size());
    }


    private void setWaitingTimer() {
        mUiHandler.sendEmptyMessageDelayed(MSG_RESPONSE_WAITING_TIMER_UP, 1000);
    }

    private void removeWaitingTimer() {
        mUiHandler.removeMessages(MSG_RESPONSE_WAITING_TIMER_UP);
    }

    private void waitForResponse() {
        Timber.d("waitForResponse()..");
        synchronized (lock) {
            while (mWaitingForResponse) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void continueQueueExecution() {
        Timber.d("continueQueueExecution()..");
        synchronized (lock) {
            mWaitingForResponse = false;
            lock.notify();
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Timber.d("onConnectionStateChange()..");
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Timber.d("onConnectionStateChange().. STATE_CONNECTED services : " + gatt.getServices());
            gatt.discoverServices();

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Timber.d("onConnectionStateChange().. STATE_DISCONNECTED device : " + gatt.getDevice());
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mConnectionSuccessful = false;
                    mConnectionListener.onConnectedToGatt(false);
                }
            });
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Timber.d("onServicesDiscovered status: " + status + ", services: " + gatt.getServices());

        List<BluetoothGattService> services = gatt.getServices();
        for (BluetoothGattService service : services) {

            Timber.d("service found : " + service.getUuid());
            Timber.d("service found type: " + service.getType());

            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                Timber.d("characteristic : " + characteristic.getUuid());
                int properties = characteristic.getProperties();
                Timber.d("characteristic properties: " + properties);
                if ((properties & BluetoothGattCharacteristic.PROPERTY_READ )> 0) {
                    Timber.d("PROPERTY_READ");
                }
                if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY )> 0) {
                    Timber.d("PROPERTY_NOTIFY");
                }
                if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE )> 0) {
                    Timber.d("PROPERTY_WRITE");
                }
                if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE )> 0) {
                    Timber.d("PROPERTY_WRITE_NO_RESPONSE");
                }

                List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    Timber.d("descriptor: " + descriptor.getUuid());
                }

            }

        }

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                mConnectionSuccessful = true;
                mConnectionListener.onConnectedToGatt(true);
            }
        });

        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                BluetoothGattCharacteristic characteristic
                        =  mBTGatt.getService(MiBandUtils.HeartRate.service).getCharacteristic(MiBandUtils.HeartRate.measurementCharacteristic);
                mBTGatt.setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(MiBandUtils.HeartRate.descriptor);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBTGatt.writeDescriptor(descriptor);
                mConnectionSuccessful = true;
                mConnectionListener.onConnectedToGatt(true);
            }
        });
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        Timber.d("onCharacteristicWrite()..");
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        continueQueueExecution();
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Timber.d("onCharacteristicChanged().. multiple res: " + mExpectingMultipleResponses + "obj: " + this);
        if (mExpectingMultipleResponses) {
            byte[] response = characteristic.getValue();
            for (byte b : response) {
                mResponseList.add(b);
            }
            removeWaitingTimer();
            setWaitingTimer();
        } else {
            continueQueueExecution();
        }
    }

}
