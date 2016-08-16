package org.cytoscape.inseq.internal.gl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.typenetwork.Transcript;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

/**
 * This class handles all OpenGL rendering code.
 * 
 */
public class JqadvGL {
    
    private FloatBuffer vertices;
    private int verticesVBO;
    private int imageVBO;

    private int MAX_TEXTURE_SIZE;
    private int MAX_TEXTURE_UNITS;

    private Texture image;
    private int num_tiles;
    private Texture pointSprites;
    private ShaderProgram jqsp;
    private ShaderProgram imgsp;

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

    float[] img;
    FloatBuffer img_buffer;

    float[] colours;
    FloatBuffer colours_buffer;
    GLUniformData uni_colours;
    
    float[] symbols;
    FloatBuffer symbols_buffer;
    GLUniformData uni_symbols;
    GLUniformData ptsize;
    GLUniformData sprite;
    GLUniformData background;

    private BufferedImage bufferedImage;
    private boolean imageChanged;

    public void setImage(BufferedImage image) {
        this.bufferedImage = image;
        imageChanged = true;
    }

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
            symbols[i] = i % 4;
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

    /**
     * Detect how large textures can be, and how many we can have.
     * If our image is larger than MAX_TEXTURE_SIZE, we will need to break
     * it into no more than (MAX_TEXTURE_UNITS - 1) pieces (as one unit is 
     * to be used for point sprites).
     */
    private void detectHardwareLimits(GL2 gl2) {
        int[] tex_size = new int[1];
        int[] tex_num = new int[1];
        gl2.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, IntBuffer.wrap(tex_size));
        gl2.glGetIntegerv(GL2.GL_MAX_TEXTURE_IMAGE_UNITS, IntBuffer.wrap(tex_num));
        MAX_TEXTURE_SIZE = tex_size[0];
        MAX_TEXTURE_UNITS = tex_num[0];
    }

    /**
     * Attempts to create an image that will back the points.
     * If it will not fit within a single texture, it is split into smaller
     * fragments. If the image is ridiculously huge and this fails, the image
     * will be scaled down until it fits.
     */
    private void makeBackgroundImage(GL2 gl2) {

        int w = bufferedImage.getWidth();
        int h = bufferedImage.getHeight();
        
        // Detect texture limits.
        detectHardwareLimits(gl2);

        Dimension req = Util.getRequiredTiles(MAX_TEXTURE_SIZE, MAX_TEXTURE_UNITS, w, h); 
        img = Util.getVertices(req, w, h);

        // make and bind subimage tiles
        int tilew = w / req.width;
        int tileh = h / req.height;
        System.out.println(w +"," + h);
        for(int i = 0; i < req.width*req.height && i < MAX_TEXTURE_UNITS; i++) {

            int vl1 = (int) ((i%req.width) * tilew);
            int vu1 = (int) (((i/req.width)%req.height) * tileh);
            System.out.println(vl1 +"," + vu1);
            gl2.glActiveTexture(GL.GL_TEXTURE0 + i+1);
            BufferedImage sub = new BufferedImage(tilew, tileh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) sub.getGraphics();
            g.drawImage(bufferedImage, 0, 0, tilew, tileh, vl1, vu1, vl1+tilew, vu1+tileh, null);

            Texture tile = AWTTextureIO.newTexture(GLProfile.getDefault(), sub, true);
            gl2.glBindTexture(GL.GL_TEXTURE_2D, tile.getTextureObject());
        }

        img_buffer = FloatBuffer.wrap(img);
        num_tiles = img.length / 24;
        System.out.println("Rendering image as " + num_tiles + " tile(s)");

        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, imageVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                         img.length * GLBuffers.SIZEOF_FLOAT,
                         img_buffer,
                         GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        
    }

    protected void init(GL2 gl2) {

        // Enable specific OpenGL capabilities.
        gl2.glEnable(GL.GL_BLEND);
        gl2.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl2.glEnable(GL2.GL_POINT_SPRITE);
        gl2.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);

        // Create shaders and initialize the program.
        generateShaderProgram(gl2);
        
        // Retrieve and bind the point sprites. These are the symbols that
        // appear on each transcript location.
        gl2.glActiveTexture(GL.GL_TEXTURE0);
        pointSprites = Util.textureFromResource("/texture/sprite_sheet.png");
        gl2.glBindTexture(GL.GL_TEXTURE_2D, pointSprites.getTextureObject());

        // Disable texture interpolation for point sprites.
        gl2.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl2.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);


        vertices = FloatBuffer.wrap(values);


        int buf[] = new int[2];
        gl2.glGenBuffers(2, buf, 0);
        verticesVBO = buf[0];
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, verticesVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER, 
                         nPoints * 3 * GLBuffers.SIZEOF_FLOAT,
                         vertices,
                         GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        imageVBO = buf[1];
        if(bufferedImage != null) {
            makeBackgroundImage(gl2);
        }
        uni_offset_x = new GLUniformData("offset_x", offset_x);
        st.ownUniform(uni_offset_x);
        st.uniform(gl2, uni_offset_x);
        uni_offset_y = new GLUniformData("offset_y", offset_y);
        st.ownUniform(uni_offset_y);
        st.uniform(gl2, uni_offset_y);
        
        sprite = new GLUniformData("sprite", 0);
        st.ownUniform(sprite);
        st.uniform(gl2, sprite);
        
        background = new GLUniformData("background", 1);
        st.ownUniform(background);
        st.uniform(gl2, background);

        uni_mouse_x = new GLUniformData("mouse_x", mouse_x);
        st.ownUniform(uni_mouse_x);
        st.uniform(gl2, uni_mouse_x);
        uni_mouse_y = new GLUniformData("mouse_y", mouse_y);
        st.ownUniform(uni_mouse_y);
        st.uniform(gl2, uni_mouse_y);
        ptsize = new GLUniformData("ptsize", (float) pointSprites.getHeight());
        st.ownUniform(ptsize);
        st.uniform(gl2, ptsize);
        GLUniformData texnum = new GLUniformData("texnum", (float) pointSprites.getWidth() / pointSprites.getHeight());
        System.out.println(texnum.floatValue());
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
    
    /**
     * Create the shader program used to render our image and points.
     * Look under the resources folder to find the source code for these
     * shaders.
     */
    private void generateShaderProgram(GL2 gl2) {
        st = new ShaderState();

        final ShaderCode imgvp = 
            ShaderCode.create(gl2,
                    GL2.GL_VERTEX_SHADER, this.getClass(),
                    "shader", null, "image", false);
        final ShaderCode imgfp = 
            ShaderCode.create(gl2,
                    GL2.GL_FRAGMENT_SHADER, this.getClass(),
                    "shader", null, "image", false);
        imgsp = new ShaderProgram();
        imgsp.add(gl2, imgvp, System.err);
        imgsp.add(gl2, imgfp, System.err);
        st.attachShaderProgram(gl2, imgsp, false);
        
        final ShaderCode jqvp = 
            ShaderCode.create(gl2,
                    GL2.GL_VERTEX_SHADER, this.getClass(),
                    "shader", null, "jqadv", false);
        final ShaderCode jqfp = 
            ShaderCode.create(gl2,
                    GL2.GL_FRAGMENT_SHADER, this.getClass(),
                    "shader", null, "jqadv", false);
        jqsp = new ShaderProgram();
        jqsp.add(gl2, jqvp, System.err);
        jqsp.add(gl2, jqfp, System.err);
        st.attachShaderProgram(gl2, jqsp, true);
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

        if(imageChanged) {
            makeBackgroundImage(gl2);
            imageChanged = false;
        }
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

        st.attachShaderProgram(gl2, imgsp, true);
        
        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        for(int i = 0; i < num_tiles; i++) {
            background.setData(i + 1);
            st.uniform(gl2, background);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, imageVBO);
            gl2.glVertexPointer(4, GL.GL_FLOAT, 0, i*24*GLBuffers.SIZEOF_FLOAT);
            gl2.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        }
        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);

        st.attachShaderProgram(gl2, jqsp, true);

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
