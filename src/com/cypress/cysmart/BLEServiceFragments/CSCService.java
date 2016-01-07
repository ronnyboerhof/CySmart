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
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.DecimalTextWatcher;
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;


/**
 * Fragment to display the Cycling speed cadence
 */
public class CSCService extends Fragment {

    // GATT Service and Characteristics
    private static BluetoothGattService mservice;
    private static BluetoothGattCharacteristic mNotifyCharacteristic;


    // Data field variables
    private TextView mDistanceRan;
    private TextView mCadence;
    private TextView mCaloriesBurnt;
    private Chronometer mTimer;
    private EditText mWeightEdittext;

    //ProgressDialog
    private ProgressDialog mProgressDialog;

    private float weightInt;
    private String weightString;

    /**
     * aChart variables
     */
    private LinearLayout mGraphLayoutParent;
    private double mGraphLastXValue = 0;
    private double mPreviosTime = 0;
    private double mCurrentTime = 0;
    private GraphicalView mChart;
    private XYSeries mDataSeries;

    private boolean HANDLER_FLAG = false;
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
                // Check CSC value
                if (extras.containsKey(Constants.EXTRA_CSC_VALUE)) {
                    ArrayList<String> received_csc_data = intent
                            .getStringArrayListExtra(Constants.EXTRA_CSC_VALUE);
                    displayLiveData(received_csc_data);
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

    public CSCService create(BluetoothGattService service) {
        mservice = service;
        return new CSCService();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.cyclingspeed_n_cadence,
                container, false);
        mDistanceRan = (TextView) rootView.findViewById(R.id.cycling_distance);
        mCadence = (TextView) rootView.findViewById(R.id.cadence);
        mCaloriesBurnt = (TextView) rootView.findViewById(R.id.calories_burnt);
        mTimer = (Chronometer) rootView.findViewById(R.id.time_counter);
        mWeightEdittext = (EditText) rootView.findViewById(R.id.weight_data);
        mWeightEdittext.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        mWeightEdittext.addTextChangedListener(new DecimalTextWatcher(mWeightEdittext));
        mProgressDialog = new ProgressDialog(getActivity());

        // Setting up chart
        setupChart(rootView);

        // Start/Stop listener
        Button start_stop_btn = (Button) rootView
                .findViewById(R.id.start_stop_btn);
        start_stop_btn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Button btn = (Button) v;
                String buttonText = btn.getText().toString();
                String startText = getResources().getString(
                        R.string.blood_pressure_start_btn);
                String stopText = getResources().getString(
                        R.string.blood_pressure_stop_btn);
                weightString = mWeightEdittext.getText().toString();

                if (weightString.equalsIgnoreCase("")) {
                    weightInt = 0;
                    if (buttonText.equalsIgnoreCase(startText)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.csc_weight_toast_empty), Toast.LENGTH_SHORT).show();
                        mCaloriesBurnt.setText("0.00");
                    }
                }

                if (weightString.equalsIgnoreCase(".") || weightString.equalsIgnoreCase("0.") || weightString.equalsIgnoreCase("0")) {
                    weightInt = 0;
                    if (buttonText.equalsIgnoreCase(startText)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.csc_weight_toast_empty), Toast.LENGTH_SHORT).show();
                    }
                }

                if (weightInt <= 1.0 && weightInt != 0) {
                    if (buttonText.equalsIgnoreCase(startText)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.csc_weight_toast_zero), Toast.LENGTH_SHORT).show();
                        mCaloriesBurnt.setText("0.00");
                    }
                }

                if (weightInt <= 200) {
                    if (buttonText.equalsIgnoreCase(startText)) {
                        btn.setText(stopText);
                        mCaloriesBurnt.setText("0.00");
                        mWeightEdittext.setEnabled(false);
                        if (mNotifyCharacteristic != null) {
                            stopBroadcastDataNotify(mNotifyCharacteristic);

                        }
                        getGattData();
                        mTimer.start();
                        mTimer.setBase(SystemClock.elapsedRealtime());
                        mTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                            @Override
                            public void onChronometerTick(Chronometer chronometer) {
                                showCaloriesBurnt();
                            }
                        });
                    } else {
                        mWeightEdittext.setEnabled(true);
                        btn.setText(startText);
                        stopBroadcastDataNotify(mNotifyCharacteristic);
                        mTimer.stop();
                    }
                } else {
                    if (buttonText.equalsIgnoreCase(startText)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.csc_weight_toast_greater), Toast.LENGTH_SHORT).show();
                        btn.setText(stopText);
                        mCaloriesBurnt.setText("0.00");
                        mWeightEdittext.setEnabled(false);
                        if (mNotifyCharacteristic != null) {
                            stopBroadcastDataNotify(mNotifyCharacteristic);
                        }
                        getGattData();
                        mTimer.start();
                        mTimer.setBase(SystemClock.elapsedRealtime());
                        mTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                            @Override
                            public void onChronometerTick(Chronometer chronometer) {
                                showCaloriesBurnt();
                            }
                        });
                    } else {
                        mWeightEdittext.setEnabled(true);
                        btn.setText(startText);
                        stopBroadcastDataNotify(mNotifyCharacteristic);
                        mCaloriesBurnt.setText("0.00");
                        mTimer.stop();
                    }
                }

            }
        });
        setHasOptionsMenu(true);
        return rootView;
    }

    /**
     * Display live cycling data
     */
    private void displayLiveData(final ArrayList<String> csc_data) {
        if (csc_data != null) {
            weightString = mWeightEdittext.getText().toString();
            try {
                Number cycledDist = NumberFormat.getInstance().parse(
                        csc_data.get(0));
                NumberFormat distformatter = NumberFormat.getNumberInstance();
                distformatter.setMinimumFractionDigits(2);
                distformatter.setMaximumFractionDigits(2);
                String distanceRanInt = distformatter.format(cycledDist);
                mDistanceRan.setText(distanceRanInt);
                mCadence.setText(csc_data.get(1));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            if (mCurrentTime == 0) {
                mGraphLastXValue = 0;
                mCurrentTime = Utils.getTimeInSeconds();
            } else {
                mPreviosTime = mCurrentTime;
                mCurrentTime = Utils.getTimeInSeconds();
                mGraphLastXValue = mGraphLastXValue + (mCurrentTime - mPreviosTime) / 1000;
            }
            try {
                float val = Float.valueOf(csc_data.get(1));
                mDataSeries.add(mGraphLastXValue, val);
                mChart.repaint();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private float showElapsedTime() {
        float SECOND = 1000;
        float MINUTE = 60 * SECOND;
        float elapsedMillis = SystemClock.elapsedRealtime() - mTimer.getBase();
        elapsedMillis = elapsedMillis / MINUTE;
        return elapsedMillis;
    }

    @Override
    public void onResume() {
        super.onResume();
        HANDLER_FLAG = true;
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.csc_fragment));
    }

    @Override
    public void onPause() {
        super.onPause();
        HANDLER_FLAG = false;
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        if (mNotifyCharacteristic != null) {
            stopBroadcastDataNotify(mNotifyCharacteristic);
        }
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
            if (uuidchara.equalsIgnoreCase(GattAttributes.CSC_MEASUREMENT)) {
                mNotifyCharacteristic = gattCharacteristic;
                prepareBroadcastDataNotify(mNotifyCharacteristic);
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
     *
     * @param parent
     */
    private void setupChart(View parent) {
        /**
         * Setting graph titles
         */
        String graphTitle = getResources().getString(R.string.csc_fragment);
        String graphXAxis = getResources().getString(R.string.health_temperature_time);
        String graphYAxis = getResources().getString(R.string.csc_cadence_graph);


        // Creating an  XYSeries for running speed
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
        mRenderer.setLineWidth(5);

        // Creating a XYMultipleSeriesRenderer to customize the whole chart
        XYMultipleSeriesRenderer mMultiRenderer = new XYMultipleSeriesRenderer();
        int deviceDPi = getResources().getDisplayMetrics().densityDpi;
        switch (getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_XHIGH:
                mMultiRenderer.setMargins(new int[]{40, 90, 25, 10});
                mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_XHDPI);
                mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_XHDPI);
                mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_XHDPI);
                mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_XHDPI);
                break;
            case DisplayMetrics.DENSITY_HIGH:
                mMultiRenderer.setMargins(new int[]{30, 50, 25, 10});
                mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_HDPI);
                mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_HDPI);
                mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_HDPI);
                mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_HDPI);
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                mMultiRenderer.setMargins(new int[]{50, 100, 35, 20});
                mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_XXHDPI);
                mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_XXHDPI);
                mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_XXHDPI);
                mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_XXHDPI);
                break;

            default:
                if (deviceDPi > DisplayMetrics.DENSITY_XXHIGH && deviceDPi <
                        DisplayMetrics.DENSITY_XXXHIGH) {
                    mMultiRenderer.setMargins(new int[]{50, 100, 35, 20});
                    mMultiRenderer.setAxisTitleTextSize(Constants.TEXT_SIZE_XXHDPI);
                    mMultiRenderer.setChartTitleTextSize(Constants.TEXT_SIZE_XXHDPI);
                    mMultiRenderer.setLabelsTextSize(Constants.TEXT_SIZE_XXHDPI);
                    mMultiRenderer.setLegendTextSize(Constants.TEXT_SIZE_XXHDPI);
                } else {
                    mMultiRenderer.setMargins(new int[]{30, 50, 25, 10});
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
        mMultiRenderer.setYAxisMin(0);
        mMultiRenderer.setXAxisMin(0);
        mMultiRenderer.setMarginsColor(Color.argb(0x00, 0xff, 0x00, 0x00));
        mMultiRenderer.setPanEnabled(true, true);
        mMultiRenderer.setZoomEnabled(false, false);
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
        mGraphLayoutParent = (LinearLayout) parent.findViewById(R.id.chart_container);


        mChart = ChartFactory.getLineChartView(getActivity(),
                mDataset, mMultiRenderer);


        // Adding the Line Chart to the LinearLayout
        mGraphLayoutParent.addView(mChart);

    }

    private void showCaloriesBurnt(){
        try {
            Number numWeight = NumberFormat.getInstance().parse(weightString);
            weightInt = numWeight.floatValue();
            float caloriesBurntInt = (((showElapsedTime()) * weightInt) * 8);
            caloriesBurntInt = caloriesBurntInt / 1000;
            NumberFormat formatter = NumberFormat.getNumberInstance();
            formatter.setMinimumFractionDigits(4);
            formatter.setMaximumFractionDigits(4);
            String finalBurn = formatter.format(caloriesBurntInt);
            mCaloriesBurnt.setText(finalBurn);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
