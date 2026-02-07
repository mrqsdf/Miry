package com.miry.platform;

import org.joml.Vector2f;

/**
 * Platform host abstraction used by the runtime and UI (window, framebuffer scale, mouse/buttons).
 */
public interface MiryHost {
    int getWindowWidth();

    int getWindowHeight();

    int getFramebufferWidth();

    int getFramebufferHeight();

    float getScaleFactor();

    default float getFramebufferScaleX() {
        int w = Math.max(1, getWindowWidth());
        return getFramebufferWidth() / (float) w;
    }

    default float getFramebufferScaleY() {
        int h = Math.max(1, getWindowHeight());
        return getFramebufferHeight() / (float) h;
    }

    default float getFramebufferScale() {
        return Math.max(getFramebufferScaleX(), getFramebufferScaleY());
    }

    double getTime();

    boolean isKeyDown(int key);

    boolean isMouseDown(int button);

    Vector2f getMousePos();

    void setCursorLocked(boolean locked);

    String getClipboard();

    void setClipboard(String text);

    long getNativeWindow();
}
