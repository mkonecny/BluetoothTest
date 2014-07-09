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


public class MyActivity extends Activity {

    private static final String TAG = "MyActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    byte[] advertisingBytes = new byte[] { //TODO test data based on ibeacon demo
        (byte) 0x4c, (byte) 0x00,   // Apple manufacturer ID
        (byte) 0x02, (byte) 0x15,   // iBeacon advertisement identifier
        // 16-byte Proximity UUID follows
        (byte) 0x2F, (byte) 0x23, (byte) 0x44, (byte) 0x54, (byte) 0xCF, (byte) 0x6D, (byte) 0x4a, (byte) 0x0F,
        (byte) 0xAD, (byte) 0xF2, (byte) 0xF4, (byte) 0x91, (byte) 0x1B, (byte) 0xA9, (byte) 0xFF, (byte) 0xA6,
        (byte) 0x00, (byte) 0x01,   // major: 1
        (byte) 0x00, (byte) 0x02 }; // minor: 2

    //BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeAdvertiser bluetoothLeAdvertiser;

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
    }

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
        startAdvertise();
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

        dataBuilder.setManufacturerData((int) 0x01, advertisingBytes);
        UUID uuid = UUID.randomUUID();
        ParcelUuid pUUID = new ParcelUuid(uuid); //test random uuid
        Log.d(TAG, "Generated UUID: " + uuid.toString());
        dataBuilder.setServiceData(pUUID, new byte[]{});
        dataBuilder.setIncludeTxPowerLevel(false); //TODO possible necessity for nexus 5 advertising

        Toast.makeText(MyActivity.this, "setup complete", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Configuration completed");
        //bluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), startCallback);
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
