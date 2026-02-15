package com.miry.graphics.batch;

import com.miry.core.MiryDebug;
import com.miry.graphics.Shader;
import com.miry.graphics.Texture;
import com.miry.ui.font.TextRenderer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.stb.STBEasyFont;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glGetIntegerv;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL11.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER_BINDING;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_BOX;
import static org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB;
import static org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB;
import static org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA;
import static org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA;

/**
 * 2D sprite-batch renderer used as the {@link com.miry.ui.render.UiRenderer} backend.
 * <p>
 * Supports colored rects, textured quads, clip rectangles (scissor), and text rendering via {@link TextRenderer}.
 */
public final class BatchRenderer implements AutoCloseable, com.miry.ui.render.UiRenderer {
    // x,y,u,v,fillRGBA,strokeRGBA,sizeX,sizeY,radiusPx,borderPx,gradRGBA,gradMode,p1,p2,p3,extra4
    private static final int FLOATS_PER_VERTEX = 28;
    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;

    private final int maxQuads;
    private final FloatBuffer vertexBuffer;
    private final IntBuffer indexBuffer;

    private final int vao;
    private final int vbo;
    private final int ebo;

    private final Shader shader;
    private final Texture whiteTexture;
    private TextRenderer textRenderer;

    private int quadCount;
    private Texture currentTexture;
    private boolean drawing;
    private float scaleFactor = 1.0f;
    private int windowHeight;
    private int framebufferWidth;
    private int framebufferHeight;

    private final Deque<ClipRect> clipStack = new ArrayDeque<>();
    private ByteBuffer easyFontBuffer = org.lwjgl.BufferUtils.createByteBuffer(16 * 1024);
    private final GlStateSnapshot glState = new GlStateSnapshot();

