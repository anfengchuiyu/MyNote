package com.example.medialearn.rtmp_push;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import com.example.medialearn.R;
import com.example.medialearn.gles.GlUtil;
import com.example.medialearn.gles.RawUtil;

import java.nio.FloatBuffer;

/**
 * Created by liuzhe
 * DateTime: 2021/1/30
 * Description: 离屏渲染：把所有的纹理先绘制到 fbo 上面，然后再从 fbo 绘制到窗口上
 */
public class FboRender {


    private float[] vertexCoords = new float[]{
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f
    };

    //android屏幕，纹理坐标，原点在左上角
    private float[] fragmentCoords = new float[]{
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };

    private FloatBuffer vertexBuffer;
    private FloatBuffer fragBuffer;
    private int mProgram;
    private int uTextureLocation;
    private int mTextureId;

    private Context context;
    private int mVboId;
    private int mFboId;


    public FboRender(Context context) {
        this.context = context;
        vertexBuffer = GlUtil.createFloatBuffer(vertexCoords);
        fragBuffer = GlUtil.createFloatBuffer(fragmentCoords);
    }


    public void onSurfaceCreated(int width, int height) {
        mProgram = GlUtil.createProgram(RawUtil.getFromRaw(context, R.raw.vertex_shader_fbo),
                RawUtil.getFromRaw(context, R.raw.fragment_shader_fbo));
        uTextureLocation = GLES30.glGetUniformLocation(mProgram, "uTexture");

        //创建VBO
        int[] vbos = new int[1];
        GLES30.glGenBuffers(1, vbos, 0);
        //绑定VBO
        mVboId = vbos[0];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVboId);
        //开辟VBO
        int vboBufferSize = (vertexCoords.length + fragmentCoords.length) * 4;
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vboBufferSize, null, GLES30.GL_STATIC_DRAW);
        //赋值
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, vertexCoords.length * 4, vertexBuffer);
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, vertexCoords.length * 4,
                fragmentCoords.length * 4, fragBuffer);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);


        //创建并纹理
        int[] textureIds = new int[1];
        GLES30.glGenTextures(1, textureIds, 0);
        mTextureId = textureIds[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glUniform1i(uTextureLocation, 0);

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        //创建fbo并把纹理绑定到fbo
        int[] fbos = new int[1];
        GLES30.glGenBuffers(1, fbos, 0);
        mFboId = fbos[0];
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFboId);

        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
                width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D, mTextureId, 0);
        if (GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("FboRender", "fbo bind failure");
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }


    public void onSurfaceChanged(int width, int height) {
        GLES30.glViewport(0, 0, width, height);
    }


    public void onDrawFrame() {
        GLES30.glUseProgram(mProgram);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVboId);

        /**
         * 设置坐标
         * 2：2个为一个点
         * GLES20.GL_FLOAT：float 类型
         * false：不做归一化
         * 8：步长是 8
         */
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 8, vertexCoords.length * 4);

        GLES30.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        //解绑
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    public void onBindFbo() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFboId);
    }

    public void onUnBindFbo() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }


    public int getTextureId() {
        return mTextureId;
    }
}
