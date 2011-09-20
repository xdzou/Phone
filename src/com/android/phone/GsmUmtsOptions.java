/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2011, Code Aurora Forum. All rights reserved
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

import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyProperties;

/**
 * List of Network-specific settings screens.
 */
public class GsmUmtsOptions {
    private static final String LOG_TAG = "GsmUmtsOptions";

    private PreferenceScreen mButtonAPNExpand;
    private PreferenceScreen mButtonOperatorSelectionExpand;
    private CheckBoxPreference mButtonPrefer2g;

    private static final String BUTTON_APN_EXPAND_KEY = "button_apn_key";
    private static final String BUTTON_OPERATOR_SELECTION_EXPAND_KEY = "button_carrier_sel_key";
    private static final String BUTTON_PREFER_2G_KEY = "button_prefer_2g_key";

    private PreferenceActivity mPrefActivity;
    private PreferenceScreen mPrefScreen;

    private int mSubscription = 0;

    private Phone mPhone;

    public GsmUmtsOptions(PreferenceActivity prefActivity, PreferenceScreen prefScreen, int subscription) {
        mPrefActivity = prefActivity;
        mPrefScreen = prefScreen;
        mSubscription = subscription;
        mPhone = PhoneApp.getPhone(mSubscription);
        create();
    }

    protected void create() {
        mPrefActivity.addPreferencesFromResource(R.xml.gsm_umts_options);
        mButtonAPNExpand = (PreferenceScreen) mPrefScreen.findPreference(BUTTON_APN_EXPAND_KEY);
        mButtonAPNExpand.getIntent().putExtra(Settings.SUBSCRIPTION, mSubscription);
        mButtonOperatorSelectionExpand =
                (PreferenceScreen) mPrefScreen.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
        mButtonOperatorSelectionExpand.getIntent().putExtra(Settings.SUBSCRIPTION, mSubscription);
        mButtonPrefer2g = (CheckBoxPreference) mPrefScreen.findPreference(BUTTON_PREFER_2G_KEY);
        Use2GOnlyCheckBoxPreference.updatePhone(mPhone);
        enableScreen();
    }

    public void enableScreen() {
        if (mPhone.getPhoneType() != Phone.PHONE_TYPE_GSM) {
            log("Not a GSM phone");
            mButtonOperatorSelectionExpand.setEnabled(false);
            mButtonPrefer2g.setEnabled(false);
        } else if (mPrefActivity.getResources().getBoolean(R.bool.csp_enabled)) {
            if (mPhone.isCspPlmnEnabled()) {
                log("[CSP] Enabling Operator Selection menu.");
                mButtonOperatorSelectionExpand.setEnabled(true);
            } else {
                log("[CSP] Disabling Operator Selection menu.");
                mButtonOperatorSelectionExpand =
                        (PreferenceScreen) mPrefScreen.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
                if (mButtonOperatorSelectionExpand != null) {
                    mPrefScreen.removePreference(mPrefScreen
                          .findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY));
                } else {
                    log("Operator Selection Menu already removed!");
                }
            }
        }
    }

    public boolean preferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(BUTTON_PREFER_2G_KEY)) {
            log("preferenceTreeClick: return true");
            return true;
        }
        log("preferenceTreeClick: return false");
        return false;
    }

    protected void log(String s) {
        android.util.Log.d(LOG_TAG, s);
    }
}
