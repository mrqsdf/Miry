package com.miry.ui.widgets;

import com.miry.platform.InputConstants;
import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

import java.io.File;
import java.nio.file.Path;

/**
 * Simple file picker dialog: file tree + OK/Cancel.
 */
public final class FilePickerDialog extends BaseWidget {
    @FunctionalInterface
    public interface PickHandler {
        void onPick(Path path);
    }

    private final String title;
    private final ScrollView scroll = new ScrollView(1, 1);
    private FileBrowser browser;
    private boolean open;
    private PickHandler onPick;
    private Runnable onCancel;

    public FilePickerDialog(String title, Path rootPath) {
        this.title = title == null ? "Pick File" : title;
        setRoot(rootPath);
        setFocusable(false);
    }

    public void setRoot(Path rootPath) {
        Path p = rootPath != null ? rootPath : Path.of(".");
        browser = new FileBrowser(p, 24);
    }

    public void open(PickHandler onPick, Runnable onCancel) {
        this.onPick = onPick;
        this.onCancel = onCancel;
        this.open = true;
    }

    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean handleKey(UiContext ctx, KeyEvent e) {
        if (!open || e == null || !e.isPressOrRepeat()) return false;
        if (e.key() == InputConstants.KEY_ESCAPE) {
            cancel();
            return true;
        }
        if (browser != null && browser.handleKey(ctx, e)) return true;
        if (e.key() == InputConstants.KEY_ENTER) {
            pickSelected();
            return true;
        }
        return false;
    }

    public void render(UiRenderer r,
                       UiContext ctx,
                       UiInput input,
                       Theme theme,
                       int screenWidth,
                       int screenHeight) {
        if (!open || r == null || theme == null || input == null) return;

        float mx = input.mousePos().x;
        float my = input.mousePos().y;

        r.drawRect(0, 0, screenWidth, screenHeight, 0x88000000);

        int dialogW = Math.min(880, Math.max(540, screenWidth - 160));
        int dialogH = Math.min(640, Math.max(420, screenHeight - 140));
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

        int pad = theme.design.space_md;
        int headerH = 44;
        int footerH = 56;
        int listX = x + pad;
        int listY = y + headerH;
        int listW = dialogW - pad * 2;
        int listH = dialogH - headerH - footerH;

        r.drawText(title, x + pad, r.baselineForBox(y, headerH), Theme.toArgb(theme.text));

        scroll.configureFromTheme(theme);
        scroll.setViewSize(listW, listH);

        int contentH = browser != null ? browser.computeContentHeight() : 1;
        scroll.setContentSize(listW, contentH);

        boolean hoveredList = mx >= listX && my >= listY && mx < listX + listW && my < listY + listH;
        if (hoveredList && input.scrollY() != 0.0) {
            double wheel = input.consumeScrollY();
            scroll.scroll(0.0f, (float) (-wheel * 28.0));
        }
        scroll.handleScrollbarInput(ctx, input, listX, listY);

        r.pushClipRect(listX, listY, listW, listH);
        int scrollOffset = Math.round(scroll.scrollY());
        if (browser != null) {
            browser.render(r, ctx, input, theme, listX, listY, listW, listH, scrollOffset, true);
        }
        r.popClipRect();

        int track = Theme.mulAlpha(Theme.toArgb(theme.widgetOutline), 0.35f);
        int thumb = Theme.mulAlpha(Theme.toArgb(theme.widgetOutline), 0.65f);
        int thumbHover = Theme.toArgb(theme.widgetOutline);
        scroll.renderScrollbars(r, input, listX, listY, track, thumb, thumbHover);

        // Buttons
        int btnW = 120;
        int btnH = 34;
        int btnY = y + dialogH - (pad + btnH);
        int cancelX = x + dialogW - pad - btnW;
        int okX = cancelX - pad - btnW;

        boolean okHover = mx >= okX && my >= btnY && mx < okX + btnW && my < btnY + btnH;
        boolean cancelHover = mx >= cancelX && my >= btnY && mx < cancelX + btnW && my < btnY + btnH;

        int btnBg = Theme.toArgb(theme.widgetBg);
        int btnHoverBg = Theme.toArgb(theme.widgetHover);

        r.drawRoundedRect(okX, btnY, btnW, btnH, radius, okHover ? btnHoverBg : btnBg, border, outline);
        r.drawRoundedRect(cancelX, btnY, btnW, btnH, radius, cancelHover ? btnHoverBg : btnBg, border, outline);
        r.drawText("OK", okX + pad, r.baselineForBox(btnY, btnH), Theme.toArgb(theme.text));
        r.drawText("Cancel", cancelX + pad, r.baselineForBox(btnY, btnH), Theme.toArgb(theme.text));

        // Selected path preview
        File sel = browser != null ? browser.selectedFile() : null;
        String selText = sel != null ? sel.getPath() : "(no selection)";
        String clipped = r.clipText(selText, dialogW - pad * 4 - btnW * 2);
        r.drawText(clipped, x + pad, r.baselineForBox(btnY, btnH), Theme.toArgb(theme.textMuted));

        // Input handling
        if (input.mousePressed()) {
            boolean inside = mx >= x && my >= y && mx < x + dialogW && my < y + dialogH;
            if (!inside) {
                cancel();
            } else {
                if (okHover) {
                    pickSelected();
                } else if (cancelHover) {
                    cancel();
                }
            }
        }
    }

    private void pickSelected() {
        if (browser == null) return;
        File f = browser.selectedFile();
        if (f == null) return;
        if (onPick != null) {
            onPick.onPick(f.toPath());
        }
        close();
    }

    private void cancel() {
        if (onCancel != null) {
            onCancel.run();
        }
        close();
    }
}
