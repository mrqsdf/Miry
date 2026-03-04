package com.miry.ui.component;

import org.joml.Vector4f;

public class Color {

    private int argb;
    private float r;
    private float g;
    private float b;
    private float a;

    public Color(int argb) {
        this.argb = argb;
        this.a = ((argb >> 24) & 0xFF) / 255f;
        this.r = ((argb >> 16) & 0xFF) / 255f;
        this.g = ((argb >> 8) & 0xFF) / 255f;
        this.b = (argb & 0xFF) / 255f;
    }

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        int ir = clamp255(Math.round(r * 255f));
        int ig = clamp255(Math.round(g * 255f));
        int ib = clamp255(Math.round(b * 255f));
        int ia = clamp255(Math.round(a * 255f));
        this.argb = (ia << 24) | (ir << 16) | (ig << 8) | ib;
    }

    public Color(int r, int g, int b, int a) {
        this.r = r / 255f;
        this.g = g / 255f;
        this.b = b / 255f;
        this.a = a / 255f;
        this.argb = (a << 24) | (r << 16) | (g << 8) | b;
    }

    public Color(java.awt.Color color) {
        this.r = color.getRed() / 255f;
        this.g = color.getGreen() / 255f;
        this.b = color.getBlue() / 255f;
        this.a = color.getAlpha() / 255f;
        int ir = clamp255(Math.round(r * 255f));
        int ig = clamp255(Math.round(g * 255f));
        int ib = clamp255(Math.round(b * 255f));
        int ia = clamp255(Math.round(a * 255f));
        this.argb = (ia << 24) | (ir << 16) | (ig << 8) | ib;
    }

    public Color(Vector4f vec) {
        this(vec.x, vec.y, vec.z, vec.w);
    }

    public int getArgb() {
        return argb;
    }

    public float getR() {
        return r;
    }

    public float getG() {
        return g;
    }

    public float getB() {
        return b;
    }

    public float getA() {
        return a;
    }

    public java.awt.Color toAwtColor() {
        return new java.awt.Color(r, g, b, a);
    }


    public Vector4f toVector4f() {
        return new Vector4f(r, g, b, a);
    }

    public Color set(Color color){
        this.argb = color.argb;
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
        return this;
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
