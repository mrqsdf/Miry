package com.miry.ui.widgets;

import com.miry.ui.core.BaseWidget;
import com.miry.ui.UiContext;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * Scroll container state helper with interactive scrollbars.
 */
public final class ScrollView extends BaseWidget {
    private float scrollX;
    private float scrollY;
    private float velocityX;
    private float velocityY;
    private int contentWidth;
    private int contentHeight;
    private int viewWidth;
    private int viewHeight;
    private boolean showScrollbars = true;
    private float inertiaDecay = 0.9f;

    private boolean draggingVThumb;
    private boolean draggingHThumb;
    private float dragOffset;

    private int scrollbarThicknessPx = 10;
    private int thumbMinSizePx = 24;

    public ScrollView(int viewWidth, int viewHeight) {
        setFocusable(false);
        this.viewWidth = Math.max(1, viewWidth);
        this.viewHeight = Math.max(1, viewHeight);
    }

    /**
     * Configures scrollbar sizing from design tokens.
     * <p>
     * This keeps the scrollbar math consistent between rendering and hit-testing.
     */
    public void configureFromTheme(Theme theme) {
        if (theme == null) {
            return;
        }
        scrollbarThicknessPx = theme.design.space_sm + theme.design.border_medium; // 8+2=10 default
        thumbMinSizePx = theme.design.widget_height_sm; // 24 default
    }

    public void setScrollbarThicknessPx(int px) {
        scrollbarThicknessPx = Math.max(4, px);
    }

    public void setThumbMinSizePx(int px) {
        thumbMinSizePx = Math.max(4, px);
    }

    public void setContentSize(int width, int height) {
        this.contentWidth = Math.max(0, width);
        this.contentHeight = Math.max(0, height);
        clampScroll();
    }

    public void setViewSize(int width, int height) {
        this.viewWidth = Math.max(1, width);
        this.viewHeight = Math.max(1, height);
        clampScroll();
    }

    public void scroll(float dx, float dy) {
        scrollX += dx;
        scrollY += dy;
        clampScroll();
    }

    public void addVelocity(float vx, float vy) {
        velocityX += vx;
        velocityY += vy;
    }

    public void update(float dt) {
        if (Math.abs(velocityX) > 0.01f || Math.abs(velocityY) > 0.01f) {
            scrollX += velocityX * dt;
            scrollY += velocityY * dt;
            clampScroll();
            velocityX *= inertiaDecay;
            velocityY *= inertiaDecay;
        }
    }

    private void clampScroll() {
        float maxScrollX = Math.max(0, contentWidth - viewWidth);
        float maxScrollY = Math.max(0, contentHeight - viewHeight);
        scrollX = Math.max(0, Math.min(scrollX, maxScrollX));
        scrollY = Math.max(0, Math.min(scrollY, maxScrollY));
    }

    public boolean handleScrollbarInput(UiInput input, int x, int y) {
        return handleScrollbarInput(null, input, x, y);
    }

    /**
     * Handles scrollbar interaction, optionally using {@link UiContext} pointer capture.
     */
    public boolean handleScrollbarInput(UiContext ctx, UiInput input, int x, int y) {
        if (!enabled() || !showScrollbars || input == null) {
            return false;
        }

        boolean canInteract = interactive(ctx, true);
        if (!canInteract) {
            return false;
        }

        float maxScrollX = Math.max(0, contentWidth - viewWidth);
        float maxScrollY = Math.max(0, contentHeight - viewHeight);

        float mx = input.mousePos().x;
        float my = input.mousePos().y;

        boolean pressed = input.mousePressed();
        boolean down = input.mouseDown();
        boolean released = input.mouseReleased();

        if (released) {
            draggingVThumb = false;
            draggingHThumb = false;
            if (ctx != null && ctx.pointer().isCaptured(id())) {
                ctx.pointer().release();
            }
        }

        boolean consumed = false;

        if (maxScrollY > 0) {
            Thumb v = verticalThumb(x, y, maxScrollY);
            if (pressed && v.hitThumb(mx, my)) {
                draggingVThumb = true;
                dragOffset = my - v.thumbY;
                if (ctx != null) {
                    ctx.pointer().capture(id());
                }
                consumed = true;
            } else if (pressed && v.hitTrack(mx, my)) {
                if (my < v.thumbY) {
                    scroll(0, -viewHeight);
                } else if (my > v.thumbY + v.thumbH) {
                    scroll(0, viewHeight);
                }
                consumed = true;
            }

            if (draggingVThumb && down) {
                float trackRange = Math.max(1.0f, v.trackH - v.thumbH);
                float t = (my - v.trackY - dragOffset) / trackRange;
                t = Math.max(0.0f, Math.min(1.0f, t));
                scrollY = t * maxScrollY;
                clampScroll();
                consumed = true;
            }
        }

        if (maxScrollX > 0) {
            Thumb h = horizontalThumb(x, y, maxScrollX);
            if (pressed && h.hitThumb(mx, my)) {
                draggingHThumb = true;
                dragOffset = mx - h.thumbX;
                if (ctx != null) {
                    ctx.pointer().capture(id());
                }
                consumed = true;
            } else if (pressed && h.hitTrack(mx, my)) {
                if (mx < h.thumbX) {
                    scroll(-viewWidth, 0);
                } else if (mx > h.thumbX + h.thumbW) {
                    scroll(viewWidth, 0);
                }
                consumed = true;
            }

            if (draggingHThumb && down) {
                float trackRange = Math.max(1.0f, h.trackW - h.thumbW);
                float t = (mx - h.trackX - dragOffset) / trackRange;
                t = Math.max(0.0f, Math.min(1.0f, t));
                scrollX = t * maxScrollX;
                clampScroll();
                consumed = true;
            }
        }

        return consumed;
    }

