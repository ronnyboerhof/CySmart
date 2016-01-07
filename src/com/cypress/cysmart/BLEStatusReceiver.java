package com.cypress.cysmart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.OTAFirmwareUpdate.OTAFirmwareUpgradeFragment;

/**
 * Receiver class for BLE disconnect Event.
 * This receiver will be called when a disconnect message from the connected peripheral
 * is received by the application
 */
public class BLEStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
            Logger.e("onReceive--"+HomePageActivity.mApplicationInBackground);
            if(!HomePageActivity.mApplicationInBackground){
                Toast.makeText(context,
                        context.getResources().getString(R.string.alert_message_bluetooth_disconnect),
                        Toast.LENGTH_SHORT).show();
                if (OTAFirmwareUpgradeFragment.mFileupgradeStarted) {
                    //Resetting all preferences on Stop Button
                    Utils.setStringSharedPreference(context, Constants.PREF_OTA_FILE_ONE_NAME, "Default");
                    Utils.setStringSharedPreference(context, Constants.PREF_OTA_FILE_TWO_PATH, "Default");
                    Utils.setStringSharedPreference(context, Constants.PREF_OTA_FILE_TWO_NAME, "Default");
                    Utils.setStringSharedPreference(context, Constants.PREF_BOOTLOADER_STATE, "Default");
                    Utils.setIntSharedPreference(context, Constants.PREF_PROGRAM_ROW_NO, 0);
                }
                Intent homePage=new Intent(context,HomePageActivity.class);
                homePage.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(homePage);

            }

         }

    }
}
