package com.example.medialearn.camera1_demo;

import android.Manifest;
import android.app.Activity;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.example.medialearn.R;

import java.io.IOException;

/**
 * Created by liuzhe
 * DateTime: 2021/2/13
 * Description:
 */
public class Camera1DemoAty extends Activity {

    private SurfaceView surfaceView;
    private CameraHelp cameraHelp;
    private View container;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera1_demo);

        container = findViewById(R.id.container);
        surfaceView = findViewById(R.id.surfaceview);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraHelp.openBackCamera();
                cameraHelp.startPreview(surfaceView.getHolder());
            }
        });
        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraHelp.openFrontCamera();
                cameraHelp.startPreview(surfaceView.getHolder());
            }
        });

        cameraHelp = new CameraHelp(this, 1920, 1080);

        surfaceView.post(new Runnable() {
            @Override
            public void run() {
                int containerHeight = container.getHeight();
                float rate = cameraHelp.getPreviewHeight() * 1.0f / cameraHelp.getPreviewWidth();
                ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
                lp.width = (int) (containerHeight * rate);
                lp.height = containerHeight;
                surfaceView.setLayoutParams(lp);
            }
        });

        requestPermissions(new String[] {Manifest.permission.CAMERA}, 10);
    }

    private static class CameraHelp {

        private static final String TAG = "CameraUtil";

        private Activity activity;
        private Camera mCamera;
        private int previewWidth;
        private int previewHeight;


        public CameraHelp(Activity activity,int previewWidth, int previewHeight) {
            this.activity = activity;
            this.previewWidth = previewWidth;
            this.previewHeight = previewHeight;
        }




        public void openFrontCamera(){
            openCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }

        public void openBackCamera(){
            openCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        }

        public void startPreview(SurfaceHolder surfaceHolder){
            if (mCamera != null) {
                try {
                    mCamera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
            }
        }

        public int getPreviewWidth() {
            return previewWidth;
        }

        public int getPreviewHeight() {
            return previewHeight;
        }

        private void openCamera(int cameraId){
            releaseCamera();

            Camera camera = Camera.open(cameraId);

            Camera.Parameters parameters = camera.getParameters();
            Camera.Size ppsfv = parameters.getPreferredPreviewSizeForVideo();

            boolean find = false;
            for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                if (size.width == previewWidth && size.height == previewHeight) {
                    parameters.setPreviewSize(previewWidth, previewHeight);
                    find = true;
                    break;
                }
            }

            if (ppsfv != null && !find) {
                parameters.setPreviewSize(ppsfv.width, ppsfv.height);
            }

            setCamerDisplayOrientation(activity, camera, cameraId);

            camera.setParameters(parameters);
            mCamera = camera;
        }


        private void setCamerDisplayOrientation(Activity activity,  Camera camera, int cameraId){
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Log.i(TAG, "setCamerDisplayOrientation: rotation=" + rotation + " cameraId=" + cameraId);
            int degress = 0;

            switch (rotation) {
                case Surface.ROTATION_0:
                    degress = 0;
                    break;
                case Surface.ROTATION_90:
                    degress = 90;
                    break;
                case Surface.ROTATION_180:
                    degress = 180;
                    break;
                case Surface.ROTATION_270:
                    degress = 270;
                    break;
            }
            int result = 0;
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (cameraInfo.orientation + degress) % 360;
                result = (360 - result) % 360;

            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                result = (cameraInfo.orientation - degress + 360) % 360;
            }
            Log.i(TAG, "setCamerDisplayOrientation: result=" + result + " cameraId=" + cameraId + " facing=" + cameraInfo.facing + " cameraInfo.orientation=" + cameraInfo.orientation);

            camera.setDisplayOrientation(result);
        }

        public void releaseCamera(){
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }

    }


}
