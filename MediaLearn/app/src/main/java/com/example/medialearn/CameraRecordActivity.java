package com.example.medialearn;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.medialearn.camera_help.CameraUtils;
import com.example.medialearn.elg.EglCore;
import com.example.medialearn.elg.WindowSurface;
import com.example.medialearn.gles.FullFrameRect;
import com.example.medialearn.gles.Texture2dProgram;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by liuzhe
 * DateTime: 2020/12/11
 * Description: 带水印的视频录制
 */
public class CameraRecordActivity extends Activity {

    private EglCore mEglCore;
    private WindowSurface mDisplaySurface;
    private WindowSurface mEncoderSurface;
    private SurfaceTexture mCameraTexture;
    private FullFrameRect mFullFrameBlit;


    private int mTextureId;
    private final float[] mTmpMatrix = new float[16];


    private Camera mCamera;
    private SurfaceView surfaceView;


    private MainHandler mainHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_record);
        surfaceView = findViewById(R.id.surface);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(Constans.LOG_TAG, "surfaceCreated");
                mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
                mDisplaySurface = new WindowSurface(mEglCore, holder.getSurface(), false);
                mDisplaySurface.makeCurrent();

                mFullFrameBlit = new FullFrameRect(
                        new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
                mTextureId = mFullFrameBlit.createTextureObject();
                mCameraTexture = new SurfaceTexture(mTextureId);
                mCameraTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        mainHandler.sendEmptyMessage(MainHandler.MSG_DRAW_FRAME);
                    }
                });
                startPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(Constans.LOG_TAG, "surfaceChanged");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(Constans.LOG_TAG, "surfaceDestroyed");
            }
        });

        mainHandler = new MainHandler(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        openCamera(1280, 720);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void openCamera(int desiredWidth, int desiredHeight) {
        if (mCamera == null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            int numCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numCameras; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCamera = Camera.open(i);
                    break;
                }
            }

            if (mCamera == null) {
                return;
            }

            Camera.Parameters parms = mCamera.getParameters();
            CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);
            parms.setRecordingHint(true);
            mCamera.setParameters(parms);

            mCamera.setDisplayOrientation(90);

            Log.d(Constans.LOG_TAG, "open camera");
        }
    }

    private void startPreview() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(mCameraTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
            Log.d(Constans.LOG_TAG, "camera startPreview");
        }
    }

    private void drawframe(){
        mDisplaySurface.makeCurrent();
        mCameraTexture.updateTexImage();
        mCameraTexture.getTransformMatrix(mTmpMatrix);

        int viewWidth = surfaceView.getWidth();
        int viewHeight = surfaceView.getHeight();
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        mFullFrameBlit.drawFrame(mTextureId, mTmpMatrix);

        mDisplaySurface.swapBuffers();
    }


    private static class MainHandler extends Handler {

        public static final int MSG_DRAW_FRAME = 10;

        private final WeakReference<CameraRecordActivity> atyRef;

        public MainHandler(CameraRecordActivity activity) {
            atyRef = new WeakReference<>(activity);
        }


        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_DRAW_FRAME:
                    if (atyRef.get() != null) {
                        atyRef.get().drawframe();
                    }
                    break;
            }
        }
    }

    private class CameraTexutre {

    }

}
