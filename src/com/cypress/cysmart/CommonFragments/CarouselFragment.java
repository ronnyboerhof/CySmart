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

import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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
import com.cypress.cysmart.BLEServiceFragments.IridiumBurstService;
import com.cypress.cysmart.BLEServiceFragments.IridiumStatusService;
import com.cypress.cysmart.BLEServiceFragments.IridiumMessageService;
import com.cypress.cysmart.BLEServiceFragments.RGBFragment;
import com.cypress.cysmart.BLEServiceFragments.RSCService;
import com.cypress.cysmart.BLEServiceFragments.SensorHubService;
import com.cypress.cysmart.CommonUtils.CarouselLinearLayout;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.GATTDBFragments.GattServicesFragment;
import com.cypress.cysmart.OTAFirmwareUpdate.OTAFirmwareUpgradeFragment;
import com.cypress.cysmart.R;
import com.cypress.cysmart.RDKEmulatorView.RemoteControlEmulatorFragment;

import java.util.HashMap;
import java.util.List;

public class CarouselFragment extends Fragment {

    public final static String EXTRA_FRAG_DEVICE_ADDRESS = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_DEVICE_ADDRESS";
    private final static HashMap<String, BluetoothGattService> bleHashMap = new HashMap<String, BluetoothGattService>();
    /**
     * Argument keys passed between fragments
     */
    private final static String EXTRA_FRAG_POS = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_POS";
    private final static String EXTRA_FRAG_SCALE = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_SCALE";
    private final static String EXTRA_FRAG_NAME = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_NAME";
    private final static String EXTRA_FRAG_UUID = "com.cypress.cysmart.fragments.CarouselFragment.EXTRA_FRAG_UUID";
    /**
     * BluetoothGattCharacteristic Notify
     */
    public static BluetoothGattCharacteristic mNotifyCharacteristic;
    /**
     * BluetoothGattCharacteristic Read
     */
    public static BluetoothGattCharacteristic mReadCharacteristic;
    /**
     * BluetoothGattService current
     */
    private static BluetoothGattService service;
    /**
     * Current UUID
     */
    private static String mCurrentUUID;
    /**
     * BluetoothGattCharacteristic List length
     */
    int mgattCharacteristicsLength = 0;
    /**
     * BluetoothGattCharacteristic current
     */
    BluetoothGattCharacteristic mCurrentCharacteristic;
    /**
     * CarouselView Image is actually a button
     */
    private Button mCarouselButton;

    /**
     * Fragment new Instance creation with arguments
     *
     * @param pos
     * @param scale
     * @param name
     * @param uuid
     * @param service
     * @return CarouselFragment
     */
    public static Fragment newInstance(int pos,
                                       float scale, String name, String uuid, BluetoothGattService service) {
        CarouselFragment fragment = new CarouselFragment();
        if (service.getInstanceId() > 0) {
            uuid = uuid + service.getInstanceId();
        }
        bleHashMap.put(uuid, service);
        Bundle b = new Bundle();
        b.putInt(EXTRA_FRAG_POS, pos);
        b.putFloat(EXTRA_FRAG_SCALE, scale);
        b.putString(EXTRA_FRAG_NAME, name);
        b.putString(EXTRA_FRAG_UUID, uuid);
        fragment.setArguments(b);
        return fragment;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.carousel_fragment_item, container, false);

        final int pos = this.getArguments().getInt(EXTRA_FRAG_POS);
        final String name = this.getArguments().getString(EXTRA_FRAG_NAME);
        final String uuid = this.getArguments().getString(EXTRA_FRAG_UUID);

        TextView tv = (TextView) rootView.findViewById(R.id.text);
        tv.setText(name);


        if (name.equalsIgnoreCase(getResources().getString(
                R.string.profile_control_unknown_service))) {
            service = bleHashMap.get(uuid);
            mCurrentUUID = service.getUuid().toString();
            TextView tv_uuid = (TextView) rootView.findViewById(R.id.text_uuid);
            tv_uuid.setText(mCurrentUUID);
        }

        mCarouselButton = (Button) rootView.findViewById(R.id.content);
        mCarouselButton.setBackgroundResource(pos);
        mCarouselButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // Getting the Mapped service from the UUID
                service = bleHashMap.get(uuid);
                mCurrentUUID = service.getUuid().toString();

