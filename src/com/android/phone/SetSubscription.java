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

import java.lang.Integer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.view.Display;
import android.content.Context;
import android.widget.CheckBox;
import android.widget.LinearLayout.LayoutParams;
import android.content.DialogInterface;
import android.app.AlertDialog;

import android.content.Intent;

import com.android.internal.telephony.ProxyManager;


/**
 * Displays a dialer like interface to Set the Subscriptions.
 */
public class SetSubscription extends PreferenceActivity implements View.OnClickListener,
       DialogInterface.OnDismissListener, DialogInterface.OnClickListener {

    private static final String TAG = "SetSubscription";

    private Bundle mSubscrInfo;
    private TextView mOkButton, mCancelButton;
    SubscriptionCheckBoxPreference sub_array[];
    private boolean sub_err = false;

    //String keys for preference lookup
    private static final String PREF_PARENT_KEY = "subscr_parent";
    private static final String SUB_GROUP_01_KEY = "sub_group_01";
    private static final String SUB_GROUP_02_KEY = "sub_group_02";

    private final int MAX_SUBSCRIPTIONS = 2;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        initFromIntent(getIntent());

        addPreferencesFromResource(R.xml.set_subscription_pref);
        setContentView(R.layout.set_subscription_pref_layout);

        mOkButton = (TextView) findViewById(R.id.ok);
        mOkButton.setOnClickListener(this);
        mCancelButton = (TextView) findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(this);

        TextView t1 = (TextView) findViewById(R.id.sub_0);
        TextView t2 = (TextView) findViewById(R.id.sub_1);
        TextView t3 = (TextView) findViewById(R.id.app_name);

        // align the labels
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        t1.setLayoutParams(new LayoutParams(75, LayoutParams.WRAP_CONTENT));
        t2.setLayoutParams(new LayoutParams(75, LayoutParams.WRAP_CONTENT));
        t3.setLayoutParams(new LayoutParams(width - 150, LayoutParams.WRAP_CONTENT));

        // To store the selected subscriptions
        // index 0 for sub0 and index 1 for sub1
        sub_array = new SubscriptionCheckBoxPreference[MAX_SUBSCRIPTIONS];

        populateList();
    }

    /** get the data from the intent */
    private void initFromIntent(Intent intent) {
        if (intent != null) {
            mSubscrInfo = intent.getExtras();
        } else {
            finish();
        }
    }


    /** add radio buttons to the group */
    private void populateList() {
        PreferenceScreen prefParent = (PreferenceScreen) getPreferenceScreen().findPreference(PREF_PARENT_KEY);
        int i = 0;
        int numApps = 0;
        String[] subscrInfo = mSubscrInfo.getStringArray("SUBSCR INFO 01");
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();

        if ((subscrInfo != null ) && (subscrInfo.length > 0)) {
            numApps = subscrInfo.length;

            // Create a subgroup for the apps in card 01
            PreferenceCategory subGroup01 = new PreferenceCategory(this);
            subGroup01.setKey(SUB_GROUP_01_KEY);
            subGroup01.setTitle(R.string.card_01);
            prefParent.addPreference(subGroup01);

            // Add each element as a CheckBoxPreference to the group
            for (; (i < numApps) && (subscrInfo[i] != null); i++){
                Log.d(TAG, "populateList:  subscrInfo[" + i + "] = " + subscrInfo[i]);
                SubscriptionCheckBoxPreference newCheckBox = new SubscriptionCheckBoxPreference(this, width);
                newCheckBox.setTitleText(subscrInfo[i]);
                // Key is the string : "slot<SlotId> index<IndexId>"
                newCheckBox.setKey(new String("slot0 index" + Integer.toString(i)));
                newCheckBox.setOnSubPreferenceClickListener(mCheckBoxListener);
                subGroup01.addPreference(newCheckBox);
            }
        }

        subscrInfo = mSubscrInfo.getStringArray("SUBSCR INFO 02");

        if ((subscrInfo != null ) && (subscrInfo.length > 0)) {
            numApps += subscrInfo.length;

            // Create a subgroup for the apps in card 02
            PreferenceCategory subGroup02 = new PreferenceCategory(this);
            subGroup02 = new PreferenceCategory(this);
            subGroup02.setKey(SUB_GROUP_02_KEY);
            subGroup02.setTitle(R.string.card_02);
            prefParent.addPreference(subGroup02);

            // add each element as a SubscriptionCheckBoxPreference to the group
            for (int j = 0; (i < numApps) && (subscrInfo[j] != null); i++, j++){
                Log.d(TAG, "populateList:  subscrInfo[" + i + "] = " + subscrInfo[j]);
                SubscriptionCheckBoxPreference newCheckBox = new SubscriptionCheckBoxPreference(this, width);
                newCheckBox.setTitleText(subscrInfo[j]);
                // Key is the string :  "slot<SlotId> index<IndexId>"
                newCheckBox.setKey(new String("slot1 index" + Integer.toString(j)));
                newCheckBox.setOnSubPreferenceClickListener(mCheckBoxListener);
                subGroup02.addPreference(newCheckBox);
            }
        }
    }

    Preference.OnPreferenceClickListener mCheckBoxListener = new Preference.OnPreferenceClickListener() {
        public boolean onPreferenceClick(Preference preference) {
            SubscriptionCheckBoxPreference subPref = (SubscriptionCheckBoxPreference)preference;
            SubscriptionID checked = subPref.getSelectedSubscription();

            Log.d(TAG, "onPreferenceClick: KEY = " + subPref.getKey() + " checked = " + checked);

            if (checked == SubscriptionID.SUB_0) {
                // If user already selected a sub0 uncheck it
                if (sub_array[0] != null) {
                    sub_array[0].setUnChecked();
                }
                // Store the sub_array[0] if there is sub0 selected.
                //if (sub_array[0] == null)
                sub_array[0] = subPref;

                // If the user changes the sub_array[1] to sub0 for the same sim app.
                // mark sub_array[1] as null.
                if (subPref == sub_array[1]) {
                    sub_array[1] = null;
                }
            } else if (checked == SubscriptionID.SUB_1) {
                // If user already selected a sub1 uncheck it
                if (sub_array[1] != null) {
                    sub_array[1].setUnChecked();
                }
                sub_array[1] = subPref;

                // If the user changes the sub_array[0] to sub0 for the same sim app.
                // mark sub_array[0] as null.
                if (subPref == sub_array[0]) {
                    sub_array[0] = null;
                }
            } else {
                // Use unchecks the preference, clear the array if this is present.
                if (subPref == sub_array[0]) {
                    sub_array[0] = null;
                }
                if (subPref == sub_array[1]) {
                    sub_array[1] = null;
                }
            }
            return true;
        }
    };

    // for View.OnClickListener
    public void onClick(View v) {
        if (v == mOkButton) {
            setSubscription();
        } else if (v == mCancelButton) {
            finish();
        }
    }

    private void setSubscription() {
        int num_sub_selected = 0;

        for (int i = 0; i < sub_array.length; i++) {
            if (sub_array[i] != null) {
                num_sub_selected++;
            }
        }

        if (num_sub_selected == 0) {
            // Show a message to prompt the user to select atleast one.
            Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.set_subscription_error_atleast_one,
                    Toast.LENGTH_SHORT);
            toast.show();
        } else {
            int slot_id[] = new int[MAX_SUBSCRIPTIONS];
            int sub_index[] = new int[MAX_SUBSCRIPTIONS];

            for (int i = 0; i < MAX_SUBSCRIPTIONS; i++) {
                if (sub_array[i] == null) {
                    Log.d(TAG, "setSubscription: Sub " + i + " not selected. Setting -1");
                    slot_id[i] = -1;
                    sub_index[i] = -1;
                } else {
                    // Key is the string :  "slot<SlotId> index<IndexId>"
                    // Split the string into two and get the SlotId and IndexId.
                    String key = sub_array[i].getKey();
                    Log.d(TAG, "setSubscription: key = " + key);
                    String splitKey[] = key.split(" ");
                    String sSlotId = splitKey[0].substring(splitKey[0].indexOf("slot") + 4);
                    slot_id[i] = Integer.parseInt(sSlotId);
                    String sIndexId = splitKey[1].substring(splitKey[1].indexOf("index") + 5);
                    sub_index[i] = Integer.parseInt(sIndexId);
                }
            }

            ProxyManager mProxyManager = ProxyManager.getInstance();
            String result[] = mProxyManager.setSubscription(MAX_SUBSCRIPTIONS, slot_id, sub_index);

            if (result != null) {
                displayAlertDialog(result);
            } else {
                finish();
            }
        }
    }

    void displayAlertDialog(String msg[]) {
        String dispMsg = "";

        if (msg[0] != null && msg[0].equals("FAILED")) {
            sub_err = true;
        }
        if (msg[1] != null && msg[1].equals("FAILED")) {
            sub_err = true;
        }

        for (int i = 0; i < msg.length; i++) {
            if (msg[i] != null) {
                dispMsg = dispMsg + "Set Subscription on Sub " + i + ": " + msg[i] + "\n";
            }
        }

        Log.d(TAG, "displayAlertDialog:  dispMsg = " + dispMsg);
        new AlertDialog.Builder(this).setMessage(dispMsg)
            .setTitle(android.R.string.dialog_alert_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, this)
            .show()
            .setOnDismissListener(this);
    }

    // This is a method implemented for DialogInterface.OnDismissListener
    public void onDismiss(DialogInterface dialog) {
        // If the setSubscription failed for any of the sub, then don'd dismiss the
        // set subscription screen.
        if(!sub_err) {
            finish();
        }
    }

    // This is a method implemented for DialogInterface.OnClickListener.
    // Used to dismiss the dialogs when they come up.
    public void onClick(DialogInterface dialog, int which) {
        // If the setSubscription failed for any of the sub, then don'd dismiss the
        // set subscription screen.
        if(!sub_err) {
            finish();
        }
    }
}


