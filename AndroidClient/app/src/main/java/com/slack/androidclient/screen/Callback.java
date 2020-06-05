package com.slack.androidclient.screen;

import android.graphics.Bitmap;

/**
 * Created by slack on 2020/6/4 下午5:13.
 */
public interface Callback {
    default void callback(Bitmap bitmap) {
        //
    }

    default void onStart() {
        //
    }

    default void onStop() {
        //
    }
}
