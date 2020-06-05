package com.slack.androidclient.screen;

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
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.slack.androidclient.R;
import com.slack.androidclient.WrapWebSocketServer;

import java.io.File;
import java.io.IOException;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Created by slack on 2020/6/3 下午3:55.
 * 提供屏幕录制服务, 简单录屏，保存为视频文件
 * android 10:
 * 1. Caused by: java.lang.SecurityException: Media projections require a foreground service of type ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
 *  创建启动前台服务startForegroundService()，创建前台 Notification
 * 2. 文件读写 android:requestLegacyExternalStorage="true"
 */
public class RecordService extends Service {

    private boolean isRecording;
    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder; // 使用MediaRecorder，录制保存为视频
    private VirtualDisplay mVirtualDisplay;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        print("onBind...");
        return new RecordBinder();
    }

    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
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
        if (isRecording) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        print("startRecord..." + data.toString() + ", " + width + ", " + height + ", " + dpi);
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager != null) {
            mMediaProjection = manager.getMediaProjection(-1, data);
        }

        prepareMediaRecorder(width, height);
        prepareVirtualDisplay(width, height, dpi);
        mMediaRecorder.start();
        isRecording = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stopRecord() {
        if (!isRecording) {
            return;
        }
        print("stopRecord...");
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                mVirtualDisplay.release();
                mMediaProjection.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mMediaRecorder.release();
        }
        mMediaRecorder = null;
        mMediaProjection = null;
        isRecording = false;
    }


    public void pauseRecord() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (mMediaRecorder != null) {
                mMediaRecorder.pause();
            }
        }
    }

    public void resumeRecord() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (mMediaRecorder != null) {
                mMediaRecorder.resume();
            }
        }
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

    /**
     * MediaRecorder
     */
    private void prepareMediaRecorder(int width, int height) {

        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        File file = new File(Environment.getExternalStorageDirectory(), "screen_record.mp4");
        String path = file.getAbsolutePath();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(path);
        mMediaRecorder.setVideoSize(width, height);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncodingBitRate((int) (width * height * 3.6));
        mMediaRecorder.setVideoFrameRate(20);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * VirtualDisplay
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void prepareVirtualDisplay(int width, int height, int dpi) {
        if (mMediaProjection == null) {
            return;
        }

        mVirtualDisplay = mMediaProjection.createVirtualDisplay("slack_screen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    private void print(String info) {
        Log.i("slack", info);
    }
}
