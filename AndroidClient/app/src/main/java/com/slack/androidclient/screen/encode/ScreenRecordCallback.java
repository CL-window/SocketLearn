package com.slack.androidclient.screen.encode;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * Created by slack on 2020/6/4 下午7:37.
 * 录屏回调
 */
public interface ScreenRecordCallback {
    /**
     * 录制开始
     */
    void onStart();

    /**
     * 录制结束
     */
    void onStop(Throwable error);

    /**
     * 视频数据
     */
    void onEncodeVideo(ByteBuffer buffer, MediaCodec.BufferInfo info);

    /**
     * 音频数据
     */
    void onEncodeAudio(ByteBuffer buffer, MediaCodec.BufferInfo info);
}
