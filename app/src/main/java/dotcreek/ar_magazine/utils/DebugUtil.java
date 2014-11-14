package dotcreek.ar_magazine.utils;

import android.util.Log;

/**
 * Created by Kevin on 14/11/2014.
 */
public class DebugUtil {

    //Set true for activate log message
    private static final boolean DEBUG_STATE = true;

    //Tags
    public static final String TAG_MAINACTIVITY = "MainActivity::";
    public static final String TAG_MAINMANAGER = "MainManager::";
    public static final String TAG_CAMERAMANAGER = "CameraManager::";
    public static final String TAG_RENDERMANAGER = "RenderManager::";
    public static final String TAG_SURFACEVIEWMANAGER = "SurfaceViewManager::";
    public static final String TAG_VUFORIAMANAGER = "VuforiaManager::";



    public void LogInfo(String tag,String message){

        if(DEBUG_STATE == true){
            Log.i(tag,message);
        }
    }

    public void LogError(String tag,String message){

        if(DEBUG_STATE == true){
            Log.e(tag,message);
        }
    }

}
