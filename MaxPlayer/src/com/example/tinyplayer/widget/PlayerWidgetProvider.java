package com.example.tinyplayer.widget;

import com.example.tinyplayer.MyMediaPlayer;
import com.example.tinyplayer.MyNotifier;
import com.example.tinyplayer.PlaybackService;
import com.example.tinyplayer.Player;
import com.example.tinyplayer.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class PlayerWidgetProvider extends AppWidgetProvider {
    static final String TAG = "Max PlayerWidgetProvider";

    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate";
    public static final String MIRACAST_CONTROL_ACTION = "miracast_control_action";

    private static PlayerWidgetProvider sInstance;

    public static synchronized PlayerWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new PlayerWidgetProvider();
        }
        return sInstance;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // TODO Auto-generated method stub
        // super.onUpdate(context, appWidgetManager, appWidgetIds);
        defaultAppWidget(context, appWidgetIds);

        // Send broadcast intent to any running MediaPlaybackService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(MyNotifier.SERVICE_STARTER_KEY);
        updateIntent.putExtra(MyNotifier.SERVICE_COMMAND_KEY, PlayerWidgetProvider.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }

    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_info_new);

        views.setTextViewText(R.id.trackname, res.getText(R.string.app_name));
        views.setTextViewText(R.id.artist, res.getText(R.string.widget_initial_text));

        // Link actions buttons to intents
        linkButtons(context, views);
        pushUpdate(context, appWidgetIds, views);
    }

    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to
        // all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(new ComponentName(context, this.getClass()), views);
        }
    }

    /**
     * Check against {@link AppWidgetManager} if there are any instances of this
     * widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this.getClass()));
        return (appWidgetIds.length > 0);
    }

    /**
     * Handle a change notification coming over from {@link PlaybackService}
     */
    public void notifyChange(PlaybackService service, String what, MyMediaPlayer.PLAYBACK_STATE state) {
        Log.i(TAG, "hasInstances(service) = " + hasInstances(service));
        if (hasInstances(service)) {
            performUpdate(service, null, state);
        }
    }

    static boolean mPlaying;
    static boolean mConnecting;

    /**
     * Update all active widget instances by pushing changes
     */
    public void performUpdate(PlaybackService service, int[] appWidgetIds, MyMediaPlayer.PLAYBACK_STATE state) {
        Log.i(TAG, "performUpdate(PlaybackService" + service + ",int[]" + appWidgetIds + ")");
        final Resources res = service.getResources();
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.appwidget_info_new);

        // Set correct drawable for pause state
        mPlaying = service.isPlaying();
        mConnecting = service.isConnectDisplay();

        Log.i(TAG, "playing = " + service.isPlaying());
        Log.i(TAG, "connecting = " + service.isConnectDisplay());

        // if (mConnecting) {
        // views.setTextViewText(R.id.trackname,
        // res.getText(R.string.app_name));
        // views.setTextViewText(R.id.artist, "connected.");
        // } else {
        // views.setTextViewText(R.id.trackname,
        // res.getText(R.string.app_name));
        // views.setTextViewText(R.id.artist,
        // res.getText(R.string.widget_initial_text));
        // }

        switch (state) {
        case IDLE:
            views.setImageViewResource(R.id.play, R.drawable.btn_playback_play_normal_jb_dark);
            views.setOnClickPendingIntent(R.id.play, null);
            views.setTextViewText(R.id.artist, res.getText(R.string.widget_initial_text));
            break;
        case PAUSED:
            views.setTextViewText(R.id.artist, "Paused");
            views.setImageViewResource(R.id.play, R.drawable.btn_playback_play_normal_jb_dark);
            views.setOnClickPendingIntent(R.id.play, MyNotifier.mServiceResumeIntent);
            break;
        case PLAYING:
            views.setTextViewText(R.id.artist, "Playing");
            views.setImageViewResource(R.id.play, R.drawable.btn_playback_pause_normal_jb_dark);
            views.setOnClickPendingIntent(R.id.play, MyNotifier.mServicePauseIntent);
            break;
        default:
            break;
        }

        // Link actions buttons to intents
        linkButtons(service, views);
        pushUpdate(service, appWidgetIds, views);
    }

    /**
     * Link up various button actions using {@link PendingIntents}.
     * 
     * @param playerActive
     *            True if player is active in background, which means widget
     *            click will launch {@link Player},
     */
    private void linkButtons(Context context, RemoteViews views) {
        Log.i(TAG, "linkButtons");
        // Connect up various buttons and touch events
        views.setOnClickPendingIntent(R.id.albumart, MyNotifier.mActivityIntent);
        views.setOnClickPendingIntent(R.id.trackname, MyNotifier.mActivityIntent);
        views.setOnClickPendingIntent(R.id.artist, MyNotifier.mActivityIntent);
        // if(!mPlaying){
        // views.setOnClickPendingIntent(R.id.play,
        // MyNotifier.mServicePauseIntent);
        // }

        Intent intent = new Intent(MIRACAST_CONTROL_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.ratings_off, pendingIntent);

        views.setOnClickPendingIntent(R.id.stop, MyNotifier.mServiceStopIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        super.onReceive(context, intent);

        if (intent.getAction().equals(MIRACAST_CONTROL_ACTION)) {
            Toast.makeText(context, "TO DO ...", Toast.LENGTH_LONG).show();
        }

    }

}
