package dotcreek.ar_magazine.managers;

import android.app.Activity;

import dotcreek.ar_magazine.helpers.VideoPlayerHelper;

/**
 * DotCreek
 * Augmented Reality Magazine For Glass
 * Edited by Kevin on november 2014
 *
 * VideoManager: This class control VideoPlayback Related Functions, like play video, stop video, resume video etc..
 */
public class VideoManager {

    private Activity mainActivity;
    private MainRenderManager mainRenderManager;

    //Public constants
    public static final int NUM_TARGETS = 1;
    public static final int VIDEO = 0;

    // Movie for the Targets
    private VideoPlayerHelper mVideoPlayerHelper[] = null;
    private int mSeekPosition[] = null;
    private boolean mWasPlaying[] = null;
    private String mMovieName[] = null;

    // A boolean to indicate whether we come from full screen:
    private boolean mReturningFromFullScreen = false;

    public VideoManager(Activity activity, MainRenderManager render){

        mainActivity = activity;
        mainRenderManager = render;

        mVideoPlayerHelper = new VideoPlayerHelper[NUM_TARGETS];
        mSeekPosition = new int[NUM_TARGETS];
        mWasPlaying = new boolean[NUM_TARGETS];
        mMovieName = new String[NUM_TARGETS];

        // Create the video player helper that handles the playback of the movie for the targets
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            mVideoPlayerHelper[i] = new VideoPlayerHelper();
            mVideoPlayerHelper[i].init();
            mVideoPlayerHelper[i].setActivity(mainActivity);
        }

        //Load video
        mMovieName[VIDEO] = "VideoPlayback/dotcreek.mp4";
    }


    //Function that play video when user tap in touchpad
    public void startVideo(){

        for (int i = 0; i < NUM_TARGETS; i++) {
            // Verify if the video is in screen
            //if (mainRenderManager.isVideoInScreen() == true) {
            // Check if it is playable on texture
            if (mVideoPlayerHelper[i].isPlayableOnTexture()) {
                // We can play only if the movie was paused, ready or stopped
                if ((mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.PAUSED)
                        || (mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.READY)
                        || (mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.STOPPED)
                        || (mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.REACHED_END)) {
                    // Pause all other media
                    pauseAll(i);

                    // If it has reached the end then rewind
                    if ((mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.REACHED_END))
                        mSeekPosition[i] = 0;

                    //True para full screen
                    mVideoPlayerHelper[i].play(false, mSeekPosition[i]);
                    mSeekPosition[i] = VideoPlayerHelper.CURRENT_POSITION;
                }
                else if (mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.PLAYING) {
                    // If it is playing then we pause it
                    mVideoPlayerHelper[i].pause();
                }
            }
            else if (mVideoPlayerHelper[i].isPlayableFullscreen()) {
                // If it isn't playable on texture
                // Either because it wasn't requested or because it
                // isn't supported then request playback fullscreen.
                mVideoPlayerHelper[i].play(true,VideoPlayerHelper.CURRENT_POSITION);
            }

            break;
            //}
        }

    }

    // Pause all movies except oneif the value of 'except' is -1 then do a blanket pause
    private void pauseAll(int except)
    {
        // And pause all the playing videos:
        for (int i = 0; i < NUM_TARGETS; i++){
            // We can make one exception to the pause all calls:
            if (i != except)
            {
                // Check if the video is playable on texture
                if (mVideoPlayerHelper[i].isPlayableOnTexture()){

                    // If it is playing then we pause it
                    mVideoPlayerHelper[i].pause();
                }
            }
        }
    }

    public void loadMovie(){

        for (int i = 0; i < NUM_TARGETS; i++)
        {
            mainRenderManager.setVideoPlayerHelper(i, mVideoPlayerHelper[i]);
            mainRenderManager.requestLoad(i, mMovieName[i], 0, false);
        }
    }

    public void setDimensions(){

        for (int i = 0; i < NUM_TARGETS; i++)
        {
            float[] temp = { 0f, 0f };
            mainRenderManager.targetPositiveDimensions[i].setData(temp);
            mainRenderManager.videoPlaybackTextureID[i] = -1;
        }

    }

    /** Stop pause and resume functions */
    public void stopVideo(){

        for (int i = 0; i < NUM_TARGETS; i++)
        {
            // If the activity is destroyed we need to release all resources:
            if (mVideoPlayerHelper[i] != null)
                mVideoPlayerHelper[i].deinit();
            mVideoPlayerHelper[i] = null;
        }
    }

    public void resumeVideo(){

        // Reload all the movies
        if (mainRenderManager != null)
        {
            for (int i = 0; i < NUM_TARGETS; i++)
            {
                if (!mReturningFromFullScreen)
                {
                    mainRenderManager.requestLoad(i, mMovieName[i], mSeekPosition[i],
                            false);
                } else
                {
                    mainRenderManager.requestLoad(i, mMovieName[i], mSeekPosition[i],
                            mWasPlaying[i]);
                }
            }
        }

        mReturningFromFullScreen = false;
    }

    public void pauseVideo(){

        // Store the playback state of the movies and unload them:
        for (int i = 0; i < NUM_TARGETS; i++)
        {
            // If the activity is paused we need to store the position in which
            // this was currently playing:
            if (mVideoPlayerHelper[i].isPlayableOnTexture())
            {
                mSeekPosition[i] = mVideoPlayerHelper[i].getCurrentPosition();
                mWasPlaying[i] = mVideoPlayerHelper[i].getStatus() == VideoPlayerHelper.MEDIA_STATE.PLAYING ? true: false;
            }

            // We also need to release the resources used by the helper, though
            // we don't need to destroy it:
            if (mVideoPlayerHelper[i] != null)
                mVideoPlayerHelper[i].unload();

        }

        mReturningFromFullScreen = false;


    }

}
