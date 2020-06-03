import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Server extends WebSocketServer {

    public Server() {
        super(new InetSocketAddress(8888));
    }

    @Override
    public void start() {
        super.start();
        print("start..." + getAddress());
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        super.stop();
        print("stop...");
    }

    @Override
    public void stop(int timeout) throws IOException, InterruptedException {
        super.stop(timeout);
        print("stop...");
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        print("onOpen...");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        print("onClose: " + s + ", " + i + ", " + b);
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        print("onMessage..." + s);
        webSocket.send("服务器 -> " + s);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        print("onError...");
        e.printStackTrace();
    }

    private void print(String info) {
        System.out.println(info + ", " + this.toString());
    }
}
