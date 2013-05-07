/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (c) 2011-2012 The Linux Foundation. All rights reserved.
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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ThrottleManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.MSimTelephonyManager;
import android.telephony.TelephonyManager;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.qualcomm.internal.telephony.SubscriptionManager;
import com.qualcomm.internal.telephony.CardSubscriptionManager;
import com.qualcomm.internal.telephony.SubscriptionData;
import com.android.phone.CdmaOptions;
import com.android.phone.DataUsageListener;
import com.android.phone.GsmUmtsOptions;
import com.android.phone.R;

import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;

import com.qrd.plugin.feature_query.FeatureQuery;

/**
 * "Mobile network settings" screen.  This preference screen lets you
 * enable/disable mobile data, and control data roaming and other
 * network-specific mobile data features.  It's used on non-voice-capable
 * tablets as well as regular phone devices.
 *
 * Note that this PreferenceActivity is part of the phone app, even though
 * you reach it from the "Wireless & Networks" section of the main
 * Settings app.  It's not part of the "Call settings" hierarchy that's
 * available from the Phone app (see CallFeaturesSetting for that.)
 */
public class MSimMobileNetworkSubSettings extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener{

    // debug data
    private static final String LOG_TAG = "MSimMobileNetworkSubSettings";
    private static final boolean DBG = true;
    public static final int REQUEST_CODE_EXIT_ECM = 17;

    //String keys for preference lookup
    private static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";

    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;

    //Information about logical "up" Activity
    private static final String UP_ACTIVITY_PACKAGE = "com.android.settings";
    private static final String UP_ACTIVITY_CLASS =
            "com.android.settings.Settings$WirelessSettingsActivity";

    //UI objects
    private ListPreference mButtonPreferredNetworkMode;

    private static final String iface = "rmnet0"; //TODO: this will go away

    protected Phone mPhone;
    protected BroadcastReceiver mReceiver;
    private MyHandler mHandler;
    private boolean mOkClicked;
    private int mSubscription;

    //GsmUmts options and Cdma options
    GsmUmtsOptions mGsmUmtsOptions;
    CdmaOptions mCdmaOptions;

    private Preference mClickedPreference;

    //String keys for preference lookup
    private static final String BUTTON_MANAGE_SUB_KEY = "button_settings_manage_sub";
    private static final String BUTTON_DATA_USAGE_KEY = "button_data_usage_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    private static final String BUTTON_CDMA_LTE_DATA_SERVICE_KEY = "cdma_lte_data_service_key";
    private static final String BUTTON_DATA_ENABLE_KEY = "button_data_enabled_key";

    //UI objects
    private CheckBoxPreference mButtonDataRoam;
    private Preference mLteDataServicePref;

