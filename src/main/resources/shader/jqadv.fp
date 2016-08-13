#version 120

uniform sampler2D texture;
varying vec4 f_color;

void main(void) {
    gl_FragColor = texture2D(texture, gl_PointCoord) * f_color;
}
