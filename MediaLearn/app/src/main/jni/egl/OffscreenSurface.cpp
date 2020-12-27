//
// Created by zhe on 2020/12/27.
//

#include "OffscreenSurface.h"


OffscreenSurface::OffscreenSurface(EglCore *eglCore1, EglCore *eglCore, int width, int height)
        : EglSurfaceBase(eglCore1) {
    createOffscreen(width, height);
}

void OffscreenSurface::release() {
    releaseEglSurface();
}
