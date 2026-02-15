package com.miry.ui.layout;

/**
 * Pure data snapshot of a dock tree (no GL, no host dependencies).
 * Intended for inspection, tooling, and persistence outside the runtime nodes.
 */
public sealed interface DockLayout permits DockLayout.Split, DockLayout.Leaf, DockLayout.Tabs {
    record Split(boolean vertical, float splitRatio, DockLayout childA, DockLayout childB) implements DockLayout {}

    record Leaf(String panelTitle) implements DockLayout {}

    record Tabs(java.util.List<String> panelTitles, int activeIndex) implements DockLayout {}
}
