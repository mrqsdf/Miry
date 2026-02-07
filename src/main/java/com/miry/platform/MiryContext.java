package com.miry.platform;

import java.util.Objects;

/**
 * Global access point for the active {@link MiryHost}.
 */
public final class MiryContext {
    private static volatile MiryHost host;

    private MiryContext() {}

    public static MiryHost host() {
        MiryHost h = host;
        if (h == null) {
            throw new IllegalStateException("MiryHost not set. Call MiryContext.setHost(...) before using Flux.");
        }
        return h;
    }

    public static void setHost(MiryHost host) {
        MiryContext.host = Objects.requireNonNull(host, "host");
    }
}
