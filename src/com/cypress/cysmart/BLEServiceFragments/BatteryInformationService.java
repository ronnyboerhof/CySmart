/*
 * Copyright Cypress Semiconductor Corporation, 2014-2015 All rights reserved.
 * 
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign), United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate license agreement between you and Cypress, this Software
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

package com.cypress.cysmart.BLEServiceFragments;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import java.util.List;

/**
 * Fragment to show the battery information service
 */
public class BatteryInformationService extends Fragment {

    // Service and characteristics
    private static BluetoothGattService mService;
    private static BluetoothGattCharacteristic mReadCharacteristic;
    private static BluetoothGattCharacteristic mNotifyCharacteristic;
    private Boolean mNotifyCharacteristicEnabled = false;

    //ProgressDialog
    private ProgressDialog mProgressDialog;
    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            // GATT Data available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Logger.i("Data Available");
                // Check for battery information
                if (extras.containsKey(Constants.EXTRA_BTL_VALUE)) {
                    String received_btl_data = intent
                            .getStringExtra(Constants.EXTRA_BTL_VALUE);
                    Logger.i("received_btl_data " + received_btl_data);
                    if (!received_btl_data.equalsIgnoreCase(" ")) {
                        displayBatteryLevel(received_btl_data);

                    }

                }

            }

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDING) {
                    // Bonding...
                    Logger.i("Bonding is in process....");
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, true);
                }  else if (state == BluetoothDevice.BOND_BONDED) {
                    String dataLog=getResources().getString(R.string.dl_commaseparator)
                            +"["+BluetoothLeService.getmBluetoothDeviceName()+"|"
                            +BluetoothLeService.getmBluetoothDeviceAddress()+"]"+
                            getResources().getString(R.string.dl_commaseparator)+
                            getResources().getString(R.string.dl_connection_paired);
                    Logger.datalog(dataLog);
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, false);
                    getGattData();

                } else if (state == BluetoothDevice.BOND_NONE) {
                    String dataLog=getResources().getString(R.string.dl_commaseparator)
                            +"["+BluetoothLeService.getmBluetoothDeviceName()+"|"
                            +BluetoothLeService.getmBluetoothDeviceAddress()+"]"+
                            getResources().getString(R.string.dl_commaseparator)+
                            getResources().getString(R.string.dl_connection_unpaired);
                    Logger.datalog(dataLog);
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, false);
                }
            }

        }

    };
    // Data fields
    private TextView mBatteryLevelText;
    private ProgressBar mBatteryProgress;
    private Button mNotifyButton;
    private Button mReadButton;

    /**
     * Method to display the battery level
     *
     * @param received_btl_data
     */
    private void displayBatteryLevel(String received_btl_data) {
        mBatteryLevelText.setText(received_btl_data + "%");
        int battery = Integer.parseInt(received_btl_data);
        mBatteryProgress.setProgress(battery);
        String batteryValue = Integer.toHexString(Integer.parseInt(received_btl_data));

    }

    public BatteryInformationService create(BluetoothGattService service) {
        BatteryInformationService fragment = new BatteryInformationService();
        mService = service;
        return fragment;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.battery_info_fragment,
                container, false);

        mBatteryLevelText = (TextView) rootView
                .findViewById(R.id.battery_level);
        mBatteryProgress = (ProgressBar) rootView
                .findViewById(R.id.battery_level_progressbar);
        mNotifyButton = (Button) rootView
                .findViewById(R.id.battery_level_notify);
        mReadButton = (Button) rootView
                .findViewById(R.id.battery_level_read);
        mProgressDialog = new ProgressDialog(getActivity());
        mNotifyButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mNotifyCharacteristic != null) {
                    if (mNotifyButton.getText().toString().equalsIgnoreCase(getString(R.string.battery_start_notify))) {
                        mNotifyButton.setText(getString(R.string.battery_stop_notify));
                        prepareBroadcastDataNotify(mNotifyCharacteristic);
                        mNotifyCharacteristicEnabled = true;
                    } else if (mNotifyButton.getText().toString().equalsIgnoreCase(getString(R.string.battery_stop_notify))) {
                        mNotifyButton.setText(getString(R.string.battery_start_notify));
                        stopBroadcastDataNotify(mNotifyCharacteristic);
                        mNotifyCharacteristicEnabled = false;
                    }
                }
            }
        });
        mReadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mReadCharacteristic != null) {
                    prepareBroadcastDataRead(mReadCharacteristic);
                }
            }
        });
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onResume() {
        getGattData();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.battery_info_fragment));
        super.onResume();
    }

    @Override
    public void onDestroy() {
        if (mNotifyCharacteristic != null && mNotifyCharacteristicEnabled) {
            stopBroadcastDataNotify(mNotifyCharacteristic);
        }
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        super.onDestroy();
    }

    /**
     * Method to get required characteristics from service
     */
    void getGattData() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mService
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();
            if (uuidchara.equalsIgnoreCase(GattAttributes.BATTERY_LEVEL)) {
                mReadCharacteristic = gattCharacteristic;
                mNotifyCharacteristic = gattCharacteristic;

                /**
                 * Checking the various GattCharacteristics and listing in the ListView
                 */
                if (checkCharacteristicsPropertyPresence(gattCharacteristic.getProperties(),
                        BluetoothGattCharacteristic.PROPERTY_READ)) {
                      mReadButton.setVisibility(View.VISIBLE);
                 }
                if (checkCharacteristicsPropertyPresence(gattCharacteristic.getProperties(),
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
                    mNotifyButton.setVisibility(View.VISIBLE);
                }
                prepareBroadcastDataRead(gattCharacteristic);
                break;
            }
        }
    }

    /**
     * Preparing Broadcast receiver to broadcast read characteristics
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataRead(
            BluetoothGattCharacteristic gattCharacteristic) {
        final int charaProp = gattCharacteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            mReadCharacteristic = gattCharacteristic;
            BluetoothLeService.readCharacteristic(gattCharacteristic);
        }
    }

    /**
     * Preparing Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        Logger.i("Notify called");
        final int charaProp = gattCharacteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
           BluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                    true);

        }

    }

    /**
     * Stopping Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    void stopBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        Logger.i("Notify stopped");
        final int charaProp = gattCharacteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            if (mNotifyCharacteristic != null) {
                BluetoothLeService.setCharacteristicNotification(
                        mNotifyCharacteristic, false);
            }

        }

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.global, menu);
        MenuItem graph = menu.findItem(R.id.graph);
        MenuItem log = menu.findItem(R.id.log);
        MenuItem search = menu.findItem(R.id.search);
        search.setVisible(false);
        graph.setVisible(false);
        log.setVisible(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    // Return the properties of mGattCharacteristics
    boolean checkCharacteristicsPropertyPresence(int characteristics,
                                                 int characteristicsSearch) {
        return (characteristics & characteristicsSearch) == characteristicsSearch;
    }
}
