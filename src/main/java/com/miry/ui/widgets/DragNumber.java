package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * Drag-to-adjust numeric value widget.
 */
public final class DragNumber extends BaseWidget {
    private float value;
    private float minValue;
    private float maxValue;
    private float dragSpeed = 0.1f;
    private boolean dragging;
    private float dragStartX;
    private float dragStartValue;

    public DragNumber(float initialValue, float min, float max) {
        this.value = clamp(initialValue, min, max);
        this.minValue = min;
        this.maxValue = max;
    }

    public void startDrag(float mouseX) {
        dragging = true;
        dragStartX = mouseX;
        dragStartValue = value;
    }

    public void updateDrag(float mouseX) {
        if (!dragging) return;
        float delta = (mouseX - dragStartX) * dragSpeed;
        value = clamp(dragStartValue + delta, minValue, maxValue);
    }

    public void endDrag() {
        dragging = false;
    }

    public void render(UiRenderer r,
                       UiContext ctx,
                       UiInput input,
                       Theme theme,
                       int x,
                       int y,
                       int width,
                       int height,
                       String label,
                       boolean interactive) {
        if (r == null || theme == null) {
            return;
        }

        registerFocusable(ctx);

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;
        boolean hovered = canInteract && pxInside(mx, my, x, y, width, height);
        boolean pressed = hovered && input.mouseDown();
        stepTransitions(ctx, theme, hovered, pressed);

        if (hovered && canInteract && input.mousePressed()) {
            focus(ctx);
            startDrag(mx);
            if (ctx != null) {
                ctx.pointer().capture(id());
            }
        }
        if (canInteract && dragging && input.mouseDown()) {
            updateDrag(mx);
        }
        if (!canInteract || (input != null && input.mouseReleased())) {
            endDrag();
            if (ctx != null && ctx.pointer().isCaptured(id())) {
                ctx.pointer().release();
            }
        }

        int bg = enabled()
            ? Theme.lerpArgb(theme.widgetBg, theme.widgetHover, hoverT())
            : Theme.toArgb(theme.disabledBg);
        if (enabled() && (pressT() > 0.001f || dragging)) {
            bg = Theme.lerpArgb(theme.widgetHover, theme.widgetActive, Math.max(pressT(), dragging ? 1.0f : 0.0f) * 0.14f);
        }
        int outline = Theme.toArgb((isFocused(ctx) || dragging) ? theme.widgetActive : theme.widgetOutline);
        int textColor = enabled() ? Theme.toArgb(theme.text) : Theme.toArgb(theme.disabledFg);

        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, x, y, width, height, bg, outline, 1);
        } else {
            r.drawRect(x, y, width, height, bg);
            drawOutline(r, x, y, width, height, 1, outline);
        }
        if (enabled() && !pressed) {
            r.drawRect(x + 1, y + 1, width - 2, 1, 0x22000000);
        }
        drawFocusRing(r, theme, x, y, width, height);

        String text = String.format("%s: %.2f", label, value);
        float baselineY = r.baselineForBox(y, height);
        r.drawText(text, x + 8, baselineY, textColor);

        if (dragging) {
            r.drawRect(x, y + height - 2, width, 2, Theme.toArgb(theme.widgetActive));
        }
    }

    public void render(UiRenderer r, int x, int y, int width, int height, int bgColor, int textColor, String label) {
        r.drawRect(x, y, width, height, bgColor);
        r.drawRect(x, y, width, 1, 0xFF3A3A42);

        String text = String.format("%s: %.2f", label, value);
        float baselineY = r.baselineForBox(y, height);
        r.drawText(text, x + 8, baselineY, textColor);

        if (dragging) {
            r.drawRect(x, y + height - 2, width, 2, 0xFF4C9AFF);
        }
    }

    public float value() { return value; }
    public void setValue(float v) { value = clamp(v, minValue, maxValue); }
    public float minValue() { return minValue; }
    public float maxValue() { return maxValue; }
    public void setRange(float min, float max) {
        minValue = min;
        maxValue = max;
        value = clamp(value, min, max);
    }
    public void setDragSpeed(float speed) { dragSpeed = Math.max(0.001f, speed); }
    public boolean isDragging() { return dragging; }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
