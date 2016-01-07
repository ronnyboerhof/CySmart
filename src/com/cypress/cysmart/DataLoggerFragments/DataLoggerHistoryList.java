package com.cypress.cysmart.DataLoggerFragments;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.DataModelClasses.DataLoggerModel;
import com.cypress.cysmart.ListAdapters.DataLoggerListAdapter;
import com.cypress.cysmart.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Activity to select the history file
 */
public class DataLoggerHistoryList extends Activity{
    /**
     * ListView for loading the data logger files
     */
    private ListView mListFileNames;
    /**
     * Adapter for ListView
     */
    DataLoggerListAdapter mAdapter;
    /**
     * File names
     */
    private ArrayList<DataLoggerModel> mDataLoggerArrayList;
    DataLoggerModel mDataLoggerModel;
    /**
     * Directory of the file
     */
    private String mDirectory;
    /**
     * File
     */
    private File mFile;
    private static String mLastFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.datalogger_list);

        if (Utils.isTablet(this)) {
            Logger.d("tablet");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            Logger.d("Phone");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        mListFileNames = (ListView)findViewById(R.id.data_logger_history_list);
        mDataLoggerArrayList = new ArrayList<DataLoggerModel>();
        mDataLoggerModel = new DataLoggerModel();

        // Getting the directory CySmart
        mDirectory = Environment.getExternalStorageDirectory() + File.separator
                + getResources().getString(R.string.data_logger_directory);
        mFile = new File(mDirectory);

        String filePattern = ".txt";

        // Listing all files in the directory
        final File list[] = mFile.listFiles();
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (list[i].getName().toString().contains(filePattern)) {
                    Logger.i(list[i].getAbsolutePath());
                    mDataLoggerModel = new DataLoggerModel(list[i].getName(), list[i].lastModified(), list[i].getAbsolutePath());
                    mDataLoggerArrayList.add(mDataLoggerModel);
                }
            }
        }

        Collections.sort(mDataLoggerArrayList, new Comparator<DataLoggerModel>() {
            @Override
            public int compare(DataLoggerModel dataLoggerModel, DataLoggerModel dataLoggerModel2) {

//                return ((int) (dataLoggerModel2.getFileDate() - dataLoggerModel.getFileDate()));
                return dataLoggerModel2.getFileDate().compareTo(dataLoggerModel.getFileDate());
            }
        });

        // Adding data to adapter
        DataLoggerListAdapter adapter = new DataLoggerListAdapter(
                this, mDataLoggerArrayList);
        mListFileNames.setAdapter(adapter);
        mListFileNames.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
                                    long arg3) {
                /**
                 * Getting the absolute path. Adding the DataLogger fragment
                 * with the data of the file user selected
                 */
                String path = mDataLoggerArrayList.get(pos).getFilePath();
                Logger.i("Selected file path" + mDataLoggerArrayList.get(pos).getFilePath());
                Intent returnIntent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString(Constants.DATA_LOGGER_FILE_NAAME, path);
                bundle.putBoolean(Constants.DATA_LOGGER_FLAG, true);
                returnIntent.putExtras(bundle);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });
    }
}
