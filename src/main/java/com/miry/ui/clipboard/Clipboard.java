package com.miry.ui.clipboard;

import org.lwjgl.glfw.GLFW;

/**
 * Clipboard wrapper backed by GLFW.
 */
public final class Clipboard {
    private final long window;

    public Clipboard(long window) {
        this.window = window;
    }

    public void setText(String text) {
        if (text != null) {
            GLFW.glfwSetClipboardString(window, text);
        }
    }

    public String getText() {
        String s = GLFW.glfwGetClipboardString(window);
        return s != null ? s : "";
    }
}
