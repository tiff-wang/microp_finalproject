#include "main.h"
#include "stm32f4xx_hal.h"
#include "lis3dsh.h"
#include <math.h>
#define M_PI 3.14159265358979323846

void readACC(void);
float getPitch(float x, float y, float z);
float getRoll(float x, float y, float z);


extern uint8_t status;
extern float Buffer[];
extern float accX,accY, accZ;
extern float accXX[], accYY[], accZZ[];
extern uint8_t MyFlag;
extern int windowSize;
extern int state;

//extern float tempMax, tempMin;
//extern int tappy;
extern int foundTap;
extern int nbValues;

float singlePitch = 0.0;
float singleRoll = 0.0;
extern float pitch[];
extern float roll[];

void readACC(void){
	if (MyFlag/10){ //At 1000Hz --> true at every 0.01 sec
		// SEE: HAL_SYSTICK_Config(HAL_RCC_GetHCLKFreq()/1000);
		
		nbValues++;

		MyFlag = 0;
		//Reading the accelerometer status register
		LIS3DSH_Read (&status, LIS3DSH_STATUS, 1);
							/*
							LIS3DSH_Read(uint8_t* pBuffer, uint8_t ReadAddr, uint16_t NumByteToRead)
							pBuffer : pointer to the buffer that receives the data read from the LIS3DSH.
							ReadAddr : LIS3DSH's internal address to read from.
							NumByteToRead : number of bytes to read from the LIS3DSH. --> 1byte = 4 bits
							*/
		
		//The first four bits denote if we have new data on all XYZ axes, 			---> how does this work: 4 bits for XYZ0, what is 0?
		//X axis only, Y axis only or Z axis only. If any or all changed, proceed
		if ((status & 0x0F) != 0x00){ //& --> AND gate

			LIS3DSH_ReadACC(&Buffer[0]); //Read LIS3DSH output register, and calculate the acceleration
																	 //ACC[mg]=SENSITIVITY* (out_h*256+out_l)/16 (12 bit rappresentation)
			accX = (float)Buffer[0];
			accY = (float)Buffer[1];
			accZ = (float)Buffer[2];
			/*
			if(tappy){
				printf("X: %4f     Y: %4f     Z: 	%4f 	foundTap: %i\n", accX, accY, accZ, foundTap);
			}
			*/
			//Sliding window implementation [Latest...PresentValue]
			if(state ==5){

				for(int i = 0; i < 1000; i++){
					pitch[i] = pitch[i+1];
					roll[i] = roll[i+1];
				}
				
				singlePitch = getPitch(accX, accY, accZ);
				pitch[999] = singlePitch;
				singleRoll = getRoll(accX, accY, accZ);
				roll[999] = singleRoll;
				printf("singlePitch: %4f    singleRoll: %4f     nbValues: %i\n", singlePitch, singleRoll, nbValues);
			}
			else{
				
				for(int i = 0; i <windowSize - 1; i++){
					accXX[i] = accXX[i+1];
					accYY[i] = accYY[i+1];
					accZZ[i] = accZZ[i+1];
				}
				//Store new fetched value
				accXX[windowSize-1] = accX;
				accYY[windowSize-1] = accY;
				accZZ[windowSize-1] = accZ;
			}
		}
	}
}
float getPitch(float x, float y, float z){
	float pitch = atan2(y,(sqrt(x*x + z*z)))* 180 / M_PI;
	return pitch;
}

float getRoll(float x, float y, float z){
	float roll = atan2(-x,z) * 180 / M_PI;
	return roll;
}

