package com.miry.ui.component.widget;

import com.miry.ui.component.Color;
import com.miry.ui.component.Component;
import com.miry.ui.widgets.ColorPicker;

public class ColorPickerComponent extends Component {

    private ColorPicker mColorPicker;
    private Runnable mOnColorChanged;

    public ColorPickerComponent(String id, ColorPicker colorPicker) {
        super(id);
        this.mColorPicker = colorPicker;
    }

    public ColorPickerComponent(String id) {
        super(id);
        this.mColorPicker = new ColorPicker();
    }

    public ColorPickerComponent(String id, int defaultColor) {
        super(id);
        this.mColorPicker = new ColorPicker(defaultColor);
    }

    public ColorPickerComponent(String id, Color defaultColor) {
        super(id);
        this.mColorPicker = new ColorPicker(defaultColor.getArgb());
    }

    public ColorPicker colorPicker() {
        return mColorPicker;
    }

    public int arbg() {
        return mColorPicker.toArgb();
    }

    public Color color() {
        return mColorPicker.color();
    }

    public ColorPickerComponent setHsva(float h, float s, float v, float a) {
        mColorPicker.setHsva(h, s, v, a);
        return this;
    }

    public ColorPickerComponent fromArgb(int argb) {
        mColorPicker.fromArgb(argb);
        return this;
    }

    public ColorPickerComponent fromColor(Color color) {
        mColorPicker.fromArgb(color.getArgb());
        return this;
    }

    public float getHue() {
        return mColorPicker.hue();
    }

    public float getSaturation() {
        return mColorPicker.saturation();
    }

    public float getValue() {
        return mColorPicker.value();
    }

    public float getAlpha() {
        return mColorPicker.alpha();
    }

    public ColorPickerComponent setOnChange(Runnable onColorChanged) {
        this.mOnColorChanged = onColorChanged;
        return this;
    }

    public Runnable getOnChange() {
        return mOnColorChanged;
    }

}
