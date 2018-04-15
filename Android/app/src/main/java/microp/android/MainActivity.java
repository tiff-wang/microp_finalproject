/* 

useful links: 
- test file url on firebase: 
https://firebasestorage.googleapis.com/v0/b/microp-g2.appspot.com/o/test-3.mp3?alt=media&token=3e551f22-a2a1-42af-870f-b745ae8cd570

*/

package microp.android;

import android.Manifest;
import android.app.Activity;
import java.util.Random;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
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
import java.util.LinkedList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import android.support.annotation.NonNull;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.*;
import java.io.*;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class MainActivity extends AppCompatActivity {
    FirebaseStorage storage = FirebaseStorage.getInstance();
    // Create a storage reference from our app
    StorageReference storageRef = storage.getReference();
    Random r = new Random();

    // Create file metadata including the content type

    private BluetoothAdapter bluetoothAdapter;
    private ScanCallback scanCallback;
    private BluetoothLeScannerCompat bleScannerCompat;
    private BluetoothGattCallback gattCallback;
    private boolean scanning = false;
    private Handler handler;
    private static int REQUEST_ENABLE_BT = 1;

    private static final String TAG = "ble";
    private static final String DEVICE_NAME = "IsaaNRG";
    private static final String DEVICE_ADDRESS = "03:80:E1:00:34:12";
    private static final UUID SERVICE_UUID = UUID.fromString("02366E80-CF3A-11E1-9AB4-0002A5D5C51B");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("340A1B80-CF4B-11E1-AC36-0002A5D5C51B");
    private static final UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final byte[] SEND_VOICE_CODE = { 40, 0, 41, 0, 42, 0, 43, 0, 44, 0, 45, 0, 46, 0, 47, 0, 48, 0, 49, 0 };
    private static final byte[] SEND_ACC_CODE = { 10, 0, 9, 0, 8, 0, 7, 0, 6, 0, 5, 0, 4, 0, 3, 0, 2, 0, 1, 0 };
    private static final byte[] STOP_CODE = { 100, 0, 99, 0, 98, 0, 97, 0, 96, 0, 95, 0, 94, 0, 93, 0, 92, 0, 91, 0 };
    private static final int SCAN_TIME = 10000;

    private LinkedList<Byte> receivedBytes = new LinkedList<>();

    private TextView textView;
    private TextView textView2;

    private enum CaptureType {
        None, // None means nothing is being recording at the moment.
        Voice,
        Acc
    }
    private CaptureType captureType = CaptureType.None;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Starting main activity");

        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.log);
        textView2 = findViewById(R.id.log2);

        handler = new Handler();

