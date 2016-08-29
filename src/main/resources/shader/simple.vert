#version 120

attribute vec2 shape;
uniform mat4 P;
uniform mat4 Mv;

uniform float closed;
varying float c;

void main(void) {
    c = closed;
    gl_Position = P * Mv * vec4(shape, 0, 1);
}
