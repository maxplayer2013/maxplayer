package com.example.tinyplayer;

public abstract class MyMediaPlayer {
    public static enum PLAYBACK_STATE {
        IDLE, PLAYING, PAUSED,
    }

    public static enum PLAYBACK_TYPE {
        NONE, AV, SLIDESHOW,
    }

    protected PLAYBACK_STATE mPlaybackState = PLAYBACK_STATE.IDLE;
    protected boolean mIsActive = false;

    public PLAYBACK_STATE getPlaybackState() {
        return mPlaybackState;
    }

    public boolean isPlaying() {
        return mPlaybackState == PLAYBACK_STATE.PLAYING;
    }

    public int savePositionForNextPlayback(){
        return 0;
    }
    
    public void onPlaybackStateChanged(PLAYBACK_STATE state) {
        mPlaybackState = state;
    }

    public boolean isActive() {
        return mIsActive;
    }

    public void startPlayback() {
        mIsActive = true;
    }

    public void stopPlayback() {
        mIsActive = false;
    }
}
