package com.miry.ui.gizmo;

import com.miry.graphics.Framebuffer;
import com.miry.graphics.Shader;
import com.miry.graphics.Texture;
import com.miry.ui.input.UiInput;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetIntegerv;
import static org.lwjgl.opengl.GL11.glGetBoolean;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL11.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER_BINDING;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB;
import static org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB;
import static org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA;
import static org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA;
import static org.lwjgl.opengl.GL30C.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_READ_FRAMEBUFFER;

/**
 * Renders editor gizmos into a transparent texture so hosts can overlay them on any viewport.
 * <p>
 * This keeps gizmo interaction + rendering separate from any scene/viewport rendering (e.g. Minecraft capture).
 * </p>
 */
public final class GizmoOverlay3D implements AutoCloseable {
    public enum Mode {
        NONE,
        TRANSLATE,
        ROTATE,
        SCALE,
        COMPOSITE
    }

    private final Framebuffer framebuffer = new Framebuffer();
    private final Shader shader = Shader.fromResources("shaders/viewport3d.vert", "shaders/viewport3d.frag");
    private final UiInput framebufferInput = new UiInput();

    private final TranslateGizmo3D translateGizmo = new TranslateGizmo3D();
    private final ScaleGizmo3D scaleGizmo = new ScaleGizmo3D();
    private final RotateGizmo3D rotateGizmo = new RotateGizmo3D();

    private Mode mode = Mode.TRANSLATE;
    private GizmoSpace gizmoSpace = GizmoSpace.WORLD;
    private final Matrix3f localAxes = new Matrix3f().identity();

    private final int vao;
    private final int vbo;
    private int capacityFloats;
    private int vertexCount;
    private FloatBuffer scratch;

    public GizmoOverlay3D() {
        int prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 1L, GL_DYNAMIC_DRAW);

