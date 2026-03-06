package com.miry.core;

import com.miry.graphics.Framebuffer;
import com.miry.graphics.ScreenColorSampler;
import com.miry.graphics.Texture;
import com.miry.graphics.batch.BatchRenderer;
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
import com.miry.ui.theme.Theme;
import com.miry.ui.widgets.EyedropperButton;
import com.miry.ui.widgets.ToastManager;
import com.miry.ui.window.WindowManager;
import org.joml.Vector4f;

import java.nio.ByteBuffer;

/**
 * Base class for host application callbacks invoked by the {@link Engine}.
 * <p>
 * This class defines the lifecycle of a Miry application. Subclasses should override these methods
 * to implement their specific logic for initialization, updates, rendering, and cleanup.
 * </p>
 */
public abstract class Application {

    private int batchSize;
    protected BatchRenderer batch;
    protected Theme theme;
    protected FontAtlas fontAtlas;
    protected TextRenderer textRenderer;
    protected Ui ui;
    protected UiContext uiContext;
    protected final UiInput input = new UiInput();
    protected DockSpace dockSpace;
    protected WindowManager windowManager;
    protected ToastManager toasts;
    protected EyedropperButton eyedropper;
    protected Framebuffer uiFramebuffer;
    protected boolean prevLeft;
    protected boolean prevRight;

    protected double lastInteractionTime;
    protected float prevScrollY;
    protected float prevMouseX, prevMouseY;
    protected long window;
    protected int pickedColorArgb = 0xFF4772B3;


    protected Application(int batchSize){
        this.batchSize = batchSize;
    }

    /**
     * Initializes the application. This method is called once by the engine during startup.
     * <p>
     * The default implementation sets up the batch renderer, theme, UI context, and dock space.
     * Subclasses can override {@link #onInit()} to perform additional initialization after this setup.
     * </p>
     */
    protected final void init(){
        lastInteractionTime = System.nanoTime() / 1_000_000_000.0;

        this.batch = new BatchRenderer(batchSize);
        this.theme = new Theme();
        this.ui = new Ui(theme);

        this.window = MiryContext.host().getNativeWindow();

        this.uiContext = new UiContext(window);
        ui.setContext(uiContext);

        this.windowManager = new WindowManager();
        this.toasts = new ToastManager();

        this.eyedropper = new EyedropperButton();

        uiFramebuffer = new Framebuffer();


        this.dockSpace = new DockSpace();
        this.dockSpace.setUi(ui);
        this.dockSpace.setUiContext(uiContext);

        onInit();
    }

    /**
     * Called once during engine initialization, before the main loop starts.
     * <p>
     * Use this method to initialize resources, load assets, and set up the initial state of the application.
     * </p>
     */
    protected void onInit() {}

    /**
     * Called every frame to update the application state.
     *
     * @param dt   The time elapsed since the last frame, in seconds (delta time).
     * @param host The {@link MiryHost} providing access to platform-specific functionality.
     */

    protected final void update(float dt, MiryHost host){

        onUpdate(dt, host);

    }

    /**
     * Called every frame to update the application state.
     *
     * @param dt The time elapsed since the last frame, in seconds (delta time).
     */
    protected void onUpdate(float dt, MiryHost host) {

    }

    /**
     * Called every frame to render the application content.
     * <p>
     * The default implementation calls {@link #onRender(MiryHost)} followed by {@link #finishRender(MiryHost)}.
     * Subclasses can override {@link #onRender(MiryHost)} to implement their rendering logic, while
     * {@link #finishRender(MiryHost)} can be used for any necessary finalization steps after rendering.
     * </p>
     */
    protected final void render(MiryHost host) {

        onRender(host);
        finishRender(host);
    }

    /**
     * Called every frame to render the application content.
     * <p>
     * This method is invoked after the screen has been cleared. OpenGL commands can be issued here.
     * </p>
     */
    protected void onRender(MiryHost host) {}

    /**
     * Called after {@link #onRender(MiryHost)} to perform any necessary finalization steps.
     * <p>
     * This method is invoked after all rendering for the frame is complete. It can be used to
     * perform cleanup, swap buffers, or execute any post-render logic.
     * </p>
     */
    private void finishRender(MiryHost host) {
    }

    /**
     * Shuts down the application and releases resources. This method is called once by the engine during shutdown.
     * <p>
     * The default implementation calls {@link #onShutdown()} and then closes the framebuffer, font atlas,
     * UI context, and batch renderer. Subclasses can override {@link #onShutdown()} to perform additional cleanup
     * before these resources are released.
     * </p>
     */
    protected final void shutdown() {
        onShutdown();
        if (uiFramebuffer != null) uiFramebuffer.close();
        if (fontAtlas != null) fontAtlas.close();
        if (uiContext != null) uiContext.close();
        if (batch != null) batch.close();
    }

    /**
     * Called once when the engine is shutting down.
     * <p>
     * Use this method to release resources, save state, and perform any necessary cleanup.
     * </p>
     */
    protected void onShutdown() {}

    /**
     * Indicates whether the application requires continuous rendering (like a game or 3D view).
     * <p>
     * If {@code true}, the engine will render every frame as fast as possible.
     * If {@code false}, the engine will wait for input events or active animations before rendering,
     * effectively sleeping to save CPU/GPU resources.
     * </p>
     * <p>
     * Override this to return {@code false} for standard GUI applications.
     * </p>
     *
     * @return {@code true} for continuous rendering (default), {@code false} for lazy rendering.
     */
    public boolean isContinuous() {
        // Smart optimization: Render continuously if user interacted recently (hysteresis).
        double now = System.nanoTime() / 1_000_000_000.0;
        return (now - lastInteractionTime) < 2.0;
    }

    /**
     * Checks if the application has active animations or pending work that requires a repaint.
     * <p>
     * This is polled by the engine when {@link #isContinuous()} is {@code false} to decide whether to
     * sleep or render the next frame.
     * </p>
     *
     * @return {@code true} if a frame should be rendered immediately.
     */
    public boolean needsRepaint() {
        // Wake up for active animations or toasts.
        if (uiContext.animations().activeCount() > 0) return true;
        if (toasts.activeCount() > 0) return true;
        // Keep rendering if a text field is focused (for caret blinking)
        return uiContext.focus().hasAnyFocus();
    }


    /**
     * Internal method called by the engine to process UI events.
     * <p>
     * This method polls events from the {@link UiContext} and dispatches them to the {@link DockSpace}.
     * It also updates the idle timer based on user interactions.
     * </p>
     *
     * @param blockedByWindow Indicates if the event processing is currently blocked by an open window.
     */
    protected void processEvents(boolean blockedByWindow) {
        UiEvent event;
        while ((event = uiContext.pollEvent()) != null) {
            // Any event resets the idle timer
            lastInteractionTime = System.nanoTime() / 1_000_000_000.0;

            dockSpace.processEvents(blockedByWindow, event);
        }
    }

    /**
     * Internal method to set up the default font atlas and text renderer.
     */
    protected void installFont() {
        ByteBuffer fontData = FontData.loadDefault();
        float scale = Math.max(0.1f, MiryContext.host().getFramebufferScale());
        int atlasSize = Math.min(2048, Math.max(1024, Math.round(768.0f * scale)));
        fontAtlas = new FontAtlas(fontData, 20.0f, atlasSize, scale, FontAtlas.Mode.COVERAGE);
        textRenderer = new TextRenderer(fontAtlas);
        batch.setTextRenderer(textRenderer);
    }
}
