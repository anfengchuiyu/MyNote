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
        glDeleteShader(vertex);
        glDeleteProgram(fragment);
        glDeleteProgram(program);
        return 0;
    }

    glDeleteShader(vertex);
    glDeleteProgram(fragment);

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

    }

    return 0;
}
