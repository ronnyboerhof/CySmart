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
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.CustomSpinner;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FindMeService extends Fragment {

    private ImageView mTransmissionPower;
    private TextView mTransmissionPowerValue;
    private static String mFragmentTitleName;
    private View rootView;
    /**
     * Flag to handle the handler
     */
    private boolean mHandlerFlag = true;

    //ProgressDialog
    private ProgressDialog mProgressDialog;

    // GATT service and characteristic
    private static BluetoothGattService mCurrentservice;
    private static ArrayList<HashMap<String, BluetoothGattService>> mExtraservice;
    private static BluetoothGattCharacteristic mReadCharacteristic_tp;


    // Immediate alert constants
    private static final String IMM_NO_ALERT = "0x00";
    private static final String IMM_MID_ALERT = "0x01";
    private static final String IMM_HIGH_ALERT = "0x02";

    // Immediate alert text
    private static final String IMM_NO_ALERT_TEXT = " No Alert ";
    private static final String IMM_MID_ALERT_TEXT = " Mid Alert ";
    private static final String IMM_HIGH_ALERT_TEXT = " High Alert ";

    //Selected spinner position
    private int mSelectedLinkLossPosition=3;
    private int mSelectedImmediateAlertPosition=3;

    private CustomSpinner mSpinnerLinkLoss;
    private CustomSpinner mSpinnerImmediateAlert;

    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            // GATT Data available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // Check power value
                if (extras.containsKey(Constants.EXTRA_POWER_VALUE)) {
                    int received_pwr_data = intent.getIntExtra(
                            Constants.EXTRA_POWER_VALUE, 246);
                    Handler handler = new Handler();
                     Runnable mrun = new Runnable() {

                        @Override
                        public void run() {
                            if (mHandlerFlag) {
                                prepareBroadcastDataReadtp(mReadCharacteristic_tp);
                            }

                        }
                    };
                    handler.postDelayed(mrun, 500);
                    if (received_pwr_data != 246) {
                        float value = received_pwr_data;
                        float flval = (float) 1 / 120;
                        float scaleVal = (value + 100) * flval;
                        Logger.i("scaleVal " + scaleVal);
                        mTransmissionPower.animate().setDuration(500)
                                .scaleX(scaleVal);
                        mTransmissionPower.animate().setDuration(500)
                                .scaleY(scaleVal);
                        mTransmissionPowerValue.setText(String
                                .valueOf(received_pwr_data));

                    }

                }

            }
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR);

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

    public FindMeService create(
            BluetoothGattService currentservice,
            ArrayList<HashMap<String, BluetoothGattService>> gattExtraServiceData,String fragname) {
        FindMeService fragment = new FindMeService();
        mCurrentservice = currentservice;
        mExtraservice = gattExtraServiceData;
        mFragmentTitleName=fragname;
        Logger.i("mFragmentTitleName-->"+mFragmentTitleName);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.profile_findme, container, false);
        mProgressDialog = new ProgressDialog(getActivity());
        setHasOptionsMenu(true);
        return rootView;
    }

    /**
     * Prepare Broadcast receiver to broadcast read characteristics Transmission
     * power
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataReadtp(
            BluetoothGattCharacteristic gattCharacteristic) {
        final BluetoothGattCharacteristic characteristic = gattCharacteristic;
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            mReadCharacteristic_tp = characteristic;
            BluetoothLeService.readCharacteristic(characteristic);
        }
    }

    @Override
    public void onResume() {
        getGattData();
        updateSpinners();
        mHandlerFlag=true;
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        Utils.setUpActionBar(getActivity(),
                mFragmentTitleName);
        super.onResume();
    }

    private void updateSpinners() {
        Logger.i("mSelectedLinkLossPosition-->"+mSelectedLinkLossPosition);
        if(mSpinnerImmediateAlert!=null){
            if(mSelectedImmediateAlertPosition!=3)
            mSpinnerImmediateAlert.setSelection(mSelectedImmediateAlertPosition);
        }

        if(mSpinnerLinkLoss!=null){
            if(mSelectedLinkLossPosition!=3)
                mSpinnerLinkLoss.setSelection(mSelectedLinkLossPosition);
        }

    }

    /**
     * Method to get required characteristics from service
     */
    private void getGattData() {
        LinearLayout ll_layout = (LinearLayout) rootView
                .findViewById(R.id.linkloss_layout);
        LinearLayout im_layout = (LinearLayout) rootView
                .findViewById(R.id.immalert_layout);
        LinearLayout tp_layout = (LinearLayout) rootView
                .findViewById(R.id.transmission_layout);
        RelativeLayout tpr_layout = (RelativeLayout) rootView
                .findViewById(R.id.transmission_rel_layout);

        for (int position = 0; position < mExtraservice.size(); position++) {
            HashMap<String, BluetoothGattService> item = mExtraservice
                    .get(position);
            BluetoothGattService bgs = item.get("UUID");
            List<BluetoothGattCharacteristic> gattCharacteristicsCurrent = bgs
                    .getCharacteristics();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristicsCurrent) {
                String uuidchara = gattCharacteristic.getUuid().toString();
                if (uuidchara.equalsIgnoreCase(GattAttributes.ALERT_LEVEL)) {
                    if (bgs.getUuid().toString()
                            .equalsIgnoreCase(GattAttributes.LINK_LOSS_SERVICE)) {
                        ll_layout.setVisibility(View.VISIBLE);
                        mSpinnerLinkLoss = (CustomSpinner) rootView
                                .findViewById(R.id.linkloss_spinner);
                      // Create an ArrayAdapter using the string array and a
                        // default
                        // spinner layout
                        ArrayAdapter<CharSequence> adapter_linkloss = ArrayAdapter
                                .createFromResource(getActivity(),
                                        R.array.findme_immediate_alert_array,
                                        android.R.layout.simple_spinner_item);
                        // Specify the layout to use when the list of choices
                        // appears
                        adapter_linkloss
                                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        // Apply the adapter to the spinner
                        mSpinnerLinkLoss.setAdapter(adapter_linkloss);
                        mSpinnerLinkLoss
                                .setOnItemSelectedListener(new OnItemSelectedListener() {

                                    @Override
                                    public void onItemSelected(
                                            AdapterView<?> parent, View view,
                                            int position, long id) {

                                        if (parent.getItemAtPosition(position)
                                                .toString()
                                                .equalsIgnoreCase("No Alert")) {
                                            byte[] convertedBytes = convertingTobyteArray(
                                                    IMM_NO_ALERT);
                                            BluetoothLeService
                                                    .writeCharacteristicNoresponse(
                                                            gattCharacteristic,
                                                            convertedBytes);
                                            Toast.makeText(
                                                    getActivity(),
                                                    getResources().getString(R.string.find_value_written_toast)
                                                            + IMM_NO_ALERT_TEXT
                                                            + getResources().getString(R.string.find_value_success_toast),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        if (parent.getItemAtPosition(position)
                                                .toString()
                                                .equalsIgnoreCase("Mid Alert")) {
                                            byte[] convertedBytes = convertingTobyteArray(
                                                    IMM_MID_ALERT);
                                            BluetoothLeService
                                                    .writeCharacteristicNoresponse(
                                                            gattCharacteristic,
                                                            convertedBytes);
                                                   Toast.makeText(
                                                    getActivity(),
                                                    getResources().getString(R.string.find_value_written_toast)
                                                            + IMM_MID_ALERT_TEXT
                                                            + getResources().getString(R.string.find_value_success_toast),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        if (parent.getItemAtPosition(position)
                                                .toString()
                                                .equalsIgnoreCase("High Alert")) {
                                            byte[] convertedBytes = convertingTobyteArray(
                                                    IMM_HIGH_ALERT);
                                            BluetoothLeService
                                                    .writeCharacteristicNoresponse(
                                                            gattCharacteristic,
                                                            convertedBytes);
                                            Toast.makeText(
                                                    getActivity(),
                                                    getResources().getString(R.string.find_value_written_toast)
                                                            + IMM_HIGH_ALERT_TEXT
                                                            + getResources().getString(R.string.find_value_success_toast),
                                                    Toast.LENGTH_SHORT).show();
                                        }

                                    }

                                    @Override
                                    public void onNothingSelected(
                                            AdapterView<?> parent) {
                                        // TODO Auto-generated method stub

                                    }
                                });
                    }
                    if (bgs.getUuid()
                            .toString()
                            .equalsIgnoreCase(
                                    GattAttributes.IMMEDIATE_ALERT_SERVICE)) {
                        im_layout.setVisibility(View.VISIBLE);
                        mSpinnerImmediateAlert = (CustomSpinner) rootView
                                .findViewById(R.id.immediate_spinner);
                        // Create an ArrayAdapter using the string array and a
                        // default
                        // spinner layout
                        ArrayAdapter<CharSequence> adapter_immediate_alert = ArrayAdapter
                                .createFromResource(getActivity(),
                                        R.array.findme_immediate_alert_array,
                                        android.R.layout.simple_spinner_item);
                        // Specify the layout to use when the list of choices
                        // appears
                        adapter_immediate_alert
                                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        // Apply the adapter to the spinner
                        mSpinnerImmediateAlert
                                .setAdapter(adapter_immediate_alert);
                        mSpinnerImmediateAlert
                                .setOnItemSelectedListener(new OnItemSelectedListener() {

                                    @Override
                                    public void onItemSelected(
                                            AdapterView<?> parent, View view,
                                            int position, long id) {

                                        if (parent.getItemAtPosition(position)
                                                .toString()
                                                .equalsIgnoreCase("No Alert")) {
                                            byte[] convertedBytes = convertingTobyteArray(
                                                    IMM_NO_ALERT);
                                            BluetoothLeService
                                                    .writeCharacteristicNoresponse(
                                                            gattCharacteristic,
                                                            convertedBytes);
                                                     Toast.makeText(
                                                    getActivity(),
                                                    getResources().getString(R.string.find_value_written_toast)
                                                            + IMM_NO_ALERT_TEXT
                                                            + getResources().getString(R.string.find_value_success_toast),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        if (parent.getItemAtPosition(position)
                                                .toString()
                                                .equalsIgnoreCase("Mid Alert")) {
                                            byte[] convertedBytes = convertingTobyteArray(
                                                    IMM_MID_ALERT);
                                            BluetoothLeService
                                                    .writeCharacteristicNoresponse(
                                                            gattCharacteristic,
                                                            convertedBytes);
                                            Toast.makeText(
                                                    getActivity(),
                                                    getResources().getString(R.string.find_value_written_toast)
                                                            + IMM_MID_ALERT_TEXT
                                                            + getResources().getString(R.string.find_value_success_toast),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        if (parent.getItemAtPosition(position)
                                                .toString()
                                                .equalsIgnoreCase("High Alert")) {
                                            byte[] convertedBytes = convertingTobyteArray(
                                                    IMM_HIGH_ALERT);
                                            BluetoothLeService
                                                    .writeCharacteristicNoresponse(
                                                            gattCharacteristic,
                                                            convertedBytes);
                                            Toast.makeText(
                                                    getActivity(),
                                                    getResources().getString(R.string.find_value_written_toast)
                                                            + IMM_HIGH_ALERT_TEXT
                                                            + getResources().getString(R.string.find_value_success_toast),
                                                    Toast.LENGTH_SHORT).show();
                                        }

                                    }

                                    @Override
                                    public void onNothingSelected(
                                            AdapterView<?> parent) {
                                        // TODO Auto-generated method stub

                                    }
                                });
                    }

                }
               if (uuidchara
                        .equalsIgnoreCase(GattAttributes.TRANSMISSION_POWER_LEVEL)) {
                    tp_layout.setVisibility(View.VISIBLE);
                    tpr_layout.setVisibility(View.VISIBLE);
                    mReadCharacteristic_tp = gattCharacteristic;
                    mTransmissionPower = (ImageView) rootView
                            .findViewById(R.id.findme_tx_power_img);
                    mTransmissionPowerValue = (TextView) rootView
                            .findViewById(R.id.findme_tx_power_txt);
                    if (mReadCharacteristic_tp != null) {
                        prepareBroadcastDataReadtp(mReadCharacteristic_tp);
                    }

                }

            }
        }

    }

    @Override
    public void onPause() {
        mHandlerFlag=false;
        if(mSpinnerImmediateAlert!=null)
        mSelectedImmediateAlertPosition=mSpinnerImmediateAlert.getSelectedItemPosition();
        if(mSpinnerLinkLoss!=null)
        mSelectedLinkLossPosition = mSpinnerLinkLoss.getSelectedItemPosition();
        super.onPause();
    }

    @Override
    public void onDestroy() {
       // mReadCharacteristic_ll = null;
        mReadCharacteristic_tp = null;
        getActivity().unregisterReceiver(mGattUpdateReceiver);

        super.onDestroy();
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
    /**
     * Method to convert hex to byteArray
     */
    private byte[] convertingTobyteArray(String result) {
        String[] splited = result.split("\\s+");
        byte[] valueByte = new byte[splited.length];
        for (int i = 0; i < splited.length; i++) {
            if (splited[i].length() > 2) {
                String trimmedByte = splited[i].split("x")[1];
                valueByte[i] = (byte) convertstringtobyte(trimmedByte);
            }

        }
        return valueByte;
    }
    /**
     * Convert the string to byte
     *
     * @param string
     * @return
     */
    private int convertstringtobyte(String string) {
        return Integer.parseInt(string, 16);
    }

}
