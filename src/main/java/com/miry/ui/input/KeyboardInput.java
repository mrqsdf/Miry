package com.miry.ui.input;

import com.miry.core.MiryDebug;
import com.miry.ui.event.EventQueue;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;

/**
 * GLFW-backed keyboard input bridge that feeds the UI {@link EventQueue}.
 */
public final class KeyboardInput {
    public enum Mode {
        /**
         * Installs GLFW callbacks on the window handle
         * <p>
         * This can conflict with hosts that already own GLFW callbacks
         */
        INSTALL_GLFW_CALLBACKS,
        /**
         * Does not install GLFW callbacks; the host must forward key/char events manually using
         * {@link #pushKeyEvent(int, int, KeyEvent.Action, int)} and {@link #pushCharEvent(int)}.
         */
        MANUAL
    }

    private final long window;
    private final EventQueue eventQueue;
    private GLFWKeyCallback keyCallback;
    private GLFWCharCallback charCallback;
    private long prevKeyCallbackAddress;
    private long prevCharCallbackAddress;
    private GLFWKeyCallbackI prevKeyCallback;
    private GLFWCharCallbackI prevCharCallback;
    private final boolean[] keyState = new boolean[512];
    private final Mode mode;
    private boolean dispatchingPrevKey;
    private boolean dispatchingPrevChar;

    public KeyboardInput(long window, EventQueue eventQueue) {
        this(window, eventQueue, Mode.INSTALL_GLFW_CALLBACKS);
    }

    public KeyboardInput(long window, EventQueue eventQueue, Mode mode) {
        this.window = window;
        this.eventQueue = eventQueue;
        Mode requested = mode == null ? Mode.INSTALL_GLFW_CALLBACKS : mode;
        this.mode = (window == 0L) ? Mode.MANUAL : requested;
        if (this.mode == Mode.INSTALL_GLFW_CALLBACKS) {
            install();
        }
    }

    private void install() {
        MiryDebug.log("KeyboardInput.install (window=" + window + ")");
        keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long win, int key, int scancode, int action, int mods) {
                // Avoid infinite recursion if a host also chains callbacks by calling the "previous" callback.
                // Example: host callback calls previous (us), and we call previous (host) => loop.
                if (prevKeyCallback != null && !dispatchingPrevKey) {
                    dispatchingPrevKey = true;
                    try {
                        prevKeyCallback.invoke(win, key, scancode, action, mods);
                    } catch (Throwable t) {
                        MiryDebug.log("prevKeyCallback failed (window=" + window + ")", t);
                    } finally {
                        dispatchingPrevKey = false;
                    }
                }
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
                if (prevCharCallback != null && !dispatchingPrevChar) {
                    dispatchingPrevChar = true;
                    try {
                        prevCharCallback.invoke(win, codepoint);
                    } catch (Throwable t) {
                        MiryDebug.log("prevCharCallback failed (window=" + window + ")", t);
                    } finally {
                        dispatchingPrevChar = false;
                    }
                }
                eventQueue.push(new TextInputEvent(codepoint));
            }
        };

        long t0 = MiryDebug.nowNs();
        try {
            // Prefer the "n" variants to avoid LWJGL callback wrapping on set, which can hang in some hosts.
            MiryDebug.trace("-> nglfwSetKeyCallback(window=" + window + ")");
            prevKeyCallbackAddress = GLFW.nglfwSetKeyCallback(window, keyCallback.address());
            MiryDebug.trace("<- nglfwSetKeyCallback(prev=" + prevKeyCallbackAddress + ")");

            MiryDebug.trace("-> nglfwSetCharCallback(window=" + window + ")");
            prevCharCallbackAddress = GLFW.nglfwSetCharCallback(window, charCallback.address());
            MiryDebug.trace("<- nglfwSetCharCallback(prev=" + prevCharCallbackAddress + ")");

            // Only wrap previous pointers if we might invoke them (for chaining).
            prevKeyCallback = prevKeyCallbackAddress != 0L ? GLFWKeyCallback.create(prevKeyCallbackAddress) : null;
            prevCharCallback = prevCharCallbackAddress != 0L ? GLFWCharCallback.create(prevCharCallbackAddress) : null;
        } catch (Throwable t) {
            MiryDebug.log("KeyboardInput.install failed (window=" + window + ")", t);
        } finally {
            MiryDebug.logIfSlow("KeyboardInput.install (set callbacks)", t0);
        }
    }

    public void cleanup() {
        if (mode == Mode.INSTALL_GLFW_CALLBACKS) {
            try {
                GLFW.nglfwSetKeyCallback(window, prevKeyCallbackAddress);
                GLFW.nglfwSetCharCallback(window, prevCharCallbackAddress);
            } catch (Throwable t) {
                MiryDebug.log("KeyboardInput.cleanup failed (window=" + window + ")", t);
            }
            prevKeyCallbackAddress = 0L;
            prevCharCallbackAddress = 0L;
            prevKeyCallback = null;
            prevCharCallback = null;
        }
        if (keyCallback != null) {
            keyCallback.free();
            keyCallback = null;
        }
        if (charCallback != null) {
            charCallback.free();
            charCallback = null;
        }
    }

    public boolean isKeyDown(int key) {
        return key >= 0 && key < keyState.length && keyState[key];
    }

    /**
     * Manual event injection for {@link Mode#MANUAL}.
     */
    public void pushKeyEvent(int key, int scancode, KeyEvent.Action action, int mods) {
        if (action == null) {
            return;
        }
        if (key >= 0 && key < keyState.length) {
            keyState[key] = (action != KeyEvent.Action.RELEASE);
        }
        eventQueue.push(new KeyEvent(key, scancode, action, mods));
    }

    /**
     * Manual event injection for {@link Mode#MANUAL}.
     */
    public void pushCharEvent(int codepoint) {
        eventQueue.push(new TextInputEvent(codepoint));
    }
}
