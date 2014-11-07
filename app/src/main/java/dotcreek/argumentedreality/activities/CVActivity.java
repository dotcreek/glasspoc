package dotcreek.argumentedreality.activities;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

import java.util.List;

import dotcreek.argumentedreality.logic.CameraParameters;
import dotcreek.argumentedreality.logic.ImageProcessor;
import dotcreek.argumentedreality.logic.Marker;
import dotcreek.argumentedreality.logic.MarkerProcessor;
import dotcreek.argumentedreality.logic.OpenGLRenderer;
import dotcreek.argumentedreality.logic.OutputProcessor;
import rajawali.RajawaliActivity;

/**
 * Class that control activity_cv layout. This class is the main activity of Augmented vision,
 * the Camera view of computarized vision and the Surface view of OpenGL are here.
 * By Kevin Alfaro for AugmentedReality Magazine
 * 2014
 */


public class CVActivity extends RajawaliActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    /**     Variables    */

    //region TAG del log
    private static final String TAG = "CV ArgumentedReality";
    private static final int HEIGHT = 360;
    private static final int WIDTH = 600;
    //endregion

    //region Controlador de audio
    private AudioManager mAudioManager;
    //endregion

    //region Controlador del touchpad
    private GestureDetector mGestureDetector;
    //endregion

    //region Variables OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mOutput;
    //endregion

    //region Procesadores y sus variables
    private MarkerProcessor pMarker;
    private ImageProcessor pImage;
    private OutputProcessor pOutput;
    private List<MatOfPoint> lstPossibleMarkers;
    private List<Marker> lstValidMarkers;
    private CameraParameters cpParameters;
    //endregion

    //region Variables OpenGL
    private OpenGLRenderer mRenderer;

    //endregion


    /**     Funciones   */

    //region Funciones OnCreate OnDestroy OnResume OnPause

    @Override
    public void onCreate(Bundle bundle) {

        super.onCreate(bundle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /* Controlador de audio */
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        /* Controlador del touchpad  */
        mGestureDetector = createGestureDetector(this);

         /*Configuración del OpenCV*/
        mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setMaxFrameSize(WIDTH,HEIGHT);

        /*Configuración OpenGL*/
        mSurfaceView.setZOrderMediaOverlay(true);
        setGLBackgroundTransparent(true);
        mRenderer = new OpenGLRenderer(this);
        mRenderer.setSurfaceView(mSurfaceView);
        super.setRenderer(mRenderer);

        mLayout.addView(mOpenCvCameraView);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(WIDTH,HEIGHT);
        mLayout.setLayoutParams(layoutParams);
        mOpenCvCameraView.setLayoutParams(new FrameLayout.LayoutParams(WIDTH,HEIGHT));

    }

    @Override
    protected void onResume() {
        /*Se vuelven a cargar las librerias OpenCV*/
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    //endregion

    //region Funciones del GestureDetector

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

    /* Funcion que envia los eventos del touchpad al GestureDetector */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }
    //endregion

    //region Funciones de la camara OpenCV

    /* Callback del OpenCV */
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

    /* Inicia camara */
    public void onCameraViewStarted(int width, int height) {

        mOutput = new Mat(height, width, CvType.CV_8UC4);

        /* Se crean los procesadores */
        pMarker = new MarkerProcessor();
        pImage = new ImageProcessor(width,height);

        /* Se obtienen los valores de calibración */
        cpParameters = new CameraParameters(width, height, this);


    }

    /* Detiene camara */
    public void onCameraViewStopped() {
        pImage.release();
    }

    /*  Recibe frame */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //Se obtienen los posibles markers de la imagen que pueden ser procesadors por el pMarker
        lstPossibleMarkers = pImage.getMarkers(inputFrame);
        //Log.i("Prueba","Posible markers" + lstPossibleMarkers.size());

        //Se obtienen los markers validos con su respectivo ID y coordenadas
        lstValidMarkers = pMarker.getValidMarkers(lstPossibleMarkers, inputFrame.gray());
        Log.i("Prueba","Valid markers" + lstValidMarkers.size());

        //Se crea el procesador que gestiona la realidad aumentada
        pOutput = new OutputProcessor(cpParameters.getIntrinsics(),cpParameters.getDistortion(),inputFrame.rgba());
        pOutput.augmentReality(lstValidMarkers,mRenderer);
        mOutput = pOutput.getOutputImage();

        /*//Preview del marker
        mCanonical.convertTo(mCanonical,mRGB.type());
        mCanonical.copyTo(mGray.submat(new Rect(0, 0, 70, 70)));*/


        return mOutput;
    }

    //endregion




}
