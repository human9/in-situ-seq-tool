#version 120

attribute vec3 coord2d;
uniform mat4 P;
uniform mat4 Mv;
varying vec4 f_color;
uniform float ptsize;
uniform vec3[128] colours;
uniform float[128] symbols;
uniform float texnum;
uniform float sel;
uniform float ptscale;
uniform float extrascale;
varying float offset;
varying float size;
varying float num;
varying float x;

void main(void) {
  if(coord2d.z >= 0.0) {
      x = sel;
      size = ptsize;
      num = texnum;
      offset = symbols[int(coord2d.z)]; 
      gl_Position = P * Mv * vec4(coord2d.x, coord2d.y, 0, 1); 

      f_color = vec4(colours[int(coord2d.z)], 1);
      gl_PointSize = ptscale * ptsize * (1.0 + sel * 1.6);
    }
    else {
		gl_Position = vec4(-2, -2, 0, 1);
		gl_PointSize = 0;
    }
}

