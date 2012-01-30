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

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.phone.CameraHandler.CameraState;

import java.io.IOException;
import java.util.List;

/**
 * Helper class to initialize and run the InCallScreen's "Video Call" UI.
 */
public class VideoCallPanel extends RelativeLayout implements TextureView.SurfaceTextureListener, View.OnClickListener {
    private static final String LOG_TAG = "VideoCallPanel";
    private static final boolean DBG = true;

    private Context mContext;
    private VideoCallManager mVideoCallManager;

    // "Video Call" UI elements and state
    private ViewGroup mVideoCallPanel;
    private ZoomControlBar mZoomControl;
    private TextureView mFarEndView;
    private TextureView mCameraPreview;
    private SurfaceTexture mCameraSurface;
    private SurfaceTexture mFarEndSurface;
    private ImageView mCameraPicker;

    // Camera related
    private Parameters mParameters;
    private int mZoomMax;
    private int mZoomValue;  // The current zoom value
    Size mPreviewSize;

    // Multiple cameras support
    private int mNumberOfCameras;
    private int mFrontCameraId;
    private int mBackCameraId;
    private int mCameraId;

    // Property used to indicate that the IMS DPL in running in loopback mode
    private boolean mIsImsLoopback = false;

    /**
    * This class implements the zoom listener for zoomControl
    */
    private class ZoomChangeListener implements ZoomControl.OnZoomChangedListener {
        @Override
        public void onZoomValueChanged(int index) {
            VideoCallPanel.this.onZoomValueChanged(index);
        }
    }

    public VideoCallPanel(Context context) {
        super(context);
        mContext = context;
    }

    public VideoCallPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public VideoCallPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    /**
     * Finalize view from inflation.
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (DBG) log("onFinishInflate(this = " + this + ")...");

        // Check the IMS loopback property
        int property = SystemProperties.getInt("net.lte.VT_LOOPBACK_ENABLE", 0);
        mIsImsLoopback = (property == 1) ? true : false;
        if (DBG) log("Is IMS running in loopback mode: " + mIsImsLoopback);

        // Get UI widgets
        mVideoCallPanel = (ViewGroup) findViewById(R.id.videoCallPanel);
        mZoomControl = (ZoomControlBar) findViewById(R.id.zoom_control);
        mFarEndView = (TextureView) findViewById(R.id.video_view);
        mCameraPreview = (TextureView) findViewById(R.id.camera_view);
        mCameraPicker = (ImageView) findViewById(R.id.camera_picker);

        // Set listeners
        mCameraPreview.setSurfaceTextureListener(this);
        mFarEndView.setSurfaceTextureListener(this);
        mCameraPicker.setOnClickListener(this);

        // Get the camera IDs for front and back cameras
        mVideoCallManager = VideoCallManager.getInstance((Activity)mContext);
        mBackCameraId = mVideoCallManager.getBackCameraId();
        mFrontCameraId = mVideoCallManager.getFrontCameraId();

        // Check if camera supports dual cameras
        mNumberOfCameras = mVideoCallManager.getNumberOfCameras();
        if (mNumberOfCameras > 1) {
            mCameraPicker.setVisibility(View.VISIBLE);
        } else {
            mCameraPicker.setVisibility(View.GONE);
        }
    }

    /**
     * Call is either is either being originated or an MT call is received.
     */
    public void onCallInitiating() {
        if (DBG) log("onCallInitiating");

        // Reset camera to front camera
        mCameraId = mFrontCameraId;

        // Initialize DPL
        mVideoCallManager.mediaInit();
    }

    /**
     * Call is disconnected so reset all the parameter/settings that are
     * required to be set to specific values on start of every video call
     */
    public void onCallDisconnect() {
        // Set the surface to null so IMS DPL can release the resources
        if (DBG) log("onCallDisconnect");

        // Deinitialize DPL
        // This code needs to be called when all the LTE based calls are done
        mVideoCallManager.mediaDeInit();
    }

    /**
     * Called during layout when the size of the view has changed. This method
     * store the VideoCallPanel size to be later used to resize the camera
     * preview accordingly
     */
    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        if (DBG) log("onSizeChanged");
        if (DBG) log("Video Panel width:" + xNew + ", height:" + yNew);

