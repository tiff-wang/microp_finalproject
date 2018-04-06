# Instructions

**FOR UART TO WORK**: CONNECT PC TO GROUND!!

## Who to Contact
Jianing: Android, Farimah: FireBase, Nghia: Bluetooth/Nucleo board, Amir: rest

## Connections
Audio sample: 8ksample/sec
Accelerometer sample: 100sample/sec (pitch and roll, NOT x, y, z)
F4DiscBoad <-UART-> Nucleo board
F4DiscBoard <-SPI-> Accelerometer
Microphone --ADC--> F4DiscBoard

## Project Description
Tap board (detect Z axis accel.), then record mic for 1sec. Send data to android, then Firebase. In Firebase, perform signal processing to determine what number was said during that 1sec. Send back the result to the phone, then back to the Discovery Board, to finally blink an LED X times, where X is the number returned from Firebase.

### If we suck: THESE ARE THE NEW REQUIREMENTS
If sending data back is too hard, TA wants this: two taps on board sends Accel. data for 10s, but 1 tap sends 1sec of microphone

## Tutorial 1
SPI:
```
 Clk - Clk
MOSI - MOSI
MISO - MISO
  CS - CS
```
UART (not USART): See tutorial slides. 9600 bits/second. Code should be 1 line: `HAL_UART_TRANSMIT()`. 1 line: `HAL_UART_RECEIVE()`.
UART: Use any timeout (~4000ms)

ADC set up using timer as in lab 3 and 4 (to read at 8kHz in this case).

Accelerometer: To detect taps, read Z-axis, but **also** read other 2 axes to make sure they are **not** accelerating (to prevent side taps).

BLE: Bluetooth Low Energy

**Useful tools:** Teraterm, BLE Scanner (Android app)

## Tutorial 2

FinalPrj is sample code for Discovery board

### BLE
See Doc_23 on MyCourses. Also, Doc_20: "Quick Start Guide" for STM32 Nucleo.

Sample project for Nucleo board: BLE_SampleProject zip file on MyCourses. Need to add some driver (?). Let Amir know if any errors occur in this base project.

### Android App
Use Android Studio

- Must declare BLE permissions in the manifest file
    - BLUETOOTH
    - BLUETOOTH_ADMIN
- Initialize `BluetoothAdapter` and `BluetoothManager`
- Docs on developer.android.com
- Use UUID defined in Keil. Also in Keil, need to set up Read, Write, Notify.
- Seriously, just look at her tutorial, it is by far the best we had this semester -- **IT CONTAINS CODE!!**

### Firebase
firebase.google.com

Create new project (from the online web page)
Then Add Firebase to your android app by registering the app

- Under "Storage" (in web interface)
    - Under "Rules": Put always `true`
