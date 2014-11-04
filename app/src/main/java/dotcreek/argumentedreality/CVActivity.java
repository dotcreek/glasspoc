package dotcreek.argumentedreality;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
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
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


public class CVActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{

    /**     Variables    */

    //region TAG del log
    private static final String TAG = "CV ArgumentedReality";
    //endregion

    //region Controlador de audio
    private AudioManager mAudioManager;
    //endregion

    //region Controlador del touchpad
    private GestureDetector mGestureDetector;
    //endregion

    //region Variables OpenCV
    private Mat mRGB;
    private Mat mGray;
    private Mat mThreshold;
    private Mat mBlur;
    private Mat mHierarchy;
    private Mat mCanonical;
    private Mat mIntrinsics;
    private Mat rvec;
    private Mat tvec;
    private MatOfDouble mDistortion;
    private MatOfPoint mTempContour;
    private MatOfPoint mPoints;
    private MatOfPoint2f mContour ;
    private MatOfPoint2f mApprox ;
    private CameraBridgeViewBase mOpenCvCameraView;
    private List<MatOfPoint> lstContours;
    //endregion

    //region Procesador de Marcas
    private MarkerProcessor mpProcesador;
    //endregion+

    //region Variables OpenGL

    //endregion


    /**     Funciones   */

    //region Funciones OnCreate OnDestroy OnResume OnPause

    @Override
    protected void onCreate(Bundle bundle) {

        super.onCreate(bundle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_cv);

        /* Controlador de audio */
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        /* Controlador del touchpad  */
        mGestureDetector = createGestureDetector(this);

         /*Configuración del OpenCV*/
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setCvCameraViewListener(this);
        //mOpenCvCameraView.setMaxFrameSize(800,600);

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

        Log.i(TAG, "width height" + width + " " + height);

        /* Se obtienen los valores de calibración */
        CameraCalibrator mCalibrator = new CameraCalibrator(width, height);
        if (CalibrationResult.tryLoad(this, mCalibrator.getCameraMatrix(), mCalibrator.getDistortionCoefficients())) {
            mCalibrator.setCalibrated();
            mIntrinsics = new Mat();
            mDistortion = new MatOfDouble();
            mCalibrator.getCameraMatrix().copyTo(mIntrinsics);
            mCalibrator.getDistortionCoefficients().copyTo(mDistortion);
        }

        /* Se crean las matrices */
        mRGB = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mThreshold = new Mat();
        mBlur = new Mat();
        mHierarchy = new Mat();
        mpProcesador = new MarkerProcessor();


    }

    /* Detiene camara */
    public void onCameraViewStopped() {
        mRGB.release();
        mGray.release();
        mThreshold.release();
        mBlur.release();
        mHierarchy.release();
    }

    /*  Recibe frame */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        //Matrices usadas
        mHierarchy = new Mat();
        mRGB =inputFrame.rgba();
        mGray = inputFrame.gray();

        //Se aplica filtro gaussiano para elimitar ruido
        Imgproc.blur(mGray,mBlur,new Size(3, 3));
        Imgproc.threshold(mBlur,mThreshold,128.0,255.0,Imgproc.THRESH_OTSU);

        /*//Se aplica el umbral para separar el cuadrado negro
        Imgproc.adaptiveThreshold(mGray,mThreshold,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY_INV,7,7);*/