// Widget for displaying the sim app name and two check boxes.
class SubscriptionCheckBoxPreference extends Preference implements View.OnClickListener {

    CheckBox mCheckBox1, mCheckBox2;
    TextView mTitleView;
    String mTitle;
    int mWidth;
    Preference.OnPreferenceClickListener mOnPrefClickListener;

    public SubscriptionCheckBoxPreference(Context context, int width) {
        super(context);
        setLayoutResource(R.layout.preference_set_sub);
        mWidth = width;
        mOnPrefClickListener = null;
    }

    public void setTitleText(String resId) {
        mTitle = resId;
        if (mTitleView != null){
            mTitleView.setText(mTitle);
        }
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);

        mCheckBox1 = (CheckBox) view.findViewById(R.id.check1);
        mCheckBox1.setOnClickListener(this);
        mCheckBox1.setLayoutParams(new LayoutParams(75, LayoutParams.WRAP_CONTENT));
        mCheckBox2 = (CheckBox) view.findViewById(R.id.check2);
        mCheckBox2.setOnClickListener(this);
        mCheckBox2.setLayoutParams(new LayoutParams(75, LayoutParams.WRAP_CONTENT));
        mTitleView = (TextView) view.findViewById(R.id.title1);
        mTitleView.setText(mTitle);
        mTitleView.setLayoutParams(new LayoutParams(mWidth - 150, LayoutParams.WRAP_CONTENT));
    }

    // for View.OnClickListener
    public void onClick(View v) {
        // User can select only one of the check box corresponds to a subscription.
        // So uncheck the other check box if it is already checked.
        if (v == mCheckBox1) {
            if(mCheckBox2.isChecked()) {
                mCheckBox2.setChecked(false);
            }
        }
        if (v == mCheckBox2) {
            if(mCheckBox1.isChecked()) {
                mCheckBox1.setChecked(false);
            }
        }

        if (mOnPrefClickListener != null) {
            mOnPrefClickListener.onPreferenceClick(this);
        }
    }

    public void setUnChecked() {
        mCheckBox1.setChecked(false);
        mCheckBox2.setChecked(false);
    }

    public SubscriptionID getSelectedSubscription() {
        SubscriptionID ret;
        if (mCheckBox1.isChecked()) {
            ret = SubscriptionID.SUB_0;
        } else if (mCheckBox2.isChecked()) {
            ret = SubscriptionID.SUB_1;
        } else {
            ret = SubscriptionID.NONE;
        }
        return ret;
    }
    public void setOnSubPreferenceClickListener(Preference.OnPreferenceClickListener onPreferenceClickListener) {
        mOnPrefClickListener = onPreferenceClickListener;
    }
}

enum SubscriptionID {
    SUB_0,
    SUB_1,
    NONE;
}

