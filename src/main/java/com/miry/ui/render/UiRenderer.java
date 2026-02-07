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
}
