/**
  ******************************************************************************
  * File Name          : main.c
  * Description        : Main program body
  ******************************************************************************
  ** This notice applies to any and all portions of this file
  * that are not between comment pairs USER CODE BEGIN and
  * USER CODE END. Other portions of this file, whether 
  * inserted by the user or by software development tools
  * are owned by their respective copyright owners.
  *
  * COPYRIGHT(c) 2018 STMicroelectronics
  *
  * Redistribution and use in source and binary forms, with or without modification,
  * are permitted provided that the following conditions are met:
  *   1. Redistributions of source code must retain the above copyright notice,
  *      this list of conditions and the following disclaimer.
  *   2. Redistributions in binary form must reproduce the above copyright notice,
  *      this list of conditions and the following disclaimer in the documentation
  *      and/or other materials provided with the distribution.
  *   3. Neither the name of STMicroelectronics nor the names of its contributors
  *      may be used to endorse or promote products derived from this software
  *      without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
  * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
  * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
  * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *
  ******************************************************************************
  */
/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "stm32f4xx_hal.h"
#include "lis3dsh.h"
#include <math.h>
#include <stdio.h>
/* Private variables ---------------------------------------------------------*/

LIS3DSH_InitTypeDef 		Acc_instance;
ADC_HandleTypeDef hadc1;

/* User Private variables ---------------------------------------------------------*/
	//ACC.
	uint8_t status;							// What does this status represent???
	float Buffer[3];						// Buffer[3] for what???
	float accX, accY, accZ;
	uint8_t MyFlag = 0;					// Flagging system used for??? --> gets updated in SysTick_Handler --> 50Hz???
	//General Logic
	int state = 0;
	//Tap detection
	int tap = 0;
	int accCounter = 0;									//Used for double tap detection
	int minThreshold = 150;							//Gap need to be determined --> counter uses HAL_GetTick()
	int maxThreshold = 1500;
	int boolFirstPass = 1; 							//Boolean serving to tell that first tap occured
	int previousTap = -1;								//Served to check when was the first tap was done
	int windowSize = 10;								//Length of window for recodring accelorometer
	float accXX[10] = {0.0};
	float accYY[10] = {0.0};
	float accZZ[10] = {0.0};
	
	//stabilized state
	int nbValues = 0;										//To ensure accXX, accYY, or accZZ has gotten ride of all the 0 it was initialized to
	int stable = 0;											//For accStable(); Return variable 
	int stableNb = 0;										//Served to get the number of times accStable() returns 1 in a row
	//MIC
	int firstPass = 1; //boolean for allowing 1 sec delay before reading mic. data
	int prevCNT = 0; 	 //Counter for allowing 1 sec delay before reading mic. data + use for timing the number return (N) for LED -BLINKY()
	float firADC = 0.0;
	float audioBuffer[10000] = {0};
	int audioIndex = 0 ;
	//int adcReadings =0;							//Use for testing purposes (print)
	int audioBool =0; 							//Boolean for audio reading (used in state 3)
	//Discovery board - pitch and roll
	float pitch[1000] = {0};
	float roll[1000] ={0};
	//Blinky
	int nbBlink = 0;
	
	

	

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
static void MX_GPIO_Init(void);
void initializeACC			(void);
static void MX_ADC1_Init(void);
float getPitch(float x, float y, float z);
float getRoll(float x, float y, float z);
	
