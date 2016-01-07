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
 * arising out of the mApplication or use of Software or any product or
 * circuit described in the Software. Cypress does not authorize its
 * products for use as critical components in any products where a
 * malfunction or failure may reasonably be expected to result in
 * significant injury or death ("High Risk Product"). By including
 * Cypress's product in a High Risk Product, the manufacturer of such
 * system or mApplication assumes all risk of such use and in doing so
 * indemnifies Cypress against all liability.
 * 
 * Use of this Software may be limited by and subject to the applicable
 * Cypress software license agreement.
 * 
 * 
 */

package com.cypress.cysmart.GATTDBFragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.DialogListner;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.HexKeyBoard;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.UUIDDatabase;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.CySmartApplication;
import com.cypress.cysmart.R;

public class GattDetailsFragment extends Fragment implements DialogListner, OnClickListener {

    // Indicate/Notify/Read Flag
    public static boolean mIsNotifyEnabled;
    public static boolean mIsIndicateEnabled;
    // Alert dialog
    AlertDialog mAlertDialog;
    //characteristics
    private BluetoothGattCharacteristic mReadCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mIndicateCharacteristic;
    // TextView variables
    private TextView mServiceName;
    private TextView mCharacteristiceName;
    private TextView mHexValue;
    private TextView mAsciivalue;
    private TextView mDatevalue;
    private TextView mTimevalue;
    private TextView mBtnread;
    private TextView mBtnwrite;
    private TextView mBtnnotify;
    private TextView mBtnIndicate;
    // Application
    private CySmartApplication mApplication;
    private boolean mIsReadEnabled;
    // View
    private ViewGroup mContainer;
    // Back button
    private ImageView backbtn;

    //Descriptor button
    private Button btn_descriptor;

    //ProgressDialog
    private ProgressDialog mProgressDialog;

