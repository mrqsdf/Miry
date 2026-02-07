package com.miry.ui.cursor;

import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

/**
 * Convenience wrapper that creates and switches GLFW standard cursors.
 */
public final class CursorManager implements AutoCloseable {
    private final long window;
    private final Map<CursorType, Long> cursors = new EnumMap<>(CursorType.class);
    private CursorType currentType = CursorType.ARROW;

    public CursorManager(long window) {
        this.window = window;
        cursors.put(CursorType.ARROW, GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
        cursors.put(CursorType.IBEAM, GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR));
        cursors.put(CursorType.CROSSHAIR, GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR));
        cursors.put(CursorType.HAND, GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR));
        cursors.put(CursorType.HRESIZE, GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR));
        cursors.put(CursorType.VRESIZE, GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR));
        cursors.put(CursorType.NWSE_RESIZE, GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR));
        cursors.put(CursorType.NESW_RESIZE, GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR));
    }

    public void setCursor(CursorType type) {
        if (currentType == type) return;
        currentType = type;
        Long cursor = cursors.get(type);
        if (cursor != null) {
            GLFW.glfwSetCursor(window, cursor);
        }
    }

    public CursorType currentType() {
        return currentType;
    }

    @Override
    public void close() {
        for (Long cursor : cursors.values()) {
            if (cursor != null && cursor != 0L) {
                GLFW.glfwDestroyCursor(cursor);
            }
        }
        cursors.clear();
    }
}
