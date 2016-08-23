#version 120

attribute vec2 coord;

void main(void) {
    gl_Position = vec4(coord, 0, 1);
}
