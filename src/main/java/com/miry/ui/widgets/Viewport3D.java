package com.miry.ui.widgets;

import com.miry.graphics.Framebuffer;
import com.miry.graphics.Shader;
import com.miry.graphics.Texture;
import com.miry.ui.gizmo.TranslateGizmo3D;
import com.miry.ui.input.UiInput;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;

/**
 * Offscreen-rendered 3D viewport used for editor previews.
 * <p>
 * Renders into an internal framebuffer and exposes the color texture for UI embedding.
 * Includes a basic translate gizmo (X/Y/Z) and orbit/zoom camera controls.
 */
public final class Viewport3D implements AutoCloseable {
    private final Framebuffer framebuffer = new Framebuffer();
    private final Shader shader = Shader.fromResources("shaders/viewport3d.vert", "shaders/viewport3d.frag");
    private final TranslateGizmo3D gizmo = new TranslateGizmo3D();
    private final UiInput framebufferInput = new UiInput();

    private final int cubeVao;
    private final int cubeVbo;
    private final int cubeEbo;
    private final int cubeIndexCount;

    private final int linesVao;
    private final int linesVbo;
    private int linesCapacityFloats = 0;
    private int linesVertexCount = 0;

    private final int gizmoVao;
    private final int gizmoVbo;
    private int gizmoCapacityFloats = 0;
    private int gizmoVertexCount = 0;

    private final Vector3f target = new Vector3f(0.0f, 0.0f, 0.0f);
    private float yaw = 0.6f;
    private float pitch = 0.35f;
    private float distance = 4.2f;

    private boolean orbiting;
    private float lastMouseX;
    private float lastMouseY;

    private final Vector3f objectPos = new Vector3f(0.0f, 0.0f, 0.0f);

    public Viewport3D() {
        // Cube: position + color per vertex.
        float[] cubeVerts = {
            // x,y,z,   r,g,b
            -0.5f, -0.5f, -0.5f,   0.75f, 0.25f, 0.25f,
             0.5f, -0.5f, -0.5f,   0.25f, 0.75f, 0.25f,
             0.5f,  0.5f, -0.5f,   0.25f, 0.25f, 0.75f,
            -0.5f,  0.5f, -0.5f,   0.75f, 0.75f, 0.25f,
            -0.5f, -0.5f,  0.5f,   0.25f, 0.75f, 0.75f,
             0.5f, -0.5f,  0.5f,   0.85f, 0.55f, 0.25f,
             0.5f,  0.5f,  0.5f,   0.85f, 0.25f, 0.85f,
            -0.5f,  0.5f,  0.5f,   0.25f, 0.85f, 0.55f
        };
        int[] cubeIdx = {
            0, 1, 2, 2, 3, 0,
            4, 5, 6, 6, 7, 4,
            0, 4, 7, 7, 3, 0,
            1, 5, 6, 6, 2, 1,
            3, 2, 6, 6, 7, 3,
            0, 1, 5, 5, 4, 0
        };
        cubeIndexCount = cubeIdx.length;

        cubeVao = glGenVertexArrays();
        cubeVbo = glGenBuffers();
        cubeEbo = glGenBuffers();

        glBindVertexArray(cubeVao);
        glBindBuffer(GL_ARRAY_BUFFER, cubeVbo);
        FloatBuffer vb = BufferUtils.createFloatBuffer(cubeVerts.length);
        vb.put(cubeVerts).flip();
        glBufferData(GL_ARRAY_BUFFER, vb, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cubeEbo);
        IntBuffer ib = BufferUtils.createIntBuffer(cubeIdx.length);
        ib.put(cubeIdx).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_DYNAMIC_DRAW);

        int stride = 6 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glBindVertexArray(0);

