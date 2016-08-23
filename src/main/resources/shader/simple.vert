#version 120

attribute vec2 shape;

uniform float offset_x;
uniform float offset_y;
uniform float mouse_x;
uniform float mouse_y;
uniform float width;
uniform float height;
uniform float scale_master;
uniform float extrascale;
uniform float closed;
varying float c;

void main(void) {
    c = closed;
    gl_Position = vec4(((shape.x * extrascale + offset_x) * scale_master + mouse_x) / width,
			          ((-shape.y * extrascale + offset_y) * scale_master + mouse_y) / height, 0, 1);
}
