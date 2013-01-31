package com.example.tinyplayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.example.tinyplayer.InlineUtil.Leg;
import com.example.tinyplayer.widget.PlayerWidgetProvider;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.widget.RemoteViews;

import static com.example.tinyplayer.InlineUtil.*;

public class MyNotifier {

    public static final String TAG = MyNotifier.class.getSimpleName();
    private Context mApplicationContext;
    private PlaybackService mPlaybackService;
    private PlayerWidgetProvider mPlayerWidgetProvider = PlayerWidgetProvider.getInstance();

    public MyNotifier(PlaybackService service) {
        mPlaybackService = service;
        mApplicationContext = mPlaybackService.getApplicationContext();
    }

    public void cancel() {
        mNotificationManager.cancel(BACKGROUND_PLAYBACK_ONGOING);
        //update widget
        mPlayerWidgetProvider.performUpdate(mPlaybackService, null, MyMediaPlayer.PLAYBACK_STATE.IDLE);
    }

    public void notifyPlaybackState(MyMediaPlayer.PLAYBACK_STATE state) {
        Notification notification = buildNotificationICS(state);
        // notification.defaults = Notification.DEFAULT_SOUND;
        mNotificationManager.notify(BACKGROUND_PLAYBACK_ONGOING, notification);
        //update widget
        mPlayerWidgetProvider.performUpdate(mPlaybackService, null, state);
    }

    //
    public static final int BACKGROUND_PLAYBACK_ONGOING = 8001;

    public static final String SERVICE_STARTER_KEY = "service_starter";
    public static final String SERVICE_COMMAND_KEY = "service_command";
    public static final int BY_NOTIFICATION = 8001;

    //
    public static final int SERVICE_COMMAND_STOP = 8801;
    public static final int SERVICE_COMMAND_PAUSE = 8802;
    public static final int SERVICE_COMMAND_RESUME = 8803;
    public static final String SERVICE_ACTION_GENERIC = "com.example.tinyplayer." + SERVICE_COMMAND_KEY;
    public static final String SERVICE_ACTION_PAUSE = SERVICE_ACTION_GENERIC + "." + SERVICE_COMMAND_PAUSE;
    public static final String SERVICE_ACTION_RESUME = SERVICE_ACTION_GENERIC + "." + SERVICE_COMMAND_RESUME;
    public static final String SERVICE_ACTION_STOP = SERVICE_ACTION_GENERIC + "." + SERVICE_COMMAND_STOP;

    // ======about notification====== //
    private NotificationManager mNotificationManager;
    public static PendingIntent mActivityIntent = null;
    public static PendingIntent mServicePauseIntent = null;
    public static PendingIntent mServiceResumeIntent = null;
    public static PendingIntent mServiceStopIntent = null;
    private Intent serviceIntent1 = null;
    private Intent serviceIntent2 = null;
    private Intent serviceIntent3 = null;

