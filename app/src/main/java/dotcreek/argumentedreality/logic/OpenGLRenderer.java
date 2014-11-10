package dotcreek.argumentedreality.logic;
import android.content.Context;

import javax.microedition.khronos.opengles.GL10;

import dotcreek.argumentedreality.R;
import rajawali.Object3D;
import rajawali.lights.DirectionalLight;
import rajawali.materials.Material;
import rajawali.parser.LoaderOBJ;
import rajawali.parser.ParsingException;
import rajawali.renderer.RajawaliRenderer;

/**
 * Class makes the 3D objects renderization.
 * By Kevin Alfaro for AugmentedReality Magazine
 * 2014
 */

public class OpenGLRenderer extends RajawaliRenderer {

    /* Variables */
	private DirectionalLight mLight;
	private Object3D m3DObject;
	private boolean IsInitialized = false;
	private Material mCustomMaterial;


	public OpenGLRenderer(Context context) {
		super(context);
		setFrameRate(60);
	}

	public void initScene() {

		mLight = new DirectionalLight(1f, 0.2f, -1.0f); // set the direction
		mLight.setColor(1.0f, 1.0f, 1.0f);
		mLight.setPower(2);

		LoaderOBJ objParser = new LoaderOBJ(mContext.getResources(),mTextureManager, R.raw.camaro_obj);

		try {
			objParser.parse();
		} catch (ParsingException e) {

			e.printStackTrace();
		}

		m3DObject = objParser.getParsedObject();
		m3DObject.setPosition(0, 0, 0);
		m3DObject.setScale(0.3f);
        getCurrentScene().addChild(m3DObject);
		//IsInitialized = true;

	}
	
	@Override
	public void onDrawFrame(GL10 glUnused) {
		super.onDrawFrame(glUnused);

	}

    public void NewPosition(double x, double y, double z){

        m3DObject.setPosition(x, -y, -z);

    }

    public void NewRotation(double x, double y, double z){
        m3DObject.setRotation(x,y,z);
    }
	
	public boolean getState() {

		return IsInitialized;

	}


}