    //Status buttons
    private String startNotifyText;
    private String stopNotifyText;
    private  String startIndicateText;
    private  String stopIndicateText;

    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Bundle extras = intent.getExtras();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // Data Received
                if (extras.containsKey(Constants.EXTRA_BYTE_VALUE)) {
                    if (extras.containsKey(Constants.EXTRA_BYTE_UUID_VALUE)) {
                        if (mApplication != null) {
                            BluetoothGattCharacteristic requiredCharacteristic = mApplication.getBluetoothgattcharacteristic();
                            String uuidRequired = requiredCharacteristic.getUuid().toString();
                            String receivedUUID = intent.getStringExtra(Constants.EXTRA_BYTE_UUID_VALUE);
                            if (uuidRequired.equalsIgnoreCase(receivedUUID)) {
                                byte[] array = intent
                                        .getByteArrayExtra(Constants.EXTRA_BYTE_VALUE);
                                displayHexValue(array);
                                displayASCIIValue(mHexValue.getText().toString());
                                displayTimeandDate();
                            }
                        }
                    }
                }
                if (extras.containsKey(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE)) {
                    if(extras.containsKey(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE_CHARACTERISTIC_UUID)){
                        BluetoothGattCharacteristic requiredCharacteristic = mApplication.
                                getBluetoothgattcharacteristic();
                        String uuidRequired = requiredCharacteristic.getUuid().toString();
                        String receivedUUID = intent.getStringExtra(
                                Constants.EXTRA_DESCRIPTOR_BYTE_VALUE_CHARACTERISTIC_UUID);
                        Logger.i("uuidRequired--"+uuidRequired+"receivedUUID "+receivedUUID);
                        byte[] array = intent
                                .getByteArrayExtra(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE);
                        if(uuidRequired.equalsIgnoreCase(receivedUUID)){
                            updateButtonStatus(array);
                        }

                    }
                 }
            }
            if(action.equals(BluetoothLeService.ACTION_GATT_CHARACTERISTIC_ERROR)){
                if(extras.containsKey(Constants.EXTRA_CHARACTERISTIC_ERROR_MESSAGE)){
                    String errorMessage=extras.
                            getString(Constants.EXTRA_CHARACTERISTIC_ERROR_MESSAGE);
                            displayAlertWithMessage(errorMessage);
                            mAsciivalue.setText("");
                            mHexValue.setText("");
                }

            }
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDING) {
                    // Bonding...
                    Logger.i("Bonding is in process....");
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, true);
                } else if (state == BluetoothDevice.BOND_BONDED) {
                    // Bonded...
                    String dataLog=getResources().getString(R.string.dl_commaseparator)
                            +"["+BluetoothLeService.getmBluetoothDeviceName()+"|"
                            +BluetoothLeService.getmBluetoothDeviceAddress()+"]"+
                            getResources().getString(R.string.dl_commaseparator)+
                            getResources().getString(R.string.dl_connection_paired);
                    Logger.datalog(dataLog);
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, false);
                    if (mIsIndicateEnabled) {
                        prepareBroadcastDataIndicate(mIndicateCharacteristic);
                    }
                    if (mIsNotifyEnabled) {
                        prepareBroadcastDataNotify(mNotifyCharacteristic);
                    }
                    if (mIsReadEnabled) {
                        prepareBroadcastDataRead(mReadCharacteristic);
                    }

                } else if (state == BluetoothDevice.BOND_NONE) {
                    String dataLog=getResources().getString(R.string.dl_commaseparator)
                            +"["+BluetoothLeService.getmBluetoothDeviceName()+"|"
                            +BluetoothLeService.getmBluetoothDeviceAddress()+"]"+
                            getResources().getString(R.string.dl_commaseparator)+
                            getResources().getString(R.string.dl_connection_unpaired);
                    Logger.datalog(dataLog);
                }
            }

        }

    };

    private void displayAlertWithMessage(String errorcode) {
        String errorMessage=getResources().getString(R.string.alert_message_write_error)+
                "\n"+getResources().getString(R.string.alert_message_write_error_code)+errorcode+
                "\n"+getResources().getString(R.string.alert_message_try_again);
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        TextView myMsg = new TextView(getActivity());
        myMsg.setText(errorMessage);
        myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
        builder.setView(myMsg);
        builder.setTitle(getActivity().getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setPositiveButton(
                        getActivity().getResources().getString(
                                R.string.alert_message_exit_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
        alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }


    public GattDetailsFragment create() {
        GattDetailsFragment fragment = new GattDetailsFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.gattdb_details, container,
                false);
        this.mContainer = container;
        mApplication = (CySmartApplication) getActivity().getApplication();
        mServiceName = (TextView) rootView.findViewById(R.id.txtservicename);
        mHexValue = (TextView) rootView.findViewById(R.id.txthex);
        mCharacteristiceName = (TextView) rootView
                .findViewById(R.id.txtcharatrname);
        mBtnnotify = (TextView) rootView.findViewById(R.id.txtnotify);
        mBtnIndicate = (TextView) rootView.findViewById(R.id.txtindicate);
        mBtnread = (TextView) rootView.findViewById(R.id.txtread);
        mBtnwrite = (TextView) rootView.findViewById(R.id.txtwrite);
        mAsciivalue = (TextView) rootView.findViewById(R.id.txtascii);
        mTimevalue = (TextView) rootView.findViewById(R.id.txttime);
        mDatevalue = (TextView) rootView.findViewById(R.id.txtdate);
        backbtn = (ImageView) rootView.findViewById(R.id.imgback);
        mProgressDialog = new ProgressDialog(getActivity());
        /**
         * Soft back button listner
         */
        backbtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();

            }
        });
        btn_descriptor = (Button) rootView.findViewById(R.id.characteristic_descriptors);
        if (mApplication.getBluetoothgattcharacteristic().getDescriptors().size() == 0) {
            btn_descriptor.setVisibility(View.GONE);
        }
        /**
         * Descriptor button listner
         */
        btn_descriptor.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                 * Passing the characteristic details to GattDetailsFragment and
                 * adding that fragment to the view
                 */
                Bundle bundle = new Bundle();
                bundle.putString(Constants.GATTDB_SELECTED_SERVICE,
                        mServiceName.getText().toString());
                bundle.putString(Constants.GATTDB_SELECTED_CHARACTERISTICE,
                        mCharacteristiceName.getText().toString());
                FragmentManager fragmentManager = getFragmentManager();
                GattDescriptorFragment gattDescriptorFragment = new GattDescriptorFragment()
                        .create();
                gattDescriptorFragment.setArguments(bundle);
                fragmentManager.beginTransaction()
                        .add(R.id.container, gattDescriptorFragment)
                        .addToBackStack(null).commit();
            }
        });
        RelativeLayout parent = (RelativeLayout) rootView
                .findViewById(R.id.parent);
        parent.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

            }
        });
        /**
         * button listeners
         */
        mBtnread.setOnClickListener(this);
        mBtnnotify.setOnClickListener(this);
        mBtnIndicate.setOnClickListener(this);
        mBtnwrite.setOnClickListener(this);

        mServiceName.setSelected(true);
        mCharacteristiceName.setSelected(true);
        mAsciivalue.setSelected(true);
        mHexValue.setSelected(true);

        // Getting the characteristics from the application
        mReadCharacteristic = mApplication.getBluetoothgattcharacteristic();
        mNotifyCharacteristic = mApplication.getBluetoothgattcharacteristic();
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mServiceName.setText(bundle
                    .getString(Constants.GATTDB_SELECTED_SERVICE));
            mCharacteristiceName.setText(bundle
                    .getString(Constants.GATTDB_SELECTED_CHARACTERISTICE));
        }
        startNotifyText=getResources().getString(R.string.gatt_services_notify);
        stopNotifyText=getResources().getString(R.string.gatt_services_stop_notify);
        startIndicateText=getResources().getString(R.string.gatt_services_indicate);
        stopIndicateText=getResources().getString(R.string.gatt_services_stop_indicate);
        UIbuttonvisibility();
        setHasOptionsMenu(true);
        /**
         * Check for HID Service
         */
        BluetoothGattService mBluetoothGattService=mReadCharacteristic.getService();
        if(mBluetoothGattService.getUuid().toString().
                equalsIgnoreCase(GattAttributes.HUMAN_INTERFACE_DEVICE_SERVICE)){
            showHIDWarningMessage();
        }
        Logger.i("Notification status---->"+mIsNotifyEnabled);
        return rootView;
    }
    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        mIsNotifyEnabled=false;
        mIsIndicateEnabled=false;
