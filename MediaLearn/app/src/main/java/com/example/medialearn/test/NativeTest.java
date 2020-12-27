package com.example.medialearn.test;

import android.view.Surface;

/**
 * Created by liuzhe
 * DateTime: 2020/12/25
 * Description:
 */
public class NativeTest {

    static {
        System.loadLibrary("native");
    }

    public native static String sayHello();



    public native static void native_eglinit();
    public native static void native_surfaceCreated(Surface surface);

    public native static void native_surfaceChanged(int widht, int height);

    public native static void native_surfaceDestroyed();

}
