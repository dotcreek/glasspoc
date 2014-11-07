package dotcreek.argumentedreality.helpers;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

/**
 * External Class from OpenCV.org for CameraCalibration
 * Edited by Kevin Alfaro for AugmentedReality Magazine
 * 2014
 */

abstract class FrameRender {
    protected CameraCalibrator mCalibrator;

    public abstract Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame);
}
