package com.android.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "BootBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "receive action: " + action);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            if (!TelephonyManager.isMultiSimEnabled())
            {
               Log.d(TAG, "disable MSimCallFeaturesSetting");
               PackageManager pm = context.getPackageManager();
               pm.setComponentEnabledSetting(new ComponentName("com.android.phone",
                                             "com.android.phone.MSimCallFeaturesSetting"),
                                             PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
            }else{
                Log.d(TAG, "disable CallFeaturesSetting");
                PackageManager pm = context.getPackageManager();
                pm.setComponentEnabledSetting(new ComponentName("com.android.phone",
                                              "com.android.phone.CallFeaturesSetting"),
                                              PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
            }
            return;
        } 
    }
}
