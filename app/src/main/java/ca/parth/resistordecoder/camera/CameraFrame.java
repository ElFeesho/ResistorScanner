package ca.parth.resistordecoder.camera;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class CameraFrame {
    private Mat mYuvFrameData;
    private Mat mRgba;

    public CameraFrame(Mat Yuv420sp) {
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
