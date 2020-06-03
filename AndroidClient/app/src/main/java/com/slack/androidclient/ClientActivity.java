package com.slack.androidclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 通过 wifi 连接 服务器
 */
public class ClientActivity extends AppCompatActivity implements View.OnClickListener {

    private WrapSocket socket;
    private WrapWebSocket webSocket;
    private Handler handler;
    private TextView info;
    private Button button;
    private boolean useSocket = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        findViewById(R.id.connectBtn).setOnClickListener(this);
        findViewById(R.id.handle_close).setOnClickListener(this);
        findViewById(R.id.readBtn).setOnClickListener(this);
        info = findViewById(R.id.readInfo);
        findViewById(R.id.handle_down).setOnClickListener(this);
        findViewById(R.id.handle_enter).setOnClickListener(this);
        findViewById(R.id.handle_left).setOnClickListener(this);
        findViewById(R.id.handle_right).setOnClickListener(this);
        findViewById(R.id.handle_up).setOnClickListener(this);
        findViewById(R.id.handle_esc).setOnClickListener(this);

        button = findViewById(R.id.socketBtn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                useSocket = !useSocket;
                if (useSocket) {
                    button.setText("Use Socket");
                } else {
                    button.setText("Use WebSocket");
                }
            }
        });
        useSocket = true;

        HandlerThread thread = new HandlerThread("slack");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connectBtn:
                getConnect();
                break;
            case R.id.handle_close:
                disconnect();
                break;
            case R.id.readBtn:
                read();
                break;
            case R.id.handle_left:
                send("[left]");
                break;
            case R.id.handle_enter:
                send("[enter]");
                break;
            case R.id.handle_right:
                send("[right]");
                break;
            case R.id.handle_down:
                send("[down]");
                break;
            case R.id.handle_up:
                send("[up]");
                break;
            case R.id.handle_esc:
                send("[esc]");
                break;
        }
    }

    private void getConnect() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (useSocket) {
                        socket = connect();
                    } else {
                        webSocket = connectWebSocket();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 返回套接字句�?	 * @return Socket
     *
     * @throws Exception
     */
    public WrapSocket connect() {
        EditText etIp = (EditText) findViewById(R.id.ip);
        String host = etIp.getText().toString();
        EditText etPort = (EditText) findViewById(R.id.port);
        int port = Integer.parseInt(etPort.getText().toString());
        return new WrapSocket(host, port);
    }

    public WrapWebSocket connectWebSocket() {
        EditText etIp = (EditText) findViewById(R.id.ip);
        String host = etIp.getText().toString();
        EditText etPort = (EditText) findViewById(R.id.port);
        int port = Integer.parseInt(etPort.getText().toString());
        WrapWebSocket socket = new WrapWebSocket(host, port);
        socket.connect();
        return socket;
    }

    public void read() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                final String result;
                if (useSocket) {
                    result = socket.read();
                } else {
                    result = webSocket.read();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        info.setText(result);
                    }
                });
            }
        });
    }

    /**
     * 断开套接连接
     *
     * @throws Exception
     */
    public void disconnect() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (null != socket) {
                        socket.close();
                    }
                    if (null != webSocket) {
                        webSocket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void send(final String str) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (useSocket) {
                        if (socket.isConnected()) {
                            socket.send(str);
                        }
                    } else {
                        if (webSocket.isConnected()) {
                            webSocket.send(str);
                        }
                    }
                    Log.i("slack", "send: " + str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
