package com.cypress.cysmart.OTAFirmwareUpdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.GattAttributes;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.TextProgressBar;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.DataModelClasses.OTAFlashRowModel;
import com.cypress.cysmart.HomePageActivity;
import com.cypress.cysmart.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * OTA update fragment
 */
public class OTAFirmwareUpgradeFragment extends Fragment implements View.OnClickListener, FileReadStatusUpdater {
    //Option Mapping
    public static final int mApplicationUpgrade = 101;
    public static final int mApplicationAndStackCombined = 201;
    public static final int mApplicationAndStackSeparate = 301;
    public static boolean mFileupgradeStarted = false;
    // GATT service and characteristics
    private static BluetoothGattService mservice;
    private static BluetoothGattCharacteristic mOTACharacteristic;
    //Custom OTA Write class
    private static OTAFirmwareWrite otaFirmwareWrite;
    //header data variables
    private static String mSiliconID;
    private static String mSiliconRev;
    private static String mCheckSumType;
    //Notification Manager
    android.app.NotificationManager mNotificationMAnager;
    int mNotificationId = 1;
    NotificationCompat.Builder mBuilder;
    //UI Elements
    private TextView mProgressText;
    private Button mStopUpgradeButton;
    private TextProgressBar mProgressTop;
    private TextProgressBar mProgressBottom;
    private TextView mFileNameTop;
    private TextView mFileNameBottom;
    private Button appDownload;
    private Button appStackCombDownload;
    private Button appStackSepDownload;
    private RelativeLayout progBarLayoutTop;
    private RelativeLayout progBarLayoutBottom;
    private View mView;
    private ProgressDialog mProgressDialog;
    //File read variables
    private int mTotalLines = 0;
    //CYCAD file data list
    private ArrayList<OTAFlashRowModel> mFlashRowList;
    //   flags
    private boolean HANDLER_FLAG = true;
    //Current Upgrade file path
    private String mCurrentFilePath;
    private int mProgressBarPosition = 0;
    private BroadcastReceiver mGattOTAStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            /**
             * Shared preference to hold the state of the boot loader
             */

