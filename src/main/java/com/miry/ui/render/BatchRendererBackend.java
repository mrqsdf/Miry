package com.miry.ui.render;

import com.miry.graphics.batch.BatchRenderer;

import java.util.List;
import java.util.Objects;

/**
 * {@link UiRenderBackend} implementation backed by {@link BatchRenderer}.
 * <p>
 * This keeps the current OpenGL batch path available while enabling draw-list recording.
 */
public final class BatchRendererBackend implements UiRenderBackend, UiFontMetrics {
    private final BatchRenderer renderer;

    public BatchRendererBackend(BatchRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    @Override
    public void render(UiDrawList drawList, int windowWidth, int windowHeight, float scaleFactor) {
        Objects.requireNonNull(drawList, "drawList");

        renderer.begin(windowWidth, windowHeight, scaleFactor);
        try {
            List<UiDrawList.Command> commands = drawList.commands();
            for (UiDrawList.Command command : commands) {
                if (command instanceof UiDrawList.PushClip c) {
                    renderer.pushClip(c.x(), c.y(), c.width(), c.height());
                } else if (command instanceof UiDrawList.PopClip) {
                    renderer.popClip();
                } else if (command instanceof UiDrawList.Rect r) {
                    renderer.drawRect(r.x(), r.y(), r.w(), r.h(), r.argb());
                } else if (command instanceof UiDrawList.RoundedRect rr) {
                    renderer.drawRoundedRect(rr.x(), rr.y(), rr.w(), rr.h(), rr.radiusPx(), rr.fillTL(), rr.fillTR(), rr.fillBR(), rr.fillBL(), rr.borderPx(), rr.strokeArgb());
                } else if (command instanceof UiDrawList.TexturedRect t) {
                    renderer.drawTexturedRect(t.texture(), t.x(), t.y(), t.w(), t.h(), t.u0(), t.v0(), t.u1(), t.v1(), t.argb());
                } else if (command instanceof UiDrawList.Text t) {
                    renderer.drawText(t.text(), t.x(), t.y(), t.argb());
                }
            }
        } finally {
            renderer.end();
        }
    }

    @Override
    public float measureText(String text) {
        return renderer.measureText(text);
    }

    @Override
    public float lineHeight() {
        return renderer.lineHeight();
    }

    @Override
    public float ascent() {
        return renderer.ascent();
    }
}
