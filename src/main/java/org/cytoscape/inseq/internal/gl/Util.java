package org.cytoscape.inseq.internal.gl;

import java.awt.image.BufferedImage;
import java.io.IOException;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

/**
 * Util 
 *
 */
public class Util 
{

    public static void updateUniform(GL2 gl2, ShaderState st, String name, float u) {
        GLUniformData ud = st.getUniform(name);
        ud.setData(u);
        st.uniform(gl2, ud);
    }
    
    public static void updateUniform(GL2 gl2, ShaderState st, String name, int u) {
        GLUniformData ud = st.getUniform(name);
        ud.setData(u);
        st.uniform(gl2, ud);
    }

    /**
     * Uniform creation convenience method.
     */
    public static void makeUniform(GL2 gl2, ShaderState st, String name, float u) {

        GLUniformData ud = new GLUniformData(name, u);
        st.ownUniform(ud);
        st.uniform(gl2, ud);
    }

    /**
     * Shader program compilation convenience method.
     */
    public static ShaderProgram compileProgram(GL2 gl2, String name) {
        final ShaderCode vp = 
            ShaderCode.create(gl2,
                    GL2.GL_VERTEX_SHADER, Util.class,
                    "shader", null, name, "vert", null, false);
        final ShaderCode fp = 
            ShaderCode.create(gl2,
                    GL2.GL_FRAGMENT_SHADER, Util.class,
                    "shader", null, name, "frag", null, false);

        ShaderProgram p = new ShaderProgram();
        p.add(gl2, vp, System.err);
        p.add(gl2, fp, System.err);

        return p;
    }
	public static Texture textureFromResource(String path) {
		java.net.URL url = Util.class.getResource(path);
        try {
            return TextureIO.newTexture(url, true, "png");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
	}

	/**
	 * Generates coordinates for an openGL quad.
	 * Returns an array of 6 floats to be drawn with GL_TRIANGLES.
	 */
	public static float[] makeQuad(float x1, float y1, float x2, float y2) {
		float[] quad = new float[] {
            x1, y2,
            x2, y2,
            x2, y1,
            x2, y1,
            x1, y2,
            x1, y1
        };
		return quad;
	}

    /**
     * Calculate a simple euclidean distance between two points.
     */
    public static double euclideanDistance(float[] a, float[] b) {
        double sqrdist = Math.pow((a[0] - b[0]) , 2) + Math.pow((a[1] - b[1]), 2);
        return Math.sqrt(sqrdist);
    }

    public static Texture textureFromBufferedImage(BufferedImage img) {
        return AWTTextureIO.newTexture(GLProfile.getDefault(), img, true);
    }

}
