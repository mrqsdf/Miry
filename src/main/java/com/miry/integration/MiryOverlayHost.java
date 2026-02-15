package com.miry.integration;

/**
 * Minimal host surface interface for embedding Miry as an overlay.
 */
public interface MiryOverlayHost {
    /**
     * GLFW window handle (required by some features such as cursor changes and optional mouse-wrapping drags).
     */
    long windowHandle();

    int windowWidth();

    int windowHeight();

    int framebufferWidth();

    int framebufferHeight();

    /**
     * UI scale (window pixels to framebuffer pixels). For HiDPI you may prefer to use X/Y separately in your own code,
     * but Miry’s immediate-mode layer expects a single factor.
     */
    float framebufferScale();
}

