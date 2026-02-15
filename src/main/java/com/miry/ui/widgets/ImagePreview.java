package com.miry.ui.widgets;

import com.miry.graphics.Texture;
import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * Image preview widget with pan, zoom, and channel isolation controls.
 */
public final class ImagePreview extends BaseWidget {
    public enum ChannelMode { RGB, RED, GREEN, BLUE, ALPHA, LUMINANCE }

    private Texture texture;
    private float panX = 0.0f;
    private float panY = 0.0f;
    private float zoom = 1.0f;
    private ChannelMode channelMode = ChannelMode.RGB;
    private boolean showCheckerboard = true;
    private boolean showPixelGrid = false;
    private boolean dragging = false;
    private float dragStartX = 0.0f;
    private float dragStartY = 0.0f;
    private float panStartX = 0.0f;
    private float panStartY = 0.0f;

    public void setTexture(Texture texture) {
        this.texture = texture;
        resetView();
    }

    public Texture texture() {
        return texture;
    }

    public void setChannelMode(ChannelMode mode) {
        this.channelMode = mode != null ? mode : ChannelMode.RGB;
    }

    public ChannelMode channelMode() {
        return channelMode;
    }

    public void setShowCheckerboard(boolean show) {
        this.showCheckerboard = show;
    }

    public boolean showCheckerboard() {
        return showCheckerboard;
    }

    public void setShowPixelGrid(boolean show) {
        this.showPixelGrid = show;
    }

    public boolean showPixelGrid() {
        return showPixelGrid;
    }

    public void resetView() {
        panX = 0.0f;
        panY = 0.0f;
        zoom = 1.0f;
    }

    public void fitToView(int viewWidth, int viewHeight) {
        if (texture == null) return;
        float scaleX = viewWidth / (float) texture.width();
        float scaleY = viewHeight / (float) texture.height();
        zoom = Math.min(scaleX, scaleY) * 0.95f;
        panX = 0.0f;
        panY = 0.0f;
    }

    public void render(UiRenderer r, UiContext ctx, UiInput input, Theme theme, int x, int y, int width, int height, boolean interactive) {
        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = canInteract ? input.mousePos().x : -1;
        float my = canInteract ? input.mousePos().y : -1;
        boolean mousePressed = canInteract && input.mousePressed();
        boolean mouseDown = canInteract && input.mouseDown();
        boolean mouseReleased = canInteract && input.mouseReleased();

        boolean hovered = canInteract && mx >= x && my >= y && mx < x + width && my < y + height;

        // Background
        r.drawRect(x, y, width, height, Theme.darkenArgb(Theme.toArgb(theme.panelBg), 0.1f));

        r.pushClipRect(x, y, width, height);

        if (texture != null) {
            int imgW = texture.width();
            int imgH = texture.height();
            int displayW = Math.round(imgW * zoom);
            int displayH = Math.round(imgH * zoom);
            int imgX = x + width / 2 + Math.round(panX) - displayW / 2;
            int imgY = y + height / 2 + Math.round(panY) - displayH / 2;

            // Checkerboard background for transparency
            if (showCheckerboard) {
                drawCheckerboard(r, imgX, imgY, displayW, displayH, 8);
            }

            // Draw image with channel mode
            int tint = getChannelTint();
            r.drawTexture(texture, imgX, imgY, displayW, displayH, tint);

            // Pixel grid at high zoom
            if (showPixelGrid && zoom >= 8.0f) {
                drawPixelGrid(r, theme, imgX, imgY, displayW, displayH, imgW, imgH);
            }
        }

        r.popClipRect();

        // Handle pan
        if (hovered && mousePressed) {
            dragging = true;
            dragStartX = mx;
            dragStartY = my;
            panStartX = panX;
            panStartY = panY;
            if (ctx != null) {
                ctx.pointer().capture(id());
            }
        }

        if (dragging && mouseDown) {
            panX = panStartX + (mx - dragStartX);
            panY = panStartY + (my - dragStartY);
        }

        if (dragging && mouseReleased) {
            dragging = false;
            if (ctx != null && ctx.pointer().isCaptured(id())) {
                ctx.pointer().release();
            }
        }

        // Handle zoom
        if (hovered && input != null) {
            float scroll = input.consumeMouseScrollDelta();
            if (scroll != 0.0f) {
                float oldZoom = zoom;
                zoom *= (1.0f + scroll * 0.1f);
                zoom = Math.max(0.05f, Math.min(32.0f, zoom));

                // Zoom towards mouse cursor
                float localX = mx - (x + width / 2 + panX);
                float localY = my - (y + height / 2 + panY);
                float zoomFactor = zoom / oldZoom;
                panX += localX * (1.0f - zoomFactor);
                panY += localY * (1.0f - zoomFactor);
            }
        }

        // Info overlay
        if (texture != null) {
            String info = String.format("%dx%d | %.0f%%", texture.width(), texture.height(), zoom * 100.0f);
            if (channelMode != ChannelMode.RGB) {
                info += " | " + channelMode.name();
            }
            int infoColor = Theme.toArgb(theme.text);
            int infoBg = Theme.mulAlpha(Theme.toArgb(theme.panelBg), 0.85f);
            float infoW = r.measureText(info);
            int infoX = x + theme.design.space_sm;
            int infoY = y + theme.design.space_sm;
            int infoH = Math.round(r.lineHeight());
            int pad = theme.design.space_xs;
            r.drawRoundedRect(infoX, infoY, infoW + pad * 2, infoH + pad * 2, theme.design.radius_sm, infoBg);
            r.drawText(info, infoX + pad, r.baselineForBox(infoY + pad, infoH), infoColor);
        }
    }

