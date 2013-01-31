package com.example.tinyplayer;

import java.util.ArrayList;
import java.util.List;

import com.example.tinyplayer.InlineUtil.Leg;
import com.example.tinyplayer.widget.PlayerWidgetProvider;

import android.annotation.SuppressLint;
import android.app.Presentation;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Toast;

import static com.example.tinyplayer.InlineUtil.*;

@SuppressLint("NewApi")
public class PlaybackService extends Service {
    private static final String TAG = PlaybackService.class.getSimpleName();
    private final LocalBinder binder = new LocalBinder();

    @SuppressWarnings("unused")
    private Context mApplicationContext;
    private MyNotifier mNotifier;

    private SurfaceHolder mForgroundSurfaceHolder = null;
    public static boolean STOP_SERVICE_ON_COMPLETE = true;
    public static boolean RESET_ON_GO_BACKGROUND = true;
    
    public static String WIDGET_CHANGE="widgetchange"; 
    private PlayerWidgetProvider mAppWidgetProvider = PlayerWidgetProvider.getInstance();

    @Override
    public IBinder onBind(Intent arg0) {
        return this.binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApplicationContext = getApplicationContext();
        mNotifier = new MyNotifier(this);
        mNotifier.initNotification();
        mMyAVPlayer = new MyAVPlayer(this, mNotifier);
        mMySildShowPlayer = new MySlideShowPlayer(this, mNotifier);

        mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        
        notifyChange(WIDGET_CHANGE);
        
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(MyNotifier.SERVICE_STARTER_KEY);
        registerReceiver(mIntentReceiver, commandFilter);

        Leg.d(TAG, "onCreate ============================= service " ) ;
        // Toast.makeText(this, "PlaybackService created ...",
        // Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        dismissPresentation(mSlideShowPresentation, null);
        dismissPresentation(mVideoPresentation, null);

        mNotifier.uninitNotification();
        super.onDestroy();

        // Toast.makeText(this, "PlaybackService destroyed ...",
        // Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2) {
        handleStartIntent(paramIntent);
        return START_STICKY;
    }

    //
    public MyAVPlayer mMyAVPlayer;
    private MySlideShowPlayer mMySildShowPlayer;

    @SuppressWarnings("unused")
    private Intent mStartIntent;

    private List<Uri> mUris = new ArrayList<Uri>();

    // ++++++++++++++++

    private void copyList(List<Uri> source, List<Uri> dest) {
        if (dest == null) {
            return;
        }

        dest.clear();
        if (source != null) {
            dest.addAll(source);
        }
    }

    public void continueInBackground(Intent intent) {
        handleStartIntent(intent);
    }

    private void handleStartIntent(Intent paramIntent) {
        Leg.i(TAG, "handleStartIntent()");
        if (paramIntent == null) {
            return;
        }

        // by notification?
        int starter = paramIntent.getIntExtra(MyNotifier.SERVICE_STARTER_KEY, 0);
        if (starter == MyNotifier.BY_NOTIFICATION) {
            Leg.i(TAG, "process action in handleStart() function");
            processActionCommand(paramIntent);
            return;
        }

        boolean binding = paramIntent.getBooleanExtra("com.example.tinyplayer.Binding", false);
        Leg.i(TAG, "isBinding: " + binding);
        if(binding){
            return;
        }
        
        Leg.i(TAG, "handleStartIntent --------------------------------------------- >");
        //
        mStartIntent = paramIntent;
        int id = paramIntent.getIntExtra("com.example.tinyplayer.PresentationDisplayId", Display.DEFAULT_DISPLAY);

        int currentPosition = paramIntent.getIntExtra("com.example.tinyplayer.CurrentPosition", 0);
        boolean startAtOnce = paramIntent.getBooleanExtra("com.example.tinyplayer.StartAtOnce", false);
        ArrayList<Uri> tempUris = paramIntent.getParcelableArrayListExtra("com.example.tinyplayer.Uris");

        boolean fg = paramIntent.getBooleanExtra("com.example.tinyplayer.Foreground", false);
        boolean fs = paramIntent.getBooleanExtra("com.example.tinyplayer.FreshStart", false);

        int size = (tempUris == null) ? 0 : tempUris.size();
        boolean sv = isSingleVideo(tempUris);

        Log.i(TAG, "main play back routin");
        if (fs) {
            stopPlayback();
            copyList(tempUris, mUris);
        }

        if (fg) {
            if (sv) {
                prepareFgVideo();
            }
            return;

        } else {
            prepareBgPlayback();
            return;
        }
    }

    private void prepareFgVideo() {
        Leg.i(TAG, "prepareFgVideo()");
        mMyAVPlayer.startPlayback(true);
        mMyAVPlayer.reset();
        Leg.i(TAG, "about to setdatasource media player");
        this.mMyAVPlayer.setDataSource(mUris.get(0));
        if (mForgroundSurfaceHolder != null) {
            Leg.i(TAG, "about to set surface holder to media player");
            mMyAVPlayer.setDisplay(true, mForgroundSurfaceHolder);
        }
        // mMediaPlayer.setLooping(true);
        Leg.i(TAG, "about to prepare media player");
        this.mMyAVPlayer.prepare();
    }

