
# Miry

UI toolkit for Java + LWJGL (OpenGL), built around a small immediate-mode core and level editor widgets (node graphs, color picker, modals, scroll views) plus a 3D viewport widget with gizmo interaction.

## Requirements

- Java 17+
- OpenGL 3.3+
- Windows/macOS/Linux (LWJGL natives)
## Font

- Default: loads `/fonts/default.ttf` from resources if present, otherwise falls back to common system fonts
- Override: set `MIRY_FONT_PATH` to a `.ttf`/`.otf` file (or legacy `MYRI_FONT_PATH` / `FLUX_FONT_PATH`)

## Embedding

Widgets render through `com.miry.ui.render.UiRenderer`. The demo uses `com.miry.graphics.batch.BatchRenderer` (OpenGL sprite batch), but you can swap this for your own renderer as long as it implements `UiRenderer`.
## Status

Work-in-progress editor UI library; APIs and visuals are expected to evolve.
