package dotcreek.argumentedreality.helpers;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

/**
 * External Class from OpenCV.org for CameraCalibration
 * Edited by Kevin Alfaro for AugmentedReality Magazine
 * 2014
 */

public class CalibrationFrameRender extends FrameRender {

    public CalibrationFrameRender(CameraCalibrator calibrator) {
        mCalibrator = calibrator;
    }

    @Override
    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgbaFrame = inputFrame.rgba();
        Mat grayFrame = inputFrame.gray();
        mCalibrator.processFrame(grayFrame, rgbaFrame);

        return rgbaFrame;
    }
}
