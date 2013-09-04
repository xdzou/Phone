/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2011-2013 The Linux Foundation. All rights reserved.
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

import java.util.List;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.content.res.Resources;

import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.codeaurora.telephony.msim.MSimPhoneFactory;
import com.codeaurora.telephony.msim.SubscriptionManager;


import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;

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
    private static final String MCC_CHINA = "460";
    private static final String MCC_MACAU = "455";
    private static final String PROP_DATA_DISABLE_SUB2 = "persist.env.data.disable.sub2";
    private PreferenceActivity mPrefActivity;
    private PreferenceScreen mPrefScreen;
    private int mSubscription = 0;
    private Phone mPhone;

    public GsmUmtsOptions(PreferenceActivity prefActivity, PreferenceScreen prefScreen) {
        this(prefActivity,  prefScreen, 0);
    }

    public GsmUmtsOptions(PreferenceActivity prefActivity,
            PreferenceScreen prefScreen, int subscription) {
        mPrefActivity = prefActivity;
        mPrefScreen = prefScreen;
        mSubscription = subscription;
        // TODO DSDS: Try to move DSDS changes to new file
        mPhone = PhoneGlobals.getInstance().getPhone(mSubscription);
        create();
    }

    protected void create() {
        mPrefActivity.addPreferencesFromResource(R.xml.gsm_umts_options);
        mButtonAPNExpand = (PreferenceScreen) mPrefScreen.findPreference(BUTTON_APN_EXPAND_KEY);
        // disable APN on SUB2
        boolean disableSub2Apn = false;
        if (MSimConstants.SUB2 == mSubscription
                && SystemProperties.getBoolean(PROP_DATA_DISABLE_SUB2,false) ) {
            boolean inChina = true;
            if (SubscriptionManager.getInstance().isSubActive(mSubscription)) {
                String operatorNumberic = MSimPhoneFactory.getPhone(MSimConstants.SUB2)
                        .getServiceState().getOperatorNumeric();
                Log.d(LOG_TAG, " operatorNumber: " + operatorNumberic);
                if (null != operatorNumberic && operatorNumberic.length() >= 3) {
                    String mcc = (String) operatorNumberic.subSequence(0, 3);
                    // China mainland and Macau
                    if (!mcc.equals(MCC_CHINA) && !mcc.equals(MCC_MACAU)) {
                        inChina = false;
                    }
                }
            }
            if (inChina) {
                disableSub2Apn = true;
            }
        }
        if (disableSub2Apn) {
            log("disable sub2 apn");
            mPrefScreen.removePreference(mButtonAPNExpand);
        } else {
            mButtonAPNExpand.getIntent().putExtra(SUBSCRIPTION_KEY,
                    mSubscription);
        }
        mButtonOperatorSelectionExpand =
                (PreferenceScreen) mPrefScreen.findPreference(BUTTON_OPERATOR_SELECTION_EXPAND_KEY);
        mButtonOperatorSelectionExpand.getIntent().putExtra(SUBSCRIPTION_KEY, mSubscription);
        mButtonPrefer2g = (CheckBoxPreference) mPrefScreen.findPreference(BUTTON_PREFER_2G_KEY);
        Use2GOnlyCheckBoxPreference.updatePhone(mPhone);
        enableScreen();
    }

    /**
     * check whether NetworkSetting apk exist in system, if true, replace the
     * intent of the NetworkSetting Activity with the intent of NetworkSetting
     */
    private void enablePlmnIncSearch() {
        if (mButtonOperatorSelectionExpand != null) {
            PackageManager pm = mButtonOperatorSelectionExpand.getContext().getPackageManager();

            // check whether the target handler exist in system
            List<ResolveInfo> list = pm.queryIntentActivities(new Intent(
                    "android.settings.NETWORK_OPERATOR_SETTINGS_ASYNC"), 0);
            int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                ResolveInfo resolveInfo = list.get(i);

                // check is it installed in system.img, exclude the application
                // installed by user
                if ((resolveInfo.activityInfo.applicationInfo.flags &
                        ApplicationInfo.FLAG_SYSTEM) != 0) {

                    // set the target intent
                    mButtonOperatorSelectionExpand.setIntent(new Intent().setClassName(
                            resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name)
                            .putExtra(SUBSCRIPTION_KEY, mSubscription));

                    // get current SPN
                    MSimTelephonyManager mtm = MSimTelephonyManager.getDefault();
                    TelephonyManager tm = TelephonyManager.getDefault();
                    String spn = null;
                    if (mtm.isMultiSimEnabled()) {
                        spn = mtm.getNetworkOperatorName(mSubscription);
                    } else {
                        spn = tm.getNetworkOperatorName();
                    }

                    // set current SPN as the summary of the preference view
                    if (!TextUtils.isEmpty(spn) && !"null".equals(spn)) {
                        mButtonOperatorSelectionExpand.setSummary(spn);
                    } else {
                        mButtonOperatorSelectionExpand.setSummary(R.string.sum_carrier_select);
                    }
                    return;
                }
            }
        }
    }

    public void onResume() {
        updateOperatorSelectionVisibility();
    }

    public void enableScreen() {
        if (mPhone.getPhoneType() != PhoneConstants.PHONE_TYPE_GSM) {
            log("Not a GSM phone, disabling GSM preferences (apn, use2g, select operator)");
            mButtonAPNExpand.setEnabled(false);
            mButtonOperatorSelectionExpand.setEnabled(false);
            mButtonPrefer2g.setEnabled(false);
        } else {
            log("Not a CDMA phone");
            Resources res = mPrefActivity.getResources();

            // Determine which options to display, for GSM these are defaulted
            // are defaulted to true in Phone/res/values/config.xml. But for
            // some operators like verizon they maybe overriden in operator
            // specific resources or device specifc overlays.
            if (!res.getBoolean(R.bool.config_apn_expand)) {
                mPrefScreen.removePreference(mPrefScreen.findPreference(BUTTON_APN_EXPAND_KEY));
            }
            if (!res.getBoolean(R.bool.config_operator_selection_expand)) {
                if (mButtonOperatorSelectionExpand != null) {
                    mPrefScreen.removePreference(mButtonOperatorSelectionExpand);
                    mButtonOperatorSelectionExpand = null;
               }
            }
            if (!res.getBoolean(R.bool.config_prefer_2g)) {
                mPrefScreen.removePreference(mPrefScreen.findPreference(BUTTON_PREFER_2G_KEY));
            }
        }
        updateOperatorSelectionVisibility();
    }

    private void updateOperatorSelectionVisibility() {
        log("updateOperatorSelectionVisibility. mPhone = " + mPhone.getPhoneName());
        Resources res = mPrefActivity.getResources();
        if (mButtonOperatorSelectionExpand == null) {
            android.util.Log.e(LOG_TAG, "mButtonOperatorSelectionExpand is null");
            return;
        }

        enablePlmnIncSearch();
        if (!mPhone.isManualNetSelAllowed()) {
            log("Manual network selection not allowed.Disabling Operator Selection menu.");
            mButtonOperatorSelectionExpand.setEnabled(false);
        } else if (res.getBoolean(R.bool.csp_enabled)) {
            if (mPhone.isCspPlmnEnabled()) {
                log("[CSP] Enabling Operator Selection menu.");
                mButtonOperatorSelectionExpand.setEnabled(true);
            } else {
                log("[CSP] Disabling Operator Selection menu.");
                if (mButtonOperatorSelectionExpand != null) {
                    mPrefScreen.removePreference(mButtonOperatorSelectionExpand);
                    mButtonOperatorSelectionExpand = null;
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
