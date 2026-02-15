package com.miry.demo;

import com.miry.core.Engine;


public final class Launcher {

    private Launcher() {
    }
    public static void main(String[] args) {
        new Engine("Miry UI - Showcase Demo", 1580, 920, new ShowcaseDemoApplication()).run();
    }
}