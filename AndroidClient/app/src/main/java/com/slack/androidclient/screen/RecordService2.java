package com.slack.androidclient.screen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import com.slack.androidclient.R;
import com.slack.androidclient.WrapWebSocketServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Created by slack on 2020/6/3 下午3:55.
 * 提供屏幕录制服务 编码，推流
 * 录屏的数据，需要编码后才可以使用
 * 只编码了视频，音频一样的处理逻辑
 */
public class RecordService2 extends Service {

    private boolean isRecording;
    private MediaProjection mMediaProjection;
    private ImageReader mImageReader; // 使用ImageReader，获取单帧数据
    private VirtualDisplay mVirtualDisplay;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        print("onBind...");
        return new RecordBinder();
    }

    public class RecordBinder extends Binder {
        public RecordService2 getRecordService() {
            return RecordService2.this;
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
        print("startRecord2..." + data.toString() + ", " + width + ", " + height + ", " + dpi);
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager != null) {
            mMediaProjection = manager.getMediaProjection(-1, data);
        }

        prepareImageReader(width, height);
        prepareVirtualDisplay(width, height, dpi);
        isRecording = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void stopRecord() {
        if (!isRecording) {
            return;
        }
        print("stopRecord2...");
        try {
            if (mImageReader != null) {
                mImageReader.setOnImageAvailableListener(null, null);
                mImageReader.close();
            }
            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
            }
            if (mMediaProjection != null) {
                mMediaProjection.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mImageReader = null;
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

    private void prepareImageReader(final int width, final int height) {
        mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        HandlerThread thread = new HandlerThread("screen_reader");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                try {
                    Image image = null;
                    try {
                        image = mImageReader.acquireLatestImage();
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    if (image == null) {
                        return;
                    }
                    Image.Plane[] planes = image.getPlanes();
                    if (planes == null || planes.length == 0 || planes[0] == null) {
                        image.close();
                        return;
                    }

                    ByteBuffer byteBuffer = planes[0].getBuffer();
                    if (byteBuffer == null) {
                        image.close();
                        return;
                    }


                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;

                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(byteBuffer);

//                    saveToFile(bitmap);

                    if (callback != null) {
                        callback.callback(bitmap);
                    }
                    image.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }, handler);
    }

    private boolean save = true;
    private void saveToFile(Bitmap bitmap) {
        if (save) {
            save = false;
            try {
                File file = new File(Environment.getExternalStorageDirectory(), "file_screen_record.jpg");
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
    }

    private void print(String info) {
        Log.i("slack", info);
    }

    private Callback callback;
    public void setCallback(Callback c) {
        callback = c;
    }

}
