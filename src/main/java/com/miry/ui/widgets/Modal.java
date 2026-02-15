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
    private int lastModalX;
    private int lastModalY;
    private int lastModalW;
    private int lastModalH;
    private int lastButtonW;
    private int lastButtonH;
    private int lastButtonGap;
    private int lastButtonY;
    private int lastButtonStartX;

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

        int spaceSm = theme != null ? theme.design.space_sm : 8;
        int spaceMd = theme != null ? theme.design.space_md : 12;
        int spaceLg = theme != null ? theme.design.space_lg : 16;
        int space2xl = theme != null ? theme.design.space_2xl : 32;
        int borderThin = theme != null ? theme.design.border_thin : 1;

        int modalWidth = theme != null ? (space2xl * 12 + spaceLg) : 400;
        int modalHeight = theme != null ? (space2xl * 6 + spaceSm) : 200;
        int modalX = (screenWidth - modalWidth) / 2;
        int modalY = (screenHeight - modalHeight) / 2;
        lastModalX = modalX;
        lastModalY = modalY;
        lastModalW = modalWidth;
        lastModalH = modalHeight;

        if (theme != null) {
            int shadow = Theme.toArgb(theme.shadow);
            drawDropShadow(r, modalX, modalY, modalWidth, modalHeight, shadow, 0.0f, theme.design.space_xs, theme.design.shadow_lg, theme.design.radius_sm, 1.0f);
        } else {
            drawDropShadow(r, modalX, modalY, modalWidth, modalHeight, 0x2A000000, 0.0f, 6.0f, 12);
        }
        if (theme != null && theme.skins.popup != null) {
            theme.skins.popup.draw(r, modalX, modalY, modalWidth, modalHeight, bgColor);
        } else {
            float radius = theme != null ? theme.design.radius_sm : 3.0f;
            int t = borderThin;
            int top = Theme.lightenArgb(bgColor, 0.02f);
            int bottom = Theme.darkenArgb(bgColor, 0.02f);
            r.drawRoundedRect(modalX, modalY, modalWidth, modalHeight, radius, top, top, bottom, bottom, t, outlineColor);
        }

        float titleBaseline = modalY + (spaceLg + (theme != null ? theme.design.space_xs : 4)) + r.ascent();
        float messageBaseline = titleBaseline + r.lineHeight() + spaceSm;
        r.drawText(title, modalX + spaceLg, titleBaseline, textColor);
        r.drawText(message, modalX + spaceLg, messageBaseline, textColor);

        int buttonW = theme != null ? (space2xl * 3 + (theme.design.space_xs)) : 100;
        int buttonH = theme != null ? (theme.design.widget_height_md + borderThin * 2) : 30;
        int buttonGap = theme != null ? (spaceSm + borderThin * 2) : 10;

        int buttonY = modalY + modalHeight - buttonH - (spaceLg + spaceSm);
        int buttonX = modalX + modalWidth - buttonW - spaceLg;
        lastButtonW = buttonW;
        lastButtonH = buttonH;
        lastButtonGap = buttonGap;
        lastButtonY = buttonY;
        lastButtonStartX = buttonX;
        for (int i = buttons.size() - 1; i >= 0; i--) {
            Button btn = buttons.get(i);
            boolean hovered = mouseX >= buttonX && mouseY >= buttonY && mouseX < buttonX + buttonW && mouseY < buttonY + buttonH;
            int bg = hovered ? buttonHoverColor : buttonBgColor;
            if (!enabled()) {
                bg = dimAlpha(bg, 0.55f);
            }
            if (theme != null && theme.skins.widget != null) {
                theme.skins.widget.drawWithOutline(r, buttonX, buttonY, buttonW, buttonH, bg, outlineColor, borderThin);
            } else {
                float br = theme != null ? theme.design.radius_sm : 3.0f;
                int bt = Math.max(1, borderThin);
                int top = Theme.lightenArgb(bg, 0.06f);
                int bottom = Theme.darkenArgb(bg, 0.06f);
                r.drawRoundedRect(buttonX, buttonY, buttonW, buttonH, br, top, top, bottom, bottom, bt, outlineColor);
            }
            if (i == selectedButtonIndex) {
                r.drawRect(buttonX, buttonY + buttonH - borderThin, buttonW, borderThin, outlineColor);
                r.drawRect(buttonX, buttonY, borderThin, buttonH, outlineColor);
                r.drawRect(buttonX + buttonW - borderThin, buttonY, borderThin, buttonH, outlineColor);
            }
            float baselineY = r.baselineForBox(buttonY, buttonH);
            r.drawText(btn.label, buttonX + spaceSm, baselineY, textColor);
            buttonX -= (buttonW + buttonGap);
        }

        if (!enabled()) {
            r.drawRect(modalX, modalY, modalWidth, modalHeight, 0x33000000);
        }
    }

    public boolean handleClick(int screenWidth, int screenHeight, int mx, int my) {
        if (!enabled() || !open) return false;

        int modalWidth = lastModalW > 0 ? lastModalW : 400;
        int modalHeight = lastModalH > 0 ? lastModalH : 200;
        int modalX = lastModalW > 0 ? lastModalX : (screenWidth - modalWidth) / 2;
        int modalY = lastModalH > 0 ? lastModalY : (screenHeight - modalHeight) / 2;

        if (mx < modalX || my < modalY || mx >= modalX + modalWidth || my >= modalY + modalHeight) {
            return true;
        }

        int buttonW = lastButtonW > 0 ? lastButtonW : 100;
        int buttonH = lastButtonH > 0 ? lastButtonH : 30;
        int buttonGap = lastButtonGap > 0 ? lastButtonGap : 10;
        int buttonY = lastButtonY > 0 ? lastButtonY : (modalY + modalHeight - 50);
        int buttonX = lastButtonStartX > 0 ? lastButtonStartX : (modalX + modalWidth - 120);
        for (int i = buttons.size() - 1; i >= 0; i--) {
            Button btn = buttons.get(i);
            if (mx >= buttonX && my >= buttonY && mx < buttonX + buttonW && my < buttonY + buttonH) {
                selectedButtonIndex = i;
                if (btn.action != null) {
                    btn.action.run();
                }
                close();
                return true;
            }
            buttonX -= (buttonW + buttonGap);
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
