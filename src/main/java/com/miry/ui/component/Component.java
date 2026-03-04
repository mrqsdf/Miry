package com.miry.ui.component;
//todo add doc

import com.miry.ui.Ui;
import com.miry.ui.render.UiRenderer;

import java.util.ArrayList;
import java.util.List;

public class Component {

    private String id;
    private List<Component> children;

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
            } else if (child instanceof  SeparatorComponent) {
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

}
