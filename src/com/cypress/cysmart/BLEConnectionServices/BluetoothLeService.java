/*
 * Copyright Cypress Semiconductor Corporation, 2014-2014-2015 All rights reserved.
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

package com.cypress.cysmart.BLEConnectionServices;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.cypress.cysmart.BLEProfileDataParserClasses.BloodPressureParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.CSCParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.CapSenseParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.DescriptorParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.GlucoseParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.HRMParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.HTMParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.RGBParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.RSCParser;
import com.cypress.cysmart.BLEProfileDataParserClasses.SensorHubParser;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.UUIDDatabase;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given BlueTooth LE device.
 */
public class BluetoothLeService extends Service {

    /**
     * GATT Status constants
     */
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_DISCONNECTED_CAROUSEL =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_CAROUSEL";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_OTA_DATA_AVAILABLE =
            "com.cysmart.bluetooth.le.ACTION_OTA_DATA_AVAILABLE";
    public final static String ACTION_GATT_DISCONNECTED_OTA =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_OTA";
    public final static String ACTION_GATT_CONNECT_OTA =
            "com.example.bluetooth.le.ACTION_GATT_CONNECT_OTA";
    public final static String ACTION_GATT_SERVICES_DISCOVERED_OTA =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED_OTA";
    public final static String ACTION_GATT_CHARACTERISTIC_ERROR =
            "com.example.bluetooth.le.ACTION_GATT_CHARACTERISTIC_ERROR";
    /**
     * Connection status Constants
     */
    public static final int STATE_DISCONNECTED = 0;
    private final static String ACTION_GATT_DISCONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTING";
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_DISCONNECTING = 4;
    private static final int STATE_BONDED = 5;
    /**
     * BluetoothAdapter for handling connections
     */
    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothGatt mBluetoothGatt;
    private static int mConnectionState = STATE_DISCONNECTED;
    private static boolean m_otaExitBootloaderCmdInProgress = false;
    /**
     * Device address
     */
    private static String mBluetoothDeviceAddress;
    private static String mBluetoothDeviceName;
    private static Context mContext;
    /**
     * Implements callback methods for GATT events that the app cares about. For
     * example,connection change and services discovered.
     */
    private final static BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {

            Logger.i("onConnectionStateChange");
            String intentAction;
            // GATT Server connected
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastConnectionUpdate(intentAction);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_established);
                Logger.datalog(dataLog);
            }
            // GATT Server disconnected
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                broadcastConnectionUpdate(intentAction);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_disconnected);
                Logger.datalog(dataLog);
            }
            // GATT Server disconnected
            else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                intentAction = ACTION_GATT_DISCONNECTING;
                mConnectionState = STATE_DISCONNECTING;
                broadcastConnectionUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // GATT Services discovered
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String dataLog2 = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_service_discovery_status) +
                        mContext.getResources().getString(R.string.dl_status_success);
                Logger.datalog(dataLog2);
                broadcastConnectionUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_service_discovery_status) +
                        mContext.getResources().
                                getString(R.string.dl_status_failure) + status;
                Logger.datalog(dataLog);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
            String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

            String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
            String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);

            String descriptorUUID = descriptor.getUuid().toString();
            String descriptorName = GattAttributes.lookup(descriptorUUID, descriptorUUID);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[00]";
                Logger.datalog(dataLog);
            } else {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_status_failure) +
                        +status;
                Logger.datalog(dataLog);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                     int status) {
            String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
            String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

            String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
            String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);

            String descriptorUUIDText = descriptor.getUuid().toString();
            String descriptorName = GattAttributes.lookup(descriptorUUIDText, descriptorUUIDText);

            String descriptorValue = " " + Utils.ByteArraytoHex(descriptor.getValue()) + " ";
            if (status == BluetoothGatt.GATT_SUCCESS) {
                UUID descriptorUUID = descriptor.getUuid();
                final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                Bundle mBundle = new Bundle();
                // Putting the byte value read for GATT Db
                mBundle.putByteArray(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE,
                        descriptor.getValue());
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_response) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + descriptorValue + "]";
                Logger.datalog(dataLog);
                mBundle.putString(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE_UUID,
                        descriptor.getUuid().toString());
                mBundle.putString(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE_CHARACTERISTIC_UUID,
                        descriptor.getCharacteristic().getUuid().toString());
                if (descriptorUUID.equals(UUIDDatabase.UUID_CLIENT_CHARACTERISTIC_CONFIG)) {
                    String valueReceived = DescriptorParser
                            .getClientCharacteristicConfiguration(descriptor);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, valueReceived);
                }
                if (descriptorUUID.equals(UUIDDatabase.UUID_CHARACTERISTIC_EXTENDED_PROPERTIES)) {
                    HashMap<String, String> receivedValuesMap = DescriptorParser
                            .getCharacteristicExtendedProperties(descriptor);
                    String reliableWriteStatus = receivedValuesMap.get(Constants.firstBitValueKey);
                    String writeAuxillaryStatus = receivedValuesMap.get(Constants.secondBitValueKey);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, reliableWriteStatus + "\n"
                            + writeAuxillaryStatus);
                }
                if (descriptorUUID.equals(UUIDDatabase.UUID_CHARACTERISTIC_USER_DESCRIPTION)) {
                    String description = DescriptorParser
                            .getCharacteristicUserDescription(descriptor);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, description);
                }
                if (descriptorUUID.equals(UUIDDatabase.UUID_SERVER_CHARACTERISTIC_CONFIGURATION)) {
                    String broadcastStatus = DescriptorParser.
                            getServerCharacteristicConfiguration(descriptor);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, broadcastStatus);
                }
                if (descriptorUUID.equals(UUIDDatabase.UUID_REPORT_REFERENCE)) {
                    ArrayList<String> reportReferencealues = DescriptorParser.getReportReference(descriptor);
                    String reportReference;
                    String reportReferenceType;
                    if (reportReferencealues.size() == 2) {
                        reportReference = reportReferencealues.get(0);
                        reportReferenceType = reportReferencealues.get(1);
                        mBundle.putString(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID, reportReference);
                        mBundle.putString(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_TYPE, reportReferenceType);
                        mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, reportReference + "\n" +
                                reportReferenceType);
                    }

                }
                if (descriptorUUID.equals(UUIDDatabase.UUID_CHARACTERISTIC_PRESENTATION_FORMAT)) {
                    String value = DescriptorParser.getCharacteristicPresentationFormat(descriptor);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE,
                            value);
                }
                intent.putExtras(mBundle);
                /**
                 * Sending the broad cast so that it can be received on
                 * registered receivers
                 */

                mContext.sendBroadcast(intent);
            } else {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_request_status) +
                        mContext.getResources().
                                getString(R.string.dl_status_failure) + status;
                Logger.datalog(dataLog);
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);


            String dataLog = "";
            if (status == BluetoothGatt.GATT_SUCCESS) {
                dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_status_success);
                Logger.datalog(dataLog);
            } else {
                dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status) +
                        mContext.getResources().
                                getString(R.string.dl_status_failure) + status;
                Intent intent = new Intent(ACTION_GATT_CHARACTERISTIC_ERROR);
                intent.putExtra(Constants.EXTRA_CHARACTERISTIC_ERROR_MESSAGE, "" + status);
                mContext.sendBroadcast(intent);
                Logger.datalog(dataLog);
            }

            Log.v("CYSMART", dataLog);
            boolean isExitBootloaderCmd = false;
            synchronized (mGattCallback){
                isExitBootloaderCmd = m_otaExitBootloaderCmdInProgress;
                if(m_otaExitBootloaderCmdInProgress)
                    m_otaExitBootloaderCmdInProgress = false;
            }

            if(isExitBootloaderCmd)
                onOtaExitBootloaderComplete(status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);

            // GATT Characteristic read
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String characteristicValue = " " + Utils.ByteArraytoHex(characteristic.getValue()) + " ";
                UUID charUuid = characteristic.getUuid();
                final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                Bundle mBundle = new Bundle();
                // Putting the byte value read for GATT Db
                mBundle.putByteArray(Constants.EXTRA_BYTE_VALUE,
                        characteristic.getValue());
                mBundle.putString(Constants.EXTRA_BYTE_UUID_VALUE,
                        characteristic.getUuid().toString());
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_response) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + characteristicValue + "]";
                Logger.datalog(dataLog);
                // Body sensor location read value
                if (charUuid.equals(UUIDDatabase.UUID_BODY_SENSOR_LOCATION)) {
                    mBundle.putString(Constants.EXTRA_BSL_VALUE,
                            HRMParser.getBodySensorLocation(characteristic));
                }
                // Manufacture name read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_MANUFATURE_NAME_STRING)) {
                    mBundle.putString(Constants.EXTRA_MNS_VALUE,
                            Utils.getManufacturerNameString(characteristic));
                }
                // Model number read value
                else if (charUuid.equals(UUIDDatabase.UUID_MODEL_NUMBER_STRING)) {
                    mBundle.putString(Constants.EXTRA_MONS_VALUE,
                            Utils.getModelNumberString(characteristic));
                }
                // Serial number read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_SERIAL_NUMBER_STRING)) {
                    mBundle.putString(Constants.EXTRA_SNS_VALUE,
                            Utils.getSerialNumberString(characteristic));
                }
                // Hardware revision read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_HARDWARE_REVISION_STRING)) {
                    mBundle.putString(Constants.EXTRA_HRS_VALUE,
                            Utils.getHardwareRevisionString(characteristic));
                }
                // Firmware revision read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_FIRWARE_REVISION_STRING)) {
                    mBundle.putString(Constants.EXTRA_FRS_VALUE,
                            Utils.getFirmwareRevisionString(characteristic));
                }
                // Software revision read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_SOFTWARE_REVISION_STRING)) {
                    mBundle.putString(Constants.EXTRA_SRS_VALUE,
                            Utils.getSoftwareRevisionString(characteristic));
                }
                // Battery level read value
                else if (charUuid.equals(UUIDDatabase.UUID_BATTERY_LEVEL)) {
                    mBundle.putString(Constants.EXTRA_BTL_VALUE,
                            Utils.getBatteryLevel(characteristic));
                }
                // PNP ID read value
                else if (charUuid.equals(UUIDDatabase.UUID_PNP_ID)) {
                    mBundle.putString(Constants.EXTRA_PNP_VALUE,
                            Utils.getPNPID(characteristic));
                }
                // System ID read value
                else if (charUuid.equals(UUIDDatabase.UUID_SYSTEM_ID)) {
                    mBundle.putString(Constants.EXTRA_SID_VALUE,
                            Utils.getSYSID(characteristic));
                }
                // Regulatory data read value
                else if (charUuid.equals(UUIDDatabase.UUID_IEEE)) {
                    mBundle.putString(Constants.EXTRA_RCDL_VALUE,
                            Utils.ByteArraytoHex(characteristic.getValue()));
                }
                // Health thermometer sensor location read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_HEALTH_THERMOMETER_SENSOR_LOCATION)) {
                    mBundle.putString(Constants.EXTRA_HSL_VALUE, HTMParser
                            .getHealthThermoSensorLocation(characteristic));
                }
                // CapSense proximity read value
                else if (charUuid.equals(UUIDDatabase.UUID_CAPSENSE_PROXIMITY) ||
                        charUuid.equals(UUIDDatabase.UUID_CAPSENSE_PROXIMITY_CUSTOM)) {
                    mBundle.putInt(Constants.EXTRA_CAPPROX_VALUE,
                            CapSenseParser.getCapSenseProximity(characteristic));
                }
                // CapSense slider read value
                else if (charUuid.equals(UUIDDatabase.UUID_CAPSENSE_SLIDER) ||
                        charUuid.equals(UUIDDatabase.UUID_CAPSENSE_SLIDER_CUSTOM)) {
                    mBundle.putInt(Constants.EXTRA_CAPSLIDER_VALUE,
                            CapSenseParser.getCapSenseSlider(characteristic));
                }
                // CapSense buttons read value
                else if (charUuid.equals(UUIDDatabase.UUID_CAPSENSE_BUTTONS) ||
                        charUuid.equals(UUIDDatabase.UUID_CAPSENSE_BUTTONS_CUSTOM)) {
                    mBundle.putIntegerArrayList(
                            Constants.EXTRA_CAPBUTTONS_VALUE,
                            CapSenseParser.getCapSenseButtons(characteristic));
                }
                // Alert level read value
                else if (charUuid.equals(UUIDDatabase.UUID_ALERT_LEVEL)) {
                    mBundle.putString(Constants.EXTRA_ALERT_VALUE,
                            Utils.getAlertLevel(characteristic));
                }
                // Transmission power level read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_TRANSMISSION_POWER_LEVEL)) {
                    mBundle.putInt(Constants.EXTRA_POWER_VALUE,
                            Utils.getTransmissionPower(characteristic));
                }
                // RGB Led read value
                else if (charUuid.equals(UUIDDatabase.UUID_RGB_LED) ||
                        charUuid.equals(UUIDDatabase.UUID_RGB_LED_CUSTOM)) {
                    mBundle.putString(Constants.EXTRA_RGB_VALUE,
                            RGBParser.getRGBValue(characteristic));
                }
                // Glucose read value
                else if (charUuid.equals(UUIDDatabase.UUID_GLUCOSE)) {
                    mBundle.putStringArrayList(Constants.EXTRA_GLUCOSE_VALUE,
                            GlucoseParser.getGlucoseHealth(characteristic));
                }
                // Running speed read value
                else if (charUuid.equals(UUIDDatabase.UUID_RSC_MEASURE)) {
                    mBundle.putStringArrayList(Constants.EXTRA_RSC_VALUE,
                            RSCParser.getRunningSpeednCadence(characteristic));
                }
                // Accelerometer X read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_ACCELEROMETER_READING_X)) {
                    mBundle.putInt(Constants.EXTRA_ACCX_VALUE, SensorHubParser
                            .getAcceleroMeterXYZReading(characteristic));
                }
                // Accelerometer Y read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_ACCELEROMETER_READING_Y)) {
                    mBundle.putInt(Constants.EXTRA_ACCY_VALUE, SensorHubParser
                            .getAcceleroMeterXYZReading(characteristic));
                }
                // Accelerometer Z read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_ACCELEROMETER_READING_Z)) {
                    mBundle.putInt(Constants.EXTRA_ACCZ_VALUE, SensorHubParser
                            .getAcceleroMeterXYZReading(characteristic));
                }
                // Temperature read value
                else if (charUuid.equals(UUIDDatabase.UUID_TEMPERATURE_READING)) {
                    mBundle.putFloat(Constants.EXTRA_STEMP_VALUE,
                            SensorHubParser
                                    .getThermometerReading(characteristic));
                }
                // Barometer read value
                else if (charUuid.equals(UUIDDatabase.UUID_BAROMETER_READING)) {
                    mBundle.putInt(Constants.EXTRA_SPRESSURE_VALUE,
                            SensorHubParser.getBarometerReading(characteristic));
                }
                // Accelerometer scan interval read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_ACCELEROMETER_SENSOR_SCAN_INTERVAL)) {
                    mBundle.putInt(
                            Constants.EXTRA_ACC_SENSOR_SCAN_VALUE,
                            SensorHubParser
                                    .getSensorScanIntervalReading(characteristic));
                }
                // Accelerometer analog sensor read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_ACCELEROMETER_ANALOG_SENSOR)) {
                    mBundle.putInt(Constants.EXTRA_ACC_SENSOR_TYPE_VALUE,
                            SensorHubParser
                                    .getSensorTypeReading(characteristic));
                }
                // Accelerometer data accumulation read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_ACCELEROMETER_DATA_ACCUMULATION)) {
                    mBundle.putInt(Constants.EXTRA_ACC_FILTER_VALUE,
                            SensorHubParser
                                    .getFilterConfiguration(characteristic));
                }
                // Temperature sensor scan read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_TEMPERATURE_SENSOR_SCAN_INTERVAL)) {
                    mBundle.putInt(
                            Constants.EXTRA_STEMP_SENSOR_SCAN_VALUE,
                            SensorHubParser
                                    .getSensorScanIntervalReading(characteristic));
                }
                // Temperature analog sensor read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_TEMPERATURE_ANALOG_SENSOR)) {
                    mBundle.putInt(Constants.EXTRA_STEMP_SENSOR_TYPE_VALUE,
                            SensorHubParser
                                    .getSensorTypeReading(characteristic));
                }
                // Barometer sensor scan interval read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_BAROMETER_SENSOR_SCAN_INTERVAL)) {
                    mBundle.putInt(
                            Constants.EXTRA_SPRESSURE_SENSOR_SCAN_VALUE,
                            SensorHubParser
                                    .getSensorScanIntervalReading(characteristic));
                }
                // Barometer digital sensor
                else if (charUuid
                        .equals(UUIDDatabase.UUID_BAROMETER_DIGITAL_SENSOR)) {
                    mBundle.putInt(Constants.EXTRA_SPRESSURE_SENSOR_TYPE_VALUE,
                            SensorHubParser
                                    .getSensorTypeReading(characteristic));
                }
                // Barometer threshold for indication read value
                else if (charUuid
                        .equals(UUIDDatabase.UUID_BAROMETER_THRESHOLD_FOR_INDICATION)) {
                    mBundle.putInt(Constants.EXTRA_SPRESSURE_THRESHOLD_VALUE,
                            SensorHubParser.getThresholdValue(characteristic));
                }
                // Iridium burst text message
                else if (charUuid.equals(UUIDDatabase.UUID_IRIDIUM_BURST_TEXT)) {
                    mBundle.putString(Constants.EXTRA_IRIDIUM_VALUE,
                            Utils.getIridiumBurstTextString(characteristic));
                }
                else if (charUuid.equals(UUIDDatabase.UUID_IRIDIUM_STATUS_IMEI_TEXT)) {
                    mBundle.putString(Constants.EXTRA_IRIDIUM_IMEI_VALUE,
                            Utils.getIridiumImeiString(characteristic));
                }
                else if (charUuid.equals(UUIDDatabase.UUID_IRIDIUM_STATUS_SIGNAL_LEVEL_TEXT)) {
                    mBundle.putString(Constants.EXTRA_IRIDIUM_RSSI_VALUE,
                            Utils.getIridiumSignalStatusString(characteristic));
                }
                else if (charUuid.equals(UUIDDatabase.UUID_IRIDIUM_STATUS_NEWORK_STATUS_TEXT)) {
                    mBundle.putString(Constants.EXTRA_IRIDIUM_NETWORK_VALUE,
                            Utils.getIridiumNetworkStatusString(characteristic));
                }
                else if (charUuid.equals(UUIDDatabase.UUID_IRIDIUM_MESSAGE_STATUS_TEXT)) {
                    mBundle.putString(Constants.EXTRA_IRIDIUM_MESSAGE_STATUS_VALUE,
                            Utils.getIridiumMessageStatusString(characteristic));
                }
                else if (charUuid.equals(UUIDDatabase.UUID_IRIDIUM_MESSAGE_TEXT1_TEXT)) {
                    mBundle.putString(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT1_VALUE,
                            Utils.getIridiumBurstTextString(characteristic));
                }
                else if (charUuid.equals(UUIDDatabase.UUID_IRIDIUM_MESSAGE_TEXT2_TEXT)) {
                    mBundle.putString(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT2_VALUE,
                            Utils.getIridiumBurstTextString(characteristic));
                }
                else if (charUuid.equals(UUIDDatabase.UUID_IRIDIUM_MESSAGE_TEXT3_TEXT)) {
                    mBundle.putString(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT3_VALUE,
                            Utils.getIridiumBurstTextString(characteristic));
                }
                else if (charUuid.equals(UUIDDatabase.UUID_IRIDIUM_MESSAGE_TEXT4_TEXT)) {
                    mBundle.putString(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT4_VALUE,
                            Utils.getIridiumBurstTextString(characteristic));
                }
                else if (charUuid.equals(UUIDDatabase.UUID_IRIDIUM_MESSAGE_TEXT5_TEXT)) {
                    mBundle.putString(Constants.EXTRA_IRIDIUM_MESSAGE_TEXT5_VALUE,
                            Utils.getIridiumBurstTextString(characteristic));
                }

                intent.putExtras(mBundle);

                /**
                 * Sending the broad cast so that it can be received on
                 * registered receivers
                 */

                mContext.sendBroadcast(intent);

            } else {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_request_status) +
                        mContext.getResources().
                                getString(R.string.dl_status_failure) + status;
                Logger.datalog(dataLog);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);

            String characteristicValue = Utils.ByteArraytoHex(characteristic.getValue());
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().
                            getString(R.string.dl_characteristic_notification_response) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.datalog(dataLog);
            Log.v("CYSMART", dataLog);
            broadcastNotifyUpdate(characteristic);
        }
    };
    private final IBinder mBinder = new LocalBinder();
    /**
     * Flag to check the mBound status
     */
    public boolean mBound;
    /**
     * BlueTooth manager for handling connections
     */
    private BluetoothManager mBluetoothManager;

    public static String getmBluetoothDeviceAddress() {
        return mBluetoothDeviceAddress;
    }

    public static String getmBluetoothDeviceName() {
        return mBluetoothDeviceName;
    }

    private static void broadcastConnectionUpdate(final String action) {
        Logger.i("action :" + action);
        final Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }

    private static void broadcastNotifyUpdate(final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
        Bundle mBundle = new Bundle();
        mBundle.putByteArray(Constants.EXTRA_BYTE_VALUE,
                characteristic.getValue());
        mBundle.putString(Constants.EXTRA_BYTE_UUID_VALUE, characteristic.getUuid().toString());
        // Heart rate Measurement notify value
        if (UUIDDatabase.UUID_HEART_RATE_MEASUREMENT.equals(characteristic
                .getUuid())) {
            String heart_rate = HRMParser.getHeartRate(characteristic);
            String energy_expended = HRMParser
                    .getEnergyExpended(characteristic);
            ArrayList<Integer> rrintervel = HRMParser
                    .getRRInterval(characteristic);
            mBundle.putString(Constants.EXTRA_HRM_VALUE, heart_rate);
            mBundle.putString(Constants.EXTRA_HRM_EEVALUE, energy_expended);
            mBundle.putIntegerArrayList(Constants.EXTRA_HRM_RRVALUE, rrintervel);
        }
        // Health thermometer notify value
        if (UUIDDatabase.UUID_HEALTH_THERMOMETER.equals(characteristic
                .getUuid())) {
            ArrayList<String> health_temp = HTMParser.getHealthThermo(characteristic);
            mBundle.putStringArrayList(Constants.EXTRA_HTM_VALUE, health_temp);
        }

        // CapSense Proximity notify value
        if (UUIDDatabase.UUID_CAPSENSE_PROXIMITY.equals(characteristic.getUuid()) ||
                UUIDDatabase.UUID_CAPSENSE_PROXIMITY_CUSTOM.equals(characteristic.getUuid())) {
            int capsense_proximity = CapSenseParser
                    .getCapSenseProximity(characteristic);
            mBundle.putInt(Constants.EXTRA_CAPPROX_VALUE, capsense_proximity);
        }
        // CapSense slider notify value
        if (UUIDDatabase.UUID_CAPSENSE_SLIDER.equals(characteristic.getUuid()) ||
                UUIDDatabase.UUID_CAPSENSE_SLIDER_CUSTOM.equals(characteristic.getUuid())) {
            int capsense_slider = CapSenseParser
                    .getCapSenseSlider(characteristic);
            mBundle.putInt(Constants.EXTRA_CAPSLIDER_VALUE, capsense_slider);

        }
        // CapSense buttons notify value
        if (UUIDDatabase.UUID_CAPSENSE_BUTTONS.equals(characteristic.getUuid()) ||
                UUIDDatabase.UUID_CAPSENSE_BUTTONS_CUSTOM.equals(characteristic.getUuid())) {
            ArrayList<Integer> capsense_buttons = CapSenseParser
                    .getCapSenseButtons(characteristic);
            mBundle.putIntegerArrayList(Constants.EXTRA_CAPBUTTONS_VALUE,
                    capsense_buttons);

        }
        // Glucose notify value
        if (UUIDDatabase.UUID_GLUCOSE.equals(characteristic.getUuid())) {
            ArrayList<String> glucose_values = GlucoseParser
                    .getGlucoseHealth(characteristic);
            mBundle.putStringArrayList(Constants.EXTRA_GLUCOSE_VALUE,
                    glucose_values);

        }
        // Blood pressure measurement notify value
        if (UUIDDatabase.UUID_BLOOD_PRESSURE_MEASUREMENT.equals(characteristic
                .getUuid())) {
            String blood_pressure_systolic = BloodPressureParser
                    .getSystolicBloodPressure(characteristic);
            String blood_pressure_diastolic = BloodPressureParser
                    .getDiaStolicBloodPressure(characteristic);
            String blood_pressure_systolic_unit = BloodPressureParser
                    .getSystolicBloodPressureUnit(characteristic, mContext);
            String blood_pressure_diastolic_unit = BloodPressureParser
                    .getDiaStolicBloodPressureUnit(characteristic, mContext);
            mBundle.putString(
                    Constants.EXTRA_PRESURE_SYSTOLIC_UNIT_VALUE,
                    blood_pressure_systolic_unit);
            mBundle.putString(
                    Constants.EXTRA_PRESURE_DIASTOLIC_UNIT_VALUE,
                    blood_pressure_diastolic_unit);
            mBundle.putString(
                    Constants.EXTRA_PRESURE_SYSTOLIC_VALUE,
                    blood_pressure_systolic);
            mBundle.putString(
                    Constants.EXTRA_PRESURE_DIASTOLIC_VALUE,
                    blood_pressure_diastolic);

        }
        // Running speed measurement notify value
        if (UUIDDatabase.UUID_RSC_MEASURE.equals(characteristic.getUuid())) {
            ArrayList<String> rsc_values = RSCParser
                    .getRunningSpeednCadence(characteristic);
            mBundle.putStringArrayList(Constants.EXTRA_RSC_VALUE, rsc_values);

        }
        // Cycling speed Measurement notify value
        if (UUIDDatabase.UUID_CSC_MEASURE.equals(characteristic.getUuid())) {
            ArrayList<String> csc_values = CSCParser
                    .getCyclingSpeednCadence(characteristic);
            mBundle.putStringArrayList(Constants.EXTRA_CSC_VALUE, csc_values);

        }
        // Accelerometer x notify value
        if (UUIDDatabase.UUID_ACCELEROMETER_READING_X.equals(characteristic
                .getUuid())) {
            mBundle.putInt(Constants.EXTRA_ACCX_VALUE,
                    SensorHubParser.getAcceleroMeterXYZReading(characteristic));

        }
        // Accelerometer Y notify value
        if (UUIDDatabase.UUID_ACCELEROMETER_READING_Y.equals(characteristic
                .getUuid())) {
            mBundle.putInt(Constants.EXTRA_ACCY_VALUE,
                    SensorHubParser.getAcceleroMeterXYZReading(characteristic));
        }
        // Accelerometer Z notify value
        if (UUIDDatabase.UUID_ACCELEROMETER_READING_Z.equals(characteristic
                .getUuid())) {
            mBundle.putInt(Constants.EXTRA_ACCZ_VALUE,
                    SensorHubParser.getAcceleroMeterXYZReading(characteristic));

        }
        // Temperature notify value
        if (UUIDDatabase.UUID_TEMPERATURE_READING.equals(characteristic
                .getUuid())) {
            mBundle.putFloat(Constants.EXTRA_STEMP_VALUE,
                    SensorHubParser.getThermometerReading(characteristic));

        }
        // Barometer notify value
        if (UUIDDatabase.UUID_BAROMETER_READING
                .equals(characteristic.getUuid())) {
            mBundle.putInt(Constants.EXTRA_SPRESSURE_VALUE,
                    SensorHubParser.getBarometerReading(characteristic));
        }
        // Battery level read value
        if (UUIDDatabase.UUID_BATTERY_LEVEL
                .equals(characteristic.getUuid())) {
            mBundle.putString(Constants.EXTRA_BTL_VALUE,
                    Utils.getBatteryLevel(characteristic));
        }
        //RDK characteristic
        if (UUIDDatabase.UUID_REP0RT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor
                    (UUIDDatabase.UUID_REPORT_REFERENCE);
            if (descriptor != null) {
                BluetoothLeService.readDescriptor(characteristic.getDescriptor(
                        UUIDDatabase.UUID_REPORT_REFERENCE));
                ArrayList<String> reportReferenceValues = DescriptorParser.getReportReference(characteristic.
                        getDescriptor(UUIDDatabase.UUID_REPORT_REFERENCE));
                if (reportReferenceValues.size() == 2) {
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID,
                            reportReferenceValues.get(0));
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_TYPE,
                            reportReferenceValues.get(1));
                }


            }

        }
        //case for OTA characteristic received
        if (UUIDDatabase.UUID_OTA_UPDATE_CHARACTERISTIC
                .equals(characteristic.getUuid())) {
            Intent intentOTA = new Intent(BluetoothLeService.ACTION_OTA_DATA_AVAILABLE);
            intentOTA.putExtras(mBundle);
            mContext.sendBroadcast(intentOTA);
        }
        intent.putExtras(mBundle);
        /**
         * Sending the broad cast so that it can be received on registered
         * receivers
         */

        mContext.sendBroadcast(intent);
    }

    private static void onOtaExitBootloaderComplete(int status) {
        Bundle bundle = new Bundle();
        bundle.putByteArray(Constants.EXTRA_BYTE_VALUE, new byte[]{(byte)status});
        Intent intentOTA = new Intent(BluetoothLeService.ACTION_OTA_DATA_AVAILABLE);
        intentOTA.putExtras(bundle);
        mContext.sendBroadcast(intentOTA);
    }

    /**
     * Connects to the GATT server hosted on the BlueTooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public static void connect(final String address, final String devicename, Context context) {
        mContext = context;
        if (mBluetoothAdapter == null || address == null) {
            return;
        }

        BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            return;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        //  refreshDeviceCache(mBluetoothGatt);
        mBluetoothDeviceAddress = address;
        mBluetoothDeviceName = devicename;
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                + "[" + devicename + "|" + address + "] " +
                mContext.getResources().getString(R.string.dl_connection_request);
        Logger.datalog(dataLog);
        mConnectionState = STATE_CONNECTING;
    }

    public static boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh");
            if (localMethod != null) {
                return (Boolean) localMethod.invoke(localBluetoothGatt);
            }
        } catch (Exception localException) {
            Logger.i("An exception occured while refreshing device");
        }
        return false;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public static void disconnect() {
        Logger.i("disconnect called");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Logger.i("disconnect returned");
            return;
        }
        //  Logger.datalog(mContext.getResources().getString(R.string.dl_device_connecting));
        mBluetoothGatt.disconnect();
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                mContext.getResources().getString(R.string.dl_disconnection_request);
        Logger.datalog(dataLog);
        mBluetoothGatt.close();
    }

    public static void discoverServices() {
        // Logger.datalog(mContext.getResources().getString(R.string.dl_service_discover_request));
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            mBluetoothGatt.discoverServices();
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                    + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                    mContext.getResources().getString(R.string.dl_service_discovery_request);
            Logger.datalog(dataLog);
        }

    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public static void readCharacteristic(
            BluetoothGattCharacteristic characteristic) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                "[" + serviceName + "|" + characteristicName + "] " +
                mContext.getResources().getString(R.string.dl_characteristic_read_request);
        Logger.datalog(dataLog);
    }

    /**
     * Request a read on a given {@code BluetoothGattDescriptor }.
     *
     * @param descriptor The descriptor to read from.
     */
    public static void readDescriptor(
            BluetoothGattDescriptor descriptor) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        //Logger.datalog(mContext.getResources().getString(R.string.dl_descriptor_read_request));
        mBluetoothGatt.readDescriptor(descriptor);

    }

    /**
     * Request a write with no response on a given
     * {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic
     * @param byteArray      to write
     */
    public static void writeCharacteristicNoresponse(
            BluetoothGattCharacteristic characteristic, byte[] byteArray) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);

        String characteristicValue = Utils.ByteArraytoHex(byteArray);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = byteArray;
            characteristic.setValue(valueByte);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.datalog(dataLog);

        }
    }

    public  static  void  writeOTABootLoaderCommand(
            BluetoothGattCharacteristic characteristic,
            byte[] value,
            boolean isExitBootloaderCmd)
    {
        synchronized (mGattCallback) {
            writeOTABootLoaderCommand(characteristic, value);
            if(isExitBootloaderCmd)
                m_otaExitBootloaderCmdInProgress = true;
        }
    }

    public static void writeOTABootLoaderCommand(
            BluetoothGattCharacteristic characteristic, byte[] value)
    {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);

        String characteristicValue = Utils.ByteArraytoHex(value);
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            return;
        }
        else
        {
            byte[] valueByte = value;
            characteristic.setValue(valueByte);
            int counter = 20;
            boolean status;
            do
            {
                status = mBluetoothGatt.writeCharacteristic(characteristic);
                if(!status)
                {
                    Log.v("CYSMART","writeCharacteristic() status: False");
                    try
                    {
                        Thread.sleep(100,0);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }

            }while (!status && (counter-- > 0));


            if(status)
            {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[ " + characteristicValue + " ]";
                Logger.datalog(dataLog);
                Log.v("CYSMART", dataLog);
            }
            else
            {
                Log.v("CYSMART", "writeOTABootLoaderCommand failed!");
            }
        }

    }

    private static String getHexValue(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (byte byteChar : array) {
            sb.append(String.format("%02x", byteChar));
        }
        return "" + sb;
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic
     * @param byteArray
     */

    public static void writeCharacteristicGattDb(
            BluetoothGattCharacteristic characteristic, byte[] byteArray) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);

        String characteristicValue = Utils.ByteArraytoHex(byteArray);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = byteArray;
            characteristic.setValue(valueByte);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.datalog(dataLog);
        }

    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic} for RGB.
     *
     * @param characteristic
     * @param red
     * @param green
     * @param blue
     * @param intensity
     */
    public static void writeCharacteristicRGB(
            BluetoothGattCharacteristic characteristic, int red, int green,
            int blue, int intensity) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = new byte[4];
            valueByte[0] = (byte) red;
            valueByte[1] = (byte) green;
            valueByte[2] = (byte) blue;
            valueByte[3] = (byte) intensity;
            characteristic.setValue(valueByte);
            String characteristicValue = Utils.ByteArraytoHex(valueByte);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.datalog(dataLog);

        }

    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public static void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);

        String descriptorUUID = GattAttributes.CLIENT_CHARACTERISTIC_CONFIG;
        String descriptorName = GattAttributes.lookup(descriptorUUID, descriptorUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        if (characteristic.getDescriptor(UUID
                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled == true) {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) + "]";
                Logger.datalog(dataLog);

            } else {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) + "]";
                Logger.datalog(dataLog);
            }
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled) {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_start_notification);
            Logger.datalog(dataLog);
        } else {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_stop_notification);
            Logger.datalog(dataLog);
        }

    }

    public static boolean stopCharacteristicNotify(BluetoothGattCharacteristic
                                                           bluetoothGattCharacteristic,
                                                   BluetoothGattDescriptor bluetoothGattDescriptor) {

        mBluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, false);
        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        return mBluetoothGatt.writeDescriptor(bluetoothGattDescriptor);

    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public static void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled,boolean waitNeeded) {

        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);

        String descriptorUUID = GattAttributes.CLIENT_CHARACTERISTIC_CONFIG;
        String descriptorName = GattAttributes.lookup(descriptorUUID, descriptorUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        if(waitNeeded){
            if (characteristic.getDescriptor(UUID
                    .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
                if (enabled == true) {
                    BluetoothGattDescriptor descriptor = characteristic
                            .getDescriptor(UUID
                                    .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                    descriptor
                            .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    int counter = 10;
                    boolean status;
                    do {
                        status = mBluetoothGatt.writeDescriptor(descriptor);
                        if (!status) {
                            try {
                                Thread.sleep(100, 0);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    } while (!status && (counter-- > 0));
                    if (status) {
                        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                                "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                                mContext.getResources().getString(R.string.dl_characteristic_write_request)
                                + mContext.getResources().getString(R.string.dl_commaseparator) +
                                "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) + "]";
                        Logger.datalog(dataLog);
                    }


                } else {
                    BluetoothGattDescriptor descriptor = characteristic
                            .getDescriptor(UUID
                                    .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                    descriptor
                            .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    int counter = 10;
                    boolean status = false;
                    do {
                        status = mBluetoothGatt.writeDescriptor(descriptor);
                        if (!status) {
                            try {
                                Thread.sleep(100, 0);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    } while (!status && (counter-- > 0));
                    if (status) {
                        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                                "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                                mContext.getResources().getString(R.string.dl_characteristic_write_request)
                                + mContext.getResources().getString(R.string.dl_commaseparator) +
                                "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) + "]";
                        Logger.datalog(dataLog);
                    }
                }
            }
            int counter = 10;
            boolean status;
            do {
                status = mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
                if (!status) {
                    try {
                        Thread.sleep(100, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } while (!status && (counter-- > 0));

        }


    }
    /**
     * Enables or disables indications on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable indications. False otherwise.
     */
    public static void setCharacteristicIndication(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookup(serviceUUID, serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookup(characteristicUUID, characteristicUUID);

        String descriptorUUID = GattAttributes.CLIENT_CHARACTERISTIC_CONFIG;
        String descriptorName = GattAttributes.lookup(descriptorUUID, descriptorUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }

        if (characteristic.getDescriptor(UUID
                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled == true) {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) + "]";
                Logger.datalog(dataLog);
            } else {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) + "]";
                Logger.datalog(dataLog);
            }

        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled) {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_start_indication);
            Logger.datalog(dataLog);
        } else {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_stop_indication);
            Logger.datalog(dataLog);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public static List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    public static int getConnectionState() {

        return mConnectionState;
    }

    public static boolean getBondedState() {
        Boolean bonded;
        BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(mBluetoothDeviceAddress);
        bonded = device.getBondState() == BluetoothDevice.BOND_BONDED;
        return bonded;
    }

    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBound = false;
        close();
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local BlueTooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        return mBluetoothAdapter != null;

    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @Override
    public void onCreate() {
        // Initializing the service
        if (!initialize()) {
            Logger.d("Service not initialized");
        }
    }

    /**
     * Local binder class
     */
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
}