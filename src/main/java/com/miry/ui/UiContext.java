package com.miry.ui;

import com.miry.ui.anim.AnimationManager;
import com.miry.ui.capture.PointerCapture;
import com.miry.ui.clipboard.Clipboard;
import com.miry.ui.cursor.CursorManager;
import com.miry.ui.dnd.DragDropManager;
import com.miry.ui.event.EventQueue;
import com.miry.ui.event.UiEvent;
import com.miry.ui.focus.FocusManager;
import com.miry.ui.input.KeyboardInput;
import com.miry.ui.shortcut.ShortcutManager;
import com.miry.ui.undo.UndoStack;

/**
 * Top-level UI runtime context.
 * <p>
 * Owns event queue, focus management, clipboard, cursor manager, keyboard input callbacks,
 * drag & drop state, animation manager, and undo/redo stack.
 */
public final class UiContext implements AutoCloseable {
    private final EventQueue eventQueue;
    private final FocusManager focusManager;
    private final PointerCapture pointerCapture;
    private final ShortcutManager shortcutManager;
    private final Clipboard clipboard;
    private final CursorManager cursorManager;
    private final KeyboardInput keyboardInput;
    private final DragDropManager dragDropManager;
    private final AnimationManager animationManager;
    private final UndoStack undoStack;
    private float lastDt;

    public UiContext(long window) {
        this.eventQueue = new EventQueue();
        this.focusManager = new FocusManager(eventQueue);
        this.pointerCapture = new PointerCapture();
        this.shortcutManager = new ShortcutManager();
        this.clipboard = new Clipboard(window);
        this.cursorManager = new CursorManager(window);
        this.keyboardInput = new KeyboardInput(window, eventQueue);
        this.dragDropManager = new DragDropManager();
        this.animationManager = new AnimationManager();
        this.undoStack = new UndoStack();
    }

    public void update(float dt) {
        lastDt = Math.max(0.0f, dt);
        focusManager.beginFrame();
        animationManager.update(lastDt);
    }

    public float lastDt() {
        return lastDt;
    }

    public UiEvent pollEvent() {
        return eventQueue.poll();
    }

    public EventQueue events() { return eventQueue; }
    public FocusManager focus() { return focusManager; }
    public PointerCapture pointer() { return pointerCapture; }
    public ShortcutManager shortcuts() { return shortcutManager; }
    public Clipboard clipboard() { return clipboard; }
    public CursorManager cursors() { return cursorManager; }
    public KeyboardInput keyboard() { return keyboardInput; }
    public DragDropManager dragDrop() { return dragDropManager; }
    public AnimationManager animations() { return animationManager; }
    public UndoStack undo() { return undoStack; }

    @Override
    public void close() {
        keyboardInput.cleanup();
        cursorManager.close();
    }
}
