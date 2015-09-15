package ca.parth.resistordecoder.camera;

import android.hardware.Camera;
import android.util.Log;

public class GingerBreadCameraOpenStrategy implements CameraOpenStrategy {

    public static final String TAG = "CameraOpenStrategy";

    @Override
    public Camera openCamera() {
        Camera result = null;

        try {
            result = Camera.open();
        } catch (Exception e) {
            Log.e(TAG, "Camera is not available (in use or does not exist): " + e.getLocalizedMessage());
        }

        if (result == null)
        {
            for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(camIdx) + ")");
                try {
                    result = Camera.open(camIdx);
                    break;
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera #" + camIdx + "failed to open: " + e.getLocalizedMessage());
                }

            }
        }

        return result;
    }
}