        linesVao = glGenVertexArrays();
        linesVbo = glGenBuffers();
        glBindVertexArray(linesVao);
        glBindBuffer(GL_ARRAY_BUFFER, linesVbo);
        glBufferData(GL_ARRAY_BUFFER, 1L, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glBindVertexArray(0);

        gizmoVao = glGenVertexArrays();
        gizmoVbo = glGenBuffers();
        glBindVertexArray(gizmoVao);
        glBindBuffer(GL_ARRAY_BUFFER, gizmoVbo);
        glBufferData(GL_ARRAY_BUFFER, 1L, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glBindVertexArray(0);
    }

    public Texture texture() {
        return framebuffer.colorTexture();
    }

    public Vector3f objectPosition() {
        return objectPos;
    }

    public void updateInput(UiInput input,
                            boolean rightDown,
                            boolean rightPressed,
                            boolean rightReleased,
                            int viewportX,
                            int viewportY,
                            int viewportW,
                            int viewportH) {
        updateInput(input, rightDown, rightPressed, rightReleased, viewportX, viewportY, viewportW, viewportH, 1.0f, 1.0f);
    }

    /**
     * Updates camera orbit/zoom and gizmo interaction.
     *
     * @param framebufferScaleX window→framebuffer scale (HiDPI)
     * @param framebufferScaleY window→framebuffer scale (HiDPI)
     */
    public void updateInput(UiInput input,
                            boolean rightDown,
                            boolean rightPressed,
                            boolean rightReleased,
                            int viewportX,
                            int viewportY,
                            int viewportW,
                            int viewportH,
                            float framebufferScaleX,
                            float framebufferScaleY) {
        if (input == null || viewportW <= 0 || viewportH <= 0) {
            return;
        }

        float mx = input.mousePos().x;
        float my = input.mousePos().y;
        boolean inside = mx >= viewportX && my >= viewportY && mx < viewportX + viewportW && my < viewportY + viewportH;

        if (rightPressed && inside) {
            orbiting = true;
            lastMouseX = mx;
            lastMouseY = my;
        }
        if (rightReleased) {
            orbiting = false;
        }
        if (orbiting && rightDown) {
            float dx = mx - lastMouseX;
            float dy = my - lastMouseY;
            lastMouseX = mx;
            lastMouseY = my;

            yaw += dx * 0.0125f;
            pitch += dy * 0.0125f;
            pitch = clamp(pitch, -1.35f, 1.35f);
        }

        if (inside && input.scrollY() != 0.0) {
            float factor = (float) Math.pow(1.12, -input.scrollY());
            distance = clamp(distance * factor, 1.2f, 22.0f);
        }

        // Gizmo hover + drag (left button only via UiInput).
        if (!orbiting && inside) {
            float sx = Math.max(0.1f, framebufferScaleX);
            float sy = Math.max(0.1f, framebufferScaleY);

            framebufferInput
                .setMousePos(mx * sx, my * sy)
                .setMouseButtons(input.mouseDown(), input.mousePressed(), input.mouseReleased())
                .setScrollY(0.0);

            int vx = Math.round(viewportX * sx);
            int vy = Math.round(viewportY * sy);
            int vw = Math.round(viewportW * sx);
            int vh = Math.round(viewportH * sy);

            Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(50.0), vw / (float) Math.max(1, vh), 0.05f, 200.0f);
            Matrix4f view = new Matrix4f().lookAt(cameraPosition(), target, new Vector3f(0.0f, 1.0f, 0.0f));
            Matrix4f vp = new Matrix4f(proj).mul(view);
            Vector3f camPos = cameraPosition();

            gizmo.update(framebufferInput, vp, camPos, vx, vy, vw, vh, objectPos);
        }
    }

    public void renderToTexture(int pixelW, int pixelH, int restoreViewportW, int restoreViewportH, float timeSeconds) {
        framebuffer.ensureSize(pixelW, pixelH);
        framebuffer.bind();

        glEnable(GL_DEPTH_TEST);
        glClearColor(0.07f, 0.07f, 0.09f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(50.0), pixelW / (float) pixelH, 0.05f, 200.0f);
        Matrix4f view = new Matrix4f();
        Vector3f camPos = cameraPosition();
        view.lookAt(camPos, target, new Vector3f(0.0f, 1.0f, 0.0f));

        shader.bind();

        // Cube
        Matrix4f model = new Matrix4f()
            .translate(objectPos)
            .rotateY(timeSeconds * 0.55f)
            .rotateX(timeSeconds * 0.23f);
        Matrix4f mvp = new Matrix4f(proj).mul(view).mul(model);
        shader.setUniform("uMvp", mvp);
        glBindVertexArray(cubeVao);
        glDrawElements(GL_TRIANGLES, cubeIndexCount, org.lwjgl.opengl.GL11.GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);

        // Lines (grid + gizmo)
        Matrix4f vp = new Matrix4f(proj).mul(view);
        shader.setUniform("uMvp", vp);
        updateLines();
        glBindVertexArray(linesVao);
        glDrawArrays(GL_LINES, 0, linesVertexCount);
        glBindVertexArray(0);

        // Gizmo triangles rendered on top (no depth test).
        glDisable(GL_DEPTH_TEST);
        updateGizmo(vp, camPos, pixelW, pixelH);
        glBindVertexArray(gizmoVao);
        glDrawArrays(GL_TRIANGLES, 0, gizmoVertexCount);
        glBindVertexArray(0);

        shader.unbind();

        Framebuffer.unbind(restoreViewportW, restoreViewportH);
    }