    public void renderScrollbars(UiRenderer r, UiInput input, int x, int y, int trackColor, int thumbColor, int thumbHoverColor) {
        if (!showScrollbars) return;
        if (!enabled()) {
            trackColor = dimAlpha(trackColor, 0.45f);
            thumbColor = dimAlpha(thumbColor, 0.45f);
            thumbHoverColor = dimAlpha(thumbHoverColor, 0.45f);
        }

        float maxScrollX = Math.max(0, contentWidth - viewWidth);
        float maxScrollY = Math.max(0, contentHeight - viewHeight);

        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;

        float radius = scrollbarThicknessPx * 0.5f;
        int inset = Math.max(1, scrollbarThicknessPx / 5);

        if (maxScrollY > 0) {
            Thumb v = verticalThumb(x, y, maxScrollY);
            r.drawRoundedRect(v.trackX, v.trackY, v.trackW, v.trackH, radius, trackColor);
            boolean hovered = v.hitThumb(mx, my) || draggingVThumb;
            int tc = hovered ? thumbHoverColor : thumbColor;
            r.drawRoundedRect(v.thumbX + inset, v.thumbY + inset, Math.max(1, v.thumbW - inset * 2), Math.max(1, v.thumbH - inset * 2), radius, tc);
        }

        if (maxScrollX > 0) {
            Thumb h = horizontalThumb(x, y, maxScrollX);
            r.drawRoundedRect(h.trackX, h.trackY, h.trackW, h.trackH, radius, trackColor);
            boolean hovered = h.hitThumb(mx, my) || draggingHThumb;
            int tc = hovered ? thumbHoverColor : thumbColor;
            r.drawRoundedRect(h.thumbX + inset, h.thumbY + inset, Math.max(1, h.thumbW - inset * 2), Math.max(1, h.thumbH - inset * 2), radius, tc);
        }
    }

    private Thumb verticalThumb(int x, int y, float maxScrollY) {
        int barW = scrollbarThicknessPx;
        int trackX = x + viewWidth - barW;
        int trackY = y;
        int trackW = barW;
        int trackH = viewHeight;

        int thumbH = Math.max(thumbMinSizePx, (int) ((float) viewHeight / Math.max(1, contentHeight) * viewHeight));
        thumbH = Math.min(trackH, thumbH);
        int thumbY = (int) (scrollY / maxScrollY * (trackH - thumbH));

        return new Thumb(trackX, trackY, trackW, trackH, trackX, trackY + thumbY, barW, thumbH);
    }

    private Thumb horizontalThumb(int x, int y, float maxScrollX) {
        int barH = scrollbarThicknessPx;
        int trackX = x;
        int trackY = y + viewHeight - barH;
        int trackW = viewWidth;
        int trackH = barH;

        int thumbW = Math.max(thumbMinSizePx, (int) ((float) viewWidth / Math.max(1, contentWidth) * viewWidth));
        thumbW = Math.min(trackW, thumbW);
        int thumbX = (int) (scrollX / maxScrollX * (trackW - thumbW));

        return new Thumb(trackX, trackY, trackW, trackH, trackX + thumbX, trackY, thumbW, barH);
    }

    private record Thumb(int trackX, int trackY, int trackW, int trackH, int thumbX, int thumbY, int thumbW, int thumbH) {
        boolean hitThumb(float mx, float my) {
            return mx >= thumbX && my >= thumbY && mx < thumbX + thumbW && my < thumbY + thumbH;
        }

        boolean hitTrack(float mx, float my) {
            return mx >= trackX && my >= trackY && mx < trackX + trackW && my < trackY + trackH;
        }
    }

    public float scrollX() { return scrollX; }
    public float scrollY() { return scrollY; }
    public int contentWidth() { return contentWidth; }
    public int contentHeight() { return contentHeight; }
    public int viewWidth() { return viewWidth; }
    public int viewHeight() { return viewHeight; }
    public void setShowScrollbars(boolean show) { this.showScrollbars = show; }
    public void setInertiaDecay(float decay) { this.inertiaDecay = Math.max(0.0f, Math.min(1.0f, decay)); }

    private static int dimAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int na = Math.max(0, Math.min(255, Math.round(a * Math.max(0.0f, Math.min(1.0f, factor)))));
        return (na << 24) | (argb & 0x00FFFFFF);
    }
}
