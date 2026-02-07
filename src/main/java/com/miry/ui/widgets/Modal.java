package com.miry.ui.widgets;

import com.miry.platform.InputConstants;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple modal dialog widget.
 * <p>
 * Supports {@code Esc} to close and {@code Enter} to activate a default button.
 */
public final class Modal extends BaseWidget {
    public enum Type { INFO, WARNING, ERROR, CONFIRM, INPUT }

    private final Type type;
    private final String title;
    private final String message;
    private final List<Button> buttons = new ArrayList<>();
    private boolean open;
    private String inputText = "";
    private int defaultButtonIndex = -1;
    private int selectedButtonIndex = -1;

    public Modal(Type type, String title, String message) {
        this.type = type;
        this.title = title;
        this.message = message;
        setFocusable(false);
    }

    public void addButton(String label, Runnable action) {
        buttons.add(new Button(label, action));
    }

    public void open() {
        this.open = true;
        selectedButtonIndex = defaultButtonIndex >= 0 ? defaultButtonIndex : (buttons.isEmpty() ? -1 : buttons.size() - 1);
    }

    public void close() {
        this.open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public void setDefaultButtonIndex(int index) {
        this.defaultButtonIndex = index;
    }

    public boolean handleKey(KeyEvent event) {
        if (!enabled() || !open || event == null || !event.isPress()) {
            return false;
        }

        if (event.key() == InputConstants.KEY_ESCAPE) {
            close();
            return true;
        }
        if (event.key() == InputConstants.KEY_TAB) {
            if (buttons.isEmpty()) {
                return true;
            }
            int dir = event.hasShift() ? -1 : 1;
            int idx = selectedButtonIndex >= 0 ? selectedButtonIndex : 0;
            idx = (idx + dir + buttons.size()) % buttons.size();
            selectedButtonIndex = idx;
            return true;
        }
        if (event.key() == InputConstants.KEY_LEFT) {
            if (buttons.isEmpty()) {
                return true;
            }
            int idx = selectedButtonIndex >= 0 ? selectedButtonIndex : 0;
            idx = (idx - 1 + buttons.size()) % buttons.size();
            selectedButtonIndex = idx;
            return true;
        }
        if (event.key() == InputConstants.KEY_RIGHT) {
            if (buttons.isEmpty()) {
                return true;
            }
            int idx = selectedButtonIndex >= 0 ? selectedButtonIndex : 0;
            idx = (idx + 1) % buttons.size();
            selectedButtonIndex = idx;
            return true;
        }
        if (event.key() == InputConstants.KEY_ENTER) {
            int idx = selectedButtonIndex >= 0 ? selectedButtonIndex : (defaultButtonIndex >= 0 ? defaultButtonIndex : buttons.size() - 1);
            if (idx >= 0 && idx < buttons.size()) {
                Button btn = buttons.get(idx);
                if (btn.action != null) {
                    btn.action.run();
                }
            }
            close();
            return true;
        }

        return false;
    }

    public void render(UiRenderer r,
                       int screenWidth,
                       int screenHeight,
                       int mouseX,
                       int mouseY,
                       int bgColor,
                       int overlayColor,
                       int textColor,
                       int buttonBgColor,
                       int buttonHoverColor,
                       int outlineColor) {
        render(r, null, screenWidth, screenHeight, mouseX, mouseY, bgColor, overlayColor, textColor, buttonBgColor, buttonHoverColor, outlineColor);
    }

    /**
     * Themed render overload that uses {@link Theme#skins} when available.
     */
    public void render(UiRenderer r,
                       Theme theme,
                       int screenWidth,
                       int screenHeight,
                       int mouseX,
                       int mouseY,
                       int bgColor,
                       int overlayColor,
                       int textColor,
                       int buttonBgColor,
                       int buttonHoverColor,
                       int outlineColor) {
        if (!open) return;

        r.drawRect(0, 0, screenWidth, screenHeight, overlayColor);

        int modalWidth = 400;
        int modalHeight = 200;
        int modalX = (screenWidth - modalWidth) / 2;
        int modalY = (screenHeight - modalHeight) / 2;

        r.drawRect(modalX + 6, modalY + 8, modalWidth, modalHeight, 0x2A000000);
        if (theme != null && theme.skins.popup != null) {
            theme.skins.popup.draw(r, modalX, modalY, modalWidth, modalHeight, bgColor);
        } else {
            r.drawRect(modalX, modalY, modalWidth, modalHeight, bgColor);
        }
        r.drawRect(modalX, modalY, modalWidth, 2, outlineColor);

        float titleBaseline = modalY + 20.0f + r.ascent();
        float messageBaseline = titleBaseline + r.lineHeight() + 8.0f;
        r.drawText(title, modalX + 20, titleBaseline, textColor);
        r.drawText(message, modalX + 20, messageBaseline, textColor);

        int buttonY = modalY + modalHeight - 50;
        int buttonX = modalX + modalWidth - 120;
        for (int i = buttons.size() - 1; i >= 0; i--) {
            Button btn = buttons.get(i);
            boolean hovered = mouseX >= buttonX && mouseY >= buttonY && mouseX < buttonX + 100 && mouseY < buttonY + 30;
            int bg = hovered ? buttonHoverColor : buttonBgColor;
            if (!enabled()) {
                bg = dimAlpha(bg, 0.55f);
            }
            r.drawRect(buttonX, buttonY, 100, 30, bg);
            r.drawRect(buttonX, buttonY, 100, 1, outlineColor);
            if (i == selectedButtonIndex) {
                r.drawRect(buttonX, buttonY + 29, 100, 1, outlineColor);
                r.drawRect(buttonX, buttonY, 1, 30, outlineColor);
                r.drawRect(buttonX + 99, buttonY, 1, 30, outlineColor);
            }
            float baselineY = r.baselineForBox(buttonY, 30);
            r.drawText(btn.label, buttonX + 10, baselineY, textColor);
            buttonX -= 110;
        }

        if (!enabled()) {
            r.drawRect(modalX, modalY, modalWidth, modalHeight, 0x33000000);
        }
    }

    public boolean handleClick(int screenWidth, int screenHeight, int mx, int my) {
        if (!enabled() || !open) return false;

        int modalWidth = 400;
        int modalHeight = 200;
        int modalX = (screenWidth - modalWidth) / 2;
        int modalY = (screenHeight - modalHeight) / 2;

        if (mx < modalX || my < modalY || mx >= modalX + modalWidth || my >= modalY + modalHeight) {
            return true;
        }

        int buttonY = modalY + modalHeight - 50;
        int buttonX = modalX + modalWidth - 120;
        for (int i = buttons.size() - 1; i >= 0; i--) {
            Button btn = buttons.get(i);
            if (mx >= buttonX && my >= buttonY && mx < buttonX + 100 && my < buttonY + 30) {
                selectedButtonIndex = i;
                if (btn.action != null) {
                    btn.action.run();
                }
                close();
                return true;
            }
            buttonX -= 110;
        }

        return true;
    }

    public Type type() { return type; }
    public String title() { return title; }
    public String message() { return message; }
    public String inputText() { return inputText; }
    public void setInputText(String text) { this.inputText = text != null ? text : ""; }

    private record Button(String label, Runnable action) {}

    private static int dimAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int na = Math.max(0, Math.min(255, Math.round(a * Math.max(0.0f, Math.min(1.0f, factor)))));
        return (na << 24) | (argb & 0x00FFFFFF);
    }
}
