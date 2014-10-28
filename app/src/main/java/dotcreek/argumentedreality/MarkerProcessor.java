package dotcreek.argumentedreality;



import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MarkerProcessor {


    MatOfPoint2f mDest;

    public MarkerProcessor(){

       mDest = new MatOfPoint2f(new Point(0, 0), new Point(69, 0), new Point(69, 69), new Point(0, 69));

    }

    //Funcion que cambia la perspectiva del marker para tener una forma regular y poder comprobar validez del mismo
    public Mat CambiarPerspectiva(MatOfPoint puntos, Mat mGris){


        Mat mOrigin = new MatOfPoint2f(puntos.toArray());
        Mat mCanonical = new Mat(70, 70, mGris.type());
        Mat m = new Mat();
        m = Imgproc.getPerspectiveTransform(mOrigin, mDest);
        Imgproc.warpPerspective(mGris, mCanonical, m, new Size(70, 70));

        return mCanonical;
    }


    public int CalcularID(Mat marca,int nRotations){

        Imgproc.threshold(marca, marca, 125, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        int sCelda = marca.rows()/7; //Tamaño de la celda

        //Se comprueba que el marco (Las filas y columnas 0 y 6) sea negro
        for (int y=0;y<7;y++)
        {
            int inc=6; //Incremento

            if (y==0 || y==6) inc=1; //Solo comprueba el borde

            for (int x=0;x<7;x+=inc)
            {
                int celdaX = x * sCelda;
                int celdaY = y * sCelda;
                Mat celda = marca.submat(new Rect(celdaX,celdaY,sCelda,sCelda));

                int cNZ = Core.countNonZero(celda); //Cuenta la cantidad de pixeles diferentes a cero

                if (cNZ > (sCelda*sCelda) / 2) // Si mas de la mitad de la celda es de un color diferente a cero
                {
                    //La imagen no es un marker valido
                    return -1;
                }
            }
        }

        //Se calcula el ID de la marca segun la información dentro del marco negro
        Mat mBits = new Mat();
        mBits = Mat.zeros(5,5, CvType.CV_8UC1);

        //get information(for each inner square, determine if it is black or white)
        for (int y=0;y<5;y++)
        {
            for (int x=0;x<5;x++)
            {
                int celdaX = (x+1)*sCelda;
                int celdaY = (y+1)*sCelda;
                Mat celda = marca.submat(new Rect(celdaX,celdaY,sCelda,sCelda));

                int cNZ = Core.countNonZero(celda); //Cuenta la cantidad de pixeles diferentes a cero

                if (cNZ > (sCelda*sCelda) / 2) // Si mas de la mitad de la celda es de un color diferente a cero
                {
                    //La celda es blanca
                    double[] uno ={1,0,0};
                    mBits.put(y,x,uno);
                    //PUT LOG
                }
            }
        }
        //Se buscan todas las posibles rotaciones
        List<Mat> rotations = new ArrayList<Mat>();
        for (int i = 0; i < 5; i++) {
            rotations.add(new Mat());
        }

        rotations.set(0,mBits);

        int distances[] = new int[4];
        distances[0] = hammDistMarker(rotations.get(0));

        Pair<Integer,Integer> minDist = new Pair(distances[0],0);

        for (int i=1; i<4; i++)
        {
            //get the hamming distance to the nearest possible word
            rotations.set(i,rotate(rotations.get(i-1)));
            distances[i] = hammDistMarker(rotations.get(i));

            if (distances[i] < minDist.getFirst())
            {
                minDist.setFirst(distances[i]);
                minDist.setSecond(i);
            }
        }

        nRotations = minDist.getSecond();
        if (minDist.getFirst() == 0)
        {
            return mat2id(rotations.get(minDist.getSecond()));
        }

        return -1;
    }


    private Mat rotate(Mat in)
    {
        Mat out = new Mat();
        in.copyTo(out);
        for (int i=0;i<in.rows();i++)
        {
            for (int j=0;j<in.cols();j++)
            {
                //out.at<uchar>(i,j)=in.at<uchar>(in.cols-j-1,i);
                double[] num = in.get(in.cols()-j-1,i);
                out.put(i,j,num);
            }
        }
        return out;
    }


    private int hammDistMarker(Mat bits){
        int ids[][]={
            {1,0,0,0,0},
            {1,0,1,1,1},
            {0,1,0,0,1},
            {0,1,1,1,0}
        };

        int dist=0;

        for (int y=0;y<5;y++){

            double minSum= 1e5; //hamming distance to each possible word

            for (int p=0;p<4;p++){

                int sum=0;
                //now, count
                for (int x=0;x<5;x++){

                    //sum += bits.at<uchar>(y,x) == ids[p][x] ? 0 : 1;
                    double[] uno =  bits.get(y,x);
                    //GET LOG
                    sum += uno[0] == ids[p][x] ? 0 : 1;
                }

                if (minSum>sum)
                    minSum=sum;
            }

            //do the and
            dist += minSum;
        }

        return dist;
    }

    private int mat2id( Mat bits)
    {

        int val=0;
        for (int y=0;y<5;y++)
        {
            val<<=1;
            double num[] =bits.get(y,1);
            if ( num[0] != 0) val|=1;
            val<<=1;
            double num2[] =bits.get(y,3);
            if (num2[0] != 0) val|=1;
        }
        return val;
    }
}
