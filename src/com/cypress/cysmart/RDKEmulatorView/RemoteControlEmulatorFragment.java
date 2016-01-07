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


import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.UUIDDatabase;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * Fragment class to show the emulator view of the Remote control RDK which has Human Interface
 * Device sservice
 */
public class RemoteControlEmulatorFragment extends Fragment {

    //Flags
    public static boolean mIsrecording = false;
    // GATT service and characteristics
    private static BluetoothGattService mservice;
    private final ArrayList<ReportCharacteristicDescriptionModel> charaDataModel = new
            ArrayList<ReportCharacteristicDescriptionModel>();
    //  Handler flag
    private boolean HANDLER_FLAG = true;
    private boolean mNotiificationsEnabled=false;
    //UI Elements
    private ProgressDialog mProgressDialog;
    //Remote control emulator buttons
    private ImageButton mVolumePlusbtn;
    private ImageButton mVolumeMinusBtn;
    private ImageButton mChannelPlusBtn;
    private ImageButton mChannelMinusBtn;
    private ImageButton mLeftBtn;
    private ImageButton mRightBtn;
    private ImageButton mBackBtn;
    private ImageButton mGesturebtn;
    private ImageButton mExitBtn;
    private ImageButton mPowerBtn;
    private ImageButton mRecBtn;
    //PCM data
    private byte[] mPCMData;
    //Constants
    private int SAMPLE_RATE = 16000;
    private int BUFFER_SIZE=8000;
    private int HANDLER_DELAY = 1500;
    //AudioTrack
    private AudioTrack mAudioTrack;
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
                     * Converting the received voice data to HEX value
                     * Update the value in the UI
                     */
                    String hexValue = getHexValue(array);
                    updateEmulatorView(hexValue);
                    /**
                     * Report reference descriptor received
                     */
                    if (extras.containsKey(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID)) {
                        String reportReference = intent.getStringExtra
                                (Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID);
                        /**
                         * Audio report reference control received
                         */
                        if (reportReference.equalsIgnoreCase(ReportAttributes.
                                AUDIO_REPORT_REFERENCE_CONTROL_STRING)) {

                            /**
                             * Extracting the first byte to verify
                             * sync is required
                             */
                            String firstByte = hexValue.substring(0, 2);
                            if (firstByte.equalsIgnoreCase(ReportAttributes.MICROPHONE_SYNC)) {
                                /**
                                 * Sync required
                                 * Updating the ADPCMStateModel values
                                 */
                                if (hexValue.length() == 8) {
                                    ADPCMStateModel.prevIndex = array[1];
                                    ADPCMStateModel.prevSample = ((int) array[2] << 8);
                                    ADPCMStateModel.prevSample |= array[3];
                                }
                            }


                        }
                        /**
                         * Audio report reference data received
                         */
                        if (reportReference.equalsIgnoreCase(ReportAttributes.
                                AUDIO_REPORT_REFERENCE_DATA_STRING)) {
                            /**
                             * Getting the converted PCM Data
                             * writing the byte data to AudioTrack class
                             */
                            mPCMData = ADPCMConverter.getPCMData(array);
                            mAudioTrack.write(mPCMData, 0, mPCMData.length);
                            if(mIsrecording){
                                createPCMFile(mPCMData);
                            }

                        }

                    }
                }

            }
            /**
             * Bonding Action is in Process
             * Update the GUI with a Progress dialog
             * during bonding
             */
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDING) {
                    // Bonding...
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, true);
                } else if (state == BluetoothDevice.BOND_BONDED) {
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + BluetoothLeService.getmBluetoothDeviceName() + "|"
                            + BluetoothLeService.getmBluetoothDeviceAddress() + "]" +
                            getResources().getString(R.string.dl_commaseparator) +
                            getResources().getString(R.string.dl_connection_paired);
                    Logger.datalog(dataLog);
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, false);
                    getAllCharacteristicReportReference();
                } else if (state == BluetoothDevice.BOND_NONE) {
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + BluetoothLeService.getmBluetoothDeviceName() + "|"
                            + BluetoothLeService.getmBluetoothDeviceAddress() + "]" +
                            getResources().getString(R.string.dl_commaseparator) +
                            getResources().getString(R.string.dl_connection_unpaired);
                    Logger.datalog(dataLog);
                }
            }
        }


    };
    //View
    private View mParentView;

    /**
     * Constructor
     *
     * @param bluetoothGattService
     * @return RemoteControlEmulatorService
     */
    public RemoteControlEmulatorFragment create(BluetoothGattService bluetoothGattService) {
        mservice = bluetoothGattService;
        RemoteControlEmulatorFragment remoteControlEmulatorFragment = new
                RemoteControlEmulatorFragment();
        return remoteControlEmulatorFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /**
         * Getting the current orientation of the screen
         * Loading different view for LandScape and portrait
         */
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mParentView = inflater.inflate(R.layout.rdk_emulator_view_landscape, container,
                    false);
        } else {
            mParentView = inflater.inflate(R.layout.rdk_emulator_view_portrait, container,
                    false);
        }
        mProgressDialog = new ProgressDialog(getActivity());
        /**
         * Getting the ID's of all Emulator view UI elements
         */
        Button mTrackpadView = (Button) mParentView.findViewById(R.id.trackpad_btn);
        Button mMicrophoneView = (Button) mParentView.findViewById(R.id.microphone_btn);
        mVolumePlusbtn = (ImageButton) mParentView.findViewById(R.id.volume_plus_btn);
        mVolumeMinusBtn = (ImageButton) mParentView.findViewById(R.id.volume_minus_btn);
        mChannelPlusBtn = (ImageButton) mParentView.findViewById(R.id.channel_plus_btn);
        mChannelMinusBtn = (ImageButton) mParentView.findViewById(R.id.channel_minus_btn);
        mLeftBtn = (ImageButton) mParentView.findViewById(R.id.left_btn);
        mRightBtn = (ImageButton) mParentView.findViewById(R.id.right_btn);
        mBackBtn = (ImageButton) mParentView.findViewById(R.id.back_btn);
        mGesturebtn = (ImageButton) mParentView.findViewById(R.id.gesture_btn);
        mExitBtn = (ImageButton) mParentView.findViewById(R.id.exit_btn);
        mPowerBtn = (ImageButton) mParentView.findViewById(R.id.power_btn);
        mRecBtn = (ImageButton) mParentView.findViewById(R.id.record_btn);
        mRecBtn = (ImageButton) mParentView.findViewById(R.id.record_btn);
        /**
         * AudioTrack class initialisation as follows
         *  streamType- AudioManager.STREAM_MUSIC,
         *  sampleRateInHz- 16000,
         *  channelConfig- AudioFormat.CHANNEL_OUT_MONO,
         *  audioFormat-AudioFormat.ENCODING_PCM_16BIT,
         *  bufferSizeInBytes-8000,
         *  mode- AudioTrack.MODE_STREAM
         *
         */
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE,
                AudioTrack.MODE_STREAM);
        /**
         * TrackPAd button click listner
         */
        mTrackpadView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TrackpadEmulatorFragment trackpadService = new TrackpadEmulatorFragment();
                try {
                    displayView(trackpadService);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        /**
         * Microphone Button click listner
         */
        mMicrophoneView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MicrophoneEmulatorFragment microphoneService = new MicrophoneEmulatorFragment();
                microphoneService.create(mservice);
                displayView(microphoneService);
            }
        });
        return mParentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        HANDLER_FLAG = true;
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.rdk_emulator_view));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(mGattUpdateReceiver, intentFilter);
        initializeBondingIFnotBonded();
    }

    @Override
    public void onPause() {
        HANDLER_FLAG = false;
         super.onPause();
    }

    @Override
    public void onDestroy() {
        mNotiificationsEnabled=false;
        stopBroadcastAllNotifications();
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        super.onDestroy();
    }

    private void initializeBondingIFnotBonded() {
        Logger.i("Bonding check");
        final BluetoothDevice device = BluetoothLeService.mBluetoothAdapter
                .getRemoteDevice(BluetoothLeService.getmBluetoothDeviceAddress());
        if (!BluetoothLeService.getBondedState()) {
            pairDevice(device);

        } else if(!mNotiificationsEnabled){
            getAllCharacteristicReportReference();
        }
    }

    //For Pairing
    private void pairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    /**
     * Method to get all Characteristic with report reference
     */
    private void getAllCharacteristicReportReference() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mservice
                .getCharacteristics();
        charaDataModel.clear();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();
            if (uuidchara.equalsIgnoreCase(GattAttributes.REP0RT)) {
                ReportCharacteristicDescriptionModel data = new ReportCharacteristicDescriptionModel(
                        gattCharacteristic, gattCharacteristic.getInstanceId());
                charaDataModel.add(data);
            }
        }
        prepareBroadcastAllNotifications();

    }

    /**
     * prepare notifications of all Report characteristic found from the service
     */
    private void prepareBroadcastAllNotifications() {
        mProgressDialog.setTitle(getResources().getString(
                R.string.alert_message_prepare_title));
        mProgressDialog.setMessage(getResources().getString(
                R.string.alert_message_prepare_message)
                + "\n"
                + getResources().getString(R.string.alert_message_wait));
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        for (int pos = 0; pos < charaDataModel.size(); pos++) {
            final BluetoothGattCharacteristic characteristic =
                    charaDataModel.get(pos).getmGattCharacteristic();
            if (characteristic.
                    getDescriptor(UUIDDatabase.UUID_REPORT_REFERENCE) != null) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (HANDLER_FLAG) {
                            BluetoothLeService.setCharacteristicNotification(characteristic,
                                    true);
                        }
                    }
                }, HANDLER_DELAY * pos);

            }
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (HANDLER_FLAG) {
                    mProgressDialog.dismiss();
                    getActivity().registerReceiver(mGattUpdateReceiver,
                            Utils.makeGattUpdateIntentFilter());
                    mNotiificationsEnabled = true;
                }
            }
        }, HANDLER_DELAY * 6);

    }

    /**
     * stop notifications of all Report characteristc
     */
    private void stopBroadcastAllNotifications() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mservice
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();
            if (uuidchara.equalsIgnoreCase(GattAttributes.REP0RT)) {
                final int charaProp = gattCharacteristic.getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    BluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                            false);

                }
            }
        }
    }

    /**
     * Method to update the GUI with the corresponding report received
     *
     * @param buttonValue
     */

    private void updateEmulatorView(String buttonValue) {
        int value = ReportAttributes.lookupReportValues(buttonValue);
        switch (value) {
            case 101:
                mPowerBtn.setPressed(true);
                break;
            case 102:
                mVolumePlusbtn.setPressed(true);
                break;
            case 103:
                mVolumeMinusBtn.setPressed(true);
                break;
            case 104:
                mChannelPlusBtn.setPressed(true);
                break;
            case 105:
                mChannelMinusBtn.setPressed(true);
                break;
            case 106:
                mRecBtn.setPressed(true);
                mAudioTrack.play();
                break;
            case 107:
                mLeftBtn.setPressed(true);
                break;
            case 108:
                mRightBtn.setPressed(true);
                break;
            case 109:
                mBackBtn.setPressed(true);
                break;
            case 110:
                mExitBtn.setPressed(true);
                break;
            case 201:
                mRecBtn.setPressed(false);
                mAudioTrack.stop();
                break;
            case 202:
                mLeftBtn.setPressed(false);
                mRightBtn.setPressed(false);
                break;
            case 203:
                mBackBtn.setPressed(false);
                break;
            default:
                mPowerBtn.setPressed(false);
                mVolumePlusbtn.setPressed(false);
                mVolumeMinusBtn.setPressed(false);
                mChannelPlusBtn.setPressed(false);
                mChannelMinusBtn.setPressed(false);
                mLeftBtn.setPressed(false);
                mRightBtn.setPressed(false);
                mBackBtn.setPressed(false);
                mExitBtn.setPressed(false);
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


    /**
     * Used for replacing the main content of the view with provided fragments
     *
     * @param fragment
     */
    void displayView(Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().add(R.id.container, fragment)
                .addToBackStack(null).commit();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LayoutInflater inflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mParentView = inflater.inflate(R.layout.rgb_view_landscape, null);
            ViewGroup rootViewG = (ViewGroup) getView();
            // Remove all the existing views from the root view.
            try {
                assert rootViewG != null;
                rootViewG.removeAllViews();
                rootViewG.addView(mParentView);
            } catch (Exception e) {
                e.printStackTrace();
            }


        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            LayoutInflater inflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mParentView = inflater.inflate(R.layout.rgb_view_portrait, null);
            ViewGroup rootViewG = (ViewGroup) getView();
            // Remove all the existing views from the root view.
            try {
                assert rootViewG != null;
                rootViewG.removeAllViews();
                rootViewG.addView(mParentView);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
    void createPCMFile(byte[] data){
        String filename=MicrophoneEmulatorFragment.mfilePCM;
        FileOutputStream output;
        try {
            output = new FileOutputStream(filename, true);
            output.write(data);
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
