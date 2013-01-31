package com.example.tinyplayer;

import java.util.List;

import android.app.Presentation;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.widget.ImageView;

import com.example.tinyplayer.InlineUtil.Leg;

public class PresentationSlideShow extends Presentation {
    final static String TAG = PresentationSlideShow.class.getSimpleName();

    private ImageView mImageView;

    private MySlideShowPlayer mMySildeShowPlayer;

    public PresentationSlideShow(Context context, Display display, MySlideShowPlayer slideShowPlayer) {
        super(context, display);
        mMySildeShowPlayer = slideShowPlayer;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Leg.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.presentation_slideshow);

        mImageView = (ImageView) findViewById(R.id.main_imageview);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mMySildeShowPlayer != null) {
            mMySildeShowPlayer.setImageView(mImageView);
            mMySildeShowPlayer.startPlayback();
        }
    }

    public void setMediaUris(List<Uri> uris) {
        if (mMySildeShowPlayer != null) {
            mMySildeShowPlayer.setMediaUris(uris);
        }
    }
}
