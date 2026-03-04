package com.miry.core;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwInitHint;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwGetWindowContentScale;
import static org.lwjgl.glfw.GLFW.glfwSetWindowContentScaleCallback;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryStack.stackPush;

import org.lwjgl.system.MemoryStack;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

/**
 * GLFW window wrapper used by the demo runtime.
 * <p>
 * Provides window size, framebuffer size, and per-monitor content scale.
 */
public final class Window implements AutoCloseable {
    private final long handle;
    private int windowWidth;
    private int windowHeight;
    private int framebufferWidth;
    private int framebufferHeight;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private final GLFWErrorCallback errorCallback;

    public Window(String title, int width, int height) {
        errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();

        if (!glfwInit()) {
            throw new IllegalStateException("GLFW init failed.");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        handle = glfwCreateWindow(width, height, title, 0, 0);
        if (handle == 0) {
            glfwTerminate();
            throw new IllegalStateException("Failed to create GLFW window.");
        }

        glfwMakeContextCurrent(handle);
        GL.createCapabilities();
        glEnable(GL_MULTISAMPLE);
        glfwSwapInterval(1);
        glfwShowWindow(handle);

        updateSizesAndScale();
        glfwSetFramebufferSizeCallback(handle, (win, fbw, fbh) -> {
            framebufferWidth = Math.max(1, fbw);
            framebufferHeight = Math.max(1, fbh);
        });
        glfwSetWindowSizeCallback(handle, (win, w, h) -> {
            windowWidth = Math.max(1, w);
            windowHeight = Math.max(1, h);
        });
        glfwSetWindowContentScaleCallback(handle, (win, sx, sy) -> {
            scaleX = Math.max(0.1f, sx);
            scaleY = Math.max(0.1f, sy);
        });
    }

    public long getNativeWindow() {
        return handle;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public void waitEvents() {
        org.lwjgl.glfw.GLFW.glfwWaitEvents();
    }

    public void waitEvents(double timeout) {
        org.lwjgl.glfw.GLFW.glfwWaitEventsTimeout(timeout);
    }

    public void wakeUp() {
        org.lwjgl.glfw.GLFW.glfwPostEmptyEvent();
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public void requestClose() {
        glfwSetWindowShouldClose(handle, true);
    }

    private void updateSizesAndScale() {
        try (MemoryStack stack = stackPush()) {
            var winW = stack.mallocInt(1);
            var winH = stack.mallocInt(1);
            glfwGetWindowSize(handle, winW, winH);
            windowWidth = Math.max(1, winW.get(0));
            windowHeight = Math.max(1, winH.get(0));

            var fbW = stack.mallocInt(1);
            var fbH = stack.mallocInt(1);
            glfwGetFramebufferSize(handle, fbW, fbH);
            framebufferWidth = Math.max(1, fbW.get(0));
            framebufferHeight = Math.max(1, fbH.get(0));

            var sx = stack.mallocFloat(1);
            var sy = stack.mallocFloat(1);
            glfwGetWindowContentScale(handle, sx, sy);
            scaleX = Math.max(0.1f, sx.get(0));
            scaleY = Math.max(0.1f, sy.get(0));
        }
    }

    @Override
    public void close() {
        glfwDestroyWindow(handle);
        glfwTerminate();
        errorCallback.free();
    }
}
