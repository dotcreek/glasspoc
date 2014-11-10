package dotcreek.argumentedreality.logic;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that process the frames from the camera, apply basic filters, search squares in the image, and select the markers
 * By Kevin Alfaro for AugmentedReality Magazine
 * 2014
 */



public class ImageProcessor {

    //Constante que divide un frame, para detenerminar el tamaño minimo de un cuadrado (Entre menos divisor, el tamaño minimo es mayor)
    private static final int DIVISOR = 6;
    //Vertices que tienen los poligonos a buscar (se mantiene en 4 porque los markers son cuadrados o rectangulos)
    private static final int POLYGON_VERTICES = 4;
    //Determina el perimetro minimo que deben tener los cuadrados
    private static final int MIN_SIZE_SQUARE = 50 ;

    //Matrices de filtracion basica
    private Mat mRGB;
    private Mat mGray;
    private Mat mThreshold;
    private Mat mBlur;
    private Mat mHierarchy;

    //Matrices de busqueda y clasificacion de figuras
    private MatOfPoint mTempContour;
    private MatOfPoint mPoints;
    private MatOfPoint2f mApprox ;
    private MatOfPoint2f mContour ;




    public ImageProcessor(int width, int height){

        mRGB = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mThreshold = new Mat();
        mBlur = new Mat();
        mHierarchy = new Mat();


    }

    /* Funcion que obtiene un marker final que puede ser procesado por la clase MarkerProcessor */
    public List<MatOfPoint> getMarkers(CameraBridgeViewBase.CvCameraViewFrame inputFrame){

        //Matrices usadas
        mHierarchy = new Mat();
        mRGB =inputFrame.rgba();
        mGray = inputFrame.gray();

        List<MatOfPoint> lstMarkers = new ArrayList();

        //Se filtra el frame para obtener los contornos
        List<MatOfPoint> lstContours = getContours();

        //Se buscan los cuadrados en los contornos
        for (int i = 0; i < lstContours.size(); i++) {

            //Contorno Temporal
            mTempContour = lstContours.get(i);
            //Contorno convertido a MOPF2
            mContour = new MatOfPoint2f(mTempContour.toArray());
            //Aprox curve MOPF2 (Esquinas/Puntos)
            mApprox = new MatOfPoint2f();

            //Ignora los contornos mas pequeños
            if (mContour.total() > mRGB.cols() / DIVISOR) {

                //Se procesan los contornos para obtener las esquinas
                Imgproc.approxPolyDP(mContour, mApprox, mTempContour.total() * 0.02, true);

                //Se guardas las esquinas en una matriz de puntos
                mPoints = new MatOfPoint(mApprox.toArray());

                //Numero de vertices ----> Convexo -------> perimetro del contorno
                if (mPoints.toArray().length == POLYGON_VERTICES && Imgproc.isContourConvex(mPoints) && (int) Math.abs(Imgproc.contourArea(mApprox)) > MIN_SIZE_SQUARE) {
                    //if(mPoints.toArray().length==4 && Imgproc.isContourConvex(mPoints)){

                    //Marker posible! Se añade a la lista
                    lstMarkers.add(mPoints);

                }
            }
        }

        return lstMarkers;

    }

    /* Funcion que libera el valor de las matrices */
    public void release(){

        mRGB.release();
        mGray.release();
        mThreshold.release();
        mBlur.release();
        mHierarchy.release();
    }



    private List<MatOfPoint> getContours(){

        //Se aplica filtro gaussiano para elimitar ruido
        Imgproc.blur(mGray, mBlur, new Size(3, 3));
        Imgproc.threshold(mBlur,mThreshold,128.0,255.0,Imgproc.THRESH_OTSU);

        /*//Se aplica el umbral para separar el cuadrado negro
        Imgproc.adaptiveThreshold(mGray,mThreshold,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY_INV,7,7);*/

        //Se buscan los contornos
        List<MatOfPoint> lstContours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mThreshold,lstContours,mHierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_NONE);
        mHierarchy.release();

        return lstContours;

    }
}
