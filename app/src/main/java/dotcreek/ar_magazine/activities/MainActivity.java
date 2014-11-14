package dotcreek.ar_magazine.activities;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import dotcreek.ar_magazine.R;
import dotcreek.ar_magazine.managers.MainManager;
import dotcreek.ar_magazine.utils.DebugUtil;


/**
 * DotCreek
 * Augmented Reality Magazine For Glass
 * Created by Kevin on 11/11/2014
 *
 * MainActivity: This class manage all related with user interface, also is the responsible to
 * start the main manager class.
 */

public class MainActivity extends Activity {

    // Managers objects
    private MainManager mainManager;
    private AudioManager audioManager;
    private GestureDetector gestureDetector;

    //Debug utility
    DebugUtil debugUtil = new DebugUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        debugUtil.LogInfo(debugUtil.TAG_MAINACTIVITY,"OnCreate function");
        super.onCreate(savedInstanceState);

        // Set Landscape Orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Set Windows Flags
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Create the AudioManager
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        // Create GestureDetector
        gestureDetector = createGestureDetector(this);

        // Create the  User Interface (UI)
        RelativeLayout layoutUserInterface = createUserInterface();

        // Create the MainManager
        mainManager = new MainManager(this,layoutUserInterface);

        // Start Loading animation in UI layout
        mainManager.startLoadingAnimation(layoutUserInterface);

        // Initialize all the vuforia process
        mainManager.startVuforiaManager();

        // Load textures from assets
        mainManager.loadTextures();


    }

    @Override
    protected void onResume()
    {
        debugUtil.LogInfo(debugUtil.TAG_MAINACTIVITY,"OnResume function");
        super.onResume();

        try{
            mainManager.resumeAR();
        }
        catch (Exception e)
        {
            debugUtil.LogError(debugUtil.TAG_MAINACTIVITY,"ResumeAr try failed");
        }

    }

    @Override
    protected void onPause()
    {
        debugUtil.LogInfo(debugUtil.TAG_MAINACTIVITY,"OnPause function");
        super.onPause();

        try
        {
            mainManager.pauseAR();
        } catch (Exception e)
        {
            debugUtil.LogError(debugUtil.TAG_MAINACTIVITY,"PauseAr try failed");
        }
    }


    @Override
    protected void onDestroy()
    {
        debugUtil.LogInfo(debugUtil.TAG_MAINACTIVITY,"OnDestroy function");
        super.onDestroy();

        try
        {
            mainManager.stopAR();
        } catch (Exception e)
        {
            debugUtil.LogError(debugUtil.TAG_MAINACTIVITY,"StopAr try failed");
        }
    }


    // Create the main UI Layout
    public RelativeLayout createUserInterface(){

        LayoutInflater inflater = LayoutInflater.from(this);
        RelativeLayout layoutUserInterface = (RelativeLayout) inflater.inflate(R.layout.camera_overlay,null, false);
        layoutUserInterface.setVisibility(View.VISIBLE);
        layoutUserInterface.setBackgroundColor(Color.BLACK);

        return layoutUserInterface;
    }

    /**     Option Menu Functions      */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            default:
                return false;
        }
    }


    /**     Gesture Detector Functions      */
    private GestureDetector createGestureDetector(Context context) {

        GestureDetector gDetector = new GestureDetector(context);
        //Base Listener
        gDetector.setBaseListener( new GestureDetector.BaseListener() {

            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {

                    audioManager.playSoundEffect(Sounds.TAP);
                    openOptionsMenu();
                    return true;
                }
                else if (gesture == Gesture.SWIPE_DOWN) {

                    audioManager.playSoundEffect(Sounds.DISMISSED);
                    finish();
                    return true;
                }
                return false;
            }
        });

        //Finger Listener
        gDetector.setFingerListener(new com.google.android.glass.touchpad.GestureDetector.FingerListener() {
            @Override
            public void onFingerCountChanged(int previousCount, int currentCount) {

            }
        });

        //Scroll Listener
        gDetector.setScrollListener(new com.google.android.glass.touchpad.GestureDetector.ScrollListener() {

            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                // do something on scrolling
                return false;
            }
        });
        return gDetector;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (gestureDetector != null) {
            return gestureDetector.onMotionEvent(event);
        }
        return false;
    }





}
