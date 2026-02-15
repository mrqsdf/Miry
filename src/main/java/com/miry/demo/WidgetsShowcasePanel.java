package com.miry.demo;

import com.miry.platform.MiryContext;
import com.miry.ui.PanelContext;
import com.miry.ui.Ui;
import com.miry.ui.UiContext;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import com.miry.ui.input.UiInput;
import com.miry.ui.panels.Panel;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Icon;
import com.miry.ui.theme.Theme;
import com.miry.ui.util.NumericExpression;
import com.miry.ui.widgets.CollapsibleHeader;
import com.miry.ui.widgets.ColorPicker;
import com.miry.ui.widgets.ComboBox;
import com.miry.ui.widgets.ContextMenu;
import com.miry.ui.widgets.DragNumber;
import com.miry.ui.widgets.DraggableNumberField;
import com.miry.ui.widgets.EyedropperButton;
import com.miry.ui.widgets.Modal;
import com.miry.ui.widgets.ScrollView;
import com.miry.ui.widgets.SegmentedControl;
import com.miry.ui.widgets.SearchableComboBox;
import com.miry.ui.widgets.TextField;
import com.miry.ui.widgets.ThumbnailPreview;
import com.miry.ui.widgets.ToastManager;
import com.miry.ui.widgets.Tooltip;
import com.miry.ui.widgets.TreeNode;
import com.miry.ui.widgets.TreeView;
import com.miry.ui.widgets.VectorInputField;
import com.miry.ui.widgets.VerticalIconTabs;
import com.miry.ui.widgets.Viewport3D;

/**
 * Panel that showcases all widgets in {@code com.miry.ui.widgets}.
 */
public final class WidgetsShowcasePanel extends Panel {
    private final ToastManager toasts;
    private final EyedropperButton eyedropper;
    private final Viewport3D viewport3d;

    private final ScrollView scroll = new ScrollView(1, 1);
    private final Tooltip tooltip = new Tooltip();

    private final TextField plain = new TextField("Edit me");
    private final TextField numeric = new TextField("50/2 + 10");
    private final DraggableNumberField draggableNumber = new DraggableNumberField(50.0f, 0.0f, 100.0f);
    private final DragNumber legacyDragNumber = new DragNumber(25.0f, 0.0f, 100.0f);
    private final VectorInputField vec3 = new VectorInputField("Vector3", 3, -10.0f, 10.0f, 0.0f);

    private final SegmentedControl segmented = new SegmentedControl();
    private int segmentedIndex;

    private final ComboBox<String> combo = new ComboBox<>();
    private final SearchableComboBox<String> searchable = new SearchableComboBox<>();

    private final VerticalIconTabs iconTabs = new VerticalIconTabs();
    private final CollapsibleHeader accordion = new CollapsibleHeader();
    private final ThumbnailPreview thumbnail = new ThumbnailPreview();
    private final ColorPicker colorPicker = new ColorPicker(0xFF4772B3);

    private final ScrollView treeScroll = new ScrollView(1, 1);
    private final TreeView<String> tree;

    private final ContextMenu contextMenu = new ContextMenu();
    private final Modal modal = new Modal(Modal.Type.CONFIRM, "Confirm", "Close the modal or press Esc.");

