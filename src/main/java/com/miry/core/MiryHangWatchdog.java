package com.miry.core;

import java.util.Locale;

/**
 * Background watchdog that prints the main thread's stack trace if it appears stuck.
 * <p>
 * Enable via {@code -Dmiry.debug=true} (or {@code MIRY_DEBUG=1}).
 */
public final class MiryHangWatchdog implements AutoCloseable {
    private final Thread watchedThread;
    private final long hangNs;
    private final Thread thread;

    private volatile String stage = "startup";
    private volatile long stageStartNs = System.nanoTime();
    private volatile boolean closed;
    private volatile long lastReportNs;

    private MiryHangWatchdog(Thread watchedThread, long hangNs) {
        this.watchedThread = watchedThread;
        this.hangNs = hangNs;
        this.thread = new Thread(this::run, "miry-hang-watchdog");
        this.thread.setDaemon(true);
    }

    public static MiryHangWatchdog startForCurrentThread() {
        if (!MiryDebug.enabled()) return null;
        long hangNs = readHangNs();
        MiryHangWatchdog wd = new MiryHangWatchdog(Thread.currentThread(), hangNs);
        wd.thread.start();
        MiryDebug.log("Hang watchdog enabled (hangMs=" + (hangNs / 1_000_000L) + ")");
        return wd;
    }

    public void stage(String stage) {
        if (stage == null) stage = "";
        this.stage = stage;
        this.stageStartNs = System.nanoTime();
    }

    private void run() {
        while (!closed) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {
            }
            if (closed) break;

            long now = System.nanoTime();
            long dt = now - stageStartNs;
            if (dt < hangNs) {
                continue;
            }
            if (now - lastReportNs < hangNs) {
                continue;
            }
            lastReportNs = now;

            System.err.println("[miry dbg " + MiryDebug.thread() + "] HANG? stage=\"" + stage + "\" for " + formatMs(dt) + "ms");
            StackTraceElement[] st = watchedThread.getStackTrace();
            for (StackTraceElement e : st) {
                System.err.println("  at " + e);
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        thread.interrupt();
    }

    private static long readHangNs() {
        String prop = System.getProperty("miry.debug.hangMs");
        double ms = 2000.0;
        if (prop != null) {
            try {
                ms = Double.parseDouble(prop.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        ms = Math.max(50.0, ms);
        return (long) (ms * 1_000_000.0);
    }

    private static String formatMs(long dtNs) {
        return String.format(Locale.ROOT, "%.3f", dtNs / 1_000_000.0);
    }
}

