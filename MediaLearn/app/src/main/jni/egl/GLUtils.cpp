//
// Created by zhe on 2020/12/27.
//

#include <malloc.h>
#include <AndroidLogUtil.h>
#include "GLUtils.h"


GLuint GLUtils::createProgram(const char *vertexShader, const char *fragShader) {
    // 加载顶点着色器
    GLuint vertex = loaderShader(GL_VERTEX_SHADER, vertexShader);
    if (vertex == 0) {
        return 0;
    }

    // 加载片元着色器
    GLuint fragment = loaderShader(GL_FRAGMENT_SHADER, fragShader);
    if (fragment == 0) {
        glDeleteShader(vertex);
        return 0;
    }

    // 创建program
    GLuint program = glCreateProgram();
    if (program == 0) {
        glDeleteShader(vertex);
        glDeleteProgram(fragment);
        return 0;
    }

    // 绑定shader
    glAttachShader(program, vertex);
    glAttachShader(program, fragment);

    //链接program程序
    glLinkProgram(program);


    glDetachShader(program, vertex);
    glDeleteShader(vertex);
    glDetachShader(program, fragment);
    glDeleteShader(fragment);

    //检查链接状态
    GLint linked;
    glGetProgramiv(program, GL_LINK_STATUS, &linked);
    if (!linked) {
        GLint infolen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infolen);
        if (infolen > 1) {
            char *infoLog = (char *) malloc(sizeof(char *) * infolen);
            glGetProgramInfoLog(program, infolen, NULL, infoLog);
            LOGE("Error linking program: \b%s\n", infoLog);
            free(infoLog);
        }
        glDeleteProgram(program);
        return 0;
    }

    LOGE("GLUtils createProgram success: %d", program);
    return program;
}

GLuint GLUtils::loaderShader(GLenum type, const char *shaderSrc) {
    //创建shader
    GLuint shader = glCreateShader(type);
    if (shader == 0) {
        return 0;
    }
    //加载shader源码
    glShaderSource(shader, 1, &shaderSrc, NULL);
    //编译shader
    glCompileShader(shader);

    //检查编译状态
    GLint compiled;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        GLint infolen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infolen);

        if (infolen > 1) {
            char *infoLog = (char *) malloc(sizeof(char *) * infolen);
            glGetShaderInfoLog(shader, infolen, NULL, infoLog);
            LOGE("Error compiling shader: \n%s\n", infoLog);
            free(infoLog);
        }
        glDeleteShader(shader);
        return 0;
    }

    LOGE("GLUtils loaderShader success: %d", shader);
    return shader;
}

GLuint GLUtils::createTexture(GLenum type) {
    GLuint textureId;
    //设置解包对齐
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    //创建纹理
    glGenTextures(1, &textureId);
    //绑定纹理
    glBindTexture(type, textureId);
    //设置放大缩小模式
    glTexParameterf(type, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(type, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameterf(type, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameterf(type, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    return textureId;
}

/*void GLUtils::checkGLError(const char *op) {
    for (GLint error = glGetError(); error; error = glGetError()) {

    }
}*/
