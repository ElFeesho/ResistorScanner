package ca.parth.resistordecoder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.SeekBar;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;

import ca.parth.resistordecoder.camera.CameraConfigStrategy;
import ca.parth.resistordecoder.camera.CameraOpenStrategy;
import ca.parth.resistordecoder.camera.DefaultCameraConfigStrategy;
import ca.parth.resistordecoder.camera.GingerBreadCameraOpenStrategy;

public class ResistorCameraView extends SurfaceView implements Camera.PreviewCallback, SurfaceHolder.Callback {

    public interface CvCameraViewListener {
        /**
         * This method is invoked when camera preview has started. After this method is invoked
         * the frames will start to be delivered to client via the onCameraFrame() callback.
         *
         * @param width  -  the width of the frames that will be delivered
         * @param height - the height of the frames that will be delivered
         */
        public void onCameraViewStarted(int width, int height);

        /**
         * This method is invoked when camera preview has been stopped for some reason.
         * No frames will be delivered via onCameraFrame() callback after this method is called.
         */
        public void onCameraViewStopped();

        /**
         * This method is invoked when delivery of the frame needs to be done.
         * The returned values - is a modified frame which needs to be displayed on the screen.
         * TODO: pass the parameters specifying the format of the frame (BPP, YUV or RGB and etc)
         */
        public Mat onCameraFrame(Mat inputFrame);
    }

    public interface CvCameraViewListener2 {
        /**
         * This method is invoked when camera preview has started. After this method is invoked
         * the frames will start to be delivered to client via the onCameraFrame() callback.
         *
         * @param width  -  the width of the frames that will be delivered
         * @param height - the height of the frames that will be delivered
         */
        void onCameraViewStarted(int width, int height);

        /**
         * This method is invoked when camera preview has been stopped for some reason.
         * No frames will be delivered via onCameraFrame() callback after this method is called.
         */
        void onCameraViewStopped();

        /**
         * This method is invoked when delivery of the frame needs to be done.
         * The returned values - is a modified frame which needs to be displayed on the screen.
         * TODO: pass the parameters specifying the format of the frame (BPP, YUV or RGB and etc)
         */
        Mat onCameraFrame(CvCameraViewFrame inputFrame);
    }

    /**
     * This class interface is abstract representation of single frame from camera for onCameraFrame callback
     * Attention: Do not use objects, that represents this interface out of onCameraFrame callback!
     */
    public interface CvCameraViewFrame {

        /**
         * This method returns RGBA Mat with frame
         */
        Mat rgba();
    }


    private class JavaCameraFrame implements CvCameraViewFrame {
        private Mat mYuvFrameData;
        private Mat mRgba;

        public JavaCameraFrame(Mat Yuv420sp, int width, int height) {
            super();
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
        }

        public Mat rgba() {
            Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            return mRgba;
        }

        public void release() {
            mRgba.release();
        }
    }

    private class CameraWorker implements Runnable {

        public void run() {
            do {
                synchronized (ResistorCameraView.this) {
                    try {
                        ResistorCameraView.this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (!mStopThread) {
                    if (!mFrameChain[mChainIdx].empty())
                        deliverAndDrawFrame(mCameraFrame[mChainIdx]);
                    mChainIdx = 1 - mChainIdx;
                }
            } while (!mStopThread);
            Log.d(TAG, "Finish processing thread");
        }
    }

    public static final int CAMERA_ID_ANY = -1;

    private static final int MAGIC_TEXTURE_ID = 10;
    private static final String TAG = "ResistorCameraView";
    private static final int MAX_UNSPECIFIED = -1;
    private static final int STOPPED = 0;
    private static final int STARTED = 1;
    private final Object mSyncObject = new Object();
    protected Camera camera;
    protected JavaCameraFrame[] mCameraFrame = new JavaCameraFrame[2];
    protected int mFrameWidth;
    protected int mFrameHeight;
    protected int mMaxHeight;
    protected int mMaxWidth;
    protected int mCameraIndex = CAMERA_ID_ANY;
    protected boolean mEnabled;
    private SeekBar _zoomControl;
    private byte[] mBuffer;
    private Mat[] mFrameChain = new Mat[2];
    private int mChainIdx = 0;
    private Thread mThread;
    private boolean mStopThread;
    private SurfaceTexture mSurfaceTexture;
    private int mState = ca.parth.resistordecoder.ResistorCameraView.STOPPED;
    private Bitmap mCacheBitmap;
    private CvCameraViewListener2 mListener;
    private boolean mSurfaceExist;
    private CameraOpenStrategy cameraOpenStrategy;
    private CameraConfigStrategy cameraConfigStrategy;

    public ResistorCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.getHolder().addCallback(this);

        cameraOpenStrategy = new GingerBreadCameraOpenStrategy();
        cameraConfigStrategy = new DefaultCameraConfigStrategy();

        mMaxWidth = MAX_UNSPECIFIED;
        mMaxHeight = MAX_UNSPECIFIED;

        mCameraIndex = CAMERA_ID_ANY;
    }

    public void setZoomControl(SeekBar zoomControl) {
        _zoomControl = zoomControl;
    }

