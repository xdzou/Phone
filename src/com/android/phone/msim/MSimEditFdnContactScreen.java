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

import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.MSimTelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.MSimConstants;
import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;
import com.android.internal.telephony.TelephonyIntents;


/**
 * Activity to let the user add or edit an FDN contact.
 */
public class MSimEditFdnContactScreen extends EditFdnContactScreen {
    private static final String LOG_TAG = "MSimEditFdnContactScreen";
    private static final boolean DBG = false;

    private static int mSubscription = 0;

    private BroadcastReceiver mSimStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null
                    && intent.getAction().equals(
                            TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                String stateExtra = intent
                        .getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                // Obtain the subscription info from intent.
                int sub = intent.getIntExtra(MSimConstants.SUBSCRIPTION_KEY,
                        MSimConstants.DEFAULT_SUBSCRIPTION);
                // Check is the intent is for this subscription
                if (sub != mSubscription) {
                    return;
                }

                if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)) {
                    String absentReason = intent
                            .getStringExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON);
                    if (!IccCardConstants.INTENT_VALUE_ABSENT_ON_PERM_DISABLED
                            .equals(absentReason)) {
                        Toast.makeText(context, R.string.fdn_service_unavailable,
                                Toast.LENGTH_SHORT).show();

                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(
                TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        registerReceiver(mSimStateChangedReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mSimStateChangedReceiver);
    }

    @Override
    protected void resolveIntent() {
        Intent intent = getIntent();

        mName =  intent.getStringExtra(INTENT_EXTRA_NAME);
        mNumber =  intent.getStringExtra(INTENT_EXTRA_NUMBER);
        mSubscription = getIntent().getIntExtra(SUBSCRIPTION_KEY, 0);
        mAddContact = TextUtils.isEmpty(mNumber);
    }

    @Override
    protected Uri getContentURI() {
        String[] fdn = {"fdn", "fdn_sub2", "fdn_sub3"};

        if (mSubscription < MSimTelephonyManager.getDefault().getPhoneCount()) {
            return Uri.parse("content://iccmsim/" + fdn[mSubscription]);
        } else {
            Log.e(LOG_TAG, "Error received invalid sub =" + mSubscription);
            return null;
        }
    }

    @Override
    protected void addContact() {
        if (DBG) log("addContact");

        if (!isValidNumber(getNumberFromTextField())) {
            handleResult(false, true);
            return;
        }

        Uri uri = getContentURI();

        ContentValues bundle = new ContentValues(4);
        bundle.put("tag", getNameFromTextField());
        bundle.put("number", getNumberFromTextField());
        bundle.put("pin2", mPin2);
        bundle.put(SUBSCRIPTION_KEY, mSubscription);

        mQueryHandler = new QueryHandler(getContentResolver());
        mQueryHandler.startInsert(0, null, uri, bundle);
        displayProgress(true);
        showStatus(getResources().getText(R.string.adding_fdn_contact));
    }

    @Override
    protected void updateContact() {
        if (DBG) log("updateContact");

        if (!isValidNumber(getNumberFromTextField())) {
            handleResult(false, true);
            return;
        }
        Uri uri = getContentURI();

        ContentValues bundle = new ContentValues();
        bundle.put("tag", mName);
        bundle.put("number", mNumber);
        bundle.put("newTag", getNameFromTextField());
        bundle.put("newNumber", getNumberFromTextField());
        bundle.put("pin2", mPin2);
        bundle.put(SUBSCRIPTION_KEY, mSubscription);

        mQueryHandler = new QueryHandler(getContentResolver());
        mQueryHandler.startUpdate(0, null, uri, bundle, null, null);
        displayProgress(true);
        showStatus(getResources().getText(R.string.updating_fdn_contact));
    }

    /**
     * Handle the delete command, based upon the state of the Activity.
     */
    @Override
    protected void deleteSelected() {
        // delete ONLY if this is NOT a new contact.
        if (!mAddContact) {
            Intent intent = new Intent();
            intent.setClass(this, MSimDeleteFdnContactScreen.class);
            intent.putExtra(INTENT_EXTRA_NAME, mName);
            intent.putExtra(INTENT_EXTRA_NUMBER, mNumber);
            intent.putExtra(SUBSCRIPTION_KEY, mSubscription);
            startActivity(intent);
        }
        finish();
    }

    @Override
    protected void log(String msg) {
        Log.d(LOG_TAG, "[MSimEditFdnContact] " + msg);
    }
}
