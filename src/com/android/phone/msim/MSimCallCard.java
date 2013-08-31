/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2011-2013 The Linux Foundation. All rights reserved.
 *
 * Not a Contribution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import android.content.Context;
import android.telephony.MSimTelephonyManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;

import java.util.List;


/**
 * "Call card" UI element: the in-call screen contains a tiled layout of call
 * cards, each representing the state of a current "call" (ie. an active call,
 * a call on hold, or an incoming call.)
 */
public class MSimCallCard extends CallCard {
    private static final String LOG_TAG = "MSimCallCard";
    private static final boolean DBG = (MSimPhoneGlobals.DBG_LEVEL >= 2);

    private Context mContext;
    //Display subscription info for incoming call.
    private TextView mSubInfo;
    // Display sub icon beside the call state label
    private ImageView mCallStateSubIcon;
    // Display sub icon above the call duration when a call is connected
    private ImageView mInCallSubIcon;

    public MSimCallCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        if (DBG) log("MSimCallCard constructor...");
        if (DBG) log("- this = " + this);
        if (DBG) log("- context " + context + ", attrs " + attrs);
    }

   @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (DBG) log("CallCard onFinishInflate(this = " + this + ")...");
    }

    @Override
    /* package */ void updateState(CallManager cm) {
        if (DBG) log("updateState(" + cm + ")...");
        if (cm.getFirstActiveRingingCall().getState().isAlive()) {
            mPrimaryCallInfo = (ViewGroup) mCallInfoContainer
                    .findViewById(R.id.msim_primary_incoming_call_info);
            ((ViewGroup) mCallInfoContainer
                    .findViewById(R.id.msim_primary_call_info)).setVisibility(View.GONE);
        } else {
            mPrimaryCallInfo = (ViewGroup) mCallInfoContainer
                    .findViewById(R.id.msim_primary_call_info);
            ((ViewGroup) mCallInfoContainer
                    .findViewById(R.id.msim_primary_incoming_call_info)).setVisibility(View.GONE);
            mInCallSubIcon = (ImageView) mPrimaryCallInfo.findViewById(R.id.inCallSubIcon);
        }
        mSubInfo = (TextView) mPrimaryCallInfo.findViewById(R.id.subInfo);
        mCallStateSubIcon = (ImageView) mPrimaryCallInfo.findViewById(R.id.callStateSubIcon);

        doUpdate(cm);
    }

   // TODO need to find proper way to do this
    void updateSubInfo() {
        String[] sub = {"SUB 1", "SUB 2", "SUB 3"};
        int activeSub = -1;

        activeSub = PhoneUtils.getActiveSubscription();
        mSubInfo.setText(sub[activeSub]);

        CallManager cm = PhoneGlobals.getInstance().mCM;
        Call.State state = cm.getActiveFgCall().getState();
        if (!cm.hasActiveRingingCall() && state == Call.State.ACTIVE) {
            mInCallSubIcon.setImageDrawable(PhoneUtils.getMultiSimIcon(mContext, activeSub));
            mInCallSubIcon.setVisibility(View.VISIBLE);
            mCallStateSubIcon.setVisibility(View.GONE);
        } else {
            mCallStateSubIcon.setImageDrawable(PhoneUtils.getMultiSimIcon(mContext, activeSub));
            mCallStateSubIcon.setVisibility(View.VISIBLE);
            if (mInCallSubIcon != null) {
                mInCallSubIcon.setVisibility(View.GONE);
            }
        }

        log(" Updating SUB info " + sub[activeSub]);
    }

    /**
     * Updates the UI for the state where the phone is in use, but not ringing.
     */
    @Override
    protected void updateForegroundCall(CallManager cm) {
        super.updateForegroundCall(cm);

        //Update the subscriptio name on UI.
        updateSubInfo();
    }

    /**
     * Updates the UI for the state where an incoming call is ringing (or
     * call waiting), regardless of whether the phone's already offhook.
     */
    @Override
    protected void updateRingingCall(CallManager cm) {
        super.updateRingingCall(cm);

        //Update the subscriptio name on UI.
        updateSubInfo();
    }

    // Accessibility event support.
    // Since none of the CallCard elements are focusable, we need to manually
    // fill in the AccessibilityEvent here (so that the name / number / etc will
    // get pronounced by a screen reader, for example.)
    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.dispatchPopulateAccessibilityEvent(event);
        if (mSubInfo != null) {
            dispatchPopulateAccessibilityEvent(event, mSubInfo);
        }
        return true;
    }

    @Override
    protected boolean isEmergencyNumberWithoutSIMCard(Connection c) {
        return PhoneNumberUtils.isEmergencyNumber(c.getAddress()) &&
                (MSimTelephonyManager.getDefault().getSimState(c.getCall().getPhone()
                        .getSubscription()) == TelephonyManager.SIM_STATE_ABSENT);
    }

    // Debugging / testing code

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
