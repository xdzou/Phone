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

import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;

import com.android.phone.CallNotifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.MSimTelephonyManager;
import android.telephony.PhoneStateListener;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Phone app module that listens for phone state changes and various other
 * events from the telephony layer, and triggers any resulting UI behavior
 * (like starting the Ringer and Incoming Call UI, playing in-call tones,
 * updating notifications, writing call log entries, etc.)
 */
public class MSimCallNotifier extends CallNotifier {
    private static final String LOG_TAG = "MSimCallNotifier";
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final String XDIVERT_STATUS = "xdivert_status_key";

    /**
     * Initialize the singleton CallNotifier instance.
     * This is only done once, at startup, from PhoneApp.onCreate().
     */
    /* package */ static CallNotifier init(PhoneGlobals app, Phone phone, Ringer ringer,
                                           CallLogAsync callLog) {
        synchronized (MSimCallNotifier.class) {
            if (sInstance == null) {
                sInstance = new MSimCallNotifier(app, phone, ringer, callLog);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return (MSimCallNotifier) sInstance;
        }
    }

    /** Private constructor; @see init() */
    protected MSimCallNotifier(PhoneGlobals app, Phone phone, Ringer ringer, CallLogAsync callLog) {
        super(app, phone, ringer, callLog);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case PHONE_MWI_CHANGED:
                Phone phone = (Phone)msg.obj;
                onMwiChanged(mApplication.phone.getMessageWaitingIndicator(), phone);
                break;
            default:
                 super.handleMessage(msg);
        }
    }

    @Override
    protected void listen() {
        TelephonyManager telephonyManager = (TelephonyManager)mApplication.
                getSystemService(Context.TELEPHONY_SERVICE);
        for (int i = 0; i < MSimTelephonyManager.getDefault().getPhoneCount(); i++) {
            telephonyManager.listen(getPhoneStateListener(i),
                    PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
                    | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR);
        }
    }

    private void onMwiChanged(boolean visible, Phone phone) {
        if (VDBG) log("onMwiChanged(): " + visible);

        // "Voicemail" is meaningless on non-voice-capable devices,
        // so ignore MWI events.
        if (!PhoneGlobals.sVoiceCapable) {
            // ...but still log a warning, since we shouldn't have gotten this
            // event in the first place!
            // (PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR events
            // *should* be blocked at the telephony layer on non-voice-capable
            // capable devices.)
            Log.w(LOG_TAG, "Got onMwiChanged() on non-voice-capable device! Ignoring...");
            return;
        }

        ((MSimNotificationMgr)mApplication.notificationMgr).updateMwi(visible, phone);
    }

    /**
     * Posts a delayed PHONE_MWI_CHANGED event, to schedule a "retry" for a
     * failed NotificationMgr.updateMwi() call.
     */
    /* package */
    void sendMwiChangedDelayed(long delayMillis, Phone phone) {
        Message message = Message.obtain(this, PHONE_MWI_CHANGED, phone);
        sendMessageDelayed(message, delayMillis);
    }

    protected void onCfiChanged(boolean visible, int subscription) {
        if (VDBG) log("onCfiChanged(): " + visible + " sub: " + subscription);
        ((MSimNotificationMgr)mApplication.notificationMgr).updateCfi(visible, subscription);
    }

    protected void onXDivertChanged(boolean visible) {
        if (VDBG) log("onXDivertChanged(): " + visible);
        ((MSimNotificationMgr)mApplication.notificationMgr).updateXDivert(visible);
    }

    // Gets the XDivert Status from shared preference.
    protected boolean getXDivertStatus() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                mApplication);
        boolean status = sp.getBoolean(XDIVERT_STATUS, false);
        Log.d(LOG_TAG, "getXDivertStatus status = " + status);
        return status;
    }

    // Sets the XDivert Status to shared preference.
    protected void setXDivertStatus(boolean status) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                mApplication);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(XDIVERT_STATUS, status);
        editor.apply();
    }

    private PhoneStateListener getPhoneStateListener(int sub) {
        Log.d(LOG_TAG, "getPhoneStateListener: SUBSCRIPTION == " + sub);

        PhoneStateListener phoneStateListener = new PhoneStateListener(sub) {
            @Override
            public void onMessageWaitingIndicatorChanged(boolean mwi) {
                // mSubscription is a data member of PhoneStateListener class.
                // Each subscription is associated with one PhoneStateListener.
                onMwiChanged(mwi, PhoneGlobals.getInstance().getPhone(mSubscription));
            }

            @Override
            public void onCallForwardingIndicatorChanged(boolean cfi) {
                onCfiChanged(cfi, mSubscription);
            }
        };
        return phoneStateListener;
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
