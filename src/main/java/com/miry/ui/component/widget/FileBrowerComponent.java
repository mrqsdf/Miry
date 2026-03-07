package com.miry.ui.component.widget;

import com.miry.ui.UiContext;
import com.miry.ui.component.Component;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.widgets.FileBrowser;
import com.miry.ui.widgets.TreeView;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

public class FileBrowerComponent extends Component {

    private FileBrowser fileBrowser;
    private Consumer<File> onFileSelected;

    public FileBrowerComponent(String id, String defaultPath, int itemHeight) {
        super(id);
        this.fileBrowser = new FileBrowser(Path.of(defaultPath), itemHeight);
    }

    public FileBrowerComponent(String id, Path defaultPath, int itemHeight) {
        super(id);
        this.fileBrowser = new FileBrowser(defaultPath, itemHeight);
    }

    public FileBrowerComponent(String id, FileBrowser fileBrowser) {
        super(id);
        this.fileBrowser = fileBrowser;
    }

    public FileBrowser fileBrowser() {
        return fileBrowser;
    }

    public TreeView<File> treeView() {
        return fileBrowser.treeView();
    }

    public File selectedFile() {
        return fileBrowser.selectedFile();
    }

    public FileBrowerComponent setLazyLoad(boolean lazyLoad) {
        fileBrowser.setLazyLoad(lazyLoad);
        return this;
    }

    @Override
    public void handleKey(UiContext uiContext, KeyEvent keyEvent) {
        fileBrowser.handleKey(uiContext, keyEvent);
    }

    public int computeContentHeight() {
        return fileBrowser.computeContentHeight();
    }

    public FileBrowerComponent onFileSelected(Consumer<File> callback) {
        this.onFileSelected = callback;
        return this;
    }

    public void triggerFileSelected(File file) {
        if (onFileSelected != null) {
            onFileSelected.accept(file);
        }
    }
}
