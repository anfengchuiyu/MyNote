//
// Created by zhe on 2020/12/27.
//

#include "EglCore.h"
#include "../utils/AndroidLogUtil.h"

EglCore::EglCore() {
    init(NULL, 0);
}

EglCore::~EglCore() {
    release();
}

EglCore::EglCore(EGLContext sharedContext, int flags) {
    init(sharedContext, flags);
}

void EglCore::init(EGLContext sharedContext, int flags) {
    if (mEGLDisplay != EGL_NO_DISPLAY) {
        return;
    }

    if (sharedContext == NULL) {
        sharedContext = EGL_NO_CONTEXT;
    }

    mEGLDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (mEGLDisplay == EGL_NO_DISPLAY) {
        LOGE("unable to get EGL display");
        return;
    }

    EGLint elgMajorVersion, eglMinorVersion;
    if (!eglInitialize(mEGLDisplay, &elgMajorVersion, &eglMinorVersion)) {
        LOGE("unable to eglInitialize");
        return;
    }
    LOGE("eglInitialize: elgMajorVersion=%d, eglMinorVersion=%d", elgMajorVersion, eglMinorVersion);


    if ((flags & FLAG_TRY_GLES3) != 0) {
        EGLConfig config = getConfig(flags, 3);
        if (config != NULL) {
            int attrib3_list[] = {
                    EGL_CONTEXT_CLIENT_VERSION, 3,
                    EGL_NONE
            };
            EGLContext context = eglCreateContext(mEGLDisplay, config, sharedContext, attrib3_list);
            checkEglError("elgCreateContext");
            if (eglGetError() == EGL_SUCCESS) {
                mEGLConfig = config;
                mEGLContext = context;
                mGlVersion = 3;
            }
        }
    }

    if (mEGLContext == EGL_NO_CONTEXT) {
        EGLConfig config = getConfig(flags, 2);
        int attrib2_list[] = {
                EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL_NONE
        };
        EGLContext context = eglCreateContext(mEGLDisplay, config, sharedContext, attrib2_list);
        checkEglError("elgCreateContext");
        if (eglGetError() == EGL_SUCCESS) {
            mEGLConfig = config;
            mEGLContext = context;
            mGlVersion = 2;
        }
    }

    // 获取eglPresentationTimeANDROID方法的地址
    eglPresentationTimeANDROID = (EGL_PRESENTATION_TIME_ANDROIDPROC)
            eglGetProcAddress("eglPresentationTimeANDROID");
    if (!eglPresentationTimeANDROID) {
        LOGE("eglPresentationTimeANDROID is not available!");
    }

    EGLint value;
    eglQueryContext(mEGLDisplay, mEGLContext, EGL_CONTEXT_CLIENT_VERSION, &value);
    LOGE("EGLContext created, client version %d", value);
}

EGLConfig EglCore::getConfig(int flags, int version) {
    int renderableType = EGL_OPENGL_ES2_BIT;
    if (version >= 3) {
        renderableType = EGL_OPENGL_ES3_BIT_KHR;
    }

    int attribList[] = {
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            //EGL_DEPTH_SIZE, 16,
            //EGL_STENCIL_SIZE, 8,
            EGL_RENDERABLE_TYPE, renderableType,
            EGL_NONE, 0,      // placeholder for recordable [@-3]
            EGL_NONE
    };
    int length = sizeof(attribList) / sizeof(attribList[0]);
    if ((flags & FLAG_RECORDABLE) != 0) {
        attribList[length - 3] = EGL_RECORDABLE_ANDROID;
        attribList[length - 2] = 1;
    }
    EGLConfig configs = NULL;
    int numConfigs;
    if (!eglChooseConfig(mEGLDisplay, attribList, &configs, 1, &numConfigs)) {
        LOGE("unable to find RGB8888 / %d  EGLConfig", version);
        return NULL;
    }
    return configs;
}

void EglCore::checkEglError(const char *msg) {
    int error;
    if ((error = eglGetError()) != EGL_SUCCESS) {
        LOGE("%s: EGL error: %x", msg, error);
    }
}

void EglCore::release() {
    if (mEGLDisplay != EGL_NO_DISPLAY) {
        eglMakeCurrent(mEGLDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroyContext(mEGLDisplay, mEGLContext);
        eglReleaseThread();
        eglTerminate(mEGLDisplay);
    }
    mEGLDisplay = EGL_NO_DISPLAY;
    mEGLContext = EGL_NO_CONTEXT;
    mEGLConfig = NULL;
}

int EglCore::getGlVersion() {
    return mGlVersion;
}

EGLSurface EglCore::createWindowSurface(ANativeWindow *surface) {
    if (surface == NULL) {
        LOGE("ANativeWindow is NULL!");
        return NULL;
    }

    int surfaceAttribs[] = {
            EGL_NONE
    };
    EGLSurface eglSurface = eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface,
                                                   surfaceAttribs);
    checkEglError("eglCreateWindowSurface");
    if (eglSurface == NULL) {
        LOGE("EGLSurface is null!");
        return NULL;
    }

    return eglSurface;
}

EGLSurface EglCore::createOffscreenSurface(int width, int height) {
    int surfaceAttribs[] = {
            EGL_WIDTH, width,
            EGL_HEIGHT, height,
            EGL_NONE
    };
    EGLSurface eglSurface = eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, surfaceAttribs);
    checkEglError("eglCreatePbufferSurface");
    if (eglSurface == NULL) {
        LOGE("EGLSurface is null!");
        return NULL;
    }
    return eglSurface;
}

void EglCore::makeCurrent(EGLSurface eglSurface) {
    if (mEGLDisplay == EGL_NO_DISPLAY) {
        LOGE("makeCurrent EGL_NO_DISPLAY!");
        return;
    }
    if (!eglMakeCurrent(mEGLDisplay, eglSurface, eglSurface, mEGLContext)) {
        LOGE("eglMakeCurrent failed!");
    }
}

void EglCore::makeCurrent(EGLSurface drawSurface, EGLSurface readSurface) {
    if (mEGLDisplay == EGL_NO_DISPLAY) {
        LOGE("makeCurrent EGL_NO_DISPLAY!");
        return;
    }
    if (!eglMakeCurrent(mEGLDisplay, drawSurface, readSurface, mEGLContext)) {
        LOGE("eglMakeCurrent failed!");
    }
}

void EglCore::makeNothingCurrent() {
    if (!eglMakeCurrent(mEGLDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)) {
        LOGE("eglMakeCurrent failed!");
    }
}

bool EglCore::swapBuffers(EGLSurface eglSurface) {
    return eglSwapBuffers(mEGLDisplay, eglSurface);
}

void EglCore::releaseSurface(EGLSurface eglSurface) {
    eglDestroySurface(mEGLDisplay, eglSurface);
}

EGLContext EglCore::getEGLContext() {
    return mEGLContext;
}

bool EglCore::isCurrent(EGLSurface eglSurface) {
    return mEGLContext == eglGetCurrentContext() &&
           eglSurface == eglGetCurrentSurface(EGL_DRAW);
}

int EglCore::querySurface(EGLSurface eglSurface, int what) {
    int value;
    eglQuerySurface(mEGLDisplay, eglSurface, what, &value);
    return value;
}

const char *EglCore::queryString(int what) {
    return eglQueryString(mEGLDisplay, what);
}

void EglCore::setPresentationTime(EGLSurface eglSurface, long nsecs) {
    eglPresentationTimeANDROID(mEGLDisplay, eglSurface, nsecs);
}
