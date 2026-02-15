package com.miry.ui.render;

import com.miry.graphics.Texture;

/**
 * Minimal immediate-mode rendering abstraction for the UI layer.
 * <p>
 * Coordinates are in UI pixels in the current framebuffer (top-left origin).
 * Implementations are expected to batch/flush as needed.
 */
public interface UiRenderer {
    void flush();

    void pushClip(int x, int y, int width, int height);

    void popClip();

    void drawRect(float x, float y, float w, float h, int argb);

    /**
     * Draws a filled triangle.
     * <p>
     * Renderers that only support quads may approximate this (e.g. bounding-box fill).
     */
    default void drawTriangle(float ax, float ay, float bx, float by, float cx, float cy, int argb) {
        float minX = Math.min(ax, Math.min(bx, cx));
        float minY = Math.min(ay, Math.min(by, cy));
        float maxX = Math.max(ax, Math.max(bx, cx));
        float maxY = Math.max(ay, Math.max(by, cy));
        drawRect(minX, minY, Math.max(0.0f, maxX - minX), Math.max(0.0f, maxY - minY), argb);
    }

    /**
     * Draws an anti-aliased rounded rectangle (when supported by the backend).
     * <p>
     * Default implementation falls back to {@link #drawRect(float, float, float, float, int)}.
     */
    default void drawRoundedRect(float x, float y, float w, float h, float radiusPx, int argb) {
        drawRect(x, y, w, h, argb);
    }

    /**
     * Draws an anti-aliased rounded rectangle with per-corner colors.
     * <p>
     * Useful for simple linear gradients (e.g. top -> bottom).
     */
    default void drawRoundedRect(float x,
                                 float y,
                                 float w,
                                 float h,
                                 float radiusPx,
                                 int argbTL,
                                 int argbTR,
                                 int argbBR,
                                 int argbBL) {
        drawRoundedRect(x, y, w, h, radiusPx, argbTL);
    }

    /**
     * Draws a rounded rect with a stroke in a single call (when supported by the backend).
     * <p>
     * Default implementation falls back to drawing fill + outline separately.
     */
    default void drawRoundedRect(float x,
                                 float y,
                                 float w,
                                 float h,
                                 float radiusPx,
                                 int fillArgb,
                                 float borderPx,
                                 int strokeArgb) {
        drawRoundedRect(x, y, w, h, radiusPx, fillArgb, fillArgb, fillArgb, fillArgb, borderPx, strokeArgb);
    }

    /**
     * Draws a rounded rect with gradient fill and a stroke.
     */
    default void drawRoundedRect(float x,
                                 float y,
                                 float w,
                                 float h,
                                 float radiusPx,
                                 int fillTL,
                                 int fillTR,
                                 int fillBR,
                                 int fillBL,
                                 float borderPx,
                                 int strokeArgb) {
        // Fallback: emulate stroke by drawing a border rect then an inner fill rect.
        int t = Math.max(0, Math.round(borderPx));
        if (t > 0) {
            drawRoundedRect(x, y, w, h, radiusPx, strokeArgb);
        }
        if (w > t * 2.0f && h > t * 2.0f) {
            drawRoundedRect(x + t, y + t, w - t * 2.0f, h - t * 2.0f, Math.max(0.0f, radiusPx - t), fillTL, fillTR, fillBR, fillBL);
        } else if (t <= 0) {
            drawRoundedRect(x, y, w, h, radiusPx, fillTL, fillTR, fillBR, fillBL);
        }
    }

    /**
     * Draw a filled circle (anti-aliased when supported).
     */
    default void drawCircle(float cx, float cy, float radiusPx, int argb) {
        float r = Math.max(0.0f, radiusPx);
        float d = r * 2.0f;
        drawRoundedRect(cx - r, cy - r, d, d, r, argb);
    }

    /**
     * Draw a circle with stroke + optional fill.
     */
    default void drawCircle(float cx, float cy, float radiusPx, int fillArgb, float borderPx, int strokeArgb) {
        float r = Math.max(0.0f, radiusPx);
        float d = r * 2.0f;
        drawRoundedRect(cx - r, cy - r, d, d, r, fillArgb, borderPx, strokeArgb);
    }

    /**
     * Draws a capsule (rounded line segment) between two points.
     * <p>
     * Useful for vector icons, graph wires, and gizmo helpers.
     */
    default void drawCapsule(float ax, float ay, float bx, float by, float radiusPx, int argb) {
        drawCapsule(ax, ay, bx, by, radiusPx, argb, 0.0f, 0);
    }

    /**
     * Draws a capsule with stroke + optional fill.
     */
    default void drawCapsule(float ax,
                             float ay,
                             float bx,
                             float by,
                             float radiusPx,
                             int fillArgb,
                             float borderPx,
                             int strokeArgb) {
        float r = Math.max(0.0f, radiusPx);
        float minX = Math.min(ax, bx) - r;
        float minY = Math.min(ay, by) - r;
        float maxX = Math.max(ax, bx) + r;
        float maxY = Math.max(ay, by) + r;
        drawRoundedRect(minX, minY, maxX - minX, maxY - minY, r, fillArgb, borderPx, strokeArgb);
    }

