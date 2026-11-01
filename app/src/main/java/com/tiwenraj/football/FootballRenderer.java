package com.tiwenraj.football;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public final class FootballRenderer implements GLSurfaceView.Renderer {
    public interface ScoreListener {
        void onScore(int score);
    }

    private static final float FIELD_HALF_WIDTH = 10f;
    private static final float FIELD_HALF_LENGTH = 16f;

    private final Object stateLock = new Object();
    private final ScoreListener scoreListener;

    private float playerX = 0f;
    private float playerZ = -8.2f;
    private float ballX = 0f;
    private float ballZ = -5.3f;
    private float ballVelocityX = 0f;
    private float ballVelocityZ = 0f;
    private float movementX = 0f;
    private float movementZ = 0f;
    private int goals = 0;
    private boolean highContrast = false;

    private int program;
    private int positionHandle;
    private int matrixHandle;
    private int colorHandle;
    private FloatBuffer cubeBuffer;
    private FloatBuffer sphereBuffer;
    private int sphereVertexCount;

    private final float[] projection = new float[16];
    private final float[] view = new float[16];
    private final float[] viewProjection = new float[16];
    private final float[] model = new float[16];
    private final float[] modelViewProjection = new float[16];
    private long lastFrameNanos = 0L;

    public FootballRenderer(ScoreListener scoreListener) {
        this.scoreListener = scoreListener;
    }

    public void setMovement(float x, float z) {
        synchronized (stateLock) {
            movementX = x;
            movementZ = z;
        }
    }

    public void nudge(float x, float z) {
        synchronized (stateLock) {
            playerX = clamp(playerX + x * 1.1f, -8.8f, 8.8f);
            playerZ = clamp(playerZ + z * 1.1f, -14f, 7.5f);
        }
    }

    public void kick() {
        synchronized (stateLock) {
            float dx = ballX - playerX;
            float dz = ballZ - playerZ;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);
            if (distance <= 4.2f) {
                ballVelocityX = (playerX - ballX) * 0.7f;
                ballVelocityZ = 14.5f;
            }
        }
    }

    public void setHighContrast(boolean enabled) {
        synchronized (stateLock) {
            highContrast = enabled;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.04f, 0.10f, 0.12f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        program = createProgram();
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        matrixHandle = GLES20.glGetUniformLocation(program, "uMvp");
        colorHandle = GLES20.glGetUniformLocation(program, "uColor");

        cubeBuffer = createFloatBuffer(createCubeVertices());
        float[] sphere = createSphereVertices(14, 20);
        sphereVertexCount = sphere.length / 3;
        sphereBuffer = createFloatBuffer(sphere);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        int safeHeight = Math.max(height, 1);
        GLES20.glViewport(0, 0, width, safeHeight);

        float aspect = (float) width / (float) safeHeight;
        if (aspect >= 1f) {
            Matrix.frustumM(projection, 0, -aspect, aspect, -1f, 1f, 2f, 100f);
        } else {
            Matrix.frustumM(projection, 0, -1f, 1f, -1f / aspect, 1f / aspect, 2f, 100f);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L
                ? 0.016f
                : Math.min((now - lastFrameNanos) / 1_000_000_000f, 0.05f);
        lastFrameNanos = now;

        int updatedScore = updateGame(delta);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        Matrix.setLookAtM(view, 0,
                0f, 19.5f, -31f,
                0f, 0f, 0f,
                0f, 1f, 0f);
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0);

        drawPitch();
        drawGoal();
        drawPlayer(playerX, playerZ, new float[]{0.10f, 0.42f, 0.95f, 1f});
        drawPlayer(-ballX * 0.28f, 11.2f, new float[]{0.86f, 0.16f, 0.15f, 1f});
        drawSphere(ballX, 0.38f, ballZ, 0.34f, 0.34f, 0.34f,
                new float[]{0.96f, 0.97f, 0.98f, 1f});

        if (updatedScore >= 0 && scoreListener != null) {
            scoreListener.onScore(updatedScore);
        }
    }

    private int updateGame(float delta) {
        synchronized (stateLock) {
            playerX = clamp(playerX + movementX * 5.8f * delta,
                    -8.8f, 8.8f);
            playerZ = clamp(playerZ + movementZ * 5.8f * delta,
                    -14f, 7.5f);

            ballX += ballVelocityX * delta;
            ballZ += ballVelocityZ * delta;
            float damping = (float) Math.pow(0.97, delta * 60f);
            ballVelocityX *= damping;
            ballVelocityZ *= damping;

            if (ballZ >= 15.8f && Math.abs(ballX) <= 2.15f) {
                goals++;
                resetBall();
                return goals;
            }

            if (Math.abs(ballX) > FIELD_HALF_WIDTH - 0.5f
                    || ballZ > FIELD_HALF_LENGTH + 1f
                    || ballZ < -FIELD_HALF_LENGTH - 1f) {
                resetBall();
            }
            return -1;
        }
    }

    private void resetBall() {
        ballX = 0f;
        ballZ = -5.3f;
        ballVelocityX = 0f;
        ballVelocityZ = 0f;
    }

    private void drawPitch() {
        boolean contrast;
        synchronized (stateLock) {
            contrast = highContrast;
        }

        float[] grass = contrast
                ? new float[]{0.025f, 0.075f, 0.09f, 1f}
                : new float[]{0.055f, 0.36f, 0.17f, 1f};
        float[] stripeA = contrast
                ? new float[]{0.04f, 0.11f, 0.13f, 1f}
                : new float[]{0.07f, 0.41f, 0.20f, 1f};
        float[] line = contrast
                ? new float[]{1f, 0.91f, 0.12f, 1f}
                : new float[]{0.93f, 0.98f, 0.90f, 1f};

        cube(0f, -0.10f, 0f, 20f, 0.20f, 32f, grass);

        for (int i = 0; i < 8; i += 2) {
            float z = -14f + i * 4f;
            cube(0f, 0.006f, z, 19.8f, 0.012f, 4f, stripeA);
        }

        // Touchlines and end lines.
        cube(-9.8f, 0.025f, 0f, 0.07f, 0.035f, 31.5f, line);
        cube(9.8f, 0.025f, 0f, 0.07f, 0.035f, 31.5f, line);
        cube(0f, 0.025f, -15.75f, 19.6f, 0.035f, 0.07f, line);
        cube(0f, 0.025f, 15.75f, 19.6f, 0.035f, 0.07f, line);

        // Halfway line and centre circle.
        cube(0f, 0.03f, 0f, 19.6f, 0.035f, 0.07f, line);
        for (int i = 0; i < 36; i++) {
            double angle = Math.PI * 2d * i / 36d;
            float x = (float) Math.cos(angle) * 2.35f;
            float z = (float) Math.sin(angle) * 2.35f;
            cube(x, 0.035f, z, 0.15f, 0.035f, 0.15f, line);
        }
        sphere(0f, 0.055f, 0f, 0.13f, 0.035f, 0.13f, line);

        // Penalty boxes at both ends.
        drawBoxMarkings(-15.7f, -9.6f, line);
        drawBoxMarkings(9.6f, 15.7f, line);
    }

    private void drawBoxMarkings(float zA, float zB, float[] color) {
        float low = Math.min(zA, zB);
        float high = Math.max(zA, zB);
        cube(0f, 0.035f, low, 9.6f, 0.035f, 0.07f, color);
        cube(0f, 0.035f, high, 9.6f, 0.035f, 0.07f, color);
        cube(-4.8f, 0.035f, (low + high) / 2f,
                0.07f, 0.035f, high - low, color);
        cube(4.8f, 0.035f, (low + high) / 2f,
                0.07f, 0.035f, high - low, color);
    }

    private void drawGoal() {
        float[] white = new float[]{0.96f, 0.98f, 0.98f, 1f};
        float goalZ = 16.05f;
        cube(-2.15f, 1.15f, goalZ, 0.16f, 2.3f, 0.16f, white);
        cube(2.15f, 1.15f, goalZ, 0.16f, 2.3f, 0.16f, white);
        cube(0f, 2.25f, goalZ, 4.45f, 0.16f, 0.16f, white);

        float[] net = new float[]{0.66f, 0.82f, 0.84f, 1f};
        for (int i = -2; i <= 2; i++) {
            cube(i * 0.8f, 1.1f, 16.8f, 0.035f, 2.1f, 0.035f, net);
        }
        cube(0f, 0.06f, 16.8f, 4.1f, 0.04f, 0.04f, net);
    }

    private void drawPlayer(float x, float z, float[] shirt) {
        float[] shorts = new float[]{0.07f, 0.10f, 0.15f, 1f};
        float[] skin = new float[]{0.72f, 0.48f, 0.31f, 1f};
        float[] shoes = new float[]{0.035f, 0.04f, 0.05f, 1f};

        cube(x, 0.91f, z, 0.67f, 0.83f, 0.42f, shirt);
        cube(x - 0.19f, 0.34f, z, 0.25f, 0.42f, 0.29f, shorts);
        cube(x + 0.19f, 0.34f, z, 0.25f, 0.42f, 0.29f, shorts);
        cube(x - 0.19f, 0.13f, z + 0.02f, 0.18f, 0.28f, 0.19f, shoes);
        cube(x + 0.19f, 0.13f, z + 0.02f, 0.18f, 0.28f, 0.19f, shoes);
        cube(x - 0.43f, 0.88f, z, 0.20f, 0.72f, 0.22f, shirt);
        cube(x + 0.43f, 0.88f, z, 0.20f, 0.72f, 0.22f, shirt);
        sphere(x, 1.57f, z, 0.27f, 0.29f, 0.27f, skin);
    }

    private void cube(float x, float y, float z,
                      float sx, float sy, float sz, float[] color) {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, z);
        Matrix.scaleM(model, 0, sx, sy, sz);
        drawMesh(cubeBuffer, 36, model, color);
    }

    private void drawSphere(float x, float y, float z,
                            float sx, float sy, float sz, float[] color) {
        sphere(x, y, z, sx, sy, sz, color);
    }

    private void sphere(float x, float y, float z,
                        float sx, float sy, float sz, float[] color) {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, z);
        Matrix.scaleM(model, 0, sx, sy, sz);
        drawMesh(sphereBuffer, sphereVertexCount, model, color);
    }

    private void drawMesh(FloatBuffer vertices, int count,
                          float[] modelMatrix, float[] color) {
        Matrix.multiplyMM(modelViewProjection, 0,
                viewProjection, 0, modelMatrix, 0);

        GLES20.glUseProgram(program);
        vertices.position(0);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
                false, 3 * 4, vertices);
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false,
                modelViewProjection, 0);
        GLES20.glUniform4fv(colorHandle, 1, color, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    private int createProgram() {
        String vertexShader =
                "uniform mat4 uMvp;\n" +
                "attribute vec3 aPosition;\n" +
                "void main() { gl_Position = uMvp * vec4(aPosition, 1.0); }";
        String fragmentShader =
                "precision mediump float;\n" +
                "uniform vec4 uColor;\n" +
                "void main() { gl_FragColor = uColor; }";

        int vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        int fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
        int linkedProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(linkedProgram, vertex);
        GLES20.glAttachShader(linkedProgram, fragment);
        GLES20.glLinkProgram(linkedProgram);

        int[] status = new int[1];
        GLES20.glGetProgramiv(linkedProgram, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String error = GLES20.glGetProgramInfoLog(linkedProgram);
            GLES20.glDeleteProgram(linkedProgram);
            throw new IllegalStateException("Could not link OpenGL program: " + error);
        }
        GLES20.glDeleteShader(vertex);
        GLES20.glDeleteShader(fragment);
        return linkedProgram;
    }

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);

        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String error = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException("Could not compile OpenGL shader: " + error);
        }
        return shader;
    }

    private float[] createCubeVertices() {
        float[][] faces = {
                {-0.5f,-0.5f, 0.5f,  0.5f,-0.5f, 0.5f,  0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f},
                { 0.5f,-0.5f,-0.5f, -0.5f,-0.5f,-0.5f, -0.5f, 0.5f,-0.5f,  0.5f, 0.5f,-0.5f},
                {-0.5f, 0.5f, 0.5f,  0.5f, 0.5f, 0.5f,  0.5f, 0.5f,-0.5f, -0.5f, 0.5f,-0.5f},
                {-0.5f,-0.5f,-0.5f,  0.5f,-0.5f,-0.5f,  0.5f,-0.5f, 0.5f, -0.5f,-0.5f, 0.5f},
                { 0.5f,-0.5f, 0.5f,  0.5f,-0.5f,-0.5f,  0.5f, 0.5f,-0.5f,  0.5f, 0.5f, 0.5f},
                {-0.5f,-0.5f,-0.5f, -0.5f,-0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,-0.5f}
        };
        float[] output = new float[6 * 6 * 3];
        int offset = 0;
        for (float[] face : faces) {
            int[] order = {0, 1, 2, 0, 2, 3};
            for (int vertex : order) {
                for (int axis = 0; axis < 3; axis++) {
                    output[offset++] = face[vertex * 3 + axis];
                }
            }
        }
        return output;
    }

    private float[] createSphereVertices(int stacks, int slices) {
        float[] output = new float[stacks * slices * 6 * 3];
        int offset = 0;

        for (int stack = 0; stack < stacks; stack++) {
            double lat0 = -Math.PI / 2d + Math.PI * stack / stacks;
            double lat1 = -Math.PI / 2d + Math.PI * (stack + 1) / stacks;

            for (int slice = 0; slice < slices; slice++) {
                double lon0 = 2d * Math.PI * slice / slices;
                double lon1 = 2d * Math.PI * (slice + 1) / slices;

                float[] p1 = spherePoint(lat0, lon0);
                float[] p2 = spherePoint(lat1, lon0);
                float[] p3 = spherePoint(lat1, lon1);
                float[] p4 = spherePoint(lat0, lon1);

                for (float[] point : new float[][]{p1, p2, p3, p1, p3, p4}) {
                    output[offset++] = point[0];
                    output[offset++] = point[1];
                    output[offset++] = point[2];
                }
            }
        }
        return output;
    }

    private float[] spherePoint(double latitude, double longitude) {
        float x = (float) (Math.cos(latitude) * Math.cos(longitude));
        float y = (float) Math.sin(latitude);
        float z = (float) (Math.cos(latitude) * Math.sin(longitude));
        return new float[]{x, y, z};
    }

    private FloatBuffer createFloatBuffer(float[] values) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(values.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        buffer.put(values);
        buffer.position(0);
        return buffer;
    }

    private float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
