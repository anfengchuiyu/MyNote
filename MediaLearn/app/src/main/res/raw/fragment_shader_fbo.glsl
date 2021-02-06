#version 300 es
precision mediump float;

in vec2 vTextureCoord;
uniform sampler2D uTexture;
out vec4 outColor;

void main(){
    outColor = texture(uTexture, vTextureCoord);
}