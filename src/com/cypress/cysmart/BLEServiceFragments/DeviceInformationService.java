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

import java.util.List;

/**
 * Fragment to display the Device information service
 */
public class DeviceInformationService extends Fragment {

    // GATT Service and Characteristics
    private static BluetoothGattService mService;
    private static BluetoothGattCharacteristic mReadCharacteristic;

    // Data view variables
    private TextView mManufacturerName;
    private TextView mModelName;
    private TextView mSerialName;
    private TextView mHardwareRevisionName;
    private TextView mFirmwareRevisionName;
    private TextView mSoftwareRevisionName;
    private TextView mPnpId;
    private TextView mSysId;
    private TextView mRegulatoryCertificationDataList;
    private AlertDialog alert;
    //ProgressDialog
    private ProgressDialog mProgressDialog;


    // Flag for data set
    private static boolean mManufacturerSet = false;
    private static boolean mmModelNumberSet = false;
    private static boolean mSerialNumberSet = false;
    private static boolean mHardwareNumberSet = false;
    private static boolean mFirmwareNumberSet = false;
    private static boolean mSoftwareNumberSet = false;
    private static boolean mPnpidSet = false;
    private static boolean mRegulatoryCertificationDataListSet = false;
    private static boolean mSystemidSet = false;

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
                // Check manufacture name
                if (extras.containsKey(Constants.EXTRA_MNS_VALUE)) {
                    String received_mns_data = intent
                            .getStringExtra(Constants.EXTRA_MNS_VALUE);
                    if (!received_mns_data.equalsIgnoreCase(" ")) {
                        if (!mManufacturerSet) {
                            mManufacturerSet = true;
                            displayManufactureName(received_mns_data);
                            List<BluetoothGattCharacteristic> mgatt = mService
                                    .getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : mgatt) {
                                String uuidchara = gattCharacteristic.getUuid()
                                        .toString();
                                if (uuidchara
                                        .equalsIgnoreCase(GattAttributes.MODEL_NUMBER_STRING)) {
                                    Logger.i("Characteristic " + uuidchara);
                                    mReadCharacteristic = gattCharacteristic;
                                    prepareBroadcastDataRead(gattCharacteristic);

                                }

                            }
                        }

                    }

                }
                // Check model number
                if (extras.containsKey(Constants.EXTRA_MONS_VALUE)) {
                    String received_mons_data = intent
                            .getStringExtra(Constants.EXTRA_MONS_VALUE);
                    if (!received_mons_data.equalsIgnoreCase(" ")) {
                        if (!mmModelNumberSet) {
                            mmModelNumberSet = true;
                            displayModelNumber(received_mons_data);
                            List<BluetoothGattCharacteristic> mgatt = mService
                                    .getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : mgatt) {
                                String uuidchara = gattCharacteristic.getUuid()
                                        .toString();
                                if (uuidchara
                                        .equalsIgnoreCase(GattAttributes.SERIAL_NUMBER_STRING)) {
                                    Logger.i("Characteristic " + uuidchara);
                                    mReadCharacteristic = gattCharacteristic;
                                    prepareBroadcastDataRead(gattCharacteristic);

                                }

                            }
                        }

                    }
                }
                // Check serial number
                if (extras.containsKey(Constants.EXTRA_SNS_VALUE)) {
                    String received_sns_data = intent
                            .getStringExtra(Constants.EXTRA_SNS_VALUE);
                    if (!received_sns_data.equalsIgnoreCase(" ")) {
                        if (!mSerialNumberSet) {
                            mSerialNumberSet = true;
                            displaySerialNumber(received_sns_data);
                            List<BluetoothGattCharacteristic> mgatt = mService
                                    .getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : mgatt) {
                                String uuidchara = gattCharacteristic.getUuid()
                                        .toString();
                                if (uuidchara
                                        .equalsIgnoreCase(GattAttributes.HARDWARE_REVISION_STRING)) {
                                    Logger.i("Characteristic " + uuidchara);
                                    mReadCharacteristic = gattCharacteristic;
                                    prepareBroadcastDataRead(gattCharacteristic);

                                }

                            }
                        }

                    }
                }
                // check hardware revision
                if (extras.containsKey(Constants.EXTRA_HRS_VALUE)) {
                    String received_hrs_data = intent
                            .getStringExtra(Constants.EXTRA_HRS_VALUE);
                    if (!received_hrs_data.equalsIgnoreCase(" ")) {
                        if (!mHardwareNumberSet) {
                            mHardwareNumberSet = true;
                            displayhardwareNumber(received_hrs_data);
                            List<BluetoothGattCharacteristic> mgatt = mService
                                    .getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : mgatt) {
                                String uuidchara = gattCharacteristic.getUuid()
                                        .toString();
                                if (uuidchara
                                        .equalsIgnoreCase(GattAttributes.FIRMWARE_REVISION_STRING)) {
                                    Logger.i("Characteristic " + uuidchara);
                                    mReadCharacteristic = gattCharacteristic;
                                    prepareBroadcastDataRead(gattCharacteristic);

                                }

                            }
                        }

                    }
                }
                // check firmware revision
                if (extras.containsKey(Constants.EXTRA_FRS_VALUE)) {
                    String received_frs_data = intent
                            .getStringExtra(Constants.EXTRA_FRS_VALUE);
                    if (!received_frs_data.equalsIgnoreCase(" ")) {
                        if (!mFirmwareNumberSet) {
                            mFirmwareNumberSet = true;
                            displayfirmwareNumber(received_frs_data);
                            List<BluetoothGattCharacteristic> mgatt = mService
                                    .getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : mgatt) {
                                String uuidchara = gattCharacteristic.getUuid()
                                        .toString();
                                if (uuidchara
                                        .equalsIgnoreCase(GattAttributes.SOFTWARE_REVISION_STRING)) {
                                    mReadCharacteristic = gattCharacteristic;
                                    prepareBroadcastDataRead(gattCharacteristic);

                                }

                            }
                        }

                    }
                }
                // Check software revision
                if (extras.containsKey(Constants.EXTRA_SRS_VALUE)) {
                    String received_srs_data = intent
                            .getStringExtra(Constants.EXTRA_SRS_VALUE);
                    if (!received_srs_data.equalsIgnoreCase(" ")) {
                        if (!mSoftwareNumberSet) {
                            mSoftwareNumberSet = true;
                            displaySoftwareNumber(received_srs_data);
                            List<BluetoothGattCharacteristic> mgatt = mService
                                    .getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : mgatt) {
                                String uuidchara = gattCharacteristic.getUuid()
                                        .toString();
                                if (uuidchara
                                        .equalsIgnoreCase(GattAttributes.PNP_ID)) {
                                    Logger.i("Characteristic " + uuidchara);
                                    mReadCharacteristic = gattCharacteristic;
                                    prepareBroadcastDataRead(gattCharacteristic);

                                }

                            }
                        }

                    }
                }
                // Check PNP
                if (extras.containsKey(Constants.EXTRA_PNP_VALUE)) {
                    String received_pnpid = intent
                            .getStringExtra(Constants.EXTRA_PNP_VALUE);
                    if (!received_pnpid.equalsIgnoreCase(" ")) {
                        if (!mPnpidSet) {
                            mPnpidSet = true;
                            displayPnpId(received_pnpid);
                            List<BluetoothGattCharacteristic> mgatt = mService
                                    .getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : mgatt) {
                                String uuidchara = gattCharacteristic.getUuid()
                                        .toString();
                                if (uuidchara
                                        .equalsIgnoreCase(GattAttributes.IEEE)) {
                                    Logger.i("Characteristic " + uuidchara);
                                    mReadCharacteristic = gattCharacteristic;
                                    prepareBroadcastDataRead(gattCharacteristic);

                                }
                            }
                        }
                    }
                }
                // Check regulatory id
                if (extras.containsKey(Constants.EXTRA_RCDL_VALUE)) {
                    String received_rcdl_value = intent
                            .getStringExtra(Constants.EXTRA_RCDL_VALUE);
                    if (!received_rcdl_value.equalsIgnoreCase(" ")) {
                        if (!mRegulatoryCertificationDataListSet) {
                            mRegulatoryCertificationDataListSet=true;
                            displayRegulatoryData(received_rcdl_value);
                            List<BluetoothGattCharacteristic> mgatt = mService
                                    .getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : mgatt) {
                                String uuidchara = gattCharacteristic.getUuid()
                                        .toString();
                                if (uuidchara
                                        .equalsIgnoreCase(GattAttributes.SYSTEM_ID)) {
                                    Logger.i("Characteristic " + uuidchara);
                                    mReadCharacteristic = gattCharacteristic;
                                    prepareBroadcastDataRead(gattCharacteristic);

                                }

                            }

                        }

                    }
                }
                // Check software id
                if (extras.containsKey(Constants.EXTRA_SID_VALUE)) {
                    String received_sid_value = intent
                            .getStringExtra(Constants.EXTRA_SID_VALUE);
                    if (!received_sid_value.equalsIgnoreCase(" ")) {
                        if (!mSystemidSet) {
                            displaySystemid(received_sid_value);
                            mReadCharacteristic = null;
                            mSystemidSet = true;

                        }

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
    public DeviceInformationService create(BluetoothGattService service) {
        DeviceInformationService fragment = new DeviceInformationService();
        mService = service;
        return fragment;
    }

    /**
     * Display RCDL Value
     * @param received_rcdl_value
     */
    private void displayRegulatoryData(String received_rcdl_value) {
        mRegulatoryCertificationDataList.setText(received_rcdl_value);

    }
    /**
     * Display SystemID
     *
     * @param received_sid_value
     */

    void displaySystemid(String received_sid_value) {
        mSysId.setText(received_sid_value);

    }

    /**
     * Display PNPID
     *
     * @param received_pnpid
     */
    void displayPnpId(String received_pnpid) {
        mPnpId.setText(received_pnpid);

    }

    /**
     * Display Software revision number
     *
     * @param received_srs_data
     */
    void displaySoftwareNumber(String received_srs_data) {
        mSoftwareRevisionName.setText(received_srs_data);

    }

    /**
     * Display hardware revision number
     *
     * @param received_hrs_data
     */
    void displayhardwareNumber(String received_hrs_data) {
        mHardwareRevisionName.setText(received_hrs_data);

    }
    /**
     * Display firmware revision number
     *
     * @param received_frs_data
     */
    void displayfirmwareNumber(String received_frs_data) {
        mFirmwareRevisionName.setText(received_frs_data);

    }
    /**
     * Display serial number
     *
     * @param received_sns_data
     */
    void displaySerialNumber(String received_sns_data) {
        mSerialName.setText(received_sns_data);

    }

    /**
     * Display model number
     *
     * @param received_mons_data
     */
    void displayModelNumber(String received_mons_data) {
        mModelName.setText(received_mons_data);


    }

    /**
     * Display manufacture name
     *
     * @param received_mns_data
     */

    void displayManufactureName(String received_mns_data) {
        mManufacturerName.setText(received_mns_data);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.device_information_measurement, container, false);
        mManufacturerName = (TextView) rootView
                .findViewById(R.id.div_manufacturer);
        mModelName = (TextView) rootView.findViewById(R.id.div_model);
        mSerialName = (TextView) rootView.findViewById(R.id.div_serial);
        mHardwareRevisionName = (TextView) rootView
                .findViewById(R.id.div_hardware);
        mFirmwareRevisionName = (TextView) rootView
                .findViewById(R.id.div_firmware);
        mSoftwareRevisionName = (TextView) rootView
                .findViewById(R.id.div_software);
        mProgressDialog = new ProgressDialog(getActivity());
        mPnpId = (TextView) rootView.findViewById(R.id.div_pnp);
        mSysId = (TextView) rootView.findViewById(R.id.div_system);
        mRegulatoryCertificationDataList = (TextView) rootView.findViewById(R.id.div_regulatory);
        getActivity().getActionBar().setTitle(R.string.device_info);
        setHasOptionsMenu(true);
        return rootView;
    }

    /**
     * Prepare Broadcast receiver to broadcast read characteristics
     *
     * @param gattCharacteristic
     */

    void prepareBroadcastDataRead(
            BluetoothGattCharacteristic gattCharacteristic) {
        final BluetoothGattCharacteristic characteristic = gattCharacteristic;
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            mReadCharacteristic = characteristic;
            BluetoothLeService.readCharacteristic(characteristic);
        }
    }

    @Override
    public void onResume() {
        makeDefaultBooleans();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        clearUI();
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.device_info));
        getGattData();
        super.onResume();
    }

    /**
     * clear all data fields
     */
    private void clearUI() {
        mManufacturerName.setText("");
        mModelName.setText("");
        mSerialName.setText("");
        mHardwareRevisionName.setText("");
        mSoftwareRevisionName.setText("");
        mSoftwareRevisionName.setText("");
        mPnpId.setText("");
        mSysId.setText("");
    }

    /**
     * Flag up default
     */
    private void makeDefaultBooleans() {
        mManufacturerSet = false;
        mmModelNumberSet = false;
        mSerialNumberSet = false;
        mHardwareNumberSet = false;
        mFirmwareNumberSet=false;
        mSoftwareNumberSet = false;
        mPnpidSet = false;
        mSystemidSet = false;
        mRegulatoryCertificationDataListSet=false;
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        super.onPause();
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
                    .equalsIgnoreCase(GattAttributes.MANUFACTURER_NAME_STRING)) {
                Logger.i("Characteristic div" + uuidchara);
                mReadCharacteristic = gattCharacteristic;
                prepareBroadcastDataRead(gattCharacteristic);
                break;
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