    private void updateLines() {
        FloatBuffer buf = buildLinesBuffer();
        glBindBuffer(GL_ARRAY_BUFFER, linesVbo);
        if (buf.capacity() > linesCapacityFloats) {
            linesCapacityFloats = buf.capacity();
            glBufferData(GL_ARRAY_BUFFER, (long) linesCapacityFloats * Float.BYTES, GL_DYNAMIC_DRAW);
        }
        glBufferSubData(GL_ARRAY_BUFFER, 0, buf);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void updateGizmo(Matrix4f viewProj, Vector3f cameraPos, int pixelW, int pixelH) {
        FloatBuffer buf = buildGizmoBuffer(viewProj, cameraPos, pixelW, pixelH);
        glBindBuffer(GL_ARRAY_BUFFER, gizmoVbo);
        if (buf.capacity() > gizmoCapacityFloats) {
            gizmoCapacityFloats = buf.capacity();
            glBufferData(GL_ARRAY_BUFFER, (long) gizmoCapacityFloats * Float.BYTES, GL_DYNAMIC_DRAW);
        }
        glBufferSubData(GL_ARRAY_BUFFER, 0, buf);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private FloatBuffer buildGizmoBuffer(Matrix4f viewProj, Vector3f cameraPos, int pixelW, int pixelH) {
        // Upper bound: each axis writes ribbon quad (6) + head tri (3), plus center quad (6).
        int maxVerts = (3 * (6 + 3)) + 6;
        FloatBuffer fb = BufferUtils.createFloatBuffer(maxVerts * 6);
        gizmo.writeTriangles(fb, viewProj, cameraPos, 0, 0, pixelW, pixelH, objectPos);
        fb.flip();
        gizmoVertexCount = fb.remaining() / 6;
        return fb;
    }

    private FloatBuffer buildLinesBuffer() {
        // Each vertex: pos(3) + color(3)
        int gridCount = 21;
        int gridLines = gridCount * 2 + gridCount * 2; // x-lines + z-lines
        int gridVerts = gridLines * 2;

        int axisLines = 3;
        int axisVerts = axisLines * 2;
        int totalVerts = gridVerts + axisVerts;
        FloatBuffer fb = BufferUtils.createFloatBuffer(totalVerts * 6);

        // Grid on XZ plane
        float half = (gridCount - 1) * 0.5f;
        for (int i = 0; i < gridCount; i++) {
            float t = i - half;
            float c = (Math.abs(t) < 0.001f) ? 0.20f : 0.12f;
            // Line parallel to X at z=t
            putLine(fb, -half, 0.0f, t, half, 0.0f, t, c, c, c);
            // Line parallel to Z at x=t
            putLine(fb, t, 0.0f, -half, t, 0.0f, half, c, c, c);
        }

        // World axes at origin
        putLine(fb, 0, 0, 0, 1.5f, 0, 0, 0.85f, 0.25f, 0.25f);
        putLine(fb, 0, 0, 0, 0, 1.5f, 0, 0.25f, 0.85f, 0.25f);
        putLine(fb, 0, 0, 0, 0, 0, 1.5f, 0.25f, 0.45f, 0.95f);

        fb.flip();
        linesVertexCount = fb.remaining() / 6;
        return fb;
    }

    private static void putLine(FloatBuffer fb,
                                float ax, float ay, float az,
                                float bx, float by, float bz,
                                float r, float g, float b) {
        fb.put(ax).put(ay).put(az).put(r).put(g).put(b);
        fb.put(bx).put(by).put(bz).put(r).put(g).put(b);
    }

    private Vector3f cameraPosition() {
        float cp = (float) Math.cos(pitch);
        float sp = (float) Math.sin(pitch);
        float cy = (float) Math.cos(yaw);
        float sy = (float) Math.sin(yaw);
        return new Vector3f(cy * cp, sp, sy * cp).mul(distance).add(target);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public void close() {
        shader.close();
        framebuffer.close();
        glDeleteVertexArrays(cubeVao);
        glDeleteBuffers(cubeVbo);
        glDeleteBuffers(cubeEbo);
        glDeleteVertexArrays(linesVao);
        glDeleteBuffers(linesVbo);
        glDeleteVertexArrays(gizmoVao);
        glDeleteBuffers(gizmoVbo);
    }
}