    private Preference mButtonDataUsage;
    private DataUsageListener mDataUsageListener;

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /** TODO: Refactor and get rid of the if's using subclasses */
        if (mGsmUmtsOptions != null &&
                mGsmUmtsOptions.preferenceTreeClick(preference) == true) {
            //update the preferred network mode summary
            mPhone.getPreferredNetworkType(mHandler.obtainMessage(
                    MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE));
            return true;
        } else if (mCdmaOptions != null &&
                   mCdmaOptions.preferenceTreeClick(preference) == true) {
            if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {

                mClickedPreference = preference;

                // In ECM mode launch ECM app dialog
                startActivityForResult(
                    new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                    REQUEST_CODE_EXIT_ECM);
            }
            return true;
        } else if (preference == mButtonPreferredNetworkMode) {
            //displays the value taken from the Settings.System
            int settingsNetworkMode = getPreferredNetworkMode();
            if (FeatureQuery.FEATURE_PHONE_GLOBAL_MODE) {
                settingsNetworkMode = transformNetworkMode(settingsNetworkMode);
            }
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            return true;
        } else if (preference == mButtonDataRoam) {
            if (DBG) log("onPreferenceTreeClick: preference = mButtonDataRoam");

            //normally called on the toggle click
            if (mButtonDataRoam.isChecked()) {
                // First confirm with a warning dialog about charges
                mOkClicked = false;
                new AlertDialog.Builder(this).setMessage(
                        getResources().getString(R.string.roaming_warning))
                        .setTitle(android.R.string.dialog_alert_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which) {
                                        mPhone.setDataRoamingEnabled(true);
                                        mOkClicked = true;
                                    }
                                })
                        .setNegativeButton(android.R.string.no,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which) {
                                        // Reset the toggle
                                        mButtonDataRoam.setChecked(false);
                                    }
                                })
                        .show()
                        .setOnDismissListener(
                                new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(
                                            DialogInterface dialog) {
                                        // Assuming that onClick gets called
                                        // first
                                        if (!mOkClicked) {
                                            mButtonDataRoam
                                                    .setChecked(false);
                                        }
                                    }
                                });
            } else {
                mPhone.setDataRoamingEnabled(false);
            }
            return true;
        }else if (preference == mLteDataServicePref) {
            String tmpl = android.provider.Settings.Secure.getString(getContentResolver(),
                        android.provider.Settings.Secure.SETUP_PREPAID_DATA_SERVICE_URL);
            if (!TextUtils.isEmpty(tmpl)) {
                TelephonyManager tm = (TelephonyManager) getSystemService(
                        Context.TELEPHONY_SERVICE);
                String imsi = tm.getSubscriberId();
                if (imsi == null) {
                    imsi = "";
                }
                final String url = TextUtils.isEmpty(tmpl) ? null
                        : TextUtils.expandTemplate(tmpl, imsi).toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } else {
                android.util.Log.e(LOG_TAG, "Missing SETUP_PREPAID_DATA_SERVICE_URL");
            }
            return true;
        } else {
            // if the button is anything but the simple toggle preference,
            // we'll need to disable all preferences to reject all click
            // events until the sub-activity's UI comes up.
            preferenceScreen.setEnabled(false);
            // Let the intents be launched by the Preference manager
            return false;
        }
    }

    /**
     * Receiver for misc intent broadcasts the Phone app cares about.
     */
    protected class PhoneAppBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("action :" + action );
            if (action.equals(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED)) {
                int phoneType = mPhone.getPhoneType();
                log("phoneType is :" + phoneType );
                if (phoneType == PhoneConstants.PHONE_TYPE_GSM && mCdmaOptions!=null && mGsmUmtsOptions == null) {
                    mCdmaOptions.removeCDMAOptions();
                    mCdmaOptions = null;
                    mGsmUmtsOptions = new GsmUmtsOptions(MSimMobileNetworkSubSettings.this, getPreferenceScreen(), mSubscription);
                } else if (phoneType == PhoneConstants.PHONE_TYPE_CDMA && mGsmUmtsOptions!=null && mCdmaOptions == null) {
                    mGsmUmtsOptions.removeGSMOptions();
                    mGsmUmtsOptions = null;
                    mCdmaOptions = new CdmaOptions(MSimMobileNetworkSubSettings.this, getPreferenceScreen(),mPhone, mSubscription);
                }
            }
            //when receive the filter event,should all set setScreenState again
            setScreenState();
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        PhoneGlobals app = PhoneGlobals.getInstance();
        addPreferencesFromResource(R.xml.msim_network_sub_setting);

        mSubscription = getIntent().getIntExtra(SUBSCRIPTION_KEY, app.getDefaultSubscription());
        log("Settings onCreate subscription =" + mSubscription);
        mPhone = app.getPhone(mSubscription);
        mHandler = new MyHandler();

        // Register for ACTION_RADIO_TECHNOLOGY_CHANGED intent broadcasts.
        IntentFilter intentFilter =
                new IntentFilter(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        mReceiver = new PhoneAppBroadcastReceiver();
        registerReceiver(mReceiver, intentFilter);

        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();

        PreferenceCategory pcSettingsLabel = new PreferenceCategory(this);
        pcSettingsLabel.setTitle(R.string.settings_label);
        prefSet.addPreference(pcSettingsLabel);
        mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_PREFERED_NETWORK_MODE);

        boolean isLteOnCdma = mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;
        if (getResources().getBoolean(R.bool.world_phone) == true) {

            if (SystemProperties.getBoolean("ro.monkey", false)) {
                prefSet.removePreference(mButtonPreferredNetworkMode);
            } else {
                // set the listener for the mButtonPreferredNetworkMode list
                // preference so we can issue change Preferred Network Mode.
                mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);

            //Get the networkMode from Settings.System and displays it
            int settingsNetworkMode = getPreferredNetworkMode();
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
                mCdmaOptions = new CdmaOptions(this, prefSet, mPhone,mSubscription);
            mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, mSubscription);
            }
        } else {
            /*if (!isLteOnCdma) {
                         prefSet.removePreference(mButtonPreferredNetworkMode);
                    }*/
            int phoneType = mPhone.getPhoneType();
            if (FeatureQuery.FEATURE_PHONE_GLOBAL_MODE && mSubscription == 0) {
                    int settingsNetworkMode = getPreferredNetworkMode();
                    int trfNetworkMode = transformNetworkMode(settingsNetworkMode);
                    log("the preferred network mode = " + settingsNetworkMode + "; the transform network mode = "+trfNetworkMode);

                    mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);

                    if (isMultiAppCard()) {
                        mButtonPreferredNetworkMode.setEntries(
                                R.array.preferred_network_mode_choices_global_mode);
                        mButtonPreferredNetworkMode.setEntryValues(
                                R.array.preferred_network_mode_values_global_mode);
                        mButtonPreferredNetworkMode.setValue(Integer.toString(trfNetworkMode));
                    } else {
                        String appType = getSingleAppType();
                        if ("RUIM".equals(appType) || "CSIM".equals(appType)) {
                            mButtonPreferredNetworkMode.setEntries(R.array.preferred_network_mode_choices_cdma);
                            mButtonPreferredNetworkMode.setEntryValues(R.array.preferred_network_mode_values_cdma);
                            if (trfNetworkMode == Phone.NT_MODE_CDMA)
                                mButtonPreferredNetworkMode.setValue(Integer.toString(trfNetworkMode));
                        } else if ("USIM".equals(appType) || "SIM".equals(appType)) {
                            mButtonPreferredNetworkMode.setEntries(R.array.preferred_network_mode_choices_gsm);
                            mButtonPreferredNetworkMode.setEntryValues(R.array.preferred_network_mode_values_gsm);
                            if (trfNetworkMode == Phone.NT_MODE_GSM_ONLY)
                                mButtonPreferredNetworkMode.setValue(Integer.toString(trfNetworkMode));
                        } else {
                            Log.w(LOG_TAG, "it is unknown app type: "+appType);
                        }
                    }

            } else {
                if(FeatureQuery.FEATURE_PREFERRED_NETWORK_MODE_CU && mSubscription == 0){
                    mButtonPreferredNetworkMode.setEntries(R.array.preferred_network_mode_choices_wcdma);
                    mButtonPreferredNetworkMode.setEntryValues(R.array.preferred_network_mode_values_wcdma);
                    mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);
                }else{
                    prefSet.removePreference(mButtonPreferredNetworkMode);
                }
            }
            if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, mSubscription);
            } else if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                mCdmaOptions = new CdmaOptions(this, prefSet, mPhone, mSubscription);
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        //add preference category
        PreferenceCategory pcDataSettings = new PreferenceCategory(this);
        pcDataSettings.setTitle(R.string.title_data_settings);
        prefSet.addPreference(pcDataSettings);

        this.addPreferencesFromResource(R.xml.msim_network_setting);
        //remove manage sub button
        prefSet.removePreference(prefSet.findPreference(BUTTON_MANAGE_SUB_KEY));

        mButtonDataRoam = (CheckBoxPreference) prefSet.findPreference(BUTTON_ROAMING_KEY);
        mButtonDataUsage = prefSet.findPreference(BUTTON_DATA_USAGE_KEY);
        mLteDataServicePref = prefSet.findPreference(BUTTON_CDMA_LTE_DATA_SERVICE_KEY);
        prefSet.removePreference(prefSet.findPreference(BUTTON_DATA_ENABLE_KEY));

        boolean isSimLteOnCdma = mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;

        final boolean missingDataServiceUrl = TextUtils.isEmpty(
                android.provider.Settings.Secure.getString(getContentResolver(),
                        android.provider.Settings.Secure.SETUP_PREPAID_DATA_SERVICE_URL));
        if (!isSimLteOnCdma || missingDataServiceUrl) {
            prefSet.removePreference(mLteDataServicePref);
        } else {
            android.util.Log.d(LOG_TAG, "keep ltePref");
        }

        mDataUsageListener = new DataUsageListener(this, mButtonDataUsage, prefSet);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setScreenState();

        if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null)  {
            mPhone.getPreferredNetworkType(mHandler.obtainMessage(
                    MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE));
        }

        // Set UI state in onResume because a user could go home, launch some
        // app to change this setting's backend, and re-launch this settings app
        // and the UI state would be inconsistent with actual state
        mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());
        mDataUsageListener.resume();
    }

    private void setScreenState() {
        boolean airplane = (android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.AIRPLANE_MODE_ON, 0) != 0);
        log("setScreenState sub : " + mSubscription);
        // Set the preferences disabled if the sim state can not be recognized or deactivate.
        boolean bValidSimstate = MSimTelephonyManager.getDefault().isValidSimState(mSubscription);
        //need judge whether to enable apn item when resume,when airplane on,disable,off,wait sim status ready.
        getPreferenceScreen().setEnabled(!airplane && bValidSimstate);
        log("isValidSimState :" + bValidSimstate);
        //and use the null condition to judge the phone option used now
        if(null != mCdmaOptions){
            mCdmaOptions.setApnItemStatus(bValidSimstate && !airplane && isAPNNumericLoaded());
        }
        if(null != mGsmUmtsOptions){
            mGsmUmtsOptions.setApnItemStatus(bValidSimstate && !airplane && isAPNNumericLoaded());
        }
        /*
         * NOTICE: In DSDS mode, SLOT2, i.e. mSubscrition = 1, only supports GSM
         * mode, so always set this button disabled
         */
        CheckBoxPreference mButtonPrefer2g = (CheckBoxPreference) findPreference("button_prefer_2g_key");
        if (mButtonPrefer2g != null) {
            Use2GOnlyCheckBoxPreference.updatePhone(mPhone);
            if (mSubscription == 1)
                mButtonPrefer2g.setEnabled(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataUsageListener.pause();
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on CLIR.
     *
     * @param preference is the preference to be changed, should be mButtonCLIR.
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mButtonPreferredNetworkMode) {
            //NOTE onPreferenceChange seems to be called even if there is no change
            //Check if the button value is changed from the System.Setting
            mButtonPreferredNetworkMode.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            int settingsNetworkMode = getPreferredNetworkMode();
            if (buttonNetworkMode != settingsNetworkMode) {
                int modemNetworkMode = buttonNetworkMode;
                // if new mode is invalid set mode to default preferred
                if ((modemNetworkMode < Phone.NT_MODE_WCDMA_PREF)
                        || (modemNetworkMode > Phone.NT_MODE_LTE_WCDMA)) {
                    log("Invalid Network Mode (" + modemNetworkMode + ") Chosen. Ignore mode");
                    return true;
                }

                UpdatePreferredNetworkModeSummary(buttonNetworkMode);
                setPreferredNetworkMode(buttonNetworkMode);
                //Set the modem network mode
                mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                        .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        }

        // always let the preference setting proceed.
        return true;
    }

    private int getPreferredNetworkMode() {
        int nwMode;
        try {
            nwMode = android.telephony.MSimTelephonyManager.getIntAtIndex(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                    mSubscription);
        } catch (SettingNotFoundException snfe) {
            log("getPreferredNetworkMode: Could not find PREFERRED_NETWORK_MODE!!!");
            nwMode = preferredNetworkMode;
        }
        return nwMode;
    }

    private void setPreferredNetworkMode(int nwMode) {
        android.telephony.MSimTelephonyManager.putIntAtIndex(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Global.PREFERRED_NETWORK_MODE,
                    mSubscription, nwMode);
        // Refresh the GSM UMTS options UI on network mode change
        if (mGsmUmtsOptions != null) mGsmUmtsOptions.enableScreen();

    }

    private class MyHandler extends Handler {

        static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE = 0;
        static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PREFERRED_NETWORK_TYPE:
                    handleGetPreferredNetworkTypeResponse(msg);
                    break;

                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
            }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int modemNetworkMode = ((int[])ar.result)[0];

                if (DBG) {
                    log ("handleGetPreferredNetworkTypeResponse: modemNetworkMode = " +
                            modemNetworkMode);
                }

                int settingsNetworkMode = getPreferredNetworkMode();
                if (DBG) {
                    log("handleGetPreferredNetworkTypeReponse: settingsNetworkMode = " +
                            settingsNetworkMode);
                }

                //check that modemNetworkMode is from an accepted value
                if (modemNetworkMode == Phone.NT_MODE_WCDMA_PREF ||
                        modemNetworkMode == Phone.NT_MODE_GSM_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_WCDMA_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_GSM_UMTS ||
                        modemNetworkMode == Phone.NT_MODE_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_CDMA_NO_EVDO ||
                        modemNetworkMode == Phone.NT_MODE_EVDO_NO_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_GLOBAL ||
                        modemNetworkMode == Phone.NT_MODE_LTE_CDMA_AND_EVDO ||
                        modemNetworkMode == Phone.NT_MODE_LTE_GSM_WCDMA ||
                        modemNetworkMode == Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA ||
                        modemNetworkMode == Phone.NT_MODE_LTE_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_LTE_WCDMA) {
                    if (DBG) {
                        log("handleGetPreferredNetworkTypeResponse: if 1: modemNetworkMode = " +
                                modemNetworkMode);
                    }

                    //check changes in modemNetworkMode and updates settingsNetworkMode
                    if (modemNetworkMode != settingsNetworkMode) {
                        if (DBG) {
                            log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                    "modemNetworkMode != settingsNetworkMode");
                        }

                        settingsNetworkMode = modemNetworkMode;

                        if (DBG) { log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                "settingsNetworkMode = " + settingsNetworkMode);
                        }

                        //changes the Settings.System accordingly to modemNetworkMode
                        setPreferredNetworkMode(settingsNetworkMode);
                    }

                    UpdatePreferredNetworkModeSummary(modemNetworkMode);
                    // changes the mButtonPreferredNetworkMode accordingly to modemNetworkMode
                    if (FeatureQuery.FEATURE_PHONE_GLOBAL_MODE) {
                         modemNetworkMode = transformNetworkMode(modemNetworkMode);
                    }
                    mButtonPreferredNetworkMode.setValue(Integer.toString(modemNetworkMode));
                } else {
                    if (DBG) log("handleGetPreferredNetworkTypeResponse: else: reset to default");
                    resetNetworkModeToDefault();
                }
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int networkMode = Integer.valueOf(
                        mButtonPreferredNetworkMode.getValue()).intValue();
                setPreferredNetworkMode(networkMode);
            } else {
                mPhone.getPreferredNetworkType(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE));
            }
            // Update '2GOnly checkbox' based on recent preferred network type selection.
            if (mGsmUmtsOptions != null)
                Use2GOnlyCheckBoxPreference.updatePhone(mPhone);
        }

        private void resetNetworkModeToDefault() {
            //set the mButtonPreferredNetworkMode
            mButtonPreferredNetworkMode.setValue(Integer.toString(preferredNetworkMode));
            //set the Settings.System
            setPreferredNetworkMode(preferredNetworkMode);
            //Set the Modem
            mPhone.setPreferredNetworkType(preferredNetworkMode,
                    this.obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
        }
    }

    private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
        if (FeatureQuery.FEATURE_PHONE_GLOBAL_MODE) {
            int trfNetworkMode = transformNetworkMode(NetworkMode);
            if (trfNetworkMode == Phone.NT_MODE_GSM_ONLY) {
                 mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_gsm_summary);
            } else if (trfNetworkMode == Phone.NT_MODE_GLOBAL) {
                 mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_global_summary);
            } else {
                 mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_cdma_summary);
            }
        } else {
            switch(NetworkMode) {
                case Phone.NT_MODE_WCDMA_PREF:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_wcdma_perf_summary);
                    break;
                case Phone.NT_MODE_GSM_ONLY:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_gsm_only_summary);
                    break;
                case Phone.NT_MODE_WCDMA_ONLY:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_wcdma_only_summary);
                    break;
                case Phone.NT_MODE_GSM_UMTS:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_gsm_wcdma_summary);
                    break;
                case Phone.NT_MODE_CDMA:
                    switch (mPhone.getLteOnCdmaMode()) {
                        case PhoneConstants.LTE_ON_CDMA_TRUE:
                            mButtonPreferredNetworkMode.setSummary(
                                R.string.preferred_network_mode_cdma_summary);
                        break;
                        case PhoneConstants.LTE_ON_CDMA_FALSE:
                        default:
                            mButtonPreferredNetworkMode.setSummary(
                                R.string.preferred_network_mode_cdma_evdo_summary);
                            break;
                    }
                    break;
                case Phone.NT_MODE_CDMA_NO_EVDO:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_cdma_only_summary);
                    break;
                case Phone.NT_MODE_EVDO_NO_CDMA:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_evdo_only_summary);
                    break;
                case Phone.NT_MODE_LTE_ONLY:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_lte_summary);
                    break;
                case Phone.NT_MODE_LTE_GSM_WCDMA:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_lte_gsm_wcdma_summary);
                    break;
                case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_lte_cdma_evdo_summary);
                    break;
                case Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_global_summary);
                    break;
                case Phone.NT_MODE_GLOBAL:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_cdma_evdo_gsm_wcdma_summary);
                    break;
                case Phone.NT_MODE_LTE_WCDMA:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_lte_wcdma_summary);
                    break;
                default:
                    mButtonPreferredNetworkMode.setSummary(
                            R.string.preferred_network_mode_global_summary);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case REQUEST_CODE_EXIT_ECM:
            Boolean isChoiceYes =
                data.getBooleanExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false);
            if (isChoiceYes) {
                // If the phone exits from ECM mode, show the CDMA Options
                mCdmaOptions.showDialog(mClickedPreference);
            } else {
                // do nothing
            }
            break;

        default:
            break;
        }
    }

    private static void log(String msg) {
        if(DBG){
            Log.d(LOG_TAG, msg);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            // Commenting out "logical up" capability. This is a workaround for issue 5278083.
            //
            // Settings app may not launch this activity via UP_ACTIVITY_CLASS but the other
            // Activity that looks exactly same as UP_ACTIVITY_CLASS ("SubSettings" Activity).
            // At that moment, this Activity launches UP_ACTIVITY_CLASS on top of the Activity.
            // which confuses users.
            // TODO: introduce better mechanism for "up" capability here.
            /*Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(UP_ACTIVITY_PACKAGE, UP_ACTIVITY_CLASS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);*/
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Only three items: Global, CDMA and GSM. In order to show this
     * item, we should transform network mode
     */
    private int transformNetworkMode(int networkMode) {
        int tranfsNetworkMode = Phone.NT_MODE_CDMA;
        switch(networkMode) {
            case Phone.NT_MODE_GLOBAL:
            case Phone.NT_MODE_LTE_CMDA_EVDO_GSM_WCDMA:
                return Phone.NT_MODE_GLOBAL;
            case Phone.NT_MODE_WCDMA_PREF:
            case Phone.NT_MODE_GSM_ONLY:
            case Phone.NT_MODE_WCDMA_ONLY:
            case Phone.NT_MODE_GSM_UMTS:
            case Phone.NT_MODE_LTE_GSM_WCDMA:
            case Phone.NT_MODE_LTE_WCDMA:
                return Phone.NT_MODE_GSM_ONLY;
            case Phone.NT_MODE_CDMA:
            case Phone.NT_MODE_CDMA_NO_EVDO:
            case Phone.NT_MODE_EVDO_NO_CDMA:
            case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
            case Phone.NT_MODE_LTE_ONLY:
            default:
                return Phone.NT_MODE_CDMA;
        }

    }

    /*
     * check if the card is multi app card
     */
    private boolean isMultiAppCard() {
        CardSubscriptionManager cardSubMgr = CardSubscriptionManager.getInstance();
        if (cardSubMgr != null) {
            SubscriptionData subData = cardSubMgr.getCardSubscriptions(mSubscription);
            if (subData != null) {
                log("the num app is "+subData.getLength());
                return subData.getLength() > 1;
            }
        }

        return false;
    }

    /*
     * when the card is single app, we should get its type
     */
    private String getSingleAppType() {
        MSimTelephonyManager mSimTM = MSimTelephonyManager.getDefault();
        return mSimTM.getCardType(mSubscription);
    }

    //get apn property to judge if it has been loaded
    private boolean isAPNNumericLoaded(){
        String mccMncFromSim = MSimTelephonyManager.getTelephonyProperty(
                TelephonyProperties.PROPERTY_APN_SIM_OPERATOR_NUMERIC, mSubscription, null);
        if(DBG) Log.d(LOG_TAG," mccMncFromSim = "+mccMncFromSim);
        boolean isAPNNumericLoaded = null == mccMncFromSim || mccMncFromSim.equals("");
        return !isAPNNumericLoaded;
    }

}
