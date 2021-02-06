#version 300 es
layout(location = 0) in vec4 aPosition;
layout(location = 1) in vec2 aTextureCoord;
out vec2 vTextureCoord;

void main(){
    vTextureCoord = aTextureCoord;
    gl_Position = aPosition;
}