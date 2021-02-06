package com.example.medialearn;

/**
 * Created by liuzhe
 * DateTime: 2021/1/11
 * Description:
 */
public class LivePush {

    static {
        System.loadLibrary("native");
    }

    private ConnectListener connectListener;

    public void setConnectListener(ConnectListener connectListener) {
        this.connectListener = connectListener;
    }

    public native void startPush(String url);

    public native void stopPush();

    public native void release();

    public native void setVideoSpsPps(byte[] spsData, int spsLen, byte[] ppsData, int ppsLen);

    public native void sendVideoPacket(byte[] data, int dataLen, boolean keyFrame);

    public native void sendAudioPacket(byte[] data, int len);


    private void nativeCall_ConnectSuccess() {
        if (connectListener != null) {
            connectListener.connectSuccess();
        }
    }

    private void nativeCall_ConnectError(String errorMsg) {
        stopPush();
        if (connectListener != null) {
            connectListener.connectError(errorMsg);
        }
    }

    public interface ConnectListener {
        void connectSuccess();
        void connectError(String errorMsg);
    }

}
