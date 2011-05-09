/*
 * Copyright (c) 2011, Code Aurora Forum. All rights reserved.
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.Handler;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.view.KeyEvent;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;


import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ProxyManager;
import com.android.internal.telephony.ProxyManager.SubscriptionData;
import com.android.internal.telephony.ProxyManager.Subscription;

import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.ServiceState;


public class MultiSimDialerActivity extends Activity {
    private static final String TAG = "MultiSimDialerActivity";
    private static final boolean DBG = true;

    private Context mContext;
    private String mCallNumber;
    private String mNumber;
    private AlertDialog mAlertDialog = null;
    private TextView mTextNumber;
    private Intent mIntent;
    private int mPhoneCount = 0;

    public static final String PHONE_SUBSCRIPTION = "Subscription";
    public static final int INVALID_SUB = 99;
    public static final int SUB1 = 0;
    public static final int SUB2 = 1;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getApplicationContext();
        mCallNumber = getResources().getString(R.string.call_number);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPhoneCount = TelephonyManager.getPhoneCount();
        mIntent = getIntent();
        if (DBG) Log.v(TAG, "Intent = " + mIntent);

        mNumber = mIntent.getExtras().getString("phoneNumber");
        if (DBG) Log.v(TAG, "mNumber " + mNumber);
        if (mNumber != null) {
            mNumber = PhoneNumberUtils.convertKeypadLettersToDigits(mNumber);
            mNumber = PhoneNumberUtils.stripSeparators(mNumber);
        }

        Phone phone = null;
        boolean phoneInCall = false;
        //checking if any of the phones are in use
        for (int i = 0; i < mPhoneCount; i++) {
             phone = PhoneFactory.getPhone(i);
             boolean inCall = isInCall(phone);
             if ((phone != null) && (inCall)) {
                 phoneInCall = true;
                 break;
             }
        }
        if (phoneInCall) {
            if (DBG) Log.v(TAG, "subs [" + phone.getSubscription() + "] is in call");
            // use the sub which is already in call
            startOutgoingCall(phone.getSubscription());
        } else {
            if (DBG) Log.v(TAG, "launch dsdsdialer");
            // if none in use, launch the MultiSimDialer
            launchMSDialer();
        }
        Log.d(TAG, "end of onResume()");
    }

    protected void onPause() {
        super.onPause();
        if(DBG) Log.v(TAG, "onPause : " + mIntent);
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }

   private int getSubscriptionForEmergencyCall(){
       Log.d(TAG,"emergency call, getVoiceSubscriptionInService");
       int sub = PhoneApp.getInstance().getVoiceSubscriptionInService();
       return sub;
    }

    private void launchMSDialer() {
        boolean isEmergency = PhoneNumberUtils.isEmergencyNumber(mNumber);
        if (isEmergency) {
            Log.d(TAG,"emergency call");
            startOutgoingCall(getSubscriptionForEmergencyCall());
            return;
        }

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialer_ms,(ViewGroup) findViewById(R.id.layout_root));

        AlertDialog.Builder builder = new AlertDialog.Builder(MultiSimDialerActivity.this);
        builder.setView(layout);
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                Log.d(TAG, "key code is :" + keyCode);
                switch (keyCode) {
                case KeyEvent.KEYCODE_BACK: {
                    mAlertDialog.dismiss();
                    startOutgoingCall(INVALID_SUB);
                    return true;
                    }
                case KeyEvent.KEYCODE_CALL: {
                    Log.d(TAG, "event is" + event.getAction());
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        return true;
                    } else {
                        mAlertDialog.dismiss();
                        startOutgoingCall(PhoneFactory.getVoiceSubscription());
                        return true;
                    }
                    }
                case KeyEvent.KEYCODE_SEARCH:
                    return true;
                default:
                    return false;
                }
            }
        });

        mAlertDialog = builder.create();

        mTextNumber = (TextView)layout.findViewById(R.id.CallNumber);
        String vm =  mIntent.getExtras().getString("voicemail");
        if ((vm != null) && (vm.equals("voicemail"))) {
            mTextNumber.setText(mCallNumber + "VoiceMail" );
            Log.d(TAG, "its voicemail!!!");
        } else {
            mTextNumber.setText(mCallNumber + mNumber);
        }

        Button callCancel = (Button)layout.findViewById(R.id.callcancel);
        callCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mAlertDialog.dismiss();
                startOutgoingCall(INVALID_SUB);
            }
        });

        Button[] callButton = new Button[mPhoneCount];
        int[] callMark = {R.id.callmark1, R.id.callmark2};
        int[] subString = {R.string.sub_1, R.string.sub_2};
        int index = 0;
        for (index = 0; index < mPhoneCount; index++) {
            callButton[index] =  (Button) layout.findViewById(callMark[index]);
            callButton[index].setText(subString[index]);
            callButton[index].setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mAlertDialog.dismiss();
                    switch (v.getId()) {
                    case R.id.callmark1:
                        startOutgoingCall(SUB1);
                        break;
                    case R.id.callmark2:
                        startOutgoingCall(SUB2);
                        break;
                    }
                }
            });
        }


        if (SUB1 == PhoneFactory.getVoiceSubscription()) {
            callButton[SUB1].setBackgroundResource(R.drawable.highlight_btn_call);
        } else {
            callButton[SUB2].setBackgroundResource(R.drawable.highlight_btn_call);
        }

        mAlertDialog.show();
    }

    boolean isInCall(Phone phone) {
        if (phone != null) {
            if ((phone.getForegroundCall().getState().isAlive()) ||
                   (phone.getBackgroundCall().getState().isAlive()) ||
                   (phone.getRingingCall().getState().isAlive()))
                return true;
        }
        return false;
    }

    private void startOutgoingCall(int subscription) {
         mIntent.putExtra(PHONE_SUBSCRIPTION, subscription);
         mIntent.setClass(MultiSimDialerActivity.this, OutgoingCallBroadcaster.class);
         if (DBG) Log.v(TAG, "startOutgoingCall for sub " +subscription + " from intent: "+ mIntent);
         if (subscription < mPhoneCount) {
             setResult(RESULT_OK, mIntent);
         } else {
             setResult(RESULT_CANCELED, mIntent);
             Log.d(TAG, "call cancelled");
         }
         finish();
    }
}
