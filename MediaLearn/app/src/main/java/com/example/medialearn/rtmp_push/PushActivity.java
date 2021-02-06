package com.example.medialearn.rtmp_push;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.medialearn.R;

import java.io.IOException;

/**
 * Created by liuzhe
 * DateTime: 2021/1/23
 * Description:
 */
public class PushActivity extends Activity {


    private CameraView cameraView;
    private PushManager pushManager;

    private static final String liveUrl = "rtmp://192.168.0.102:1935/live/test";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push);
        cameraView = findViewById(R.id.cameraView);
        cameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.requestCameraFocus();
            }
        });

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pushManager = new PushManager();
                try {
                    pushManager.init(PushActivity.this, liveUrl,
                            720 / 2, 1280 / 2, cameraView.getEglContext(), cameraView.getTextureId());
                    pushManager.startPush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


    }
}
