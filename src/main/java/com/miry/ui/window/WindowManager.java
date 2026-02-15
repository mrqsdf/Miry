package com.miry.ui.window;

import com.miry.graphics.Texture;
import com.miry.ui.UiContext;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;
import com.miry.ui.vector.VectorIcons;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple floating window manager: z-order, dragging, resizing, and backdrop blur.
 */
public final class WindowManager {
    private final List<UiWindow> windows = new ArrayList<>();
    private int nextId = 1;

    private UiWindow active;
    private DragMode dragMode = DragMode.NONE;
    private int dragOffX;
    private int dragOffY;
    private boolean blocksInput;

    private enum DragMode {
        NONE,
        MOVE,
        RESIZE_R,
        RESIZE_B,
        RESIZE_RB
    }

    public UiWindow create(String title, int x, int y, int w, int h) {
        UiWindow win = new UiWindow(nextId++, title, x, y, w, h);
        windows.add(win);
        return win;
    }

    public List<UiWindow> windows() {
        return windows;
    }

    public boolean hasWindows() {
        return !windows.isEmpty();
    }

    public void bringToFront(UiWindow win) {
        if (win == null) return;
        if (windows.remove(win)) {
            windows.add(win);
        }
    }

    public void update(UiContext ctx, UiInput input, int windowW, int windowH) {
        blocksInput = false;
        if (input == null || windows.isEmpty()) {
            if (input != null && input.mouseReleased()) {
                dragMode = DragMode.NONE;
                active = null;
            }
            return;
        }

        float mx = input.mousePos().x;
        float my = input.mousePos().y;
        UiWindow hovered = hitTestTop((int) mx, (int) my);
        blocksInput = hovered != null || (active != null && input.mouseDown());

        if (input.mousePressed()) {
            UiWindow hit = hovered;
            if (hit != null) {
                bringToFront(hit);

                if (hitClose(hit, (int) mx, (int) my)) {
                    windows.remove(hit);
                    if (ctx != null && ctx.pointer().isCaptured(hit.id())) {
                        ctx.pointer().release();
                    }
                    active = null;
                    dragMode = DragMode.NONE;
                    return;
                }

                active = hit;
                dragMode = hitDragMode(hit, (int) mx, (int) my);
                dragOffX = (int) mx - hit.x();
                dragOffY = (int) my - hit.y();
                if (ctx != null) {
                    ctx.pointer().capture(hit.id());
                }
            }
        }

        boolean dragging = active != null && dragMode != DragMode.NONE && input.mouseDown();
        if (dragging) {
            switch (dragMode) {
                case MOVE -> {
                    if (active.movable()) {
                        active.setPosition(
                            clamp((int) mx - dragOffX, 0, Math.max(0, windowW - active.width())),
                            clamp((int) my - dragOffY, 0, Math.max(0, windowH - active.height()))
                        );
                    }
                }
                case RESIZE_R -> {
                    if (active.resizable()) {
                        int newW = Math.max(80, (int) mx - active.x());
                        active.setSize(Math.min(newW, Math.max(80, windowW - active.x())), active.height());
                    }
                }
                case RESIZE_B -> {
                    if (active.resizable()) {
                        int newH = Math.max(60, (int) my - active.y());
                        active.setSize(active.width(), Math.min(newH, Math.max(60, windowH - active.y())));
                    }
                }
                case RESIZE_RB -> {
                    if (active.resizable()) {
                        int newW = Math.max(80, (int) mx - active.x());
                        int newH = Math.max(60, (int) my - active.y());
                        active.setSize(
                            Math.min(newW, Math.max(80, windowW - active.x())),
                            Math.min(newH, Math.max(60, windowH - active.y()))
                        );
                    }
                }
            }
        }

        if (input.mouseReleased()) {
            dragMode = DragMode.NONE;
            if (ctx != null && active != null && ctx.pointer().isCaptured(active.id())) {
                ctx.pointer().release();
            }
            active = null;
        }
    }

    /**
     * True when a window is hovered or being interacted with, and background layers should not handle input.
     */
    public boolean blocksInput() {
        return blocksInput;
    }

