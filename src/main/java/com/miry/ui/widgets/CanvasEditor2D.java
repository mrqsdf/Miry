package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.cursor.CursorType;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Theme;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 2D canvas editor with grid, pan/zoom, and selection.
 */
public final class CanvasEditor2D extends BaseWidget {
    public interface CanvasObject {
        Vector2f position();
        float rotation();
        Vector2f scale();
        Vector2f size(); // local size (unscaled), in world units
        void setPosition(Vector2f pos);
        void setRotation(float degrees);
        void setScale(Vector2f scale);
        boolean contains(float x, float y);
        void render(UiRenderer r, Theme theme, int px, int py);
    }

    public enum ToolMode { SELECT, PAN, RULER }

    private enum Mode { NONE, DRAG, RESIZE, ROTATE, BOX_SELECT, PAN, RULER }
    private enum Handle { NONE, N, NE, E, SE, S, SW, W, NW, ROTATE, PIVOT }

    private final List<CanvasObject> objects = new ArrayList<>();
    private final Set<CanvasObject> selection = new HashSet<>();
    private final Vector2f panOffset = new Vector2f(0, 0);
    private float zoom = 1.0f;
    private boolean showGrid = true;
    private float gridSize = 32.0f;
    private int primaryGridSteps = 8;  // Draw primary line every N grid steps
    private boolean snapToGrid = false;

    private ToolMode toolMode = ToolMode.SELECT;

    private Mode mode = Mode.NONE;
    private Handle activeHandle = Handle.NONE;
    private Handle hoverHandle = Handle.NONE;

    private final Vector2f mouseWorld = new Vector2f();
    private final Vector2f dragStartWorld = new Vector2f();
    private final Vector2f boxStartWorld = new Vector2f();
    private final Vector2f boxCurrentWorld = new Vector2f();

    private final Vector2f pivotWorld = new Vector2f();
    private boolean pivotCustom = false;

    private final Map<CanvasObject, Snapshot> startSnapshots = new IdentityHashMap<>();
    private final Vector2f startGroupMin = new Vector2f();
    private final Vector2f startGroupMax = new Vector2f();
    private final Vector2f startGroupCenter = new Vector2f();
    private float startMouseAngleDeg;

    // Snap guides (screen space)
    private boolean snapX;
    private boolean snapY;
    private float snapScreenX;
    private float snapScreenY;

    // Ruler tool state
    private final Vector2f rulerStartWorld = new Vector2f();
    private final Vector2f rulerEndWorld = new Vector2f();
    private boolean rulerActive = false;
    private boolean frameShiftDown = false;
    private boolean frameAltDown = false;

    public void addObject(CanvasObject obj) { objects.add(obj); }
    public void removeObject(CanvasObject obj) { objects.remove(obj); selection.remove(obj); }
    public void clearObjects() { objects.clear(); selection.clear(); }
    public Set<CanvasObject> selection() { return selection; }
    public void setShowGrid(boolean show) { this.showGrid = show; }
    public boolean showGrid() { return showGrid; }
    public void setZoom(float zoom) { this.zoom = Math.max(0.1f, Math.min(10.0f, zoom)); }
    public void setToolMode(ToolMode mode) { this.toolMode = mode != null ? mode : ToolMode.SELECT; }
    public ToolMode toolMode() { return toolMode; }
    public void setSnapToGrid(boolean snap) { this.snapToGrid = snap; }
    public boolean snapToGrid() { return snapToGrid; }
    public void setGridSize(float size) { this.gridSize = Math.max(1.0f, size); }
    public float gridSize() { return gridSize; }
    public void setPrimaryGridSteps(int steps) { this.primaryGridSteps = Math.max(1, steps); }
    public int primaryGridSteps() { return primaryGridSteps; }

    public void bringSelectionToFront() {
        if (selection.isEmpty()) return;
        for (CanvasObject obj : new ArrayList<>(selection)) {
            objects.remove(obj);
            objects.add(obj);
        }
    }

    public void sendSelectionToBack() {
        if (selection.isEmpty()) return;
        int insert = 0;
        for (CanvasObject obj : new ArrayList<>(selection)) {
            objects.remove(obj);
            objects.add(insert++, obj);
        }
    }

    public void moveSelectionUp() {
        if (selection.isEmpty()) return;
        for (int i = objects.size() - 2; i >= 0; i--) {
            CanvasObject a = objects.get(i);
            CanvasObject b = objects.get(i + 1);
            if (selection.contains(a) && !selection.contains(b)) {
                objects.set(i, b);
                objects.set(i + 1, a);
            }
        }
    }

    public void moveSelectionDown() {
        if (selection.isEmpty()) return;
        for (int i = 1; i < objects.size(); i++) {
            CanvasObject a = objects.get(i - 1);
            CanvasObject b = objects.get(i);
            if (!selection.contains(a) && selection.contains(b)) {
                objects.set(i - 1, b);
                objects.set(i, a);
            }
        }
    }

    public void render(UiRenderer r, UiInput input, Theme theme, int x, int y, int width, int height) {
        render(r, null, input, theme, x, y, width, height, true);
    }

