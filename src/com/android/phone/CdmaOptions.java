/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2009, Code Aurora Forum. All rights reserved.
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
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import android.telephony.TelephonyManager;
import com.android.internal.telephony.Phone;

/**
 * List of Phone-specific settings screens.
 */
public class CdmaOptions extends PreferenceActivity {

    private CdmaRoamingListPreference mButtonCdmaRoam;
    private CdmaSubscriptionListPreference mButtonCdmaSubscription;

    private static final String BUTTON_CDMA_ROAMING_KEY = "cdma_roaming_mode_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "subscription_key";

    private int mSubscription;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.cdma_options);

        PreferenceScreen prefSet = getPreferenceScreen();
        if (TelephonyManager.isDsdsEnabled()) {
            // Get the subscription info passed with the intent. getIntent()
            // fetches the intent that started this activity.
            mSubscription = getIntent().getIntExtra(Settings.SUBSCRIPTION, 0);
        }
        mButtonCdmaRoam =
                (CdmaRoamingListPreference) prefSet.findPreference(BUTTON_CDMA_ROAMING_KEY);
        mButtonCdmaSubscription =
                (CdmaSubscriptionListPreference) prefSet.findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY);
    }

    @Override
    protected void onResume() {
        Phone phone;
        if (TelephonyManager.isDsdsEnabled()) {
            phone = PhoneApp.getPhone(mSubscription);
        } else {
            phone = PhoneApp.getDefaultPhone();
        }
        if (phone.getPhoneType() != Phone.PHONE_TYPE_CDMA) {
            mButtonCdmaRoam.setEnabled(false);
            mButtonCdmaSubscription.setEnabled(false);
        } else {
            mButtonCdmaRoam.setEnabled(true);
            mButtonCdmaSubscription.setEnabled(true);
        }
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(BUTTON_CDMA_ROAMING_KEY)) {
            return true;
        }
        return false;
    }
}
