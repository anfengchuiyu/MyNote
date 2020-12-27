//
// Created by zhe on 2020/12/27.
//

#ifndef MY_APPLICATION_OFFSCREENSURFACE_H
#define MY_APPLICATION_OFFSCREENSURFACE_H


#include "EglSurfaceBase.h"

class OffscreenSurface : EglSurfaceBase{
public:
    OffscreenSurface(EglCore *eglCore1, EglCore *eglCore, int width, int height);
    void release();

};


#endif //MY_APPLICATION_OFFSCREENSURFACE_H
