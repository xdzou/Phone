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
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;

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
    private TextView mElapsedTimeSec;
    private TextView mCityNameSec;
    private TextView mSecondaryPhoneNumber;
    private TextView mSecondaryLabel;
    private TextView mSecondaryPrefixOfLabel;
    private TextView mSecondarySuffixOfLabel;

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

        mCityNameSec = (TextView)mPrimaryCallInfo.findViewById(R.id.cityNameSec);
        mElapsedTimeSec = (TextView) mPrimaryCallInfo.findViewById(R.id.elapsedTimeSec);

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

        if (sDsdaEnabled) {
            if (mInCallSubIcon != null) {
                mInCallSubIcon.setVisibility(View.GONE);
            }
            mCallStateSubIcon.setVisibility(View.GONE);
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

        if (mSecondaryPhoneNumber != null) {
            dispatchPopulateAccessibilityEvent(event, mSecondaryPhoneNumber);
        }
        if (mSecondaryLabel != null) {
            dispatchPopulateAccessibilityEvent(event, mSecondaryLabel);
        }
        return true;
    }

    @Override
    protected boolean isEmergencyNumberWithoutSIMCard(Connection c) {
        return PhoneNumberUtils.isEmergencyNumber(c.getAddress()) &&
                (MSimTelephonyManager.getDefault().getSimState(c.getCall().getPhone()
                        .getSubscription()) == TelephonyManager.SIM_STATE_ABSENT);
    }

    @Override
    protected void updateAlreadyDisconnected(CallManager cm) {
        super.updateAlreadyDisconnected(cm);

        if (sDsdaEnabled) {
            mElapsedTimeSec.setVisibility(View.VISIBLE);
            mElapsedTime.setVisibility(View.GONE);
        }
    }

    @Override
    protected void updateCallStateWidgets(Call call) {
        super.updateCallStateWidgets(call);

        final Call.State state = call.getState();

        // update the elapsed time widget.
        switch (state) {
        case ACTIVE:
        case DISCONNECTING:
            if (sDsdaEnabled) {
                AnimationUtils.Fade.show(mElapsedTimeSec);
            }

            updateElapsedTimeWidget(call);
            break;

        case DISCONNECTED:
            // In the "Call ended" state, leave the mElapsedTime widget
            // visible, but don't touch it (so we continue to see the
            // elapsed time of the call that just ended.)
            // Check visibility to keep possible fade-in animation.
            if (sDsdaEnabled) {
                if (mElapsedTimeSec.getVisibility() != View.VISIBLE) {
                    mElapsedTimeSec.setVisibility(View.VISIBLE);
                }
            }
            break;

        default:
            // Call state here is IDLE, ACTIVE, HOLDING, DIALING, ALERTING,
            // INCOMING, or WAITING.
            // In all of these states, the "elapsed time" is meaningless, so
            // don't show it.
            if (sDsdaEnabled && mElapsedTimeSec != null) {
                AnimationUtils.Fade.hide(mElapsedTimeSec, View.INVISIBLE);
            }
            break;
        }
    }

    @Override
    protected void updateElapsedTimeWidget(long timeElapsed) {
        super.updateElapsedTimeWidget(timeElapsed);

        if (sDsdaEnabled) {
            mElapsedTimeSec.setText(DateUtils.formatElapsedTime(timeElapsed));
            mElapsedTime.setVisibility(View.GONE);
        }
    }

    @Override
    protected void displaySecondaryCallStatus(CallManager cm, Call call) {
        super.displaySecondaryCallStatus(cm, call);

        if ((call == null) || (PhoneGlobals.getInstance().isOtaCallInActiveState())) {
            return;
        }

        Phone phone = call.getPhone();
        Call.State state = call.getState();

        switch (state) {
            case HOLDING:
                if (!PhoneUtils.isConferenceCall(call)) {
                    PhoneUtils.CallerInfoToken infoToken = PhoneUtils.startGetCallerInfo(
                            getContext(), call, this, mSecondaryCallName);

                    String name = PhoneUtils.getCompactNameFromCallerInfo(infoToken.currentInfo,
                            getContext());
                    String number = infoToken.currentInfo.phoneNumber;
                    String label = infoToken.currentInfo.getPhoneLabel(getContext());

                    if (infoToken.currentInfo.name == null) {
                        // Since the real name is null, the name will display the number.
                        // No need display number again.
                        number = null;
                    }

                    updateSecondInfoUi(name, number, label);
                }
                break;

            case ACTIVE:
                if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {

                    List<Connection> connections = call.getConnections();
                    if (connections.size() <= 2) {
                        // This means that the current Mobile Originated call IS the first 3-Way
                        // and hence we display the first callers/party's info here.
                        Connection conn = call.getEarliestConnection();
                        PhoneUtils.CallerInfoToken infoToken = PhoneUtils.startGetCallerInfo(
                                getContext(), conn, this, mSecondaryCallName);

                        // Get the compactName to be displayed, but then check that against
                        // the number presentation value for the call. If it's not an allowed
                        // presentation, then display the appropriate presentation string instead.
                        CallerInfo info = infoToken.currentInfo;

                        String name = PhoneUtils.getCompactNameFromCallerInfo(info, getContext());
                        if (info != null && info.numberPresentation !=
                                PhoneConstants.PRESENTATION_ALLOWED) {
                            name = PhoneUtils.getPresentationString(
                                    getContext(), info.numberPresentation);
                        }

                        String number = info.phoneNumber;
                        String label = info.getPhoneLabel(getContext());

                        if (infoToken.currentInfo.name == null) {
                            // Since the real name is null, the name area will display the number.
                            // No need display number again.
                            number = null;
                        }

                        updateSecondInfoUi(name, number, label);
                    }
                }
                break;
        }
    }

    @Override
    protected void showSecondaryCallInfo() {
        super.showSecondaryCallInfo();

        if (mSecondaryPhoneNumber == null) {
            mSecondaryPhoneNumber = (TextView) findViewById(R.id.secondaryPhoneNumber);
        }

        if (mSecondaryLabel == null) {
            mSecondaryLabel = (TextView) findViewById(R.id.secondaryLabel);
        }

        if (mSecondaryPrefixOfLabel == null) {
            mSecondaryPrefixOfLabel = (TextView) findViewById(R.id.prefix_of_label_second);
        }

        if (mSecondarySuffixOfLabel == null) {
            mSecondarySuffixOfLabel = (TextView) findViewById(R.id.suffix_of_label_second);
        }
    }

    @Override
    protected void updateGenericInfoUi() {
        super.updateGenericInfoUi();

        if (sDsdaEnabled) {
            if (mCityNameSec != null) {
                mCityNameSec.setVisibility(View.GONE);
            }

            if (mCityName != null) {
                mCityName.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void updateInfoUi(String displayName, String displayNumber,
            String label, String cityName) {
        super.updateInfoUi(displayName, displayNumber, label, cityName);

        if (sDsdaEnabled) {
            if (mCityNameSec != null) {
                if (TextUtils.isEmpty(cityName)) {
                    mCityNameSec.setVisibility(View.GONE);
                } else {
                    mCityNameSec.setText(cityName);
                    mCityNameSec.setVisibility(View.VISIBLE);
                }
            }

            if (mCityName != null) {
                mCityName.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Updates the second info portion of the call card with passed in values.
     */
    protected void updateSecondInfoUi(String displayName, String displayNumber,
            String label) {
        mSecondaryCallName.setText(displayName);
        mSecondaryCallName.setVisibility(View.VISIBLE);

        if (TextUtils.isEmpty(displayNumber)) {
            mSecondaryPhoneNumber.setVisibility(View.GONE);
            // We have a real second phone number, so make it always LTR
            mSecondaryCallName.setTextDirection(View.TEXT_DIRECTION_LTR);
        } else {
            mSecondaryPhoneNumber.setText(displayNumber);
            mSecondaryPhoneNumber.setVisibility(View.VISIBLE);
            // We have a real second phone number, so make it always LTR
            mSecondaryPhoneNumber.setTextDirection(View.TEXT_DIRECTION_LTR);
        }

        if (TextUtils.isEmpty(label)) {
            mSecondaryLabel.setVisibility(View.GONE);
            if (mSecondaryPrefixOfLabel != null) {
                mSecondaryPrefixOfLabel.setVisibility(View.GONE);
            }
            if (mSecondarySuffixOfLabel != null) {
                mSecondarySuffixOfLabel.setVisibility(View.GONE);
            }
        } else {
            mSecondaryLabel.setText(label);
            mSecondaryLabel.setVisibility(View.VISIBLE);

            if (mSecondaryPrefixOfLabel != null) {
                mSecondaryPrefixOfLabel.setText(getResources()
                        .getString(R.string.prefix_of_label));
                mSecondaryPrefixOfLabel.setVisibility(View.VISIBLE);
            }
            if (mSecondarySuffixOfLabel != null) {
                mSecondarySuffixOfLabel.setText(getResources()
                        .getString(R.string.suffix_of_label));
                mSecondarySuffixOfLabel.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void updateDisplayForConference(Call call) {
        super.updateDisplayForConference(call);

        if (sDsdaEnabled && (mCityNameSec != null)) {
            mCityNameSec.setVisibility(View.GONE);
        }

        updateCallTypeLabel(call);
    }

    @Override
    public void clear() {
        super.clear();

        if (mElapsedTimeSec != null) {
            mElapsedTimeSec.setVisibility(View.GONE);
            mElapsedTimeSec.setText(null);
        }
    }

    // Debugging / testing code

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
