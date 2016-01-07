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
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.DataModelClasses.CapSenseButtonsGridModel;
import com.cypress.cysmart.ListAdapters.CapSenseButtonsGridAdapter;
import com.cypress.cysmart.R;

import java.util.ArrayList;

/**
 * Fragment to display the CapSense Buttons
 */
public class CapsenseServiceButtons extends Fragment {

    // GATT Services and characteristics
    private static BluetoothGattService mservice;
    public static BluetoothGattCharacteristic mNotifyCharacteristic;
    public static BluetoothGattCharacteristic mReadCharacteristic;

    // Data variables
    private int mCount = 1;
    private GridView mGridView;
    private CapSenseButtonsGridAdapter mCapsenseButtonsAdapter;
    private ArrayList<CapSenseButtonsGridModel> mData = new ArrayList<CapSenseButtonsGridModel>();
    private ArrayList<Integer> mReceivedButtons = new ArrayList<Integer>();
    private AlertDialog alert;

    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();

            // Data Available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                // Check for CapSense buttons
                if (extras.containsKey(Constants.EXTRA_CAPBUTTONS_VALUE)) {
                    mReceivedButtons = extras
                            .getIntegerArrayList(Constants.EXTRA_CAPBUTTONS_VALUE);
                    displayLiveData(mReceivedButtons);

                }
            }
        }
    };

    public CapsenseServiceButtons create(BluetoothGattService service) {
        CapsenseServiceButtons fragment = new CapsenseServiceButtons();
        mservice = service;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.capsense_buttons, container,
                false);
        mGridView = (GridView) rootView
                .findViewById(R.id.capsense_buttons_grid);
        setHasOptionsMenu(true);
        return rootView;
    }

    /**
     * Display buttons data
     *
     * @param button_data
     */
    private void displayLiveData(ArrayList<Integer> button_data) {
        int buttonCount = button_data.get(0);
        fillButtons(buttonCount);
        setDataAdapter();
        mCapsenseButtonsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mGattUpdateReceiver);
    }

    /**
     * Insert The Data
     *
     * @param buttons
     */
    private void fillButtons(int buttons) {
        mData.clear();
        for (int i = 0; i < buttons; i++) {
            mData.add(new CapSenseButtonsGridModel("" + (mCount + i)
            ));
        }
    }

    /**
     * Set the Data Adapter
     */
    private void setDataAdapter() {
        mCapsenseButtonsAdapter = new CapSenseButtonsGridAdapter(getActivity(),
                mData, mReceivedButtons);
        mGridView.setAdapter(mCapsenseButtonsAdapter);
    }

}
