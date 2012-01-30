/* Copyright (c) 2012, Code Aurora Forum. All rights reserved.
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
 *     * Neither the name of Code Aurora Forum, Inc. nor the names of its
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

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

/**
 *  Provides an interface to interact with IMS DPL to handle the media
 *  part of the video telephony call
 */
public class MediaHandler {
    private static final String TAG = "VideoCallMediaHandler";
    private static SurfaceTexture sSurface;

    private static native int nativeInit();
    private static native void nativeRenderThread();
    private static native void nativeDeInit();
    private static native void nativeHandleRawFrame(byte[] frame);
    private static native int nativeSetSurface (SurfaceTexture st);
    private static native void nativeSetCallMode(int mode);

    private static String libraryName = System.getProperty("ro.vt_ims_library", "vt_jni");
    static {
        System.loadLibrary("vt_jni");
    }

    private static Thread sRenderThread;

    /*
     * Initialize the IMS DPL
     */
    public static void init() {
        Log.d(TAG, "init called");
        if (sRenderThread == null) {
            if (nativeInit() != 0) {
                throw new RuntimeException("Unable to initialize Dpl");
            }
            sRenderThread = new Thread() {
                @Override
                public void run() {
                    nativeRenderThread();
                }
            };
        }
    }

    /*
     * Deinitialize the IMS DPL
     */
    public static void deInit() {
        boolean interrupted = false;
        Log.d(TAG, "deInit called");
        if (sRenderThread != null) {
            nativeDeInit();

            do {
                try {
                    interrupted = false;
                    sRenderThread.join();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while waiting for Render thread to complete");
                    interrupted = true;
                }
            } while(interrupted);
            sRenderThread = null;
        }
    }

    /**
     * Send the camera preview frames to the IMS DPL
     * to be sent to the far end party
     *
     * @param data raw frames from the camera
     */
    public static void sendPreviewFrame(byte[] frame) {
        Log.d(TAG, "handleRawFrame(" + frame + ")");
        nativeHandleRawFrame(frame);
    }

    /**
     * Send the SurfaceTexture to IMS DPL
     *
     * @param st
     */
    public static void setSurface(SurfaceTexture st) {
        Log.d(TAG, "setSurface(" + st + ")");
        sSurface = st;
        nativeSetSurface(st);
    }
}
