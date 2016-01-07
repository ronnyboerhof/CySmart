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
import android.widget.ImageView;
import android.widget.LinearLayout;
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
public class IridiumStatusService extends Fragment {
    // Service and characteristics
    private static BluetoothGattService mService;
    private static BluetoothGattCharacteristic mIndicateCharacteristic;

    // Data fields
    private EditText mImeiText;
    private TextView mNetworkStatus;
    private ImageView mSlider;
    private LinearLayout mSliderLayout;
    private ProgressDialog mProgressDialog;

    private static boolean mImeiTextSet = false;
    private static boolean mSignalLevelSet = false;
    private static boolean mNetworkStatusSet = false;

    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();

            // GATT Data available
            // GATT Data available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Logger.i("Data Available");
                if (extras.containsKey(Constants.EXTRA_IRIDIUM_IMEI_VALUE)) {
                    String received_imei = intent.
                            getStringExtra(Constants.EXTRA_IRIDIUM_IMEI_VALUE);
                    if (!received_imei.equalsIgnoreCase(" ")) {
                        if (!mImeiTextSet) {
                            mImeiTextSet = true;
                            displayIMEI(received_imei);

                            List<BluetoothGattCharacteristic> mgatt = mService
                                    .getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : mgatt) {
                                String uuidchara = gattCharacteristic.getUuid()
                                        .toString();
                                if (uuidchara
                                        .equalsIgnoreCase(GattAttributes.IRIDIUM_STATUS_SIGNAL_LEVEL)) {
                                    Logger.i("Characteristic " + uuidchara);
                                    mIndicateCharacteristic = gattCharacteristic;
                                    prepareBroadcastDataRead(gattCharacteristic);
                                }
                            }
                        }
                    }
                }
                if (extras.containsKey(Constants.EXTRA_IRIDIUM_RSSI_VALUE)) {
                    String received_rssi = intent.
                            getStringExtra(Constants.EXTRA_IRIDIUM_RSSI_VALUE);
                    if (!received_rssi.equalsIgnoreCase(" ")) {
                        if (!mSignalLevelSet) {
                            mSignalLevelSet = true;
                            if (received_rssi.charAt(0) == '5')
                                displaySignalLevel(100);
                            else if (received_rssi.charAt(0) == '0')
                                displaySignalLevel(0);
                            else
                                displaySignalLevel(((received_rssi.charAt(0) - '0') * 20) - 1);
                            List<BluetoothGattCharacteristic> mgatt = mService
                                    .getCharacteristics();
                            for (BluetoothGattCharacteristic gattCharacteristic : mgatt) {
                                String uuidchara = gattCharacteristic.getUuid()
                                        .toString();
                                if (uuidchara
                                        .equalsIgnoreCase(GattAttributes.IRIDIUM_STATUS_NETWORK_STATUS)) {
                                    Logger.i("Characteristic " + uuidchara);
                                    mIndicateCharacteristic = gattCharacteristic;
                                    prepareBroadcastDataRead(gattCharacteristic);
                                }
                            }
                        }
                    }
                }
                if (extras.containsKey(Constants.EXTRA_IRIDIUM_NETWORK_VALUE)) {
                    String received_network_status = intent.
                            getStringExtra(Constants.EXTRA_IRIDIUM_NETWORK_VALUE);
                    if (!mNetworkStatusSet) {
                        mNetworkStatusSet = true;

                        if (received_network_status.charAt(0) == 0) {
                            dislayNetworkStatus("Not connected");
                        } else {
                            dislayNetworkStatus("Connected");
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
                            +"["+ BluetoothLeService.getmBluetoothDeviceName()+"|"
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

    public IridiumStatusService create(BluetoothGattService service) {
        IridiumStatusService fragment = new IridiumStatusService();
        mService = service;
        return fragment;
    }

    void displayIMEI(String imeiText) {
        mImeiText.setText(imeiText);
    }

    void dislayNetworkStatus(String statusText) {
        mNetworkStatus.setText(statusText);
    }

    private void displaySignalLevel(int level) {
        int slided;
        slided = 100 - level;
        mSlider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT, level));
        mSliderLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT, slided));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.iridium_status, container, false);

        mImeiText = (EditText) rootView.findViewById(R.id.imei_text);
        mNetworkStatus = (TextView) rootView.findViewById(R.id.network_status);
        mSlider = (ImageView) rootView.findViewById(R.id.slider_view_1);
        mProgressDialog = new ProgressDialog(getActivity());
        mSliderLayout = (LinearLayout) rootView
                .findViewById(R.id.slider_view_2);
        getActivity().getActionBar().setTitle(R.string.iridium_status);
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
        makeDefaultBooleans();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        clearUI();
        getGattData();
        super.onResume();
    }

    /**
     * clear all data fields
     */
    private void clearUI() {
        mImeiText.setText("");
        mNetworkStatus.setText("");
    }

    /**
     * Flag up default
     */
    private void makeDefaultBooleans() {
        mImeiTextSet = false;
        mSignalLevelSet = false;
        mNetworkStatusSet = false;
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
                    .equalsIgnoreCase(GattAttributes.IRIDIUM_STATUS_IMEI)) {
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
    }}
