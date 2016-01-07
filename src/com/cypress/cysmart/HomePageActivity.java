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

package com.cypress.cysmart;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.BLEServiceFragments.BatteryInformationService;
import com.cypress.cysmart.BLEServiceFragments.BloodPressureService;
import com.cypress.cysmart.BLEServiceFragments.CSCService;
import com.cypress.cysmart.BLEServiceFragments.CapsenseService;
import com.cypress.cysmart.BLEServiceFragments.DeviceInformationService;
import com.cypress.cysmart.BLEServiceFragments.FindMeService;
import com.cypress.cysmart.BLEServiceFragments.GlucoseService;
import com.cypress.cysmart.BLEServiceFragments.HealthTemperatureService;
import com.cypress.cysmart.BLEServiceFragments.HeartRateService;
import com.cypress.cysmart.BLEServiceFragments.RGBFragment;
import com.cypress.cysmart.BLEServiceFragments.RSCService;
import com.cypress.cysmart.BLEServiceFragments.SensorHubService;
import com.cypress.cysmart.CommonFragments.AboutFragment;
import com.cypress.cysmart.CommonFragments.NavigationDrawerFragment;
import com.cypress.cysmart.CommonFragments.ProfileControlFragment;
import com.cypress.cysmart.CommonFragments.ProfileScanningFragment;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.GATTDBFragments.GattDescriptorFragment;
import com.cypress.cysmart.GATTDBFragments.GattServicesFragment;
import com.cypress.cysmart.OTAFirmwareUpdate.OTAFirmwareUpgradeFragment;
import com.cypress.cysmart.RDKEmulatorView.RemoteControlEmulatorFragment;

import java.lang.reflect.Method;

/**
 * Base activity to hold all fragments
 */
