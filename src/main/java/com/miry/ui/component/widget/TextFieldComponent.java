package com.miry.ui.component.widget;

import com.miry.ui.Ui;
import com.miry.ui.UiContext;
import com.miry.ui.component.Color;
import com.miry.ui.component.Component;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import com.miry.ui.theme.Theme;
import com.miry.ui.util.NumericExpression;
import com.miry.ui.widgets.TextField;

public class TextFieldComponent extends Component {

    private final TextField textField;
    private boolean isNumberOnly = false;
    private Color bgColor;
    private Color textColor;
    private Color hoverColor;
    private String themeId;

    public TextFieldComponent(String id, String defaultText) {
        super(id);
        this.textField = new TextField(defaultText);
    }

    public TextFieldComponent(String id, TextField textField) {
        super(id);
        this.textField = textField;
    }

    public String text() {
        return textField.text();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public TextField textField() {
        return textField;
    }

    public TextFieldComponent setNumberOnly(boolean numberOnly) {
        this.isNumberOnly = numberOnly;
        textField.setNumericMode(numberOnly);
        return this;
    }

    public TextFieldComponent setNumberOnly(boolean numberOnly, NumericExpression.UnitKind unitKind, int decimal){
        this.isNumberOnly = numberOnly;
        textField.setNumericMode(numberOnly, unitKind, decimal);
        return this;
    }

    public boolean isNumberOnly() {
        return isNumberOnly;
    }

    @Override
    public void handleKey(UiContext ctx, KeyEvent e) {
        if (ctx != null && ctx.focus().focused() == textField.id()) {
            textField.handleKey(e, ctx.clipboard());
        }
    }

    @Override
    public void handleTextInput(UiContext ctx, TextInputEvent e) {
        if (ctx != null && ctx.focus().focused() == textField.id()) {
            textField.handleTextInput(e);
        }
    }

    public Color getBgColor(Ui ui) {
        if (ui == null && bgColor == null)
            throw new IllegalStateException("UI context is required to resolve theme colors");
        if (bgColor != null) return bgColor;
        else
            return themeId != null ? ui.theme().getColor(themeId + ".bg") != null ? ui.theme().getColor(themeId + ".bg") : new Color(Theme.toArgb(ui.theme().widgetBg)) : new Color(Theme.toArgb(ui.theme().widgetBg));
    }

    public Color getHoverColor(Ui ui) {
        if (ui == null && hoverColor == null)
            throw new IllegalStateException("UI context is required to resolve theme colors");
        if (hoverColor != null) return hoverColor;
        else
            return themeId != null ? ui.theme().getColor(themeId + ".hover") != null ? ui.theme().getColor(themeId + ".hover") : new Color(Theme.toArgb(ui.theme().widgetHover)) : new Color(Theme.toArgb(ui.theme().widgetHover));
    }

    public Color getTextColor(Ui ui) {
        if (ui == null && textColor == null)
            throw new IllegalStateException("UI context is required to resolve theme colors");
        if (textColor != null) return textColor;
        else
            return themeId != null ? ui.theme().getColor(themeId + ".text") != null ? ui.theme().getColor(themeId + ".text") : new Color(Theme.toArgb(ui.theme().text)) : new Color(Theme.toArgb(ui.theme().text));
    }

    public TextFieldComponent setBgColor(Color bgColor) {
        this.bgColor = bgColor;
        return this;
    }

    public TextFieldComponent setHoverColor(Color hoverColor) {
        this.hoverColor = hoverColor;
        return this;
    }

    public TextFieldComponent setTextColor(Color textColor) {
        this.textColor = textColor;
        return this;
    }
}