    public void render(UiRenderer r, UiContext ctx, UiInput input, Theme theme, int x, int y, int width, int height, boolean interactive) {
        r.drawRect(x, y, width, height, Theme.darkenArgb(Theme.toArgb(theme.panelBg), 0.1f));

        boolean canInteract = interactive(ctx, interactive) && input != null;
        float mx = canInteract ? input.mousePos().x : 0;
        float my = canInteract ? input.mousePos().y : 0;
        boolean mouseDown = canInteract && input.mouseDown();
        boolean mousePressed = canInteract && input.mousePressed();
        boolean mouseReleased = canInteract && input.mouseReleased();
        boolean hovered = canInteract && mx >= x && my >= y && mx < x + width && my < y + height;
        frameShiftDown = canInteract && input.shiftDown();
        frameAltDown = canInteract && input.altDown();

        // World space mouse
        screenToWorld(mx - x, my - y, width, height, mouseWorld);

        // Hover handle -> cursor feedback
        hoverHandle = hovered && mode == Mode.NONE ? hitTestHandle(mx, my, x, y, width, height, theme) : Handle.NONE;
        updateCursor(ctx, hoverHandle);

        // Pan (ctrl+drag, or PAN tool). In ruler mode, hold Ctrl to pan.
        boolean wantPan = hovered && mouseDown && (toolMode == ToolMode.PAN || input.ctrlDown());
        if (wantPan) {
            if (mode != Mode.PAN) {
                mode = Mode.PAN;
                dragStartWorld.set(mx, my); // screen-space for panning
                if (ctx != null) ctx.pointer().capture(id());
            }
            panOffset.add(mx - dragStartWorld.x, my - dragStartWorld.y);
            dragStartWorld.set(mx, my);
        } else if (mode == Mode.PAN && !mouseDown) {
            mode = Mode.NONE;
            if (ctx != null && ctx.pointer().isCaptured(id())) ctx.pointer().release();
        }

        // Ruler tool
        if (toolMode == ToolMode.RULER) {
            if (hovered && mousePressed && mode == Mode.NONE) {
                mode = Mode.RULER;
                rulerStartWorld.set(mouseWorld);
                rulerEndWorld.set(mouseWorld);
                rulerActive = true;
                if (ctx != null) ctx.pointer().capture(id());
            } else if (mode == Mode.RULER && mouseDown) {
                rulerEndWorld.set(mouseWorld);
                if (frameShiftDown) {
                    float dx = rulerEndWorld.x - rulerStartWorld.x;
                    float dy = rulerEndWorld.y - rulerStartWorld.y;
                    if (Math.abs(dx) >= Math.abs(dy)) {
                        rulerEndWorld.y = rulerStartWorld.y;
                    } else {
                        rulerEndWorld.x = rulerStartWorld.x;
                    }
                }
            } else if (mode == Mode.RULER && mouseReleased) {
                mode = Mode.NONE;
                rulerActive = false;
                if (ctx != null && ctx.pointer().isCaptured(id())) ctx.pointer().release();
            }
        } else {
            if (rulerActive && mode != Mode.RULER) {
                rulerActive = false;
            }
        }

        // Zoom
        if (hovered) {
            float wheelDelta = input.consumeMouseScrollDelta();
            if (wheelDelta != 0) {
                float localX = mx - x;
                float localY = my - y;
                Vector2f worldBefore = new Vector2f();
                screenToWorld(localX, localY, width, height, worldBefore);

                float factor = 1.0f + wheelDelta * 0.1f;
                if (factor < 0.1f) factor = 0.1f;
                zoom = Math.max(0.1f, Math.min(10.0f, zoom * factor));

                // Keep the world point under the cursor stable.
                panOffset.set(localX - worldBefore.x * zoom, localY - worldBefore.y * zoom);
            }
        }

        // Grid
        if (showGrid) {
            renderGrid(r, theme, x, y, width, height);
        }

        // Objects
        r.pushClipRect(x, y, width, height);
        for (CanvasObject obj : objects) {
            Vector2f screenPos = new Vector2f();
            worldToScreen(obj.position().x, obj.position().y, width, height, screenPos);
            obj.render(r, theme, x + (int)screenPos.x, y + (int)screenPos.y);
        }
        r.popClipRect();

        // Reset snap guides each frame; set during drag.
        snapX = false;
        snapY = false;

        // Start interactions on press (handles -> objects -> background)
        if (hovered && mousePressed && mode == Mode.NONE && toolMode == ToolMode.SELECT) {
            if (!selection.isEmpty() && hoverHandle != Handle.NONE) {
                beginTransform(ctx, hoverHandle);
            } else {
                CanvasObject hit = hitTestObject();
                if (hit != null) {
                    boolean ctrl = input.ctrlDown();
                    boolean shift = input.shiftDown();

                    if (!ctrl && !shift) {
                        if (!selection.contains(hit)) {
                            selection.clear();
                            selection.add(hit);
                            updatePivotFromSelection();
                        }
                    } else if (ctrl) {
                        if (selection.contains(hit)) selection.remove(hit);
                        else selection.add(hit);
                        updatePivotFromSelection();
                    } else {
                        selection.add(hit);
                        updatePivotFromSelection();
                    }

                    beginDrag(ctx);
                } else {
                    if (!input.ctrlDown() && !input.shiftDown()) {
                        selection.clear();
                        updatePivotFromSelection();
                    }
                    mode = Mode.BOX_SELECT;
                    boxStartWorld.set(mouseWorld);
                    boxCurrentWorld.set(mouseWorld);
                    if (ctx != null) ctx.pointer().capture(id());
                }
            }
        }

        if (mode == Mode.DRAG && mouseDown) {
            Vector2f delta = new Vector2f(mouseWorld).sub(dragStartWorld);
            boolean constrainAxis = frameShiftDown && activeHandle == Handle.NONE;
            if (constrainAxis) {
                if (Math.abs(delta.x) >= Math.abs(delta.y)) delta.y = 0;
                else delta.x = 0;
            }
            if (activeHandle != Handle.PIVOT) {
                if (!frameAltDown) {
                    applySnapForDrag(delta, x, y, width, height);
                }
                if (constrainAxis) {
                    if (Math.abs(delta.x) >= Math.abs(delta.y)) delta.y = 0;
                    else delta.x = 0;
                }
            }
            if (activeHandle == Handle.PIVOT) {
                pivotWorld.set(new Vector2f(dragStartWorld).add(delta));
                pivotCustom = true;
            } else {
                for (Map.Entry<CanvasObject, Snapshot> e : startSnapshots.entrySet()) {
                    e.getKey().setPosition(new Vector2f(e.getValue().pos).add(delta));
                }
            }
        } else if (mode == Mode.RESIZE && mouseDown) {
            applyResize(mouseWorld);
        } else if (mode == Mode.ROTATE && mouseDown) {
            applyRotate(mouseWorld);
        } else if (mode == Mode.BOX_SELECT && mouseDown) {
            boxCurrentWorld.set(mouseWorld);
        }

        if ((mode == Mode.DRAG || mode == Mode.RESIZE || mode == Mode.ROTATE || mode == Mode.BOX_SELECT) && mouseReleased) {
            commitInteraction(ctx);
        }

        // Selection outline
        r.pushClipRect(x, y, width, height);
        if (!selection.isEmpty()) {
            if (selection.size() == 1) {
                drawSingleSelection(r, theme, x, y, width, height);
            } else {
                drawGroupSelection(r, theme, x, y, width, height);
            }
            drawSnapGuides(r, x, y, width, height);
        }

        if (mode == Mode.BOX_SELECT) {
            Vector2f a = new Vector2f();
            Vector2f b = new Vector2f();
            worldToScreen(boxStartWorld.x, boxStartWorld.y, width, height, a);
            worldToScreen(boxCurrentWorld.x, boxCurrentWorld.y, width, height, b);
            float rx = x + Math.min(a.x, b.x);
            float ry = y + Math.min(a.y, b.y);
            float rw = Math.abs(b.x - a.x);
            float rh = Math.abs(b.y - a.y);
            r.drawRect(rx, ry, rw, rh, 0x334488FF);
            r.drawRectOutline((int) rx, (int) ry, (int) rw, (int) rh, 2, 0xFF4488FF);
        }

        // Ruler tool
        if (rulerActive || mode == Mode.RULER) {
            drawRuler(r, theme, x, y, width, height);
        }
        r.popClipRect();
    }

