package com.slack.androidclient;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Created by slack on 2020/6/3 下午12:48.
 * Android 做服务器
 */
public class WrapWebSocketServer extends WebSocketServer {

    private WebSocket mWebSocket;
    public WrapWebSocketServer() {
//        super(new InetSocketAddress("0.0.0.0", 8888));
        super(new InetSocketAddress(8888));
    }

    @Override
    public void start() {
        super.start();
        Log.i("slack", "start..." + getAddress());
    }

    public void stop() {
        try {
            super.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop(int timeout) {
        try {
            super.stop(timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(String info) {
        if (mWebSocket != null) {
            mWebSocket.send(info);
        }
        Log.i("slack", "send info: " + info);
    }

    public void send(byte[] info) {
        if (mWebSocket != null) {
            mWebSocket.send(info);
        }
        Log.i("slack", "send byte");
    }

    public void send(ByteBuffer info) {
        if (mWebSocket != null) {
            mWebSocket.send(info);
        }
        Log.i("slack", "send ByteBuffer");
    }


    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        mWebSocket = webSocket;
        Log.i("slack", "onOpen...");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        mWebSocket = null;
        Log.i("slack", "onClose...");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        Log.i("slack", "onMessage..." + s);
        webSocket.send("Android Server -> " + s);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        Log.i("slack", "onError...");
        e.printStackTrace();
    }
}
