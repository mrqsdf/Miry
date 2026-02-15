package com.miry.ui.vector;

import com.miry.ui.render.UiRenderer;

/**
 * Filled vector icon (triangulated) with optional stroke outline.
 */
public final class FilledVectorIcon {
    private final VectorIcon icon;

    public FilledVectorIcon(float viewW, float viewH, VectorPath path) {
        this.icon = new VectorIcon(viewW, viewH, path);
    }

    public void draw(UiRenderer r, float x, float y, float sizePx, float strokePx, int fillArgb, int strokeArgb) {
        if (r == null) return;
        icon.drawFill(r, x, y, sizePx, fillArgb);
        if (((strokeArgb >>> 24) & 0xFF) != 0 && strokePx > 0.01f) {
            icon.drawStroke(r, x, y, sizePx, strokePx, strokeArgb);
        }
    }
}

