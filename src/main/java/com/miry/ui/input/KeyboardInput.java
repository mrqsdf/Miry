package com.miry.ui.input;

import com.miry.ui.event.EventQueue;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;

/**
 * GLFW-backed keyboard input bridge that feeds the UI {@link EventQueue}.
 */
public final class KeyboardInput {
    private final long window;
    private final EventQueue eventQueue;
    private GLFWKeyCallback keyCallback;
    private GLFWCharCallback charCallback;
    private final boolean[] keyState = new boolean[512];

    public KeyboardInput(long window, EventQueue eventQueue) {
        this.window = window;
        this.eventQueue = eventQueue;
        install();
    }

    private void install() {
        keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long win, int key, int scancode, int action, int mods) {
                if (key >= 0 && key < keyState.length) {
                    keyState[key] = (action != GLFW.GLFW_RELEASE);
                }
                KeyEvent.Action act = switch (action) {
                    case GLFW.GLFW_PRESS -> KeyEvent.Action.PRESS;
                    case GLFW.GLFW_RELEASE -> KeyEvent.Action.RELEASE;
                    case GLFW.GLFW_REPEAT -> KeyEvent.Action.REPEAT;
                    default -> null;
                };
                if (act != null) {
                    eventQueue.push(new KeyEvent(key, scancode, act, mods));
                }
            }
        };

        charCallback = new GLFWCharCallback() {
            @Override
            public void invoke(long win, int codepoint) {
                eventQueue.push(new TextInputEvent(codepoint));
            }
        };

        GLFW.glfwSetKeyCallback(window, keyCallback);
        GLFW.glfwSetCharCallback(window, charCallback);
    }

    public void cleanup() {
        if (keyCallback != null) {
            keyCallback.free();
        }
        if (charCallback != null) {
            charCallback.free();
        }
    }

    public boolean isKeyDown(int key) {
        return key >= 0 && key < keyState.length && keyState[key];
    }
}
