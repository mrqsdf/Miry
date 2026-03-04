package com.miry.ui;

import com.miry.graphics.Texture;
import com.miry.ui.component.*;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;
import org.joml.Vector2f;

import java.util.*;
import java.util.function.BiConsumer;

import static com.miry.ui.util.MathUtils.clamp01;

/**
 * Immediate-mode UI (IMUI) helper for rapid prototyping and layout-driven panels.
 * <p>
 * This class provides a stateless, procedural API for drawing buttons, toggles, labels, and scroll areas.
 * It is distinct from the retained-mode {@link com.miry.ui.widgets} package, although they share the same
 * {@link Theme} and rendering backend.
 * </p>
 * <p>
 * Ideal for debugging tools, inspector panels, and dynamic layouts where state management overhead is undesirable.
 * </p>
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

    /**
     * Creates a new UI context with the specified theme.
     *
     * @param theme The theme to use for styling widgets.
     */
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

    /**
     * Prepares the UI for a new frame.
     * <p>
     * Must be called once per frame before any widget methods.
     * </p>
     *
     * @param input The current input state.
     * @param dt    The delta time since the last frame.
     */
    public void beginFrame(UiInput input, float dt) {
        this.dt = Math.max(0.0f, dt);
        idSeed = 0x1234567;
        idSeedStack.clear();
        this.input.setMousePos(input.mousePos().x, input.mousePos().y)
                .setMouseButtons(input.mouseDown(), input.mousePressed(), input.mouseReleased())
                .setModifiers(input.ctrlDown(), input.shiftDown(), input.altDown(), input.superDown())
                .setScrollY(input.scrollY());
        mouse.set(this.input.mousePos());
        hotId = 0;
        if (activeId != 0 && !this.input.mouseDown() && !this.input.mouseReleased()) {
            activeId = 0;
        }
    }

    /**
     * Begins a layout panel region.
     * <p>
     * Widgets drawn after this call will be positioned relative to the panel's content area.
     * </p>
     *
     * @param x      The x-coordinate of the panel.
     * @param y      The y-coordinate of the panel.
     * @param width  The width of the panel.
     * @param height The height of the panel.
     */
    public void beginPanel(int x, int y, int width, int height) {
        layoutStack.clear();
        contentX = x + theme.tokens.padding;
        contentY = y + theme.tokens.padding;
        contentW = Math.max(1, width - theme.tokens.padding * 2);
        cursorX = contentX;
        cursorY = contentY;
    }

    /**
     * Ends the current panel region.
     */
    public void endPanel() {
    }

    /**
     * Pushes an integer ID to the ID stack to prevent hash collisions.
     *
     * @param value The value to mix into the current ID hash.
     */
    public void pushId(int value) {
        idSeedStack.push(idSeed);
        idSeed = mix(idSeed, value);
    }

    /**
     * Pushes a string ID to the ID stack.
     *
     * @param value The string to hash and mix into the current ID.
     */
    public void pushId(String value) {
        pushId(hash32(value == null ? "" : value));
    }

    /**
     * Pops the last ID from the ID stack.
     */
    public void popId() {
        if (!idSeedStack.isEmpty()) {
            idSeed = idSeedStack.pop();
        }
    }

    /**
     * Draws a button widget.
     *
     * @param r     The renderer to use.
     * @param label The text to display on the button.
     * @return {@code true} if the button was clicked this frame.
     */
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

        int outline = theme.widgetOutline.getArgb();
        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, rect.x, rect.y, rect.w, rect.h, bg, outline, 1);
        } else {
            float radius = theme.design.radius_sm;
            int border = theme.design.border_thin;
            drawBevelButton(r, rect.x, rect.y, rect.w, rect.h, radius, border, bg, outline);
        }
        float baselineY = r.baselineForBox(rect.y, rect.h);
        r.drawText(label, rect.x + 10, baselineY, theme.text.getArgb());

        return clicked;
    }

    //todo add doc
    public boolean button(UiRenderer r, ButtonComponent component) {
        TextComponent labelComp = component.getLabel();
        List<TextComponent> components = new ArrayList<>();
        collectTextComponents(labelComp, components);
        int id = id(component.getId());
        Rect rect = nextRect(theme.tokens.itemHeight);
        boolean hovered = rect.contains(mouse.x, mouse.y);
        boolean clicked = buttonAction(hovered, id);
        float hoverT = anim(id).step(hovered ? 1.0f : 0.0f, dt, theme.tokens.animSpeed);
        int bg = Theme.lerpArgb(component.getBgColor(this), component.getHoverColor(this), hoverT);
        if (activeId == id) {
            bg = Theme.lerpArgb(component.getHoverColor(this), component.getActiveColor(this), 0.35f);
        }

        int outline = theme.widgetOutline.getArgb();
        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, rect.x, rect.y, rect.w, rect.h, bg, outline, 1);
        } else {
            float radius = theme.design.radius_sm;
            int border = theme.design.border_thin;
            drawBevelButton(r, rect.x, rect.y, rect.w, rect.h, radius, border, bg, outline);
        }
        float baselineY = r.baselineForBox(rect.y, rect.h);
        int x = rect.x + 10;
        drawTextComponents(r, components, x, baselineY);
        if (clicked && component.getOnClick() != null) {
            component.getOnClick().run();
        }

        return clicked;
    }

    private boolean button(UiRenderer r, ButtonComponent component, Rect rect) {
        TextComponent labelComp = component.getLabel();
        List<TextComponent> components = new ArrayList<>();
        collectTextComponents(labelComp, components);
        int id = id(component.getId());
        boolean hovered = rect.contains(mouse.x, mouse.y);
        boolean clicked = buttonAction(hovered, id);
        float hoverT = anim(id).step(hovered ? 1.0f : 0.0f, dt, theme.tokens.animSpeed);
        int bg = Theme.lerpArgb(component.getBgColor(this), component.getHoverColor(this), hoverT);
        if (activeId == id) {
            bg = Theme.lerpArgb(component.getHoverColor(this), component.getActiveColor(this), 0.35f);
        }

        int outline = theme.widgetOutline.getArgb();
        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, rect.x, rect.y, rect.w, rect.h, bg, outline, 1);
        } else {
            float radius = theme.design.radius_sm;
            int border = theme.design.border_thin;
            drawBevelButton(r, rect.x, rect.y, rect.w, rect.h, radius, border, bg, outline);
        }
        float baselineY = r.baselineForBox(rect.y, rect.h);
        int x = rect.x + 10;
        drawTextComponents(r, components, x, baselineY);
        if (clicked && component.getOnClick() != null) {
            component.getOnClick().run();
        }

        return clicked;
    }

    private boolean buttonAction(boolean hovered, int id) {
        boolean clicked = false;
        if (hovered) {
            hotId = id;
        }
        if (hovered && input.mousePressed()) {
            activeId = id;
        }
        if (activeId == id && hovered && input.mouseReleased()) {
            clicked = true;
            activeId = 0;
        }
        if (activeId == id && input.mouseReleased() && !hovered) {
            activeId = 0;
        }
        return clicked;
    }

    /**
     * Draws a toggle switch / checkbox.
     *
     * @param r     The renderer to use.
     * @param label The text label.
     * @param value The current state of the toggle.
     * @return The new state of the toggle (toggled if clicked).
     */
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
        int outline = theme.widgetOutline.getArgb();
        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, rect.x, rect.y, rect.w, rect.h, bg, outline, 1);
        } else {
            float radius = theme.design.radius_sm;
            int border = theme.design.border_thin;
            drawBevelButton(r, rect.x, rect.y, rect.w, rect.h, radius, border, bg, outline);
        }

        int box = rect.h - 10;
        float boxR = Math.min(3.0f, theme.design.radius_sm);
        int boxFill = value ? theme.widgetActive.getArgb() : theme.widgetOutline.getArgb();
        r.drawRoundedRect(rect.x + 6, rect.y + 5, box, box, boxR, boxFill);
        float baselineY = r.baselineForBox(rect.y, rect.h);
        r.drawText(label, rect.x + 6 + box + 10, baselineY, theme.text.getArgb());
        return value;
    }

    //todo add doc
    public boolean toggle(UiRenderer r, ToggleComponent component) {
        Rect rect = nextRect(theme.tokens.itemHeight);
        return toggle(r, component, rect);
    }

    private boolean toggle(UiRenderer r, ToggleComponent component, Rect rect) {
        int id = id(component.getId());
        boolean value = component.isToggled();
        boolean changed = false;
        boolean hovered = rect.contains(mouse.x, mouse.y);
        if (hovered) {
            hotId = id;
        }
        if (hovered && input.mousePressed()) {
            activeId = id;
        }
        if (activeId == id && hovered && input.mouseReleased()) {
            value = !value;
            changed = true;
            activeId = 0;
        }
        if (activeId == id && input.mouseReleased() && !hovered) {
            activeId = 0;
        }

        float hoverT = anim(id).step(hovered ? 1.0f : 0.0f, dt, theme.tokens.animSpeed);
        int bg = Theme.lerpArgb(theme.widgetBg, theme.widgetHover, hoverT);
        int outline = theme.widgetOutline.getArgb();
        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, rect.x, rect.y, rect.w, rect.h, bg, outline, 1);
        } else {
            float radius = theme.design.radius_sm;
            int border = theme.design.border_thin;
            drawBevelButton(r, rect.x, rect.y, rect.w, rect.h, radius, border, bg, outline);
        }

        int box = rect.h - 10;
        float boxR = Math.min(3.0f, theme.design.radius_sm);
        int boxFill = component.getToggleColor(this).getArgb();
        r.drawRoundedRect(rect.x + 6, rect.y + 5, box, box, boxR, boxFill);

        List<TextComponent> components = new ArrayList<>();
        collectTextComponents(component.getLabel(), components);
        float baselineY = r.baselineForBox(rect.y, rect.h);
        int x = rect.x + 6 + box + 10;
        drawTextComponents(r, components, x, baselineY);

        if (changed && component.getOnChange() != null) {
            component.getOnChange().accept(value);
        }

        return value;
    }

    /**
     * Begins a scrollable area.
     *
     * @param r             The renderer.
     * @param key           Unique ID for the scroll state.
     * @param x             X position.
     * @param y             Y position.
     * @param width         Width of the view area.
     * @param height        Height of the view area.
     * @param contentHeight Total height of the content inside.
     * @return A {@link ScrollArea} object that must be passed to {@link #endScrollArea(ScrollArea)}.
     */
    public ScrollArea beginScrollArea(UiRenderer r, String key, int x, int y, int width, int height, int contentHeight) {
        int id = id(key);
        ScrollState state = scrollStates.computeIfAbsent(id, k -> new ScrollState());

        boolean hovered = pxInside(mouse.x, mouse.y, x, y, width, height);
        if (hovered && input.scrollY() != 0.0) {
            double wheel = input.consumeScrollY();
            state.scrollY -= (float) (wheel * 28.0);
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

    /**
     * Ends a scrollable area. Restores the previous clipping and layout state.
     *
     * @param area The scroll area object returned by {@link #beginScrollArea}.
     */
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

    /**
     * Draws a float slider.
     *
     * @param r     The renderer.
     * @param label The label text.
     * @param value The current value.
     * @param min   The minimum value.
     * @param max   The maximum value.
     * @return The updated value.
     */
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
        int outline = theme.widgetOutline.getArgb();
        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, rect.x, rect.y, rect.w, rect.h, bg, outline, 1);
        } else {
            r.drawRect(rect.x, rect.y, rect.w, rect.h, bg);
        }

        float t = (value - min) / (max - min);
        t = clamp01(t);
        float fillW = rect.w * t;
        int fill = theme.widgetActive.getArgb();
        float fillRadius = Math.max(0.0f, theme.design.radius_sm - 1);
        if (fillW > 1.0f) {
            r.drawRoundedRect(rect.x + 1, rect.y + 1, Math.max(0.0f, fillW - 2.0f), rect.h - 2, fillRadius, fill);
        }

        float baselineY = r.baselineForBox(rect.y, rect.h);
        r.drawText(label, rect.x + 10, baselineY, theme.text.getArgb());
        return value;
    }

    //todo add doc
    public float sliderFloat(UiRenderer r, SliderComponent component) {
        Rect rect = nextRect(theme.tokens.itemHeight);
        return sliderFloat(r, component, rect);
    }

    private float sliderFloat(UiRenderer r, SliderComponent component, Rect rect) {
        TextComponent labelComp = component.getLabel();
        List<TextComponent> components = new ArrayList<>();
        collectTextComponents(labelComp, components);
        int id = id(component.getId());
        float min = component.getMin();
        float max = component.getMax();
        float value = component.getValue();
        boolean changed = false;

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
            changed = true;
        }
        if (activeId == id && input.mouseReleased()) {
            activeId = 0;
        }

        float hoverT = anim(id).step((hovered || activeId == id) ? 1.0f : 0.0f, dt, theme.tokens.animSpeed);
        int bg = Theme.lerpArgb(component.getBackgroundColor(this), component.getHoverColor(this), hoverT);
        int outline = theme.widgetOutline.getArgb();
        if (theme.skins.widget != null) {
            theme.skins.widget.drawWithOutline(r, rect.x, rect.y, rect.w, rect.h, bg, outline, 1);
        } else {
            r.drawRect(rect.x, rect.y, rect.w, rect.h, bg);
        }

        float t = (value - min) / (max - min);
        t = clamp01(t);
        float fillW = rect.w * t;
        int fill = component.getActiveColor(this).getArgb();
        float fillRadius = Math.max(0.0f, theme.design.radius_sm - 1);
        if (fillW > 1.0f) {
            r.drawRoundedRect(rect.x + 1, rect.y + 1, Math.max(0.0f, fillW - 2.0f), rect.h - 2, fillRadius, fill);
        }

        float baselineY = r.baselineForBox(rect.y, rect.h);
        int x = rect.x + 10;
        drawTextComponents(r, components, x, baselineY);

        if (changed && component.getOnChange() != null) {
            component.getOnChange().accept(value);
        }

        return value;
    }

    /**
     * Draws an image.
     *
     * @param r        The renderer.
     * @param texture  The texture to draw.
     * @param width    Width in pixels.
     * @param height   Height in pixels.
     * @param tintArgb Color tint.
     */
    public void image(UiRenderer r, Texture texture, int width, int height, int tintArgb) {
        Rect rect = nextRect(height);
        int iw = Math.min(width, rect.w);
        int ih = Math.min(height, rect.h);
        r.drawTexturedRect(texture, rect.x, rect.y, iw, ih, tintArgb);
    }

    public void image(UiRenderer r, TextureComponent component) {
        Rect rect = nextRect(component.getHeight());
        image(r, component, rect);
    }

    private void image(UiRenderer r, TextureComponent component, Rect rect) {
        int iw = Math.min(component.getWidth(), rect.w);
        int ih = Math.min(component.getHeight(), rect.h);
        r.drawTexturedRect(component.getTexture(), rect.x, rect.y, iw, ih, component.getTintArgb());
    }

    public void label(UiRenderer r, String text, boolean muted) {
        Rect rect = nextRect(theme.tokens.itemHeight);
        int color = muted ? theme.textMuted.getArgb() : theme.text.getArgb();
        float baselineY = r.baselineForBox(rect.y, rect.h);
        r.drawText(text, rect.x, baselineY, color);
    }

    //todo add doc

    public void label(UiRenderer r, TextComponent text) {
        List<TextComponent> components = new ArrayList<>();
        collectTextComponents(text, components);
        Rect rect = nextRect(theme.tokens.itemHeight);
        float baselineY = r.baselineForBox(rect.y, rect.h);
        int x = rect.x;
        drawTextComponents(r, components, x, baselineY);
    }

    private void label(UiRenderer r, TextComponent text, Rect rect) {
        List<TextComponent> components = new ArrayList<>();
        collectTextComponents(text, components);
        float baselineY = r.baselineForBox(rect.y, rect.h);
        int x = rect.x;
        drawTextComponents(r, components, x, baselineY);
    }

    //todo add doc
    private void collectTextComponents(TextComponent text, List<TextComponent> components) {
        components.add(text);
        for (Component child : text.getChildren()) {
            if (child instanceof TextComponent textComponent) {
                collectTextComponents(textComponent, components);
            }
        }
    }

    private int drawTextComponents(UiRenderer r, List<TextComponent> components, int x, float y) {
        int currentX = x;
        for (TextComponent comp : components) {
            int color = comp.getThemeId() != null ? theme.getColor(comp.getThemeId()).getArgb() : comp.getColor(this).getArgb();
            r.drawText(comp.getText(), currentX, y, color);
            currentX += (int) (r.measureText(comp.getText()) + 4);
        }
        return currentX;
    }

    //todo add doc
    public void renderComponent(UiRenderer r, Component component) {
        component.render(this, r);
    }

    public void grid(UiRenderer r, GridComponent component) {

        pushId(component.getId());

        Component[][] cells = component.getGrid();
        if (cells == null || cells.length == 0) {
            popId();
            return;
        }

        final int rows = cells.length;

        int cols = 0;
        for (Component[] row : cells) {
            if (row != null) cols = Math.max(cols, row.length);
        }
        if (cols <= 0) {
            popId();
            return;
        }

        // "CSS gap" (use theme spacing for now)
        final int gap = Math.max(0, theme.tokens.itemSpacing);

        // "row height" default (until GridComponent exposes something like getRowHeight())
        final int rowH = Math.max(1, component.getCellHeight());

        // Grid origin (current cursor position, like flow layout)
        final int startX = cursorX;
        final int startY = cursorY;

        // Column width distribution (equal columns for now)
        final int totalGapW = gap * (cols - 1);
        final int colW = Math.max(1, (contentW - totalGapW) / cols);

        // Precompute cell rects
        Rect[][] rects = new Rect[rows][cols];
        for (int rr = 0; rr < rows; rr++) {
            for (int cc = 0; cc < cols; cc++) {
                int x = startX + cc * (colW + gap);
                int y = startY + rr * (rowH + gap);
                rects[rr][cc] = rect(x, y, colW, rowH);
            }
        }

        // Collect spans by component instance (same reference)
        Map<Component, int[]> bounds = new IdentityHashMap<>();
        for (int rr = 0; rr < rows; rr++) {
            Component[] row = cells[rr];
            for (int cc = 0; cc < cols; cc++) {
                Component c = (row != null && cc < row.length) ? row[cc] : null;
                if (c == null) continue;
                int[] b = bounds.get(c);
                if (b == null) {
                    // minR, minC, maxR, maxC
                    b = new int[]{rr, cc, rr, cc};
                    bounds.put(c, b);
                } else {
                    b[0] = Math.min(b[0], rr);
                    b[1] = Math.min(b[1], cc);
                    b[2] = Math.max(b[2], rr);
                    b[3] = Math.max(b[3], cc);
                }
            }
        }

        // Validate "rectangular span" for each component; otherwise split into per-cell draws.
        Set<Component> nonRectangular = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Map.Entry<Component, int[]> e : bounds.entrySet()) {
            Component c = e.getKey();
            int[] b = e.getValue();
            int minR = b[0], minC = b[1], maxR = b[2], maxC = b[3];

            boolean ok = true;
            for (int rr = minR; rr <= maxR && ok; rr++) {
                Component[] row = cells[rr];
                for (int cc = minC; cc <= maxC; cc++) {
                    Component in = (row != null && cc < row.length) ? row[cc] : null;
                    if (in != c) {
                        ok = false;
                        break;
                    }
                }
            }
            if (!ok) nonRectangular.add(c);
        }

        boolean[][] consumed = new boolean[rows][cols];

        // Render grid
        for (int rr = 0; rr < rows; rr++) {
            Component[] row = cells[rr];

            for (int cc = 0; cc < cols; cc++) {
                if (consumed[rr][cc]) continue;

                Component c = (row != null && cc < row.length) ? row[cc] : null;
                if (c == null) continue;


                // If component span is non-rectangular, render per cell
                if (nonRectangular.contains(c)) {
                    Rect cellR = rects[rr][cc];
                    consumed[rr][cc] = true;

                    pushId(rr * 73856093 ^ cc * 19349663);
                    renderComponent(r, c, cellR);
                    popId();
                    continue;
                }

                // Rectangular span
                int[] b = bounds.get(c);
                int minR = b[0], minC = b[1], maxR = b[2], maxC = b[3];

                // Only draw at top-left of the span
                if (rr != minR || cc != minC) {
                    consumed[rr][cc] = true;
                    continue;
                }

                // Mark consumed area
                for (int rrr = minR; rrr <= maxR; rrr++) {
                    for (int ccc = minC; ccc <= maxC; ccc++) {
                        if (rrr >= 0 && rrr < rows && ccc >= 0 && ccc < cols) {
                            consumed[rrr][ccc] = true;
                        }
                    }
                }

                Rect a = rects[minR][minC];
                Rect z = rects[maxR][maxC];

                int spanX = a.x;
                int spanY = a.y;
                int spanW = (z.x + z.w) - a.x;
                int spanH = (z.y + z.h) - a.y;

                Rect spanRect = rect(spanX, spanY, spanW, spanH);

                pushId(minR * 73856093 ^ minC * 19349663);
                renderComponent(r, c, spanRect);
                popId();
            }
        }

        // Advance cursor like a block layout: total grid height + itemSpacing
        int gridH = rows * rowH + gap * (rows - 1);
        cursorY = startY + gridH + theme.tokens.itemSpacing;

        popId();
    }

    private void renderComponent(UiRenderer r, Component c, Rect rect) {
        if (c instanceof ButtonComponent bc) {
            button(r, bc, rect);
        } else if (c instanceof TextComponent text) {
            label(r, text, rect);
        } else if (c instanceof TextureComponent tc) {
            image(r, tc, rect);
        } else if (c instanceof SliderComponent sc) {
            sliderFloat(r, sc, rect);
        } else if (c instanceof ToggleComponent tc) {
            toggle(r, tc, rect);
        } else {
            // Placeholder for now (Toggle, Slider, Label, custom...)
            drawPlaceHolder(rect, c, r);
        }
    }

    private void drawPlaceHolder(Rect rc, Component c, UiRenderer r) {
        int bg = theme.widgetBg.getArgb();
        int outline = theme.widgetOutline.getArgb();
        r.drawRect(rc.x, rc.y, rc.w, rc.h, bg);
        // tiny outline effect
        r.drawRect(rc.x, rc.y, rc.w, 1, outline);
        r.drawRect(rc.x, rc.y + rc.h - 1, rc.w, 1, outline);
        r.drawRect(rc.x, rc.y, 1, rc.h, outline);
        r.drawRect(rc.x + rc.w - 1, rc.y, 1, rc.h, outline);

        String name = (c == null) ? "empty" : c.getClass().getSimpleName();
        float by = r.baselineForBox(rc.y, rc.h);
        r.drawText(name, rc.x + 6, by, theme.textMuted.getArgb());
    }

    public void spacer(int pixels) {
        cursorY += Math.max(0, pixels);
    }

    public void separator(UiRenderer r) {
        separator(r, theme.widgetOutline, 1);
    }

    public void seperator(UiRenderer r, Color color) {
        separator(r, color, 1);
    }

    public void separator(UiRenderer r, Color color, int thickness) {
        spacer(thickness / 2);
        r.drawRect(contentX, cursorY, contentW, thickness, color.getArgb());
        spacer(thickness / 2);
    }

    private static void drawBevelButton(UiRenderer r,
                                        float x,
                                        float y,
                                        float w,
                                        float h,
                                        float radius,
                                        int borderPx,
                                        int bg,
                                        int outline) {
        int t = Math.max(1, borderPx);
        if (w <= t * 2.0f || h <= t * 2.0f) {
            r.drawRect(x, y, w, h, bg);
            return;
        }

        int top = Theme.lightenArgb(bg, 0.06f);
        int bottom = Theme.darkenArgb(bg, 0.06f);
        r.drawRoundedRect(x, y, w, h, radius, top, top, bottom, bottom, t, outline);

        int hl = Theme.lightenArgb(bg, 0.10f);
        int hlA = (hl >>> 24) & 0xFF;
        hl = ((Math.min(255, (int) (hlA * 0.35f))) << 24) | (hl & 0x00FFFFFF);
        r.drawRoundedRect(x + t, y + t, w - t * 2.0f, 1.0f, Math.max(0.0f, radius - t - 1.0f), hl);
    }

    // (no outline helper; nine-slice outline uses NineSlice.drawWithOutline)

    private Rect nextRect(int height) {
        int h = Math.max(1, height);
        Rect r = new Rect(cursorX, cursorY, contentW, h);
        cursorY += h + theme.tokens.itemSpacing;
        return r;
    }

    private Rect rect(int x, int y, int w, int h) {
        return new Rect(x, y, w, h);
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

    private record LayoutState(int contentX, int contentY, int contentW, int cursorX, int cursorY) {
    }

    private static final class ScrollState {
        private float scrollY;
    }

    public record ScrollArea(UiRenderer renderer, int id, int x, int y, int width, int height, int maxScroll,
                             float scrollY) {
    }
}
