package microp.android;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
//import android.bluetooth.le.ScanCallback;
//import android.bluetooth.le.ScanFilter;
//import android.bluetooth.le.ScanResult;
//import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.UUID;

import android.support.annotation.NonNull;
import android.net.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.*;
import java.io.*;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class MainActivity extends AppCompatActivity {
    FirebaseStorage storage = FirebaseStorage.getInstance();
    // Create a storage reference from our app
    StorageReference storageRef = storage.getReference();
    StorageReference ourFileRef = storageRef.child("boardData/testfile.mp3");
    // Create file metadata including the content type
    StorageMetadata metadata = new StorageMetadata.Builder()
            .setContentType("audio/mpeg")
            .build();

    private BluetoothAdapter bluetoothAdapter;
    private ScanCallback scanCallback;
    private BluetoothLeScanner bleScanner;
    private BluetoothLeScannerCompat bleScannerCompat;
    private BluetoothGatt gatt;
    private BluetoothGattCallback gattCallback;
    private static int REQUEST_ENABLE_BT = 1;

    private TextView textView;
    // textView.setText("The new text that I'd like to display now that the user has pushed a button.");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("ble", "Starting main activity");
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.log);

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Prompt for permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i("ble", "Location access not granted!");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
            }
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // If Bluetooth is not enabled on the user's phone, prompt him/her to enable it. Otherwise, stop the application.
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        final Button button = findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScan();
                startScan();
            }
        });

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
//                Log.i("ble", "Scan callback called");

                if (result.getDevice().getAddress().equals("03:80:E1:00:34:12") && result.getDevice().getName().equals("IsaaNRG")) {
                    Log.i("ble", "Device \"IsaaNRG\" found");
                    textView.setText("Device \"IsaaNRG\" found");
                    connectDevice(result.getDevice().getAddress());
                }
                // if you implement a RecyclerView inside your fragment or Activity for scanning, write an addDevice method in its corresponding Adapter class and call that method as following. Otherwise no need to include this statement.
//                mRecyclerViewAdapter.addDevice(result.getDevice().getAddress(), result.getDevice().getName());
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                Log.i("ble", "Batch scan callback called");
                Log.i("ble", Integer.toString(results.size()));
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.i("ble", "Scan Failed");
                Log.i("ble", Integer.toString(errorCode));
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
                Log.i("ble", "Gatt callback: onServicesDiscovered");
                // As soon as services are discovered, acquire characteristic and try enabling

                // TODO Do we need to change these UUIDs for our needs?? What do they do?
                BluetoothGattService movService = gatt.getService(UUID.fromString("02366E80-CF3A-11E1-9AB4-0002A5D5C51B"));
                BluetoothGattCharacteristic characteristic = movService.getCharacteristic(UUID.fromString("340A1B80-CF4B-11E1-AC36-0002A5D5C51B"));
                if (characteristic != null) {
                    Log.i("ble", "Characteristic found!");

                    gatt.setCharacteristicNotification(characteristic, true);
//                    characteristic.getDescriptors();
                    BluetoothGattDescriptor desc = characteristic.getDescriptor(characteristic.getDescriptors().get(0).getUuid());
                    Log.i("ble", "Descriptor is " + desc); // this is not null
                    desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    Log.i("ble", "Descriptor write: " + gatt.writeDescriptor(desc)); // returns true

                    deviceConnected();
                    gatt.readCharacteristic(characteristic);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);

//                Log.i("ble", "onCharacteristicRead");

//                Log.i("ble", Arrays.toString(characteristic.getValue()));
                // TODO we received data, handle it

//                gatt.readCharacteristic(characteristic); TODO just removed this, does it still work?
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic) {
                Log.i("ble", Arrays.toString(characteristic.getValue()));
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
        Log.i("ble", "Starting scan");
        textView.setText("Scanning for device");

        if (bleScannerCompat == null) {
//            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            bleScannerCompat = BluetoothLeScannerCompat.getScanner();
        }

        // Start the scan in low latency mode

//        bleScanner.startScan(new ArrayList<ScanFilter>(), new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback);
        bleScannerCompat.startScan(scanCallback);
    }

    private void stopScan() {
        Log.i("ble", "Stop scanning");
        if (bleScannerCompat != null)
            bleScannerCompat.stopScan(scanCallback);
    }

    private void connectDevice(String address) {
        Log.i("ble", "Connecting Device");
        textView.setText("Device Connected");
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        gatt = device.connectGatt(this, false, gattCallback);
    }

    private void deviceConnected() {
        Log.i("ble", "Device connected");
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
