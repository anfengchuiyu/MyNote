//
// Created by zhe on 2020/12/28.
//

#include "Triangle.h"
#include "GLUtils.h"

Triangle::Triangle() {

}

Triangle::~Triangle() {

}

int Triangle::init() {
    char vertexShaderSrc[] =
            "#version 300 es\n"
            "layout(location = 0) in vec4 a_position;\n"
            "layout(location = 1) in vec4 a_color;\n"
            "out vec4 v_color;"
            "void main() {\n"
            "   gl_Position = a_position;\n"
            "   v_color = a_color;\n"
            "}\n";
    char fragmentShaderSrc[] =
            "#version 300 es\n"
            "precision mediump float;\n"
            "in vec4 v_color;\n"
            "out vec4 fragColor;\n"
            "void main() {\n"
            "   fragColor = v_color;\n"
            "}\n";

    programHandle = GLUtils::createProgram(vertexShaderSrc, fragmentShaderSrc);
    if (programHandle <= 0) {
        return -1;
    }
    glClearColor(0.0f, 0.0f, 0.5f, 1.0f);
    return 0;
}

void Triangle::onDraw(int width, int height) {
    GLfloat vertices[] = {
            0.0f, 0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
    };
    GLfloat color[] = {
            1.0f, 0.0f, 0.0f, 1.0f
    };

    GLint vertexCount = sizeof(vertices) / (sizeof(vertices[0]) * 3);

    glViewport(0, 0, width, height);
    glClear(GL_COLOR_BUFFER_BIT);

    glUseProgram(programHandle);

    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 3 * sizeof(GLfloat), vertices);
    glEnableVertexAttribArray(0);
    glVertexAttrib4fv(1, color);

    glDrawArrays(GL_TRIANGLES, 0, vertexCount);

    glDisableVertexAttribArray(0);
}

void Triangle::destroy() {
    if (programHandle > 0) {
        glDeleteProgram(programHandle);
    }
    programHandle = -1;
}