    public WidgetsShowcasePanel(ToastManager toasts, EyedropperButton eyedropper, Viewport3D viewport3d) {
        super("Widgets");
        this.toasts = toasts;
        this.eyedropper = eyedropper;
        this.viewport3d = viewport3d;

        numeric.setNumericMode(true, NumericExpression.UnitKind.NONE, 3);

        draggableNumber.setNumericFormat(NumericExpression.UnitKind.NONE, 2);
        draggableNumber.setDragSpeed(0.12f);
        draggableNumber.setSnapStep(1.0f);

        legacyDragNumber.setDragSpeed(0.12f);
        legacyDragNumber.setSnapStep(1.0f);

        combo.addItem("Option A");
        combo.addItem("Option B");
        combo.addItem("Option C");
        combo.addItem("Very Long Option Name…");
        combo.setSelectedIndex(0);

        searchable.addItem("Add Modifier…");
        searchable.addItem("Bevel");
        searchable.addItem("Boolean");
        searchable.addItem("Array");
        searchable.addItem("Mirror");
        searchable.addItem("Subdivision Surface");
        searchable.setSelectedIndex(0);

        iconTabs.addTab(Icon.CHECK, "Object");
        iconTabs.addTab(Icon.CHEVRON_DOWN, "Modifiers");
        iconTabs.addTab(Icon.CHEVRON_RIGHT, "Material");

        contextMenu.addItem("Toast: Saved", () -> {
            if (this.toasts != null) this.toasts.show("Saved File");
        });
        contextMenu.addItem("Toast: Copied", () -> {
            if (this.toasts != null) this.toasts.show("Copied!");
        });
        contextMenu.addSeparator();
        contextMenu.addItem("Open Modal", modal::open);

        modal.addButton("Cancel", modal::close);
        modal.addButton("OK", () -> {
            if (this.toasts != null) this.toasts.show("OK");
        });
        modal.setDefaultButtonIndex(1);

        TreeNode<String> root = new TreeNode<>("Scene");
        root.setExpanded(true);
        TreeNode<String> world = new TreeNode<>("World");
        world.addChild(new TreeNode<>("DirectionalLight3D"));
        world.addChild(new TreeNode<>("Camera3D"));
        world.addChild(new TreeNode<>("Player"));
        world.setExpanded(true);
        TreeNode<String> ui = new TreeNode<>("UI");
        ui.addChild(new TreeNode<>("CanvasLayer"));
        ui.addChild(new TreeNode<>("ColorRect"));
        ui.setExpanded(true);
        root.addChild(world);
        root.addChild(ui);
        root.addChild(new TreeNode<>("Misc"));
        tree = new TreeView<>(root, 26);
    }

    public void update(float dt) {
        scroll.update(dt);
        treeScroll.update(dt);
        tooltip.update(dt);
    }

    public boolean blocksBackgroundInput() {
        return modal.isOpen()
            || contextMenu.isOpen()
            || combo.isOpen()
            || searchable.isOpen();
    }

    public void handleKey(UiContext ctx, KeyEvent e) {
        if (e == null) return;
        if (modal.isOpen()) {
            modal.handleKey(e);
            return;
        }
        if (searchable.handleKey(ctx, e, ctx != null ? ctx.clipboard() : null)) return;
        if (combo.isOpen() && combo.handleKey(e)) return;
        if (tree.handleKey(ctx, e)) return;

        if (ctx != null && ctx.focus().focused() == plain.id()) {
            plain.handleKey(e, ctx.clipboard());
        } else if (ctx != null && ctx.focus().focused() == numeric.id()) {
            numeric.handleKey(e, ctx.clipboard());
        }
        draggableNumber.handleKey(ctx, e, ctx != null ? ctx.clipboard() : null);
        for (int i = 0; i < vec3.dimensions(); i++) {
            vec3.field(i).handleKey(ctx, e, ctx != null ? ctx.clipboard() : null);
        }
    }

    public void handleTextInput(UiContext ctx, TextInputEvent e) {
        if (e == null) return;
        if (modal.isOpen()) return;
        if (searchable.handleTextInput(ctx, e)) return;
        if (combo.isOpen() && combo.handleTextInput(e)) return;

        if (ctx != null && ctx.focus().focused() == plain.id()) {
            plain.handleTextInput(e);
        } else if (ctx != null && ctx.focus().focused() == numeric.id()) {
            numeric.handleTextInput(e);
        }
        draggableNumber.handleTextInput(ctx, e);
        for (int i = 0; i < vec3.dimensions(); i++) {
            vec3.field(i).handleTextInput(ctx, e);
        }
    }

