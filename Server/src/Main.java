import java.nio.ByteBuffer;
import java.util.Map;

public class Main {

    /**
     * start...0.0.0.0/0.0.0.0:8888, Server@28a418fc
     * connect..., Client@e9e54c2
     * onOpen..., Server@28a418fc
     * onOpen..., Client@e9e54c2
     * onMessage...hello, Server@28a418fc
     * onMessage: 服务器 -> hello, Client@e9e54c2
     */
    private static void clientConnect() {
        Client client = new Client();
        client.connect(new Client.Callback() {
            @Override
            public void onConnected() {
                client.send("hello");
            }
        });
    }

    /**
     * java 客户端连接 Android 服务器, 需要知道 Android服务器地址
     * connect..., Client@2f2c9b19
     * onOpen..., Client@2f2c9b19
     * onMessage: Android Server -> hello, Client@2f2c9b19
     */
    private static void connectAndroidServer() {
        Client client = new Client("192.168.3.208");
        client.connect(new Client.Callback() {
            @Override
            public void onConnected() {
                client.send("hello");
            }
        });
    }

    /**
     * 关闭手机网络，与电脑连接usb, 开启Android上的服务
     * adb devices
     * 1e755cc
     * adb -s 1e755cc forward 本地 tcp:8888 USB连接的手机 tcp:8888
     * connect..., Client@533ddba
     * onOpen..., Client@533ddba
     * onMessage: Android Server -> hello, Client@533ddba
     */
    private static void connectAndroidServerByUSB() {
        ADBExecutor adbExecutor = new ADBExecutor("/Users/slack/Library/Android/sdk/platform-tools/adb");
//        adbExecutor.execAdbDevices();
        Map<String, Integer> device_hostport_map = adbExecutor.execAdbOnlineDevicesPortForward();
        for (Map.Entry<String, Integer> entry : device_hostport_map.entrySet()) {
            String device = entry.getKey();
            int port = entry.getValue();
            Client client = new Client("localhost", port);
            client.connect(new Client.Callback() {
                @Override
                public void onConnected() {
                    client.send("hello");
                }
            });
            // 只处理一个设备
            break;
        }
    }

    public static void main(String[] args) {

        // java 服务器， 等待 Android客户端连接
//        Server server = new Server();
//        server.start();


        // java 服务器 + java 客户端
//        Server server = new Server();
//        server.start();
//        clientConnect();

        // java 客户端连接 Android 服务器, 需要知道 Android服务器地址
//        connectAndroidServer();

//        connectAndroidServerByUSB();

        connectAndroidServerJrame();
    }

    private static void connectAndroidServerJrame() {
        Client client = new Client("192.168.3.208");
        client.connect(new Client.Callback() {
            @Override
            public void onConnected() {
                client.send("hello");
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                //
            }

            @Override
            public void onMessage(String msg) {
                //
            }
        });
    }

}