    private UiWindow hitTestTop(int mx, int my) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            UiWindow w = windows.get(i);
            if (mx >= w.x() && my >= w.y() && mx < w.x() + w.width() && my < w.y() + w.height()) {
                return w;
            }
        }
        return null;
    }

    private static DragMode hitDragMode(UiWindow w, int mx, int my) {
        int titleH = 28;
        int resize = 12;
        boolean inTitle = my >= w.y() && my < w.y() + titleH;
        boolean inResizeR = mx >= w.x() + w.width() - resize;
        boolean inResizeB = my >= w.y() + w.height() - resize;
        if (w.resizable() && inResizeR && inResizeB) return DragMode.RESIZE_RB;
        if (w.resizable() && inResizeR) return DragMode.RESIZE_R;
        if (w.resizable() && inResizeB) return DragMode.RESIZE_B;
        if (w.movable() && inTitle) return DragMode.MOVE;
        return DragMode.NONE;
    }

    private static boolean hitClose(UiWindow w, int mx, int my) {
        int titleH = 28;
        int size = 16;
        int pad = 6;
        int x0 = w.x() + w.width() - pad - size;
        int y0 = w.y() + (titleH - size) / 2;
        return mx >= x0 && my >= y0 && mx < x0 + size && my < y0 + size;
    }

    public void render(UiRenderer r,
                       UiContext ctx,
                       UiInput input,
                       Theme theme,
                       int windowW,
                       int windowH,
                       Texture blurredBackdrop) {
        if (r == null || theme == null) {
            return;
        }

        int border = theme.design.border_thin;
        float radius = theme.design.radius_sm;
        int outline = Theme.toArgb(theme.widgetOutline);
        int titleText = Theme.toArgb(theme.text);

        for (UiWindow w : windows) {
            int x = w.x();
            int y = w.y();
            int ww = w.width();
            int wh = w.height();

            if (w.backdropBlur() && blurredBackdrop != null) {
                float u0 = x / (float) Math.max(1, windowW);
                float v0 = 1.0f - (y / (float) Math.max(1, windowH));
                float u1 = (x + ww) / (float) Math.max(1, windowW);
                float v1 = 1.0f - ((y + wh) / (float) Math.max(1, windowH));
                r.drawRoundedRect(x, y, ww, wh, radius, 0xAA1D1D1D, border, outline);
                r.flush();
                r.pushClip(x + border, y + border, Math.max(0, ww - border * 2), Math.max(0, wh - border * 2));
                r.drawTexturedRect(blurredBackdrop, x, y, ww, wh, u0, v0, u1, v1, 0xCCFFFFFF);
                r.flush();
                r.popClip();
            }

            int panel = Theme.toArgb(theme.panelBg);
            int top = Theme.lightenArgb(panel, 0.04f);
            int bottom = Theme.darkenArgb(panel, 0.04f);
            r.drawRoundedRect(x, y, ww, wh, radius, top, top, bottom, bottom, border, outline);

            // Titlebar gradient.
            int titleH = 28;
            int titleBgA = Theme.toArgb(theme.headerBg);
            int titleTop = Theme.lightenArgb(titleBgA, 0.06f);
            int titleBottom = Theme.darkenArgb(titleBgA, 0.06f);
            r.drawLinearGradientRoundedRect(x, y, ww, titleH, radius, titleTop, titleBottom, 0.0f, 1.0f, border, outline);
            float baseline = r.baselineForBox(y, titleH);
            r.drawText(w.title(), x + theme.design.space_sm, baseline, titleText);

            // Close button.
            {
                int icon = theme.design.icon_sm;
                int pad = Math.max(4, theme.design.space_sm - 2);
                int bx = x + ww - pad - icon;
                int by = y + (titleH - icon) / 2;

                boolean hovered = false;
                if (input != null) {
                    float mx = input.mousePos().x;
                    float my = input.mousePos().y;
                    hovered = mx >= bx && my >= by && mx < bx + icon && my < by + icon;
                }
                if (hovered) {
                    int hoverBg = Theme.toArgb(theme.widgetHover);
                    r.drawRoundedRect(bx - 3, by - 3, icon + 6, icon + 6, theme.design.radius_sm, hoverBg);
                }

                int iconColor = hovered ? Theme.toArgb(theme.text) : Theme.toArgb(theme.textMuted);
                VectorIcons.CLOSE.drawStroke(r, bx, by, icon, 2.0f, iconColor);
            }

            int pad = theme.design.space_md;
            int cx = x + pad;
            int cy = y + titleH + pad;
            int cw = Math.max(0, ww - pad * 2);
            int ch = Math.max(0, wh - titleH - pad * 2);
            UiWindow.Content content = w.content();
            if (content != null && cw > 0 && ch > 0) {
                r.flush();
                r.pushClip(cx, cy, cw, ch);
                content.render(r, ctx, input, theme, cx, cy, cw, ch);
                r.flush();
                r.popClip();
            }

            // Resize handle hint.
            if (w.resizable()) {
                int h = 10;
                r.drawRect(x + ww - h, y + wh - 1, h, 1, Theme.toArgb(theme.textMuted));
                r.drawRect(x + ww - 1, y + wh - h, 1, h, Theme.toArgb(theme.textMuted));
            }
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
