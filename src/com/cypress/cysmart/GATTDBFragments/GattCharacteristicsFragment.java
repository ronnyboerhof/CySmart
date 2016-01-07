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
import android.widget.TextView;

import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CySmartApplication;
import com.cypress.cysmart.ListAdapters.GattCharacteriscticsListAdapter;
import com.cypress.cysmart.R;

import java.util.List;

/**
 * GATT Characteristic Fragment under GATT DB
 */
public class GattCharacteristicsFragment extends Fragment {

    // List for storing BluetoothGattCharacteristics
    private List<BluetoothGattCharacteristic> gattCharacteristics;

    // Application
    private CySmartApplication mApplication;

    // ListView
    private ListView mgattListView;

    // Text Heading
    private TextView mTextHeading;

    // GATT Service name
    private String mGattServiceName = "";

    // Back button
    private ImageView mBackButton;

    public GattCharacteristicsFragment create() {
        GattCharacteristicsFragment fragment = new GattCharacteristicsFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gatt_list,
                container, false);
        mApplication = (CySmartApplication) getActivity().getApplication();
        getActivity().getActionBar().setTitle(R.string.gatt_db);
        mgattListView = (ListView) rootView
                .findViewById(R.id.ListView_gatt_services);
        mTextHeading = (TextView) rootView.findViewById(R.id.txtservices);
        mTextHeading.setText(getString(R.string.gatt_characteristics_heading));
        mBackButton = (ImageView) rootView.findViewById(R.id.imgback);

        // Back button listener
        mBackButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();

            }
        });
        // Getting the GATT characteristics from application
        gattCharacteristics = mApplication.getGattCharacteristics();

        // Getting the selected service from the arguments
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mGattServiceName = bundle
                    .getString(Constants.GATTDB_SELECTED_SERVICE);
        }
        // Preparing list data
        GattCharacteriscticsListAdapter adapter = new GattCharacteriscticsListAdapter(
                getActivity(), gattCharacteristics);
        if (adapter != null) {
            mgattListView.setAdapter(adapter);
        }

        // List listener
        mgattListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int pos,
                                    long arg3) {
                mApplication.setBluetoothgattcharacteristic(gattCharacteristics
                        .get(pos));
                String characteristicuuid = gattCharacteristics.get(pos).getUuid().toString();
                String characteristicsname = GattAttributes.lookup(characteristicuuid,
                        characteristicuuid);

                /**
                 * Passing the characteristic details to GattDetailsFragment and
                 * adding that fragment to the view
                 */
                Bundle bundle = new Bundle();
                bundle.putString(Constants.GATTDB_SELECTED_SERVICE,
                        mGattServiceName);
                bundle.putString(Constants.GATTDB_SELECTED_CHARACTERISTICE,
                        characteristicsname);
                FragmentManager fragmentManager = getFragmentManager();
                GattDetailsFragment gattDetailsfragment = new GattDetailsFragment()
                        .create();
                gattDetailsfragment.setArguments(bundle);
                fragmentManager.beginTransaction()
                        .add(R.id.container, gattDetailsfragment)
                        .addToBackStack(null).commit();
            }
        });
        setHasOptionsMenu(true);
        return rootView;
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
