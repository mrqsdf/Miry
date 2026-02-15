package com.miry.platform;

import java.util.Objects;

/**
 * Global access point for the active {@link MiryHost}.
 * <p>
 * This class manages a singleton reference to the platform-specific host implementation.
 * It serves as a bridge for accessing platform capabilities from anywhere in the application.
 * </p>
 */
public final class MiryContext {
    private static volatile MiryHost host;

    private MiryContext() {
    }

    /**
     * Retrieves the currently active {@link MiryHost}.
     *
     * @return The active host instance.
     * @throws IllegalStateException if the host has not been initialized.
     */
    public static MiryHost host() {
        MiryHost h = host;
        if (h == null) {
            throw new IllegalStateException("MiryHost not set. Call MiryContext.setHost(...) before using context features.");
        }
        return h;
    }

    /**
     * Sets the active {@link MiryHost}.
     * <p>
     * This is typically called during engine initialization.
     * </p>
     *
     * @param host The host implementation to set. Must not be null.
     * @throws NullPointerException if {@code host} is null.
     */
    public static void setHost(MiryHost host) {
        MiryContext.host = Objects.requireNonNull(host, "host");
    }
}