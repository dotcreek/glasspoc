/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package dotcreek.ar_magazine.interfaces;

import com.qualcomm.vuforia.State;


//  Interface to be implemented by the activity which uses SampleApplicationSession
public interface ApplicationControl
{
    
    // To be called to initialize the trackers
    boolean doInitTrackers();
    
    
    // To be called to load the trackers' data
    boolean doLoadTrackersData();
    
    
    // To be called to start tracking with the initialized trackers and their
    // loaded data
    void doStartTrackers();
    
    
    // To be called to stop the trackers
    void doStopTrackers();
    

    
    // This callback is called after the Vuforia initialization is complete,
    // the trackers are initialized, their data loaded and
    // tracking is ready to start
    void onInitARDone();
    
    
    // This callback is called every cycle
    void onQCARUpdate(State state);
    
}
