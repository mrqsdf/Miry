package com.miry.ui.render;

import com.miry.graphics.Texture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Backend-agnostic UI draw list.
 * <p>
 * The UI layer can record rendering commands into a {@link UiDrawList} without touching OpenGL.
 * A {@link UiRenderBackend} consumes the list to render using any graphics API.
 */
public final class UiDrawList {
    public sealed interface Command permits
            UiDrawList.PushClip,
            UiDrawList.PopClip,
            UiDrawList.Rect,
            UiDrawList.RoundedRect,
            UiDrawList.TexturedRect,
            UiDrawList.Text {
    }

    public record PushClip(int x, int y, int width, int height) implements Command {
        public PushClip {
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("Clip width/height must be >= 0");
            }
        }
    }

    public record PopClip() implements Command {
    }

    public record Rect(float x, float y, float w, float h, int argb) implements Command {
    }

    public record RoundedRect(float x,
                              float y,
                              float w,
                              float h,
                              float radiusPx,
                              int fillTL,
                              int fillTR,
                              int fillBR,
                              int fillBL,
                              float borderPx,
                              int strokeArgb) implements Command {
    }

    public record TexturedRect(Texture texture,
                               float x, float y, float w, float h,
                               float u0, float v0, float u1, float v1,
                               int argb) implements Command {
        public TexturedRect {
            Objects.requireNonNull(texture, "texture");
        }
    }

    public record Text(String text, float x, float y, int argb) implements Command {
        public Text {
            Objects.requireNonNull(text, "text");
        }
    }

    private final List<Command> commands = new ArrayList<>(512);

    public void clear() {
        commands.clear();
    }

    public void add(Command command) {
        commands.add(Objects.requireNonNull(command, "command"));
    }

    public List<Command> commands() {
        return Collections.unmodifiableList(commands);
    }
}
