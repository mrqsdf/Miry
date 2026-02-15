package com.miry.graphics;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;

import java.nio.IntBuffer;

/**
 * OpenGL framebuffer wrapper with a color texture attachment and depth-stencil renderbuffer.
 * <p>
 * This class simplifies the creation and management of Framebuffer Objects (FBOs) for offscreen rendering.
 * It automatically handles resizing the attachments when {@link #ensureSize(int, int)} is called.
 * </p>
 */
public final class Framebuffer implements AutoCloseable {
    private final int fbo;
    private final int rbo;
    private final Texture color;
    private int width;
    private int height;

    /**
     * Creates a new Framebuffer.
     * <p>
     * The underlying OpenGL resources (FBO, RBO, and Texture) are generated immediately.
     * The initial size is undefined until {@link #ensureSize(int, int)} is called.
     * </p>
     */
    public Framebuffer() {
        fbo = glGenFramebuffers();
        rbo = glGenRenderbuffers();
        color = new Texture();
        color.setFilteringLinear();
    }

    /**
     * Resizes the framebuffer attachments if the requested dimensions differ from the current ones.
     * <p>
     * This method reallocates the color texture and depth-stencil renderbuffer to match the new size.
     * It binds the framebuffer to perform these operations and restores the previous binding afterwards.
     * </p>
     *
     * @param width  The desired width in pixels.
     * @param height The desired height in pixels.
     * @throws IllegalStateException if the framebuffer is not complete after resizing.
     */
    public void ensureSize(int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (w == this.width && h == this.height) {
            return;
        }
        this.width = w;
        this.height = h;

        // IMPORTANT: preserve split READ/DRAW bindings for host apps (e.g., Minecraft).
        int prevDrawFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int prevRbo = glGetInteger(GL_RENDERBUFFER_BINDING);

        color.allocateRgba(w, h);

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color.id(), 0);

        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, w, h);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);

        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        glBindRenderbuffer(GL_RENDERBUFFER, prevRbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDrawFbo);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, prevReadFbo);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer incomplete: status=" + status);
        }
    }

    /**
     * Binds this framebuffer for rendering.
     * <p>
     * Sets the OpenGL viewport to match the framebuffer's dimensions.
     * </p>
     */
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, width, height);
    }

    /**
     * Binds this framebuffer and returns a scope that restores the previously bound framebuffer + viewport on close.
     * <p>
     * This is the preferred API when embedding into a host application that manages its own FBOs (e.g., Minecraft),
     * ensuring that the host's state is preserved.
     * </p>
     *
     * @return A {@link Binding} object that restores state when closed.
     */
    public Binding bindScoped() {
        // IMPORTANT: preserve split READ/DRAW bindings for host apps (e.g., Minecraft).
        int prevDrawFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int prevViewportX = 0;
        int prevViewportY = 0;
        int prevViewportW = 0;
        int prevViewportH = 0;
        try (var stack = stackPush()) {
            IntBuffer vp = stack.mallocInt(4);
            glGetIntegerv(GL_VIEWPORT, vp);
            prevViewportX = vp.get(0);
            prevViewportY = vp.get(1);
            prevViewportW = vp.get(2);
            prevViewportH = vp.get(3);
        }
        bind();
        return new Binding(prevDrawFbo, prevReadFbo, prevViewportX, prevViewportY, prevViewportW, prevViewportH);
    }

    /**
     * Unbinds the current framebuffer (binds default FBO 0) and resets the viewport.
     *
     * @param viewportWidth  The width of the default viewport.
     * @param viewportHeight The height of the default viewport.
     */
    public static void unbind(int viewportWidth, int viewportHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, Math.max(1, viewportWidth), Math.max(1, viewportHeight));
    }

    /**
     * Gets the color attachment texture.
     *
     * @return The backing {@link Texture} object.
     */
    public Texture colorTexture() {
        return color;
    }

    /**
     * Gets the current width of the framebuffer.
     *
     * @return The width in pixels.
     */
    public int width() {
        return width;
    }

    /**
     * Gets the current height of the framebuffer.
     *
     * @return The height in pixels.
     */
    public int height() {
        return height;
    }

    /**
     * A scoped binding helper that restores the previous framebuffer and viewport state upon closure.
     */
    public final class Binding implements AutoCloseable {
        private final int prevDrawFbo;
        private final int prevReadFbo;
        private final int prevViewportX;
        private final int prevViewportY;
        private final int prevViewportW;
        private final int prevViewportH;
        private boolean closed;

        private Binding(int prevDrawFbo, int prevReadFbo, int prevViewportX, int prevViewportY, int prevViewportW, int prevViewportH) {
            this.prevDrawFbo = prevDrawFbo;
            this.prevReadFbo = prevReadFbo;
            this.prevViewportX = prevViewportX;
            this.prevViewportY = prevViewportY;
            this.prevViewportW = prevViewportW;
            this.prevViewportH = prevViewportH;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, prevReadFbo);
            glViewport(prevViewportX, prevViewportY, prevViewportW, prevViewportH);
        }
    }

    /**
     * Deletes the framebuffer and its attachments (renderbuffer and color texture).
     */
    @Override
    public void close() {
        color.close();
        glDeleteRenderbuffers(rbo);
        glDeleteFramebuffers(fbo);
    }
}
