package ca.parth.resistordecoder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;


public class MainActivity extends AppCompatActivity {

    static {
        OpenCVLoader.initDebug();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportFragmentManager().findFragmentById(R.id.container) == null)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new ResistorDecoderFragment()).commit();
        }

        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        if(!settings.getBoolean("shownInstructions", false))
        {
            new FirstTimeLaunchAlertDialogFragment().show(getSupportFragmentManager(), "firstTimeLaunch");
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("shownInstructions", true);
            editor.apply();
        }
    }

}
