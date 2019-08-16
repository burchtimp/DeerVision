package com.tpbsw.deervision;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.ArrayList;

public class ProtanopiaFilter {
    private static final double[][] constants = {{0.56667, 0.43333,  0.0},  // Red Constants
                                                {0.55833, 0.44167,  0.0},// Green Constants
                                                {0.0, 0.24167, 0.75833}}; // Blue Constants
    private static final int R = 0;
    private static final int G = 1;
    private static final int B = 2;

    public static void processImage(Mat image) {
        Mat outMat = new Mat(3,3, CvType.CV_8UC1);
        ArrayList<Mat> bgr = new ArrayList<>();
        ArrayList<Mat> bgrOut = new ArrayList<>();
        Core.split(image, bgr);
        Mat tmp = new Mat (image.size(), image.type());
        Mat tmp2 = new Mat (image.size(), image.type());
        Mat tmp3 = new Mat (image.size(), image.type());
        for (double [] c: constants) {
            tmp = clearMat(tmp);
            tmp2 = clearMat(tmp2);
            tmp3 = clearMat(tmp3);
            Core.multiply(bgr.get(B), new Scalar(c[B]), tmp);
            Core.multiply(bgr.get(G), new Scalar(c[G]), tmp2);
            Core.multiply(bgr.get(R), new Scalar(c[R]), tmp3);
            outMat = clearMat(outMat);
            Core.add(tmp, tmp2, outMat);
            Core.add(outMat, tmp3, outMat);
            bgrOut.add(outMat);
        }

        Core.merge(bgrOut, image);
        for(Mat m: bgr) m.release();
        for(Mat m: bgrOut) m.release();
        outMat.release();
        tmp.release();
        tmp2.release();
        tmp3.release();
    }
    private static Mat clearMat(Mat mat) {
        mat = Mat.zeros(mat.size(), mat.type());
        return mat;
    }
}
