package com.slack.androidclient.screen.encode;

import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by slack on 2020/6/4 下午7:32.
 * 输出每一帧数据
 */
public class ScreenRecordEncode {
    private static final String TAG = "ScreenRecord";
    protected static final boolean VERBOSE = true;
    protected static final int INVALID_INDEX = -1;
    protected VideoEncoder mVideoEncoder;
    protected MicRecorder mAudioEncoder;

    protected MediaFormat mVideoOutputFormat = null, mAudioOutputFormat = null;

    protected AtomicBoolean mForceQuit = new AtomicBoolean(false);
    protected AtomicBoolean mIsRunning = new AtomicBoolean(false);
    protected VirtualDisplay mVirtualDisplay;

    protected HandlerThread mWorker;
    protected EncodeHandler mHandler;

    protected ScreenRecordCallback mCallback;
    private final LinkedList<Integer> mPendingVideoEncoderBufferIndices = new LinkedList<>();
    private final LinkedList<Integer> mPendingAudioEncoderBufferIndices = new LinkedList<>();
    private final LinkedList<MediaCodec.BufferInfo> mPendingAudioEncoderBufferInfos = new LinkedList<>();
    private final LinkedList<MediaCodec.BufferInfo> mPendingVideoEncoderBufferInfos = new LinkedList<>();

    /**
     * @param display for 为了调用 VirtualDisplay#setSurface(Surface)
     */
    public ScreenRecordEncode(VideoEncodeConfig video,
                              AudioEncodeConfig audio,
                              VirtualDisplay display) {
        mVirtualDisplay = display;
        mVideoEncoder = new VideoEncoder(video);
        mAudioEncoder = audio == null ? null : new MicRecorder(audio);
    }

    /**
     * stop task
     */
    public final void quit() {
        mForceQuit.set(true);
        if (!mIsRunning.get()) {
            release();
        } else {
            signalStop(false);
        }

    }

    public void start() {
        if (mWorker != null) throw new IllegalStateException();
        mWorker = new HandlerThread(TAG);
        mWorker.start();
        mHandler = new EncodeHandler(mWorker.getLooper());
        mHandler.sendEmptyMessage(MSG_START);
    }

    public void setCallback(ScreenRecordCallback callback) {
        mCallback = callback;
    }


    private static final int MSG_START = 0;
    private static final int MSG_STOP = 1;
    private static final int MSG_ERROR = 2;
    private static final int STOP_WITH_EOS = 1;

