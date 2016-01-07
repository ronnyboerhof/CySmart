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

import android.app.Application;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Application model data class
 */
public class CySmartApplication extends Application {

    private ArrayList<HashMap<String, BluetoothGattService>> mGattServiceData = new ArrayList<HashMap<String, BluetoothGattService>>();
    private ArrayList<HashMap<String, BluetoothGattService>> mGattServiceMasterData = new ArrayList<HashMap<String, BluetoothGattService>>();

    private List<BluetoothGattCharacteristic> mGattCharacteristics;
    private BluetoothGattCharacteristic mBluetoothgattcharacteristic;
    private BluetoothGattDescriptor mBluetoothGattDescriptor;



    /**
     * getter method for Blue tooth GATT characteristic
     *
     * @return {@link BluetoothGattCharacteristic}
     */
    public BluetoothGattCharacteristic getBluetoothgattcharacteristic() {
        return mBluetoothgattcharacteristic;
    }

    /**
     * setter method for Blue tooth GATT characteristics
     *
     * @param bluetoothgattcharacteristic
     */
    public void setBluetoothgattcharacteristic(
            BluetoothGattCharacteristic bluetoothgattcharacteristic) {
        this.mBluetoothgattcharacteristic = bluetoothgattcharacteristic;
    }

    /**
     * getter method for Blue tooth GATT characteristic
     *
     * @return {@link BluetoothGattCharacteristic}
     */
    public BluetoothGattDescriptor getBluetoothgattDescriptor() {
        return mBluetoothGattDescriptor;
    }

    /**
     * setter method for Blue tooth GATT Descriptor
     *
     * @param bluetoothGattDescriptor
     */
    public void setBluetoothgattdescriptor(
            BluetoothGattDescriptor bluetoothGattDescriptor) {
        this.mBluetoothGattDescriptor = bluetoothGattDescriptor;
    }

    /**
     * getter method for blue tooth GATT Characteristic list
     *
     * @return {@link List<BluetoothGattCharacteristic>}
     */
    public List<BluetoothGattCharacteristic> getGattCharacteristics() {
        return mGattCharacteristics;
    }

    /**
     * setter method for blue tooth GATT Characteristic list
     *
     * @param gattCharacteristics
     */
    public void setGattCharacteristics(
            List<BluetoothGattCharacteristic> gattCharacteristics) {
        this.mGattCharacteristics = gattCharacteristics;
    }

    public ArrayList<HashMap<String, BluetoothGattService>> getGattServiceData() {
        return mGattServiceData;
    }

    public void setGattServiceData(
            ArrayList<HashMap<String, BluetoothGattService>> gattServiceData) {
        this.mGattServiceData = gattServiceData;
    }

    public ArrayList<HashMap<String, BluetoothGattService>> getGattServiceMasterData() {
        return mGattServiceMasterData;
    }

    public void setGattServiceMasterData(
            ArrayList<HashMap<String, BluetoothGattService>> gattServiceMasterData) {
        this.mGattServiceMasterData = gattServiceMasterData;

    }
}
