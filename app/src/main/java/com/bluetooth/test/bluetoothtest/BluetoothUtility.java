package com.bluetooth.test.bluetoothtest;

import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bluetooth LE advertising and scanning utilities.
 *
 * Created by micah on 7/16/14.
 */
public class BluetoothUtility {

    /**
     * Contants
     */
    private static final int REQUEST_ENABLE_BT = 1;

    /**
     * String Constants
     */
    private static final String TAG = "MyActivity";
    private static final String BLUETOOTH_ADAPTER_NAME = "Zoku_Android_1";

    /**
     * Advertising + Scanning Constants
     */
    private boolean scanning;
    private boolean advertising;
    private AdvertiseCallback advertiseCallback; //Must implement and set
    private BluetoothGattServerCallback gattServerCallback; //Must implement and set
    private ScanCallback scanCallback; //Must implement and set
    private List<ParcelUuid> serviceUuids;

    /**
     * Bluetooth Objects
     */
    Activity activity;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothGattServer gattServer;
    private ArrayList<BluetoothGattService> advertisingServices;
    private BluetoothLeScanner bluetoothLeScanner;

    /**
     * Scanning Objects
     */


    //Initialize Utility objects
    BluetoothUtility(Activity a) {
        activity = a;
        scanning = false;
        advertising = false;

        bluetoothManager = (BluetoothManager) activity.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothAdapter.setName(BLUETOOTH_ADAPTER_NAME);
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        advertisingServices = new ArrayList<BluetoothGattService>();
        serviceUuids = new ArrayList<ParcelUuid>();
    }

    public void cleanUp() {
        if(getAdvertising()) stopAdvertise();
        if(getScanning()) stopBleScan();
        if(gattServer != null) gattServer.close();
    }

    //Check if bluetooth is enabled, if not, then request enable
    private void enableBluetooth() {
        if(bluetoothAdapter == null) {
            //bluetoothState.setText("Bluetooth NOT supported");
        } else if(!bluetoothAdapter.isEnabled()) {
            //bluetoothAdapter.enable();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /*-------------------------------------------------------------------------------*/

    public boolean getAdvertising() {
        return advertising;
    }

    public void setAdvertiseCallback(AdvertiseCallback callback) {
        advertiseCallback = callback;
    }
    public void setGattServerCallback(BluetoothGattServerCallback callback) {
        gattServerCallback = callback;
    }

    /**
     * BLE Advertising
     */
    //Public method to begin advertising services
    public void startAdvertise() {
        if(getAdvertising()) return;
        enableBluetooth();

        startGattServer();

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

        //UUID uuid = UUID.randomUUID();
        //ParcelUuid pUUID = new ParcelUuid(uuid); //test random uuid

        //parcelArray.add(pUUID);
        //parcelArray.add(ParcelUuid.fromString(SERVICE_UUID_1));
        //parcelArray.add(ParcelUuid.fromString(SERVICE_DEVICE_INFORMATION));

        //Log.d(TAG, "Generated UUID: " + uuid.toString());

        dataBuilder.setIncludeTxPowerLevel(false); //necessity to fit in 31 byte advertisement

        //dataBuilder.setManufacturerData(0, advertisingBytes);

        for (ParcelUuid pu: serviceUuids) {
            dataBuilder.addServiceUuid(pu);
        }
        
        //dataBuilder.setServiceData(pUUID, new byte[]{});

        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setConnectable(true);

        bluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), advertiseCallback);
        advertising = true;
    }

    //Stop ble advertising and clean up
    public void stopAdvertise() {
        if(!getAdvertising()) return;
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        gattServer.clearServices();
        gattServer.close();
        advertisingServices.clear();
        advertising = false;
    }

    public BluetoothGattServer getGattServer() {
        return gattServer;
    }

    private void startGattServer() {
        gattServer = bluetoothManager.openGattServer(activity, gattServerCallback);

        for(int i = 0; i < advertisingServices.size(); i++) {
            gattServer.addService(advertisingServices.get(i));
        }
    }

    public void addService(BluetoothGattService service) {
       advertisingServices.add(service);
       serviceUuids.add(new ParcelUuid(service.getUuid()));
    }

    /*-------------------------------------------------------------------------------*/

    public boolean getScanning() {
        //TODO check lescanning boolean
        return scanning;
    }

    public void setScanCallback(ScanCallback callback) {
        scanCallback = callback;
    }

    /**
     * BLE Scanning
     */
    public void startBleScan() {
        if(getScanning()) return;
        enableBluetooth();
        scanning = true;
        ScanFilter.Builder filterBuilder = new ScanFilter.Builder(); //TODO currently default, scans all devices
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(filterBuilder.build());
        bluetoothLeScanner.startScan(filters, settingsBuilder.build(), scanCallback);

        Log.d(TAG, "Bluetooth is currently scanning...");
    }

    public void stopBleScan() {
        if(!getScanning()) return;
        scanning = false;
        bluetoothLeScanner.stopScan(scanCallback);
        Log.d(TAG, "Scanning has been stopped");
    }
}
