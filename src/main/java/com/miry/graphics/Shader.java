package com.miry.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDetachShader;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Minimal OpenGL shader program wrapper with convenience resource loading.
 */
public final class Shader implements AutoCloseable {
    private final int programId;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

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

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public int id() {
        return programId;
    }

    public void setUniform(String name, int value) {
        glUniform1i(location(name), value);
    }

    public void setUniform(String name, float value) {
        glUniform1f(location(name), value);
    }

    public void setUniform(String name, Vector3f value) {
        glUniform3f(location(name), value.x, value.y, value.z);
    }

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
                throw new IllegalArgumentException("Unknown uniform: " + n);
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

    @Override
    public void close() {
        glDeleteProgram(programId);
    }
}
