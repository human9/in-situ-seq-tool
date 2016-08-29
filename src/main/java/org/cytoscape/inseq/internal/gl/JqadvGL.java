package org.cytoscape.inseq.internal.gl;

import java.awt.Color;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
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
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.PMVMatrix;
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

    int z = 0;

    float xOffset = 0;
    float yOffset = 0;

    float xMouse = 0;
    float yMouse = 0;
    
    private Texture symbols_tex;

    private ImageTiler imageTiler;

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
    private float point_scale = 1;
    private boolean makeCenter = true;
    private int numTiles = 0;

    private int nPoints;
    private boolean pc;
    private boolean initDone = false;

    private float[] coords;
    float[] img;
    float[] colours;
    float[] bkgrnd;
    float[] symbols;
    FloatBuffer selectionShape;
    int capacity;

    GLUniformData uniColours;
    GLUniformData uniSymbols;

    private BufferedImage pointSprites;

    private Transcript selection;

    private float extrascale = 1f;

    private List<Transcript> transcripts;
    private List<InseqSession.Gene> genesAlphabetical;
    private InseqSession session;

    final public UpdateEngine engine;

    public static final int[] SAMPLE_COUNT = new int[]{4};
    public static final int RENDER_MODES = Region.COLORCHANNEL_RENDERING_BIT + Region.VBAA_RENDERING_BIT;
    private RegionRenderer renderer;
    private TextRegionUtil util;
    private Font font;

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

    public void setPointScale(float value) {

        extrascale = value;
        engine.core.resume();
    }

    public void largePoints(boolean e) {
        if(e)
            point_scale = 2;
        else
            point_scale = 1;

        engine.core.resume();
    }

    public JqadvGL(InseqSession s, GLAutoDrawable canvas) {
        this.session = s;
        this.transcripts = s.getRaw();
        this.engine = new UpdateEngine(canvas);

        genesAlphabetical = s.getGenes();
        // num = how many types of gene we have
        int num = genesAlphabetical.size();

        // vertices required to cover the background
        bkgrnd = Util.makeQuad(-1, -1, 1, 1);


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
        gl2.glActiveTexture(GL.GL_TEXTURE0 + 1);
        symbols_tex = Util.textureFromBufferedImage(pointSprites);
        gl2.glBindTexture(GL.GL_TEXTURE_2D, symbols_tex.getTextureObject());
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
        Util.makeUniform(gl2, st, "ptscale", point_scale);
        Util.makeUniform(gl2, st, "texnum", (float) pointSprites.getWidth() / pointSprites.getHeight());
        Util.makeUniform(gl2, st, "extrascale", extrascale);
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

        try {
            InputStream fs = JqadvGL.class.getResourceAsStream("/font/DroidSans.ttf");
            font = FontFactory.get(fs, true);
        } catch (IOException e) {
            System.out.println("could not create font file");
        }
        gl2.glActiveTexture(GL.GL_TEXTURE0);

        RenderState renderState = RenderState.createRenderState(SVertex.factory());
        renderState.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);

        renderState.setColorStatic(1,1,1,1);
        renderer = RegionRenderer.create(renderState, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
        renderer.init((GL2ES2)gl2, RENDER_MODES);
        
        util = new TextRegionUtil(RENDER_MODES);

        initDone = true;

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

        xMouse = 0; yMouse = 0;
        float wsc = w / session.min.width;
        float hsc = h / session.min.height;
        float target = Math.min(wsc, hsc);

        while(scale < target) {
            if(!scale(-1, 0, 0)) {
                // reached scale limit
                break;
            }
        }
        while(scale > target) {
            if(!scale(1, 0, 0)) {
                break;
            }
        }
        xOffset = ((w - session.min.width * scale) / 2) / scale;
        yOffset = ((h - session.min.height * scale) / 2) / scale;
    }

    protected void setup(GL2 gl2, int width, int height) {
        
        // Update viewport size to canvas dimensions
        gl2.glViewport(0, 0, width, height);

        xOffset += ((width - w) / 2) / scale;
        yOffset += ((height - h) / 2) / scale;

        w = width;
        h = height;
        
        // Center the view
        if (makeCenter) {
            centerView(); 
            makeCenter = false;
        }
        
        renderer.reshapeOrtho(width, height, -1, 1);

        PMatrix.identity()
               .ortho2D(0, w, h, 0)
               .get(PBuffer);
        P.setData(PBuffer);
        st.uniform(gl2, P);
        
    }

    /**
     * Convert a viewport pixel to a graph coordinate.
     */
    public float[] pixelToGraph(float[] p) {
        Vector3f v = new Vector3f(p[0], p[1], 0);
        MviMatrix.transformPosition(v);
        return new float[] {v.x, v.y};
    }
    
    /**
     * Convert graph coordinate to viewport pixel.
     */
    public float[] graphToPixel(float[] p) {
        Vector3f v = new Vector3f(p[0], p[1], 0);
        MvMatrix.transformPosition(v);
        return new float[] {v.x, v.y};
    }

    protected void render(GL2 gl2, int width, int height) {

        gl2.glEnable(GL.GL_BLEND);
        gl2.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl2.glEnable(GL2.GL_POINT_SPRITE);
        gl2.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);

        engine.makeChanges(gl2);

        MvMatrix.identity()
                .translate(xMouse, yMouse, 0f)
                .scale(scale, scale, 0f)
                .translate(xOffset, yOffset, 0f)
                .get(MvBuffer);
        
        Mv.setData(MvBuffer);
        st.uniform(gl2, Mv);
        
        // Make inverse matrix by just doing the opposite of above
        // I dunno it's fast and it works
        MviMatrix.identity()
                .translate(-xOffset, -yOffset, 0f)
                .scale(1/scale, 1/scale, 0f)
                .translate(-xMouse, -yMouse, 0f);

        Util.updateUniform(gl2, st, "extrascale", extrascale);
        Util.updateUniform(gl2, st, "ptscale", point_scale);

        gl2.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);

        st.attachShaderProgram(gl2, bgrndsp, true);
        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, bkgrndVBO);
        gl2.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
        gl2.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);

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
                gl2.glDrawArrays(GL.GL_LINE_STRIP, 1, capacity / 2 - 1);
            } else {
                Util.updateUniform(gl2, st, "closed", 0f);
                gl2.glDrawArrays(GL.GL_LINE_STRIP, 1, capacity / 2 - 2);
            }
            gl2.glLineWidth(1f);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

            gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        }
       
        if(selectionShape != null && capacity > 9) {
        
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

        gl2.glActiveTexture(GL.GL_TEXTURE0);
        
        renderer.enable((GL2ES2)gl2, true);
        
        PMVMatrix matrix = renderer.getMatrix();
        matrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        matrix.glLoadIdentity();
        matrix.glTranslatef(5, h-15, 0);
        
        float fontSize = font.getPixelSize(10, 96);
        float[] colour = new float[4];
        for(InseqSession.Gene gene : genesAlphabetical) {

            gene.color.getRGBComponents(colour);
            colour[3] = 1;

            util.drawString3D((GL2ES2)gl2, renderer, font, fontSize, gene.name,
                    colour, SAMPLE_COUNT);
        
            matrix.glTranslatef(0, -15, 0);
        }

        if(selection != null) {
            float size = font.getPixelSize(10, 96);
            String display = session.name(selection.type) + " " + selection;
            matrix.glLoadIdentity();
            matrix.glTranslatef(w-(size/2)*display.length(), h-15, 0);
            session.getGeneColour(selection.type).getRGBColorComponents(colour);
            util.drawString3D((GL2ES2)gl2, renderer, font, size, display,
                    colour, SAMPLE_COUNT);
        }

        // Bottom text
        matrix.glLoadIdentity();
        matrix.glTranslatef(5, 10, 0);
        
        DecimalFormat df = new DecimalFormat("#.##");
        util.drawString3D((GL2ES2)gl2, renderer, font, fontSize, "Zoom: " + df.format(getScale()*100)+"%",
                new float[] {1,1,1,1}, SAMPLE_COUNT);

        renderer.enable((GL2ES2)gl2, false);


/*
        // BOX
        st.attachShaderProgram(gl2, matsp, true);

        
        gl2.glEnable(GL.GL_BLEND);
        gl2.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        float[] square = Util.makeQuad(-30, -30, 30, 30);
        gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, miscVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                6 * 2 * GLBuffers.SIZEOF_FLOAT,
                FloatBuffer.wrap(square),
                GL.GL_DYNAMIC_DRAW);
        gl2.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
        gl2.glDrawArrays(GL.GL_TRIANGLES, 0, 6);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        String text = "LOADING IMAGE, PLEASE WAIT";
        float size = font.getPixelSize(14, 96);
        float x1 = (w/2 - (size/4)*text.length()) / w - 1;
        float y1 = (h/2) / h - 1;
        float[] square = Util.makeQuad(x1, y1, x1 + (size/4 * text.length()) / w, y1 + (size/4) / h);
        


        renderer.enable((GL2ES2)gl2, true);
        matrix.glLoadIdentity();
        matrix.glTranslatef(w/2 - (size/4)*text.length(), h/2, 0);
        
        util.drawString3D((GL2ES2)gl2, renderer, font, size, text,
                new float[] {1,0,0,1}, SAMPLE_COUNT);

        renderer.enable((GL2ES2)gl2, false);

*/


    }

    /**
     * Marks the selected point.
     */
    public void selectTranscript(Transcript t) {
        selection = t;
        engine.core.resume();
    }

    public float getScale() {
        return scale;
    }

    /**
     * Adjusts the master scale.
     * Returns false if unchaged, true otherwise.
     */
    public boolean scale(int direction, float x, float y) {
        
        // If mouse has moved since last scale, we need to
        // adjust for this (to allow zooming from mouse position).
        if(xMouse != x || yMouse != y) {

            xOffset += (xMouse - x) / scale;
            yOffset += (yMouse - y) / scale;

            xMouse = x;
            yMouse = y;

        }

        if(direction < 0) {
            if(scale < 100f) {
                scale *= 1.1f;
                return true;
            }
        } else {
            if(scale > 0.01f) {
                scale /= 1.1f;
                return true;
            }
        }
        return false; 
    }

    /**
     * Queried during rendering for any changes that need to be uploaded to the
     * GPU.
     * Keeps multiple variable updating contained and orderly.
     */
    public class UpdateEngine {


        Animator core;

        public UpdateEngine(GLAutoDrawable drawable) {
            core = new Animator(drawable);
            core.start();
            core.pause();
        }

        // An empty set of update type enums
        private EnumSet<UpdateType> updates = EnumSet.noneOf(UpdateType.class);

        private BufferedImage image;
        
        /**
         * Pans the image.
         */

        ArrayDeque<float[]> eventFiFo = new ArrayDeque<float[]>();

        public void move(float x, float y) {

            eventFiFo.addLast(new float[] {x,y});
            core.resume();
        }

        public void updateScale(float direction, float x, float y) {
            eventFiFo.addLast(new float[] {direction,x,y});
            core.resume();
        }

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
         * Allows changes that require access to the main OpenGL thread to
         * run later.
         */
        public void makeChanges(GL2 gl2) {
            

            float[] e;
            int size = (int) Math.floor(eventFiFo.size() * 0.5);
            while(eventFiFo.size() > size) {
                e = eventFiFo.removeFirst();
                switch(e.length) {
                    default:
                        System.err.println("Invalid event: This should never happen");
                        break;
                    case 2:
                        xOffset -= e[0] / scale;
                        yOffset -= e[1] / scale;
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
                        util.clear(gl2);
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
        SYMBOL_CHANGED,
    }
}