    private IntentFilter mNotificationFilter = null;
    private BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Leg.i(TAG, "process intent:" + action);
            if (action.startsWith(SERVICE_ACTION_GENERIC)) {
                mPlaybackService.processActionCommand(intent);
            }
        }
    };

    public void initNotification() {
        mNotificationManager = (NotificationManager) mPlaybackService.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent activityIntent = new Intent(mPlaybackService, Player.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mActivityIntent = PendingIntent.getActivity(mApplicationContext, 0, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        serviceIntent1 = new Intent(SERVICE_ACTION_PAUSE);
        serviceIntent1.setClass(mPlaybackService, PlaybackService.class);
        serviceIntent1.putExtra(SERVICE_COMMAND_KEY, SERVICE_COMMAND_PAUSE);
        serviceIntent1.putExtra(SERVICE_STARTER_KEY, BY_NOTIFICATION);

        serviceIntent2 = new Intent(SERVICE_ACTION_RESUME);
        serviceIntent2.setClass(mPlaybackService, PlaybackService.class);
        serviceIntent2.putExtra(SERVICE_COMMAND_KEY, SERVICE_COMMAND_RESUME);
        serviceIntent2.putExtra(SERVICE_STARTER_KEY, BY_NOTIFICATION);

        serviceIntent3 = new Intent(SERVICE_ACTION_STOP);
        serviceIntent3.setClass(mPlaybackService, PlaybackService.class);
        serviceIntent3.putExtra(SERVICE_COMMAND_KEY, SERVICE_COMMAND_STOP);
        serviceIntent3.putExtra(SERVICE_STARTER_KEY, BY_NOTIFICATION);

        mServicePauseIntent = PendingIntent.getService(mApplicationContext, 0, serviceIntent1, 0);
        mServiceResumeIntent = PendingIntent.getService(mApplicationContext, 0, serviceIntent2, 0);
        mServiceStopIntent = PendingIntent.getService(mApplicationContext, 0, serviceIntent3, 0);

        mNotificationFilter = new IntentFilter();
        mNotificationFilter.addAction(SERVICE_ACTION_GENERIC);
        mNotificationFilter.addAction(SERVICE_ACTION_PAUSE);
        mNotificationFilter.addAction(SERVICE_ACTION_RESUME);
        mNotificationFilter.addAction(SERVICE_ACTION_STOP);

        mPlaybackService.registerReceiver(mNotificationReceiver, mNotificationFilter);

    }

    public void uninitNotification() {
        mPlaybackService.unregisterReceiver(mNotificationReceiver);
    }

    // build notification ICS
    private Notification buildNotificationICS(MyMediaPlayer.PLAYBACK_STATE state) {
        Notification notification = null;

        RemoteViews localRemoteViews1 = new RemoteViews(mApplicationContext.getPackageName(), R.layout.statusbar);
        setUpNotificationRemoteView(localRemoteViews1, state);

        notification = new Notification();
        notification.flags = (Notification.DEFAULT_VIBRATE | notification.flags);
        notification.icon = R.drawable.icon_small;
        notification.contentIntent = mActivityIntent;
        notification.contentView = localRemoteViews1;

        if (isJellyBeanOrGreater()) {
            RemoteViews localRemoteViews2 = new RemoteViews(mApplicationContext.getPackageName(),
                    R.layout.statusbar_expended);
            setUpNotificationRemoteView(localRemoteViews2, state);

            setNotificationBigContentView(notification, localRemoteViews2);
        }

        switch (state) {
        case IDLE:
            break;
        case PAUSED:
            break;
        case PLAYING:
            break;
        default:
            break;

        }
        return notification;
    }

    private void setUpNotificationRemoteView(RemoteViews paramRemoteViews, MyMediaPlayer.PLAYBACK_STATE state) {

        paramRemoteViews.setImageViewResource(R.id.albumart, R.drawable.icon_large);
        // mActivityIntent
        // paramRemoteViews.setOnClickPendingIntent(R.id.notificationbg,
        // mActivityIntent);
        paramRemoteViews.setOnClickPendingIntent(R.id.stop, mServiceStopIntent);
        paramRemoteViews.setOnClickPendingIntent(R.id.veto, mServiceStopIntent);

        switch (state) {
        case IDLE:
            break;
        case PAUSED:
            paramRemoteViews.setTextViewText(R.id.trackname, "Paused");
            paramRemoteViews.setOnClickPendingIntent(R.id.playpause, mServiceResumeIntent);
            paramRemoteViews.setImageViewResource(R.id.playpause, R.drawable.btn_playback_play_normal_jb_dark);
            break;
        case PLAYING:
            paramRemoteViews.setTextViewText(R.id.trackname, "Playing");
            paramRemoteViews.setOnClickPendingIntent(R.id.playpause, mServicePauseIntent);
            paramRemoteViews.setImageViewResource(R.id.playpause, R.drawable.btn_playback_pause_normal_jb_dark);
            break;
        default:
            break;

        }

        if (isJellyBeanOrGreater()) {
            paramRemoteViews.setTextViewText(R.id.artistalbum, "Here should be artist&album");
        } else {
            paramRemoteViews.setTextViewText(R.id.artist, "Here shows artist");
            paramRemoteViews.setTextViewText(R.id.album, "Here shows album");

        }

    }

    private void setNotificationBigContentView(Notification notification, RemoteViews remoteView) {
        try {
            Field localField = Notification.class.getField("bigContentView");
            localField.set(notification, remoteView);
        } catch (Exception e) {

        }

    }

    // build notification JB
    Notification buildNotificationJB(MyMediaPlayer.PLAYBACK_STATE state) {
        Resources res = mApplicationContext.getResources();
        Notification.Builder builder = null;
        Notification notification = null;
        switch (state) {
        case IDLE:
            break;
        case PAUSED:
            builder = new Notification.Builder(mApplicationContext);
            builder.setContentIntent(mActivityIntent)
                    .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.icon_large))
                    .setSmallIcon(R.drawable.icon_small).setTicker("B5 Player").setWhen(System.currentTimeMillis())
                    .setAutoCancel(false).setContentTitle("Playing in background").setContentTitle("Playing")
                    .setContentText("We should display video name here.");
            // builder.addAction(R.drawable.ic_media_play, "Resume",
            // mServiceResumeIntent);
            // builder.addAction(R.drawable.ic_media_stop, "Stop",
            // mServiceStopIntent);
            // notification = builder.build();
            break;
        case PLAYING:
            builder = new Notification.Builder(mApplicationContext);
            builder.setContentIntent(mActivityIntent)
                    .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.icon_large))
                    .setSmallIcon(R.drawable.icon_small).setTicker("B5 Player").setWhen(System.currentTimeMillis())
                    .setAutoCancel(false).setContentTitle("Playing in background").setContentTitle("Playing")
                    .setContentText("We should display video name here.");

            // builder.addAction(R.drawable.ic_media_pause, "Pause",
            // mServicePauseIntent);
            // builder.addAction(R.drawable.ic_media_stop, "Stop",
            // mServiceStopIntent);
            // notification = builder.build();
            break;
        default:
            break;
        }
        return notification;
    }

    public void collapseStatusBar() {
        Leg.d(TAG, "collapseStatusBar()");
        try {

            Object statusBarManager = mPlaybackService.getSystemService("statusbar");
            Method collapse;

            if (Build.VERSION.SDK_INT <= 16) {
                collapse = statusBarManager.getClass().getMethod("collapse");
            } else {
                collapse = statusBarManager.getClass().getMethod("collapsePanels");
            }
            collapse.invoke(statusBarManager);
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }

}
