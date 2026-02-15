package com.miry.ui.widgets;

import com.miry.ui.core.BaseWidget;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Status bar with left/center/right sections and progress indicator.
 */
public final class StatusBar extends BaseWidget {
    public enum Section { LEFT, CENTER, RIGHT }

    public static final class StatusItem {
        public final Section section;
        public String text;
        public int color = 0;

        public StatusItem(Section section, String text) {
            this.section = section;
            this.text = text != null ? text : "";
        }
    }

    private final List<StatusItem> items = new ArrayList<>();
    private float progress = -1.0f;
    private String progressText = "";

    public void clear() {
        items.clear();
        progress = -1.0f;
    }

    public void addItem(StatusItem item) { items.add(item); }
    public void addLeft(String text) { items.add(new StatusItem(Section.LEFT, text)); }
    public void addCenter(String text) { items.add(new StatusItem(Section.CENTER, text)); }
    public void addRight(String text) { items.add(new StatusItem(Section.RIGHT, text)); }

    public void setProgress(float progress, String text) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
        this.progressText = text != null ? text : "";
    }

    public void clearProgress() { progress = -1.0f; }

    public void render(UiRenderer r, Theme theme, int x, int y, int width, int height) {
        r.drawRect(x, y, width, height, Theme.toArgb(theme.headerBg));
        r.drawRect(x, y, width, 1, Theme.toArgb(theme.headerLine));

        int pad = theme.design.space_sm;

        // Progress bar
        if (progress >= 0.0f) {
            int barW = 200;
            int barH = height - pad * 2;
            int barX = x + width - barW - pad;
            int barY = y + pad;

            r.drawRoundedRect(barX, barY, barW, barH, theme.design.radius_sm, Theme.toArgb(theme.widgetBg));
            int fillW = (int)(barW * progress);
            if (fillW > 0) {
                r.pushClipRect(barX, barY, barW, barH);
                r.drawRoundedRect(barX, barY, fillW, barH, theme.design.radius_sm, Theme.toArgb(theme.accent));
                r.popClipRect();
            }
            r.drawRoundedRectOutline(barX, barY, barW, barH, theme.design.radius_sm, theme.design.border_thin, Theme.toArgb(theme.widgetOutline));

            if (!progressText.isEmpty()) {
                float tw = r.measureText(progressText);
                r.drawText(progressText, barX + (barW - tw) / 2, r.baselineForBox(barY, barH), Theme.toArgb(theme.text));
            }
            width = barX - x - pad * 2;
        }

        // Items
        List<StatusItem> left = new ArrayList<>(), center = new ArrayList<>(), right = new ArrayList<>();
        for (StatusItem item : items) {
            switch (item.section) {
                case LEFT -> left.add(item);
                case CENTER -> center.add(item);
                case RIGHT -> right.add(item);
            }
        }

        float baseline = r.baselineForBox(y, height);
        int mutedColor = Theme.toArgb(theme.textMuted);

        int cursorX = x + pad;
        for (StatusItem item : left) {
            r.drawText(item.text, cursorX, baseline, item.color != 0 ? item.color : mutedColor);
            cursorX += (int)r.measureText(item.text) + pad * 2;
        }

        if (!center.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < center.size(); i++) {
                if (i > 0) sb.append("  |  ");
                sb.append(center.get(i).text);
            }
            float tw = r.measureText(sb.toString());
            r.drawText(sb.toString(), x + (width - tw) / 2, baseline, mutedColor);
        }

        cursorX = x + width - pad;
        for (int i = right.size() - 1; i >= 0; i--) {
            StatusItem item = right.get(i);
            float tw = r.measureText(item.text);
            r.drawText(item.text, cursorX - tw, baseline, item.color != 0 ? item.color : mutedColor);
            cursorX -= (int)tw + pad * 2;
        }
    }
}
