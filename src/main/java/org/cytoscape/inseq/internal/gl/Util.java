package org.cytoscape.inseq.internal.gl;

import java.io.IOException;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

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
}
