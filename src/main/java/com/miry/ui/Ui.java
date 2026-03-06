package com.miry.ui;

import com.miry.graphics.Texture;
import com.miry.ui.component.*;
import com.miry.ui.component.graphic.GraphicComponent;
import com.miry.ui.component.graphic.GraphicDataSeries;
import com.miry.ui.component.graphic.GraphicUtils;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;
import com.miry.ui.widgets.ComboBox;
import com.miry.ui.widgets.TextField;
import org.joml.Vector2f;

import java.util.*;
import java.util.function.BiConsumer;

import static com.miry.ui.component.graphic.GraphicUtils.*;
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
    private int contentH;
    private UiContext uiContext;

    private final Map<Integer, Anim> anims = new HashMap<>();
    private final Map<Integer, ScrollState> scrollStates = new HashMap<>();

    private int idSeed = 0x1234567;
    private final Deque<Integer> idSeedStack = new ArrayDeque<>();
    private final Deque<LayoutState> layoutStack = new ArrayDeque<>();
    private final GraphicUtils graphicUtils;

    /**
     * Creates a new UI context with the specified theme.
     *
     * @param theme The theme to use for styling widgets.
     */
    public Ui(Theme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.graphicUtils = new GraphicUtils(theme);
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
        contentH = Math.max(1, height - theme.tokens.padding * 2);
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

        if (changed) {
            component.toggle();
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

        if (changed) {
            component.slider(value);
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

    /**
     * Renders a {@link ScrollAreaComponent} in "flow layout" (vertical stacking) and updates its content height.
     *
     * <p>Features added:</p>
     * <ul>
     *   <li><b>Y offset support</b>: you can shift the scroll area down inside its allocated region
     *       (useful if you rendered non-component elements before it).</li>
     *   <li><b>Rect-less usage</b>: you can call {@link #scrollArea(UiRenderer, ScrollAreaComponent)} and the UI
     *       will allocate the region automatically using {@code nextRect(height)}.</li>
     * </ul>
     *
     * <p><b>How content height is handled</b>:</p>
     * <ul>
     *   <li>The component stores a cached {@code contentHeight}.</li>
     *   <li>Each frame, we render children inside the scroll area (flow).</li>
     *   <li>After rendering, we compute the actually used content height and store it back into the component.
     *       This makes scrolling correct starting from the next frame (which is perfectly fine in IMUI).</li>
     * </ul>
     */
    public void scrollArea(UiRenderer r, ScrollAreaComponent sc, Rect rect) {
        pushId(sc.getId() == null ? "scroll" : sc.getId());

        // Optional vertical offset inside the provided rect
        final int yOffset = Math.max(0, sc.getYOffset());
        final int viewX = rect.x;
        final int viewY = rect.y + yOffset;
        final int viewW = rect.w;
        final int viewH = Math.max(1, rect.h - yOffset);

        final String key = (sc.getKey() != null ? sc.getKey() : sc.getId());
        final int cachedContentH = Math.max(viewH, sc.getContentHeight());

        Ui.ScrollArea area = beginScrollArea(r, key, viewX, viewY, viewW, viewH, cachedContentH);
        UiRenderer sr = area.renderer();

        // Render children in flow layout inside the scroll area
        for (Component child : sc.getChildren()) {
            renderComponentFlow(sr, child);
        }

        // Update contentHeight based on what was actually consumed this frame
        int usedContentH = Math.max(viewH, (cursorY - contentY));
        sc.setContentHeight(usedContentH);

        endScrollArea(area);

        popId();
    }

    /**
     * Renders a {@link ScrollAreaComponent} without providing a {@link Rect}.
     *
     * <p>This is the "immediate-mode" style call: the scroll area takes a vertical slot from the current cursor
     * position using {@code nextRect(sc.getHeight())}.</p>
     *
     * <p>Useful when you just want:</p>
     * <pre>
     *   ui.scrollArea(r, myScroll);
     * </pre>
     *
     * <p>Requirements:</p>
     * <ul>
     *   <li>{@link ScrollAreaComponent#getHeight()} must be &gt; 0</li>
     *   <li>If you want it to occupy remaining panel space, you can compute that height externally
     *       and set it on the component.</li>
     * </ul>
     */
    public void scrollArea(UiRenderer r, ScrollAreaComponent sc) {
        int h = sc.getHeight();

        if (h <= 0) {
            h = Math.max(1, contentH - cursorY);
        }

        Rect rect = nextRect(h);
        scrollArea(r, sc, rect);
    }

    private void renderComponentFlow(UiRenderer r, Component c) {
        if (c == null) return;

        if (c instanceof ButtonComponent bc) {
            button(r, bc); // version flow (nextRect)
        } else if (c instanceof TextComponent tc) {
            label(r, tc);  // version flow (nextRect)
        } else if (c instanceof TextureComponent tex) {
            // à toi d'adapter selon ton TextureComponent (width/height)
            image(r, tex.getTexture(), tex.getWidth(), tex.getHeight(), tex.getTintArgb()); // version flow (nextRect)
        } else if (c instanceof SliderComponent sc) {
            sliderFloat(r, sc); // version flow (nextRect)
        } else if (c instanceof ToggleComponent tc) {
            toggle(r, tc); // version flow (nextRect)
        } else if (c instanceof GridComponent gc) {
            grid(r, gc);   // grid gère cursorY elle-même
        } else if (c instanceof GroupedComponent gc) {
            group(r, gc);  // group gère cursorY elle-même
        } else if (c instanceof ScrollAreaComponent sac) {
            Rect rr = nextRect(theme.tokens.itemHeight * 6);
            scrollArea(r, sac, rr);
        } else if (c instanceof GraphicComponent graph) {
            Rect rr = nextRect(graph.getHeight());
            graphic(r, graph, rr);
        } else if (c instanceof TextFieldComponent tf) {
            textField(r, tf);
        } else if (c instanceof ComboBoxComponent<?> cb) {
            comboBox(r, cb);
        } else {
            Rect rr = nextRect(theme.tokens.itemHeight);
            drawPlaceHolder(rr, c, r);
        }
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

    // Update your renderComponent switch to support GraphicComponent.
// Keep comments in English.

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
        } else if (c instanceof TextFieldComponent tfc) {
            textField(r, tfc, rect);
        }else if (c instanceof ComboBoxComponent<?> cbc) {
            comboBox(r, (ComboBoxComponent<Object>) cbc, rect);
        } else if (c instanceof ScrollAreaComponent sc) {
            scrollArea(r, sc, rect);
        } else if (c instanceof GraphicComponent gc) {
            // Render the graphic inside the given rect without breaking external layout.
            layoutStack.push(new LayoutState(contentX, contentY, contentW, cursorX, cursorY));

            contentX = rect.x;
            contentY = rect.y;
            contentW = rect.w;
            cursorX = rect.x;
            cursorY = rect.y;

            graphic(r, gc, rect);

            LayoutState prev = layoutStack.pop();
            contentX = prev.contentX();
            contentY = prev.contentY();
            contentW = prev.contentW();
            cursorX = prev.cursorX();
            cursorY = prev.cursorY();
        } else if (c instanceof GridComponent gc) {
            grid(r, gc);
        } else if (c instanceof SpacerComponent sp) {
            spacer(sp);
        } else if (c instanceof SeparatorComponent sep) {
            separator(r, sep);
        } else {
            drawPlaceHolder(rect, c, r);
        }
    }

    public void group(UiRenderer r, GroupedComponent component) {
        List<Component> children = component.getChildren();
        if (children == null || children.isEmpty()) return;

        pushId(component.getId() == null ? "group" : component.getId());

        final int gap = Math.max(0, theme.tokens.itemSpacing);

        final int startX = cursorX;
        final int startY = cursorY;

        final int lineH = Math.max(1, theme.tokens.itemHeight);

        final int n = children.size();

        int[] desiredW = new int[n];
        int fixedSum = 0;
        int flexCount = 0;

        for (int i = 0; i < n; i++) {
            Component c = children.get(i);

            int w = -1;

            if (c instanceof TextComponent tc) {
                List<TextComponent> parts = new ArrayList<>();
                collectTextComponents(tc, parts);

                float width = 0f;
                for (TextComponent part : parts) {
                    width += r.measureText(part.getText()) + 4f;
                }
                w = Math.max(1, Math.round(width) + 2);
            } else if (c instanceof TextureComponent) {
                w = lineH;
            } else {
                w = -1;
            }

            desiredW[i] = w;

            if (w > 0) fixedSum += w;
            else flexCount++;
        }

        int totalGap = gap * Math.max(0, n - 1);
        int available = Math.max(1, contentW - totalGap);

        if (fixedSum > available && fixedSum > 0) {
            float s = (float) available / (float) fixedSum;
            fixedSum = 0;
            for (int i = 0; i < n; i++) {
                if (desiredW[i] > 0) {
                    desiredW[i] = Math.max(1, Math.round(desiredW[i] * s));
                    fixedSum += desiredW[i];
                }
            }
        }

        int remaining = Math.max(0, available - fixedSum);
        int flexW = (flexCount > 0) ? (remaining / flexCount) : 0;
        int flexR = (flexCount > 0) ? (remaining % flexCount) : 0;

        int x = startX;
        for (int i = 0; i < n; i++) {
            Component c = children.get(i);

            int w = desiredW[i];
            if (w <= 0) {
                w = flexW + (flexR > 0 ? 1 : 0);
                if (flexR > 0) flexR--;
                w = Math.max(1, w);
            }

            Rect rc = rect(x, startY, w, lineH);

            pushId(i);
            renderComponent(r, c, rc);
            popId();

            x += w + gap;
        }

        cursorY = startY + lineH + theme.tokens.itemSpacing;

        popId();
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

    public void graphic(UiRenderer r, GraphicComponent component) {
        // Render as a fixed-size widget that does NOT break the layout flow.
        // Width is clamped to the available content width, height is respected.
        pushId(component.getId());

        int w = Math.max(1, Math.min(component.getWidth(), contentW));
        int h = Math.max(1, component.getHeight());

        Rect rect = rect(cursorX, cursorY, w, h);
        cursorY += h + theme.tokens.itemSpacing;

        graphic(r, component, rect);

        popId();
    }

    private void graphic(UiRenderer r, GraphicComponent component, Rect rect) {
        // High-level layout: Title (top), Plot (center), Legend (right or bottom), Axis labels.
        // We keep everything inside rect and use clipping to avoid overflowing.

        final int bg = theme.widgetBg.getArgb();
        final int outline = theme.widgetOutline.getArgb();
        final int text = theme.text.getArgb();
        final int textMuted = theme.textMuted.getArgb();

        r.flush();
        r.pushClip(rect.x, rect.y, rect.w, rect.h);

        // Background and border
        r.drawRoundedRect(rect.x, rect.y, rect.w, rect.h, theme.design.radius_sm, bg, theme.design.border_thin, outline);

        // Padding inside the chart widget
        final int pad = Math.max(6, theme.tokens.padding);
        int x0 = rect.x + pad;
        int y0 = rect.y + pad;
        int x1 = rect.x + rect.w - pad;
        int y1 = rect.y + rect.h - pad;

        // Title area
        final int titleH = (!isNullOrBlank(component.getTitle())) ? (int) Math.ceil(r.lineHeight() + 6) : 0;
        if (titleH > 0) {
            String title = component.getTitle();
            float tw = r.measureText(title);
            float tx = x0 + Math.max(0, (x1 - x0 - (int) tw) / 2.0f);
            float ty = r.baselineForBox(y0, titleH);
            r.drawText(title, tx, ty, text);
            y0 += titleH;
        }

        // Decide legend placement: right if enough width, otherwise bottom
        final boolean wantLegend = component.isShowLegend();
        final int legendMinW = 110;
        final int legendMaxW = 180;
        final int legendGap = 8;

        int legendW = 0;
        int legendH = 0;
        boolean legendRight = false;

        List<GraphicDataSeries> points = component.getDataSeries();
        int seriesCount = (points == null) ? 0 : points.size();

        if (wantLegend && seriesCount > 0) {
            int availableW = x1 - x0;
            // Prefer right legend when there's enough horizontal space
            if (availableW >= (legendMinW + 220)) {
                legendRight = true;
                legendW = clampInt(legendMaxW, legendMinW, Math.max(legendMinW, availableW / 3));
                legendH = y1 - y0;
                x1 -= (legendW + legendGap);
            } else {
                legendRight = false;
                legendH = (int) Math.ceil(r.lineHeight() * Math.min(seriesCount, 4) + 14);
                legendW = x1 - x0;
                y1 -= (legendH + legendGap);
            }
        }

        // Axis label areas
        final int axisLabelH = (!isNullOrBlank(component.getAxisXLabel())) ? (int) Math.ceil(r.lineHeight() + 4) : 0;
        final int axisLabelW = (!isNullOrBlank(component.getAxisYLabel())) ? (int) Math.ceil(r.lineHeight() + 4) : 0;

        // Plot area (reserve space for axis labels and tick labels)
        final int tickLabelPad = 6;
        final int yTicksW = axisLabelW + 26; // left side for Y values + Y label
        final int xTicksH = axisLabelH + 22; // bottom for X values + X label

        int plotX0 = x0 + yTicksW;
        int plotY0 = y0 + 6;
        int plotX1 = x1 - 6;
        int plotY1 = y1 - xTicksH;

        // Safety clamp
        if (plotX1 <= plotX0 + 10 || plotY1 <= plotY0 + 10) {
            // Not enough space: just draw placeholder message
            String msg = "Graphic: not enough space";
            r.drawText(msg, x0, r.baselineForBox(y0, Math.max(1, y1 - y0)), textMuted);
            r.popClip();
            return;
        }

        // Compute scaling based on explicit min/max, or auto-scale from points with padding
        GraphicUtils.Scale scale = graphicUtils.computeScale(component, points, plotX0, plotY0, plotX1, plotY1);

        // Draw grid (optional) and axes (always)
        graphicUtils.drawAxesAndGrid(r, component, plotX0, plotY0, plotX1, plotY1, scale);

        // Draw chart by type
        switch (component.getType()) {
            case LINE -> graphicUtils.drawLineChart(r, component, plotX0, plotY0, plotX1, plotY1, scale, false);
            case AREA -> graphicUtils.drawLineChart(r, component, plotX0, plotY0, plotX1, plotY1, scale, true);
            case COLUMN -> graphicUtils.drawColumnChart(r, component, plotX0, plotY0, plotX1, plotY1, scale);
            case BAR -> graphicUtils.drawBarChart(r, component, plotX0, plotY0, plotX1, plotY1, scale);
            case PIE -> graphicUtils.drawPieChart(r, component, plotX0, plotY0, plotX1, plotY1);
            case SPIDER_WEB -> graphicUtils.drawRadarChart(r, component, plotX0, plotY0, plotX1, plotY1);
            case CLOUD -> graphicUtils.drawCloudChart(r, component, plotX0, plotY0, plotX1, plotY1, scale);
            default -> {
                String msg = "GraphicType not implemented: " + component.getType();
                r.drawText(msg, plotX0, r.baselineForBox(plotY0, plotY1 - plotY0), textMuted);
            }
        }

        // Draw axis labels
        graphicUtils.drawAxisLabels(r, component, x0, y0, x1, y1, plotX0, plotY0, plotX1, plotY1);

        // Draw legend
        if (wantLegend && seriesCount > 0 && (legendW > 0 && legendH > 0)) {
            if (legendRight) {
                graphicUtils.drawLegendRight(r, component, points, x1 + legendGap, y0, legendW, legendH);
            } else {
                graphicUtils.drawLegendBottom(r, component, points, x0, y1 + legendGap, legendW, legendH);
            }
        }

        r.popClip();
    }

    public void spacer(int pixels) {
        cursorY += Math.max(0, pixels);
    }

    public void spacer(SpacerComponent component){
        spacer(component.getSpacer());
    }

    public void separator(UiRenderer r) {
        separator(r, theme.widgetOutline, 1);
    }

    public void seperator(UiRenderer r, Color color) {
        separator(r, color, 1);
    }

    public void separator(UiRenderer r, SeparatorComponent component){
        separator(r, component.getColor(this), component.getThickness());
    }

    public void separator(UiRenderer r, Color color, int thickness) {
        spacer(thickness / 2);
        r.drawRect(contentX, cursorY, contentW, thickness, color.getArgb());
        spacer(thickness / 2);
    }

    public void textField(UiRenderer r, TextFieldComponent component) {
        // Default layout: use one row height (like other widgets)
        Rect rect = nextRect(theme.tokens.itemHeight);
        textField(r, component, rect);
    }

    private void textField(UiRenderer r, TextFieldComponent component, Rect rect) {
        if (r == null || component == null) return;

        TextField tf = component.textField();

        // Compute hover/press state for visuals (TextField will also do it, but it needs input+ctx anyway)
        boolean hovered = rect.contains(mouse.x, mouse.y);

        // Use full widget render to restore focus on click
        UiContext ctx = this.uiContext;

        // If ctx is missing, we fall back to raw rendering (no focus possible)
        if (ctx == null) {
            int id = id(component.getId());
            float hoverT = anim(id).step(hovered ? 1.0f : 0.0f, dt, theme.tokens.animSpeed);

            Color bg = component.getBgColor(this);
            Color hoverBg = component.getHoverColor(this);
            int fg = component.getTextColor(this).getArgb();
            int caret = theme.widgetActive.getArgb();

            int bgFinal = Theme.lerpArgb(bg, hoverBg, hoverT);
            tf.render(r, rect.x, rect.y, rect.w, rect.h, bgFinal, fg, caret);
            r.drawRectOutline(rect.x, rect.y, rect.w, rect.h, Math.max(1, theme.design.border_thin), theme.widgetOutline.getArgb());
            return;
        }

        // Temporarily patch theme colors so TextField uses your component colors.
        // This keeps the original TextField behavior (focus, selection, caret blinking, etc.)
        // while letting your component control colors.
        Color prevWidgetBg = new Color(theme.widgetBg.getArgb());
        Color prevWidgetHover = new Color(theme.widgetHover.getArgb());
        Color prevText = new Color(theme.text.getArgb());


        try {
            // Theme expects colors; your component returns Color with ARGB.
            // If theme.widgetBg is your own Color class, adapt accordingly.
            theme.widgetBg.set(component.getBgColor(this));
            theme.widgetHover.set(component.getHoverColor(this));
            theme.text.set(component.getTextColor(this));

            // Render with full interaction (focus + pressed + hover)
            tf.render(
                    r,
                    ctx,
                    input(),
                    theme,
                    rect.x,
                    rect.y,
                    rect.w,
                    rect.h,
                    true
            );
        } finally {
            // Restore theme state to avoid affecting other widgets
            theme.widgetBg.set(prevWidgetBg);
            theme.widgetHover.set(prevWidgetHover);
            theme.text.set(prevText);
        }
    }

    public <T> boolean comboBox(UiRenderer r, ComboBoxComponent<T> component) {
        // Default layout: use one row height (like other widgets)
        Rect rect = nextRect(theme.tokens.itemHeight);
        return comboBox(r, component, rect);
    }

    private <T> boolean comboBox(UiRenderer r, ComboBoxComponent<T> component, Rect rect) {
        if (r == null || component == null) return false;

        ComboBox<T> cb = component.comboBox();
        UiContext ctx = this.uiContext;

        // Resolve component colors once (component already handles theme fallback)
        Color bgColor = component.getBgColor(this);
        Color hoverColor = component.getHoverColor(this);
        Color textColor = component.getTextColor(this);

        // If no context, we can still render a basic button and popup, but focus/overlay is limited.
        // We prefer not to break: render button + immediate popup (no overlay queue).
        if (ctx == null) {
            int id = id(component.getId());
            boolean hovered = rect.contains(mouse.x, mouse.y);

            // IMUI press/active handling for the button
            boolean clicked = false;
            if (hovered) hotId = id;
            if (hovered && input.mousePressed()) activeId = id;
            if (activeId == id && hovered && input.mouseReleased()) {
                clicked = true;
                activeId = 0;
            }
            if (activeId == id && input.mouseReleased() && !hovered) activeId = 0;

            float hoverT = anim(id).step(hovered ? 1.0f : 0.0f, dt, theme.tokens.animSpeed);
            int bgFinal = Theme.lerpArgb(bgColor, hoverColor, hoverT);

            // Toggle popup on click
            if (clicked) cb.setOpen(!cb.isOpen());

            // Render button
            cb.renderButton(r, rect.x, rect.y, rect.w, rect.h, bgFinal, textColor.getArgb());
            r.drawRectOutline(rect.x, rect.y, rect.w, rect.h, Math.max(1, theme.design.border_thin), theme.widgetOutline.getArgb());

            // Render popup directly (no overlay pass)
            boolean changed = false;
            if (cb.isOpen()) {
                int popupX = rect.x;
                int popupY = rect.y + rect.h;

                int popupMaxH = Math.max(theme.tokens.itemHeight * 6, rect.h * 6);
                int itemH = theme.tokens.itemHeight;

                int hoverIndex = cb.hoverIndex((int) mouse.x, (int) mouse.y, popupX, popupY, rect.w, popupMaxH, itemH);

                if (input.mousePressed()) {
                    int before = cb.selectedIndex();
                    boolean consumed = cb.handlePopupClick((int) mouse.x, (int) mouse.y, popupX, popupY, rect.w, popupMaxH, itemH);
                    changed = before != cb.selectedIndex();
                    if (!consumed && !hovered) cb.setOpen(false);
                }

                int popupBg = theme.panelBg.getArgb();
                int popupHover = hoverColor.getArgb();
                cb.renderPopup(r, popupX, popupY, rect.w, popupMaxH, itemH, popupBg, popupHover, textColor.getArgb(), hoverIndex);
            }

            return changed;
        }

        // Context available: use the full ComboBox.render(...) so focus + overlay works properly.
        // We temporarily patch theme colors so the widget uses component colors.
        // This matches the TextField approach (save -> set -> render -> restore).

        Color prevWidgetBg = new Color(theme.widgetBg.getArgb());
        Color prevWidgetHover = new Color(theme.widgetHover.getArgb());
        Color prevText = new Color(theme.text.getArgb());
        Color prevPanelBg = new Color(theme.panelBg.getArgb());

        try {
            // Apply component colors
            theme.widgetBg.set(bgColor);
            theme.widgetHover.set(hoverColor);
            theme.text.set(textColor);

            // Popup background usually uses panelBg; keep theme panel bg, or you can override if desired.
            // Here we keep it stable (but we still restore because we captured it).
            // theme.panelBg.set(...);

            int popupMaxH = Math.max(theme.tokens.itemHeight * 8, rect.h * 8);
            int itemH = theme.tokens.itemHeight;

            // Defer popup so it renders above other UI automatically via ctx.overlay()
            boolean deferPopup = true;

            return cb.render(
                    r,
                    ctx,
                    input(),
                    theme,
                    rect.x,
                    rect.y,
                    rect.w,
                    rect.h,
                    popupMaxH,
                    itemH,
                    true,
                    deferPopup
            );
        } finally {
            // Restore theme colors to avoid affecting other widgets
            theme.widgetBg.set(prevWidgetBg);
            theme.widgetHover.set(prevWidgetHover);
            theme.text.set(prevText);
            theme.panelBg.set(prevPanelBg);
        }
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

    public void setContext(UiContext uiContext) {
        this.uiContext = uiContext;
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
