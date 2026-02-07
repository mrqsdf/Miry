package com.miry.core;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.system.MemoryStack.stackPush;

import org.lwjgl.system.MemoryStack;

/**
 * Global input helpers used by the demo runtime.
 * <p>
 * This is a thin wrapper around GLFW state queries and callbacks.
 */
public final class Input {
    private static long window;
    private static double scrollY;

    private Input() {}

    public static void init(long windowHandle) {
        window = windowHandle;
        scrollY = 0.0;
        glfwSetScrollCallback(windowHandle, (win, xoff, yoff) -> scrollY += yoff);
    }

    public static boolean isKeyDown(int key) {
        return glfwGetKey(window, key) == GLFW_PRESS;
    }

    public static boolean isMouseButtonDown(int button) {
        return glfwGetMouseButton(window, button) == GLFW_PRESS;
    }

    public static double getMouseX() {
        try (MemoryStack stack = stackPush()) {
            var x = stack.mallocDouble(1);
            var y = stack.mallocDouble(1);
            glfwGetCursorPos(window, x, y);
            return x.get(0);
        }
    }

    public static double getMouseY() {
        try (MemoryStack stack = stackPush()) {
            var x = stack.mallocDouble(1);
            var y = stack.mallocDouble(1);
            glfwGetCursorPos(window, x, y);
            return y.get(0);
        }
    }

    public static double consumeScrollY() {
        double v = scrollY;
        scrollY = 0.0;
        return v;
    }
}
