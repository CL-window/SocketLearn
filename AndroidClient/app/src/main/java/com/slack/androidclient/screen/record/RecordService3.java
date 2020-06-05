package com.slack.androidclient.screen.record;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.slack.androidclient.R;
import com.slack.androidclient.screen.Callback;
import com.slack.androidclient.screen.ScreenRecordActivity;
import com.slack.androidclient.screen.encode.VideoEncodeConfig;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Created by slack on 2020/6/3 下午3:55.
 * 提供屏幕录制服务 编码，推流
 * 录屏的数据，需要编码后才可以使用
 * 只编码了视频，音频一样的处理逻辑
 */
public class RecordService3 extends Service {

    private boolean isRecording;
    private MediaProjection mMediaProjection;
    private ScreenRecorder mScreenRecorder;
//    WrapWebSocketServer server = new WrapWebSocketServer();


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        print("onBind...");
        return new RecordBinder();
    }

    public class RecordBinder extends Binder {
        public RecordService3 getRecordService() {
            return RecordService3.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        print("onStartCommand...");
        createNotificationChannel();

        Intent data = intent.getParcelableExtra("data");
        int width = intent.getIntExtra("width", 1080);
        int height = intent.getIntExtra("height", 1920);
        int dpi = intent.getIntExtra("dpi", 360);
        startRecord(data, width, height, dpi);

        return super.onStartCommand(intent, flags, startId);
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecord(Intent data, int width, int height, int dpi) {
        print("startRecord3..." + data.toString() + ", " + width + ", " + height + ", " + dpi);
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager != null) {
            mMediaProjection = manager.getMediaProjection(-1, data);
        }

        prepareVideoEncoder(width, height, dpi);
        if (mScreenRecorder != null) {
            mScreenRecorder.start();
        }
        isRecording = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stopRecord() {
        if (!isRecording) {
            return;
        }
        print("stopRecord3...");
        try {
            if (mScreenRecorder != null) {
                mScreenRecorder.quit();
            }
            if (mMediaProjection != null) {
                mMediaProjection.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMediaProjection = null;
    }


    /**
     * Notification
     */
    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        Intent nfIntent = new Intent(this, ScreenRecordActivity.class); //点击后跳转的界面，可以设置跳转数据

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("正在录制屏幕...") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音
        startForeground(110, notification);

    }

    private void prepareVideoEncoder(final int width, final int height, int dpi) {
        if (mMediaProjection == null) {
            return;
        }
        VirtualDisplay virtualDisplay = mMediaProjection.createVirtualDisplay("slack_screen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, null, null, null);

        int bitrate = (int) (width * height * 3.6);
        int framerate = 20;
        int iFrameInternal = 10;
        VideoEncodeConfig video = new VideoEncodeConfig(width, height, bitrate,
                framerate, iFrameInternal, null);

        File file = new File(Environment.getExternalStorageDirectory(), "screen_record.mp4");
        String path = file.getAbsolutePath();
        mScreenRecorder = new ScreenRecorder(video, null, virtualDisplay, path);
    }

    private void print(String info) {
        Log.i("slack", info);
    }

    private Callback callback;
    public void setCallback(Callback c) {
        callback = c;
    }

}
