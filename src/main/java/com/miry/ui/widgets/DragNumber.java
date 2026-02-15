package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;
import com.miry.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

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
    private float dragLastX;
    private float dragAccumPx;
    private int wrapMarginPx = 2;
    private float snapStep = 1.0f;

    public DragNumber(float initialValue, float min, float max) {
        this.value = clamp(initialValue, min, max);
        this.minValue = min;
        this.maxValue = max;
    }

    public void startDrag(float mouseX) {
        dragging = true;
        dragStartX = mouseX;
        dragStartValue = value;
        dragLastX = mouseX;
        dragAccumPx = 0.0f;
    }

    public void updateDrag(UiContext ctx, UiInput input, float mouseX, float mouseY) {
        if (!dragging) return;

        boolean shiftDown = ctx != null && (ctx.keyboard().isKeyDown(InputConstants.KEY_LEFT_SHIFT) || ctx.keyboard().isKeyDown(InputConstants.KEY_RIGHT_SHIFT));
        boolean ctrlDown = ctx != null && (ctx.keyboard().isKeyDown(InputConstants.KEY_LEFT_CONTROL) || ctx.keyboard().isKeyDown(InputConstants.KEY_RIGHT_CONTROL));

        float dx = mouseX - dragLastX;
        dragLastX = mouseX;
        dragAccumPx += dx;

        float speed = dragSpeed * (shiftDown ? 0.1f : 1.0f);
        float next = dragStartValue + dragAccumPx * speed;
        if (ctrlDown) {
            float step = snapStep * (shiftDown ? 0.1f : 1.0f);
            step = Math.max(1e-6f, step);
            next = Math.round(next / step) * step;
        }
        value = clamp(next, minValue, maxValue);

        // Mouse wrapping: if the pointer hits a window edge while captured, warp it to the other side so dragging can continue.
        if (ctx != null && input != null && ctx.pointer().isCaptured(id())) {
            long window = ctx.windowHandle();
            if (window != 0L) {
                int margin = Math.max(1, wrapMarginPx);
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    var wBuf = stack.mallocInt(1);
                    var hBuf = stack.mallocInt(1);
                    GLFW.glfwGetWindowSize(window, wBuf, hBuf);
                    int winW = wBuf.get(0);
                    int winH = hBuf.get(0);
                    if (winW > margin * 2 && winH > 0) {
                        double newX = Double.NaN;
                        if (mouseX <= margin) {
                            newX = winW - margin - 1;
                        } else if (mouseX >= winW - margin - 1) {
                            newX = margin + 1;
                        }
                        if (Double.isFinite(newX)) {
                            double newY = Math.max(0.0, Math.min(winH - 1.0, mouseY));
                            GLFW.glfwSetCursorPos(window, newX, newY);
                            dragLastX = (float) newX;
                        }
                    }
                }
            }
        }
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
            updateDrag(ctx, input, mx, my);
        }
        if (!canInteract || (input != null && input.mouseReleased())) {
            endDrag();
            if (ctx != null && ctx.pointer().isCaptured(id())) {
                ctx.pointer().release();
            }
        }

        int bg = enabled() ? computeStateColor(theme) : Theme.toArgb(theme.disabledBg);
        int outline = enabled() ? computeBorderColor(theme) : Theme.toArgb(theme.widgetOutline);
        if (enabled() && dragging) {
            outline = Theme.toArgb(theme.widgetActive);
        }
        int textColor = enabled() ? computeTextColor(theme) : Theme.toArgb(theme.disabledFg);

        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, x, y, width, height, bg, outline, theme.design.border_thin);
        } else {
            int t = theme.design.border_thin;
            float radius = theme.design.radius_sm;
            int top = Theme.lightenArgb(bg, 0.06f);
            int bottom = Theme.darkenArgb(bg, 0.06f);
            r.drawRoundedRect(x, y, width, height, radius, top, top, bottom, bottom, t, outline);
        }
        if (enabled() && !pressed) {
            int t = theme.design.border_thin;
            int hl = Theme.lightenArgb(bg, 0.12f);
            int a = (int) (((hl >>> 24) & 0xFF) * 0.20f);
            r.drawRect(x + t, y + t, width - t * 2, t, (a << 24) | (hl & 0x00FFFFFF));
        }
        drawFocusRing(r, theme, x, y, width, height);

        String text = String.format("%s: %.2f", label, value);
        float baselineY = r.baselineForBox(y, height);
        r.drawText(text, x + theme.design.space_sm, baselineY, textColor);

        if (dragging) {
            int t = Math.max(1, theme.design.border_medium);
            r.drawRect(x, y + height - t, width, t, Theme.toArgb(theme.widgetActive));
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
    public void setSnapStep(float step) { snapStep = Math.max(1e-6f, step); }
    public void setWrapMarginPx(int px) { wrapMarginPx = Math.max(1, px); }
    public boolean isDragging() { return dragging; }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
