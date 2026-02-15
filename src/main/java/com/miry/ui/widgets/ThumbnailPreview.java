package com.miry.ui.widgets;

import com.miry.graphics.Texture;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * Simple thumbnail/preview widget with checkerboard background (for alpha textures).
 */
public final class ThumbnailPreview {
    private Texture texture;

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public Texture texture() {
        return texture;
    }

    public void render(UiRenderer r, Theme theme, int x, int y, int w, int h) {
        if (r == null || theme == null) return;

        int border = theme.design.border_thin;
        float radius = theme.design.radius_sm;
        int outline = Theme.toArgb(theme.widgetOutline);
        int bg = Theme.toArgb(theme.widgetBg);
        r.drawRoundedRect(x, y, w, h, radius, bg, border, outline);

        int innerX = x + border;
        int innerY = y + border;
        int innerW = Math.max(0, w - border * 2);
        int innerH = Math.max(0, h - border * 2);

        // Checkerboard.
        int a = Theme.mulAlpha(Theme.toArgb(theme.textMuted), 0.10f);
        int b = Theme.mulAlpha(Theme.toArgb(theme.textMuted), 0.18f);
        int sz = 10;
        for (int yy = 0; yy < innerH; yy += sz) {
            for (int xx = 0; xx < innerW; xx += sz) {
                boolean odd = (((xx / sz) + (yy / sz)) & 1) == 1;
                r.drawRect(innerX + xx, innerY + yy, Math.min(sz, innerW - xx), Math.min(sz, innerH - yy), odd ? a : b);
            }
        }

        if (texture != null && innerW > 0 && innerH > 0) {
            // Fit + center while preserving aspect.
            float tw = Math.max(1, texture.width());
            float th = Math.max(1, texture.height());
            float s = Math.min(innerW / tw, innerH / th);
            int dw = Math.max(1, Math.round(tw * s));
            int dh = Math.max(1, Math.round(th * s));
            int dx = innerX + (innerW - dw) / 2;
            int dy = innerY + (innerH - dh) / 2;
            r.drawTexturedRect(texture, dx, dy, dw, dh, 0xFFFFFFFF);
        }
    }
}

