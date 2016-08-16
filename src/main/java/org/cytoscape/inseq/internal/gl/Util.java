package org.cytoscape.inseq.internal.gl;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

/**
 * Util 
 *
 */
public class Util 
{
	public static Texture textureFromResource(String path) {
		java.net.URL url = Util.class.getResource(path);
        try {
            return TextureIO.newTexture(url, true, "png");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
	}

    public static Texture textureFromBufferedImage(BufferedImage img) {
        return AWTTextureIO.newTexture(GLProfile.getDefault(), img, true);
    }

    public static Dimension getRequiredTiles(int dim_max, int tile_max, int w, int h) {
        
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

    public static float[] getVertices(Dimension tiles, int w, int h) {
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
