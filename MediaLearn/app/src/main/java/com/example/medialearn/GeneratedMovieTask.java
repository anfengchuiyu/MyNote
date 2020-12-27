package com.example.medialearn;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import com.example.medialearn.elg.EglCore;
import com.example.medialearn.elg.WindowSurface;

import java.nio.ByteBuffer;

/**
 * Created by liuzhe
 * DateTime: 2020/11/15
 * Description:
 */
public class GeneratedMovieTask implements Runnable {

    private static final String TAG = "GeneratedMovieTask";
    private static final int IFRAME_INTERVAL = 5;

    private MediaCodec mEncoder;
    private MediaMuxer mMuxer;
    private EglCore mEglCore;
    private WindowSurface mInputSurface;

    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex;


    private String mimeType = "video/avc";
    private int width = 480;
    private int height = 640;
    private int bitrate = 5000000;
    private int framesPerSecond = 30;
    private String outputFilePath = "/sdcard/A/movie_learn_generate.mp4";


    public static void execute(){
        new Thread(new GeneratedMovieTask()).start();
    }


    @Override
    public void run() {
        try {
            mBufferInfo = new MediaCodec.BufferInfo();

            MediaFormat format = MediaFormat.createVideoFormat(mimeType, width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, framesPerSecond);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

            Log.d(TAG, "format: " + format);

            mEncoder = MediaCodec.createEncoderByType(mimeType);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Log.d(TAG, "encoder is: " + mEncoder.getCodecInfo().getName());

            Surface surface = mEncoder.createInputSurface();

            //EGL环境创建
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);
            //创建渲染表面: 创建EglSurface并和surface关联
            mInputSurface = new WindowSurface(mEglCore, surface, true);
            //将当前线程与上下文进行绑定
            mInputSurface.makeCurrent();


            mEncoder.start();

            mMuxer = new MediaMuxer(outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            mTrackIndex = -1;

            Log.d(TAG, "generateFrame total: " + 240);
            for (int i = 0; i < 240; i++) {
                // Drain any data from the encoder into the muxer.
                drainEncoder(false);

                generateFrame(i);

                mInputSurface.setPresentationTime(computePresentationTimeNsec(i));
                mInputSurface.swapBuffers();

                Log.d(TAG, "generateFrame index: " + i);
            }
            drainEncoder(true);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            releaseEncoder();
        }
    }


    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;

        if (endOfStream) {
            mEncoder.signalEndOfInputStream();
        }

        while (true) {
            //INFO_TRY_AGAIN_LATER,
            //        INFO_OUTPUT_FORMAT_CHANGED,
            //        INFO_OUTPUT_BUFFERS_CHANGED,
            int index = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            Log.d(TAG, "dequeueOutputBuffer index: " + index);
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.d(TAG, "dequeueOutputBuffer index: INFO_TRY_AGAIN_LATER");
                break;
            } else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d(TAG, "dequeueOutputBuffer index: INFO_OUTPUT_FORMAT_CHANGED");
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
            } else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                Log.d(TAG, "dequeueOutputBuffer index: INFO_OUTPUT_BUFFERS_CHANGED");
            } else {
                ByteBuffer outputBuffer = mEncoder.getOutputBuffer(index);


                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    outputBuffer.position(mBufferInfo.offset);
                    outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

                    mMuxer.writeSampleData(mTrackIndex, outputBuffer, mBufferInfo);
                }


                mEncoder.releaseOutputBuffer(index, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.d(TAG, "reached end of stream unexpectedly");
                    } else {
                        Log.d(TAG, "end of stream reached");
                    }
                    break;
                }
            }

        }
    }

    private void releaseEncoder() {
        Log.d(TAG, "releasing encoder objects");
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
    }


    /**
     * Generates a frame of data using GL commands.
     */
    private void generateFrame(int frameIndex) {
        final int BOX_SIZE = 80;
        frameIndex %= 240;
        int xpos, ypos;

        int absIndex = Math.abs(frameIndex - 120);
        xpos = absIndex * width / 120;
        ypos = absIndex * height / 120;

        float lumaf = absIndex / 120.0f;

        GLES20.glClearColor(lumaf, lumaf, lumaf, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(BOX_SIZE / 2, ypos, BOX_SIZE, BOX_SIZE);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glScissor(xpos, BOX_SIZE / 2, BOX_SIZE, BOX_SIZE);
        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    /**
     * Generates the presentation time for frame N, in nanoseconds.  Fixed frame rate.
     */
    private long computePresentationTimeNsec(int frameIndex) {
        final long ONE_BILLION = 1000000000;
        return frameIndex * ONE_BILLION / framesPerSecond;
    }

}
