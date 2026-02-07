package com.miry.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_A;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_B;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_G;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_R;

/**
 * OpenGL 2D texture wrapper.
 * <p>
 * Used for font atlases, procedural demo textures, and offscreen-rendered viewport previews.
 */
public final class Texture implements AutoCloseable {
    private final int id;
    private int width;
    private int height;

    public Texture() {
        id = glGenTextures();
        bind(0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        unbind();
    }

    public int id() {
        return id;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void bind(int slot) {
        glActiveTexture(GL13.GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void setFilteringNearest() {
        bind(0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        unbind();
    }

    public void setFilteringLinear() {
        bind(0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        unbind();
    }

    public void uploadRgba(int width, int height, ByteBuffer rgbaPixels) {
        if (rgbaPixels == null) {
            throw new IllegalArgumentException("rgbaPixels cannot be null");
        }
        this.width = width;
        this.height = height;
        bind(0);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgbaPixels);
        unbind();
    }

    public void allocateRgba(int width, int height) {
        this.width = width;
        this.height = height;
        bind(0);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        unbind();
    }

    public static Texture white1x1() {
        Texture t = new Texture();
        ByteBuffer bb = org.lwjgl.BufferUtils.createByteBuffer(4);
        bb.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).flip();
        t.uploadRgba(1, 1, bb);
        return t;
    }

    public static Texture fromGrayscale(ByteBuffer grayscale, int width, int height) {
        Texture t = new Texture();
        t.width = width;
        t.height = height;
        t.bind(0);

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

        t.unbind();
        return t;
    }

    @Override
    public void close() {
        glDeleteTextures(id);
    }
}
