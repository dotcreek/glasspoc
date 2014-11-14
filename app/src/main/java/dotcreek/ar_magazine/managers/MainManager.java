package dotcreek.ar_magazine.managers;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.Marker;
import com.qualcomm.vuforia.MarkerTracker;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Tracker;
import com.qualcomm.vuforia.TrackerManager;
import com.qualcomm.vuforia.Vec2F;
import com.qualcomm.vuforia.Vuforia;

import java.util.Vector;

import dotcreek.ar_magazine.interfaces.ApplicationControl;
import dotcreek.ar_magazine.utils.DebugUtil;
import dotcreek.ar_magazine.utils.LoadingDialogUtil;
import dotcreek.ar_magazine.utils.Texture;

/**
 * DotCreek
 * Augmented Reality Magazine For Glass
 * Created by Kevin november 2014
 *
 * MainManager: This class is the central manager that control other secondaries managers:
 * the Vuforia manager, Camera manager, Render manager and Opengl manager, the functions that
 * are not related with secondaries manager are controlled here.
 */
public class MainManager implements ApplicationControl{

    // An object used for synchronizing Vuforia initialization, dataset loading and the Android onDestroy() life cycle event.
    private Object objSinchronizer = new Object();

    // The OpenGL Manager
    private SurfaceViewManager surfaceViewManager;

    // The 3D Renderer
    private RenderManager renderManager;

    // The Device Camera Manager
    private CameraManager cameraManager;

    // The Vuforia Engine Manager
    private VuforiaManager vuforiaManager;

    // Textures for rendering
    private Vector<Texture> vectorTextures;

    // Interface control
    private Activity mainActivity;
    private RelativeLayout layoutUserInterface;
    private LoadingDialogUtil loadingDialogUtil;

    // Array of markers
    private Marker dataSet[];

    //Debug utility
    DebugUtil debugUtil = new DebugUtil();

    public MainManager(Activity activity,RelativeLayout layout){

        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"Main manager constructor");
        // Get the UI
        mainActivity = activity;
        layoutUserInterface = layout;

