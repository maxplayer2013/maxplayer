package com.example.tinyplayer;

import static com.example.tinyplayer.InlineUtil.getMediaType;

import java.util.ArrayList;
import java.util.List;

import com.example.tinyplayer.InlineUtil.Leg;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

public class MySlideShowPlayer extends MyMediaPlayer {

    public static final String TAG = MySlideShowPlayer.class.getSimpleName();
    public static final int UPDATE_UI = 6001;

    private PlaybackService mPlaybackService;
    private MyNotifier mNotifier;

    ImageView mImageView = null;
    private ArrayList<Uri> mUriList = new ArrayList<Uri>();
    private int mPC = 0;
    private int mSavedPC = 0;
    static boolean isImage = false;

    public MySlideShowPlayer(PlaybackService service, MyNotifier notifier) {
        mPlaybackService = service;
        mNotifier = notifier;
        mRefreshHandler = new RefreshHandler(this);
    }

    public void setImageView(ImageView imageView) {
        mImageView = imageView;
    }

    public void setMediaUris(List<Uri> uris) {
        mUriList.clear();
        mUriList.addAll(uris);
    }

    @Override
    public void startPlayback() {
        super.startPlayback();
        mPC = 0;
        if (mSavedPC > 0) {
            mPC = mSavedPC;
            mSavedPC = 0;
        }
        mRefreshHandler.schedualMessage(UPDATE_UI, 50);
        onPlaybackStateChanged(PLAYBACK_STATE.PLAYING);
    }

    @Override
    public boolean isPlaying() {
        return super.isPlaying();
    }

    @Override
    public int savePositionForNextPlayback() {
        if (isPlaying()) {
            mSavedPC = mPC;
        }
        return mSavedPC;
    }

    @Override
    public void stopPlayback() {
        super.stopPlayback();
        // is this relation ship with onCompletion confusing?

        mNotifier.cancel();
        mRefreshHandler.removeMessages(UPDATE_UI);
    }

    public void pause() {
        mRefreshHandler.removeMessages(UPDATE_UI);
        onPlaybackStateChanged(PLAYBACK_STATE.PAUSED);
    }

    public void resume() {
        mRefreshHandler.schedualMessage(UPDATE_UI, 50);
        onPlaybackStateChanged(PLAYBACK_STATE.PLAYING);
    }

    private void onCompletion() {
        stopPlayback();
        if (mPlaybackService != null) {
            mPlaybackService.onPlaybackCompletion(this);
        }
    }

    @Override
    public void onPlaybackStateChanged(PLAYBACK_STATE state) {
        mPlaybackState = state;
        mNotifier.notifyPlaybackState(state);
    }

    public void updateImageView() {
        Leg.i(TAG, "updateImageView -----------------------------1111111111----------------------------> >>> : mPC "
                + mPC);
        if (mImageView != null) {

            int size = mUriList.size();
            Leg.i(TAG, "size is " + size);
            if (size > 0) {
                if (mPC >= size && !mLooping) {
                    mPC = 0;
                    onCompletion();
                    isImage = false;
                    return;
                }

                Uri uri = null;
                boolean foundImage = false;

                for (; mPC < size && !foundImage; mPC++) {
                    uri = mUriList.get(mPC);

                    Leg.i(TAG, uri.getPath());

                    if (getMediaType(uri) == 2) {
                        foundImage = true;
                        break;
                    }
                }

                if (foundImage && uri != null) {
                    mImageView.setImageURI(uri);
                    Leg.i(TAG,
                            "updateImageView -----------------------------2222222----------------------------> >>> : mPC "
                                    + mPC);
                    // imageView.setPadding(left, top, right, bottom);
                    mPC++;
                    mRefreshHandler.schedualMessage(UPDATE_UI, 1500);
                }
            }
        }

    }

    //
    RefreshHandler mRefreshHandler;

    static class RefreshHandler extends Handler {

        MySlideShowPlayer mPlayer;

        public RefreshHandler(MySlideShowPlayer player) {
            mPlayer = player;
        }

        @Override
        public void handleMessage(Message msg) {
            int code = msg.what;
            switch (code) {
            case UPDATE_UI:
                if (mPlayer != null) {
                    mPlayer.updateImageView();
                }
                break;
            }
        }

        public void schedualMessage(int what, long delayMillis) {
            this.removeMessages(what);
            sendMessageDelayed(obtainMessage(what), delayMillis);
        }
    }

    boolean mLooping = false;
    private boolean mPaused = false;

    public void swapLooping() {
        mLooping = !mLooping;
    }

    public void setPreviousPic() {
        // TODO Auto-generated method stub
        mRefreshHandler.removeMessages(UPDATE_UI);
        Leg.i(TAG,
                "========================================set previous pic , mPc = " + mPC + ", size : "
                        + mUriList.size());
        if (mPC < 2)
            return;
        mImageView.setImageURI(mUriList.get(mPC - 2));
        mPC--;
        Leg.i(TAG, "===============>>>>>>>>>>>>>>>>>>>>>>>>>>>>>set previous pic , mPc = " + mPC);
    }

    public void setNextPic() {
        // TODO Auto-generated method stub
        Leg.i(TAG,
                "========================================set next pic , mPc = " + mPC + ", size : " + mUriList.size());
        mRefreshHandler.removeMessages(UPDATE_UI);

        if (mPC >= mUriList.size())
            return;
        mImageView.setImageURI(mUriList.get(mPC));
        mPC++;
        Leg.i(TAG, "===============>>>>>>>>>>>>>>>>>>>>>>>>>>>>>set next pic , mPc = " + mPC);
    }

    public void setpausePlayback() {
        // TODO Auto-generated method stub
        if (!mRefreshHandler.hasMessages(UPDATE_UI))
            mRefreshHandler.schedualMessage(UPDATE_UI, 1500);
    };

}
