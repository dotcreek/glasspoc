package dotcreek.argumentedreality;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;


public class CVActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{


    private static final String TAG = "CV ArgumentedReality";
    private final Handler mHandler = new Handler();

    /** Audio manager usado para reproducir efectos de sonido */
    private AudioManager mAudioManager;

    /** Detector de gestos para mostrar el menu */
    private GestureDetector mGestureDetector;

    /** OpenCV*/
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mRGB;
    private Mat mGray;
    private Mat mThreshold;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_cv);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mGestureDetector = createGestureDetector(this);

         /*Configuración del OpenCV*/
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setCvCameraViewListener(this);
        //mOpenCvCameraView.setMaxFrameSize(600,337);

    }

    @Override
    protected void onResume() {
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**      Funciones del GestureDetector       */
    private GestureDetector createGestureDetector(Context context) {

        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {

            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {

                    mAudioManager.playSoundEffect(Sounds.TAP);
                    return true;
                } else if (gesture == Gesture.TWO_TAP) {
                    // do something on two finger tap
                    return true;
                } else if (gesture == Gesture.SWIPE_RIGHT) {
                    // do something on right (forward) swipe

                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    // do something on left (backwards) swipe

                    return true;
                } else if (gesture == Gesture.SWIPE_DOWN) {

                    mAudioManager.playSoundEffect(Sounds.DISMISSED);
                    finish();
                    return true;
                }

                return false;
            }
        });

        gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
            @Override
            public void onFingerCountChanged(int previousCount, int currentCount) {



            }
        });
        gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {

            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                // do something on scrolling
                return false;
            }
        });
        return gestureDetector;
    }

    /** Funcion que envia los eventos del touchpad al GestureDetector */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }

    /**     Funciones OpenCV     */

    /** Callback del OpenCV */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV Cargado correctamente");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    /**     Inicia camara       */
    public void onCameraViewStarted(int width, int height) {
        mRGB = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mThreshold = new Mat(height, width, CvType.CV_8UC1);

    }

    /**     Detiene camara       */
    public void onCameraViewStopped() {
        mRGB.release();
        mGray.release();
        mThreshold.release();
    }
    /**     Recibe frame        */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRGB =inputFrame.rgba();
        mGray = inputFrame.gray();
        Imgproc.threshold(mGray,mThreshold,128.0,255.0,Imgproc.THRESH_OTSU);

     return mThreshold;
    }




}
