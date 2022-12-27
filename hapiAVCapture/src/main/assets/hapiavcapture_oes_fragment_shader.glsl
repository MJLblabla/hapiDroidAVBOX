#version 300 es
//OpenGL ES3.0外部纹理扩展
#extension GL_OES_EGL_image_external_essl3: require
precision mediump float;
//precision highp float;
uniform samplerExternalOES yuvTexSampler;
in vec2 yuvTexCoords; //纹理坐标
out vec4 vFragColor;

void main() {
    vFragColor = texture(yuvTexSampler, yuvTexCoords);
}