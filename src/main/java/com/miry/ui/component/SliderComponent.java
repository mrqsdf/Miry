package com.miry.ui.component;
//todo add doc

import com.miry.ui.Ui;
import com.miry.ui.theme.Theme;

import java.util.function.Consumer;

public class SliderComponent extends Component {
    private TextComponent label;
    private float value;
    private float min;
    private float max;
    private float step;
    private String themeId;
    private Color backgroundColor;
    private Color hoverColor;
    private Color activeColor;
    private Consumer<Float> onChange;

    public SliderComponent(String id,TextComponent label, float value, float min, float max, float step) {
        super(id);
        this.label = label;
        this.value = value;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public SliderComponent(TextComponent label, float value, float min, float max, float step) {
        this(label.getId(), label, value, min, max, step);
    }

    public SliderComponent setThemeId(String themeId) {
        this.themeId = themeId;
        return this;
    }

    public SliderComponent setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public SliderComponent setHoverColor(Color hoverColor) {
        this.hoverColor = hoverColor;
        return this;
    }

    public SliderComponent setActiveColor(Color activeColor) {
        this.activeColor = activeColor;
        return this;
    }

    public SliderComponent setOnChange(Consumer<Float> onChange) {
        this.onChange = onChange;
        return this;
    }

    public TextComponent getLabel() {
        return label;
    }

    public float getValue() {
        return value;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getStep() {
        return step;
    }

    public Color getBackgroundColor(Ui ui) {
        if (ui == null && backgroundColor == null)
            throw new IllegalStateException("UI context is required to resolve theme colors");
        if (backgroundColor != null) return backgroundColor;
        else
            return themeId != null ? ui.theme().getColor(themeId + ".sliderBg") != null ? ui.theme().getColor(themeId + ".sliderBg") : new Color(Theme.toArgb(ui.theme().widgetBg)) : new Color(Theme.toArgb(ui.theme().widgetBg));
    }

    public Color getHoverColor(Ui ui) {
        if (ui == null && hoverColor == null)
            throw new IllegalStateException("UI context is required to resolve theme colors");
        if (hoverColor != null) return hoverColor;
        else
            return themeId != null ? ui.theme().getColor(themeId + ".sliderHover") != null ? ui.theme().getColor(themeId + ".sliderHover") : new Color(Theme.toArgb(ui.theme().widgetHover)) : new Color(Theme.toArgb(ui.theme().widgetHover));
    }

    public Color getActiveColor(Ui ui) {
        if (ui == null && activeColor == null)
            throw new IllegalStateException("UI context is required to resolve theme colors");
        if (activeColor != null) return activeColor;
        else
            return themeId != null ? ui.theme().getColor(themeId + ".sliderActive") != null ? ui.theme().getColor(themeId + ".sliderActive") : new Color(Theme.toArgb(ui.theme().widgetActive)) : new Color(Theme.toArgb(ui.theme().widgetActive));
    }

    public Consumer<Float> getOnChange() {
        return onChange;
    }

}
