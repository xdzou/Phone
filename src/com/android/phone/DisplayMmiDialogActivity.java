/*
 * Copyright (c) 2011-2012, The Linux Foundation. All rights reserved.
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
 *
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

import android.os.Bundle;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.KeyEvent;
import android.widget.EditText;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import com.android.internal.app.AlertActivity;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.MmiCode;
import android.util.Log;


public class DisplayMmiDialogActivity extends AlertActivity {
    
    private static final String LOG_TAG = "DisplayMmiDialogActivity";
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);
    
    public static final String USSD_MESSAGE_TITLE = "ussd_title";
    public static final String USSD_MESSAGE_TEXT  = "ussd_message";
    
    private int mTitle;
    private String mText;

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent mIntent = getIntent();        
        mTitle = mIntent.getIntExtra(USSD_MESSAGE_TITLE, 0);
        mText  = mIntent.getStringExtra(USSD_MESSAGE_TEXT);

        //display the information when screen locked,especially at PIN unlock interface.
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.flags |= flags;
        getWindow().setAttributes(lp);
    }

    protected void onResume() {
        super.onResume();
        
        final PhoneGlobals app = PhoneGlobals.getInstance();
        final Phone mPhone = PhoneGlobals.getInstance().phone;

        MmiCode mMmiCode = PhoneUtils.getCurrentMmiCode();
        MmiCode.State mState = null;
        if (mMmiCode != null) {
            mState = mMmiCode.getState();
        } else {
            finish();
            return;
        }
        
        if ((app.getPUKEntryActivity() != null) && (mState == MmiCode.State.COMPLETE)) {
            if (DBG) log("displaying PUK unblocking progress dialog.");

            // create the progress dialog, make sure the flags and type are
            // set correctly.
            ProgressDialog pd = new ProgressDialog(app);
            pd.setTitle(mTitle);
            pd.setMessage(mText);
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
            pd.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            // display the dialog
            pd.show();

            // indicate to the Phone app that the progress dialog has
            // been assigned for the PUK unlock / SIM READY process.
            app.setPukEntryProgressDialog(pd);

        } else {
            // In case of failure to unlock, we'll need to reset the
            // PUK unlock activity, so that the user may try again.
            if (app.getPUKEntryActivity() != null) {
                app.setPukEntryActivity(null);
            }

            // A USSD in a pending state means that it is still
            // interacting with the user.
            if (mState != MmiCode.State.PENDING) {
                if (DBG) log("MMI code has finished running.");

                if (DBG) log("Extended NW displayMMIInitiate (" + mText + ")");
                if (mText == null || mText.length() == 0)
                    return;

                final DialogInterface.OnClickListener mUSSDFailDialogListener =
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            switch (whichButton) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    finish();
                                    break;
                            }
                        }
                    };
                
                // displaying system alert dialog on the screen instead of
                // using another activity to display the message.  This
                // places the message at the forefront of the UI.
                AlertDialog newDialog = new AlertDialog.Builder(DisplayMmiDialogActivity.this)
                        .setMessage(mText)
                        .setPositiveButton(R.string.ok, mUSSDFailDialogListener)
                        .setCancelable(true)
                        .create();

                newDialog.getWindow().setType(
                        WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
                newDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                newDialog.show();            
            } else {
                if (DBG) log("USSD code has requested user input. Constructing input dialog.");

                // USSD MMI code that is interacting with the user.  The
                // basic set of steps is this:
                //   1. User enters a USSD request
                //   2. We recognize the request and displayMMIInitiate
                //      (above) creates a progress dialog.
                //   3. Request returns and we get a PENDING or COMPLETE
                //      message.
                //   4. These MMI messages are caught in the PhoneApp
                //      (onMMIComplete) and the InCallScreen
                //      (mHandler.handleMessage) which bring up this dialog
                //      and closes the original progress dialog,
                //      respectively.
                //   5. If the message is anything other than PENDING,
                //      we are done, and the alert dialog (directly above)
                //      displays the outcome.
                //   6. If the network is requesting more information from
                //      the user, the MMI will be in a PENDING state, and
                //      we display this dialog with the message.
                //   7. User input, or cancel requests result in a return
                //      to step 1.  Keep in mind that this is the only
                //      time that a USSD should be canceled.

                // inflate the layout with the scrolling text area for the dialog.
                LayoutInflater inflater = (LayoutInflater) getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                View dialogView = inflater.inflate(R.layout.dialog_ussd_response, null);

                // get the input field.
                final EditText inputText = (EditText) dialogView.findViewById(R.id.input_field);

                // specify the dialog's click listener, with SEND and CANCEL logic.
                final DialogInterface.OnClickListener mUSSDDialogListener =
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            switch (whichButton) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    mPhone.sendUssdResponse(inputText.getText().toString());
                                    finish();
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    PhoneUtils.cancelUssdDialog();
                                    finish();
                                    break;
                            }
                        }
                    };

                // build the dialog
                final AlertDialog newDialog = new AlertDialog.Builder(DisplayMmiDialogActivity.this)
                        .setMessage(mText)
                        .setView(dialogView)
                        .setPositiveButton(R.string.send_button, mUSSDDialogListener)
                        .setNegativeButton(R.string.cancel, mUSSDDialogListener)
                        .setCancelable(false)
                        .create();

                // attach the key listener to the dialog's input field and make
                // sure focus is set.
                final View.OnKeyListener mUSSDDialogInputListener =
                    new View.OnKeyListener() {
                        public boolean onKey(View v, int keyCode, KeyEvent event) {
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_CALL:
                                case KeyEvent.KEYCODE_ENTER:
                                    if(event.getAction() == KeyEvent.ACTION_DOWN) {
                                        mPhone.sendUssdResponse(inputText.getText().toString());
                                        newDialog.dismiss();
                                        finish();
                                    }
                                    return true;
                            }
                            return false;
                        }
                    };
                inputText.setOnKeyListener(mUSSDDialogInputListener);
                inputText.requestFocus();

                // set the window properties of the dialog
                newDialog.getWindow().setType(
                        WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
                newDialog.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                // now show the dialog!
                newDialog.show();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.v(LOG_TAG, "onNewIntent");
        // force to finish ourself and then start new one
        finish();
        startActivity(intent);
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
