//
// Created by zhe on 2020/12/27.
//

#ifndef MY_APPLICATION_GLRENDER_H
#define MY_APPLICATION_GLRENDER_H

#include <android/native_window.h>
#include "EglCore.h"
#include "WindowSurface.h"
#include "Triangle.h"

class GLRender {
public:
    GLRender();

    virtual ~GLRender();

    void surfaceCreated(ANativeWindow *window);

    void surfaceChanged(int width, int height);

    void onDrawFrame();

    void surfaceDestroyed(void);

private:
    EglCore *mEglCore;
    WindowSurface *mWindowSurface;

    Triangle *triangle;
};


#endif //MY_APPLICATION_GLRENDER_H
