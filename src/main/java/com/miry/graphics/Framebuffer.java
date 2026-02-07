package com.miry.graphics;

import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8;
import static org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDeleteRenderbuffers;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;
import static org.lwjgl.opengl.GL30.glViewport;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glDrawBuffer;

/**
 * Small OpenGL framebuffer wrapper with a color texture attachment and depth-stencil renderbuffer.
 */
public final class Framebuffer implements AutoCloseable {
    private final int fbo;
    private final int rbo;
    private final Texture color;
    private int width;
    private int height;

    public Framebuffer() {
        fbo = glGenFramebuffers();
        rbo = glGenRenderbuffers();
        color = new Texture();
        color.setFilteringLinear();
    }

    public void ensureSize(int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (w == this.width && h == this.height) {
            return;
        }
        this.width = w;
        this.height = h;

        color.allocateRgba(w, h);

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color.id(), 0);

        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, w, h);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);

        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer incomplete: status=" + status);
        }
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, width, height);
    }

    public static void unbind(int viewportWidth, int viewportHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, Math.max(1, viewportWidth), Math.max(1, viewportHeight));
    }

    public Texture colorTexture() {
        return color;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public void close() {
        color.close();
        glDeleteRenderbuffers(rbo);
        glDeleteFramebuffers(fbo);
    }
}
