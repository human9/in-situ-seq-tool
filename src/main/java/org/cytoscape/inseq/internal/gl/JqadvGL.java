package org.cytoscape.inseq.internal.gl;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.List;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.typenetwork.Transcript;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;

public class JqadvGL {
    
    private FloatBuffer vertices;
    private int verticesVBO;

    private Texture image;
    private Texture pointSprites;

    private int glProgram;

    private float offset_x = 0;
    private float offset_y = 0;
    private float mouse_x = 0;
    private float mouse_y = 0;
    private float w;
    private float h;

    private float scale_master = 1;

    private int nPoints;

    private float[] values;

    private ShaderState st;

    GLUniformData uni_width;
    GLUniformData uni_height;
    GLUniformData uni_scale_master;
    GLUniformData uni_offset_x;
    GLUniformData uni_offset_y;
    GLUniformData uni_mouse_x;
    GLUniformData uni_mouse_y;

    float[] colours;
    FloatBuffer colours_buffer;
    GLUniformData uni_colours;
    
    float[] symbols;
    FloatBuffer symbols_buffer;
    GLUniformData uni_symbols;
    GLUniformData ptsize;
    GLUniformData sprite;

    public JqadvGL(InseqSession s, List<Transcript> list) {
        
        // num = how many types of gene we have
        int num = s.getGenes().size();

        // create array specifying what colour each type uses.
        colours = new float[num * 3];
        for(int i = 0; i < num; i++) {
            Color c = s.getGeneColour(i);
            int a = i*3;
            float[] f = new float[3];
            System.arraycopy(c.getRGBColorComponents(f), 0, colours, a, 3);
        }
        colours_buffer = FloatBuffer.wrap(colours);
        
        // create the array specifying which symbol each type uses.
        symbols = new float[num];
        for(int i = 0; i < num; i++) {
            symbols[i] = i % 2;
        }
        symbols_buffer = FloatBuffer.wrap(symbols);

        values = new float[list.size() * 3];
        int i = 0;
        for(Transcript t : list) {
            values[i++] = (float) t.pos.x;
            values[i++] = (float) t.pos.y;
            values[i++] = t.type;
        }
        nPoints = list.size();

    }

    protected void init(GL2 gl2) {

        gl2.glEnable(GL.GL_BLEND);
        gl2.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl2.glEnable(GL2.GL_POINT_SPRITE);
        gl2.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
        
        gl2.glActiveTexture(GL.GL_TEXTURE0);
        pointSprites = Util.textureFromResource("/texture/sprite_sheet.png");
        pointSprites.bind(gl2);
        gl2.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl2.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);

        gl2.glActiveTexture(GL.GL_TEXTURE0 + 1);
        image = Util.textureFromResource("/texture/background.jpg");
        image.bind(gl2);

        vertices = FloatBuffer.wrap(values);

        st = new ShaderState();
        final ShaderCode vp = 
            ShaderCode.create(gl2,
                    GL2.GL_VERTEX_SHADER, this.getClass(),
                    "shader", null, "jqadv", false);
        final ShaderCode fp = 
            ShaderCode.create(gl2,
                    GL2.GL_FRAGMENT_SHADER, this.getClass(),
                    "shader", null, "jqadv", false);
        ShaderProgram sp = new ShaderProgram();
        sp.add(gl2, vp, System.err);
        sp.add(gl2, fp, System.err);
        st.attachShaderProgram(gl2, sp, true);

        int buf[] = new int[1];
        gl2.glGenBuffers(1, buf, 0);
        verticesVBO = buf[0];
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, verticesVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER, 
                         nPoints * 3 * GLBuffers.SIZEOF_FLOAT,
                         vertices,
                         GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        glProgram = sp.program();

        gl2.glUseProgram(glProgram);

        uni_offset_x = new GLUniformData("offset_x", offset_x);
        st.ownUniform(uni_offset_x);
        st.uniform(gl2, uni_offset_x);
        uni_offset_y = new GLUniformData("offset_y", offset_y);
        st.ownUniform(uni_offset_y);
        st.uniform(gl2, uni_offset_y);
        
        sprite = new GLUniformData("sprite", 0);
        st.ownUniform(sprite);
        st.uniform(gl2, sprite);

