import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * 在mac上测试
 * 查看指定端口信息： lsof -i:端口号
 * 查看所有TCP进程： lsof -iTCP -sTCP:LISTEN 
 * 关闭：           kill -9 [PID进程号]
 */
public class Server {

    public static class WifiServer {
        // 监听指定的端口
        public static final int port = 8888;
        public ServerSocket server;
        public boolean run = true;

        public WifiServer() {
            try {
                run = true;
                server = new ServerSocket(port);
                Socket socket = server.accept();
                System.out.println("client connected,ip:"+socket.getInetAddress()+",port:"+socket.getPort());
                while(run) {
                    if (socket.isClosed()) {
                        break;
                    }
                    System.out.println("client connected runing...");

                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    String a = dis.readUTF();
                    System.out.println("接收到的信息："+a);

                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    dos.writeUTF("服务器->："+a);
                    dos.flush();

                }
                socket.close();
                server.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            System.out.println("client connected end...");
        }

    }


    public static void main(String[] args) {
        WifiServer server;
        try {
            server = new WifiServer();
            System.out.println("client runing...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}