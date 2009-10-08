/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * "SIM network unlock" PIN entry screen.
 *
 * @see PhoneApp.EVENT_SIM_NETWORK_LOCKED
 *
 * TODO: This UI should be part of the lock screen, not the
 * phone app (see bug 1804111).
 */
public class IccNetworkDepersonalizationPanel extends IccPanel {

    //debug constants
    private static final boolean DBG = false;

    //events
    private static final int EVENT_ICC_NTWRK_DEPERSONALIZATION_RESULT = 100;
    private static final int EVENT_ICC_DEPERSONALIZATION_RESULT = 101;

    //Constants
    private final static int INT_SIZE = 4;
    private final String mOemIdentifier = "QUALCOMM";
    private final int mHeaderSize = mOemIdentifier.length() + 2 * INT_SIZE;
    private final static int NETWORK = 1;
    private final static int NETWORK_SUBSET = 2;
    private final static int SERVICE_PROVIDER = 3;
    private final static int CORPORATE = 4;
    private final static int SIM = 5;

    private Phone mPhone;
    private int mPersoSubtype;
    /** Starting number for OEMHOOK request and response IDs */
    int OEMHOOK_BASE = 0x80000;
    /** De-activate SIM personalization */
    int OEMHOOK_ME_DEPERSONALIZATION = OEMHOOK_BASE + 4;

    //UI elements
    private EditText     mPinEntry;
    private LinearLayout mEntryPanel;
    private LinearLayout mStatusPanel;
    private TextView     mStatusText;
    private TextView     mPersoSubtypeText;

    private Button       mUnlockButton;
    private Button       mDismissButton;

