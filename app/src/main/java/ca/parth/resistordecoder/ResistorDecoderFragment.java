package ca.parth.resistordecoder;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;

public class ResistorDecoderFragment extends Fragment implements ResistorCameraView.CvCameraViewListener2, ResistorImageProcessor.ResistanceCalculatedCallback {

    private BaseLoaderCallback loaderCallback;

    private ResistorView resistorView;
    private TextView currentResistance;
    private CheckBox enableFlash;
    private ResistorCameraView resistorCameraView;
    private ResistorImageProcessor resistorProcessor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loaderCallback = new BaseLoaderCallback(getActivity()) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                        resistorCameraView.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_resistor_decoder, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        resistorView = (ResistorView) view.findViewById(R.id.resistorView);
        currentResistance = (TextView) view.findViewById(R.id.currentResistance);
        enableFlash = (CheckBox) view.findViewById(R.id.flash);
        resistorCameraView = (ResistorCameraView) view.findViewById(R.id.ResistorCameraView);
        resistorCameraView.setVisibility(SurfaceView.VISIBLE);
        resistorCameraView.setZoomControl((SeekBar) view.findViewById(R.id.CameraZoomControls));
        resistorCameraView.setCvCameraViewListener(this);

        resistorProcessor = new ResistorImageProcessor();

        enableFlash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    resistorCameraView.enableFlash();
                } else {
                    resistorCameraView.disableFlash();
                }
            }
        });

    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (resistorCameraView != null)
            resistorCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (resistorCameraView != null)
            resistorCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(ResistorCameraView.CvCameraViewFrame inputFrame) {
        return resistorProcessor.processFrame(inputFrame, this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    public void resistanceCalculated(final int resistance) {
        final String valueStr;
        if (resistance >= 1e3 && resistance < 1e6) {
            valueStr = String.valueOf(resistance / 1e3) + " kΩ";
        } else if (resistance >= 1e6) {
            valueStr = String.valueOf(resistance / 1e6) + "MΩ";
        } else {
            valueStr = String.valueOf(resistance) + " Ω";
        }

        currentResistance.post(new Runnable() {
            public void run() {
                currentResistance.setText(valueStr);
                resistorView.setResistance(resistance);
            }
        });
    }
}
