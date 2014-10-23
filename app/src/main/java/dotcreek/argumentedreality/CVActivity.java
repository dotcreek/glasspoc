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
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
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
    private Mat mIntrinsics;
    private MatOfDouble mDistortion;
    private List<MatOfPoint> lstContours;
    private MatOfPoint mPoints;
    private CameraCalibrator mCalibrator;
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

        Log.i(TAG, "width height" + width + " " + height);

        mCalibrator = new CameraCalibrator(width, height);
        if (CalibrationResult.tryLoad(this, mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients())) {
            mCalibrator.setCalibrated();
            mIntrinsics = new Mat();
            mDistortion = new MatOfDouble();
            mCalibrator.getCameraMatrix().copyTo(mIntrinsics);
            mCalibrator.getDistortionCoefficients().copyTo(mDistortion);
        }

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
        Imgproc.blur(mGray,mBlur,new Size(3, 3));

        //Se aplica el umbral para separar el cuadrado negro
        Imgproc.threshold(mBlur,mThreshold,128.0,255.0,Imgproc.THRESH_OTSU);

        //Se buscan los contornos
        lstContours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mThreshold,lstContours,mHierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);
        mHierarchy.release();

        //Se buscan los cuadrados en la imagen
        for(int i=0;i<lstContours.size();i++){

            //Contorno
            MatOfPoint tempContour=lstContours.get(i);
            //Contorno convertido a MOPF2
            MatOfPoint2f mContour = new MatOfPoint2f(tempContour.toArray());
            //Aprox curve MOPF2 (Esquinas)
            MatOfPoint2f mApprox = new MatOfPoint2f();

            //Se procesan los contornos para obtener las esquinas
            Imgproc.approxPolyDP(mContour,mApprox,tempContour.total()*0.02,true);

            //Se guardas las esquinas en una matriz de puntos
            mPoints=new MatOfPoint(mApprox.toArray());

            //Si tiene 4 esquinas, y es un contorno convexo, se detecta como cuadrado
            //&& (Math.abs(Imgproc.contourArea(mApprox))>10)

            //Numero de vertices ----> Convexo -------> Tamaño del contorno
            //if(mPoints.toArray().length==4 && Imgproc.isContourConvex(mPoints) && (int)Math.abs(Imgproc.contourArea(mApprox))>100){
            if(mPoints.toArray().length==4 && Imgproc.isContourConvex(mPoints)){
                Log.i(TAG, "Contorno = "+ Math.abs(Imgproc.contourArea(mApprox)));

                MatOfPoint3f objectPoints = new MatOfPoint3f(new Point3(-1,-1,0),new Point3(-1,1,0), new Point3(1,1,0),new Point3(1,-1,0));
                Mat rvec = new Mat();
                Mat tvec= new Mat();
                Calib3d.solvePnP(objectPoints,mApprox,mIntrinsics,mDistortion,rvec,tvec);

                //Se unen las cuatro esquinas para dibujar un cuadrado
                dibujarCuadrado(mRGB, mPoints, new Scalar(255, 0, 0));
                //escribirPuntos(mRGB,mPoints,new Scalar(0,0,255));

                MatOfPoint3f linea3d = new MatOfPoint3f(new Point3(0,0,0),new Point3(0,0,1));
                MatOfPoint2f linea2d = new MatOfPoint2f();
                Calib3d.projectPoints(linea3d, rvec, tvec, mIntrinsics, mDistortion, linea2d);
                Core.line(mRGB,linea2d.toArray()[0],linea2d.toArray()[1],new Scalar(0,255,0),2);

            }
        }


        return mRGB;
    }

    /** Funcion que toma 4 puntos cardinales y crea lineas entre ellos para dibujar un cuadrado */
    private void dibujarCuadrado(Mat imagen,MatOfPoint puntos,Scalar color){

        Core.line(imagen, puntos.toArray()[0], puntos.toArray()[1], color,2);
        Core.line(imagen, puntos.toArray()[1], puntos.toArray()[2], color,2);
        Core.line(imagen, puntos.toArray()[2], puntos.toArray()[3], color,2);
        Core.line(imagen, puntos.toArray()[3], puntos.toArray()[0], color, 2);

    }

    private void escribirPuntos(Mat imagen,MatOfPoint puntos,Scalar color){
        Core.putText(imagen, puntos.toArray()[0].toString(), puntos.toArray()[0], Core.FONT_ITALIC, 0.7, color, 2);
        Core.putText(imagen, puntos.toArray()[1].toString(), puntos.toArray()[1], Core.FONT_ITALIC, 0.7, color, 2);
        Core.putText(imagen, puntos.toArray()[2].toString(), puntos.toArray()[2], Core.FONT_ITALIC, 0.7, color, 2);
        Core.putText(imagen, puntos.toArray()[3].toString(), puntos.toArray()[3], Core.FONT_ITALIC, 0.7, color, 2);
    }







}
