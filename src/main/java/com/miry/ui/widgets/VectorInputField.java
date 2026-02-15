package com.miry.ui.widgets;

import com.miry.platform.InputConstants;
import com.miry.ui.UiContext;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

/**
 * A compact tuple input (X/Y/Z/W) composed of {@link DraggableNumberField}s.
 * <p>
 * Supports "gang dragging" via the left gutter to adjust all components together.
 */
public final class VectorInputField {
    private final String label;
    private final String[] axes;
    private final DraggableNumberField[] fields;

    private boolean gangDragging;
    private float gangDragStartX;
    private float gangDragLastX;
    private float gangDragAccumPx;
    private float[] gangStartValues;
    private float gangSpeed = 0.1f;
    private float gangSnapStep = 1.0f;
    private int wrapMarginPx = 2;

    public VectorInputField(String label, int dimensions, float min, float max, float initial) {
        this.label = label != null ? label : "";
        int dims = Math.max(2, Math.min(4, dimensions));
        this.axes = switch (dims) {
            case 2 -> new String[]{"X", "Y"};
            case 3 -> new String[]{"X", "Y", "Z"};
            default -> new String[]{"X", "Y", "Z", "W"};
        };
        this.fields = new DraggableNumberField[dims];
        for (int i = 0; i < dims; i++) {
            fields[i] = new DraggableNumberField(initial, min, max);
            fields[i].setDragSpeed(0.12f);
            fields[i].setSnapStep(1.0f);
        }
    }

    public DraggableNumberField field(int index) {
        return fields[index];
    }

    public int dimensions() {
        return fields.length;
    }

    public void setGangSpeed(float speed) {
        gangSpeed = Math.max(0.001f, speed);
    }

    public void setGangSnapStep(float step) {
        gangSnapStep = Math.max(1e-6f, step);
    }

    public void setWrapMarginPx(int px) {
        wrapMarginPx = Math.max(1, px);
    }

    private void startGangDrag(float mouseX) {
        gangDragging = true;
        gangDragStartX = mouseX;
        gangDragLastX = mouseX;
        gangDragAccumPx = 0.0f;
        gangStartValues = new float[fields.length];
        for (int i = 0; i < fields.length; i++) {
            gangStartValues[i] = fields[i].value();
        }
    }

    private void updateGangDrag(UiContext ctx, UiInput input, float mouseX, float mouseY) {
        if (!gangDragging) return;
        boolean shiftDown = ctx != null && (ctx.keyboard().isKeyDown(InputConstants.KEY_LEFT_SHIFT) || ctx.keyboard().isKeyDown(InputConstants.KEY_RIGHT_SHIFT));
        boolean ctrlDown = ctx != null && (ctx.keyboard().isKeyDown(InputConstants.KEY_LEFT_CONTROL) || ctx.keyboard().isKeyDown(InputConstants.KEY_RIGHT_CONTROL));

        float dx = mouseX - gangDragLastX;
        gangDragLastX = mouseX;
        gangDragAccumPx += dx;

        float speed = gangSpeed * (shiftDown ? 0.1f : 1.0f);
        float delta = gangDragAccumPx * speed;
        if (ctrlDown) {
            float step = gangSnapStep * (shiftDown ? 0.1f : 1.0f);
            step = Math.max(1e-6f, step);
            delta = Math.round(delta / step) * step;
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i].setValue(gangStartValues[i] + delta);
        }

        // Mouse wrapping while captured.
        if (ctx != null && input != null && ctx.pointer().isCaptured(0x7F00_0000 + hashCode())) {
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
                            gangDragLastX = (float) newX;
                        }
                    }
                }
            }
        }
    }

    private void endGangDrag(UiContext ctx) {
        gangDragging = false;
        int capId = 0x7F00_0000 + hashCode();
        if (ctx != null && ctx.pointer().isCaptured(capId)) {
            ctx.pointer().release();
        }
    }

    public int render(UiRenderer r,
                      UiContext ctx,
                      UiInput input,
                      Theme theme,
                      int x,
                      int y,
                      int width,
                      boolean interactive) {
        if (r == null || theme == null) return 0;

        int labelH = 20;
        int fieldH = 34;
        int gap = 6;
        int totalH = labelH + gap + fieldH;

        int labelColor = Theme.toArgb(theme.textMuted);
        r.drawText(label, x, r.baselineForBox(y, labelH), labelColor);

        int gutter = 10;
        int rowY = y + labelH + gap;

        boolean canInteract = input != null && interactive;
        float mx = input != null ? input.mousePos().x : -1;
        float my = input != null ? input.mousePos().y : -1;
        boolean overGutter = canInteract && mx >= x && my >= rowY && mx < x + gutter && my < rowY + fieldH;

        if (canInteract && input.mousePressed() && overGutter && !gangDragging) {
            startGangDrag(mx);
            if (ctx != null) {
                ctx.pointer().capture(0x7F00_0000 + hashCode());
            }
        }
        if (canInteract && gangDragging && input.mouseDown()) {
            updateGangDrag(ctx, input, mx, my);
        }
        if (gangDragging && (!canInteract || input.mouseReleased())) {
            endGangDrag(ctx);
        }

        // Gang handle visuals.
        int handleBg = Theme.mulAlpha(Theme.toArgb(theme.widgetOutline), overGutter || gangDragging ? 0.35f : 0.20f);
        r.drawRoundedRect(x, rowY, gutter, fieldH, theme.design.radius_sm, handleBg);

        int innerX = x + gutter + 6;
        int available = Math.max(1, width - (innerX - x));
        int per = (available - (fields.length - 1) * 4) / fields.length;

        for (int i = 0; i < fields.length; i++) {
            int fx = innerX + i * (per + 4);
            int fw = (i == fields.length - 1) ? (x + width - fx) : per;

            // Axis mini-label (Blender-ish).
            int axisW = 14;
            int axisBg = Theme.mulAlpha(Theme.toArgb(theme.widgetHover), 0.55f);
            r.drawRoundedRect(fx, rowY, axisW, fieldH, theme.design.radius_sm, axisBg);
            r.drawText(axes[i], fx + 4, r.baselineForBox(rowY, fieldH), Theme.toArgb(theme.textMuted));

            int fieldX = fx + axisW + 2;
            int fieldW = fw - axisW - 2;
            fields[i].render(r, ctx, input, theme, fieldX, rowY, fieldW, fieldH, interactive && !gangDragging);
        }

        return totalH;
    }
}

