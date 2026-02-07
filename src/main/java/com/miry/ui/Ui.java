package com.miry.ui;

import com.miry.graphics.Texture;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;
import org.joml.Vector2f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Small immediate-mode UI helper used by the dock/layout demo panels.
 * <p>
 * This is separate from the retained widgets demonstrated in {@code com.miry.demo.ComprehensiveDemo}.
 */
public final class Ui {
    private final Theme theme;
    private float dt;

    private final Vector2f mouse = new Vector2f();
    private final UiInput input = new UiInput();

    private int hotId;
    private int activeId;

    private int cursorX;
    private int cursorY;
    private int contentX;
    private int contentY;
    private int contentW;

    private final Map<Integer, Anim> anims = new HashMap<>();
    private final Map<Integer, ScrollState> scrollStates = new HashMap<>();

    private int idSeed = 0x1234567;
    private final Deque<Integer> idSeedStack = new ArrayDeque<>();
    private final Deque<LayoutState> layoutStack = new ArrayDeque<>();

    public Ui(Theme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    public Theme theme() {
        return theme;
    }

    public UiInput input() {
        return input;
    }

    public Vector2f mouse() {
        return mouse;
    }

    public void beginFrame(UiInput input, float dt) {
        this.dt = Math.max(0.0f, dt);
        idSeed = 0x1234567;
        idSeedStack.clear();
        this.input.setMousePos(input.mousePos().x, input.mousePos().y)
                .setMouseButtons(input.mouseDown(), input.mousePressed(), input.mouseReleased())
                .setScrollY(input.scrollY());
        mouse.set(this.input.mousePos());
        hotId = 0;
    }

    public void beginPanel(int x, int y, int width, int height) {
        layoutStack.clear();
        contentX = x + theme.tokens.padding;
        contentY = y + theme.tokens.padding;
        contentW = Math.max(1, width - theme.tokens.padding * 2);
        cursorX = contentX;
        cursorY = contentY;
    }

    public void endPanel() {
        if (input.mouseReleased() && activeId != 0) {
            activeId = 0;
        }
    }

    public void pushId(int value) {
        idSeedStack.push(idSeed);
        idSeed = mix(idSeed, value);
    }

    public void pushId(String value) {
        pushId(hash32(value == null ? "" : value));
    }

    public void popId() {
        if (!idSeedStack.isEmpty()) {
            idSeed = idSeedStack.pop();
        }
    }

    public boolean button(UiRenderer r, String label) {
        int id = id(label);
        Rect rect = nextRect(theme.tokens.itemHeight);
        boolean hovered = rect.contains(mouse.x, mouse.y);
        if (hovered) {
            hotId = id;
        }

        if (hovered && input.mousePressed()) {
            activeId = id;
        }

        boolean clicked = false;
        if (activeId == id && hovered && input.mouseReleased()) {
            clicked = true;
            activeId = 0;
        }
        if (activeId == id && input.mouseReleased() && !hovered) {
            activeId = 0;
        }

        float hoverT = anim(id).step(hovered ? 1.0f : 0.0f, dt, theme.tokens.animSpeed);
        int bg = Theme.lerpArgb(theme.widgetBg, theme.widgetHover, hoverT);
        if (activeId == id) {
            bg = Theme.lerpArgb(theme.widgetHover, theme.widgetActive, 0.35f);
        }

        int outline = Theme.toArgb(theme.widgetOutline);
        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, rect.x, rect.y, rect.w, rect.h, bg, outline, 1);
        } else {
            r.drawRect(rect.x, rect.y, rect.w, rect.h, bg);
            r.drawRect(rect.x, rect.y, rect.w, 1, outline);
        }
        float baselineY = r.baselineForBox(rect.y, rect.h);
        r.drawText(label, rect.x + 10, baselineY, Theme.toArgb(theme.text));

