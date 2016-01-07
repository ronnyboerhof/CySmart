/*
 * Copyright Cypress Semiconductor Corporation, 2014-2015 All rights reserved.
 *
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign),
 * United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate
 * license agreement between you and Cypress, this Software
 * must be treated like any other copyrighted material. Reproduction,
 * modification, translation, compilation, or representation of this
 * Software in any other form (e.g., paper, magnetic, optical, silicon)
 * is prohibited without Cypress's express written permission.
 *
 * Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * NONINFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE. Cypress reserves the right to make changes
 * to the Software without notice. Cypress does not assume any liability
 * arising out of the application or use of Software or any product or
 * circuit described in the Software. Cypress does not authorize its
 * products for use as critical components in any products where a
 * malfunction or failure may reasonably be expected to result in
 * significant injury or death ("High Risk Product"). By including
 * Cypress's product in a High Risk Product, the manufacturer of such
 * system or application assumes all risk of such use and in doing so
 * indemnifies Cypress against all liability.
 *
 * Use of this Software may be limited by and subject to the applicable
 * Cypress software license agreement.
 *
 *
 */

package com.cypress.cysmart.OTAFirmwareUpdate;

import android.bluetooth.BluetoothGattCharacteristic;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.DataModelClasses.OTAFlashRowModel;

/**
 * Separate class for handling the write operation during OTA firmware upgrade
 */
public class OTAFirmwareWrite {
    private BluetoothGattCharacteristic mOTACharacteristic;

    private int ONE = 1;
    private int TWO = 2;
    private int THREE = 3;
    private int FOUR = 4;
    private int FIVE = 5;
    private int SIX = 6;
    private int SEVEN = 7;
    private int EIGHT = 8;
    private int NINE = 9;
    private int TEN = 10;
    private int ELEVEN = 11;
    private int TWELVE = 12;
    private int FOURTEEN = 14;
    private int SIXTEEN = 16;


    public OTAFirmwareWrite(BluetoothGattCharacteristic writeCharacteristic) {
        this.mOTACharacteristic = writeCharacteristic;
    }

