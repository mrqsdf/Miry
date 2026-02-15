package com.miry.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Minimal OpenGL shader program wrapper with convenience resource loading.
 * <p>
 * This class handles compiling vertex and fragment shaders, linking them into a program,
 * and setting uniform values. It also provides a utility for loading shader source code from the classpath.
 * </p>
 */
public final class Shader implements AutoCloseable {
    private final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    /**
     * Creates and links a new shader program from source strings.
     *
     * @param vertexSource   The source code for the vertex shader.
     * @param fragmentSource The source code for the fragment shader.
     * @throws IllegalStateException if compilation or linking fails.
     */
    public Shader(String vertexSource, String fragmentSource) {
        int vertexId = compile(GL_VERTEX_SHADER, vertexSource);
        int fragmentId = compile(GL_FRAGMENT_SHADER, fragmentSource);

        programId = glCreateProgram();
        glAttachShader(programId, vertexId);
        glAttachShader(programId, fragmentId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(programId);
            glDetachShader(programId, vertexId);
            glDetachShader(programId, fragmentId);
            glDeleteShader(vertexId);
            glDeleteShader(fragmentId);
            glDeleteProgram(programId);
            throw new IllegalStateException("Shader link failed:\n" + log);
        }

        glDetachShader(programId, vertexId);
        glDetachShader(programId, fragmentId);
        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
    }

    /**
     * Installs this shader program as part of the current rendering state.
     */
    public void bind() {
        glUseProgram(programId);
    }

    /**
     * Removes the current shader program from the rendering state (binds 0).
     */
    public void unbind() {
        glUseProgram(0);
    }

    /**
     * Gets the OpenGL program ID.
     *
     * @return The program ID.
     */
    public int id() {
        return programId;
    }

    /**
     * Sets a uniform integer value (e.g., sampler binding).
     *
     * @param name  The name of the uniform variable.
     * @param value The value to set.
     */
    public void setUniform(String name, int value) {
        glUniform1i(location(name), value);
    }

    /**
     * Sets a uniform float value.
     *
     * @param name  The name of the uniform variable.
     * @param value The value to set.
     */
    public void setUniform(String name, float value) {
        glUniform1f(location(name), value);
    }

    /**
     * Sets a uniform vec2 value.
     *
     * @param name The name of the uniform variable.
     * @param x    The x component.
     * @param y    The y component.
     */
    public void setUniform2f(String name, float x, float y) {
        glUniform2f(location(name), x, y);
    }

    /**
     * Sets a uniform vec3 value.
     *
     * @param name  The name of the uniform variable.
     * @param value The vector value.
     */
    public void setUniform(String name, Vector3f value) {
        glUniform3f(location(name), value.x, value.y, value.z);
    }

    /**
     * Sets a uniform mat4 value.
     *
     * @param name  The name of the uniform variable.
     * @param value The matrix value.
     */
    public void setUniform(String name, Matrix4f value) {
        try (MemoryStack stack = stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.get(fb);
            glUniformMatrix4fv(location(name), false, fb);
        }
    }

    private int location(String name) {
        return uniformLocations.computeIfAbsent(name, n -> {
            int loc = glGetUniformLocation(programId, n);
            if (loc < 0) {
                // Warning suppressed for demo purposes, but normally helpful.
                // throw new IllegalArgumentException("Unknown uniform: " + n);
            }
            return loc;
        });
    }

    private static int compile(int type, String source) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0) {
            String log = glGetShaderInfoLog(id);
            glDeleteShader(id);
            throw new IllegalStateException("Shader compile failed:\n" + log);
        }
        return id;
    }

    /**
     * Loads a shader program from resources on the classpath.
     *
     * @param vertexPath   Path to the vertex shader resource (e.g., "shaders/my_shader.vert").
     * @param fragmentPath Path to the fragment shader resource (e.g., "shaders/my_shader.frag").
     * @return A new {@link Shader} instance.
     * @throws IllegalStateException if resource reading or compilation fails.
     */
    public static Shader fromResources(String vertexPath, String fragmentPath) {
        return new Shader(readResourceUtf8(vertexPath), readResourceUtf8(fragmentPath));
    }

    private static String readResourceUtf8(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        try (var in = Shader.class.getClassLoader().getResourceAsStream(normalized)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read resource: " + path, e);
        }
    }

    /**
     * Deletes the OpenGL program object.
     */
    @Override
    public void close() {
        glDeleteProgram(programId);
    }
}