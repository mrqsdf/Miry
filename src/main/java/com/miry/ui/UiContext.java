package com.miry.ui;

import com.miry.core.MiryDebug;
import com.miry.ui.anim.AnimationManager;
import com.miry.ui.capture.PointerCapture;
import com.miry.ui.clipboard.Clipboard;
import com.miry.ui.core.OverlayQueue;
import com.miry.ui.cursor.CursorManager;
import com.miry.ui.dnd.DragDropManager;
import com.miry.ui.event.EventQueue;
import com.miry.ui.event.UiEvent;
import com.miry.ui.focus.FocusManager;
import com.miry.ui.input.KeyboardInput;
import com.miry.ui.shortcut.ShortcutManager;
import com.miry.ui.undo.UndoStack;

/**
 * Top-level UI runtime context and state container.
 * <p>
 * This class acts as the central hub for the UI system, managing event queues, input focus,
 * clipboard access, cursor state, drag-and-drop operations, animations, and the undo/redo stack.
 * </p>
 * <p>
 * An instance of {@code UiContext} is required to use the retained-mode {@link com.miry.ui.widgets} system.
 * </p>
 */
public final class UiContext implements AutoCloseable {
    /**
     * Configuration options for the UI context.
     */
    public record Config(boolean installGlfwKeyboardCallbacks) {
        /** Default configuration: installs GLFW keyboard callbacks automatically. */
        public static final Config DEFAULT = new Config(true);
        /** Manual input configuration: does not install callbacks (useful for embedding). */
        public static final Config MANUAL_INPUT = new Config(false);
    }

    private final long window;
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
    private final OverlayQueue overlayQueue;
    private float lastDt;

    /**
     * Creates a new UI context for the specified window using the default configuration.
     *
     * @param window The native GLFW window handle.
     */
    public UiContext(long window) {
        this(window, Config.DEFAULT);
    }

    /**
     * Creates a new UI context with a specific configuration.
     *
     * @param window The native GLFW window handle.
     * @param config The configuration to use.
     */
    public UiContext(long window, Config config) {
        this.window = window;
        MiryDebug.log("UiContext.<init> window=" + window + " config=" + (config == null ? "null" : config));
        this.eventQueue = new EventQueue();
        this.focusManager = new FocusManager(eventQueue);
        this.pointerCapture = new PointerCapture();
        this.shortcutManager = new ShortcutManager();
        this.clipboard = new Clipboard(window);
        this.cursorManager = new CursorManager(window);
        boolean installRequested = (config == null || config.installGlfwKeyboardCallbacks());
        boolean installDisabledByProp = Boolean.getBoolean("miry.disableGlfwKeyboardCallbacks");
        boolean install = (window != 0L) && installRequested && !installDisabledByProp;
        if (installDisabledByProp) {
            MiryDebug.log("GLFW keyboard callbacks disabled via -Dmiry.disableGlfwKeyboardCallbacks=true");
        }
        this.keyboardInput = new KeyboardInput(window, eventQueue,
            install ? KeyboardInput.Mode.INSTALL_GLFW_CALLBACKS : KeyboardInput.Mode.MANUAL);
        this.dragDropManager = new DragDropManager();
        this.animationManager = new AnimationManager();
        this.undoStack = new UndoStack();
        this.overlayQueue = new OverlayQueue();
    }

    /**
     * Returns the native window handle associated with this context.
     * @return The window handle.
     */
    public long windowHandle() {
        return window;
    }

    /**
     * Updates the UI state for the current frame.
     * <p>
     * This method advances animations, updates focus state, and prepares the overlay queue.
     * </p>
     *
     * @param dt The delta time since the last frame, in seconds.
     */
    public void update(float dt) {
        lastDt = Math.max(0.0f, dt);
        focusManager.beginFrame();
        animationManager.update(lastDt);
        overlayQueue.beginFrame();
    }

    /**
     * Returns the delta time provided in the last {@link #update(float)} call.
     * @return Delta time in seconds.
     */
    public float lastDt() {
        return lastDt;
    }

    /**
     * Polls the next event from the event queue.
     *
     * @return The next {@link UiEvent}, or {@code null} if the queue is empty.
     */
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
    public OverlayQueue overlay() { return overlayQueue; }

    @Override
    public void close() {
        keyboardInput.cleanup();
        cursorManager.close();
    }
}
