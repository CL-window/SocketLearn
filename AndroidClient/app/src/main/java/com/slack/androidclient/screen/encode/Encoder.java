package com.slack.androidclient.screen.encode;

import java.io.IOException;

/**
 * Created by slack on 2020/6/3 下午10:00.
 */
public interface Encoder {

    void prepare() throws IOException;

    void stop();

    void release();

    void setCallback(EncoderCallback callback);

    interface Callback {
        void onError(Encoder encoder, Exception exception);
    }
}
