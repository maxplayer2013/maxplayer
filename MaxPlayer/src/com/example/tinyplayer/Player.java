package com.example.tinyplayer;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.MediaRouteActionProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.MediaRouter;
import android.media.MediaRouter.RouteInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tinyplayer.InlineUtil.Leg;
import com.example.tinyplayer.MyMediaPlayer.PLAYBACK_STATE;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.CanvasTransformer;
import com.slidingmenu.lib.app.SlidingActivity;

@SuppressLint("NewApi")
public class Player extends SlidingActivity implements OnClickListener, OnTouchListener {

    public static final String TAG = Player.class.getSimpleName();

    // Button mPlayInBackground;
    // Button mGoBackground;
    // Button mStopPlayback;
    // Button mPickVideo;

    ImageView mBackgroundImage;
    SurfaceView mVideoSurface;
    ImageView mPauseImage;

    ImageButton mNextButton_slider;
    ImageButton mPreviousButton_slider;
    ImageButton mPlay_pauseButton_slider;

    SurfaceHolder mSurfaceHolder;

    //
    AlertDialog mPresentationAvailableDialog;

    //
    private MediaRouter mMediaRouter;
    private DisplayManager mDisplayManager;
    private IntentFilter mIntentfl;
    
    private View mediaControllerLayout;
    private SeekBar mSeekBar;
    private ImageButton playButton;
    private ScheduledExecutorService scheduledExecutorService;
    private Handler handler;
    //
    DisplayMetrics mDefaultDM;
    
    private ImageView slidingMenu_open;
    private TextView slidingMenu_open_text;
    private ImageView slidingMenu_repeat;
    private TextView slidingMenu_repeat_text;
    private ImageView slidingMenu_about;
    private TextView slidingMenu_about_text;
    private CheckBox slidingMenu_desktop;
    private TextView slidingMenu_desktop_text;
    
    private Integer myInt;

    public Player() {
        super();
        mIntentfl = new IntentFilter();
        mIntentfl.addAction("com.example.player.isShowSliding");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Leg.i(TAG, "onCreate()================================");
        super.onCreate(savedInstanceState);

        if (!isPortrait()) {
            // requestWindowFeature(Window.FEATURE_NO_TITLE);
            updateFullscreenStatus(true);
        }
        setContentView(R.layout.activity_player);

        mMediaRouter = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);
        mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);

        mContentView = getWindow().getDecorView().findViewById(android.R.id.content);
        mBackgroundImage = (ImageView) findViewById(R.id.background);

//        mPauseImage = (ImageView) findViewById(R.id.play_pause);
        mVideoSurface = (SurfaceView) findViewById(R.id.video_surface);

        mPreviousButton_slider = (ImageButton) findViewById(R.id.prev);
        mPlay_pauseButton_slider = (ImageButton) findViewById(R.id.pause);
        mNextButton_slider = (ImageButton) findViewById(R.id.next);

        mVideoSurface.setZOrderOnTop(true);
        mVideoSurface.setAlpha((float) 0.18);
        mVideoSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);

