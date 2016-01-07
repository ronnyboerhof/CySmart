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

package com.cypress.cysmart.BLEServiceFragments;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.List;

//Fragment to display the heart rate service
public class HeartRateService extends Fragment {

    // Data view variables
    private TextView mDataField_HRM;
    private TextView mDataField_HREE;
    private TextView mDataField_HRRR;
    private TextView mDataField_BSL;
    private ImageView mHeartView;

    // GATT service and characteristics
    private static BluetoothGattService mservice;
    private static BluetoothGattCharacteristic mNotifyCharacteristic;
    private static BluetoothGattCharacteristic mReadCharacteristic;

    // Flags
    private boolean HANDLER_FLAG = false;

    /**
     * aChart variables
     */
    private LinearLayout mGraphLayoutParent;
    private double mGraphLastXValue = 0;
    private double mPreviosTime=0;
    private double mCurrentTime=0;
    private GraphicalView mChart;
    private XYSeries mDataSeries ;

    //ProgressDialog
    private ProgressDialog mProgressDialog;

    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            // GATT Data available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // Check body sensor location
                if (extras.containsKey(Constants.EXTRA_BSL_VALUE)) {
                    String received_bsl_data = intent
                            .getStringExtra(Constants.EXTRA_BSL_VALUE);
                    displayBSLData(received_bsl_data);
                    prepareBroadcastDataNotify(mNotifyCharacteristic);
                }
                // Check heart rate
                if (extras.containsKey(Constants.EXTRA_HRM_VALUE)) {
                    String received_heart_rate = extras
                            .getString(Constants.EXTRA_HRM_VALUE);
                     displayHRMData(received_heart_rate);

                }
                // Check energy expended
                if (extras.containsKey(Constants.EXTRA_HRM_EEVALUE)) {
                    String received_heart_rate_ee = extras
                            .getString(Constants.EXTRA_HRM_EEVALUE);
                     displayHRMEEData(received_heart_rate_ee);

                }
                // Check rr interval
                if (extras.containsKey(Constants.EXTRA_HRM_RRVALUE)) {
                    ArrayList<Integer> received_rr_interval = extras
                            .getIntegerArrayList(Constants.EXTRA_HRM_RRVALUE);
                     displayHRMRRData(received_rr_interval);

                }

            }
            //Received when the bond state is changed
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDING) {
                    // Bonding...
                    String dataLog=getResources().getString(R.string.dl_commaseparator)
                            +"["+BluetoothLeService.getmBluetoothDeviceName()+"|"
                            +BluetoothLeService.getmBluetoothDeviceAddress()+"]"+
                            getResources().getString(R.string.dl_commaseparator)+
                            getResources().getString(R.string.dl_connection_pairing_request_received);
                    Logger.datalog(dataLog);
                    String dataLog2=getResources().getString(R.string.dl_commaseparator)
                            +"["+BluetoothLeService.getmBluetoothDeviceAddress()+"|"
                            +BluetoothLeService.getmBluetoothDeviceName()+"]"+
                            getResources().getString(R.string.dl_commaseparator)+
                            getResources().getString(R.string.dl_connection_pairing_request);
                    Logger.datalog(dataLog2);
                    Utils.bondingProgressDialog(getActivity(), mProgressDialog, true);
                } else if (state == BluetoothDevice.BOND_BONDED) {
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

    public HeartRateService create(BluetoothGattService service) {
        mservice = service;
        return new HeartRateService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.hrm_measurement, container,
                false);
        mDataField_HRM = (TextView) rootView.findViewById(R.id.hrm_heartrate);
        mDataField_HREE = (TextView) rootView.findViewById(R.id.heart_rate_ee);
        mDataField_HRRR = (TextView) rootView.findViewById(R.id.heart_rate_rr);
        mDataField_BSL = (TextView) rootView.findViewById(R.id.hrm_sensor_data);
        mProgressDialog = new ProgressDialog(getActivity());
        mHeartView = (ImageView) rootView.findViewById(R.id.heart_icon);
        setHasOptionsMenu(true);
        Animation pulse = AnimationUtils.loadAnimation(getActivity(),
                R.anim.pulse);
        mHeartView.startAnimation(pulse);
        // Setting up chart
        setupChart(rootView);
        return rootView;
    }

    private void displayBSLData(String hrm_body_sensor_data) {
        if (hrm_body_sensor_data != null) {
            mDataField_BSL.setText(hrm_body_sensor_data);
        }

    }

    private void displayHRMData(final String hrm_data) {
        if (hrm_data != null) {
            mDataField_HRM.setText(hrm_data);
            if(mCurrentTime==0){
                mGraphLastXValue=0;
                mCurrentTime=Utils.getTimeInSeconds();
            }else{
                mPreviosTime=mCurrentTime;
                mCurrentTime=Utils.getTimeInSeconds();
                mGraphLastXValue=mGraphLastXValue+(mCurrentTime-mPreviosTime)/1000;
            }
            double val = Integer.valueOf(hrm_data);
            mDataSeries.add(mGraphLastXValue,val);
            mChart.repaint();
        }

    }

    private void displayHRMEEData(String hrm_ee_data) {
        if (hrm_ee_data != null) {
            mDataField_HREE.setText(hrm_ee_data);
        }
    }

    private void displayHRMRRData(ArrayList<Integer> hrm_rr_data) {
        if (hrm_rr_data != null) {
            for (int i = 0; i < hrm_rr_data.size(); i++) {
                String data = String.valueOf(hrm_rr_data.get(i));
                mDataField_HRRR.setText(data);
            }

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        HANDLER_FLAG = true;
        getGattData();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.heart_rate));
    }

    @Override
    public void onDestroy() {
        HANDLER_FLAG = false;
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        stopBroadcastDataNotify(mNotifyCharacteristic);
        super.onDestroy();
    }

    /**
     * Stopping Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    void stopBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        final int charaProp = gattCharacteristic.getProperties();

        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            if (mNotifyCharacteristic != null) {
                Logger.d("Stopped notification");
                BluetoothLeService.setCharacteristicNotification(
                        mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }

        }

    }

    /**
     * Preparing Broadcast receiver to broadcast read characteristics
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataRead(
            BluetoothGattCharacteristic gattCharacteristic) {
        final int charaProp = gattCharacteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

            mReadCharacteristic = gattCharacteristic;
            BluetoothLeService.readCharacteristic(gattCharacteristic);
        }
    }

    /**
     * Preparing Broadcast receiver to broadcast notify characteristics
     *
     * @param gattCharacteristic
     */
    void prepareBroadcastDataNotify(
            BluetoothGattCharacteristic gattCharacteristic) {
        final int charaProp = gattCharacteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = gattCharacteristic;
            BluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                    true);
        }
    }

    /**
     * Method to get required characteristics from service
     */
    void getGattData() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mservice
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();
            if (uuidchara.equalsIgnoreCase(GattAttributes.BODY_SENSOR_LOCATION)) {
                mReadCharacteristic = gattCharacteristic;
                prepareBroadcastDataRead(mReadCharacteristic);
            }
            if (uuidchara
                    .equalsIgnoreCase(GattAttributes.HEART_RATE_MEASUREMENT)) {
                mNotifyCharacteristic = gattCharacteristic;

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
        graph.setVisible(true);
        log.setVisible(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        switch (item.getItemId()) {
            case R.id.graph:
                if (mGraphLayoutParent.getVisibility() != View.VISIBLE) {
                    mGraphLayoutParent.setVisibility(View.VISIBLE);
                } else {
                    mGraphLayoutParent.setVisibility(View.GONE);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    /**
     * Setting up the aChart Third party library
     * @param parent
     */
    private void setupChart(View parent){
        /**
         * Setting graph titles
         */
        String graphTitle= getResources().getString(R.string.hrm_graph_label);
        String graphXAxis=getResources().getString(R.string.health_temperature_time);
        String graphYAxis=getResources().getString(R.string.hrm_graph_label);


        // Creating an  XYSeries for temperature
        mDataSeries = new XYSeries(graphTitle);


        // Creating a dataset to hold each series
        XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

        // Adding temperature Series to the dataset
        mDataset.addSeries(mDataSeries);


        // Creating XYSeriesRenderer to customize
        XYSeriesRenderer mRenderer = new XYSeriesRenderer();
        mRenderer.setColor(getResources().getColor(R.color.main_bg_color));
        mRenderer.setPointStyle(PointStyle.CIRCLE);
        mRenderer.setFillPoints(true);
        mRenderer.setLineWidth(3);



        // Creating a XYMultipleSeriesRenderer to customize the whole chart
        XYMultipleSeriesRenderer mMultiRenderer = new XYMultipleSeriesRenderer();
        int deviceDPi=getResources().getDisplayMetrics().densityDpi;
        switch (deviceDPi) {
            case DisplayMetrics.DENSITY_XHIGH:
                mMultiRenderer.setMargins(new int[] { 40, 90, 25, 10 });
                mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_XHDPI);
                mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_XHDPI);
                mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_XHDPI);
                mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_XHDPI);
                break;
            case DisplayMetrics.DENSITY_HIGH:
                mMultiRenderer.setMargins(new int[] { 30, 50, 25, 10 });
                mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_HDPI);
                mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_HDPI);
                mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_HDPI);
                mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_HDPI);
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                mMultiRenderer.setMargins(new int[] { 50, 100, 35, 20 });
                mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_XXHDPI);
                mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_XXHDPI);
                mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_XXHDPI);
                mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_XXHDPI);
                break;
            default:
                if(deviceDPi>DisplayMetrics.DENSITY_XXHIGH&&deviceDPi<
                        DisplayMetrics.DENSITY_XXXHIGH){
                    mMultiRenderer.setMargins(new int[] { 50, 100, 35, 20 });
                    mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_XXHDPI);
                    mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_XXHDPI);
                    mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_XXHDPI);
                    mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_XXHDPI);
                }else{
                    mMultiRenderer.setMargins(new int[] { 30, 50, 25, 10 });
                    mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_LDPI);
                    mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_LDPI);
                    mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_LDPI);
                    mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_LDPI);
                }
                break;
        }
        mMultiRenderer.setXTitle(graphXAxis);
        mMultiRenderer.setLabelsColor(Color.BLACK);
        mMultiRenderer.setYTitle(graphYAxis);
        mMultiRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        mMultiRenderer.setPanEnabled(true, true);
        mMultiRenderer.setYLabelsColor(0, Color.BLACK);
        mMultiRenderer.setXLabelsColor(Color.BLACK);
        mMultiRenderer.setApplyBackgroundColor(true);
        mMultiRenderer.setBackgroundColor(Color.WHITE);
        mMultiRenderer.setGridColor(Color.BLACK);
        mMultiRenderer.setShowGrid(true);
        mMultiRenderer.setShowLegend(false);


        // Adding mRenderer to multipleRenderer
        mMultiRenderer.addSeriesRenderer(mRenderer);

        // Getting a reference to LinearLayout of the MainActivity Layout
        mGraphLayoutParent = (LinearLayout)parent.findViewById(R.id.chart_container);


        mChart = ChartFactory.getLineChartView(getActivity(),
                mDataset, mMultiRenderer);


        // Adding the Line Chart to the LinearLayout
        mGraphLayoutParent.addView(mChart);

    }
}
