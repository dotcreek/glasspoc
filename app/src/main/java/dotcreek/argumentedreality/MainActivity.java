package dotcreek.argumentedreality;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;


public class MainActivity extends Activity {


    private final Handler mHandler = new Handler();

    /** Audio manager usado para reproducir efectos de sonido */
    private AudioManager mAudioManager;

    /** Detector de gestos para mostrar el menu */
    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mGestureDetector = createGestureDetector(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    /**     Funciones del menu de opciones      */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.start:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startApp();
                    }
                });
                return true;

            case R.id.instructions:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startInstructions();
                    }
                });
                return true;

            default:
                return false;
        }
    }


    /**      Funciones del GestureDetector       */
    private GestureDetector createGestureDetector(Context context) {

        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {

            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {

                    mAudioManager.playSoundEffect(Sounds.TAP);
                    openOptionsMenu();
                    return true;
                } else if (gesture == Gesture.TWO_TAP) {
                    // do something on two finger tap
                    return true;
                } else if (gesture == Gesture.SWIPE_RIGHT) {
                    // do something on right (forward) swipe

                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    // do something on left (backwards) swipe

                    return true;
                } else if (gesture == Gesture.SWIPE_DOWN) {

                    mAudioManager.playSoundEffect(Sounds.DISMISSED);
                    finish();
                    return true;
                }

                return false;
            }
        });

        gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
            @Override
            public void onFingerCountChanged(int previousCount, int currentCount) {



            }
        });
        gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {

            @Override
            public boolean onScroll(float displacement, float delta, float velocity) {
                // do something on scrolling
                return false;
            }
        });
        return gestureDetector;
    }

    /** Funcion que envia los eventos del touchpad al GestureDetector */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }


    private void startApp() {
        startActivity(new Intent(this, CVActivity.class));
        finish();
    }

    private void startInstructions() {
        startActivity(new Intent(this, InstructionsActivity.class));
    }

}
