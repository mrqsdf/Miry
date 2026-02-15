package com.miry.ui.render;

/**
 * Renders a {@link UiDrawList} using a concrete graphics backend.
 * <p>
 * The backend is responsible for batching, state management and GPU calls.
 */
public interface UiRenderBackend {
    void render(UiDrawList drawList, int windowWidth, int windowHeight, float scaleFactor);
}