    private void beginDrag(UiContext ctx) {
        mode = Mode.DRAG;
        activeHandle = Handle.NONE;
        captureStartSnapshots();
        dragStartWorld.set(mouseWorld);
        updateGroupBoundsFromSelection(startGroupMin, startGroupMax, null, null);
        startGroupCenter.set((startGroupMin.x + startGroupMax.x) * 0.5f, (startGroupMin.y + startGroupMax.y) * 0.5f);
        if (!pivotCustom) pivotWorld.set(startGroupCenter);
        if (ctx != null) ctx.pointer().capture(id());
    }

    private void beginTransform(UiContext ctx, Handle handle) {
        captureStartSnapshots();
        updateGroupBoundsFromSelection(startGroupMin, startGroupMax, null, null);
        startGroupCenter.set((startGroupMin.x + startGroupMax.x) * 0.5f, (startGroupMin.y + startGroupMax.y) * 0.5f);
        if (!pivotCustom) pivotWorld.set(startGroupCenter);

        activeHandle = handle;
        if (handle == Handle.ROTATE) {
            mode = Mode.ROTATE;
            startMouseAngleDeg = angleDeg(new Vector2f(mouseWorld).sub(pivotWorld));
        } else if (handle == Handle.PIVOT) {
            mode = Mode.DRAG;
            dragStartWorld.set(pivotWorld);
        } else {
            mode = Mode.RESIZE;
        }
        if (ctx != null) ctx.pointer().capture(id());
    }

    private void commitInteraction(UiContext ctx) {
        if (ctx != null && ctx.pointer().isCaptured(id())) ctx.pointer().release();

        if (mode == Mode.BOX_SELECT) {
            applyBoxSelection();
        } else if (mode == Mode.DRAG || mode == Mode.RESIZE || mode == Mode.ROTATE) {
            if (ctx != null) {
                TransformCommand cmd = TransformCommand.capture("Transform", startSnapshots);
                if (cmd != null && cmd.hasChanges()) {
                    ctx.undo().execute(cmd);
                }
            }
        }

        mode = Mode.NONE;
        activeHandle = Handle.NONE;
        startSnapshots.clear();
    }

    private void captureStartSnapshots() {
        startSnapshots.clear();
        for (CanvasObject o : selection) {
            startSnapshots.put(o, new Snapshot(o));
        }
    }

    private CanvasObject hitTestObject() {
        for (int i = objects.size() - 1; i >= 0; i--) {
            CanvasObject obj = objects.get(i);
            if (obj.contains(mouseWorld.x, mouseWorld.y)) return obj;
        }
        return null;
    }

    private Handle hitTestHandle(float mx, float my, int x, int y, int w, int h, Theme theme) {
        int handleSize = Math.max(10, theme.design.icon_sm);
        int hs = handleSize / 2;

        Vector2f pivotScreen = new Vector2f();
        worldToScreen(pivotWorld.x, pivotWorld.y, w, h, pivotScreen);
        float px = x + pivotScreen.x;
        float py = y + pivotScreen.y;
        if (mx >= px - hs && my >= py - hs && mx < px + hs && my < py + hs) {
            return Handle.PIVOT;
        }

        if (selection.size() == 1) {
            CanvasObject obj = selection.iterator().next();
            OrientedBox obb = new OrientedBox(obj);
            return obb.hitTest(mx, my, x, y, w, h, handleSize);
        }

        Vector2f min = new Vector2f();
        Vector2f max = new Vector2f();
        updateGroupBoundsFromSelection(min, max, null, null);
        Vector2f[] pts = aabbHandlePoints(min, max);
        Handle[] handles = new Handle[]{Handle.NW, Handle.N, Handle.NE, Handle.E, Handle.SE, Handle.S, Handle.SW, Handle.W, Handle.ROTATE};
        for (int i = 0; i < pts.length; i++) {
            Vector2f sp = new Vector2f();
            worldToScreen(pts[i].x, pts[i].y, w, h, sp);
            float sx = x + sp.x;
            float sy = y + sp.y;
            if (mx >= sx - hs && my >= sy - hs && mx < sx + hs && my < sy + hs) {
                return handles[i];
            }
        }
        return Handle.NONE;
    }

    private void updateCursor(UiContext ctx, Handle handle) {
        if (ctx == null || ctx.cursors() == null) return;
        CursorType type = switch (handle) {
            case N, S -> CursorType.VRESIZE;
            case E, W -> CursorType.HRESIZE;
            case NE, SW -> CursorType.NESW_RESIZE;
            case NW, SE -> CursorType.NWSE_RESIZE;
            case ROTATE -> CursorType.CROSSHAIR;
            case PIVOT -> CursorType.HAND;
            default -> CursorType.ARROW;
        };
        ctx.cursors().setCursor(type);
    }

    private void drawSingleSelection(UiRenderer r, Theme theme, int x, int y, int w, int h) {
        CanvasObject obj = selection.iterator().next();
        OrientedBox obb = new OrientedBox(obj);
        int outline = Theme.toArgb(theme.accent);
        obb.drawOutline(r, x, y, w, h, outline, 2);
        obb.drawHandles(r, theme, x, y, w, h, Math.max(10, theme.design.icon_sm));
        drawPivot(r, theme, x, y, w, h);
    }

