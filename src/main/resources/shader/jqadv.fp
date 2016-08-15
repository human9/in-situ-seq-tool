#version 120

uniform sampler2D sprite;
varying vec4 f_color;
varying float offset;
varying float size;

void main(void) {
    float numtextures = 2;
    vec2 TextureCoord = vec2(offset/numtextures, 0);
    vec2 TextureSize = vec2(0.5, 1);
    vec2 real = TextureCoord + (gl_PointCoord * TextureSize);

    gl_FragColor = texture2D(sprite, real) * f_color;
}