void readACC(void);
int readTap(void);
int accStable(void);
//ADC??????????????????????????????????????
extern void ADC_IRQHandler(void);
//Blinky
void blinky(int N);

	
int main(void)
{


  HAL_Init();
  SystemClock_Config();
  MX_GPIO_Init();
	initializeACC	();	// Like any other peripheral, you need to initialize it. Refer to the its driver to learn more.
	MX_ADC1_Init();
	HAL_ADC_Start_IT(&hadc1);
	// and example of sending a data through UART, but you need to configure the UART block:
	// HAL_UART_Transmit(&huart2,"FinalProject\n",14,2000); 


  while (1)
  {
		switch(state) {
			case 0:
			{
				readACC();
				if(nbValues > 10){
					if(accStable()){
						stableNb ++;
					}
					//else restart
					else{
						/*
						tempMaxX = -INFINITY;
						tempMaxY = -INFINITY;
						tempMaxZ = -INFINITY;
						tempMinX = INFINITY;
						tempMinY = INFINITY;
						tempMinZ = INFINITY;
						*/
						stableNb =0;
					}
				nbValues = 0;
				}
				if(stableNb > 50){
					state =1;
					stableNb = 0;
				}
				break;
			}
			case 1:	
			{
				
				//Read acceleroemeter
				readACC();
				accCounter = HAL_GetTick();
			
				if(nbValues > 50){
					HAL_GPIO_WritePin(GPIOD, LD4_Pin, GPIO_PIN_SET);	//GREEN LED
					//Detect if single our double tap
					//either 1 or 2 tap was DETECTED
					if(previousTap != -1 && accCounter - previousTap > maxThreshold){
						accCounter = 0;
						previousTap = -1;
						HAL_GPIO_WritePin(GPIOD, LD4_Pin, GPIO_PIN_RESET);
						if (tap ==1){
							tap =0;
							nbValues = 0;
							state = 2;
						}
						else{
							tap =0;
							nbValues = 0;
							state = 5;
						}
					}
					if(readTap()){
						if(previousTap != -1 && accCounter - previousTap > minThreshold){
							//Detect Double tap
							tap =2;
						}
						else{
							//If here --> first tap detected
							//Start Timer
							previousTap = HAL_GetTick();
							tap =1;
						}
					}
				}
				
				break;
			}
			case 2:
			{
				//Set sampling frequency to higher than 8K samples/sec
				//Record Audio
				if (firstPass){
					firstPass = 0;
					audioBool = 1;				//use for ensuring the right data gets sent on state 3
					prevCNT = HAL_GetTick();
					HAL_GPIO_WritePin(GPIOD, LD3_Pin, GPIO_PIN_SET);	//ORANGE LED
				}
				
				//allow some delay (500ms)
				if(HAL_GetTick() - prevCNT > 500){
					//Set an interrupt or pullforconversion?
					ADC_IRQHandler();
					audioBuffer[audioIndex] = firADC;
					//printf("audio %f \n", audioBuffer[audioIndex]);
					audioIndex ++;
				}
				
				/*
				if(HAL_GetTick() - prevCNT > 1000){
					HAL_GPIO_WritePin(GPIOD, LD3_Pin, GPIO_PIN_SET);	//ORANGE LED
					HAL_ADC_Start(&hadc1); 
					
					//MUST PUT IN BYTE
					int i = -1;
					while(HAL_ADC_PollForConversion(&hadc1, 10000) == HAL_OK){
						i++;
						adcReadings = HAL_ADC_GetValue(&hadc1);
						audioBuffer[i] = adcReadings;
						printf("audio %i \n", audioBuffer[i]);
					}
					*/
				if(audioIndex > 10000){
					//When here --> done reading mic. data
					HAL_GPIO_WritePin(GPIOD, LD3_Pin, GPIO_PIN_RESET);
					firstPass = 1; //set it ready for next Pass
					audioIndex = 0;
					state = 3;
				}
				
				break;
			}
			case 3:
			{
				//Question: When sending data with Hal_UART... does code keep running or waits till all data is sent, then jumps to the nextl line?
				
				//Transfer data to Nucleo Board (dataToSend)
				//Data to sent: Audio(1s) OR pitch and roll(10s)
				if(audioBool){
					HAL_GPIO_WritePin(GPIOD, LD6_Pin, GPIO_PIN_SET); //BLUE LED
					// and example of sending a data through UART, but you need to configure the UART block:
					// HAL_UART_Transmit(&huart2,"FinalProject\n",14,2000);
				}
				else{
					HAL_GPIO_WritePin(GPIOD, LD6_Pin, GPIO_PIN_SET); //BLUE LED
					// and example of sending a data through UART, but you need to configure the UART block:
					// HAL_UART_Transmit(&huart2,"FinalProject\n",14,2000); 
				}
				
				//WHEN RECEIVE FINISH TRANSMITTING MESSAGE
				if (firstPass){
					firstPass = 0;
					prevCNT = HAL_GetTick();
				}
				if(HAL_GetTick() - prevCNT > 10000){
					if(audioBool){
						
						//Reset audioBool
						audioBool = 0; 
						HAL_GPIO_WritePin(GPIOD, LD6_Pin, GPIO_PIN_RESET); //BLUE LED
						state = 4;						
					}
					else{
						//Reset audioBool
						audioBool = 0; 
						HAL_GPIO_WritePin(GPIOD, LD6_Pin, GPIO_PIN_RESET); //BLUE LED
						state = 0;
					}
				}
				break;
			}
			case 4:
			{
				//Wait till receive data from Nucleo Board (N)
				//If(data.recceived)
				//int N = data.get;
				blinky(5);
				state = 0;
				//Blink LED2 N times -->blinky(N)
				break;
			}
			case 5:
			{
				//Set sampling frequency to 100Hz
				//accData = Read accelerometer for 10 seconds
				HAL_GPIO_WritePin(GPIOD, LD5_Pin, GPIO_PIN_SET);	//RED LED
				readACC();
				
				if(nbValues >= 1016){
					//send data --> go to state 3
					
					HAL_GPIO_WritePin(GPIOD, LD5_Pin, GPIO_PIN_RESET);	//RED LED
					printf("pitch[0]: %4f    roll[0]: %4f     nbValues: %i\n", pitch[0], roll[0], nbValues);
					//printf("pitch[12]: %4f    roll[12]: %4f     nbValues: %i\n", pitch[12], roll[12], nbValues);
					
					nbValues = 0;
					state = 3;
				}
				break;
			}
		}
	}
}

