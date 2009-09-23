/*
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


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyProperties;

/**
 * Phone app Emergency Callback screen.
 */
public class EmergencyCallbackMode extends Activity {

    /** Event for TTY mode change */
    private static final int EVENT_EXIT_ECBM    = 100;

    private Phone mPhone;
    private Uri mEmergencyNumber;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ecbm_layout);

        //get mPhone instance
        mPhone = PhoneFactory.getDefaultPhone();

        // Watch for button clicks.
        ImageButton dialButton = (ImageButton)findViewById(R.id.button_dial);
        dialButton.setOnClickListener(mDialListener);

        Button exitButton = (Button)findViewById(R.id.button_exit);
        exitButton.setOnClickListener(mExitListener);

        Button okButton = (Button)findViewById(R.id.button_ok);
        okButton.setOnClickListener(mOkListener);

    }

    private OnClickListener mDialListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            reDialEmergencyNumber();
        }
    };

    private OnClickListener mExitListener = new OnClickListener()
    {
        public void onClick(View v) {
            // Send ECBM exit
            mPhone.exitEmergencyCallbackMode();
        }
    };


    private void retreat() {
        //cancel ECBM notification
        NotificationMgr.getDefault().cancelEcbmNotification();
        // Finish Emergency Callback Mode Application
        finish();
    }


    private OnClickListener mOkListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            // create a notification
            NotificationMgr.getDefault().notifyECBM();
            // finish Application
            finish();
        }
    };


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean ret = true;
        // suppress all key presses except of call key
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                reDialEmergencyNumber();
            }
            case KeyEvent.KEYCODE_ENDCALL: {
                mPhone.exitEmergencyCallbackMode();
                retreat();
            }
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP: {
                ret = false;
            }
        }
        return ret;
    }

    private void reDialEmergencyNumber()
    {
        Intent intent = new Intent(Intent.ACTION_CALL_EMERGENCY,  mEmergencyNumber);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //cancel ECBM notification
        NotificationMgr.getDefault().cancelEcbmNotification();

        boolean inECBM = SystemProperties.getBoolean(TelephonyProperties.PROPERTY_INECM_MODE, false);
        if(!inECBM) {
            // Phone will call us whenever emergency callback mode changes
            // If we are no longer in ECBM, just quit.
            finish();
        }
        Intent intent = getIntent();

        if(mEmergencyNumber == null)
            mEmergencyNumber = intent.getData();
        if(mEmergencyNumber == null) {
            mEmergencyNumber = Uri.parse("tel:911");
        }
    }

}
