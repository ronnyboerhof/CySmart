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

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattService;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.cypress.cysmart.R;

/**
 * Fragment to display the CapSense Proximity
 */
public class CapsenseServiceProximity extends Fragment {

    // GATT Service
    private static BluetoothGattService mservice;

    // Data variables
    private static ImageView mproximityViewForeground;
    private static ImageView mproximityViewBackground;
    private AlertDialog alert;
    private static MediaPlayer player;

    private static final int PROXIMITY_WATERMARK_LOW = 0;
    private static final int PROXIMITY_WATERMARK_MAX = 255;
    private static final int PROXIMITY_WATERMARK_INDICATOR = 127;
    private static boolean valueIncreased = false;



    public CapsenseServiceProximity create(BluetoothGattService service) {
        CapsenseServiceProximity fragment = new CapsenseServiceProximity();
        mservice = service;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.capsense_proximity,
                container, false);
        mproximityViewForeground = (ImageView) rootView
                .findViewById(R.id.proximity_view_1);
        mproximityViewBackground = (ImageView) rootView
                .findViewById(R.id.proximity_view_2);
        player = MediaPlayer.create(getActivity(), R.raw.beep);

        setHasOptionsMenu(true);

        return rootView;
    }

    /**
     * Display the proximity value
     *
     * @param proximity_value
     */
    public static void displayLiveData(int proximity_value) {
        int priximity2value = 255 - proximity_value;
        mproximityViewBackground.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, proximity_value));
        mproximityViewForeground.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, priximity2value));

        if (proximity_value >= PROXIMITY_WATERMARK_INDICATOR) {
            try {
                if (valueIncreased) {
                    player.start();
                    valueIncreased = false;
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (player != null && player.isPlaying()) {//If music is playing already
                    player.stop();//Stop playing the music
                }

            }

        }
        if (proximity_value >= PROXIMITY_WATERMARK_LOW && proximity_value <= PROXIMITY_WATERMARK_INDICATOR) {
            valueIncreased = true;
        }


    }

    @Override
    public void onResume() {
        super.onResume();

        player = MediaPlayer.create(getActivity(), R.raw.beep);
        valueIncreased = true;
    }

    @Override
    public void onPause() {
        if (player != null) {
            player.stop();
        }
        valueIncreased = false;
        super.onPause();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

}
