package com.miry.ui.nodegraph;

import java.util.Objects;

/**
 * A node pin (input or output) used for connections in {@link NodeGraph}.
 * <p>
 * Pins are positioned in screen space each frame by {@link NodeGraph} for hit testing and wire rendering.
 */
public final class NodePin {
    public enum PinType { INPUT, OUTPUT }

    private final int id;
    private final String name;
    private final PinType type;
    private final String dataType;
    private float x;
    private float y;

    public NodePin(int id, String name, PinType type) {
        this(id, name, type, "any", 0.0f, 0.0f);
    }

    public NodePin(int id, String name, PinType type, String dataType) {
        this(id, name, type, dataType, 0.0f, 0.0f);
    }

    public NodePin(int id, String name, PinType type, String dataType, float x, float y) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.dataType = Objects.requireNonNull(dataType, "dataType");
        this.x = x;
        this.y = y;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public PinType type() {
        return type;
    }

    public String dataType() {
        return dataType;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
