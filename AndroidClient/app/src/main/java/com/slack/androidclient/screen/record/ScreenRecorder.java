package com.slack.androidclient.screen.record;

import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaMuxer;

import com.slack.androidclient.screen.encode.AudioEncodeConfig;
import com.slack.androidclient.screen.encode.ScreenRecordCallback;
import com.slack.androidclient.screen.encode.ScreenRecordEncode;
import com.slack.androidclient.screen.encode.VideoEncodeConfig;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;

/**
 * Created by slack on 2020/6/4 下午6:28.
 *
 * 屏幕录制，保存为文件
 */
public class ScreenRecorder extends ScreenRecordEncode {
    private static final int INVALID_INDEX = -1;
    private String mDstPath;

    private int mVideoTrackIndex = INVALID_INDEX, mAudioTrackIndex = INVALID_INDEX;
    private MediaMuxer mMuxer;
    private boolean mMuxerStarted = false;

    /**
     * @param display for 为了调用 VirtualDisplay#setSurface(Surface)
     * @param dstPath saving path, path is a new mp4 file 需要保存的mp4文件地址, 如果地址为空，则输出每一帧数据，
     */
    public ScreenRecorder(VideoEncodeConfig video,
                          AudioEncodeConfig audio,
                          VirtualDisplay display,
                          @NonNull String dstPath) {
        super(video, audio, display);
        this.mDstPath = dstPath;
        setCallback(mInnerRecordCallback);
        try {
            mMuxer = new MediaMuxer(mDstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final ScreenRecordCallback mInnerRecordCallback = new ScreenRecordCallback() {
        @Override
        public void onStart() {
            print("ScreenRecord start...");
        }

        @Override
        public void onStop(Throwable error) {
            print("ScreenRecord onStop...");
        }

        @Override
        public void onEncodeVideo(ByteBuffer buffer, MediaCodec.BufferInfo info) {
            startMuxerIfReady();
            if (mMuxer != null) {
                mMuxer.writeSampleData(mVideoTrackIndex, buffer, info);
            }
        }

        @Override
        public void onEncodeAudio(ByteBuffer buffer, MediaCodec.BufferInfo info) {
            startMuxerIfReady();
            if (mMuxer != null) {
                mMuxer.writeSampleData(mAudioTrackIndex, buffer, info);
            }
        }
    };

    private void startMuxerIfReady() {
        if (mMuxer != null && !mMuxerStarted) {
            mVideoTrackIndex = mMuxer.addTrack(mVideoOutputFormat);
            mAudioTrackIndex = mAudioEncoder == null ? INVALID_INDEX : mMuxer.addTrack(mAudioOutputFormat);
            mMuxer.start();
            mMuxerStarted = true;
        }
    }

    protected void release() {
        super.release();
        if (mMuxer != null) {
            try {
                mMuxer.stop();
                mMuxer.release();
            } catch (Exception e) {
                // ignored
            }
            mMuxer = null;
        }
    }

}
