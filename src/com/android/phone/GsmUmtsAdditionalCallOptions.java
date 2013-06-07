package com.android.phone;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;

import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;

public class GsmUmtsAdditionalCallOptions extends
        TimeConsumingPreferenceActivity {
    private static final String LOG_TAG = "GsmUmtsAdditionalCallOptions";
    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final String BUTTON_CLIR_KEY  = "button_clir_key";
    private static final String BUTTON_CW_KEY    = "button_cw_key";

    private CLIRListPreference mCLIRButton;
    private CallWaitingCheckBoxPreference mCWButton;

    private final ArrayList<Preference> mPreferences = new ArrayList<Preference>();
    private int mInitIndex= 0;
    private int mSubscription = 0;
    private BroadcastReceiver mAirplaneListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean airplanOn = intent.getBooleanExtra("state", false);
            if (DBG)
                Log.d(LOG_TAG, "intent:" + intent);
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action) && airplanOn) {
                mCWButton.onAirplanOn();
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.gsm_umts_additional_options);
        mSubscription = getIntent().getIntExtra(SUBSCRIPTION_KEY,
                PhoneGlobals.getInstance().getDefaultSubscription());
        if (DBG)
            Log.d(LOG_TAG, "GsmUmtsAdditionalCallOptions onCreate, subscription: " + mSubscription);
        PreferenceScreen prefSet = getPreferenceScreen();
        mCLIRButton = (CLIRListPreference) prefSet.findPreference(BUTTON_CLIR_KEY);
        mCWButton = (CallWaitingCheckBoxPreference) prefSet.findPreference(BUTTON_CW_KEY);

        mPreferences.add(mCLIRButton);
        mPreferences.add(mCWButton);

        if (icicle == null) {
            if (DBG) Log.d(LOG_TAG, "start to init ");
            mCLIRButton.init(this, false, mSubscription);
        } else {
            if (DBG) Log.d(LOG_TAG, "restore stored states");
            mInitIndex = mPreferences.size();
            mCLIRButton.init(this, true, mSubscription);
            mCWButton.init(this, true, mSubscription);
            int[] clirArray = icicle.getIntArray(mCLIRButton.getKey());
            if (clirArray != null) {
                if (DBG) Log.d(LOG_TAG, "onCreate:  clirArray[0]="
                        + clirArray[0] + ", clirArray[1]=" + clirArray[1]);
                mCLIRButton.handleGetCLIRResult(clirArray);
            } else {
                mCLIRButton.init(this, false, mSubscription);
            }
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mAirplaneListener, filter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCLIRButton.clirArray != null) {
            outState.putIntArray(mCLIRButton.getKey(), mCLIRButton.clirArray);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        if (mInitIndex < mPreferences.size()-1 && !isFinishing()) {
            mInitIndex++;
            Preference pref = mPreferences.get(mInitIndex);
            if (pref instanceof CallWaitingCheckBoxPreference) {
                ((CallWaitingCheckBoxPreference) pref).init(this, false, mSubscription);
            }
        }
        super.onFinished(preference, reading);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            CallFeaturesSetting.goUpToTopLevelSetting(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (null != mAirplaneListener) {
            unregisterReceiver(mAirplaneListener);
        }
        super.onDestroy();
    }
}
