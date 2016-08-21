#version 120

uniform sampler2D sprite;
varying vec4 f_color;
varying float offset;
varying float size;
varying float num;
varying float x;
varying float d;

void main(void) {

    if(d == 1.0) {
        discard;
    }
    vec2 TextureCoord = vec2(offset/num, 0);
    vec2 TextureSize = vec2(1/num, 1);
    vec2 real = TextureCoord + (gl_PointCoord * TextureSize);

    if(x == 0)
        gl_FragColor = texture2D(sprite, real) * f_color;
    else {
        vec2 circ = 2.0 * gl_PointCoord - 1.0;
        if(dot(circ, circ) > 1.0) {
            discard;
        }
        gl_FragColor = vec4(f_color.x, f_color.y, f_color.z, dot(circ, circ)+0.1);
    }

}
