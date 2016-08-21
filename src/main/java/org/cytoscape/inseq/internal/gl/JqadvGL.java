package org.cytoscape.inseq.internal.gl;

import java.awt.Color;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.util.ParseUtil;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;

/**
 * This class handles all OpenGL rendering code.
 * 
 */
public class JqadvGL {
    
    private FloatBuffer vertices;
    private int verticesVBO;
    private int imageVBO;
    private int bkgrndVBO;
    private int selectionVBO;

    private int num_tiles;
    private BufferedImage image;


    // Shaders
    private ShaderState st;
    private ShaderProgram jqsp;
    private ShaderProgram imgsp;
    private ShaderProgram bgrndsp;
    private ShaderProgram simplesp;

    private float offset_x = 0;
    private float offset_y = 0;
    private float mouse_x = 0;
    private float mouse_y = 0;
    private float w;
    private float h;
    private float scale_master = 1;
    private float point_scale = 1f;

    private int nPoints;
    private boolean pc;

    private float[] coords;
    float[] img;
    float[] colours;
    float[] bkgrnd;
    float[] symbols;
    FloatBuffer selectionShape;

    GLUniformData uniColours;
    GLUniformData uniSymbols;

    private BufferedImage pointSprites;

    private Transcript selection;

    private float extrascale = 1f;

    private List<Transcript> transcripts;
    private InseqSession session;
    private GLCanvas canvas;

    final public UpdateEngine engine = new UpdateEngine();

    public void setPointScale(float value) {

        extrascale = value;
    }

    public void largePoints(boolean e) {
        if(e)
            point_scale = 2;
        else
            point_scale = 1;
    }