/** System Clock Configuration
*/
void SystemClock_Config(void)
{

  RCC_OscInitTypeDef RCC_OscInitStruct;
  RCC_ClkInitTypeDef RCC_ClkInitStruct;

    /**Configure the main internal regulator output voltage 
    */
  __HAL_RCC_PWR_CLK_ENABLE();

  __HAL_PWR_VOLTAGESCALING_CONFIG(PWR_REGULATOR_VOLTAGE_SCALE1);

    /**Initializes the CPU, AHB and APB busses clocks 
    */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSE;
  RCC_OscInitStruct.HSEState = RCC_HSE_ON;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;
  RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSE;
  RCC_OscInitStruct.PLL.PLLM = 8;
  RCC_OscInitStruct.PLL.PLLN = 336;
  RCC_OscInitStruct.PLL.PLLP = RCC_PLLP_DIV2;
  RCC_OscInitStruct.PLL.PLLQ = 7;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    _Error_Handler(__FILE__, __LINE__);
  }

    /**Initializes the CPU, AHB and APB busses clocks 
    */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV4;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV2;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_5) != HAL_OK)
  {
    _Error_Handler(__FILE__, __LINE__);
  }

    /**Configure the Systick interrupt time 
    */
  HAL_SYSTICK_Config(HAL_RCC_GetHCLKFreq()/1000);

    /**Configure the Systick 
    */
  HAL_SYSTICK_CLKSourceConfig(SYSTICK_CLKSOURCE_HCLK);

  /* SysTick_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(SysTick_IRQn, 0, 0);
}

/* ADC1 init function */
static void MX_ADC1_Init(void)
{

  ADC_ChannelConfTypeDef sConfig;

    /**Configure the global features of the ADC (Clock, Resolution, Data Alignment and number of conversion) 
    */
  hadc1.Instance = ADC1;
  hadc1.Init.ClockPrescaler = ADC_CLOCK_SYNC_PCLK_DIV4;
  hadc1.Init.Resolution = ADC_RESOLUTION_10B;
  hadc1.Init.ScanConvMode = DISABLE;
  hadc1.Init.ContinuousConvMode = ENABLE;
  hadc1.Init.DiscontinuousConvMode = DISABLE;
  hadc1.Init.ExternalTrigConvEdge = ADC_EXTERNALTRIGCONVEDGE_RISING;
  hadc1.Init.ExternalTrigConv = ADC_EXTERNALTRIGCONV_T2_TRGO;
  hadc1.Init.DataAlign = ADC_DATAALIGN_RIGHT;
  hadc1.Init.NbrOfConversion = 1;
  hadc1.Init.DMAContinuousRequests = DISABLE;
  hadc1.Init.EOCSelection = ADC_EOC_SINGLE_CONV;	
  if (HAL_ADC_Init(&hadc1) != HAL_OK)
  {
    _Error_Handler(__FILE__, __LINE__);
  }

    /**Configure for the selected ADC regular channel its corresponding rank in the sequencer and its sample time. 
    */
  sConfig.Channel = ADC_CHANNEL_1;
  sConfig.Rank = 1;
  sConfig.SamplingTime = ADC_SAMPLETIME_3CYCLES;
  if (HAL_ADC_ConfigChannel(&hadc1, &sConfig) != HAL_OK)
  {
    _Error_Handler(__FILE__, __LINE__);
  }

}



