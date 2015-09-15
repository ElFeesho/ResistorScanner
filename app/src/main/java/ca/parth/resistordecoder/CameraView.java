package ca.parth.resistordecoder;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.TextureView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.List;

import ca.parth.resistordecoder.camera.CameraFrame;

public class CameraView extends TextureView implements Camera.PreviewCallback {

    public interface CameraFrameAvailableListener {
        void onCameraFrame(CameraFrame inputFrame);
    }

    private Camera camera;
    private CameraFrame[] mCameraFrame = new CameraFrame[2];
    private byte[] mBuffer;
    private int chainIndex = 0;
    private Mat[] frameChain = new Mat[2];
    private CameraFrameAvailableListener cameraFrameAvailableListener;

    public CameraView(Context context) {
        this(context, null, 0);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSurfaceTextureListener(new SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                camera = Camera.open();
                try {
                    camera.setPreviewTexture(surface);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                camera.setDisplayOrientation(90);
                Camera.Parameters parameters = camera.getParameters();

                int size = parameters.getPreviewSize().width * parameters.getPreviewSize().height;
                size = size * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
                mBuffer = new byte[size];

                camera.addCallbackBuffer(mBuffer);
                camera.setPreviewCallbackWithBuffer(CameraView.this);

                frameChain[0] = new Mat(parameters.getPreviewSize().height + (parameters.getPreviewSize().height / 2), parameters.getPreviewSize().width, CvType.CV_8UC1);
                frameChain[1] = new Mat(parameters.getPreviewSize().height + (parameters.getPreviewSize().height / 2), parameters.getPreviewSize().width, CvType.CV_8UC1);

                mCameraFrame[0] = new CameraFrame(frameChain[0]);
                mCameraFrame[1] = new CameraFrame(frameChain[1]);

                enableFocus(parameters);
                camera.setParameters(parameters);
                camera.startPreview();

                Matrix transformation = new Matrix();
                transformation.setScale(1, (float) 2.5);
                setTransform(transformation);

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                camera.stopPreview();
                camera.release();
                return true;
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
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

    public void setCameraFrameAvailableListener(CameraFrameAvailableListener cameraFrameAvailableListener) {
        this.cameraFrameAvailableListener = cameraFrameAvailableListener;
    }

    @Override
    public void onPreviewFrame(byte[] frame, Camera camera) {
        frameChain[1 - chainIndex].put(0, 0, frame);
        if (!frameChain[chainIndex].empty()) {
            deliverFrame(mCameraFrame[chainIndex]);
        }
        chainIndex = 1 - chainIndex;
        if (camera != null) {
            camera.addCallbackBuffer(mBuffer);
        }
    }

    protected void deliverFrame(CameraFrame frame) {
        if (cameraFrameAvailableListener != null) {
            cameraFrameAvailableListener.onCameraFrame(frame);
        }
    }
}
