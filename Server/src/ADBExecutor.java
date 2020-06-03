import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * https://blog.csdn.net/joshly/java/article/details/47070485
 *
 * 使用ADB进行端口的映射转发：adb forward tcp:local_port tcp:remote_port
 * adb forward tcp:8888 tcp:9999
 * 转发PC机8888端口的数据到手机的9999端口。
 */
public class ADBExecutor {
    public static final int ANDROID_PORT = 8888;
    public static final int HOST_BASE_PORT = 8888;

    private final String adb_directory;

    public ADBExecutor(String adb_directory) {
        this.adb_directory = adb_directory;
    }

    //得到当前连接上的设备的序列号
    public ArrayList<String> execAdbDevices() {
        System.out.println("adb devices");

        ArrayList<String> ret_device_id_list = new ArrayList<String>();
        Process proc = null;
        try {
            proc = new ProcessBuilder(this.adb_directory, "devices").start();
            proc.waitFor();
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

        String devices_result = this.collectResultFromProcess(proc);
        String[] device_id_list = devices_result.split("\\r?\\n");

        if (device_id_list.length <= 1) {
            System.out.println("No Devices Attached.");
            return ret_device_id_list;
        }

        /**
         * collect the online devices
         */
        String str_device_id = null;
        String device = null;
        String[] str_device_id_parts = null;
        // ignore the first line which is "List of devices attached"
        for (int i = 1; i < device_id_list.length; i++) {
            str_device_id = device_id_list[i];
            str_device_id_parts = str_device_id.split("\\s+");
            // add the online device
            if (str_device_id_parts[1].equals("device")) {
                device = str_device_id_parts[0];
                ret_device_id_list.add(device);
                System.out.println(device);
            }
        }
        //System.exit(0);
        return ret_device_id_list;
    }

    //对于当前连接上的设备都进行映射
    public HashMap<String, Integer> execAdbOnlineDevicesPortForward() {
        List<String> device_id_list = this.execAdbDevices();
        HashMap<String, Integer> device_hostport_map = new HashMap<String, Integer>();

        int index = 0;
        for (String device : device_id_list) {

            int host_port = ADBExecutor.HOST_BASE_PORT + index * 10;
            this.execAdbSingleDevicePortForward(device, host_port, ADBExecutor.ANDROID_PORT);
            device_hostport_map.put(device, host_port);
            index++;
        }

        return device_hostport_map;
    }

    //具体的映射过程
    public void execAdbSingleDevicePortForward(String device_id, int host_port, int to_port) {
        System.out.println("adb -s " + device_id + " forward 本地 tcp:" + host_port + " USB连接的手机 tcp:" + to_port);

        Process proc = null;
        try {
            proc = new ProcessBuilder(this.adb_directory, "-s", device_id, "forward", "tcp:" + host_port, "tcp:" + to_port).start();
            proc.waitFor();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ire) {
            ire.printStackTrace();
        }
    }

    //手机命令执行之后的结果
    private String collectResultFromProcess(Process proc) {
        StringBuilder sb_result = new StringBuilder();

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        String result_line = null;

        try {
            while ((result_line = stdInput.readLine()) != null) {
                sb_result.append(result_line);
                sb_result.append("\n");
            }

            while ((result_line = stdError.readLine()) != null) {
                sb_result.append(result_line);
                sb_result.append("\n");
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return sb_result.toString();
    }
}