//        mPauseImage.setOnClickListener(this);
        mVideoSurface.setOnClickListener(this);
        mVideoSurface.setOnTouchListener(this);

        mPreviousButton_slider.setOnClickListener(this);
        mPlay_pauseButton_slider.setOnClickListener(this);
        mNextButton_slider.setOnClickListener(this);

        //
        mPredict = 0;
        getSurfaceHolder();

        //
        mDefaultDM = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mDefaultDM);

        this.registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                dismissSlidingController();
            }

        }, mIntentfl);
        
        mediaControllerLayout = findViewById(R.id.mediaControllerLayout);
        mSeekBar = (SeekBar) findViewById(R.id.videoSeekBar);
        
        
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar,
					int progress, boolean fromUser) {
				if (fromUser) {
					mPlaybackService.seekTo((int) (progress * 1.0
							/ seekBar.getMax() * (mPlaybackService.getDuration())));
					seekBar.setProgress(progress);
				}
			}
        });
        
		scheduledExecutorService = Executors.newScheduledThreadPool(1);
		handler = new Handler();
        
		scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						Log.d(TAG, "test-----");
						if (mPlaybackService.isPlaying()) {
							Log.d(TAG, "test++++");
							int position = mPlaybackService
									.getCurrentPosition();
							int duration = mPlaybackService.getDuration();
							Log.d(TAG, "test++++" + Integer.toString(duration) + Integer.toString(position));
							mSeekBar.setProgress((int) ((float)position
									/ (float)duration * mSeekBar.getMax()));
							
							mediaControllerLayout.setVisibility(View.VISIBLE);
							playButton.setImageResource(R.drawable.ic_media_pause);
							
							Log.d(TAG, "test++++" + (int) (position
									/ duration * mSeekBar.getMax()));
						}
					}
				});

			}
		}, 1000, 1000, TimeUnit.MILLISECONDS);
        
        playButton = (ImageButton) findViewById(R.id.playButton);
        playButton.setOnClickListener(this);
        
        
        //icon show sliding menu
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        //sliding menu
		 setBehindContentView(R.layout.slidingmenu_left);
		 
		 // customize the SlidingMenu
		 SlidingMenu sm = getSlidingMenu();
		 sm.setMode(SlidingMenu.LEFT);
		 sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		 sm.setFadeDegree(0.0f);
		 sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		 sm.setBehindScrollScale(0.0f);
		 //animation
		 sm.setBehindCanvasTransformer(
				 new CanvasTransformer(){
					@Override
					public void transformCanvas(Canvas canvas, float percentOpen) {
						// TODO Auto-generated method stub
						Log.d(TAG, "testAni---------------");
						canvas.translate(0, 0);
					}
		 });

        slidingMenu_open = (ImageView) findViewById(R.id.sliding_open);
        slidingMenu_open_text = (TextView) findViewById(R.id.sliding_open_text);
        slidingMenu_repeat = (ImageView) findViewById(R.id.sliding_loop);
        slidingMenu_repeat_text = (TextView) findViewById(R.id.sliding_loop_text);
        slidingMenu_about = (ImageView) findViewById(R.id.sliding_about);
        slidingMenu_about_text = (TextView) findViewById(R.id.sliding_about_text);
        slidingMenu_desktop = (CheckBox) findViewById(R.id.sliding_desktop);
        slidingMenu_desktop_text = (TextView) findViewById(R.id.sliding_desktop_text);
        
        slidingMenu_open.setOnClickListener(this);
        slidingMenu_open_text.setOnClickListener(this);
        
        slidingMenu_repeat.setOnClickListener(this);
        slidingMenu_repeat_text.setOnClickListener(this);
        
        slidingMenu_about.setOnClickListener(this);
        slidingMenu_about_text.setOnClickListener(this);
        
        slidingMenu_desktop_text.setOnClickListener(this);
    }
		 
    private void showSlidingController() {
        mPreviousButton_slider.setVisibility(View.VISIBLE);
        mPlay_pauseButton_slider.setVisibility(View.VISIBLE);
        mNextButton_slider.setVisibility(View.VISIBLE);
    }

    private void dismissSlidingController() {
        mPreviousButton_slider.setVisibility(View.INVISIBLE);
        mPlay_pauseButton_slider.setVisibility(View.INVISIBLE);
        mNextButton_slider.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onRestart() {
        Leg.i(TAG, "onRestart()");
        super.onRestart();
    }

    @Override
    protected void onStart() {
        Leg.i(TAG, "onStart()=============================================");
        mPredict = 0;
        handleStartIntent();
        super.onStart();
    }

    @Override
    protected void onStop() {
        Leg.i(TAG, "onStop()=================================================");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Leg.i(TAG, "onDestory()=============================================");
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        if (presentationRouteSelected()) {
            mBackgroundImage.setImageResource(R.drawable.miracast);
            /*
             * if(MySlideShowPlayer.isImage){ showSlidingController(); }
             */
        } else {
            mBackgroundImage.setImageResource(R.drawable.background);
        }

        if (MySlideShowPlayer.isImage) {
            showSlidingController();
        }

        Leg.i(TAG, "onResume()============================================================");
        doBindPlaybackService();
        mMediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mMediaRouterCallback);
        mSurfaceHolder.addCallback(mFgSurfaceHolderCB);
        PlaybackService.STOP_SERVICE_ON_COMPLETE = false;   
        super.onResume();
    }

    @Override
    protected void onPause() {
        Leg.i(TAG, "onPause()==============================================================================");

        MyMediaPlayer.PLAYBACK_STATE state = mPlaybackService.getPlaybackState();
        Leg.i(TAG, "current play state is: " + state);
        switch (state) {
        case PAUSED:
            break;
        case PLAYING:
            Leg.i(TAG, "going to start background pb");
            startPlaybackInService(false, false);
            break;
        default:
            break;

        }

        mMediaRouter.removeCallback(mMediaRouterCallback);
        PlaybackService.STOP_SERVICE_ON_COMPLETE = true;
        doUnbindPlaybackService();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Leg.i(TAG, "onNewIntent()");
        setIntent(intent);
        super.onNewIntent(intent);
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_player, menu);
        
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.menu_media_route);
        if (mediaRouteMenuItem != null) {
            MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) mediaRouteMenuItem
                    .getActionProvider();
            mediaRouteActionProvider.setRouteTypes(MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
        }
        return true;
    }
    
   

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
        if (mPlaybackService != null)
            if (mPlaybackService.mMyAVPlayer != null)
                Leg.d(TAG, "onConfigurationChanged   ======================================================="
                        + mPlaybackService.mMyAVPlayer.getCurrentPosition() + mPlaybackService.getPlaybackState());
        
        mPlaybackService.mMyAVPlayer.getCurrentPosition(); 
    }

    @Override
    public void onBackPressed() {
        if (mPlaybackService != null) {
            mPlaybackService.stopPlayback();
        }
        finish();
    }

    @Override
    public void onClick(View v) {
        if (mPlaybackService == null)
            return;
        if (v == mVideoSurface || v == playButton) {
            if (mPlaybackService != null) {
                MyMediaPlayer.PLAYBACK_STATE state = mPlaybackService.getPlaybackState();
                switch (state) {
                case PAUSED:
                    mPlaybackService.resumePlayback();
                    mediaControllerLayout.setVisibility(View.VISIBLE);
                    playButton.setImageResource(R.drawable.ic_media_pause);
//                    mPauseImage.setImageResource(R.drawable.ic_media_pause);
                    break;
                case PLAYING:
                    mPlaybackService.pausePlayback();
                    mediaControllerLayout.setVisibility(View.VISIBLE);
                    playButton.setImageResource(R.drawable.ic_media_play);
//                    mPauseImage.setImageResource(R.drawable.ic_media_play);
                    break;
                default:
                    pickVideo();
                    break;

                }

            }
        } else if (v == mNextButton_slider) {

            mPlaybackService.setNextPic();

            Leg.i(TAG, "mNextButton_slider ====================== ");

        } else if (v == mPreviousButton_slider) {
            Leg.i(TAG, "mPreviousButton_slider ====================== ");
            mPlaybackService.setPreviousPic();

        } else if (v == mPlay_pauseButton_slider) {
            mPlaybackService.setpausePlayback();

        } else if (v == slidingMenu_open || v == slidingMenu_open_text) {
        	pickVideo();
        	showContent();
        	
        } else if (v == slidingMenu_repeat || v == slidingMenu_repeat_text) {
        	if (!mPlaybackService.isLooping()) {
            	switchLooping();
            	slidingMenu_repeat.setImageResource(R.drawable.ic_mp_repeat_all_btn);
        	} else {
        		switchLooping();
        		slidingMenu_repeat.setImageResource(R.drawable.ic_mp_repeat_off_btn);
        	}

        }  else if (v == slidingMenu_about || v == slidingMenu_about_text ) {
        	openAboutDialog();
        	
        } else if (v == slidingMenu_desktop_text) {
        	if (slidingMenu_desktop.isChecked()) {
        		slidingMenu_desktop.setChecked(false);
        	} else {
        		slidingMenu_desktop.setChecked(true);
        	}
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
//        mPauseImage.setVisibility(View.VISIBLE);
        dismissControl();
        return false;
    }

    private void dismissControl() {
        mHandler.removeMessages(DISMISS_CONTROL);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(DISMISS_CONTROL), 4000);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_switch_orientation:
            switchOrientation();
            break;
        case R.id.menu_item_switch_looping:
            switchLooping();
            break;
        case R.id.menu_item_start_playback:
            // onClick(mPlayInBackground);
            break;
        case R.id.menu_item_stop_playback:
            // onClick(mStopPlayback);
            break;
        case R.id.menu_item_about:
            openAboutDialog();
            break;
        case R.id.menu_item_pick_video:
            pickVideo();
//            mPauseImage.setImageResource(R.drawable.ic_media_pause);
            break;
        case android.R.id.home:
        	  Log.d(TAG, "show slidingmenu------");
        	  showMenu();
        	  break;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openAboutDialog() {
        Dialog diaLeg = new AlertDialog.Builder(this)
                .setTitle("MaxPlayer")
                .setMessage(
                		"Version 1.2: \nXie Chen.\nAdded sliding menu."
                        +"\n\nVersion 1.1: \nOuyang Jinmiao, Xie Chen.\n1.Added AppWidget.\n2.Fixed some issues."
                                + "\n\nVersion 1.0: \nTian Yu, Ouyang JinMiao, Liu Bing, Zhang Lei, Xie Chen."
                                + "\n1.Added Mircast on Actionbar.\n2.Support playing in backService for video and pictures slide-show.")
                .setNegativeButton("ok", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface diaLeg, int which) {
                        diaLeg.dismiss();
                    }

                }).create();
        diaLeg.show();
    }

    private void showPresentationAvailableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.display_model_select).setTitle("MaxPlayer");

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface diaLeg, int id) {
                startPlaybackInService(false, false);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface diaLeg, int id) {
            }
        });
        mPresentationAvailableDialog = builder.create();
        mPresentationAvailableDialog.show();
    }

    // ====== surface holder ====== //
    private SurfaceHolder getSurfaceHolder() {
        mVideoSurface = (SurfaceView) findViewById(R.id.video_surface);

        if (mVideoSurface != null) {
            mSurfaceHolder = mVideoSurface.getHolder();
            mSurfaceHolder.addCallback(mFgSurfaceHolderCB);
        }

        return mSurfaceHolder;
    }

    private FgSurfaceHolderCallback mFgSurfaceHolderCB = new FgSurfaceHolderCallback();

    class FgSurfaceHolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Surface surface = holder.getSurface();
            if (surface != null && surface.isValid()) {
                evaluatePredict(START_PRD_SURFACE_VALID);
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mPredict &= ~START_PRD_SURFACE_VALID;
        }

    }

    // ====== handler ====== //
    public static final int BASE = 1000;
    public static final int START_PLAY_BACK = BASE + 1;
    public static final int STOP_PLAY_BACK = BASE + 2;
    public static final int DISMISS_CONTROL = BASE + 3;
    Handler mHandler = new MyHandler(this);

    static class MyHandler extends Handler {

        Player mPlayer;

        public MyHandler(Player player) {
            mPlayer = player;
        }

        @Override
        public void handleMessage(Message msg) {
            int code = msg.what;
            switch (code) {
            case START_PLAY_BACK:
                if (mPlayer.presentationRouteSelected()) {
                    mPlayer.startPlaybackInService(false, true);
                } else {
                    mPlayer.startPlaybackInService(true, true);
                }
                break;
            case STOP_PLAY_BACK:
                if (mPlayer.mPlaybackService != null) {
                    mPlayer.mPlaybackService.stopPlayback();
                }
                break;
            case DISMISS_CONTROL:
                if (mPlayer.mPlaybackService.isPlaying()) {
                    Leg.d(TAG, "is playing and dismiss diaLeg timeout 4s");
//                    mPlayer.mPauseImage.setVisibility(View.INVISIBLE);
                }
                break;
            }
            super.handleMessage(msg);
        }

    }

    // ====== about pick file ====== //
    public static final int PICK_VIDEO_REQUEST_CODE = 1;

    void pickVideo() {
        Intent mediaChooser = new Intent(Intent.ACTION_GET_CONTENT);
        mediaChooser.setType("video/*");
        startActivityForResult(mediaChooser, PICK_VIDEO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Leg.i(TAG,
                String.format("================================requestCode:%d, resultCode:%d", requestCode, resultCode));
        if (requestCode == PICK_VIDEO_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                mUris.clear();
                Uri uri = data.getData();

                if (uri != null) {
                    Intent newIntent = new Intent(mStartIntent);
                    newIntent.setAction("INNER_ACTION_VIDEO_PICKED");
                    newIntent.setData(uri);
                    setIntent(newIntent);
                }

            } else {
                // don't set new video path
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // ====== using service ====== //
    // start fg, start bg, continue bg

    void startPlaybackInService(boolean fg, boolean freshStart) {
        Leg.i(TAG,
                "startPlaybackInService =====================  mUris : " + mUris.size() + ", muris :"
                        + mUris.toString());

        Intent localIntent = mStartIntent;
        localIntent.setClass(this, PlaybackService.class);
        localIntent.putExtra("com.example.tinyplayer.Binding", false);
        localIntent.putExtra("com.example.tinyplayer.CurrentPosition", 0);
        localIntent.putExtra("com.example.tinyplayer.StartAtOnce", true);
        localIntent.putParcelableArrayListExtra("com.example.tinyplayer.Uris", mUris);

        localIntent.putExtra("com.example.tinyplayer.Foreground", fg);
        localIntent.putExtra("com.example.tinyplayer.FreshStart", freshStart);
        localIntent.putExtra("com.example.tinyplayer.PresentationDisplayId", mPresentationId);

        if (mPlaybackService == null) {
            if (fg || freshStart) {
                Leg.i(TAG, "start new service");
                startService(localIntent);
            }

        } else {
            Leg.i(TAG, "use existing service");
            mPlaybackService.continueInBackground(localIntent);
        }

    }

    public void switchLooping() {
        if (mPlaybackService != null) {
            mPlaybackService.swapLooping();
        }
    }

    // ====== about bind service ====== //
    private PlaybackService mPlaybackService;
    private boolean mIsBoundToPlaybackService;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mIsBoundToPlaybackService = true;
            mPlaybackService = ((PlaybackService.LocalBinder) service).getService();

            mPlaybackService.setDefaultDM(mDefaultDM.widthPixels, mDefaultDM.heightPixels);

            evaluatePredict(START_PRD_SERVICE_BOUND);

            Toast.makeText(Player.this, "Bound to background playback service", Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            mIsBoundToPlaybackService = false;
            mPlaybackService = null;
            Toast.makeText(Player.this, "Unbound from background playback service", Toast.LENGTH_SHORT).show();
        }
    };

    void doBindPlaybackService() {
        if (mIsBoundToPlaybackService && mPlaybackService != null) {
            return;
        }
        Intent localIntent = getIntent();
        localIntent.setClass(this, PlaybackService.class);
        localIntent.putExtra("com.example.tinyplayer.Binding", true);
        localIntent.putExtra("com.example.tinyplayer.CurrentPosition", 0);
        localIntent.putExtra("com.example.tinyplayer.StartAtOnce", false);
        ComponentName name = startService(localIntent);
        Leg.i(TAG, "going to bound service ,++++++++++++++++++++++++++++++++++ name : " + name);
        if (name != null) {
            bindService(new Intent(Player.this, PlaybackService.class), mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    void doUnbindPlaybackService() {
        if (mIsBoundToPlaybackService) {
            if (mPlaybackService != null) {
                mPlaybackService.setForegroundSurfaceHolder(null);
            }
            unbindService(mConnection);
            mIsBoundToPlaybackService = false;
        }
    }

    // ====== about media route ====== //
    private int mPresentationId = -1;
    DisplayMetrics mPresentationDM;

    private void onPresentationDisplayChanged(Display pD) {
        int newId = (pD != null) ? pD.getDisplayId() : Display.DEFAULT_DISPLAY;

        if (mPresentationId == newId) {
            return;
        }

        // int old = mPresentationId;
        mPresentationId = newId;
        if (Display.DEFAULT_DISPLAY != mPresentationId) {
            // pD.getMetrics(mPresentationDM);
            pD.getWidth();
            pD.getHeight();

            showPresentationAvailableDialog();
            mBackgroundImage.setImageResource(R.drawable.miracast);
        } else {
            dismissSlidingController();
            mBackgroundImage.setImageResource(R.drawable.background);
        }
    }

    @SuppressLint("NewApi")
    private final MediaRouter.SimpleCallback mMediaRouterCallback = new MediaRouter.SimpleCallback() {
        @Override
        public void onRouteSelected(MediaRouter router, int type, RouteInfo info) {
            Leg.d(TAG, "onRouteSelected: type=" + type + ", info=" + info);
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, RouteInfo info) {
            Leg.d(TAG, "onRouteUnselected: type=" + type + ", info=" + info);
        }

        @Override
        public void onRoutePresentationDisplayChanged(MediaRouter router, RouteInfo info) {
            Leg.d(TAG, "onRoutePresentationDisplayChanged: info=" + info);

            Display pDisp = info.getPresentationDisplay();
            Leg.d(TAG, "info.isEnabled: " + info.isEnabled());
            if (pDisp != null) {
                Leg.d(TAG, "display.isValid: " + pDisp.isValid());
                Player.this.onPresentationDisplayChanged(pDisp);
            } else {
                Player.this.onPresentationDisplayChanged(null);
            }

        }
    };

    public boolean presentationRouteSelected() {
        boolean ret = false;
        MediaRouter.RouteInfo info = mMediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
        Display pD = info.getPresentationDisplay();
        if (pD != null && pD.getDisplayId() != Display.DEFAULT_DISPLAY) {
            ret = true;
        }
        return ret;
    }

    // ====== about start intent ====== //
    Intent mStartIntent = null;

    static final int START_PRD_START_INTENT = 1;
    static final int START_PRD_SERVICE_BOUND = 2;
    static final int START_PRD_SURFACE_VALID = 4;
    static final int START_PRD_SI_SB_SV = 7;
    static final int START_PRD_SB_SV = 6;

    int mPredict = 0;

    private ArrayList<Uri> mUris = new ArrayList<Uri>();

    private void handleStartIntent() {
        Leg.i(TAG, "handleStartIntent");
        mStartIntent = getIntent();

        if (mStartIntent == null) {
            return;
        }

        String action = mStartIntent.getAction();
        String type = mStartIntent.getType();
        Leg.i(TAG, "action: " + action + ", type: " + type);

        if ("INNER_ACTION_PROCESSED".equals(action)) {
            return;
        }

        boolean sliding = mStartIntent.getBooleanExtra("com.example.tinyplayer.sliding", false);

        if (sliding) {
            // dismissSlidingController();
        }

        Intent newIntent = new Intent(mStartIntent);
        newIntent.setAction("INNER_ACTION_PROCESSED");
        setIntent(newIntent);
        // android.intent.action.MAIN

        mUris.clear();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Leg.i(TAG, "ACTION_SEND: " + type.toString());
            if (type.startsWith("image/")) {
                fetchUris(2, false, mStartIntent);

            } else if (type.startsWith("video/")) {
                fetchUris(1, false, mStartIntent);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            Leg.i(TAG, "ACTION_SEND_MULTIPLE: " + type.toString());
            if (type.startsWith("image/")) {
                fetchUris(2, true, mStartIntent);
                showSlidingController();
            } else if (type.startsWith("video/")) {
                fetchUris(1, true, mStartIntent);

            } else if (type.startsWith("*/")) {
                fetchUris(0, true, mStartIntent);

            }
        } else if ("INNER_ACTION_VIDEO_PICKED".equals(action)) {
            mUris.clear();
            Uri temp = mStartIntent.getData();
            if (temp != null) {
                mUris.add(temp);
            }
        }

        int size = mUris.size();
        Leg.i(TAG, "size of mUris: " + size);

        if (size >= 1) {
            evaluatePredict(START_PRD_START_INTENT);
        }

    }

    private void fetchUris(int i, boolean mutiple, Intent intent) {
        mUris.clear();
        if (mutiple) {
            ArrayList<Uri> tempUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            mUris.addAll(tempUris);
        } else {
            Uri tempUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            mUris.add(tempUri);
        }
        int size = mUris.size();
        Leg.i(TAG, "size of mUris in fetchUris()" + size);
        if (size <= 0) {
            return;
        }
    }

    private void calculatePredict(int mask) {
        Leg.i(TAG, "calculatePredict(): " + mask);
        mPredict |= mask;
    }

    private void evaluatePredict(int mask) {
        Leg.i(TAG, "evaluatePredict(" + mask + ")");
        calculatePredict(mask);

        Leg.i(TAG, "calculated predict: " + mPredict);
        if ((mPredict & START_PRD_SB_SV) == START_PRD_SB_SV) {
            Leg.i(TAG, "prediction: foreground surface");
            Surface surface = mSurfaceHolder.getSurface();
            if (surface != null && surface.isValid()) {
                mPlaybackService.setForegroundSurfaceHolder(mSurfaceHolder);
            } else {
                mPlaybackService.setForegroundSurfaceHolder(null);
            }
        }

        if ((mPredict & START_PRD_SI_SB_SV) == START_PRD_SI_SB_SV) {
            Leg.i(TAG, "prediction: send start playback message");
            mHandler.removeMessages(START_PLAY_BACK);
            mHandler.removeMessages(STOP_PLAY_BACK);
            if (!mPlaybackService.isPlaying()) {
                mHandler.obtainMessage(STOP_PLAY_BACK).sendToTarget();
                mHandler.obtainMessage(START_PLAY_BACK).sendToTarget();
            }
        }

    }

    // ====== about orientation ====== //
    View mContentView;

    private boolean isPortrait() {
        int orien = getResources().getConfiguration().orientation;
        return orien == Configuration.ORIENTATION_PORTRAIT;
    }

    private void updateFullscreenStatus(boolean useFullScreen) {
        if (useFullScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        // mContentView.requestLayout();
    }

    private void switchOrientation() {
        if (isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @SuppressWarnings("unused")
    private int getOrientation() {
        return getResources().getConfiguration().orientation;
    }

    // @Override
    // public void onConfigurationChanged(Configuration newConfig) {
    // Auto-generated method stub
    // super.onConfigurationChanged(newConfig);
    // }

}
