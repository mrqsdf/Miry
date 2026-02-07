package com.miry.core;

/**
 * Host application callbacks invoked by {@link Engine}.
 * <p>
 * Implementations typically create resources in {@link #onInit()}, update state in {@link #onUpdate(float)},
 * and render in {@link #onRender()}.
 */
public abstract class Application {
    protected void onInit() {}

    protected void onUpdate(float dt) {}

    protected void onRender() {}

    protected void onShutdown() {}
}
