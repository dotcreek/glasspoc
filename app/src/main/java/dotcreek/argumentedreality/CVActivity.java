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
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


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
    private Mat mBlur;
    private Mat mHierarchy;
    private List<MatOfPoint> lstContours;
    private List<MatOfPoint> lstSquares;
    private MatOfPoint mPoints;

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
        mThreshold = new Mat();
        mBlur = new Mat();
        mHierarchy = new Mat();


    }

    /**     Detiene camara       */
    public void onCameraViewStopped() {
        mRGB.release();
        mGray.release();
        mThreshold.release();
        mBlur.release();
        mHierarchy.release();
    }
    /**     Recibe frame        */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //Matrices usadas
        mHierarchy = new Mat();
        mRGB =inputFrame.rgba();
        mGray = inputFrame.gray();

        //Se aplica filtro gaussiano para elimitar ruido
        Imgproc.blur(mGray,mBlur,new Size(5, 5));

        //Se aplica el umbral para separar el cuadrado negro
        Imgproc.threshold(mBlur,mThreshold,128.0,255.0,Imgproc.THRESH_OTSU);

        //Se buscan los contornos
        lstContours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mThreshold,lstContours,mHierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);
        mHierarchy.release();

        //Se buscan los cuadrados en la imagen
        lstSquares = new ArrayList<MatOfPoint>();
        for(int i=0;i<lstContours.size();i++){

            MatOfPoint tempContour=lstContours.get(i);
            MatOfPoint2f mContour = new MatOfPoint2f(tempContour.toArray()); //Guardara el contour actual
            MatOfPoint2f mApprox = new MatOfPoint2f(); //Aprox Curve

            //lstContours.get(i).convertTo(mContour, CvType.CV_32FC2); //Se convierte el contour actual en Mat of pointf2 y se guarda en mContour

            Imgproc.approxPolyDP(mContour,mApprox,tempContour.total()*0.02,true);

            mPoints=new MatOfPoint(mApprox.toArray());
            //mApprox.convertTo(lstContours.get(i), CvType.CV_32S); //Se convierte el approx en Mat Of Point y se coloca en la lista


            //Log.i(TAG, "mPoints " +mPoints.toArray().length);
            //if(mPoints.toArray().length==4 && (Math.abs(mApprox.total())>1000) && Imgproc.isContourConvex(mPoints)){
            if(mPoints.toArray().length==4 && Imgproc.isContourConvex(mPoints)){
                lstSquares.add(mPoints);
            }
        }

        //Se dibuja  los cuadrados
        Imgproc.drawContours(mRGB,lstSquares,-1, new Scalar(0, 255, 0));

        //Se dibujan los contornos
        //Imgproc.drawContours(mRGB,lstContours,-1, new Scalar(0, 255, 0));

     return mRGB;
    }




}
