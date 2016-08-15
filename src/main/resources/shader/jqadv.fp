#version 120

uniform sampler2D sprite;
varying vec4 f_color;
varying float offset;
varying float size;
varying float num;

void main(void) {
    vec2 TextureCoord = vec2(offset/num, 0);
    vec2 TextureSize = vec2(1/num, 1);
    vec2 real = TextureCoord + (gl_PointCoord * TextureSize);

    gl_FragColor = texture2D(sprite, real) * f_color;
}
