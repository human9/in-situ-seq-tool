#version 120

attribute vec2 coord;
uniform mat4 P;
uniform mat4 Mv;

void main(void) {
    gl_Position = P * Mv * vec4(coord, 0, 1);
}
