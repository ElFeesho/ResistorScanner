package ca.parth.resistordecoder.camera;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;

import org.opencv.core.Size;

import java.util.List;

public class DefaultCameraConfigStrategy implements CameraConfigStrategy {
    private static class JavaCameraSizeAccessor {

        public int getWidth(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.width;
        }

        public int getHeight(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.height;
        }
    }

    @Override
    public void configure(Camera camera, int width, int height) {
        try {
            Camera.Parameters params = camera.getParameters();
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();

            if (sizes != null) {
                /* Select the size that fits surface considering maximum size allowed */
                Size frameSize = calculateCameraFrameSize(sizes, width, height);

                params.setPreviewFormat(ImageFormat.NV21);
                params.setPreviewSize((int)frameSize.width, (int)frameSize.height);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH && !Build.MODEL.equals("GT-I9100")) {
                    params.setRecordingHint(true);
                }

                camera.setParameters(params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This helper method can be called by subclasses to select camera preview size.
     * It goes over the list of the supported preview sizes and selects the maximum one which
     * fits both values set via setMaxFrameSize() and surface frame allocated for this view
     *
     * @param supportedSizes
     * @param surfaceWidth
     * @param surfaceHeight
     * @return optimal frame size
     */
    private Size calculateCameraFrameSize(List<?> supportedSizes, int surfaceWidth, int surfaceHeight) {
        int calcWidth = 0;
        int calcHeight = 0;
        JavaCameraSizeAccessor accessor = new JavaCameraSizeAccessor();

        int maxAllowedWidth = surfaceWidth;
        int maxAllowedHeight = surfaceHeight;

        for (Object size : supportedSizes) {
            int width = accessor.getWidth(size);
            int height = accessor.getHeight(size);

            if (width <= maxAllowedWidth && height <= maxAllowedHeight) {
                if (width >= calcWidth && height >= calcHeight) {
                    calcWidth = width;
                    calcHeight = height;
                }
            }
        }

        return new Size(calcWidth, calcHeight);
    }
}