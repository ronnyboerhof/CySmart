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
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import java.util.List;

/**
 * Fragment to display the blood pressure service
 */
public class BloodPressureService extends Fragment {

    // Service and characteristics
    private static BluetoothGattService mService;
    private static BluetoothGattCharacteristic mIndicateCharacteristic;

    // Data fields
    private TextView mSystolicPressure;
    private TextView mDiastolicPressure;
    private TextView mSystolicPressureUnit;
    private TextView mDiastolicPressureUnit;

    private ProgressDialog mProgressDialog;


    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            // GATT Data available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                // Check for SYSTOLIC pressure
                if (extras.containsKey(Constants.EXTRA_PRESURE_SYSTOLIC_VALUE)) {
                    String receivedSystolicPressure = extras
                            .getString(Constants.EXTRA_PRESURE_SYSTOLIC_VALUE);
                    displaySYSData(receivedSystolicPressure);

                }
                // Check for DIASTOLIC pressure
                if (extras.containsKey(Constants.EXTRA_PRESURE_DIASTOLIC_VALUE)) {
                    String receivedDiastolicPressure = extras
                            .getString(Constants.EXTRA_PRESURE_DIASTOLIC_VALUE);
                    displayDIAData(receivedDiastolicPressure);

                }
                // Check for SYSTOLIC pressure unit
                if (extras
                        .containsKey(Constants.EXTRA_PRESURE_SYSTOLIC_UNIT_VALUE)) {
                    String receivedSystolicPressure = extras
                            .getString(Constants.EXTRA_PRESURE_SYSTOLIC_UNIT_VALUE);
                    displaySYSUnitData(receivedSystolicPressure);

                }
                // Check for DIASTOLIC pressure unit
                if (extras
                        .containsKey(Constants.EXTRA_PRESURE_DIASTOLIC_UNIT_VALUE)) {
                    String receivedDiastolicPressure = extras
                            .getString(Constants.EXTRA_PRESURE_DIASTOLIC_UNIT_VALUE);
                    displayDIAUnitData(receivedDiastolicPressure);

                }
            }
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDING) {
                    // Bonding...
                    Logger.i("Bonding is in process....");
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, true);
                } else if (state == BluetoothDevice.BOND_BONDED) {
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

    public BloodPressureService create(BluetoothGattService service) {
        BloodPressureService fragment = new BloodPressureService();
        mService = service;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.blood_pressure_measurement,
                container, false);
        mProgressDialog = new ProgressDialog(getActivity());
        mSystolicPressure = (TextView) rootView.findViewById(R.id.bp_sys_value);
        mDiastolicPressure = (TextView) rootView
                .findViewById(R.id.bp_dia_value);

        mSystolicPressureUnit = (TextView) rootView
                .findViewById(R.id.bp_sys_value_unit);
        mDiastolicPressureUnit = (TextView) rootView
                .findViewById(R.id.bp_dia_value_unit);
        Button startStopBtn = (Button) rootView
                .findViewById(R.id.start_stop_btn);


        // Start/Stop button listener
        startStopBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Button btn = (Button) v;
                String buttonText = btn.getText().toString();
                String startText = getResources().getString(
                        R.string.blood_pressure_start_btn);
                String stopText = getResources().getString(
                        R.string.blood_pressure_stop_btn);
                if (buttonText.equalsIgnoreCase(startText)) {
                    btn.setText(stopText);
                    if (mIndicateCharacteristic != null) {
                        prepareBroadcastDataIndicate(mIndicateCharacteristic);
                    }
                    getGattData();
                } else {
                    btn.setText(startText);
                    stopBroadcastDataIndicate(mIndicateCharacteristic);
                }

            }
        });
        setHasOptionsMenu(true);
        return rootView;
    }

    /**
     * Display DIASTOLIC data
     *
     * @param received_diastolic_pressure
     */

    void displayDIAData(String received_diastolic_pressure) {
        mDiastolicPressure.setText(String.format("%.2f", Float.parseFloat(received_diastolic_pressure)));

    }

    /**
     * Display SYSTOLIC Data
     *
     * @param received_systolic_pressure
     */
    void displaySYSData(String received_systolic_pressure) {
        mSystolicPressure.setText(String.format("%.2f", Float.parseFloat(received_systolic_pressure)));

    }

    /**
     * Display the DIASTOLIC unit
     *
     * @param received_diastolic_pressure
     */
    void displayDIAUnitData(String received_diastolic_pressure) {
        mDiastolicPressureUnit.setText(received_diastolic_pressure);
    }

    /**
     * Display the SYSTOLIC unit
     *
     * @param received_systolic_pressure
     */

    void displaySYSUnitData(String received_systolic_pressure) {
        mSystolicPressureUnit.setText(received_systolic_pressure);
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

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.blood_pressure));

    }

    @Override
    public void onDestroy() {
        if (mIndicateCharacteristic != null) {
            stopBroadcastDataIndicate(mIndicateCharacteristic);
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
            if (uuidchara
                    .equalsIgnoreCase(GattAttributes.BLOOD_PRESSURE_MEASUREMENT)) {
                mIndicateCharacteristic = gattCharacteristic;
                prepareBroadcastDataIndicate(gattCharacteristic);
                break;
            }

        }
    }

    /**
     * Preparing Broadcast receiver to broadcast indicate characteristics
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataIndicate(
            BluetoothGattCharacteristic gattCharacteristic) {
        final int charaProp = gattCharacteristic.getProperties();

        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            if (mIndicateCharacteristic != null) {
                mIndicateCharacteristic = gattCharacteristic;
                BluetoothLeService.setCharacteristicIndication(
                        mIndicateCharacteristic, true);
            }
        }


    }

    /**
     * Stopping Broadcast receiver to broadcast indicate characteristics
     *
     * @param gattCharacteristic
     */
    void stopBroadcastDataIndicate(
            BluetoothGattCharacteristic gattCharacteristic) {
        final int charaProp = gattCharacteristic.getProperties();

        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            if (mIndicateCharacteristic != null) {
                Logger.d("Stopped notification");
                BluetoothLeService.setCharacteristicIndication(
                        mIndicateCharacteristic, false);
                mIndicateCharacteristic = null;
            }

        }

    }

}
