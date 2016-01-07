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

package com.cypress.cysmart.GATTDBFragments;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CySmartApplication;
import com.cypress.cysmart.ListAdapters.GattServiceListAdapter;
import com.cypress.cysmart.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Fragment to show the GATT services details in GATT DB
 */
public class GattServicesFragment extends Fragment {

    // BluetoothGattService
    private static BluetoothGattService mService;

    // HashMap to store service
    private static ArrayList<HashMap<String, BluetoothGattService>> mGattServiceData;
    private static ArrayList<HashMap<String, BluetoothGattService>> mModifiedServiceData;

    // GattCharacteristics list
    private static List<BluetoothGattCharacteristic> mGattCharacteristics;

    // Application
    private CySmartApplication mApplication;

    // ListView
    private ListView mGattListView;

    //
    private ImageView mBackButton;
    private int HANDLER_DELAY = 500;

    public GattServicesFragment create() {
        GattServicesFragment fragment = new GattServicesFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gatt_list,
                container, false);
        mApplication = (CySmartApplication) getActivity().getApplication();
        mGattListView = (ListView) rootView
                .findViewById(R.id.ListView_gatt_services);
        mBackButton = (ImageView) rootView.findViewById(R.id.imgback);
        mBackButton.setVisibility(View.GONE);
        RelativeLayout parent = (RelativeLayout) rootView
                .findViewById(R.id.parent);
        parent.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });


        // Getting the service data from the application
        mGattServiceData = mApplication.getGattServiceMasterData();

        // Preparing list data
        mModifiedServiceData = new ArrayList<HashMap<String, BluetoothGattService>>();
        for (int i = 0; i < mGattServiceData.size(); i++) {
            if (!(mGattServiceData.get(i).get("UUID").getUuid().toString()
                    .equalsIgnoreCase(GattAttributes.GENERIC_ATTRIBUTE_SERVICE) || mGattServiceData
                    .get(i).get("UUID").getUuid().toString()
                    .equalsIgnoreCase(GattAttributes.GENERIC_ACCESS_SERVICE))) {
                mModifiedServiceData.add(mGattServiceData.get(i));

            }
        }
        // Setting adapter
        GattServiceListAdapter adapter = new GattServiceListAdapter(
                getActivity(), mModifiedServiceData);
        mGattListView.setAdapter(adapter);

        // List listener
        mGattListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
                                    long arg3) {
                mService = mModifiedServiceData.get(pos).get("UUID");
                mGattCharacteristics = mService.getCharacteristics();
                String selected_service_name = GattAttributes.lookup(
                        mService.getUuid().toString(),
                        getResources().getString(
                                R.string.profile_control_unknown_service));

                mApplication.setGattCharacteristics(mGattCharacteristics);

                // Passing service details to GattCharacteristicsFragment and
                // adding that fragment to the current view
                Bundle bundle = new Bundle();
                bundle.putString(Constants.GATTDB_SELECTED_SERVICE,
                        selected_service_name);
                FragmentManager fragmentManager = getFragmentManager();
                GattCharacteristicsFragment gattcharacteristicsfragment = new GattCharacteristicsFragment()
                        .create();
                gattcharacteristicsfragment.setArguments(bundle);
                fragmentManager.beginTransaction()
                        .add(R.id.container, gattcharacteristicsfragment)
                        .addToBackStack(null).commit();
            }
        });
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setTitle(R.string.gatt_db);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        new stopAllNotifications().execute();
        Toast.makeText(getActivity(), getResources().
                        getString(R.string.profile_control_stop_both_notify_indicate_toast),
                Toast.LENGTH_SHORT).show();
    }

    private void stopAllEnabledNotifications() {
        List<BluetoothGattService> bluetoothGattServices = BluetoothLeService.
                getSupportedGattServices();
        for (int count = 0; count < bluetoothGattServices.size(); count++) {
            List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = bluetoothGattServices.
                    get(count).getCharacteristics();
            for (int pos = 0; pos < bluetoothGattCharacteristics.size(); pos++) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic =
                        bluetoothGattCharacteristics.get(pos);
                BluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristic,
                        false, true);
                if (android.os.Build.VERSION.SDK_INT < 21) {
                    // only for below lollipop devices.
                    // Delay needed to handle each charachersitic.
                    Logger.e("Kitkat device");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    private class stopAllNotifications extends AsyncTask<String, Boolean, String> {
        @Override
        protected void onPreExecute() {
            Logger.e("onPreExecute");
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            stopAllEnabledNotifications();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Logger.e("onPostExecute");
            super.onPostExecute(s);
        }
    }

}
