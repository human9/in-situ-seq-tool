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
import com.jogamp.graph.font.Font;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;

/**
 * This class is the calling point for all OpenGL rendering code.
 * The init() method sets up the OpenGL context and shaders etc.
 * The "main loop" is within the render() method.
 * Upon resizing, the setup() method is called.
 * Other methods are primarily for adjustment of parameters like
 * zoom, pan, background image scale, etc.
 * 
 */
public class JqadvGL {
    
    // Vertex Buffor Objects
    private int pointsVBO,
                imageVBO,
                bkgrndVBO,
                selectionVBO,
                hudVBO,
                legendVBO;

    // Shader control
    private ShaderState st;

    // Shader programs
    private ShaderProgram jqsp,
                          imgsp,
                          bgrndsp,
                          simplesp,
                          boxsp,
                          matsp,
                          hudsp;


    // Remembering where we are and what size we are
    private float xOffset = 0;
    private float yOffset = 0;
    private float w;
    private float h;
    private float scale = 1;

    private boolean initDone, pathClosed;
    private boolean makeCenter = true;
    private boolean HUDVisible = true;
    private boolean showAll = true;
    private float[] coords, hud, legend, colours, symbols;
    private float[] point_scale = {1f};
    private float[] extrascale = {1f};
    private FloatBuffer selectionShape;
    private BufferedImage image;
    private int capacity;
    private float[] tiny = {-1f};

    private Transcript selectedTranscript;
    private InseqSession session;

    ImageTiler imageTiler = new ImageTiler();
    TextRenderer textRenderer;

    // Projection matrix
    Matrix4f PMatrix = new Matrix4f();
    FloatBuffer PBuffer = Buffers.newDirectFloatBuffer(16);

    // Modelview matrix
    Matrix4f MvMatrix = new Matrix4f();
    FloatBuffer MvBuffer = Buffers.newDirectFloatBuffer(16);

    // The inverse modelview matrix
    Matrix4f MviMatrix = new Matrix4f();

    // An empty set of update type enums
    private EnumSet<UpdateType> updates = EnumSet.noneOf(UpdateType.class);

    // Mouse/keybouard events can build up here before being applied
    private ArrayDeque<float[]> eventFiFo = new ArrayDeque<>();

    // To redraw if more events pending
    protected Animator animator;

    public JqadvGL(InseqSession s, GLAutoDrawable drawable) {
        this.session = s;
        this.textRenderer = new TextRenderer(session);


        // vertices required to cover the background

        // create array specifying what colour each type uses.
        int num = session.getNumGenes();
        colours = new float[num * 3];
        for(int i = 0; i < num; i++) {
            Color c = s.getGeneColour(i);
            int a = i*3;
            float[] f = new float[3];
            System.arraycopy(c.getRGBColorComponents(f), 0, colours, a, 3);
        }
        
        // Create an array of our transcript locations.
        // This array has 3 floats per point, X, Y, and type.
        // Setting the type to a negative value will stop it being displayed.
        coords = new float[s.getRaw().size() * 3];
        int i = 0;
        for(Transcript t : s.getRaw()) {
            coords[i++] = (float) t.pos.x;
            coords[i++] = (float) t.pos.y;
            coords[i++] = t.type;
        }

        hud = new float[] {
             1.0f, -1.0f,
            -1.0f, -1.0f,
            -1.0f, -0.9f,
             1.0f, -0.9f,
        };

        float lheight = 0.5f;
        float lwidth = 0.2f;
        legend = new float[] {
            -1.0f,  1.0f,
            -1.0f, lheight,
            lwidth, lheight,
            lwidth, 1.0f
        };
        
        animator = new Animator(drawable);
        animator.start();

    }


    protected void init(GL2ES2 gl2) {

        // Attempt to enable vsync
        gl2.setSwapInterval(1);

        // Enable specific OpenGL capabilities.
        gl2.glEnable(GL.GL_BLEND);
        gl2.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl2.glEnable(GL2.GL_POINT_SPRITE);
        gl2.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);

        // Create vertex and fragment shader programs.
        generateShaderProgram(gl2);
        