//        //upload Acc
//        try {// because it throws an exception
//            byte[][] test = new byte[3][20];
//            for(int i = 0 ; i < test.length ; i++){
//                for(int j = 0 ; j < test[0].length ; j++){
//                   test[i][j] = (byte)r.nextInt(8);
//                }
//            }
//            uploadAcc(test);
////            byte[] test_voice = SEND_VOICE_CODE;
////            uploadVoice(test_voice);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        // Make sure BluetoothLE is supported on the device
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Prompt for location permissions
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Location access not granted!");
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

        // onClickListener for the "Restart Scan" button
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
//                Log.i(TAG, "Scan callback called");

                if (result.getDevice().getAddress().equals(DEVICE_ADDRESS) && result.getDevice().getName().equals(DEVICE_NAME)) {
                    Log.i(TAG, "Device " + DEVICE_NAME + " found");
                    stopScan();
                    connectDevice(result.getDevice().getAddress());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.i(TAG, "Scan Failed with code " + errorCode);
            }
        };

        gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                switch(newState) {
                    case BluetoothGatt.STATE_CONNECTED:
                        // As soon as we are connected, discover services
                        gatt.discoverServices();
                        // When services are discovered, the 'onServicesDiscovered' callback defined below will be called
                        break;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.i(TAG, "Gatt callback: onServicesDiscovered");
                // As soon as services are discovered, acquire characteristic and try enabling

                BluetoothGattService accService = gatt.getService(SERVICE_UUID);
                BluetoothGattCharacteristic characteristic = accService.getCharacteristic(CHARACTERISTIC_UUID);
                if (characteristic != null) {
                    Log.i(TAG, "Characteristic found!");

                    gatt.setCharacteristicNotification(characteristic, true);
                    BluetoothGattDescriptor desc = characteristic.getDescriptor(DESCRIPTOR_UUID);
                    if (desc != null) {
                        Log.i(TAG, "Descriptor connected");
                        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); // The 'onCharacteristicChanged' callback will now be called every time the value is changed
                        boolean allGood = gatt.writeDescriptor(desc);
                        Log.i("ble", "Descriptor write: " + allGood); // Should print true

                        deviceConnected();
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic) {
                byte[] value = characteristic.getValue();
                Log.i(TAG, Arrays.toString(value)); // TODO remove

                // Check for the specific codes that specify the start or stop of a send sequence
                if (Arrays.equals(value, SEND_ACC_CODE)) {
                    Log.i(TAG, "Detected SEND_ACC_CODE");
                    updateDataStatus();
                    receivedBytes.clear();
                    captureType = CaptureType.Acc;
                } else if (Arrays.equals(value, SEND_VOICE_CODE)) {
                    Log.i(TAG, "Detected SEND_VOICE_CODE");
                    updateDataStatus();
                    receivedBytes.clear();
                    captureType = CaptureType.Voice;
                } else if (Arrays.equals(value, STOP_CODE)) {
                    Log.i(TAG, "Detected STOP_CODE");
                    updateDataStatus();
                    byte[] byteArray = new byte[receivedBytes.size()];
                    int i = 0;
                    for (Byte receivedByte: receivedBytes) byteArray[i++] = receivedByte; // convert to byte[]

                    try {
                        switch (captureType) {
                            case Voice:
                                uploadVoice(byteArray);
                                break;
                            case Acc:
                                byte[][] accArray = new byte[3][(int) Math.ceil(byteArray.length / 3.0)];
                                int j = 0;
                                int k = 0;
                                for (byte b : byteArray) { // Convert to 3 arrays of bytes (x, y, z)
                                    accArray[j++][k] = b;
                                    if (j >= 3) {
                                        j = 0;
                                        k++;
                                    }
                                }
                                uploadAcc(accArray);
                                break;
                        }
                    } catch(Exception e) {
                        Log.e(TAG, "Firebase upload error");
                    }

                    captureType = CaptureType.None; // We are done recording

                } else if (captureType != CaptureType.None) { // We are recording
                    for (byte b: value) receivedBytes.add(b); // Append all received bytes to the linked list
                }
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

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
    }

    private void startScan() {
        Log.i(TAG, "Starting scan");
        textView.setText("Scanning for device...");

        if (bleScannerCompat == null) {
            bleScannerCompat = BluetoothLeScannerCompat.getScanner();
        }

        // Scan for 10s only
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (scanning) {
                    bleScannerCompat.stopScan(scanCallback);
                    scanning = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("Scan stopped (Timeout)");
                        }
                    });
                }
            }
        }, SCAN_TIME);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setUseHardwareBatchingIfSupported(false).build();
        List<ScanFilter> filters = new ArrayList<>();
        bleScannerCompat.startScan(filters, settings, scanCallback);
        scanning = true;
    }

    private void stopScan() {
        Log.i(TAG, "Stop scanning");
        if (bleScannerCompat != null)
            bleScannerCompat.stopScan(scanCallback);
        scanning = false;
    }

    private void connectDevice(String address) {
        Log.i(TAG, "Connecting Device");
        textView.setText("Connecting device...");
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        device.connectGatt(this, true, gattCallback);
    }

    private void updateDataStatus() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (captureType) {
                    case None:
                        textView2.setText("Waiting for new data transmission to start");
                        break;
                    case Acc:
                        textView2.setText("Receiving accelerometer data");
                        break;
                    case Voice:
                        textView2.setText("Receiving voice data");
                }
            }
        });
    }

    private void deviceConnected() {
        Log.i(TAG, "Device connected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText("Device " + DEVICE_NAME + " successfully connected");
            }
        });
        updateDataStatus();
        stopScan(); // Stop looking for more devices
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

    void uploadVoice(byte[] voiceArray) throws IOException {
        StorageReference localRef = storageRef.child("voice.raw");
        // Now we need to use the UploadTask class to upload to our cloud
//        String s = "";
//        int counter = 0 ;
//        for(int i = 0 ; i < voiceArray.length / 2 ; i++){
//            s += Integer.toHexString(0x10000 | (voiceArray[i * 2 + 1] << 8) + voiceArray[i * 2]).substring(1) + " " ;
//            counter = (counter + 1) % 8;
//            if(counter == 0 ) s += "\n";
//        }

//        UploadTask uploadTask = localRef.putBytes(s.getBytes());
        UploadTask uploadTask = localRef.putBytes(voiceArray);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                System.out.println("Upload Failed");
                Log.i(TAG, "Upload to firebase failed");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>(){
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download Uri downloadUrl = taskSnapshot.getDownloadUrl();
                System.out.println("Upload Success");
                Log.i(TAG, "Upload to firebase was successful");
            }
        });
    }


    void uploadAcc(byte[][] acc) throws IOException {
        StorageReference localRef = storageRef.child("acc_graph.txt");
        StorageMetadata metadata = new StorageMetadata.Builder()
                           .setContentType("text/plain")
                           .build();
        // the text we will upload
        String stringAcc = toIntString(acc);
        //convert the text to bytes
        byte[] file = stringAcc.getBytes();
        // Now we need to use the UploadTask class to upload to our cloud
        UploadTask uploadTask = localRef.putBytes(file, metadata);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
        // Handle unsuccessful uploads
                System.out.println("Upload Failed");
                Log.i(TAG, "Upload to firebase failed");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>(){
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download Uri downloadUrl = taskSnapshot.getDownloadUrl();
                System.out.println("Upload Success");
                Log.i(TAG, "Upload to firebase was successful");
            }
        });
    }

    public static String toIntString(byte[][] array) {
        String result = "";
        for(byte[] voice: array){
            String line = "";
            for(int i = 0 ; i < voice.length - 1 ; i+=2){
                line += (voice[i + 1] << 8 + voice[i]) + " ";
            }
            result += line + "\n";
        }
        return result;
    }
}