        //Se buscan los contornos
        lstContours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mThreshold,lstContours,mHierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);
        mHierarchy.release();

        //Se buscan los cuadrados en la imagen
        for(int i=0;i<lstContours.size();i++){

            //Contorno Temporal
            mTempContour=lstContours.get(i);
            //Contorno convertido a MOPF2
            mContour = new MatOfPoint2f(mTempContour.toArray());
            //Aprox curve MOPF2 (Esquinas/Puntos)
            mApprox = new MatOfPoint2f();

            if(mContour.total() > mRGB.cols()/5) {

                //Se procesan los contornos para obtener las esquinas
                Imgproc.approxPolyDP(mContour, mApprox, mTempContour.total() * 0.02, true);

                //Se guardas las esquinas en una matriz de puntos
                mPoints = new MatOfPoint(mApprox.toArray());

                //Numero de vertices ----> Convexo -------> Tamaño del contorno
                if (mPoints.toArray().length == 4 && Imgproc.isContourConvex(mPoints) && (int) Math.abs(Imgproc.contourArea(mApprox)) > 100) {
                    //if(mPoints.toArray().length==4 && Imgproc.isContourConvex(mPoints)){


                   /* Se camba la perspectiva de la marca */
                   mCanonical = mpProcesador.CambiarPerspectiva(mPoints,mGray);


                    /* Se calcula el ID de la marca */
                    int nRotations = 0;
                    int id = mpProcesador.CalcularID(mCanonical,nRotations);

                    //Si la marca tiene un ID Valido
                    if (id != -1) {
                        escribirID(mRGB, mPoints, new Scalar(0, 0, 255), id);
                        //Se unen las cuatro esquinas para dibujar un cuadrado
                        dibujarCuadrado(mRGB, mPoints, new Scalar(255, 0, 0));
                        //escribirPuntos(mRGB,mPoints,new Scalar(0,0,255));
                    }

                     /* Se establecen los parametros 3D */
                    configuración3D();

                    /*//Preview del marker
                    mCanonical.convertTo(mCanonical,mRGB.type());
                    mCanonical.copyTo(mGray.submat(new Rect(0, 0, 70, 70)));*/

                }
            }
        }
        return mRGB;
    }

    //endregion

    //region Funciones de realidad aumentada OpenCV

    /* Funcion que toma 4 puntos cardinales y crea lineas entre ellos para dibujar un cuadrado */
    private void dibujarCuadrado(Mat imagen,MatOfPoint puntos,Scalar color){
        Core.line(imagen, puntos.toArray()[0], puntos.toArray()[1], color,2);
        Core.line(imagen, puntos.toArray()[1], puntos.toArray()[2], color,2);
        Core.line(imagen, puntos.toArray()[2], puntos.toArray()[3], color,2);
        Core.line(imagen, puntos.toArray()[3], puntos.toArray()[0], color, 2);

    }

    /* Funcion que escribe en pantalla los puntos cardinales con su respectiva ubicacion */
    private void escribirPuntos(Mat imagen,MatOfPoint puntos,Scalar color){
        Core.putText(imagen, puntos.toArray()[0].toString(), puntos.toArray()[0], Core.FONT_ITALIC, 0.7, color, 2);
        Core.putText(imagen, puntos.toArray()[1].toString(), puntos.toArray()[1], Core.FONT_ITALIC, 0.7, color, 2);
        Core.putText(imagen, puntos.toArray()[2].toString(), puntos.toArray()[2], Core.FONT_ITALIC, 0.7, color, 2);
        Core.putText(imagen, puntos.toArray()[3].toString(), puntos.toArray()[3], Core.FONT_ITALIC, 0.7, color, 2);
    }

    /* Función que escribe el ID de un Marker */
    private void escribirID(Mat imagen,MatOfPoint puntos,Scalar color,int ID){

        double x = (puntos.toArray()[0].x + puntos.toArray()[2].x)/2;
        double y = (puntos.toArray()[0].y + puntos.toArray()[2].y)/2;

        Core.putText(imagen, "ID: "+ID, new Point((int)x,(int)y), Core.FONT_ITALIC, 0.7, color, 2);

    }

    /* Función que establece los parametros 3D*/
    private void configuración3D(){
        MatOfPoint3f objectPoints = new MatOfPoint3f(new Point3(-1, -1, 0), new Point3(-1, 1, 0), new Point3(1, 1, 0), new Point3(1, -1, 0));
        rvec = new Mat();
        tvec = new Mat();
        Calib3d.solvePnP(objectPoints, mApprox, mIntrinsics, mDistortion, rvec, tvec);

    }

    /*Funcion que dibuja una linea en 3D*/
    private void dibujarLinea(Mat imagen){

        MatOfPoint3f linea3d = new MatOfPoint3f(new Point3(0, 0, 0), new Point3(0, 0, 1));
        MatOfPoint2f linea2d = new MatOfPoint2f();
        Calib3d.projectPoints(linea3d, rvec, tvec, mIntrinsics, mDistortion, linea2d);
        Core.line(imagen, linea2d.toArray()[0], linea2d.toArray()[1], new Scalar(0, 255, 0), 2);
    }

    //endregion


}
