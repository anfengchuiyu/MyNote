//
// Created by zhe on 2020/12/27.
//

#include "EglSurfaceBase.h"
#include "../utils/AndroidLogUtil.h"
#include <GLES2/gl2.h>

EglSurfaceBase::EglSurfaceBase(EglCore *eglCore) : mEglCore(eglCore) {
    mEglSurface = EGL_NO_SURFACE;
}

void EglSurfaceBase::createWindowSurface(ANativeWindow *nativeWindow) {
    if (mEglSurface != EGL_NO_SURFACE) {
        LOGE("surface already created");
        return;
    }
    mEglSurface = mEglCore->createWindowSurface(nativeWindow);
}

void EglSurfaceBase::createOffscreen(int width, int height) {
    if (mEglSurface != EGL_NO_SURFACE) {
        LOGE("surface already created");
        return;
    }
    mEglSurface = mEglCore->createOffscreenSurface(width, height);
    mWidth = width;
    mHeight = height;
}

int EglSurfaceBase::getWidth() {
    if (mWidth < 0) {
        mWidth = mEglCore->querySurface(mEglSurface, EGL_WIDTH);
    }
    return mWidth;
}

int EglSurfaceBase::getHeight() {
    if (mHeight < 0) {
        mHeight = mEglCore->querySurface(mEglSurface, EGL_HEIGHT);
    }
    return mHeight;
}

void EglSurfaceBase::releaseEglSurface() {
    mEglCore->releaseSurface(mEglSurface);
    mEglSurface = EGL_NO_SURFACE;
    mWidth = -1;
    mHeight = -1;
}

void EglSurfaceBase::makeCurrent() {
    mEglCore->makeCurrent(mEglSurface);
}

bool EglSurfaceBase::swapBuffers() {
    bool result = mEglCore->swapBuffers(mEglSurface);
    if (!result) {
        LOGE("WARNING: swapBuffers() failed");
    }
    return result;
}

void EglSurfaceBase::setPresentationTime(long nsecs) {
    mEglCore->setPresentationTime(mEglSurface, nsecs);
}

char *EglSurfaceBase::getCurrentFrame() {
    char *pixels = NULL;
    glReadPixels(0, 0, getWidth(), getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, pixels);
    return pixels;
}