        // Resize preview window if the size of the view changed
        resizeCameraPreview(yNew);
        resizeFarEndView(xNew, yNew);
    }

    /**
     * Called when the InCallScreen activity is being paused. This method hides
     * the VideoCallPanel so that other activities can use the camera at this
     * time.
     */
    public void onPause() {
        if (DBG) log("onPause");
        mVideoCallPanel.setVisibility(View.GONE);
    }

    /**
     * This method opens the camera and starts the camera preview
     */
    private void initializeCamera() {
        if (DBG) log("Initializing camera");

        // Open camera if not already open
        if (false == openCamera(mCameraId)) {
            return;
        }
        initializeZoom();

        // Start camera preview
        startPreview();
    }

    /**
     * This method crates the camera object if camera is not disabled
     *
     * @param cameraId ID of the front or the back camera
     * @return Camera instance on success, null otherwise
     */
    private boolean openCamera(int cameraId) {
        boolean result = false;

        try {
            return mVideoCallManager.openCamera(cameraId);
        } catch (Exception e) {
            loge("Failed to open camera device, error " + e.toString());
            return result;
        }
    }

    /**
     * This method starts the camera preview
     */
    private void startPreview() {
        try {
            mCameraPreview.setVisibility(View.VISIBLE);
            mVideoCallManager.startCameraPreview(mCameraSurface);
        } catch (IOException ioe) {
            closeCamera();
            loge("Exception while setting preview texture, " + ioe.toString());
        }
    }

    /**
     * This method disconnect and releases the camera
     */
    private void closeCamera() {
            mVideoCallManager.closeCamera();
    }

    /**
     * This method stops the camera preview
     */
    private void stopPreview() {
        mCameraPreview.setVisibility(View.GONE);
        mVideoCallManager.stopCameraPreview();
    }

    /* Implementation of listeners */

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (surface.equals(mCameraPreview.getSurfaceTexture())) {
            if (DBG) log("Camera surface texture created");
            mCameraSurface = surface;
            if (mVideoCallManager.getCameraState() == CameraState.CAMERA_CLOSED) {
                initializeCamera();
            } else {
                // Set preview display if the surface is being created and preview
                // was already started. That means preview display was set to null
                // and we need to set it now.
                mVideoCallManager.setDisplay(mCameraSurface);
            }
        } else if (surface.equals(mFarEndView.getSurfaceTexture())) {
            if (DBG) log("Video surface texture created");
            mFarEndSurface = surface;
            mVideoCallManager.setFarEndSurface(mFarEndSurface);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (surface.equals(mCameraPreview.getSurfaceTexture())) {
            if (DBG) log("CameraPreview surface texture destroyed");
            stopPreview();
            closeCamera();
            mCameraSurface = null;
        } else if (surface.equals(mFarEndView.getSurfaceTexture())) {
            if (DBG) log("FarEndView surface texture destroyed");
            mFarEndSurface = null;
            mVideoCallManager.setFarEndSurface(null);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored camera does all the work for us
    }

    /**
     * This method is called when the visibility of the VideoCallPanel is changed
     */
    @Override
    protected void onVisibilityChanged (View changedView, int visibility) {
        if (changedView != this || mVideoCallManager == null) {
            return;
        }

        switch(visibility)
        {
            case View.INVISIBLE:
            case View.GONE:
                if (DBG) log("VideoCallPanel View is GONE or INVISIBLE");
                // Stop the preview and close the camera now because other
                // activities may need to use it
                if (mVideoCallManager.getCameraState() != CameraState.CAMERA_CLOSED) {
                    stopPreview();
                    closeCamera();
                }
                break;
            case View.VISIBLE:
                if (DBG) log("VideoCallPanel View is VISIBLE");
                if (mVideoCallManager.getCameraState() == CameraState.CAMERA_CLOSED) {
                    initializeCamera();
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int direction =  mVideoCallManager.getCameraDirection();

        // Switch the camera front/back/off
        // The state machine is as follows
        // front --> back --> stop preview --> front...
        switch(direction) {
            case -1:
                switchCamera(mFrontCameraId);
                break;
            case Camera.CameraInfo.CAMERA_FACING_FRONT:
                switchCamera(mBackCameraId);
                break;
            case Camera.CameraInfo.CAMERA_FACING_BACK:
                switchCamera(-1);
                break;
        }
    }

    /**
     * This method get the zoom related parameters from the camera and
     * initialized the zoom control
     */
    private void initializeZoom() {
        // Get the parameter to make sure we have the up-to-date zoom value.
        mParameters = mVideoCallManager.getCameraParameters();
        if (!mParameters.isZoomSupported()) {
            mZoomControl.setVisibility(View.GONE); // Disable ZoomControl
            return;
        }

        mZoomControl.setVisibility(View.VISIBLE); // Enable ZoomControl
        mZoomMax = mParameters.getMaxZoom();
        // Currently we use immediate zoom for fast zooming to get better UX and
        // there is no plan to take advantage of the smooth zoom.
        mZoomControl.setZoomMax(mZoomMax);
        mZoomControl.setZoomIndex(mParameters.getZoom());
        mZoomControl.setOnZoomChangeListener(new ZoomChangeListener());
    }

    /**
     * This method gets called when the zoom control reports that the zoom value
     * has changed. This method sets the camera zoom value accordingly.
     *
     * @param index
     */
    private void onZoomValueChanged(int index) {
        mZoomValue = index;

        // Set zoom
        if (mParameters.isZoomSupported()) {
            mParameters.setZoom(mZoomValue);
            mVideoCallManager.setCameraParameters(mParameters);
        }
    }

    /**
     * This method resizes the camera preview based on the aspect ratio
     * supported by camera and the size of VideoCallPanel
     *
     * @param targetSize
     */
    private void resizeCameraPreview(int targetSize) {
        if (DBG) log("resizeCameraPreview");

        // For now, set the preview size to be 1/4th of the VideoCallPanel
        mPreviewSize = mVideoCallManager.getCameraPreviewSize(targetSize/4, true);
        if (mPreviewSize != null) {
            log("Camera view width:" + mPreviewSize.width + ", height:" + mPreviewSize.height);
            ViewGroup.LayoutParams cameraPreivewLp = mCameraPreview.getLayoutParams();
            cameraPreivewLp.height = mPreviewSize.height;
            cameraPreivewLp.width = mPreviewSize.width;
            mCameraPreview.setLayoutParams(cameraPreivewLp);

            // TODO: The camerea preview shoudl be set to the negotiated width and
            // height during a SIP negotiation and not the size of the camera view
            // Set the dimensions for camera preview
            if (mIsImsLoopback) {
                // In loopback mode the IMS is hard coded to render the camera frames
                // of only the size 176x144 on the far end surface
                mParameters.setPreviewSize(176, 144);
            } else {
                mParameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            }
            mVideoCallManager.setCameraParameters(mParameters);
        }
    }

    /**
     * This method resizes the far end view based on the negotiate width and
     * height of the media by IMS DPL and the size of VideoCallPanel
     *
     * @param targetWidth
     * @param targetHeight
     */
    private void resizeFarEndView(int targetWidth, int targetHeight) {
        if (DBG) log("resizeFarEndView");

        log("Far end view width:" + targetWidth + ", height:" + targetHeight);
        ViewGroup.LayoutParams farEndViewLp = mFarEndView.getLayoutParams();
        farEndViewLp.height = targetHeight;
        farEndViewLp.width = targetWidth;
        mFarEndView.setLayoutParams(farEndViewLp);
    }

    /**
     * This method switches the camera to front/back or off
     * @param cameraId
     */
    private void switchCamera(int cameraId) {
        // Change the camera Id
        mCameraId = cameraId;

        // Stop camera preview if already running
        if (mVideoCallManager.getCameraState() != CameraState.CAMERA_CLOSED) {
            stopPreview();
            closeCamera();
        }

        // Restart camera if camera doesn't need to stay off
        if (cameraId != -1) {
            initializeCamera();
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Log.e(LOG_TAG, msg);
    }
}
