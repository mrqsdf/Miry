package com.miry.ui.core;

import com.miry.ui.UiContext;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * Small retained-state base class used by the existing widgets in {@code com.miry.ui.widgets}.
 *
 * This is intentionally lightweight: widgets in this repo are explicit-position widgets
 * (render(x,y,w,h) style), so BaseWidget focuses on consistent interaction state:
 * hover/press/focus transitions, disabled state, focus ring, and focus registration.
 */
public abstract class BaseWidget {
    private final int id = WidgetIds.next();
    private boolean enabled = true;
    private boolean focusable = true;

    private float hoverT;
    private float pressT;
    private float focusT;

    public final int id() {
        return id;
    }

    public final boolean enabled() {
        return enabled;
    }

    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public final boolean focusable() {
        return focusable;
    }

    public final void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    /**
     * Register this widget as focusable for the current frame (Tab navigation).
     * Call this during render in paint order.
     */
    public final void registerFocusable(UiContext ctx) {
        if (ctx != null && focusable && enabled) {
            ctx.focus().registerFocusable(id);
        }
    }

    public final boolean isFocused(UiContext ctx) {
        return ctx != null && ctx.focus().isFocused(id);
    }

    public final void focus(UiContext ctx) {
        if (ctx != null && focusable && enabled) {
            ctx.focus().setFocus(id);
        }
    }

    protected final boolean interactive(UiContext ctx, boolean interactive) {
        return interactive
            && enabled
            && (ctx == null || !ctx.pointer().hasCaptured() || ctx.pointer().isCaptured(id));
    }

    /**
     * Step hover/press/focus transitions.
     */
    protected final void stepTransitions(UiContext ctx, Theme theme, boolean hovered, boolean pressed) {
        float dt = ctx != null ? ctx.lastDt() : 0.0f;
        float speed = theme != null ? theme.tokens.animSpeed : 14.0f;

        boolean focused = isFocused(ctx);
        float hTarget = (enabled && hovered) ? 1.0f : 0.0f;
        float pTarget = (enabled && pressed) ? 1.0f : 0.0f;
        float fTarget = (enabled && focused) ? 1.0f : 0.0f;

        hoverT = approachExp(hoverT, hTarget, speed, dt);
        pressT = approachExp(pressT, pTarget, speed, dt);
        focusT = approachExp(focusT, fTarget, speed, dt);
    }

    protected final float hoverT() {
        return hoverT;
    }

    protected final float pressT() {
        return pressT;
    }

    protected final float focusT() {
        return focusT;
    }

    protected final void drawFocusRing(UiRenderer r, Theme theme, float x, float y, float w, float h) {
        if (r == null || theme == null) {
            return;
        }
        if (!enabled || focusT <= 0.01f) {
            return;
        }

        int base = Theme.toArgb(theme.focusRing);
        int alpha = (int) (((base >>> 24) & 0xFF) * clamp01(focusT));
        int c = (alpha << 24) | (base & 0x00FFFFFF);

        int t = Math.max(1, theme.tokens.focusRingThickness);
        float ox = x - t;
        float oy = y - t;
        float ow = w + t * 2.0f;
        float oh = h + t * 2.0f;
        drawOutline(r, ox, oy, ow, oh, t, c);
    }

    protected static boolean pxInside(float px, float py, float x, float y, float w, float h) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    protected static void drawOutline(UiRenderer r, float x, float y, float w, float h, int thickness, int argb) {
        int t = Math.max(1, thickness);
        r.drawRect(x, y, w, t, argb);
        r.drawRect(x, y + h - t, w, t, argb);
        r.drawRect(x, y, t, h, argb);
        r.drawRect(x + w - t, y, t, h, argb);
    }

    private static float approachExp(float value, float target, float speed, float dt) {
        if (dt <= 0.0f) {
            return target;
        }
        float k = 1.0f - (float) Math.exp(-speed * dt);
        return value + (target - value) * k;
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
