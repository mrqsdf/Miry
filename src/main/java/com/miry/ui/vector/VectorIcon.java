package com.miry.ui.vector;

import com.miry.ui.render.UiRenderer;

/**
 * Stroke-only vector icon rendered using capsule segments.
 */
public final class VectorIcon {
    private final float viewW;
    private final float viewH;
    private final VectorPath path;

    public VectorIcon(float viewW, float viewH, VectorPath path) {
        this.viewW = Math.max(1e-3f, viewW);
        this.viewH = Math.max(1e-3f, viewH);
        this.path = path == null ? new VectorPath(java.util.List.of()) : path;
    }

    public void drawStroke(UiRenderer r, float x, float y, float sizePx, float strokePx, int argb) {
        if (r == null) return;
        float size = Math.max(0.0f, sizePx);
        float s = Math.max(1e-4f, Math.min(size / viewW, size / viewH));
        float w = viewW * s;
        float h = viewH * s;
        float ox = x + (size - w) * 0.5f;
        float oy = y + (size - h) * 0.5f;

        float radius = Math.max(0.01f, strokePx * 0.5f);
        for (VectorPath.Contour c : path.contours()) {
            float[] pts = c.points();
            int n = pts.length / 2;
            if (n < 2) continue;
            for (int i = 0; i < n - 1; i++) {
                float ax = ox + pts[i * 2] * s;
                float ay = oy + pts[i * 2 + 1] * s;
                float bx = ox + pts[(i + 1) * 2] * s;
                float by = oy + pts[(i + 1) * 2 + 1] * s;
                if (dist2(ax, ay, bx, by) < 1e-6f) continue;
                r.drawCapsule(ax, ay, bx, by, radius, argb);
            }
            if (c.closed()) {
                float ax = ox + pts[(n - 1) * 2] * s;
                float ay = oy + pts[(n - 1) * 2 + 1] * s;
                float bx = ox + pts[0] * s;
                float by = oy + pts[1] * s;
                if (dist2(ax, ay, bx, by) >= 1e-6f) {
                    r.drawCapsule(ax, ay, bx, by, radius, argb);
                }
            }
        }
    }

    /**
     * Draws a filled icon by triangulating closed contours.
     * <p>
     * For best visual results, pair this with {@link #drawStroke(UiRenderer, float, float, float, float, int)}
     * to fake edge anti-aliasing when MSAA isn't enabled.
     */
    public void drawFill(UiRenderer r, float x, float y, float sizePx, int argb) {
        if (r == null) return;
        float size = Math.max(0.0f, sizePx);
        float s = Math.max(1e-4f, Math.min(size / viewW, size / viewH));
        float w = viewW * s;
        float h = viewH * s;
        float ox = x + (size - w) * 0.5f;
        float oy = y + (size - h) * 0.5f;

        for (VectorPath.Contour c : path.contours()) {
            if (!c.closed()) continue;
            float[] pts = c.points();
            int n = pts.length / 2;
            if (n < 3) continue;
            int[] tris = PolygonTriangulator.triangulate(pts);
            for (int i = 0; i + 2 < tris.length; i += 3) {
                int ia = tris[i];
                int ib = tris[i + 1];
                int ic = tris[i + 2];
                float ax = ox + pts[ia * 2] * s;
                float ay = oy + pts[ia * 2 + 1] * s;
                float bx = ox + pts[ib * 2] * s;
                float by = oy + pts[ib * 2 + 1] * s;
                float cx = ox + pts[ic * 2] * s;
                float cy = oy + pts[ic * 2 + 1] * s;
                r.drawTriangle(ax, ay, bx, by, cx, cy, argb);
            }
        }
    }

    private static float dist2(float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        return dx * dx + dy * dy;
    }
}
