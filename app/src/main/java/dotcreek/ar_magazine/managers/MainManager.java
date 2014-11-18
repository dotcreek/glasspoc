package dotcreek.ar_magazine.managers;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.DataSet;
import com.qualcomm.vuforia.ImageTracker;
import com.qualcomm.vuforia.Marker;
import com.qualcomm.vuforia.MarkerTracker;
import com.qualcomm.vuforia.STORAGE_TYPE;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Tracker;
import com.qualcomm.vuforia.TrackerManager;
import com.qualcomm.vuforia.Vec2F;
import com.qualcomm.vuforia.Vuforia;

import java.util.Vector;

import dotcreek.ar_magazine.interfaces.ApplicationControl;
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
    private MainRenderManager mainRenderManager;

    // The Device Camera Manager
    private CameraManager cameraManager;

    // The Vuforia Engine Manager
    private VuforiaManager vuforiaManager;

    // The videoplayback Manager
    private VideoManager videoManager;

    // Textures for rendering
    private Vector<Texture> vectorTextures;
    private Vector<Texture> vectorVideoTextures;

    // Interface control
    private Activity mainActivity;
    private RelativeLayout layoutUserInterface;
    private LoadingDialogUtil loadingDialogUtil;

    // Array of markers
    private Marker dataSet[];

    //Image Dataset
    DataSet dataSetVideoTargets = null;



    public MainManager(Activity activity,RelativeLayout layout){

        // Get the UI
        mainActivity = activity;
        layoutUserInterface = layout;

        // Set the camera Manager
        cameraManager = new CameraManager(activity,this);


    }

    /**     Functions that not are not directly related with secondaries managers        */

    // This function show a loading spin while application managers are charging
    public void startLoadingAnimation(RelativeLayout layout){

        // Create the loadingDialogUtil and set the layout
        loadingDialogUtil = new LoadingDialogUtil(layout);

        // Shows the loading indicator at start
        loadingDialogUtil.sendEmptyMessage(LoadingDialogUtil.SHOW_LOADING_DIALOG);

        // Adds the inflated layout to the view
        mainActivity.addContentView(layout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    }

    public void startVuforiaManager(){

        vuforiaManager = new VuforiaManager(objSinchronizer,mainActivity,this);

    }

    public void startVideo(){

        videoManager.startVideo();
    }

    // Load any specific textures for 3D objects
    public void loadTextures(){

        // 3D Textures
        vectorTextures = new Vector<Texture>();
        vectorTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_Q.png",mainActivity.getAssets()));
        vectorTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_C.png",mainActivity.getAssets()));
        vectorTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_A.png",mainActivity.getAssets()));
        vectorTextures.add(Texture.loadTextureFromApk("FrameMarkers/letter_R.png",mainActivity.getAssets()));


        // Video textures
        vectorVideoTextures = new Vector<Texture>();
        vectorVideoTextures.add(Texture.loadTextureFromApk("VideoPlayback/dcframe.png",mainActivity.getAssets()));
        vectorVideoTextures.add(Texture.loadTextureFromApk("VideoPlayback/play.png",mainActivity.getAssets()));
        vectorVideoTextures.add(Texture.loadTextureFromApk("VideoPlayback/busy.png",mainActivity.getAssets()));
        vectorVideoTextures.add(Texture.loadTextureFromApk("VideoPlayback/error.png",mainActivity.getAssets()));


    }

    // Function that clear the textures vector
    public void unloadTextures(){

        //3D Textures
        vectorTextures.clear();
        vectorTextures = null;

        //Video Textures
        vectorVideoTextures.clear();
        vectorVideoTextures = null;

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

            UnloadTrackersData();
            // Deinitialize the trackers:
            deinitTrackers();

            // Deinitialize Vuforia SDK:
            Vuforia.deinit();
        }

        //Set vector of textures null
        unloadTextures();

        //Relase video resources
        videoManager.stopVideo();
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

        // Reload all the movies
        videoManager.resumeVideo();

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

        // Pause movies
        videoManager.pauseVideo();

    }

    /**     Aplication control Callbacks     */

    @Override
    public boolean doInitTrackers()
    {
        // Indicate if the trackers were initialized correctly
        boolean result = true;

        //Object that control all trackers
        TrackerManager trackerManager = TrackerManager.getInstance();

        // Initialize the marker tracker
        trackerManager.initTracker(MarkerTracker.getClassType());

        // Initialize the image tracker
        trackerManager.initTracker(ImageTracker.getClassType());

        return result;

    }

    public boolean UnloadTrackersData()
    {
        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        //Object that control all trackers
        TrackerManager trackerManager = TrackerManager.getInstance();

        //Image tracker
        ImageTracker imageTracker = (ImageTracker) trackerManager.getTracker(ImageTracker.getClassType());
        if (imageTracker == null)
        {

            return false;
        }

        if (dataSetVideoTargets != null)
        {
            if (imageTracker.getActiveDataSet() == dataSetVideoTargets
                    && !imageTracker.deactivateDataSet(dataSetVideoTargets)){

                result = false;

            }
            else if (!imageTracker.destroyDataSet(dataSetVideoTargets)){

                result = false;
            }

            dataSetVideoTargets = null;
        }

        return result;
    }

    //Load the marker ID's that will be used or that will appear on the magazine
    @Override
    public boolean doLoadTrackersData(){

        //Object that control all trackers
        TrackerManager trackerManager = TrackerManager.getInstance();

        //MarkerTracker Dataset
        MarkerTracker markerTracker = (MarkerTracker)trackerManager.getTracker(MarkerTracker.getClassType());
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

        //ImageTracker Dataset
        ImageTracker imageTracker = (ImageTracker) trackerManager.getTracker(ImageTracker.getClassType());
        if (imageTracker == null)
        {

            return false;
        }

        // Create the data sets:
        dataSetVideoTargets = imageTracker.createDataSet();
        if (dataSetVideoTargets == null)
        {

            return false;
        }

        // Load the data sets of imsge targets
        if (!dataSetVideoTargets.load("dcbanners.xml",
                STORAGE_TYPE.STORAGE_APPRESOURCE))
        {

            return false;
        }

        // Activate the data set:
        if (!imageTracker.activateDataSet(dataSetVideoTargets))
        {

            return false;
        }


        //Successfully initialized MarkerTracker

        return true;

    }


    @Override
    public void doStartTrackers(){

        //Object that control all trackers
        TrackerManager trackerManager = TrackerManager.getInstance();

        //MarkerTracker
        MarkerTracker markerTracker = (MarkerTracker) trackerManager.getTracker(MarkerTracker.getClassType());
        if (markerTracker != null)
            markerTracker.start();

        //Image Tracker
        Tracker imageTracker = TrackerManager.getInstance().getTracker(ImageTracker.getClassType());
        if (imageTracker != null)
        {
            imageTracker.start();
            //Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 2);
        }


    }


    @Override
    public void doStopTrackers(){

        // Stop MarkerTracker
        TrackerManager tManager = TrackerManager.getInstance();
        MarkerTracker markerTracker = (MarkerTracker) tManager.getTracker(MarkerTracker.getClassType());
        if (markerTracker != null)
            markerTracker.stop();

        //Stop imageTracker
        Tracker imageTracker = TrackerManager.getInstance().getTracker(ImageTracker.getClassType());
        if (imageTracker != null)
            imageTracker.stop();



    }


    public void deinitTrackers()
    {
        //Object that control all trackers
        TrackerManager trackerManager = TrackerManager.getInstance();

        //deinit marker tracker
        trackerManager.deinitTracker(MarkerTracker.getClassType());

        //deinit Image tracker
        trackerManager.deinitTracker(ImageTracker.getClassType());
    }


    @Override
    public void onInitARDone(){

        // OpenGL ES view
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        // Initialice and make surfaceview Transparent
        surfaceViewManager = new SurfaceViewManager(mainActivity);
        surfaceViewManager.init(translucent, depthSize, stencilSize);

        // Create the 3D renderer and set textures
        mainRenderManager = new MainRenderManager(cameraManager);
        mainRenderManager.setTextures(vectorTextures,vectorVideoTextures);

        //Set videoplayback Manager
        videoManager= new VideoManager(mainActivity,mainRenderManager);

        // The renderer comes has the OpenGL context, thus, loading to texture
        // must happen when the surface has been created. This means that we
        // can't load the movie from this thread (GUI) but instead we must
        // tell the GL thread to load it once the surface has been created.
        videoManager.loadMovie();

        // Set the renderer to 3D interface (SurfaceView)
        surfaceViewManager.setRenderer(mainRenderManager);

        videoManager.setDimensions();

        mainRenderManager.mIsActive = true;

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
