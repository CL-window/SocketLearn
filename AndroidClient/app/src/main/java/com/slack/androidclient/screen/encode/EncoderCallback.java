package com.slack.androidclient.screen.encode;

import android.media.MediaCodec;
import android.media.MediaFormat;

/**
 * Created by slack on 2020/6/4 下午6:14.
 * 编码回调， 编码器内部使用
 */
abstract class EncoderCallback implements Encoder.Callback {
    public void onInputBufferAvailable(BaseEncoder encoder, int index) {
        //
    }

    public void onOutputFormatChanged(BaseEncoder encoder, MediaFormat format) {
        //
    }

    public void onOutputBufferAvailable(BaseEncoder encoder, int index, MediaCodec.BufferInfo info) {
        //
    }
}
