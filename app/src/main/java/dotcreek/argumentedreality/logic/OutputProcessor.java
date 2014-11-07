package dotcreek.argumentedreality.logic;

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

import java.util.List;

/**
 * Class that manage the Augmented reality functions, like draw 2d and 3d Objects over image.
 * By Kevin Alfaro for AugmentedReality Magazine
 * 2014
 */
public class OutputProcessor {

    Mat mIntrinsics;
    MatOfDouble mDistortion;
    Mat rvec;
    Mat tvec;
    Mat imagen;

    public OutputProcessor(Mat intrinsics, MatOfDouble distortion, Mat input){

        mIntrinsics = intrinsics;
        mDistortion = distortion;
        imagen = input;
    }

    /* Funcion que aplica las funciones de realidad aumentada */
    public void augmentReality(List<Marker> markerList,OpenGLRenderer mRenderer){

        if(markerList.size()>0) {
            for (int i = 0; i < markerList.size(); i++) {

                escribirID(markerList.get(i).getCoordenadas(), new Scalar(0, 0, 255), markerList.get(i).getID());
                dibujarCuadrado(markerList.get(i).getCoordenadas(), new Scalar(0, 0, 255));
                configuración3D(markerList.get(i).getCoordenadas());
                dibujarLinea();
                update3DObject(mRenderer);
            }
        }
    }

    /* Funcion que retorna la imagen con realidad aumentada aplicada*/
    public Mat getOutputImage(){

        return imagen;
    }

    /* Funcion que toma 4 puntos cardinales y crea lineas entre ellos para dibujar un cuadrado */
    private void dibujarCuadrado(MatOfPoint puntos,Scalar color){
        Core.line(imagen, puntos.toArray()[0], puntos.toArray()[1], color,2);
        Core.line(imagen, puntos.toArray()[1], puntos.toArray()[2], color,2);
        Core.line(imagen, puntos.toArray()[2], puntos.toArray()[3], color,2);
        Core.line(imagen, puntos.toArray()[3], puntos.toArray()[0], color, 2);

    }

    /* Funcion que escribe en pantalla los puntos cardinales con su respectiva ubicacion */
    private void escribirPuntos(MatOfPoint puntos,Scalar color){
        Core.putText(imagen, puntos.toArray()[0].toString(), puntos.toArray()[0], Core.FONT_ITALIC, 0.7, color, 2);
        Core.putText(imagen, puntos.toArray()[1].toString(), puntos.toArray()[1], Core.FONT_ITALIC, 0.7, color, 2);
        Core.putText(imagen, puntos.toArray()[2].toString(), puntos.toArray()[2], Core.FONT_ITALIC, 0.7, color, 2);
        Core.putText(imagen, puntos.toArray()[3].toString(), puntos.toArray()[3], Core.FONT_ITALIC, 0.7, color, 2);
    }

    /* Función que escribe el ID de un Marker */
    private void escribirID(MatOfPoint puntos,Scalar color,int ID){

        double x = (puntos.toArray()[0].x + puntos.toArray()[2].x)/2;
        double y = (puntos.toArray()[0].y + puntos.toArray()[2].y)/2;

        Core.putText(imagen, "ID: " + ID, new Point((int) x, (int) y), Core.FONT_ITALIC, 0.7, color, 2);

    }

    /* Función que establece los parametros 3D*/
    private void configuración3D(MatOfPoint puntos){

        MatOfPoint2f mApprox = new MatOfPoint2f();
        puntos.convertTo(mApprox, CvType.CV_32FC2);

        MatOfPoint3f objectPoints = new MatOfPoint3f(new Point3(-1, -1, 0), new Point3(-1, 1, 0), new Point3(1, 1, 0), new Point3(1, -1, 0));
        rvec = new Mat();
        tvec = new Mat();
        Calib3d.solvePnP(objectPoints, mApprox, mIntrinsics, mDistortion, rvec, tvec);

    }

    /*Funcion que dibuja una linea en 3D*/
    private void dibujarLinea(){

        MatOfPoint3f linea3d = new MatOfPoint3f(new Point3(0, 0, 0), new Point3(0, 0, 1));
        MatOfPoint2f linea2d = new MatOfPoint2f();
        Calib3d.projectPoints(linea3d, rvec, tvec, mIntrinsics, mDistortion, linea2d);
        Core.line(imagen, linea2d.toArray()[0], linea2d.toArray()[1], new Scalar(0, 255, 0), 2);
    }

    private void update3DObject(OpenGLRenderer mRenderer){

        mRenderer.NewPosition(tvec.get(0,0)[0],tvec.get(1,0)[0],tvec.get(2,0)[0]);
        mRenderer.NewRotation(rvec.get(0,0)[0],rvec.get(1,0)[0],rvec.get(2,0)[0]);

    }
}
