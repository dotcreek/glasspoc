package dotcreek.argumentedreality.logic;

import org.opencv.core.MatOfPoint;

/**
 * Class Marker with ID and Coordinates attributes.
 * By Kevin Alfaro for AugmentedReality Magazine
 * 2014
 */
public class Marker {

    //Atributos
    private int ID;
    private MatOfPoint Coordenadas;

    public Marker(int id, MatOfPoint coordenadas){
        setCoordenadas(coordenadas);
        setID(id);
    }

    public void setID(int id){
        ID = id;
    }

    public void setCoordenadas(MatOfPoint coordenadas) {
        Coordenadas = coordenadas;
    }

    public int getID() {
        return ID;
    }

    public MatOfPoint getCoordenadas() {
        return Coordenadas;
    }
}
