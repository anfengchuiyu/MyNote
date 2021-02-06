package com.example.medialearn.rtmp_push;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.opengl.EGLContext;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.medialearn.LivePush;
import com.example.medialearn.elg.EglCore;
import com.example.medialearn.elg.WindowSurface;
import com.example.medialearn.gles.GlUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by liuzhe
 * DateTime: 2021/1/24
 * Description:
 */
public class PushManager {

    private Context mContext;

    //相机共享的EGL上下文
    private EGLContext mEglContext;


    //硬编码MediaCodec的surface
    private Surface mSurface;

    private LivePush mLivePush;

    private GLSurfaceView.Renderer pushRender;

    private int mVideoWidth;
    private int mVideoHeight;
    private String mLiveUrl;

    private MediaCodec mVideoCodec;
    private MediaCodec mAudioCodec;


    private VideoRenderThread videoRenderThread;
    private VideoEncoderThread videoEncoderThread;
    private AudioRecordThread audioRecordThread;
    private AudioEncoderThread audioEncoderThread;


    private static final int AUDIO_SAMPLE_RATE = 44100;
    private static final int AUDIO_CHANNELS = 2;

    public void init(Context context, String liveUrl, int videoWidth, int videoHeight, EGLContext eglContext, int textureId) throws IOException {
        mContext = context;
        mLivePush = new LivePush();
        mLiveUrl = liveUrl;
        mVideoWidth = videoWidth;
        mVideoHeight = videoHeight;


        Log.d("zhe", "camera fbo textureId=" + textureId);

        mEglContext = eglContext;
        pushRender = new PushRender(textureId);

        mLivePush.setConnectListener(new LivePush.ConnectListener() {
            @Override
            public void connectSuccess() {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "rtmp connect success", Toast.LENGTH_SHORT).show();
                    }
                });
                startThreads();
            }

            @Override
            public void connectError(final String errorMsg) {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "rtmp connect error: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startThreads();
            }
        }, 5000);


        // 创建视频编码器
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                mVideoWidth, mVideoHeight);
        //设置颜色格式
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoWidth * mVideoHeight * 4);
        //设置帧率
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 24);
        // 设置 I 帧的间隔时间
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        mVideoCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mVideoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mSurface = mVideoCodec.createInputSurface();

        // 开启一个编码采集 InputSurface 上的数据，合成视频
        videoEncoderThread = new VideoEncoderThread(this);


        // 创建音频编码器
        MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                AUDIO_SAMPLE_RATE, AUDIO_CHANNELS);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_SAMPLE_RATE * AUDIO_CHANNELS * 2);

        mAudioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        mAudioCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        audioRecordThread = new AudioRecordThread(this);
        audioEncoderThread = new AudioEncoderThread(this);

        videoRenderThread = new VideoRenderThread(this);
    }


    public void startPush() {
        mLivePush.startPush(mLiveUrl);
    }


    private void startThreads() {
        videoRenderThread.start();
        videoEncoderThread.start();
        audioEncoderThread.start();
        audioRecordThread.start();
    }

    //视频的编码线程
    private static final class VideoEncoderThread extends Thread {
        private WeakReference<PushManager> mPushManagerRef;
        private volatile boolean exit = false;
        private MediaCodec mVideoCodec;
        private MediaCodec.BufferInfo mBufferInfo;
        private long mVideoPts = 0;
        private byte[] mVideoSPS;
        private byte[] mVideoPPS;

        public VideoEncoderThread(PushManager pushManager) {
            mPushManagerRef = new WeakReference<>(pushManager);
            mBufferInfo = new MediaCodec.BufferInfo();
            mVideoCodec = pushManager.mVideoCodec;
        }

        @Override
        public void run() {
            try {
                mVideoCodec.start();
                while (!exit) {
                    PushManager pushManager = mPushManagerRef.get();
                    if (pushManager == null) {
                        return;
                    }

                    int outputBufferIndex = mVideoCodec.dequeueOutputBuffer(mBufferInfo, 0);
                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // 获取 sps 和 pps
                        ByteBuffer byteBuffer = mVideoCodec.getOutputFormat().getByteBuffer("csd-0");
                        mVideoSPS = new byte[byteBuffer.remaining()];
                        byteBuffer.get(mVideoSPS, 0, mVideoSPS.length);

                        byteBuffer = mVideoCodec.getOutputFormat().getByteBuffer("csd-1");
                        mVideoPPS = new byte[byteBuffer.remaining()];
                        byteBuffer.get(mVideoPPS, 0, mVideoPPS.length);
                    } else {
                        while (outputBufferIndex >= 0) {
                            // 获取数据
                            ByteBuffer outBuffer = mVideoCodec.getOutputBuffers()[outputBufferIndex];
                            outBuffer.position(mBufferInfo.offset);
                            outBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                            // 修改 pts
                            if (mVideoPts == 0) {
                                mVideoPts = mBufferInfo.presentationTimeUs;
                            }
                            mBufferInfo.presentationTimeUs -= mVideoPts;

                            //在关键帧前先把 sps 和 pps 推到流媒体服务器
                            if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                                mPushManagerRef.get().mLivePush.setVideoSpsPps(mVideoSPS,
                                        mVideoSPS.length, mVideoPPS, mVideoPPS.length);
                            }

                            byte[] data = new byte[outBuffer.remaining()];
                            outBuffer.get(data, 0, data.length);
                            mPushManagerRef.get().mLivePush.sendVideoPacket(data, data.length,
                                    mBufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME);


                            mVideoCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mVideoCodec.dequeueOutputBuffer(mBufferInfo, 0);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                onDestroy();
            }
        }


        private void onDestroy() {
            mVideoCodec.stop();
            mVideoCodec.release();
        }

        public void requestExit() {
            exit = true;
        }

    }

    //视频渲染线程
    private static final class VideoRenderThread extends Thread {
        private WeakReference<PushManager> mPushManagerRef;
        private EglCore mEglCore;
        private WindowSurface windowSurface;
        private volatile boolean exit = false;

        private boolean eglCreated = false;
        private boolean surfaceCreated = false;
        private boolean surfaceChanged = false;

        public VideoRenderThread(PushManager pushManager) {
            mPushManagerRef = new WeakReference<>(pushManager);

        }

        @Override
        public void run() {
            try {
                while (!exit) {
                    PushManager pushManager = mPushManagerRef.get();
                    if (pushManager == null) {
                        return;
                    }

                    if (!eglCreated) {
                        mEglCore = new EglCore(pushManager.mEglContext, EglCore.FLAG_TRY_GLES3);
                        windowSurface = new WindowSurface(mEglCore, pushManager.mSurface, false);
                        windowSurface.makeCurrent();
                        eglCreated = true;
                    }

                    if (!surfaceCreated) {
                        pushManager.pushRender.onSurfaceCreated(null, null);
                        surfaceCreated = true;
                    }

                    if (!surfaceChanged) {
                        pushManager.pushRender.onSurfaceChanged(null, pushManager.mVideoWidth, pushManager.mVideoHeight);
                        surfaceChanged = true;
                    }


                    pushManager.pushRender.onDrawFrame(null);

                    windowSurface.swapBuffers();

                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                onDestroy();
            }
        }


        private void onDestroy() {
            if (mEglCore != null) {
                mEglCore.release();
            }
            if (windowSurface != null) {
                windowSurface.release();
            }
        }

        public void requestExit() {
            exit = true;
        }

    }


    //音频编码线程
    private static final class AudioEncoderThread extends Thread {
        private WeakReference<PushManager> mPushManagerRef;
        private volatile boolean exit = false;
        private MediaCodec mAudioCodec;
        private MediaCodec.BufferInfo mBufferInfo;
        private long mAudioPts = 0;

        public AudioEncoderThread(PushManager pushManager) {
            mPushManagerRef = new WeakReference<>(pushManager);
            mAudioCodec = pushManager.mAudioCodec;
            mBufferInfo = new MediaCodec.BufferInfo();
        }

        @Override
        public void run() {
            try {
                // 开启 start AudioCodec
                mAudioCodec.start();

                while (!exit) {
                    PushManager pushManager = mPushManagerRef.get();

                    if (pushManager == null) {
                        return;
                    }

                    int outputBufferIndex = mAudioCodec.dequeueOutputBuffer(mBufferInfo, 0);
                    while (outputBufferIndex >= 0) {
                        // 获取数据
                        ByteBuffer outBuffer = mAudioCodec.getOutputBuffers()[outputBufferIndex];
                        outBuffer.position(mBufferInfo.offset);
                        outBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                        // 修改 pts
                        if (mAudioPts == 0) {
                            mAudioPts = mBufferInfo.presentationTimeUs;
                        }
                        mBufferInfo.presentationTimeUs -= mAudioPts;


                        byte[] data = new byte[outBuffer.remaining()];
                        outBuffer.get(data, 0, data.length);
                        //push audio data
                        pushManager.mLivePush.sendAudioPacket(data, data.length);


                        mAudioCodec.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = mAudioCodec.dequeueOutputBuffer(mBufferInfo, 0);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                onDestroy();
            }
        }


        private void onDestroy() {
            try {
                mAudioCodec.stop();
                mAudioCodec.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void requestExit() {
            exit = true;
        }

    }

    //音频采集线程
    private static final class AudioRecordThread extends Thread {
        private WeakReference<PushManager> mPushManagerRef;
        private volatile boolean exit = false;
        private long mAudioPts = 0;
        private AudioRecord mAudioRecord;
        // 这里是 pcm 数据
        private byte[] mAudioData;
        private final int mMinBufferSize;

        private MediaCodec mAudioCodec;

        public AudioRecordThread(PushManager pushManager) {
            mPushManagerRef = new WeakReference<>(pushManager);

            mAudioCodec = pushManager.mAudioCodec;

            mMinBufferSize = AudioRecord.getMinBufferSize(
                    PushManager.AUDIO_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);

            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    PushManager.AUDIO_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    mMinBufferSize);

            mAudioData = new byte[mMinBufferSize];
        }

        @Override
        public void run() {
            try {
                mAudioRecord.startRecording();

                while (!exit) {
                    // 不断读取 pcm 数据
                    mAudioRecord.read(mAudioData, 0, mMinBufferSize);

                    // 把数据写入到 mAudioCodec 的 InputBuffer
                    int inputBufferIndex = mAudioCodec.dequeueInputBuffer(0);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer byteBuffer = mAudioCodec.getInputBuffers()[inputBufferIndex];
                        byteBuffer.clear();
                        byteBuffer.put(mAudioData);

                        // pts  44100 * 2 *2
                        mAudioPts += mMinBufferSize * 1000000 / PushManager.AUDIO_SAMPLE_RATE
                                * PushManager.AUDIO_CHANNELS * 2;
                        // size 22050*2*2
                        mAudioCodec.queueInputBuffer(inputBufferIndex, 0, mMinBufferSize, mAudioPts, 0);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                onDestroy();
            }
        }

        private void onDestroy() {
            try {
                mAudioCodec.stop();
                mAudioCodec.release();
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }

        private void requestExit() {
            exit = true;
        }

    }


    private static class PushRender implements GLSurfaceView.Renderer {

        private float[] vertexCoord = new float[]{
                -1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f
        };

        private float[] fragmentCoord = new float[]{
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f
        };

        private FloatBuffer vertexBuffer;
        private FloatBuffer fragmentBuffer;
        private int mProgram;
        private int uTextureSamplerLocation;
        private int mTextureId;

        private String vertexStr =
                "#version 300 es\n" +
                        "layout(location = 0) in vec4 aPosition;\n" +
                        "layout(location = 1) in vec4 aTextureCoord;\n" +
                        "out vec2 vTextureCoord; //传给片元着色器\n" +
                        "void main(){\n" +
                        "\tvTextureCoord = aTextureCoord.xy;\n" +
                        "\tgl_Position = aPosition;\n" +
                        "}";

        private String fragStr =
                "#version 300 es\n" +
                        "precision mediump float;\n" +
                        "uniform sampler2D uTextureSampler;\n" +
                        "out vec2 vTextureCoord;\n" +
                        "out vec4 gl_FragColor;\n" +
                        "void main(){\n" +
                        "\tgl_FragColor = texture(uTextureSampler, vTextureCoord);\n" +
                        "}\n";


        public PushRender(int textureId) {
            this.mTextureId = textureId;
            vertexBuffer = GlUtil.createFloatBuffer(vertexCoord);
            fragmentBuffer = GlUtil.createFloatBuffer(fragmentCoord);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            mProgram = GlUtil.createProgram(vertexStr, fragStr);
            uTextureSamplerLocation = GLES30.glGetUniformLocation(mProgram, "uTextureSampler");
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES30.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES30.glUseProgram(mProgram);

            //绑定纹理
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);
            //绑定外部纹理到纹理单元0
            //GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
            //将此纹理单元床位片段着色器的uTextureSampler外部纹理采样器


            GLES30.glEnableVertexAttribArray(0);
            GLES30.glVertexAttribPointer(0, 2,
                    GLES30.GL_FLOAT, false, 2 * 4, vertexBuffer);

            GLES30.glEnableVertexAttribArray(1);
            GLES30.glVertexAttribPointer(1, 2,
                    GLES30.GL_FLOAT, false, 2 * 4, fragmentBuffer);

            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

            //解绑
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        }
    }


    private Handler uiHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {

            }
        }
    };
}
