package com.miry.ui.component;

import com.miry.ui.Ui;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;
//todo add doc

public class TextComponent extends Component{

    private Color color;
    private String text;
    private String themeId;
    private boolean underline;
    private float fontSize;
    private boolean bold;
    private boolean italic;
    private boolean strike;

    public TextComponent(String id, String text) {
        super(id);
        this.text = text;
        this.themeId = null;
        this.underline = false;
        this.fontSize = 14f; // Default font size
        this.bold = false;
        this.italic = false;
        this.strike = false;
    }

    public TextComponent(String text){
        this(text, text);
    }

    public TextComponent setColor(Color color) {
        this.color = color;
        return this;
    }

    public TextComponent setThemeColor(String themeId) {
        this.themeId = themeId;
        return this;
    }

    public TextComponent setUnderline(boolean underline) {
        this.underline = underline;
        return this;
    }

    public TextComponent setFontSize(float fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public TextComponent setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public TextComponent setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }
    public TextComponent setStrike(boolean strike) {
        this.strike = strike;
        return this;
    }

    public Color getColor(Ui ui) {
        if (ui == null && color == null) throw new IllegalStateException("UI context is required to resolve theme colors");
        if (color != null) return color;
        else {
            Color themeColor = themeId != null ? ui.theme().getColor(themeId + ".textColor") != null ? ui.theme().getColor(themeId + ".textColor") : new Color(Theme.toArgb(ui.theme().text)) : new Color(Theme.toArgb(ui.theme().text));
            return themeColor;
        }

    }

    public String getText() {
        return text;
    }

    public String getThemeId() {
        return themeId;
    }

    public boolean isUnderline() {
        return underline;
    }

    public float getFontSize() {
        return fontSize;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public boolean isStrike() {
        return strike;
    }


}
