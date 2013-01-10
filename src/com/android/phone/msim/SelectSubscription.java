/*
 * Copyright (c) 2010, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.phone;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.MSimTelephonyManager;
import android.util.Log;

import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;

public class SelectSubscription extends PreferenceActivity {

    private static final String LOG_TAG = "SelectSubscription";
    private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);

    private static final String PREF_PARENT_KEY = "parent_pref";
    public static final String PACKAGE = "PACKAGE";
    public static final String TARGET_CLASS = "TARGET_CLASS";
    private int[] resourceIndex = {R.string.sub1, R.string.sub2, R.string.sub3};
    private int[] summaryIndex = {R.string.sub1_summary, R.string.sub2_summary,
            R.string.sub3_summary};

    private Preference subscriptionPref;


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
        addPreferencesFromResource(R.xml.multi_sim_setting);

        PreferenceScreen prefParent = (PreferenceScreen) getPreferenceScreen().
                findPreference(PREF_PARENT_KEY);

        Intent intent =  getIntent();
        String pkg = intent.getStringExtra(PACKAGE);
        String targetClass = intent.getStringExtra(TARGET_CLASS);

        int numPhones = MSimTelephonyManager.getDefault().getPhoneCount();
        Intent selectIntent;


        for (int i = 0; i < numPhones; i++) {
            selectIntent = new Intent();
            subscriptionPref = new Preference(getApplicationContext());
            // Set the package and target class.
            selectIntent.setClassName(pkg, targetClass);
            selectIntent.setAction(Intent.ACTION_MAIN);
            selectIntent.putExtra(SUBSCRIPTION_KEY, i);
            subscriptionPref.setIntent(selectIntent);
            subscriptionPref.setTitle(resourceIndex[i]);
            subscriptionPref.setSummary(summaryIndex[i]);
            subscriptionPref.setOnPreferenceClickListener(mPreferenceClickListener);
            prefParent.addPreference(subscriptionPref);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    Preference.OnPreferenceClickListener mPreferenceClickListener =
            new Preference.OnPreferenceClickListener() {
       public boolean onPreferenceClick(Preference preference) {
           startActivity(preference.getIntent());
           return true;
       }
    };
}
