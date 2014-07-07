package com.bluetooth.test.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.bluetooth.le.*;
import android.widget.Toast;

import java.util.UUID;


public class MyActivity extends Activity {

    private static final String TAG = "MyActivity";
    byte[] advertisingBytes = new byte[] { //TODO test data based on ibeacon demo
        (byte) 0x4c, (byte) 0x00,   // Apple manufacturer ID
        (byte) 0x02, (byte) 0x15,   // iBeacon advertisement identifier
        // 16-byte Proximity UUID follows
        (byte) 0x2F, (byte) 0x23, (byte) 0x44, (byte) 0x54, (byte) 0xCF, (byte) 0x6D, (byte) 0x4a, (byte) 0x0F,
        (byte) 0xAD, (byte) 0xF2, (byte) 0xF4, (byte) 0x91, (byte) 0x1B, (byte) 0xA9, (byte) 0xFF, (byte) 0xA6,
        (byte) 0x00, (byte) 0x01,   // major: 1
        (byte) 0x00, (byte) 0x02 }; // minor: 2

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;

    BluetoothLeAdvertiser bluetoothLeAdvertiser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        initBluetooth();
    }

    private void initBluetooth() {
        bluetoothManager = (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
    }

    public void handleStartClick(View view) {
        //System.out.println("button press registered");
        //Toast.makeText(MyActivity.this, "You pressed it!", Toast.LENGTH_SHORT).show();
        startAdvertise();
    }

    public void handleStopClick(View view) {
        stopAdvertise();
    }

    private void startAdvertise() {
        AdvertisementData.Builder dataBuilder = new AdvertisementData.Builder();
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

        dataBuilder.setManufacturerData((int) 0x01, advertisingBytes);
        UUID uuid = UUID.randomUUID();
        ParcelUuid pUUID = new ParcelUuid(uuid); //test random uuid
        Log.d(TAG, "Generated UUID: " + uuid.toString());
        dataBuilder.setServiceData(pUUID, new byte[]{});
        dataBuilder.setIncludeTxPowerLevel(false); //TODO possible necessity for nexus 5 advertising

        Toast.makeText(MyActivity.this, "setup complete", Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Configuration completed");
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
