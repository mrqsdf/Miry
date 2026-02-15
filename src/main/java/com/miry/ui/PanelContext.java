package com.miry.ui;

import com.miry.ui.render.UiRenderer;

/**
 * Context object passed to {@link com.miry.ui.panels.Panel} implementations at render time.
 * <p>
 * This record-like class bundles the necessary dependencies and state (renderer, UI context, bounds)
 * required for a panel to render its content and handle layout.
 * </p>
 */
public final class PanelContext {
    private UiRenderer renderer;
    private Ui ui;
    private UiContext uiContext;
    private int x;
    private int y;
    private int width;
    private int height;

    public PanelContext() {}

    /**
     * Updates the context with new values.
     *
     * @param renderer  The renderer to use.
     * @param ui        The immediate-mode UI helper.
     * @param uiContext The global UI state context.
     * @param x         The absolute x-coordinate of the panel's content area.
     * @param y         The absolute y-coordinate of the panel's content area.
     * @param width     The width of the panel.
     * @param height    The height of the panel.
     */
    public void set(UiRenderer renderer, Ui ui, UiContext uiContext, int x, int y, int width, int height) {
        this.renderer = renderer;
        this.ui = ui;
        this.uiContext = uiContext;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Gets the renderer.
     * @return The {@link UiRenderer}.
     */
    public UiRenderer renderer() {
        return renderer;
    }

    /**
     * Gets the immediate-mode UI helper.
     * @return The {@link Ui} instance.
     */
    public Ui ui() {
        return ui;
    }

    /**
     * Gets the global UI context.
     * @return The {@link UiContext}.
     */
    public UiContext uiContext() {
        return uiContext;
    }

    /**
     * Gets the x-coordinate of the panel.
     * @return The X position.
     */
    public int x() {
        return x;
    }

    /**
     * Gets the y-coordinate of the panel.
     * @return The Y position.
     */
    public int y() {
        return y;
    }

    /**
     * Gets the width of the panel.
     * @return The width.
     */
    public int width() {
        return width;
    }

    /**
     * Gets the height of the panel.
     * @return The height.
     */
    public int height() {
        return height;
    }
}