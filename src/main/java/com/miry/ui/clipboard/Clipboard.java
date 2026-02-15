package com.miry.ui.clipboard;

import com.miry.core.MiryDebug;
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
        if (window == 0L) {
            return;
        }
        if (text != null) {
            long t0 = MiryDebug.nowNs();
            try {
                MiryDebug.trace("-> glfwSetClipboardString(len=" + text.length() + ", window=" + window + ")");
                GLFW.glfwSetClipboardString(window, text);
            } catch (Throwable t) {
                MiryDebug.log("Clipboard.setText failed (window=" + window + ")", t);
            } finally {
                MiryDebug.logIfSlow("glfwSetClipboardString", t0);
            }
        }
    }

    public String getText() {
        if (window == 0L) {
            return "";
        }
        long t0 = MiryDebug.nowNs();
        try {
            MiryDebug.trace("-> glfwGetClipboardString(window=" + window + ")");
            String s = GLFW.glfwGetClipboardString(window);
            return s != null ? s : "";
        } catch (Throwable t) {
            MiryDebug.log("Clipboard.getText failed (window=" + window + ")", t);
            return "";
        } finally {
            MiryDebug.logIfSlow("glfwGetClipboardString", t0);
        }
    }
}
