package com.miry.demo;

import com.miry.core.Application;
import com.miry.core.Input;
import com.miry.graphics.Framebuffer;
import com.miry.graphics.ScreenColorSampler;
import com.miry.graphics.Texture;
import com.miry.graphics.batch.BatchRenderer;
import com.miry.graphics.post.GaussianBlur;
import com.miry.platform.InputConstants;
import com.miry.platform.MiryContext;
import com.miry.platform.MiryHost;
import com.miry.ui.Ui;
import com.miry.ui.UiContext;
import com.miry.ui.event.KeyEvent;
import com.miry.ui.event.TextInputEvent;
import com.miry.ui.event.UiEvent;
import com.miry.ui.font.FontAtlas;
import com.miry.ui.font.FontData;
import com.miry.ui.font.TextRenderer;
import com.miry.ui.input.UiInput;
import com.miry.ui.layout.DockSpace;
import com.miry.ui.layout.LeafNode;
import com.miry.ui.layout.SplitNode;
import com.miry.ui.nodegraph.NodeGraph;
import com.miry.ui.theme.Theme;
import com.miry.ui.window.WindowManager;
import com.miry.ui.widgets.EyedropperButton;
import com.miry.ui.widgets.ToastManager;
import com.miry.ui.widgets.Viewport3D;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;

/**
 * Main showcase application demonstrating the capabilities of the Miry UI library.
 * <p>
 * This demo includes:
 * <ul>
 *     <li>SDF-based rendering of primitives and fonts.</li>
 *     <li>Vector icon system.</li>
 *     <li>Node graph editor.</li>
 *     <li>3D viewport with gizmo.</li>
 *     <li>Numeric expressions and unit conversions in input fields.</li>
 *     <li>Virtualized lists and trees.</li>
 *     <li>Floating windows with backdrop blur.</li>
 * </ul>
 */
public final class ShowcaseDemoApplication extends Application {

    private GaussianBlur blur;


    private Viewport3D viewport3d;
    private ViewportPanel viewportPanel;

    private final NodeGraph nodeGraph = new NodeGraph();
    private NodeGraphPanel nodeGraphPanel;

    private InputPanel inputPanel;
    private WidgetsShowcasePanel widgetsPanel;
    private PrimitivesPanel primitivesPanel;

    private ComponentPanel componentPanel;

    public ShowcaseDemoApplication() {
        super(30_000);
    }

    @Override
    protected void onInit() {

        installFont();

        viewport3d = new Viewport3D();
        viewportPanel = new ViewportPanel();
        viewportPanel.setViewport(viewport3d);

        uiFramebuffer = new Framebuffer();
        blur = new GaussianBlur();


        var search = windowManager.create("Search", 120, 110, 460, 280);
        search.setBackdropBlur(true);
        search.setContent((r, ctx, in, th, x, y, w, h) -> {
            float base = r.baselineForBox(y, 24);
            r.drawText("Floating windows + blur", x, base, Theme.toArgb(th.text));
            r.drawText("Drag/resize me. Click the X to close.", x, r.baselineForBox(y + 26, 24), Theme.toArgb(th.textMuted));
            r.drawText("Icons are vector strokes; text is bitmap coverage.", x, r.baselineForBox(y + 52, 24), Theme.toArgb(th.textMuted));
        });

        // Populate node graph content.
        DemoNodeGraphs.populateBasic(nodeGraph);
        nodeGraphPanel = new NodeGraphPanel(nodeGraph);

        // Create new widgets demo panel

        // Layout:
        // Left: primitives + new widgets
        primitivesPanel = new PrimitivesPanel();
        LeafNode primitives = new LeafNode(primitivesPanel);
        primitives.setBackgroundArgb(Theme.toArgb(theme.panelBg));
        LeafNode hello = new LeafNode(new ComponentPanel());
        hello.setBackgroundArgb(Theme.toArgb(theme.panelBg));
        SplitNode leftCol = new SplitNode(hello, primitives , true, 0.5f);

        // Center: viewport + input panel
        LeafNode viewportLeaf = new LeafNode(viewportPanel);
        viewportLeaf.setBackgroundArgb(0xFF15151A);

        inputPanel = new InputPanel(viewport3d, eyedropper, toasts, () -> pickedColorArgb);
        LeafNode inputLeaf = new LeafNode(inputPanel);
        inputLeaf.setBackgroundArgb(Theme.toArgb(theme.panelBg));

        widgetsPanel = new WidgetsShowcasePanel(toasts, eyedropper, viewport3d);
        LeafNode widgetsLeaf = new LeafNode(widgetsPanel);
        widgetsLeaf.setBackgroundArgb(Theme.toArgb(theme.panelBg));

        SplitNode centerBottom = new SplitNode(inputLeaf, widgetsLeaf, false, 0.48f);
        SplitNode centerCol = new SplitNode(viewportLeaf, centerBottom, true, 0.62f);

        // Right: tokens + node graph
        LeafNode tokens = new LeafNode(new TokenShowcasePanel());
        tokens.setBackgroundArgb(Theme.toArgb(theme.panelBg));
        LeafNode graph = new LeafNode(nodeGraphPanel);
        graph.setBackgroundArgb(Theme.toArgb(theme.panelBg));
        SplitNode rightCol = new SplitNode(tokens, graph, true, 0.52f);

        SplitNode leftAndCenter = new SplitNode(leftCol, centerCol, false, 0.30f);
        SplitNode root = new SplitNode(leftAndCenter, rightCol, false, 0.78f);
        dockSpace.setRoot(root);
    }

