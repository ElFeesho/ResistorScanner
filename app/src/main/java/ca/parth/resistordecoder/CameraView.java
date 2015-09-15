package ca.parth.resistordecoder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.TextureView;

import java.io.IOException;

public class CameraView extends TextureView
{
    private Camera camera;

    public interface Listener
    {
        void photoTaken(Bitmap photo);
    }


    public CameraView(Context context)
    {
        this(context, null, 0);
    }

    public CameraView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        setSurfaceTextureListener(new SurfaceTextureListener()
        {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
            {
                try
                {
                    camera = Camera.open();
                    camera.setPreviewTexture(surface);
                    camera.setDisplayOrientation(90);
                    Camera.Parameters parameters = camera.getParameters();
                    camera.setParameters(parameters);
                    camera.startPreview();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
            {
                camera.stopPreview();
                camera.release();
                return true;
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
            {
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface)
            {
            }
        });
    }

    public void takePhoto(final Listener photoTakenListener)
    {
        camera.autoFocus(new Camera.AutoFocusCallback()
        {
            @Override
            public void onAutoFocus(boolean success, Camera camera)
            {
                camera.takePicture(null, null, new Camera.PictureCallback()
                {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera)
                    {
                        Bitmap photo = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90);
                        Bitmap rotated = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, false);
                        photoTakenListener.photoTaken(rotated);
                    }
                });
            }
        });
    }

}