/** Configure pins as 
        * Analog 
        * Input 
        * Output
        * EVENT_OUT
        * EXTI
     PC3   ------> I2S2_SD
     PA4   ------> I2S3_WS
     PA5   ------> SPI1_SCK
     PA6   ------> SPI1_MISO
     PA7   ------> SPI1_MOSI
     PB10   ------> I2S2_CK
     PC7   ------> I2S3_MCK
     PA9   ------> USB_OTG_FS_VBUS
     PA10   ------> USB_OTG_FS_ID
     PA11   ------> USB_OTG_FS_DM
     PA12   ------> USB_OTG_FS_DP
     PC10   ------> I2S3_CK
     PC12   ------> I2S3_SD
     PB6   ------> I2C1_SCL
     PB9   ------> I2C1_SDA
*/
static void MX_GPIO_Init(void)
{

  GPIO_InitTypeDef GPIO_InitStruct;

  /* GPIO Ports Clock Enable */
  __HAL_RCC_GPIOE_CLK_ENABLE();
  __HAL_RCC_GPIOC_CLK_ENABLE();
  __HAL_RCC_GPIOH_CLK_ENABLE();
  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOB_CLK_ENABLE();
  __HAL_RCC_GPIOD_CLK_ENABLE();

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(CS_I2C_SPI_GPIO_Port, CS_I2C_SPI_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(OTG_FS_PowerSwitchOn_GPIO_Port, OTG_FS_PowerSwitchOn_Pin, GPIO_PIN_SET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOD, LD4_Pin|LD3_Pin|LD5_Pin|LD6_Pin 
                          |Audio_RST_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin : CS_I2C_SPI_Pin */
  GPIO_InitStruct.Pin = CS_I2C_SPI_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(CS_I2C_SPI_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pin : OTG_FS_PowerSwitchOn_Pin */
  GPIO_InitStruct.Pin = OTG_FS_PowerSwitchOn_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(OTG_FS_PowerSwitchOn_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pin : PDM_OUT_Pin */
  GPIO_InitStruct.Pin = PDM_OUT_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_AF_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  GPIO_InitStruct.Alternate = GPIO_AF5_SPI2;
  HAL_GPIO_Init(PDM_OUT_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pin : B1_Pin */
  GPIO_InitStruct.Pin = B1_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_IT_RISING;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(B1_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pin : I2S3_WS_Pin */
  GPIO_InitStruct.Pin = I2S3_WS_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_AF_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  GPIO_InitStruct.Alternate = GPIO_AF6_SPI3;
  HAL_GPIO_Init(I2S3_WS_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pins : SPI1_SCK_Pin SPI1_MISO_Pin SPI1_MOSI_Pin */
  GPIO_InitStruct.Pin = SPI1_SCK_Pin|SPI1_MISO_Pin|SPI1_MOSI_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_AF_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  GPIO_InitStruct.Alternate = GPIO_AF5_SPI1;
  HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);

  /*Configure GPIO pin : BOOT1_Pin */
  GPIO_InitStruct.Pin = BOOT1_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_INPUT;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(BOOT1_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pin : CLK_IN_Pin */
  GPIO_InitStruct.Pin = CLK_IN_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_AF_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  GPIO_InitStruct.Alternate = GPIO_AF5_SPI2;
  HAL_GPIO_Init(CLK_IN_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pins : LD4_Pin LD3_Pin LD5_Pin LD6_Pin 
                           Audio_RST_Pin */
  GPIO_InitStruct.Pin = LD4_Pin|LD3_Pin|LD5_Pin|LD6_Pin 
                          |Audio_RST_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOD, &GPIO_InitStruct);

  /*Configure GPIO pins : I2S3_MCK_Pin I2S3_SCK_Pin I2S3_SD_Pin */
  GPIO_InitStruct.Pin = I2S3_MCK_Pin|I2S3_SCK_Pin|I2S3_SD_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_AF_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  GPIO_InitStruct.Alternate = GPIO_AF6_SPI3;
  HAL_GPIO_Init(GPIOC, &GPIO_InitStruct);

  /*Configure GPIO pin : VBUS_FS_Pin */
  GPIO_InitStruct.Pin = VBUS_FS_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_INPUT;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(VBUS_FS_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pins : OTG_FS_ID_Pin OTG_FS_DM_Pin OTG_FS_DP_Pin */
  GPIO_InitStruct.Pin = OTG_FS_ID_Pin|OTG_FS_DM_Pin|OTG_FS_DP_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_AF_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  GPIO_InitStruct.Alternate = GPIO_AF10_OTG_FS;
  HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);

  /*Configure GPIO pin : OTG_FS_OverCurrent_Pin */
  GPIO_InitStruct.Pin = OTG_FS_OverCurrent_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_INPUT;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(OTG_FS_OverCurrent_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pins : Audio_SCL_Pin Audio_SDA_Pin */
  GPIO_InitStruct.Pin = Audio_SCL_Pin|Audio_SDA_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_AF_OD;
  GPIO_InitStruct.Pull = GPIO_PULLUP;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  GPIO_InitStruct.Alternate = GPIO_AF4_I2C1;
  HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);

  /*Configure GPIO pin : MEMS_INT2_Pin */
  GPIO_InitStruct.Pin = MEMS_INT2_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_EVT_RISING;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(MEMS_INT2_GPIO_Port, &GPIO_InitStruct);

}

/* USER CODE BEGIN 4 */



void initializeACC(void){
	
	Acc_instance.Axes_Enable				= LIS3DSH_XYZ_ENABLE;
	Acc_instance.AA_Filter_BW				= LIS3DSH_AA_BW_50;
	Acc_instance.Full_Scale					= LIS3DSH_FULLSCALE_2;
	Acc_instance.Power_Mode_Output_DataRate		= LIS3DSH_DATARATE_100;
	Acc_instance.Self_Test					= LIS3DSH_SELFTEST_NORMAL;
	Acc_instance.Continous_Update   = LIS3DSH_ContinousUpdate_Disabled;
	
	LIS3DSH_Init(&Acc_instance);	
	
	
	/* Enabling interrupt conflicts with push button
  ACC_Interrupt_Config.Dataready_Interrupt	= LIS3DSH_DATA_READY_INTERRUPT_ENABLED;
	ACC_Interrupt_Config.Interrupt_signal			= LIS3DSH_ACTIVE_HIGH_INTERRUPT_SIGNAL;
	ACC_Interrupt_Config.Interrupt_type				= LIS3DSH_INTERRUPT_REQUEST_PULSED;
	
	LIS3DSH_DataReadyInterruptConfig(&ACC_Interrupt_Config);
	*/
}

int accStable(void){
	float tempMaxX, tempMaxY, tempMaxZ = -INFINITY;
	float tempMinX, tempMinY, tempMinZ = INFINITY;
	stable = 0;
	for(int i=0; i<windowSize; i++){
		//update tempMin
		if(accXX[i] < tempMinX){
			tempMinX = accXX[i];
		}
		if(accYY[i] < tempMinY){
			tempMinY = accYY[i];
		}
		if(accZZ[i] < tempMinZ){
			tempMinZ = accZZ[i];
		}
		//update tempMax
		if(accXX[i] > tempMaxX){
			tempMaxX = accXX[i];
		}
		if(accYY[i] > tempMaxY){
			tempMaxY = accYY[i];
		}
		if(accZZ[i] > tempMaxZ){
			tempMaxZ = accZZ[i];
		}
	}
	if((fabs(tempMaxX) - fabs(tempMinX)) < 30 && (fabs(tempMaxY) - fabs(tempMinY)) < 30 && (fabs(tempMaxZ) - fabs(tempMinZ)) < 30){
		stable =1;
	}
	return stable;
}


int readTap(void){
	int tappy = 0;
	float tempMaxY, tempMaxX, tempMaxZ = -INFINITY;					//Used for readTap() for maximum and minumum evaluation
	float tempMinY, tempMinX, tempMinZ = INFINITY;
	for(int i=0; i<windowSize; i++){
		if(accYY[i] < tempMinY){
			tempMinY = accYY[i];
		}
		if(accYY[i] > tempMaxY){
			tempMaxY = accYY[i];
		}
		if(accXX[i] < tempMinX){
			tempMinX = accXX[i];
		}
		if(accXX[i] > tempMaxX){
			tempMaxX = accXX[i];
		}
		if(accZZ[i] < tempMinZ){
			tempMinZ = accZZ[i];
		}
		if(accZZ[i] > tempMaxZ){
			tempMaxZ = accZZ[i];
		}
	}
	if((fabs(tempMaxY) - fabs(tempMinY)) > 40 || (fabs(tempMaxX) -fabs(tempMinX) > 40) || (fabs(tempMaxZ) - fabs(tempMinZ) > 40)){
		tappy =1;
	
	}
	return tappy;
}

void blinky(int N){
	int counter = HAL_GetTick();
	int idx = 1;
	HAL_GPIO_WritePin(GPIOD, LD5_Pin, GPIO_PIN_SET); //GREEN LED
	while(N != 0){
		
		if(HAL_GetTick() - counter > 500){
			//Toggle LED
			HAL_GPIO_WritePin(GPIOD, LD5_Pin, (idx == 0)? GPIO_PIN_SET : GPIO_PIN_RESET); //GREEN LED
			idx ++;
			idx = idx%2;
			counter = HAL_GetTick();
			if(idx ==1){
				N--;
			}
		}
	}
	HAL_GPIO_WritePin(GPIOD, LD5_Pin, GPIO_PIN_RESET);
}
/* USER CODE END 4 */

/**
  * @brief  This function is executed in case of error occurrence.
  * @param  None
  * @retval None
  */
void _Error_Handler(char * file, int line)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */
  while(1) 
  {
  }
  /* USER CODE END Error_Handler_Debug */ 
}

#ifdef USE_FULL_ASSERT

/**
   * @brief Reports the name of the source file and the source line number
   * where the assert_param error has occurred.
   * @param file: pointer to the source file name
   * @param line: assert_param error line source number
   * @retval None
   */
void assert_failed(uint8_t* file, uint32_t line)
{
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
    ex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */

}

#endif

/**
  * @}
  */ 

/**
  * @}
*/ 

/************************ (C) COPYRIGHT STMicroelectronics *****END OF FILE****/
