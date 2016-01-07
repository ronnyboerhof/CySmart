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
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import java.util.List;

/**
 * Created by Ronny on 9-11-2015.
 */
public class IridiumBurstService extends Fragment {
    // Service and characteristics
    private static BluetoothGattService mService;
    private static BluetoothGattCharacteristic mIndicateCharacteristic;

    // Data fields
    private TextView mIridiumBurstText;
    private ProgressDialog mProgressDialog;
    private String mTextMessage;

    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();

            // GATT Data available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Logger.i("Data Available");
                // Check text message
/*                if (extras.containsKey(Constants.EXTRA_IRIDIUM_VALUE)) {
                    byte[] received_mns_data = intent
                            .getByteArrayExtra(Constants.EXTRA_IRIDIUM_VALUE);
                    if (received_mns_data != null)
                        Logger.i(received_mns_data.toString());
                }*/
/*                if (extras.containsKey(Constants.EXTRA_BYTE_UUID_VALUE)) {
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
                }*/

                if (extras.containsKey(Constants.EXTRA_BYTE_VALUE)) {
                    String received_text_msg = intent
                            .getStringExtra(Constants.EXTRA_IRIDIUM_VALUE);
                    if (!received_text_msg.equalsIgnoreCase(" ")) {
//                        displayIridiumBurstMessage(received_text_msg.substring(1));
                        if (received_text_msg.charAt(0) > 19) {
                            mTextMessage = mTextMessage + received_text_msg.substring(1);
                            getGattData();
                        }
                        else {
                            mTextMessage = mTextMessage + received_text_msg.substring(1, (received_text_msg.charAt(0) + 1));
                            displayIridiumBurstMessage(mTextMessage);
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

    public IridiumBurstService create(BluetoothGattService service) {
        IridiumBurstService fragment = new IridiumBurstService();
        mService = service;
        return fragment;
    }

    void displayIridiumBurstMessage(String received_mns_data){
        mIridiumBurstText.setText(received_mns_data);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.iridium_burst, container, false);
        mIridiumBurstText = (TextView) rootView
                .findViewById(R.id.iridium_burst_text);
        getActivity().getActionBar().setTitle(R.string.iridium_burst);
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
            mIndicateCharacteristic = characteristic;
            BluetoothLeService.readCharacteristic(characteristic);
        }
    }

    @Override
    public void onResume() {
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        clearUI();
/*        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.iridium_burst));*/
        mTextMessage = "";
        getGattData();
        super.onResume();
    }

    /**
     * clear all data fields
     */
    private void clearUI() {
        mIridiumBurstText.setText("");
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        super.onPause();
    }

    void getGattData() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mService
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();
            if (uuidchara
                    .equalsIgnoreCase(GattAttributes.IRIDIUM_BURST_TEXT)) {
                Logger.i("Characteristic div" + uuidchara);
                mIndicateCharacteristic = gattCharacteristic;
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
