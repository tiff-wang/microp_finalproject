#include "main.h"
#include "stm32f4xx_hal.h"
#include "lis3dsh.h"
#include <math.h>

extern uint8_t status;
extern float Buffer[];
extern float accX,accY, accZ;
extern float accXX[], accYY[], accZZ[];
extern uint8_t MyFlag;
extern int windowSize;

void readACC(void){
	if (MyFlag/200){ //At 1000Hz --> true at every 0.2 sec
		// SEE: HAL_SYSTICK_Config(HAL_RCC_GetHCLKFreq()/1000);

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
			printf("X: %4f     Y: %4f     Z: %4f \n", accX, accY, accZ);
		}
	}
	//Sliding window implementation [Latest...PresentValue]
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