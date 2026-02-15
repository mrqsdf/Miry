package com.miry.ui.widgets;

import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple toast notification manager.
 * <p>
 * Usage: call {@link #show(String)} then each frame {@link #update(float)} and {@link #render(UiRenderer, Theme, int, int)}.
 */
public final class ToastManager {
    private static final class Toast {
        final String text;
        final float total;
        float t;

        Toast(String text, float total) {
            this.text = text;
            this.total = total;
        }
    }

    private final List<Toast> toasts = new ArrayList<>();

    public int activeCount() {
        return toasts.size();
    }

    public void show(String text) {
        show(text, 2.0f);
    }

    public void show(String text, float seconds) {
        String s = text == null ? "" : text;
        float dur = Math.max(0.25f, seconds);
        toasts.add(new Toast(s, dur));
    }

    public void update(float dt) {
        float d = Math.max(0.0f, dt);
        for (Toast t : toasts) {
            t.t += d;
        }
        for (Iterator<Toast> it = toasts.iterator(); it.hasNext(); ) {
            Toast t = it.next();
            if (t.t >= t.total) {
                it.remove();
            }
        }
    }

    public void render(UiRenderer r, Theme theme, int windowW, int windowH) {
        if (r == null || theme == null || toasts.isEmpty()) {
            return;
        }

        Toast last = toasts.get(toasts.size() - 1);
        float u = last.t / Math.max(1e-4f, last.total);
        float fadeIn = smoothstep(0.0f, 0.12f, u);
        float fadeOut = 1.0f - smoothstep(0.72f, 1.0f, u);
        float a = clamp01(fadeIn * fadeOut);
        if (a <= 0.001f) {
            return;
        }

        int padX = theme.design.space_md;
        int padY = theme.design.space_sm;
        float textW = r.measureText(last.text);
        int boxW = Math.round(textW) + padX * 2;
        int boxH = Math.max(28, padY * 2 + Math.round(r.lineHeight()));

        int x = (windowW - boxW) / 2;
        int y = windowH - boxH - 24;

        float lift = (1.0f - a) * 10.0f;
        float radius = theme.design.radius_sm;
        int bg = Theme.mulAlpha(Theme.toArgb(theme.panelBg), 0.90f * a);
        int outline = Theme.mulAlpha(Theme.toArgb(theme.widgetOutline), 0.80f * a);
        int fg = Theme.mulAlpha(Theme.toArgb(theme.text), a);

        r.drawRoundedRect(x, y - lift, boxW, boxH, radius, bg, theme.design.border_thin, outline);
        float baseline = r.baselineForBox(y - lift, boxH);
        r.drawText(last.text, x + padX, baseline, fg);
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : Math.min(1.0f, v);
    }

    private static float smoothstep(float a, float b, float x) {
        float t = clamp01((x - a) / Math.max(1e-6f, (b - a)));
        return t * t * (3.0f - 2.0f * t);
    }
}