    private class EncodeHandler extends Handler {
        EncodeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START:
                    try {
                        record();
                        if (mCallback != null) {
                            mCallback.onStart();
                        }
                        break;
                    } catch (Exception e) {
                        msg.obj = e;
                    }
                case MSG_STOP:
                case MSG_ERROR:
                    stopEncoders();
                    if (msg.arg1 != STOP_WITH_EOS) signalEndOfStream();
                    if (mCallback != null) {
                        mCallback.onStop((Throwable) msg.obj);
                    }
                    release();
                    break;
            }
        }
    }

    private void signalEndOfStream() {
        MediaCodec.BufferInfo eos = new MediaCodec.BufferInfo();
        ByteBuffer buffer = ByteBuffer.allocate(0);
        eos.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        print("Signal EOS to muxer ");
        obtainVideoData(eos, buffer);
        obtainAudioData(eos, buffer);
    }

    private void record() {
        if (mIsRunning.get() || mForceQuit.get()) {
            throw new IllegalStateException();
        }
        if (mVirtualDisplay == null) {
            throw new IllegalStateException("maybe release");
        }
        mIsRunning.set(true);

        try {
            // create encoder and input surface
            prepareVideoEncoder();
            prepareAudioEncoder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // "turn on" VirtualDisplay after VideoEncoder prepared
        mVirtualDisplay.setSurface(mVideoEncoder.getInputSurface());
        print("set surface to display: " + mVirtualDisplay.getDisplay());
    }

    private void prepareVideo(int index, MediaCodec.BufferInfo buffer) {
        if (!mIsRunning.get()) {
            print("muxVideo: Already stopped!");
            return;
        }
        if (mVideoOutputFormat == null) {
            mPendingVideoEncoderBufferIndices.add(index);
            mPendingVideoEncoderBufferInfos.add(buffer);
            return;
        }
        ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(index);
        obtainVideoData(buffer, encodedData);
        mVideoEncoder.releaseOutputBuffer(index);
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            print("Stop encoder and muxer, since the buffer has been marked with EOS");
            // send release msg
            signalStop(true);
        }
    }


    private void prepareAudio(int index, MediaCodec.BufferInfo buffer) {
        if (!mIsRunning.get()) {
            Log.w(TAG, "muxAudio: Already stopped!");
            return;
        }
        if (mAudioOutputFormat == null) {
            mPendingAudioEncoderBufferIndices.add(index);
            mPendingAudioEncoderBufferInfos.add(buffer);
            return;

        }
        ByteBuffer encodedData = mAudioEncoder.getOutputBuffer(index);
        obtainAudioData(buffer, encodedData);
        mAudioEncoder.releaseOutputBuffer(index);
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            print("Stop encoder and muxer, since the buffer has been marked with EOS");
            signalStop(true);
        }
    }

    private void obtainVideoData(MediaCodec.BufferInfo buffer, ByteBuffer encodedData) {
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            // Ignore it.
            print("Ignoring BUFFER_FLAG_CODEC_CONFIG");
            buffer.size = 0;
        }
        boolean eos = (buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
        if (buffer.size == 0 && !eos) {
            print("info.size == 0, drop it.");
            encodedData = null;
        } else {
            if (buffer.presentationTimeUs != 0) { // maybe 0 if eos
                resetVideoPts(buffer);
            }
            if (VERBOSE)
                print("[" + Thread.currentThread().getId() + "] Got buffer, video track"
                        + ", info: size=" + buffer.size
                        + ", presentationTimeUs=" + buffer.presentationTimeUs);

        }
        if (encodedData != null) {
            encodedData.position(buffer.offset);
            encodedData.limit(buffer.offset + buffer.size);
            if (!eos && mCallback != null) {
                mCallback.onEncodeVideo(encodedData, buffer);
            }
            print("Sent " + buffer.size + " bytes to MediaMuxer on video track ");
        }
    }

    private void obtainAudioData(MediaCodec.BufferInfo buffer, ByteBuffer encodedData) {
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            // Ignore it.
            print("Ignoring BUFFER_FLAG_CODEC_CONFIG");
            buffer.size = 0;
        }
        boolean eos = (buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
        if (buffer.size == 0 && !eos) {
            print("info.size == 0, drop it.");
            encodedData = null;
        } else {
            if (buffer.presentationTimeUs != 0) { // maybe 0 if eos
                resetAudioPts(buffer);
            }
            print("[" + Thread.currentThread().getId() + "] Got buffer, audio track"
                    + ", info: size=" + buffer.size
                    + ", presentationTimeUs=" + buffer.presentationTimeUs);

        }
        if (encodedData != null) {
            encodedData.position(buffer.offset);
            encodedData.limit(buffer.offset + buffer.size);
            if (!eos && mCallback != null) {
                mCallback.onEncodeAudio(encodedData, buffer);
            }
        }
    }

    private long mVideoPtsOffset, mAudioPtsOffset;

    private void resetAudioPts(MediaCodec.BufferInfo buffer) {
        if (mAudioPtsOffset == 0) {
            mAudioPtsOffset = buffer.presentationTimeUs;
            buffer.presentationTimeUs = 0;
        } else {
            buffer.presentationTimeUs -= mAudioPtsOffset;
        }
    }

    private void resetVideoPts(MediaCodec.BufferInfo buffer) {
        if (mVideoPtsOffset == 0) {
            mVideoPtsOffset = buffer.presentationTimeUs;
            buffer.presentationTimeUs = 0;
        } else {
            buffer.presentationTimeUs -= mVideoPtsOffset;
        }
    }

    /**
     * 保持视频 & 音频同步
     * 如果视频先来，音频还没有准备好，先记录下音频数据
     */
    private void prepareAudioAndVideoData() {
        if (mVideoOutputFormat == null ||
                (mAudioEncoder != null && mAudioOutputFormat == null)) {
            return;
        }

        if (mPendingVideoEncoderBufferIndices.isEmpty() && mPendingAudioEncoderBufferIndices.isEmpty()) {
            return;
        }
        print("Mux pending video output buffers...");
        MediaCodec.BufferInfo info;
        while ((info = mPendingVideoEncoderBufferInfos.poll()) != null) {
            int index = mPendingVideoEncoderBufferIndices.poll();
            prepareVideo(index, info);
        }
        if (mAudioEncoder != null) {
            while ((info = mPendingAudioEncoderBufferInfos.poll()) != null) {
                int index = mPendingAudioEncoderBufferIndices.poll();
                prepareAudio(index, info);
            }
        }
        print("Mux pending video output buffers done.");
    }

    // @WorkerThread
    private void prepareVideoEncoder() throws IOException {
        EncoderCallback callback = new EncoderCallback() {
            boolean ranIntoError = false;

            @Override
            public void onOutputBufferAvailable(BaseEncoder codec, int index, MediaCodec.BufferInfo info) {
                print("VideoEncoder output buffer available: index=" + index);
                try {
                    prepareVideo(index, info);
                } catch (Exception e) {
                    print("Muxer encountered an error! ", e);
                    Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
                }
            }

            @Override
            public void onError(Encoder codec, Exception e) {
                ranIntoError = true;
                print("VideoEncoder ran into an error! ", e);
                Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
            }

            @Override
            public void onOutputFormatChanged(BaseEncoder codec, MediaFormat format) {
                print("Video output format changed.\n New format: " + format.toString());
                mVideoOutputFormat = format;
                prepareAudioAndVideoData();
            }
        };
        mVideoEncoder.setCallback(callback);
        mVideoEncoder.prepare();
    }

    private void prepareAudioEncoder() throws IOException {
        final MicRecorder micRecorder = mAudioEncoder;
        if (micRecorder == null) return;
        EncoderCallback callback = new EncoderCallback() {
            boolean ranIntoError = false;

            @Override
            public void onOutputBufferAvailable(BaseEncoder codec, int index, MediaCodec.BufferInfo info) {
                print("[" + Thread.currentThread().getId() + "] AudioEncoder output buffer available: index=" + index);
                try {
                    prepareAudio(index, info);
                } catch (Exception e) {
                    print("Muxer encountered an error! ", e);
                    Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
                }
            }

            @Override
            public void onOutputFormatChanged(BaseEncoder codec, MediaFormat format) {
                print("Audio output format changed.\n New format: " + format.toString());
                mAudioOutputFormat = format;
                prepareAudioAndVideoData();
            }

            @Override
            public void onError(Encoder codec, Exception e) {
                ranIntoError = true;
                print("MicRecorder ran into an error! ", e);
                Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
            }


        };
        micRecorder.setCallback(callback);
        micRecorder.prepare();
    }

    private void signalStop(boolean stopWithEOS) {
        Message msg = Message.obtain(mHandler, MSG_STOP, stopWithEOS ? STOP_WITH_EOS : 0, 0);
        mHandler.sendMessageAtFrontOfQueue(msg);
    }

    private void stopEncoders() {
        mIsRunning.set(false);
        mPendingAudioEncoderBufferInfos.clear();
        mPendingAudioEncoderBufferIndices.clear();
        mPendingVideoEncoderBufferInfos.clear();
        mPendingVideoEncoderBufferIndices.clear();
        // maybe called on an error has been occurred
        try {
            if (mVideoEncoder != null) mVideoEncoder.stop();
        } catch (IllegalStateException e) {
            // ignored
        }
        try {
            if (mAudioEncoder != null) mAudioEncoder.stop();
        } catch (IllegalStateException e) {
            // ignored
        }

    }

    protected void release() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.setSurface(null);
            mVirtualDisplay = null;
        }

        mVideoOutputFormat = mAudioOutputFormat = null;

        if (mWorker != null) {
            mWorker.quitSafely();
            mWorker = null;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.release();
            mAudioEncoder = null;
        }

        mHandler = null;
    }

    @Override
    protected void finalize() throws Throwable {
        if (mVirtualDisplay != null) {
            print("release() not called!");
            release();
        }
    }

    protected void print(String info) {
        if (VERBOSE) Log.i(TAG, info);
    }

    protected void print(String info, Throwable e) {
        if (VERBOSE) Log.e(TAG, info, e);
    }
}