    public JqadvGL(InseqSession s, GLCanvas canvas) {
        this.session = s;
        this.transcripts = s.getRaw();
        this.canvas = canvas;
        
        // num = how many types of gene we have
        int num = s.getGenes().size();

        // vertices required to cover the background
        bkgrnd = new float[] {
            -1f,  1f,
             1f,  1f,
             1f, -1f,
             1f, -1f,
            -1f,  1f,
            -1f, -1f
        };

        // create array specifying what colour each type uses.
        colours = new float[num * 3];
        for(int i = 0; i < num; i++) {
            Color c = s.getGeneColour(i);
            int a = i*3;
            float[] f = new float[3];
            System.arraycopy(c.getRGBColorComponents(f), 0, colours, a, 3);
        }
        
        pointSprites = ParseUtil.getImageResource("/texture/sprite_sheet.png");
        // create the array specifying which symbol each type uses.
        symbols = new float[num];
        for(int i = 0; i < num; i++) {
            symbols[i] = s.getGeneSymbol(i);
        }

        // create array of our transcript locations
        coords = new float[transcripts.size() * 3];
        int i = 0;
        for(Transcript t : transcripts) {
            coords[i++] = (float) t.pos.x;
            coords[i++] = (float) t.pos.y;
            coords[i++] = t.type;
        }
        vertices = FloatBuffer.wrap(coords);
        nPoints = transcripts.size();
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
        Texture symbols_tex = Util.textureFromBufferedImage(pointSprites);
        gl2.glBindTexture(GL.GL_TEXTURE_2D, symbols_tex.getTextureObject());

        // Disable texture interpolation for point sprites.
        gl2.glTexParameteri(GL.GL_TEXTURE_2D,
                            GL.GL_TEXTURE_MIN_FILTER,
                            GL.GL_NEAREST);
        gl2.glTexParameteri(GL.GL_TEXTURE_2D,
                            GL.GL_TEXTURE_MAG_FILTER,
                            GL.GL_NEAREST);

        // Create four vertex buffer objects
        // 1. verticesVBO: contains the actual transcript points
        // 2. imageVBO: coordinates to stretch an image across
        // 3. bkgrndVBO: A rectangle that covers the entire screen
        // 4. selectionVBO: Coordinates of the current selection
        int buf[] = new int[4];
        gl2.glGenBuffers(4, buf, 0);
        verticesVBO = buf[0];
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, verticesVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER, 
                         nPoints * 3 * GLBuffers.SIZEOF_FLOAT,
                         vertices,
                         GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        imageVBO = buf[1];
        
        if(image != null) {
            //TODO: Maybe ask if you really want to reload the image
            //(it can take a while if it's huge)
            num_tiles = ImageTiler.makeBackgroundImage(gl2, imageVBO, image);
        }

        bkgrndVBO = buf[2];
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, bkgrndVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                6 * 2 * GLBuffers.SIZEOF_FLOAT,
                FloatBuffer.wrap(bkgrnd),
                GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        
        selectionVBO = buf[3];
        if(selectionShape != null) {

            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, selectionVBO);
            gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                             selectionShape.capacity() * GLBuffers.SIZEOF_FLOAT,
                             selectionShape,
                             GL.GL_STATIC_DRAW);
        }
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);


        Util.makeUniform(gl2, st, "offset_x", offset_x);
        Util.makeUniform(gl2, st, "offset_y", offset_y);
        Util.makeUniform(gl2, st, "mouse_x", mouse_x);
        Util.makeUniform(gl2, st, "mouse_y", mouse_y);
        Util.makeUniform(gl2, st, "sprite", 0);
        Util.makeUniform(gl2, st, "background", 1);
        Util.makeUniform(gl2, st, "ptsize", (float) pointSprites.getHeight());
        Util.makeUniform(gl2, st, "ptscale", point_scale);
        Util.makeUniform(gl2, st, "texnum", (float) pointSprites.getWidth() / pointSprites.getHeight());
        Util.makeUniform(gl2, st, "scale_master", scale_master);
        Util.makeUniform(gl2, st, "extrascale", extrascale);
        Util.makeUniform(gl2, st, "sel", 0f);
        Util.makeUniform(gl2, st, "width", 1f);
        Util.makeUniform(gl2, st, "height", 1f);
        Util.makeUniform(gl2, st, "closed", 0f);

        uniSymbols = new GLUniformData("symbols", 1, FloatBuffer.wrap(symbols));
        st.uniform(gl2, uniSymbols);
        uniColours = new GLUniformData("colours", 3, FloatBuffer.wrap(colours));
        st.uniform(gl2, uniColours);

    }
    
    /**
     * Create the shader program used to render our image and points.
     * Look under the resources folder to find the source code for these
     * shaders.
     */
    private void generateShaderProgram(GL2 gl2) {

        st = new ShaderState();

        imgsp = Util.compileProgram(gl2, "image");
        st.attachShaderProgram(gl2, imgsp, false);

        jqsp = Util.compileProgram(gl2, "jqadv");
        st.attachShaderProgram(gl2, jqsp, true);
        
        simplesp = Util.compileProgram(gl2, "simple");
        st.attachShaderProgram(gl2, simplesp, false);

        bgrndsp = Util.compileProgram(gl2, "bgrnd");
        st.attachShaderProgram(gl2, bgrndsp, true);
        
    }

    protected void setup(GL2 gl2, int width, int height) {
        
        // Update viewport size to canvas dimensions
        gl2.glViewport(0, 0, width, height);

        w = width;
        h = height;
        
        Util.updateUniform(gl2, st, "width", (float)width);
        Util.updateUniform(gl2, st, "height", (float)height);
    }

    protected void render(GL2 gl2, int width, int height) {

        engine.makeChanges(gl2);

        Util.updateUniform(gl2, st, "extrascale", extrascale);
        Util.updateUniform(gl2, st, "ptscale", point_scale);
        Util.updateUniform(gl2, st, "scale_master", scale_master);
        Util.updateUniform(gl2, st, "offset_x", offset_x);
        Util.updateUniform(gl2, st, "offset_y", offset_y);
        Util.updateUniform(gl2, st, "mouse_x", mouse_x);
        Util.updateUniform(gl2, st, "mouse_y", mouse_y);

        gl2.glClear(GL.GL_COLOR_BUFFER_BIT);

        st.attachShaderProgram(gl2, bgrndsp, true);
        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, bkgrndVBO);
        gl2.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
        gl2.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);

        st.attachShaderProgram(gl2, imgsp, true);
        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        for(int i = 0; i < num_tiles; i++) {
            Util.updateUniform(gl2, st, "background", i+1);
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
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glDisableClientState(GL2.GL_POINT_SPRITE);


        if(selectionShape != null) {
            
            st.attachShaderProgram(gl2, simplesp, true);
            gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
            
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, selectionVBO);
            gl2.glLineWidth(3f);
            gl2.glEnable(GL.GL_LINE_SMOOTH);
            gl2.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
            if(pc) {
                Util.updateUniform(gl2, st, "closed", 1f);
                gl2.glDrawArrays(GL.GL_LINE_LOOP, 0, selectionShape.capacity() / 2);
            } else {
                Util.updateUniform(gl2, st, "closed", 0f);
                gl2.glDrawArrays(GL.GL_LINE_STRIP, 0, selectionShape.capacity() / 2);
            }
            gl2.glLineWidth(1f);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

            gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        }

        if(selection != null) {
            
            st.attachShaderProgram(gl2, jqsp, true);
            gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
            gl2.glEnableClientState(GL2.GL_POINT_SPRITE);
            
            Util.updateUniform(gl2, st, "sel", 1f);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, verticesVBO);
            gl2.glVertexPointer(3, GL.GL_FLOAT, 0, selection.index * 3 * GLBuffers.SIZEOF_FLOAT);
            gl2.glDrawArrays(GL.GL_POINTS, 0, 1);
            Util.updateUniform(gl2, st, "sel", 0f);

            gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
            gl2.glDisableClientState(GL2.GL_POINT_SPRITE);
        }

    }

    /**
     * Marks the selected point.
     */
    public void selectTranscript(Transcript t) {
        selection = t;
    }

    public float getScale() {
        return scale_master;
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
        gl[0] = ((x * extrascale + offset_x) * scale_master + mouse_x) / w;
        gl[1] = ((y  * extrascale - offset_y) * scale_master - mouse_y) / h;
        return gl;
    }
    
    /**
     * Converts a gl point into graph coordinate space.
     * The inverse of graphToGL.
     */
    public float[] glToGraph(float x, float y) {
        float[] graph = new float[2];
        graph[0] = ((x * w - mouse_x) / scale_master - offset_x) / extrascale;
        graph[1] = ((y * h + mouse_y) / scale_master + offset_y) / extrascale;
        return graph;
    }

    /**
     * Queried during rendering for any changes that need to be uploaded to the
     * GPU.
     * Keeps multiple variable updating contained and orderly.
     */
    public class UpdateEngine {

        // An empty set of update type enums
        private EnumSet<UpdateType> updates = EnumSet.noneOf(UpdateType.class);

        public void changeNetworkComponents() {

            for(int i = 0; i < transcripts.size(); i++) {
                // set it to negative to make it not appear
                Transcript t = transcripts.get(i);
                if(session.isActive(t)) {
                    coords[i*3+2] = t.type;
                } else {
                    coords[i*3+2] = -t.type-1;
                }
            }
            
            updates.add(UpdateType.NETWORK_COMPONENT_SELECTED);
            canvas.display();
        }

        public void selectionChanged(boolean pathClosed) {

            if(session.getSelection() != null) {
                PathIterator pi = session.getSelection().getPathIterator(null);
                float[] segment = new float[6];
                ArrayList<Float> shape = new ArrayList<Float>();
                while(!pi.isDone()) {
                    pi.currentSegment(segment);
                    shape.add(segment[0]);
                    shape.add(segment[1]);
                    pi.next();
                }
                pc = pathClosed;
                selectionShape = FloatBuffer.allocate(shape.size());
                for(int a = 0; a < shape.size(); a++) {
                    selectionShape.put(a, shape.get(a));
                }
            } else {
                selectionShape = null;
            }

            updates.add(UpdateType.SELECTION_AREA_CHANGED);
            canvas.display();
        }

        public void changeImage(BufferedImage i) {
            image = i;
            updates.add(UpdateType.IMAGE_CHANGED);
            canvas.display();
        }

        public void changeSymbol(Integer type, Integer symbol) {
            symbols[type] = symbol;
            updates.add(UpdateType.SYMBOL_CHANGED);
            canvas.display();
        }

        public void changeColour(Integer type, Color c) {
            int a = type*3;
            float[] f = new float[3];
            System.arraycopy(
                    c.getRGBColorComponents(f), 0, colours, a, 3);
            updates.add(UpdateType.COLOUR_CHANGED);
            canvas.display();
        }

        /**
         * Allows changes that require access to the main OpenGL thread to
         * run later.
         */
        public void makeChanges(GL2 gl2) {
            
            for(Iterator<UpdateType> i = updates.iterator(); i.hasNext();) {
                UpdateType update = i.next();
                switch(update) {
                    case NETWORK_COMPONENT_SELECTED:
                        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, verticesVBO);
                        gl2.glBufferData(GL.GL_ARRAY_BUFFER, 
                                         nPoints * 3 * GLBuffers.SIZEOF_FLOAT,
                                         vertices,
                                         GL.GL_STATIC_DRAW);
                        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
                        break;
                    case SELECTION_AREA_CHANGED:
                        if(session.getSelection() != null) {
                            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, selectionVBO);
                            gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                                             selectionShape.capacity() * GLBuffers.SIZEOF_FLOAT,
                                             selectionShape,
                                             GL.GL_DYNAMIC_DRAW);
                        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
                        }
                        break;
                    case IMAGE_CHANGED:
                        num_tiles = ImageTiler.makeBackgroundImage(gl2, imageVBO, image);
                        break;
                    case COLOUR_CHANGED:
                        uniColours.setData(FloatBuffer.wrap(colours));
                        st.uniform(gl2, uniColours);
                        break;
                    case SYMBOL_CHANGED:
                        uniSymbols.setData(FloatBuffer.wrap(symbols));
                        st.uniform(gl2, uniSymbols);
                        break;
                }
                i.remove();
            }
        }
    }
    
    private enum UpdateType {
        NETWORK_COMPONENT_SELECTED,
        SELECTION_AREA_CHANGED,
        IMAGE_CHANGED,
        COLOUR_CHANGED,
        SYMBOL_CHANGED
    }
}
