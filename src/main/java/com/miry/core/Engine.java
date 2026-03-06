package com.miry.core;

import com.miry.platform.MiryContext;
import com.miry.platform.MiryHost;
import com.miry.platform.glfw.GlfwFMiryHost;
import org.lwjgl.opengl.GL11;

import java.util.Objects;

/**
 * The main engine loop for Miry applications.
 * <p>
 * The {@code Engine} class is responsible for creating the application window, initializing the input system,
 * setting up the platform host, and managing the main application loop. It drives the {@link Application}
 * lifecycle methods.
 * </p>
 */
public final class Engine {
    private final Window window;
    private final EngineTime time = new EngineTime();
    private final Application application;
    private final MiryHost host;

    /**
     * Creates a new Engine instance.
     *
     * @param title       The title of the application window.
     * @param width       The initial width of the window, in screen coordinates.
     * @param height      The initial height of the window, in screen coordinates.
     * @param application The {@link Application} instance to run. Must not be null.
     * @throws NullPointerException if {@code application} is null.
     */
    public Engine(String title, int width, int height, Application application) {
        this.window = new Window(title, width, height);
        Input.init(window.getNativeWindow());
        this.host = new GlfwFMiryHost(window);
        MiryContext.setHost(host);
        this.application = Objects.requireNonNull(application, "application");
    }

    /**
     * Starts the engine and enters the main loop.
     * <p>
     * This method initializes the application, then continuously polls events, updates, and renders
     * the application until the window is closed. Finally, it shuts down the application and releases resources.
     * </p>
     */
    public void run() {
        MiryHangWatchdog watchdog = MiryHangWatchdog.startForCurrentThread();
        if (watchdog != null) watchdog.stage("engine.time.init");
        time.init();
        if (watchdog != null) watchdog.stage("app.onInit");
        application.init();

        while (!window.shouldClose()) {
            boolean continuous = application.isContinuous();
            boolean needed = application.needsRepaint();

            if (continuous || needed) {
                if (watchdog != null) watchdog.stage("window.pollEvents");
                window.pollEvents();
            } else {
                if (watchdog != null) watchdog.stage("window.waitEventsTimeout");
                window.waitEvents(0.400);
            }

            if (watchdog != null) watchdog.stage("engine.time.tick");
            time.tick();
            if (watchdog != null) watchdog.stage("app.onUpdate");
            MiryHost host = MiryContext.host();
            application.update(time.deltaTime(), host);

            if (watchdog != null) watchdog.stage("gl.clear");
            GL11.glViewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());
            GL11.glClearColor(0.08f, 0.08f, 0.10f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            if (watchdog != null) watchdog.stage("app.onRender");
            application.render(host);
            if (watchdog != null) watchdog.stage("window.swapBuffers");
            window.swapBuffers();
        }

        if (watchdog != null) watchdog.stage("app.onShutdown");
        application.onShutdown();
        window.close();
        if (watchdog != null) watchdog.close();
    }
}
