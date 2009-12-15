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

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

/**
 * List of Phone-specific settings screens.
 */
public class CdmaOptions extends PreferenceActivity {

    private CdmaRoamingListPreference mButtonCdmaRoam;
    private CdmaSubscriptionListPreference mButtonCdmaSubscription;

    private static final String BUTTON_CDMA_ROAMING_KEY = "cdma_roaming_mode_key";
    private static final String BUTTON_CDMA_SUBSCRIPTION_KEY = "subscription_key";
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.cdma_options);

        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonCdmaRoam =
                (CdmaRoamingListPreference) prefSet.findPreference(BUTTON_CDMA_ROAMING_KEY);
        mButtonCdmaSubscription =
                (CdmaSubscriptionListPreference) prefSet.findPreference(BUTTON_CDMA_SUBSCRIPTION_KEY);
    }

    @Override
    protected void onResume() {
        if (PhoneFactory.getDefaultPhone().getPhoneType() != Phone.PHONE_TYPE_CDMA) {
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
