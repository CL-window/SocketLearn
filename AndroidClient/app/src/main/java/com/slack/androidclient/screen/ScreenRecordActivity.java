package com.slack.androidclient.screen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Toast;

import com.slack.androidclient.R;
import com.slack.androidclient.screen.record.RecordService3;

import java.util.List;

public class ScreenRecordActivity extends AppCompatActivity {

    private final static int REQ_WRITE_CODE = 0x23;
    private final static int REQ_SCREEN_RECORD_CODE = 0x23;
    private RecordService mRecordService;
    private RecordService2 mRecordService2;
    private RecordService3 mRecordService3;
    private FloatView floatView;

    private State state = State.State_Screen_File;

    public enum State {
        State_Screen_File, // 保存为mp4
        State_Screen_Show, // 取出每一帧展示
        State_Screen_Push, // 取出每一帧编码，推流

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_recodr);
    }

    /**
     * 1.检查文件读写权限: 音频，写文件， 悬浮窗
     * 2.请求屏幕录制权限
     * 3.启动服务 开始录制
     */
    public void startRecordToFile(View view) {
        state = State.State_Screen_File;
        initService();
        alert("最好打开悬浮窗权限...");
        if (checkWritePermission(this)) {
            checkScreenRecordPermission(this);
        }
    }

    public void startRecordShow(View view) {
        state = State.State_Screen_Show;
        initService();
        alert("最好打开悬浮窗权限...");
        if (checkWritePermission(this)) {
            checkScreenRecordPermission(this);
        }
    }

    public void startRecordPush(View view) {
        state = State.State_Screen_Push;
        initService();
        alert("最好打开悬浮窗权限...");
        if (checkWritePermission(this)) {
            checkScreenRecordPermission(this);
        }
    }

    public void stopRecord(View view) {
        switch (state) {
            case State_Screen_Show:
                if (mRecordService2 != null) {
                    mRecordService2.stopRecord();
                }
                break;
            case State_Screen_Push:
                if (mRecordService3 != null) {
                    mRecordService3.stopRecord();
                }
                break;
            default:
                if (mRecordService != null) {
                    mRecordService.stopRecord();
                }
                break;
        }
        removeFloatWin();
    }

    private boolean checkWritePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkPermission =
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            + ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                //动态申请
                ActivityCompat.requestPermissions(activity, new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_CODE);
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 简单处理，没有再次检查判断权限
        if (requestCode == REQ_WRITE_CODE) {
            boolean allGrant = true;
            for (int result : grantResults) {
                if (result != PermissionChecker.PERMISSION_GRANTED) {
                    allGrant = false;
                    break;
                }
            }
            if (allGrant) {
                // 开始录制
                checkScreenRecordPermission(this);
            } else {
                // 打开设置页面
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + ScreenRecordActivity.this.getPackageName()));
                ScreenRecordActivity.this.startActivity(intent);
            }
        }
    }

    /**
     * MediaProjectionManager since 5.0+
     */
    private void checkScreenRecordPermission(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager manager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (manager != null) {
                Intent intent = manager.createScreenCaptureIntent();
                PackageManager packageManager = activity.getPackageManager();
                if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    //存在录屏授权的Activity
                    activity.startActivityForResult(intent, REQ_SCREEN_RECORD_CODE);
                } else {
                    alert("暂时无法录屏...");
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SCREEN_RECORD_CODE && resultCode == RESULT_OK) {
            startRecordImpl(data);
        }
    }

    @Override
    protected void onDestroy() {
        removeFloatWin();
        unbindService(mServiceConnection);
        super.onDestroy();
    }

    @Override
    public void finish() {
        stopRecord(null);
        super.finish();
    }

    private Callback callback = new Callback() {
        @Override
        public void callback(Bitmap bitmap) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (floatView != null) {
                        floatView.updateImage(bitmap);
                    }
                }
            });
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            switch (state) {
                case State_Screen_Show:
                    RecordService2.RecordBinder recordBinder1 = (RecordService2.RecordBinder) service;
                    mRecordService2 = recordBinder1.getRecordService();
                    mRecordService2.setCallback(callback);
                    break;
                case State_Screen_Push:
                    RecordService3.RecordBinder recordBinder3 = (RecordService3.RecordBinder) service;
                    mRecordService3 = recordBinder3.getRecordService();
                    mRecordService3.setCallback(callback);
                    break;
                default:
                    RecordService.RecordBinder recordBinder2 = (RecordService.RecordBinder) service;
                    mRecordService = recordBinder2.getRecordService();
                    break;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void initService() {
        Intent intent = new Intent(this, getServiceClass());
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private Class<?> getServiceClass() {
        switch (state) {
            case State_Screen_Show:
                return RecordService2.class;
            case State_Screen_Push:
                return RecordService3.class;
            default:
                return RecordService.class;
        }
    }

    private void startRecordImpl(Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initWindow();
            Display display = getWindowManager().getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(this, getServiceClass());
                intent.putExtra("data", data);
                intent.putExtra("width", metrics.widthPixels);
                intent.putExtra("height", metrics.heightPixels);
                intent.putExtra("dpi", metrics.densityDpi);
                startForegroundService(intent);
            } else {
                switch (state) {
                    case State_Screen_Show:
                        if (mRecordService2 != null) {
                            mRecordService2.startRecord(data, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
                        }
                        break;
                    case State_Screen_Push:
                        if (mRecordService3 != null) {
                            mRecordService3.startRecord(data, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
                        }
                        break;
                    default:
                        if (mRecordService != null) {
                            mRecordService.startRecord(data, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
                        }
                        break;
                }
            }
        }
    }

    private void initWindow() {
        boolean show = false;
        switch (state) {
            case State_Screen_Show:
                show = true;
                break;
        }
        floatView = new FloatView(this, show);
        floatView.addToWindow(this);
        floatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.i("slack", "FloatView onClick...");
//                stopRecord(null);
                moveToFront();
            }
        });
    }

    /**
     * 这个方法是把activity唤起到前台，有点问题，最好用启动Activity的方式，设置为 singleTask
     */
    protected void moveToFront() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return;
        }
        String packageName = this.getPackageName();
        List<ActivityManager.RunningTaskInfo> recentTasks = manager.getRunningTasks(100);
        for (ActivityManager.RunningTaskInfo recentTask : recentTasks) {
            if (recentTask.baseActivity.getPackageName().equals(packageName)) {
                manager.moveTaskToFront(recentTask.id, ActivityManager.MOVE_TASK_WITH_HOME);
                Log.i("slack", "FloatView moveTaskToFront...");
                break;
            }
        }
    }

    private void removeFloatWin() {
        if (floatView != null) {
            floatView.removeFromWindow();
            floatView = null;
        }
    }

    private void alert(String info) {
        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
    }
}
