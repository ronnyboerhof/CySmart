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
package com.cypress.cysmart.CommonUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ProgressBar;

import com.cypress.cysmart.R;

/**
 * Custom progressBar with text
 */
public class TextProgressBar extends ProgressBar {
    private String progressText;
    private Paint progressPaint;

    public TextProgressBar(Context context) {
        super(context);
        initializeProgressBar();
    }

    public TextProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeProgressBar();
    }

    public TextProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializeProgressBar();
    }

    public void initializeProgressBar() {
        progressText="0%";
        progressPaint=new Paint();
        switch (getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_XHIGH:
                progressPaint.setTextSize(30);
                break;
            case DisplayMetrics.DENSITY_HIGH:
                progressPaint.setTextSize(16);
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                progressPaint.setTextSize(40);
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                progressPaint.setTextSize(50);
                break;
            default:
                progressPaint.setTextSize(50);
                break;
        }
        progressPaint.setColor(getResources().getColor(R.color.main_bg_color));
    }


    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Rect boundsProgress = new Rect();
        progressPaint.getTextBounds(progressText, 0, progressText.length(), boundsProgress);
        int xp;
        switch (getResources().getDisplayMetrics().densityDpi) {

            case DisplayMetrics.DENSITY_XHIGH:
                xp = getWidth() - 110;
                break;
            case DisplayMetrics.DENSITY_HIGH:
                xp = getWidth() - 70;
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                xp = getWidth() - 90;
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                xp = getWidth() - 130;
                break;
            default:
                xp = getWidth() - 130;
                break;
        }
        int yp = getHeight() / 2 - boundsProgress.centerY();
       canvas.drawText(progressText, xp, yp, progressPaint);
    }

    public synchronized void setProgressText(String text) {
        this.progressText = text;
        drawableStateChanged();
    }


}