    @Override
    protected void onUpdate(float dt, MiryHost host) {
        // Detect input activity for hysteresis

        float mx = host.getMousePos().x;
        float my = host.getMousePos().y;
        float sy = (float) Input.consumeScrollY(); // Note: consume call moves here to capture value

        boolean inputActive = false;
        if (Math.abs(mx - prevMouseX) > 0.1f || Math.abs(my - prevMouseY) > 0.1f) inputActive = true;
        if (Math.abs(sy) > 0.001f) inputActive = true;
        if (host.isMouseDown(InputConstants.MOUSE_BUTTON_LEFT) || host.isMouseDown(InputConstants.MOUSE_BUTTON_RIGHT)) inputActive = true;
        // (Keyboard checks handled in processEvents or by focus check)

        if (inputActive) {
            lastInteractionTime = System.nanoTime() / 1_000_000_000.0;
        }
        prevMouseX = mx;
        prevMouseY = my;

        if (primitivesPanel != null) {
            primitivesPanel.update(dt);
        }
        toasts.update(dt);
        if (widgetsPanel != null) {
            widgetsPanel.update(dt);
        }

        // Input state update
        Vector2f mp = host.getMousePos(); // Re-read or reuse mx/my
        boolean left = host.isMouseDown(InputConstants.MOUSE_BUTTON_LEFT);
        boolean right = host.isMouseDown(InputConstants.MOUSE_BUTTON_RIGHT);
        boolean leftPressed = left && !prevLeft;
        boolean leftReleased = !left && prevLeft;
        boolean rightPressed = right && !prevRight;
        boolean rightReleased = !right && prevRight;
        prevLeft = left;
        prevRight = right;

        boolean ctrlDown = host.isKeyDown(InputConstants.KEY_LEFT_CONTROL) || host.isKeyDown(InputConstants.KEY_RIGHT_CONTROL);
        boolean shiftDown = host.isKeyDown(InputConstants.KEY_LEFT_SHIFT) || host.isKeyDown(InputConstants.KEY_RIGHT_SHIFT);
        boolean altDown = host.isKeyDown(InputConstants.KEY_LEFT_ALT) || host.isKeyDown(InputConstants.KEY_RIGHT_ALT);
        boolean superDown = host.isKeyDown(InputConstants.KEY_LEFT_SUPER) || host.isKeyDown(InputConstants.KEY_RIGHT_SUPER);

        input.setMousePos(mp.x, mp.y)
            .setMouseButtons(left, leftPressed, leftReleased)
            .setModifiers(ctrlDown, shiftDown, altDown, superDown)
            .setScrollY(sy); // Use the consumed value

        ui.beginFrame(input, dt);
        uiContext.update(dt);

        int w = host.getWindowWidth();
        int h = host.getWindowHeight();
        dockSpace.resize(w, h);
        windowManager.update(uiContext, input, w, h);
        boolean block = windowManager.blocksInput()
            || uiContext.pointer().hasCaptured()
            || (widgetsPanel != null && widgetsPanel.blocksBackgroundInput());
        if (!block) {
            dockSpace.update(input);
        }

        // Viewport input.
        if (viewport3d != null && !block) {
            viewport3d.updateInput(
                input,
                right,
                rightPressed,
                rightReleased,
                viewportPanel.vx,
                viewportPanel.vy,
                viewportPanel.vw,
                viewportPanel.vh,
                host.getFramebufferScaleX(),
                host.getFramebufferScaleY()
            );
        }

        // Node graph interaction in its panel.
        if (nodeGraph != null && !block) {
            boolean middleDown = host.isMouseDown(InputConstants.MOUSE_BUTTON_MIDDLE);
            boolean spaceDown = host.isKeyDown(InputConstants.KEY_SPACE);
            nodeGraph.setUndoStack(uiContext.undo());
            nodeGraph.setClipboard(uiContext.clipboard());
            nodeGraph.update(
                input,
                middleDown,
                spaceDown,
                ctrlDown,
                shiftDown,
                nodeGraphPanel.vx,
                nodeGraphPanel.vy,
                nodeGraphPanel.vw,
                nodeGraphPanel.vh
            );
        }

        processEvents(block);
    }



