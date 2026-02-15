package com.miry.ui.cursor;

import com.miry.core.MiryDebug;
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
        if (window == 0L) {
            return;
        }
        long t0 = MiryDebug.nowNs();
        try {
            MiryDebug.trace("-> glfwCreateStandardCursor(...) x8 (window=" + window + ")");
            cursors.put(CursorType.ARROW, GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
            cursors.put(CursorType.IBEAM, GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR));
            cursors.put(CursorType.CROSSHAIR, GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR));
            cursors.put(CursorType.HAND, GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR));
            cursors.put(CursorType.HRESIZE, GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR));
            cursors.put(CursorType.VRESIZE, GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR));
            cursors.put(CursorType.NWSE_RESIZE, GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR));
            cursors.put(CursorType.NESW_RESIZE, GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR));
        } catch (Throwable t) {
            MiryDebug.log("CursorManager init failed (window=" + window + ")", t);
        } finally {
            MiryDebug.logIfSlow("CursorManager init (create standard cursors)", t0);
        }
    }

    public void setCursor(CursorType type) {
        if (currentType == type) return;
        currentType = type;
        if (window == 0L) {
            return;
        }
        Long cursor = cursors.get(type);
        if (cursor != null) {
            long t0 = MiryDebug.nowNs();
            try {
                MiryDebug.trace("-> glfwSetCursor(type=" + type + ", window=" + window + ")");
                GLFW.glfwSetCursor(window, cursor);
            } catch (Throwable t) {
                MiryDebug.log("glfwSetCursor failed (window=" + window + ", type=" + type + ")", t);
            } finally {
                MiryDebug.logIfSlow("glfwSetCursor", t0);
            }
        }
    }

    public CursorType currentType() {
        return currentType;
    }

    @Override
    public void close() {
        if (window == 0L) {
            cursors.clear();
            return;
        }
        for (Long cursor : cursors.values()) {
            if (cursor != null && cursor != 0L) {
                GLFW.glfwDestroyCursor(cursor);
            }
        }
        cursors.clear();
    }
}
