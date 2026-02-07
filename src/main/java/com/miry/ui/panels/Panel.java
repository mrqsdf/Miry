package com.miry.ui.panels;

import com.miry.ui.PanelContext;

/**
 * A dockable editor panel.
 * <p>
 * Panels are rendered by {@link com.miry.ui.layout.LeafNode} and receive a {@link PanelContext}
 * each frame.
 */
public abstract class Panel {
    private final String title;

    protected Panel(String title) {
        this.title = title == null ? "" : title;
    }

    public final String title() {
        return title;
    }

    public abstract void render(PanelContext ctx);
}