    public void render(UiRenderer r, UiInput input, Theme theme, int x, int y, int width, int height) {
        render(r, null, input, theme, x, y, width, height, true);
    }

    private void drawCheckerboard(UiRenderer r, int x, int y, int w, int h, int tileSize) {
        int light = 0xFFCCCCCC;
        int dark = 0xFFAAAAAA;

        int startX = x;
        int startY = y;
        int endX = x + w;
        int endY = y + h;

        for (int ty = startY; ty < endY; ty += tileSize) {
            for (int tx = startX; tx < endX; tx += tileSize) {
                int row = (ty - startY) / tileSize;
                int col = (tx - startX) / tileSize;
                boolean isLight = (row + col) % 2 == 0;
                int color = isLight ? light : dark;
                int tw = Math.min(tileSize, endX - tx);
                int th = Math.min(tileSize, endY - ty);
                r.drawRect(tx, ty, tw, th, color);
            }
        }
    }

    private void drawPixelGrid(UiRenderer r, Theme theme, int imgX, int imgY, int displayW, int displayH, int imgW, int imgH) {
        int gridColor = Theme.mulAlpha(Theme.toArgb(theme.text), 0.2f);
        float pixelW = displayW / (float) imgW;
        float pixelH = displayH / (float) imgH;

        // Draw vertical lines
        for (int i = 0; i <= imgW; i++) {
            int lx = Math.round(imgX + i * pixelW);
            r.drawRect(lx, imgY, 1, displayH, gridColor);
        }

        // Draw horizontal lines
        for (int i = 0; i <= imgH; i++) {
            int ly = Math.round(imgY + i * pixelH);
            r.drawRect(imgX, ly, displayW, 1, gridColor);
        }
    }

    private int getChannelTint() {
        return switch (channelMode) {
            case RGB -> 0xFFFFFFFF;
            case RED -> 0xFFFF0000;
            case GREEN -> 0xFF00FF00;
            case BLUE -> 0xFF0000FF;
            case ALPHA -> 0xFFFFFFFF; // Alpha visualization would need shader support
            case LUMINANCE -> 0xFFFFFFFF; // Luminance would need shader support
        };
    }
}
