package com.bluetooth.test.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.bluetooth.le.*;
import android.widget.Toast;


public class MyActivity extends Activity {

    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
    }

    public void handleButtonClick(View view) {
        System.out.println("button press registered");
        Toast.makeText(MyActivity.this, "You pressed it!", Toast.LENGTH_SHORT).show();
    }

    private void bluetoothAdvertise() {
        BluetoothLeAdvertiser bluetoothLeAdvertiser = BluetoothLeAdvertiser.getBluetoothLeAdvertiser();
        AdvertisementData.Builder dataBuilder = new AdvertisementData.Builder();
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

        dataBuilder.setIncludeTxPowerLevel(false); //TODO possible necessity for nexus 5 advertising
        AdvertisementData data = dataBuilder.build();
        AdvertiseSettings settings = settingsBuilder.build();

        bluetoothLeAdvertiser.startAdvertising(settings, data, testAdvertiseCallback);
    }

    private AdvertiseCallback testAdvertiseCallback = new AdvertiseCallback() {

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
}
