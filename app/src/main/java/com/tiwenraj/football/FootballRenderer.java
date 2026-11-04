package com.tiwenraj.football;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Simple playable badminton rally rendered with OpenGL ES 2.0. */
public final class FootballRenderer implements GLSurfaceView.Renderer {
    public interface ScoreListener { void onScore(int score); }

    private final Object lock = new Object();
    private final ScoreListener listener;
    private float px = 0, pz = -6, sx = 0, sy = 2, sz = -5;
    private float vx, vy, vz, mx, mz, ox = 0;
    private boolean flying = false, contrast = false, returning = false;
    private int score = 0, program, pos, mvp, col;
    private FloatBuffer vertices;
    private final float[] proj = new float[16], view = new float[16], vp = new float[16];
    private final float[] model = new float[16], out = new float[16];
    private long last;

    public FootballRenderer(ScoreListener listener) { this.listener = listener; }

    public void setMovement(float x, float z) {
        synchronized (lock) { mx = x; mz = z; }
    }

    public void nudge(float x, float z) {
        synchronized (lock) {
            px = clamp(px + x, -3.5f, 3.5f);
            pz = clamp(pz + z, -8.5f, -1f);
        }
    }

    /** Serve, or strike the shuttle back when it is returning to the player's side. */
    public void kick() {
        synchronized (lock) {
            float distance = (float) Math.hypot(sx - px, sz - pz);
            if (!flying && distance < 3.5f) {
                flying = true;
                returning = false;
                sx = px + .3f; sy = 2f; sz = pz + .8f;
                vx = (ox - px) * .12f; vy = 5f; vz = 9f;
            } else if (flying && returning && distance < 2.4f) {
                returning = false;
                vx = (ox - px) * .18f;
                vy = 5.2f;
                vz = 9f;
            }
        }
    }

