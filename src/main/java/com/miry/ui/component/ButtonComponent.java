package com.miry.ui.component;

import com.miry.ui.Ui;
import com.miry.ui.theme.Theme;
import org.w3c.dom.css.CSSUnknownRule;
//todo add doc

public class ButtonComponent extends Component {

    private TextComponent label;
    private Color bgColor;
    private Color hoverColor;
    private Color activeColor;

    private Runnable onClick;

    public ButtonComponent(String id, TextComponent label) {
        super(id);
        this.label = label;
    }

    public ButtonComponent(TextComponent label) {
        this(label.getId(), label);
    }

    public ButtonComponent setBgColor(Color bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    public ButtonComponent setHoverColor(Color hoverColor) {
        this.hoverColor = hoverColor;
        return this;
    }

    public ButtonComponent setActiveColor(Color activeColor) {
        this.activeColor = activeColor;
        return this;
    }

    public ButtonComponent setOnClick(Runnable onClick) {
        this.onClick = onClick;
        return this;
    }

    public TextComponent getLabel() {
        return label;
    }

    public Color getBgColor(Ui ui) {
        if (ui == null && bgColor == null)
            throw new IllegalStateException("UI context is required to resolve theme colors");
        if (bgColor != null) return bgColor;
        else
            return themeId != null ? ui.theme().getColor(themeId + ".buttonBg") != null ? ui.theme().getColor(themeId + ".buttonBg") : new Color(Theme.toArgb(ui.theme().widgetBg)) : new Color(Theme.toArgb(ui.theme().widgetBg));
    }

    public Color getHoverColor(Ui ui) {
        if (ui == null && hoverColor == null)
            throw new IllegalStateException("UI context is required to resolve theme colors");
        if (hoverColor != null) return hoverColor;
        else
            return themeId != null ? ui.theme().getColor(themeId + ".buttonHover") != null ? ui.theme().getColor(themeId + ".buttonHover") : new Color(Theme.toArgb(ui.theme().widgetHover)) : new Color(Theme.toArgb(ui.theme().widgetHover));
    }

    public Color getActiveColor(Ui ui) {
        if (ui == null && activeColor == null)
            throw new IllegalStateException("UI context is required to resolve theme colors");
        if (activeColor != null) return activeColor;
        else
            return themeId != null ? ui.theme().getColor(themeId + ".buttonActive") != null ? ui.theme().getColor(themeId + ".buttonActive") : new Color(Theme.toArgb(ui.theme().widgetActive)) : new Color(Theme.toArgb(ui.theme().widgetActive));
    }



    public Runnable getOnClick() {
        return onClick;
    }


}
