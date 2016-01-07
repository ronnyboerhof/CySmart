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

package com.cypress.cysmart.DataLoggerFragments;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.DataModelClasses.DataLoggerModel;
import com.cypress.cysmart.ListAdapters.DataLoggerListAdapter;
import com.cypress.cysmart.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Fragment to show the DataLogger history
 */
public class DataLoggerHistoryListFragment extends Fragment {

    /**
     * ListView for loading the data logger files
     */
    private ListView mListFileNames;
    /**
     * Adapter for ListView
     */
    DataLoggerListAdapter mAdapter;
    /**
     * File names
     */
    private ArrayList<DataLoggerModel> mDataLoggerArrayList;
    DataLoggerModel mDataLoggerModel;
    /**
     * Directory of the file
     */
    private String mDirectory;
    /**
     * File
     */
    private File mFile;
    private static String mLastFragment;

    public DataLoggerHistoryListFragment create(String lastFrag) {
        this.mLastFragment=lastFrag;
        DataLoggerHistoryListFragment fragment = new DataLoggerHistoryListFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.datalogger_list, container,
                false);
        mListFileNames = (ListView) rootView
                .findViewById(R.id.data_logger_history_list);
        mDataLoggerArrayList = new ArrayList<DataLoggerModel>();
        mDataLoggerModel = new DataLoggerModel();

        // Getting the directory CySmart
        mDirectory = Environment.getExternalStorageDirectory() + File.separator
                + getResources().getString(R.string.data_logger_directory);
        mFile = new File(mDirectory);

        String filePattern = ".txt";

        // Listing all files in the directory
        final File list[] = mFile.listFiles();
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (list[i].getName().toString().contains(filePattern)) {
                    Logger.i(list[i].getAbsolutePath());
                    mDataLoggerModel = new DataLoggerModel(list[i].getName(), list[i].lastModified(), list[i].getAbsolutePath());
                    mDataLoggerArrayList.add(mDataLoggerModel);
                }
            }
        }

        Collections.sort(mDataLoggerArrayList, new Comparator<DataLoggerModel>() {
            @Override
            public int compare(DataLoggerModel dataLoggerModel, DataLoggerModel dataLoggerModel2) {

//                return ((int) (dataLoggerModel2.getFileDate() - dataLoggerModel.getFileDate()));
                return dataLoggerModel2.getFileDate().compareTo(dataLoggerModel.getFileDate());
            }
        });

        // Adding data to adapter
        DataLoggerListAdapter adapter = new DataLoggerListAdapter(
                getActivity(), mDataLoggerArrayList);
        mListFileNames.setAdapter(adapter);
        mListFileNames.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
                                    long arg3) {
                /**
                 * Getting the absolute path. Adding the DataLogger fragment
                 * with the data of the file user selected
                 */
                String path = mDataLoggerArrayList.get(pos).getFilePath();
                Logger.i("Selected file path" + mDataLoggerArrayList.get(pos).getFilePath());
                Bundle bundle = new Bundle();
                bundle.putString(Constants.DATA_LOGGER_FILE_NAAME, path);
                bundle.putBoolean(Constants.DATA_LOGGER_FLAG, true);
                FragmentManager fragmentManager = getFragmentManager();
                Fragment currentFragment = fragmentManager.findFragmentById(R.id.container);
                DataLoggerFragment dataloggerfragment = new DataLoggerFragment()
                        .create(currentFragment.getTag());
                dataloggerfragment.setArguments(bundle);
                fragmentManager.beginTransaction()
                        .add(R.id.container, dataloggerfragment)
                        .addToBackStack(null).commit();
            }
        });
        return rootView;
    }

    @Override
    public void onPause() {
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.data_logger));
        super.onPause();
    }

    @Override
    public void onResume() {
        Utils.setUpActionBar(getActivity(),
               Constants.DATALOGER_HISTORY);
        super.onResume();
    }
}
