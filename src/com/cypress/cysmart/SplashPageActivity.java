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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * Activity to display the initial splash screen
 */

public class SplashPageActivity extends Activity {
    /**
     * Page display time
     */
    private final int SPLASH_DISPLAY_LENGTH = 3000;
    /**
     * Flag to handle the handler
     */
    private boolean mHandlerFlag = true;

    private Handler handler = new Handler();
    private Runnable mrun = new Runnable() {

        @Override
        public void run() {

            if (mHandlerFlag) {
                // Finish the current Activity and start HomePage Activity
                Intent home = new Intent(SplashPageActivity.this,
                        HomePageActivity.class);
                startActivity(home);
                SplashPageActivity.this.finish();
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_splash);
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }
        /**
         * Run the code inside the runnable after the display time is finished
         * using a handler
         */
        handler.postDelayed(mrun, SPLASH_DISPLAY_LENGTH);
    }

    @Override
    public void onBackPressed() {
        /**
         * Disable the handler to execute when user presses back when in
         * SplashPage Activity
         */
        mHandlerFlag = false;
        super.onBackPressed();
    }
}
