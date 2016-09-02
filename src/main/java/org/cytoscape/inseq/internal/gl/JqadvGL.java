package org.cytoscape.inseq.internal.gl;

import java.awt.Color;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.typenetwork.Transcript;
import org.cytoscape.inseq.internal.util.ParseUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.util.Animator;
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

    float xOffset = 0;
    float yOffset = 0;

    // Shaders
    private ShaderState st;
    private ShaderProgram jqsp;
    private ShaderProgram imgsp;
    private ShaderProgram bgrndsp;
    private ShaderProgram simplesp;
    private ShaderProgram boxsp;
    private ShaderProgram matsp;

    private float w;
    private float h;
    private float scale = 1;
    private boolean makeCenter = true;
    private int numTiles = 0;

    private int nPoints;

    protected boolean pc;
    protected float[] coords;
    protected float[] colours;
    protected float[] bkgrnd;
    protected float[] symbols;
    protected FloatBuffer selectionShape;
    protected int capacity;

    private GLUniformData uniColours;
    private GLUniformData uniSymbols;

    private Transcript selectedTranscript;

    private List<Transcript> transcripts;
    private InseqSession session;

    private ImageTiler imageTiler;

    TextRenderer textRenderer;

    // Projection matrix
    Matrix4f PMatrix = new Matrix4f();
    FloatBuffer PBuffer = Buffers.newDirectFloatBuffer(16);
    GLUniformData P;

    // Modelview matrix
    Matrix4f MvMatrix = new Matrix4f();
    FloatBuffer MvBuffer = Buffers.newDirectFloatBuffer(16);
    GLUniformData Mv;

    // The inverse modelview matrix
    // Has no uniform or buffer as it isn't used in any shaders
    Matrix4f MviMatrix = new Matrix4f();

    // An empty set of update type enums
    protected EnumSet<UpdateType> updates = EnumSet.noneOf(UpdateType.class);
    // Events can build up here before being applied
    protected ArrayDeque<float[]> eventFiFo = new ArrayDeque<float[]>();
    protected Animator core;
    private boolean initDone;
    private float point_scale = 1f;
    private float extrascale = 1f;
    private BufferedImage image;

    public JqadvGL(InseqSession s, GLAutoDrawable drawable) {
        this.session = s;
        this.transcripts = s.getRaw();
        this.textRenderer = new TextRenderer(session);


        // vertices required to cover the background
        bkgrnd = Util.makeQuad(-1, -1, 1, 1);

        // create array specifying what colour each type uses.
        int num = session.getNumGenes();
        colours = new float[num * 3];
        for(int i = 0; i < num; i++) {
            Color c = s.getGeneColour(i);
            int a = i*3;
            float[] f = new float[3];
            System.arraycopy(c.getRGBColorComponents(f), 0, colours, a, 3);
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

        core = new Animator(drawable);

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

        BufferedImage pointSprites = ParseUtil.getImageResource("/texture/sprite_sheet.png");
        // create the array specifying which symbol each type uses.
        int num = session.getNumGenes();
        symbols = new float[num];
        for(int i = 0; i < num; i++) {
            symbols[i] = session.getGeneSymbol(i);
        }

        gl2.glActiveTexture(GL.GL_TEXTURE0 + 1);
        Texture pointSpritesTexture = Util.textureFromBufferedImage(pointSprites);
        gl2.glBindTexture(GL.GL_TEXTURE_2D, pointSpritesTexture.getTextureObject());
        // Disable texture interpolation for point sprites.
        gl2.glTexParameteri(GL.GL_TEXTURE_2D,
                            GL.GL_TEXTURE_MIN_FILTER,
                            GL.GL_NEAREST);
        gl2.glTexParameteri(GL.GL_TEXTURE_2D,
                            GL.GL_TEXTURE_MAG_FILTER,
                            GL.GL_NEAREST);

        gl2.glActiveTexture(GL.GL_TEXTURE0);


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
        
        if(imageTiler != null) {
            //TODO: Maybe ask if you really want to reload the image
            //(it can take a while if it's huge)
            imageTiler.bindVertices(gl2, imageVBO);
            numTiles = imageTiler.bindTexture(gl2);
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

        Util.makeUniform(gl2, st, "background", 2);
        Util.makeUniform(gl2, st, "ptsize", (float) pointSprites.getHeight());
        Util.makeUniform(gl2, st, "ptscale", 1f);
        Util.makeUniform(gl2, st, "texnum", (float) pointSprites.getWidth() / pointSprites.getHeight());
        Util.makeUniform(gl2, st, "extrascale", 1f);
        Util.makeUniform(gl2, st, "sel", 0f);
        Util.makeUniform(gl2, st, "closed", 0f);

        P = new GLUniformData("P", 4, 4, PBuffer);
        st.uniform(gl2, P);
        Mv = new GLUniformData("Mv", 4, 4, MvBuffer);
        st.uniform(gl2, Mv);

        GLUniformData sprite = new GLUniformData("sprite", 1);
        st.uniform(gl2, sprite);
        uniSymbols = new GLUniformData("symbols", 1, FloatBuffer.wrap(symbols));
        st.uniform(gl2, uniSymbols);
        uniColours = new GLUniformData("colours", 3, FloatBuffer.wrap(colours));
        st.uniform(gl2, uniColours);

        gl2.glActiveTexture(GL.GL_TEXTURE0);

        textRenderer.initRender(gl2);

        core.start();
        core.pause();

        initDone = true;

    }

    /**
     * Add a translation event into the queue.
     */
    public void move(float x, float y) {
        eventFiFo.addLast(new float[] {x,y});
        core.resume();
    }

    /**
     * Add a scaling event into the queue.
     */
    public void updateScale(float direction, float x, float y) {
        eventFiFo.addLast(new float[] {direction,x,y});
        core.resume();
    }

    /**
     * Set a new scale and signal that it should be updated.
     */
    public void setImageScale(float value) {
        extrascale = value;
        updates.add(UpdateType.IMAGE_SCALE_CHANGED);
        core.resume();
    }

    /**
     * Indicate whether initialisation is complete.
     */
    public void setInitDone(boolean d) {
        initDone = d;
    }

    public void largePoints(boolean e) {
        if(e)
            point_scale = 2;
        else
            point_scale = 1;

        updates.add(UpdateType.POINT_SCALE_CHANGED);
        core.resume();
    }

    public void changeNetworkComponents() {

        List<Transcript> transcripts = session.getRaw();

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
        core.resume();
    }

    public void selectionChanged(boolean pathClosed) {

        if(initDone) {
            if(session.getSelection() != null) {
                PathIterator pi = session.getSelection().getPathIterator(null);
                float[] segment = new float[6];
                ArrayList<Float> shape = new ArrayList<Float>();
                // For stencil buffer triangle drawing
                shape.add(0f);
                shape.add(0f);
                while(!pi.isDone()) {
                    pi.currentSegment(segment);
                    shape.add(segment[0]);
                    shape.add(segment[1]);
                    pi.next();
                }
                pc = pathClosed;
                // add origin at end
                shape.add(shape.get(2));
                shape.add(shape.get(3));

                selectionShape = FloatBuffer.allocate(shape.size());
                for(int a = 0; a < shape.size(); a++) {
                    selectionShape.put(a, shape.get(a));
                }
            } else {
                selectionShape = null;
            }

            updates.add(UpdateType.SELECTION_AREA_CHANGED);
            core.resume();
        }
    }

    public void changeImage(BufferedImage i) {
        image = i;
        updates.add(UpdateType.IMAGE_CHANGED);
        core.resume();
    }

    public void changeSymbol(Integer type, Integer symbol) {
        symbols[type] = symbol;
        updates.add(UpdateType.SYMBOL_CHANGED);
        core.resume();
    }

    public void changeColour(Integer type, Color c) {
        int a = type*3;
        float[] f = new float[3];
        System.arraycopy(
                c.getRGBColorComponents(f), 0, colours, a, 3);
        updates.add(UpdateType.COLOUR_CHANGED);
        core.resume();
    }

    /**
     * Marks the selected point.
     */
    public void selectTranscript(Transcript t) {
        selectedTranscript = t;
        core.resume();
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

        boxsp = Util.compileProgram(gl2, "box");
        st.attachShaderProgram(gl2, boxsp, false);
        
        matsp = Util.compileProgram(gl2, "mat");
        st.attachShaderProgram(gl2, matsp, false);

        bgrndsp = Util.compileProgram(gl2, "bgrnd");
        st.attachShaderProgram(gl2, bgrndsp, true);
        
    }

    /**
     * Center and scale the view so that as much of the data as possible is visible.
     */
    public void centerView() {
        
        xOffset = -(w/2 - session.min.width  / 2);
        yOffset = -(h/2 - session.min.height / 2);
        scale = 1;

        float wsc = w / session.min.width;
        float hsc = h / session.min.height;
        float target = Math.min(wsc, hsc);

        while(scale < target) {
            if(!scale(-1, w/2, h/2)) {
                // reached scale limit
                break;
            }
        }
        while(scale > target) {
            if(!scale(1, w/2, h/2)) {
                break;
            }
        }
    }

    protected void setup(GL2 gl2, int width, int height) {
        
        // Update viewport size to canvas dimensions
        gl2.glViewport(0, 0, width, height);

        // Keep view centered
        xOffset += (w-width)/2;
        yOffset += (h-height)/2;

        w = width;
        h = height;
        
        // Center the view
        if (makeCenter) {
            centerView(); 
            makeCenter = false;
        }

        textRenderer.reshapeRegion(width, height);

        PMatrix.identity()
               .ortho(0, w, h, 0, -1000, 1000)
               .get(PBuffer);
        P.setData(PBuffer);
        st.uniform(gl2, P);
        
    }

    /**
     * Convert a viewport pixel to a graph coordinate.
     */
    public float[] pixelToGraph(float[] p) {
        //float xZ = (float)( (p[1] + yOffset) * Math.tan(xRotate) );
        //float yZ = (float)( (p[0] + xOffset) * Math.tan(yRotate) );
        float z = 0;//xZ + yZ;
        Vector3f v = new Vector3f(p[0], p[1], z);

        MviMatrix.transformPosition(v);
        return new float[] {v.x, v.y};
        //return new float[] {p[0], p[1]/(float)Math.cos(angle)};
    }

    /**
     * Convert graph coordinate to viewport pixel.
     */
    public float[] graphToPixel(float[] p) {
        Vector3f v = new Vector3f(p[0], p[1], 0);
        MvMatrix.transformPosition(v);
        return new float[] {v.x, v.y};
    }

    /*
    int z = 0;
    float xRotate = (float) Math.toRadians(0);
    float yRotate = (float) Math.toRadians(0);
    */

    private void constructMatrices(GL2 gl2) {

        //xRotate = (float) Math.toRadians(z++);
        MvMatrix.identity()
                .translate(-xOffset, -yOffset, 0f)
                //.rotateX(xRotate)
                //.rotateY(yRotate)
                .scale(scale, scale, 0f)
                .get(MvBuffer);
        
          MviMatrix.identity()
                .scale(1/scale, 1/scale, 0f)
                //.rotateY(-yRotate)
                //.rotateX(-xRotate)
                .translate(xOffset, yOffset, 0f);

    }

    private void drawBackground(GL2 gl2) {
        st.attachShaderProgram(gl2, bgrndsp, true);
        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, bkgrndVBO);
        gl2.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
        gl2.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    }

    private void drawImage(GL2 gl2) {
        st.attachShaderProgram(gl2, imgsp, true);
        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        for(int i = 0; i < numTiles; i++) {
            Util.updateUniform(gl2, st, "background", i+2);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, imageVBO);
            gl2.glVertexPointer(4, GL.GL_FLOAT, 0, i*24*GLBuffers.SIZEOF_FLOAT);
            gl2.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        }
        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
    }

    private void drawPoints(GL2 gl2) {
        st.attachShaderProgram(gl2, jqsp, true);

        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glEnableClientState(GL2.GL_POINT_SPRITE);

        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, verticesVBO);

        gl2.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
        gl2.glDrawArrays(GL.GL_POINTS, 0, nPoints);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glDisableClientState(GL2.GL_POINT_SPRITE);

    }

    private void drawSelectionLine(GL2 gl2) {

        st.attachShaderProgram(gl2, simplesp, true);
        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, selectionVBO);
        gl2.glLineWidth(3f);
        gl2.glEnable(GL.GL_LINE_SMOOTH);
        gl2.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
        if(pc) {
            Util.updateUniform(gl2, st, "closed", 1f);
            gl2.glDrawArrays(GL.GL_LINE_STRIP, 1, capacity / 2 - 1);
        } else {
            Util.updateUniform(gl2, st, "closed", 0f);
            gl2.glDrawArrays(GL.GL_LINE_STRIP, 1, capacity / 2 - 2);
        }
        gl2.glLineWidth(1f);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);

    }

    private void drawSelectionMask(GL2 gl2) {

        gl2.glEnable(GL.GL_STENCIL_TEST);

        st.attachShaderProgram(gl2, simplesp, true);
        //fill polygon w/ stencil buffer
        gl2.glColorMask(false, false, false, false);
        gl2.glStencilFunc(GL.GL_ALWAYS, 1, 0);
        gl2.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_INVERT);
        gl2.glStencilMask(1);

        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, selectionVBO);
        gl2.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
        gl2.glDrawArrays(GL.GL_TRIANGLE_FAN, 0, capacity / 2);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        
        st.attachShaderProgram(gl2, boxsp, true);

        gl2.glColorMask(true, true, true, true);
        gl2.glStencilFunc(GL.GL_EQUAL, 0, 1);
        gl2.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);

        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, bkgrndVBO);
        gl2.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
        gl2.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        
        gl2.glDisable(GL.GL_STENCIL_TEST);

    }

    private void drawSelectedPointBubble(GL2 gl2) {
        st.attachShaderProgram(gl2, jqsp, true);
        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glEnableClientState(GL2.GL_POINT_SPRITE);
        
        Util.updateUniform(gl2, st, "sel", 1f);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, verticesVBO);
        gl2.glVertexPointer(3, GL.GL_FLOAT, 0, selectedTranscript.index * 3 * GLBuffers.SIZEOF_FLOAT);
        gl2.glDrawArrays(GL.GL_POINTS, 0, 1);
        Util.updateUniform(gl2, st, "sel", 0f);

        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glDisableClientState(GL2.GL_POINT_SPRITE);
    }


    protected void render(GL2 gl2, int width, int height) {
    

        gl2.glEnable(GL.GL_BLEND);
        gl2.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl2.glEnable(GL2.GL_POINT_SPRITE);
        gl2.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);

        fetchUpdates(gl2); 
        constructMatrices(gl2);

        gl2.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);

        drawBackground(gl2);
        drawImage(gl2);
        drawPoints(gl2);

        if(selectionShape != null) {
            
            drawSelectionLine(gl2); 

            // capacity will be at least 10 if a valid shape is drawn
            if(capacity > 9) {
            
                drawSelectionMask(gl2);
            }
        }
        
        if(selectedTranscript != null) {
            drawSelectedPointBubble(gl2);    
        }
        
        textRenderer.renderText(gl2, scale, selectedTranscript);
    }

    public float getScale() {
        return scale;
    }

    /**
     * Adjusts the master scale.
     * Returns false if unchaged, true otherwise.
     */
    public boolean scale(int direction, float x, float y) {
        
        if(direction < 0) {
            if(scale < 100f) {
                xOffset -= (x+xOffset) - (x+xOffset)*1.1; 
                yOffset -= (y+yOffset) - (y+yOffset)*1.1; 
                scale *= 1.1f;
                return true;
            }
        } else {
            if(scale > 0.005f) {
                xOffset -= (x+xOffset) - (x+xOffset)/1.1; 
                yOffset -= (y+yOffset) - (y+yOffset)/1.1; 
                scale /= 1.1f;
                return true;
            }
        }
        return false; 
    }

    private enum UpdateType {
        NETWORK_COMPONENT_SELECTED,
        SELECTION_AREA_CHANGED,
        IMAGE_CHANGED,
        COLOUR_CHANGED,
        SYMBOL_CHANGED,
        POINT_SCALE_CHANGED,
        IMAGE_SCALE_CHANGED
    } 

    public void fetchUpdates(GL2 gl2) {
        
        float[] e;
        int size = (int) Math.floor(eventFiFo.size() * 0.5);
        while(eventFiFo.size() > size) {
            e = eventFiFo.removeFirst();
            switch(e.length) {
                default:
                    System.err.println("Invalid event: This should never happen");
                    break;
                case 2:
                    xOffset += e[0];
                    yOffset += e[1];
                    break;
                case 3:
                    scale((int)e[0], e[1], e[2]);
                    break;
            }

        }
        if(eventFiFo.isEmpty()) core.pause();

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
                    if(selectionShape != null) {
                        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, selectionVBO);
                        capacity = selectionShape.capacity();
                        gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                                         selectionShape.capacity() * GLBuffers.SIZEOF_FLOAT,
                                         selectionShape,
                                         GL.GL_DYNAMIC_DRAW);
                    gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
                    }
                    break;
                case IMAGE_CHANGED:
                    imageTiler = new ImageTiler(gl2, image);
                    imageTiler.bindVertices(gl2, imageVBO);
                    numTiles = imageTiler.bindTexture(gl2);
                    image = null;
                    break;
                case COLOUR_CHANGED:
                    uniColours.setData(FloatBuffer.wrap(colours));
                    textRenderer.clearRegion(gl2);
                    st.uniform(gl2, uniColours);
                    break;
                case SYMBOL_CHANGED:
                    uniSymbols.setData(FloatBuffer.wrap(symbols));
                    st.uniform(gl2, uniSymbols);
                    break;
                case POINT_SCALE_CHANGED:
                    Util.updateUniform(gl2, st, "ptscale", point_scale);
                    break;
                case IMAGE_SCALE_CHANGED:
                    Util.updateUniform(gl2, st, "extrascale", extrascale);
                    break;
            }
            i.remove();
        }
    }

}
