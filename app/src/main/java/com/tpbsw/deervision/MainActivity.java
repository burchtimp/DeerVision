package com.tpbsw.deervision;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    boolean deerVision = false;
    private Mat masterFrame, processedFrame;


    public void onSwitchVision(View deerView) {
        cameraBridgeViewBase.disableView();
        Button switchView = (Button) findViewById(R.id.buttonSwitchView);
        if (deerVision) {
            switchView.setText(R.string.deerVisionString);
            deerVision = false;
        } else {
            switchView.setText(R.string.humanVisionString);
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
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat frame = inputFrame.rgba();
        if (!deerVision) {
            return frame;
        }

        return doDeerVisionProcessing(frame);
    }

    private Mat doDeerVisionProcessing(Mat frame) {
        // simulate ungulate protanopia
        ProtanopiaFilter.processImage(frame);

        Imgproc.GaussianBlur(frame, frame, new Size(13, 13), 0);
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

        processedFrame.release();

        processedFrame = frame.clone();

        processedFrame.setTo(new Scalar(255, 103, 0), thresholdFrame);
        thresholdFrame.release();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }
}
