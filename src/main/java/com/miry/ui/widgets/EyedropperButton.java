package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Icon;
import com.miry.ui.theme.Theme;

/**
 * Eyedropper button: toggles a "picking" mode and emits a sample request for the host to fulfill
 * via {@code glReadPixels} after the frame is rendered.
 */
public final class EyedropperButton extends BaseWidget {
    public record SampleRequest(float x, float y) {}

    private boolean picking;
    private SampleRequest pending;

    public boolean isPicking() {
        return picking;
    }

    public SampleRequest consumeSampleRequest() {
        SampleRequest r = pending;
        pending = null;
        return r;
    }

    public void cancel(UiContext ctx) {
        picking = false;
        pending = null;
        if (ctx != null && ctx.pointer().isCaptured(id())) {
            ctx.pointer().release();
        }
    }

    public void render(UiRenderer r,
                       UiContext ctx,
                       UiInput input,
                       Theme theme,
                       int x,
                       int y,
                       int w,
                       int h,
                       boolean interactive) {
        if (r == null || theme == null) return;
        registerFocusable(ctx);

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;

        boolean hovered = canInteract && pxInside(mx, my, x, y, w, h);
        boolean pressed = hovered && input.mouseDown();
        stepTransitions(ctx, theme, hovered, pressed);

        if (canInteract && input.mousePressed()) {
            if (hovered) {
                focus(ctx);
                picking = !picking;
                pending = null;
                if (ctx != null) {
                    if (picking) {
                        ctx.pointer().capture(id());
                    } else if (ctx.pointer().isCaptured(id())) {
                        ctx.pointer().release();
                    }
                }
            } else if (picking) {
                // Click anywhere to sample.
                pending = new SampleRequest(mx, my);
                picking = false;
                if (ctx != null && ctx.pointer().isCaptured(id())) {
                    ctx.pointer().release();
                }
            }
        }

        int bg = Theme.toArgb(theme.widgetBg);
        int outline = Theme.toArgb(theme.widgetOutline);
        if (picking) {
            outline = Theme.toArgb(theme.widgetActive);
        } else if (hovered) {
            bg = Theme.toArgb(theme.widgetHover);
        }

        int border = theme.design.border_thin;
        float radius = theme.design.radius_sm;
        int top = Theme.lightenArgb(bg, 0.06f);
        int bottom = Theme.darkenArgb(bg, 0.06f);
        r.drawRoundedRect(x, y, w, h, radius, top, top, bottom, bottom, border, outline);

        float iconSize = Math.min(theme.design.icon_sm, h - theme.design.space_sm);
        int iconColor = Theme.toArgb(picking ? theme.text : theme.textMuted);
        theme.icons.draw(r, Icon.EYEDROPPER, x + (w - iconSize) * 0.5f, y + (h - iconSize) * 0.5f, iconSize, iconColor);

        drawFocusRing(r, theme, x, y, w, h);
    }
}