    @Override
    protected void onRender(MiryHost host) {
        int w = host.getWindowWidth();
        int h = host.getWindowHeight();
        int fbW = host.getFramebufferWidth();
        int fbH = host.getFramebufferHeight();

        // Render viewport texture at current panel size.
        if (viewport3d != null && viewportPanel.vw > 4 && viewportPanel.vh > 4) {
            int pxW = Math.max(1, Math.round(viewportPanel.vw * host.getFramebufferScaleX()));
            int pxH = Math.max(1, Math.round(viewportPanel.vh * host.getFramebufferScaleY()));
            viewport3d.renderToTexture(pxW, pxH, fbW, fbH, (float) host.getTime());
        }

        uiFramebuffer.ensureSize(fbW, fbH);
        try (Framebuffer.Binding ignored = uiFramebuffer.bindScoped()) {
            Vector4f bg = ui.theme().windowBg.toVector4f();
            org.lwjgl.opengl.GL11.glClearColor(bg.x, bg.y, bg.z, 1.0f);
            org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT);

            batch.begin(w, h, host.getFramebufferScale());
            dockSpace.render(batch);
            uiContext.overlay().render(batch);
            batch.end();
        }

        // Optimization: only blur if windows are actually open.
        // Full-screen blur is expensive (multiple passes).
        Texture blurred = null;
        if (windowManager.hasWindows()) {
             blurred = blur.blur(uiFramebuffer.colorTexture(), fbW, fbH, 1);
        }

        batch.begin(w, h, host.getFramebufferScale());
        batch.drawTexturedRect(uiFramebuffer.colorTexture(), 0, 0, w, h, 0.0f, 1.0f, 1.0f, 0.0f, 0xFFFFFFFF);
        windowManager.render(batch, uiContext, input, ui.theme(), w, h, blurred);
        toasts.render(batch, ui.theme(), w, h);
        batch.end();

        // Fulfill eyedropper sample requests after the frame is drawn (reads from backbuffer).
        EyedropperButton.SampleRequest req = eyedropper.consumeSampleRequest();
        if (req != null) {
            int px = Math.round(req.x() * host.getFramebufferScaleX());
            int py = fbH - 1 - Math.round(req.y() * host.getFramebufferScaleY());
            pickedColorArgb = ScreenColorSampler.sampleArgb(px, py);
            toasts.show(String.format("Picked #%06X", pickedColorArgb & 0x00FFFFFF));
        }
    }

    @Override
    protected void onShutdown() {
        if (viewport3d != null) viewport3d.close();
        if (blur != null) blur.close();
    }
}
