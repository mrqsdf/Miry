package com.miry.ui.nodegraph;

import java.util.*;
import java.util.function.Function;

/**
 * Factory for creating custom compute nodes.
 * Register node templates and instantiate them.
 */
public final class NodeFactory {
    private final Map<String, NodeTemplate> templates = new LinkedHashMap<>();
    private final NodeGraph graph;

    public NodeFactory(NodeGraph graph) {
        this.graph = graph;
        registerBuiltinNodes();
    }

    /**
     * Register a node template
     */
    public void register(String path, Function<NodeGraph, ComputeNode> factory) {
        templates.put(path, new NodeTemplate(path, factory));
    }

    /**
     * Create node instance
     */
    public ComputeNode create(String path, float x, float y) {
        NodeTemplate template = templates.get(path);
        if (template == null) {
            throw new IllegalArgumentException("Unknown node type: " + path);
        }

        ComputeNode node = template.factory.apply(graph);
        node.setPosition(x, y);
        node.setOwnerGraph(graph);
        return node;
    }

    /**
     * Get all categories
     */
    public Set<String> getCategories() {
        Set<String> cats = new TreeSet<>();
        for (String path : templates.keySet()) {
            int slash = path.indexOf('/');
            if (slash > 0) {
                cats.add(path.substring(0, slash));
            } else {
                cats.add("Other");
            }
        }
        return cats;
    }

    /**
     * Get nodes in category
     */
    public List<String> getNodesInCategory(String category) {
        List<String> nodes = new ArrayList<>();
        for (String path : templates.keySet()) {
            if (path.startsWith(category + "/")) {
                nodes.add(path);
            }
        }
        return nodes;
    }

    /**
     * Search by name
     */
    public List<String> search(String query) {
        String lower = query.toLowerCase();
        return templates.keySet().stream()
            .filter(path -> path.toLowerCase().contains(lower))
            .toList();
    }

    private void registerBuiltinNodes() {
        // Math nodes
        register("Math/Add", g -> {
            int id = g.allocateId();
            ComputeNode n = new ComputeNode(id, "Add", 0, 0);
            n.setOwnerGraph(g);
            n.withInput("A", "float", 0f);
            n.withInput("B", "float", 0f);
            n.withOutput("Result", "float");
            n.withCompute(node -> {
                float a = node.getInput("A", 0f);
                float b = node.getInput("B", 0f);
                node.setOutput("Result", a + b);
            });
            return n;
        });

        register("Math/Subtract", g -> {
            int id = g.allocateId();
            ComputeNode n = new ComputeNode(id, "Subtract", 0, 0);
            n.setOwnerGraph(g);
            n.withInput("A", "float", 0f);
            n.withInput("B", "float", 0f);
            n.withOutput("Result", "float");
            n.withCompute(node -> {
                float a = node.getInput("A", 0f);
                float b = node.getInput("B", 0f);
                node.setOutput("Result", a - b);
            });
            return n;
        });

        register("Math/Multiply", g -> {
            int id = g.allocateId();
            ComputeNode n = new ComputeNode(id, "Multiply", 0, 0);
            n.setOwnerGraph(g);
            n.withInput("A", "float", 1f);
            n.withInput("B", "float", 1f);
            n.withOutput("Result", "float");
            n.withCompute(node -> {
                float a = node.getInput("A", 1f);
                float b = node.getInput("B", 1f);
                node.setOutput("Result", a * b);
            });
            return n;
        });

        register("Math/Divide", g -> {
            int id = g.allocateId();
            ComputeNode n = new ComputeNode(id, "Divide", 0, 0);
            n.setOwnerGraph(g);
            n.withInput("A", "float", 1f);
            n.withInput("B", "float", 1f);
            n.withOutput("Result", "float");
            n.withCompute(node -> {
                float a = node.getInput("A", 1f);
                float b = node.getInput("B", 1f);
                node.setOutput("Result", b != 0 ? a / b : 0f);
            });
            return n;
        });

        register("Math/Power", g -> {
            int id = g.allocateId();
            ComputeNode n = new ComputeNode(id, "Power", 0, 0);
            n.setOwnerGraph(g);
            n.withInput("Base", "float", 2f);
            n.withInput("Exponent", "float", 2f);
            n.withOutput("Result", "float");
            n.withCompute(node -> {
                float base = node.getInput("Base", 2f);
                float exp = node.getInput("Exponent", 2f);
                node.setOutput("Result", (float) Math.pow(base, exp));
            });
            return n;
        });

        register("Math/Clamp", g -> {
            int id = g.allocateId();
            ComputeNode n = new ComputeNode(id, "Clamp", 0, 0);
            n.setOwnerGraph(g);
            n.withInput("Value", "float", 0.5f);
            n.withInput("Min", "float", 0f);
            n.withInput("Max", "float", 1f);
            n.withOutput("Result", "float");
            n.withCompute(node -> {
                float v = node.getInput("Value", 0.5f);
                float min = node.getInput("Min", 0f);
                float max = node.getInput("Max", 1f);
                node.setOutput("Result", Math.max(min, Math.min(max, v)));
            });
            return n;
        });

        // Vector nodes
        register("Vector/Combine", g -> {
            int id = g.allocateId();
            ComputeNode n = new ComputeNode(id, "Combine Vec3", 0, 0);
            n.setOwnerGraph(g);
            n.withInput("X", "float", 0f);
            n.withInput("Y", "float", 0f);
            n.withInput("Z", "float", 0f);
            n.withOutput("Vector", "vec3");
            n.withCompute(node -> {
                float x = node.getInput("X", 0f);
                float y = node.getInput("Y", 0f);
                float z = node.getInput("Z", 0f);
                node.setOutput("Vector", new float[]{x, y, z});
            });
            return n;
        });

        register("Vector/Split", g -> {
            int id = g.allocateId();
            ComputeNode n = new ComputeNode(id, "Split Vec3", 0, 0);
            n.setOwnerGraph(g);
            n.withInput("Vector", "vec3", new float[]{0, 0, 0});
            n.withOutput("X", "float");
            n.withOutput("Y", "float");
            n.withOutput("Z", "float");
            n.withCompute(node -> {
                float[] v = node.getInput("Vector", new float[]{0, 0, 0});
                node.setOutput("X", v[0]);
                node.setOutput("Y", v[1]);
                node.setOutput("Z", v[2]);
            });
            return n;
        });

        // Utility nodes
        register("Utility/Value", g -> {
            int id = g.allocateId();
            ComputeNode n = new ComputeNode(id, "Value", 0, 0);
            n.setOwnerGraph(g);
            n.withInput("Value", "float", 1f);
            n.withOutput("Output", "float");
            n.withCompute(node -> {
                float v = node.getInput("Value", 1f);
                node.setOutput("Output", v);
            });
            return n;
        });

        register("Utility/Array", g -> {
            int id = g.allocateId();
            ComputeNode n = new ComputeNode(id, "Array", 0, 0);
            n.setOwnerGraph(g);
            n.withAutoExpand("Element", "any");
            n.withOutput("Array", "any");
            n.withCompute(node -> {
                List<Object> array = new ArrayList<>();
                for (String inputName : node.getInputNames()) {
                    Object val = node.getInput(inputName, null);
                    if (val != null) {
                        array.add(val);
                    }
                }
                node.setOutput("Array", array);
            });
            return n;
        });
    }

    private record NodeTemplate(String path, Function<NodeGraph, ComputeNode> factory) {}
}
