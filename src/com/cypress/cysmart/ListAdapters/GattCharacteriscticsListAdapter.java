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

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.R;

import java.util.List;

/**
 * Adapter class for GattCharacteristics ListView
 */
public class GattCharacteriscticsListAdapter extends BaseAdapter {
    /**
     * BluetoothGattCharacteristic list
     */
    private List<BluetoothGattCharacteristic> mGattCharacteristics;
    private Context mContext;

    public GattCharacteriscticsListAdapter(Context mContext,
                                           List<BluetoothGattCharacteristic> list) {
        this.mContext = mContext;
        this.mGattCharacteristics = list;
    }

    @Override
    public int getCount() {
        return mGattCharacteristics.size();
    }

    @Override
    public Object getItem(int i) {
        return mGattCharacteristics.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            LayoutInflater mInflator = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = mInflator.inflate(R.layout.gattdb_characteristics_list_item,
                    viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.serviceName = (TextView) view
                    .findViewById(R.id.txtservicename);
            viewHolder.propertyName = (TextView) view
                    .findViewById(R.id.txtstatus);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.serviceName.setSelected(true);
        BluetoothGattCharacteristic item = mGattCharacteristics.get(i);

        String name = GattAttributes.lookup(item.getUuid().toString(), item
                .getUuid().toString());


        viewHolder.serviceName.setText(name);
        String proprties;
        String read = null, write = null, notify = null;

        /**
         * Checking the various GattCharacteristics and listing in the ListView
         */
        if (getGattCharacteristicsPropertices(item.getProperties(),
                BluetoothGattCharacteristic.PROPERTY_READ)) {
            read = mContext.getString(R.string.gatt_services_read);
        }
        if (getGattCharacteristicsPropertices(item.getProperties(),
                BluetoothGattCharacteristic.PROPERTY_WRITE)
                | getGattCharacteristicsPropertices(item.getProperties(),
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
            write = mContext.getString(R.string.gatt_services_write);
        }
        if (getGattCharacteristicsPropertices(item.getProperties(),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
            notify = mContext.getString(R.string.gatt_services_notify);
        }
        if (getGattCharacteristicsPropertices(item.getProperties(),
                BluetoothGattCharacteristic.PROPERTY_INDICATE)) {
            notify = mContext.getString(R.string.gatt_services_indicate);
        }
        // Handling multiple properties listing in the ListView
        if (read != null) {
            proprties = read;
            if (write != null) {
                proprties = proprties + " & " + write;
            }
            if (notify != null) {
                proprties = proprties + " & " + notify;
            }
        } else {
            if (write != null) {
                proprties = write;

                if (notify != null) {
                    proprties = proprties + " & " + notify;
                }
            } else {
                proprties = notify;
            }
        }
        viewHolder.propertyName.setText(proprties);
        return view;
    }

    /**
     * Holder class for the ListView variable
     */
    class ViewHolder {
        TextView serviceName, propertyName;

    }

    // Return the properties of mGattCharacteristics
    boolean getGattCharacteristicsPropertices(int characteristics,
                                              int characteristicsSearch) {

        if ((characteristics & characteristicsSearch) == characteristicsSearch) {
            return true;
        }
        return false;

    }
}
