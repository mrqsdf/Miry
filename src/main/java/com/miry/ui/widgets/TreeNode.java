package com.miry.ui.widgets;

import com.miry.ui.theme.Icon;

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
    private boolean visible = true;
    private Icon icon;
    private Object userData;

    public TreeNode(T data) {
        this.data = data;
    }

    public T data() { return data; }
    public List<TreeNode<T>> children() { return children; }
    public boolean expanded() { return expanded; }
    public boolean selected() { return selected; }
    public boolean visible() { return visible; }
    public Icon icon() { return icon; }
    public Object userData() { return userData; }

    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public void setIcon(Icon icon) { this.icon = icon; }
    public void setUserData(Object userData) { this.userData = userData; }
    public void toggleExpanded() { this.expanded = !this.expanded; }
    public void toggleVisible() { this.visible = !this.visible; }

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
