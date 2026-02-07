package com.miry.ui.theme;

import com.miry.graphics.Texture;
import com.miry.ui.render.UiRenderer;

/**
 * Nine-slice (9-patch) textured skin that scales without distorting corners.
 * <p>
 * The source region is defined in normalized UVs and split into a 3x3 grid using pixel insets
 * (left/top/right/bottom) measured in source pixels within the region.
 */
public final class NineSlice {
    private final Texture texture;
    private final float u0;
    private final float v0;
    private final float u1;
    private final float v1;
    private final int regionWidthPx;
    private final int regionHeightPx;
    private final int leftPx;
    private final int topPx;
    private final int rightPx;
    private final int bottomPx;

    /**
     * Creates a nine-slice using the full texture (UV 0..1).
     */
    public NineSlice(Texture texture, int leftPx, int topPx, int rightPx, int bottomPx) {
        this(texture, 0.0f, 0.0f, 1.0f, 1.0f,
            texture != null ? texture.width() : 0,
            texture != null ? texture.height() : 0,
            leftPx, topPx, rightPx, bottomPx);
    }

    /**
     * Creates a nine-slice using a sub-rectangle of {@code texture}.
     *
     * @param u0 normalized left UV
     * @param v0 normalized top UV
     * @param u1 normalized right UV
     * @param v1 normalized bottom UV
     * @param regionWidthPx width of the region in pixels
     * @param regionHeightPx height of the region in pixels
     */
    public NineSlice(Texture texture,
                     float u0,
                     float v0,
                     float u1,
                     float v1,
                     int regionWidthPx,
                     int regionHeightPx,
                     int leftPx,
                     int topPx,
                     int rightPx,
                     int bottomPx) {
        this.texture = texture;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        this.regionWidthPx = Math.max(0, regionWidthPx);
        this.regionHeightPx = Math.max(0, regionHeightPx);
        this.leftPx = Math.max(0, leftPx);
        this.topPx = Math.max(0, topPx);
        this.rightPx = Math.max(0, rightPx);
        this.bottomPx = Math.max(0, bottomPx);
    }

    public Texture texture() {
        return texture;
    }

    /**
     * Draws the nine-slice at {@code (x,y,w,h)} tinted by {@code tintArgb}.
     */
    public void draw(UiRenderer r, float x, float y, float w, float h, int tintArgb) {
        if (r == null || texture == null) {
            return;
        }
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }

        int srcW = regionWidthPx > 0 ? regionWidthPx : texture.width();
        int srcH = regionHeightPx > 0 ? regionHeightPx : texture.height();
        if (srcW <= 0 || srcH <= 0) {
            r.drawTexturedRect(texture, x, y, w, h, tintArgb);
            return;
        }

        int l = clamp(leftPx, 0, srcW);
        int t = clamp(topPx, 0, srcH);
        int rr = clamp(rightPx, 0, srcW - l);
        int bb = clamp(bottomPx, 0, srcH - t);

        float dl = Math.min(l, w * 0.5f);
        float dr = Math.min(rr, w - dl);
        float dt = Math.min(t, h * 0.5f);
        float db = Math.min(bb, h - dt);

        float x0 = x;
        float x1 = x + dl;
        float x2 = x + w - dr;
        float x3 = x + w;

        float y0 = y;
        float y1 = y + dt;
        float y2 = y + h - db;
        float y3 = y + h;

        float uu0 = u0;
        float uu1 = lerp(u0, u1, l / (float) srcW);
        float uu2 = lerp(u0, u1, (srcW - rr) / (float) srcW);
        float uu3 = u1;

        float vv0 = v0;
        float vv1 = lerp(v0, v1, t / (float) srcH);
        float vv2 = lerp(v0, v1, (srcH - bb) / (float) srcH);
        float vv3 = v1;

        // corners
        drawPatch(r, x0, y0, x1, y1, uu0, vv0, uu1, vv1, tintArgb);
        drawPatch(r, x2, y0, x3, y1, uu2, vv0, uu3, vv1, tintArgb);
        drawPatch(r, x0, y2, x1, y3, uu0, vv2, uu1, vv3, tintArgb);
        drawPatch(r, x2, y2, x3, y3, uu2, vv2, uu3, vv3, tintArgb);

        // edges
        drawPatch(r, x1, y0, x2, y1, uu1, vv0, uu2, vv1, tintArgb);
        drawPatch(r, x1, y2, x2, y3, uu1, vv2, uu2, vv3, tintArgb);
        drawPatch(r, x0, y1, x1, y2, uu0, vv1, uu1, vv2, tintArgb);
        drawPatch(r, x2, y1, x3, y2, uu2, vv1, uu3, vv2, tintArgb);

        // center
        drawPatch(r, x1, y1, x2, y2, uu1, vv1, uu2, vv2, tintArgb);
    }

    /**
     * Convenience helper that draws an outline (same slice, expanded) and then the fill.
     * <p>
     * This avoids the square-corner outlines produced by drawing a rectangular border on top of a
     * rounded/alpha-masked skin.
     *
     * @param outlinePx outline thickness in pixels
     */
    public void drawWithOutline(UiRenderer r,
                                float x,
                                float y,
                                float w,
                                float h,
                                int fillArgb,
                                int outlineArgb,
                                int outlinePx) {
        int t = Math.max(0, outlinePx);
        if (t > 0) {
            draw(r, x - t, y - t, w + t * 2.0f, h + t * 2.0f, outlineArgb);
        }
        draw(r, x, y, w, h, fillArgb);
    }

    private void drawPatch(UiRenderer r,
                           float x0,
                           float y0,
                           float x1,
                           float y1,
                           float u0,
                           float v0,
                           float u1,
                           float v1,
                           int tintArgb) {
        float w = x1 - x0;
        float h = y1 - y0;
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }
        r.drawTexturedRect(texture, x0, y0, w, h, u0, v0, u1, v1, tintArgb);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
