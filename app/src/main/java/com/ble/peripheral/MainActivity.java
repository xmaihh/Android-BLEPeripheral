package com.ble.peripheral;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

import javax.security.auth.callback.CallbackHandler;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BlePeripheral";
    private TextView textView;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private static final int REQUEST_ENABLE_BT = 1;
    private static UUID UUID_SERVER = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_CHARWRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothManager mBluetoothManager;
    private boolean RUN_THREAD = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        init();
        showText("starting ... ");
        if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            initGattServer();
        } else {
            textView.setTextColor(getResources().getColor(R.color.colorAccent));
            showText("This device not support peripheral");
        }
    }

    /**
     * 首先检查是否支持BLE、BLE Peripheral
     */
    private void init() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_LONG).show();
//            finish();
        }

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //判断是否打开蓝牙,如果没有打开需要回调打开蓝牙,这里省略,请自行打开蓝牙
        }

        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "This device not support peripheral", Toast.LENGTH_SHORT).show();
        }

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Toast.makeText(this, "This device not support peripheral", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 构造广播函数的三个重要参数：广播设置参数、广播数据、Callback
     */
    private void initGattServer() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(true)
                .build();
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .build();
        AdvertiseData scanResponseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(UUID_SERVER))
                .setIncludeTxPowerLevel(false)
                .build();
        AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                showText("1. initGattServer Success");
                initServices(getApplicationContext());
                if (settingsInEffect != null) {
                    Log.d(TAG, "onStartSuccess TxPowerLv=" + settingsInEffect.getTxPowerLevel() + " mode=" + settingsInEffect.getMode()
                            + " timeout=" + settingsInEffect.getTimeout());
                } else {
                    Log.e(TAG, "onStartSuccess, settingInEffect is null");
                }
                Log.e(TAG, "onStartSuccess settingsInEffect" + settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                showText("1. initGattServer failed");
            }
        };
        mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback);
    }

    private void initServices(Context ctx) {
        mBluetoothGattServer = mBluetoothManager.openGattServer(ctx, mBluetoothGattCallback);
        BluetoothGattService service = new BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // add a write characteristic
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID_CHARWRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE |
                        BluetoothGattCharacteristic.PERMISSION_READ);

        // add a descriptor
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID_DESCRIPTOR,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        characteristicWrite.addDescriptor(descriptor);

        service.addCharacteristic(characteristicWrite);
        mBluetoothGattServer.addService(service);

        showText("2. initServices OK");
    }

    private BluetoothGattServerCallback mBluetoothGattCallback = new BluetoothGattServerCallback() {

        private Thread mThread;

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.e(TAG, String.format("1.onConnectionStateChange:device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("1.onConnectionStateChange:status = %s, newState =%s ", status, newState));
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            Log.e(TAG, String.format("onServiceAdded：status = %s", status));
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.e(TAG, String.format("onCharacteristicReadRequest:device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("onCharacteristicReadRequest:requestId = %s, offset = %s", requestId, offset));

            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestBytes) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, requestBytes);
            Log.e(TAG, String.format("3.onCharacteristicWriteRequest:device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("3.onCharacteristicWriteRequest:requestId = %s, preparedWrite=%s, responseNeeded=%s, offset=%s, value=%s", requestId, preparedWrite, responseNeeded, offset, StringUtils.toHexString(requestBytes)));
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, requestBytes);
            // 4. 处理响应内容
            onResponseToClient(requestBytes, device, requestId, characteristic);
        }

        int i = 0;

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            Log.e(TAG, String.format("2.onDescriptorWriteRequest:device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("2.onDescriptorWriteRequest:requestId = %s, preparedWrite = %s, responseNeeded = %s, offset = %s, value = %s,", requestId, preparedWrite, responseNeeded, offset, StringUtils.toHexString(value)));

            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);

            if (value[0] == 1) {  //enable notification
                RUN_THREAD = true;
                mThread = new Thread() {
                    @Override
                    public void run() {
                        while (true) {

                            try {
                                Thread.sleep(10);
                                notifyData(device, new byte[]{(byte) i}, false);
                                i++;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                mThread.start();
            } else {  // disable notification
                RUN_THREAD = false;
                mThread.interrupt();
                mThread = null;
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.e(TAG, String.format("onDescriptorReadRequest:device name = %s, address = %s", device.getName(), device.getAddress()));
            Log.e(TAG, String.format("onDescriptorReadRequest:requestId = %s", requestId));

            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            Log.e(TAG, String.format("onNotificationSent  name = %s, address = %s", device.getName(), device.getAddress()));
        }
    };

    private void onResponseToClient(byte[] requestBytes, BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristicRead) {
        Log.e(TAG, String.format("4.onResponseToClient:device name = %s, address = %s", device.getName(), device.getAddress()));
        Log.e(TAG, String.format("4.onResponseToClient:requestId = %s", requestId));
        String msg = StringUtils.transferForPrint(requestBytes);
        showText("4.收到:" + msg);

        String str = new String(requestBytes) + " hello>";
        characteristicRead.setValue(str.getBytes());
        mBluetoothGattServer.notifyCharacteristicChanged(device, characteristicRead, false);

        showText("4.回應:" + str);
    }

    private void notifyData(BluetoothDevice device, byte[] value, boolean confirm) {
        BluetoothGattCharacteristic characteristic = null;
        for (BluetoothGattService service : mBluetoothGattServer.getServices()) {
            for (BluetoothGattCharacteristic mCharacteristic : service.getCharacteristics()) {
                if (mCharacteristic.getDescriptor(UUID_DESCRIPTOR) != null) {
                    characteristic = mCharacteristic;
                    break;
                }
            }
            if (characteristic != null) {
                characteristic.setValue(value);
                mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, confirm);
            }
        }
    }

    private void showText(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.append(str + "\r\n");
            }
        });
    }
}
