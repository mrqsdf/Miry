package com.miry.ui.component;
//todo add doc

import com.miry.ui.Ui;
import com.miry.ui.UiContext;
import com.miry.ui.event.*;
import com.miry.ui.render.UiRenderer;

import java.util.ArrayList;
import java.util.List;

public class Component {

    protected String id;
    protected List<Component> children;

    protected String themeId;

    public Component(String id) {
        this.id = id;
        this.children = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void render(Ui ui, UiRenderer renderer) {
        for (Component child : children) {
            if (child instanceof ButtonComponent) {
                ui.button(renderer, (ButtonComponent) child);
            } else if (child instanceof TextComponent) {
                ui.label(renderer, (TextComponent) child);
            } else if (child instanceof ToggleComponent) {
                ui.toggle(renderer, (ToggleComponent) child);
            } else if (child instanceof SliderComponent) {
                ui.sliderFloat(renderer, (SliderComponent) child);
            } else if (child instanceof GridComponent) {
                ui.grid(renderer, (GridComponent) child);
            } else if (child instanceof SpacerComponent) {
                ui.spacer((SpacerComponent) child);
            } else if (child instanceof SeparatorComponent) {
                ui.separator(renderer, (SeparatorComponent) child);
            } else {
                child.render(ui, renderer);
            }
        }
    }

    public Component addChild(Component child) {
        children.add(child);
        return this;
    }

    public List<Component> getChildren() {
        return children;
    }

    public void handleKey(UiContext uiContext, KeyEvent keyEvent) {
        children.forEach(child -> child.handleKey(uiContext, keyEvent));
    }

    public void handleTextInput(UiContext uiContext, TextInputEvent textInputEvent) {
        children.forEach(child -> child.handleTextInput(uiContext, textInputEvent));
    }

    public void handleFocus(UiContext uiContext, FocusEvent focusEvent) {
        children.forEach(child -> child.handleFocus(uiContext, focusEvent));
    }

    public void handleMouseButton(UiContext uiContext, MouseButtonEvent mouseButtonEvent) {
        children.forEach(child -> child.handleMouseButton(uiContext, mouseButtonEvent));
    }

    public void handleMouseMove(UiContext uiContext, MouseMoveEvent mouseMoveEvent) {
        children.forEach(child -> child.handleMouseMove(uiContext, mouseMoveEvent));
    }

    public void handleScroll(UiContext uiContext, ScrollEvent scrollEvent) {
        children.forEach(child -> child.handleScroll(uiContext, scrollEvent));
    }

    public String getThemeId() {
        return themeId;
    }

    public Component setThemeId(String themeId) {
        this.themeId = themeId;
        return this;
    }

    public void clearChildren() {
        children.clear();
    }

}
