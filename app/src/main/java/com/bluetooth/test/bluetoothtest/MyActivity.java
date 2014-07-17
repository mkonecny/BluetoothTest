package com.bluetooth.test.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.bluetooth.le.*;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import com.bluetooth.test.bluetoothtest.BluetoothUtility;


public class MyActivity extends Activity {
    private static final String TAG = "MyActivity";
    BluetoothUtility ble;
    private String serviceOneCharUuid;
    private static final String SERVICE_UUID_1 = "00001802-0000-1000-8000-00805f9b34fb";
    private static final String SERVICE_DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb";

    Button startAdvButton;
    Button stopAdvButton;
    Button scanButton;
    Button stopScanButton;
    TextView bluetoothState;
    ArrayAdapter<String> btArrayAdapter;
    ListView listDevicesFound;
    private ArrayList<String> foundDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        scanButton = (Button)findViewById(R.id.scan_button);
        stopScanButton = (Button)findViewById(R.id.scan_stop_button);
        startAdvButton = (Button)findViewById(R.id.adv_start_button);
        stopAdvButton = (Button)findViewById(R.id.adv_stop_button);
        bluetoothState = (TextView)findViewById(R.id.bluetooth_state_text);
        listDevicesFound = (ListView)findViewById(R.id.devicesfound);
        btArrayAdapter = new ArrayAdapter<String>(MyActivity.this, android.R.layout.simple_list_item_1);
        listDevicesFound.setAdapter(btArrayAdapter);

        ble = new BluetoothUtility(this);

        ble.setAdvertiseCallback(advertiseCallback);
        ble.setGattServerCallback(gattServerCallback);
        //ble.setLeScanCallback(leScanCallback);
        ble.setScanCallback(scanCallback);

        foundDevices = new ArrayList<String>();

        addServiceToGattServer();
        //IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        //filter.addAction(BluetoothDevice.ACTION_UUID);
        //registerReceiver(ActionFoundReceiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ble.cleanUp();
        //unregisterReceiver(ActionFoundReceiver);
    }

    public void handleStartClick(View view) {
        ble.startAdvertise();
        startAdvButton.setEnabled(false);
        stopAdvButton.setEnabled(true);
    }

    public void handleStopClick(View view) {
        ble.stopAdvertise();
        startAdvButton.setEnabled(true);
        stopAdvButton.setEnabled(false);
    }

    public void handleScanStart(View view) {
        foundDevices.clear();
        btArrayAdapter.clear();
        ble.startBleScan();
        scanButton.setEnabled(false);
        stopScanButton.setEnabled(true);
    }

    public void handleScanStop(View view) {
        ble.stopBleScan();
        scanButton.setEnabled(true);
        stopScanButton.setEnabled(false);
    }

    //TODO currently scan yields rapid repeats of same device when found
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onAdvertisementUpdate(ScanResult scanResult) {
            BluetoothDevice device = scanResult.getDevice();
            if(foundDevices.contains(device.getAddress())) return;
            foundDevices.add(device.getAddress());
            String deviceInfo = device.getName() + " - " + device.getAddress();
            Log.d(TAG, "Device: " + deviceInfo + " Scanned!");
            //TODO use ScanRecord to retrieve more data
            ScanRecord scanRecord = ScanRecord.parseFromBytes(scanResult.getScanRecord());
            List<ParcelUuid> uuids = scanRecord.getServiceUuids();

            if(uuids != null) {
                Log.d(TAG, "UUIDS FOUND FROM DEVICE");
                for(int i = 0; i < uuids.size(); i++) {
                    deviceInfo += "\n" + uuids.get(i).toString();
                }
            }

            final String text = deviceInfo;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btArrayAdapter.add(text);
                    btArrayAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onScanFailed(int i) {
            Log.e(TAG, "Scan attempt failed");
        }
    };

    /*private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(foundDevices.contains(bluetoothDevice.getAddress())) return;
                foundDevices.add(bluetoothDevice.getAddress());

                bluetoothDevice.fetchUuidsWithSdp();
                Log.d(TAG, "Found a new device: " + bluetoothDevice.getAddress());
            }
        });
        }
    };*/

    /*public BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_UUID.equals(action)) {
                Log.d(TAG, "Action Received: " + action.toString());
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                String deviceInfo = device.getName() + " - " + device.getAddress();
                if(uuids != null) {
                    Log.d(TAG, uuids.toString());
                    for (int i = 0; i < uuids.length; i++) {
                        deviceInfo += uuids[i].toString() + "\n";
                    }
                } else {
                    Log.d(TAG, "No UUID's found");
                }
                Log.d(TAG, "Device: " + deviceInfo + " Scanned!");
                if(btArrayAdapter.getPosition(deviceInfo) == -1) {
                    btArrayAdapter.add(deviceInfo);
                    btArrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };*/

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onSuccess(AdvertiseSettings advertiseSettings) {
            String successMsg = "Advertisement command attempt successful";
            Log.d(TAG, successMsg);
        }

        @Override
        public void onFailure(int i) {
            String failMsg = "Advertisement command attempt failed: " + i;
            Log.e(TAG, failMsg);
        }
    };

    private void addServiceToGattServer() {
        serviceOneCharUuid = UUID.randomUUID().toString();

        BluetoothGattService firstService = new BluetoothGattService(
                UUID.fromString(SERVICE_UUID_1),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        // alert level char.
        BluetoothGattCharacteristic firstServiceChar = new BluetoothGattCharacteristic(
                UUID.fromString(serviceOneCharUuid),
                BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ |
                        BluetoothGattCharacteristic.PERMISSION_WRITE);
        firstService.addCharacteristic(firstServiceChar);
        ble.addService(firstService);
    }

    public BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            Log.d(TAG, "onConnectionStateChange status=" + status + "->" + newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "onCharacteristicReadRequest requestId=" + requestId + " offset=" + offset);

            if (characteristic.getUuid().equals(UUID.fromString(serviceOneCharUuid))) {
                Log.d(TAG, "SERVICE_UUID_1");
                characteristic.setValue("Text:This is a test characteristic");
                ble.getGattServer().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                        characteristic.getValue());
            }

        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.d(TAG, "onCharacteristicWriteRequest requestId=" + requestId + " preparedWrite="
                    + Boolean.toString(preparedWrite) + " responseNeeded="
                    + Boolean.toString(responseNeeded) + " offset=" + offset);
        }
    };

}


