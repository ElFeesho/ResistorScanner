package ca.parth.resistordecoder.camera;

import android.hardware.Camera;

public interface CameraConfigStrategy {
    void configure(Camera camera, int width, int height);
}
