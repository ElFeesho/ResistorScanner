package ca.parth.resistordecoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.InstrumentationTestCase;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ResistorImageProcessorTest extends InstrumentationTestCase {

    static
    {
        OpenCVLoader.initDebug();
    }

    public void testTheResistorImageProcessorCanDecodeThe820OhmResistorFromTheGithubReadme() throws IOException {

        Bitmap image = BitmapFactory.decodeStream(getInstrumentation().getContext().getAssets().open("test820resistance.png"));
        Mat mat = new Mat(image.getWidth(), image.getHeight(), CvType.CV_8UC4);

        ByteBuffer buffer = ByteBuffer.allocate(image.getByteCount());

        image.copyPixelsToBuffer(buffer);
        mat.put(0, 0, buffer.array());

        ResistorImageProcessor processor = new ResistorImageProcessor();
        final int[] calculatedResistance = new int[1];
        processor.processFrame(mat, new ResistorImageProcessor.ResistanceCalculatedCallback() {
            @Override
            public void resistanceCalculated(int resistance) {
                calculatedResistance[0] = resistance;
            }
        });

        assertEquals(820, calculatedResistance[0]);
    }
}
