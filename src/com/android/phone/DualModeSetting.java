/*
 * Copyright (c) 2010, Code Aurora Forum. All rights reserved.
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
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class DualModeSetting extends PreferenceActivity {

    private static final String LOG_TAG = "DualModeSetting";
    private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);

    private static final String KEY_SUBSCRIPTION_0 = "button_sub_id_00";
    private static final String KEY_SUBSCRIPTION_1 = "button_sub_id_01";
    public static final String SUBSCRIPTION_ID = "SUBSCRIPTION_ID";

    private PreferenceScreen subscriptionPref0, subscriptionPref1;

    @Override
    public void onPause() {
        super.onPause();
    }

    /*
     * Activity class methods
     */

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (DBG) log("Creating activity");
        addPreferencesFromResource(R.xml.dual_mode_setting);

        // get buttons
        PreferenceScreen prefSet = getPreferenceScreen();
         // setting selected subscription
        subscriptionPref0 = (PreferenceScreen) findPreference(KEY_SUBSCRIPTION_0);
        subscriptionPref1 = (PreferenceScreen) findPreference(KEY_SUBSCRIPTION_1);

        subscriptionPref0.getIntent().putExtra(SUBSCRIPTION_ID, 0);
        subscriptionPref1.getIntent().putExtra(SUBSCRIPTION_ID, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
