//
// Created by zhe on 2020/12/28.
//

#ifndef MY_APPLICATION_TRIANGLE_H
#define MY_APPLICATION_TRIANGLE_H


#include <GLES3/gl3.h>

class Triangle {
public:
    Triangle();
    ~Triangle();
    int init();
    void onDraw(int width, int height);
    void destroy();

private:
    GLuint programHandle;
};


#endif //MY_APPLICATION_TRIANGLE_H