    public BatchRenderer(int maxQuads) {
        if (maxQuads <= 0) {
            throw new IllegalArgumentException("maxQuads must be > 0");
        }
        this.maxQuads = maxQuads;

        vertexBuffer = BufferUtils.createFloatBuffer(maxQuads * VERTICES_PER_QUAD * FLOATS_PER_VERTEX);
        indexBuffer = BufferUtils.createIntBuffer(maxQuads * INDICES_PER_QUAD);

        int prevVao = GL11.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuffer = GL11.glGetInteger(GL_ARRAY_BUFFER_BINDING);
        try {
            vao = glGenVertexArrays();
            vbo = glGenBuffers();
            ebo = glGenBuffers();

            glBindVertexArray(vao);

            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, (long) vertexBuffer.capacity() * Float.BYTES, GL_DYNAMIC_DRAW);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long) indexBuffer.capacity() * Integer.BYTES, GL_DYNAMIC_DRAW);

            int stride = FLOATS_PER_VERTEX * Float.BYTES;
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0L);

            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2L * Float.BYTES);

            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, stride, 4L * Float.BYTES);

            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, stride, 8L * Float.BYTES);

            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, stride, 12L * Float.BYTES);

            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 4, GL11.GL_FLOAT, false, stride, 16L * Float.BYTES);

            GL20.glEnableVertexAttribArray(6);
            GL20.glVertexAttribPointer(6, 4, GL11.GL_FLOAT, false, stride, 20L * Float.BYTES);

            GL20.glEnableVertexAttribArray(7);
            GL20.glVertexAttribPointer(7, 4, GL11.GL_FLOAT, false, stride, 24L * Float.BYTES);
        } finally {
            glBindBuffer(GL_ARRAY_BUFFER, prevArrayBuffer);
            glBindVertexArray(prevVao);
        }

        shader = Shader.fromResources("shaders/batch.vert", "shaders/batch.frag");
        whiteTexture = Texture.white1x1();
        currentTexture = whiteTexture;
    }

    public void begin(int windowWidth, int windowHeight, float scaleFactor) {
        if (drawing) {
            throw new IllegalStateException("Already drawing; call end() first.");
        }
        long t0 = MiryDebug.nowNs();
        drawing = true;
        glState.capture();
        // Ensure a VAO is active for core-profile safety in embedded hosts
        glBindVertexArray(vao);

        this.scaleFactor = Math.max(0.1f, scaleFactor);
        this.windowHeight = Math.max(1, windowHeight);
        int[] vp = queryViewportSize();
        this.framebufferWidth = vp[0];
        this.framebufferHeight = vp[1];
        clipStack.clear();
        glDisable(GL_SCISSOR_TEST);

        quadCount = 0;
        vertexBuffer.clear();
        indexBuffer.clear();
        currentTexture = whiteTexture;

        shader.bind();
        shader.setUniform("uTexture", 0);
        Matrix4f projection = new Matrix4f().ortho(0.0f, windowWidth, windowHeight, 0.0f, -1.0f, 1.0f);
        shader.setUniform("uProjection", projection);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        MiryDebug.logIfSlow("BatchRenderer.begin", t0);
    }

    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    public void end() {
        long t0 = MiryDebug.nowNs();
        flush();
        glState.restore();
        drawing = false;
        MiryDebug.logIfSlow("BatchRenderer.end", t0);
    }

    public void flush() {
        long t0 = MiryDebug.nowNs();
        if (!drawing || quadCount == 0) {
            vertexBuffer.clear();
            indexBuffer.clear();
            quadCount = 0;
            return;
        }
        int quads = quadCount;

        vertexBuffer.flip();
        indexBuffer.flip();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indexBuffer);

        currentTexture.bind(0);
        glDrawElements(GL11.GL_TRIANGLES, indexBuffer.limit(), GL11.GL_UNSIGNED_INT, 0L);

        vertexBuffer.clear();
        indexBuffer.clear();
        quadCount = 0;
        MiryDebug.logIfSlow("BatchRenderer.flush (quads=" + quads + ")", t0);
    }

    public void drawRect(float x, float y, float w, float h, int argb) {
        drawTexturedRect(whiteTexture, x, y, w, h, 0.0f, 0.0f, 1.0f, 1.0f, argb);
    }

    @Override
    public void drawTriangle(float ax, float ay, float bx, float by, float cx, float cy, int argb) {
        if (!drawing) {
            throw new IllegalStateException("Call begin() before drawing.");
        }
        if (currentTexture != whiteTexture && quadCount > 0) {
            flush();
        }
        currentTexture = whiteTexture;

        if (quadCount >= maxQuads) {
            flush();
        }

        int baseVertex = quadCount * VERTICES_PER_QUAD;
        putVertex(ax, ay, 0.0f, 0.0f, argb, 0, 0.0f, 0.0f, 0.0f, 0.0f, argb, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0, 0, 0);
        putVertex(bx, by, 0.0f, 0.0f, argb, 0, 0.0f, 0.0f, 0.0f, 0.0f, argb, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0, 0, 0);
        putVertex(cx, cy, 0.0f, 0.0f, argb, 0, 0.0f, 0.0f, 0.0f, 0.0f, argb, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0, 0, 0);
        putVertex(ax, ay, 0.0f, 0.0f, argb, 0, 0.0f, 0.0f, 0.0f, 0.0f, argb, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0, 0, 0);

        indexBuffer.put(baseVertex);
        indexBuffer.put(baseVertex + 1);
        indexBuffer.put(baseVertex + 2);
        indexBuffer.put(baseVertex + 2);
        indexBuffer.put(baseVertex + 3);
        indexBuffer.put(baseVertex);
        quadCount++;
    }

    @Override
    public void drawRoundedRect(float x, float y, float w, float h, float radiusPx, int argb) {
        drawRoundedRect(x, y, w, h, radiusPx, argb, argb, argb, argb, 0.0f, 0);
    }

    @Override
    public void drawCircle(float cx, float cy, float radiusPx, int argb) {
        drawCircle(cx, cy, radiusPx, argb, 0.0f, 0);
    }

    @Override
    public void drawCircle(float cx, float cy, float radiusPx, int fillArgb, float borderPx, int strokeArgb) {
        float r = Math.max(0.0f, radiusPx);
        float d = r * 2.0f;
        float x = cx - r;
        float y = cy - r;
        // mode=10 => circle, solid
        drawQuad(whiteTexture, x, y, d, d, 0.0f, 0.0f, 1.0f, 1.0f,
            fillArgb, fillArgb, fillArgb, fillArgb,
            strokeArgb, strokeArgb, strokeArgb, strokeArgb,
            d, d, r, borderPx,
            fillArgb, 10.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void drawCapsule(float ax, float ay, float bx, float by, float radiusPx, int argb) {
        drawCapsule(ax, ay, bx, by, radiusPx, argb, 0.0f, 0);
    }

    @Override
    public void drawCapsule(float ax,
                            float ay,
                            float bx,
                            float by,
                            float radiusPx,
                            int fillArgb,
                            float borderPx,
                            int strokeArgb) {
        float r = Math.max(0.0f, radiusPx);
        float minX = Math.min(ax, bx) - r;
        float minY = Math.min(ay, by) - r;
        float maxX = Math.max(ax, bx) + r;
        float maxY = Math.max(ay, by) + r;
        float w = Math.max(1e-3f, maxX - minX);
        float h = Math.max(1e-3f, maxY - minY);

        float cx = minX + w * 0.5f;
        float cy = minY + h * 0.5f;
        float axL = ax - cx;
        float ayL = ay - cy;
        float bxL = bx - cx;
        float byL = by - cy;

        // mode=20 => capsule, solid. vExtra carries segment endpoints in local px space.
        drawQuad(whiteTexture, minX, minY, w, h, 0.0f, 0.0f, 1.0f, 1.0f,
            fillArgb, fillArgb, fillArgb, fillArgb,
            strokeArgb, strokeArgb, strokeArgb, strokeArgb,
            w, h, r, borderPx,
            fillArgb, 20.0f, 0.0f, 0.0f, 0.0f,
            axL, ayL, bxL, byL);
    }

    @Override
    public void drawRoundedRect(float x,
                                float y,
                                float w,
                                float h,
                                float radiusPx,
                                int fillTL,
                                int fillTR,
                                int fillBR,
                                int fillBL) {
        drawRoundedRect(x, y, w, h, radiusPx, fillTL, fillTR, fillBR, fillBL, 0.0f, 0);
    }

    @Override
    public void drawRoundedRect(float x,
                                float y,
                                float w,
                                float h,
                                float radiusPx,
                                int fillTL,
                                int fillTR,
                                int fillBR,
                                int fillBL,
                                float borderPx,
                                int strokeArgb) {
        drawQuad(whiteTexture, x, y, w, h, 0.0f, 0.0f, 1.0f, 1.0f,
            fillTL, fillTR, fillBR, fillBL,
            strokeArgb, strokeArgb, strokeArgb, strokeArgb,
            w, h, radiusPx, borderPx,
            fillTL, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void drawLinearGradientRoundedRect(float x,
                                              float y,
                                              float w,
                                              float h,
                                              float radiusPx,
                                              int startArgb,
                                              int endArgb,
                                              float dirX,
                                              float dirY,
                                              float borderPx,
                                              int strokeArgb) {
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        float nx = len > 1e-6f ? (dirX / len) : 0.0f;
        float ny = len > 1e-6f ? (dirY / len) : 1.0f;
        // gradParams: mode=1, p1=dirX, p2=dirY, p3=bias(0)
        drawQuad(whiteTexture, x, y, w, h, 0.0f, 0.0f, 1.0f, 1.0f,
            startArgb, startArgb, startArgb, startArgb,
            strokeArgb, strokeArgb, strokeArgb, strokeArgb,
            w, h, radiusPx, borderPx,
            endArgb, 1.0f, nx, ny, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void drawRadialGradientRoundedRect(float x,
                                              float y,
                                              float w,
                                              float h,
                                              float radiusPx,
                                              int innerArgb,
                                              int outerArgb,
                                              float centerUx,
                                              float centerUy,
                                              float radiusU,
                                              float borderPx,
                                              int strokeArgb) {
        float cx = clamp01(centerUx);
        float cy = clamp01(centerUy);
        float r = Math.max(1e-4f, radiusU);
        // gradParams: mode=2, p1=cx, p2=cy, p3=r
        drawQuad(whiteTexture, x, y, w, h, 0.0f, 0.0f, 1.0f, 1.0f,
            innerArgb, innerArgb, innerArgb, innerArgb,
            strokeArgb, strokeArgb, strokeArgb, strokeArgb,
            w, h, radiusPx, borderPx,
            outerArgb, 2.0f, cx, cy, r,
            0.0f, 0.0f, 0.0f, 0.0f);
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    @Override
    public void drawText(String text, float x, float y, int argb) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (textRenderer != null) {
            float topY = y - textRenderer.atlas().ascent();
            textRenderer.drawText(this, text, x, topY, argb);
            return;
        }

        // Ensure current texture is white (solid color quads).
        if (currentTexture != whiteTexture && quadCount > 0) {
            flush();
        }
        currentTexture = whiteTexture;

        int maxUtf8Bytes;
        try {
            maxUtf8Bytes = Math.addExact(Math.multiplyExact(text.length(), 4), 1);
        } catch (ArithmeticException ex) {
            return;
        }
        int needed;
        try {
            needed = Math.multiplyExact(maxUtf8Bytes, 270);
        } catch (ArithmeticException ex) {
            return;
        }
        if (easyFontBuffer.capacity() < needed) {
            easyFontBuffer = org.lwjgl.BufferUtils.createByteBuffer(Math.max(needed, easyFontBuffer.capacity() * 2));
        }
        easyFontBuffer.clear();

        int quads = STBEasyFont.stb_easy_font_print(x, y - 14.0f, text, null, easyFontBuffer);
        int stride = 16;
        int vertCount = quads * 4;
        for (int i = 0; i < vertCount; i += 4) {
            float x0 = easyFontBuffer.getFloat((i + 0) * stride);
            float y0 = easyFontBuffer.getFloat((i + 0) * stride + 4);
            float x1 = easyFontBuffer.getFloat((i + 1) * stride);
            float y1 = easyFontBuffer.getFloat((i + 2) * stride + 4);
            float w = x1 - x0;
            float h = y1 - y0;
            drawRect(x0, y0, w, h, argb);
        }
    }

    @Override
    public float measureText(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        if (textRenderer != null) {
            return textRenderer.measureText(text);
        }
        return text.length() * 8.0f;
    }

    @Override
    public float lineHeight() {
        if (textRenderer != null) {
            return textRenderer.atlas().lineHeight();
        }
        return 16.0f;
    }

    @Override
    public float ascent() {
        if (textRenderer != null) {
            return textRenderer.atlas().ascent();
        }
        return 12.0f;
    }

    @Override
    public void pushClip(int x, int y, int width, int height) {
        if (!drawing) {
            throw new IllegalStateException("Call begin() before pushClip().");
        }
        flush();
        ClipRect next = toFramebufferClip(x, y, width, height);
        ClipRect applied = clipStack.isEmpty() ? next : clipStack.peek().intersect(next);
        clipStack.push(applied);
        applyClip(applied);
    }

    @Override
    public void popClip() {
        if (!drawing) {
            throw new IllegalStateException("Call begin() before popClip().");
        }
        flush();
        if (clipStack.isEmpty()) {
            glDisable(GL_SCISSOR_TEST);
            return;
        }
        clipStack.pop();
        if (clipStack.isEmpty()) {
            glDisable(GL_SCISSOR_TEST);
        } else {
            applyClip(clipStack.peek());
        }
    }

    @Override
    public void drawTexturedRect(Texture texture, float x, float y, float w, float h, int argb) {
        drawTexturedRect(texture, x, y, w, h, 0.0f, 0.0f, 1.0f, 1.0f, argb);
    }

    @Override
    public void drawTexturedRect(Texture texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int argb) {
        drawQuad(texture, x, y, w, h, u0, v0, u1, v1,
            argb, argb, argb, argb,
            0, 0, 0, 0,
            w, h, 0.0f, 0.0f,
            argb, 0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Draws a bitmap-coverage textured quad with slight alpha shaping tuned for UI text on dark backgrounds.
     * <p>
     * This is used by the default (non-SDF) font atlas path.
     */
    public void drawCoverageTexturedRect(Texture texture,
                                         float x,
                                         float y,
                                         float w,
                                         float h,
                                         float u0,
                                         float v0,
                                         float u1,
                                         float v1,
                                         int argb) {
        drawQuad(texture, x, y, w, h, u0, v0, u1, v1,
            argb, argb, argb, argb,
            0, 0, 0, 0,
            w, h, 0.0f, 0.0f,
            argb, -2.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f);
    }

    /**
     * Draws a single-channel SDF textured quad (used for SDF font rendering).
     * <p>
     * The batch shader interprets the sampled alpha as a signed distance with edge at 0.5.
     */
    public void drawSdfTexturedRect(Texture texture,
                                    float x,
                                    float y,
                                    float w,
                                    float h,
                                    float u0,
                                    float v0,
                                    float u1,
                                    float v1,
                                    int argb) {
        drawQuad(texture, x, y, w, h, u0, v0, u1, v1,
            argb, argb, argb, argb,
            0, 0, 0, 0,
            w, h, 0.0f, 0.0f,
            argb, -1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f);
    }

    private void drawQuad(Texture texture,
                          float x,
                          float y,
                          float w,
                          float h,
                          float u0,
                          float v0,
                          float u1,
                          float v1,
                          int fillTL,
                          int fillTR,
                          int fillBR,
                          int fillBL,
                          int strokeTL,
                          int strokeTR,
                          int strokeBR,
                          int strokeBL,
                          float sizeX,
                          float sizeY,
                          float radiusPx,
                          float borderPx,
                          int gradArgb,
                          float gradMode,
                          float gradP1,
                          float gradP2,
                          float gradP3,
                          float extra0,
                          float extra1,
                          float extra2,
                          float extra3) {
        if (!drawing) {
            throw new IllegalStateException("Call begin() before drawing.");
        }
        if (texture == null) {
            texture = whiteTexture;
        }
        if (texture != currentTexture && quadCount > 0) {
            flush();
        }
        currentTexture = texture;

        if (quadCount >= maxQuads) {
            flush();
        }

        int baseVertex = quadCount * VERTICES_PER_QUAD;
        putQuadVertices(x, y, w, h, u0, v0, u1, v1,
            fillTL, fillTR, fillBR, fillBL,
            strokeTL, strokeTR, strokeBR, strokeBL,
            sizeX, sizeY, radiusPx, borderPx,
            gradArgb, gradMode, gradP1, gradP2, gradP3,
            extra0, extra1, extra2, extra3);

        indexBuffer.put(baseVertex);
        indexBuffer.put(baseVertex + 1);
        indexBuffer.put(baseVertex + 2);
        indexBuffer.put(baseVertex + 2);
        indexBuffer.put(baseVertex + 3);
        indexBuffer.put(baseVertex);

        quadCount++;
    }

    private void putQuadVertices(float x,
                                 float y,
                                 float w,
                                 float h,
                                 float u0,
                                 float v0,
                                 float u1,
                                 float v1,
                                 int fillTL,
                                 int fillTR,
                                 int fillBR,
                                 int fillBL,
                                 int strokeTL,
                                 int strokeTR,
                                 int strokeBR,
                                 int strokeBL,
                                 float sizeX,
                                 float sizeY,
                                 float radiusPx,
                                 float borderPx,
                                 int gradArgb,
                                 float gradMode,
                                 float gradP1,
                                 float gradP2,
                                 float gradP3,
                                 float extra0,
                                 float extra1,
                                 float extra2,
                                 float extra3) {
        // TL
        putVertex(x, y, u0, v0, fillTL, strokeTL, sizeX, sizeY, radiusPx, borderPx, gradArgb, gradMode, gradP1, gradP2, gradP3, extra0, extra1, extra2, extra3);
        // TR
        putVertex(x + w, y, u1, v0, fillTR, strokeTR, sizeX, sizeY, radiusPx, borderPx, gradArgb, gradMode, gradP1, gradP2, gradP3, extra0, extra1, extra2, extra3);
        // BR
        putVertex(x + w, y + h, u1, v1, fillBR, strokeBR, sizeX, sizeY, radiusPx, borderPx, gradArgb, gradMode, gradP1, gradP2, gradP3, extra0, extra1, extra2, extra3);
        // BL
        putVertex(x, y + h, u0, v1, fillBL, strokeBL, sizeX, sizeY, radiusPx, borderPx, gradArgb, gradMode, gradP1, gradP2, gradP3, extra0, extra1, extra2, extra3);
    }

    private void putVertex(float x,
                           float y,
                           float u,
                           float v,
                           int fillArgb,
                           int strokeArgb,
                           float sizeX,
                           float sizeY,
                           float radiusPx,
                           float borderPx,
                           int gradArgb,
                           float gradMode,
                           float gradP1,
                           float gradP2,
                           float gradP3,
                           float extra0,
                           float extra1,
                           float extra2,
                           float extra3) {
        float fr = ((fillArgb >> 16) & 0xff) / 255.0f;
        float fg = ((fillArgb >> 8) & 0xff) / 255.0f;
        float fb = (fillArgb & 0xff) / 255.0f;
        float fa = ((fillArgb >>> 24) & 0xff) / 255.0f;

        float sr = ((strokeArgb >> 16) & 0xff) / 255.0f;
        float sg = ((strokeArgb >> 8) & 0xff) / 255.0f;
        float sb = (strokeArgb & 0xff) / 255.0f;
        float sa = ((strokeArgb >>> 24) & 0xff) / 255.0f;

        float gr = ((gradArgb >> 16) & 0xff) / 255.0f;
        float gg = ((gradArgb >> 8) & 0xff) / 255.0f;
        float gb = (gradArgb & 0xff) / 255.0f;
        float ga = ((gradArgb >>> 24) & 0xff) / 255.0f;

        vertexBuffer.put(x).put(y).put(u).put(v);
        vertexBuffer.put(fr).put(fg).put(fb).put(fa);
        vertexBuffer.put(sr).put(sg).put(sb).put(sa);
        vertexBuffer.put(sizeX).put(sizeY).put(radiusPx).put(borderPx);
        vertexBuffer.put(gr).put(gg).put(gb).put(ga);
        vertexBuffer.put(gradMode).put(gradP1).put(gradP2).put(gradP3);
        vertexBuffer.put(extra0).put(extra1).put(extra2).put(extra3);
    }

    @Override
    public void close() {
        whiteTexture.close();
        shader.close();
        GL15.glDeleteBuffers(vbo);
        GL15.glDeleteBuffers(ebo);
        GL30.glDeleteVertexArrays(vao);
    }

    private void applyClip(ClipRect clip) {
        glEnable(GL_SCISSOR_TEST);
        glScissor(clip.x, clip.y, clip.width, clip.height);
    }

    private ClipRect toFramebufferClip(int x, int y, int width, int height) {
        int sx = Math.round(x * scaleFactor);
        int sy = Math.round((windowHeight - (y + height)) * scaleFactor);
        int sw = Math.round(width * scaleFactor);
        int sh = Math.round(height * scaleFactor);

        sx = clamp(sx, 0, Integer.MAX_VALUE);
        sy = clamp(sy, 0, Integer.MAX_VALUE);
        sw = Math.max(0, sw);
        sh = Math.max(0, sh);

        // Clamp to current viewport size to avoid undefined behavior and accidental full clipping.
        if (framebufferWidth > 0) {
            sx = clamp(sx, 0, framebufferWidth);
            sw = clamp(sw, 0, framebufferWidth - sx);
        }
        if (framebufferHeight > 0) {
            sy = clamp(sy, 0, framebufferHeight);
            sh = clamp(sh, 0, framebufferHeight - sy);
        }
        return new ClipRect(sx, sy, sw, sh);
    }

    private int[] queryViewportSize() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer vp = stack.mallocInt(4);
            glGetIntegerv(GL11.GL_VIEWPORT, vp);
            return new int[]{vp.get(2), vp.get(3)};
        }
    }

    private static final class GlStateSnapshot {
        private int prevProgram;
        private int prevVao;
        private int prevArrayBuffer;
        private int prevElementArrayBuffer;
        private int prevActiveTexture;
        private int prevTexture0;
        private boolean prevBlendEnabled;
        private boolean prevScissorEnabled;
        private boolean prevDepthTestEnabled;
        private int prevViewportX;
        private int prevViewportY;
        private int prevViewportW;
        private int prevViewportH;
        private int prevBlendSrcRgb;
        private int prevBlendDstRgb;
        private int prevBlendSrcAlpha;
        private int prevBlendDstAlpha;
        private int prevScissorX;
        private int prevScissorY;
        private int prevScissorW;
        private int prevScissorH;

        void capture() {
            prevProgram = GL11.glGetInteger(GL_CURRENT_PROGRAM);
            prevVao = GL11.glGetInteger(GL_VERTEX_ARRAY_BINDING);
            prevArrayBuffer = GL11.glGetInteger(GL_ARRAY_BUFFER_BINDING);
            prevElementArrayBuffer = GL11.glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);

            prevBlendEnabled = glIsEnabled(GL_BLEND);
            prevScissorEnabled = glIsEnabled(GL_SCISSOR_TEST);
            prevDepthTestEnabled = glIsEnabled(GL_DEPTH_TEST);

            prevBlendSrcRgb = GL11.glGetInteger(GL_BLEND_SRC_RGB);
            prevBlendDstRgb = GL11.glGetInteger(GL_BLEND_DST_RGB);
            prevBlendSrcAlpha = GL11.glGetInteger(GL_BLEND_SRC_ALPHA);
            prevBlendDstAlpha = GL11.glGetInteger(GL_BLEND_DST_ALPHA);

            prevViewportX = 0;
            prevViewportY = 0;
            prevViewportW = 0;
            prevViewportH = 0;
            prevScissorX = 0;
            prevScissorY = 0;
            prevScissorW = 0;
            prevScissorH = 0;
            try (MemoryStack stack = stackPush()) {
                IntBuffer sb = stack.mallocInt(4);
                glGetIntegerv(GL_SCISSOR_BOX, sb);
                prevScissorX = sb.get(0);
                prevScissorY = sb.get(1);
                prevScissorW = sb.get(2);
                prevScissorH = sb.get(3);

                IntBuffer vp = stack.mallocInt(4);
                glGetIntegerv(GL_VIEWPORT, vp);
                prevViewportX = vp.get(0);
                prevViewportY = vp.get(1);
                prevViewportW = vp.get(2);
                prevViewportH = vp.get(3);
            }

            prevActiveTexture = GL11.glGetInteger(GL_ACTIVE_TEXTURE);
            glActiveTexture(GL_TEXTURE0);
            prevTexture0 = GL11.glGetInteger(GL_TEXTURE_BINDING_2D);
            glActiveTexture(prevActiveTexture);
        }

        void restore() {
            glViewport(prevViewportX, prevViewportY, prevViewportW, prevViewportH);

            if (prevDepthTestEnabled) {
                glEnable(GL_DEPTH_TEST);
            } else {
                glDisable(GL_DEPTH_TEST);
            }

            if (prevBlendEnabled) {
                glEnable(GL_BLEND);
            } else {
                glDisable(GL_BLEND);
            }
            glBlendFuncSeparate(prevBlendSrcRgb, prevBlendDstRgb, prevBlendSrcAlpha, prevBlendDstAlpha);

            if (prevScissorEnabled) {
                glEnable(GL_SCISSOR_TEST);
                glScissor(prevScissorX, prevScissorY, prevScissorW, prevScissorH);
            } else {
                glDisable(GL_SCISSOR_TEST);
            }

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, prevTexture0);
            glActiveTexture(prevActiveTexture);

            GL20.glUseProgram(prevProgram);
            // Always restore the host VAO binding. Leaving Miry's VAO bound when the host expects 0 (or a different VAO)
            // can desync the ELEMENT_ARRAY_BUFFER binding and crash drivers on subsequent glDrawElements calls.
            glBindVertexArray(prevVao);
            glBindBuffer(GL_ARRAY_BUFFER, prevArrayBuffer);
            // In core profile, GL_ELEMENT_ARRAY_BUFFER binding is VAO state; restoring it while no VAO is bound is invalid.
            if (prevVao != 0) {
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, prevElementArrayBuffer);
            }
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private record ClipRect(int x, int y, int width, int height) {
        ClipRect intersect(ClipRect other) {
            int nx = Math.max(x, other.x);
            int ny = Math.max(y, other.y);
            int nr = Math.min(x + width, other.x + other.width);
            int nb = Math.min(y + height, other.y + other.height);
            return new ClipRect(nx, ny, Math.max(0, nr - nx), Math.max(0, nb - ny));
        }
    }
}
