package dotcreek.argumentedreality.logic;

import android.app.Activity;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

import dotcreek.argumentedreality.helpers.CalibrationResult;
import dotcreek.argumentedreality.helpers.CameraCalibrator;

/**
 * Class that get and save camera parameters from App Preferences.
 * By Kevin Alfaro for AugmentedReality Magazine
 * 2014
 */

public class CameraParameters {

    //Matrices de calibracion
    private Mat mIntrinsics;
    private MatOfDouble mDistortion;

    /* Funcion que obtiene los valores de calibracion almacenados en los SharedPreferences */
    public CameraParameters(int width, int height, Activity activity){

        CameraCalibrator mCalibrator = new CameraCalibrator(width, height);
        if (CalibrationResult.tryLoad(activity, mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients())) {
            mCalibrator.setCalibrated();
            mIntrinsics = new Mat();
            mDistortion = new MatOfDouble();
            mCalibrator.getCameraMatrix().copyTo(mIntrinsics);
            mCalibrator.getDistortionCoefficients().copyTo(mDistortion);
        }


    }

    public Mat getIntrinsics(){
        return mIntrinsics;

    }

    public MatOfDouble getDistortion(){

        return mDistortion;
    }
}