    private void prepareBgPlayback() {
        Leg.i(TAG, "prepareBgPlayback()");

        if (mUris.size() <= 0) {
            return;
        }

        Display display = findDisplayById(0);
        if (display == null) {
            return;
        }

        boolean fs = mStartIntent.getBooleanExtra("com.example.tinyplayer.FreshStart", false);
        boolean savePosition = false;
        if(isPlaying() && !fs){
            savePosition = true;
        }
        
        Presentation p;

        if (isSingleVideo(mUris)) {
            Leg.i(TAG, "single video file");            
            if(savePosition){
                mMyAVPlayer.savePositionForNextPlayback();
            }
            dismissPresentation(mVideoPresentation, null);

            mVideoPresentation = new PresentationVideo(this, display, mMyAVPlayer);
            mVideoPresentation.setVideoUri(mUris.get(0));

            p = mVideoPresentation;
        } else {
            Leg.i(TAG, "multiple files or single image file");
            if(savePosition){
                mMySildShowPlayer.savePositionForNextPlayback();                
            }
            dismissPresentation(mSlideShowPresentation, null);
            
            mSlideShowPresentation = new PresentationSlideShow(this, display, mMySildShowPlayer);
            mSlideShowPresentation.setMediaUris(mUris);
            
            p = mSlideShowPresentation;
        }

        try {
            p.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            p.show();
            // isPlayingBackGround = true;
        } catch (WindowManager.InvalidDisplayException ex) {
            Log.w(TAG, "Couldn't show presentation!  Display was removed in " + "the meantime.", ex);
        }

    }

    // ++++++++++++++++

    private boolean isSingleVideo(List<Uri> tempUris) {
        if (tempUris == null || tempUris.size() <= 0) {
            return false;
        }
        Uri uri = tempUris.get(0);
        if (uri != null && 1 == getMediaType(uri)) {
            return true;
        }
        return false;
    }

    public void onPlaybackCompletion(MyMediaPlayer player) {
        if (player instanceof MyAVPlayer) {
            dismissPresentation(mVideoPresentation, null);
        } else if (player instanceof MySlideShowPlayer) {
            dismissPresentation(mSlideShowPresentation, null);
            Intent ii = new Intent("com.example.player.isShowSliding");
            ii.putExtra("isShowSliding", false);
            mApplicationContext.sendBroadcast(ii);
        }

        if (STOP_SERVICE_ON_COMPLETE) {
            stopSelf();
        }
    }

    //
    public class LocalBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    public void setForegroundSurfaceHolder(SurfaceHolder holder) {

        if (mForgroundSurfaceHolder == holder) {
            return;
        }

        if (holder != null) {
            Surface surface = holder.getSurface();
            if (surface != null && surface.isValid()) {
                mForgroundSurfaceHolder = holder;
            }
        }

        if (mForgroundSurfaceHolder != holder) {
            mForgroundSurfaceHolder = null;
        }

    }

    // service functions

    public boolean isPlaying() {
        MyMediaPlayer.PLAYBACK_STATE state = getPlaybackState();
        return state == MyMediaPlayer.PLAYBACK_STATE.PLAYING;
    }

    public MyMediaPlayer.PLAYBACK_TYPE getPlaybackType() {
        MyMediaPlayer.PLAYBACK_TYPE ret = MyMediaPlayer.PLAYBACK_TYPE.NONE;
        if (mMyAVPlayer.isActive()) {
            ret = MyMediaPlayer.PLAYBACK_TYPE.AV;
        } else if (mMySildShowPlayer.isActive()) {
            ret = MyMediaPlayer.PLAYBACK_TYPE.SLIDESHOW;
        }

        return ret;
    }

    public MyMediaPlayer.PLAYBACK_STATE getPlaybackState() {
        MyMediaPlayer.PLAYBACK_STATE ret = MyMediaPlayer.PLAYBACK_STATE.IDLE;
        MyMediaPlayer.PLAYBACK_TYPE type = getPlaybackType();
        switch (type) {
        case AV:
            ret = mMyAVPlayer.getPlaybackState();
            break;
        case SLIDESHOW:
            ret = mMySildShowPlayer.getPlaybackState();
            break;
        default:
            break;
        }
        return ret;
    }

    private DisplayManager mDisplayManager;

    @SuppressLint("NewApi")
	private Display findDisplayById(int id) {
        String displayCategory = DisplayManager.DISPLAY_CATEGORY_PRESENTATION;
        Display[] displays = mDisplayManager.getDisplays(displayCategory);

        Display ret = null;

        if (displays.length > 0) {
            ret = displays[0];
            for (Display d : displays) {
                if (d.getDisplayId() == id) {
                    ret = d;
                    break;
                }
            }
        }

        return ret;
    }
    
