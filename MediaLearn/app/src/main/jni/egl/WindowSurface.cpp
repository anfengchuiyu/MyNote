//
// Created by zhe on 2020/12/27.
//

#include <AndroidLogUtil.h>
#include "WindowSurface.h"

WindowSurface::WindowSurface(EglCore *eglCore, ANativeWindow *window, bool releaseSurface)
        : EglSurfaceBase(eglCore) {
    mWindow = window;
    createWindowSurface(mWindow);
    mReleaseSurface = releaseSurface;
}

WindowSurface::WindowSurface(EglCore *eglCore, ANativeWindow *window)
        : EglSurfaceBase(eglCore) {
    mWindow = window;
    createWindowSurface(mWindow);
}

void WindowSurface::release() {
    releaseEglSurface();
    if (mWindow != NULL) {
        ANativeWindow_release(mWindow);
        mWindow = NULL;
    }
}

void WindowSurface::recreate(EglCore *eglCore) {
    if (mWindow == NULL) {
        LOGE("not yet implemented ANativeWindow");
        return;
    }
    mEglCore = eglCore;
    createWindowSurface(mWindow);
}
