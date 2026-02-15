package com.miry.demo;

import com.miry.ui.PanelContext;
import com.miry.ui.nodegraph.NodeGraph;
import com.miry.ui.panels.Panel;

public final class NodeGraphPanel extends Panel {
    private final NodeGraph graph;
    int vx, vy, vw, vh;

    public NodeGraphPanel(NodeGraph graph) {
        super("Node Graph");
        this.graph = graph;
    }

    @Override
    public void render(PanelContext ctx) {
        vx = ctx.x();
        vy = ctx.y();
        vw = ctx.width();
        vh = ctx.height();

        ctx.renderer().drawRect(vx, vy, vw, vh, 0xFF14141A);
        if (graph != null) {
            graph.render(ctx.renderer(), vx, vy, vw, vh);
        }
    }
}
