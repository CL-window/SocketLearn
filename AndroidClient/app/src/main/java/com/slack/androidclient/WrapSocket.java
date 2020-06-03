package com.slack.androidclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Created by slack on 2020/6/2 下午4:54.
 * Copyright since 2016 Benqu. All rights reserved.
 */
public class WrapSocket {
    public Socket socket;

    public WrapSocket(String ip, int port) {
        try {
            socket = new Socket(ip, port);
        } catch (Exception e) {
            socket = null;
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return socket != null;
    }

    public void send(String str) {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF(str);
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String read() {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            return dis.readUTF();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error";
    }


    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            socket = null;
        }
    }

}
