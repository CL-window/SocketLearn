package com.slack.androidclient.screen.encode;

import android.media.MediaFormat;

/**
 * Created by slack on 2020/6/3 下午10:23.
 */
public class AudioEncoder extends BaseEncoder {
    private final AudioEncodeConfig mConfig;

    AudioEncoder(AudioEncodeConfig config) {
        super(config.codecName);
        this.mConfig = config;
    }

    @Override
    protected MediaFormat createMediaFormat() {
        return mConfig.toFormat();
    }

}
