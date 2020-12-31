//
// Created by zhe on 2020/12/25.
//

#include <jni.h>
#include <GLRender.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

GLRender *glRender = new GLRender();
//ANativeWindow *window = NULL;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_medialearn_test_NativeTest_sayHello(JNIEnv *env, jclass clazz) {
    jstring jstr = env->NewStringUTF("hello, this is from native.");
    return jstr;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_medialearn_test_NativeTest_native_1eglinit(JNIEnv *env, jclass clazz) {
    glRender = new GLRender();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_medialearn_test_NativeTest_native_1surfaceCreated(JNIEnv *env, jclass clazz,
                                                                   jobject surface) {

    /*if (window) {
        ANativeWindow_release(window);
        window = NULL;
    }*/
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    glRender->surfaceCreated(window);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_medialearn_test_NativeTest_native_1surfaceChanged(JNIEnv *env, jclass clazz,
                                                                   jint widht, jint height) {
    glRender->surfaceChanged(widht, height);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_medialearn_test_NativeTest_native_1surfaceDestroyed(JNIEnv *env, jclass clazz) {
    glRender->surfaceDestroyed();
    if (glRender) {
        delete glRender;
        glRender = NULL;
    }
}