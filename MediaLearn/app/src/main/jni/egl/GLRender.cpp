//
// Created by zhe on 2020/12/27.
//

#include "GLRender.h"

GLRender::GLRender() {
    mEglCore = NULL;
    mWindowSurface = NULL;
}

GLRender::~GLRender() {
    if (mEglCore) {
        mEglCore->release();
        delete mEglCore;
        mEglCore = NULL;
    }
}

void GLRender::surfaceCreated(ANativeWindow *window) {
    if (mEglCore == NULL) {
        mEglCore = new EglCore(NULL, FLAG_RECORDABLE);
    }
    mWindowSurface = new WindowSurface(mEglCore, window, false);
    mWindowSurface->makeCurrent();
}

void GLRender::surfaceChanged(int width, int height) {
    mWindowSurface->makeCurrent();

    //draw

    mWindowSurface->swapBuffers();
}

void GLRender::surfaceDestroyed(void) {
    if (mWindowSurface) {
        mWindowSurface->release();
        delete mWindowSurface;
        mWindowSurface = NULL;
    }
    if (mEglCore) {
        mEglCore->release();
        delete mEglCore;
        mEglCore = NULL;
    }
}


