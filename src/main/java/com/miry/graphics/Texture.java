package com.miry.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_A;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_B;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_G;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_R;

/**
 * OpenGL 2D texture wrapper.
 * <p>
 * This class encapsulates an OpenGL texture resource, providing methods for creation, binding,
 * uploading pixel data, and configuring filtering parameters. It implements {@link AutoCloseable}
 * for proper resource cleanup.
 * </p>
 * <p>
 * Uses include font atlases, procedural demo textures, and offscreen-rendered viewport previews.
 * </p>
 */
public final class Texture implements AutoCloseable {
    private final int id;
    private final boolean ownsId;
    private int width;
    private int height;

    /**
     * Creates a new, empty OpenGL texture with nearest-neighbor filtering.
     * <p>
     * The texture ID is generated, but no storage is allocated until an upload or allocation method is called.
     * </p>
     */
    public Texture() {
        id = glGenTextures();
        ownsId = true;
        withBoundForEdit(0, () -> {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        });
    }

    private Texture(int existingId, int width, int height, boolean ownsId) {
        if (existingId == 0) {
            throw new IllegalArgumentException("existingId must be a non-zero GL texture id");
        }
        this.id = existingId;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.ownsId = ownsId;
    }

    /**
     * Gets the OpenGL ID of this texture.
     *
     * @return The texture ID.
     */
    public int id() {
        return id;
    }

    /**
     * Gets the width of the texture in pixels.
     *
     * @return The width.
     */
    public int width() {
        return width;
    }

    /**
     * Gets the height of the texture in pixels.
     *
     * @return The height.
     */
    public int height() {
        return height;
    }

    /**
     * Wraps an existing OpenGL texture ID into a {@code Texture} object.
     * <p>
     * Useful when embedding Miry as an overlay inside another engine (e.g., Minecraft) where textures
     * are created and managed externally.
     * </p>
     *
     * @param glTextureId The existing OpenGL texture ID.
     * @param width       The width of the texture.
     * @param height      The height of the texture.
     * @param ownsId      Whether this {@code Texture} object should delete the GL texture when closed.
     * @return A new {@code Texture} instance wrapping the provided ID.
     */
    public static Texture wrapExternal(int glTextureId, int width, int height, boolean ownsId) {
        return new Texture(glTextureId, width, height, ownsId);
    }

    /**
     * Binds the texture to the specified texture unit.
     *
     * @param slot The texture unit to bind to (0 for GL_TEXTURE0, 1 for GL_TEXTURE1, etc.).
     */
    public void bind(int slot) {
        glActiveTexture(GL13.GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    /**
     * Unbinds the current 2D texture from the active texture unit.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Sets the texture filtering mode to Nearest Neighbor.
     * <p>
     * Useful for pixel art or sharp edges.
     * </p>
     */
    public void setFilteringNearest() {
        withBoundForEdit(0, () -> {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        });
    }

    /**
     * Sets the texture filtering mode to Linear.
     * <p>
     * Useful for smooth textures and fonts.
     * </p>
     */
    public void setFilteringLinear() {
        withBoundForEdit(0, () -> {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        });
    }

    /**
     * Uploads RGBA pixel data to the texture.
     *
     * @param width      The width of the image data.
     * @param height     The height of the image data.
     * @param rgbaPixels The pixel data buffer, expected to be in RGBA format (unsigned bytes).
     * @throws IllegalArgumentException if {@code rgbaPixels} is null.
     */
    public void uploadRgba(int width, int height, ByteBuffer rgbaPixels) {
        if (rgbaPixels == null) {
            throw new IllegalArgumentException("rgbaPixels cannot be null");
        }
        this.width = width;
        this.height = height;
        withBoundForEdit(0, () -> glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgbaPixels));
    }

    /**
     * Allocates texture storage with RGBA format without uploading initial data.
     * <p>
     * Useful for creating textures that will be used as framebuffer attachments.
     * </p>
     *
     * @param width  The width of the texture.
     * @param height The height of the texture.
     */
    public void allocateRgba(int width, int height) {
        this.width = width;
        this.height = height;
        withBoundForEdit(0, () -> glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null));
    }

    /**
     * Creates a 1x1 white texture.
     * <p>
     * Useful as a default texture or for "blank" rendering.
     * </p>
     *
     * @return A new 1x1 white texture.
     */
    public static Texture white1x1() {
        Texture t = new Texture();
        ByteBuffer bb = org.lwjgl.BufferUtils.createByteBuffer(4);
        bb.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).flip();
        t.uploadRgba(1, 1, bb);
        return t;
    }

    /**
     * Creates a texture from single-channel grayscale data.
     * <p>
     * The texture is configured to map the single channel to the Alpha component when sampled,
     * appearing as white with varying opacity. This is efficient for font rendering.
     * </p>
     *
     * @param grayscale The grayscale pixel data.
     * @param width     The width of the image.
     * @param height    The height of the image.
     * @return A new texture configured for grayscale-as-alpha rendering.
     */
    public static Texture fromGrayscale(ByteBuffer grayscale, int width, int height) {
        Texture t = new Texture();
        t.width = width;
        t.height = height;
        t.withBoundForEdit(0, () -> {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            // Treat the single channel as alpha coverage so the batch shader can stay generic:
            // sample -> (1,1,1,coverage) and then `texture * vColor` becomes `vec4(vColor.rgb, vColor.a * coverage)`.
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL_ONE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL_ONE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL_ONE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_RED);

            int prevUnpackAlignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, grayscale);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, prevUnpackAlignment);
        });
        return t;
    }

    private void withBoundForEdit(int slot, Runnable action) {
        int prevActive = glGetInteger(GL_ACTIVE_TEXTURE);

        glActiveTexture(GL_TEXTURE0 + slot);
        int prevBinding = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, id);
        try {
            action.run();
        } finally {
            glBindTexture(GL_TEXTURE_2D, prevBinding);
            glActiveTexture(prevActive);
        }
    }

    /**
     * Deletes the OpenGL texture resource.
     * <p>
     * This method must be called when the texture is no longer needed to free GPU memory.
     * If the texture was created via {@link #wrapExternal(int, int, int, boolean)} with {@code ownsId=false},
     * this method does nothing.
     * </p>
     */
    @Override
    public void close() {
        if (ownsId) {
            glDeleteTextures(id);
        }
    }
}