    private void drawGroupSelection(UiRenderer r, Theme theme, int x, int y, int w, int h) {
        Vector2f min = new Vector2f();
        Vector2f max = new Vector2f();
        updateGroupBoundsFromSelection(min, max, null, null);

        Vector2f a = new Vector2f();
        Vector2f b = new Vector2f();
        worldToScreen(min.x, min.y, w, h, a);
        worldToScreen(max.x, max.y, w, h, b);
        int sx = x + (int) a.x;
        int sy = y + (int) a.y;
        int sw = (int) (b.x - a.x);
        int sh = (int) (b.y - a.y);

        r.drawRectOutline(sx, sy, sw, sh, 2, Theme.toArgb(theme.accent));

        Vector2f[] pts = aabbHandlePoints(min, max);
        Handle[] handles = new Handle[]{Handle.NW, Handle.N, Handle.NE, Handle.E, Handle.SE, Handle.S, Handle.SW, Handle.W, Handle.ROTATE};
        int handleSize = Math.max(10, theme.design.icon_sm);
        for (int i = 0; i < pts.length; i++) {
            Vector2f sp = new Vector2f();
            worldToScreen(pts[i].x, pts[i].y, w, h, sp);
            float hx = x + sp.x - handleSize / 2.0f;
            float hy = y + sp.y - handleSize / 2.0f;
            if (handles[i] == Handle.ROTATE) {
                r.drawCircle(hx + handleSize / 2.0f, hy + handleSize / 2.0f, handleSize * 0.35f, 0xFFFFFFFF, theme.design.border_thin, Theme.toArgb(theme.widgetOutline));
            } else {
                r.drawRoundedRect(hx, hy, handleSize, handleSize, theme.design.radius_sm, 0xFFFFFFFF, theme.design.border_thin, Theme.toArgb(theme.widgetOutline));
            }
        }

        drawPivot(r, theme, x, y, w, h);
    }

    private void drawPivot(UiRenderer r, Theme theme, int x, int y, int w, int h) {
        Vector2f sp = new Vector2f();
        worldToScreen(pivotWorld.x, pivotWorld.y, w, h, sp);
        float cx = x + sp.x;
        float cy = y + sp.y;
        float rad = Math.max(3.0f, theme.design.icon_xs * 0.4f);
        r.drawCircle(cx, cy, rad, 0xFFFFFFFF, theme.design.border_thin, Theme.toArgb(theme.widgetOutline));
    }

    private void drawSnapGuides(UiRenderer r, int x, int y, int w, int h) {
        int guide = 0x88FF33CC;
        if (snapX) {
            r.drawLine((int) snapScreenX, y, (int) snapScreenX, y + h, 2, guide);
        }
        if (snapY) {
            r.drawLine(x, (int) snapScreenY, x + w, (int) snapScreenY, 2, guide);
        }
    }

