package com.miry.graphics;

import com.miry.core.MiryDebug;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glReadPixels;


public final class ScreenColorSampler {
    private ScreenColorSampler() {}

    /**
     * Samples at framebuffer pixel coordinates (origin bottom-left).
     *
     * @return ARGB packed int.
     */
    public static int sampleArgb(int fbX, int fbY) {
        int x = Math.max(0, fbX);
        int y = Math.max(0, fbY);
        long t0 = MiryDebug.nowNs();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer px = stack.malloc(4);
            MiryDebug.trace("-> glReadPixels(1x1 @ " + x + "," + y + ")");
            glReadPixels(x, y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, px);
            int r = px.get(0) & 0xFF;
            int g = px.get(1) & 0xFF;
            int b = px.get(2) & 0xFF;
            int a = px.get(3) & 0xFF;
            return (a << 24) | (r << 16) | (g << 8) | b;
        } finally {
            MiryDebug.logIfSlow("glReadPixels(1x1)", t0);
        }
    }
}
