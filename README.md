# Micro Processors ECSE 426 - Final Project

##### Group 2 
Isaac Chan 
Jérôme Parent-Levesque 
Mathieu Savoie
Tiffany Wang 

#### Project description 

The goal of this project is to create a end-to-end connection between a STM32F4Discovery board and Google Firebase. 
The entire pipeline is: 
STM32F4Discovery -- (UART) --> Nucleo Board -- (BLE) --> Android -- (Wi-Fi) --> Firebase

The whole system is capable of doing two things. 
1. Record accelerometer data on the Discovery board and graph the pill and roll on Firebase
2. Record audio data and do a speed-to-text voice recognition on Firebase


###### Breakdown

**Discovery Board**
The project is written using Keil and MDK-ARM Microcontroller Development Kit http://www.keil.com/products/arm/mdk.asp
We used uVision IDE to develop and compile the project 

**Nucleo Board**
The project is written using Keil and MDK-ARM Microcontroller Development Kit as well. However, this part requires an additional RTE components to function. 

**Android** 
The android app is developed on Android Studio using SDK 23 on Android v8. 

**Firebase** 
The firebase functions are deployed using Node.JS. You can easily deploy new functions using the npm package 'firebase tools'
To run the project, please clone the repo and do 'npm install' to install the dependencies. 
The voice recognition is done using Google Speech API and the graph was done using Plotly. 

