/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (c) 2011-2012 Code Aurora Forum. All rights reserved.
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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.MSimTelephonyManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;
import static com.android.internal.telephony.MSimConstants.SUB1;
import static com.android.internal.telephony.MSimConstants.SUB2;
import static com.android.internal.telephony.MSimConstants.SUB3;

/**
 * SIM Address Book UI for the Phone app.
 */
public class MSimContacts extends SimContacts {
    private static final String LOG_TAG = "MSimContacts";

    protected int mSubscription = 0;
    private int IMPORT_FROM_ALL = 3;

    @Override
    protected Uri resolveIntent() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        mSubscription  = extras.getInt(SUBSCRIPTION_KEY);
        if (mSubscription == SUB1) {
            intent.setData(Uri.parse("content://iccmsim/adn"));
        } else if (mSubscription == SUB2) {
            intent.setData(Uri.parse("content://iccmsim/adn_sub2"));
        } else if (mSubscription == SUB3) {
            intent.setData(Uri.parse("content://iccmsim/adn_sub3"));
        } else if (mSubscription == IMPORT_FROM_ALL) {
            intent.setData(Uri.parse("content://iccmsim/adn_all"));
        } else {
            Log.d(TAG, "resolveIntent:Invalid subcription");
        }

        if (Intent.ACTION_PICK.equals(intent.getAction())) {
            // "index" is 1-based
            mInitialSelection = intent.getIntExtra("index", 0) - 1;
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            mInitialSelection = 0;
        }
        return intent.getData();
    }

    @Override
    protected Uri getUri() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        mSubscription  = extras.getInt(SUBSCRIPTION_KEY);
        if (mSubscription == SUB1) {
            return Uri.parse("content://iccmsim/adn");
        } else if (mSubscription == SUB2) {
            return Uri.parse("content://iccmsim/adn_sub2");
        } else if (mSubscription == SUB3) {
            return Uri.parse("content://iccmsim/adn_sub3");
        } else {
            Log.d(TAG, "Invalid subcription");
            return null;
        }
    }

    protected boolean isImportFromAllOption() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        mSubscription  = extras.getInt(SUBSCRIPTION_KEY);
        if (mSubscription == IMPORT_FROM_ALL) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_IMPORT_ALL, 0, R.string.importAllSimEntries);
        if (!isImportFromAllOption()) {
            menu.add(0, MENU_DELETE_ALL, 0, R.string.deleteAllSimEntries);
            menu.add(0, MENU_ADD_CONTACT, 0, R.string.addSimEntries);
        } else {
            Log.e(LOG_TAG, "Only import is supported");
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            AdapterView.AdapterContextMenuInfo itemInfo =
                    (AdapterView.AdapterContextMenuInfo) menuInfo;
            TextView textView = (TextView) itemInfo.targetView.findViewById(android.R.id.text1);
            if (textView != null) {
                menu.setHeaderTitle(textView.getText());
            }
            menu.add(0, MENU_IMPORT_ONE, 0, R.string.importSimEntry);
            if (!isImportFromAllOption()) {
                menu.add(0, MENU_EDIT_CONTACT, 0, R.string.editContact);
                menu.add(0, MENU_SMS, 0, R.string.sendSms);
                menu.add(0, MENU_DIAL, 0, R.string.dial);
                menu.add(0, MENU_DELETE, 0, R.string.delete);
            } else {
                Log.e(LOG_TAG, "Only import is supported");
            }
        }
    }

}