    @Override
    public void render(PanelContext ctx) {
        Ui ui = ctx.ui();
        UiContext uiContext = ctx.uiContext();
        UiInput input = ui.input();
        UiRenderer r = ctx.renderer();
        Theme theme = ui.theme();

        ui.beginPanel(ctx.x(), ctx.y(), ctx.width(), ctx.height());
        ui.label(r, "All widgets (scrollable)", true);
        ui.separator(r);

        int pad = theme.design.space_md;
        int x = ctx.x() + pad;
        int y = ctx.y() + 64;
        int w = Math.max(1, ctx.width() - pad * 2);
        int h = Math.max(1, ctx.height() - 74);

        scroll.configureFromTheme(theme);
        scroll.setViewSize(w, h);

        float mx = input.mousePos().x;
        float my = input.mousePos().y;
        boolean hovered = mx >= x && my >= y && mx < x + w && my < y + h;
        boolean blocked = modal.isOpen() || contextMenu.isOpen();

        // Wheel routing: keep the wheel for the nested TreeView scroll when hovered.
        boolean overNestedWheel = false;
        if (!blocked && hovered && input.scrollY() != 0.0) {
            int baseY = y - Math.round(scroll.scrollY());
            int treeW = Math.min(520, w);
            int treeH = 260;
            int treeTop = baseY + offsetToTreeContentTop();
            overNestedWheel = hoveredRect(input, x, treeTop, treeW, treeH);
        }

        if (!blocked && hovered && input.scrollY() != 0.0 && !overNestedWheel) {
            double wheel = input.consumeScrollY();
            scroll.scroll(0.0f, (float) (-wheel * 28.0));
        }
        if (!blocked) {
            scroll.handleScrollbarInput(uiContext, input, x, y);
        }

        int contentH = computeContentHeight(theme, w);
        scroll.setContentSize(w, contentH);

        r.flush();
        r.pushClip(x, y, w, h);
        int cy = y - Math.round(scroll.scrollY());

        cy = sectionHeader(r, theme, x, cy, w, "Text");
        plain.render(r, uiContext, input, theme, x, cy, Math.min(520, w), 34, !blocked);
        cy += 46;
        numeric.render(r, uiContext, input, theme, x, cy, Math.min(520, w), 34, !blocked);
        cy += 46;

        cy = sectionHeader(r, theme, x, cy, w, "Numbers");
        draggableNumber.render(r, uiContext, input, theme, x, cy, 220, 34, !blocked);
        legacyDragNumber.render(r, uiContext, input, theme, x + 240, cy, Math.min(260, w - 240), 34, "DragNumber", !blocked);
        cy += 46;

        cy = sectionHeader(r, theme, x, cy, w, "Vector Input");
        cy += vec3.render(r, uiContext, input, theme, x, cy, Math.min(640, w), !blocked) + 8;

        cy = sectionHeader(r, theme, x, cy, w, "Segmented Control");
        segmentedIndex = segmented.render(r, uiContext, input, theme, x, cy, Math.min(520, w), 34, new String[]{"Global", "Local", "Normal"}, segmentedIndex, !blocked);
        cy += 46;

        cy = sectionHeader(r, theme, x, cy, w, "ComboBoxes");
        combo.render(r, uiContext, input, theme, x, cy, Math.min(360, w), 34, 220, 28, !blocked, true);
        cy += 46;
        searchable.render(r, uiContext, input, theme, x, cy, Math.min(360, w), 34, 260, 28, !blocked);
        cy += 46;

        cy = sectionHeader(r, theme, x, cy, w, "Tabs + Accordion + Thumbnail");
        int blockH = 260;
        int tabsW = 34;
        r.drawRoundedRect(x, cy, Math.min(720, w), blockH, theme.design.radius_sm, Theme.toArgb(theme.panelBg), theme.design.border_thin, Theme.toArgb(theme.widgetOutline));
        iconTabs.render(r, uiContext, input, theme, x, cy, tabsW, blockH, 24, !blocked);
        int contentX = x + tabsW + theme.design.space_sm;
        int contentY = cy + theme.design.space_sm;
        int contentW2 = Math.max(1, Math.min(720, w) - tabsW - theme.design.space_sm * 2);
        if (viewport3d != null) {
            thumbnail.setTexture(viewport3d.texture());
        }
        thumbnail.render(r, theme, contentX, contentY, Math.min(120, contentW2), 90);
        int ax = contentX + Math.min(120, contentW2) + 12;
        int ay = contentY;
        int aw = Math.max(140, contentX + contentW2 - ax);
        accordion.render(
            r,
            uiContext,
            input,
            theme,
            ax,
            ay,
            aw,
            30,
            160,
            "Accordion",
            !blocked,
            (rr, cx0, cy0, cw0, ch0) -> rr.drawText("Content goes here.", cx0 + 8, rr.baselineForBox(cy0, 24), Theme.toArgb(theme.textMuted))
        );
        cy += blockH + 16;

        cy = sectionHeader(r, theme, x, cy, w, "Color Picker");
        colorPicker.render(r, blocked ? null : input, theme, x, cy, Math.min(760, w), 360, !blocked);
        cy += 376;

        cy = sectionHeader(r, theme, x, cy, w, "TreeView (virtualized) + ScrollView");
        int treeH = 260;
        int treeW = Math.min(520, w);
        treeScroll.configureFromTheme(theme);
        treeScroll.setViewSize(treeW, treeH);
        treeScroll.setContentSize(treeW, tree.computeContentHeight());
        boolean treeHovered = hoveredRect(input, x, cy, treeW, treeH);
        if (!blocked && treeHovered && input.scrollY() != 0.0) {
            double wheel = input.consumeScrollY();
            treeScroll.scroll(0.0f, (float) (-wheel * 28.0));
        }
        if (!blocked) {
            treeScroll.handleScrollbarInput(uiContext, input, x, cy);
        }
        int scrollOffset = Math.round(treeScroll.scrollY());
        r.flush();
        r.pushClip(x, cy, treeW, treeH);
        tree.render(r, uiContext, blocked ? null : input, theme, x, cy, treeW, treeH, scrollOffset, !blocked);
        r.flush();
        r.popClip();
        treeScroll.renderScrollbars(r, input, x, cy, 0x1A000000, Theme.toArgb(theme.textMuted), Theme.toArgb(theme.text));
        cy += treeH + 16;

        cy = sectionHeader(r, theme, x, cy, w, "Tooltip + Context Menu + Modal");
        int btnH = 34;
        int rowY = cy;

        if (button(r, input, theme, x, rowY, 200, btnH, "Open Context Menu", !blocked)) {
            contextMenu.open((int) mx, (int) my);
        }
        if (button(r, input, theme, x + 212, rowY, 140, btnH, "Open Modal", !blocked)) {
            modal.open();
        }
        if (toasts != null && button(r, input, theme, x + 364, rowY, 140, btnH, "Show Toast", !blocked)) {
            toasts.show("Saved File");
        }

        int tipX = x;
        int tipY = rowY + btnH + 12;
        r.drawRoundedRect(tipX, tipY, 210, btnH, theme.design.radius_sm, Theme.toArgb(theme.widgetBg), theme.design.border_thin, Theme.toArgb(theme.widgetOutline));
        r.drawText("Hover me (tooltip)", tipX + 10, r.baselineForBox(tipY, btnH), Theme.toArgb(theme.text));
        if (hoveredRect(input, tipX, tipY, 210, btnH)) {
            tooltip.show("This is a tooltip.", (int) mx + 12, (int) my + 14);
        } else {
            tooltip.hide();
        }

        if (eyedropper != null) {
            int ex = tipX + 222;
            eyedropper.render(r, uiContext, input, theme, ex, tipY, 44, btnH, !blocked);
        }

        r.flush();
        r.popClip();

        scroll.renderScrollbars(r, input, x, y, 0x12000000, Theme.toArgb(theme.textMuted), Theme.toArgb(theme.text));

        ui.endPanel();

        // Overlay widgets (not clipped by the scroll view).
        renderOverlays(uiContext, input, theme);
    }

