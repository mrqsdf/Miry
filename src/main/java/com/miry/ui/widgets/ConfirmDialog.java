package com.miry.ui.widgets;

import com.miry.ui.event.KeyEvent;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * Simple confirmation dialog (Yes/No/Cancel) built on {@link Modal}.
 */
public final class ConfirmDialog {
    public enum Result { YES, NO, CANCEL }

    private final Modal modal;
    private ResultHandler handler;

    @FunctionalInterface
    public interface ResultHandler {
        void onResult(Result result);
    }

    public ConfirmDialog(String title, String message) {
        modal = new Modal(Modal.Type.CONFIRM, title, message);
        modal.addButton("Cancel", () -> fire(Result.CANCEL));
        modal.addButton("No", () -> fire(Result.NO));
        modal.addButton("Yes", () -> fire(Result.YES));
        modal.setDefaultButtonIndex(2);
    }

    public void setHandler(ResultHandler handler) {
        this.handler = handler;
    }

    public void open() {
        modal.open();
    }

    public void close() {
        modal.close();
    }

    public boolean isOpen() {
        return modal.isOpen();
    }

    public boolean handleKey(KeyEvent e) {
        return modal.handleKey(e);
    }

    public boolean handleClick(int screenWidth, int screenHeight, int mx, int my) {
        return modal.handleClick(screenWidth, screenHeight, mx, my);
    }

    public void render(UiRenderer r, Theme theme, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        int bg = Theme.toArgb(theme.panelBg);
        int overlay = 0x88000000;
        int text = Theme.toArgb(theme.text);
        int btn = Theme.toArgb(theme.widgetBg);
        int btnHover = Theme.toArgb(theme.widgetHover);
        int outline = Theme.toArgb(theme.widgetOutline);
        modal.render(r, theme, screenWidth, screenHeight, mouseX, mouseY, bg, overlay, text, btn, btnHover, outline);
    }

    private void fire(Result result) {
        if (handler != null) {
            handler.onResult(result);
        }
    }
}

