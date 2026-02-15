package com.miry.core;

/**
 * Tiny, opt-in debug logger/timer for diagnosing freezes in embedded hosts.
 * <p>
 * Enable via {@code -Dmiry.debug=true} or {@code MIRY_DEBUG=1}.
 */
public final class MiryDebug {
    private MiryDebug() {}

    private static final boolean ENABLED = readEnabled();
    private static final boolean TRACE = ENABLED && readTraceEnabled();
    private static final long SLOW_CALL_NS = readSlowCallNs();

    public static boolean enabled() {
        return ENABLED;
    }

    public static boolean traceEnabled() {
        return TRACE;
    }

    /**
     * Logs a breadcrumb intended to narrow down a hard hang (prints before a potentially blocking native call).
     */
    public static void trace(String msg) {
        if (!TRACE) return;
        System.err.println(prefix() + msg);
    }

    public static void log(String msg) {
        if (!ENABLED) return;
        System.err.println(prefix() + msg);
    }

    public static void log(String msg, Throwable t) {
        if (!ENABLED) return;
        System.err.println(prefix() + msg);
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }

    public static long nowNs() {
        return System.nanoTime();
    }

    public static void logIfSlow(String what, long startNs) {
        if (!ENABLED) return;
        long dt = System.nanoTime() - startNs;
        if (dt >= SLOW_CALL_NS) {
            System.err.println(prefix() + what + " took " + formatMs(dt) + "ms");
        }
    }

    public static String thread() {
        Thread t = Thread.currentThread();
        return t.getName() + "#" + t.getId();
    }

    private static String prefix() {
        return "[miry dbg " + thread() + "] ";
    }

    private static String formatMs(long dtNs) {
        return String.format(java.util.Locale.ROOT, "%.3f", dtNs / 1_000_000.0);
    }

    private static boolean readEnabled() {
        String prop = System.getProperty("miry.debug");
        if (prop != null) {
            return isTruthy(prop);
        }
        String env = System.getenv("MIRY_DEBUG");
        return env != null && isTruthy(env);
    }

    private static long readSlowCallNs() {
        String prop = System.getProperty("miry.debug.slowMs");
        double ms = 25.0;
        if (prop != null) {
            try {
                ms = Double.parseDouble(prop.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        ms = Math.max(0.0, ms);
        return (long) (ms * 1_000_000.0);
    }

    private static boolean readTraceEnabled() {
        String prop = System.getProperty("miry.debug.trace");
        if (prop != null) {
            return isTruthy(prop);
        }
        String env = System.getenv("MIRY_DEBUG_TRACE");
        return env != null && isTruthy(env);
    }

    private static boolean isTruthy(String s) {
        String v = s.trim().toLowerCase(java.util.Locale.ROOT);
        return v.equals("1") || v.equals("true") || v.equals("yes") || v.equals("on");
    }
}
