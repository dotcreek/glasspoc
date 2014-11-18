package dotcreek.ar_magazine.managers;

import android.app.Activity;
import android.util.DisplayMetrics;

import com.qualcomm.vuforia.CameraCalibration;
import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.Matrix44F;
import com.qualcomm.vuforia.PIXEL_FORMAT;
import com.qualcomm.vuforia.Renderer;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.Vec2I;
import com.qualcomm.vuforia.VideoBackgroundConfig;
import com.qualcomm.vuforia.VideoMode;
import com.qualcomm.vuforia.Vuforia;

import dotcreek.ar_magazine.interfaces.ApplicationControl;

/**
 * DotCreek
 * Augmented Reality Magazine For Glass
 * Edited by Kevin on november 2014
 *
 * CameraManager: This class configure the google glass camera. Also make the camera calibration for 3D rendering
 * PD: This class contains majority external functions, please avoid to edit it or make a backup a before edition.
 */
public class CameraManager {

    // Holds the camera configuration to use upon resuming
    private int cameraDevice;
    private boolean isCameraRunning = false;
    // Display size of the device:
    private int mScreenWidth = 0;
    private int mScreenHeight = 0;

    private Matrix44F projectionMatrix;

    private Activity mainActivity;
    private ApplicationControl mainManagerControl;

    public CameraManager(Activity activity,ApplicationControl control){
        mainManagerControl = control;
        mainActivity = activity;
    }

    public void startCamera(){


        if(isCameraRunning)
        {
            //Camera already running, unable to open again

        }
        cameraDevice = CameraDevice.CAMERA.CAMERA_DEFAULT;

        if (!CameraDevice.getInstance().init(cameraDevice))
        {
           //Unable to open camera device: "

        }

        configureVideoBackground();

        if (!CameraDevice.getInstance().selectVideoMode(
                CameraDevice.MODE.MODE_DEFAULT))
        {
            //Unable to set video mode

        }

        if (!CameraDevice.getInstance().start())
        {
            //Unable to start camera device:

        }

        Vuforia.setFrameFormat(PIXEL_FORMAT.RGB565, true);

        setProjectionMatrix();

        mainManagerControl.doStartTrackers();

        isCameraRunning = true;
    }

    // Configures the video mode and sets offsets for the camera's image (External function)
    private void configureVideoBackground()
    {

        // Query display dimensions:
        DisplayMetrics metrics = new DisplayMetrics();
        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;

        CameraDevice cameraDevice = CameraDevice.getInstance();
        VideoMode vm = cameraDevice.getVideoMode(CameraDevice.MODE.MODE_DEFAULT);

        VideoBackgroundConfig config = new VideoBackgroundConfig();
        config.setEnabled(true);
        config.setSynchronous(true);
        config.setPosition(new Vec2I(0, 0));

        int xSize = 0, ySize = 0;

        xSize = mScreenWidth;
        ySize = (int) (vm.getHeight() * (mScreenWidth / (float) vm
                .getWidth()));

        if (ySize < mScreenHeight)
        {
            xSize = (int) (mScreenHeight * (vm.getWidth() / (float) vm
                    .getHeight()));
            ySize = mScreenHeight;
        }


        config.setSize(new Vec2I(xSize, ySize));

        Renderer.getInstance().setVideoBackgroundConfig(config);

    }

    public void stopCamera()
    {
        if(isCameraRunning)
        {
            mainManagerControl.doStopTrackers();
            CameraDevice.getInstance().stop();
            CameraDevice.getInstance().deinit();
            isCameraRunning = false;
        }
    }

    // Method for setting / updating the projection matrix for AR content
    // rendering
    private void setProjectionMatrix()
    {
        CameraCalibration camCal = CameraDevice.getInstance().getCameraCalibration();
        projectionMatrix = Tool.getProjectionGL(camCal, 10.0f, 5000.0f);
    }

    // Gets the projection matrix to be used for rendering
    public Matrix44F getProjectionMatrix()
    {
        return projectionMatrix;
    }
}