    //private textwatcher to control text entry.
    private TextWatcher mPinEntryWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence buffer, int start, int olen, int nlen) {
        }

        public void onTextChanged(CharSequence buffer, int start, int olen, int nlen) {
        }

        public void afterTextChanged(Editable buffer) {
            if (SpecialCharSequenceMgr.handleChars(
                    getContext(), buffer.toString(), true)) {
                mPinEntry.getText().clear();
            }
        }
    };

    //handler for unlock function results
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            String[] mPersoSubtypeNames = {"Network","Network SubType",
                                            "Service Provider","Corporate","SIM"};
            if (msg.what == EVENT_ICC_NTWRK_DEPERSONALIZATION_RESULT ||
                msg.what == EVENT_ICC_DEPERSONALIZATION_RESULT) {
                AsyncResult res = (AsyncResult) msg.obj;
                if (res.exception != null) {
                    Log.i(TAG,mPersoSubtypeNames[mPersoSubtype - 1] + " Depersonalization failed.");
                    indicateError();
                    postDelayed(new Runnable() {
                                    public void run() {
                                        hideAlert();
                                        mPinEntry.getText().clear();
                                        mPinEntry.requestFocus();
                                    }
                                }, 3000);
                } else {
                    Log.i(TAG,mPersoSubtypeNames[mPersoSubtype - 1] + " Depersonalization success.");
                    indicateSuccess();
                    postDelayed(new Runnable() {
                                    public void run() {
                                        dismiss();
                                    }
                                }, 3000);
                }
            }
        }
    };

    //constructor
    public IccNetworkDepersonalizationPanel(Context context) {
        super(context);
        mPersoSubtype = NETWORK;
    }

    //constructor
    public IccNetworkDepersonalizationPanel(Context context,int subtype) {
        super(context);
        mPersoSubtype = subtype;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.sim_ndp);

        // PIN entry text field
        mPinEntry = (EditText) findViewById(R.id.pin_entry);
        mPinEntry.setKeyListener(DialerKeyListener.getInstance());
        mPinEntry.setOnClickListener(mUnlockListener);

        // Attach the textwatcher
        CharSequence text = mPinEntry.getText();
        Spannable span = (Spannable) text;
        span.setSpan(mPinEntryWatcher, 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        mEntryPanel = (LinearLayout) findViewById(R.id.entry_panel);
        mPersoSubtypeText = (TextView) findViewById(R.id.perso_subtype_text);
        setPersoPanelTitle();

        mUnlockButton = (Button) findViewById(R.id.ndp_unlock);
        mUnlockButton.setOnClickListener(mUnlockListener);

        // The "Dismiss" button is present in some (but not all) products,
        // based on the "sim_network_unlock_allow_dismiss" resource.
        mDismissButton = (Button) findViewById(R.id.ndp_dismiss);
        if (getContext().getResources().getBoolean(R.bool.sim_network_unlock_allow_dismiss)) {
            if (DBG) log("Enabling 'Dismiss' button...");
            mDismissButton.setVisibility(View.VISIBLE);
            mDismissButton.setOnClickListener(mDismissListener);
        } else {
            if (DBG) log("Removing 'Dismiss' button...");
            mDismissButton.setVisibility(View.GONE);
        }

        //status panel is used since we're having problems with the alert dialog.
        mStatusPanel = (LinearLayout) findViewById(R.id.status_panel);
        mStatusText = (TextView) findViewById(R.id.status_text);

        mPhone = PhoneFactory.getDefaultPhone();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    //Mirrors IccPinUnlockPanel.onKeyDown().
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    View.OnClickListener mUnlockListener = new View.OnClickListener() {
        public void onClick(View v) {
            String pin = mPinEntry.getText().toString();

            if (TextUtils.isEmpty(pin)) {
                return;
            }

            if (mPersoSubtype == NETWORK) {
                  Log.e(TAG,"requesting network depersonalization");
                  mPhone.getIccCard().supplyNetworkDepersonalization(pin,
                        Message.obtain(mHandler, EVENT_ICC_NTWRK_DEPERSONALIZATION_RESULT));
            } else {
                  Log.e(TAG,"requesting depersonalization with subtype " + mPersoSubtype);
                  sendDepersoOemRilRequestRaw(pin);
            }
            indicateBusy();
        }
    };

    private void indicateBusy() {
        int[] busyLabels = {R.string.requesting_unlock,R.string.requesting_nw_subset_unlock,
                            R.string.requesting_sp_unlock,R.string.requesting_corporate_unlock,
                            R.string.requesting_sim_unlock};
        mStatusText.setText(busyLabels[mPersoSubtype - 1]);
        mEntryPanel.setVisibility(View.GONE);
        mStatusPanel.setVisibility(View.VISIBLE);
    }

    private void indicateError() {
        int[] errorLabels = {R.string.unlock_failed,R.string.nw_subset_unlock_failed,
                             R.string.sp_unlock_failed,R.string.corporate_unlock_failed,
                             R.string.sim_unlock_failed};
        mStatusText.setText(errorLabels[mPersoSubtype - 1]);
        mEntryPanel.setVisibility(View.GONE);
        mStatusPanel.setVisibility(View.VISIBLE);
    }

    private void indicateSuccess() {
        int[] successLabels = {R.string.unlock_success,R.string.nw_subset_unlock_success,
                               R.string.sp_unlock_success,R.string.corporate_unlock_success,
                               R.string.sim_unlock_success};
        mStatusText.setText(successLabels[mPersoSubtype - 1]);
        mEntryPanel.setVisibility(View.GONE);
        mStatusPanel.setVisibility(View.VISIBLE);
    }

    private void hideAlert() {
        mEntryPanel.setVisibility(View.VISIBLE);
        mStatusPanel.setVisibility(View.GONE);
    }

    View.OnClickListener mDismissListener = new View.OnClickListener() {
            public void onClick(View v) {
                if (DBG) log("mDismissListener: skipping depersonalization...");
                dismiss();
            }
        };

    //Sets appropriate title for the Depersonalization Panel.
    private void setPersoPanelTitle() {
        int[] panelTitles = {R.string.label_ndp,R.string.label_nsdp,
                             R.string.label_spdp,R.string.label_cdp,
                             R.string.label_sdp};
        mPersoSubtypeText.setText(panelTitles[mPersoSubtype - 1]);
    }

    //Sends RIL_REQUEST_OEM_HOOK_RAW with Depersonalization information.
    void sendDepersoOemRilRequestRaw(String pin) {
        int requestSize = INT_SIZE + pin.length();
        byte[] request = new byte[mHeaderSize + requestSize + 1];
        byte termChar = '\0';
        ByteBuffer reqBuffer = ByteBuffer.wrap(request);
        reqBuffer.order(ByteOrder.nativeOrder());

        try {
           // Add OEM identifier String
           reqBuffer.put(mOemIdentifier.getBytes());

           // Add Request ID
           reqBuffer.putInt(OEMHOOK_ME_DEPERSONALIZATION);

           // Add Request payload length
           reqBuffer.putInt(requestSize);

           reqBuffer.putInt(mPersoSubtype); // Depersonalization Subtype
           reqBuffer.put(pin.getBytes()); // Depersonalization PIN
           reqBuffer.put(termChar); // Null character indicating end of string
        } catch (Exception e) {
           Log.e(TAG,"Skipping Depersonalization because of exception " + e);
           dismiss();
        }

        mPhone.invokeOemRilRequestRaw(request,
              Message.obtain(mHandler, EVENT_ICC_DEPERSONALIZATION_RESULT));
    }

    private void log(String msg) {
        Log.v(TAG, "[IccNetworkDepersonalizationPanel] " + msg);
    }
}
