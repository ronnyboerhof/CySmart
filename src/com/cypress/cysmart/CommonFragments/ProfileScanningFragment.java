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

package com.cypress.cysmart.CommonFragments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.CySmartApplication;
import com.cypress.cysmart.DataLoggerFragments.DataLoggerFragment;
import com.cypress.cysmart.HomePageActivity;
import com.cypress.cysmart.R;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileScanningFragment extends Fragment {

    // Stops scanning after 2 seconds.
    private static final long SCAN_PERIOD = 2000;
    // Activity request constant
    private static final int REQUEST_ENABLE_BT = 1;
    // device details
    public static String mDeviceName = "name";
    public static String mDeviceAddress = "address";
    //Pair status button and variables
    public static Button pairButton;
    static ArrayList<HashMap<String, BluetoothGattService>> gattServiceData = new ArrayList<HashMap<String, BluetoothGattService>>();
    static ArrayList<HashMap<String, BluetoothGattService>> gattServiceFindMeData = new ArrayList<HashMap<String, BluetoothGattService>>();
    static ArrayList<HashMap<String, BluetoothGattService>> gattServiceProximityData = new ArrayList<HashMap<String, BluetoothGattService>>();
    static ArrayList<HashMap<String, BluetoothGattService>> gattServiceSensorHubData = new ArrayList<HashMap<String, BluetoothGattService>>();
    // Scanning status flag
    private static boolean mScanning;
    // Devices list
    private static ArrayList<BluetoothDevice> mLeDevices;
    //Arraylist with mapping service name and uuid
    private static ArrayList<HashMap<String, BluetoothGattService>> gattdbServiceData = new ArrayList<HashMap<String, BluetoothGattService>>();
    // Blue tooth adapter for BLE device scan
    private static BluetoothAdapter mBluetoothAdapter;
    /**
     * Blue tooth GATT service data
     */
    private static ArrayList<HashMap<String, BluetoothGattService>> gattServiceMasterData = new ArrayList<HashMap<String, BluetoothGattService>>();
    // UUID key
    private final String LIST_UUID = "UUID";
    // Adapter for Devices list
    private LeDeviceListAdapter mLeDeviceListAdapter;
    // Swipe refresh layout
    private SwipeRefreshLayout mswipeLayout;
    // RSSI values
    private Map<String, Integer> mdevRssiValues;
    // Application
    private CySmartApplication application;
    // ListView
    private ListView mProfileListView;
    // No device found view
    // private RelativeLayout mNoDeviceFound;
    private TextView mRefreshText;
    // progress dialog variable
    private ProgressDialog mpdia;
    //  flags
    private boolean HANDLER_FLAG = false;
    private boolean BLUETOOTH_STATUS_FLAG = true;
    private boolean searchEnabled = false;
    private Handler mConnectHandler;
    private Handler mHandler;
    /**
     * Call back for BLE Scan
     * This call back is called when a BLE device is found near by.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            Activity mActivity = getActivity();
            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!searchEnabled) {
                            mLeDeviceListAdapter.addDevice(device, rssi);
                            try {
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }

        }
    };
    private boolean receiverEnabled = false;
    /**
     * BroadcastReceiver for receiving the GATT communication status
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // Status received when connected to GATT Server
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                BluetoothLeService.discoverServices();

            }
            // Services Discovered from GATT Server
            else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                prepareGattServices(BluetoothLeService.getSupportedGattServices());

            }

        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // View related variable initialization
        View mrootView = inflater.inflate(R.layout.fragment_profile_scan, container,
                false);
        mHandler = new Handler();
        mConnectHandler = new Handler();
        mdevRssiValues = new HashMap<String, Integer>();
        application = (CySmartApplication) getActivity().getApplication();
        mswipeLayout = (SwipeRefreshLayout) mrootView
                .findViewById(R.id.swipe_container);
        mswipeLayout.setColorScheme(R.color.dark_blue, R.color.medium_blue,
                R.color.light_blue, R.color.faint_blue);
        mProfileListView = (ListView) mrootView
                .findViewById(R.id.listView_profiles);
        mRefreshText = (TextView) mrootView.findViewById(R.id.no_dev);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mProfileListView.setAdapter(mLeDeviceListAdapter);
        mProfileListView.setTextFilterEnabled(true);
        setHasOptionsMenu(true);

        mpdia = new ProgressDialog(getActivity());
        mpdia.setCancelable(false);

        checkBleSupportAndInitialize();
        prepareList();

        /**
         * Swipe listener,initiate a new scan on refresh. Stop the swipe refresh
         * after 5 seconds
         */
        mswipeLayout
                .setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

                    @Override
                    public void onRefresh() {
                        if (!mScanning) {
                            // Prepare list view and initiate scanning
                            if (mLeDeviceListAdapter != null) {
                                mLeDeviceListAdapter.clear();
                                try {
                                    mLeDeviceListAdapter.notifyDataSetChanged();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            scanLeDevice(true);
                            mScanning = true;
                            searchEnabled = false;
                            mRefreshText.setText(getResources().getString(
                                    R.string.profile_control_device_scanning));
                        }

                    }

                });


        /**
         * Creating the dataLogger file and
         * updating the datalogger history
         */
        Logger.createDataLoggerFile(getActivity());
        mProfileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (mLeDeviceListAdapter.getCount() > 0) {
                    final BluetoothDevice device = mLeDeviceListAdapter
                            .getDevice(position);
                    if (device != null) {
                        scanLeDevice(false);

                        connectDevice(device);
                    }
                }
            }
        });

        return mrootView;
    }

    private void checkBleSupportAndInitialize() {
        // Use this check to determine whether BLE is supported on the device.
        if (!getActivity().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), R.string.device_ble_not_supported,
                    Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
        // Initializes a Blue tooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getActivity()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            // Device does not support Blue tooth
            Toast.makeText(getActivity(),
                    R.string.device_bluetooth_not_supported, Toast.LENGTH_SHORT)
                    .show();
            getActivity().finish();
        }
    }


    /**
     * Method to connect to the device selected. The time allotted for having a
     * connection is 8 seconds. After 8 seconds it will disconnect if not
     * connected and initiate scan once more
     *
     * @param device
     */

    void connectDevice(BluetoothDevice device) {
        // Register the broadcast receiver for connection status
        if (!receiverEnabled) {
            Logger.e("Registering receiver some how ");
            getActivity().registerReceiver(mGattUpdateReceiver,
                    Utils.makeGattUpdateIntentFilter());
            receiverEnabled = true;
        }
        mpdia.setTitle(getResources().getString(
                R.string.alert_message_connect_title));

        mpdia.setMessage(getResources().getString(
                R.string.alert_message_connect)
                + "\n"
                + device.getName()
                + "\n"
                + device.getAddress()
                + "\n"
                + getResources().getString(R.string.alert_message_wait));

        if(!getActivity().isDestroyed()&&mpdia!=null){
            mpdia.show();
        }
        mDeviceAddress = device.getAddress();
        mDeviceName = device.getName();
        // Get the connection status of the device
        if (BluetoothLeService.getConnectionState() == BluetoothLeService.STATE_DISCONNECTED) {
            Logger.i("BluetoothLeService.getConnectionState()--->" + BluetoothLeService.getConnectionState());
            // Disconnected,so connect
            HANDLER_FLAG=true;
            BluetoothLeService.connect(mDeviceAddress, mDeviceName, getActivity());
        }
        else {
            Logger.i("BluetoothLeService.getConnectionState()--->" + BluetoothLeService.getConnectionState());
            // Connecting to some devices,so disconnect and then connect
            BluetoothLeService.disconnect();
            HANDLER_FLAG=true;
            BluetoothLeService.connect(mDeviceAddress, mDeviceName, getActivity());
        }
        mConnectHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Logger.e("Connect handler called");
                if (HANDLER_FLAG) {
                    mpdia.dismiss();
                    BluetoothLeService.disconnect();
                    try {
                        Toast.makeText(getActivity(),
                                R.string.profile_control_delay_message,
                                Toast.LENGTH_SHORT).show();
                        if (mLeDeviceListAdapter != null)
                            mLeDeviceListAdapter.clear();
                        if (mLeDeviceListAdapter != null) {
                            try {
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        // mswipeLayout.setVisibility(View.INVISIBLE);
                        // mNoDeviceFound.setVisibility(View.VISIBLE);
                        scanLeDevice(true);
                        mScanning = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 10000);

    }

    /**
     * Method to scan BLE Devices. The status of the scan will be detected in
     * the BluetoothAdapter.LeScanCallback
     *
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(BLUETOOTH_STATUS_FLAG){
                        Logger.e("scan handler called");
                        mScanning = false;
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        mRefreshText.setText(getResources().getString(
                                R.string.profile_control_no_device_message));
                        mswipeLayout.setRefreshing(false);
                        scanLeDevice(false);
                    }

                }
            }, SCAN_PERIOD);
            if (!mScanning) {
                mScanning = true;
                mRefreshText.setText(getResources().getString(
                        R.string.profile_control_device_scanning));
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mswipeLayout.setRefreshing(true);
            }
        } else {
            mScanning = false;
            mswipeLayout.setRefreshing(false);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            try {
                mLeDeviceListAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Preparing the BLE Devicelist
     */
    public void prepareList() {
        // Initializes ActionBar as required
        setUpActionBar();
        // Prepare list view and initiate scanning
        if (mLeDeviceListAdapter != null) {
            mLeDeviceListAdapter.clear();
            try {
                mLeDeviceListAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        scanLeDevice(true);

        searchEnabled = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.e("onResume");
        BLUETOOTH_STATUS_FLAG=true;
        checkBluetoothStatus();
        prepareList();

    }



    @Override
    public void onPause() {
        Logger.e("onPause");
        BLUETOOTH_STATUS_FLAG=false;
        if(mpdia!=null&&mpdia.isShowing()){
            mpdia.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiverEnabled) {
            scanLeDevice(false);
            BLUETOOTH_STATUS_FLAG=false;
            if (mLeDeviceListAdapter != null)
                mLeDeviceListAdapter.clear();
            if (mLeDeviceListAdapter != null) {
                try {
                    mLeDeviceListAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            getActivity().unregisterReceiver(mGattUpdateReceiver);
            receiverEnabled = false;
            mswipeLayout.setRefreshing(false);
        }
    }

    /**
     * Getting the GATT Services
     *
     * @param gattServices
     */
    private void prepareGattServices(List<BluetoothGattService> gattServices) {
        // Optimization code for Sensor HUb
        if (isSensorHubPresent(gattServices)) {
            prepareSensorHubData(gattServices);
        } else {
            prepareData(gattServices);
        }

    }

    /**
     * Check whether SensorHub related services are present in the discovered
     * services
     *
     * @param gattServices
     * @return {@link Boolean}
     */
    boolean isSensorHubPresent(List<BluetoothGattService> gattServices) {
        boolean present = false;
        for (BluetoothGattService gattService : gattServices) {
            String uuid = gattService.getUuid().toString();
            if (uuid.equalsIgnoreCase(GattAttributes.BAROMETER_SERVICE)) {
                present = true;
            }
        }
        return present;
    }

    /**
     * Prepare GATTServices data.
     *
     * @param gattServices
     */
    private void prepareData(List<BluetoothGattService> gattServices) {
        boolean mFindmeSet = false;
        boolean mProximitySet = false;
        boolean mGattSet = false;
        if (gattServices == null)
            return;
        // Clear all array list before entering values.
        gattServiceData.clear();
        gattServiceFindMeData.clear();
        gattServiceMasterData.clear();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, BluetoothGattService> currentServiceData = new HashMap<String, BluetoothGattService>();
            String uuid = gattService.getUuid().toString();
            // Optimization code for FindMe Profile
            if (uuid.equalsIgnoreCase(GattAttributes.IMMEDIATE_ALERT_SERVICE)) {
                currentServiceData.put(LIST_UUID, gattService);
                gattServiceMasterData.add(currentServiceData);
                if (!gattServiceFindMeData.contains(currentServiceData)) {
                    gattServiceFindMeData.add(currentServiceData);
                }
                if (!mFindmeSet) {
                    mFindmeSet = true;
                    gattServiceData.add(currentServiceData);
                }

            }
            // Optimization code for Proximity Profile
            else if (uuid.equalsIgnoreCase(GattAttributes.LINK_LOSS_SERVICE)
                    || uuid.equalsIgnoreCase(GattAttributes.TRANSMISSION_POWER_SERVICE)) {
                currentServiceData.put(LIST_UUID, gattService);
                gattServiceMasterData.add(currentServiceData);
                if (!gattServiceProximityData.contains(currentServiceData)) {
                    gattServiceProximityData.add(currentServiceData);
                }
                if (!mProximitySet) {
                    mProximitySet = true;
                    gattServiceData.add(currentServiceData);
                }

            }// Optimization code for GATTDB
            else if (uuid
                    .equalsIgnoreCase(GattAttributes.GENERIC_ACCESS_SERVICE)
                    || uuid.equalsIgnoreCase(GattAttributes.GENERIC_ATTRIBUTE_SERVICE)) {
                currentServiceData.put(LIST_UUID, gattService);
                gattdbServiceData.add(currentServiceData);
                if (!mGattSet) {
                    mGattSet = true;
                    gattServiceData.add(currentServiceData);
                }

            } else {
                currentServiceData.put(LIST_UUID, gattService);
                gattServiceMasterData.add(currentServiceData);
                gattServiceData.add(currentServiceData);
            }

        }
        application.setGattServiceData(gattServiceData);
        application.setGattServiceMasterData(gattServiceMasterData);

        // All service discovered and optimized.Dismiss the alert dialog
        if (gattdbServiceData.size() > 0) {
            mConnectHandler.removeCallbacksAndMessages(null);
            if (mpdia != null && mpdia.isShowing()) {
                mpdia.dismiss();
            }
            /**
             * Setting the handler flag to false. adding new fragment
             * ProfileControlFragment to the view
             */
            HANDLER_FLAG = false;
            if (receiverEnabled) {
                Logger.e("unregisterReceiver---->");
                getActivity().unregisterReceiver(mGattUpdateReceiver);
                receiverEnabled = false;
                FragmentManager fragmentManager = getFragmentManager();
                ProfileControlFragment profileControlFragment = new ProfileControlFragment()
                        .create(mDeviceName, mDeviceAddress);
                fragmentManager
                        .beginTransaction()
                        .add(R.id.container, profileControlFragment,
                                Constants.PROFILE_CONTROL_FRAGMENT_TAG)
                        .addToBackStack(null).commit();
            }

        }

    }

    private void prepareSensorHubData(List<BluetoothGattService> gattServices) {

        boolean mGattSet = false;
        boolean mSensorHubSet = false;

        if (gattServices == null)
            return;
        // Clear all array list before entering values.
        gattServiceData.clear();
        gattServiceMasterData.clear();
        gattServiceSensorHubData.clear();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, BluetoothGattService> currentServiceData = new HashMap<String, BluetoothGattService>();
            String uuid = gattService.getUuid().toString();

            // Optimization code for SensorHub Profile
            if (uuid.equalsIgnoreCase(GattAttributes.LINK_LOSS_SERVICE)
                    || uuid.equalsIgnoreCase(GattAttributes.TRANSMISSION_POWER_SERVICE)
                    || uuid.equalsIgnoreCase(GattAttributes.IMMEDIATE_ALERT_SERVICE)
                    || uuid.equalsIgnoreCase(GattAttributes.BAROMETER_SERVICE)
                    || uuid.equalsIgnoreCase(GattAttributes.ACCELEROMETER_SERVICE)
                    || uuid.equalsIgnoreCase(GattAttributes.ANALOG_TEMPERATURE_SERVICE)
                    || uuid.equalsIgnoreCase(GattAttributes.BATTERY_SERVICE)
                    || uuid.equalsIgnoreCase(GattAttributes.DEVICE_INFORMATION_SERVICE)) {
                currentServiceData.put(LIST_UUID, gattService);
                gattServiceMasterData.add(currentServiceData);
                if (!gattServiceSensorHubData.contains(currentServiceData)) {
                    gattServiceSensorHubData.add(currentServiceData);
                }
                if (!mSensorHubSet
                        && uuid.equalsIgnoreCase(GattAttributes.BAROMETER_SERVICE)) {
                    mSensorHubSet = true;
                    gattServiceData.add(currentServiceData);
                }

            } // Optimization code for GATTDB
            else if (uuid
                    .equalsIgnoreCase(GattAttributes.GENERIC_ACCESS_SERVICE)
                    || uuid.equalsIgnoreCase(GattAttributes.GENERIC_ATTRIBUTE_SERVICE)) {
                currentServiceData.put(LIST_UUID, gattService);
                gattdbServiceData.add(currentServiceData);
                if (!mGattSet) {
                    mGattSet = true;
                    gattServiceData.add(currentServiceData);
                }

            } else {
                currentServiceData.put(LIST_UUID, gattService);
                gattServiceMasterData.add(currentServiceData);
                gattServiceData.add(currentServiceData);
            }
        }
        application.setGattServiceMasterData(gattServiceMasterData);
        application.setGattServiceData(gattServiceData);

        // All service discovered and optimized.Dismiss the alert dialog
        if (gattdbServiceData.size() > 0) {
            mConnectHandler.removeCallbacksAndMessages(null);
            if (mpdia != null && mpdia.isShowing()) {
                mpdia.dismiss();
            }
            /**
             * Setting the handler flag to false. adding new fragment
             * ProfileControlFragment to the view
             */
            HANDLER_FLAG = false;
            if (receiverEnabled) {
                getActivity().unregisterReceiver(mGattUpdateReceiver);
                receiverEnabled = false;
                FragmentManager fragmentManager = getFragmentManager();
                ProfileControlFragment profileControlFragment = new ProfileControlFragment()
                        .create(mDeviceName, mDeviceAddress);
                fragmentManager
                        .beginTransaction()
                        .add(R.id.container, profileControlFragment,
                                Constants.PROFILE_CONTROL_FRAGMENT_TAG)
                        .addToBackStack(null).commit();
            }

        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable BlueTooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == Activity.RESULT_CANCELED) {
            getActivity().finish();
        } else {
            // Check which request we're responding to
            if (requestCode == REQUEST_ENABLE_BT) {

                // Make sure the request was successful
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(
                            getActivity(),
                            getResources().getString(
                                    R.string.device_bluetooth_on),
                            Toast.LENGTH_SHORT).show();
                    mLeDeviceListAdapter = new LeDeviceListAdapter();
                    mProfileListView.setAdapter(mLeDeviceListAdapter);
                    scanLeDevice(true);
                } else {
                    getActivity().finish();
                }
            }
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.global, menu);
        MenuItem item = menu.findItem(R.id.graph);
        MenuItem log = menu.findItem(R.id.log);
        final MenuItem searchMenuItem = menu.findItem(R.id.search);
        item.setVisible(false);
        log.setVisible(true);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false);
        SearchView.OnQueryTextListener textChangeListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                // this is your adapter that will be filtered

                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                if (searchMenuItem != null) {
                    searchMenuItem.collapseActionView();
                    // this is your adapter that will be filtered
                    if (mLeDeviceListAdapter != null) {
                        searchEnabled = true;
                        searchDeviceList(query);
                    }
                }
                return false;

            }
        };
        searchView.setOnQueryTextListener(textChangeListener);
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean queryTextFocused) {
                if (!queryTextFocused) {
                    searchMenuItem.collapseActionView();
                    searchView.setQuery("", false);
                }
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here.
        switch (item.getItemId()) {
            case R.id.log:
                // DataLogger
                File file = new File(Environment.getExternalStorageDirectory()
                        + File.separator + "CySmart" + File.separator
                        + Utils.GetDate() + ".txt");
                String path = file.getAbsolutePath();
                Bundle bundle = new Bundle();
                bundle.putString(Constants.DATA_LOGGER_FILE_NAAME, path);
                bundle.putBoolean(Constants.DATA_LOGGER_FLAG, false);
                /**
                 * Adding new fragment DataLoggerFragment to the view
                 */
                FragmentManager fragmentManager = getFragmentManager();
                Fragment currentFragment=fragmentManager.findFragmentById(R.id.container);
                DataLoggerFragment dataloggerfragment = new DataLoggerFragment()
                        .create(currentFragment.getTag());
                dataloggerfragment.setArguments(bundle);
                fragmentManager.beginTransaction()
                        .add(R.id.container, dataloggerfragment)
                        .addToBackStack(null).commit();
                return true;
            case R.id.share:
                // Share
                HomePageActivity.containerView.invalidate();
                View v1 = getActivity().getWindow().getDecorView().getRootView();
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                String temporaryPath = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        File.separator + "CySmart" + File.separator + "file.jpg";
                File filetoshare = new File(temporaryPath);
                if (filetoshare.exists()) {
                    filetoshare.delete();
                }
                try {
                    filetoshare.createNewFile();
                    Utils.screenShotMethod(v1);
                    Logger.i("temporaryPath>" + temporaryPath);
                    shareIntent.putExtra(Intent.EXTRA_STREAM,
                            Uri.fromFile(new File(temporaryPath)));
                    shareIntent.setType("image/jpg");
                    startActivity(Intent.createChooser(shareIntent, getResources()
                            .getText(R.string.send_to)));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            case R.id.clearcache:
                showWarningMessage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Setting up the ActionBar
     */
    void setUpActionBar() {
        ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            actionBar.setIcon(new ColorDrawable(getResources().getColor(
                    android.R.color.transparent)));
        }
        if (actionBar != null) {
            actionBar.setTitle(R.string.profile_scan_fragment);
        }
    }

    private void searchDeviceList(CharSequence query) {
        ArrayList<BluetoothDevice> foundDevices = new ArrayList<BluetoothDevice>(mLeDevices);
        mLeDevices.clear();
        try {
            mLeDeviceListAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            for (int pos = 0; pos < foundDevices.size(); pos++) {
                String data = query.toString();
                if (foundDevices.get(pos).getName().toLowerCase().contains(
                        data.toLowerCase())) {
                    mLeDeviceListAdapter.addDevice(foundDevices.get(pos), mLeDeviceListAdapter.getRssiValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //For Pairing
    private void pairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

        } catch (Exception e) {
            if (mpdia != null && mpdia.isShowing()) {
                mpdia.dismiss();
            }
        }

    }

    //For UnPairing
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

        } catch (Exception e) {
            if (mpdia != null && mpdia.isShowing()) {
                mpdia.dismiss();
            }
        }

    }

    public void checkBluetoothStatus() {
        /**
         * Ensures Blue tooth is enabled on the device. If Blue tooth is not
         * currently enabled, fire an intent to display a dialog asking the user
         * to grant permission to enable it.
         */
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void showWarningMessage() {
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(
                getActivity().getResources().getString(
                        R.string.alert_message_clear_cache))
                .setTitle(getActivity().getResources().getString(R.string.alert_title_clear_cache))
                .setCancelable(false)
                .setPositiveButton(
                        getActivity().getResources().getString(
                                R.string.alert_message_exit_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                if (BluetoothLeService.mBluetoothGatt != null)
                                    BluetoothLeService.refreshDeviceCache(BluetoothLeService.mBluetoothGatt);
                                BluetoothLeService.disconnect();
                                Toast.makeText(getActivity(),
                                        getResources().getString(R.string.alert_message_bluetooth_disconnect),
                                        Toast.LENGTH_SHORT).show();
                                Intent homePage = getActivity().getIntent();
                                getActivity().finish();
                                getActivity().overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                                startActivity(homePage);
                                getActivity().overridePendingTransition(R.anim.slide_left, R.anim.push_left);

                            }
                        })
                .setNegativeButton(getActivity().getResources().getString(
                        R.string.alert_message_exit_cancel), null);
        alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    /**
     * Holder class for the list view view widgets
     */
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi;
        Button pairStatus;
    }

    /**
     * List Adapter for holding devices found through scanning.
     */
    private class LeDeviceListAdapter extends BaseAdapter {

        private LayoutInflater mInflator;
        private int rssiValue;


        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = getActivity().getLayoutInflater();
        }

        private void addDevice(BluetoothDevice device, int rssi) {
            this.rssiValue = rssi;
            // New device found
            if (!mLeDevices.contains(device)) {
                mdevRssiValues.put(device.getAddress(), rssi);
                mLeDevices.add(device);
            } else {
                mdevRssiValues.put(device.getAddress(), rssi);
            }
        }

        public int getRssiValue() {
            return rssiValue;
        }

        /**
         * Getter method to get the blue tooth device
         *
         * @param position
         * @return BluetoothDevice
         */
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        /**
         * Clearing all values in the device array list
         */
        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }


        @Override
        public View getView(final int position, View view, ViewGroup viewGroup) {
            final ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, viewGroup,
                        false);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view
                        .findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view
                        .findViewById(R.id.device_name);
                viewHolder.deviceRssi = (TextView) view
                        .findViewById(R.id.device_rssi);
                viewHolder.pairStatus = (Button) view.findViewById(R.id.btn_pair);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            /**
             * Setting the name and the RSSI of the BluetoothDevice. provided it
             * is a valid one
             */
            final BluetoothDevice device = mLeDevices.get(position);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                try {
                    viewHolder.deviceName.setText(deviceName);
                    viewHolder.deviceAddress.setText(device.getAddress());
                    byte rssival = (byte) mdevRssiValues.get(device.getAddress())
                            .intValue();
                    if (rssival != 0) {
                        viewHolder.deviceRssi.setText(String.valueOf(rssival));
                    }
                    String pairStatus = (device.getBondState() == BluetoothDevice.BOND_BONDED) ? getActivity().getResources().getString(R.string.bluetooth_pair) : getActivity().getResources().getString(R.string.bluetooth_unpair);
                    viewHolder.pairStatus.setText(pairStatus);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                viewHolder.deviceName.setText(R.string.device_unknown);
                viewHolder.deviceName.setSelected(true);
                viewHolder.deviceAddress.setText(device.getAddress());
            }
            viewHolder.pairStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pairButton = (Button) view;
                    mDeviceAddress = device.getAddress();
                    mDeviceName = device.getName();
                    String status = pairButton.getText().toString();
                    if (status.equalsIgnoreCase(getResources().getString(R.string.bluetooth_pair))) {
                        unpairDevice(device);
                    } else {
                        pairDevice(device);
                    }

                }
            });
            return view;
        }
    }
}
