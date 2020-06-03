## Socket 使用
* 只是简单的测试学习，打通了Android作为客户端连接server 和 Android作为服务器提供服务以及通过USB传送数据，未来可以做很多的东西，比如实现手机截屏内容实时传输在电脑上，在电脑上用鼠标控制手机
#### 使用Android作为客户端连接服务器
* 通过Wifi 连接， AndroidClient 使用 Socket 连接服务器, 实现了两种，一种是Socket, 一种是前辈封装好的 Java_WebSocket
* Server.java 其实就是使用Socket，服务器使用 ServerSocket，就一个Java文件，main()方法直接允许
* Server文件夹 使用 [Java_WebSocket](https://github.com/TooTallNate/Java-WebSocket),可以找到很多的例子，这个需要使用第三方库，时使用IntelliJ IDAE 创建的一个Java项目

* AndroidClient 的 客户端 里测试使用
    * 默认使用Socket, 即需要启动 Server.java
    * 可以切换为使用 WebSocket, 需要启动 Server文件夹, 地址为测试服务器的地址
    * 先点击 连接（Connect Server）
    * 客户端可以发送消息，但是需要先连接上服务器才可以发消息
    * 服务器在收到客户端的消息后会返回一条消息，客户端使用 (Read From Server)按钮读取服务器的消息

#### 使用Android作为客户端连接服务器
* AndroidClient 的 服务器 里开启和关闭服务
    * 根据Android的IP地址连接服务器，测试代码位于 Server文件夹 下 Main.java
    * 通过USB连接后，通过USB连接服务器，测试代码位于 Server文件夹 下 Main.java， 用到了ADBExecutor.java
* 通过WIFI连接，需要知道手机的IP地址
* 通过USB连接，是在手机连接电脑后，通过命令 adb device 可以正常发现设备的情况下，使用命令 adb forward 端口转发
* [adb forward ](https://developer.android.com/studio/command-line/adb) 
    ```
    adb forward tcp:111 tcp:222
    1.操作步骤是手机端先开一个server, 监听222端口
    2.PC端开启一个client, 监听111端口
    命令要在步骤2之前执行

    PC端                        Android手机端
   客户端连接111端口            server监听222端口
        |                           |
        |socket                     |socket
        |                           |
    adb进程监听111  ---adb转发---  adb进程连接到222端口的server

    ```
