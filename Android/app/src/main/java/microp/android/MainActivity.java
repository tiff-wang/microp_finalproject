/* 

useful links: 
- test file url on firebase: 
https://firebasestorage.googleapis.com/v0/b/microp-g2.appspot.com/o/test-3.mp3?alt=media&token=3e551f22-a2a1-42af-870f-b745ae8cd570

*/

package microp.android;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import android.support.annotation.NonNull;
import android.net.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.*;
import java.io.*;

public class MainActivity extends AppCompatActivity {
    FirebaseStorage storage = FirebaseStorage.getInstance();
    // Create a storage reference from our app
    StorageReference storageRef = storage.getReference();
    StorageReference ourFileRef = storageRef.child("boardData/testfile.wav");
    // Create file metadata including the content type
    StorageMetadata metadata = new StorageMetadata.Builder()
            .setContentType("audio/x-wav")
            .build();

    private BluetoothAdapter bluetoothAdapter;
    private ScanCallback scanCallback;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt gatt;
    private BluetoothGattCallback gattCallback;
    private static int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("ble", "Starting main activity");
        setContentView(R.layout.activity_main);

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // If Bluetooth is not enabled on the user's phone, prompt him/her to enable it. Otherwise, stop the application.
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.i("ble", "Scan callback called");

                connectDevice(result.getDevice().getAddress());
                // if you implement a RecyclerView inside your fragment or Activity for scanning, write an addDevice method in its corresponding Adapter class and call that method as following. Otherwise no need to include this statement.
//                mRecyclerViewAdapter.addDevice(result.getDevice().getAddress(), result.getDevice().getName());
            }
        };

        final Activity activity = this;

        gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                switch(newState) {
                    case BluetoothGatt.STATE_CONNECTED:
                        // As soon as we are connected, discover services
                        gatt.discoverServices();
                        // When services are discovered, the 'onServicesDiscovered' callback defined below will be called
                        Log.i("ble", "onConnectionsStateChange");
                        break;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                // As soon as services are discovered, acquire characteristic and try enabling

                // TODO Do we need to change these UUIDs for our needs?? What do they do?
                BluetoothGattService movService = gatt.getService(UUID.fromString("02366E80-CF3A-11E1-9AB4-0002A5D5C51B"));
                BluetoothGattCharacteristic characteristic = movService.getCharacteristic(UUID.fromString("40A1B80-CF4B-11E1-AC36-0002A5D5C51B"));
                if (characteristic == null) {
                    Toast.makeText(activity, R.string.service_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                }

                gatt.readCharacteristic(characteristic);
                deviceConnected();
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);

                Log.i("ble", "onCharacteristicRead");

                Log.i("ble", Arrays.toString(characteristic.getValue()));
                // TODO we received data, handle it
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        startScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
    }

    private void startScan() {
        Log.i("ble", "Start scan");

        if (bleScanner == null)
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();

        // Start the scan in low latency mode
        bleScanner.startScan(new ArrayList<ScanFilter>(), new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback);
    }

    private void stopScan() {
        bleScanner.stopScan(scanCallback);
    }

    private void connectDevice(String address) {
        Log.i("ble", "connectDevice");
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        gatt = device.connectGatt(this, false, gattCallback);
    }

    private void deviceConnected() {
        Log.i("ble", "Device connected");
        // TODO
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // An intent is started if the user's phone supports Bluetooth, but it is not enabled.
        if (requestCode == REQUEST_ENABLE_BT) {
            // If the result is negative (The user did not change his settings to enable Bluetooth) then stop the application
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, R.string.ble_disabled, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    void uploadResource() throws IOException {
// the text we will upload
        String someTextToUpload = "Hellghggfhgfghfgfho world! \nthis is to test uploading data";
//convert the text to bytes
        byte[] file = someTextToUpload.getBytes();
// Now we need to use the UploadTask class to upload to our cloud
        UploadTask uploadTask = ourFileRef.putBytes(file, metadata);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
// Handle unsuccessful uploads
                System.out.println("Failed");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>(){
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
// taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download Uri downloadUrl = taskSnapshot.getDownloadUrl();
                System.out.println("Success");
            }
        });
    }
}