/*{ // device information
    BluetoothGattService dis = new BluetoothGattService(
            UUID.fromString(SERVICE_DEVICE_INFORMATION),
            BluetoothGattService.SERVICE_TYPE_PRIMARY);
    // manufacturer name string char.
    BluetoothGattCharacteristic mansc = new BluetoothGattCharacteristic(
            UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);
    // model number string char.
    BluetoothGattCharacteristic monsc = new BluetoothGattCharacteristic(
            UUID.fromString(BleUuid.CHAR_MODEL_NUMBER_STRING),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);
    // serial number string char.
    BluetoothGattCharacteristic snsc = new BluetoothGattCharacteristic(
            UUID.fromString(BleUuid.CHAR_SERIAL_NUMBEAR_STRING),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ);
    dis.addCharacteristic(mansc);
    dis.addCharacteristic(monsc);
    dis.addCharacteristic(snsc);
    gattServer.addService(dis);
}*/


/*package com.bluetooth.test.bluetoothtest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bluetooth.test.bluetoothtest.BleUtil;
import com.bluetooth.test.bluetoothtest.BleUuid;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisementData;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

public class MyActivity extends Activity {




    private static final String TAG = "aBeacon";
    private BluetoothManager mBTManager;
    private BluetoothAdapter mBTAdapter;
    private BluetoothGattServer mGattServer;
    private BluetoothLeAdvertiser mBTAdvertiser;
    private boolean mIsAdvertising = false;
    private byte[] mAlertLevel = new byte[] {
            (byte) 0x00
    };

    private AdvertiseCallback mAdvCallback = new AdvertiseCallback() {

        @Override
        public void onSuccess(AdvertiseSettings settingsInEffect) {
            // Advする際に設定した値と実際に動作させることに成功したSettingsが違うとsettingsInEffectに
            // 有効な値が格納される模様です。設定通りに動かすことに成功した際にはnullが返る模様。
            if (settingsInEffect != null) {
                Log.d(TAG, "onSuccess TxPowerLv="
                        + settingsInEffect.getTxPowerLevel()
                        + " mode=" + settingsInEffect.getMode()
                        + " type=" + settingsInEffect.getType());
            } else {
                Log.d(TAG, "onSuccess, settingInEffect is null");
            }
        }

        @Override
        public void onFailure(int errorCode) {
            Log.d(TAG, "onFailure errorCode=" + errorCode);
        }
    };

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServiceAdded status=GATT_SUCCESS service="
                        + service.getUuid().toString());
            } else {
                Log.d(TAG, "onServiceAdded status!=GATT_SUCCESS");
            }
        };

        public void onConnectionStateChange(android.bluetooth.BluetoothDevice device, int status,
                                            int newState) {
            // Log.d(TAG, "onConnectionStateChange status=" + status + "->" + newState);
        };

        // とにかくなんでもReadRequestとWriteRequestを通るっぽいので
        public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device,
                                                int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicReadRequest requestId=" + requestId + " offset=" + offset);
            if (characteristic.getUuid().equals(
                    UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING))) {
                Log.d(TAG, "CHAR_MANUFACTURER_NAME_STRING");
                characteristic.setValue("Name:Hoge");
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                        characteristic.getValue());
            } else if (characteristic.getUuid().equals(
                    UUID.fromString(BleUuid.CHAR_MODEL_NUMBER_STRING))) {
                Log.d(TAG, "CHAR_MODEL_NUMBER_STRING");
                characteristic.setValue("Model:Redo");
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                        characteristic.getValue());
            } else if (characteristic.getUuid().equals(
                    UUID.fromString(BleUuid.CHAR_SERIAL_NUMBEAR_STRING))) {
                Log.d(TAG, "CHAR_SERIAL_NUMBEAR_STRING");
                characteristic.setValue("Serial:777");
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                        characteristic.getValue());
            } else if (characteristic.getUuid().equals(
                    UUID.fromString(BleUuid.CHAR_ALERT_LEVEL))) {
                Log.d(TAG, "CHAR_ALERT_LEVEL");
                characteristic.setValue(mAlertLevel);
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                        characteristic.getValue());
            }
        };

        public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice device,
                                                 int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite,
                                                 boolean responseNeeded, int offset, byte[] value) {
            Log.d(TAG, "onCharacteristicWriteRequest requestId=" + requestId + " preparedWrite="
                    + Boolean.toString(preparedWrite) + " responseNeeded="
                    + Boolean.toString(responseNeeded) + " offset=" + offset);
            if (characteristic.getUuid().equals(
                    UUID.fromString(BleUuid.CHAR_ALERT_LEVEL))) {
                Log.d(TAG, "CHAR_ALERT_LEVEL");
                if (value != null && value.length > 0) {
                    Log.d(TAG, "value.length=" + value.length);
                    mAlertLevel[0] = value[0];
                } else {
                    Log.d(TAG, "invalid value written");
                }
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                        null);
            }
        };
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_my);

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();

        startAdvertise();
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopAdvertise();
    }

    private void init() {
        // BLE check
        if (!BleUtil.isBLESupported(this)) {
            Toast.makeText(this, "BLE is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // BT check
        mBTManager = BleUtil.getManager(this);
        if (mBTManager != null) {
            mBTAdapter = mBTManager.getAdapter();
        }
        if (mBTAdapter == null) {
            Toast.makeText(this, "Bluetooth is currently unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void startAdvertise() {
        if ((mBTAdapter != null) && (!mIsAdvertising)) {
            mGattServer = mBTManager.openGattServer(this, mGattServerCallback);
            { // immediate alert
                BluetoothGattService ias = new BluetoothGattService(
                        UUID.fromString(BleUuid.SERVICE_IMMEDIATE_ALERT),
                        BluetoothGattService.SERVICE_TYPE_PRIMARY);
                // alert level char.
                BluetoothGattCharacteristic alc = new BluetoothGattCharacteristic(
                        UUID.fromString(BleUuid.CHAR_ALERT_LEVEL),
                        BluetoothGattCharacteristic.PROPERTY_READ |
                                BluetoothGattCharacteristic.PROPERTY_WRITE,
                        BluetoothGattCharacteristic.PERMISSION_READ |
                                BluetoothGattCharacteristic.PERMISSION_WRITE);
                ias.addCharacteristic(alc);
                mGattServer.addService(ias);
            }

            { // device information
                BluetoothGattService dis = new BluetoothGattService(
                        UUID.fromString(BleUuid.SERVICE_DEVICE_INFORMATION),
                        BluetoothGattService.SERVICE_TYPE_PRIMARY);
                // manufacturer name string char.
                BluetoothGattCharacteristic mansc = new BluetoothGattCharacteristic(
                        UUID.fromString(BleUuid.CHAR_MANUFACTURER_NAME_STRING),
                        BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattCharacteristic.PERMISSION_READ);
                // model number string char.
                BluetoothGattCharacteristic monsc = new BluetoothGattCharacteristic(
                        UUID.fromString(BleUuid.CHAR_MODEL_NUMBER_STRING),
                        BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattCharacteristic.PERMISSION_READ);
                // serial number string char.
                BluetoothGattCharacteristic snsc = new BluetoothGattCharacteristic(
                        UUID.fromString(BleUuid.CHAR_SERIAL_NUMBEAR_STRING),
                        BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattCharacteristic.PERMISSION_READ);
                dis.addCharacteristic(mansc);
                dis.addCharacteristic(monsc);
                dis.addCharacteristic(snsc);
                mGattServer.addService(dis);
            }

            if (mBTAdvertiser == null) {
                mBTAdvertiser = mBTAdapter.getBluetoothLeAdvertiser();
            }
            mBTAdvertiser.startAdvertising(createAdvSettings(), createAdvData(), mAdvCallback);
            setProgressBarIndeterminateVisibility(true);
        }
    }

    private void stopAdvertise() {
        if (mGattServer != null) {
            mGattServer.clearServices();
            mGattServer.close();
            mGattServer = null;
        }
        if (mBTAdvertiser != null) {
            mBTAdvertiser.stopAdvertising(mAdvCallback);
        }
        mIsAdvertising = false;
        setProgressBarIndeterminateVisibility(false);
    }

    private static AdvertisementData createAdvData() {
        //        // 某Beacon
        //        final byte[] manufacturerData = new byte[] {
        //                (byte) 0x4c, (byte) 0x00, (byte) 0x02, (byte) 0x15, // fix
        //                // proximity uuid 01020304-0506-0708-1112-131415161718
        //                (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, // uuid
        //                (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, // uuid
        //                (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, // uuid
        //                (byte) 0x15, (byte) 0x16, (byte) 0x17, (byte) 0x18, // uuid
        //                (byte) 0x01, (byte) 0x01, // major 257
        //                (byte) 0x02, (byte) 0x02, // minor 514
        //                (byte) 0xc5
        //                // Tx Power -59
        //        };
        AdvertisementData.Builder builder = new AdvertisementData.Builder();
        // TxPowerLevelの設定に応じてTxPowerをAdvに混ぜてくれる設定だと思うのですが
        // いまいち分かっていません。trueにすると最大31オクテットなサイズが減っちゃうので
        // 某BeaconなパケをAdvするためにはfalseにする必要があります。
        builder.setIncludeTxPowerLevel(false);
        // 1つ目の引数がmanufacturerIdって書いてあるんですがAndroidのscanRecordでは読み取れないため適当値です。
        // builder.setManufacturerData(0x1234578, manufacturerData);

        // Device InformationとImmediate AlertをAdvで喋らせます
        List<ParcelUuid> uuidList = new ArrayList<ParcelUuid>();
        uuidList.add(ParcelUuid.fromString(BleUuid.SERVICE_DEVICE_INFORMATION));
        uuidList.add(ParcelUuid.fromString(BleUuid.SERVICE_IMMEDIATE_ALERT));
        builder.setServiceUuids(uuidList);

        return builder.build();
    }

    private static AdvertiseSettings createAdvSettings() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        // Read/WriteさせるのでConnectableを指定します。
        builder.setType(AdvertiseSettings.ADVERTISE_TYPE_CONNECTABLE);
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        return builder.build();
    }
}*/