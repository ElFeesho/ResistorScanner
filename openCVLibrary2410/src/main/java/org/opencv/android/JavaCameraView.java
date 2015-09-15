package org.opencv.android;

import android.content.Context;
import android.util.AttributeSet;

/**
 * This class is an implementation of the Bridge View between OpenCV and Java Camera.
 * This class relays on the functionality available in base class and only implements
 * required functions:
 * connectCamera - opens Java camera and sets the PreviewCallback to be delivered.
 * disconnectCamera - closes the camera and stops preview.
 * When frame is delivered via callback from Camera - it processed via OpenCV to be
 * converted to RGBA32 and then passed to the external callback for modifications if required.
 */
public class JavaCameraView {

    public JavaCameraView(Context context, int cameraId) {
    }

    public JavaCameraView(Context context, AttributeSet attrs) {
    }



}
