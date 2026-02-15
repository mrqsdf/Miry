package com.miry.ui.widgets;

import com.miry.ui.UiContext;
import com.miry.ui.core.BaseWidget;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.input.UiInput;
import com.miry.ui.render.UiRenderer;
import com.miry.ui.theme.Icon;
import com.miry.ui.theme.Theme;

import java.io.File;
import java.nio.file.Path;

/**
 * File browser using TreeView.
 */
public final class FileBrowser extends BaseWidget {
    private final TreeView<File> treeView;
    private final TreeNode<File> root;
    private boolean lazyLoad = true;

    public FileBrowser(Path rootPath, int itemHeight) {
        File rootFile = rootPath.toFile();
        this.root = new TreeNode<>(rootFile);
        this.root.setExpanded(true);
        this.treeView = new TreeView<>(root, itemHeight);
        this.treeView.setLabelFunction(f -> {
            if (f == null) return "";
            String name = f.getName();
            return (name == null || name.isEmpty()) ? f.getPath() : name;
        });
        loadDirectory(root, rootFile);
    }

    public TreeView<File> treeView() { return treeView; }
    public void setLazyLoad(boolean lazyLoad) { this.lazyLoad = lazyLoad; }
    public File selectedFile() {
        var selected = treeView.selectedNodes();
        return selected.isEmpty() ? null : selected.iterator().next().data();
    }

    private void loadDirectory(TreeNode<File> node, File dir) {
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        java.util.Arrays.sort(files, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (File file : files) {
            if (file.isHidden()) continue;
            TreeNode<File> child = new TreeNode<>(file);
            child.setIcon(file.isDirectory() ? Icon.FOLDER : Icon.FILE);
            node.addChild(child);
            if (lazyLoad && file.isDirectory() && file.listFiles() != null && file.listFiles().length > 0) {
                child.addChild(new TreeNode<>(null)); // Dummy
            }
        }
    }

    public void render(UiRenderer r, UiContext ctx, UiInput input, Theme theme, int x, int y, int width, int height, int scrollOffset, boolean interactive) {
        treeView.render(r, ctx, input, theme, x, y, width, height, scrollOffset, interactive);
        if (lazyLoad) {
            ensureExpandedLoaded(root);
        }
    }

    public boolean handleKey(UiContext ctx, KeyEvent event) {
        return treeView.handleKey(ctx, event);
    }

    public int computeContentHeight() {
        return treeView.computeContentHeight();
    }

    private void ensureExpandedLoaded(TreeNode<File> node) {
        File f = node.data();
        if (f != null && f.isDirectory() && node.expanded()) {
            // If first child is a dummy sentinel, replace it with the real directory listing.
            if (node.children().size() == 1 && node.children().get(0).data() == null) {
                node.children().clear();
                loadDirectory(node, f);
            }
        }
        for (TreeNode<File> child : node.children()) {
            ensureExpandedLoaded(child);
        }
    }
}