    private void drawRuler(UiRenderer r, Theme theme, int x, int y, int w, int h) {
        Vector2f a = new Vector2f();
        Vector2f b = new Vector2f();
        worldToScreen(rulerStartWorld.x, rulerStartWorld.y, w, h, a);
        worldToScreen(rulerEndWorld.x, rulerEndWorld.y, w, h, b);

        float ax = x + a.x;
        float ay = y + a.y;
        float bx = x + b.x;
        float by = y + b.y;

        int accent = Theme.toArgb(theme.accent);
        int main = Theme.mulAlpha(accent, 0.85f);
        int sub = Theme.mulAlpha(accent, 0.45f);

        r.drawLine((int) ax, (int) ay, (int) bx, (int) by, 2, main);
        r.drawCircle(ax, ay, Math.max(3.0f, theme.design.icon_xs * 0.35f), accent, theme.design.border_thin, Theme.toArgb(theme.widgetOutline));
        r.drawCircle(bx, by, Math.max(3.0f, theme.design.icon_xs * 0.35f), accent, theme.design.border_thin, Theme.toArgb(theme.widgetOutline));

        float dx = rulerEndWorld.x - rulerStartWorld.x;
        float dy = rulerEndWorld.y - rulerStartWorld.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

        // Component "L" helper.
        Vector2f c = new Vector2f();
        worldToScreen(rulerEndWorld.x, rulerStartWorld.y, w, h, c);
        float cx = x + c.x;
        float cy = y + c.y;
        if (dist > 0.001f) {
            r.drawLine((int) ax, (int) ay, (int) cx, (int) cy, 1, sub);
            r.drawLine((int) cx, (int) cy, (int) bx, (int) by, 1, sub);
        }

        String line1 = fmt1(dist) + " px";
        String line2 = "dx " + fmt1(dx) + "  dy " + fmt1(dy) + "  " + fmt1(angle) + "°";
        float pad = Math.max(4.0f, theme.design.space_xs);
        float tw = Math.max(r.measureText(line1), r.measureText(line2));
        float boxW = tw + pad * 2.0f;
        float boxH = r.lineHeight() * 2.0f + pad * 2.0f;

        float midX = (ax + bx) * 0.5f;
        float midY = (ay + by) * 0.5f;
        float tx = midX + 12.0f;
        float ty = midY - boxH - 12.0f;
        tx = clamp(tx, x + 2.0f, x + w - boxW - 2.0f);
        ty = clamp(ty, y + 2.0f, y + h - boxH - 2.0f);

        int bg = Theme.mulAlpha(Theme.toArgb(theme.widgetBg), 0.92f);
        int stroke = Theme.toArgb(theme.widgetOutline);
        r.drawRoundedRect(tx, ty, boxW, boxH, theme.design.radius_sm, bg, theme.design.border_thin, stroke);

        float textX = tx + pad;
        float y1 = r.baselineForBox(ty + pad, r.lineHeight());
        float y2 = y1 + r.lineHeight();
        int shadow = 0xAA000000;
        int fg = Theme.toArgb(theme.text);
        r.drawText(line1, textX + 1, y1 + 1, shadow);
        r.drawText(line1, textX, y1, fg);
        r.drawText(line2, textX + 1, y2 + 1, shadow);
        r.drawText(line2, textX, y2, Theme.toArgb(theme.textMuted));
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String fmt1(float v) {
        float snapped = Math.round(v * 10.0f) / 10.0f;
        return Float.toString(snapped);
    }

    private void applyBoxSelection() {
        Vector2f min = new Vector2f(Math.min(boxStartWorld.x, boxCurrentWorld.x), Math.min(boxStartWorld.y, boxCurrentWorld.y));
        Vector2f max = new Vector2f(Math.max(boxStartWorld.x, boxCurrentWorld.x), Math.max(boxStartWorld.y, boxCurrentWorld.y));
        selection.clear();
        for (CanvasObject obj : objects) {
            Vector2f omin = new Vector2f();
            Vector2f omax = new Vector2f();
            boundsWorldAabb(obj, omin, omax);
            if (aabbIntersects(min, max, omin, omax)) {
                selection.add(obj);
            }
        }
        updatePivotFromSelection();
    }

    private void applyResize(Vector2f mouseWorld) {
        if (selection.isEmpty()) return;
        if (activeHandle == Handle.NONE || activeHandle == Handle.ROTATE || activeHandle == Handle.PIVOT) return;

        if (selection.size() == 1) {
            CanvasObject obj = selection.iterator().next();
            Snapshot s = startSnapshots.get(obj);
            if (s == null) return;
            resizeSingle(obj, s, mouseWorld);
        } else {
            resizeGroup(mouseWorld);
        }
    }

    private void resizeSingle(CanvasObject obj, Snapshot start, Vector2f mouseWorld) {
        float rotRad = (float) Math.toRadians(start.rotDeg);
        float hx0 = (start.size.x * start.scale.x) * 0.5f;
        float hy0 = (start.size.y * start.scale.y) * 0.5f;

        // invRot(world-pos) gives localScaled coords (already multiplied by scale)
        Vector2f localScaled = invRotate(new Vector2f(mouseWorld).sub(start.pos), rotRad);

        Vector2f anchorLocal = oppositeAnchorLocalScaled(activeHandle, hx0, hy0);
        Vector2f anchorWorld = new Vector2f(start.pos).add(rotate(anchorLocal, rotRad));

        float hx1 = hx0;
        float hy1 = hy0;
        if (handleHasX(activeHandle)) hx1 = Math.max(2.0f, Math.abs(localScaled.x));
        if (handleHasY(activeHandle)) hy1 = Math.max(2.0f, Math.abs(localScaled.y));

        float sx = Math.max(0.05f, (hx1 * 2.0f) / Math.max(1e-3f, start.size.x));
        float sy = Math.max(0.05f, (hy1 * 2.0f) / Math.max(1e-3f, start.size.y));

        Vector2f newAnchorLocal = oppositeAnchorLocalScaled(activeHandle, hx1, hy1);
        Vector2f newPos = new Vector2f(anchorWorld).sub(rotate(newAnchorLocal, rotRad));

        obj.setScale(new Vector2f(sx, sy));
        obj.setPosition(newPos);
    }

    private void resizeGroup(Vector2f mouseWorld) {
        Vector2f min0 = startGroupMin;
        Vector2f max0 = startGroupMax;
        Vector2f center0 = startGroupCenter;

        float w0 = Math.max(2.0f, max0.x - min0.x);
        float h0 = Math.max(2.0f, max0.y - min0.y);

        Vector2f anchor0 = oppositeAnchorWorldAabb(activeHandle, min0, max0);

        Vector2f min1 = new Vector2f(min0);
        Vector2f max1 = new Vector2f(max0);
        if (handleHasX(activeHandle)) {
            if (activeHandle == Handle.W || activeHandle == Handle.NW || activeHandle == Handle.SW) min1.x = Math.min(mouseWorld.x, max0.x - 2);
            else max1.x = Math.max(mouseWorld.x, min0.x + 2);
        }
        if (handleHasY(activeHandle)) {
            if (activeHandle == Handle.N || activeHandle == Handle.NW || activeHandle == Handle.NE) min1.y = Math.min(mouseWorld.y, max0.y - 2);
            else max1.y = Math.max(mouseWorld.y, min0.y + 2);
        }

        float w1 = Math.max(2.0f, max1.x - min1.x);
        float h1 = Math.max(2.0f, max1.y - min1.y);

        float sx = handleHasX(activeHandle) ? (w1 / w0) : 1.0f;
        float sy = handleHasY(activeHandle) ? (h1 / h0) : 1.0f;

        Vector2f center1 = new Vector2f((min1.x + max1.x) * 0.5f, (min1.y + max1.y) * 0.5f);
        Vector2f anchor1 = oppositeAnchorWorldAabb(activeHandle, min1, max1);
        center1.add(new Vector2f(anchor0).sub(anchor1));

        for (Map.Entry<CanvasObject, Snapshot> e : startSnapshots.entrySet()) {
            CanvasObject obj = e.getKey();
            Snapshot s = e.getValue();
            Vector2f rel = new Vector2f(s.pos).sub(center0).mul(sx, sy);
            obj.setPosition(new Vector2f(center1).add(rel));
            obj.setScale(new Vector2f(s.scale.x * sx, s.scale.y * sy));
        }
    }

    private void applyRotate(Vector2f mouseWorld) {
        if (selection.isEmpty()) return;
        float ang = angleDeg(new Vector2f(mouseWorld).sub(pivotWorld));
        float delta = ang - startMouseAngleDeg;
        if (frameShiftDown) {
            delta = Math.round(delta / 15.0f) * 15.0f;
        }

        if (selection.size() == 1) {
            CanvasObject obj = selection.iterator().next();
            Snapshot s = startSnapshots.get(obj);
            if (s == null) return;
            obj.setRotation(s.rotDeg + delta);
        } else {
            float rotRad = (float) Math.toRadians(delta);
            for (Map.Entry<CanvasObject, Snapshot> e : startSnapshots.entrySet()) {
                CanvasObject obj = e.getKey();
                Snapshot s = e.getValue();
                Vector2f rel = new Vector2f(s.pos).sub(pivotWorld);
                obj.setPosition(new Vector2f(pivotWorld).add(rotate(rel, rotRad)));
                obj.setRotation(s.rotDeg + delta);
            }
        }
    }

    private void applySnapForDrag(Vector2f delta, int x, int y, int w, int h) {
        if (selection.isEmpty()) return;
        float thresholdWorld = 8.0f / Math.max(0.1f, zoom);

        // Use group AABB center for snapping reference.
        Vector2f gMin = new Vector2f();
        Vector2f gMax = new Vector2f();
        updateGroupBoundsFromSelection(gMin, gMax, startSnapshots, delta);
        float gxC = (gMin.x + gMax.x) * 0.5f;
        float gyC = (gMin.y + gMax.y) * 0.5f;
        float[] gx = new float[]{gxC, gMin.x, gMax.x};
        float[] gy = new float[]{gyC, gMin.y, gMax.y};

        Float bestDx = null;
        Float bestDy = null;
        float bestAbsDx = Float.POSITIVE_INFINITY;
        float bestAbsDy = Float.POSITIVE_INFINITY;
        float guideXWorld = 0.0f;
        float guideYWorld = 0.0f;

        // Grid snapping
        if (snapToGrid && gridSize > 0) {
            float gridSnapX = Math.round(gxC / gridSize) * gridSize;
            float gridSnapY = Math.round(gyC / gridSize) * gridSize;
            float gdx = gridSnapX - gxC;
            float gdy = gridSnapY - gyC;
            float gadx = Math.abs(gdx);
            float gady = Math.abs(gdy);
            if (gadx < thresholdWorld && gadx < bestAbsDx) {
                bestAbsDx = gadx;
                bestDx = gdx;
                guideXWorld = gridSnapX;
            }
            if (gady < thresholdWorld && gady < bestAbsDy) {
                bestAbsDy = gady;
                bestDy = gdy;
                guideYWorld = gridSnapY;
            }
        }

        for (CanvasObject o : objects) {
            if (selection.contains(o)) continue;
            Vector2f omin = new Vector2f();
            Vector2f omax = new Vector2f();
            boundsWorldAabb(o, omin, omax);
            float oxC = (omin.x + omax.x) * 0.5f;
            float oyC = (omin.y + omax.y) * 0.5f;
            float[] ox = new float[]{oxC, omin.x, omax.x};
            float[] oy = new float[]{oyC, omin.y, omax.y};

            for (float gxv : gx) {
                for (float oxv : ox) {
                    float dx = oxv - gxv;
                    float adx = Math.abs(dx);
                    if (adx < thresholdWorld && adx < bestAbsDx) {
                        bestAbsDx = adx;
                        bestDx = dx;
                        guideXWorld = oxv;
                    }
                }
            }

            for (float gyv : gy) {
                for (float oyv : oy) {
                    float dy = oyv - gyv;
                    float ady = Math.abs(dy);
                    if (ady < thresholdWorld && ady < bestAbsDy) {
                        bestAbsDy = ady;
                        bestDy = dy;
                        guideYWorld = oyv;
                    }
                }
            }
        }

        if (bestDx != null) {
            delta.x += bestDx;
            Vector2f sp = new Vector2f();
            worldToScreen(guideXWorld, 0, w, h, sp);
            snapScreenX = x + sp.x;
            snapX = true;
        }
        if (bestDy != null) {
            delta.y += bestDy;
            Vector2f sp = new Vector2f();
            worldToScreen(0, guideYWorld, w, h, sp);
            snapScreenY = y + sp.y;
            snapY = true;
        }
    }

    private void updatePivotFromSelection() {
        if (pivotCustom) return;
        if (selection.isEmpty()) {
            pivotWorld.set(0, 0);
            return;
        }
        Vector2f min = new Vector2f();
        Vector2f max = new Vector2f();
        updateGroupBoundsFromSelection(min, max, null, null);
        pivotWorld.set((min.x + max.x) * 0.5f, (min.y + max.y) * 0.5f);
    }

    private void updateGroupBoundsFromSelection(Vector2f outMin, Vector2f outMax, Map<CanvasObject, Snapshot> base, Vector2f delta) {
        boolean first = true;
        for (CanvasObject obj : selection) {
            Vector2f omin = new Vector2f();
            Vector2f omax = new Vector2f();
            if (base != null && delta != null) {
                Snapshot s = base.get(obj);
                boundsWorldAabb(s, delta, omin, omax);
            } else {
                boundsWorldAabb(obj, omin, omax);
            }
            if (first) {
                outMin.set(omin);
                outMax.set(omax);
                first = false;
            } else {
                outMin.set(Math.min(outMin.x, omin.x), Math.min(outMin.y, omin.y));
                outMax.set(Math.max(outMax.x, omax.x), Math.max(outMax.y, omax.y));
            }
        }
        if (first) {
            outMin.set(0, 0);
            outMax.set(0, 0);
        }
    }

    private static boolean aabbIntersects(Vector2f aMin, Vector2f aMax, Vector2f bMin, Vector2f bMax) {
        return aMin.x <= bMax.x && aMax.x >= bMin.x && aMin.y <= bMax.y && aMax.y >= bMin.y;
    }

    private void boundsWorldAabb(CanvasObject obj, Vector2f outMin, Vector2f outMax) {
        boundsWorldAabb(new Snapshot(obj), new Vector2f(0, 0), outMin, outMax);
    }

    private static void boundsWorldAabb(Snapshot s, Vector2f delta, Vector2f outMin, Vector2f outMax) {
        float rot = (float) Math.toRadians(s.rotDeg);
        float hx = (s.size.x * s.scale.x) * 0.5f;
        float hy = (s.size.y * s.scale.y) * 0.5f;
        Vector2f[] local = new Vector2f[]{
            new Vector2f(-hx, -hy),
            new Vector2f(hx, -hy),
            new Vector2f(hx, hy),
            new Vector2f(-hx, hy)
        };
        outMin.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        outMax.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        for (Vector2f p : local) {
            Vector2f wp = new Vector2f(s.pos).add(delta).add(rotate(p, rot));
            outMin.set(Math.min(outMin.x, wp.x), Math.min(outMin.y, wp.y));
            outMax.set(Math.max(outMax.x, wp.x), Math.max(outMax.y, wp.y));
        }
    }

    private static boolean handleHasX(Handle h) {
        return h == Handle.E || h == Handle.W || h == Handle.NE || h == Handle.SE || h == Handle.SW || h == Handle.NW;
    }

    private static boolean handleHasY(Handle h) {
        return h == Handle.N || h == Handle.S || h == Handle.NE || h == Handle.NW || h == Handle.SE || h == Handle.SW;
    }

    private static Vector2f oppositeAnchorLocalScaled(Handle h, float hx, float hy) {
        return switch (h) {
            case W -> new Vector2f(hx, 0);
            case E -> new Vector2f(-hx, 0);
            case N -> new Vector2f(0, hy);
            case S -> new Vector2f(0, -hy);
            case NW -> new Vector2f(hx, hy);
            case NE -> new Vector2f(-hx, hy);
            case SW -> new Vector2f(hx, -hy);
            case SE -> new Vector2f(-hx, -hy);
            default -> new Vector2f(0, 0);
        };
    }

    private static Vector2f oppositeAnchorWorldAabb(Handle h, Vector2f min, Vector2f max) {
        return switch (h) {
            case W -> new Vector2f(max.x, (min.y + max.y) * 0.5f);
            case E -> new Vector2f(min.x, (min.y + max.y) * 0.5f);
            case N -> new Vector2f((min.x + max.x) * 0.5f, max.y);
            case S -> new Vector2f((min.x + max.x) * 0.5f, min.y);
            case NW -> new Vector2f(max.x, max.y);
            case NE -> new Vector2f(min.x, max.y);
            case SW -> new Vector2f(max.x, min.y);
            case SE -> new Vector2f(min.x, min.y);
            default -> new Vector2f((min.x + max.x) * 0.5f, (min.y + max.y) * 0.5f);
        };
    }

    private Vector2f[] aabbHandlePoints(Vector2f min, Vector2f max) {
        float cx = (min.x + max.x) * 0.5f;
        float cy = (min.y + max.y) * 0.5f;
        Vector2f nw = new Vector2f(min.x, min.y);
        Vector2f n = new Vector2f(cx, min.y);
        Vector2f ne = new Vector2f(max.x, min.y);
        Vector2f e = new Vector2f(max.x, cy);
        Vector2f se = new Vector2f(max.x, max.y);
        Vector2f s = new Vector2f(cx, max.y);
        Vector2f sw = new Vector2f(min.x, max.y);
        Vector2f w = new Vector2f(min.x, cy);
        float rotOff = 42.0f / Math.max(0.1f, zoom);
        Vector2f rot = new Vector2f(cx, min.y - rotOff);
        return new Vector2f[]{nw, n, ne, e, se, s, sw, w, rot};
    }

    private static float angleDeg(Vector2f v) {
        return (float) Math.toDegrees(Math.atan2(v.y, v.x));
    }

    private static Vector2f rotate(Vector2f v, float rotRad) {
        float c = (float) Math.cos(rotRad);
        float s = (float) Math.sin(rotRad);
        return new Vector2f(v.x * c - v.y * s, v.x * s + v.y * c);
    }

    private static Vector2f invRotate(Vector2f v, float rotRad) {
        float c = (float) Math.cos(rotRad);
        float s = (float) Math.sin(rotRad);
        return new Vector2f(v.x * c + v.y * s, -v.x * s + v.y * c);
    }

    private static final class Snapshot {
        final Vector2f pos;
        final Vector2f scale;
        final Vector2f size;
        final float rotDeg;

        Snapshot(CanvasObject obj) {
            pos = new Vector2f(obj.position());
            scale = new Vector2f(obj.scale());
            size = new Vector2f(obj.size());
            rotDeg = obj.rotation();
        }
    }

    private static final class TransformCommand implements com.miry.ui.undo.Command {
        private final String desc;
        private final List<CanvasObject> objects;
        private final List<Snapshot> before;
        private final List<Snapshot> after;

        private TransformCommand(String desc, List<CanvasObject> objects, List<Snapshot> before, List<Snapshot> after) {
            this.desc = desc;
            this.objects = objects;
            this.before = before;
            this.after = after;
        }

        static TransformCommand capture(String desc, Map<CanvasObject, Snapshot> beforeMap) {
            if (beforeMap == null || beforeMap.isEmpty()) return null;
            List<CanvasObject> objs = new ArrayList<>(beforeMap.keySet());
            List<Snapshot> before = new ArrayList<>(objs.size());
            List<Snapshot> after = new ArrayList<>(objs.size());
            for (CanvasObject o : objs) {
                before.add(beforeMap.get(o));
                after.add(new Snapshot(o));
            }
            return new TransformCommand(desc, objs, before, after);
        }

        boolean hasChanges() {
            for (int i = 0; i < objects.size(); i++) {
                Snapshot a = before.get(i);
                Snapshot b = after.get(i);
                if (!a.pos.equals(b.pos)) return true;
                if (!a.scale.equals(b.scale)) return true;
                if (Math.abs(a.rotDeg - b.rotDeg) > 1e-4f) return true;
            }
            return false;
        }

        @Override public void execute() { apply(after); }
        @Override public void undo() { apply(before); }

        private void apply(List<Snapshot> snaps) {
            for (int i = 0; i < objects.size(); i++) {
                CanvasObject o = objects.get(i);
                Snapshot s = snaps.get(i);
                o.setPosition(new Vector2f(s.pos));
                o.setScale(new Vector2f(s.scale));
                o.setRotation(s.rotDeg);
            }
        }

        @Override public String description() { return desc; }
    }

    private final class OrientedBox {
        final Vector2f[] corners = new Vector2f[4];
        final float rotRad;

        OrientedBox(CanvasObject obj) {
            Snapshot s = new Snapshot(obj);
            rotRad = (float) Math.toRadians(s.rotDeg);
            float hx = (s.size.x * s.scale.x) * 0.5f;
            float hy = (s.size.y * s.scale.y) * 0.5f;
            Vector2f[] local = new Vector2f[]{
                new Vector2f(-hx, -hy),
                new Vector2f(hx, -hy),
                new Vector2f(hx, hy),
                new Vector2f(-hx, hy)
            };
            for (int i = 0; i < 4; i++) {
                corners[i] = new Vector2f(s.pos).add(rotate(local[i], rotRad));
            }
        }

        Handle hitTest(float mx, float my, int x, int y, int w, int h, int handleSize) {
            int hs = handleSize / 2;
            Vector2f[] pts = handlePoints();
            Handle[] handles = new Handle[]{Handle.NW, Handle.N, Handle.NE, Handle.E, Handle.SE, Handle.S, Handle.SW, Handle.W, Handle.ROTATE};
            for (int i = 0; i < pts.length; i++) {
                Vector2f sp = new Vector2f();
                worldToScreen(pts[i].x, pts[i].y, w, h, sp);
                float sx = x + sp.x;
                float sy = y + sp.y;
                if (mx >= sx - hs && my >= sy - hs && mx < sx + hs && my < sy + hs) return handles[i];
            }
            return Handle.NONE;
        }

        void drawOutline(UiRenderer r, int x, int y, int w, int h, int argb, int thickness) {
            for (int i = 0; i < 4; i++) {
                Vector2f a = corners[i];
                Vector2f b = corners[(i + 1) & 3];
                Vector2f sa = new Vector2f();
                Vector2f sb = new Vector2f();
                worldToScreen(a.x, a.y, w, h, sa);
                worldToScreen(b.x, b.y, w, h, sb);
                r.drawLine(x + (int) sa.x, y + (int) sa.y, x + (int) sb.x, y + (int) sb.y, thickness, argb);
            }
        }

        void drawHandles(UiRenderer r, Theme theme, int x, int y, int w, int h, int handleSize) {
            Vector2f[] pts = handlePoints();
            Handle[] handles = new Handle[]{Handle.NW, Handle.N, Handle.NE, Handle.E, Handle.SE, Handle.S, Handle.SW, Handle.W, Handle.ROTATE};
            int border = Theme.toArgb(theme.widgetOutline);
            for (int i = 0; i < pts.length; i++) {
                Vector2f sp = new Vector2f();
                worldToScreen(pts[i].x, pts[i].y, w, h, sp);
                float hx = x + sp.x - handleSize / 2.0f;
                float hy = y + sp.y - handleSize / 2.0f;
                if (handles[i] == Handle.ROTATE) {
                    Vector2f topMid = new Vector2f(corners[0]).lerp(corners[1], 0.5f);
                    Vector2f topScreen = new Vector2f();
                    worldToScreen(topMid.x, topMid.y, w, h, topScreen);
                    r.drawLine(x + (int) topScreen.x, y + (int) topScreen.y, (int) (hx + handleSize / 2.0f), (int) (hy + handleSize / 2.0f), 2, border);
                    r.drawCircle(hx + handleSize / 2.0f, hy + handleSize / 2.0f, handleSize * 0.35f, 0xFFFFFFFF, theme.design.border_thin, border);
                } else {
                    r.drawRoundedRect(hx, hy, handleSize, handleSize, theme.design.radius_sm, 0xFFFFFFFF, theme.design.border_thin, border);
                }
            }
        }

        private Vector2f[] handlePoints() {
            Vector2f nw = corners[0];
            Vector2f ne = corners[1];
            Vector2f se = corners[2];
            Vector2f sw = corners[3];
            Vector2f n = new Vector2f(nw).lerp(ne, 0.5f);
            Vector2f e = new Vector2f(ne).lerp(se, 0.5f);
            Vector2f s = new Vector2f(sw).lerp(se, 0.5f);
            Vector2f w = new Vector2f(nw).lerp(sw, 0.5f);
            Vector2f up = rotate(new Vector2f(0, -1), rotRad);
            float off = 42.0f / Math.max(0.1f, zoom);
            Vector2f rot = new Vector2f(n).add(up.mul(off));
            return new Vector2f[]{nw, n, ne, e, se, s, sw, w, rot};
        }
    }

    private void renderGrid(UiRenderer r, Theme theme, int x, int y, int width, int height) {
        int gridColor = 0x22FFFFFF;
        int primaryColor = 0x44FFFFFF;
        float worldX0 = -panOffset.x / zoom;
        float worldY0 = -panOffset.y / zoom;
        float worldX1 = worldX0 + width / zoom;
        float worldY1 = worldY0 + height / zoom;

        int startX = (int)Math.floor(worldX0 / gridSize);
        int endX = (int)Math.ceil(worldX1 / gridSize);
        int startY = (int)Math.floor(worldY0 / gridSize);
        int endY = (int)Math.ceil(worldY1 / gridSize);

        // Regular grid lines
        for (int gx = startX; gx <= endX; gx++) {
            float wx = gx * gridSize;
            Vector2f screenPos = new Vector2f();
            worldToScreen(wx, 0, width, height, screenPos);
            int sx = x + (int)screenPos.x;
            boolean isPrimary = (gx % primaryGridSteps == 0) && gx != 0;
            r.drawLine(sx, y, sx, y + height, isPrimary ? 2 : 1, isPrimary ? primaryColor : gridColor);
        }

        for (int gy = startY; gy <= endY; gy++) {
            float wy = gy * gridSize;
            Vector2f screenPos = new Vector2f();
            worldToScreen(0, wy, width, height, screenPos);
            int sy = y + (int)screenPos.y;
            boolean isPrimary = (gy % primaryGridSteps == 0) && gy != 0;
            r.drawLine(x, sy, x + width, sy, isPrimary ? 2 : 1, isPrimary ? primaryColor : gridColor);
        }

        // Origin axes (thicker and more visible)
        Vector2f origin = new Vector2f();
        worldToScreen(0, 0, width, height, origin);
        int ox = x + (int)origin.x;
        int oy = y + (int)origin.y;
        r.drawLine(ox, y, ox, y + height, 2, 0xAAFF0000);  // Red X-axis
        r.drawLine(x, oy, x + width, oy, 2, 0xAA00FF00);  // Green Y-axis
    }

    private void screenToWorld(float sx, float sy, int viewWidth, int viewHeight, Vector2f out) {
        out.set((sx - panOffset.x) / zoom, (sy - panOffset.y) / zoom);
    }

    private void worldToScreen(float wx, float wy, int viewWidth, int viewHeight, Vector2f out) {
        out.set(wx * zoom + panOffset.x, wy * zoom + panOffset.y);
    }
}