                // Heart rate service
                if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.HEART_RATE_SERVICE)) {
                    HeartRateService heartRateMeasurement = new HeartRateService()
                            .create(service);
                    displayView(heartRateMeasurement, getResources().getString(R.string.heart_rate));

                }
                // Device information service
                else if (service
                        .getUuid()
                        .toString()
                        .equalsIgnoreCase(
                                GattAttributes.DEVICE_INFORMATION_SERVICE)) {
                    DeviceInformationService deviceInformationMeasurementFragment = new DeviceInformationService()
                            .create(service);
                    displayView(deviceInformationMeasurementFragment, getResources().getString(R.string.device_info));

                }
                // Battery service
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.BATTERY_SERVICE)) {
                    BatteryInformationService batteryInfoFragment = new BatteryInformationService()
                            .create(service);
                    displayView(batteryInfoFragment, getResources().getString(R.string.battery_info_fragment));
                }
                // Health Temperature Measurement
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.HEALTH_TEMP_SERVICE)) {
                    HealthTemperatureService healthTempMeasurement = new HealthTemperatureService()
                            .create(service);
                    displayView(healthTempMeasurement, getResources().getString(R.string.health_thermometer_fragment));

                }
                // Find Me
                else if (service
                        .getUuid()
                        .toString()
                        .equalsIgnoreCase(
                                GattAttributes.IMMEDIATE_ALERT_SERVICE)) {
                    FindMeService findMeService = new FindMeService().create(
                            service,
                            ProfileScanningFragment.gattServiceFindMeData, name);
                    displayView(findMeService, getResources().getString(R.string.findme_fragment));
                }
                // Proximity
                else if (service
                        .getUuid()
                        .toString()
                        .equalsIgnoreCase(
                                GattAttributes.LINK_LOSS_SERVICE)
                        || service
                        .getUuid()
                        .toString()
                        .equalsIgnoreCase(
                                GattAttributes.TRANSMISSION_POWER_SERVICE)) {
                    FindMeService findMeService = new FindMeService().create(
                            service,
                            ProfileScanningFragment.gattServiceProximityData, name);
                    displayView(findMeService, getResources().getString(R.string.proximity_fragment));
                }
                // CapSense
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.CAPSENSE_SERVICE) || service.getUuid().toString().equalsIgnoreCase(GattAttributes.CAPSENSE_SERVICE_CUSTOM)) {
                    List<BluetoothGattCharacteristic> gattCapSenseCharacteristics = service
                            .getCharacteristics();
                    CapsenseService capSensePager = new CapsenseService()
                            .create(service, gattCapSenseCharacteristics.size());
                    displayView(capSensePager, getResources().getString(R.string.capsense));
                }
                // GattDB
                else if (service
                        .getUuid()
                        .toString()
                        .equalsIgnoreCase(
                                GattAttributes.GENERIC_ATTRIBUTE_SERVICE)
                        || service
                        .getUuid()
                        .toString()
                        .equalsIgnoreCase(
                                GattAttributes.GENERIC_ACCESS_SERVICE)) {
                    GattServicesFragment gattsericesfragment = new GattServicesFragment()
                            .create();
                    displayView(gattsericesfragment, getResources().getString(R.string.gatt_db));

                }
                // RGB Service
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.RGB_LED_SERVICE) || service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.RGB_LED_SERVICE_CUSTOM)) {

                    RGBFragment rgbfragment = new RGBFragment().create(service);
                    displayView(rgbfragment, getResources().getString(R.string.rgb_led));

                }
                // Glucose Service
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.GLUCOSE_SERVICE)) {
                    if (Constants.GMS_ENABLED) {
                        GlucoseService glucosefragment = new GlucoseService()
                                .create(service);
                        displayView(glucosefragment, getResources().getString(R.string.glucose_fragment));
                    } else {
                        showWarningMessage();
                    }


                }
                // Blood Pressure Service
                else if (service
                        .getUuid()
                        .toString()
                        .equalsIgnoreCase(GattAttributes.BLOOD_PRESSURE_SERVICE)) {
                    BloodPressureService bloodPressureService = new BloodPressureService()
                            .create(service);
                    displayView(bloodPressureService, getResources().getString(R.string.blood_pressure));

                }
                // Running service
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.RSC_SERVICE)) {
                    RSCService rscService = new RSCService().create(service);
                    displayView(rscService, getResources().getString(R.string.rsc_fragment));

                }
                // Cycling service
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.CSC_SERVICE)) {
                    CSCService cscService = new CSCService().create(service);
                    displayView(cscService, getResources().getString(R.string.csc_fragment));
                }
                // Barometer(SensorHub) Service
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.BAROMETER_SERVICE)) {
                    SensorHubService sensorHubService = new SensorHubService()
                            .create(service,
                                    ProfileScanningFragment.gattServiceSensorHubData);
                    displayView(sensorHubService, getResources().getString(R.string.sen_hub));
                }
                // HID(Remote Control Emulator) Service
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.HUMAN_INTERFACE_DEVICE_SERVICE)) {
                    String connectedDeviceName = BluetoothLeService.getmBluetoothDeviceName();
                    String remoteName = getResources().getString(R.string.rdk_emulator_view);
                    if (connectedDeviceName.indexOf(remoteName) != -1) {
                        if (Constants.RDK_ENABLED) {
                            RemoteControlEmulatorFragment remoteControlEmulatorFragment =
                                    new RemoteControlEmulatorFragment()
                                            .create(service);
                            displayView(remoteControlEmulatorFragment, getResources().getString(R.string.rdk_emulator_view));
                        } else {
                            showWarningMessage();
                        }

                    } else {
                        showWarningMessage();
                    }

                }
                // OTA Firmware Update Service
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.OTA_UPDATE_SERVICE)) {
                    if (Constants.OTA_ENABLED) {
//                        OTAUpgradeTypeSelectionFragment firmwareTypeService = new OTAUpgradeTypeSelectionFragment()
//                                .create(service, false);
                        OTAFirmwareUpgradeFragment firmwareUpgradeFragment = new OTAFirmwareUpgradeFragment().
                                create(service);
                        displayView(firmwareUpgradeFragment, getResources().getString(R.string.ota_upgrade));
                    } else {
                        showWarningMessage();
                    }
                }
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.IRIDIUM_BURST_SERVICE)) {
                    IridiumBurstService iridiumBurstFragment = new IridiumBurstService()
                            .create(service);
                    displayView(iridiumBurstFragment, getResources().getString(R.string.iridium_burst));
                }
                else if (service.getUuid().toString()
                            .equalsIgnoreCase(GattAttributes.IRIDIUM_STATUS_SERVICE)) {
                        IridiumStatusService iridiumStatusFragment = new IridiumStatusService()
                                .create(service);
                        displayView(iridiumStatusFragment, getResources().getString(R.string.iridium_status));
                }
                else if (service.getUuid().toString()
                        .equalsIgnoreCase(GattAttributes.IRIDIUM_MESSAGE_SERVICE)) {
                    IridiumMessageService iridiumMessageFragment = new IridiumMessageService()
                            .create(service);
                    displayView(iridiumMessageFragment, getResources().getString(R.string.iridium_messages));
                }
            }
        });
        CarouselLinearLayout root = (CarouselLinearLayout) rootView
                .findViewById(R.id.root);
        float scale = this.getArguments().getFloat(EXTRA_FRAG_SCALE);
        root.setScaleBoth(scale);
        return rootView;

    }


    /**
     * Used for replacing the main content of the view with provided fragments
     *
     * @param fragment
     */
    void displayView(Fragment fragment, String mTagName) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().add(R.id.container, fragment, mTagName)
                .addToBackStack(null).commit();
    }

    void showWarningMessage() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getActivity());
        // set title
        alertDialogBuilder
                .setTitle(R.string.alert_message_unknown_title);
        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.alert_message_unkown)
                .setCancelable(false)
                .setPositiveButton(R.string.alert_message_yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                GattServicesFragment gattsericesfragment = new GattServicesFragment()
                                        .create();
                                displayView(gattsericesfragment, getResources().getString(R.string.gatt_db));
                            }
                        })
                .setNegativeButton(R.string.alert_message_no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
