package com.miry.ui.nodegraph;

import java.util.*;
import java.util.function.Consumer;

/**
 * GraphNode extension with dynamic pins, values, and compute logic.
 * Integrates with existing NodeGraph system.
 */
public final class ComputeNode extends GraphNode {
    private final Map<Integer, Object> pinValues = new HashMap<>();
    private final Map<String, Integer> inputsByName = new LinkedHashMap<>();
    private final Map<String, Integer> outputsByName = new LinkedHashMap<>();
    private Consumer<ComputeNode> computeFunc;
    private boolean autoExpandInputs = false;
    private String autoExpandTemplate = "Input";
    private String autoExpandType = "any";

    private NodeGraph ownerGraph;
    private int nextAutoIndex = 0;

    public ComputeNode(int id, String title, float x, float y) {
        super(id, title, x, y);
    }

    /**
     * Set the graph that owns this node (needed for auto-expansion)
     */
    public void setOwnerGraph(NodeGraph graph) {
        this.ownerGraph = graph;
    }

    /**
     * Add input with default value
     */
    public ComputeNode withInput(String name, String dataType, Object defaultValue) {
        if (ownerGraph == null) throw new IllegalStateException("Must call setOwnerGraph() first");

        int pinId = ownerGraph.allocateId();
        NodePin pin = new NodePin(pinId, name, NodePin.PinType.INPUT, dataType);
        addInput(pin);

        inputsByName.put(name, pinId);
        pinValues.put(pinId, defaultValue);
        return this;
    }

    /**
     * Add output
     */
    public ComputeNode withOutput(String name, String dataType) {
        if (ownerGraph == null) throw new IllegalStateException("Must call setOwnerGraph() first");

        int pinId = ownerGraph.allocateId();
        NodePin pin = new NodePin(pinId, name, NodePin.PinType.OUTPUT, dataType);
        addOutput(pin);

        outputsByName.put(name, pinId);
        return this;
    }

    /**
     * Enable auto-expanding inputs
     */
    public ComputeNode withAutoExpand(String template, String type) {
        this.autoExpandInputs = true;
        this.autoExpandTemplate = template;
        this.autoExpandType = type;

        // Create first auto-expand slot
        String name = template + " " + nextAutoIndex++;
        withInput(name, type, null);
        return this;
    }

    /**
     * Set compute function
     */
    public ComputeNode withCompute(Consumer<ComputeNode> func) {
        this.computeFunc = func;
        return this;
    }

    /**
     * Get input value (from connection or default)
     */
    @SuppressWarnings("unchecked")
    public <T> T getInput(String name, T fallback) {
        Integer pinId = inputsByName.get(name);
        if (pinId == null) return fallback;

        // Check if connected
        if (ownerGraph != null) {
            for (NodeConnection conn : ownerGraph.connections()) {
                if (conn.to().id() == pinId) {
                    // Find the source node and get its output value
                    int srcPinId = conn.from().id();
                    for (GraphNode node : ownerGraph.nodes()) {
                        if (node instanceof ComputeNode cn) {
                            for (String outName : cn.outputsByName.keySet()) {
                                if (cn.outputsByName.get(outName) == srcPinId) {
                                    Object val = cn.pinValues.get(srcPinId);
                                    if (val != null) {
                                        return (T) val;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Use stored value or fallback
        Object stored = pinValues.get(pinId);
        return stored != null ? (T) stored : fallback;
    }

    /**
     * Set input value (when not connected)
     */
    public void setInput(String name, Object value) {
        Integer pinId = inputsByName.get(name);
        if (pinId != null) {
            pinValues.put(pinId, value);
        }
    }

    /**
     * Set output value
     */
    public void setOutput(String name, Object value) {
        Integer pinId = outputsByName.get(name);
        if (pinId != null) {
            pinValues.put(pinId, value);
        }
    }

    /**
     * Get output value
     */
    @SuppressWarnings("unchecked")
    public <T> T getOutput(String name) {
        Integer pinId = outputsByName.get(name);
        if (pinId == null) return null;
        return (T) pinValues.get(pinId);
    }

    /**
     * Execute compute function
     */
    public void compute() {
        if (computeFunc != null) {
            computeFunc.accept(this);
        }
    }

    /**
     * Called by NodeGraph when a connection is made TO this node
     */
    public void onInputConnected(NodePin pin) {
        if (!autoExpandInputs) return;
        if (pin == null) return;

        // Check if this is the last auto-expand input
        List<NodePin> inputs = inputs();
        if (inputs.isEmpty()) return;

        NodePin lastInput = inputs.get(inputs.size() - 1);
        if (lastInput.id() == pin.id() && lastInput.name().startsWith(autoExpandTemplate)) {
            // Create new auto-expand slot
            String name = autoExpandTemplate + " " + nextAutoIndex++;
            withInput(name, autoExpandType, null);
        }
    }

    /**
     * Remove input by name
     */
    public void removeInput(String name) {
        Integer pinId = inputsByName.remove(name);
        if (pinId != null) {
            // Find and remove from parent list
            for (int i = 0; i < inputs().size(); i++) {
                if (inputs().get(i).id() == pinId) {
                    inputs().remove(i);
                    break;
                }
            }
            pinValues.remove(pinId);
        }
    }

    /**
     * Remove output by name
     */
    public void removeOutput(String name) {
        Integer pinId = outputsByName.remove(name);
        if (pinId != null) {
            // Find and remove from parent list
            for (int i = 0; i < outputs().size(); i++) {
                if (outputs().get(i).id() == pinId) {
                    outputs().remove(i);
                    break;
                }
            }
            pinValues.remove(pinId);
        }
    }

    /**
     * Check if input exists
     */
    public boolean hasInput(String name) {
        return inputsByName.containsKey(name);
    }

    /**
     * Get all input names
     */
    public Set<String> getInputNames() {
        return new LinkedHashSet<>(inputsByName.keySet());
    }

    /**
     * Get all output names
     */
    public Set<String> getOutputNames() {
        return new LinkedHashSet<>(outputsByName.keySet());
    }
}
