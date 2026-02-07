package com.miry.ui;

import com.miry.ui.render.UiRenderer;

/**
 * Context object passed to {@link com.miry.ui.panels.Panel} implementations at render time.
 * <p>
 * Provides access to {@link UiRenderer}, {@link Ui}, and the panel bounds.
 */
public final class PanelContext {
    private UiRenderer renderer;
    private Ui ui;
    private int x;
    private int y;
    private int width;
    private int height;

    public PanelContext() {}

    public void set(UiRenderer renderer, Ui ui, int x, int y, int width, int height) {
        this.renderer = renderer;
        this.ui = ui;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public UiRenderer renderer() {
        return renderer;
    }

    public Ui ui() {
        return ui;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
