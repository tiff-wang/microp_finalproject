# Instructions

## Who to Contact
Jianing: Android, Farimah: FireBase, Nghia: Bluetooth/Nucleo board, Amir: rest

## Connections
Audio sample: 8ksample/sec
Accelerometer sample: 100sample/sec (pitch and roll, NOT x, y, z)
F4DiscBoad <-UART-> Nucleo board
F4DiscBoard <-SPI-> Accelerometer
Microphone --ADC--> F4DiscBoard

## Project Description
Tap board (detect Z axis accel.), then record mic for 1sec. Send data to android, then Firebase. In Firebase, perform signal processing to determine what number was said during that 1sec. Send back the result to the phone, then back to the Discovery Board, to finally record all accel. data for X seconds, where X is the number returned from Firebase. This data is then finally sent back to Firebase for storing.

### If we suck
If sending data back is too hard, TA wants this: two taps on board sends Accel. data, but 1 tap sends 1sec of microphone

## Tutorial
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
