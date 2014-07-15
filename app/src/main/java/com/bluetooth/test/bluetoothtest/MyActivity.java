package com.bluetooth.test.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
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


public class MyActivity extends Activity {

    private static final String TAG = "MyActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    final byte[] advertisingBytes = new byte[] { //TODO test data based on ibeacon demo
        (byte) 0x4c, (byte) 0x00,   // Apple manufacturer ID
        (byte) 0x02, (byte) 0x15,   // iBeacon advertisement identifier
        // 16-byte Proximity UUID follows
        (byte) 0x2F, (byte) 0x23, (byte) 0x44, (byte) 0x54, (byte) 0xCF, (byte) 0x6D, (byte) 0x4a, (byte) 0x0F,
        (byte) 0xAD, (byte) 0xF2, (byte) 0xF4, (byte) 0x91, (byte) 0x1B, (byte) 0xA9, (byte) 0xFF, (byte) 0xA6,
        (byte) 0x00, (byte) 0x01,   // major: 1
        (byte) 0x00, (byte) 0x02 }; // minor: 2

    final byte[] manufacturerData = new byte[] {
            (byte) 0x4c, (byte) 0x00, (byte) 0x02, (byte) 0x15, // fix
            //Proximity uuid 01020304-0506-0708-1112-131415161718
            (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, //​​uuid
            (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, //​​uuid
            (byte) 0x11, (byte) 0x12, (byte) 0x13, (byte) 0x14, //​​uuid
            (byte) 0x15, (byte) 0x16, (byte) 0x17, (byte) 0x18, //​​uuid
                        (byte) 0x01, (byte) 0x01, // major 257
                        (byte) 0x02, (byte) 0x02, // minor 514
                        (byte) 0xc5
                        //Tx Power -59
                        };

    final byte mLeManufacturerData[] = { (byte)0x67, (byte)0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07 };

    //BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeAdvertiser bluetoothLeAdvertiser;

    private Handler handler;
    private boolean scanning;
    private static final long SCAN_PERIOD = 10000;

    Button scanButton;
    TextView bluetoothState;
    ArrayAdapter<String> btArrayAdapter;
    ListView listDevicesFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        scanButton = (Button)findViewById(R.id.scan_button);
        bluetoothState = (TextView)findViewById(R.id.bluetooth_state_text);
        listDevicesFound = (ListView)findViewById(R.id.devicesfound);
        btArrayAdapter = new ArrayAdapter<String>(MyActivity.this, android.R.layout.simple_list_item_1);
        listDevicesFound.setAdapter(btArrayAdapter);

        initBluetooth();
        checkBluetooth();
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(StateChangeReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(ActionFoundReceiver);
    }

    private void checkBluetooth() {
        if(bluetoothAdapter == null) {
            bluetoothState.setText("Bluetooth NOT supported");
        } else {
            if(bluetoothAdapter.isEnabled()) {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothState.setText("Bluetooth is currently discovering...");
                } else {
                    bluetoothState.setText("Bluetooth is Enabled");
                    scanButton.setEnabled(true);
                }
            } else {
                bluetoothState.setText("Bluetooth is NOT Enabled");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void initBluetooth() {
        //bluetoothManager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        handler = new Handler();
    }

    public void LeScanStart(View view) {
        checkBluetooth();
        if(!bluetoothAdapter.isEnabled()) {
            return;
        }

        // Stops scanning after a pre-defined scan period.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scanning = false;
                bluetoothAdapter.stopLeScan(leScanCallback);
                scanButton.setEnabled(!scanning);
                checkBluetooth();
            }
        }, SCAN_PERIOD);

        btArrayAdapter.clear();
        scanning = true;
        bluetoothAdapter.startLeScan(leScanCallback);
        bluetoothState.setText("Bluetooth is currently scanning...");
        scanButton.setEnabled(!scanning);
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String deviceInfo = bluetoothDevice.getName() + " - " + bluetoothDevice.getAddress();
                    Log.d(TAG, "Device: " + deviceInfo + " Scanned!");
                    //TODO currently scan yields rapid repeats of same device when found
                    if(btArrayAdapter.getPosition(deviceInfo) == -1) {
                        btArrayAdapter.add(deviceInfo);
                        btArrayAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    };

    public void handleStartScanClick(View view) {
        btArrayAdapter.clear();
        bluetoothAdapter.startDiscovery();
        bluetoothState.setText("Bluetooth is currently discovering...");
        scanButton.setEnabled(false);
        //Toast.makeText(MyActivity.this, "Beginning Scan...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT) {
            checkBluetooth();
        }
    }

    private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                btArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private final BroadcastReceiver StateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
               checkBluetooth();
            }
        }
    };

    public void handleStartClick(View view) {
        //System.out.println("button press registered");
        //Toast.makeText(MyActivity.this, "You pressed it!", Toast.LENGTH_SHORT).show();
        //startAdvertise();
    }

    public void handleStopClick(View view) {
        stopAdvertise();
    }

    private void startAdvertise() {
        if(!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        AdvertisementData.Builder dataBuilder = new AdvertisementData.Builder();
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();


        UUID uuid = UUID.randomUUID();
        //ParcelUuid pUUID = new ParcelUuid(uuid); //test random uuid
        ParcelUuid pUUID = ParcelUuid.fromString("0000FFFE-0000-1000-8000-00805F9B34FB");
        List<ParcelUuid> parcelArray = new ArrayList<ParcelUuid>();
        parcelArray.add(pUUID);
        Log.d(TAG, "Generated UUID: " + uuid.toString());

        dataBuilder.setIncludeTxPowerLevel(false); //necessity to fit in 31 byte advertisement
        dataBuilder.setManufacturerData(0x0067, mLeManufacturerData);
        dataBuilder.setServiceUuids(parcelArray);
        dataBuilder.setServiceData(pUUID, new byte[]{});

        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setType(AdvertiseSettings.ADVERTISE_TYPE_CONNECTABLE);

        Toast.makeText(MyActivity.this, "Setup complete", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Configuration completed");
        bluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), startCallback);
    }

    private void stopAdvertise() {
        bluetoothLeAdvertiser.stopAdvertising(stopCallback);
    }

    private AdvertiseCallback startCallback = new AdvertiseCallback() {
        @Override
        public void onSuccess(AdvertiseSettings advertiseSettings) {
            String successMsg = "Advertisement attempt successful";
            Toast.makeText(MyActivity.this, successMsg, Toast.LENGTH_SHORT).show();
            Log.d(TAG, successMsg);
        }

        @Override
        public void onFailure(int i) {
            String failMsg = "Advertisement attempt failed";
            Toast.makeText(MyActivity.this, failMsg, Toast.LENGTH_SHORT).show();
            Log.d(TAG, failMsg);
        }
    };

    private AdvertiseCallback stopCallback = new AdvertiseCallback() {
        @Override
        public void onSuccess(AdvertiseSettings advertiseSettings) {
            String successMsg = "Stopping advertisement successful";
            Toast.makeText(MyActivity.this, successMsg, Toast.LENGTH_SHORT).show();
            Log.d(TAG, successMsg);
        }

        @Override
        public void onFailure(int i) {
            String failMsg = "Stopping advertisement failed";
            Toast.makeText(MyActivity.this, failMsg, Toast.LENGTH_SHORT).show();
            Log.d(TAG, failMsg);
        }
    };
}


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