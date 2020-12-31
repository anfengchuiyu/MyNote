package com.example.medialearn;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.medialearn.test.NativeTest;

/**
 * Created by liuzhe
 * DateTime: 2020/12/27
 * Description:
 */
public class NativeEGLActivity extends Activity {

    private HandlerThread glRenderThread;
    private Handler renderHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_egl);

        SurfaceView surfaceView = findViewById(R.id.surfaceview);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                glRenderThread = new HandlerThread("gl_render_thread");
                glRenderThread.start();
                renderHandler = new RenderHandler(glRenderThread.getLooper());
                renderHandler.sendEmptyMessage(0);

                Message msg = renderHandler.obtainMessage(1, holder.getSurface());
                msg.sendToTarget();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Message msg = renderHandler.obtainMessage(2, width, height);
                msg.sendToTarget();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                renderHandler.sendEmptyMessage(3);
                glRenderThread.quitSafely();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        glRenderThread.quitSafely();
    }

    private class RenderHandler extends Handler {

        public RenderHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Log.d("hello", msg.toString());
            switch (msg.what) {
                case 0:
                    NativeTest.native_eglinit();
                    break;
                case 1:
                    Surface surface = (Surface) msg.obj;
                    NativeTest.native_surfaceCreated(surface);
                    break;
                case 2:
                    NativeTest.native_surfaceChanged(msg.arg1, msg.arg2);
                    break;
                case 3:
                    NativeTest.native_surfaceDestroyed();
                    glRenderThread.quit();
                    break;
            }
        }
    }

}
