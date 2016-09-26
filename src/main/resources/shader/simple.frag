#version 120

varying float c;

void main(void) {
    gl_FragColor = vec4(1-c, c, 0, 1);
}
