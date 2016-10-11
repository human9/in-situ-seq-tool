#version 120

attribute vec2 shape;

void main(void) {

    gl_Position = vec4(shape, 0, 1);
}
