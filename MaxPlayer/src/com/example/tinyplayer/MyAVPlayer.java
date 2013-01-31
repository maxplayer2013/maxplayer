package com.example.tinyplayer;

import java.io.IOException;

import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.example.tinyplayer.InlineUtil.Leg;
import com.example.tinyplayer.widget.PlayerWidgetProvider;

class MyAVPlayer extends MyMediaPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnVideoSizeChangedListener {

    private static final String TAG = MyAVPlayer.class.getSimpleName();

    MediaPlayer mMediaPlayer;

    int pausedPosition = 0;
    private int mStartAt;
    private boolean mLooping = false;
    private boolean mPrepared = false;
    private boolean mPlayInPresentation = false;

    private SurfaceHolder mForegroundSurfaceHolder = null;
    private SurfaceHolder mPresentationSurfaceHolder = null;

    private int mScreenHeight = 1;
    private int mScreenWidth = 1;
    private int mVideoHeight = 1;
    private int mVideoWidth = 1;

    //
    private PlaybackService mPlaybackService;
    private MyNotifier mNotifier;

    // not using the surface obj
    Surface mSurface;

    //
    public MyAVPlayer(PlaybackService service, MyNotifier notifier) {
        mPlaybackService = service;
        mNotifier = notifier;

        mMediaPlayer = getNewMediaPlayer();
    }

    public void startPlayback(boolean foreground) {
        mPlayInPresentation = !foreground;
        startPlayback();
    }

    @Override
    public void startPlayback() {
        super.startPlayback();
    }

    @Override
    public void stopPlayback() {
        super.stopPlayback();

        if (!mPrepared) {
            return;
        }

        if (mMediaPlayer != null) {
            Leg.i(TAG, "about to stop&release&null media player");
            stop();
            release();
        }
        onPlaybackStateChanged(PLAYBACK_STATE.IDLE, false);

        mMediaPlayer = getNewMediaPlayer();
    }

    public void resetPlayback() {
        Leg.i(TAG, String.format("resetPlayback()"));
        if (!mPrepared) {
            return;
        }

        if (mMediaPlayer != null) {
            Leg.i(TAG, "about to stop&release&null media player");
            stop();
            release();
            onPlaybackStateChanged(PLAYBACK_STATE.IDLE, false);
        }

        mMediaPlayer = getNewMediaPlayer();
    }

    public void swapLooping() {
        if (isPlaying()) {
            mLooping = !mMediaPlayer.isLooping();
            mMediaPlayer.setLooping(mLooping);
        } else {
            mLooping = !mLooping;
        }
    }

    public void setDM(int width, int height) {
        Leg.i(TAG, String.format("screen width:%d, height:%d", width, height));
        mScreenWidth = width;
        mScreenHeight = height;
    }

    public void setStartAt(int startAt) {
        Leg.i(TAG, String.format("setStartAt(%d)", startAt));
        mStartAt = startAt;
    }

    public void notifyPlaybackState() {
        onPlaybackStateChanged(mPlaybackState);
    }

    @Override
    public void onPlaybackStateChanged(PLAYBACK_STATE state) {
        onPlaybackStateChanged(mPlaybackState, true);
    }

    private MediaPlayer getNewMediaPlayer() {

        mPrepared = false;
        MediaPlayer obj = new MediaPlayer();

        setOnPreparedListener(obj);
        setOnSeekCompleteListener(obj);
        setOnCompletionListener(obj);
        setOnVideoSizeChangedListener(obj);

        return obj;
    }

    // ======play back state====== //
    private void onPlaybackStateChanged(PLAYBACK_STATE state, boolean verify) {
        Leg.i(TAG, String.format("onPlaybackStateChanged(%s)", state));
        PLAYBACK_STATE oldState = mPlaybackState;

        if (verify) {
            mPlaybackState = verifyPlaybackState(state);
        } else {
            mPlaybackState = state;
        }
        notifyPlaybackState(oldState, mPlaybackState);
    }

    private void notifyPlaybackState(PLAYBACK_STATE oldState, PLAYBACK_STATE newState) {
        Leg.i(TAG, String.format("notifyPlaybackState(%s, %s)", oldState, newState));
        switch (newState) {
        case IDLE:
            // cancel all notification
            mNotifier.cancel();
            break;
        case PAUSED:
        case PLAYING:
            // mNotifier.cancel(BACKGROUND_PLAYBACK_ONGOING);
            mNotifier.notifyPlaybackState(newState);
            break;
        }
    }

    private PLAYBACK_STATE verifyPlaybackState(PLAYBACK_STATE state) {
        PLAYBACK_STATE ret = state;
        if (mMediaPlayer != null) {
            if (isPlaying()) {
                return PLAYBACK_STATE.PLAYING;
            }
        } else {
            ret = PLAYBACK_STATE.IDLE;
        }
        return ret;
    }

    // ======media player methods proxies====== //
    public boolean isLooping() {
        if (isPlaying()) {
            mLooping = mMediaPlayer.isLooping();
        }
        return mLooping;
    }

    public void setLooping(boolean looping) {
        mLooping = looping;
        if (mMediaPlayer != null) {
            mMediaPlayer.setLooping(looping);
        }
    }

    public void release() {
        if (mMediaPlayer != null) {
            Leg.i(TAG, "release()");
            mMediaPlayer.release();
            mPrepared = false;
        }
    }

