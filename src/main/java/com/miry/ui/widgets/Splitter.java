package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * Resizable splitter dividing two panels horizontally or vertically.
 */
public final class Splitter extends BaseWidget {
    public enum Orientation { HORIZONTAL, VERTICAL }

    private Orientation orientation = Orientation.HORIZONTAL;
    private float splitRatio = 0.5f;
    private int handleSize = 6;
    private boolean dragging = false;
    private float dragStartRatio = 0.5f;
    private int dragStartPos = 0;
    private float hoverT = 0.0f;

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation != null ? orientation : Orientation.HORIZONTAL;
    }

    public Orientation orientation() {
        return orientation;
    }

    public void setSplitRatio(float ratio) {
        this.splitRatio = Math.max(0.1f, Math.min(0.9f, ratio));
    }

    public float splitRatio() {
        return splitRatio;
    }

    public void setHandleSize(int size) {
        this.handleSize = Math.max(4, Math.min(20, size));
    }

    public int handleSize() {
        return handleSize;
    }

    /**
     * Renders the splitter and returns the split position (either horizontal x or vertical y).
     * Use this position to layout your two panels.
     */
    public int render(UiRenderer r, UiContext ctx, UiInput input, Theme theme, int x, int y, int width, int height, boolean interactive) {
        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = canInteract ? input.mousePos().x : -1;
        float my = canInteract ? input.mousePos().y : -1;
        boolean mousePressed = canInteract && input.mousePressed();
        boolean mouseDown = canInteract && input.mouseDown();
        boolean mouseReleased = canInteract && input.mouseReleased();

        float dt = ctx != null ? ctx.lastDt() : 0.0f;
        float speed = theme.design.animSpeed_fast;

        int splitPos;
        int handleX, handleY, handleW, handleH;

        if (orientation == Orientation.HORIZONTAL) {
            splitPos = Math.round(width * splitRatio);
            handleX = x + splitPos - handleSize / 2;
            handleY = y;
            handleW = handleSize;
            handleH = height;
        } else {
            splitPos = Math.round(height * splitRatio);
            handleX = x;
            handleY = y + splitPos - handleSize / 2;
            handleW = width;
            handleH = handleSize;
        }

        boolean hovered = canInteract && mx >= handleX && mx < handleX + handleW && my >= handleY && my < handleY + handleH;
        hoverT = approachExp(hoverT, hovered ? 1.0f : 0.0f, speed, dt);

        // Start drag
        if (hovered && mousePressed) {
            dragging = true;
            dragStartRatio = splitRatio;
            dragStartPos = orientation == Orientation.HORIZONTAL ? (int) mx : (int) my;
            if (ctx != null) {
                ctx.pointer().capture(id());
            }
        }

        // Update drag
        if (dragging && mouseDown) {
            int currentPos = orientation == Orientation.HORIZONTAL ? (int) mx : (int) my;
            int totalSize = orientation == Orientation.HORIZONTAL ? width : height;
            int delta = currentPos - dragStartPos;
            int startPixels = Math.round(totalSize * dragStartRatio);
            int newPixels = startPixels + delta;
            splitRatio = Math.max(0.1f, Math.min(0.9f, newPixels / (float) totalSize));

            if (orientation == Orientation.HORIZONTAL) {
                splitPos = Math.round(width * splitRatio);
                handleX = x + splitPos - handleSize / 2;
            } else {
                splitPos = Math.round(height * splitRatio);
                handleY = y + splitPos - handleSize / 2;
            }
        }

        // End drag
        if (dragging && mouseReleased) {
            dragging = false;
            if (ctx != null && ctx.pointer().isCaptured(id())) {
                ctx.pointer().release();
            }
        }

        // Draw handle
        int handleBg = Theme.mulAlpha(Theme.toArgb(theme.widgetHover), 0.4f + 0.4f * hoverT);
        if (dragging) {
            handleBg = Theme.mulAlpha(Theme.toArgb(theme.widgetActive), 0.7f);
        }
        if (handleBg != 0) {
            r.drawRoundedRect(handleX, handleY, handleW, handleH, theme.design.radius_sm, handleBg);
        }

        // Draw handle grip indicator
        if (hoverT > 0.01f || dragging) {
            int gripColor = Theme.mulAlpha(Theme.toArgb(theme.text), 0.3f + 0.3f * hoverT);
            if (orientation == Orientation.HORIZONTAL) {
                int gripX = handleX + handleW / 2;
                int gripY = handleY + handleH / 2;
                int gripLen = Math.min(30, handleH / 3);
                r.drawRect(gripX - 1, gripY - gripLen, 2, gripLen * 2, gripColor);
            } else {
                int gripX = handleX + handleW / 2;
                int gripY = handleY + handleH / 2;
                int gripLen = Math.min(30, handleW / 3);
                r.drawRect(gripX - gripLen, gripY - 1, gripLen * 2, 2, gripColor);
            }
        }

        return splitPos;
    }

    public int render(UiRenderer r, UiInput input, Theme theme, int x, int y, int width, int height) {
        return render(r, null, input, theme, x, y, width, height, true);
    }
}
