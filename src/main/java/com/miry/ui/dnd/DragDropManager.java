package com.miry.ui.dnd;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tracks the current drag payload and dispatches drops to registered targets.
 * <p>
 * This is a small, UI-level DnD helper (not an OS/system drag-and-drop implementation).
 */
public final class DragDropManager {
    private DragPayload<?> dragPayload;
    private final List<DropTarget<?>> dropTargets = new ArrayList<>();
    private DropTarget<?> hoveredTarget;
    private float dragX, dragY;

    public <T> void startDrag(DragPayload<T> payload, float x, float y) {
        this.dragPayload = payload;
        this.dragX = x;
        this.dragY = y;
    }

    public void updateDrag(float x, float y) {
        if (!isDragging()) return;
        dragX = x;
        dragY = y;

        hoveredTarget = null;
        for (DropTarget<?> target : dropTargets) {
            if (target.acceptedType().equals(dragPayload.type()) && target.contains(x, y)) {
                hoveredTarget = target;
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void endDrag() {
        if (hoveredTarget != null && dragPayload != null) {
            ((Consumer<Object>) hoveredTarget.onDrop()).accept(dragPayload.data());
        }
        dragPayload = null;
        hoveredTarget = null;
    }

    public <T> void registerDropTarget(DropTarget<T> target) {
        dropTargets.add(target);
    }

    public void clearDropTargets() {
        dropTargets.clear();
    }

    public boolean isDragging() {
        return dragPayload != null;
    }

    public DragPayload<?> dragPayload() {
        return dragPayload;
    }

    public DropTarget<?> hoveredTarget() {
        return hoveredTarget;
    }

    public float dragX() { return dragX; }
    public float dragY() { return dragY; }
}
