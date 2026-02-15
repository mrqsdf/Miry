package com.miry.graphics.post;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Fullscreen quad mesh: position.xy, uv.xy.
 */
public final class FullscreenQuad implements AutoCloseable {
    private final int vao;
    private final int vbo;

    public FullscreenQuad() {
        int prevVao = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING);
        int prevArrayBuffer = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER_BINDING);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        FloatBuffer vb = BufferUtils.createFloatBuffer(6 * 4);
        // Two triangles.
        put(vb, -1, -1, 0, 0);
        put(vb, 1, -1, 1, 0);
        put(vb, 1, 1, 1, 1);
        put(vb, -1, -1, 0, 0);
        put(vb, 1, 1, 1, 1);
        put(vb, -1, 1, 0, 1);
        vb.flip();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);

        int stride = 4 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2L * Float.BYTES);

        glBindBuffer(GL_ARRAY_BUFFER, prevArrayBuffer);
        glBindVertexArray(prevVao);
    }

    private static void put(FloatBuffer fb, float x, float y, float u, float v) {
        fb.put(x).put(y).put(u).put(v);
    }

    public void bind() {
        glBindVertexArray(vao);
    }

    public void unbind() {
        glBindVertexArray(0);
    }

    @Override
    public void close() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
