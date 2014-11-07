package dotcreek.argumentedreality.helpers;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

/**
 * External Class from OpenCV.org for CameraCalibration
 * Edited by Kevin Alfaro for AugmentedReality Magazine
 * 2014
 */

public class OnCameraFrameRender {

    private FrameRender mFrameRender;

    public OnCameraFrameRender(FrameRender frameRender) {
        mFrameRender = frameRender;
    }

    public Mat render(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return mFrameRender.render(inputFrame);
    }

}
