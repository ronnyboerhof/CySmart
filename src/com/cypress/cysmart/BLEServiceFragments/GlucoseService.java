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

import android.app.AlertDialog;
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
import android.view.ViewGroup;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to show the glucose service
 */
public class GlucoseService extends Fragment {

    // GATT Service and characteristics
    private static BluetoothGattService mservice;
    private static BluetoothGattCharacteristic mNotifyCharacteristic;

    //ProgressDialog
    private ProgressDialog mProgressDialog;

    // Data view variables
    private TextView mGlucoceConcentration;
    private TextView mGlucoseRecordTime;
    private TextView mGlucoseType;
    private TextView mGlucoseSampleLocation;
    private TextView mGlucoseConcentrationUnit;
    private AlertDialog alert;
    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (extras.containsKey(Constants.EXTRA_GLUCOSE_VALUE)) {
                    ArrayList<String> received_glucose_data = intent
                            .getStringArrayListExtra(Constants.EXTRA_GLUCOSE_VALUE);
                    displayLiveData(received_glucose_data);
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

    public GlucoseService create(BluetoothGattService service) {
        GlucoseService fragment = new GlucoseService();
        mservice = service;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.glucose_measurement,
                container, false);
        mGlucoceConcentration = (TextView) rootView
                .findViewById(R.id.glucose_measure);
        mGlucoseRecordTime = (TextView) rootView
                .findViewById(R.id.recording_time_data);
        mGlucoseType = (TextView) rootView.findViewById(R.id.glucose_type);
        mGlucoseSampleLocation = (TextView) rootView
                .findViewById(R.id.glucose_sample_location);
        mGlucoseConcentrationUnit = (TextView) rootView
                .findViewById(R.id.glucose_unit);
        mProgressDialog = new ProgressDialog(getActivity());
        mGlucoceConcentration.setSelected(true);
        mGlucoseRecordTime.setSelected(true);
        mGlucoseType.setSelected(true);
        mGlucoseSampleLocation.setSelected(true);
        setHasOptionsMenu(true);
        // getGattData();
        return rootView;
    }

    private void displayLiveData(ArrayList<String> glucose_data) {
        if (glucose_data != null) {

            try {
                mGlucoceConcentration.setText(glucose_data.get(0));
                mGlucoseType.setText(glucose_data.get(1));
                mGlucoseSampleLocation.setText(glucose_data.get(2));
                mGlucoseRecordTime.setText(glucose_data.get(3));
                mGlucoseConcentrationUnit.setText(glucose_data.get(4));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getGattData();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        /**
         * Initializes ActionBar as required
         */
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.glucose_fragment));

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mNotifyCharacteristic != null) {
            stopBroadcastDataNotify(mNotifyCharacteristic);
        }
        getActivity().unregisterReceiver(mGattUpdateReceiver);
    }

    /**
     * Stopping Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    void stopBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        final BluetoothGattCharacteristic characteristic = gattCharacteristic;
        final int charaProp = characteristic.getProperties();

        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            if (mNotifyCharacteristic != null) {
                BluetoothLeService.setCharacteristicNotification(
                        mNotifyCharacteristic, false);
            }

        }

    }

    /**
     * Preparing Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        final BluetoothGattCharacteristic characteristic = gattCharacteristic;
        final int charaProp = characteristic.getProperties();

        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = characteristic;
            BluetoothLeService.setCharacteristicNotification(characteristic,
                    true);
        }


    }

    /**
     * Method to get required characteristics from service
     */
    void getGattData() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mservice
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();

            if (uuidchara.equalsIgnoreCase(GattAttributes.GLUCOSE_COCNTRN)) {
                mNotifyCharacteristic = gattCharacteristic;
                prepareBroadcastDataNotify(mNotifyCharacteristic);
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
}