public class HomePageActivity extends FragmentActivity implements
        NavigationDrawerFragment.NavigationDrawerCallbacks {

    public static FrameLayout containerView;
    public static Boolean mApplicationInBackground = false;
    /**
     * Used to manage connections of the Blue tooth LE Device
     */
    private static BluetoothLeService mBluetoothLeService;
    private static DrawerLayout parentView;
    /**
     * Code to manage Service life cycle.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            // Initializing the service
            if (!mBluetoothLeService.initialize()) {
                Logger.d("Service not initialized");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private boolean BLUETOOTH_STATUS_FLAG = true;
    private String Paired;
    private String Unpaired;
    // progress dialog variable
    private ProgressDialog mpdia;
    private AlertDialog mAlert;
    /**
     * Fragment managing the behaviors, interactions and presentation of the
     * navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    /**
     * Broadcast receiver for getting the bonding information
     */
    private BroadcastReceiver mBondStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            //Received when the bond state is changed
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDING) {
                    // Bonding...
                    String dataLog2 = getResources().getString(R.string.dl_commaseparator)
                            + "[" + ProfileScanningFragment.mDeviceName + "|"
                            + ProfileScanningFragment.mDeviceAddress + "] " +
                            getResources().getString(R.string.dl_connection_pairing_request);
                    Logger.datalog(dataLog2);
                    Utils.bondingProgressDialog(HomePageActivity.this, mpdia, true);
                } else if (state == BluetoothDevice.BOND_BONDED) {
                    // Bonded...
                    if (ProfileScanningFragment.pairButton != null) {
                        ProfileScanningFragment.pairButton.setText(Paired);
                    }
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + ProfileScanningFragment.mDeviceName + "|"
                            + ProfileScanningFragment.mDeviceAddress + "] " +
                            getResources().getString(R.string.dl_connection_paired);
                    Logger.datalog(dataLog);
                    Utils.bondingProgressDialog(HomePageActivity.this, mpdia, false);

                } else if (state == BluetoothDevice.BOND_NONE) {
                    // Not bonded...
                    if (ProfileScanningFragment.pairButton != null) {
                        ProfileScanningFragment.pairButton.setText(Unpaired);
                    }
                    String dataLog = getResources().getString(R.string.dl_commaseparator)
                            + "[" + ProfileScanningFragment.mDeviceName + "|"
                            + ProfileScanningFragment.mDeviceAddress + "] " +
                            getResources().getString(R.string.dl_connection_unpaired);
                    Logger.datalog(dataLog);
                    Utils.bondingProgressDialog(HomePageActivity.this, mpdia, false);
                }
            }
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                Logger.i("BluetoothAdapter.ACTION_STATE_CHANGED.");
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) ==
                        BluetoothAdapter.STATE_OFF) {
                    Logger.i("BluetoothAdapter.STATE_OFF");
                    if (BLUETOOTH_STATUS_FLAG) {
                        connectionLostBluetoothalertbox(true);
                    }

                }
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) ==
                        BluetoothAdapter.STATE_ON) {
                    Logger.i("BluetoothAdapter.STATE_ON");
                    if (BLUETOOTH_STATUS_FLAG) {
                        connectionLostBluetoothalertbox(false);
                    }

                }

            }
        }
    };

    /**
     * Method to detect whether the device is phone or tablet
     */
    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isTablet(this)) {
            Logger.d("tablet");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            Logger.d("Phone");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        setContentView(R.layout.activity_main);
        Paired = getResources().getString(R.string.bluetooth_pair);
        Unpaired = getResources().getString(R.string.bluetooth_unpair);
        parentView = (DrawerLayout) findViewById(R.id.drawer_layout);
        containerView = (FrameLayout) findViewById(R.id.container);
        mpdia = new ProgressDialog(this);
        mpdia.setCancelable(false);
        mAlert = new AlertDialog.Builder(this).create();
        mAlert.setMessage(getResources().getString(
                R.string.alert_message_bluetooth_reconnect));
        mAlert.setCancelable(false);
        mAlert.setTitle(getResources().getString(R.string.app_name));
        mAlert.setButton(Dialog.BUTTON_POSITIVE, getResources().getString(
                R.string.alert_message_exit_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intentActivity = getIntent();
                finish();
                overridePendingTransition(
                        R.anim.slide_left, R.anim.push_left);
                startActivity(intentActivity);
                overridePendingTransition(
                        R.anim.slide_right, R.anim.push_right);
            }
        });
        mAlert.setCanceledOnTouchOutside(false);
        getTitle();

        // Getting the id of the navigation fragment from the attached xml
        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));


        Intent gattServiceIntent = new Intent(getApplicationContext(),
                BluetoothLeService.class);
        startService(gattServiceIntent);

        /**
         * Attaching the profileScanning fragment to start scanning for nearby
         * devices
         */
        ProfileScanningFragment profileScanningFragment = new ProfileScanningFragment();
        displayView(profileScanningFragment,
                Constants.PROFILE_SCANNING_FRAGMENT_TAG);

    }

    public void connectionLostBluetoothalertbox(Boolean status) {
        //Disconnected
        if (status) {
            mAlert.show();
        } else {
            if (mAlert != null && mAlert.isShowing())
                mAlert.dismiss();
        }

    }

    @Override
    protected void onPause() {
        // Getting the current active fragment
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.container);
        if (currentFragment instanceof ProfileScanningFragment || currentFragment instanceof
                AboutFragment) {
            Intent gattServiceIntent = new Intent(getApplicationContext(),
                    BluetoothLeService.class);
            stopService(gattServiceIntent);
        }
        mApplicationInBackground=true;
        BLUETOOTH_STATUS_FLAG = false;
        unregisterReceiver(mBondStateReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        mApplicationInBackground=false;
        BLUETOOTH_STATUS_FLAG = true;
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mBondStateReceiver, intentFilter);
        super.onResume();
    }

    /**
     * Handling the back pressed actions
     */
    @Override
    public void onBackPressed() {

        // Getting the current active fragment
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.container);

        // Profile scanning fragment active
        if (currentFragment instanceof ProfileScanningFragment) {
            if (parentView.isDrawerOpen(Gravity.START)) {
                parentView.closeDrawer(Gravity.START);
            } else {
                alertbox();
            }

        } else if (currentFragment instanceof AboutFragment ) {
            if (parentView.isDrawerOpen(Gravity.START)) {
                parentView.closeDrawer(Gravity.START);
            } else {
                if (BluetoothLeService.getConnectionState() == 2 ||
                        BluetoothLeService.getConnectionState() == 1 ||
                        BluetoothLeService.getConnectionState() == 4) {
                    BluetoothLeService.disconnect();
                    Toast.makeText(this,
                            getResources().getString(R.string.alert_message_bluetooth_disconnect),
                            Toast.LENGTH_SHORT).show();
                }

                // Guiding the user back to profile scanning fragment
                Intent intent = getIntent();
                finish();
                overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_right, R.anim.push_right);
            }

        } else if (currentFragment instanceof ProfileControlFragment) {
            if (parentView.isDrawerOpen(Gravity.START)) {
                parentView.closeDrawer(Gravity.START);
            } else {
                // Guiding the user back to profile scanning fragment
                //  Logger.i("BLE DISCONNECT---->"+BluetoothLeService.getConnectionState());
                if (BluetoothLeService.getConnectionState() == 2 ||
                        BluetoothLeService.getConnectionState() == 1 ||
                        BluetoothLeService.getConnectionState() == 4) {
                    BluetoothLeService.disconnect();
                    Toast.makeText(this,
                            getResources().getString(R.string.alert_message_bluetooth_disconnect),
                            Toast.LENGTH_SHORT).show();
                }

                Intent intent = getIntent();
                finish();
                overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                super.onBackPressed();
            }

        } else if (currentFragment instanceof GattDescriptorFragment) {
            CySmartApplication mApplication = (CySmartApplication) getApplication();
            BluetoothGattDescriptor descriptor = mApplication.getBluetoothgattDescriptor();
            if (descriptor != null) {
                BluetoothLeService.readDescriptor(descriptor);
            }
            super.onBackPressed();
        }else if (currentFragment instanceof HeartRateService
                || currentFragment instanceof HealthTemperatureService
                || currentFragment instanceof DeviceInformationService
                || currentFragment instanceof BatteryInformationService
                || currentFragment instanceof BloodPressureService
                || currentFragment instanceof CapsenseService
                || currentFragment instanceof CSCService
                || currentFragment instanceof FindMeService
                || currentFragment instanceof GlucoseService
                || currentFragment instanceof RGBFragment
                || currentFragment instanceof RSCService
                || currentFragment instanceof SensorHubService
                || currentFragment instanceof RemoteControlEmulatorFragment
                || currentFragment instanceof GattServicesFragment) {
            if (parentView.isDrawerOpen(Gravity.START)) {
                parentView.closeDrawer(Gravity.START);
            } else {
                Utils.setUpActionBar(
                        this,
                        getResources().getString(
                                R.string.profile_control_fragment));
                super.onBackPressed();
            }
        } else if (currentFragment instanceof OTAFirmwareUpgradeFragment) {
            if (parentView.isDrawerOpen(Gravity.START)) {
                parentView.closeDrawer(Gravity.START);
            } else if (OTAFirmwareUpgradeFragment.mFileupgradeStarted) {
                AlertDialog alert;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(
                        this.getResources().getString(
                                R.string.alert_message_ota_pending))
                        .setTitle(this.getResources().getString(R.string.app_name))
                        .setCancelable(false)
                        .setPositiveButton(
                                this.getResources().getString(
                                        R.string.alert_message_yes),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        if (BluetoothLeService.getConnectionState() == 2 ||
                                                BluetoothLeService.getConnectionState() == 1 ||
                                                BluetoothLeService.getConnectionState() == 4) {
                                            final BluetoothDevice device = BluetoothLeService.mBluetoothAdapter
                                                    .getRemoteDevice(BluetoothLeService.getmBluetoothDeviceAddress());
                                            OTAFirmwareUpgradeFragment.mFileupgradeStarted = false;
                                            unpairDevice(device);
                                            BluetoothLeService.disconnect();
                                            Toast.makeText(HomePageActivity.this,
                                                    getResources().getString(R.string.alert_message_bluetooth_disconnect),
                                                    Toast.LENGTH_SHORT).show();
                                            Intent intent = getIntent();
                                            finish();
                                            overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                                            startActivity(intent);
                                            overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                                        }
                                    }
                                })
                        .setNegativeButton(this.getResources().getString(
                                R.string.alert_message_no), null);
                alert = builder.create();
                alert.setCanceledOnTouchOutside(true);
                if (!this.isDestroyed())
                    alert.show();
            }
            else {
                Utils.setUpActionBar(
                        this,
                        getResources().getString(
                                R.string.profile_control_fragment));
                super.onBackPressed();
            }
        } else {
            if (parentView.isDrawerOpen(Gravity.START)) {
                parentView.closeDrawer(Gravity.START);
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Logger.e("onNavigationDrawerItemSelected "+position);
        /**
         * Update the main content by replacing fragments with user selected
         * option
         */
        switch (position) {
            case 0:
                /**
                 * BLE Devices
                 */
                if (BluetoothLeService.getConnectionState() == 2 ||
                        BluetoothLeService.getConnectionState() == 1 ||
                        BluetoothLeService.getConnectionState() == 4) {
                    BluetoothLeService.disconnect();
                }
                Intent intent = getIntent();
                finish();
                overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_right, R.anim.push_right);

                break;
            case 2:
                /**
                 * About
                 */
                AboutFragment aboutFragment = new AboutFragment();
                displayView(aboutFragment, Constants.ABOUT_FRAGMENT_TAG);

                break;
            default:
                break;
        }

    }

    /**
     * Used for replacing the main content of the view with provided fragments
     *
     * @param fragment
     * @param tag
     */
    void displayView(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment, tag).commit();
    }

    /**
     * Method to create an alert before user exit from the application
     */
    void alertbox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                HomePageActivity.this);
        builder.setMessage(
                getResources().getString(R.string.alert_message_exit))
                .setCancelable(false)
                .setTitle(getResources().getString(R.string.app_name))
                .setPositiveButton(
                        getResources()
                                .getString(R.string.alert_message_exit_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Finish the current activity
                                HomePageActivity.this.finish();
                                Intent gattServiceIntent = new Intent(getApplicationContext(),
                                        BluetoothLeService.class);
                                stopService(gattServiceIntent);

                            }
                        })
                .setNegativeButton(
                        getResources().getString(
                                R.string.alert_message_exit_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Cancel the dialog box
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    //For UnPairing
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