        // Retrieve the point sprites.
        // These are the symbols that are shown on each transcript location.
        BufferedImage pointSprites = ParseUtil.getImageResource("/texture/sprite_sheet.png");
        // create an array specifying which symbol each type uses.
        symbols = new float[session.getNumGenes()];
        for(int i = 0; i < session.getNumGenes(); i++) {
            symbols[i] = session.getGeneSymbol(i);
        }

        // Bind point sprites to GL_TEXTURE0 + 1.
        gl2.glActiveTexture(GL.GL_TEXTURE0 + 1);
        Texture pointSpritesTexture = Util.textureFromBufferedImage(pointSprites);
        gl2.glBindTexture(GL.GL_TEXTURE_2D, pointSpritesTexture.getTextureObject());
        // Disable texture interpolation for point sprites.
        gl2.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl2.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl2.glActiveTexture(GL.GL_TEXTURE0);

        // Create four vertex buffer objects
        // 1. verticesVBO: contains the actual transcript points
        // 2. imageVBO: coordinates to stretch an image across
        // 3. bkgrndVBO: A rectangle that covers the entire screen
        // 4. selectionVBO: Coordinates of the current selection
        // 5. hudVBO: describes the space allocated for scale/other text
        int vbo[] = new int[6];
        gl2.glGenBuffers(6, vbo, 0);
        pointsVBO    = vbo[0];
        imageVBO     = vbo[1];
        bkgrndVBO    = vbo[2];
        selectionVBO = vbo[3];
        hudVBO       = vbo[4];
        legendVBO    = vbo[5];


        // Create GL Buffers
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, hudVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                         hud.length * GLBuffers.SIZEOF_FLOAT,
                         FloatBuffer.wrap(hud),
                         GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, legendVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                         legend.length * GLBuffers.SIZEOF_FLOAT,
                         FloatBuffer.wrap(legend),
                         GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, pointsVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER, 
                         coords.length * GLBuffers.SIZEOF_FLOAT,
                         FloatBuffer.wrap(coords),
                         GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        
        float[] bkgrnd = Util.makeQuad(-1, -1, 1, 1);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, bkgrndVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                6 * 2 * GLBuffers.SIZEOF_FLOAT,
                FloatBuffer.wrap(bkgrnd),
                GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        

        // Bind uniforms (variables besides the arrays that are uploaded to shaders)
        Util.makeUniform(gl2, st, "background", 2);
        Util.makeUniform(gl2, st, "ptsize", (float) pointSprites.getHeight());
        Util.makeUniform(gl2, st, "texnum", (float) pointSprites.getWidth() / pointSprites.getHeight());
        Util.makeUniform(gl2, st, "sel", 0f);
        Util.makeUniform(gl2, st, "closed", 0f);

        // Projection and modelview matrices are also uniforms
        // We don't necesarrily NEED to use matrices
        // But it's nicer than having lots of variables to deal with in the shader
        GLUniformData P = new GLUniformData("P", 4, 4, PBuffer);
        st.uniform(gl2, P);
        GLUniformData Mv = new GLUniformData("Mv", 4, 4, MvBuffer);
        st.uniform(gl2, Mv);
        GLUniformData uniPtScale = new GLUniformData("ptscale", 1, FloatBuffer.wrap(point_scale));
        st.uniform(gl2, uniPtScale);
        GLUniformData uniExtraScale = new GLUniformData("extrascale", 1, FloatBuffer.wrap(extrascale));
        st.uniform(gl2, uniExtraScale);
        GLUniformData uniTiny = new GLUniformData("tiny", 1, FloatBuffer.wrap(tiny));
        st.uniform(gl2, uniTiny);

        GLUniformData sprite = new GLUniformData("sprite", 1);
        st.uniform(gl2, sprite);
        GLUniformData uniSymbols = new GLUniformData("symbols", 1, FloatBuffer.wrap(symbols));
        st.uniform(gl2, uniSymbols);
        GLUniformData uniColours = new GLUniformData("colours", 3, FloatBuffer.wrap(colours));
        st.uniform(gl2, uniColours);

        gl2.glActiveTexture(GL.GL_TEXTURE0);