    protected boolean initializeCamera(int width, int height) {
        boolean ret = initializeCameraTwo(width, height);

        Camera.Parameters params = camera.getParameters();

        enableFocus(params);

        if (params.isZoomSupported()) {
            enableZoomControls(params);
        }

        camera.setParameters(params);

        return ret;
    }

    private void enableZoomControls(Camera.Parameters params) {
        final SharedPreferences settings = getContext().getSharedPreferences("ZoomCtl", 0);

        // set zoom level to previously set level if available, otherwise maxZoom
        final int maxZoom = params.getMaxZoom();
        int currentZoom = settings.getInt("ZoomLvl", maxZoom);
        params.setZoom(currentZoom);

        if (_zoomControl == null) return;

        _zoomControl.setMax(maxZoom);
        _zoomControl.setProgress(currentZoom);
        _zoomControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Camera.Parameters params = camera.getParameters();
                params.setZoom(progress);
                camera.setParameters(params);

                if (settings != null) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt("ZoomLvl", progress);
                    editor.apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void enableFocus(Camera.Parameters params) {
        List<String> FocusModes = params.getSupportedFocusModes();
        if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (FocusModes != null && FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
    }

    public void enableFlash() {
        Camera.Parameters parameters = camera.getParameters();
        enableFlash(parameters);
        camera.setParameters(parameters);
    }

    public void disableFlash() {
        Camera.Parameters parameters = camera.getParameters();
        disableFlash(parameters);
        camera.setParameters(parameters);
    }


    private void enableFlash(Camera.Parameters params) {
        List<String> FlashModes = params.getSupportedFlashModes();
        if (FlashModes != null && FlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }
    }

    private void disableFlash(Camera.Parameters params) {
        params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = (int) ((MeasureSpec.getSize(widthMeasureSpec) / 16.0f) * 9);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
    }

    protected boolean initializeCameraTwo(int width, int height) {
        Log.d(TAG, "Initialize java camera");
        boolean result = true;
        synchronized (this) {
            camera = cameraOpenStrategy.openCamera();
            cameraConfigStrategy.configure(camera, width, height);

            Camera.Parameters parameters = camera.getParameters();
            camera.setDisplayOrientation(90);
            camera.setParameters(parameters);

            mFrameWidth = parameters.getPreviewSize().width;
            mFrameHeight = parameters.getPreviewSize().height;


            int size = mFrameWidth * mFrameHeight;
            size = size * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
            mBuffer = new byte[size];

            camera.addCallbackBuffer(mBuffer);
            camera.setPreviewCallbackWithBuffer(this);

            mFrameChain[0] = new Mat(mFrameHeight + (mFrameHeight / 2), mFrameWidth, CvType.CV_8UC1);
            mFrameChain[1] = new Mat(mFrameHeight + (mFrameHeight / 2), mFrameWidth, CvType.CV_8UC1);

            AllocateCache();

            mCameraFrame[0] = new JavaCameraFrame(mFrameChain[0], mFrameWidth, mFrameHeight);
            mCameraFrame[1] = new JavaCameraFrame(mFrameChain[1], mFrameWidth, mFrameHeight);


            try {
                mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
                camera.setPreviewTexture(mSurfaceTexture);
                /* Finally we are ready to start the preview */
                Log.d(TAG, "startPreview");
                camera.startPreview();

            } catch (IOException e) {
                e.printStackTrace();
                result = false;
            }
        }

        return result;
    }

    protected void releaseCamera() {
        synchronized (this) {
            if (camera != null) {
                camera.stopPreview();
                camera.setPreviewCallback(null);

                camera.release();
            }
            camera = null;
            if (mFrameChain != null) {
                mFrameChain[0].release();
                mFrameChain[1].release();
            }
            if (mCameraFrame != null) {
                mCameraFrame[0].release();
                mCameraFrame[1].release();
            }
        }
    }

    protected boolean connectCamera(int width, int height) {

        /* 1. We need to instantiate camera
         * 2. We need to start thread which will be getting frames
         */
        /* First step - initialize camera connection */
        Log.d(TAG, "Connecting to camera");
        if (!initializeCamera(width, height))
            return false;

        /* now we can start update thread */
        Log.d(TAG, "Starting processing thread");
        mStopThread = false;
        mThread = new Thread(new CameraWorker());
        mThread.start();

        return true;
    }

    protected void disconnectCamera() {
        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        Log.d(TAG, "Disconnecting from camera");
        try {
            mStopThread = true;
            Log.d(TAG, "Notify thread");
            synchronized (this) {
                this.notify();
            }
            Log.d(TAG, "Wating for thread");
            if (mThread != null)
                mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mThread = null;
        }

        /* Now release camera */
        releaseCamera();
    }

    public void onPreviewFrame(byte[] frame, Camera arg1) {
        synchronized (this) {
            mFrameChain[1 - mChainIdx].put(0, 0, frame);
            this.notify();
        }
        if (camera != null)
            camera.addCallbackBuffer(mBuffer);
    }

    /**
     * Sets the camera index
     *
     * @param cameraIndex new camera index
     */
    public void setCameraIndex(int cameraIndex) {
        this.mCameraIndex = cameraIndex;
    }

    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        Log.d(TAG, "call surfaceChanged event");
        synchronized (mSyncObject) {
            if (!mSurfaceExist) {
                mSurfaceExist = true;
                checkCurrentState();
            } else {
                /** Surface changed. We need to stop camera and restart with new parameters */
                /* Pretend that old surface has been destroyed */
                mSurfaceExist = false;
                checkCurrentState();
                /* Now use new surface. Say we have it now */
                mSurfaceExist = true;
                checkCurrentState();
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        /* Do nothing. Wait until surfaceChanged delivered */
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (mSyncObject) {
            mSurfaceExist = false;
            checkCurrentState();
        }
    }

    /**
     * This method is provided for clients, so they can enable the camera connection.
     * The actual onCameraViewStarted callback will be delivered only after both this method is called and surface is available
     */
    public void enableView() {
        synchronized (mSyncObject) {
            mEnabled = true;
            checkCurrentState();
        }
    }

    /**
     * This method is provided for clients, so they can disable camera connection and stop
     * the delivery of frames even though the surface view itself is not destroyed and still stays on the scren
     */
    public void disableView() {
        synchronized (mSyncObject) {
            mEnabled = false;
            checkCurrentState();
        }
    }

    /**
     * @param listener
     */

    public void setCvCameraViewListener(CvCameraViewListener2 listener) {
        mListener = listener;
    }

    /**
     * Called when mSyncObject lock is held
     */
    private void checkCurrentState() {
        int targetState;

        if (mEnabled && mSurfaceExist && getVisibility() == VISIBLE) {
            targetState = STARTED;
        } else {
            targetState = STOPPED;
        }

        if (targetState != mState) {
            /* The state change detected. Need to exit the current state and enter target state */
            processExitState(mState);
            mState = targetState;
            processEnterState(mState);
        }
    }

    private void processEnterState(int state) {
        switch (state) {
            case STARTED:
                onEnterStartedState();
                if (mListener != null) {
                    mListener.onCameraViewStarted(mFrameWidth, mFrameHeight);
                }
                break;
            case STOPPED:
                onEnterStoppedState();
                if (mListener != null) {
                    mListener.onCameraViewStopped();
                }
                break;
        }
    }

    private void processExitState(int state) {
        switch (state) {
            case STARTED:
                onExitStartedState();
                break;
            case STOPPED:
                onExitStoppedState();
                break;
        }
    }

    private void onEnterStoppedState() {
        /* nothing to do */
    }

    private void onExitStoppedState() {
        /* nothing to do */
    }

    // NOTE: The order of bitmap constructor and camera connection is important for android 4.1.x
    // Bitmap must be constructed before surface
    private void onEnterStartedState() {
        /* Connect camera */
        if (!connectCamera(getWidth(), getHeight())) {
            AlertDialog ad = new AlertDialog.Builder(getContext()).create();
            ad.setCancelable(false); // This blocks the 'BACK' button
            ad.setMessage("It seems that you device does not support camera (or it is locked). Application will be closed.");
            ad.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    ((Activity) getContext()).finish();
                }
            });
            ad.show();

        }
    }

