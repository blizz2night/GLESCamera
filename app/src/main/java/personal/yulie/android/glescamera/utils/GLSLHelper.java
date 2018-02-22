package personal.yulie.android.glescamera.utils;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;

/**
 * Created by android on 18-1-29.
 */

public class GLSLHelper {
    private static final String TAG = "GLSLHelper";

    public static final String sVTextureCoordinates = "vTextureCoordinates";
    public static final String sATextureCoordinates = "aTextureCoordinates";
    public static final String sAPosition = "aPosition";
    public static final String sUExTextureUnit = "uExTextureUnit";
    public static final String sUMatrix = "uMatrix";
    public static final String sUTextureMatrix = "uTextureMatrix";


    public static String readFromResource(Context context, int resourceId) {
        StringBuilder body = new StringBuilder();
        String line;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        context.getResources()
                                .openRawResource(resourceId)
                )
        )) {
            while ((line = reader.readLine()) != null) {
                body.append(line);
                body.append('\n');
            }
        } catch (IOException|Resources.NotFoundException e) {
            Log.e(TAG, "readFromResource: ", e);
        }
        return body.toString();
    }

    public static int compileShader(int type, String shaderCode) {
        final int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            Log.w(TAG, "compileShader: glCreateShader failed"+type);
            return 0;
        }

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        final int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);

        if (status[0] == 0) {
            glDeleteShader(shader);
            Log.w(TAG, "compileShader: glCompileShader failed "+type+"\n"
                    +GLES20.glGetShaderInfoLog(shader));
            return 0;
        }

        return shader;
    }

    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        final int program = GLES20.glCreateProgram();
        if (program == 0) {
            Log.w(TAG, "linkProgram: glCreateProgram failed");
            return 0;
        }

        GLES20.glAttachShader(program, vertexShaderId);
        GLES20.glAttachShader(program, fragmentShaderId);

        GLES20.glLinkProgram(program);

        final int[] status = new int[1];
        GLES20.glGetProgramiv(program, GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            glDeleteProgram(program);
            Log.w(TAG, "linkProgram: glLinkProgram failed \n"
                    +GLES20.glGetProgramInfoLog(program));
            return 0;
        }

        return program;
    }

    public static int buildProgram(String vertexShaderSource, String fragmentShaderSource) {
        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);

        int program = linkProgram(vertexShader, fragmentShader);
        return program;
    }

}
