#version 120

attribute vec4 tex_coords;
uniform mat4 P;
uniform mat4 Mv;
uniform float extrascale;

varying vec2 coord;

void main(void) {
    gl_Position = P * Mv * vec4(tex_coords.x * extrascale, tex_coords.y * extrascale, 0, 1);
    coord = vec2(tex_coords.z, tex_coords.w);
}
