package com.example.tinyplayer;

import android.app.Presentation;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.example.tinyplayer.InlineUtil.Leg;

public class PresentationVideo extends Presentation {
    final static String TAG = PresentationVideo.class.getSimpleName();

    private static Context mContext;
    private MyAVPlayer mMyAVPlayer;
    private SurfaceView mSurfaceView;

    private Uri mVideoUri;

    //
    public void setVideoUri(Uri uri) {
        mVideoUri = uri;
    }

    //
    private void startPlayback() {
        Leg.i(TAG, "startPlayback()");
        Surface surface = mSurfaceView.getHolder().getSurface();
        if (!surface.isValid()) {
            Leg.i(TAG, "not valid surface");
            return;
        }
        if (PlaybackService.RESET_ON_GO_BACKGROUND) {
            try {
                Leg.i(TAG, "about to stop & set null surface & reset & prepare media player");

                mMyAVPlayer.startPlayback(false);
                mMyAVPlayer.resetPlayback();
//                mMyAVPlayer.setSurface(false, surface);
                mMyAVPlayer.setDisplay(false, mSurfaceView.getHolder());
                // mMyMediaPlayer.setDataSource(getRealPathFromURI(mVideoUri));
                mMyAVPlayer.setDataSource(mVideoUri);
                mMyAVPlayer.prepare();
            } catch (Exception exception) {
                Leg.i(TAG, exception.toString());

            }
        } else {
            Leg.i(TAG, "about to pause & set null surface & start media player");
            mMyAVPlayer.pause();
            mMyAVPlayer.setSurface(false, surface);
            mMyAVPlayer.start();
        }
    }

    //
    public PresentationVideo(Context context, Display display, MyAVPlayer player) {
        super(context, display);
        mContext = context;
        mMyAVPlayer = player;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.presentation_playback);

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Set up the surface view for visual interest.
        mSurfaceView = (SurfaceView) findViewById(R.id.video_surface);
        mSurfaceView.getHolder().addCallback(new SimpleSurfaceHolderCallback());


    }

    @Override
    protected void onStop() {
        Leg.i(TAG, "onStop()");
        mMyAVPlayer.stopPlayback();
//        mMyAVPlayer.mPlayVideoInPresentation = false;
        super.onStop();
    }

    class SimpleSurfaceHolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            startPlayback();

        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }

    // get filename from Uri
    public String getRealPathFromURI(Uri uri) {
        String[] columns = { MediaStore.Video.Media.DATA };
        CursorLoader loader = new CursorLoader(mContext, uri, columns, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_data = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_data);
    }
}
