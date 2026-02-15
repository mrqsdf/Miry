package com.miry.ui.window;

import com.miry.ui.UiContext;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;

/**
 * In-editor floating window (not an OS window).
 */
public final class UiWindow {
    @FunctionalInterface
    public interface Content {
        void render(UiRenderer r, UiContext ctx, UiInput input, Theme theme, int x, int y, int w, int h);
    }

    private final int id;
    private String title;
    private int x;
    private int y;
    private int w;
    private int h;
    private boolean backdropBlur = true;
    private boolean resizable = true;
    private boolean movable = true;
    private Content content;

    UiWindow(int id, String title, int x, int y, int w, int h) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.x = x;
        this.y = y;
        this.w = Math.max(40, w);
        this.h = Math.max(30, h);
    }

    public int id() { return id; }
    public String title() { return title; }
    public void setTitle(String title) { this.title = title == null ? "" : title; }
    public int x() { return x; }
    public int y() { return y; }
    public int width() { return w; }
    public int height() { return h; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public void setSize(int w, int h) { this.w = Math.max(40, w); this.h = Math.max(30, h); }

    public boolean backdropBlur() { return backdropBlur; }
    public void setBackdropBlur(boolean enabled) { backdropBlur = enabled; }

    public boolean resizable() { return resizable; }
    public void setResizable(boolean enabled) { resizable = enabled; }

    public boolean movable() { return movable; }
    public void setMovable(boolean enabled) { movable = enabled; }

    public Content content() { return content; }
    public void setContent(Content content) { this.content = content; }
}

