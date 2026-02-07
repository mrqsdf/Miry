package com.miry.ui.nodegraph;

import com.miry.graphics.Texture;

import java.util.ArrayList;
import java.util.List;

/**
 * A node displayed in the node graph editor.
 */
public class GraphNode {
    private final int id;
    private final String title;
    private float x, y;
    private int width = 150;
    private int height = 100;
    private final List<NodePin> inputs = new ArrayList<>();
    private final List<NodePin> outputs = new ArrayList<>();
    private boolean selected;
    private Texture previewTexture;
    private int previewHeight = 0;

    public GraphNode(int id, String title, float x, float y) {
        this.id = id;
        this.title = title;
        this.x = x;
        this.y = y;
    }

    public void addInput(NodePin pin) {
        inputs.add(pin);
        updateHeight();
    }

    public void addOutput(NodePin pin) {
        outputs.add(pin);
        updateHeight();
    }

    private void updateHeight() {
        int slots = Math.max(inputs.size(), outputs.size());
        int base = 50 + slots * 20;
        if (previewTexture != null && previewHeight > 0) {
            base += 10 + previewHeight;
        }
        height = base;
    }

    public int id() { return id; }
    public String title() { return title; }
    public float x() { return x; }
    public float y() { return y; }
    public int width() { return width; }
    public int height() { return height; }
    public List<NodePin> inputs() { return inputs; }
    public List<NodePin> outputs() { return outputs; }
    public boolean selected() { return selected; }
    public Texture previewTexture() { return previewTexture; }
    public int previewHeight() { return previewHeight; }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }

    public void setSelected(boolean sel) {
        this.selected = sel;
    }

    public void setPreviewTexture(Texture texture, int height) {
        this.previewTexture = texture;
        this.previewHeight = Math.max(0, height);
        updateHeight();
    }

    public boolean contains(float px, float py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }
}