        uni_mouse_x = new GLUniformData("mouse_x", mouse_x);
        st.ownUniform(uni_mouse_x);
        st.uniform(gl2, uni_mouse_x);
        uni_mouse_y = new GLUniformData("mouse_y", mouse_y);
        st.ownUniform(uni_mouse_y);
        st.uniform(gl2, uni_mouse_y);
        ptsize = new GLUniformData("ptsize", (float) pointSprites.getHeight());
        st.ownUniform(ptsize);
        st.uniform(gl2, ptsize);
        GLUniformData texnum = new GLUniformData("texnum", (float) pointSprites.getHeight() / pointSprites.getWidth());
        st.ownUniform(texnum);
        st.uniform(gl2, texnum);

        uni_scale_master = new GLUniformData("scale_master", scale_master);
        st.ownUniform(uni_scale_master);
        st.uniform(gl2, uni_scale_master);

        uni_symbols = new GLUniformData("symbols", 1, symbols_buffer);
        st.ownUniform(uni_symbols);
        st.uniform(gl2, uni_symbols);
        uni_colours = new GLUniformData("colours", 3, colours_buffer);
        st.ownUniform(uni_colours);
        st.uniform(gl2, uni_colours);

    }

    protected void setup(GL2 gl2, int width, int height) {
        // coordinate system origin at lower left with width and height same as the window
        gl2.glViewport(0, 0, width, height);

        w = width;
        h = height;
        
        uni_width = new GLUniformData("width", (float)width);
        st.ownUniform(uni_width);
        st.uniform(gl2, uni_width);
        
        uni_height = new GLUniformData("height", (float)height);
        st.ownUniform(uni_height);
        st.uniform(gl2, uni_height);
    }

    protected void render(GL2 gl2, int width, int height) {
        uni_scale_master.setData(scale_master);
        st.uniform(gl2, uni_scale_master);
        uni_offset_x.setData(offset_x);
        st.uniform(gl2, uni_offset_x);
        uni_offset_y.setData(offset_y);
        st.uniform(gl2, uni_offset_y);
        uni_mouse_x.setData(mouse_x);
        st.uniform(gl2, uni_mouse_x);
        uni_mouse_y.setData(mouse_y);
        st.uniform(gl2, uni_mouse_y);

        gl2.glClear(GL.GL_COLOR_BUFFER_BIT);

        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glEnableClientState(GL2.GL_POINT_SPRITE);

        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, verticesVBO);
        gl2.glVertexPointer(3, GL.GL_FLOAT, 0, 0);

        gl2.glDrawArrays(GL.GL_POINTS, 0, nPoints);

        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glDisableClientState(GL2.GL_POINT_SPRITE);

    }

    /**
     * Adjusts the master scale.
     * Returns false if unchaged, true otherwise.
     */
    public boolean scale(int direction, float x, float y) {
        
        // If mouse has moved since last scale, we need to
        // adjust for this (to allow zooming from mouse position).
        if(mouse_x != x || mouse_y != -y) {

            offset_x += (mouse_x - x) / scale_master;
            offset_y += (mouse_y + y) / scale_master;

            mouse_x = x;
            mouse_y = -y;
        }

        if(direction < 0) {
            if(scale_master < 100f) {
                scale_master *= 1.1f;
                return true;
            }
        } else {
            if(scale_master > 0.01f) {
                scale_master /= 1.1f;
                return true;
            }
        }
        return false; 
    }

    /**
     * Pans the image.
     */
    public void move(float x, float y) {
        offset_x -= (x / scale_master);
        offset_y += (y / scale_master);
    }

    /**
     * Converts a graph point into gl coordinate space.
     * Use this to find where a graph point will be rendered on the screen.
     */
    public float[] graphToGL(float x, float y) {
        float[] gl = new float[2];
        gl[0] = ((x + offset_x) * scale_master + mouse_x) / w;
        gl[1] = ((y - offset_y) * scale_master + mouse_y) / h;
        return gl;
    }
    
    /**
     * Converts a gl point into graph coordinate space.
     * The inverse of graphToGL.
     */
    public float[] glToGraph(float x, float y) {
        float[] graph = new float[2];
        graph[0] = (x * w - mouse_x) / scale_master - offset_x;
        graph[1] = (y * h + mouse_y) / scale_master + offset_y;
        return graph;
    }
}
