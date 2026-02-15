package com.miry.ui.widgets;

import com.miry.platform.InputConstants;
import com.miry.ui.UiContext;
import com.miry.ui.clipboard.Clipboard;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;
import com.miry.ui.util.NumericExpression;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.util.Locale;

/**
 * Blender-like number field:
 * <ul>
 *     <li>Click+drag adjusts value (virtual slider) with optional mouse wrapping.</li>
 *     <li>Double-click enters typing mode (supports expressions + units).</li>
 * </ul>
 */
public final class DraggableNumberField extends BaseWidget {
    public interface Listener {
        void onChanged(float value);
    }

    private final TextField editor = new TextField();
    private Listener listener;

    private float value;
    private float minValue;
    private float maxValue;

    private float dragSpeed = 0.1f;
    private float snapStep = 1.0f;
    private int wrapMarginPx = 2;

    private boolean dragging;
    private float dragStartValue;
    private float dragLastX;
    private float dragAccumPx;

    private boolean editing;
    private float secondsSinceClick = 10.0f;
    private float doubleClickWindowSeconds = 0.35f;

    private NumericExpression.UnitKind unitKind = NumericExpression.UnitKind.NONE;
    private int decimals = 3;

    public DraggableNumberField(float initialValue, float min, float max) {
        this.value = clamp(initialValue, min, max);
        this.minValue = min;
        this.maxValue = max;
        editor.setNumericMode(true, unitKind, decimals);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public float value() {
        return value;
    }

    public void setValue(float v) {
        float next = clamp(v, minValue, maxValue);
        if (next != value) {
            value = next;
            if (listener != null) {
                listener.onChanged(value);
            }
        }
    }

    public void setRange(float min, float max) {
        minValue = min;
        maxValue = max;
        setValue(value);
    }

    public void setDragSpeed(float speed) {
        dragSpeed = Math.max(0.001f, speed);
    }

    public void setSnapStep(float step) {
        snapStep = Math.max(1e-6f, step);
    }

    public void setWrapMarginPx(int px) {
        wrapMarginPx = Math.max(1, px);
    }

    public void setDoubleClickWindowSeconds(float seconds) {
        doubleClickWindowSeconds = Math.max(0.15f, seconds);
    }

    public void setNumericFormat(NumericExpression.UnitKind unitKind, int decimals) {
        this.unitKind = unitKind != null ? unitKind : NumericExpression.UnitKind.NONE;
        this.decimals = Math.max(0, Math.min(8, decimals));
        editor.setNumericMode(true, this.unitKind, this.decimals);
    }

    public boolean isEditing() {
        return editing;
    }

    public boolean handleKey(UiContext ctx, KeyEvent e, Clipboard clipboard) {
        if (!enabled() || !editing || e == null || ctx == null) {
            return false;
        }
        if (ctx.focus().focused() != editor.id()) {
            return false;
        }

        if (e.isPressOrRepeat() && e.key() == InputConstants.KEY_ESCAPE) {
            cancelEdit(ctx);
            return true;
        }
        if (e.isPressOrRepeat() && e.key() == InputConstants.KEY_ENTER) {
            commitEdit(ctx);
            return true;
        }

        editor.handleKey(e, clipboard);
        return true;
    }

    public boolean handleTextInput(UiContext ctx, TextInputEvent e) {
        if (!enabled() || !editing || e == null || ctx == null) {
            return false;
        }
        if (ctx.focus().focused() != editor.id()) {
            return false;
        }
        editor.handleTextInput(e);
        return true;
    }

    private void startDrag(float mouseX) {
        dragging = true;
        dragStartValue = value;
        dragLastX = mouseX;
        dragAccumPx = 0.0f;
    }

    private void updateDrag(UiContext ctx, UiInput input, float mouseX, float mouseY) {
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
        setValue(next);

        // Mouse wrapping while captured.
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

    private void endDrag(UiContext ctx) {
        dragging = false;
        if (ctx != null && ctx.pointer().isCaptured(id())) {
            ctx.pointer().release();
        }
    }

    private void beginEdit(UiContext ctx) {
        editing = true;
        editor.setText(formatValue());
        editor.setCursorPos(editor.text().length());
        if (ctx != null) {
            ctx.focus().setFocus(editor.id());
        }
    }

    private void cancelEdit(UiContext ctx) {
        editing = false;
        if (ctx != null && ctx.focus().focused() == editor.id()) {
            ctx.focus().setFocus(id());
        }
    }

    private void commitEdit(UiContext ctx) {
        String s = editor.text().trim();
        if (!s.isEmpty()) {
            try {
                double v = NumericExpression.evaluate(s, unitKind);
                setValue((float) v);
            } catch (IllegalArgumentException ignored) {
                // Keep old value.
            }
        }
        editing = false;
        if (ctx != null && ctx.focus().focused() == editor.id()) {
            ctx.focus().setFocus(id());
        }
    }

    private String formatValue() {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    public void render(UiRenderer r,
                       UiContext ctx,
                       UiInput input,
                       Theme theme,
                       int x,
                       int y,
                       int width,
                       int height,
                       boolean interactive) {
        if (r == null || theme == null) return;
        registerFocusable(ctx);

        float dt = ctx != null ? ctx.lastDt() : 0.0f;
        secondsSinceClick += dt;

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;
        boolean hovered = canInteract && pxInside(mx, my, x, y, width, height);
        boolean pressed = hovered && input.mouseDown();
        stepTransitions(ctx, theme, hovered, pressed);

        if (canInteract && input.mousePressed()) {
            if (hovered) {
                focus(ctx);
                boolean doubleClick = secondsSinceClick <= doubleClickWindowSeconds;
                secondsSinceClick = 0.0f;
                if (doubleClick) {
                    beginEdit(ctx);
                } else if (!editing) {
                    startDrag(mx);
                    if (ctx != null) {
                        ctx.pointer().capture(id());
                    }
                }
            } else if (editing) {
                commitEdit(ctx);
            }
        }

        if (canInteract && dragging && input.mouseDown()) {
            updateDrag(ctx, input, mx, my);
        }
        if (dragging && (!canInteract || input.mouseReleased())) {
            endDrag(ctx);
        }

        int bg = enabled() ? computeStateColor(theme) : Theme.toArgb(theme.disabledBg);
        int outline = enabled() ? computeBorderColor(theme) : Theme.toArgb(theme.widgetOutline);
        if (enabled() && (dragging || editing)) {
            outline = Theme.toArgb(theme.widgetActive);
        }
        int textColor = enabled() ? computeTextColor(theme) : Theme.toArgb(theme.disabledFg);

        int t = theme.design.border_thin;
        float radius = theme.design.radius_sm;
        int top = Theme.lightenArgb(bg, 0.06f);
        int bottom = Theme.darkenArgb(bg, 0.06f);
        r.drawRoundedRect(x, y, width, height, radius, top, top, bottom, bottom, t, outline);
        if (enabled() && !pressed) {
            int hl = Theme.lightenArgb(bg, 0.12f);
            int a = (int) (((hl >>> 24) & 0xFF) * 0.20f);
            r.drawRect(x + t, y + t, width - t * 2, t, (a << 24) | (hl & 0x00FFFFFF));
        }

        // Progress fill behind the text (virtual slider feedback).
        float denom = Math.max(1e-6f, (maxValue - minValue));
        float u = clamp01((value - minValue) / denom);
        int innerPad = t + 1;
        int fillW = Math.round(Math.max(0.0f, (width - innerPad * 2) * u));
        if (fillW > 0) {
            int fill = Theme.mulAlpha(Theme.toArgb(theme.widgetActive), 0.22f);
            float fillRadius = Math.max(0.0f, radius - 1.0f);
            r.drawRoundedRect(x + innerPad, y + innerPad, fillW, height - innerPad * 2, fillRadius, fill);
        }

        drawFocusRing(r, theme, x, y, width, height);

        if (editing) {
            editor.render(r, ctx, input, theme, x, y, width, height, interactive);
        } else {
            String txt = formatValue();
            float baseline = r.baselineForBox(y, height);
            r.drawText(txt, x + theme.design.space_sm, baseline, textColor);
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : Math.min(1.0f, v);
    }
}

