/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2011-2012 The Linux Foundation. All rights reserved.
 *
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only
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
import android.telephony.PhoneNumberUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.PhoneConstants;
import com.android.phone.R;

import java.util.List;
import com.qrd.plugin.feature_query.FeatureQuery;

/**
 * "Call card" UI element: the in-call screen contains a tiled layout of call
 * cards, each representing the state of a current "call" (ie. an active call,
 * a call on hold, or an incoming call.)
 */
public class MSimCallCard extends CallCard {
    private static final String LOG_TAG = "MSimCallCard";
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    //Display subscription info for incoming call.
    private TextView mSubInfo;
    public MSimCallCard(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (DBG) log("MSimCallCard constructor...");
        if (DBG) log("- this = " + this);
        if (DBG) log("- context " + context + ", attrs " + attrs);
    }

   @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (DBG) log("CallCard onFinishInflate(this = " + this + ")...");
    }

    /**
     * The SLOT Information content, refresh the display when enter InCallScreen interface
     */
    @Override
    protected void updateState(CallManager cm) {
        // UX_Enhance_Dialer
        if (FeatureQuery.FEATURE_UX_DIALER_INCOMINGCALL && mInCallScreen.getInCallTouchUi().showIncomingCallControls()) {
            mPrimaryCallInfo = (ViewGroup) mCallInfoContainer.findViewById(R.id.msim_primary_incoming_call_info);
            ((ViewGroup) mCallInfoContainer.findViewById(R.id.msim_primary_call_info)).setVisibility(View.GONE);
        } else {
            mPrimaryCallInfo = (ViewGroup) mCallInfoContainer.findViewById(R.id.msim_primary_call_info);
            ((ViewGroup) mCallInfoContainer.findViewById(R.id.msim_primary_incoming_call_info)).setVisibility(View.GONE);
        }
        mSubInfo = (TextView) mPrimaryCallInfo.findViewById(R.id.subInfo);
        doUpdate(cm);
        PhoneConstants.State phoneState = cm.getState();
        Call call;
        // Active forground call will be null when an incoming call is ringing,
        // so get the first active ringing call at this time, otherwise get the
        // active forground call.
        if (phoneState == PhoneConstants.State.RINGING) {
            call = cm.getFirstActiveRingingCall();
        } else {
            call = cm.getActiveFgCall();
        }

        // If the call is null, that not show the sub information.
        if (null == call) {
            return;
        }

        Call.State state = call.getState();
        // When the call at dial-up status, calling status, incoming call and can't reply status,
        // hold call state, and so on, update the information of slot.
        /* discard for UX_Dialer
        if (state == Call.State.DIALING || state == Call.State.ALERTING
                || state == Call.State.INCOMING || state == Call.State.WAITING
                || state == Call.State.ACTIVE) {
            if (mSubInfo != null) {
                // Get the subscription from current call object.
                int subscription = call.getPhone().getSubscription();
                String subInfo = getMultiSimName(subscription);
                if (DBG) log( "Setting subinfo: " + subInfo);
                mSubInfo.setText(subInfo);
                mSubInfo.setVisibility(View.VISIBLE);
            }
        } else {
            if (DBG) log(" - call.state: " + call.getState());
        }
        */
    }

    @Override
    protected void cancelTimer(Call call) {
        Call.State state = call.getState();
        mCallTime.cancelTimer();
        if (state == Call.State.DIALING || state == Call.State.ALERTING) {
            //Display subscription info only for incoming calls.
            if (mSubInfo != null) {
                mSubInfo.setVisibility(View.GONE);
            }
        } else if (state == Call.State.INCOMING || state == Call.State.WAITING) {
            if (mSubInfo != null) {
                //Get the subscription from current call object.
                int subscription = call.getPhone().getSubscription();
                subscription++;
                // --msim--: TODO: Better to have a string resource
                String subInfo = "SUB" + subscription;
                if (DBG) Log.v(LOG_TAG, "Setting subinfo: " + subInfo);
                mSubInfo.setText(subInfo);
                mSubInfo.setVisibility(View.VISIBLE);
            }
        } else {
            if (DBG) log(" - call.state: " + call.getState());
        }
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

    protected void dispatchPopulateAccessibilityEvent(AccessibilityEvent event, View view) {
        List<CharSequence> eventText = event.getText();
        int size = eventText.size();
        view.dispatchPopulateAccessibilityEvent(event);
        // if no text added write null to keep relative position
        if (size == eventText.size()) {
            eventText.add(null);
        }
    }

    // Debugging / testing code

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
