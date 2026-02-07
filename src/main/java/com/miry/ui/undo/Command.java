package com.miry.ui.undo;

/**
 * A reversible action stored in an {@link UndoStack}.
 */
public interface Command {
    /**
     * Applies the change (used for initial execution and redo).
     */
    void execute();

    /**
     * Reverts the change.
     */
    void undo();

    /**
     * Human-readable description shown in UI (e.g. "Move Node").
     */
    String description();
}
