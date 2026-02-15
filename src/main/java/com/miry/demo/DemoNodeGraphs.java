package com.miry.demo;

import com.miry.ui.nodegraph.ComputeNode;
import com.miry.ui.nodegraph.NodeFactory;
import com.miry.ui.nodegraph.NodeGraph;

/**
 * Helpers for populating demo {@link NodeGraph} instances with a few nodes and links.
 */
final class DemoNodeGraphs {
    private DemoNodeGraphs() {
    }

    static void populateBasic(NodeGraph graph) {
        if (graph == null) {
            return;
        }

        NodeFactory factory = new NodeFactory(graph);

        // (5 + 3) * 2 = 16
        ComputeNode value = factory.create("Utility/Value", 120, 120);
        value.setInput("Value", 5f);
        graph.nodes().add(value);

        ComputeNode add = factory.create("Math/Add", 360, 155);
        add.setInput("B", 3f);
        graph.nodes().add(add);

        ComputeNode multiply = factory.create("Math/Multiply", 610, 135);
        multiply.setInput("B", 2f);
        graph.nodes().add(multiply);

        graph.addConnection(value.outputs().get(0), add.inputs().get(0));
        graph.addConnection(add.outputs().get(0), multiply.inputs().get(0));

        // A small vec3 chain so type labels show up.
        ComputeNode combine = factory.create("Vector/Combine", 140, 360);
        combine.setInput("X", 0.25f);
        combine.setInput("Y", 0.75f);
        combine.setInput("Z", 0.10f);
        graph.nodes().add(combine);

        ComputeNode split = factory.create("Vector/Split", 420, 345);
        graph.nodes().add(split);
        graph.addConnection(combine.outputs().get(0), split.inputs().get(0));

        // Start with a little offset so everything is visible.
        graph.pan(-80, -40);
    }
}

