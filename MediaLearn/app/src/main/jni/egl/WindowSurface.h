//
// Created by zhe on 2020/12/27.
//

#ifndef MY_APPLICATION_WINDOWSURFACE_H
#define MY_APPLICATION_WINDOWSURFACE_H


#include "EglSurfaceBase.h"
#include "../../../../../../../work/Android/SDK/ndk/21.0.6113669/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/include/android/native_window.h"

class WindowSurface : public EglSurfaceBase {
public:
    WindowSurface(EglCore *eglCore, ANativeWindow *window, bool releaseSurface);

    WindowSurface(EglCore *eglCore, ANativeWindow *window);

    void release();

    void recreate(EglCore *eglCore);

private:
    ANativeWindow *mWindow;
    bool mReleaseSurface;
};


#endif //MY_APPLICATION_WINDOWSURFACE_H