    /**
     * OTA Bootloader enter command method
     *
     * @param checkSumType
     */
    public void OTAEnterBootLoaderCmd(String checkSumType) {
        int startCommand = 0x01;
        int dataLength0 = 0x00;
        int dataLength1 = 0x00;

        byte[] commandBytes = new byte[7];
        commandBytes[0] = (byte) startCommand;
        commandBytes[1] = (byte) BootLoaderCommands.ENTER_BOOTLOADER;
        commandBytes[2] = (byte) dataLength0;
        commandBytes[3] = (byte) dataLength1;
        String checkSum = Integer.toHexString(BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, 16), 4, commandBytes));
        long checksum = Long.parseLong(checkSum, 16);
        commandBytes[4] = (byte) checksum;
        commandBytes[5] = (byte) (checksum >> 8);
        commandBytes[6] = (byte) BootLoaderCommands.PACKET_END;
        Logger.e("OTAEnterBootLoaderCmd");
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }

    /**
     * OTA Bootloader Get Flash Size Command
     */
    public void OTAGetFlashSizeCmd(byte[] data, String checkSumType, int dataLength) {
        byte[] commandBytes = new byte[BootLoaderCommands.BASE_CMD_SIZE + dataLength];
        int startCommand = 0x01;
        int dataLength1 = 0x00;
        commandBytes[0] = (byte) startCommand;
        commandBytes[1] = (byte) BootLoaderCommands.GET_FLASH_SIZE;
        commandBytes[2] = (byte) dataLength;
        commandBytes[3] = (byte) dataLength1;
        int dataByteLocationStart = 4;
        int datByteLocationEnd;
        for (int count = 0; count < dataLength; count++) {
            commandBytes[dataByteLocationStart] = data[count];
            dataByteLocationStart++;
        }
        datByteLocationEnd = dataByteLocationStart;
        String checkSum = Integer.toHexString(BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, 16), 4, commandBytes));
        long checksum = Long.parseLong(checkSum, 16);
        commandBytes[datByteLocationEnd] = (byte) checksum;
        commandBytes[datByteLocationEnd + 1] = (byte) (checksum >> 8);
        commandBytes[datByteLocationEnd + 2] = (byte) BootLoaderCommands.PACKET_END;
        Logger.e("OTAGetFlashSizeCmd");
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }

    public void OTAProgramRowSendDataCmd(byte[] data,
                                         String checksumType) {
        int totalSize = BootLoaderCommands.BASE_CMD_SIZE +
                data.length;
        int checksum;
        int i;
        byte[] commandBytes = new byte[totalSize];
        int startCommand = 0x01;

        commandBytes[0] = (byte) startCommand;
        commandBytes[1] = (byte) BootLoaderCommands.SEND_DATA;
        commandBytes[2] = (byte) (data.length);
        commandBytes[3] = (byte) ((int) ((data.length) >> 8));
        for (i = 0; i < data.length; i++)
            commandBytes[i + 4] = data[i];
        checksum = BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checksumType, 16),
                data.length + 4, commandBytes);
        commandBytes[totalSize - 3] = (byte) checksum;
        commandBytes[totalSize - 2] = (byte) (checksum >> 8);
        commandBytes[totalSize - 1] = (byte) BootLoaderCommands.PACKET_END;
        Logger.e("OTAProgramRowSendDataCmd Send size--->" + commandBytes.length);
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }


    /*
    *
    * OTA Bootloader Program row Command
    * */
    public void OTAProgramRowCmd(long rowMSB, long rowLSB, int arrayID, byte[] data,
                                 String checkSumType) {

        int COMMAND_DATA_SIZE = 3;
        int totalSize = BootLoaderCommands.BASE_CMD_SIZE + COMMAND_DATA_SIZE +
                data.length;
        int checksum;
        int i;
        byte[] commandBytes = new byte[totalSize];
        int startCommand = 0x01;

        commandBytes[0] = (byte) startCommand;
        commandBytes[1] = (byte) BootLoaderCommands.PROGRAM_ROW;
        commandBytes[2] = (byte) (data.length + COMMAND_DATA_SIZE);
        commandBytes[3] = (byte) ((int) ((data.length + COMMAND_DATA_SIZE) >> 8));
        commandBytes[4] = (byte) arrayID;
        commandBytes[5] = (byte) rowMSB;
        commandBytes[6] = (byte) rowLSB;
        for (i = 0; i < data.length; i++)
            commandBytes[i + 7] = data[i];
        checksum = BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, 16),
                data.length + 7, commandBytes);
        commandBytes[totalSize - 3] = (byte) checksum;
        commandBytes[totalSize - 2] = (byte) (checksum >> 8);
        commandBytes[totalSize - 1] = (byte) BootLoaderCommands.PACKET_END;
        Logger.e("OTAProgramRowCmd send size--->" + commandBytes.length);
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }

    /*
   *
   * OTA Bootloader Verify row Command
   * */
    public void OTAVerifyRowCmd(long rowMSB, long rowLSB, OTAFlashRowModel model,
                                String checkSumType) {
        int COMMAND_DATA_SIZE = 3;
        int COMMAND_SIZE = BootLoaderCommands.BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        int checksum;
        byte[] commandBytes = new byte[COMMAND_SIZE];
        int startCommand = 0x01;

        commandBytes[0] = (byte) startCommand;
        commandBytes[1] = (byte) BootLoaderCommands.VERIFY_ROW;
        commandBytes[2] = (byte) (COMMAND_DATA_SIZE);
        commandBytes[3] = (byte) (COMMAND_DATA_SIZE >> 8);
        commandBytes[4] = (byte) model.mArrayId;
        commandBytes[5] = (byte) rowMSB;
        commandBytes[6] = (byte) rowLSB;
        checksum = BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, 16),
                COMMAND_SIZE - 3, commandBytes);
        commandBytes[7] = (byte) checksum;
        commandBytes[8] = (byte) (checksum >> 8);
        commandBytes[9] = (byte) BootLoaderCommands.PACKET_END;
        Logger.e("OTAVerifyRowCmd");
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }

    /*
   *
   * OTA Verify CheckSum Command
   * */
    public void OTAVerifyCheckSumCmd(String checkSumType) {

        int checksum;
        byte[] commandBytes = new byte[BootLoaderCommands.BASE_CMD_SIZE];
        int startCommand = 0x01;

        commandBytes[0] = (byte) startCommand;
        commandBytes[1] = (byte) BootLoaderCommands.VERIFY_CHECK_SUM;
        commandBytes[2] = (byte) (0);
        commandBytes[3] = (byte) (0);
        checksum = BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, 16),
                BootLoaderCommands.BASE_CMD_SIZE - 3, commandBytes);
        commandBytes[4] = (byte) checksum;
        commandBytes[5] = (byte) (checksum >> 8);
        commandBytes[6] = (byte) BootLoaderCommands.PACKET_END;
        Logger.e("OTAVerifyCheckSumCmd");
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes);
    }

    /*
     *
     * Exit BootloaderCommand
     *
     * */
    public void OTAExitBootloaderCmd(String checkSumType) {

        int COMMAND_DATA_SIZE = 0x00;
        int COMMAND_SIZE = BootLoaderCommands.BASE_CMD_SIZE + COMMAND_DATA_SIZE;
        int checksum;
        byte[] commandBytes = new byte[BootLoaderCommands.BASE_CMD_SIZE];
        int startCommand = 0x01;

        commandBytes[0] = (byte) startCommand;
        commandBytes[1] = (byte) BootLoaderCommands.EXIT_BOOTLOADER;
        commandBytes[2] = (byte) (COMMAND_DATA_SIZE);
        commandBytes[3] = (byte) (COMMAND_DATA_SIZE >> 8);
        checksum = BootLoaderUtils.calculateCheckSum2(Integer.parseInt(checkSumType, 16),
                COMMAND_SIZE - 3, commandBytes);
        commandBytes[4] = (byte) checksum;
        commandBytes[5] = (byte) (checksum >> 8);
        commandBytes[6] = (byte) BootLoaderCommands.PACKET_END;
        Logger.e("OTAExitBootloaderCmd");
        BluetoothLeService.writeOTABootLoaderCommand(mOTACharacteristic, commandBytes, true);
    }

}
