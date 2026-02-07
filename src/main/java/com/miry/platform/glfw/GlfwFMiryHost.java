package com.miry.platform.glfw;

import com.miry.core.Input;
import com.miry.core.Window;
import com.miry.platform.MiryHost;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.glfwGetClipboardString;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwSetClipboardString;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;

/**
 * GLFW-based implementation of {@link MiryHost} used by the demo runtime.
 */
public final class GlfwFMiryHost implements MiryHost {
    private final Window window;
    private final Vector2f mousePos = new Vector2f();

    public GlfwFMiryHost(Window window) {
        this.window = window;
    }

    @Override
    public int getWindowWidth() {
        return window.getWindowWidth();
    }

    @Override
    public int getWindowHeight() {
        return window.getWindowHeight();
    }

    @Override
    public int getFramebufferWidth() {
        return window.getFramebufferWidth();
    }

    @Override
    public int getFramebufferHeight() {
        return window.getFramebufferHeight();
    }

    @Override
    public float getScaleFactor() {
        return window.getScaleX();
    }

    @Override
    public double getTime() {
        return glfwGetTime();
    }

    @Override
    public boolean isKeyDown(int key) {
        return Input.isKeyDown(key);
    }

    @Override
    public boolean isMouseDown(int button) {
        return Input.isMouseButtonDown(button);
    }

    @Override
    public Vector2f getMousePos() {
        mousePos.set((float) Input.getMouseX(), (float) Input.getMouseY());
        return mousePos;
    }

    @Override
    public void setCursorLocked(boolean locked) {
        long handle = window.getNativeWindow();
        glfwSetInputMode(handle, GLFW_CURSOR, locked ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    @Override
    public String getClipboard() {
        String s = glfwGetClipboardString(window.getNativeWindow());
        return s == null ? "" : s;
    }

    @Override
    public void setClipboard(String text) {
        glfwSetClipboardString(window.getNativeWindow(), text == null ? "" : text);
    }

    @Override
    public long getNativeWindow() {
        return window.getNativeWindow();
    }
}
