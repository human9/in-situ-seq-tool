#version 120

uniform sampler2D background;
varying vec2 coord;

void main(void) {
    gl_FragColor = texture2D(background, coord);
}