    public void setHighContrast(boolean enabled) {
        synchronized (lock) { contrast = enabled; }
    }

    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(.03f, .07f, .1f, 1);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        String vertex = "uniform mat4 u;attribute vec3 p;void main(){gl_Position=u*vec4(p,1.0);}";
        String fragment = "precision mediump float;uniform vec4 c;void main(){gl_FragColor=c;}";
        int vs = shader(35633, vertex), fs = shader(35632, fragment);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        pos = GLES20.glGetAttribLocation(program, "p");
        mvp = GLES20.glGetUniformLocation(program, "u");
        col = GLES20.glGetUniformLocation(program, "c");
        vertices = buffer(cubeData());
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, Math.max(1, height));
        float aspect = (float) width / Math.max(1, height);
        Matrix.frustumM(proj, 0, -aspect, aspect, -1, 1, 2, 80);
    }

    @Override public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = last == 0 ? .016f : Math.min((now - last) / 1e9f, .05f);
        last = now;
        int scored = step(dt);
        GLES20.glClear(16640);
        Matrix.setLookAtM(view, 0, 0, 14, -23, 0, 0, 0, 1, 0);
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0);
        drawCourt();
        drawNet();
        person(px, pz, new float[]{.08f, .4f, .95f, 1});
        person(ox, 6.8f, new float[]{.9f, .25f, .1f, 1});
        box(sx, sy, sz, .13f, .13f, .13f, new float[]{.95f, .75f, .15f, 1});
        box(sx, sy + .1f, sz - .1f, .23f, .18f, .2f, new float[]{.95f, .97f, 1, 1});
        if (scored >= 0 && listener != null) listener.onScore(scored);
    }

    private int step(float dt) {
        synchronized (lock) {
            px = clamp(px + mx * 5f * dt, -3.5f, 3.5f);
            pz = clamp(pz + mz * 5f * dt, -8.5f, -1f);
            if (flying && !returning && sz > 1f) {
                ox += clamp(sx - ox, -2.8f * dt, 2.8f * dt);
            } else {
                ox += Math.signum(-ox) * 1.2f * dt;
            }

            if (flying) {
                sx += vx * dt;
                sy += vy * dt;
                sz += vz * dt;
                vy -= 9.8f * dt;

                // Opponent attempts a return near the far service line.
                if (!returning && sz >= 5.8f) {
                    if (Math.abs(sx - ox) < 1.35f && sy > .55f) {
                        returning = true;
                        vx = (px - ox) * .12f;
                        vy = 4.6f;
                        vz = -8.2f;
                    } else {
                        score++;
                        reset();
                        return score;
                    }
                }

                // A missed return lands on the player's side: opponent wins the rally.
                if (returning && (sz <= -7.8f || sy < .15f)) reset();
                else if (Math.abs(sx) > 4.2f || sy < .15f) reset();
            } else {
                sx = px + .3f;
                sy = 2f;
                sz = pz + .8f;
            }
            return -1;
        }
    }

    private void reset() {
        flying = false;
        returning = false;
        vx = vy = vz = 0;
        sx = px + .3f;
        sy = 2f;
        sz = pz + .8f;
    }

    private void drawCourt() {
        boolean high;
        synchronized (lock) { high = contrast; }
        float[] floor = high ? new float[]{.01f, .04f, .07f, 1} : new float[]{.07f, .32f, .22f, 1};
        float[] line = high ? new float[]{1, .9f, .05f, 1} : new float[]{1, 1, 1, 1};
        box(0, -.12f, 0, 9, .2f, 19, floor);
        box(-4.4f, .02f, 0, .05f, .03f, 19, line);
        box(4.4f, .02f, 0, .05f, .03f, 19, line);
        box(0, .02f, -9.5f, 8.8f, .03f, .05f, line);
        box(0, .02f, 9.5f, 8.8f, .03f, .05f, line);
        box(0, .02f, -2.2f, 8.8f, .03f, .05f, line);
        box(0, .02f, 2.2f, 8.8f, .03f, .05f, line);
        box(-2.2f, .02f, 0, .05f, .03f, 4.4f, line);
        box(2.2f, .02f, 0, .05f, .03f, 4.4f, line);
    }

    private void drawNet() {
        float[] white = {.9f, .95f, 1, 1};
        box(0, 1.15f, 0, 8.8f, .08f, .08f, white);
        box(-4.35f, .65f, 0, .08f, 1.3f, .08f, white);
        box(4.35f, .65f, 0, .08f, 1.3f, .08f, white);
    }

    private void person(float x, float z, float[] shirt) {
        box(x, .9f, z, .58f, .85f, .38f, shirt);
        box(x - .15f, .28f, z, .18f, .45f, .2f, new float[]{.08f, .1f, .15f, 1});
        box(x + .15f, .28f, z, .18f, .45f, .2f, new float[]{.08f, .1f, .15f, 1});
        box(x, 1.55f, z, .34f, .34f, .34f, new float[]{.7f, .48f, .32f, 1});
        box(x + .5f, 1.2f, z + .2f, .08f, .08f, .8f, new float[]{.95f, .95f, 1, 1});
    }

    private void box(float x, float y, float z, float sx, float sy, float sz, float[] color) {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, z);
        Matrix.scaleM(model, 0, sx, sy, sz);
        Matrix.multiplyMM(out, 0, vp, 0, model, 0);
        GLES20.glUseProgram(program);
        vertices.position(0);
        GLES20.glEnableVertexAttribArray(pos);
        GLES20.glVertexAttribPointer(pos, 3, 5126, false, 12, vertices);
        GLES20.glUniformMatrix4fv(mvp, 1, false, out, 0);
        GLES20.glUniform4fv(col, 1, color, 0);
        GLES20.glDrawArrays(4, 0, 36);
        GLES20.glDisableVertexAttribArray(pos);
    }

    private int shader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private FloatBuffer buffer(float[] data) {
        FloatBuffer result = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        result.put(data).position(0);
        return result;
    }

    private float[] cubeData() {
        float[][] faces = {
            {-.5f,-.5f,.5f,.5f,-.5f,.5f,.5f,.5f,.5f,-.5f,.5f,.5f},
            {.5f,-.5f,-.5f,-.5f,-.5f,-.5f,-.5f,.5f,-.5f,.5f,.5f,-.5f},
            {-.5f,.5f,.5f,.5f,.5f,.5f,.5f,.5f,-.5f,-.5f,.5f,-.5f},
            {-.5f,-.5f,-.5f,.5f,-.5f,-.5f,.5f,-.5f,.5f,-.5f,-.5f,.5f},
            {.5f,-.5f,.5f,.5f,-.5f,-.5f,.5f,.5f,-.5f,.5f,.5f,.5f},
            {-.5f,-.5f,-.5f,-.5f,-.5f,.5f,-.5f,.5f,.5f,-.5f,.5f,-.5f}
        };
        float[] output = new float[108];
        int[] triangles = {0, 1, 2, 0, 2, 3};
        int k = 0;
        for (float[] face : faces) for (int index : triangles)
            for (int axis = 0; axis < 3; axis++) output[k++] = face[index * 3 + axis];
        return output;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
