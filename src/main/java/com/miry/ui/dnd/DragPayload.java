package com.miry.ui.dnd;

/**
 * A typed payload carried during a drag operation.
 *
 * @param type stable string identifier used for target filtering
 * @param data payload data
 */
public record DragPayload<T>(String type, T data) {}
