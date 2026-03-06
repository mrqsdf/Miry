package com.miry.ui.component;

import com.miry.ui.Ui;
import com.miry.ui.UiContext;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import com.miry.ui.theme.Theme;
import com.miry.ui.widgets.ComboBox;

import java.util.List;

public class ComboBoxComponent<T> extends Component {

    private final ComboBox<T> comboBox;
    private Color bgColor;
    private Color hoverColor;
    private Color textColor;
    private String themeId;

    public ComboBoxComponent(String id, List<T> items, int selectedIndex) {
        super(id);
        this.comboBox = new ComboBox<>();
        for (T item : items) {
            comboBox.addItem(item);
        }
        comboBox.setSelectedIndex(Math.max(0, Math.min(selectedIndex, items.size() - 1)));
    }

    public ComboBoxComponent(String id, ComboBox<T> comboBox) {
        super(id);
        this.comboBox = comboBox;
    }


    public ComboBox<T> comboBox() {
        return comboBox;
    }

    public ComboBoxComponent<T> setBackgroundColor(Color color) {
        this.bgColor = color;
        return this;
    }


    public ComboBoxComponent<T> setHoverColor(Color color) {
        this.hoverColor = color;
        return this;
    }

    public ComboBoxComponent<T> setTextColor(Color color) {
        this.textColor = color;
        return this;
    }

    public ComboBoxComponent<T> setThemeId(String themeId) {
        this.themeId = themeId;
        return this;
    }

    public T getSelectedItem() {
        return comboBox.selected();
    }

     public int getSelectedIndex() {
        return comboBox.selectedIndex();
    }

     public void setSelectedIndex(int index) {
        comboBox.setSelectedIndex(index);
    }

     public List<T> getItems() {
        return comboBox.items();
    }

    public int getItemCount() {
        return comboBox.items().size();
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

    @Override
    public void handleKey(UiContext uiContext, KeyEvent keyEvent) {
        if (comboBox.isOpen() && comboBox.handleKey(keyEvent)) return;
    }

    @Override
    public void handleTextInput(UiContext uiContext, TextInputEvent textInputEvent) {
        if (comboBox.isOpen() && comboBox.handleTextInput(textInputEvent)) return;

    }
}
