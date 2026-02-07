package com.miry.ui.widgets;

import com.miry.ui.core.BaseWidget;
import com.miry.ui.render.UiRenderer;

/**
 * Simple tooltip with a configurable delay.
 */
public final class Tooltip extends BaseWidget {
    private String text;
    private int x, y;
    private float hoverTime;
    private float showDelay = 0.5f;
    private boolean visible;

    public Tooltip() {
        setFocusable(false);
    }

    public void show(String text, int x, int y) {
        if (this.text == null || !this.text.equals(text)) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.hoverTime = 0.0f;
            this.visible = false;
        }
    }

    public void hide() {
        text = null;
        visible = false;
        hoverTime = 0.0f;
    }

    public void update(float dt) {
        if (text != null) {
            hoverTime += dt;
            if (hoverTime >= showDelay) {
                visible = true;
            }
        }
    }

    public void render(UiRenderer r, int bgColor, int textColor) {
        if (!visible || text == null || text.isEmpty()) return;

        int width = Math.round(r.measureText(text) + 16.0f);
        int height = Math.round(r.lineHeight() + 8.0f);
        float baselineY = r.baselineForBox(y, height);

        r.drawRect(x, y, width, height, bgColor);
        r.drawRect(x, y, width, 1, 0xFF3A3A42);
        r.drawText(text, x + 8, baselineY, textColor);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setShowDelay(float delay) {
        this.showDelay = Math.max(0.0f, delay);
    }
}