    public int getCurrentPosition() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    public int getVideoHeight() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getVideoHeight();
        } else {
            return 0;
        }
    }

    public int getVideoWidth() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getVideoWidth();
        } else {
            return 0;
        }
    }

    public void prepare() {
        if (mMediaPlayer != null) {
            Leg.i(TAG, "prepare()");
            try {
                mMediaPlayer.prepare();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setOnCompletionListener(MediaPlayer player) {
        if (player != null) {
            player.setOnCompletionListener(this);
        }
    }

    public void setOnSeekCompleteListener(MediaPlayer player) {
        if (player != null) {
            player.setOnSeekCompleteListener(this);
        }
    }

    public void setOnPreparedListener(MediaPlayer player) {
        if (player != null) {
            player.setOnPreparedListener(this);
        }
    }

    public void setOnVideoSizeChangedListener(MediaPlayer player) {
        if (player != null) {
            player.setOnVideoSizeChangedListener(this);
        }
    }

    public void setDisplay(boolean foreground, SurfaceHolder holder) {
        if (foreground) {
            mForegroundSurfaceHolder = holder;
        } else {
            mPresentationSurfaceHolder = holder;
        }
        setDisplay(holder);
    }

    public void setDisplay(SurfaceHolder holder) {
        boolean valid = true;
        if (holder == null) {
            Leg.i(TAG, "holder is null");
        } else {
            Leg.i(TAG, "surface is valide? " + holder.getSurface().isValid());
            if(!holder.getSurface().isValid()){
                valid = false;
            }
        }
        if(valid){
            mMediaPlayer.setDisplay(holder);
        }else{
            mMediaPlayer.setDisplay(null);            
        }
    }

    public void setSurface(boolean foreground, Surface surface) {
        if (foreground == !mPlayInPresentation) {
            setSurface(surface);
        }
    }

    private void setSurface(Surface surface) {
        if (mMediaPlayer != null) {
            Leg.i(TAG,
                    String.format("setSurface(%d), valid:%b", surface != null ? surface.hashCode() : 0, surface != null
                            && surface.isValid()));
            mSurface = surface;
            mMediaPlayer.setSurface(surface);
        }
    }

    public void setDataSource(Uri uri) {
        if (mMediaPlayer != null) {
            Leg.i(TAG, String.format("setDataSource(%s)", uri.toString()));
            try {
                mMediaPlayer.setDataSource(mPlaybackService, uri);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setDataSource(String path) {

        if (mMediaPlayer != null) {
            Leg.i(TAG, String.format("setDataSource(%s)", path));
            try {
                mMediaPlayer.setDataSource(path);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void reset() {
        if (mMediaPlayer != null) {
            Leg.i(TAG, "reset()");
            onPlaybackStateChanged(PLAYBACK_STATE.IDLE, false);
            mMediaPlayer.reset();
            mPrepared = false;
        }
    }

    @Override
    public boolean isPlaying() {
        if (mMediaPlayer != null && mPrepared) {
            return mMediaPlayer.isPlaying();
        } else {
            return false;
        }
    }

    @Override
    public int savePositionForNextPlayback(){
        Leg.i(TAG, "savePositionForNextPlayback()");
        if(isPlaying()){
            setStartAt(mMediaPlayer.getCurrentPosition());
        }
        return mStartAt;
    }
    
    public void start() {
        Leg.i(TAG, "start()");
        if (mMediaPlayer != null) {
            mMediaPlayer.setLooping(mLooping);
            try {
                Leg.i(TAG, "start()");
                mMediaPlayer.start();
                onPlaybackStateChanged(PLAYBACK_STATE.PLAYING, false);
            } catch (IllegalStateException e) {
                stopPlayback();
                e.printStackTrace();
            }
        }
    }

    public void pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            pausedPosition = mMediaPlayer.getCurrentPosition();
            Leg.i(TAG, "pause() ============================================= : pausedPosition : " +pausedPosition);
            onPlaybackStateChanged(PLAYBACK_STATE.PAUSED, false);
        }
    }

    protected void stop() {
        if (mMediaPlayer != null) {
            Leg.i(TAG, "stop()");
            mNotifier.cancel();
            onPlaybackStateChanged(PLAYBACK_STATE.IDLE, false);
            mMediaPlayer.stop();
        }
    }

    // ======media player event listeners====== //

    @Override
    public void onCompletion(MediaPlayer paramMediaPlayer) {
        Leg.i(TAG, "onCompletion()");
        stopPlayback();
        mPlaybackService.onPlaybackCompletion(this);
        mVideoWidth = 1;
        mVideoHeight = 1;
        // fixAspect();
    }

    @Override
    public void onPrepared(MediaPlayer paramMediaPlayer) {
        boolean seeking = false;
        if (mStartAt > 0) {
            Leg.i(TAG, "about to seek media player: " + mStartAt);
            paramMediaPlayer.seekTo(mStartAt);
            mStartAt = 0;
            seeking = true;
        }
        Leg.i(TAG, "onPrepared, about to start playback");
        mPrepared = true;
        onPlaybackStateChanged(PLAYBACK_STATE.PLAYING, false);
        paramMediaPlayer.setLooping(mLooping);
        if (!seeking)
            paramMediaPlayer.start();
        mVideoWidth = paramMediaPlayer.getVideoWidth();
        mVideoHeight = paramMediaPlayer.getVideoHeight();
        fixAspect();
    }

    @Override
    public void onSeekComplete(MediaPlayer paramMediaPlayer) {
        Leg.i(TAG, "onSeekComplete, about to start media player");
        paramMediaPlayer.start();
        // fixAspect();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.i(TAG, String.format("onVideoSizeChanged(%d, %d)", width, height));
        mVideoWidth = mMediaPlayer.getVideoWidth();
        mVideoHeight = mMediaPlayer.getVideoHeight();
        fixAspect();
    }

    public void fixAspect() {
        mPlaybackService.fixAspect(mScreenWidth, mScreenHeight, mVideoWidth, mVideoHeight);
    }

}
