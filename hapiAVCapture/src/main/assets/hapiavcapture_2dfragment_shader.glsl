#version 300 es
precision highp float;
uniform int u_nImgType;// 1:RGBA, 2:NV21, 3:NV12, 4:I420 ,5oes,6 2d
uniform sampler2D s_texture0;
uniform sampler2D s_texture1;
uniform sampler2D s_texture2;

in vec2 yuvTexCoords; //纹理坐标
out vec4 vFragColor;

void main() {

    if (u_nImgType == 2) {
        vec3 yuv;
        yuv.x = texture(s_texture0, yuvTexCoords).r;
        yuv.y = texture(s_texture1, yuvTexCoords).a - 0.5;
        yuv.z = texture(s_texture1, yuvTexCoords).r - 0.5;
        highp vec3 rgb = mat3(1.0, 1.0, 1.0,
                              0.0, -0.344, 1.770,
                              1.403, -0.714, 0.0) * yuv;
        vFragColor = vec4(rgb, 1.0);
    } else if (u_nImgType == 3) {
        vec3 yuv;
        yuv.x = texture(s_texture0, yuvTexCoords).r;
        yuv.y = texture(s_texture1, yuvTexCoords).r - 0.5;
        yuv.z = texture(s_texture1, yuvTexCoords).a - 0.5;
        highp vec3 rgb = mat3(1.0, 1.0, 1.0,
                              0.0, -0.344, 1.770,
                              1.403, -0.714, 0.0) * yuv;
        vFragColor = vec4(rgb, 1.0);
    } else if (u_nImgType == 4) {
        vec3 yuv;
        yuv.x = texture(s_texture0, yuvTexCoords).r;
        yuv.y = texture(s_texture1, yuvTexCoords).r - 0.5;
        yuv.z = texture(s_texture2, yuvTexCoords).r - 0.5;
        highp vec3 rgb = mat3(1.0, 1.0, 1.0,
                              0.0, -0.344, 1.770,
                              1.403, -0.714, 0.0) * yuv;
        vFragColor = vec4(rgb, 1.0);
    } else {
        vFragColor = texture(s_texture0, yuvTexCoords);
    }
}