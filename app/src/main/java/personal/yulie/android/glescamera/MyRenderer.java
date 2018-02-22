package personal.yulie.android.glescamera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import personal.yulie.android.glescamera.utils.GLSLHelper;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_LINEAR_MIPMAP_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glTexParameterf;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

/**
 * Created by android on 18-1-27.
 */

public class MyRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    public static final float RATIO_16_9 = 16f / 9f;
    public static final float RATIO_4_3 = 4f / 3f;
    public static final float RATIO_18_9 = 18f / 9f;
    public static final float RATIO_19_9 = 19f / 9f;
    private static final String TAG = "MyRenderer";
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT
            + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT;
    private final String mVertexShaderSource;
    private final String mFragmentShaderSource;
    private int mUExTextureUnitLocation;
    private int mATextureCoordLocation;
    private int mProgram;
    private int mAPositionLocation;
    private int mUMatrixLocation;
    private int mUTextureMatrixLocation;

    private final float[] mProjectionMatrix = new float[16];

    private Context mContext;
    private int[] mExTexture = new int[1];
    private SurfaceTexture mSurfaceTexture;
    private Camera mCamera;
    private int mCamereId = Camera.CameraInfo.CAMERA_FACING_BACK;


    private Camera.Parameters mParameter;
    private GLSurfaceView mGLSurfaceView;
    private float[] mTexTransMat = new float[16];



//    public static final float[] VERTEX_DATA = {
//            -1f, 1f, 0f, 0f,
//
//            -1f, -1f, 0f, 1f,
//            1f, -1f, 1f, 1f,
//            1f, 1f, 1f, 0f
//
//    };

    public static final float[] VERTEX_DATA = {
            -1f, 1f, 0f, 1f,
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f,
            1f, 1f, 1f, 1f

    };


    private FloatBuffer mVertexBuffer;

    public MyRenderer(Context context,GLSurfaceView GLSurfaceView) {
        mContext = context;
        mGLSurfaceView = GLSurfaceView;
        mVertexBuffer = ByteBuffer.allocateDirect(VERTEX_DATA.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEX_DATA);
        mVertexShaderSource = GLSLHelper.readFromResource(mContext, R.raw.vertex_shader);
        mFragmentShaderSource = GLSLHelper.readFromResource(mContext, R.raw.fragment_shader);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(1f,1f,1f,1f);
        mProgram = GLSLHelper.buildProgram(mVertexShaderSource, mFragmentShaderSource);
        mAPositionLocation = glGetAttribLocation(mProgram, GLSLHelper.sAPosition);
        mATextureCoordLocation = glGetAttribLocation(mProgram, GLSLHelper.sATextureCoordinates);
        mUExTextureUnitLocation = glGetUniformLocation(mProgram, GLSLHelper.sUExTextureUnit);
        mUMatrixLocation = glGetUniformLocation(mProgram, GLSLHelper.sUMatrix);
        mUTextureMatrixLocation = glGetUniformLocation(mProgram, GLSLHelper.sUTextureMatrix);
        glGenTextures(1, mExTexture, 0);
        if (mExTexture[0] == 0) {
            Log.w(TAG, "onSurfaceCreated: genTextures failed!!!");
            return;
        }

        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExTexture[0]);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);

//        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        mSurfaceTexture = new SurfaceTexture(mExTexture[0]);
        mSurfaceTexture.setOnFrameAvailableListener(this);

        mCamera = Camera.open(mCamereId);
        if (mCamera != null) {
            mParameter = mCamera.getParameters();
            mParameter.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mParameter.setPreviewSize(1280, 720);
            mCamera.setDisplayOrientation(90);
            mCamera.setParameters(mParameter);
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
            } catch (IOException e) {
                Log.e(TAG, "onSurfaceCreated: setPreviewTexture", e);
                mCamera.release();
                mCamera = null;
                return;
            }
            mCamera.startPreview();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
        final float aspectRatio = width > height ?
                width / (float) height : height / (float) width;
        if (width > height) {
            Matrix.orthoM(mProjectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f);
        } else {
            Matrix.orthoM(mProjectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(mProgram);
        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mTexTransMat);
        }
        glUniformMatrix4fv(mUMatrixLocation, 1, false, mProjectionMatrix, 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mExTexture[0]);
        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(mUExTextureUnitLocation, 0);
        glUniformMatrix4fv(mUTextureMatrixLocation, 1, false, mTexTransMat, 0);
        mVertexBuffer.position(0);
        glVertexAttribPointer(mAPositionLocation,
                POSITION_COMPONENT_COUNT,
                GL_FLOAT,
                false,
                STRIDE,
                mVertexBuffer
        );
        glEnableVertexAttribArray(mAPositionLocation);

        mVertexBuffer.position(POSITION_COMPONENT_COUNT);
        glVertexAttribPointer(mATextureCoordLocation,
                POSITION_COMPONENT_COUNT,
                GL_FLOAT,
                false,
                STRIDE,
                mVertexBuffer
                );
        glEnableVertexAttribArray(mATextureCoordLocation);

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mGLSurfaceView != null) {
            mGLSurfaceView.requestRender();
        }
    }


}
