package com.slack.androidclient.screen;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import com.slack.androidclient.R;
import com.slack.androidclient.SocketCallback;
import com.slack.androidclient.WrapWebSocket;

import java.nio.ByteBuffer;

public class ScreenPushClientActivity extends AppCompatActivity {

    private WrapWebSocket webSocket;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_push_client);

        imageView = findViewById(R.id.client_img);
        webSocket = new WrapWebSocket("192.168.3.208", 8888);
        webSocket.setCallback(new SocketCallback() {
            @Override
            public void onConnect() {

            }

            @Override
            public void onMessage(String info) {

            }

            private Bitmap mBitmap;
            @Override
            public void onMessage(ByteBuffer buffer) {
                Bitmap bitmap = mBitmap;
                mBitmap = Bitmap.createBitmap(1088, 2138, Bitmap.Config.ARGB_8888);
                mBitmap.copyPixelsFromBuffer(buffer);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(mBitmap);
                        if (bitmap != null && !bitmap.isRecycled()) {
                            bitmap.recycle();
                        }
                    }
                });
            }
        });
        webSocket.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webSocket.close();
    }
}
