package com.miry.ui.util;

import com.miry.ui.render.UiRenderer;

/**
 * Rendering utilities for common UI drawing operations.
 */
public final class RenderUtils {
    private RenderUtils() {
        throw new AssertionError("No instances");
    }

    /**
     * Draws a rectangular outline with specified thickness.
     *
     * @param r renderer
     * @param x rectangle x
     * @param y rectangle y
     * @param w rectangle width
     * @param h rectangle height
     * @param thickness outline thickness in pixels
     * @param argb outline color
     */
    public static void drawOutline(UiRenderer r, float x, float y, float w, float h,
                                   int thickness, int argb) {
        if (r == null) {
            return;
        }
        int t = Math.max(1, thickness);
        r.drawRect(x, y, w, t, argb);                    // top
        r.drawRect(x, y + h - t, w, t, argb);            // bottom
        r.drawRect(x, y, t, h, argb);                    // left
        r.drawRect(x + w - t, y, t, h, argb);            // right
    }

    /**
     * Draws a soft drop shadow using layered expanded rectangles.
     *
     * @param r renderer
     * @param x rectangle x
     * @param y rectangle y
     * @param w rectangle width
     * @param h rectangle height
     * @param shadowArgb shadow color (typically semi-transparent black)
     * @param dx shadow offset x
     * @param dy shadow offset y
     * @param blurPx blur radius in pixels
     * @param radiusPx corner radius
     * @param overallAlpha overall alpha multiplier (0.0 to 1.0)
     */
    public static void drawDropShadow(UiRenderer r,
                                      float x, float y, float w, float h,
                                      int shadowArgb, float dx, float dy,
                                      int blurPx, float radiusPx,
                                      float overallAlpha) {
        if (r == null) {
            return;
        }

        float oa = MathUtils.clamp01(overallAlpha);
        float radius = Math.max(0.0f, radiusPx);
        int blur = Math.max(0, blurPx);

        if (blur == 0) {
            r.drawRoundedRect(x + dx, y + dy, w, h, radius, ColorUtils.mulAlpha(shadowArgb, oa));
            return;
        }

        // Draw multiple layers for blur effect
        int layers = Math.min(4, Math.max(2, (blur + 3) / 4));
        for (int i = layers; i >= 0; i--) {
            float t = i / (float) layers;
            float expand = t * blur;
            float alphaMul = 0.15f + (1.0f - t) * 0.40f;
            int c = ColorUtils.mulAlpha(shadowArgb, alphaMul * oa);
            r.drawRoundedRect(x + dx - expand, y + dy - expand,
                             w + expand * 2.0f, h + expand * 2.0f,
                             radius + expand, c);
        }
    }

    /**
     * Draws a button with beveled edges (gradient + highlight).
     *
     * @param r renderer
     * @param x button x
     * @param y button y
     * @param w button width
     * @param h button height
     * @param radius corner radius
     * @param borderPx border thickness
     * @param bg background color
     * @param outline outline color
     */
    public static void drawBevelButton(UiRenderer r,
                                       float x, float y, float w, float h,
                                       float radius, int borderPx,
                                       int bg, int outline) {
        if (r == null) {
            return;
        }

        int t = Math.max(1, borderPx);
        if (w <= t * 2.0f || h <= t * 2.0f) {
            r.drawRect(x, y, w, h, bg);
            return;
        }

        // Draw gradient (lighter top, darker bottom)
        int top = ColorUtils.lighten(bg, 0.06f);
        int bottom = ColorUtils.darken(bg, 0.06f);
        r.drawRoundedRect(x, y, w, h, radius, top, top, bottom, bottom, t, outline);

        // Draw highlight at top
        int hl = ColorUtils.lighten(bg, 0.10f);
        int hlA = ColorUtils.getAlpha(hl);
        hl = ColorUtils.withAlpha(hl, Math.min(255, (int) (hlA * 0.35f)));
        r.drawRoundedRect(x + t, y + t, w - t * 2.0f, 1.0f, Math.max(0.0f, radius - t - 1.0f), hl);
    }

    /**
     * Draws a focus ring around a rectangle.
     * Typically used for keyboard navigation focus indication.
     *
     * @param r renderer
     * @param x rectangle x
     * @param y rectangle y
     * @param w rectangle width
     * @param h rectangle height
     * @param radius corner radius
     * @param thickness ring thickness
     * @param argb ring color
     */
    public static void drawFocusRing(UiRenderer r, float x, float y, float w, float h,
                                     float radius, int thickness, int argb) {
        if (r == null) {
            return;
        }
        int t = Math.max(1, thickness);
        float offset = t / 2.0f;
        r.drawRoundedRect(x - offset, y - offset,
                         w + t, h + t,
                         radius + offset, 0, t, argb);
    }

    /**
     * Draws a gradient rectangle (vertical gradient).
     *
     * @param r renderer
     * @param x rectangle x
     * @param y rectangle y
     * @param w rectangle width
     * @param h rectangle height
     * @param topColor top color
     * @param bottomColor bottom color
     */
    public static void drawVerticalGradient(UiRenderer r, float x, float y, float w, float h,
                                           int topColor, int bottomColor) {
        if (r == null) {
            return;
        }
        r.drawRoundedRect(x, y, w, h, 0.0f, topColor, topColor, bottomColor, bottomColor, 0, 0);
    }

    /**
     * Draws a horizontal gradient rectangle.
     *
     * @param r renderer
     * @param x rectangle x
     * @param y rectangle y
     * @param w rectangle width
     * @param h rectangle height
     * @param leftColor left color
     * @param rightColor right color
     */
    public static void drawHorizontalGradient(UiRenderer r, float x, float y, float w, float h,
                                             int leftColor, int rightColor) {
        if (r == null) {
            return;
        }
        r.drawRoundedRect(x, y, w, h, 0.0f, leftColor, rightColor, rightColor, leftColor, 0, 0);
    }

    /**
     * Draws a separator line (horizontal or vertical).
     *
     * @param r renderer
     * @param x start x
     * @param y start y
     * @param length line length
     * @param thickness line thickness
     * @param horizontal true for horizontal, false for vertical
     * @param argb line color
     */
    public static void drawSeparator(UiRenderer r, float x, float y, float length,
                                    int thickness, boolean horizontal, int argb) {
        if (r == null) {
            return;
        }
        int t = Math.max(1, thickness);
        if (horizontal) {
            r.drawRect(x, y, length, t, argb);
        } else {
            r.drawRect(x, y, t, length, argb);
        }
    }
}