        // text rendering is complicated, fortunately JOGL takes care of it for us
        textRenderer.initRender(gl2);

        initDone = true;

    }

    /**
     * Add a translation event into the queue.
     */
    public void move(float x, float y) {
        eventFiFo.add(new float[] {x,y});
        animator.go();
    }

    /**
     * Add a scaling event into the queue.
     */
    public void updateScale(float direction, float x, float y) {
        int SCALE_AMPLIFIER = 8;

        // By adding multiple, small zoom events we can make the overall
        // zoom effect smoother.
        for(int i = 0; i < SCALE_AMPLIFIER; i++) {
            eventFiFo.add(new float[] {direction,x,y});
        }
        animator.go();
    }

    /**
     * Change HUD visibility.
     */
    public void toggleHUD() {
        HUDVisible = !HUDVisible;
        animator.go();
    }
    
    public void HUDon() {
        HUDVisible = true;
        animator.go();
    }

    /**
     * Set a new scale and signal that it should be updated.
     */
    public void setImageScale(float value) {
        extrascale[0] = value;
        animator.go();
    }
    
    public float getImageScale() {
        return extrascale[0];
    }

    public void largePoints(int sizeSwitch) {
        switch(sizeSwitch) {
            case 0:
                point_scale[0] = 2;
                break;
            case 1:
                point_scale[0] = 1;
                tiny[0] = 1f;
                break;
            case 2:
                tiny[0] = -1f;
                break;

        }
        animator.go();
    }

    public void setShowAll(boolean state) {
        showAll = state;
        changeNetworkComponents();
    }

    public boolean getShowAll() {
        return showAll;
    }

    public void changeNetworkComponents() {

        List<Transcript> transcripts = session.getRaw();

        for(int i = 0; i < transcripts.size(); i++) {
            // set it to negative to make it not appear
            Transcript t = transcripts.get(i);
            if(showAll || session.isActive(t)) {
                coords[i*3+2] = t.type;
            } else {
                coords[i*3+2] = -t.type-1;
            }
        }
        
        updates.add(UpdateType.NETWORK_COMPONENT_SELECTED);
        animator.go();
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
                this.pathClosed = pathClosed;
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
            animator.go();
        }
    }

    public void changeImage(BufferedImage i) {
        image = i;
        updates.add(UpdateType.IMAGE_CHANGED);
        animator.go();
    }

    public void changeSymbol(Integer type, Integer symbol) {
        symbols[type] = symbol;
        animator.go();
    }

    public void changeColour(Integer type, Color c) {
        int a = type*3;
        float[] f = new float[3];
        System.arraycopy(
                c.getRGBColorComponents(f), 0, colours, a, 3);
        updates.add(UpdateType.COLOUR_CHANGED);
        animator.go();
    }

    /**
     * Marks the selected point.
     */
    public void selectTranscript(Transcript t) {
        selectedTranscript = t;
        animator.go();
    }
    
    /**
     * Create the shader program used to render our image and points.
     * Look under the resources folder to find the source code for these
     * shaders.
     */
    private void generateShaderProgram(GL2ES2 gl2) {

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

        hudsp = Util.compileProgram(gl2, "hud");
        st.attachShaderProgram(gl2, hudsp, false);

        bgrndsp = Util.compileProgram(gl2, "bgrnd");
        st.attachShaderProgram(gl2, bgrndsp, true);
        
    }

    /**
     * Center and scae the view so that as much of the data as possible is visible.
     */
    protected void centerView() {
        
        xOffset = -(w/2 - session.min.width  / 2);
        yOffset = -(h/2 - session.min.height / 2);
        scale = 1;

        float wsc = w / session.min.width;
        float hsc = h / session.min.height;
        float target = Math.min(wsc, hsc);

        while(scale < target) {
            if(!changeScale(-1, w/2, h/2)) {
                // reached scale limit
                break;
            }
        }
        while(scale > target) {
            if(!changeScale(1, w/2, h/2)) {
                break;
            }
        }
    }

    float margin = 50f;
    protected void setup(GL2ES2 gl2, int width, int height) {
        
        // Update viewport size to canvas dimensions
        gl2.glViewport(0, 0, width, height);

        // Keep view centered
        xOffset += (w-width)/2;
        yOffset += (h-height)/2;

        w = width;
        h = height;
        
        hud = new float[] {
             1.0f, -1.0f,
            -1.0f, -1.0f,
            -1.0f, -1+margin/h,
             1.0f, -1+margin/h,
        };

        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, hudVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                         hud.length * GLBuffers.SIZEOF_FLOAT,
                         FloatBuffer.wrap(hud),
                         GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);


        Font font = textRenderer.getFont();
        float fontSize = font.getPixelSize(11, 96);
        float lineHeight = font.getLineHeight(fontSize);
        float longestNameLength = font.getMetricWidth("", fontSize);
        for(InseqSession.Gene gene: session.getGenes()) {
            float stringWidth = font.getMetricWidth(gene.name, fontSize);
            if(stringWidth > longestNameLength) {
                longestNameLength = stringWidth;
            }
        }
        float lwidth = (longestNameLength * 2 + 20) / w;
        float lheight = (session.getNumGenes() * lineHeight * 2 + lineHeight) / h;
        legend = new float[] {
            -1.0f,  1.0f,
            -1.0f, 1-lheight,
            -1+lwidth, 1-lheight,
            -1+lwidth, 1.0f
        };

        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, legendVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                         legend.length * GLBuffers.SIZEOF_FLOAT,
                         FloatBuffer.wrap(legend),
                         GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        
        
        // Center the view
        if (makeCenter) {
            centerView(); 
            makeCenter = false;
        }

        textRenderer.reshapeRegion(width, height);

        PMatrix.identity()
               .ortho(0, w, h, 0, -1, 1)
               .get(PBuffer);
        
    }

    public float getWidth() {
        return w;
    }

    public float getHeight() {
        return h;
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

    private void constructMatrices(GL2ES2 gl2) {

        MvMatrix.identity()
                .translate(-xOffset, -yOffset, 0f)
                .scale(scale, scale, 0f)
                .get(MvBuffer);
        
          MviMatrix.identity()
                .scale(1/scale, 1/scale, 0f)
                .translate(xOffset, yOffset, 0f);
    }

    // This method draws a pretty checkerbox background
    // It's good for debugging (because it contrasts with most things)
    /*
    private void drawBackground(GL2ES2 gl2) {
        st.attachShaderProgram(gl2, bgrndsp, true);
        st.enableVertexAttribArray(gl2, "coord");
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, bkgrndVBO);
        gl2.glVertexAttribPointer(st.getAttribLocation(gl2, "coord"), 2, GL.GL_FLOAT, false, 0, 0);
        gl2.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        st.disableVertexAttribArray(gl2, "coord");
    }
    */
    private void drawImage(GL2ES2 gl2) {

        st.attachShaderProgram(gl2, imgsp, true);
        st.enableVertexAttribArray(gl2, "tex_coords");
        for(int i = 0; i < imageTiler.getNumTiles(); i++) {
            Util.updateUniform(gl2, st, "background", i+2);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, imageVBO);
            gl2.glVertexAttribPointer(st.getAttribLocation(gl2, "tex_coords"), 4, GL.GL_FLOAT, false, 0, i*24*GLBuffers.SIZEOF_FLOAT);
            gl2.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        }

        st.disableVertexAttribArray(gl2, "tex_coords");
    }

    private void drawPoints(GL2ES2 gl2) {
        st.attachShaderProgram(gl2, jqsp, true);

        st.enableVertexAttribArray(gl2, "coord2d");

        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, pointsVBO);

        gl2.glVertexAttribPointer(st.getAttribLocation(gl2, "coord2d"), 3, GL.GL_FLOAT, false, 0, 0);
        gl2.glDrawArrays(GL.GL_POINTS, 0, coords.length / 3);
        
        st.disableVertexAttribArray(gl2, "coord2d");
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

    }

    private void drawSelectionLine(GL2ES2 gl2) {

        st.attachShaderProgram(gl2, simplesp, true);
        
        st.enableVertexAttribArray(gl2, "shape");
        
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, selectionVBO);
        gl2.glLineWidth(3f);
        gl2.glEnable(GL.GL_LINE_SMOOTH);
        gl2.glVertexAttribPointer(st.getAttribLocation(gl2, "shape"), 2, GL.GL_FLOAT, false, 0, 0);
        if(pathClosed) {
            Util.updateUniform(gl2, st, "closed", 1f);
            gl2.glDrawArrays(GL.GL_LINE_STRIP, 1, capacity / 2 - 1);
        } else {
            Util.updateUniform(gl2, st, "closed", 0f);
            gl2.glDrawArrays(GL.GL_LINE_STRIP, 1, capacity / 2 - 2);
        }
        gl2.glLineWidth(1f);
        
        st.disableVertexAttribArray(gl2, "shape");
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

    }

    private void drawSelectionMask(GL2ES2 gl2) {

        gl2.glEnable(GL.GL_STENCIL_TEST);

        st.attachShaderProgram(gl2, simplesp, true);

        //fill polygon w/ stencil buffer
        gl2.glColorMask(false, false, false, false);
        gl2.glStencilFunc(GL.GL_ALWAYS, 1, 0);
        gl2.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_INVERT);
        gl2.glStencilMask(1);


        st.enableVertexAttribArray(gl2, "shape");
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, selectionVBO);
        gl2.glVertexAttribPointer(st.getAttribLocation(gl2, "shape"), 2, GL.GL_FLOAT, false, 0, 0);
        gl2.glDrawArrays(GL.GL_TRIANGLE_FAN, 0, capacity / 2);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        st.disableVertexAttribArray(gl2, "shape");

        st.attachShaderProgram(gl2, boxsp, true);

        gl2.glColorMask(true, true, true, true);
        gl2.glStencilFunc(GL.GL_EQUAL, 0, 1);
        gl2.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);

        st.enableVertexAttribArray(gl2, "coord");
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, bkgrndVBO);
        gl2.glVertexAttribPointer(st.getAttribLocation(gl2, "coord"), 2, GL.GL_FLOAT, false, 0, 0);
        gl2.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        st.disableVertexAttribArray(gl2, "coord");


        gl2.glDisable(GL.GL_STENCIL_TEST);

    }

    private void drawScaleBarBorder(GL2ES2 gl2) {

        st.attachShaderProgram(gl2, hudsp, true);
        st.enableVertexAttribArray(gl2, "shape");
        
        gl2.glLineWidth(3f);
        gl2.glEnable(GL.GL_LINE_SMOOTH);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, hudVBO);
        gl2.glVertexAttribPointer(st.getAttribLocation(gl2, "shape"),
                                2,
                                GL.GL_FLOAT,
                                false,
                                0,
                                0);
        gl2.glDrawArrays(GL.GL_TRIANGLE_FAN, 0, hud.length/2);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        st.disableVertexAttribArray(gl2, "shape");
        
        st.enableVertexAttribArray(gl2, "shape");
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, legendVBO);
        gl2.glVertexAttribPointer(st.getAttribLocation(gl2, "shape"),
                                2,
                                GL.GL_FLOAT,
                                false,
                                0,
                                0);
        gl2.glDrawArrays(GL.GL_TRIANGLE_FAN, 0, legend.length/2);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        st.disableVertexAttribArray(gl2, "shape");

    }

    private void drawSelectedPointBubble(GL2ES2 gl2) {
        st.attachShaderProgram(gl2, jqsp, true);
        
        st.enableVertexAttribArray(gl2, "coord2d");
        
        Util.updateUniform(gl2, st, "sel", 1f);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, pointsVBO);
        gl2.glVertexAttribPointer(st.getAttribLocation(gl2, "coord2d"), 3, GL.GL_FLOAT, false, 0, selectedTranscript.index * 3 * GLBuffers.SIZEOF_FLOAT);
        gl2.glDrawArrays(GL.GL_POINTS, 0, 1);
        Util.updateUniform(gl2, st, "sel", 0f);

        try {
            for(Transcript t : selectedTranscript.getNeighboursForNetwork(session.getSelectedNetwork()) ) {

                Util.updateUniform(gl2, st, "sel", 1f);
                gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, pointsVBO);
                gl2.glVertexAttribPointer(st.getAttribLocation(gl2, "coord2d"), 3, GL.GL_FLOAT, false, 0, t.index * 3 * GLBuffers.SIZEOF_FLOAT);
                gl2.glDrawArrays(GL.GL_POINTS, 0, 1);
                Util.updateUniform(gl2, st, "sel", 0f);
                
            }
        }
        catch (NullPointerException e) {
            // No neighbours, clean up
        }

        st.disableVertexAttribArray(gl2, "coord2d");
    }


    protected void render(GL2ES2 gl2, int width, int height) {
    

        gl2.glEnable(GL.GL_BLEND);
        gl2.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl2.glEnable(GL2.GL_POINT_SPRITE);
        gl2.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);

        fetchUpdates(gl2); 
        constructMatrices(gl2);

        gl2.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);

        //drawBackground(gl2);
        drawImage(gl2);
        drawPoints(gl2);

        if(selectionShape != null) {
            
            drawSelectionLine(gl2); 

            // capacity will be at least 10 if a valid shape is drawn
            if(capacity >= 10) {
            
                drawSelectionMask(gl2);
            }
        }
        
        if(selectedTranscript != null) {
            drawSelectedPointBubble(gl2);    
        }
            
        if(HUDVisible) {
            drawScaleBarBorder(gl2);
            textRenderer.renderText(gl2, scale, selectedTranscript);
        
        }
        
        if(!eventFiFo.isEmpty()) animator.go();
    }

    public float getScale() {
        return scale;
    }

    /**
     * Adjusts the master scale.
     * Returns false if unchaged, true otherwise.
     */
    protected boolean changeScale(int direction, float x, float y) {
        
        double SCALE_FACTOR = 1.01;

        if(direction < 0) {
            if(scale < 100f) {
                xOffset -= (x+xOffset) - (x+xOffset)*SCALE_FACTOR; 
                yOffset -= (y+yOffset) - (y+yOffset)*SCALE_FACTOR; 
                scale *= SCALE_FACTOR;
                return true;
            }
        } else {
            if(scale > 0.005f) {
                xOffset -= (x+xOffset) - (x+xOffset)/SCALE_FACTOR; 
                yOffset -= (y+yOffset) - (y+yOffset)/SCALE_FACTOR; 
                scale /= SCALE_FACTOR;
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
    } 

    /**
     * Called once every render loop to check for anything that may need updating.
     */

    public void fetchUpdates(GL2ES2 gl2) {

        // Each frame, the number of events removed from the queue equals
        // sizeof(queue) x EVENT_THROTTLE, such that if EVENT_THROTTLE = 1
        // all of the events will be removed, with a lower value meaning the
        // animation becomes more delayed (and more fluid)
        float EVENT_THROTTLE = 0.6f; 
        
        float[] e;
        int size = (int) Math.floor(eventFiFo.size() * EVENT_THROTTLE);
        while(eventFiFo.size() > size) {
            e = eventFiFo.remove();
            switch(e.length) {
                default:
                    System.err.println("Invalid event: This should never happen");
                    break;
                case 2:
                    xOffset += e[0];
                    yOffset += e[1];
                    break;
                case 3:
                    changeScale((int)e[0], e[1], e[2]);
                    break;
            }

        }
        // These are specific things we probably don't want to do every time
        // we render, but will need to do if things change.
        for(Iterator<UpdateType> i = updates.iterator(); i.hasNext();) {
            UpdateType update = i.next();
            switch(update) {
                case NETWORK_COMPONENT_SELECTED:
                    gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, pointsVBO);
                    gl2.glBufferData(GL.GL_ARRAY_BUFFER, 
                                     coords.length * GLBuffers.SIZEOF_FLOAT,
                                     FloatBuffer.wrap(coords),
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
                    imageTiler.bindTexture(gl2);
                    image = null;
                    break;
                case COLOUR_CHANGED:
                    textRenderer.clearRegion(gl2);
                    break;
            }
            i.remove();
        }
    }

}
