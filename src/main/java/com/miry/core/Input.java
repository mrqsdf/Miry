package com.miry.core;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.system.MemoryStack.stackPush;

import org.lwjgl.system.MemoryStack;

/**
 * Global input helper class for the demo runtime.
 * <p>
 * This class provides a thin static wrapper around GLFW state queries and callbacks, allowing access to
 * keyboard, mouse, and scroll input states.
 * </p>
 */
public final class Input {
    private static long window;
    private static double scrollY;

    private Input() {
        // Prevent instantiation
    }

    /**
     * Initializes the Input system with the specified window handle.
     *
     * @param windowHandle The native GLFW window handle.
     */
    public static void init(long windowHandle) {
        window = windowHandle;
        scrollY = 0.0;
        glfwSetScrollCallback(windowHandle, (win, xoff, yoff) -> scrollY += yoff);
    }

    /**
     * Checks if a specific key is currently pressed.
     *
     * @param key The GLFW key code (e.g., {@code GLFW_KEY_A}).
     * @return {@code true} if the key is pressed, {@code false} otherwise.
     */
    public static boolean isKeyDown(int key) {
        return glfwGetKey(window, key) == GLFW_PRESS;
    }

    /**
     * Checks if a specific mouse button is currently pressed.
     *
     * @param button The GLFW mouse button code (e.g., {@code GLFW_MOUSE_BUTTON_LEFT}).
     * @return {@code true} if the mouse button is pressed, {@code false} otherwise.
     */
    public static boolean isMouseButtonDown(int button) {
        return glfwGetMouseButton(window, button) == GLFW_PRESS;
    }

    /**
     * Retrieves the current X position of the mouse cursor.
     *
     * @return The cursor's X position, in screen coordinates, relative to the window content area.
     */
    public static double getMouseX() {
        try (MemoryStack stack = stackPush()) {
            var x = stack.mallocDouble(1);
            var y = stack.mallocDouble(1);
            glfwGetCursorPos(window, x, y);
            return x.get(0);
        }
    }

    /**
     * Retrieves the current Y position of the mouse cursor.
     *
     * @return The cursor's Y position, in screen coordinates, relative to the window content area.
     */
    public static double getMouseY() {
        try (MemoryStack stack = stackPush()) {
            var x = stack.mallocDouble(1);
            var y = stack.mallocDouble(1);
            glfwGetCursorPos(window, x, y);
            return y.get(0);
        }
    }

    /**
     * Consumes and returns the accumulated vertical scroll amount since the last call.
     * <p>
     * Calling this method resets the accumulated scroll value to 0.0.
     * </p>
     *
     * @return The vertical scroll offset. Positive values indicate scrolling up, negative values indicate scrolling down.
     */
    public static double consumeScrollY() {
        double v = scrollY;
        scrollY = 0.0;
        return v;
    }
}