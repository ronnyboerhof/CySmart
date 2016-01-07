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

package com.cypress.cysmart.DataLoggerFragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.HomePageActivity;
import com.cypress.cysmart.ListAdapters.DataLogsListAdapter;
import com.cypress.cysmart.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Fragment to show the DataLogger
 */
public class DataLoggerFragment extends Fragment implements AbsListView.OnScrollListener {
    /**
     * FilePath of DataLogger
     */
    private static String mFilepath;
    int mTotalLinesToRead = 0;
    ProgressDialog mProgressDialog;
    private static String mLastFragment;
    /**
     * Log Data Temporay storage
     */
    ArrayList<String> readLogData;
    /**
     * List Adapter
     */
    DataLogsListAdapter mAdapter;
    /**
     * visibility flag
     */
    private boolean mVisible = false;
    /**
     * DataLogger text
     */
    private ListView mLogList;
    /**
     * Lazyloading variables
     */
    private int mStartLine = 0;
    private int mStopLine = 500;
    private boolean lazyLoadingEnabled = false;
    /**
     * GUI elements
     */
    private TextView mFileName;
    private Button mScrollDown;

    /**
     * Constructor
     *
     * @return DataLoggerFragment
     */
    public DataLoggerFragment create(String lastFragment) {
        Logger.e("create last frag--"+lastFragment);
        this.mLastFragment=lastFragment;
        return new DataLoggerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.datalogger, container, false);
        mLogList = (ListView) rootView.findViewById(R.id.txtlog);
        mFileName = (TextView) rootView.findViewById(R.id.txt_file_name);
        mScrollDown = (Button) rootView.findViewById(R.id.btn_scroll_down);


        /*
        /History option text
        */
        TextView mDataHistory = (TextView) rootView.findViewById(R.id.txthistory);
        //mScrollView = (CustumScrollView) rootView.findViewById(R.id.scroll_view_logger);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mFilepath = bundle.getString(Constants.DATA_LOGGER_FILE_NAAME);
            mVisible = bundle.getBoolean(Constants.DATA_LOGGER_FLAG);
            File fileinView = new File(mFilepath);
            mFileName.setText(fileinView.getName());
        }
        // Handling the history text visibility based on the received Arguments
        if (mVisible) {
            mDataHistory.setVisibility(View.GONE);
        } else {
            Toast.makeText(getActivity(), getResources().
                    getString(R.string.data_logger_timestamp) + Utils.GetTimeandDateUpdate()
                    , Toast.LENGTH_SHORT).show();
            mDataHistory.setVisibility(View.VISIBLE);
        }
        mDataHistory.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent dataloggerHistory=new Intent(getActivity(),DataLoggerHistoryList.class);
                startActivityForResult(dataloggerHistory,123);

            }
        });
        prepareData();
        setHasOptionsMenu(true);
        mScrollDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mLogList.post(new Runnable() {
                    public void run() {
                        mLogList.setSelection(mLogList.getCount() - 1);
                    }
                });
            }
        });
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        getActivity();
        if (resultCode == Activity.RESULT_OK) {
            Bundle bundleReceived=data.getExtras();
            mFilepath = bundleReceived.getString(Constants.DATA_LOGGER_FILE_NAAME);
            mVisible = bundleReceived.getBoolean(Constants.DATA_LOGGER_FLAG);
            File fileinView = new File(mFilepath);
            mFileName.setText(fileinView.getName());
            prepareData();
        }
    }

    @Override
    public void onResume() {
        getActivity().setProgressBarIndeterminateVisibility(false);
        Utils.setUpActionBar(getActivity(),
                getResources().getString(R.string.data_logger));
        super.onResume();
    }

    @Override
    public void onPause() {
        Utils.setUpActionBar(getActivity(),
                mLastFragment);
        super.onPause();
    }