            synchronized (this)
            {
                final String sharedPrefStatus = Utils.getStringSharedPreference(getActivity(),
                        Constants.PREF_BOOTLOADER_STATE);
                final String action = intent.getAction();
                Bundle extras = intent.getExtras();
                if (BootLoaderUtils.ACTION_OTA_STATUS.equals(action))
                {

                    if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.ENTER_BOOTLOADER))
                    {
                        String siliconIDReceived, siliconRevReceived;
                        if (extras.containsKey(Constants.EXTRA_SILICON_ID)
                                && extras.containsKey(Constants.EXTRA_SILICON_REV))
                        {
                            siliconIDReceived = extras.getString(Constants.EXTRA_SILICON_ID);
                            siliconRevReceived = extras.getString(Constants.EXTRA_SILICON_REV);
                            if (siliconIDReceived.equalsIgnoreCase(mSiliconID) &&
                                    siliconRevReceived.equalsIgnoreCase(mSiliconRev))
                            {
                                /**
                                 * SiliconID and SiliconRev Verified
                                 * Sending Next coommand
                                 */

                                //Getting the arrayID
                                OTAFlashRowModel modelData = mFlashRowList.get(0);
                                byte[] data = new byte[1];
                                data[0] = (byte) modelData.mArrayId;
                                int dataLength = 1;
                                /**
                                 * Writing the next command
                                 * Changing the shared preference value
                                 */
                                otaFirmwareWrite.OTAGetFlashSizeCmd(data, mCheckSumType, dataLength);
                                Utils.setStringSharedPreference(getActivity(),
                                        Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands.GET_FLASH_SIZE);
                                mProgressText.setText(getActivity().getResources().getText(R.string.ota_get_flash_size));
                            }
                        }

                    } else if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.GET_FLASH_SIZE))
                    {
                        /**
                         * verifying the rows to be programmed within the bootloadable area of flash
                         * not done for time being
                         */
                        int PROGRAM_ROW_NO = Utils.getIntSharedPreference(getActivity(),
                                Constants.PREF_PROGRAM_ROW_NO);
                        writeProgrammableData(PROGRAM_ROW_NO);
                    } else if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.SEND_DATA))
                    {
                        /**
                         * verifying the status and sending the next command
                         * Changing the shared preference value
                         */
                        if (extras.containsKey(Constants.EXTRA_SEND_DATA_ROW_STATUS))
                        {
                            String statusReceived = extras.getString(Constants.EXTRA_SEND_DATA_ROW_STATUS);
                            if (statusReceived.equalsIgnoreCase("00"))
                            {
                                //Succes status received.Send programmable data
                                int PROGRAM_ROW_NO = Utils.getIntSharedPreference(getActivity(),
                                        Constants.PREF_PROGRAM_ROW_NO);
                                writeProgrammableData(PROGRAM_ROW_NO);
                            }
                        }


                    } else if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.PROGRAM_ROW))
                    {
                        String statusReceived;
                        if (extras.containsKey(Constants.EXTRA_PROGRAM_ROW_STATUS))
                        {
                            statusReceived = extras.getString(Constants.EXTRA_PROGRAM_ROW_STATUS);
                            if (statusReceived.equalsIgnoreCase("00"))
                            {
                                /**
                                 * Program Row Status Verified
                                 * Sending Next coommand
                                 */
                                //Getting the arrayI
                                int PROGRAM_ROW = Utils.getIntSharedPreference(getActivity(),
                                        Constants.PREF_PROGRAM_ROW_NO);
                                OTAFlashRowModel modelData = mFlashRowList.get(PROGRAM_ROW);
                                long rowMSB = Long.parseLong(modelData.mRowNo.substring(0, 2), 16);
                                long rowLSB = Long.parseLong(modelData.mRowNo.substring(2, 4), 16);
                                /**
                                 * Writing the next command
                                 * Changing the shared preference value
                                 */
                                otaFirmwareWrite.OTAVerifyRowCmd(rowMSB, rowLSB, modelData, mCheckSumType);
                                Utils.setStringSharedPreference(getActivity(),
                                        Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands.VERIFY_ROW);
                                mProgressText.setText(getActivity().getResources().getText(R.string.ota_verify_row));
                            }
                        }


                    } else if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.VERIFY_ROW))
                    {
                        String statusReceived, checksumReceived;
                        if (extras.containsKey(Constants.EXTRA_VERIFY_ROW_STATUS)
                                && extras.containsKey(Constants.EXTRA_VERIFY_ROW_CHECKSUM))
                        {
                            statusReceived = extras.getString(Constants.EXTRA_VERIFY_ROW_STATUS);
                            checksumReceived = extras.getString(Constants.EXTRA_VERIFY_ROW_CHECKSUM);
                            if (statusReceived.equalsIgnoreCase("00"))
                            {
                                /**
                                 * Program Row Status Verified
                                 * Sending Next coommand
                                 */
                                int PROGRAM_ROW_NO = Utils.getIntSharedPreference(getActivity(),
                                        Constants.PREF_PROGRAM_ROW_NO);
                                //Getting the arrayID
                                OTAFlashRowModel modelData = mFlashRowList.get(PROGRAM_ROW_NO);
                                long rowMSB = Long.parseLong(modelData.mRowNo.substring(0, 2), 16);
                                long rowLSB = Long.parseLong(modelData.mRowNo.substring(2, 4), 16);

                                byte[] checkSumVerify = new byte[6];
                                checkSumVerify[0] = (byte) modelData.mRowCheckSum;
                                checkSumVerify[1] = (byte) modelData.mArrayId;
                                checkSumVerify[2] = (byte) rowMSB;
                                checkSumVerify[3] = (byte) rowLSB;
                                checkSumVerify[4] = (byte) (modelData.mDataLength);
                                checkSumVerify[5] = (byte) ((modelData.mDataLength) >> 8);
                                String fileCheckSumCalculated = Integer.toHexString(BootLoaderUtils.calculateCheckSumVerifyRow(6, checkSumVerify));
                                int fileCheckSumCalculatedLength = fileCheckSumCalculated.length();
                                String fileCheckSumByte = fileCheckSumCalculated.substring((fileCheckSumCalculatedLength - 2),
                                        fileCheckSumCalculatedLength);
                                if (fileCheckSumByte.equalsIgnoreCase(checksumReceived))
                                {
                                    PROGRAM_ROW_NO = PROGRAM_ROW_NO + 1;

                                    //Shows ProgressBar status
                                    showProgress(mProgressBarPosition, PROGRAM_ROW_NO, mFlashRowList.size());
                                    if (PROGRAM_ROW_NO < mFlashRowList.size())
                                    {
                                        Utils.setIntSharedPreference(getActivity(),
                                                Constants.PREF_PROGRAM_ROW_NO,
                                                PROGRAM_ROW_NO);
                                        Utils.setIntSharedPreference(getActivity(),
                                                Constants.PREF_PROGRAM_ROW_START_POS,
                                                0);
                                        writeProgrammableData(PROGRAM_ROW_NO);
                                    }
                                    if (PROGRAM_ROW_NO == mFlashRowList.size())
                                    {
                                        Utils.setIntSharedPreference(getActivity(),
                                                Constants.PREF_PROGRAM_ROW_NO,
                                                0);
                                        Utils.setIntSharedPreference(getActivity(),
                                                Constants.PREF_PROGRAM_ROW_START_POS,
                                                0);
                                        /**
                                         * Writing the next command
                                         * Changing the shared preference value
                                         */
                                        Utils.setStringSharedPreference(getActivity(),
                                                Constants.PREF_BOOTLOADER_STATE, "" +
                                                        BootLoaderCommands.VERIFY_CHECK_SUM);
                                        otaFirmwareWrite.OTAVerifyCheckSumCmd(mCheckSumType);
                                        mProgressText.setText(getActivity().getResources().
                                                getText(R.string.ota_verify_checksum));
                                    }
                                }
                            }
                        }

                    } else if (sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.VERIFY_CHECK_SUM))
                    {
                        String statusReceived;
                        if (extras.containsKey(Constants.EXTRA_VERIFY_CHECKSUM_STATUS))
                        {
                            statusReceived = extras.getString(Constants.EXTRA_VERIFY_CHECKSUM_STATUS);
                            if (statusReceived.equalsIgnoreCase("01"))
                            {
                                /**
                                 * Verify Status Verified
                                 * Sending Next coommand
                                 */
                                //Getting the arrayID
                                otaFirmwareWrite.OTAExitBootloaderCmd(mCheckSumType);
                                Utils.setStringSharedPreference(getActivity(),
                                        Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands.EXIT_BOOTLOADER);
                                mProgressText.setText(getActivity().getResources().getText(R.string.ota_end_bootloader));

                            }
                        }

                    }
                    else if(sharedPrefStatus.equalsIgnoreCase("" + BootLoaderCommands.EXIT_BOOTLOADER)){
                        final BluetoothDevice device = BluetoothLeService.mBluetoothAdapter
                                .getRemoteDevice(BluetoothLeService.getmBluetoothDeviceAddress());
                        OTAFirmwareUpgradeFragment.mFileupgradeStarted = false;
                        unpairDevice(device);
                        saveDeviceAddress();
                        mProgressText.setText(getActivity().getResources().getText(R.string.ota_end_success));
                        if (secondFileUpdatedNeeded())
                        {
                            mBuilder.setContentText(getActivity().getResources().getText(R.string.ota_notification_stack_file))
                                    .setProgress(0, 0, false);
                            mNotificationMAnager.notify(mNotificationId, mBuilder.build());
                        } else
                        {
                            mBuilder.setContentText(getActivity().getResources().getText(R.string.ota_end_success))
                                    .setProgress(0, 0, false);
                            mNotificationMAnager.notify(mNotificationId, mBuilder.build());
                        }

                        OTAFirmwareUpgradeFragment.mFileupgradeStarted = false;
                        BluetoothLeService.disconnect();
                        unpairDevice(device);
                        Toast.makeText(getActivity(),
                                getResources().getString(R.string.alert_message_bluetooth_disconnect),
                                Toast.LENGTH_SHORT).show();
                        Intent finishIntent = getActivity().getIntent();
                        getActivity().finish();
                        getActivity().overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                        startActivity(finishIntent);
                        getActivity().overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                    }
                    if (extras.containsKey(Constants.EXTRA_ERROR_OTA))
                    {
                        String errorMessage = extras.getString(Constants.EXTRA_ERROR_OTA);
                        showErrorDialogMessage(errorMessage);
                    }
                }
                if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
                {
                    final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.ERROR);

                    if (state == BluetoothDevice.BOND_BONDING)
                    {
                        // Bonding...
                        Logger.i("Bonding is in process....");
                        Utils.bondingProgressDialog(getActivity(), mProgressDialog, true);
                    } else if (state == BluetoothDevice.BOND_BONDED)
                    {
                        String dataLog = getResources().getString(R.string.dl_commaseparator)
                                + "[" + BluetoothLeService.getmBluetoothDeviceName() + "|"
                                + BluetoothLeService.getmBluetoothDeviceAddress() + "]" +
                                getResources().getString(R.string.dl_commaseparator) +
                                getResources().getString(R.string.dl_connection_paired);
                        Logger.datalog(dataLog);
                        Utils.bondingProgressDialog(getActivity(), mProgressDialog, false);
                    } else if (state == BluetoothDevice.BOND_NONE)
                    {
                        String dataLog = getResources().getString(R.string.dl_commaseparator)
                                + "[" + BluetoothLeService.getmBluetoothDeviceName() + "|"
                                + BluetoothLeService.getmBluetoothDeviceAddress() + "]" +
                                getResources().getString(R.string.dl_commaseparator) +
                                getResources().getString(R.string.dl_connection_unpaired);
                        Logger.datalog(dataLog);
                    }
                }
            }
        }
    };

    private void showErrorDialogMessage(String errorMessage) {
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(
                getActivity().getResources().getString(
                        R.string.alert_message_ota_error) + errorMessage)
                .setTitle(getActivity().getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setPositiveButton(
                        getActivity().getResources().getString(
                                R.string.alert_message_exit_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (mOTACharacteristic != null) {
                                    stopBroadcastDataNotify(mOTACharacteristic);
                                    clearDataNPreferences();
                                    cancelPendingNotification(mNotificationMAnager,
                                            mNotificationId);
                                    final BluetoothDevice device = BluetoothLeService.mBluetoothAdapter
                                            .getRemoteDevice(BluetoothLeService.getmBluetoothDeviceAddress());
                                    OTAFirmwareUpgradeFragment.mFileupgradeStarted = false;
                                    BluetoothLeService.disconnect();
                                    unpairDevice(device);
                                    Toast.makeText(getActivity(),
                                            getResources().getString(R.string.alert_message_bluetooth_disconnect),
                                            Toast.LENGTH_SHORT).show();
                                    Intent intent = getActivity().getIntent();
                                    getActivity().finish();
                                    getActivity().overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                                    startActivity(intent);
                                    getActivity().overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                                }
                            }
                        });
        alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        if (!getActivity().isDestroyed())
            alert.show();
    }

    private void writeProgrammableData(int rowPosition) {
        int startPosition = Utils.getIntSharedPreference(getActivity(),
                Constants.PREF_PROGRAM_ROW_START_POS);
        Logger.e("Row: " + rowPosition + "Start Pos: " + startPosition);
        OTAFlashRowModel modelData = mFlashRowList.get(rowPosition);
        int verifyDataLength = modelData.mDataLength - startPosition;
        if (checkProgramRowCommandToSend(verifyDataLength)) {
            long rowMSB = Long.parseLong(modelData.mRowNo.substring(0, 2), 16);
            long rowLSB = Long.parseLong(modelData.mRowNo.substring(2, 4), 16);
            int dataLength = modelData.mDataLength - startPosition;
            byte[] dataToSend = new byte[dataLength];
            for (int pos = 0; pos < dataLength; pos++) {
                if (startPosition < modelData.mData.length) {
                    byte data = modelData.mData[startPosition];
                    dataToSend[pos] = data;
                    startPosition++;
                } else {
                    break;
                }
            }
            otaFirmwareWrite.OTAProgramRowCmd(rowMSB, rowLSB, modelData.mArrayId,
                    dataToSend, mCheckSumType);
            Utils.setStringSharedPreference(getActivity(),
                    Constants.PREF_BOOTLOADER_STATE, "" +
                            BootLoaderCommands.PROGRAM_ROW);
            Utils.setIntSharedPreference(getActivity(),
                    Constants.PREF_PROGRAM_ROW_START_POS, 0);
            mProgressText.setText(getActivity().getResources().
                    getText(R.string.ota_program_row));
        } else {
            int dataLength = BootLoaderCommands.MAX_DATA_SIZE;
            byte[] dataToSend = new byte[dataLength];
            for (int pos = 0; pos < dataLength; pos++) {
                if (startPosition < modelData.mData.length) {
                    byte data = modelData.mData[startPosition];
                    dataToSend[pos] = data;
                    startPosition++;
                } else {
                    break;
                }
            }
            otaFirmwareWrite.OTAProgramRowSendDataCmd(
                    dataToSend, mCheckSumType);
            Utils.setStringSharedPreference(getActivity(),
                    Constants.PREF_BOOTLOADER_STATE, "" +
                            BootLoaderCommands.SEND_DATA);
            Utils.setIntSharedPreference(getActivity(),
                    Constants.PREF_PROGRAM_ROW_START_POS, startPosition);
            mProgressText.setText(getActivity().getResources().
                    getText(R.string.ota_program_row));
        }
    }

    private boolean checkProgramRowCommandToSend(int totalSize) {
        if (totalSize <= BootLoaderCommands.MAX_DATA_SIZE) {
            return true;
        } else {
            return false;
        }
    }

    //Constructor
    public OTAFirmwareUpgradeFragment create(BluetoothGattService bluetoothGattService) {
        mservice = bluetoothGattService;
        return new OTAFirmwareUpgradeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.ota_upgrade_type_selection, container,
                false);
        initializeGUIElements();
        initializeNotification();
        /**
         * Second file Upgradation
         *
         */
        if (secondFileUpdatedNeeded()) {
            secondFileUpgradation();
        }
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mGattOTAStatusReceiver,
                Utils.makeGattUpdateIntentFilter());
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.ota_title));
        initializeBondingIFnotBonded();
    }

    @Override
    public void onDestroy() {
        HANDLER_FLAG = false;
        getActivity().unregisterReceiver(mGattOTAStatusReceiver);
        if (mOTACharacteristic != null) {
            stopBroadcastDataNotify(mOTACharacteristic);
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ota_app_download:
                Intent ApplicationUpgrade = new Intent(getActivity(), OTAFilesListingActivity.class);
                ApplicationUpgrade.putExtra(Constants.REQ_FILE_COUNT, mApplicationUpgrade);
                startActivityForResult(ApplicationUpgrade, mApplicationUpgrade);
                break;
            case R.id.ota_app_stack_comb:
                Intent ApplicationAndStackCombined = new Intent(getActivity(), OTAFilesListingActivity.class);
                ApplicationAndStackCombined.putExtra(Constants.REQ_FILE_COUNT, mApplicationAndStackCombined);
                startActivityForResult(ApplicationAndStackCombined, mApplicationAndStackCombined);
                break;
            case R.id.ota_app_stack_seperate:
                Intent ApplicationAndStackSeparate = new Intent(getActivity(), OTAFilesListingActivity.class);
                ApplicationAndStackSeparate.putExtra(Constants.REQ_FILE_COUNT, mApplicationAndStackSeparate);
                startActivityForResult(ApplicationAndStackSeparate, mApplicationAndStackSeparate);
                break;
            case R.id.stop_upgrade_button:
                showOTAStopAlert();
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        getActivity();
        if (resultCode == Activity.RESULT_OK) {
            ArrayList<String> selectedFiles = data.
                    getStringArrayListExtra(Constants.ARRAYLIST_SELECTED_FILE_NAMES);
            ArrayList<String> selectedFilesPaths = data.
                    getStringArrayListExtra(Constants.ARRAYLIST_SELECTED_FILE_PATHS);
            if (requestCode == mApplicationUpgrade) {
                //Application upgrade option file selected
                String fileOneNAme = selectedFiles.get(0);
                mFileNameTop.setText(fileOneNAme.replace(".cyacd", ""));
                mCurrentFilePath = selectedFilesPaths.get(0);
                getGattData();
                updateGUI(mApplicationUpgrade);
            } else if (requestCode == mApplicationAndStackCombined) {
                //Application and stack upgrade combined option file selected
                String fileOneNAme = selectedFiles.get(0);
                mFileNameTop.setText(fileOneNAme.replace(".cyacd", ""));
                mCurrentFilePath = selectedFilesPaths.get(0);
                getGattData();
                updateGUI(mApplicationAndStackCombined);
            } else if (requestCode == mApplicationAndStackSeparate) {
                //Application and stack upgrade separate option file selected
                if (selectedFiles.size() == 2) {
                    String fileOneNAme = selectedFiles.get(0);
                    mFileNameTop.setText(fileOneNAme.replace(".cyacd", ""));
                    String fileTwoNAme = selectedFiles.get(1);
                    mFileNameBottom.setText(fileTwoNAme.replace(".cyacd", ""));
                    mCurrentFilePath = selectedFilesPaths.get(0);
                    getGattData();
                    Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_ONE_NAME,
                            fileOneNAme);
                    Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_TWO_NAME,
                            fileTwoNAme);
                    Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_TWO_PATH,
                            selectedFilesPaths.get(1));
                    Logger.e("PREF_OTA_FILE_TWO_PATH-->" + selectedFilesPaths.get(1));
                }
                updateGUI(mApplicationAndStackSeparate);
            }
        } else getActivity();
        if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(getActivity(), getResources().
                    getString(R.string.toast_selection_cancelled), Toast.LENGTH_SHORT).show();

        }
    }

    private void updateGUI(int updateOtion) {
        switch (updateOtion) {
            case mApplicationUpgrade:
                /**
                 * Disabling the GUI Option select buttons.
                 * Set the selected position as Application Upgrade
                 */
                appDownload.setSelected(true);
                appDownload.setPressed(true);
                appDownload.setEnabled(false);
                appStackCombDownload.setEnabled(false);
                appStackSepDownload.setEnabled(false);
                mProgressText.setVisibility(View.VISIBLE);
                mProgressText.setEnabled(false);
                mStopUpgradeButton.setVisibility(View.VISIBLE);
                progBarLayoutTop.setVisibility(View.VISIBLE);
                progBarLayoutTop.setEnabled(false);
                progBarLayoutBottom.setEnabled(false);
                mProgressText.setText(getActivity().getResources().getText(R.string.ota_file_read));
                mProgressBarPosition = 1;
                prepareFileWriting();
                break;
            case mApplicationAndStackCombined:
                /**
                 * Disabling the GUI Option select buttons.
                 * Set the selected position as Application&Stack Upgrade(combined file)
                 */
                appStackCombDownload.setSelected(true);
                appStackCombDownload.setPressed(true);
                appDownload.setEnabled(false);
                appStackCombDownload.setEnabled(false);
                appStackSepDownload.setEnabled(false);
                mProgressText.setVisibility(View.VISIBLE);
                mStopUpgradeButton.setVisibility(View.VISIBLE);
                progBarLayoutTop.setVisibility(View.VISIBLE);
                progBarLayoutBottom.setVisibility(View.INVISIBLE);
                mProgressText.setText(getActivity().getResources().getText(R.string.ota_file_read));
                mProgressBarPosition = 1;
                prepareFileWriting();
                break;
            case mApplicationAndStackSeparate:
                /**
                 * Disabling the GUI Option select buttons.
                 * Set the selected position as Application&Stack Upgrade(separate file)
                 */
                appStackSepDownload.setSelected(true);
                appStackSepDownload.setPressed(true);
                appDownload.setEnabled(false);
                appStackCombDownload.setEnabled(false);
                appStackSepDownload.setEnabled(false);
                mProgressText.setVisibility(View.VISIBLE);
                mStopUpgradeButton.setVisibility(View.VISIBLE);
                progBarLayoutTop.setVisibility(View.VISIBLE);
                progBarLayoutBottom.setVisibility(View.VISIBLE);
                mProgressText.setText(getActivity().getResources().getText(R.string.ota_file_read));
                mProgressBarPosition = 1;
                prepareFileWriting();
                break;
        }
    }

    private void prepareFileWriting() {
        /**
         * Always start the programming from the first line
         */
        Utils.setIntSharedPreference(getActivity(), Constants.PREF_PROGRAM_ROW_NO, 0);
        Utils.setIntSharedPreference(getActivity(), Constants.PREF_PROGRAM_ROW_START_POS, 0);
        /**
         * Custom file write class initialisation
         */
        if (mOTACharacteristic != null) {
            otaFirmwareWrite = new OTAFirmwareWrite(mOTACharacteristic);
        }

        /**
         * Custom file read class initialisation
         */
        final CustomFileReader customFileReader;
        customFileReader = new CustomFileReader
                (mCurrentFilePath);
        customFileReader.setFileReadStatusUpdater(this);

        /**
         * CYCAD Header information
         */
        String[] headerData = customFileReader.analyseFileHeader();
        mSiliconID = headerData[0];
        mSiliconRev = headerData[1];
        mCheckSumType = headerData[2];
        /**
         * Start the OTA Write start after 1 seconds delay
         */
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (HANDLER_FLAG) {
                    //Getting the total lines to write
                    mTotalLines = customFileReader.getTotalLines();
                    //Getting the data lines
                    mFlashRowList = customFileReader.readDataLines();
                }
            }
        }, 1000);
    }

    private void initializeGUIElements() {
        LinearLayout parent = (LinearLayout) mView.findViewById(R.id.parent_ota_type);
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.ota_title));
        setHasOptionsMenu(true);

        appDownload = (Button) mView.findViewById(R.id.ota_app_download);
        appStackCombDownload = (Button) mView.findViewById(R.id.ota_app_stack_comb);
        appStackSepDownload = (Button) mView.
                findViewById(R.id.ota_app_stack_seperate);
        mProgressText = (TextView) mView.
                findViewById(R.id.file_status);
        mProgressTop = (TextProgressBar) mView.findViewById(R.id.upgrade_progress_bar_top);
        mProgressBottom = (TextProgressBar) mView.findViewById(R.id.upgrade_progress_bar_bottom);
        mFileNameTop = (TextView) mView.findViewById(R.id.upgrade_progress_bar_top_filename);
        mFileNameBottom = (TextView) mView.findViewById(R.id.upgrade_progress_bar_bottom_filename);
        mStopUpgradeButton = (Button) mView.findViewById(R.id.stop_upgrade_button);
        mProgressDialog = new ProgressDialog(getActivity());

        progBarLayoutTop = (RelativeLayout) mView.findViewById(R.id.progress_bar_top_rel_lay);
        progBarLayoutBottom = (RelativeLayout) mView.findViewById(R.id.progress_bar_bottom_rel_lay);

        mProgressText.setVisibility(View.INVISIBLE);
        mStopUpgradeButton.setVisibility(View.INVISIBLE);
        progBarLayoutTop.setVisibility(View.INVISIBLE);
        progBarLayoutBottom.setVisibility(View.INVISIBLE);

        mProgressText.setEnabled(false);
        mProgressText.setClickable(false);
        progBarLayoutTop.setEnabled(false);
        progBarLayoutTop.setClickable(false);
        progBarLayoutBottom.setEnabled(false);
        progBarLayoutBottom.setClickable(false);

        parent.setOnClickListener(this);
        /**
         *Application Download
         */
        appDownload.setOnClickListener(this);

        /**
         *Application and Stack Combined Option
         */
        appStackCombDownload.setOnClickListener(this);

        /**
         *Application and Stack Seperate Option
         */
        appStackSepDownload.setOnClickListener(this);

        /**
         *Used to stop on going OTA Update service
         *
         */
        mStopUpgradeButton.setOnClickListener(this);
        setHasOptionsMenu(true);
    }

    private void initializeNotification() {
        mNotificationMAnager = (android.app.NotificationManager)
                getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(getActivity());
        mBuilder.setContentTitle(getResources().getString(R.string.ota_notification_title))
                .setAutoCancel(false)
                .setContentText(getResources().getString(R.string.ota_notification_ongoing))
                .setSmallIcon(R.drawable.appicon);
    }

    public void generatePendingNotification(Context mContext,
                                            android.app.NotificationManager mNotificationManager,
                                            NotificationCompat.Builder mBuilder, int mNotificationId) {
        // Displays the progress bar for the first time.
        mBuilder.setProgress(100, 0, false);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(mContext, HomePageActivity.class);

        // This somehow makes sure, there is only 1 CountDownTimer going if the notification is pressed:
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // The stack builder object will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(HomePageActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        // Make this unique ID to make sure there is not generated just a brand new intent with new extra values:
        int requestID = (int) System.currentTimeMillis();

        // Pass the unique ID to the resultPendingIntent:
        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, requestID, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);

        // mId allows you to update the notification later on.
        mNotificationManager.notify(mNotificationId, mBuilder.build());

    }

    public void cancelPendingNotification(android.app.NotificationManager mNotificationManager,
                                          int mNotificationId) {
        mNotificationManager.cancel(mNotificationId);
    }

    public void setProgress(android.app.NotificationManager mNotificationManager,
                            NotificationCompat.Builder mBuilder,
                            int limit, int updateLimit, boolean flag, int mNotificationId) {
        mBuilder.setProgress(limit, updateLimit, flag);
        mNotificationManager.notify(mNotificationId, mBuilder.build());
    }

    private void initializeBondingIFnotBonded() {
        final BluetoothDevice device = BluetoothLeService.mBluetoothAdapter
                .getRemoteDevice(BluetoothLeService.getmBluetoothDeviceAddress());
        if (!BluetoothLeService.getBondedState()) {
            pairDevice(device);

        }
    }

    //For Pairing
    private void pairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //For UnPairing
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onFileReadProgressUpdate(int fileLine) {
        if (mTotalLines > 0 && fileLine > 0) {
            //showProgress(1, fileLine, mTotalLines);
        }
        /**
         * All data lines read and stored to data model
         */
        if (mTotalLines == fileLine) {

            if (mOTACharacteristic != null) {
                mProgressText.setText(getActivity().getResources().
                        getText(R.string.ota_file_read_complete));
                Utils.setStringSharedPreference(getActivity(),
                        Constants.PREF_BOOTLOADER_STATE, "" + BootLoaderCommands.ENTER_BOOTLOADER);
                mFileupgradeStarted = true;
                generatePendingNotification(getActivity(),
                        mNotificationMAnager, mBuilder,
                        mNotificationId);
                otaFirmwareWrite.OTAEnterBootLoaderCmd(mCheckSumType);
                mProgressText.setText(getActivity().getResources().getText(R.string.ota_enter_bootloader));

            }
        }
    }

    /**
     * Method to show progress bar
     *
     * @param fileLineNos
     * @param totalLines
     */

    private void showProgress(int fileStatus, float fileLineNos, float totalLines) {
        if (fileStatus == 1) {
            mProgressTop.setProgress((int) fileLineNos);   // Main Progress
            mProgressTop.setMax((int) totalLines); // Maximum Progress
            mProgressTop.setProgressText("" + (int) ((fileLineNos / totalLines) * 100) + "%");
            setProgress(mNotificationMAnager,
                    mBuilder, 100, (int) ((fileLineNos / totalLines) * 100), false, mNotificationId);
        }
        if (fileStatus == 2) {
            mProgressTop.setProgress(100);
            mProgressTop.setMax(100);// Main Progress
            mProgressTop.setProgressText("100%");
            mProgressBottom.setProgress((int) fileLineNos);   // Main Progress
            mProgressBottom.setMax((int) totalLines);
            mProgressBottom.setProgressText("" + (int) ((fileLineNos / totalLines) * 100) + "%");
            setProgress(mNotificationMAnager,
                    mBuilder, 100, (int) ((fileLineNos / totalLines) * 100), false, mNotificationId);
        }
    }

    /**
     * Clears all Shared Preference Data & Resets UI
     */
    public void clearDataNPreferences() {



        //Resetting all preferences on Stop Button
        Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_ONE_NAME, "Default");
        Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_TWO_PATH, "Default");
        Utils.setStringSharedPreference(getActivity(), Constants.PREF_OTA_FILE_TWO_NAME, "Default");
        Utils.setStringSharedPreference(getActivity(), Constants.PREF_BOOTLOADER_STATE, "Default");
        Utils.setIntSharedPreference(getActivity(), Constants.PREF_PROGRAM_ROW_NO, 0);
        Utils.setIntSharedPreference(getActivity(), Constants.PREF_PROGRAM_ROW_START_POS, 0);
    }

    /**
     * Returns saved device adress
     *
     * @return
     */
    private String saveDeviceAddress() {
        String deviceAddress = BluetoothLeService.getmBluetoothDeviceAddress();
        Utils.setStringSharedPreference(getActivity(),
                Constants.PREF_DEV_ADDRESS, deviceAddress);
        return Utils.getStringSharedPreference(getActivity(),
                Constants.PREF_DEV_ADDRESS);
    }

    /**
     * Method to get required characteristics from service
     */

    void getGattData() {
        List<BluetoothGattCharacteristic> gattCharacteristics = mservice
                .getCharacteristics();
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            String uuidchara = gattCharacteristic.getUuid().toString();
            if (uuidchara.equalsIgnoreCase(GattAttributes.OTA_CHARACTERISTIC)) {
                mOTACharacteristic = gattCharacteristic;
                prepareBroadcastDataNotify(gattCharacteristic);
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
            BluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                    true);
        }

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
            BluetoothLeService.setCharacteristicNotification(
                    gattCharacteristic, false);
        }

    }

    private void showOTAStopAlert() {
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(
                getActivity().getResources().getString(
                        R.string.alert_message_ota_cancel))
                .setTitle(getActivity().getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setPositiveButton(
                        getActivity().getResources().getString(
                                R.string.alert_message_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (mOTACharacteristic != null) {
                                    stopBroadcastDataNotify(mOTACharacteristic);
                                    clearDataNPreferences();
                                    cancelPendingNotification(mNotificationMAnager,
                                            mNotificationId);
                                    final BluetoothDevice device = BluetoothLeService.mBluetoothAdapter
                                            .getRemoteDevice(BluetoothLeService.getmBluetoothDeviceAddress());
                                    OTAFirmwareUpgradeFragment.mFileupgradeStarted = false;

                                    BluetoothLeService.disconnect();
                                    unpairDevice(device);
                                    Toast.makeText(getActivity(),
                                            getResources().getString(R.string.alert_message_bluetooth_disconnect),
                                            Toast.LENGTH_SHORT).show();
                                    Intent intent = getActivity().getIntent();
                                    getActivity().finish();
                                    getActivity().overridePendingTransition(R.anim.slide_right, R.anim.push_right);
                                    startActivity(intent);
                                    getActivity().overridePendingTransition(R.anim.slide_left, R.anim.push_left);
                                }
                            }
                        })
                .setNegativeButton(getActivity().getResources().getString(
                        R.string.alert_message_no), null);
        alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        if (!getActivity().isDestroyed())
            alert.show();
    }

    private boolean secondFileUpdatedNeeded() {
        String secondFilePath = Utils.getStringSharedPreference(getActivity(),
                Constants.PREF_OTA_FILE_TWO_PATH);
        Logger.e("secondFilePath-->" + secondFilePath);
        return BluetoothLeService.getmBluetoothDeviceAddress().equalsIgnoreCase(saveDeviceAddress())
                && (!secondFilePath.equalsIgnoreCase("Default")
                && (!secondFilePath.equalsIgnoreCase("")));
    }

    /**
     * Method to Write the second file
     */
    private void secondFileUpgradation() {
        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(
                getActivity().getResources().getString(
                        R.string.alert_message_ota_resume))
                .setTitle(getActivity().getResources().getString(R.string.app_name))
                .setCancelable(false)
                .setPositiveButton(
                        getActivity().getResources().getString(
                                R.string.alert_message_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                Utils.setStringSharedPreference(getActivity(),
                                        Constants.PREF_BOOTLOADER_STATE, null);
                                Utils.setIntSharedPreference(getActivity(),
                                        Constants.PREF_PROGRAM_ROW_NO, 0);
                                Utils.setIntSharedPreference(getActivity(),
                                        Constants.PREF_PROGRAM_ROW_START_POS, 0);
                                generatePendingNotification(getActivity(),
                                        mNotificationMAnager, mBuilder,
                                        mNotificationId);

                                getGattData();
                                //Updating the  file name with progress text
                                String fileOneName = Utils.getStringSharedPreference(
                                        getActivity(), Constants.PREF_OTA_FILE_ONE_NAME);
                                if (mProgressTop.getVisibility() != View.VISIBLE) {
                                    mProgressTop.setVisibility(View.VISIBLE);
                                }
                                mFileNameTop.setText(fileOneName.replace(".cyacd", ""));
                                String fileTwoName = Utils.
                                        getStringSharedPreference(getActivity(),
                                                Constants.PREF_OTA_FILE_TWO_NAME);
                                if (mProgressBottom.getVisibility() != View.VISIBLE) {
                                    mProgressBottom.setVisibility(View.VISIBLE);
                                }
                                mFileNameBottom.setText(fileTwoName.replace(".cyacd", ""));
                                appStackSepDownload.setSelected(true);
                                appStackSepDownload.setPressed(true);
                                appDownload.setEnabled(false);
                                appStackCombDownload.setEnabled(false);
                                appStackSepDownload.setEnabled(false);
                                mProgressText.setVisibility(View.VISIBLE);
                                mStopUpgradeButton.setVisibility(View.VISIBLE);
                                progBarLayoutTop.setVisibility(View.VISIBLE);
                                progBarLayoutBottom.setVisibility(View.VISIBLE);
                                mProgressText.setText(getActivity().getResources().
                                        getText(R.string.ota_file_read));
                                mCurrentFilePath = Utils.
                                        getStringSharedPreference(getActivity(),
                                                Constants.PREF_OTA_FILE_TWO_PATH);
                                clearDataNPreferences();
                                mProgressBarPosition = 2;
                                prepareFileWriting();
                            }
                        })
                .setNegativeButton(getActivity().getResources().getString(
                                R.string.alert_message_no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                clearDataNPreferences();
                            }
                        });
        alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        if (!getActivity().isDestroyed())
            alert.show();
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
