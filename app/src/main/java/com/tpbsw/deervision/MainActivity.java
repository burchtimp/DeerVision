package com.tpbsw.deervision;

import android.hardware.SensorEvent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    boolean deerVision = false;
    private Mat masterFrame, processedFrame;
    private int gcCountdown = 1;
    private float movement = 0;


    public void onSwitchVision(View deerView) {
        cameraBridgeViewBase.disableView();
        Button switchView = findViewById(R.id.buttonSwitchView);
        if (deerVision) {
            switchView.setText(R.string.humanVisionString);
            deerVision = false;
        } else {
            switchView.setText(R.string.deerVisionString);
            deerVision = true;
        }
        cameraBridgeViewBase.enableView();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraBridgeViewBase = (JavaCameraView) findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);


        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch (status) {
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

        MovementDetector.getInstance().addListener(new MovementDetector.Listener() {

            @Override
            public void onMotionDetected(SensorEvent event, float acceleration) {
                Log.d("INFO", "Acceleration: " + acceleration);
                movement = acceleration;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.about_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        Toast.makeText(this, "Selected Item" + item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()) {
            case R.id.aboutMenuItem:
                new LibsBuilder()
                        .withFields(R.string.class.getFields())
                        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                        .withActivityTitle(getResources().getString(R.string.open_source_libs))
                        .withLicenseShown(true)
                        .withAboutIconShown(true)
                        .withAboutVersionShown(true)
                        .start(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat frame = inputFrame.rgba();
        if (!deerVision) {
            return frame;
        }
        if (gcCountdown++ % 100 == 0) {
            System.gc();
            System.runFinalization();
        }

        return doDeerVisionProcessing(frame);
    }

    private Mat doDeerVisionProcessing(Mat frame) {
        // simulate ungulate protanopia
        ProtanopiaFilter.processImage(frame);

        // simulate 20/40 vision
        Imgproc.GaussianBlur(frame, frame, new Size(13, 13), 0);
        processedFrame.release();
        processedFrame = frame.clone();
        if (movement < .25) {
            // simulate enhanced movement detection
            Mat diffFrame = frame.clone();
            if (masterFrame.width() != frame.width() || masterFrame.height() != frame.height() || masterFrame.type() != frame.type()) {
                masterFrame.release();
                masterFrame = frame.clone();
            }
            Core.absdiff(frame, masterFrame, diffFrame);

            Mat thresholdFrame = frame.clone();
            Imgproc.threshold(diffFrame, thresholdFrame, 75, 255, Imgproc.THRESH_BINARY);

            diffFrame.release();
            masterFrame.release();
            masterFrame = frame;

            processedFrame.setTo(new Scalar(255, 103, 0), thresholdFrame);
            thresholdFrame.release();
        }
        return processedFrame;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        createFrames(width, height);
    }

    private void createFrames(int width, int height) {
        masterFrame = new Mat(width, height, CvType.CV_8UC1, Scalar.all(0));
        processedFrame = new Mat(width, height, CvType.CV_8UC1, Scalar.all(0));
    }

    @Override
    public void onCameraViewStopped() {
        resetFrames();
    }

    private void resetFrames() {
        masterFrame.release();
        processedFrame.release();
        System.gc();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(), "There's a problem, yo!", Toast.LENGTH_SHORT).show();
        } else {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }
        MovementDetector.getInstance().start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
        MovementDetector.getInstance().stop();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }
}
