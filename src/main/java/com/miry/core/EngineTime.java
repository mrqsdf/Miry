package com.miry.core;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

/**
 * Frame timing helper used by {@link Engine}.
 */
public final class EngineTime {
    private double timeSeconds;
    private float deltaSeconds;
    private double lastTime;

    public void init() {
        lastTime = glfwGetTime();
        timeSeconds = lastTime;
        deltaSeconds = 0.0f;
    }

    public void tick() {
        double now = glfwGetTime();
        deltaSeconds = (float) (now - lastTime);
        lastTime = now;
        timeSeconds = now;
    }

    public float deltaTime() {
        return deltaSeconds;
    }

    public double time() {
        return timeSeconds;
    }
}
