package com.miry.ui.widgets;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree node data structure used by {@link TreeView}.
 */
public final class TreeNode<T> {
    private final T data;
    private final List<TreeNode<T>> children = new ArrayList<>();
    private boolean expanded;
    private boolean selected;

    public TreeNode(T data) {
        this.data = data;
    }

    public T data() { return data; }
    public List<TreeNode<T>> children() { return children; }
    public boolean expanded() { return expanded; }
    public boolean selected() { return selected; }

    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public void toggleExpanded() { this.expanded = !this.expanded; }

    public void addChild(TreeNode<T> child) {
        children.add(child);
    }

    public void removeChild(TreeNode<T> child) {
        children.remove(child);
    }

    public int visibleCount() {
        int count = 1;
        if (expanded) {
            for (TreeNode<T> child : children) {
                count += child.visibleCount();
            }
        }
        return count;
    }
}
