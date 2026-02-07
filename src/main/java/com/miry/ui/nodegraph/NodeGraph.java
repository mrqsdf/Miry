package com.miry.ui.nodegraph;

import com.miry.platform.InputConstants;
import com.miry.ui.clipboard.Clipboard;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.undo.Command;
import com.miry.ui.undo.UndoStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Node graph editor widget.
 * <p>
 * Supports pan/zoom, selection, connecting pins (with type validation), rerouting, minimap,
 * copy/paste, and graph save/load.
 */
public final class NodeGraph {
    private final List<GraphNode> nodes = new ArrayList<>();
    private final List<NodeConnection> connections = new ArrayList<>();
    private float panX, panY;
    private float zoom = 1.0f;
    private int nextId = 1;

    private float lastMouseX;
    private float lastMouseY;
    private boolean hasLastMouse;

    private boolean panning;
    private boolean draggingNodes;
    private float dragStartWorldX;
    private float dragStartWorldY;
    private final List<DragNode> dragNodes = new ArrayList<>();

    private NodePin linkStartPin;
    private float linkMouseX;
    private float linkMouseY;

    private NodePin hoveredPin;
    private GraphNode hoveredNode;
    private NodeConnection hoveredConnection;
    private NodeConnection selectedConnection;

    private NodeConnection pendingRerouteConnection;
    private float pendingReroutePressX;
    private float pendingReroutePressY;
    private NodeConnection rerouteOriginalConnection;

    private boolean boxSelecting;
    private float boxStartX;
    private float boxStartY;
    private float boxEndX;
    private float boxEndY;

    private boolean snapToGrid = true;
    private float gridStep = 32.0f;

    private boolean minimapDragging;
    private Minimap minimap;

    private int lastViewX;
    private int lastViewY;
    private int lastViewW;
    private int lastViewH;

    private UndoStack undo;
    private Clipboard clipboard;

    public int allocateId() {
        return nextId++;
    }

    public String serializeGraph() {
        StringBuilder sb = new StringBuilder();
        sb.append("FLUX_NODEGRAPH_V1\n");
        sb.append("META\t").append(panX).append('\t').append(panY).append('\t').append(zoom).append('\n');

        for (GraphNode n : nodes) {
            sb.append("NODE\t")
                .append(n.id()).append('\t')
                .append(escape(n.title())).append('\t')
                .append(n.x()).append('\t')
                .append(n.y())
                .append('\n');
            for (NodePin p : n.inputs()) {
                sb.append("PIN\t")
                    .append(n.id()).append('\t')
                    .append(p.id()).append('\t')
                    .append("IN").append('\t')
                    .append(escape(p.name())).append('\t')
                    .append(escape(p.dataType()))
                    .append('\n');
            }
            for (NodePin p : n.outputs()) {
                sb.append("PIN\t")
                    .append(n.id()).append('\t')
                    .append(p.id()).append('\t')
                    .append("OUT").append('\t')
                    .append(escape(p.name())).append('\t')
                    .append(escape(p.dataType()))
                    .append('\n');
            }
        }

        for (NodeConnection c : connections) {
            sb.append("CONN\t")
                .append(c.id()).append('\t')
                .append(c.from().id()).append('\t')
                .append(c.to().id())
                .append('\n');
        }

        return sb.toString();
    }

    public boolean loadGraph(String text) {
        if (text == null || !text.startsWith("FLUX_NODEGRAPH_V1")) {
            return false;
        }

        List<GraphNode> newNodes = new ArrayList<>();
        List<NodeConnection> newConnections = new ArrayList<>();
        Map<Integer, GraphNode> nodeById = new HashMap<>();
        Map<Integer, NodePin> pinById = new HashMap<>();

        float newPanX = 0.0f;
        float newPanY = 0.0f;
        float newZoom = 1.0f;

        int maxId = 1;

        String[] lines = text.split("\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\t");
            if (parts.length == 0) {
                continue;
            }
            switch (parts[0]) {
                case "META" -> {
                    if (parts.length >= 4) {
                        newPanX = parseFloatOr(parts[1], 0.0f);
                        newPanY = parseFloatOr(parts[2], 0.0f);
                        newZoom = parseFloatOr(parts[3], 1.0f);
                    }
                }
                case "NODE" -> {
                    if (parts.length < 5) {
                        continue;
                    }
                    int id = parseIntOr(parts[1], -1);
                    if (id < 0) {
                        continue;
                    }
                    String title = unescape(parts[2]);
                    float x = parseFloatOr(parts[3], 0.0f);
                    float y = parseFloatOr(parts[4], 0.0f);
                    GraphNode node = new GraphNode(id, title, x, y);
                    newNodes.add(node);
                    nodeById.put(id, node);
                    maxId = Math.max(maxId, id + 1);
                }
                case "PIN" -> {
                    if (parts.length < 6) {
                        continue;
                    }
                    int nodeId = parseIntOr(parts[1], -1);
                    int pinId = parseIntOr(parts[2], -1);
                    if (nodeId < 0 || pinId < 0) {
                        continue;
                    }
                    GraphNode node = nodeById.get(nodeId);
                    if (node == null) {
                        continue;
                    }
                    String dir = parts[3];
                    NodePin.PinType type = "OUT".equals(dir) ? NodePin.PinType.OUTPUT : NodePin.PinType.INPUT;
                    String name = unescape(parts[4]);
                    String dataType = unescape(parts[5]);
                    NodePin pin = new NodePin(pinId, name, type, dataType);
                    if (type == NodePin.PinType.INPUT) {
                        node.addInput(pin);
                    } else {
                        node.addOutput(pin);
                    }
                    pinById.put(pinId, pin);
                    maxId = Math.max(maxId, pinId + 1);
                }
                case "CONN" -> {
                    if (parts.length < 4) {
                        continue;
                    }
                    int id = parseIntOr(parts[1], -1);
                    int fromId = parseIntOr(parts[2], -1);
                    int toId = parseIntOr(parts[3], -1);
                    if (id < 0 || fromId < 0 || toId < 0) {
                        continue;
                    }
                    NodePin a = pinById.get(fromId);
                    NodePin b = pinById.get(toId);
                    if (a == null || b == null) {
                        continue;
                    }
                    NodePin out = a.type() == NodePin.PinType.OUTPUT ? a : b;
                    NodePin in = a.type() == NodePin.PinType.INPUT ? a : b;
                    if (!typesCompatible(out.dataType(), in.dataType())) {
                        continue;
                    }
                    newConnections.add(new NodeConnection(id, out, in));
                    maxId = Math.max(maxId, id + 1);
                }
            }
        }