//        if (mIsNotifyEnabled) {
//            prepareBroadcastDataNotify(mNotifyCharacteristic);
//        }
//        if (mIsIndicateEnabled) {
//            prepareBroadcastDataIndicate(mIndicateCharacteristic);
//        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mGattUpdateReceiver);
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
     * Method to make the Buttons visible to the user
     */
    private void UIbuttonvisibility() {
        // Getting the properties of each characteristics
        boolean read = false, write = false, notify = false, indicate = false;
        if (getGattCharacteristicsPropertices(
                mReadCharacteristic.getProperties(),
                BluetoothGattCharacteristic.PROPERTY_READ)) {
            // Read property available
            read = true;
            mBtnread.setVisibility(View.VISIBLE);
        }
        if (getGattCharacteristicsPropertices(
                mReadCharacteristic.getProperties(),
                BluetoothGattCharacteristic.PROPERTY_WRITE)
                | getGattCharacteristicsPropertices(
                mReadCharacteristic.getProperties(),
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
            // Write property available
            write = true;
            mBtnwrite.setVisibility(View.VISIBLE);
        }
        if (getGattCharacteristicsPropertices(
                mReadCharacteristic.getProperties(),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
            // Notify property available
            notify = true;
            mBtnnotify.setVisibility(View.VISIBLE);
            BluetoothGattDescriptor descriptor=mReadCharacteristic.
                    getDescriptor(UUIDDatabase.UUID_CLIENT_CHARACTERISTIC_CONFIG);
            if(descriptor!=null){
                BluetoothLeService.readDescriptor(descriptor);
            }
        }
        if (getGattCharacteristicsPropertices(
                mReadCharacteristic.getProperties(),
                BluetoothGattCharacteristic.PROPERTY_INDICATE)) {
            // Indicate property available
            indicate = true;
            mBtnIndicate.setVisibility(View.VISIBLE);
            BluetoothGattDescriptor descriptor=mReadCharacteristic.
                    getDescriptor(UUIDDatabase.UUID_CLIENT_CHARACTERISTIC_CONFIG);
            if(descriptor!=null){
                BluetoothLeService.readDescriptor(descriptor);
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
        final BluetoothGattCharacteristic characteristic = gattCharacteristic;
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            mReadCharacteristic = characteristic;
            BluetoothLeService.readCharacteristic(characteristic);

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
     * Preparing Broadcast receiver to broadcast indicate characteristics
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataIndicate(
            BluetoothGattCharacteristic gattCharacteristic) {
        final BluetoothGattCharacteristic characteristic = gattCharacteristic;
        final int charaProp = characteristic.getProperties();

        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            mIndicateCharacteristic = characteristic;
            BluetoothLeService.setCharacteristicIndication(
                    mIndicateCharacteristic, true);

        }


    }

    /**
     * Stopping Broadcast receiver to broadcast indicate characteristics
     *
     * @param gattCharacteristic
     */
    void stopBroadcastDataIndicate(
            BluetoothGattCharacteristic gattCharacteristic) {
        final BluetoothGattCharacteristic characteristic = gattCharacteristic;
        final int charaProp = characteristic.getProperties();

        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            if (mIndicateCharacteristic != null) {
                BluetoothLeService.setCharacteristicIndication(
                        mIndicateCharacteristic, false);
            }

        }

    }

    /**
     * Method to display the ASCII Value
     *
     * @param hexValue
     */
    void displayASCIIValue(String hexValue) {

//        StringBuffer sb = new StringBuffer();
//        for (byte byteChar : array) {
//            if (byteChar >= 32 && byteChar < 127) {
//                sb.append(String.format("%c", byteChar));
//            } else {
//                sb.append(String.format("%d ", byteChar & 0xFF)); // to convert
//                // >127 to
//                // positive
//                // value
//            }
//        }
        StringBuilder output = new StringBuilder("");
        try {
            for (int i = 0; i < hexValue.length(); i += 2)
            {
                String str = hexValue.substring(i, i + 2);
                output.append((char) Integer.parseInt(str, 16));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mAsciivalue.setText(output.toString());
    }

    /**
     * Method to display the hexValue
     *
     * @param array
     */
    void displayHexValue(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (byte byteChar : array) {
            sb.append(String.format("%02x", byteChar));
        }
        mHexValue.setText( sb.toString());
    }

    /**
     * Method to display time and date
     */
    private void displayTimeandDate() {

        mTimevalue.setText(Utils.GetTimeFromMilliseconds());
        mDatevalue.setText(Utils.GetDateFromMilliseconds());
    }

    /**
     * Clearing all fields
     */
    private void clearall() {
        mTimevalue.setText("");
        mDatevalue.setText("");
        mAsciivalue.setText("");
        mHexValue.setText("");
    }

    /**
     * Return the property enabled in the characteristic
     *
     * @param characteristics
     * @param characteristicsSearch
     * @return
     */
    boolean getGattCharacteristicsPropertices(int characteristics,
                                              int characteristicsSearch) {

        return (characteristics & characteristicsSearch) == characteristicsSearch;

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txtwrite:
                HexKeyBoard hexKeyBoard = new HexKeyBoard(getActivity(), mReadCharacteristic, true);
                hexKeyBoard.setDialogListner(this);
                hexKeyBoard.show();
                break;

            case R.id.txtread:
                prepareBroadcastDataRead(mReadCharacteristic);
                mIsReadEnabled = true;
                break;

            case R.id.txtnotify:
                mNotifyCharacteristic = mApplication
                        .getBluetoothgattcharacteristic();
                TextView clickedNotifyText = (TextView) v;
                String buttonNotifyText = clickedNotifyText.getText().toString();
                if (buttonNotifyText.equalsIgnoreCase(startNotifyText)) {
                    prepareBroadcastDataNotify(mNotifyCharacteristic);
                    mBtnnotify.setText(stopNotifyText);
                    mIsNotifyEnabled = true;
                } else if (buttonNotifyText.equalsIgnoreCase(stopNotifyText)) {
                    stopBroadcastDataNotify(mNotifyCharacteristic);
                    mBtnnotify
                            .setText(startNotifyText);
                    mIsNotifyEnabled = false;
                }
                break;

            case R.id.txtindicate:
                TextView clickedIndicateText = (TextView) v;
                String buttonIndicateText = clickedIndicateText.getText().toString();
                mIndicateCharacteristic = mApplication.getBluetoothgattcharacteristic();
                if (mIndicateCharacteristic != null) {
                    if (buttonIndicateText.equalsIgnoreCase(startIndicateText)) {
                        prepareBroadcastDataIndicate(mIndicateCharacteristic);
                        mBtnIndicate
                                .setText(stopIndicateText);
                        mIsIndicateEnabled = true;

                    } else if (buttonIndicateText.equalsIgnoreCase(stopIndicateText)) {
                        stopBroadcastDataIndicate(mIndicateCharacteristic);
                        mBtnIndicate
                                .setText(startIndicateText);
                        mIsIndicateEnabled = false;
                    }
                }
                break;
        }
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

    @Override
    public void dialog0kPressed(String result) {
        displayTimeandDate();
        byte[] convertedBytes = convertingTobyteArray(result);
        // Displaying the hex and ASCII values
        displayHexValue(convertedBytes);
        displayASCIIValue(mHexValue.getText().toString());

        // Writing the hexValue to the characteristics
        try {
            BluetoothLeService.writeCharacteristicGattDb(mReadCharacteristic,
                    convertedBytes);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dialogCancelPressed(Boolean aBoolean) {

    }
    private void updateButtonStatus(byte[] array) {
        Logger.i("Updatig the buttons");
        int status=array[0];
        switch (status) {
            case 0:
                if(mBtnnotify.getVisibility()==View.VISIBLE)
                    mBtnnotify.setText(startNotifyText);

                if(mBtnIndicate.getVisibility()==View.VISIBLE)
                    mBtnIndicate.setText(startIndicateText);
                break;
            case 1:
                if(mBtnnotify.getVisibility()==View.VISIBLE)
                    mBtnnotify.setText(stopNotifyText);
                break;
            case 2:
                if(mBtnIndicate.getVisibility()==View.VISIBLE)
                    mBtnIndicate.setText(stopIndicateText);
                break;
        }
    }
    void showHIDWarningMessage() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getActivity());
        // set title
        alertDialogBuilder
                .setTitle(R.string.app_name);
        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.alert_message_hid_warning)
                .setCancelable(false)
                .setPositiveButton(R.string.alert_message_exit_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
