package com.miry.demo;

import com.miry.ui.PanelContext;
import com.miry.ui.Ui;
import com.miry.ui.UiContext;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import com.miry.ui.panels.Panel;
import com.miry.ui.theme.Theme;
import com.miry.ui.util.NumericExpression;
import com.miry.ui.widgets.*;

import java.util.function.IntSupplier;


public final class InputPanel extends Panel {
    private final TextField numericLength = new TextField("2m + 30cm");
    private final TextField numericAngle = new TextField("90deg + pi/2");
    private final TextField plain = new TextField("Edit me");
    private final DraggableNumberField drag = new DraggableNumberField(50.0f, 0.0f, 100.0f);
    private final VectorInputField vecLoc = new VectorInputField("Location", 3, -10.0f, 10.0f, 0.0f);
    private final VectorInputField vecRot = new VectorInputField("Rotation", 3, -180.0f, 180.0f, 0.0f);
    private final VectorInputField vecScale = new VectorInputField("Scale", 3, 0.01f, 10.0f, 1.0f);
    private final SegmentedControl basis = new SegmentedControl();
    private int basisMode;
    private final SearchableComboBox<String> addMenu = new SearchableComboBox<>();
    private final CollapsibleHeader transform = new CollapsibleHeader();
    private final VerticalIconTabs propTabs = new VerticalIconTabs();
    private final ThumbnailPreview thumbnail = new ThumbnailPreview();
    private boolean toggle;
    private float slider = 0.35f;

    private final Viewport3D viewport3d;
    private final EyedropperButton eyedropper;
    private final ToastManager toasts;
    private final IntSupplier pickedColorSupplier;

    public InputPanel(Viewport3D viewport3d, EyedropperButton eyedropper, ToastManager toasts, IntSupplier pickedColorSupplier) {
        super("Input");
        this.viewport3d = viewport3d;
        this.eyedropper = eyedropper;
        this.toasts = toasts;
        this.pickedColorSupplier = pickedColorSupplier;

        numericLength.setNumericMode(true, NumericExpression.UnitKind.LENGTH_METERS, 3);
        numericAngle.setNumericMode(true, NumericExpression.UnitKind.ANGLE_RADIANS, 3);

        drag.setNumericFormat(NumericExpression.UnitKind.NONE, 2);
        drag.setDragSpeed(0.12f);
        drag.setSnapStep(1.0f);

        addMenu.addItem("Add Modifier…");
        addMenu.addItem("Bevel");
        addMenu.addItem("Boolean");
        addMenu.addItem("Array");
        addMenu.addItem("Mirror");
        addMenu.addItem("Subdivision Surface");
        addMenu.setSelectedIndex(0);

        propTabs.addTab(com.miry.ui.theme.Icon.CHECK, "Object");
        propTabs.addTab(com.miry.ui.theme.Icon.CHEVRON_DOWN, "Modifiers");
        propTabs.addTab(com.miry.ui.theme.Icon.CHEVRON_RIGHT, "Material");
    }

    public void handleKey(UiContext ctx, KeyEvent e) {
        if (e == null) return;
        // Searchable dropdown always gets first shot while open.
        if (addMenu.handleKey(ctx, e, ctx.clipboard())) {
            return;
        }

        if (ctx.focus().focused() == numericLength.id()) {
            numericLength.handleKey(e, ctx.clipboard());
        } else if (ctx.focus().focused() == numericAngle.id()) {
            numericAngle.handleKey(e, ctx.clipboard());
        } else if (ctx.focus().focused() == plain.id()) {
            plain.handleKey(e, ctx.clipboard());
        }

        drag.handleKey(ctx, e, ctx.clipboard());
        for (int i = 0; i < vecLoc.dimensions(); i++) {
            vecLoc.field(i).handleKey(ctx, e, ctx.clipboard());
            vecRot.field(i).handleKey(ctx, e, ctx.clipboard());
            vecScale.field(i).handleKey(ctx, e, ctx.clipboard());
        }
    }

    public void handleTextInput(UiContext ctx, TextInputEvent e) {
        if (e == null) return;
        if (addMenu.handleTextInput(ctx, e)) {
            return;
        }

        if (ctx.focus().focused() == numericLength.id()) {
            numericLength.handleTextInput(e);
        } else if (ctx.focus().focused() == numericAngle.id()) {
            numericAngle.handleTextInput(e);
        } else if (ctx.focus().focused() == plain.id()) {
            plain.handleTextInput(e);
        }

        drag.handleTextInput(ctx, e);
        for (int i = 0; i < vecLoc.dimensions(); i++) {
            vecLoc.field(i).handleTextInput(ctx, e);
            vecRot.field(i).handleTextInput(ctx, e);
            vecScale.field(i).handleTextInput(ctx, e);
        }
    }