        nodes.clear();
        nodes.addAll(newNodes);
        connections.clear();
        connections.addAll(newConnections);
        clearSelection();
        selectedConnection = null;
        linkStartPin = null;
        rerouteOriginalConnection = null;
        pendingRerouteConnection = null;
        boxSelecting = false;
        draggingNodes = false;
        dragNodes.clear();
        panning = false;
        minimapDragging = false;

        panX = newPanX;
        panY = newPanY;
        setZoom(newZoom);

        nextId = Math.max(nextId, maxId);
        return true;
    }

    public GraphNode addNode(String title, float x, float y) {
        GraphNode node = new GraphNode(nextId++, title, x, y);
        nodes.add(node);
        return node;
    }

    public void removeNode(GraphNode node) {
        if (node == null) {
            return;
        }
        List<NodePin> pins = new ArrayList<>(node.inputs().size() + node.outputs().size());
        pins.addAll(node.inputs());
        pins.addAll(node.outputs());
        connections.removeIf(c -> pins.contains(c.from()) || pins.contains(c.to()));
        nodes.remove(node);
    }

    public NodeConnection addConnection(NodePin from, NodePin to) {
        NodeConnection conn = new NodeConnection(nextId++, from, to);
        connections.add(conn);
        return conn;
    }

    public void removeConnection(NodeConnection conn) {
        connections.remove(conn);
    }

    public void pan(float dx, float dy) {
        panX += dx;
        panY += dy;
    }

    public void setZoom(float z) {
        zoom = Math.max(0.35f, Math.min(2.75f, z));
    }

    public void setUndoStack(UndoStack undo) {
        this.undo = undo;
    }

    public void setClipboard(Clipboard clipboard) {
        this.clipboard = clipboard;
    }

    public boolean handleKey(KeyEvent event) {
        if (event == null || !event.isPressOrRepeat()) {
            return false;
        }

        if (event.hasCtrl()) {
            if (event.key() == InputConstants.KEY_Z) {
                if (undo != null) {
                    undo.undo();
                }
                return true;
            }
            if (event.key() == InputConstants.KEY_Y) {
                if (undo != null) {
                    undo.redo();
                }
                return true;
            }
            if (event.key() == InputConstants.KEY_C) {
                copySelectionToClipboard();
                return true;
            }
            if (event.key() == InputConstants.KEY_V) {
                pasteFromClipboardAtMouse();
                return true;
            }
        }

        if (event.key() == InputConstants.KEY_DELETE) {
            deleteSelection();
            return true;
        }
        if (event.key() == InputConstants.KEY_G) {
            snapToGrid = !snapToGrid;
            return true;
        }
        if (event.key() == InputConstants.KEY_F) {
            frameSelectionOrAll();
            return true;
        }

        return false;
    }

    public void update(UiInput input,
                       boolean middleDown,
                       boolean spaceDown,
                       boolean ctrlDown,
                       boolean shiftDown,
                       int viewX,
                       int viewY,
                       int viewWidth,
                       int viewHeight) {
        if (input == null) {
            return;
        }

        lastViewX = viewX;
        lastViewY = viewY;
        lastViewW = viewWidth;
        lastViewH = viewHeight;

        float mx = input.mousePos().x;
        float my = input.mousePos().y;
        linkMouseX = mx;
        linkMouseY = my;

        boolean inside = pxInside(mx, my, viewX, viewY, viewWidth, viewHeight);

        if (!hasLastMouse) {
            lastMouseX = mx;
            lastMouseY = my;
            hasLastMouse = true;
        }

        float dx = mx - lastMouseX;
        float dy = my - lastMouseY;
        boolean pressed = input.mousePressed();
        boolean down = input.mouseDown();
        boolean released = input.mouseReleased();

        // Zoom around cursor.
        if (inside && input.scrollY() != 0.0) {
            float oldZoom = zoom;
            float worldX = screenToWorldX(mx, viewX, oldZoom);
            float worldY = screenToWorldY(my, viewY, oldZoom);

            float factor = (float) Math.pow(1.12, input.scrollY());
            setZoom(oldZoom * factor);

            panX = (mx - viewX) / zoom - worldX;
            panY = (my - viewY) / zoom - worldY;
        }

        minimap = computeMinimap(viewX, viewY, viewWidth, viewHeight);
        if (pressed && inside && minimap != null && minimap.hit(mx, my)) {
            minimapDragging = true;
            panning = false;
            draggingNodes = false;
            dragNodes.clear();
            boxSelecting = false;
            linkStartPin = null;
            pendingRerouteConnection = null;
            selectedConnection = null;
        }
        if (minimapDragging && down && minimap != null) {
            minimap.panTo(mx, my, viewWidth, viewHeight, zoom, this);
        }

        boolean wantPan = inside && (middleDown || (spaceDown && down));
        if (pressed && wantPan) {
            panning = true;
            draggingNodes = false;
            dragNodes.clear();
            pendingRerouteConnection = null;
            boxSelecting = false;
            linkStartPin = null;
            if (rerouteOriginalConnection != null && !connections.contains(rerouteOriginalConnection)) {
                connections.add(rerouteOriginalConnection);
            }
            rerouteOriginalConnection = null;
        }
        if (panning) {
            if (!wantPan) {
                panning = false;
            } else {
                pan(dx / Math.max(0.1f, zoom), dy / Math.max(0.1f, zoom));
            }
        }

        // Update pin positions for hit testing.
        for (GraphNode node : nodes) {
            layoutNode(node, viewX, viewY);
        }

        hoveredPin = inside ? hitTestPin(mx, my) : null;
        hoveredNode = (inside && hoveredPin == null) ? hitTestNode(mx, my, viewX, viewY) : null;
        hoveredConnection = (inside && hoveredPin == null && hoveredNode == null && !panning && !boxSelecting)
            ? hitTestConnection(mx, my)
            : null;

        if (pressed && inside && !panning && !spaceDown && !minimapDragging) {
            if (hoveredPin != null) {
                selectedConnection = null;
                pendingRerouteConnection = null;
                boxSelecting = false;
                linkStartPin = hoveredPin;
            } else if (hoveredConnection != null) {
                selectedConnection = hoveredConnection;
                pendingRerouteConnection = hoveredConnection;
                pendingReroutePressX = mx;
                pendingReroutePressY = my;
                boxSelecting = false;
                draggingNodes = false;
                dragNodes.clear();
            } else if (hoveredNode != null) {
                selectedConnection = null;
                pendingRerouteConnection = null;
                boxSelecting = false;
                beginNodeDrag(hoveredNode, mx, my, viewX, viewY, ctrlDown, shiftDown);
            } else {
                selectedConnection = null;
                pendingRerouteConnection = null;
                draggingNodes = false;
                dragNodes.clear();
                boxSelecting = true;
                boxStartX = mx;
                boxStartY = my;
                boxEndX = mx;
                boxEndY = my;
                if (!shiftDown && !ctrlDown) {
                    clearSelection();
                }
            }
        }

        if (pendingRerouteConnection != null && down) {
            float dragDist = Math.abs(mx - pendingReroutePressX) + Math.abs(my - pendingReroutePressY);
            if (dragDist >= 6.0f) {
                rerouteOriginalConnection = pendingRerouteConnection;
                connections.remove(rerouteOriginalConnection);
                float dFrom = dist2(pendingReroutePressX, pendingReroutePressY, rerouteOriginalConnection.from().x(), rerouteOriginalConnection.from().y());
                float dTo = dist2(pendingReroutePressX, pendingReroutePressY, rerouteOriginalConnection.to().x(), rerouteOriginalConnection.to().y());
                // Click near the input end -> keep output fixed (start from output) and re-pick an input.
                linkStartPin = (dTo < dFrom) ? rerouteOriginalConnection.from() : rerouteOriginalConnection.to();
                selectedConnection = null;
                pendingRerouteConnection = null;
            }
        }

        if (boxSelecting) {
            if (down) {
                boxEndX = mx;
                boxEndY = my;
            }
            if (released) {
                finishBoxSelect(ctrlDown, shiftDown, viewX, viewY);
                boxSelecting = false;
            }
        }

        if (draggingNodes) {
            if (down) {
                float wx = screenToWorldX(mx, viewX, zoom);
                float wy = screenToWorldY(my, viewY, zoom);
                float deltaX = wx - dragStartWorldX;
                float deltaY = wy - dragStartWorldY;
                if (snapToGrid) {
                    float step = Math.max(1.0f, gridStep);
                    deltaX = Math.round(deltaX / step) * step;
                    deltaY = Math.round(deltaY / step) * step;
                }
                for (DragNode dn : dragNodes) {
                    dn.node.setPosition(dn.startX + deltaX, dn.startY + deltaY);
                }
            }
            if (released) {
                commitDragUndo();
                draggingNodes = false;
                dragNodes.clear();
            }
        }

        if (linkStartPin != null && released) {
            if (hoveredPin != null && hoveredPin != linkStartPin && hoveredPin.type() != linkStartPin.type()) {
                NodePin out = linkStartPin.type() == NodePin.PinType.OUTPUT ? linkStartPin : hoveredPin;
                NodePin in = linkStartPin.type() == NodePin.PinType.INPUT ? linkStartPin : hoveredPin;

                if (canConnect(out, in)) {
                    connectPins(out, in, rerouteOriginalConnection);
                    rerouteOriginalConnection = null;
                } else if (rerouteOriginalConnection != null) {
                    // Cancel reroute if the drop was invalid.
                    if (!connections.contains(rerouteOriginalConnection)) {
                        connections.add(rerouteOriginalConnection);
                    }
                    rerouteOriginalConnection = null;
                }
            } else if (rerouteOriginalConnection != null) {
                // Cancel reroute if no valid drop target.
                if (!connections.contains(rerouteOriginalConnection)) {
                    connections.add(rerouteOriginalConnection);
                }
                rerouteOriginalConnection = null;
            }
            linkStartPin = null;
        }

        if (released) {
            panning = false;
            minimapDragging = false;
            pendingRerouteConnection = null;
        }

        lastMouseX = mx;
        lastMouseY = my;
    }

    public void render(UiRenderer r, int viewX, int viewY, int viewWidth, int viewHeight) {
        r.flush();
        r.pushClip(viewX, viewY, viewWidth, viewHeight);

        renderGrid(r, viewX, viewY, viewWidth, viewHeight);

        for (GraphNode node : nodes) {
            layoutNode(node, viewX, viewY);
        }

        for (NodeConnection conn : connections) {
            boolean hoveredConn = conn == hoveredConnection;
            boolean selectedConn = conn == selectedConnection;

            int base = mixArgb(pinColor(conn.from()), 0xFF000000, 0.25f);
            int c = selectedConn ? 0xFFFFFFFF : (hoveredConn ? mixArgb(base, 0xFFFFFFFF, 0.45f) : base);
            float thick = selectedConn ? 1.8f : (hoveredConn ? 1.35f : 1.0f);
            renderConnection(r, conn, c, thick);
        }

        if (linkStartPin != null) {
            float x0 = linkStartPin.x();
            float y0 = linkStartPin.y();
            float x1 = linkMouseX;
            float y1 = linkMouseY;
            int c = pinColor(linkStartPin);
            renderConnection(r, x0, y0, x1, y1, c, 1.25f);
        }

        for (GraphNode node : nodes) {
            renderNode(r, node, viewX, viewY);
        }

        if (boxSelecting) {
            float x0 = Math.min(boxStartX, boxEndX);
            float y0 = Math.min(boxStartY, boxEndY);
            float x1 = Math.max(boxStartX, boxEndX);
            float y1 = Math.max(boxStartY, boxEndY);
            float w = Math.max(0.0f, x1 - x0);
            float h = Math.max(0.0f, y1 - y0);
            r.drawRect(x0, y0, w, h, 0x1A4C9AFF);
            r.drawRect(x0, y0, w, 1, 0xFF4C9AFF);
            r.drawRect(x0, y0 + h - 1, w, 1, 0xFF4C9AFF);
            r.drawRect(x0, y0, 1, h, 0xFF4C9AFF);
            r.drawRect(x0 + w - 1, y0, 1, h, 0xFF4C9AFF);
        }

        renderMinimap(r, viewX, viewY, viewWidth, viewHeight);

        r.popClip();
    }

    private void layoutNode(GraphNode node, int viewX, int viewY) {
        float sx = viewX + (node.x() + panX) * zoom;
        float sy = viewY + (node.y() + panY) * zoom;
        float sw = node.width() * zoom;
        float sh = node.height() * zoom;

        float headerH = Math.max(22.0f, 28.0f * zoom);
        float pinStartY = sy + headerH + Math.max(14.0f, 18.0f * zoom);
        float pinStepY = Math.max(18.0f, 22.0f * zoom);
        float pinInset = Math.max(6.0f, 10.0f * zoom);
        float inX = sx + pinInset;
        float outX = sx + sw - pinInset;

        for (int i = 0; i < node.inputs().size(); i++) {
            NodePin pin = node.inputs().get(i);
            pin.setPosition(inX, pinStartY + i * pinStepY);
        }
        for (int i = 0; i < node.outputs().size(); i++) {
            NodePin pin = node.outputs().get(i);
            pin.setPosition(outX, pinStartY + i * pinStepY);
        }
    }

    private void renderNode(UiRenderer r, GraphNode node, int viewX, int viewY) {
        float sx = viewX + (node.x() + panX) * zoom;
        float sy = viewY + (node.y() + panY) * zoom;
        float sw = node.width() * zoom;
        float sh = node.height() * zoom;

        boolean hovered = node == hoveredNode;
        int shadow = 0x2A000000;
        r.drawRect(sx + 3, sy + 3, sw, sh, shadow);

        int bgColor = node.selected() ? 0xFF2F2F38 : 0xFF26262D;
        int outline = node.selected() ? 0xFF4C9AFF : (hovered ? 0xFF5A5A66 : 0xFF3A3A42);
        float headerH = Math.max(22.0f, 28.0f * zoom);

        r.drawRect(sx, sy, sw, sh, bgColor);
        r.drawRect(sx, sy, sw, headerH, 0xFF2B2B34);
        r.drawRect(sx, sy, sw, 1, outline);
        r.drawRect(sx, sy + sh - 1, sw, 1, outline);
        r.drawRect(sx, sy, 1, sh, outline);
        r.drawRect(sx + sw - 1, sy, 1, sh, outline);

        float titleBaseline = r.baselineForBox(sy, headerH);
        r.drawText(node.title(), sx + 10, titleBaseline, 0xFFE6E6F0);

        float pinSize = clamp(8.0f, 12.0f * zoom, 14.0f);
        float half = pinSize * 0.5f;
        float rowH = Math.max(18.0f, 22.0f * zoom);
        for (NodePin pin : node.inputs()) {
            int base = pinColor(pin);
            int c = pin == hoveredPin ? 0xFFFFFFFF : base;
            r.drawRect(pin.x() - half - 1, pin.y() - half - 1, pinSize + 2, pinSize + 2, 0xFF0D0D10);
            r.drawRect(pin.x() - half, pin.y() - half, pinSize, pinSize, c);

            float top = pin.y() - rowH * 0.5f;
            float baseline = r.baselineForBox(top, rowH);
            r.drawText(pin.name(), pin.x() + half + 6, baseline, 0xFFD0D0DA);
        }

        for (NodePin pin : node.outputs()) {
            int base = pinColor(pin);
            int c = pin == hoveredPin ? 0xFFFFFFFF : base;
            r.drawRect(pin.x() - half - 1, pin.y() - half - 1, pinSize + 2, pinSize + 2, 0xFF0D0D10);
            r.drawRect(pin.x() - half, pin.y() - half, pinSize, pinSize, c);

            float top = pin.y() - rowH * 0.5f;
            float baseline = r.baselineForBox(top, rowH);
            float textW = r.measureText(pin.name());
            r.drawText(pin.name(), pin.x() - half - 6 - textW, baseline, 0xFFD0D0DA);
        }

        if (node.previewTexture() != null && node.previewHeight() > 0) {
            float pad = Math.max(6.0f, 10.0f * zoom);
            float previewH = node.previewHeight() * zoom;
            float px = sx + pad;
            float py = sy + sh - previewH - pad;
            float pw = Math.max(1.0f, sw - pad * 2.0f);
            float ph = Math.max(1.0f, previewH);
            r.drawRect(px, py, pw, ph, 0xFF15151A);
            r.drawTexturedRect(node.previewTexture(), px, py, pw, ph, 0xFFFFFFFF);
            r.drawRect(px, py, pw, 1, 0x663A3A42);
            r.drawRect(px, py + ph - 1, pw, 1, 0x663A3A42);
            r.drawRect(px, py, 1, ph, 0x663A3A42);
            r.drawRect(px + pw - 1, py, 1, ph, 0x663A3A42);
        }
    }

    private void renderConnection(UiRenderer r, NodeConnection conn, int color, float thicknessMul) {
        renderConnection(r, conn.from().x(), conn.from().y(), conn.to().x(), conn.to().y(), color, thicknessMul);
    }

    private void renderConnection(UiRenderer r, float x0, float y0, float x1, float y1, int color, float thicknessMul) {
        float dist = Math.max(60.0f * zoom, Math.abs(x1 - x0) * 0.5f);
        float baseThickness = clamp(2.0f, 3.0f * zoom, 6.0f) * Math.max(0.5f, thicknessMul);
        renderBezier(r, x0, y0, x0 + dist, y0, x1 - dist, y1, x1, y1, baseThickness, color);
    }

    private void renderBezier(UiRenderer r, float x0, float y0, float cx0, float cy0, float cx1, float cy1, float x1, float y1, float thickness, int color) {
        int segments = 34;
        float half = thickness * 0.5f;
        float[] prev = null;
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float[] p = bezier(x0, y0, cx0, cy0, cx1, cy1, x1, y1, t);
            r.drawRect(p[0] - half, p[1] - half, thickness, thickness, color);
            if (prev != null) {
                float mx = (prev[0] + p[0]) * 0.5f;
                float my = (prev[1] + p[1]) * 0.5f;
                r.drawRect(mx - half, my - half, thickness, thickness, color);
            }
            prev = p;
        }
    }

    private float[] bezier(float x0, float y0, float cx0, float cy0, float cx1, float cy1, float x1, float y1, float t) {
        float u = 1.0f - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;

        float x = uuu * x0 + 3 * uu * t * cx0 + 3 * u * tt * cx1 + ttt * x1;
        float y = uuu * y0 + 3 * uu * t * cy0 + 3 * u * tt * cy1 + ttt * y1;
        return new float[]{x, y};
    }

    private void renderGrid(UiRenderer r, int viewX, int viewY, int viewW, int viewH) {
        float minor = 32.0f;
        float minorPx = minor * zoom;
        while (minorPx < 12.0f) {
            minor *= 2.0f;
            minorPx *= 2.0f;
        }
        while (minorPx > 90.0f) {
            minor *= 0.5f;
            minorPx *= 0.5f;
        }

        float major = minor * 4.0f;

        float worldMinX = -panX;
        float worldMaxX = viewW / zoom - panX;
        float worldMinY = -panY;
        float worldMaxY = viewH / zoom - panY;

        int minorColor = 0x1AFFFFFF;
        int majorColor = 0x2AFFFFFF;

        float startX = (float) (Math.floor(worldMinX / minor) * minor);
        for (float x = startX; x <= worldMaxX; x += minor) {
            float sx = viewX + (x + panX) * zoom;
            int ix = Math.round(sx);
            boolean isMajor = almostMultiple(x, major);
            r.drawRect(ix, viewY, 1, viewH, isMajor ? majorColor : minorColor);
        }

        float startY = (float) (Math.floor(worldMinY / minor) * minor);
        for (float y = startY; y <= worldMaxY; y += minor) {
            float sy = viewY + (y + panY) * zoom;
            int iy = Math.round(sy);
            boolean isMajor = almostMultiple(y, major);
            r.drawRect(viewX, iy, viewW, 1, isMajor ? majorColor : minorColor);
        }

        // Origin cross (world 0,0)
        float ox = viewX + (0.0f + panX) * zoom;
        float oy = viewY + (0.0f + panY) * zoom;
        r.drawRect(Math.round(ox) - 1, viewY, 2, viewH, 0x224C9AFF);
        r.drawRect(viewX, Math.round(oy) - 1, viewW, 2, 0x224C9AFF);
    }

    private NodePin hitTestPin(float mx, float my) {
        float r = clamp(6.0f, 10.0f * zoom, 14.0f);
        float rr = r * r;
        // Iterate front-to-back so pins on top nodes win.
        for (int ni = nodes.size() - 1; ni >= 0; ni--) {
            GraphNode node = nodes.get(ni);
            for (NodePin pin : node.inputs()) {
                if (dist2(mx, my, pin.x(), pin.y()) <= rr) {
                    return pin;
                }
            }
            for (NodePin pin : node.outputs()) {
                if (dist2(mx, my, pin.x(), pin.y()) <= rr) {
                    return pin;
                }
            }
        }
        return null;
    }

    private GraphNode hitTestNode(float mx, float my, int viewX, int viewY) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            GraphNode node = nodes.get(i);
            float sx = viewX + (node.x() + panX) * zoom;
            float sy = viewY + (node.y() + panY) * zoom;
            float sw = node.width() * zoom;
            float sh = node.height() * zoom;
            if (mx >= sx && my >= sy && mx < sx + sw && my < sy + sh) {
                return node;
            }
        }
        return null;
    }

    private NodeConnection hitTestConnection(float mx, float my) {
        float threshold = clamp(6.0f, 7.5f * zoom, 14.0f);
        float thr2 = threshold * threshold;
        int segments = 24;
        for (int i = connections.size() - 1; i >= 0; i--) {
            NodeConnection conn = connections.get(i);
            float x0 = conn.from().x();
            float y0 = conn.from().y();
            float x1 = conn.to().x();
            float y1 = conn.to().y();

            float dist = Math.max(60.0f * zoom, Math.abs(x1 - x0) * 0.5f);
            float cx0 = x0 + dist;
            float cy0 = y0;
            float cx1 = x1 - dist;
            float cy1 = y1;

            float min = Float.MAX_VALUE;
            for (int s = 0; s <= segments; s++) {
                float t = s / (float) segments;
                float[] p = bezier(x0, y0, cx0, cy0, cx1, cy1, x1, y1, t);
                float d = dist2(mx, my, p[0], p[1]);
                if (d < min) {
                    min = d;
                    if (min <= thr2) {
                        return conn;
                    }
                }
            }
        }
        return null;
    }

    private void finishBoxSelect(boolean ctrlDown, boolean shiftDown, int viewX, int viewY) {
        float x0 = Math.min(boxStartX, boxEndX);
        float y0 = Math.min(boxStartY, boxEndY);
        float x1 = Math.max(boxStartX, boxEndX);
        float y1 = Math.max(boxStartY, boxEndY);

        if ((x1 - x0) < 3.0f && (y1 - y0) < 3.0f) {
            return;
        }

        for (GraphNode node : nodes) {
            float sx = viewX + (node.x() + panX) * zoom;
            float sy = viewY + (node.y() + panY) * zoom;
            float sw = node.width() * zoom;
            float sh = node.height() * zoom;

            boolean hit = rectsIntersect(x0, y0, x1 - x0, y1 - y0, sx, sy, sw, sh);
            if (!hit) {
                continue;
            }
            if (ctrlDown) {
                node.setSelected(!node.selected());
            } else {
                node.setSelected(true);
            }
        }

        if (!shiftDown && !ctrlDown) {
            bringSelectionToFront();
        }
    }

    private void bringSelectionToFront() {
        if (nodes.isEmpty()) {
            return;
        }

        List<GraphNode> unselected = new ArrayList<>(nodes.size());
        List<GraphNode> selected = new ArrayList<>();
        for (GraphNode n : nodes) {
            if (n.selected()) {
                selected.add(n);
            } else {
                unselected.add(n);
            }
        }

        nodes.clear();
        nodes.addAll(unselected);
        nodes.addAll(selected);
    }

    private Minimap computeMinimap(int viewX, int viewY, int viewW, int viewH) {
        if (nodes.isEmpty()) {
            return null;
        }

        float mmW = 180.0f;
        float mmH = 120.0f;
        float margin = 12.0f;

        float x = viewX + viewW - mmW - margin;
        float y = viewY + viewH - mmH - margin;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (GraphNode n : nodes) {
            minX = Math.min(minX, n.x());
            minY = Math.min(minY, n.y());
            maxX = Math.max(maxX, n.x() + n.width());
            maxY = Math.max(maxY, n.y() + n.height());
        }

        float pad = 80.0f;
        minX -= pad;
        minY -= pad;
        maxX += pad;
        maxY += pad;

        float worldW = Math.max(1.0f, maxX - minX);
        float worldH = Math.max(1.0f, maxY - minY);

        float scale = Math.min(mmW / worldW, mmH / worldH);
        float contentW = worldW * scale;
        float contentH = worldH * scale;
        float contentX = x + (mmW - contentW) * 0.5f;
        float contentY = y + (mmH - contentH) * 0.5f;

        return new Minimap(x, y, mmW, mmH, contentX, contentY, contentW, contentH, minX, minY, worldW, worldH, scale);
    }

    private void renderMinimap(UiRenderer r, int viewX, int viewY, int viewW, int viewH) {
        Minimap mm = computeMinimap(viewX, viewY, viewW, viewH);
        minimap = mm;
        if (mm == null) {
            return;
        }

        int bg = 0xCC1E1E24;
        int outline = 0xFF3A3A42;
        r.drawRect(mm.x(), mm.y(), mm.w(), mm.h(), bg);
        r.drawRect(mm.x(), mm.y(), mm.w(), 1, outline);
        r.drawRect(mm.x(), mm.y() + mm.h() - 1, mm.w(), 1, outline);
        r.drawRect(mm.x(), mm.y(), 1, mm.h(), outline);
        r.drawRect(mm.x() + mm.w() - 1, mm.y(), 1, mm.h(), outline);

        for (GraphNode n : nodes) {
            float nx = mm.worldToMiniX(n.x());
            float ny = mm.worldToMiniY(n.y());
            float nw = n.width() * mm.scale();
            float nh = n.height() * mm.scale();
            int c = n.selected() ? 0xCC4C9AFF : 0x883A3A42;
            r.drawRect(nx, ny, Math.max(1.0f, nw), Math.max(1.0f, nh), c);
        }

        float worldViewX = -panX;
        float worldViewY = -panY;
        float worldViewW = viewW / Math.max(0.1f, zoom);
        float worldViewH = viewH / Math.max(0.1f, zoom);

        float vx = mm.worldToMiniX(worldViewX);
        float vy = mm.worldToMiniY(worldViewY);
        float vw = worldViewW * mm.scale();
        float vh = worldViewH * mm.scale();
        int viewC = minimapDragging ? 0xFFFFFFFF : 0xFF4C9AFF;
        r.drawRect(vx, vy, vw, 1, viewC);
        r.drawRect(vx, vy + vh - 1, vw, 1, viewC);
        r.drawRect(vx, vy, 1, vh, viewC);
        r.drawRect(vx + vw - 1, vy, 1, vh, viewC);
    }

    private void commitDragUndo() {
        if (undo == null || dragNodes.isEmpty()) {
            return;
        }

        boolean changed = false;
        for (DragNode dn : dragNodes) {
            if (dn.node.x() != dn.startX || dn.node.y() != dn.startY) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            return;
        }

        int count = dragNodes.size();
        GraphNode[] moved = new GraphNode[count];
        float[] oldX = new float[count];
        float[] oldY = new float[count];
        float[] newX = new float[count];
        float[] newY = new float[count];

        for (int i = 0; i < count; i++) {
            DragNode dn = dragNodes.get(i);
            moved[i] = dn.node;
            oldX[i] = dn.startX;
            oldY[i] = dn.startY;
            newX[i] = dn.node.x();
            newY[i] = dn.node.y();
        }

        undo.execute(new Command() {
            @Override
            public void execute() {
                for (int i = 0; i < moved.length; i++) {
                    moved[i].setPosition(newX[i], newY[i]);
                }
            }

            @Override
            public void undo() {
                for (int i = 0; i < moved.length; i++) {
                    moved[i].setPosition(oldX[i], oldY[i]);
                }
            }

            @Override
            public String description() {
                return moved.length == 1 ? "Move node" : "Move nodes";
            }
        });
    }

    private boolean canConnect(NodePin out, NodePin in) {
        if (out == null || in == null) {
            return false;
        }
        if (out.type() != NodePin.PinType.OUTPUT || in.type() != NodePin.PinType.INPUT) {
            return false;
        }
        GraphNode outNode = nodeForPin(out);
        GraphNode inNode = nodeForPin(in);
        if (outNode == null || inNode == null || outNode == inNode) {
            return false;
        }
        return typesCompatible(out.dataType(), in.dataType());
    }

    private void connectPins(NodePin out, NodePin in, NodeConnection rerouteOld) {
        if (out == null || in == null) {
            return;
        }

        if (rerouteOld != null && rerouteOld.from() == out && rerouteOld.to() == in) {
            if (!connections.contains(rerouteOld)) {
                connections.add(rerouteOld);
            }
            return;
        }

        NodeConnection existing = findConnectionToInput(in);
        if (existing != null && existing.from() == out && existing.to() == in) {
            return;
        }

        int connId = nextId++;
        NodeConnection added = new NodeConnection(connId, out, in);
        NodeConnection replaced = existing;

        Command cmd = new Command() {
            @Override
            public void execute() {
                if (replaced != null) {
                    connections.remove(replaced);
                }
                if (rerouteOld != null && rerouteOld != replaced) {
                    connections.remove(rerouteOld);
                }
                connections.removeIf(c -> c.to() == in && c != added);
                if (!connections.contains(added)) {
                    connections.add(added);
                }
                selectedConnection = added;
            }

            @Override
            public void undo() {
                connections.remove(added);
                if (replaced != null && replaced != rerouteOld && !connections.contains(replaced)) {
                    connections.add(replaced);
                }
                if (rerouteOld != null && !connections.contains(rerouteOld)) {
                    connections.add(rerouteOld);
                }
                selectedConnection = null;
            }

            @Override
            public String description() {
                return "Connect pins";
            }
        };

        if (undo != null) {
            undo.execute(cmd);
        } else {
            cmd.execute();
        }
    }

    private NodeConnection findConnectionToInput(NodePin in) {
        for (NodeConnection c : connections) {
            if (c.to() == in) {
                return c;
            }
        }
        return null;
    }

    private static boolean typesCompatible(String outType, String inType) {
        String o = normalizeType(outType);
        String i = normalizeType(inType);
        if (o.equals("any") || i.equals("any")) {
            return true;
        }
        return o.equals(i);
    }

    private static String normalizeType(String type) {
        return type == null ? "any" : type.trim().toLowerCase();
    }

    private static boolean rectsIntersect(float ax, float ay, float aw, float ah, float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private void deleteSelection() {
        if (selectedConnection != null) {
            NodeConnection toRemove = selectedConnection;
            Command cmd = new Command() {
                @Override
                public void execute() {
                    connections.remove(toRemove);
                    selectedConnection = null;
                }

                @Override
                public void undo() {
                    if (!connections.contains(toRemove)) {
                        connections.add(toRemove);
                    }
                }

                @Override
                public String description() {
                    return "Delete link";
                }
            };
            if (undo != null) {
                undo.execute(cmd);
            } else {
                cmd.execute();
            }
            return;
        }

        List<GraphNode> removedNodes = new ArrayList<>();
        for (GraphNode n : nodes) {
            if (n.selected()) {
                removedNodes.add(n);
            }
        }
        if (removedNodes.isEmpty()) {
            return;
        }

        List<NodeConnection> removedConnections = new ArrayList<>();
        for (NodeConnection c : connections) {
            GraphNode a = nodeForPin(c.from());
            GraphNode b = nodeForPin(c.to());
            if (removedNodes.contains(a) || removedNodes.contains(b)) {
                removedConnections.add(c);
            }
        }

        int count = removedNodes.size();
        GraphNode[] nodesArr = removedNodes.toArray(new GraphNode[0]);
        int[] indices = new int[count];
        for (int i = 0; i < count; i++) {
            indices[i] = nodes.indexOf(nodesArr[i]);
        }
        NodeConnection[] connsArr = removedConnections.toArray(new NodeConnection[0]);

        Command cmd = new Command() {
            @Override
            public void execute() {
                for (NodeConnection c : connsArr) {
                    connections.remove(c);
                }
                for (GraphNode n : nodesArr) {
                    nodes.remove(n);
                }
                selectedConnection = null;
            }

            @Override
            public void undo() {
                // Restore nodes in original order.
                for (int i = 0; i < nodesArr.length; i++) {
                    int idx = Math.max(0, Math.min(indices[i], nodes.size()));
                    nodes.add(idx, nodesArr[i]);
                }
                for (NodeConnection c : connsArr) {
                    if (!connections.contains(c)) {
                        connections.add(c);
                    }
                }
            }

            @Override
            public String description() {
                return nodesArr.length == 1 ? "Delete node" : "Delete nodes";
            }
        };

        if (undo != null) {
            undo.execute(cmd);
        } else {
            cmd.execute();
        }
    }

    private void copySelectionToClipboard() {
        if (clipboard == null) {
            return;
        }

        List<GraphNode> selected = new ArrayList<>();
        for (GraphNode n : nodes) {
            if (n.selected()) {
                selected.add(n);
            }
        }
        if (selected.isEmpty()) {
            return;
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        for (GraphNode n : selected) {
            minX = Math.min(minX, n.x());
            minY = Math.min(minY, n.y());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("FLUX_NODEGRAPH_CLIP_V1\n");
        for (GraphNode n : selected) {
            sb.append("NODE\t")
                .append(n.id()).append('\t')
                .append(escape(n.title())).append('\t')
                .append(n.x() - minX).append('\t')
                .append(n.y() - minY)
                .append('\n');

            for (NodePin p : n.inputs()) {
                sb.append("PIN\t")
                    .append(n.id()).append('\t')
                    .append(p.id()).append('\t')
                    .append("IN").append('\t')
                    .append(escape(p.name())).append('\t')
                    .append(escape(p.dataType()))
                    .append('\n');
            }
            for (NodePin p : n.outputs()) {
                sb.append("PIN\t")
                    .append(n.id()).append('\t')
                    .append(p.id()).append('\t')
                    .append("OUT").append('\t')
                    .append(escape(p.name())).append('\t')
                    .append(escape(p.dataType()))
                    .append('\n');
            }
        }

        for (NodeConnection c : connections) {
            GraphNode a = nodeForPin(c.from());
            GraphNode b = nodeForPin(c.to());
            if (a != null && b != null && selected.contains(a) && selected.contains(b)) {
                sb.append("CONN\t")
                    .append(c.from().id()).append('\t')
                    .append(c.to().id())
                    .append('\n');
            }
        }

        clipboard.setText(sb.toString());
    }

    private void pasteFromClipboardAtMouse() {
        if (clipboard == null) {
            return;
        }
        String data = clipboard.getText();
        if (data == null || !data.startsWith("FLUX_NODEGRAPH_CLIP_V1")) {
            return;
        }

        float baseX = screenToWorldX(linkMouseX, lastViewX, zoom);
        float baseY = screenToWorldY(linkMouseY, lastViewY, zoom);

        PasteData parsed = PasteData.parse(data);
        if (parsed.nodes.isEmpty()) {
            return;
        }

        PasteCommand cmd = new PasteCommand(parsed, baseX, baseY);
        if (undo != null) {
            undo.execute(cmd);
        } else {
            cmd.execute();
        }
    }

    private void frameSelectionOrAll() {
        if (nodes.isEmpty() || lastViewW <= 0 || lastViewH <= 0) {
            return;
        }

        boolean anySelected = false;
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (GraphNode n : nodes) {
            if (n.selected()) {
                anySelected = true;
            }
        }

        for (GraphNode n : nodes) {
            if (anySelected && !n.selected()) {
                continue;
            }
            minX = Math.min(minX, n.x());
            minY = Math.min(minY, n.y());
            maxX = Math.max(maxX, n.x() + n.width());
            maxY = Math.max(maxY, n.y() + n.height());
        }

        float cx = (minX + maxX) * 0.5f;
        float cy = (minY + maxY) * 0.5f;
        panX = lastViewW / (2.0f * Math.max(0.1f, zoom)) - cx;
        panY = lastViewH / (2.0f * Math.max(0.1f, zoom)) - cy;
    }

    private static String escape(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replace("\\", "\\\\")
            .replace("\t", "\\t")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private static String unescape(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 't' -> out.append('\t');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case '\\' -> out.append('\\');
                    default -> out.append(n);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static int parseIntOr(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static float parseFloatOr(String s, float def) {
        try {
            return Float.parseFloat(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static final class PasteData {
        private final List<NodeClip> nodes = new ArrayList<>();
        private final List<PinClip> pins = new ArrayList<>();
        private final List<ConnClip> connections = new ArrayList<>();

        static PasteData parse(String text) {
            PasteData data = new PasteData();
            if (text == null || text.isEmpty()) {
                return data;
            }
            String[] lines = text.split("\n");
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\t");
                if (parts.length == 0) {
                    continue;
                }
                switch (parts[0]) {
                    case "NODE" -> {
                        if (parts.length < 5) {
                            continue;
                        }
                        int oldId = parseIntOr(parts[1], -1);
                        String title = unescape(parts[2]);
                        float dx = parseFloatOr(parts[3], 0.0f);
                        float dy = parseFloatOr(parts[4], 0.0f);
                        if (oldId >= 0) {
                            data.nodes.add(new NodeClip(oldId, title, dx, dy));
                        }
                    }
                    case "PIN" -> {
                        if (parts.length < 6) {
                            continue;
                        }
                        int oldNodeId = parseIntOr(parts[1], -1);
                        int oldPinId = parseIntOr(parts[2], -1);
                        String dir = parts[3];
                        NodePin.PinType type = "OUT".equals(dir) ? NodePin.PinType.OUTPUT : NodePin.PinType.INPUT;
                        String name = unescape(parts[4]);
                        String dataType = unescape(parts[5]);
                        if (oldNodeId >= 0 && oldPinId >= 0) {
                            data.pins.add(new PinClip(oldNodeId, oldPinId, type, name, dataType));
                        }
                    }
                    case "CONN" -> {
                        if (parts.length < 3) {
                            continue;
                        }
                        int from = parseIntOr(parts[1], -1);
                        int to = parseIntOr(parts[2], -1);
                        if (from >= 0 && to >= 0) {
                            data.connections.add(new ConnClip(from, to));
                        }
                    }
                }
            }
            return data;
        }
    }

    private record NodeClip(int oldId, String title, float dx, float dy) {}
    private record PinClip(int oldNodeId, int oldPinId, NodePin.PinType type, String name, String dataType) {}
    private record ConnClip(int fromPinId, int toPinId) {}

    private final class PasteCommand implements Command {
        private final GraphNode[] newNodes;
        private final NodeConnection[] newConnections;

        PasteCommand(PasteData data, float baseX, float baseY) {
            Map<Integer, GraphNode> nodeMap = new HashMap<>();
            Map<Integer, NodePin> pinMap = new HashMap<>();

            List<GraphNode> createdNodes = new ArrayList<>();
            for (NodeClip n : data.nodes) {
                int id = allocateId();
                GraphNode node = new GraphNode(id, n.title, baseX + n.dx, baseY + n.dy);
                createdNodes.add(node);
                nodeMap.put(n.oldId, node);
            }

            for (PinClip p : data.pins) {
                GraphNode node = nodeMap.get(p.oldNodeId);
                if (node == null) {
                    continue;
                }
                int id = allocateId();
                NodePin pin = new NodePin(id, p.name, p.type, p.dataType);
                if (p.type == NodePin.PinType.INPUT) {
                    node.addInput(pin);
                } else {
                    node.addOutput(pin);
                }
                pinMap.put(p.oldPinId, pin);
            }

            List<NodeConnection> createdConnections = new ArrayList<>();
            for (ConnClip c : data.connections) {
                NodePin from = pinMap.get(c.fromPinId);
                NodePin to = pinMap.get(c.toPinId);
                if (from == null || to == null) {
                    continue;
                }
                NodePin out = from.type() == NodePin.PinType.OUTPUT ? from : to;
                NodePin in = from.type() == NodePin.PinType.INPUT ? from : to;
                if (!canConnect(out, in)) {
                    continue;
                }
                int id = allocateId();
                createdConnections.add(new NodeConnection(id, out, in));
            }

            newNodes = createdNodes.toArray(new GraphNode[0]);
            newConnections = createdConnections.toArray(new NodeConnection[0]);
        }

        @Override
        public void execute() {
            clearSelection();
            for (GraphNode n : newNodes) {
                if (!nodes.contains(n)) {
                    nodes.add(n);
                }
                n.setSelected(true);
            }
            bringSelectionToFront();

            for (NodeConnection c : newConnections) {
                if (!connections.contains(c)) {
                    connections.add(c);
                }
            }
        }

        @Override
        public void undo() {
            for (NodeConnection c : newConnections) {
                connections.remove(c);
            }
            for (GraphNode n : newNodes) {
                nodes.remove(n);
            }
            selectedConnection = null;
        }

        @Override
        public String description() {
            return newNodes.length == 1 ? "Paste node" : "Paste nodes";
        }
    }

    private void beginNodeDrag(GraphNode node, float mx, float my, int viewX, int viewY, boolean ctrlDown, boolean shiftDown) {
        if (ctrlDown) {
            node.setSelected(!node.selected());
        } else if (shiftDown) {
            node.setSelected(true);
        } else {
            clearSelection();
            node.setSelected(true);
        }

        if (!node.selected()) {
            return;
        }

        bringToFront(node);

        draggingNodes = true;
        dragNodes.clear();
        for (GraphNode n : nodes) {
            if (n.selected()) {
                dragNodes.add(new DragNode(n, n.x(), n.y()));
            }
        }

        dragStartWorldX = screenToWorldX(mx, viewX, zoom);
        dragStartWorldY = screenToWorldY(my, viewY, zoom);
    }

    private record DragNode(GraphNode node, float startX, float startY) {}

    private record Minimap(float x, float y, float w, float h,
                           float contentX, float contentY, float contentW, float contentH,
                           float worldMinX, float worldMinY, float worldW, float worldH,
                           float scale) {
        boolean hit(float mx, float my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }

        float worldToMiniX(float worldX) {
            return contentX + (worldX - worldMinX) * scale;
        }

        float worldToMiniY(float worldY) {
            return contentY + (worldY - worldMinY) * scale;
        }

        void panTo(float mx, float my, int viewW, int viewH, float zoom, NodeGraph graph) {
            float cx = Math.max(contentX, Math.min(contentX + contentW, mx));
            float cy = Math.max(contentY, Math.min(contentY + contentH, my));
            float worldX = worldMinX + (cx - contentX) / Math.max(0.0001f, scale);
            float worldY = worldMinY + (cy - contentY) / Math.max(0.0001f, scale);

            float halfW = viewW / (2.0f * Math.max(0.1f, zoom));
            float halfH = viewH / (2.0f * Math.max(0.1f, zoom));
            graph.panX = halfW - worldX;
            graph.panY = halfH - worldY;
        }
    }

    private void bringToFront(GraphNode node) {
        int idx = nodes.indexOf(node);
        if (idx >= 0 && idx != nodes.size() - 1) {
            nodes.remove(idx);
            nodes.add(node);
        }
    }

    private void clearSelection() {
        for (GraphNode n : nodes) {
            n.setSelected(false);
        }
    }

    private GraphNode nodeForPin(NodePin pin) {
        for (GraphNode n : nodes) {
            if (n.inputs().contains(pin) || n.outputs().contains(pin)) {
                return n;
            }
        }
        return null;
    }

    private boolean hasConnection(NodePin from, NodePin to) {
        for (NodeConnection c : connections) {
            if (c.from() == from && c.to() == to) {
                return true;
            }
        }
        return false;
    }

    private float screenToWorldX(float mx, int viewX, float z) {
        return (mx - viewX) / Math.max(0.1f, z) - panX;
    }

    private float screenToWorldY(float my, int viewY, float z) {
        return (my - viewY) / Math.max(0.1f, z) - panY;
    }

    private static boolean pxInside(float px, float py, int x, int y, int w, int h) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    private static float dist2(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }

    private static boolean almostMultiple(float v, float step) {
        float m = Math.abs(v % step);
        return m < 0.001f || Math.abs(step - m) < 0.001f;
    }

    private int pinColor(NodePin pin) {
        if (pin == null) {
            return 0xFF9AA0AA;
        }

        String t = normalizeType(pin.dataType());
        int base = switch (t) {
            case "float" -> 0xFF4C9AFF;
            case "int" -> 0xFFB86BFF;
            case "vec2" -> 0xFF2ECC71;
            case "vec3" -> 0xFF1ABC9C;
            case "color" -> 0xFFFFC857;
            case "texture" -> 0xFFFF6B6B;
            case "shader" -> 0xFF9B6BFF;
            default -> 0xFF9AA0AA;
        };

        if (pin.type() == NodePin.PinType.INPUT) {
            return mixArgb(base, 0xFFFFFFFF, 0.08f);
        }
        return base;
    }

    private static int mixArgb(int a, int b, float t) {
        float tt = Math.max(0.0f, Math.min(1.0f, t));
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;

        int ba = (b >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;

        int oa = Math.round(aa + (ba - aa) * tt);
        int or = Math.round(ar + (br - ar) * tt);
        int og = Math.round(ag + (bg - ag) * tt);
        int ob = Math.round(ab + (bb - ab) * tt);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public List<GraphNode> nodes() { return nodes; }
    public List<NodeConnection> connections() { return connections; }
    public float panX() { return panX; }
    public float panY() { return panY; }
    public float zoom() { return zoom; }
}
