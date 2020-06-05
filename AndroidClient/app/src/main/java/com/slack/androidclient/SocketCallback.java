package com.slack.androidclient;

import java.nio.ByteBuffer;

/**
 * Created by slack on 2020/6/5 下午5:28.
 */
public interface SocketCallback {
    void onConnect();

    default void onMessage(String info) {
        //
    }

    default void onMessage(ByteBuffer buffer) {
        //
    }
}
