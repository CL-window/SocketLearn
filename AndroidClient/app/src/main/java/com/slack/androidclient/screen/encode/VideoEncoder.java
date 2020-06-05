package com.slack.androidclient.screen.encode;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.util.Objects;

/**
 * Created by slack on 2020/6/3 下午10:08.
 */
public class VideoEncoder extends BaseEncoder {
    private static final boolean VERBOSE = false;
    private VideoEncodeConfig mConfig;
    private Surface mSurface;


    public VideoEncoder(VideoEncodeConfig config) {
        super(config.codecName);
        this.mConfig = config;
    }

    @Override
    protected void onEncoderConfigured(MediaCodec encoder) {
        mSurface = encoder.createInputSurface();
        if (VERBOSE) Log.i("@@", "VideoEncoder create input surface: " + mSurface);
    }

    @Override
    protected MediaFormat createMediaFormat() {
        return mConfig.toFormat();
    }

    /**
     * @throws NullPointerException if prepare() not call
     */
    public Surface getInputSurface() {
        return Objects.requireNonNull(mSurface, "doesn't prepare()");
    }

    @Override
    public void release() {
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        super.release();
    }

}
