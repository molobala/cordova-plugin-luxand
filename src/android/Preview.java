package com.luxand.dsi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.util.List;

class Preview extends SurfaceView implements SurfaceHolder.Callback {
    Context mContext;
    SurfaceHolder mHolder;
    Camera mCamera;
    ProcessImageAndDrawResults mDraw;
    boolean mFinished;

    Preview(Context context, ProcessImageAndDrawResults draw) {
        super(context);
        mContext = context;
        mDraw = draw;

        //Install a SurfaceHolder.Callback so we get notified when the underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    //SurfaceView callback
    public void surfaceCreated(SurfaceHolder holder) {
        mFinished = false;

        // Find the ID of the camera
        int cameraId = 0;
        boolean frontCameraFound = false;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            //if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                frontCameraFound = true;
            }
        }

        if (frontCameraFound) {
            mCamera = Camera.open(cameraId);
        } else {
            mCamera = Camera.open();
        }

        try {
            mCamera.setPreviewDisplay(holder);

            // Preview callback used whenever new viewfinder frame is available
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if ( (mDraw == null) || mFinished )
                        return;

                    if (mDraw.mYUVData == null) {
                        // Initialize the draw-on-top companion
                        Camera.Parameters params = camera.getParameters();
                        mDraw.mImageWidth = params.getPreviewSize().width;
                        mDraw.mImageHeight = params.getPreviewSize().height;
                        mDraw.mRGBData = new byte[3 * mDraw.mImageWidth * mDraw.mImageHeight];
                        mDraw.mYUVData = new byte[data.length];
                    }

                    // Pass YUV data to draw-on-top companion
                    System.arraycopy(data, 0, mDraw.mYUVData, 0, data.length);
                    mDraw.invalidate();
                }
            });
        }
        catch (Exception exception) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage("Cannot open camera" )
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    })
                    .show();
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
        }
    }

    //SurfaceView callback
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        mFinished = true;
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    //SurfaceView callback, configuring camera
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mCamera == null) return;

        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();

        //Keep uncommented to work correctly on phones:
        //This is an undocumented although widely known feature
        /**/
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            parameters.set("orientation", "portrait");
            mCamera.setDisplayOrientation(90); // For Android 2.2 and above
            mDraw.rotated = true;
        } else {
            parameters.set("orientation", "landscape");
            mCamera.setDisplayOrientation(0); // For Android 2.2 and above
        }
        /**/

        // choose preview size closer to 640x480 for optimal performance
        List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
        int width = 0;
        int height = 0;
        for (Camera.Size s: supportedSizes) {
            if ((width - 640)*(width - 640) + (height - 480)*(height - 480) >
                    (s.width - 640)*(s.width - 640) + (s.height - 480)*(s.height - 480)) {
                width = s.width;
                height = s.height;
            }
        }

        //try to set preferred parameters
        try {
            if (width*height > 0) {
                parameters.setPreviewSize(width, height);
            }
            //parameters.setPreviewFrameRate(10);
            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_PORTRAIT);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            mCamera.setParameters(parameters);
        } catch (Exception ex) {
        }
        mCamera.startPreview();

        parameters = mCamera.getParameters();
        Camera.Size previewSize = parameters.getPreviewSize();
        makeResizeForCameraAspect(1.0f / ((1.0f * previewSize.width) / previewSize.height));
    }

    private void makeResizeForCameraAspect(float cameraAspectRatio){
        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        int matchParentWidth = this.getWidth();
        int newHeight = (int)(matchParentWidth/cameraAspectRatio);
        if (newHeight != layoutParams.height) {
            layoutParams.height = newHeight;
            layoutParams.width = matchParentWidth;
            this.setLayoutParams(layoutParams);
            this.invalidate();
        }
    }
} // end of Preview class
