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
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;

public class ResistorDecoderFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2, ResistorImageProcessor.ResistanceCalculatedCallback {

    private BaseLoaderCallback _loaderCallback;

    private ResistorView resistorView;
    private TextView currentResistance;
    private CheckBox enableFlash;
    private ResistorCameraView _resistorCameraView;
    private ResistorImageProcessor _resistorProcessor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _loaderCallback = new BaseLoaderCallback(getActivity()) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                        _resistorCameraView.enableView();
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
        _resistorCameraView = (ResistorCameraView) view.findViewById(R.id.ResistorCameraView);
        _resistorCameraView.setVisibility(SurfaceView.VISIBLE);
        _resistorCameraView.setZoomControl((SeekBar) view.findViewById(R.id.CameraZoomControls));
        _resistorCameraView.setCvCameraViewListener(this);

        _resistorProcessor = new ResistorImageProcessor();

        enableFlash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    _resistorCameraView.enableFlash();
                } else {
                    _resistorCameraView.disableFlash();
                }
            }
        });

    }


    @Override
    public void onPause()
    {
        super.onPause();
        if (_resistorCameraView != null)
            _resistorCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (_resistorCameraView != null)
            _resistorCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return _resistorProcessor.processFrame(inputFrame, this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        _loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
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
