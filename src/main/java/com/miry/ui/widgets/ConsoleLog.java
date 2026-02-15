package com.miry.ui.widgets;

import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Console log with colored output and filtering.
 */
public final class ConsoleLog extends BaseWidget {
    public enum LogLevel { INFO, WARNING, ERROR, DEBUG }

    public static final class LogEntry {
        public final LogLevel level;
        public final String message;
        public final long timestamp;

        public LogEntry(LogLevel level, String message) {
            this.level = level;
            this.message = message != null ? message : "";
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final List<LogEntry> entries = new ArrayList<>();
    private final boolean[] levelFilter = {true, true, true, true};
    private boolean autoScroll = true;
    private int scrollOffset = 0;

    public void clear() { entries.clear(); scrollOffset = 0; }
    public void log(LogLevel level, String message) { entries.add(new LogEntry(level, message)); }
    public void info(String message) { log(LogLevel.INFO, message); }
    public void warning(String message) { log(LogLevel.WARNING, message); }
    public void error(String message) { log(LogLevel.ERROR, message); }
    public void debug(String message) { log(LogLevel.DEBUG, message); }

    public void render(UiRenderer r, UiInput input, Theme theme, int x, int y, int width, int height) {
        r.drawRect(x, y, width, height, Theme.toArgb(theme.panelBg));

        int pad = theme.design.space_sm;
        int lineHeight = (int)r.lineHeight();
        int visibleLines = Math.max(1, (height - pad * 2) / lineHeight);

        boolean hovered = input != null
            && input.mousePos().x >= x && input.mousePos().y >= y
            && input.mousePos().x < x + width && input.mousePos().y < y + height;

        List<LogEntry> visible = new ArrayList<>();
        for (LogEntry entry : entries) {
            if (levelFilter[entry.level.ordinal()]) visible.add(entry);
        }

        if (autoScroll) scrollOffset = Math.max(0, visible.size() - visibleLines);

        // Scrolling
        if (hovered && enabled()) {
            float wheelDelta = input.consumeMouseScrollDelta();
            if (wheelDelta != 0) {
                scrollOffset -= (int)(wheelDelta * 3);
                scrollOffset = Math.max(0, Math.min(visible.size() - visibleLines, scrollOffset));
                autoScroll = false;
            }
        }

        r.pushClipRect(x, y, width, height);
        int cursorY = y + pad;

        for (int i = scrollOffset; i < visible.size() && i < scrollOffset + visibleLines; i++) {
            LogEntry entry = visible.get(i);

            int textColor = switch (entry.level) {
                case INFO -> Theme.toArgb(theme.text);
                case WARNING -> 0xFFFFAA00;
                case ERROR -> 0xFFFF4444;
                case DEBUG -> Theme.toArgb(theme.textMuted);
            };

            String prefix = switch (entry.level) {
                case INFO -> "[INFO] ";
                case WARNING -> "[WARN] ";
                case ERROR -> "[ERROR] ";
                case DEBUG -> "[DEBUG] ";
            };

            String text = prefix + entry.message;
            String clipped = r.clipText(text, width - pad * 2);
            r.drawText(clipped, x + pad, cursorY + r.ascent(), textColor);
            cursorY += lineHeight;
        }

        r.popClipRect();
    }
}
