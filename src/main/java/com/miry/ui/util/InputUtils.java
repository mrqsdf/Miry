package com.miry.ui.util;

import com.miry.core.MiryDebug;
import com.miry.core.Input;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

/**
 * Input handling utilities for keyboard and mouse.
 */
public final class InputUtils {
    private InputUtils() {
        throw new AssertionError("No instances");
    }

    /**
     * Checks if either Shift key is pressed.
     *
     * @param input input handler (can be null)
     * @return true if Shift is pressed
     */
    public static boolean isShiftDown(Input input) {
        if (input == null) {
            return false;
        }
        return input.isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) ||
               input.isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    /**
     * Checks if either Ctrl key is pressed.
     *
     * @param input input handler (can be null)
     * @return true if Ctrl is pressed
     */
    public static boolean isCtrlDown(Input input) {
        if (input == null) {
            return false;
        }
        return input.isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) ||
               input.isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    /**
     * Checks if either Alt key is pressed.
     *
     * @param input input handler (can be null)
     * @return true if Alt is pressed
     */
    public static boolean isAltDown(Input input) {
        if (input == null) {
            return false;
        }
        return input.isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) ||
               input.isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT);
    }

    /**
     * Checks if either Super/Windows/Command key is pressed.
     *
     * @param input input handler (can be null)
     * @return true if Super is pressed
     */
    public static boolean isSuperDown(Input input) {
        if (input == null) {
            return false;
        }
        return input.isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SUPER) ||
               input.isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SUPER);
    }

    /**
     * Wraps the mouse cursor to the opposite edge when dragging near screen edges.
     * This enables infinite drag behavior for widgets like DragNumber.
     *
     * @param window GLFW window handle
     * @param mouseX current mouse X position
     * @param mouseY current mouse Y position
     * @param wrapMarginPx margin from edge in pixels to trigger wrapping
     * @return new mouse X position, or NaN if no wrapping occurred
     */
    public static double wrapMouseX(long window, double mouseX, double mouseY, int wrapMarginPx) {
        if (window == 0L) {
            return Double.NaN;
        }

        int margin = Math.max(1, wrapMarginPx);
        long t0 = MiryDebug.nowNs();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var wBuf = stack.mallocInt(1);
            var hBuf = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(window, wBuf, hBuf);
            int winW = wBuf.get(0);
            int winH = hBuf.get(0);

            if (winW > margin * 2 && winH > 0) {
                double newX = Double.NaN;
                if (mouseX <= margin) {
                    newX = winW - margin - 1;
                } else if (mouseX >= winW - margin - 1) {
                    newX = margin + 1;
                }

                if (Double.isFinite(newX)) {
                    double newY = Math.max(0.0, Math.min(winH - 1.0, mouseY));
                    try {
                        MiryDebug.trace("-> glfwSetCursorPos (wrapMouseX, window=" + window + ")");
                        GLFW.glfwSetCursorPos(window, newX, newY);
                    } catch (Throwable t) {
                        MiryDebug.log("glfwSetCursorPos failed (window=" + window + ")", t);
                    }
                    return newX;
                }
            }
        }
        finally {
            MiryDebug.logIfSlow("InputUtils.wrapMouseX", t0);
        }
        return Double.NaN;
    }

    /**
     * Result of mouse wrapping operation.
     */
    public static class WrapResult {
        public final double newX;
        public final double newY;
        public final boolean wrapped;

        public WrapResult(double newX, double newY, boolean wrapped) {
            this.newX = newX;
            this.newY = newY;
            this.wrapped = wrapped;
        }
    }

    /**
     * Wraps the mouse cursor to the opposite edge (both X and Y).
     * More complete version that returns both coordinates.
     *
     * @param window GLFW window handle
     * @param mouseX current mouse X position
     * @param mouseY current mouse Y position
     * @param wrapMarginPx margin from edge in pixels to trigger wrapping
     * @return WrapResult with new coordinates and whether wrapping occurred
     */
    public static WrapResult wrapMouse(long window, double mouseX, double mouseY, int wrapMarginPx) {
        if (window == 0L) {
            return new WrapResult(mouseX, mouseY, false);
        }

        int margin = Math.max(1, wrapMarginPx);
        long t0 = MiryDebug.nowNs();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var wBuf = stack.mallocInt(1);
            var hBuf = stack.mallocInt(1);
            GLFW.glfwGetWindowSize(window, wBuf, hBuf);
            int winW = wBuf.get(0);
            int winH = hBuf.get(0);

            if (winW > margin * 2 && winH > 0) {
                double newX = mouseX;
                double newY = mouseY;
                boolean wrapped = false;

                if (mouseX <= margin) {
                    newX = winW - margin - 1;
                    wrapped = true;
                } else if (mouseX >= winW - margin - 1) {
                    newX = margin + 1;
                    wrapped = true;
                }

                if (wrapped) {
                    newY = Math.max(0.0, Math.min(winH - 1.0, mouseY));
                    try {
                        MiryDebug.trace("-> glfwSetCursorPos (wrapMouse, window=" + window + ")");
                        GLFW.glfwSetCursorPos(window, newX, newY);
                    } catch (Throwable t) {
                        MiryDebug.log("glfwSetCursorPos failed (window=" + window + ")", t);
                    }
                    return new WrapResult(newX, newY, true);
                }
            }
        }
        finally {
            MiryDebug.logIfSlow("InputUtils.wrapMouse", t0);
        }
        return new WrapResult(mouseX, mouseY, false);
    }
}
