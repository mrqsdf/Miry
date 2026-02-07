package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.clipboard.Clipboard;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;
import com.miry.platform.InputConstants;

/**
 * Editable text input widget with optional multi-line mode.
 */
public final class TextField extends BaseWidget {
    private final StringBuilder text = new StringBuilder();
    private int cursorPos;
    private int selectionStart;
    private int selectionEnd;
    private boolean multiline;
    private int maxLength = 1024;

    public TextField() {}

    public TextField(String initialText) {
        text.append(initialText != null ? initialText : "");
        cursorPos = text.length();
    }

    public void handleTextInput(TextInputEvent e) {
        if (!enabled()) {
            return;
        }
        if (e == null) {
            return;
        }
        if (hasSelection()) {
            deleteSelection();
        }
        if (text.length() < maxLength) {
            text.insert(cursorPos, (char) e.codepoint());
            cursorPos++;
            clearSelection();
        }
    }

    public void handleKey(KeyEvent e, Clipboard clipboard) {
        if (!enabled()) {
            return;
        }
        if (e == null) {
            return;
        }
        if (!e.isPressOrRepeat()) return;

        switch (e.key()) {
            case InputConstants.KEY_BACKSPACE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPos > 0) {
                    text.deleteCharAt(cursorPos - 1);
                    cursorPos--;
                    clearSelection();
                }
            }
            case InputConstants.KEY_DELETE -> {
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPos < text.length()) {
                    text.deleteCharAt(cursorPos);
                    clearSelection();
                }
            }
            case InputConstants.KEY_RIGHT -> {
                moveCursor(cursorPos + 1, e.hasShift());
            }
            case InputConstants.KEY_LEFT -> {
                moveCursor(cursorPos - 1, e.hasShift());
            }
            case InputConstants.KEY_HOME -> moveCursor(0, e.hasShift());
            case InputConstants.KEY_END -> moveCursor(text.length(), e.hasShift());
            case InputConstants.KEY_ENTER -> {
                if (multiline) {
                    insertChar('\n');
                }
            }
            case InputConstants.KEY_A -> {
                if (e.hasCtrl()) {
                    selectAll();
                }
            }
            case InputConstants.KEY_C -> {
                if (e.hasCtrl() && hasSelection()) {
                    clipboard.setText(getSelectedText());
                }
            }
            case InputConstants.KEY_V -> {
                if (e.hasCtrl()) {
                    paste(clipboard.getText());
                }
            }
            case InputConstants.KEY_X -> {
                if (e.hasCtrl() && hasSelection()) {
                    clipboard.setText(getSelectedText());
                    deleteSelection();
                }
            }
        }
    }

    private void insertChar(char ch) {
        if (hasSelection()) {
            deleteSelection();
        }
        if (text.length() < maxLength) {
            text.insert(cursorPos, ch);
            cursorPos++;
            clearSelection();
        }
    }

    private void moveCursor(int newPos, boolean extendSelection) {
        int clamped = Math.max(0, Math.min(newPos, text.length()));
        if (extendSelection) {
            if (!hasSelection()) {
                selectionStart = cursorPos;
                selectionEnd = clamped;
            } else {
                selectionEnd = clamped;
            }
            cursorPos = clamped;
            return;
        }
        cursorPos = clamped;
        clearSelection();
    }

    private void paste(String str) {
        if (str == null || str.isEmpty()) return;
        if (hasSelection()) {
            deleteSelection();
        }
        int remaining = maxLength - text.length();
        String toInsert = str.substring(0, Math.min(str.length(), remaining));
        text.insert(cursorPos, toInsert);
        cursorPos += toInsert.length();
    }

    private boolean hasSelection() {
        return selectionStart != selectionEnd;
    }

    private void deleteSelection() {
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        text.delete(start, end);
        cursorPos = start;
        clearSelection();
    }

    private String getSelectedText() {
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        return text.substring(start, end);
    }

    private void selectAll() {
        selectionStart = 0;
        selectionEnd = text.length();
        cursorPos = text.length();
    }

    private void clearSelection() {
        selectionStart = selectionEnd = cursorPos;
    }

    public void render(UiRenderer r,
                       UiContext ctx,
                       UiInput input,
                       Theme theme,
                       int x,
                       int y,
                       int width,
                       int height,
                       boolean interactive) {
        if (r == null || theme == null) {
            return;
        }

        registerFocusable(ctx);

        boolean canInteract = interactive(ctx, interactive) && input != null;
        boolean hovered = canInteract && pxInside(input.mousePos().x, input.mousePos().y, x, y, width, height);
        boolean pressed = hovered && input.mouseDown();
        stepTransitions(ctx, theme, hovered, pressed);

        if (hovered && input.mousePressed()) {
            focus(ctx);
        }

        int bg = enabled()
            ? Theme.lerpArgb(theme.widgetBg, theme.widgetHover, hoverT())
            : Theme.toArgb(theme.disabledBg);
        if (enabled() && pressT() > 0.001f) {
            bg = Theme.lerpArgb(theme.widgetHover, theme.widgetActive, pressT() * 0.18f);
        }

        int outline = Theme.toArgb(isFocused(ctx) ? theme.widgetActive : theme.widgetOutline);
        int textColor = enabled() ? Theme.toArgb(theme.text) : Theme.toArgb(theme.disabledFg);
        int cursorColor = Theme.toArgb(theme.widgetActive);

        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, x, y, width, height, bg, outline, 1);
        } else {
            r.drawRect(x, y, width, height, bg);
            drawOutline(r, x, y, width, height, 1, outline);
        }
        if (enabled() && !pressed) {
            r.drawRect(x + 1, y + 1, width - 2, 1, 0x22000000);
        }

        drawFocusRing(r, theme, x, y, width, height);

        boolean showCaret = enabled() && isFocused(ctx);
        renderText(r, x, y, width, height, textColor, cursorColor, showCaret);
    }

    public void render(UiRenderer r, int x, int y, int width, int height, int bgColor, int textColor, int cursorColor) {
        r.drawRect(x, y, width, height, bgColor);
        renderText(r, x, y, width, height, textColor, cursorColor, true);
    }

    private void renderText(UiRenderer r,
                            int x,
                            int y,
                            int width,
                            int height,
                            int textColor,
                            int cursorColor,
                            boolean showCaret) {
        String str = text.toString();
        float textX = x + 4.0f;
        float baselineY = multiline ? (y + 4.0f + r.ascent()) : r.baselineForBox(y, height);

        int clipX = x + 2;
        int clipY = y + 2;
        int clipW = Math.max(0, width - 4);
        int clipH = Math.max(0, height - 4);
        r.flush();
        r.pushClip(clipX, clipY, clipW, clipH);

        if (hasSelection()) {
            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);
            start = Math.min(start, str.length());
            end = Math.min(end, str.length());

            CaretPos a = caretPosForIndex(str, start);
            CaretPos b = caretPosForIndex(str, end);
            if (a.lineIndex == b.lineIndex) {
                float lineBaseline = baselineY + a.lineIndex * r.lineHeight();
                float x0 = textX + r.measureText(str.substring(a.lineStartIndex, start));
                float x1 = textX + r.measureText(str.substring(b.lineStartIndex, end));
                float topY = lineBaseline - r.ascent();
                r.drawRect(x0, topY + 1.0f, Math.max(0.0f, x1 - x0), Math.max(0.0f, r.lineHeight() - 2.0f), 0x5050A0FF);
            }
        }

        r.drawText(str, textX, baselineY, textColor);

        if (showCaret) {
            int safeCursorPos = Math.min(cursorPos, str.length());
            CaretPos caret = caretPosForIndex(str, safeCursorPos);
            float lineBaseline = baselineY + caret.lineIndex * r.lineHeight();
            float cursorX = textX + r.measureText(str.substring(caret.lineStartIndex, safeCursorPos));
            float topY = lineBaseline - r.ascent();
            r.drawRect(cursorX, topY + 1.0f, 2.0f, Math.max(0.0f, r.lineHeight() - 2.0f), cursorColor);
        }

        r.flush();
        r.popClip();
    }

    private static CaretPos caretPosForIndex(String text, int index) {
        int lineIndex = 0;
        int lineStart = 0;
        int safeIndex = Math.max(0, Math.min(index, text.length()));
        for (int i = 0; i < safeIndex; i++) {
            if (text.charAt(i) == '\n') {
                lineIndex++;
                lineStart = i + 1;
            }
        }
        return new CaretPos(lineIndex, lineStart);
    }

    private record CaretPos(int lineIndex, int lineStartIndex) {}

    public String text() { return text.toString(); }
    public void setText(String str) {
        text.setLength(0);
        text.append(str != null ? str : "");
        cursorPos = Math.min(cursorPos, text.length());
        clearSelection();
    }
    public int cursorPos() { return cursorPos; }
    public void setCursorPos(int pos) { cursorPos = Math.max(0, Math.min(pos, text.length())); }
    public void setMaxLength(int max) { this.maxLength = Math.max(1, max); }
    public void setMultiline(boolean ml) { this.multiline = ml; }
}
