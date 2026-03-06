package com.miry.ui.component;

import com.miry.ui.Ui;
import com.miry.ui.theme.Theme;

import java.util.function.Consumer;

//todo add doc
public class ToggleComponent extends Component {

    private final TextComponent label;
    private Color toggleColor;
    private String themeId;
    private boolean toggled;
    private Consumer<Boolean> onChange;

    public ToggleComponent(String id, TextComponent label, boolean toggled) {
        this(id, label, null, toggled);
    }

    public ToggleComponent(String id, TextComponent label, Color toggleColor, boolean toggled) {
        super(id);
        this.label = label;
        this.toggleColor = toggleColor;
        this.toggled = toggled;
    }

    public ToggleComponent(TextComponent label, boolean toggled) {
        this(label.getId(), label, toggled);
    }

    public ToggleComponent(TextComponent label, Color toggleColor, boolean toggled) {
        this(label.getId(), label, toggleColor, toggled);
    }

    public ToggleComponent setToggleColor(Color toggleColor) {
        this.toggleColor = toggleColor;
        return this;
    }

    public ToggleComponent setThemeId(String themeId) {
        this.themeId = themeId;
        return this;
    }

    public TextComponent getLabel() {
        return label;
    }

    public boolean isToggled() {
        return toggled;
    }

    public Color getToggleColor(Ui ui) {
        if (ui == null && toggleColor == null) throw new IllegalStateException("UI context is required to resolve theme colors");
        if (toggleColor != null) return toggleColor;
        else {
            Color active = themeId != null ? ui.theme().getColor(themeId +".toggleActive") != null ? ui.theme().getColor(themeId +".toggleActive") :  new Color(Theme.toArgb(ui.theme().widgetActive)) : new Color(Theme.toArgb(ui.theme().widgetActive));
            Color inactive = themeId != null ? ui.theme().getColor(themeId + ".toggleInactive") != null ? ui.theme().getColor(themeId + ".toggleInactive") : new Color(Theme.toArgb(ui.theme().widgetOutline)) : new Color(Theme.toArgb(ui.theme().widgetOutline));
            return toggled ? active : inactive;
        }
    }

    public String getThemeId() {
        return themeId;
    }

    public Consumer<Boolean> getOnChange() {
        return onChange;
    }

    public ToggleComponent setOnChange(Consumer<Boolean> onChange) {
        this.onChange = onChange;
        return this;
    }

    public void toggle() {
        if (onChange != null) onChange.accept(!toggled);
        this.toggled = !toggled;
    }


}
