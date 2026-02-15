package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * Flat tab strip (no close buttons) intended for dock headers and panel tabs.
 * <p>
 * The visual style is configurable via {@link Style} so callers can emulate editor UIs (e.g. Godot-like).
 */
public final class StripTabs extends BaseWidget {
    public static final class Style {
        public int containerBg;
        public int tabActiveBg;
        public int tabInactiveBg;
        public int tabHoverBg;
        public int borderColor;
        public int highlightColor;
        public int textActive;
        public int textInactive;

        public boolean equalWidth = true;
        public boolean highlightTop = true;
        public int highlightThickness = 2;

        public int paddingX = 10;
        public int paddingY = 0;
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
                      boolean interactive,
                      Style style) {
        if (r == null || theme == null || labels == null || labels.length == 0 || style == null) {
            return selected;
        }

        registerFocusable(ctx);

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = canInteract ? input.mousePos().x : -1;
        float my = canInteract ? input.mousePos().y : -1;

        r.drawRect(x, y, width, height, style.containerBg);
        if (style.borderColor != 0) {
            r.drawRect(x, y + height - 1, width, 1, style.borderColor);
        }

        int n = labels.length;
        int cursorX = x;
        int segW = style.equalWidth ? Math.max(1, width / n) : -1;
        int rem = style.equalWidth ? (width - segW * n) : 0;

        for (int i = 0; i < n; i++) {
            int tabW;
            if (style.equalWidth) {
                tabW = segW + (i == n - 1 ? rem : 0);
            } else {
                String label = labels[i] == null ? "" : labels[i];
                tabW = Math.round(r.measureText(label)) + style.paddingX * 2;
                tabW = Math.min(tabW, Math.max(1, x + width - cursorX));
            }

            int tabX = cursorX;
            int tabY = y;
            int tabH = height;

            boolean hovered = canInteract && pxInside(mx, my, tabX, tabY, tabW, tabH);
            boolean active = i == selected;
            int bg = active ? style.tabActiveBg : (hovered ? style.tabHoverBg : style.tabInactiveBg);
            if (bg != 0) {
                r.drawRect(tabX, tabY, tabW, tabH, bg);
            }

            if (active && style.highlightThickness > 0) {
                int th = Math.max(1, style.highlightThickness);
                if (style.highlightTop) {
                    r.drawRect(tabX, tabY, tabW, th, style.highlightColor);
                } else {
                    r.drawRect(tabX, tabY + tabH - th, tabW, th, style.highlightColor);
                }
            }

            String label = labels[i] == null ? "" : labels[i];
            int textColor = active ? style.textActive : style.textInactive;
            float baseline = r.baselineForBox(tabY + style.paddingY, tabH - style.paddingY * 2);
            float tx = tabX + (tabW - r.measureText(label)) * 0.5f;
            r.drawText(label, tx, baseline, textColor);

            if (hovered && canInteract && input.mousePressed()) {
                selected = i;
                focus(ctx);
            }

            cursorX += tabW;
            if (!style.equalWidth && cursorX >= x + width) {
                break;
            }
        }

        return selected;
    }
}

