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
package com.cypress.cysmart.RDKEmulatorView;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

/**
 * Fragment class to show the emulator view of the Remote control RDK which has Human Interface
 * Device sservice
 */
public class TrackpadEmulatorFragment extends Fragment {


    int mouse_ZZ_value = 0;
    int mouse_tilt_value = 0;
    //UI Elements
    private TextView mXValue;
    private TextView mYValue;
    private TextView mZValue;
    private TextView mTiltValue;
    private TextView mLeftDownValue;
    private TextView mLeftUpValue;
    private TextView mRightDownValue;
    private TextView mRightUpValue;
    private TextView mKeycodeValue;
    //Temporary Variables
    private int lefty = 0;
    private int righty = 0;
    private int leftyUp = 0;
    private int rightyUp = 0;
    private boolean leftClicked = false;
    private boolean rightClicked = false;
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
                /**
                 * Byte information send through BLE received here
                 */
                if (extras.containsKey(Constants.EXTRA_BYTE_VALUE)) {
                    byte[] array = intent
                            .getByteArrayExtra(Constants.EXTRA_BYTE_VALUE);
                    /**
                     * Report reference descriptor received
                     */
                    if (extras.containsKey(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID)) {
                        String reportReference = intent.getStringExtra
                                (Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID);
                        /**
                         * Mouse report reference data received
                         */
                        if (reportReference.equalsIgnoreCase(ReportAttributes.
                                MOUSE_REPORT_REFERENCE_STRING)) {
                            /**
                             * Update the value in the UI
                             */
                            displayData(array);
                        }
                        /**
                         * Keyboard report reference data received
                         */
                        else if (reportReference.equalsIgnoreCase(ReportAttributes.
                                KEYBOARD_REPORT_REFERENCE_STRING)) {
                            Logger.e("KEYBOARD_KEYCODE");
                            displayKeycode(array);
                        } else {
                            /**
                             * Converting the received voice data to HEX value
                             * Update the value in the UI
                             */
                            String hexValue = getHexValue(array);
                            updateKeyCodeValues(hexValue);
                        }
                    }


                }
            }
        }

    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.rdk_motion_sensor, container,
                false);
        /**
         * Getting the ID'S of the GUI elements
         */
        mXValue = (TextView) rootView.findViewById(R.id.x_value);
        mYValue = (TextView) rootView.findViewById(R.id.y_value);
        mZValue = (TextView) rootView.findViewById(R.id.z_wheel_value);
        mTiltValue = (TextView) rootView.findViewById(R.id.tilt_value);
        mLeftDownValue = (TextView) rootView.findViewById(R.id.left_click_down_value);
        mLeftUpValue = (TextView) rootView.findViewById(R.id.left_click_upp_value);
        mRightDownValue = (TextView) rootView.findViewById(R.id.right_click_down_value);
        mRightUpValue = (TextView) rootView.findViewById(R.id.right_click_up_value);
        mKeycodeValue = (TextView) rootView.findViewById(R.id.keycode_value);
        Button mClearCountersBtn = (Button) rootView.findViewById(R.id.clear_counters);
        /**
         * Clear counters button click listner
         * Replacing all value in the GUI with 00
         */
        mClearCountersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLeftDownValue.setText("00");
                mLeftUpValue.setText("00");
                mRightDownValue.setText("00");
                mRightUpValue.setText("00");
                mKeycodeValue.setText("00");
                mZValue.setText("00");
                mTiltValue.setText("00");
                mXValue.setText("00");
                mYValue.setText("00");
                lefty = 0;
                righty = 0;
                leftyUp = 0;
                rightyUp = 0;
                mouse_tilt_value = 0;
                mouse_ZZ_value = 0;
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.rdk_emulator_view));
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        super.onDestroy();
    }

    /**
     * Method to get the Hex value from bytes
     *
     * @param bytes
     * @return String
     */
    private String getHexValueByte(byte bytes) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%02x", bytes));
        return "" + sb;
    }

    /**
     * Method to update the GUI page with received values through BLE
     *
     * @param trackpadValues
     */
    private void displayData(byte[] trackpadValues) {
        String mouse_XX_value = getHexValueByte(trackpadValues[1]);
        mXValue.setText("" + trackpadValues[1]);
        String mouse_YY_value = getHexValueByte(trackpadValues[2]);
        mYValue.setText("" + trackpadValues[2]);
        int mouse_ZZ_value_temp = trackpadValues[3];
        mouse_ZZ_value = mouse_ZZ_value_temp + mouse_ZZ_value ;
        mZValue.setText("" + mouse_ZZ_value);
        int mouse_tilt_value_temp = trackpadValues[4];
        mouse_tilt_value = mouse_tilt_value_temp + mouse_tilt_value;
        mTiltValue.setText("" + mouse_tilt_value);
        if (trackpadValues[0] == 1) {
            if(!leftClicked){
                lefty++;
                mLeftDownValue.setText("" + lefty);
                leftClicked = true;
                rightClicked = false;
            }
        }
       else if (trackpadValues[0] == 2) {
            if(!rightClicked){
                righty++;
                mRightDownValue.setText("" + righty);
                rightClicked = true;
                leftClicked = false;
            }
        }
       else if (trackpadValues[0] == 0) {
            if (leftClicked) {
                leftyUp++;
                mLeftUpValue.setText("" + leftyUp);
                leftClicked = false;
            } else if (rightClicked) {
                rightyUp++;
                mRightUpValue.setText("" + rightyUp);
                rightClicked = false;
            }
        }
    }

    private void displayKeycode(byte[] keycodeReceived) {
        StringBuilder keyCodeStringBuilder = new StringBuilder();
        for (int pos = 0; pos < keycodeReceived.length; pos++) {
            int bytevalue = keycodeReceived[pos];
            if (pos == 0) {
                for (int count = 0; count < 8; count++) {
                    if (isSet((byte) bytevalue, count)) {
                        keyCodeStringBuilder.append(getModifierValue(count));
                    }
                }
            } else {
                if (bytevalue != 0) {
                    String value = KeyBoardAttributes.lookupKeycodeDescription(bytevalue & 0xFF);
                    keyCodeStringBuilder.append(value);
                }
            }
        }

        if (keyCodeStringBuilder.toString() != "") {
            mKeycodeValue.setText(keyCodeStringBuilder);
        }
    }

    private String getModifierValue(int count) {
        String modifier = "";
        switch (count) {
            case 0:
                modifier = " Keyboard LeftControl ";
                break;
            case 1:
                modifier = " Keyboard LeftShift ";
                break;
            case 2:
                modifier = " Keyboard LeftAlt ";
                break;
            case 3:
                modifier = " Keyboard Left GUI ";
                break;
            case 4:
                modifier = " Keyboard RightControl ";
                break;
            case 5:
                modifier = " Keyboard RightShift ";
                break;
            case 6:
                modifier = " Keyboard RightAlt ";
                break;
            case 7:
                modifier = " Keyboard Right GUI ";
                break;
            default:
                break;
        }
        return modifier;
    }


    /**
     * Method to update the GUI with the corresponding report received
     *
     * @param buttonValue
     */

    private void updateKeyCodeValues(String buttonValue) {
        int value = ReportAttributes.lookupReportValues(buttonValue);
        switch (value) {
            case 101:
                mKeycodeValue.setText(getResources().getString(R.string.power_on));
                break;
            case 102:
                mKeycodeValue.setText(getResources().getString(R.string.volume_up));
                break;
            case 103:
                mKeycodeValue.setText(getResources().getString(R.string.volume_down));
                break;
            case 104:
                mKeycodeValue.setText(getResources().getString(R.string.channel_up));
                break;
            case 105:
                mKeycodeValue.setText(getResources().getString(R.string.channel_down));
                break;
            case 110:
                mKeycodeValue.setText(getResources().getString(R.string.source));
                break;
            default:
                break;
        }
    }

    private String getHexValue(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (byte byteChar : array) {
            sb.append(String.format("%02x", byteChar));
        }
        return "" + sb;
    }
    // tests if bit is set in value
    boolean isSet(byte value, int bit) {
        return (value & (1 << bit)) != 0;
    }
}

