/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.os.SystemProperties;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import android.util.Log;

/**
 * List of Network-specific settings screens.
 */
public class GsmUmtsOptions extends PreferenceActivity {

    private PreferenceScreen mButtonAPNExpand;
    private PreferenceScreen mButtonOperatorSelectionExpand;
    private CheckBoxPreference mButtonPrefer2g;

    private static final String BUTTON_APN_EXPAND_KEY = "button_apn_key";
    private static final String BUTTON_OPERATOR_SELECTION_EXPAND_KEY = "button_carrier_sel_key";
    private static final String BUTTON_PREFER_2G_KEY = "button_prefer_2g_key";
    private static final String CSP_TAG = "CSP GSMUMTS";


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.gsm_umts_options);
        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonAPNExpand = (PreferenceScreen) prefSet.findPreference(BUTTON_APN_EXPAND_KEY);
        mButtonOperatorSelectionExpand =
                (PreferenceScreen) prefSet.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
        mButtonPrefer2g = (CheckBoxPreference) prefSet.findPreference(BUTTON_PREFER_2G_KEY);
        if (PhoneFactory.getDefaultPhone().getPhoneType() != Phone.PHONE_TYPE_GSM) {
            mButtonAPNExpand.setEnabled(false);
            mButtonOperatorSelectionExpand.setEnabled(false);
            mButtonPrefer2g.setEnabled(false);
        } else {
            try {
                // persist.cust.tel.adapt is a super flag, if this is set then
                // EF_CSP will be used irrespective of the value of
                // persist.cust.tel.efcsp.plmn. Otherwise EF_CSP will be used if
                // persist.cust.tel.efcsp.plmn is set.

                if (SystemProperties.getBoolean("persist.cust.tel.adapt", false)
                        || SystemProperties.getBoolean("persist.cust.tel.efcsp.plmn", false)) {

                    Log.i(CSP_TAG, "System property for ef_csp is set.");

                    int plmnStatus = PhoneFactory.getDefaultPhone().getCspPlmnStatus();
                    if (plmnStatus == 1) {
                        // This means that in elementary file EF_CSP,
                        // in value added service group, in the service byte,
                        // bit 8, i.e control bit to enable/disable manual PLMN
                        // selection is set
                        Log.v(CSP_TAG, "CSP PLMN bit is set, Enabling Network Operators menu.");
                        mButtonOperatorSelectionExpand.setEnabled(true);
                    } else if (plmnStatus == 0) {
                        Log.v(CSP_TAG, "CSP PLMN bit is not set, Disabling Network Operators menu.");
                        mButtonOperatorSelectionExpand.setEnabled(false);
                    } else {
                        Log.e(CSP_TAG, "Undefined Csp PLMN Status");
                    }
                }
            } catch (Exception e) {
                Log.e(CSP_TAG, "Exception in reading ef_csp system property" + e);
            }
        }
        /*
         * APN editor should always be enabled in the case of SV-LTE with 1x voice.
         */
        if (SystemProperties.getBoolean("ro.config.svlte1x", false)) {
            mButtonAPNExpand.setEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(BUTTON_PREFER_2G_KEY)) {
            return true;
        }
        return false;
    }
}
