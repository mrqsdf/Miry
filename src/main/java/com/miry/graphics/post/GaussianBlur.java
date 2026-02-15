package com.miry.graphics.post;

import com.miry.graphics.Framebuffer;
import com.miry.graphics.Shader;
import com.miry.graphics.Texture;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_BOX;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D;
import static org.lwjgl.opengl.GL11.glGetIntegerv;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL11.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER_BINDING;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

/**
 * Simple separable Gaussian blur (ping-pong FBO).
 * <p>
 * This is intended for backdrop blur effects, not for high quality photographic blur.
 */
public final class GaussianBlur implements AutoCloseable {
    private final Framebuffer ping;
    private final Framebuffer pong;
    private final Shader shader;
    private final FullscreenQuad quad;

    public GaussianBlur() {
        ping = new Framebuffer();
        pong = new Framebuffer();
        shader = Shader.fromResources("shaders/post/fullscreen.vert", "shaders/post/blur.frag");
        quad = new FullscreenQuad();
    }

    public Texture blur(Texture source, int width, int height, int iterations) {
        if (source == null || width <= 0 || height <= 0) {
            return source;
        }
        int it = Math.max(1, iterations);

        GlStateSnapshot glState = new GlStateSnapshot();
        glState.capture();

        // IMPORTANT: preserve split READ/DRAW bindings for host apps (e.g., Minecraft).
        int prevDrawFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);

        ping.ensureSize(width, height);
        pong.ensureSize(width, height);

        glDisable(GL_BLEND);

        try {
            shader.bind();
            shader.setUniform("uTexture", 0);
            shader.setUniform2f("uTexelSize", 1.0f / width, 1.0f / height);

            Texture input = source;
            for (int i = 0; i < it; i++) {
                // Horizontal.
                ping.bind();
                glClear(GL_COLOR_BUFFER_BIT);
                shader.setUniform2f("uDirection", 1.0f, 0.0f);
                input.bind(0);
                quad.bind();
                glDrawArrays(GL_TRIANGLES, 0, 6);

                // Vertical.
                pong.bind();
                glClear(GL_COLOR_BUFFER_BIT);
                shader.setUniform2f("uDirection", 0.0f, 1.0f);
                ping.colorTexture().bind(0);
                glDrawArrays(GL_TRIANGLES, 0, 6);

                input = pong.colorTexture();
            }

            quad.unbind();
            shader.unbind();
            return pong.colorTexture();
        } finally {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDrawFbo);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, prevReadFbo);
            glState.restore();
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
            prevProgram = glGetInteger(GL_CURRENT_PROGRAM);
            prevVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
            prevArrayBuffer = glGetInteger(GL_ARRAY_BUFFER_BINDING);
            prevElementArrayBuffer = glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);

            prevBlendEnabled = glIsEnabled(GL_BLEND);
            prevScissorEnabled = glIsEnabled(GL_SCISSOR_TEST);
            prevDepthTestEnabled = glIsEnabled(GL_DEPTH_TEST);

            prevBlendSrcRgb = glGetInteger(GL_BLEND_SRC_RGB);
            prevBlendDstRgb = glGetInteger(GL_BLEND_DST_RGB);
            prevBlendSrcAlpha = glGetInteger(GL_BLEND_SRC_ALPHA);
            prevBlendDstAlpha = glGetInteger(GL_BLEND_DST_ALPHA);

            prevViewportX = 0;
            prevViewportY = 0;
            prevViewportW = 0;
            prevViewportH = 0;
            prevScissorX = 0;
            prevScissorY = 0;
            prevScissorW = 0;
            prevScissorH = 0;
            try (MemoryStack stack = MemoryStack.stackPush()) {
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

            prevActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
            glActiveTexture(GL_TEXTURE0);
            prevTexture0 = glGetInteger(GL_TEXTURE_BINDING_2D);
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

            org.lwjgl.opengl.GL20.glUseProgram(prevProgram);
            glBindVertexArray(prevVao);
            glBindBuffer(GL_ARRAY_BUFFER, prevArrayBuffer);
            // In core profile, GL_ELEMENT_ARRAY_BUFFER binding is VAO state; restoring it while no VAO is bound is invalid.
            if (prevVao != 0) {
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, prevElementArrayBuffer);
            }
        }
    }

    @Override
    public void close() {
        quad.close();
        shader.close();
        ping.close();
        pong.close();
    }
}