        int stride = 6 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);

        glBindVertexArray(prevVao);
        glBindBuffer(GL_ARRAY_BUFFER, prevArrayBuffer);
    }

    public Mode mode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.NONE : mode;
    }

    public GizmoSpace gizmoSpace() {
        return gizmoSpace;
    }

    public void setGizmoSpace(GizmoSpace gizmoSpace) {
        this.gizmoSpace = gizmoSpace == null ? GizmoSpace.WORLD : gizmoSpace;
    }

    /**
     * Sets the local-space axes (rotation-only) used when {@link #gizmoSpace()} is {@link GizmoSpace#LOCAL}.
     */
    public void setLocalAxes(Matrix3f axes) {
        if (axes == null) {
            localAxes.identity();
        } else {
            localAxes.set(axes);
        }
    }

    public String dragLabel() {
        return switch (mode) {
            case TRANSLATE -> translateGizmo.dragLabel();
            case ROTATE -> rotateGizmo.dragLabel();
            case SCALE -> scaleGizmo.dragLabel();
            case COMPOSITE -> {
                String t = translateGizmo.dragLabel();
                if (t != null) yield t;
                String r = rotateGizmo.dragLabel();
                if (r != null) yield r;
                yield scaleGizmo.dragLabel();
            }
            case NONE -> null;
        };
    }

    public Texture texture() {
        return framebuffer.colorTexture();
    }

    public boolean dragging() {
        return switch (mode) {
            case TRANSLATE -> translateGizmo.activeHandle() != TranslateHandle.NONE;
            case SCALE -> scaleGizmo.activeHandle() != ScaleHandle.NONE;
            case ROTATE -> rotateGizmo.activeAxis() != GizmoAxis.NONE;
            case COMPOSITE -> translateGizmo.activeHandle() != TranslateHandle.NONE ||
                              rotateGizmo.activeAxis() != GizmoAxis.NONE ||
                              scaleGizmo.activeHandle() != ScaleHandle.NONE;
            case NONE -> false;
        };
    }

    public void updateInput(UiInput input,
                            Matrix4f viewProj,
                            Vector3f cameraPos,
                            int viewportX,
                            int viewportY,
                            int viewportW,
                            int viewportH,
                            float framebufferScaleX,
                            float framebufferScaleY,
                            Vector3f inOutPos,
                            Vector3f inOutEulerDeg,
                            Vector3f inOutScale) {
        if (mode == Mode.NONE || input == null || viewProj == null || cameraPos == null) {
            return;
        }
        if (viewportW <= 0 || viewportH <= 0) {
            return;
        }

        syncGizmoSpace();

        float sx = Math.max(0.1f, framebufferScaleX);
        float sy = Math.max(0.1f, framebufferScaleY);

        float mx = input.mousePos().x;
        float my = input.mousePos().y;

        framebufferInput
                .setMousePos(mx * sx, my * sy)
                .setMouseButtons(input.mouseDown(), input.mousePressed(), input.mouseReleased())
                .setModifiers(input.ctrlDown(), input.shiftDown(), input.altDown(), input.superDown())
                .setScrollY(0.0);

        int vx = Math.round(viewportX * sx);
        int vy = Math.round(viewportY * sy);
        int vw = Math.round(viewportW * sx);
        int vh = Math.round(viewportH * sy);

        switch (mode) {
            case TRANSLATE -> translateGizmo.update(framebufferInput, viewProj, cameraPos, vx, vy, vw, vh, inOutPos);
            case SCALE -> scaleGizmo.update(framebufferInput, viewProj, cameraPos, vx, vy, vw, vh, inOutPos, inOutScale);
            case ROTATE -> rotateGizmo.update(framebufferInput, viewProj, cameraPos, vx, vy, vw, vh, inOutPos, inOutEulerDeg);
            case COMPOSITE -> updateComposite(framebufferInput, viewProj, cameraPos, vx, vy, vw, vh, inOutPos, inOutEulerDeg, inOutScale);
            case NONE -> {
            }
        }
    }

    private void syncGizmoSpace() {
        translateGizmo.setSpace(gizmoSpace);
        scaleGizmo.setSpace(gizmoSpace);
        rotateGizmo.setSpace(gizmoSpace);
        translateGizmo.setLocalAxes(localAxes);
        scaleGizmo.setLocalAxes(localAxes);
        rotateGizmo.setLocalAxes(localAxes);
    }

    private void updateComposite(UiInput input,
                                 Matrix4f viewProj,
                                 Vector3f cameraPos,
                                 int vx,
                                 int vy,
                                 int vw,
                                 int vh,
                                 Vector3f inOutPos,
                                 Vector3f inOutEulerDeg,
                                 Vector3f inOutScale) {
        if (translateGizmo.activeHandle() != TranslateHandle.NONE) {
            translateGizmo.update(input, viewProj, cameraPos, vx, vy, vw, vh, inOutPos);
            return;
        }
        if (rotateGizmo.activeAxis() != GizmoAxis.NONE) {
            rotateGizmo.update(input, viewProj, cameraPos, vx, vy, vw, vh, inOutPos, inOutEulerDeg);
            return;
        }
        if (scaleGizmo.activeHandle() != ScaleHandle.NONE) {
            scaleGizmo.update(input, viewProj, cameraPos, vx, vy, vw, vh, inOutPos, inOutScale);
            return;
        }

        boolean down = input.mouseDown();
        boolean pressed = input.mousePressed();
        boolean released = input.mouseReleased();

        // Hover pre-pass (suppresses activation).
        input.setMouseButtons(down, false, false);
        translateGizmo.update(input, viewProj, cameraPos, vx, vy, vw, vh, inOutPos);
        rotateGizmo.update(input, viewProj, cameraPos, vx, vy, vw, vh, inOutPos, inOutEulerDeg);
        scaleGizmo.update(input, viewProj, cameraPos, vx, vy, vw, vh, inOutPos, inOutScale);

        // Restore buttons and activate one tool by priority.
        input.setMouseButtons(down, pressed, released);
        if (pressed) {
            if (translateGizmo.hoveredHandle() != TranslateHandle.NONE) {
                translateGizmo.update(input, viewProj, cameraPos, vx, vy, vw, vh, inOutPos);
            } else if (rotateGizmo.hoveredAxis() != GizmoAxis.NONE) {
                rotateGizmo.update(input, viewProj, cameraPos, vx, vy, vw, vh, inOutPos, inOutEulerDeg);
            } else if (scaleGizmo.hoveredHandle() != ScaleHandle.NONE) {
                scaleGizmo.update(input, viewProj, cameraPos, vx, vy, vw, vh, inOutPos, inOutScale);
            }
        }
    }

    public void renderToTexture(int pixelW, int pixelH, Matrix4f viewProj, Vector3f cameraPos, Vector3f origin) {
        if (mode == Mode.NONE || viewProj == null || cameraPos == null || origin == null) {
            vertexCount = 0;
            return;
        }
        if (pixelW <= 0 || pixelH <= 0) {
            vertexCount = 0;
            return;
        }

        // IMPORTANT: preserve split READ/DRAW bindings for host apps (e.g., Minecraft).
        int prevDrawFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int prevProgram = glGetInteger(GL_CURRENT_PROGRAM);
        int prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
        boolean prevCullFace = glIsEnabled(GL_CULL_FACE);
        boolean prevScissor = glIsEnabled(GL_SCISSOR_TEST);
        boolean prevDepthTest = glIsEnabled(GL_DEPTH_TEST);
        boolean prevBlend = glIsEnabled(GL_BLEND);
        int prevBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB);
        int prevBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB);
        int prevBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
        int prevBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);
        boolean prevDepthMask = glGetBoolean(GL_DEPTH_WRITEMASK);
        int prevViewportX;
        int prevViewportY;
        int prevViewportW;
        int prevViewportH;
        {
            IntBuffer vp = BufferUtils.createIntBuffer(4);
            glGetIntegerv(GL_VIEWPORT, vp);
            prevViewportX = vp.get(0);
            prevViewportY = vp.get(1);
            prevViewportW = vp.get(2);
            prevViewportH = vp.get(3);
        }

        try {
            framebuffer.ensureSize(pixelW, pixelH);
            framebuffer.bind();
            glViewport(0, 0, pixelW, pixelH);

            glDisable(GL_CULL_FACE);
            glDisable(GL_SCISSOR_TEST);
            glDisable(GL_DEPTH_TEST);
            glDepthMask(false);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            FloatBuffer buf = buildGizmoBuffer(viewProj, cameraPos, pixelW, pixelH, origin);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            if (buf.capacity() > capacityFloats) {
                capacityFloats = buf.capacity();
                glBufferData(GL_ARRAY_BUFFER, (long) capacityFloats * Float.BYTES, GL_DYNAMIC_DRAW);
            }
            glBufferSubData(GL_ARRAY_BUFFER, 0, buf);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            shader.bind();
            shader.setUniform("uMvp", viewProj);

            glBindVertexArray(vao);
            // Pass 1: "behind" (dim), no depth test.
            shader.setUniform("uAlpha", 0.30f);
            shader.setUniform("uColorMul", 0.65f);
            glDisable(GL_DEPTH_TEST);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);

            // Pass 2: "in front", depth test enabled.
            shader.setUniform("uAlpha", 1.00f);
            shader.setUniform("uColorMul", 1.00f);
            glEnable(GL_DEPTH_TEST);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
            glBindVertexArray(0);

            shader.unbind();
        } finally {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, prevReadFbo);
            glViewport(prevViewportX, prevViewportY, prevViewportW, prevViewportH);

            if (prevCullFace) {
                glEnable(GL_CULL_FACE);
            } else {
                glDisable(GL_CULL_FACE);
            }
            if (prevScissor) {
                glEnable(GL_SCISSOR_TEST);
            } else {
                glDisable(GL_SCISSOR_TEST);
            }
            if (prevDepthTest) {
                glEnable(GL_DEPTH_TEST);
            } else {
                glDisable(GL_DEPTH_TEST);
            }
            glDepthMask(prevDepthMask);

            if (prevBlend) {
                glEnable(GL_BLEND);
            } else {
                glDisable(GL_BLEND);
            }
            glBlendFuncSeparate(prevBlendSrcRgb, prevBlendDstRgb, prevBlendSrcAlpha, prevBlendDstAlpha);

            glUseProgram(prevProgram);
            glBindVertexArray(prevVao);
            glBindBuffer(GL_ARRAY_BUFFER, prevArrayBuffer);
        }
    }

    private FloatBuffer buildGizmoBuffer(Matrix4f viewProj, Vector3f cameraPos, int pixelW, int pixelH, Vector3f origin) {
        syncGizmoSpace();
        int initialVerts = switch (mode) {
            case TRANSLATE -> 512;
            case SCALE -> 512;
            case ROTATE -> 3 * Math.max(12, rotateGizmo.style().rotateSegments) * 6;
            case COMPOSITE -> 1536;
            case NONE -> 0;
        };

        if (initialVerts <= 0) {
            vertexCount = 0;
            if (scratch == null) {
                scratch = BufferUtils.createFloatBuffer(1);
            }
            scratch.limit(0);
            return scratch;
        }

        int initialFloats = Math.max(1, initialVerts * 6);
        if (scratch == null || scratch.capacity() < initialFloats) {
            scratch = BufferUtils.createFloatBuffer(initialFloats);
        }

        for (int attempt = 0; attempt < 8; attempt++) {
            scratch.clear();
            try {
                switch (mode) {
                    case TRANSLATE -> translateGizmo.writeTriangles(scratch, viewProj, cameraPos, 0, 0, pixelW, pixelH, origin);
                    case SCALE -> scaleGizmo.writeTriangles(scratch, viewProj, cameraPos, 0, 0, pixelW, pixelH, origin);
                    case ROTATE -> rotateGizmo.writeTriangles(scratch, viewProj, cameraPos, 0, 0, pixelW, pixelH, origin);
                    case COMPOSITE -> {
                        translateGizmo.writeTriangles(scratch, viewProj, cameraPos, 0, 0, pixelW, pixelH, origin);
                        rotateGizmo.writeTriangles(scratch, viewProj, cameraPos, 0, 0, pixelW, pixelH, origin);
                        scaleGizmo.writeTriangles(scratch, viewProj, cameraPos, 0, 0, pixelW, pixelH, origin);
                    }
                    case NONE -> {
                    }
                }
                scratch.flip();
                vertexCount = scratch.remaining() / 6;
                return scratch;
            } catch (java.nio.BufferOverflowException ignored) {
                int nextCapacity = Math.max(scratch.capacity() * 2, scratch.capacity() + 2048);
                scratch = BufferUtils.createFloatBuffer(nextCapacity);
            }
        }

        vertexCount = 0;
        scratch.limit(0);
        return scratch;
    }

    @Override
    public void close() {
        shader.close();
        framebuffer.close();
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }
}
