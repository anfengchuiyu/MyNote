//
// Created by zhe on 2020/12/27.
//

#ifndef MY_APPLICATION_GLUTILS_H
#define MY_APPLICATION_GLUTILS_H

#include <GLES3/gl3.h>
#include <GLES3/gl3ext.h>

namespace GLUtils {

    //创建program
    GLuint createProgram(const char *vertexShader, const char *fragShader);

    //加载shader
    GLuint loaderShader(GLenum type, const char *shaderSrc);

    //创建texture
    GLuint createTexture(GLenum type);

    //检查是否出错
    void checkGLError(const char *op);

}


#endif //MY_APPLICATION_GLUTILS_H
