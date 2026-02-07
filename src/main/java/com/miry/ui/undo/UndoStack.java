package com.miry.ui.undo;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bounded undo/redo stack for {@link Command} objects.
 */
public final class UndoStack {
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private int maxSize = 100;

    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
        if (undoStack.size() > maxSize) {
            undoStack.removeLast();
        }
    }

    public void undo() {
        if (!canUndo()) return;
        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
    }

    public void redo() {
        if (!canRedo()) return;
        Command cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    public String undoDescription() {
        return canUndo() ? undoStack.peek().description() : "";
    }

    public String redoDescription() {
        return canRedo() ? redoStack.peek().description() : "";
    }

    public void setMaxSize(int max) {
        this.maxSize = Math.max(1, max);
    }
}
