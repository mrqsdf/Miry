package com.miry.ui.widgets;

import com.miry.platform.InputConstants;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * Progress dialog overlay with optional cancel button.
 */
public final class ProgressDialog extends BaseWidget {
    private final String title;
    private String message = "";
    private boolean open;
    private boolean cancelable = false;
    private Runnable onCancel;
    private float progress = -1.0f; // < 0 = indeterminate
    private long startNanos = 0L;

    public ProgressDialog(String title) {
        this.title = title == null ? "Progress" : title;
        setFocusable(false);
    }

    public void setMessage(String message) {
        this.message = message != null ? message : "";
    }

    public void setProgress(float progress01) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress01));
    }

    public void setIndeterminate() {
        this.progress = -1.0f;
    }

    public void setCancelable(boolean cancelable, Runnable onCancel) {
        this.cancelable = cancelable;
        this.onCancel = onCancel;
    }

    public void open() {
        open = true;
        startNanos = System.nanoTime();
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean handleKey(KeyEvent e) {
        if (!open || e == null || !e.isPress()) return false;
        if (e.key() == InputConstants.KEY_ESCAPE && cancelable) {
            cancel();
            return true;
        }
        return false;
    }

    public boolean handleClick(int screenWidth, int screenHeight, int mx, int my) {
        if (!open) return false;

        int w = 520;
        int h = cancelable ? 190 : 160;
        int x = (screenWidth - w) / 2;
        int y = (screenHeight - h) / 2;

        if (cancelable) {
            int pad = 14;
            int btnW = 120;
            int btnH = 34;
            int btnX = x + w - pad - btnW;
            int btnY = y + h - pad - btnH;
            if (mx >= btnX && my >= btnY && mx < btnX + btnW && my < btnY + btnH) {
                cancel();
                return true;
            }
        }
        return true;
    }

    public void render(UiRenderer r,
                       UiInput input,
                       Theme theme,
                       int screenWidth,
                       int screenHeight,
                       int mouseX,
                       int mouseY) {
        if (!open || r == null || theme == null) return;

        r.drawRect(0, 0, screenWidth, screenHeight, 0x88000000);

        int dialogW = 520;
        int dialogH = cancelable ? 190 : 160;
        int x = (screenWidth - dialogW) / 2;
        int y = (screenHeight - dialogH) / 2;

        int bg = Theme.toArgb(theme.panelBg);
        int outline = Theme.toArgb(theme.widgetOutline);
        float radius = theme.design.radius_sm;
        int border = theme.design.border_thin;
        int top = Theme.lightenArgb(bg, 0.02f);
        int bottom = Theme.darkenArgb(bg, 0.02f);

        drawDropShadow(r, x, y, dialogW, dialogH, Theme.toArgb(theme.shadow), 0.0f, theme.design.space_xs, theme.design.shadow_lg, radius, 1.0f);
        r.drawRoundedRect(x, y, dialogW, dialogH, radius, top, top, bottom, bottom, border, outline);

        int pad = 14;
        int text = Theme.toArgb(theme.text);
        r.drawText(title, x + pad, r.baselineForBox(y + pad, 26), text);
        if (!message.isEmpty()) {
            r.drawText(message, x + pad, r.baselineForBox(y + pad + 26, 24), Theme.toArgb(theme.textMuted));
        }

        int barX = x + pad;
        int barY = y + (cancelable ? 88 : 78);
        int barW = dialogW - pad * 2;
        int barH = 18;

        int barBg = Theme.toArgb(theme.widgetBg);
        r.drawRoundedRect(barX, barY, barW, barH, radius, barBg);
        int fill = Theme.toArgb(theme.accent);

        if (progress >= 0.0f) {
            int fillW = Math.max(0, Math.min(barW, Math.round(barW * progress)));
            if (fillW > 0) {
                r.pushClipRect(barX, barY, barW, barH);
                r.drawRoundedRect(barX, barY, fillW, barH, radius, fill);
                r.popClipRect();
            }
            String pct = Math.round(progress * 100.0f) + "%";
            r.drawText(pct, barX + barW - r.measureText(pct), r.baselineForBox(barY, barH), Theme.toArgb(theme.textMuted));
        } else {
            // Indeterminate: animated stripe
            float t = (float) ((System.nanoTime() - startNanos) / 1_000_000_000.0);
            int stripeW = Math.max(18, barW / 4);
            int sx = barX + (int) ((t * 180.0f) % (barW + stripeW)) - stripeW;
            r.pushClipRect(barX, barY, barW, barH);
            r.drawRoundedRect(sx, barY, stripeW, barH, radius, fill);
            r.popClipRect();
        }

        r.drawRoundedRectOutline(barX, barY, barW, barH, radius, border, outline);

        String elapsed = formatElapsedSeconds((System.nanoTime() - startNanos) / 1_000_000_000.0);
        r.drawText(elapsed, barX, r.baselineForBox(barY + barH + 8, 24), Theme.toArgb(theme.textMuted));

        if (cancelable) {
            int btnW = 120;
            int btnH = 34;
            int btnX = x + dialogW - pad - btnW;
            int btnY = y + dialogH - pad - btnH;
            boolean hovered = input != null
                && mouseX >= btnX && mouseY >= btnY && mouseX < btnX + btnW && mouseY < btnY + btnH;
            int btnBg = hovered ? Theme.toArgb(theme.widgetHover) : Theme.toArgb(theme.widgetBg);
            r.drawRoundedRect(btnX, btnY, btnW, btnH, radius, btnBg, border, outline);
            r.drawText("Cancel", btnX + pad, r.baselineForBox(btnY, btnH), text);
        }
    }

    private void cancel() {
        if (onCancel != null) {
            onCancel.run();
        }
        close();
    }

    private static String formatElapsedSeconds(double seconds) {
        int s = Math.max(0, (int) Math.floor(seconds));
        int m = s / 60;
        int r = s % 60;
        if (m <= 0) return "Elapsed: " + r + "s";
        return "Elapsed: " + m + "m " + r + "s";
    }
}
