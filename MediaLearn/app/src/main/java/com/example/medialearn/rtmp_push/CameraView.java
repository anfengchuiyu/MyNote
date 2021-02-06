package com.example.medialearn.rtmp_push;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.example.medialearn.camera_help.CameraUtils;
import com.example.medialearn.gles.GlUtil;

import java.io.IOException;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by liuzhe
 * DateTime: 2021/1/23
 * Description:
 */
public class CameraView extends GLSurfaceView {

    private Camera mCamera;
    private boolean mIsFocusing;

    private int previewWidht;
    private int preViewHeight;

    private CameraRender cameraRender;
    private FboRender fboRender;

    /**
     * EGL环境上下文
     */
    protected EGLContext mEglContext;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(3);

        cameraRender = new CameraRender();
        fboRender = new FboRender(context);
        setRenderer(cameraRender);
    }

    /**
     * 通过此方法可以获取 EGL环境上下文，可用于共享渲染同一个纹理
     *
     * @return EGLContext
     */
    public EGLContext getEglContext() {
        return mEglContext;

    }

    public int getTextureId() {
        return fboRender.getTextureId();
    }

    public void open() {
        open(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    public void open(int cameraId) {
        if (cameraRender != null && cameraRender.getCameraSurfaceTexture() != null) {
            close();
            mCamera = Camera.open(cameraId);


            Camera.Parameters parms = mCamera.getParameters();
            CameraUtils.choosePreviewSize(parms, 1920, 1080);

            previewWidht = parms.getPreviewSize().width;
            preViewHeight = parms.getPreviewSize().height;

            post(new Runnable() {
                @Override
                public void run() {
                    getLayoutParams().height = (int) (getWidth() * (previewWidht * 1.0f / preViewHeight));
                    setLayoutParams(getLayoutParams());
                }
            });


            parms.setRecordingHint(true);
            mCamera.setParameters(parms);

            mCamera.setDisplayOrientation(90);

            SurfaceTexture surfaceTexture = cameraRender.getCameraSurfaceTexture();
            try {
                mCamera.setPreviewTexture(surfaceTexture);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void requestCameraFocus() {
        if (mCamera != null) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {

                }
            });
        }
    }


    private class CameraRender implements GLSurfaceView.Renderer {

        /**
         * 顶点坐标
         */
        private float[] mVertexCoordinate = new float[]{
                -1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, -1.0f,
                1.0f, 1.0f,
        };
        private FloatBuffer mVertexBuffer;

        /**
         * 纹理坐标
         */
        /*private float[] mFragmentCoordinate = new float[]{
                1.0f, 1.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f
        };*/


        private float[] mFragmentCoordinate = new float[]{
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f
        };


        private FloatBuffer mFragmentBuffer;

        private String vertexStr
                =
                "#version 300 es\n" +
                        "layout(location = 0) in vec4 aPosition;\n" +
                        "layout(location = 1) in vec4 aTextureCoord;\n" +
                        "uniform mat4 uTextureMatrix;\n" +
                        "out vec2 vTextureCoord;\n" +
                        "void main(){\n" +
                        "\tvTextureCoord = (uTextureMatrix * aTextureCoord).xy;\n" +
                        "\t//vTextureCoord = aTextureCoord;\n" +
                        "\tgl_Position = aPosition;\n" +
                        "}";

        private String fragStr
                =
                "#version 300 es\n" +
                        "#extension GL_OES_EGL_image_external_essl3 : require\n" +
                        "precision mediump float;\n" +
                        "\n" +
                        "uniform samplerExternalOES uTextureSampler;\n" +
                        "in vec2 vTextureCoord;\n" +
                        "out vec4 gl_FragColor;\n" +
                        "void main(){\n" +
                        "\tvec4 vCameraColor = texture(uTextureSampler, vTextureCoord);\n" +
                        "\tgl_FragColor = vCameraColor;\n" +
                        "}";


        private int mProgram;
        //private int aPositionLocation;
        //private int aTextureCoordLocation;
        private int uTextureMatrixLocation;
        private int uTextureSamplerLocation;
        private SurfaceTexture mCameraSt;

        private float[] mMVPMatrix = new float[16];

        private int cameraTextureId;

        public CameraRender() {
            mVertexBuffer = GlUtil.createFloatBuffer(mVertexCoordinate);
            mFragmentBuffer = GlUtil.createFloatBuffer(mFragmentCoordinate);
        }

        public SurfaceTexture getCameraSurfaceTexture() {
            return mCameraSt;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            fboRender.onSurfaceCreated(getWidth(), getHeight());

            mEglContext = EGL14.eglGetCurrentContext();

            mProgram = GlUtil.createProgram(vertexStr, fragStr);

            // 获取坐标
            //aPositionLocation = GLES30.glGetAttribLocation(mProgram, "aPosition");
            //aTextureCoordLocation = GLES30.glGetAttribLocation(mProgram, "aTextureCoord");
            uTextureMatrixLocation = GLES30.glGetUniformLocation(mProgram, "uTextureMatrix");
            uTextureSamplerLocation = GLES30.glGetUniformLocation(mProgram, "uTextureSampler");


            cameraTextureId = createEOSTexture();
            mCameraSt = new SurfaceTexture(cameraTextureId);
            mCameraSt.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    requestRender();
                }
            });

            open();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            fboRender.onSurfaceChanged(width, height);
            GLES30.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            fboRender.onBindFbo();
            //设置背景颜色
            GLES30.glClearColor(1.0f, 0.f, 0.f, 1.0f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
            //使用程序片段
            GLES30.glUseProgram(mProgram);

            //更新纹理图像
            mCameraSt.updateTexImage();
            //通过 SurfaceTexture 获取变换矩阵
            mCameraSt.getTransformMatrix(mMVPMatrix);

            //激活纹理单元0
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            //绑定外部纹理到纹理单元0
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
            //将此纹理单元床位片段着色器的uTextureSampler外部纹理采样器
            GLES30.glUniform1i(uTextureSamplerLocation, 0);


            //将纹理矩阵传给片段着色器
            GLES30.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, mMVPMatrix, 0);

            //传入顶点坐标
            GLES30.glEnableVertexAttribArray(0);
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, mVertexBuffer);

            //传入纹理坐标
            GLES30.glEnableVertexAttribArray(1);
            GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 8, mFragmentBuffer);

            // 绘制
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

            fboRender.onUnBindFbo();
            fboRender.onDrawFrame();
        }

        //创建外部纹理
        private int createEOSTexture() {
            int[] tex = new int[1];
            GLES30.glGenTextures(1, tex, 0);
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, tex[0]);
            // 设置纹理环绕方式
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
            // 设置纹理过滤方式
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_BINDING_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

            //解除纹理绑定
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

            return tex[0];
        }


    }

}
