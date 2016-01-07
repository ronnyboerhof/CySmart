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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;

/**
 * Receiver class for OTA response
 */
public class OTAResponseReceiver extends BroadcastReceiver {

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        this.mContext = context;
        /**
         * Condition to execute the next command to execute
         * Checks the Shared preferences for the currently executing command
         */
        if (BluetoothLeService.ACTION_OTA_DATA_AVAILABLE.equals(action)) {
            byte[] responseArray = intent
                    .getByteArrayExtra(Constants.EXTRA_BYTE_VALUE);
            String hexValue = Utils.ByteArraytoHex(responseArray);
            if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.ENTER_BOOTLOADER)) {
                parseEnterBootLoaderAcknowledgement(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.GET_FLASH_SIZE)) {
                parseGetFlashSizeAcknowledgement(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.SEND_DATA)) {
                parseParseSendDataAcknowledgement(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.PROGRAM_ROW)) {
                parseParseRowAcknowledgement(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.VERIFY_ROW)) {
                parseVerifyRowAcknowledgement(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.VERIFY_CHECK_SUM)) {
                parseVerifyCheckSum(hexValue);
            } else if ((Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE))
                    .equalsIgnoreCase("" + BootLoaderCommands.EXIT_BOOTLOADER)) {
                Logger.e("In Receiver Exit " + Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE));
                parseExitBootloader(hexValue);
            } else {
                Logger.i("In Receiver No case " + Utils.getStringSharedPreference(mContext, Constants.PREF_BOOTLOADER_STATE));
            }
        }
    }

    private void parseParseSendDataAcknowledgement(String hexValue) {
        String result = hexValue.trim().replace(" ", "");
        String response = result.substring(2, 4);
        String status = result.substring(4, 6);
        int reponseBytes = Integer.parseInt(response, 16);
        switch (reponseBytes) {
            case 0:
                Logger.i("CYRET_SUCCESS");
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_SEND_DATA_ROW_STATUS,
                        status);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            case 1:
                Logger.i("CYRET_ERR_FILE");
                broadCastErrorMessage("CYRET_ERR_FILE");
                break;
            case 2:
                Logger.i("CYRET_ERR_EOF");
                broadCastErrorMessage("CYRET_ERR_EOF");
                break;
            case 3:
                Logger.i("CYRET_ERR_LENGTH");
                broadCastErrorMessage("CYRET_ERR_LENGTH");
                break;
            case 4:
                Logger.i("CYRET_ERR_DATA");
                broadCastErrorMessage("CYRET_ERR_DATA");
                break;
            case 5:
                Logger.i("CYRET_ERR_CMD");
                broadCastErrorMessage("CYRET_ERR_CMD");
                break;
            case 6:
                Logger.i("CYRET_ERR_DEVICE");
                broadCastErrorMessage("CYRET_ERR_DEVICE");
                break;
            case 7:
                Logger.i("CYRET_ERR_VERSION");
                broadCastErrorMessage("CYRET_ERR_VERSION");
                break;
            case 8:
                Logger.i("CYRET_ERR_CHECKSUM");
                broadCastErrorMessage("CYRET_ERR_CHECKSUM");
                break;
            case 9:
                Logger.i("CYRET_ERR_ARRAY");
                broadCastErrorMessage("CYRET_ERR_ARRAY");
                break;
            case 10:
                Logger.i("CYRET_ERR_ROW");
                broadCastErrorMessage("CYRET_ERR_ROW");
                break;
            case 11:
                Logger.i("CYRET_BTLDR");
                broadCastErrorMessage("CYRET_BTLDR");
                break;
            case 12:
                Logger.i("CYRET_ERR_APP");
                broadCastErrorMessage("CYRET_ERR_APP");
                break;
            case 13:
                Logger.i("CYRET_ERR_ACTIVE");
                broadCastErrorMessage("CYRET_ERR_ACTIVE");
                break;
            case 14:
                Logger.i("CYRET_ERR_UNK");
                broadCastErrorMessage("CYRET_ERR_UNK");
                break;
            case 15:
                Logger.i("CYRET_ABORT");
                broadCastErrorMessage("CYRET_ABORT");
                break;
            default:
                Logger.i("CYRET DEFAULT");
                break;
        }
    }

    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseEnterBootLoaderAcknowledgement(String parse) {
        String result = parse.trim().replace(" ", "");
        String response = result.substring(2, 4);
        String siliconID = result.substring(8, 16);
        String siliconRev = result.substring(16, 18);
        int reponseBytes = Integer.parseInt(response, 16);
        switch (reponseBytes) {
            case 0:
                Logger.i("CYRET_SUCCESS");
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_SILICON_ID,
                        siliconID);
                mBundle.putString(Constants.EXTRA_SILICON_REV, siliconRev);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            case 1:
                Logger.i("CYRET_ERR_FILE");
                broadCastErrorMessage("CYRET_ERR_FILE");
                break;
            case 2:
                Logger.i("CYRET_ERR_EOF");
                broadCastErrorMessage("CYRET_ERR_EOF");
                break;
            case 3:
                Logger.i("CYRET_ERR_LENGTH");
                broadCastErrorMessage("CYRET_ERR_LENGTH");
                break;
            case 4:
                Logger.i("CYRET_ERR_DATA");
                broadCastErrorMessage("CYRET_ERR_DATA");
                break;
            case 5:
                Logger.i("CYRET_ERR_CMD");
                broadCastErrorMessage("CYRET_ERR_CMD");
                break;
            case 6:
                Logger.i("CYRET_ERR_DEVICE");
                broadCastErrorMessage("CYRET_ERR_DEVICE");
                break;
            case 7:
                Logger.i("CYRET_ERR_VERSION");
                broadCastErrorMessage("CYRET_ERR_VERSION");
                break;
            case 8:
                Logger.i("CYRET_ERR_CHECKSUM");
                broadCastErrorMessage("CYRET_ERR_CHECKSUM");
                break;
            case 9:
                Logger.i("CYRET_ERR_ARRAY");
                broadCastErrorMessage("CYRET_ERR_ARRAY");
                break;
            case 10:
                Logger.i("CYRET_ERR_ROW");
                broadCastErrorMessage("CYRET_ERR_ROW");
                break;
            case 11:
                Logger.i("CYRET_BTLDR");
                broadCastErrorMessage("CYRET_BTLDR");
                break;
            case 12:
                Logger.i("CYRET_ERR_APP");
                broadCastErrorMessage("CYRET_ERR_APP");
                break;
            case 13:
                Logger.i("CYRET_ERR_ACTIVE");
                broadCastErrorMessage("CYRET_ERR_ACTIVE");
                break;
            case 14:
                Logger.i("CYRET_ERR_UNK");
                broadCastErrorMessage("CYRET_ERR_UNK");
                break;
            case 15:
                Logger.i("CYRET_ABORT");
                broadCastErrorMessage("CYRET_ABORT");
                break;
            default:
                Logger.i("CYRET DEFAULT");
                break;
        }
    }

    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseGetFlashSizeAcknowledgement(String parse) {
        String result = parse.trim().replace(" ", "");
        String response = result.substring(2, 4);
        String dataLength = result.substring(4, 8);
        String startRow = result.substring(8, 12);
        String endRow = result.substring(12, 16);

        int reponseBytes = Integer.parseInt(response, 16);
        switch (reponseBytes) {
            case 0:
                Logger.i("CYRET_SUCCESS");
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_START_ROW,
                        startRow);
                mBundle.putString(Constants.EXTRA_END_ROW, endRow);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            case 1:
                Logger.i("CYRET_ERR_FILE");
                broadCastErrorMessage("CYRET_ERR_FILE");
                break;
            case 2:
                Logger.i("CYRET_ERR_EOF");
                broadCastErrorMessage("CYRET_ERR_EOF");
                break;
            case 3:
                Logger.i("CYRET_ERR_LENGTH");
                broadCastErrorMessage("CYRET_ERR_LENGTH");
                break;
            case 4:
                Logger.i("CYRET_ERR_DATA");
                broadCastErrorMessage("CYRET_ERR_DATA");
                break;
            case 5:
                Logger.i("CYRET_ERR_CMD");
                broadCastErrorMessage("CYRET_ERR_CMD");
                break;
            case 6:
                Logger.i("CYRET_ERR_DEVICE");
                broadCastErrorMessage("CYRET_ERR_DEVICE");
                break;
            case 7:
                Logger.i("CYRET_ERR_VERSION");
                broadCastErrorMessage("CYRET_ERR_VERSION");
                break;
            case 8:
                Logger.i("CYRET_ERR_CHECKSUM");
                broadCastErrorMessage("CYRET_ERR_CHECKSUM");
                break;
            case 9:
                Logger.i("CYRET_ERR_ARRAY");
                broadCastErrorMessage("CYRET_ERR_ARRAY");
                break;
            case 10:
                Logger.i("CYRET_ERR_ROW");
                broadCastErrorMessage("CYRET_ERR_ROW");
                break;
            case 11:
                Logger.i("CYRET_BTLDR");
                broadCastErrorMessage("CYRET_BTLDR");
                break;
            case 12:
                Logger.i("CYRET_ERR_APP");
                broadCastErrorMessage("CYRET_ERR_APP");
                break;
            case 13:
                Logger.i("CYRET_ERR_ACTIVE");
                broadCastErrorMessage("CYRET_ERR_ACTIVE");
                break;
            case 14:
                Logger.i("CYRET_ERR_UNK");
                broadCastErrorMessage("CYRET_ERR_UNK");
                break;
            case 15:
                Logger.i("CYRET_ABORT");
                broadCastErrorMessage("CYRET_ABORT");
                break;
            default:
                Logger.i("CYRET DEFAULT");
                break;
        }
    }

    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseParseRowAcknowledgement(String parse) {
        String result = parse.trim().replace(" ", "");
        String response = result.substring(2, 4);
        String status = result.substring(4, 6);
        int reponseBytes = Integer.parseInt(response, 16);
        switch (reponseBytes) {
            case 0:
                Logger.i("CYRET_SUCCESS");
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_PROGRAM_ROW_STATUS,
                        status);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            case 1:
                Logger.i("CYRET_ERR_FILE");
                broadCastErrorMessage("CYRET_ERR_FILE");
                break;
            case 2:
                Logger.i("CYRET_ERR_EOF");
                broadCastErrorMessage("CYRET_ERR_EOF");
                break;
            case 3:
                Logger.i("CYRET_ERR_LENGTH");
                broadCastErrorMessage("CYRET_ERR_LENGTH");
                break;
            case 4:
                Logger.i("CYRET_ERR_DATA");
                broadCastErrorMessage("CYRET_ERR_DATA");
                break;
            case 5:
                Logger.i("CYRET_ERR_CMD");
                broadCastErrorMessage("CYRET_ERR_CMD");
                break;
            case 6:
                Logger.i("CYRET_ERR_DEVICE");
                broadCastErrorMessage("CYRET_ERR_DEVICE");
                break;
            case 7:
                Logger.i("CYRET_ERR_VERSION");
                broadCastErrorMessage("CYRET_ERR_VERSION");
                break;
            case 8:
                Logger.i("CYRET_ERR_CHECKSUM");
                broadCastErrorMessage("CYRET_ERR_CHECKSUM");
                break;
            case 9:
                Logger.i("CYRET_ERR_ARRAY");
                broadCastErrorMessage("CYRET_ERR_ARRAY");
                break;
            case 10:
                Logger.i("CYRET_ERR_ROW");
                broadCastErrorMessage("CYRET_ERR_ROW");
                break;
            case 11:
                Logger.i("CYRET_BTLDR");
                broadCastErrorMessage("CYRET_BTLDR");
                break;
            case 12:
                Logger.i("CYRET_ERR_APP");
                broadCastErrorMessage("CYRET_ERR_APP");
                break;
            case 13:
                Logger.i("CYRET_ERR_ACTIVE");
                broadCastErrorMessage("CYRET_ERR_ACTIVE");
                break;
            case 14:
                Logger.i("CYRET_ERR_UNK");
                broadCastErrorMessage("CYRET_ERR_UNK");
                break;
            case 15:
                Logger.i("CYRET_ABORT");
                broadCastErrorMessage("CYRET_ABORT");
                break;
            default:
                Logger.i("CYRET DEFAULT");
                break;
        }
    }

    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseVerifyRowAcknowledgement(String parse) {
        String result = parse.trim().replace(" ", "");
        String response = result.substring(2, 4);
        String data = result.substring(8, 10);

        int reponseBytes = Integer.parseInt(response, 16);
        switch (reponseBytes) {
            case 0:
                Logger.i("CYRET_SUCCESS");
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_VERIFY_ROW_STATUS,
                        response);
                mBundle.putString(Constants.EXTRA_VERIFY_ROW_CHECKSUM,
                        data);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            case 1:
                Logger.i("CYRET_ERR_FILE");
                broadCastErrorMessage("CYRET_ERR_FILE");
                break;
            case 2:
                Logger.i("CYRET_ERR_EOF");
                broadCastErrorMessage("CYRET_ERR_EOF");
                break;
            case 3:
                Logger.i("CYRET_ERR_LENGTH");
                broadCastErrorMessage("CYRET_ERR_LENGTH");
                break;
            case 4:
                Logger.i("CYRET_ERR_DATA");
                broadCastErrorMessage("CYRET_ERR_DATA");
                break;
            case 5:
                Logger.i("CYRET_ERR_CMD");
                broadCastErrorMessage("CYRET_ERR_CMD");
                break;
            case 6:
                Logger.i("CYRET_ERR_DEVICE");
                broadCastErrorMessage("CYRET_ERR_DEVICE");
                break;
            case 7:
                Logger.i("CYRET_ERR_VERSION");
                broadCastErrorMessage("CYRET_ERR_VERSION");
                break;
            case 8:
                Logger.i("CYRET_ERR_CHECKSUM");
                broadCastErrorMessage("CYRET_ERR_CHECKSUM");
                break;
            case 9:
                Logger.i("CYRET_ERR_ARRAY");
                broadCastErrorMessage("CYRET_ERR_ARRAY");
                break;
            case 10:
                Logger.i("CYRET_ERR_ROW");
                broadCastErrorMessage("CYRET_ERR_ROW");
                break;
            case 11:
                Logger.i("CYRET_BTLDR");
                broadCastErrorMessage("CYRET_BTLDR");
                break;
            case 12:
                Logger.i("CYRET_ERR_APP");
                broadCastErrorMessage("CYRET_ERR_APP");
                break;
            case 13:
                Logger.i("CYRET_ERR_ACTIVE");
                broadCastErrorMessage("CYRET_ERR_ACTIVE");
                break;
            case 14:
                Logger.i("CYRET_ERR_UNK");
                broadCastErrorMessage("CYRET_ERR_UNK");
                break;
            case 15:
                Logger.i("CYRET_ABORT");
                broadCastErrorMessage("CYRET_ABORT");
                break;
            default:
                Logger.i("CYRET DEFAULT");
                break;
        }
    }

    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseVerifyCheckSum(String parse) {
        String result = parse.trim().replace(" ", "");
        String response = result.substring(2, 4);
        String checkSumStatus = result.substring(4, 6);
        int reponseBytes = Integer.parseInt(response, 16);
        switch (reponseBytes) {
            case 0:
                Logger.i("CYRET_SUCCESS");
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_VERIFY_CHECKSUM_STATUS,
                        checkSumStatus);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            case 1:
                Logger.i("CYRET_ERR_FILE");
                broadCastErrorMessage("CYRET_ERR_FILE");
                break;
            case 2:
                Logger.i("CYRET_ERR_EOF");
                broadCastErrorMessage("CYRET_ERR_EOF");
                break;
            case 3:
                Logger.i("CYRET_ERR_LENGTH");
                broadCastErrorMessage("CYRET_ERR_LENGTH");
                break;
            case 4:
                Logger.i("CYRET_ERR_DATA");
                broadCastErrorMessage("CYRET_ERR_DATA");
                break;
            case 5:
                Logger.i("CYRET_ERR_CMD");
                broadCastErrorMessage("CYRET_ERR_CMD");
                break;
            case 6:
                Logger.i("CYRET_ERR_DEVICE");
                broadCastErrorMessage("CYRET_ERR_DEVICE");
                break;
            case 7:
                Logger.i("CYRET_ERR_VERSION");
                broadCastErrorMessage("CYRET_ERR_VERSION");
                break;
            case 8:
                Logger.i("CYRET_ERR_CHECKSUM");
                broadCastErrorMessage("CYRET_ERR_CHECKSUM");
                break;
            case 9:
                Logger.i("CYRET_ERR_ARRAY");
                broadCastErrorMessage("CYRET_ERR_ARRAY");
                break;
            case 10:
                Logger.i("CYRET_ERR_ROW");
                broadCastErrorMessage("CYRET_ERR_ROW");
                break;
            case 11:
                Logger.i("CYRET_BTLDR");
                broadCastErrorMessage("CYRET_BTLDR");
                break;
            case 12:
                Logger.i("CYRET_ERR_APP");
                broadCastErrorMessage("CYRET_ERR_APP");
                break;
            case 13:
                Logger.i("CYRET_ERR_ACTIVE");
                broadCastErrorMessage("CYRET_ERR_ACTIVE");
                break;
            case 14:
                Logger.i("CYRET_ERR_UNK");
                broadCastErrorMessage("CYRET_ERR_UNK");
                break;
            case 15:
                Logger.i("CYRET_ABORT");
                broadCastErrorMessage("CYRET_ABORT");
                break;
            default:
                Logger.i("CYRET DEFAULT");
                break;
        }
    }


    /**
     * Method parses the response String and executes the corresponding cases
     *
     * @param parse
     */
    private void parseExitBootloader(String parse) {
        String response = parse.trim().replace(" ", "");
        Logger.i("RESPONSE EXIT>> " + response);
        int reponseBytes = Integer.parseInt(response, 16);
        switch (reponseBytes) {
            case 0:
                Logger.i("CYRET_SUCCESS");
                Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
                Bundle mBundle = new Bundle();
                mBundle.putString(Constants.EXTRA_VERIFY_EXIT_BOOTLOADER,
                        response);
                intent.putExtras(mBundle);
                mContext.sendBroadcast(intent);
                break;
            case 1:
                Logger.i("CYRET_ERR_FILE");
                broadCastErrorMessage("CYRET_ERR_FILE");
                break;
            case 2:
                Logger.i("CYRET_ERR_EOF");
                broadCastErrorMessage("CYRET_ERR_EOF");
                break;
            case 3:
                Logger.i("CYRET_ERR_LENGTH");
                broadCastErrorMessage("CYRET_ERR_LENGTH");
                break;
            case 4:
                Logger.i("CYRET_ERR_DATA");
                broadCastErrorMessage("CYRET_ERR_DATA");
                break;
            case 5:
                Logger.i("CYRET_ERR_CMD");
                broadCastErrorMessage("CYRET_ERR_CMD");
                break;
            case 6:
                Logger.i("CYRET_ERR_DEVICE");
                broadCastErrorMessage("CYRET_ERR_DEVICE");
                break;
            case 7:
                Logger.i("CYRET_ERR_VERSION");
                broadCastErrorMessage("CYRET_ERR_VERSION");
                break;
            case 8:
                Logger.i("CYRET_ERR_CHECKSUM");
                broadCastErrorMessage("CYRET_ERR_CHECKSUM");
                break;
            case 9:
                Logger.i("CYRET_ERR_ARRAY");
                broadCastErrorMessage("CYRET_ERR_ARRAY");
                break;
            case 10:
                Logger.i("CYRET_ERR_ROW");
                broadCastErrorMessage("CYRET_ERR_ROW");
                break;
            case 11:
                Logger.i("CYRET_BTLDR");
                broadCastErrorMessage("CYRET_BTLDR");
                break;
            case 12:
                Logger.i("CYRET_ERR_APP");
                broadCastErrorMessage("CYRET_ERR_APP");
                break;
            case 13:
                Logger.i("CYRET_ERR_ACTIVE");
                broadCastErrorMessage("CYRET_ERR_ACTIVE");
                break;
            case 14:
                Logger.i("CYRET_ERR_UNK");
                broadCastErrorMessage("CYRET_ERR_UNK");
                break;
            case 15:
                Logger.i("CYRET_ABORT");
                broadCastErrorMessage("CYRET_ABORT");
                break;
            default:
                Logger.i("CYRET DEFAULT");
                break;
        }
    }

    public void broadCastErrorMessage(String errorMessage) {
        Intent intent = new Intent(BootLoaderUtils.ACTION_OTA_STATUS);
        Bundle mBundle = new Bundle();
        mBundle.putString(Constants.EXTRA_ERROR_OTA,
                errorMessage);
        intent.putExtras(mBundle);
        mContext.sendBroadcast(intent);
    }

}
