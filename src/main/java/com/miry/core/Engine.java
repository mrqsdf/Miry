package com.miry.core;

import com.miry.platform.MiryContext;
import com.miry.platform.MiryHost;
import com.miry.platform.glfw.GlfwFMiryHost;
import org.lwjgl.opengl.GL11;

import java.util.Objects;

/**
 * Minimal engine loop for the demo applications.
 * <p>
 * Owns the window/host and calls the {@link Application} lifecycle methods.
 */
public final class Engine {
    private final Window window;
    private final EngineTime time = new EngineTime();
    private final Application application;
    private final MiryHost host;

    public Engine(String title, int width, int height, Application application) {
        this.window = new Window(title, width, height);
        Input.init(window.getNativeWindow());
        this.host = new GlfwFMiryHost(window);
        MiryContext.setHost(host);
        this.application = Objects.requireNonNull(application, "application");
    }

    public void run() {
        time.init();
        application.onInit();

        while (!window.shouldClose()) {
            time.tick();

            window.pollEvents();
            application.onUpdate(time.deltaTime());

            GL11.glViewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());
            GL11.glClearColor(0.08f, 0.08f, 0.10f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            application.onRender();
            window.swapBuffers();
        }

        application.onShutdown();
        window.close();
    }
}
