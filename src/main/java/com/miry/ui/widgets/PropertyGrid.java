package com.miry.ui.widgets;

import com.miry.ui.Ui;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

import java.util.Objects;

/**
 * Two-column property layout helper (Godot-ish inspector rows).
 * <p>
 * This is not a retained widget by itself; it is a small helper that draws consistent row chrome and
 * lets the caller render the value cell via a callback (e.g. {@link TextField}, {@link ComboBox}).
 * </p>
 */
public final class PropertyGrid {
    @FunctionalInterface
    public interface ValueRenderer {
        void render(int x, int y, int w, int h);
    }

    private int labelWidthPx = 160;

    public int labelWidthPx() {
        return labelWidthPx;
    }

    public PropertyGrid setLabelWidthPx(int labelWidthPx) {
        this.labelWidthPx = Math.max(60, labelWidthPx);
        return this;
    }

    public void row(Ui ui,
                    UiRenderer r,
                    Theme theme,
                    int x,
                    int y,
                    int w,
                    int h,
                    String label,
                    boolean enabled,
                    ValueRenderer valueRenderer) {
        Objects.requireNonNull(r, "r");
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(valueRenderer, "valueRenderer");

        int hh = Math.max(1, h);
        int ww = Math.max(1, w);

        int pad = theme.design.space_sm;
        int divider = Math.max(1, theme.design.border_thin);
        int outline = Theme.mulAlpha(Theme.toArgb(theme.widgetOutline), 0.75f);

        int labelW = Math.min(labelWidthPx, Math.max(60, ww - 110));
        int dividerX = x + labelW;

        boolean hovered = false;
        if (ui != null) {
            float mx = ui.mouse().x;
            float my = ui.mouse().y;
            hovered = mx >= x && my >= y && mx < x + ww && my < y + hh;
        }

        int bg = Theme.mulAlpha(Theme.toArgb(theme.widgetBg), 0.25f);
        if (hovered) {
            bg = Theme.mulAlpha(Theme.toArgb(theme.widgetHover), 0.30f);
        }
        if (!enabled) {
            bg = Theme.mulAlpha(bg, 0.55f);
        }
        r.drawRect(x, y, ww, hh, bg);
        r.drawRect(dividerX, y, divider, hh, outline);
        r.drawRect(x, y + hh - divider, ww, divider, outline);

        int text = Theme.toArgb(enabled ? theme.text : theme.textMuted);
        float baseline = r.baselineForBox(y, hh);
        String lbl = label == null ? "" : label;
        r.drawText(lbl, x + pad, baseline, text);

        int valueX = dividerX + divider + pad;
        int valueW = Math.max(1, x + ww - pad - valueX);
        valueRenderer.render(valueX, y + 1, valueW, Math.max(1, hh - 2));
    }
}

