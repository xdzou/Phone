/*
*Created for Unicom sending DM messages
*
*
*/

package com.android.phone;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wimax.WimaxManagerConstants;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.MSimTelephonyManager;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.MSimConstants;
import android.telephony.MSimTelephonyManager;
import android.app.PendingIntent;
import android.telephony.MSimSmsManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.app.Activity;
import com.android.internal.telephony.IccCardConstants;
import com.qrd.plugin.feature_query.DefaultQuery;

public class UnicomDMRegister {
    private static final String TAG = "UnicomDMRegister";
    private String REG_MSG_ACTION_OK = "android.selfsms.regok";
    
    /*Unicom lab test 10655464; Release 10655459*/
    private static final String SMS_NUMBER = DefaultQuery.AUTO_REG_SMS_SEVERNUM;
    
    private static final String REG_MSG_ACTION = "com.android.phone.networkswitchmanager.SMS_REG";
    private static final String PREF_FILE = "network_reg";
    private static final String SMSREG_IMSI = "smsreg_imsi";
    
    private int mRegSub = 0;
    private String strImsi = null;
    
    private static boolean REG_LOCK = false;
    private static int RETRY_TIME = 2;
    private static final int EVENT_REGISTER_SENDING = 1;
    private static final int REG_TIMER = 120*1000;
    
    private final Handler mHandler = new UnicomDMRegisterHandler();
    private Context mContext;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED))
            {
                String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                int subId = intent.getIntExtra(MSimConstants.SUBSCRIPTION_KEY, 0);
                
                Log.d(TAG, "stateExtra = " + stateExtra + ";  subID = "+ subId);
                if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(stateExtra))
                {
                    if((MSimConstants.SUB1 == subId) && shouldSmsReg(subId))
                    {
                        REG_LOCK = true;
                        SendRegisterMessage(subId);
                        RETRY_TIME --;
                    }
                }
            }
            else  if (action.equals(REG_MSG_ACTION)) {
                Log.d(TAG, "SmsRegister.SMS_REG_ACTION: ("+ getResultCode() + ")");
                if(Activity.RESULT_OK == getResultCode())
                {
                    saveImsi(strImsi);
                    mContext.unregisterReceiver(mReceiver);
                }
            }
            else if (action.equals(REG_MSG_ACTION_OK)) {
                Log.d(TAG, "android.selfsms.regok");
                saveImsi(strImsi);
                mContext.unregisterReceiver(mReceiver);
            }
        }
    };
        
    public UnicomDMRegister(Context context)
    {  
        Log.d(TAG, "start create register and receiver!");
        
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(REG_MSG_ACTION);
        filter.addAction(REG_MSG_ACTION_OK);
        mContext.registerReceiver(mReceiver, filter);
    }

    private class UnicomDMRegisterHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_REGISTER_SENDING:
               {
                   mRegSub = msg.arg1;
                   byte[] sms = getSmsContent(mRegSub);
                   Intent intent = new Intent(REG_MSG_ACTION, null);
                   PendingIntent sendIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
                   MSimSmsManager.getDefault().sendUnicomRegSms(SMS_NUMBER, null, 0x6838, sms, sendIntent, mRegSub);
                   Log.d(TAG, "SmsRegister: sending sms reg SMS_NUMBER = " + SMS_NUMBER);
                   if(RETRY_TIME > 0)
                   {/*Another time for register if failure */
                       if(shouldSmsReg(mRegSub))
                       {
                           SendRegisterMessage(mRegSub);
                           RETRY_TIME --;
                       }
                   }
                }
                break;
                
              default:
                break;
            }
        }
    }

    /*Unicom message register operations, wangfei, 2011-11-18*/
    private byte[] getSmsContent(int mRegSub) {
        String product = SystemProperties.get("ro.product.model", "");
        
        if (product == null || product.equals("")) {
            product = "Default device";
        }
        
        String brand =  SystemProperties.get("ro.product.brand", "");
        final String version = SystemProperties.get("ro.software.version", "");
        strImsi = curImsi(mRegSub);
        String imei = getImei(mRegSub);
        
        Log.d(TAG, "SmsRegister.getSmsContent()  strImsi="+strImsi+"  imei="+imei+" brand="+brand+"  product="+product+"  version="+version);
        if (strImsi == null || imei == null )
        {
            return null;
        }
        
        final String data = String.format("IMEI:%s/%s/%s/%s/%s", imei, strImsi, brand, product, version);
        
        Log.d(TAG, "SmsRegister.getSmsContent() data="+data);
        
        byte[] content = data.getBytes();

        return content;
    }

    private String getOldImsi() {
        SharedPreferences sp = mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        return sp.getString(SMSREG_IMSI, null);
    }

    /*Get IMEI number*/
    private String getImei(int mRegSub) {
        return MSimTelephonyManager.getDefault().getDeviceId(mRegSub);
    }
    
    /*Get IMSI number*/
    private String curImsi(int mRegSub) {
        return MSimTelephonyManager.getDefault().getSubscriberId(mRegSub);
    }

    private void saveImsi(String imsi) {
        SharedPreferences sp = mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SMSREG_IMSI, imsi);
        editor.commit();
    }

    private boolean shouldSmsReg(int mRegSub) {
        if(REG_LOCK)
        {
            return false;
        }
        
        String oldImsi = getOldImsi();
        String curImsi = curImsi(mRegSub);
        String imei = getImei(mRegSub);
        
        Log.d(TAG, "SmsRegister.shouldSmsReg() "+
              " oldImsi="+oldImsi+
              " curImsi="+curImsi+
              " imei="+imei);

        if(null == curImsi || curImsi.length()==0 )
        {
            return false;
        }
        
        if(!curImsi.substring(3, 5).equals("01") && !curImsi.substring(3, 5).equals("10"))
        {
            return false;
        }
        return (curImsi != null && !TextUtils.equals(oldImsi, curImsi));
    }

    private void SendRegisterMessage(int mRegSub) {
        Log.d(TAG, " SendRegisterMessage subscription = "+ mRegSub);
        Message m = mHandler.obtainMessage(EVENT_REGISTER_SENDING);
        m.arg1= mRegSub;
        mHandler.sendMessageDelayed(m, REG_TIMER);
    }

}