        // Set the camera Manager
        cameraManager = new CameraManager(activity,this);

    }

    /**     Functions that not are not directly related with secondaries managers        */

    // This function show a loading spin while application managers are charging
    public void startLoadingAnimation(RelativeLayout layout){

        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"StartLoadingAnimation Function");
        // Create the loadingDialogUtil and set the layout
        loadingDialogUtil = new LoadingDialogUtil(layout);

        // Shows the loading indicator at start
        loadingDialogUtil.sendEmptyMessage(LoadingDialogUtil.SHOW_LOADING_DIALOG);

        // Adds the inflated layout to the view
        mainActivity.addContentView(layout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    }

    public void startVuforiaManager(){

        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"StartVuforiaManager Function");
        vuforiaManager = new VuforiaManager(objSinchronizer,mainActivity,this);


    }

    // Load any specific textures for 3D objects
    public void loadTextures(){

        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"Load textures function");
        vectorTextures = new Vector<Texture>();
        vectorTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_Q.png",mainActivity.getAssets()));
        vectorTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_C.png",mainActivity.getAssets()));
        vectorTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_A.png",mainActivity.getAssets()));
        vectorTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_R.png",mainActivity.getAssets()));

    }

    // Function that clear the textures vector
    public void unloadTextures(){
        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"Unload textures function");
        vectorTextures.clear();
        vectorTextures = null;

        // Garbage collector
        System.gc();

    }

    /**     Application lifecycle Functions       */
    // This function stop the Vuforia manager and its AsyncTask, also stops the camera ald call the textures unloader.
    public void stopAR()
    {
        // Get AsyncTask
        VuforiaManager.InitVuforiaTask vuforiaTask = vuforiaManager.getVuforiaInitializer();
        VuforiaManager.LoadTrackerTask trackerTask = vuforiaManager.getTrackerLoader();

        // Cancel potentially running tasks
        if (vuforiaTask != null && vuforiaTask.getStatus() != VuforiaManager.InitVuforiaTask.Status.FINISHED){
            vuforiaTask.cancel(true);
        }

        if (trackerTask != null && trackerTask.getStatus() != VuforiaManager.LoadTrackerTask.Status.FINISHED){
            trackerTask.cancel(true);
        }

        // Cleaning asycntask and setting status false
        vuforiaManager.setTaskNull();
        vuforiaManager.setStatus(false);

        // Hardware camera stopped
        cameraManager.stopCamera();

        // Ensure that all asynchronous operations to initialize Vuforia and loading the tracker datasets do not overlap
        synchronized (objSinchronizer){

            // Deinitialize the trackers:
            deinitTrackers();

            // Deinitialize Vuforia SDK:
            Vuforia.deinit();
        }

        unloadTextures();
    }

    // Function that resumes Vuforia, restarts the trackers and the camera
    public void resumeAR()
    {
        Vuforia.onResume();

        // Resume Camera
        if (vuforiaManager.getStatus()){

            cameraManager.startCamera();
        }

        // Resume OpenGL view
        if (surfaceViewManager != null)
        {
            surfaceViewManager.setVisibility(View.VISIBLE);
            surfaceViewManager.onResume();
        }
    }

    // Pauses Vuforia and stops the camera and Opengl
    public void pauseAR(){

        Vuforia.onPause();

        //Pause Camera
        if (vuforiaManager.getStatus()){

            cameraManager.stopCamera();
        }

        //Pause de OpenGL view
        if (surfaceViewManager != null){

            surfaceViewManager.setVisibility(View.INVISIBLE);
            surfaceViewManager.onPause();
        }
    }

    /**     Aplication control Callbacks     */

    @Override
    public boolean doInitTrackers()
    {
        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"DoInitTrackers");
        // Indicate if the trackers were initialized correctly
        boolean result;

        // Initialize the marker tracker
        TrackerManager trackerManager = TrackerManager.getInstance();
        Tracker trackerBase = trackerManager.initTracker(MarkerTracker.getClassType());
        MarkerTracker markerTracker = (MarkerTracker)(trackerBase);

        if(markerTracker == null){
            result = false;
            debugUtil.LogError(debugUtil.TAG_MAINMANAGER,"Failed Initialize MarkerTracker");
        }
        else{

            result = true;
            debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"MarkerTracker Initialized!");
        }

        return result;
    }


    //Load the marker ID's that will be used or that will appear on the magazine
    @Override
    public boolean doLoadTrackersData(){

        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"LoadTrackersData");
        TrackerManager tManager = TrackerManager.getInstance();
        MarkerTracker markerTracker = (MarkerTracker)tManager.getTracker(MarkerTracker.getClassType());
        if (markerTracker == null)
            return false;

        //Array of markers
        dataSet = new Marker[4];

        dataSet[0] = markerTracker.createFrameMarker(0, "MarkerQ", new Vec2F(50, 50));
        if (dataSet[0] == null)
        {
            //Failed to create frame marker
            return false;
        }

        dataSet[1] = markerTracker.createFrameMarker(1, "MarkerC", new Vec2F(50, 50));
        if (dataSet[1] == null)
        {
            //Failed to create frame marker
            return false;
        }

        dataSet[2] = markerTracker.createFrameMarker(2, "MarkerA", new Vec2F(50, 50));
        if (dataSet[2] == null)
        {
            //Failed to create frame marker
            return false;
        }

        dataSet[3] = markerTracker.createFrameMarker(3, "MarkerR", new Vec2F(50, 50));
        if (dataSet[3] == null)
        {
            //Failed to create frame marker
            return false;
        }


        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"Succesfully LoadTrackerData");

        return true;

    }


    @Override
    public boolean doStartTrackers(){
        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"DoStartTrackers");
        // Indicate if the trackers were started correctly
        boolean result = true;

        // Start the tracker
        TrackerManager tManager = TrackerManager.getInstance();
        MarkerTracker markerTracker = (MarkerTracker) tManager.getTracker(MarkerTracker.getClassType());
        if (markerTracker != null)
            markerTracker.start();

        return result;
    }


    @Override
    public boolean doStopTrackers(){
        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"DoStopTrackers");
        // Indicate if the trackers were stopped correctly
        boolean result = true;

        // Stop tracker
        TrackerManager tManager = TrackerManager.getInstance();
        MarkerTracker markerTracker = (MarkerTracker) tManager.getTracker(MarkerTracker.getClassType());
        if (markerTracker != null)
            markerTracker.stop();

        return result;
    }


    public void deinitTrackers()
    {
        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"deinitTrackers");
        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(MarkerTracker.getClassType());
    }


    @Override
    public void onInitARDone(){

        debugUtil.LogInfo(debugUtil.TAG_MAINMANAGER,"OnInitARDone");
        // OpenGL ES view
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        // Initialice and make surfaceview Transparent
        surfaceViewManager = new SurfaceViewManager(mainActivity);
        surfaceViewManager.init(translucent, depthSize, stencilSize);

        // Create the 3D renderer and set textures
        renderManager = new RenderManager(cameraManager);
        renderManager.setTextures(vectorTextures);

        // Set the renderer to 3D interface (SurfaceView)
        surfaceViewManager.setRenderer(renderManager);

        renderManager.mIsActive = true;

        // Now add the surface view to main manager It is important that the OpenGL ES surface view gets added
        // before the camera is started and video background is configured.
        mainActivity.addContentView(surfaceViewManager, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));

        // Sets the UILayout to be drawn in front of the camera
        layoutUserInterface.bringToFront();

        // Hides the Loading Dialog
        loadingDialogUtil.sendEmptyMessage(LoadingDialogUtil.HIDE_LOADING_DIALOG);

        // Sets the layout background to transparent
        layoutUserInterface.setBackgroundColor(Color.TRANSPARENT);


        // Initialice camera and set Continuos focus.
        cameraManager.startCamera();
        CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

    }


    @Override
    public void onQCARUpdate(State state){

    }
}
