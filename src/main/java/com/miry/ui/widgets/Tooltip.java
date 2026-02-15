package com.miry.ui.widgets;

import com.miry.ui.core.BaseWidget;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * Simple tooltip with a configurable delay.
 */
public final class Tooltip extends BaseWidget {
    private String text;
    private int x, y;
    private float hoverTime;
    private float showDelay = 0.5f;
    private boolean visible;

    public Tooltip() {
        setFocusable(false);
    }

    public void show(String text, int x, int y) {
        if (this.text == null || !this.text.equals(text)) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.hoverTime = 0.0f;
            this.visible = false;
        }
    }

    public void hide() {
        text = null;
        visible = false;
        hoverTime = 0.0f;
    }

    public void update(float dt) {
        if (text != null) {
            hoverTime += dt;
            if (hoverTime >= showDelay) {
                visible = true;
            }
        }
    }

    public void render(UiRenderer r, int bgColor, int textColor) {
        render(r, null, bgColor, textColor);
    }

    public void render(UiRenderer r, Theme theme, int bgColor, int textColor) {
        if (r == null || text == null || text.isEmpty()) {
            return;
        }
        if (!visible) {
            return;
        }

        float fade = theme != null ? theme.design.anim_fast : 0.12f;
        float alphaT = fade > 0.0f ? clamp01((hoverTime - showDelay) / fade) : 1.0f;
        if (alphaT <= 0.001f) {
            return;
        }

        int padX = theme != null ? theme.design.space_md : 8;
        int padY = theme != null ? theme.design.space_sm : 4;
        int maxW = theme != null ? (theme.design.space_2xl * 8) : 280;

        String shown = text;
        float avail = Math.max(0.0f, maxW - padX * 2.0f);
        if (r.measureText(shown) > avail) {
            shown = truncateToWidth(r, shown, avail);
        }

        int width = Math.min(maxW, Math.round(r.measureText(shown) + padX * 2.0f));
        int height = Math.round(r.lineHeight() + padY * 2.0f);
        float baselineY = r.baselineForBox(y, height);

        int outline = theme != null ? Theme.toArgb(theme.widgetOutline) : 0xFF3A3A42;
        int shadow = theme != null ? Theme.toArgb(theme.shadow) : 0x2A000000;
        int shadowOffset = theme != null ? theme.design.space_xs : 3;

        float radius = theme != null ? theme.design.radius_sm : 3.0f;
        drawDropShadow(r, x, y, width, height, shadow, 0.0f, shadowOffset, theme != null ? theme.design.shadow_sm : 4, radius, alphaT);
        int t = theme != null ? theme.design.border_thin : 1;
        int o = mulAlpha(outline, alphaT);
        int b = mulAlpha(bgColor, alphaT);
        r.drawRoundedRect(x, y, width, height, radius, b, t, o);
        r.drawText(shown, x + padX, baselineY, mulAlpha(textColor, alphaT));
    }

    public boolean isVisible() {
        return visible;
    }

    public void setShowDelay(float delay) {
        this.showDelay = Math.max(0.0f, delay);
    }

    private static String truncateToWidth(UiRenderer r, String text, float maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String ellipsis = "…";
        float ellW = r.measureText(ellipsis);
        if (ellW > maxWidth) {
            return "";
        }

        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            String s = text.substring(0, mid) + ellipsis;
            if (r.measureText(s) <= maxWidth) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return text.substring(0, lo) + ellipsis;
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
