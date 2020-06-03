package com.slack.androidclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

/**
 * Android 做客户端
 */
public class ServerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
    }

    private WrapWebSocketServer socketServer;
    public void startServer(View view) {
        if (socketServer == null) {
            socketServer = new WrapWebSocketServer();
            socketServer.start();
        }
    }

    public void stopServer(View view) {
        if (socketServer != null) {
            socketServer.stop(1000);
        }
        socketServer = null;
    }
}
