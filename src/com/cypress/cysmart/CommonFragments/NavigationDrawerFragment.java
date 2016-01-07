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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.cypress.cysmart.CommonUtils.Constants;
import com.cypress.cysmart.CommonUtils.Logger;
import com.cypress.cysmart.CommonUtils.Utils;
import com.cypress.cysmart.DataModelClasses.NavigationDrawerModel;
import com.cypress.cysmart.ListAdapters.NavDrawerExpandableListAdapter;
import com.cypress.cysmart.ListAdapters.NavDrawerListAdapter;
import com.cypress.cysmart.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Fragment used for managing interactions for and presentation of a navigation
 * drawer.
 */
public class NavigationDrawerFragment extends Fragment {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    /**
     * NavigationDrawer Layout
     */
    private DrawerLayout mDrawerLayout;
    private ExpandableListView mDrawerListView;
    private View mFragmentContainerView;

    /**
     * Current user selected position in the NavigationDrawer list.
     */
    private int mCurrentSelectedPosition = 0;

    /**
     * ArrayList holding the NavigationDrawerModel data
     */
    private ArrayList<NavigationDrawerModel> mNavDrawerItems;
    private HashMap<NavigationDrawerModel, List<String>> mNavDrawerChildItems;

    /**
     * Adapter for holding the NavigationDrawer List.
     */
    private NavDrawerListAdapter mAdapter;
    private NavDrawerExpandableListAdapter mNavigationItemsAdapter;
    /**
     * NavigationDrawer menu item titles list.
     */
    private String[] mNavMenuTitles;
    /**
     * NavigationDrawer menu icons
     */
    private TypedArray mNavMenuIcons;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Getting the savedInstanceState and through that the user selected
        // position
        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState
                    .getInt(STATE_SELECTED_POSITION);
        }

        // Select either the default item (0) or the last selected item.
        //  selectItem(mCurrentSelectedPosition);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of
        // actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerListView = (ExpandableListView) inflater.inflate(
                R.layout.fragment_navigation_drawer, container, false);
        // Navigation drawer title with custom layout
        ViewGroup header = (ViewGroup) inflater.inflate(
                R.layout.fragment_drawer_header, mDrawerListView, false);
        mDrawerListView.addHeaderView(header, null, false);
        // Adding NavigationDrawer items to array
        mNavDrawerItems = new ArrayList<NavigationDrawerModel>();
        // load slide menu items
        mNavMenuTitles = getResources()
                .getStringArray(R.array.nav_drawer_items);
        // Navigation drawer icons from resources
        mNavMenuIcons = getResources().obtainTypedArray(
                R.array.nav_drawer_icons);
        /**
         * Adding NavigationDrawer items to array
         */
        mNavDrawerItems = new ArrayList<NavigationDrawerModel>();
        mNavDrawerChildItems = new HashMap<NavigationDrawerModel, List<String>>();

        // BLE Devices
        mNavDrawerItems.add(new NavigationDrawerModel(mNavMenuTitles[0],
                mNavMenuIcons.getResourceId(0, -1)));
        // Cypress
        mNavDrawerItems.add(new NavigationDrawerModel(mNavMenuTitles[1],
                mNavMenuIcons.getResourceId(1, -1)));
        //Cypress subitems
        List<String> subitems = new ArrayList<String>();
        subitems.add(getResources().getString(R.string.navigation_drawer_child_home));
        subitems.add(getResources().getString(R.string.navigation_drawer_child_ble));
        subitems.add(getResources().getString(R.string.navigation_drawer_child_mobile));
        subitems.add(getResources().getString(R.string.navigation_drawer_child_contact));
        mNavDrawerChildItems.put(mNavDrawerItems.get(1), subitems);

        // Contact US
        mNavDrawerItems.add(new NavigationDrawerModel(mNavMenuTitles[2],
                mNavMenuIcons.getResourceId(2, -1)));

        // Setting the NavigationDrawer list adapter
        mAdapter = new NavDrawerListAdapter(getActivity(), mNavDrawerItems);
        mNavigationItemsAdapter = new NavDrawerExpandableListAdapter(getActivity(), mNavDrawerItems,
                mNavDrawerChildItems);
        mDrawerListView.setAdapter(mNavigationItemsAdapter);
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        mDrawerListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView,
                                        View view, int groupPosition, long l) {
                if (groupPosition == 1) {
                    return false;
                } else {
                    selectItem(groupPosition);
                    return true;
                }

            }
        });
        mDrawerListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView,
                                        View view, int groupPosition, int childPosition, long l) {
                if (groupPosition == 1) {
                    TextView childText = (TextView) view.findViewById(R.id.lblListItem);
                    selectChildView(childText.getText().toString());
                    return true;
                } else {
                    return false;
                }

            }
        });
        mDrawerListView.expandGroup(1);
        return mDrawerListView;
    }

    private void selectChildView(String childText) {
        if (childText.equalsIgnoreCase(getResources().
                getString(R.string.navigation_drawer_child_ble))) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.LINK_BLE_PRODUCTS));
            startActivity(intent);

        } else if (childText.equalsIgnoreCase(getResources().
                getString(R.string.navigation_drawer_child_home))) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.LINK_CYPRESS_HOME));
            startActivity(intent);
        } else if (childText.equalsIgnoreCase(getResources().
                getString(R.string.navigation_drawer_child_contact))) {
            if (Utils.checkNetwork(getActivity())) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.LINK_CONTACT_US));
                startActivity(intent);
            } else {
                ContactUsFragment contactFragment = new ContactUsFragment();
                displayView(contactFragment);
                if (mDrawerLayout != null) {
                    mDrawerLayout.closeDrawer(mFragmentContainerView);
                }
            }
        } else if (childText.equalsIgnoreCase(getResources().
                getString(R.string.navigation_drawer_child_mobile))) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.LINK_CYSMART_MOBILE));
            startActivity(intent);
        }
    }

    /**
     * Used for replacing the main content of the view with provided fragments
     *
     * @param fragment
     */
    void displayView(Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.container, fragment)
                .addToBackStack(null).commit();
    }

    /**
     * Check whether NavigationDrawer is opened or closed
     *
     * @return {@link Boolean}
     */
    public boolean isDrawerOpen() {
        return mDrawerLayout != null
                && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation
     * drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer
        // opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
                GravityCompat.START);

        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                R.drawable.ic_launcher, /* NavigationDrawer image to replace 'Up' caret */
                R.string.navigation_drawer_open, /*
                                         * "open drawer" description for
										 * accessibility
										 */
                R.string.navigation_drawer_close /*
                                         * "close drawer" description for
										 * accessibility
										 */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().invalidateOptionsMenu(); // calls
                // onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().invalidateOptionsMenu(); // calls
                // onPrepareOptionsMenu()
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    void selectItem(int position) {
        Logger.e("selectItem--" + position);
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    "Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


//    //private ActionBar getActionBar() {
//        return getActivity().getActionBar();
//    }

    /**
     * CallBacks interface that all activities using this fragment must
     * implement.
     */
    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int position);
    }

}
