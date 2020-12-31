//
// Created by zhe on 2020/12/27.
//

#include <AndroidLogUtil.h>
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
    LOGD("GLRender: surfaceCreated");
    if (mEglCore == NULL) {
        mEglCore = new EglCore(NULL, FLAG_TRY_GLES3);
    }
    mWindowSurface = new WindowSurface(mEglCore, window, false);
    mWindowSurface->makeCurrent();

    triangle = new Triangle();
    triangle->init();
}

void GLRender::surfaceChanged(int width, int height) {
    LOGD("GLRender: surfaceChanged, width=%d, height=%d", width, height);
    mWindowSurface->makeCurrent();

    //draw
    triangle->onDraw(width, height);

    mWindowSurface->swapBuffers();
}


void GLRender::onDrawFrame() {

}

void GLRender::surfaceDestroyed(void) {
    LOGD("GLRender: surfaceDestroyed");
    if (triangle) {
        triangle->destroy();
        delete triangle;
        triangle = NULL;
    }
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



