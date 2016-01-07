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
package com.cypress.cysmart.ListAdapters;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonFragments.CarouselFragment;
import com.cypress.cysmart.CommonFragments.ProfileControlFragment;
import com.cypress.cysmart.CommonUtils.CarouselLinearLayout;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.HomePageActivity;
import com.cypress.cysmart.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Adapter class for the CarouselView. extends FragmentPagerAdapter
 */
public class CarouselPagerAdapter extends FragmentPagerAdapter implements
        ViewPager.OnPageChangeListener {

    /**
     * CarouselLinearLayout variables for animation
     */
    private CarouselLinearLayout mCurrentCarouselLinearLayout = null;
    private CarouselLinearLayout mNextCarouselLinearLayout = null;
    private HomePageActivity context;
    private ProfileControlFragment containerFragment;
    private FragmentManager fm;
    private float scale;

    private ArrayList<HashMap<String, BluetoothGattService>> currentServiceData;

    public CarouselPagerAdapter(Activity context,
                                ProfileControlFragment containerFragment,
                                FragmentManager fragmentManager,
                                ArrayList<HashMap<String, BluetoothGattService>> currentServiceData) {
        super(fragmentManager);
        this.fm = fragmentManager;
        this.context = (HomePageActivity) context;
        this.containerFragment = containerFragment;
        this.currentServiceData = currentServiceData;

    }

    @Override
    public Fragment getItem(int position) {

        // Make the first pager bigger than others
        if (position == ProfileControlFragment.FIRST_PAGE) {
            scale = ProfileControlFragment.BIG_SCALE;
        } else {
            scale = ProfileControlFragment.SMALL_SCALE;
        }
        position = position % ProfileControlFragment.PAGES;
        HashMap<String, BluetoothGattService> item = currentServiceData
                .get(position);
        BluetoothGattService bgs = item.get("UUID");

        /**
         * Looking for the image corresponding to the UUID.if no suitable image
         * resource is found assign the default unknown resource
         */

        int imageId = GattAttributes.lookupImage(bgs.getUuid().toString()
        );
        String name = GattAttributes.lookup(
                bgs.getUuid().toString(),
                context.getResources().getString(
                        R.string.profile_control_unknown_service));
        String uuid = bgs.getUuid().toString();
        if (uuid.equalsIgnoreCase(GattAttributes.IMMEDIATE_ALERT_SERVICE)) {
            name = context.getResources().getString(R.string.findme_fragment);
        }
        if (uuid.equalsIgnoreCase(GattAttributes.LINK_LOSS_SERVICE)
                || uuid.equalsIgnoreCase(GattAttributes.TRANSMISSION_POWER_SERVICE)) {
            name = context.getResources().getString(R.string.proximity_fragment);
        }
        if (uuid.equalsIgnoreCase(GattAttributes.CAPSENSE_SERVICE) ||
                uuid.equalsIgnoreCase(GattAttributes.CAPSENSE_SERVICE_CUSTOM)) {
            List<BluetoothGattCharacteristic> gattCharacteristics = bgs
                    .getCharacteristics();
            if (gattCharacteristics.size() > 1) {
                imageId = GattAttributes.lookupImageCapSense(bgs.getUuid()
                        .toString());
                name = GattAttributes.lookupNameCapSense(
                        bgs.getUuid().toString(),
                        context.getResources().getString(
                                R.string.profile_control_unknown_service));
            } else {
                String characteristicUUID = gattCharacteristics.get(0)
                        .getUuid().toString();
                imageId = GattAttributes.lookupImageCapSense(
                        characteristicUUID);
                name = GattAttributes.lookupNameCapSense(
                        characteristicUUID,
                        context.getResources().getString(
                                R.string.profile_control_unknown_service));
            }
        }
        if (uuid.equalsIgnoreCase(GattAttributes.GENERIC_ACCESS_SERVICE)
                || uuid.equalsIgnoreCase(GattAttributes.GENERIC_ATTRIBUTE_SERVICE)) {
            name = context.getResources().getString(R.string.gatt_db);
        }
        if (uuid.equalsIgnoreCase(GattAttributes.BAROMETER_SERVICE)
                || uuid.equalsIgnoreCase(GattAttributes.ACCELEROMETER_SERVICE)
                || uuid.equalsIgnoreCase(GattAttributes.ANALOG_TEMPERATURE_SERVICE)) {
            name = context.getResources().getString(R.string.sen_hub);
        }
        if (uuid.equalsIgnoreCase(GattAttributes.HUMAN_INTERFACE_DEVICE_SERVICE)){
            String connectedDeviceName= BluetoothLeService.getmBluetoothDeviceName();
            String remoteName=context.getString(R.string.rdk_emulator_view);
            if(connectedDeviceName.indexOf(remoteName)!=-1){
                name=context.getResources().getString(R.string.rdk_emulator_view);
                imageId=R.drawable.emulator;
            }

        }
        Fragment curFragment = CarouselFragment.newInstance(imageId,
                scale, name, uuid, bgs);
        return curFragment;
    }

    @Override
    public int getCount() {
        return ProfileControlFragment.PAGES * ProfileControlFragment.LOOPS;

    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
                               int positionOffsetPixels) {
        /**
         * Page scroll animation. Zooming forward and backward based on the
         * scroll
         */
        if (positionOffset >= 0f && positionOffset <= 1f) {
            if (position < getCount() - 1) {
                try {
                    mCurrentCarouselLinearLayout = getRootView(position);
                    mNextCarouselLinearLayout = getRootView(position + 1);
                    mCurrentCarouselLinearLayout
                            .setScaleBoth(ProfileControlFragment.BIG_SCALE
                                    - ProfileControlFragment.DIFF_SCALE
                                    * positionOffset);
                    mNextCarouselLinearLayout
                            .setScaleBoth(ProfileControlFragment.SMALL_SCALE
                                    + ProfileControlFragment.DIFF_SCALE
                                    * positionOffset);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private CarouselLinearLayout getRootView(int position) {
        CarouselLinearLayout ly;
        try {
            ly = (CarouselLinearLayout) fm
                    .findFragmentByTag(this.getFragmentTag(position)).getView()
                    .findViewById(R.id.root);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return ly;
    }

    private String getFragmentTag(int position) {
        return "android:switcher:" + containerFragment.pager.getId() + ":"
                + position;
    }
}