public void prepareData(){
    mTotalLinesToRead = getTotalLines();
    readLogData=new ArrayList<String>();
    mAdapter = new DataLogsListAdapter(getActivity(), readLogData);
    mLogList.setAdapter(mAdapter);
    mProgressDialog = new ProgressDialog(getActivity());
    //scrollMyListViewToBottom();
    if (mTotalLinesToRead > 5000) {
        // mLogList.setOnScrollListener(this);
        lazyLoadingEnabled = true;
        loadLogdata loadLogdata = new loadLogdata(mStartLine, mStopLine);
        loadLogdata.execute();
        mProgressDialog.setTitle(
                getResources().
                        getString(R.string.app_name));
        mProgressDialog.setMessage(getResources().
                getString(R.string.alert_message_log_read));
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

    } else {
        lazyLoadingEnabled = false;
        loadLogdata loadLogdata = new loadLogdata(0, 0);
        loadLogdata.execute();
    }
}

    /**
     * Reading the data from the file stored in the FilePath
     *
     * @return {@link String}
     * @throws FileNotFoundException
     */
    private ArrayList<String> logdata() throws FileNotFoundException {
        File file = new File(mFilepath);
        ArrayList<String> dataLines = new ArrayList<String>();
        if (!file.exists()) {
            return dataLines;
        } else {

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    dataLines.add(line);
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return dataLines;
        }

    }

    /**
     * Reading the data from the file stored in the FilePath for particular set of lines
     *
     * @return {@link String}
     * @throws FileNotFoundException
     */
    private ArrayList<String> logdata(int startLine, int stopLine) throws FileNotFoundException {
        File file = new File(mFilepath);
        ArrayList<String> dataLines = new ArrayList<String>();
        if (!file.exists()) {
            return dataLines;
        } else {
            BufferedReader buffreader = new BufferedReader(new FileReader(file));
            String line;
            int lines = 0;
            try {
                while ((line = buffreader.readLine()) != null) {
                    lines++;
                    if (lines > startLine && lines <= stopLine) {
                        dataLines.add(line);
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return dataLines;
        }
    }

    /**
     * Method to count the total lines in the selected file
     *
     * @return totalLines
     */
    public int getTotalLines() {
        int totalLines = 0;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(mFilepath));
            while ((bufferedReader.readLine()) != null) {
                totalLines++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalLines;
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE) {
            if (mLogList.getLastVisiblePosition() >= mLogList.getCount() - 1 - 0) {
                //load more list items:
                if (lazyLoadingEnabled) {
                    mStartLine = mStopLine;
                    mStopLine = mStopLine + 500;
                    if (mStopLine < mTotalLinesToRead) {
                        loadLogdata loadLogdata = new loadLogdata(mStartLine, mStopLine);
                        loadLogdata.execute();
                    } else {
                        loadLogdata loadLogdata = new loadLogdata(mStartLine, mTotalLinesToRead);
                        loadLogdata.execute();
                        lazyLoadingEnabled = false;
                    }
                }

            }
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.global, menu);
        MenuItem share = menu.findItem(R.id.share);
        MenuItem sharelogger = menu.findItem(R.id.sharelogger);
        MenuItem log = menu.findItem(R.id.log);
        MenuItem search = menu.findItem(R.id.search);
        MenuItem graph = menu.findItem(R.id.graph);
        search.setVisible(false);
        share.setVisible(false);
        log.setVisible(false);
        graph.setVisible(false);
        sharelogger.setVisible(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        switch (item.getItemId()) {
            case R.id.sharelogger:
                shareDataLoggerFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Sharing the data logger txt file
     */
    private void shareDataLoggerFile() {
        HomePageActivity.containerView.invalidate();
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // set the type to 'email'
        emailIntent.setType("vnd.android.cursor.dir/email");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, "");
        // the attachment
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(mFilepath)));
        // the mail subject
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Data Logger File");
        startActivity(Intent.createChooser(emailIntent, "Send email..."));

    }

    /**
     * AsyncTask class for loading logger data
     */
    private class loadLogdata extends AsyncTask<Void, Void, ArrayList<String>> {
        int startLine = 0;
        int stopLine = 0;
        ArrayList<String> newData = new ArrayList<String>();


        public loadLogdata(int startLine, int stopLine) {
            this.startLine = startLine;
            this.stopLine = stopLine;
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

        protected ArrayList<String> doInBackground(Void... params) {
            try {
                if (startLine == 0 && stopLine == 0) {
                    newData = logdata();
                } else {
                    newData = logdata(startLine, stopLine);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }

            return newData;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            readLogData.addAll(result);
            //load more list items:
            if (lazyLoadingEnabled) {
                mStartLine = mStopLine;
                mStopLine = mStopLine + 500;
                if (mStopLine < mTotalLinesToRead) {
                    loadLogdata loadLogdata = new loadLogdata(mStartLine, mStopLine);
                    loadLogdata.execute();
                } else {
                    loadLogdata loadLogdata = new loadLogdata(mStartLine, mTotalLinesToRead);
                    loadLogdata.execute();
                    lazyLoadingEnabled = false;
                    mProgressDialog.dismiss();
                }
            } else {
                mProgressDialog.dismiss();
            }
            Logger.i("Total size--->" + readLogData.size());
            mAdapter.addData(readLogData);
            mAdapter.notifyDataSetChanged();
        }
    }

}