    private void renderOverlays(UiContext ctx, UiInput input, Theme theme) {
        if (ctx == null) return;
        float mx = input != null ? input.mousePos().x : -1.0f;
        float my = input != null ? input.mousePos().y : -1.0f;

        if (contextMenu.isOpen()) {
            int itemH = 28;
            int bg = Theme.toArgb(theme.panelBg);
            int hover = Theme.toArgb(theme.widgetHover);
            int text = Theme.toArgb(theme.text);
            int wGuess = contextMenu.lastWidth() > 0 ? contextMenu.lastWidth() : 220;
            int hGuess = contextMenu.lastHeight() > 0 ? contextMenu.lastHeight() : itemH * Math.max(1, contextMenu.items().size());
            int hoverIndex = (mx >= contextMenu.x() && my >= contextMenu.y() && mx < contextMenu.x() + wGuess && my < contextMenu.y() + hGuess)
                ? (int) ((my - contextMenu.y()) / itemH)
                : -1;

            if (input != null && input.mousePressed()) {
                contextMenu.handleClick((int) mx, (int) my, itemH);
            }

            int fHover = hoverIndex;
            ctx.overlay().add(r -> contextMenu.render(r, theme, itemH, bg, hover, text, fHover));
        }

        if (modal.isOpen()) {
            int ww = MiryContext.host().getWindowWidth();
            int wh = MiryContext.host().getWindowHeight();
            int bg = Theme.toArgb(theme.panelBg);
            int overlay = 0x88000000;
            int text = Theme.toArgb(theme.text);
            int btn = Theme.toArgb(theme.widgetBg);
            int btnHover = Theme.toArgb(theme.widgetHover);
            int outline = Theme.toArgb(theme.widgetOutline);

            if (input != null && input.mousePressed()) {
                modal.handleClick(ww, wh, (int) mx, (int) my);
            }

            ctx.overlay().add(r -> modal.render(r, theme, ww, wh, (int) mx, (int) my, bg, overlay, text, btn, btnHover, outline));
        }

        if (tooltip != null) {
            int tipBg = Theme.toArgb(theme.panelBg);
            int tipText = Theme.toArgb(theme.text);
            ctx.overlay().add(r -> tooltip.render(r, theme, tipBg, tipText));
        }
    }

