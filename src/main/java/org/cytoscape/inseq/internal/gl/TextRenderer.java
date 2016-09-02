package org.cytoscape.inseq.internal.gl;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;

import org.cytoscape.inseq.internal.InseqSession;
import org.cytoscape.inseq.internal.typenetwork.Transcript;

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
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.PMVMatrix;

public class TextRenderer {

    public static final int[] SAMPLE_COUNT = new int[]{4};
    public static final int RENDER_MODES = Region.COLORCHANNEL_RENDERING_BIT + Region.VBAA_RENDERING_BIT;
    private RegionRenderer renderer;
    private TextRegionUtil util;
    private Font font;
    private List<InseqSession.Gene> genesAlphabetical;
    private InseqSession session;
    private int w;
    private int h;


    public TextRenderer(InseqSession s) {

        session = s;
        genesAlphabetical = session.getGenes();
        try {
            InputStream fs = TextRenderer.class.getResourceAsStream("/font/DroidSans.ttf");
            font = FontFactory.get(fs, true);
        } catch (IOException e) {
            System.out.println("could not create font file");
        }
    }

    public void initRender(GL2 gl2) {

        RenderState renderState = RenderState.createRenderState(SVertex.factory());
        renderState.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);

        renderState.setColorStatic(1,1,1,1);
        renderer = RegionRenderer.create(renderState, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
        renderer.init((GL2ES2)gl2, RENDER_MODES);
        
        util = new TextRegionUtil(RENDER_MODES);

    }

    public void reshapeRegion(int w, int h) {
        this.w = w;
        this.h = h;
        renderer.reshapeOrtho(w, h, -1, 1);
    }

    public void clearRegion(GL2 gl2) {
        util.clear(gl2);
    }

    public void renderText(GL2 gl2, float scale, Transcript selection) {

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
        util.drawString3D((GL2ES2)gl2, renderer, font, fontSize, "Zoom: " + df.format(scale*100)+"%",
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

}
