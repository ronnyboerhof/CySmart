/*
 * Copyright Cypress Semiconductor Corporation, 2014-2015 All rights reserved.
 *
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign),
 * United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate
 * license agreement between you and Cypress, this Software
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
package com.cypress.cysmart.RDKEmulatorView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cypress.cysmart.BLEConnectionServices.BluetoothLeService;
import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Fragment class to show the emulator view of the Remote control RDK which has Human Interface
 * Device sservice
 */
public class MicrophoneEmulatorFragment extends Fragment {

    //temporary file names
    public static final String mfilePCM = Environment.getExternalStorageDirectory()
            + File.separator + "CySmart" + File.separator + "RecordedAudio.pcm";
    // GATT service
    private static BluetoothGattService mservice;
    String mfileWAV= Environment.getExternalStorageDirectory()
            + File.separator + "CySmart" + File.separator + "RecordedAudio.wav";
    File mFilePCM;
    //Constants
    private int PACKETSIZE = 16384;
    //UI elements
    private TextView mHexValue;
    /**
     * BroadcastReceiver for receiving the GATT server status
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            // GATT Data available
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                /**
                 * Byte information send through BLE received here
                 */
                if (extras.containsKey(Constants.EXTRA_BYTE_VALUE)) {
                    byte[] array = intent
                            .getByteArrayExtra(Constants.EXTRA_BYTE_VALUE);
                    /**
                     * Report reference descriptor received
                     */
                    if (extras.containsKey(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID)) {
                        String reportReference = intent.getStringExtra
                                (Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID);
                        /**
                         * Audio report reference data received
                         */
                        if (reportReference.equalsIgnoreCase(ReportAttributes.
                                AUDIO_REPORT_REFERENCE_DATA_STRING)) {
                            /**
                             * Converting the received voice data to HEX value
                             * Update the value in the UI
                             */
                            String hexValue = getHexValue(array);
                            displayHexData(hexValue);
                        }
                    }

                }
            }
        }

    };
    // create temp file that will hold byte array
    // File tempMp3;
    private TextView mConvertedTextValue;
    private Button mGoogleVoiceRecord;
    private MediaPlayer mMediaPlayer;

    private static byte[] intToByteArray(int i) {
        byte[] b = new byte[4];
        b[0] = (byte) (i & 0x00FF);
        b[1] = (byte) ((i >> 8) & 0x000000FF);
        b[2] = (byte) ((i >> 16) & 0x000000FF);
        b[3] = (byte) ((i >> 24) & 0x000000FF);
        return b;
    }

    // convert a short to a byte array
    public static byte[] shortToByteArray(short data) {
        /*
         * NB have also tried:
         * return new byte[]{(byte)(data & 0xff),(byte)((data >> 8) & 0xff)};
         *
         */

        return new byte[]{(byte) (data & 0xff), (byte) ((data >>> 8) & 0xff)};
    }

    //Constructor
    public MicrophoneEmulatorFragment create(BluetoothGattService bluetoothGattService) {
        mservice = bluetoothGattService;
        MicrophoneEmulatorFragment remoteControlEmulatorService = new MicrophoneEmulatorFragment();
        return remoteControlEmulatorService;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.rdk_microphone, container,
                false);
        mHexValue = (TextView) rootView.findViewById(R.id.hex_value);
        mConvertedTextValue = (TextView) rootView.findViewById(R.id.converted_text_value);
        final Button mGoogleVoiceConvert = (Button) rootView.findViewById(R.id.voiceconversion);
        Button mAPIChange = (Button) rootView.findViewById(R.id.apichange);
        mGoogleVoiceRecord = (Button) rootView.findViewById(R.id.voicerecord);
        /**
         * Deleting the old recorded file and creating new one
         */
        mFilePCM = new File(mfilePCM);
        if (mFilePCM.exists()) {
            mFilePCM.delete();
        }
        try {
            mFilePCM.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
         /**
         * Convert button click listner
         */
        mGoogleVoiceConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String startRecord = getActivity().getResources().
                        getString(R.string.record);
                mGoogleVoiceRecord.setText(startRecord);
              if(mFilePCM.length()!=0){
                   String  mStoredAPIKey = Utils.getStringSharedPreference(getActivity(),
                          Constants.PREF_GOOGLE_API_KEY);
                  if(RemoteControlEmulatorFragment.mIsrecording){

                      RemoteControlEmulatorFragment.mIsrecording=false;
                      File file = new File(mfilePCM);
                      createWavFile(file,mfileWAV);
                  }
                  if (mStoredAPIKey.equalsIgnoreCase("")) {
                      showCustumAlert("",false);
                  } else {
                      if (Utils.checkNetwork(getActivity())) {
                          googleVoiceConversion googleVoiceConversion = new googleVoiceConversion();
                          googleVoiceConversion.execute();
                      } else {
                          Toast.makeText(getActivity(), "Please check your internet connection",
                                  Toast.LENGTH_SHORT).
                                  show();
                      }
                  }
              }else{
                  Toast.makeText(getActivity(),"No file recorded for conversion",Toast.LENGTH_SHORT).
                          show();
              }


            }
        });
        /**
         * Record button click listner
         */
        mGoogleVoiceRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView recordStatus = (TextView) view;
                String startRecord = getActivity().getResources().getString(R.string.record);
                String stopRecord = getActivity().getResources().getString(R.string.record_stop);
                if (recordStatus.getText().toString().equalsIgnoreCase(startRecord)) {
                    mFilePCM = new File(mfilePCM);
                    if (mFilePCM.exists()) {
                        mFilePCM.delete();
                    }
                    try {
                        mFilePCM.createNewFile();
                        RemoteControlEmulatorFragment.mIsrecording = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    recordStatus.setText(stopRecord);
                } else if (recordStatus.getText().toString().equalsIgnoreCase(stopRecord)) {
                    recordStatus.setText(startRecord);
                    RemoteControlEmulatorFragment.mIsrecording = false;
                    File file = new File(mfilePCM);
                    createWavFile(file,mfileWAV);
                }
            }
        });
        mAPIChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              String  mStoredAPIKey = Utils.getStringSharedPreference(getActivity(),
                        Constants.PREF_GOOGLE_API_KEY);
                Logger.e("mStoredAPIKey--"+mStoredAPIKey);
                showCustumAlert(mStoredAPIKey,true);
            }
        });
        return rootView;
    }

    /**
     * Method to display a custom alert.
     * Option for entering the google key in the method for voice to
     * text conversion
     */
    private void showCustumAlert(String storedKey,boolean changeNeeded) {

        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.api_key_dialog_alert, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getActivity());

        // set api_key_dialog_alert.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        //User input Edittext
        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.custom_alert_user_input);
        if(changeNeeded){
            userInput.setText(storedKey);
        }

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to sharedpreferecne
                                Logger.i("userInput.getText()--->" + userInput.getText());
                                Utils.setStringSharedPreference(getActivity(),
                                        Constants.PREF_GOOGLE_API_KEY,userInput.getText().
                                                toString());
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mGattUpdateReceiver,
                Utils.makeGattUpdateIntentFilter());
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.rdk_emulator_view));
    }

    @Override
    public void onDestroy() {
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        super.onDestroy();
    }

    /**
     * Method to update the GUI with received HEX value
     *
     * @param value
     */
    private void displayHexData(String value) {
        mHexValue.setText(value);
    }

    /**
     * Converting the byte array to Hex vale
     *
     * @param array
     * @return
     */
    private String getHexValue(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (byte byteChar : array) {
            sb.append(String.format("%02x", byteChar));
        }
        return "" + sb;
    }

    /**
     * Used for replacing the main content of the view with provided fragments
     *
     * @param fragment
     */
    void displayView(Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().add(R.id.container, fragment)
                .addToBackStack(null).commit();
    }

    private void createWavFile(File fileToConvert,String wavFilePath){
        try {
            long mySubChunk1Size = 16;
            int myBitsPerSample= 16;
            int myFormat = 1;
            long myChannels = 1;
            long mySampleRate = 16000;
            long myByteRate = mySampleRate * myChannels * myBitsPerSample/8;
            int myBlockAlign = (int) (myChannels * myBitsPerSample/8);

            byte[] clipData = getBytesFromFile(fileToConvert);

            long myDataSize = clipData.length;
            long myChunk2Size =  myDataSize * myChannels * myBitsPerSample/8;
            long myChunkSize = 36 + myChunk2Size;

            OutputStream os;
            os = new FileOutputStream(new File(wavFilePath));
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream outFile = new DataOutputStream(bos);

            outFile.writeBytes("RIFF");                                 // 00 - RIFF
            outFile.write(intToByteArray((int)myChunkSize), 0, 4);      // 04 - how big is the rest of this file?
            outFile.writeBytes("WAVE");                                 // 08 - WAVE
            outFile.writeBytes("fmt ");                                 // 12 - fmt
            outFile.write(intToByteArray((int)mySubChunk1Size), 0, 4);  // 16 - size of this chunk
            outFile.write(shortToByteArray((short)myFormat), 0, 2);     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
            outFile.write(shortToByteArray((short)myChannels), 0, 2);   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
            outFile.write(intToByteArray((int)mySampleRate), 0, 4);     // 24 - samples per second (numbers per second)
            outFile.write(intToByteArray((int)myByteRate), 0, 4);       // 28 - bytes per second
            outFile.write(shortToByteArray((short)myBlockAlign), 0, 2); // 32 - # of bytes in one sample, for all channels
            outFile.write(shortToByteArray((short)myBitsPerSample), 0, 2);  // 34 - how many bits in a sample(number)?  usually 16 or 24
            outFile.writeBytes("data");                                 // 36 - data
            outFile.write(intToByteArray((int)myDataSize), 0, 4);       // 40 - how big is this data chunk
            outFile.write(clipData);                                    // 44 - the actual data itself - just a long string of numbers

            outFile.flush();
            outFile.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] getBytesFromFile(File fileToConvert) {
        int size = (int) fileToConvert.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(fileToConvert));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Logger.e("Read file Length"+bytes.length);
        return bytes;
    }

    private class googleVoiceConversion extends AsyncTask<String, String, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("Converting audio Please wait...");
            progressDialog.show();
            mMediaPlayer = new MediaPlayer();
            try {
                mMediaPlayer.setDataSource(mfileWAV);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        protected String doInBackground(String... strings) {
            String convertedResult = "";
            String possibleOutcomes = "No possible text found";
            try {
                String apiKey = Utils.getStringSharedPreference(getActivity(),
                        Constants.PREF_GOOGLE_API_KEY);
                URL url = new URL("https://www.google.com/speech-api/v2/recognize?" +
                        "output=json&lang=en-us&key=" + apiKey);
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                // add reuqest header
                con.setRequestMethod("POST");
                //for FLAC
                //  con.setRequestProperty("Content-Type", "audio/x-flac; rate=44100");
                //for PCM wav
                con.setRequestProperty("Content-Type", "audio/l16; rate=16000");
                con.setDoOutput(true);
                InputStream audioInputStream = null;
                try {
                    audioInputStream = new FileInputStream(mfileWAV);
                    byte[] audioData = getByteDataFromInputStream(audioInputStream);
                    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                    wr.write(audioData);
                    wr.flush();
                    wr.close();
                } catch (Exception exception) {
                } finally {
                    if (audioInputStream != null) {
                        audioInputStream.close();
                    }
                }

                int responseCode = con.getResponseCode();
                InputStream inputStream = con.getInputStream();
                Reader in = new InputStreamReader(inputStream);
                BufferedReader bufferedreader = new BufferedReader(in);
                StringBuilder stringBuilder = new StringBuilder();
                String stringReadLine = null;
                while ((stringReadLine = bufferedreader.readLine()) != null) {
                    stringBuilder.append(stringReadLine + "\n");
                }
                System.out.println(" response : " + stringBuilder);
                String result = stringBuilder.toString().replace("{\"result\":[]}\n", "");
                JSONObject json = new JSONObject(result);
                JSONArray jsonArray = json.getJSONArray("result");
                Logger.i("JSON array--->result-->" + jsonArray);
                for (int count = 0; count < jsonArray.length(); count++) {
                    JSONObject jsonAlternative = jsonArray.getJSONObject(count);
                    JSONArray jsonAlternativesArray = jsonAlternative.getJSONArray("alternative");
                    Logger.i("JSON array--->alternative-->" + jsonArray);
                    for (int pos = 0; pos < jsonAlternativesArray.length(); pos++) {
                        JSONObject jsonTranscript = jsonAlternativesArray.getJSONObject(pos);
                        String transcript = "";
                        if (jsonTranscript.has("transcript")) {
                            transcript = jsonTranscript.getString("transcript");
                            if (jsonTranscript.has("confidence")) {
                                convertedResult = transcript;
                            } else {
                                possibleOutcomes = transcript + "\n";
                            }
                        }
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (convertedResult.equalsIgnoreCase("")) {
                return possibleOutcomes;
            } else {
                return convertedResult;
            }


        }

        private byte[] getByteDataFromInputStream(InputStream inputStream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[PACKETSIZE];

            try {
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                buffer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return buffer.toByteArray();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            mConvertedTextValue.setText(result);
        }
    }
}