    private static boolean hoveredRect(UiInput input, int x, int y, int w, int h) {
        if (input == null) return false;
        float mx = input.mousePos().x;
        float my = input.mousePos().y;
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private static int sectionHeader(UiRenderer r, Theme theme, int x, int y, int width, String title) {
        int h = 26;
        int bg = Theme.toArgb(theme.headerBg);
        int top = Theme.lightenArgb(bg, 0.06f);
        int bottom = Theme.darkenArgb(bg, 0.06f);
        int w = Math.max(1, width);
        r.drawLinearGradientRoundedRect(x, y, w, h, theme.design.radius_sm, top, bottom, 0.0f, 1.0f, theme.design.border_thin, Theme.toArgb(theme.widgetOutline));
        r.drawText(title, x + theme.design.space_sm, r.baselineForBox(y, h), Theme.toArgb(theme.text));
        return y + h + 10;
    }

    private static int computeContentHeight(Theme theme, int w) {
        int base = 0;
        base += 26 + 10; // header "Text"
        base += 46 + 46;
        base += 26 + 10; // header "Numbers"
        base += 46;
        base += 26 + 10; // "Vector Input"
        base += 140;
        base += 26 + 10; // "Segmented"
        base += 46;
        base += 26 + 10; // "ComboBoxes"
        base += 46 + 46;
        base += 26 + 10; // "Tabs"
        base += 260 + 16;
        base += 26 + 10; // "Color Picker"
        base += 376;
        base += 26 + 10; // "Tree"
        base += 260 + 16;
        base += 26 + 10; // "Tooltip"
        base += 120;
        return base + theme.design.space_md * 4;
    }

    private static int offsetToTreeContentTop() {
        int base = 0;
        base += 26 + 10; // header "Text"
        base += 46 + 46;
        base += 26 + 10; // header "Numbers"
        base += 46;
        base += 26 + 10; // "Vector Input"
        base += 140;
        base += 26 + 10; // "Segmented"
        base += 46;
        base += 26 + 10; // "ComboBoxes"
        base += 46 + 46;
        base += 26 + 10; // "Tabs"
        base += 260 + 16;
        base += 26 + 10; // "Color Picker"
        base += 376;
        base += 26 + 10; // "Tree" header
        return base;
    }

    private static boolean button(UiRenderer r, UiInput input, Theme theme, int x, int y, int w, int h, String label, boolean interactive) {
        int bg = Theme.toArgb(theme.widgetBg);
        int outline = Theme.toArgb(theme.widgetOutline);
        int text = Theme.toArgb(theme.text);

        float mx = input != null ? input.mousePos().x : -1.0f;
        float my = input != null ? input.mousePos().y : -1.0f;
        boolean hovered = interactive && mx >= x && my >= y && mx < x + w && my < y + h;
        boolean pressed = hovered && input != null && input.mouseDown();

        if (!interactive) {
            bg = Theme.toArgb(theme.disabledBg);
            text = Theme.toArgb(theme.disabledFg);
        } else if (pressed) {
            bg = Theme.toArgb(theme.widgetActive);
        } else if (hovered) {
            bg = Theme.toArgb(theme.widgetHover);
        }

        int t = theme.design.border_thin;
        int top = Theme.lightenArgb(bg, 0.06f);
        int bottom = Theme.darkenArgb(bg, 0.06f);
        r.drawRoundedRect(x, y, w, h, theme.design.radius_sm, top, top, bottom, bottom, t, outline);
        r.drawText(label, x + theme.design.space_sm, r.baselineForBox(y, h), text);
        return hovered && input != null && input.mouseReleased();
    }
}
