package dotcreek.argumentedreality;
import android.content.Context;

import javax.microedition.khronos.opengles.GL10;

import rajawali.Object3D;
import rajawali.lights.DirectionalLight;
import rajawali.materials.Material;
import rajawali.parser.LoaderOBJ;
import rajawali.parser.ParsingException;
import rajawali.renderer.RajawaliRenderer;

public class OpenGLRenderer extends RajawaliRenderer {

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

		LoaderOBJ objParser = new LoaderOBJ(mContext.getResources(),
				mTextureManager, R.raw.multiobjects_obj);

		try {
			objParser.parse();
		} catch (ParsingException e) {

			e.printStackTrace();

		}

		m3DObject = objParser.getParsedObject();
		m3DObject.setPosition(0, 0, 0);
		m3DObject.setScale(0.07f);
        getCurrentScene().addChild(m3DObject);
		//IsInitialized = true;

	}
	
	@Override
	public void onDrawFrame(GL10 glUnused) {
		super.onDrawFrame(glUnused);

	}

    public void NewPosition(int x, int y, int z){

        m3DObject.setPosition(x, y, z);

    }
	
	public boolean getState() {

		return IsInitialized;

	}


}
