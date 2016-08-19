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
                    "shader", null, name, false);
        final ShaderCode fp = 
            ShaderCode.create(gl2,
                    GL2.GL_FRAGMENT_SHADER, Util.class,
                    "shader", null, name, false);

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

    public static Texture textureFromBufferedImage(BufferedImage img) {
        return AWTTextureIO.newTexture(GLProfile.getDefault(), img, true);
    }

}
