package com.miry.graphics.batch;

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

/**
 * 2D sprite-batch renderer used as the {@link com.miry.ui.render.UiRenderer} backend.
 * <p>
 * Supports colored rects, textured quads, clip rectangles (scissor), and text rendering via {@link TextRenderer}.
 */
public final class BatchRenderer implements AutoCloseable, com.miry.ui.render.UiRenderer {
    private static final int FLOATS_PER_VERTEX = 8; // x,y,u,v,r,g,b,a
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
    private int framebufferHeight;

    private final Deque<ClipRect> clipStack = new ArrayDeque<>();
    private ByteBuffer easyFontBuffer = org.lwjgl.BufferUtils.createByteBuffer(16 * 1024);

    public BatchRenderer(int maxQuads) {
        if (maxQuads <= 0) {
            throw new IllegalArgumentException("maxQuads must be > 0");
        }
        this.maxQuads = maxQuads;

        vertexBuffer = BufferUtils.createFloatBuffer(maxQuads * VERTICES_PER_QUAD * FLOATS_PER_VERTEX);
        indexBuffer = BufferUtils.createIntBuffer(maxQuads * INDICES_PER_QUAD);

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

        glBindVertexArray(0);

        shader = Shader.fromResources("shaders/batch.vert", "shaders/batch.frag");
        whiteTexture = Texture.white1x1();
        currentTexture = whiteTexture;
    }

    public void begin(int windowWidth, int windowHeight, float scaleFactor) {
        if (drawing) {
            throw new IllegalStateException("Already drawing; call end() first.");
        }
        drawing = true;
        this.scaleFactor = Math.max(0.1f, scaleFactor);
        this.windowHeight = Math.max(1, windowHeight);
        this.framebufferHeight = queryViewportHeight();
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
    }

    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
    }

    public void end() {
        flush();
        shader.unbind();
        glDisable(GL_BLEND);
        glDisable(GL_SCISSOR_TEST);
        drawing = false;
    }

    public void flush() {
        if (!drawing || quadCount == 0) {
            vertexBuffer.clear();
            indexBuffer.clear();
            quadCount = 0;
            return;
        }

        vertexBuffer.flip();
        indexBuffer.flip();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indexBuffer);

        currentTexture.bind(0);
        glDrawElements(GL11.GL_TRIANGLES, indexBuffer.limit(), GL11.GL_UNSIGNED_INT, 0L);

        glBindVertexArray(0);

        vertexBuffer.clear();
        indexBuffer.clear();
        quadCount = 0;
    }

    public void drawRect(float x, float y, float w, float h, int argb) {
        drawTexturedRect(whiteTexture, x, y, w, h, 0.0f, 0.0f, 1.0f, 1.0f, argb);
    }

    @Override
    public void drawText(String text, float x, float y, int argb) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (textRenderer != null) {
            // Keep `UiRenderer.drawText` semantics as "y is baseline" to match existing widgets.
            float topY = y - textRenderer.atlas().ascent();
            textRenderer.drawText(this, text, x, topY, argb);
            return;
        }

        // Ensure current texture is white (solid color quads).
        if (currentTexture != whiteTexture && quadCount > 0) {
            flush();
        }
        currentTexture = whiteTexture;

        int needed = text.length() * 270; // generous upper bound for STBEasyFont
        if (easyFontBuffer.capacity() < needed) {
            easyFontBuffer = org.lwjgl.BufferUtils.createByteBuffer(Math.max(needed, easyFontBuffer.capacity() * 2));
        }
        easyFontBuffer.clear();

        int quads = STBEasyFont.stb_easy_font_print(x, y - 14.0f, text, null, easyFontBuffer);
        // Each quad = 4 vertices, each vertex = 16 bytes (x,y,z + padding) in stb_easy_font.
        // LWJGL stores as 16-byte stride; we read x,y at offsets 0,4.
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
        putQuadVertices(x, y, w, h, u0, v0, u1, v1, argb);

        indexBuffer.put(baseVertex);
        indexBuffer.put(baseVertex + 1);
        indexBuffer.put(baseVertex + 2);
        indexBuffer.put(baseVertex + 2);
        indexBuffer.put(baseVertex + 3);
        indexBuffer.put(baseVertex);

        quadCount++;
    }

    private void putQuadVertices(float x, float y, float w, float h, float u0, float v0, float u1, float v1, int argb) {
        float r = ((argb >> 16) & 0xff) / 255.0f;
        float g = ((argb >> 8) & 0xff) / 255.0f;
        float b = (argb & 0xff) / 255.0f;
        float a = ((argb >>> 24) & 0xff) / 255.0f;

        // TL
        vertexBuffer.put(x).put(y).put(u0).put(v0).put(r).put(g).put(b).put(a);
        // TR
        vertexBuffer.put(x + w).put(y).put(u1).put(v0).put(r).put(g).put(b).put(a);
        // BR
        vertexBuffer.put(x + w).put(y + h).put(u1).put(v1).put(r).put(g).put(b).put(a);
        // BL
        vertexBuffer.put(x).put(y + h).put(u0).put(v1).put(r).put(g).put(b).put(a);
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

        // Clamp to current viewport height to avoid undefined behavior.
        if (framebufferHeight > 0) {
            sy = clamp(sy, 0, framebufferHeight);
            sh = clamp(sh, 0, framebufferHeight - sy);
        }
        return new ClipRect(sx, sy, sw, sh);
    }

    private int queryViewportHeight() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer vp = stack.mallocInt(4);
            glGetIntegerv(GL11.GL_VIEWPORT, vp);
            return vp.get(3);
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