        return clicked;
    }

    public boolean toggle(UiRenderer r, String label, boolean value) {
        int id = id(label);
        Rect rect = nextRect(theme.tokens.itemHeight);
        boolean hovered = rect.contains(mouse.x, mouse.y);
        if (hovered) {
            hotId = id;
        }
        if (hovered && input.mousePressed()) {
            activeId = id;
        }
        if (activeId == id && hovered && input.mouseReleased()) {
            value = !value;
            activeId = 0;
        }
        if (activeId == id && input.mouseReleased() && !hovered) {
            activeId = 0;
        }

        float hoverT = anim(id).step(hovered ? 1.0f : 0.0f, dt, theme.tokens.animSpeed);
        int bg = Theme.lerpArgb(theme.widgetBg, theme.widgetHover, hoverT);
        int outline = Theme.toArgb(theme.widgetOutline);
        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, rect.x, rect.y, rect.w, rect.h, bg, outline, 1);
        } else {
            r.drawRect(rect.x, rect.y, rect.w, rect.h, bg);
        }

        int box = rect.h - 10;
        r.drawRect(rect.x + 6, rect.y + 5, box, box, value ? Theme.toArgb(theme.widgetActive) : Theme.toArgb(theme.widgetOutline));
        float baselineY = r.baselineForBox(rect.y, rect.h);
        r.drawText(label, rect.x + 6 + box + 10, baselineY, Theme.toArgb(theme.text));
        return value;
    }

    public ScrollArea beginScrollArea(UiRenderer r, String key, int x, int y, int width, int height, int contentHeight) {
        int id = id(key);
        ScrollState state = scrollStates.computeIfAbsent(id, k -> new ScrollState());

        boolean hovered = pxInside(mouse.x, mouse.y, x, y, width, height);
        if (hovered && input.scrollY() != 0.0) {
            state.scrollY -= (float) (input.scrollY() * 28.0);
        }

        int maxScroll = Math.max(0, contentHeight - height);
        state.scrollY = clamp(state.scrollY, 0.0f, maxScroll);

        layoutStack.push(new LayoutState(contentX, contentY, contentW, cursorX, cursorY));

        r.flush();
        r.pushClip(x, y, width, height);

        contentX = x;
        contentY = y - Math.round(state.scrollY);
        contentW = Math.max(1, width);
        cursorX = contentX;
        cursorY = contentY;

        return new ScrollArea(r, id, x, y, width, height, maxScroll, state.scrollY);
    }

    public void endScrollArea(ScrollArea area) {
        area.renderer.flush();
        area.renderer.popClip();
        if (!layoutStack.isEmpty()) {
            LayoutState prev = layoutStack.pop();
            contentX = prev.contentX;
            contentY = prev.contentY;
            contentW = prev.contentW;
            cursorX = prev.cursorX;
            cursorY = prev.cursorY;
        }
    }

    public float sliderFloat(UiRenderer r, String label, float value, float min, float max) {
        int id = id(label);
        Rect rect = nextRect(theme.tokens.itemHeight);

        boolean hovered = rect.contains(mouse.x, mouse.y);
        if (hovered) {
            hotId = id;
        }
        if (hovered && input.mousePressed()) {
            activeId = id;
        }
        if (activeId == id && input.mouseDown()) {
            float t = (mouse.x - rect.x) / rect.w;
            t = clamp01(t);
            value = min + (max - min) * t;
        }
        if (activeId == id && input.mouseReleased()) {
            activeId = 0;
        }

        float hoverT = anim(id).step((hovered || activeId == id) ? 1.0f : 0.0f, dt, theme.tokens.animSpeed);
        int bg = Theme.lerpArgb(theme.widgetBg, theme.widgetHover, hoverT);
        int outline = Theme.toArgb(theme.widgetOutline);
        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, rect.x, rect.y, rect.w, rect.h, bg, outline, 1);
        } else {
            r.drawRect(rect.x, rect.y, rect.w, rect.h, bg);
        }

        float t = (value - min) / (max - min);
        t = clamp01(t);
        float fillW = rect.w * t;
        r.drawRect(rect.x, rect.y, fillW, rect.h, Theme.toArgb(theme.widgetActive));

        float baselineY = r.baselineForBox(rect.y, rect.h);
        r.drawText(label, rect.x + 10, baselineY, Theme.toArgb(theme.text));
        return value;
    }

    public void image(UiRenderer r, Texture texture, int width, int height, int tintArgb) {
        Rect rect = nextRect(height);
        int iw = Math.min(width, rect.w);
        int ih = Math.min(height, rect.h);
        r.drawTexturedRect(texture, rect.x, rect.y, iw, ih, tintArgb);
    }

    public void label(UiRenderer r, String text, boolean muted) {
        Rect rect = nextRect(theme.tokens.itemHeight);
        int color = Theme.toArgb(muted ? theme.textMuted : theme.text);
        float baselineY = r.baselineForBox(rect.y, rect.h);
        r.drawText(text, rect.x, baselineY, color);
    }

    public void spacer(int pixels) {
        cursorY += Math.max(0, pixels);
    }

    public void separator(UiRenderer r) {
        spacer(2);
        r.drawRect(contentX, cursorY, contentW, 1, Theme.toArgb(theme.widgetOutline));
        spacer(theme.tokens.itemSpacing);
    }

    // (no outline helper; nine-slice outline uses NineSlice.drawWithOutline)

    private Rect nextRect(int height) {
        int h = Math.max(1, height);
        Rect r = new Rect(cursorX, cursorY, contentW, h);
        cursorY += h + theme.tokens.itemSpacing;
        return r;
    }

    private int id(String s) {
        return mix(idSeed, hash32(s));
    }

    private Anim anim(int id) {
        return anims.computeIfAbsent(id, k -> new Anim());
    }

    private static int hash32(String s) {
        int h = 0x811C9DC5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x01000193;
        }
        return h == 0 ? 1 : h;
    }

    private static int mix(int seed, int value) {
        int h = seed;
        h ^= value;
        h *= 0x01000193;
        h ^= (h >>> 16);
        return h == 0 ? 1 : h;
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static boolean pxInside(float px, float py, int x, int y, int w, int h) {
        return px >= x && py >= y && px < (x + w) && py < (y + h);
    }

    private static final class Anim {
        private float value;

        float step(float target, float dt, float speed) {
            float k = 1.0f - (float) Math.exp(-Math.max(0.0f, speed) * dt);
            value += (target - value) * k;
            return value;
        }
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(float px, float py) {
            return px >= x && py >= y && px < (x + w) && py < (y + h);
        }
    }

    private record LayoutState(int contentX, int contentY, int contentW, int cursorX, int cursorY) {}

    private static final class ScrollState {
        private float scrollY;
    }

    public record ScrollArea(UiRenderer renderer, int id, int x, int y, int width, int height, int maxScroll, float scrollY) {}
}
