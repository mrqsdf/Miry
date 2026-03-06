package com.miry.ui.panels;

import com.miry.ui.PanelContext;
import com.miry.ui.UiContext;
import com.miry.ui.event.*;

import java.awt.event.MouseEvent;

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

    public void handleKey(UiContext uiContext, KeyEvent keyEvent){
        // Default implementation does nothing, override to handle key events
    }

    public void handleTextInput(UiContext uiContext, TextInputEvent textInputEvent){
        // Default implementation does nothing, override to handle text input events
    }

    public void handleFocus(UiContext uiContext, FocusEvent focusEvent){
        // Default implementation does nothing, override to handle focus events
    }

    public void handleMouseButton(UiContext uiContext, MouseButtonEvent mouseButtonEvent){
        // Default implementation does nothing, override to handle mouse button events
    }

    public void handleMouseMove(UiContext uiContext, MouseMoveEvent mouseMoveEvent){
        // Default implementation does nothing, override to handle mouse move events
    }

    public void handleScroll(UiContext uiContext, ScrollEvent scrollEvent){
        // Default implementation does nothing, override to handle scroll events
    }
}
