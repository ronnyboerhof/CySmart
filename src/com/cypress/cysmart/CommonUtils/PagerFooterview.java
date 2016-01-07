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
import android.graphics.RectF;
import android.view.View;

import com.cypress.cysmart.R;

/**
 * ViewPager page indicator
 */
public class PagerFooterview extends View {
    private Paint mPaint;
    private RectF mRect;
    private int mViewsCount;
    private int mPosition;
    private float mid;
    private int STROKE_WIDTH = 2;
    private int VIEWPORT_TOP = 10;
    private int VIEWPORT_RIGHT = 13;
    private int VIEWPORT_BOTTOM = 23;
    private int RED = 255;
    private int GREEN = 255;
    private int BLUE = 255;


    public PagerFooterview(Context context, int numOfViews, int width) {
        super(context);
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(R.color.actionbar_bg_color));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(STROKE_WIDTH);
        this.mViewsCount = numOfViews;
        mid = width / 2;
        mRect = new RectF(mid, VIEWPORT_TOP, mid + VIEWPORT_RIGHT, VIEWPORT_BOTTOM);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRGB(RED, GREEN, BLUE);
        for (int i = 0; i < mViewsCount; i++) {
            if (mPosition == i)
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mRect.set(mid + i * 18, VIEWPORT_TOP, mid + i * 18 + VIEWPORT_RIGHT, VIEWPORT_BOTTOM);
            canvas.drawOval(mRect, mPaint);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(STROKE_WIDTH);
        }
        super.onDraw(canvas);
    }

    public void Update(int p) {
        mPosition = p;
        invalidate();
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        // Set the movement bounds for the ball
        mid = w / 2;
        mid = mid - mViewsCount * 9;
    }
}
