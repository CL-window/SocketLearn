package com.slack.androidclient.screen;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.slack.androidclient.R;

/**
 * Created by slack on 2020/6/3 下午6:02.
 * 可以随手指移动的悬浮窗
 * 默认位于 屏幕垂直居中，居右
 */
public class FloatView extends FrameLayout {

    private WindowManager windowManager;
    private final WindowManager.LayoutParams layoutParams;
    private float downX, downY;
    private int screenW, screenH;
    private ImageView imageView;

    public FloatView(Context context, boolean showImg) {
        super(context);
        LayoutInflater.from(getContext()).inflate(R.layout.float_window, this);
        imageView = findViewById(R.id.float_window_img);
        if (showImg) {
            imageView.setVisibility(VISIBLE);
        } else {
            imageView.setVisibility(GONE);
        }
        layoutParams = new WindowManager.LayoutParams();
    }

    private Bitmap bitmap;
    public void updateImage(Bitmap bit) {
        Bitmap last = this.bitmap;
        this.bitmap = bit;
        imageView.setImageBitmap(bitmap);
        if (last != null && !last.isRecycled()) {
            last.recycle();
        }
    }

    private void getWindowManager(Context context) {
        if (windowManager == null) {
            if (context == null) {
                context = getContext();
            }
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }
        if (screenW == 0 || screenH == 0) {
            if (windowManager != null) {
                Display display = windowManager.getDefaultDisplay();
                DisplayMetrics metrics = new DisplayMetrics();
                display.getMetrics(metrics);
                screenW = metrics.widthPixels;
                screenH = metrics.heightPixels;
            }
        }
    }

    /**
     * 屏幕垂直居中，居右 , 所以屏幕的 最右边垂直方向的中点为 (0, 0)
     */
    public void addToWindow(Context context) {
        getWindowManager(context);
        //设置悬浮窗布局属性
        //设置类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        //设置行为选项
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //设置悬浮窗的显示位置
        layoutParams.gravity = Gravity.RIGHT;
        //设置x轴的偏移量
        layoutParams.x = 0;
        //设置y轴的偏移量
        layoutParams.y = 0;
        //如果悬浮窗图片为透明图片，需要设置该参数为PixelFormat.RGBA_8888
        layoutParams.format = PixelFormat.RGBA_8888;
        //设置悬浮窗的宽度
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        //设置悬浮窗的高度
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        //加载显示悬浮窗
        windowManager.addView(this, layoutParams);
    }

    public void removeFromWindow() {
        getWindowManager(null);
        if (windowManager != null) {
            try {
                windowManager.removeView(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        onTouch(event);
        return super.onTouchEvent(event);
    }

    private void onTouch(MotionEvent event) {
        getWindowManager(null);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getRawX() + layoutParams.x;
                downY = event.getRawY() - layoutParams.y;
                break;
            case MotionEvent.ACTION_MOVE:
                updatePosition(event.getRawX(), event.getRawY());
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // 可以做移动结束后滑动到侧边栏，即固定在左右两边侧边栏
                break;
        }
    }

    /**
     * 坐标系：
     *                  ^
     *                  | (0, 1)
     *                  |
     *                  |
     *                  |
     *  (-1, 0)         |
     * <----------------- (0, 0)
     *                  |
     *                  |
     *                  |
     *                  |
     *                  | (0, -1)
     *
     *
     */
    private void updatePosition(float touchX, float touchY) {
        layoutParams.x = (int) (downX - touchX);
        layoutParams.y = (int) (touchY - downY);;
//        Log.i("slack", "onTouchEvent:" + downX + ", " + downY + "，" +  touchX + ", " + touchY + ", "  + layoutParams.x + ", " + layoutParams.y);
        windowManager.updateViewLayout(this, layoutParams);
    }

}
