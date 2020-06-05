package com.slack.androidclient;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;

/**
 * Created by slack on 2020/6/2 下午4:58.
 */
public class WrapWebSocket extends WebSocketClient {

    public WrapWebSocket(String ip, int port) {
        // ws:// ip地址 : 端口号
        super(URI.create("ws://" + ip + ":" + port));
    }

    private SocketCallback mSocketCallback;
    public void setCallback(SocketCallback callback) {
        mSocketCallback = callback;
    }

    public boolean isConnected() {
        Log.i("slack", "isConnected: " + this.getReadyState());
        return this.getReadyState() == WebSocket.READY_STATE_OPEN;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

        Log.i("slack", "onOpen...");
    }

    private String msg = "";
    @Override
    public void onMessage(String s) {
        msg = s;
        if (mSocketCallback != null) {
            mSocketCallback.onMessage(s);
        }
        Log.i("slack", "onMessage: " + s);
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        super.onMessage(bytes);
        if (mSocketCallback != null) {
            mSocketCallback.onMessage(bytes);
        }
        Log.i("slack", "onMessage ByteBuffer");
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        Log.i("slack", "onClose: " + s + ", " + i + ", " + b);
    }

    @Override
    public void onError(Exception e) {
        Log.i("slack", "onError...");
        e.printStackTrace();
    }

    @Override
    public void connect() {
        super.connect();
        if (mSocketCallback != null) {
            mSocketCallback.onConnect();
        }
        Log.i("slack", "connect...");
    }

    @Override
    public boolean connectBlocking() throws InterruptedException {
        Log.i("slack", "connectBlocking...");
        return super.connectBlocking();
    }

    @Override
    public void send(String text) throws NotYetConnectedException {
        super.send(text);
    }

    @Override
    public void close() {
        super.close();
        Log.i("slack", "close...");
    }

    @Override
    public void closeBlocking() throws InterruptedException {
        super.closeBlocking();
        Log.i("slack", "closeBlocking...");
    }

    public String read() {
        return msg;
    }
}
