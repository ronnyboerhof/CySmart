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
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import java.util.List;

/**
 * Created by Ronny on 19-11-2015.
 */
public class IridiumMessageService extends Fragment {
    // Service and characteristics
    private static BluetoothGattService mService;
    private static BluetoothGattCharacteristic mIndicateCharacteristic;

    // Data fields
    private TextView mIridiumBurstText1;
    private TextView mIridiumBurstText2;
    private TextView mIridiumBurstText3;
    private TextView mIridiumBurstText4;
    private TextView mIridiumBurstText5;

    private ProgressDialog mProgressDialog;

    private String mTextMessage;

    private static boolean mBurstText1Set = false;
    private static boolean mBurstText2Set = false;
    private static boolean mBurstText3Set = false;
    private static boolean mBurstText4Set = false;
    private static boolean mBurstText5Set = false;

    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();

            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (extras.containsKey(Constants.EXTRA_IRIDIUM_MESSAGE_STATUS_VALUE)) {
                    String received_status = intent.
                            getStringExtra(Constants.EXTRA_IRIDIUM_MESSAGE_STATUS_VALUE);
                }

                if (extras.containsKey(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT1_VALUE)) {
                    String received_text_msg = intent.
                            getStringExtra(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT1_VALUE);
                    if (!received_text_msg.equalsIgnoreCase(" ")) {
                        if (!mBurstText1Set) {
                            if (received_text_msg.charAt(0) > 19) {
                                mTextMessage = mTextMessage + received_text_msg.substring(1);
                                getGattData(1);
                            } else {
                                mTextMessage = mTextMessage + received_text_msg.substring(1, (received_text_msg.charAt(0) + 1));
                                displayIridiumBurstMessage(mTextMessage, 1);
                                mBurstText1Set = true;
                                mTextMessage = "";

                                getGattData(2);
                            }
                        }
                    }
                }
                if (extras.containsKey(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT2_VALUE)) {
                    String received_text_msg = intent.
                            getStringExtra(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT2_VALUE);
                    if (!received_text_msg.equalsIgnoreCase(" ")) {
                        if (!mBurstText2Set) {
                            if (received_text_msg.charAt(0) > 19) {
                                mTextMessage = mTextMessage + received_text_msg.substring(1);
                                getGattData(2);
                            } else {
                                mTextMessage = mTextMessage + received_text_msg.substring(1, (received_text_msg.charAt(0) + 1));
                                displayIridiumBurstMessage(mTextMessage, 2);
                                mBurstText2Set = true;
                                mTextMessage = "";

                                getGattData(3);
                            }
                        }
                    }
                }
                if (extras.containsKey(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT3_VALUE)) {
                    String received_text_msg = intent.
                            getStringExtra(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT3_VALUE);
                    if (!received_text_msg.equalsIgnoreCase(" ")) {
                        if (!mBurstText3Set) {
                            if (received_text_msg.charAt(0) > 19) {
                                mTextMessage = mTextMessage + received_text_msg.substring(1);
                                getGattData(3);
                            } else {
                                mTextMessage = mTextMessage + received_text_msg.substring(1, (received_text_msg.charAt(0) + 1));
                                displayIridiumBurstMessage(mTextMessage, 3);
                                mBurstText3Set = true;
                                mTextMessage = "";

                                getGattData(4);
                            }
                        }
                    }
                }
                if (extras.containsKey(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT4_VALUE)) {
                    String received_text_msg = intent.
                            getStringExtra(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT4_VALUE);
                    if (!received_text_msg.equalsIgnoreCase(" ")) {
                        if (!mBurstText4Set) {
                            if (received_text_msg.charAt(0) > 19) {
                                mTextMessage = mTextMessage + received_text_msg.substring(1);
                                getGattData(4);
                            } else {
                                mTextMessage = mTextMessage + received_text_msg.substring(1, (received_text_msg.charAt(0) + 1));
                                displayIridiumBurstMessage(mTextMessage, 4);
                                mBurstText4Set = true;
                                mTextMessage = "";

                                getGattData(5);
                            }
                        }
                    }
                }
                if (extras.containsKey(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT5_VALUE)) {
                    String received_text_msg = intent.
                            getStringExtra(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT5_VALUE);
                    if (!received_text_msg.equalsIgnoreCase(" ")) {
                        if (!mBurstText5Set) {
                            if (received_text_msg.charAt(0) > 19) {
                                mTextMessage = mTextMessage + received_text_msg.substring(1);
                                getGattData(5);
                            } else {
                                mTextMessage = mTextMessage + received_text_msg.substring(1, (received_text_msg.charAt(0) + 1));
                                displayIridiumBurstMessage(mTextMessage, 5);
                                mBurstText5Set = true;
                                mTextMessage = "";
                            }
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
                    getGattData(1);

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

    public IridiumMessageService create(BluetoothGattService service) {
        IridiumMessageService fragment = new IridiumMessageService();
        mService = service;
        return fragment;
    }

    void displayIridiumBurstMessage(String received_mns_data, int cnt){
        switch (cnt)
        {
            case 1:
                mIridiumBurstText1.setText(received_mns_data);
                break;
            case 2:
                mIridiumBurstText2.setText(received_mns_data);
                break;
            case 3:
                mIridiumBurstText3.setText(received_mns_data);
                break;
            case 4:
                mIridiumBurstText4.setText(received_mns_data);
                break;
            case 5:
                mIridiumBurstText5.setText(received_mns_data);
                break;
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.iridium_messages, container, false);
        mIridiumBurstText1 = (TextView) rootView
                .findViewById(R.id.iridium_burst_text1);
        mIridiumBurstText2 = (TextView) rootView
                .findViewById(R.id.iridium_burst_text2);
        mIridiumBurstText3 = (TextView) rootView
                .findViewById(R.id.iridium_burst_text3);
        mIridiumBurstText4 = (TextView) rootView
                .findViewById(R.id.iridium_burst_text4);
        mIridiumBurstText5 = (TextView) rootView
                .findViewById(R.id.iridium_burst_text5);
        getActivity().getActionBar().setTitle(R.string.iridium_messages);
        mProgressDialog = new ProgressDialog(getActivity());
        setHasOptionsMenu(true);
        return rootView;
    }

    void prepareBroadcastDataRead(
            BluetoothGattCharacteristic gattCharacteristic) {
        final BluetoothGattCharacteristic characteristic = gattCharacteristic;
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            mIndicateCharacteristic = characteristic;
            BluetoothLeService.readCharacteristic(characteristic);
        }
    }

    @Override
    public void onResume() {
        makeDefaultBooleans();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        clearUI();
/*        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.iridium_burst));*/
        mTextMessage = "";
        getGattData(1);
        super.onResume();
    }

    /**
     * clear all data fields
     */
    private void clearUI() {
        mIridiumBurstText1.setText("");
        mIridiumBurstText2.setText("");
        mIridiumBurstText3.setText("");
        mIridiumBurstText4.setText("");
        mIridiumBurstText5.setText("");
    }

    /**
     * Flag up default
     */
    private void makeDefaultBooleans() {
        mBurstText1Set = false;
        mBurstText2Set = false;
        mBurstText3Set = false;
        mBurstText4Set = false;
        mBurstText5Set = false;
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        super.onPause();
    }

    void getGattData(int var) {
        List<BluetoothGattCharacteristic> gattCharacteristics = mService
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();
            switch (var) {
                case 1:
                    if (uuidchara
                            .equalsIgnoreCase(GattAttributes.IRIDIUM_MESSAGE_TEXT1)) {
                        Logger.i("Characteristic div" + uuidchara);
                        mIndicateCharacteristic = gattCharacteristic;
                        prepareBroadcastDataRead(gattCharacteristic);
                        break;
                    }
                break;
                case 2:
                    if (uuidchara
                            .equalsIgnoreCase(GattAttributes.IRIDIUM_MESSAGE_TEXT2)) {
                        Logger.i("Characteristic div" + uuidchara);
                        mIndicateCharacteristic = gattCharacteristic;
                        prepareBroadcastDataRead(gattCharacteristic);
                        break;
                    }
                    break;
                case 3:
                    if (uuidchara
                            .equalsIgnoreCase(GattAttributes.IRIDIUM_MESSAGE_TEXT3)) {
                        Logger.i("Characteristic div" + uuidchara);
                        mIndicateCharacteristic = gattCharacteristic;
                        prepareBroadcastDataRead(gattCharacteristic);
                        break;
                    }
                    break;
                case 4:
                    if (uuidchara
                            .equalsIgnoreCase(GattAttributes.IRIDIUM_MESSAGE_TEXT4)) {
                        Logger.i("Characteristic div" + uuidchara);
                        mIndicateCharacteristic = gattCharacteristic;
                        prepareBroadcastDataRead(gattCharacteristic);
                        break;
                    }
                    break;
                case 5:
                    if (uuidchara
                            .equalsIgnoreCase(GattAttributes.IRIDIUM_MESSAGE_TEXT5)) {
                        Logger.i("Characteristic div" + uuidchara);
                        mIndicateCharacteristic = gattCharacteristic;
                        prepareBroadcastDataRead(gattCharacteristic);
                        break;
                    }
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
