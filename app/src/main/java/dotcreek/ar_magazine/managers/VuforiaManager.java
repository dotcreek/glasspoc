package dotcreek.ar_magazine.managers;

import android.app.Activity;
import android.os.AsyncTask;

import com.qualcomm.vuforia.Vuforia;

import dotcreek.ar_magazine.interfaces.ApplicationControl;
import dotcreek.ar_magazine.utils.DebugUtil;

/**
 * DotCreek
 * Augmented Reality Magazine For Glass
 * Created by Kevin on november 2014
 *
 * VuforiaManager: This class manage the process directly related with vuforia.
 */

public class VuforiaManager {

    // An object used for synchronizing Vuforia initialization, dataset loading and the Android onDestroy() life cycle event.
    private Object objSynchronizer;

    // Interface control
    private Activity mainActivity;

    // Interface for communication with mainmanager
    private ApplicationControl mainManagerControl;

    // Is the vuforia Engine started?
    private boolean boolStarted = false;

    // Loader Tracker AsycnTask
    private LoadTrackerTask trackerLoader;

    // Vuforia Engine AsycnTask
    private  InitVuforiaTask vuforiaInitializer;

    //Debug utility
    DebugUtil debugUtil = new DebugUtil();


    public VuforiaManager(Object synchronizer,Activity activity,ApplicationControl control){

        // Required variables
        mainManagerControl = control;
        objSynchronizer = synchronizer;
        mainActivity = activity;

        // Initialize the vuforia AsyncTask
        vuforiaInitializer = new InitVuforiaTask();
        vuforiaInitializer.execute();
    }


    // An async task to initialize Vuforia asynchronously.
    public class InitVuforiaTask extends AsyncTask<Void, Integer, Boolean>
    {
        // Initialize with invalid value:
        private int intProgressValue = -1;


        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap with initialization:
            synchronized (objSynchronizer)
            {
                debugUtil.LogInfo(debugUtil.TAG_VUFORIAMANAGER,"Vuforia Set Init Parameters");
                Vuforia.setInitParameters(mainActivity, Vuforia.GL_20);

                do
                {
                    // Vuforia.init() blocks until an initialization step is
                    // complete, then it proceeds to the next step and reports
                    // progress in percents (0 ... 100%).
                    // If Vuforia.init() returns -1, it indicates an error.
                    // Initialization is done when progress has reached 100%.
                    debugUtil.LogInfo(debugUtil.TAG_VUFORIAMANAGER,"Vuforia Init");
                    intProgressValue = Vuforia.init();

                    // Publish the progress value:
                    publishProgress(intProgressValue);

                    // We check whether the task has been canceled in the
                    // meantime (by calling AsyncTask.cancel(true)).
                    // and bail out if it has, thus stopping this thread.
                    // This is necessary as the AsyncTask will run to completion
                    // regardless of the status of the component that
                    // started is.
                }
                while (!isCancelled() && intProgressValue >= 0 && intProgressValue < 100);

                return (intProgressValue > 0);
            }
        }


        protected void onProgressUpdate(Integer... values)
        {
            // Do something with the progress value "values[0]", e.g. update
            // splash screen, progress bar, etc.
        }


        protected void onPostExecute(Boolean result)
        {
            // Done initializing Vuforia, proceed to next application
            // initialization status:

            if (result){

                boolean initTrackersResult;
                initTrackersResult = mainManagerControl.doInitTrackers();

                if (initTrackersResult){

                    try{

                        // Start TrackerLoader
                        trackerLoader = new LoadTrackerTask();
                        trackerLoader.execute();
                    }
                    catch (Exception e){

                        debugUtil.LogError(debugUtil.TAG_VUFORIAMANAGER,"Loading tracking data set failed");
                        mainActivity.finish();

                    }

                }
                else{

                    debugUtil.LogError(debugUtil.TAG_VUFORIAMANAGER,"Failed to initialize trackers");
                    mainActivity.finish();
                }

            }
            else{

                debugUtil.LogError(debugUtil.TAG_VUFORIAMANAGER,"Failed to initialize Vuforia");
                mainActivity.finish();


            }
        }
    }

    // An async task to load the tracker data asynchronously.
    public class LoadTrackerTask extends AsyncTask<Void, Integer, Boolean>
    {
        protected Boolean doInBackground(Void... params)
        {
            // Prevent the onDestroy() method to overlap:
            synchronized (objSynchronizer)
            {
                // Load the tracker data set:
                return mainManagerControl.doLoadTrackersData();
            }
        }

        protected void onPostExecute(Boolean result)
        {


            //Tracker task result log

            if (!result)
            {
                debugUtil.LogError(debugUtil.TAG_VUFORIAMANAGER,"Failed to load tracker data");
            }
            else
            {
                // Hint to the virtual machine that it would be a good time to
                // run the garbage collector:
                //
                // NOTE: This is only a hint. There is no guarantee that the
                // garbage collector will actually be run.
                System.gc();

                //Vuforia.registerCallback();
                boolStarted = true;

            }

            // Done loading the tracker, update application status
            mainManagerControl.onInitARDone();
            debugUtil.LogInfo(debugUtil.TAG_VUFORIAMANAGER,"Done loading the trackers");
        }
    }

    //Return the AsyncTask, used in MainManager to control the app lifecycle
    public LoadTrackerTask getTrackerLoader(){
        return trackerLoader;
    }

    //Return the AsyncTask, used in MainManager to control the app lifecycle
    public InitVuforiaTask getVuforiaInitializer(){
        return vuforiaInitializer;
    }

    //Return the Vuforia status, used in MainManager to control the app lifecycle
    public boolean getStatus(){

        return boolStarted;
    }

    //Set the vuforia status, used in MainManager to control the app lifecycle
    public void setStatus(boolean status){

        boolStarted = status;
    }

    //Set vuforia and trackerloader null
    public void setTaskNull(){
        vuforiaInitializer = null;
        trackerLoader = null;
    }
}
