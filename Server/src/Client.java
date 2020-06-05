import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class Client extends WebSocketClient {

    public Client() {
        super(URI.create("ws://localhost:8888"));
    }

    public Client(String ip) {
        super(URI.create("ws://"+ip+":8888"));
    }

    public Client(String ip, int port) {
        super(URI.create("ws://"+ip+":8888"));
    }

    private Callback callback;
    public void connect(Callback callback) {
        this.callback = callback;
        connect();
    }

    @Override
    public void connect() {
        super.connect();
        print("connect...");
    }

    @Override
    public boolean connectBlocking() throws InterruptedException {
        print("connectBlocking...");
        return super.connectBlocking();
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        print("onOpen...");
        if (callback != null) {
            callback.onConnected();
        }
    }

    @Override
    public void onMessage(String s) {
        print("onMessage: " + s);
        if (callback != null) {
            callback.onMessage(s);
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        super.onMessage(bytes);
        print("onMessage: ByteBuffer" + bytes);
        if (callback != null) {
            callback.onMessage(bytes);
        }
    }


    @Override
    public void onClose(int i, String s, boolean b) {
        print("onClose: " + s + ", " + i + ", " + b);
    }

    @Override
    public void onError(Exception e) {
        print("onError...");
        e.printStackTrace();
    }

    private void print(String info) {
        System.out.println(info + ", " + this.toString());
    }

    public interface Callback {
        void onConnected();
        default void onMessage(String msg) {
            //
        }
        default void onMessage(ByteBuffer bytes) {
            //
        }
    }
}