    private void onExitStartedState() {
        disconnectCamera();
        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
        }
    }

    /**
     * This method shall be called by the subclasses when they have valid
     * object and want it to be delivered to external client (via callback) and
     * then displayed on the screen.
     *
     * @param frame - the current frame to be delivered
     */
    protected void deliverAndDrawFrame(CvCameraViewFrame frame) {
        Mat modified;

        if (mListener != null) {
            modified = mListener.onCameraFrame(frame);
        } else {
            modified = frame.rgba();
        }

        boolean bmpValid = true;
        if (modified != null) {
            try {
                Utils.matToBitmap(modified, mCacheBitmap);
            } catch (Exception e) {
                Log.e(TAG, "Mat type: " + modified);
                Log.e(TAG, "Bitmap type: " + mCacheBitmap.getWidth() + "*" + mCacheBitmap.getHeight());
                Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
                bmpValid = false;
            }
        }

        if (bmpValid && mCacheBitmap != null) {
            Canvas canvas = getHolder().lockCanvas();
            if (canvas != null) {
                canvas.drawBitmap(mCacheBitmap, new Rect(0, 0, mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
                        new Rect((canvas.getWidth() - mCacheBitmap.getWidth()) / 2,
                                (canvas.getHeight() - mCacheBitmap.getHeight()) / 2,
                                (canvas.getWidth() - mCacheBitmap.getWidth()) / 2 + mCacheBitmap.getWidth(),
                                (canvas.getHeight() - mCacheBitmap.getHeight()) / 2 + mCacheBitmap.getHeight()), null);

                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    // NOTE: On Android 4.1.x the function must be called before SurfaceTexture constructor!
    protected void AllocateCache() {
        mCacheBitmap = Bitmap.createBitmap(mFrameWidth, mFrameHeight, Bitmap.Config.ARGB_8888);
    }


}
