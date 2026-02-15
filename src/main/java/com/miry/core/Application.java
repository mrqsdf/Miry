package com.miry.core;

/**
 * Base class for host application callbacks invoked by the {@link Engine}.
 * <p>
 * This class defines the lifecycle of a Miry application. Subclasses should override these methods
 * to implement their specific logic for initialization, updates, rendering, and cleanup.
 * </p>
 */
public abstract class Application {

    /**
     * Called once during engine initialization, before the main loop starts.
     * <p>
     * Use this method to initialize resources, load assets, and set up the initial state of the application.
     * </p>
     */
    protected void onInit() {}

    /**
     * Called every frame to update the application state.
     *
     * @param dt The time elapsed since the last frame, in seconds (delta time).
     */
    protected void onUpdate(float dt) {}

    /**
     * Called every frame to render the application content.
     * <p>
     * This method is invoked after the screen has been cleared. OpenGL commands can be issued here.
     * </p>
     */
    protected void onRender() {}

    /**
     * Called once when the engine is shutting down.
     * <p>
     * Use this method to release resources, save state, and perform any necessary cleanup.
     * </p>
     */
    protected void onShutdown() {}

    /**
     * Indicates whether the application requires continuous rendering (like a game or 3D view).
     * <p>
     * If {@code true}, the engine will render every frame as fast as possible.
     * If {@code false}, the engine will wait for input events or active animations before rendering,
     * effectively sleeping to save CPU/GPU resources.
     * </p>
     * <p>
     * Override this to return {@code false} for standard GUI applications.
     * </p>
     *
     * @return {@code true} for continuous rendering (default), {@code false} for lazy rendering.
     */
    public boolean isContinuous() {
        return true;
    }

    /**
     * Checks if the application has active animations or pending work that requires a repaint.
     * <p>
     * This is polled by the engine when {@link #isContinuous()} is {@code false} to decide whether to
     * sleep or render the next frame.
     * </p>
     *
     * @return {@code true} if a frame should be rendered immediately.
     */
    public boolean needsRepaint() {
        return false;
    }
}