    @Override
    public void render(PanelContext ctx) {
        Ui ui = ctx.ui();
        var r = ctx.renderer();
        Theme t = ui.theme();
        UiContext uiCtx = ctx.uiContext();

        ui.beginPanel(ctx.x(), ctx.y(), ctx.width(), ctx.height());
        ui.label(r, "Expressions: press Enter to evaluate", true);
        ui.separator(r);

        int x = ctx.x() + t.design.space_md;
        int y = ctx.y() + 64;
        int w = Math.max(160, ctx.width() - t.design.space_md * 2);

        r.drawText("Length (supports mm/cm/m/km):", x, r.baselineForBox(y, 22), Theme.toArgb(t.textMuted));
        y += 22;
        numericLength.render(r, uiCtx, ui.input(), t, x, y, w, 34, true);
        y += 46;

        r.drawText("Angle (deg/rad + pi):", x, r.baselineForBox(y, 22), Theme.toArgb(t.textMuted));
        y += 22;
        numericAngle.render(r, uiCtx, ui.input(), t, x, y, w, 34, true);
        y += 46;

        r.drawText("Plain TextField:", x, r.baselineForBox(y, 22), Theme.toArgb(t.textMuted));
        y += 22;
        plain.render(r, uiCtx, ui.input(), t, x, y, w, 34, true);
        y += 46;

        r.drawText("DragNumber (infinite drag / mouse wrap):", x, r.baselineForBox(y, 22), Theme.toArgb(t.textMuted));
        y += 22;
        drag.render(r, uiCtx, ui.input(), t, x, y, Math.min(220, w), 34, true);
        int bx = x + Math.min(220, w) + 10;
        int by = y;
        int bw = Math.max(34, Math.min(44, x + w - bx));
        eyedropper.render(r, uiCtx, ui.input(), t, bx, by, bw, 34, true);
        r.drawRect(bx + bw + 10, by + 8, 22, 18, pickedColorSupplier.getAsInt());
        r.drawText("Eyedropper", bx + bw + 40, r.baselineForBox(by, 34), Theme.toArgb(t.textMuted));
        y += 46;

        r.drawText("Searchable dropdown:", x, r.baselineForBox(y, 22), Theme.toArgb(t.textMuted));
        y += 22;
        addMenu.render(r, uiCtx, ui.input(), t, x, y, Math.min(300, w), 34, 240, 28, true);
        y += 46;

        r.drawText("Segmented control:", x, r.baselineForBox(y, 22), Theme.toArgb(t.textMuted));
        y += 22;
        basisMode = basis.render(r, uiCtx, ui.input(), t, x, y, Math.min(360, w), 34, new String[]{"Global", "Local", "Normal"}, basisMode, true);
        y += 46;

        // Properties-like block: vertical icon tabs + collapsible "Transform" with vector inputs.
        int propsX = x;
        int propsY = y;
        int propsW = Math.min(520, w);
        int tabsW = 34;
        int propsH = Math.min(240, Math.max(140, ctx.height() - (propsY - ctx.y()) - 90));

        r.drawRoundedRect(propsX, propsY, propsW, propsH, t.design.radius_sm, Theme.toArgb(t.panelBg), t.design.border_thin, Theme.toArgb(t.widgetOutline));
        propTabs.render(r, uiCtx, ui.input(), t, propsX, propsY, tabsW, propsH, 24, true);

        int contentX = propsX + tabsW + t.design.space_sm;
        int contentY = propsY + t.design.space_sm;
        int contentW = propsW - tabsW - t.design.space_sm * 2;

        // Thumbnail preview (uses the viewport texture).
        if (viewport3d != null) {
            thumbnail.setTexture(viewport3d.texture());
        }
        thumbnail.render(r, t, contentX, contentY, Math.min(120, contentW), 90);

        int tx = contentX + Math.min(120, contentW) + 12;
        int ty = contentY;
        int tw = Math.max(160, contentX + contentW - tx);
        transform.render(
            r,
            uiCtx,
            ui.input(),
            t,
            tx,
            ty,
            tw,
            30,
            160,
            "Transform",
            true,
            (rr, cx, cy, cw, ch) -> {
                int yy = cy + 8;
                yy += vecLoc.render(rr, uiCtx, ui.input(), t, cx + 8, yy, cw - 16, true) + 8;
                yy += vecRot.render(rr, uiCtx, ui.input(), t, cx + 8, yy, cw - 16, true) + 8;
                vecScale.render(rr, uiCtx, ui.input(), t, cx + 8, yy, cw - 16, true);
            }
        );

        y = propsY + propsH + 14;

        toggle = ui.toggle(r, "Toggle", toggle);
        slider = ui.sliderFloat(r, "Slider", slider, 0.0f, 1.0f);
        if (ui.button(r, "Show Toast")) {
            toasts.show("Saved File");
        }
        ui.endPanel();
    }
}
