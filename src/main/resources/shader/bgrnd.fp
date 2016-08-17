/*
 * Draws a pretty checkerboard.
 */
void main(void) {
    vec2 pos = mod(gl_FragCoord.xy,vec2(20));
    if ((pos.x > 10.0)&&(pos.y > 10.0)){
        gl_FragColor=vec4(0.3, 0.3, 0.3, 0.3);
    }
    if ((pos.x < 10.0)&&(pos.y < 10.0)){
        gl_FragColor=vec4(0.3, 0.3, 0.3, 0.3);
    }
    if ((pos.x < 10.0)&&(pos.y > 10.0)){
        gl_FragColor=vec4(0.0, 0.0, 0.0, 0.3);
    }
    if ((pos.x > 10.0)&&(pos.y < 10.0)){
        gl_FragColor=vec4(0.0, 0.0, 0.0, 0.3);
    }
}
