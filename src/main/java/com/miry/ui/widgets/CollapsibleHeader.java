package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Icon;
import com.miry.ui.theme.Theme;

/**
 * Collapsible header (accordion-like) with animated content height.
 */
public final class CollapsibleHeader extends BaseWidget {
    @FunctionalInterface
    public interface Content {
        void render(UiRenderer r, int x, int y, int w, int h);
    }

    private boolean expanded = true;
    private float animH;

    public boolean expanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * Renders a header + (optionally) its content.
     *
     * @return total height consumed.
     */
    public int render(UiRenderer r,
                      UiContext ctx,
                      UiInput input,
                      Theme theme,
                      int x,
                      int y,
                      int w,
                      int headerH,
                      int contentTargetH,
                      String title,
                      boolean interactive,
                      Content content) {
        if (r == null || theme == null) {
            return 0;
        }

        registerFocusable(ctx);

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;
        boolean hovered = canInteract && pxInside(mx, my, x, y, w, headerH);
        boolean pressed = hovered && input.mouseDown();
        stepTransitions(ctx, theme, hovered, pressed);

        if (hovered && canInteract && input.mousePressed()) {
            focus(ctx);
            expanded = !expanded;
        }

        int bg = Theme.toArgb(theme.panelBg);
        int outline = Theme.toArgb(theme.widgetOutline);
        int hover = Theme.toArgb(theme.widgetHover);
        int headerBg = hovered ? hover : bg;
        int border = theme.design.border_thin;
        float radius = theme.design.radius_sm;

        r.drawRoundedRect(x, y, w, headerH, radius, headerBg, border, outline);
        float baseline = r.baselineForBox(y, headerH);
        r.drawText(title != null ? title : "", x + theme.design.space_sm, baseline, Theme.toArgb(theme.text));

        float iconSize = Math.min(theme.design.icon_sm, headerH - theme.design.space_sm);
        Icon icon = expanded ? Icon.CHEVRON_DOWN : Icon.CHEVRON_RIGHT;
        theme.icons.draw(r, icon, x + w - iconSize - theme.design.space_sm, y + (headerH - iconSize) * 0.5f, iconSize, Theme.toArgb(theme.textMuted));

        float target = expanded ? Math.max(0.0f, contentTargetH) : 0.0f;
        animH = approachExp(animH, target, 14.0f, ctx != null ? ctx.lastDt() : 0.0f);

        int contentH = Math.round(animH);
        if (contentH > 0 && content != null) {
            int cx = x;
            int cy = y + headerH;
            r.flush();
            r.pushClip(cx, cy, w, contentH);
            content.render(r, cx, cy, w, contentH);
            r.flush();
            r.popClip();
        }

        return headerH + contentH;
    }
}

