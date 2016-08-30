package org.cytoscape.inseq.internal.gl;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

/**
 * Attempts to create an image that will back the points.
 * If it will not fit within a single texture, it is split into smaller
 * fragments. 
 */
public class ImageTiler {
        
    private Texture[] tiles;
    private Dimension req;
    int MAX_TEXTURE_SIZE;
    int MAX_TEXTURE_UNITS;
    int w;
    int h;
    BufferedImage bufferedImage;

    public ImageTiler(GL2 gl2, BufferedImage bufferedImage) {
        
        w = bufferedImage.getWidth();
        h = bufferedImage.getHeight();
        
        // Detect texture size limits.
        int[] limits = detectHardwareLimits(gl2);
        MAX_TEXTURE_SIZE = limits[0];
        MAX_TEXTURE_UNITS = limits[1];

        req = getRequiredTiles(MAX_TEXTURE_SIZE, MAX_TEXTURE_UNITS, w, h); 

        this.bufferedImage = bufferedImage;

    }

    /**
     * Detect how large textures can be, and how many we can have.
     * If our image is larger than MAX_TEXTURE_SIZE, we will need to break
     * it into no more than (MAX_TEXTURE_UNITS - 1) pieces (as one unit is 
     * to be used for point sprites).
     */
    public static int[] detectHardwareLimits(GL2 gl2) {
        int[] tex_size = new int[1];
        int[] tex_num = new int[1];
        gl2.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, IntBuffer.wrap(tex_size));
        gl2.glGetIntegerv(GL2.GL_MAX_TEXTURE_IMAGE_UNITS, IntBuffer.wrap(tex_num));
        return new int[] {tex_size[0], tex_num[0] };
    }

    public int bindTexture(GL2 gl2) {
        
        // make and bind subimage tiles
        int tilew = w / req.width;
        int tileh = h / req.height;
        BufferedImage sub = new BufferedImage(tilew, tileh, BufferedImage.TYPE_INT_ARGB);
        tiles = new Texture[req.width * req.height];

        int numTiles = 0;
        for(int i = 0; i < req.width*req.height && i < MAX_TEXTURE_UNITS - 2; i++) {

            int vl1 = (int) ((i%req.width) * tilew);
            int vu1 = (int) (((i/req.width)%req.height) * tileh);
            
            // BufferedImage buf = bufferedImage.getSubimage(vl1, vu1, tilew, tileh);
            // BufferedImage.getSubimage sometimes fails for reasons unknown.
            
            Graphics2D g = (Graphics2D) sub.getGraphics();
            g.drawImage(bufferedImage, 0, 0, tilew, tileh, vl1, vu1, vl1+tilew, vu1+tileh, null);

            Texture tile = AWTTextureIO.newTexture(GLProfile.getDefault(), sub, true);
            tiles[i] = tile;
            numTiles ++;
        }

        for(int i = 0; i < req.width*req.height && i < MAX_TEXTURE_UNITS - 2; i++) {
            gl2.glActiveTexture(GL.GL_TEXTURE0 + i+2);
            gl2.glBindTexture(GL.GL_TEXTURE_2D, tiles[i].getTextureObject());
        }
        
        gl2.glActiveTexture(GL.GL_TEXTURE0);

        return numTiles;
    }

    public void bindVertices(GL2 gl2, int imageVBO) {
        
        float[] img = getVertices(req, w, h);

        int num_tiles = img.length / 24;
        System.out.println("Rendering background with " + num_tiles + " tile(s)");

        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, imageVBO);
        gl2.glBufferData(GL.GL_ARRAY_BUFFER,
                         img.length * GLBuffers.SIZEOF_FLOAT,
                         FloatBuffer.wrap(img),
                         GL.GL_STATIC_DRAW);
        gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

    }

    private static Dimension getRequiredTiles(int dim_max, int tile_max, int w, int h) {
        
        // Will describe how many tiles are needed in each dimension.
        Dimension tiles = new Dimension();

        double width = w / (double)dim_max;
        if(width <= 1d) {
            tiles.width = 1; 
        }
        else {
            tiles.width = (int)Math.ceil(width);
        }
        double height = h / (double)dim_max;
        if(height <= 1d) {
            tiles.height = 1; 
        }
        else {
            tiles.height = (int)Math.ceil(height);
        }

        int num = tiles.width * tiles.height;
        if(num > tile_max) {
            System.out.println("Image size is beyond capabilities, "
                +"scale down your image or buy a better graphics card.");
        }

        return tiles;
    }

    private static float[] getVertices(Dimension tiles, int w, int h) {
        int num = tiles.width * tiles.height;
        
        // Each rectangle is composed of two triangles, which require 3
        // coordinates each, which have an x and y component. Then there
        // are the texture mapping coordinates which double this.
        // ie. 24 floats required per rectangle.
        float[] v = new float[24 * num];

        float tilew = (float) w / tiles.width;

        float tileh = (float) h / tiles.height;

        for(int i = 0; i < num; i++) {

            // v for vertex, i for image.
            // l for long(width), u for up(height)
            // 1 for beginning, 2 for end
            float vl1 = (i%tiles.width) * tilew;
            float vu1 = ((i/tiles.width)%tiles.height) * tileh;
            float vl2 = vl1 + tilew;
            float vu2 = vu1 + tileh;
            
            int x = i*24;

            // These are coordinates that decribe a rectangle made of two
            // triangles in the left two columns, and the desired texture
            // coordinates in the right two.
            v[x++] = vl1; v[x++] = vu2; v[x++] = 0f; v[x++] = 1f;
            v[x++] = vl2; v[x++] = vu2; v[x++] = 1f; v[x++] = 1f;
            v[x++] = vl2; v[x++] = vu1; v[x++] = 1f; v[x++] = 0f;
            v[x++] = vl2; v[x++] = vu1; v[x++] = 1f; v[x++] = 0f;
            v[x++] = vl1; v[x++] = vu2; v[x++] = 0f; v[x++] = 1f;
            v[x++] = vl1; v[x++] = vu1; v[x++] = 0f; v[x++] = 0f;
        }

        return v;

    }

}