    /**
     * Draws a rounded rect filled by a 2-color linear gradient.
     * <p>
     * Direction is expressed in UV space (0..1) where (0,1) is top->bottom.
     */
    default void drawLinearGradientRoundedRect(float x,
                                               float y,
                                               float w,
                                               float h,
                                               float radiusPx,
                                               int startArgb,
                                               int endArgb,
                                               float dirX,
                                               float dirY,
                                               float borderPx,
                                               int strokeArgb) {
        float ax = Math.abs(dirX);
        float ay = Math.abs(dirY);
        if (ay >= ax) {
            drawRoundedRect(x, y, w, h, radiusPx, startArgb, startArgb, endArgb, endArgb, borderPx, strokeArgb);
        } else {
            drawRoundedRect(x, y, w, h, radiusPx, startArgb, endArgb, endArgb, startArgb, borderPx, strokeArgb);
        }
    }

    /**
     * Draws a rounded rect filled by a 2-color radial gradient.
     * <p>
     * This is optional; renderers may fall back to a solid fill.
     */
    default void drawRadialGradientRoundedRect(float x,
                                               float y,
                                               float w,
                                               float h,
                                               float radiusPx,
                                               int innerArgb,
                                               int outerArgb,
                                               float centerUx,
                                               float centerUy,
                                               float radiusU,
                                               float borderPx,
                                               int strokeArgb) {
        drawRoundedRect(x, y, w, h, radiusPx, innerArgb, borderPx, strokeArgb);
    }

    void drawTexturedRect(Texture texture, float x, float y, float w, float h, int argb);

    /**
     * Draws a textured quad using explicit UVs.
     * <p>
     * Default implementation falls back to {@link #drawTexturedRect(Texture, float, float, float, float, int)}.
     * Renderers that support atlas sub-rects should override this method.
     */
    default void drawTexturedRect(Texture texture,
                                  float x,
                                  float y,
                                  float w,
                                  float h,
                                  float u0,
                                  float v0,
                                  float u1,
                                  float v1,
                                  int argb) {
        drawTexturedRect(texture, x, y, w, h, argb);
    }

    void drawText(String text, float x, float y, int argb);

    float measureText(String text);

    float lineHeight();

    float ascent();

    default float baselineForBox(float topY, float boxHeight) {
        return topY + (boxHeight - lineHeight()) * 0.5f + ascent();
    }

    // === Helper methods for new widgets ===

    default void pushClipRect(int x, int y, int width, int height) {
        pushClip(x, y, width, height);
    }

    default void popClipRect() {
        popClip();
    }

    default String clipText(String text, int maxWidth) {
        if (text == null || text.isEmpty()) return "";
        float w = measureText(text);
        if (w <= maxWidth) return text;

        // Binary search for the longest prefix that fits
        int lo = 0, hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            String sub = text.substring(0, mid) + "...";
            if (measureText(sub) <= maxWidth) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo > 0 ? text.substring(0, lo) + "..." : "";
    }

    default void drawLine(int x0, int y0, int x1, int y1, int thickness, int argb) {
        // Simple line using thick rect
        if (x0 == x1) {
            // Vertical
            drawRect(x0 - thickness / 2, Math.min(y0, y1), thickness, Math.abs(y1 - y0), argb);
        } else if (y0 == y1) {
            // Horizontal
            drawRect(Math.min(x0, x1), y0 - thickness / 2, Math.abs(x1 - x0), thickness, argb);
        } else {
            // Diagonal - approximate with rect
            int dx = x1 - x0;
            int dy = y1 - y0;
            float len = (float)Math.sqrt(dx * dx + dy * dy);
            drawRect(Math.min(x0, x1), Math.min(y0, y1), Math.abs(dx), Math.max(thickness, Math.abs(dy)), argb);
        }
    }

    default void drawRectOutline(int x, int y, int w, int h, int thickness, int argb) {
        drawRect(x, y, w, thickness, argb); // Top
        drawRect(x, y + h - thickness, w, thickness, argb); // Bottom
        drawRect(x, y, thickness, h, argb); // Left
        drawRect(x + w - thickness, y, thickness, h, argb); // Right
    }

    default void drawRoundedRectOutline(int x, int y, int w, int h, float radius, int thickness, int argb) {
        // Simplified - just draw the outline as a thin rounded rect frame
        drawRoundedRect(x, y, w, thickness, radius, argb);
        drawRoundedRect(x, y + h - thickness, w, thickness, radius, argb);
        drawRoundedRect(x, y, thickness, h, radius, argb);
        drawRoundedRect(x + w - thickness, y, thickness, h, radius, argb);
    }

    default void drawTexture(Texture texture, int x, int y, int w, int h, int argb) {
        drawTexturedRect(texture, x, y, w, h, argb);
    }
}
