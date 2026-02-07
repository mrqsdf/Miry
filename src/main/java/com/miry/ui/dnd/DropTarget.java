package com.miry.ui.dnd;

import java.util.function.Consumer;

/**
 * Defines a rectangular drop target for a given payload type.
 *
 * @param id unique identifier for debugging/introspection
 * @param acceptedType payload {@link DragPayload#type()} accepted by this target
 * @param x target x (pixels)
 * @param y target y (pixels)
 * @param width target width (pixels)
 * @param height target height (pixels)
 * @param onDrop invoked when a compatible payload is dropped on this target
 */
public record DropTarget<T>(int id, String acceptedType, int x, int y, int width, int height, Consumer<T> onDrop) {
    public boolean contains(float px, float py) {
        return px >= x && py >= y && px < (x + width) && py < (y + height);
    }
}
