package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * Segmented control: a row of mutually exclusive buttons that touch each other.
 */
public final class SegmentedControl extends BaseWidget {
    private int selectedIndex;

    public int selectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int idx) {
        selectedIndex = Math.max(0, idx);
    }

    public int render(UiRenderer r,
                      UiContext ctx,
                      UiInput input,
                      Theme theme,
                      int x,
                      int y,
                      int width,
                      int height,
                      String[] labels,
                      int selected,
                      boolean interactive) {
        if (r == null || theme == null || labels == null || labels.length == 0) {
            return selected;
        }

        registerFocusable(ctx);

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;
        boolean hoveredAny = canInteract && pxInside(mx, my, x, y, width, height);
        boolean pressedAny = hoveredAny && input.mouseDown();
        stepTransitions(ctx, theme, hoveredAny, pressedAny);

        int n = labels.length;
        int border = theme.design.border_thin;
        float radius = theme.design.radius_sm;
        int outline = Theme.toArgb(theme.widgetOutline);
        int bg = Theme.toArgb(theme.widgetBg);

        // Outer stroke.
        r.drawRoundedRect(x, y, width, height, radius, bg, border, outline);

        int segW = Math.max(1, width / n);
        int rem = width - segW * n;

        int runningX = x;
        for (int i = 0; i < n; i++) {
            int w = segW + (i == n - 1 ? rem : 0);
            boolean hovered = canInteract && pxInside(mx, my, runningX, y, w, height);
            boolean pressed = hovered && input.mouseDown();

            int fill = (i == selected) ? Theme.toArgb(theme.widgetActive)
                : (hovered ? Theme.toArgb(theme.widgetHover) : Theme.toArgb(theme.widgetBg));
            int text = (i == selected) ? 0xFFFFFFFF : Theme.toArgb(theme.text);

            float rPx;
            if (i == 0 || i == n - 1) {
                rPx = radius;
            } else {
                rPx = 0.0f;
            }

            // Draw each segment fill; stroke is handled by the outer rounded rect and separators.
            int top = Theme.lightenArgb(fill, 0.06f);
            int bottom = Theme.darkenArgb(fill, 0.06f);
            r.drawRoundedRect(runningX, y, w, height, rPx, top, top, bottom, bottom, 0.0f, 0);

            // Separator line.
            if (i > 0) {
                r.drawRect(runningX, y + 2, 1, height - 4, Theme.toArgb(theme.headerLine));
            }

            float baseline = r.baselineForBox(y, height);
            String label = labels[i] == null ? "" : labels[i];
            float tx = runningX + (w - r.measureText(label)) * 0.5f;
            r.drawText(label, tx, baseline, text);

            if (hovered && input.mousePressed()) {
                selected = i;
                focus(ctx);
            }

            runningX += w;
        }

        return selected;
    }
}