    public boolean isConnectDisplay(){
    	String displayCategory = DisplayManager.DISPLAY_CATEGORY_PRESENTATION;
        Display[] displays = mDisplayManager.getDisplays(displayCategory);
        if (displays.length > 0) 
        	return true;
        return false;
    }

    PresentationSlideShow mSlideShowPresentation;
    PresentationVideo mVideoPresentation;

    public void setDefaultDM(int width, int height) {
        mMyAVPlayer.setDM(width, height);
    }

    public void stopPlayback() {
        Log.i(TAG, "stopPlayback()");
        mMyAVPlayer.stopPlayback();
        mMySildShowPlayer.stopPlayback();
        dismissPresentation(mVideoPresentation, null);
        dismissPresentation(mSlideShowPresentation, null);
    }

    public void pausePlayback() {
    	Log.i(TAG, "pausePlayback()");
        if (mMyAVPlayer.getPlaybackState() == MyMediaPlayer.PLAYBACK_STATE.PAUSED) {
            mMyAVPlayer.start();
        } else if (mMySildShowPlayer.getPlaybackState() == MyMediaPlayer.PLAYBACK_STATE.PAUSED) {
            mMySildShowPlayer.resume();
        }
    }

    public void resumePlayback() {
    	Log.i(TAG, "resumePlayback()");
        if (mMyAVPlayer.getPlaybackState() == MyMediaPlayer.PLAYBACK_STATE.PLAYING) {
            mMyAVPlayer.pause();
        } else if (mMySildShowPlayer.getPlaybackState() == MyMediaPlayer.PLAYBACK_STATE.PLAYING) {
            mMySildShowPlayer.pause();
        }
    }

    public void swapLooping() {
        mMyAVPlayer.swapLooping();
        mMySildShowPlayer.swapLooping();
    }

    public void processActionCommand(Intent intent) {
        String action = intent.getAction();
        Leg.i(TAG, "process intent:" + action);
        if (action.startsWith(MyNotifier.SERVICE_ACTION_GENERIC)) {
            int command = intent.getIntExtra(MyNotifier.SERVICE_COMMAND_KEY, 0);
            switch (command) {
            case MyNotifier.SERVICE_COMMAND_RESUME:
                pausePlayback();
                break;
            case MyNotifier.SERVICE_COMMAND_PAUSE:
                resumePlayback();
                break;
            case MyNotifier.SERVICE_COMMAND_STOP:
                stopPlayback();
                break;
            }

            mNotifier.collapseStatusBar();
            notifyChange(WIDGET_CHANGE);
        }
    }

    public void fixAspect(int screenWidth, int screenHeight, int videoWidth, int videoHeight) {
        Log.i(TAG, String.format("screen(%d,%d), video(%d, %d)", screenWidth, screenHeight, videoWidth, videoHeight));
        float screenRatio = screenWidth * 1.0f / screenHeight;
        int surfaceWidth = screenWidth;
        int surfaceHeight = screenHeight;
        if (videoWidth < 176) {
            if (mForgroundSurfaceHolder != null) {
                mForgroundSurfaceHolder.setFixedSize(screenWidth, screenHeight);
                return;
            }
        }
        float videoRatio = videoWidth * 1.0f / videoHeight;

        Log.i(TAG, String.format("screen ratio%f, video ratio%f", screenRatio, videoRatio));
        if (screenRatio > videoRatio) {
            surfaceWidth = surfaceHeight * videoWidth / videoHeight;
        } else {
            surfaceHeight = surfaceWidth * videoHeight / videoWidth;
        }

        Log.i(TAG, String.format("surface w%d, h%d", surfaceWidth, surfaceHeight));

        if (mForgroundSurfaceHolder != null) {
            mForgroundSurfaceHolder.setFixedSize(surfaceWidth, surfaceHeight);
            return;
        }
    }

    @SuppressLint("NewApi")
	private static void dismissPresentation(Presentation p, Presentation pn) {
        if (p != null && p.isShowing()) {
            p.dismiss();
        }
        p = pn;
    }
    
    public void setPreviousPic(){
    	
    	mMySildShowPlayer.setPreviousPic();
    }
    
    public void setNextPic(){
    	mMySildShowPlayer.setNextPic();
    }
    
    public void setpausePlayback(){
    	
    	mMySildShowPlayer.setpausePlayback();
    }
    
    //
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	Log.i(TAG,"::::BroadcastReceiver:::::onReceive");
            String cmd = intent.getStringExtra("service_command");
            if (PlayerWidgetProvider.CMDAPPWIDGETUPDATE.equals(cmd)) {
            	notifyChange(WIDGET_CHANGE);
            }
        }
    };
    
    /**
     *  App widget: Notify the change-receivers that something has changed.
     */
    public void notifyChange(String what){
    	Log.i(TAG, "App widget notifyChange, what = "+what);
    	mAppWidgetProvider.notifyChange(this, what, getPlaybackState());
    }
    
    
}
