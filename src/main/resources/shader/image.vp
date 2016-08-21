#version 120

attribute vec4 tex_coords;
varying vec2 coord;

uniform float offset_x;
uniform float offset_y;
uniform float mouse_x;
uniform float mouse_y;
uniform float width;
uniform float height;
uniform float scale_master;

void main(void) {
    gl_Position = vec4(((tex_coords.x + offset_x) * scale_master + mouse_x) / width,
					((-tex_coords.y + offset_y) * scale_master + mouse_y) / height, 0, 1);
    coord = vec2(tex_coords.z, tex_coords.w);
}
