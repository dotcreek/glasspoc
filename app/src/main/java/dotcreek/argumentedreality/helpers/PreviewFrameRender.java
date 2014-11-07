package dotcreek.argumentedreality.helpers;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

/**
 * External Class from OpenCV.org for CameraCalibration
 * Edited by Kevin Alfaro for AugmentedReality Magazine
 * 2014
 */

public class PreviewFrameRender extends FrameRender {

    @Override
    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }
}